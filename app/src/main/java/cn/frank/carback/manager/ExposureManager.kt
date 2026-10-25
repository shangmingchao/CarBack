package cn.frank.carback.manager

import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.util.LruCache
import android.view.Choreographer
import android.view.View
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import java.lang.ref.WeakReference
import java.util.WeakHashMap

/**
 * 曝光 & 点击上报管理类（[EventTracker] 的默认实现）
 *
 * 曝光语义：**同一个 key 只上报一次，只有新的 key 才会触发曝光**。
 * - key 由业务提供（[ExposureKeyable.exposureKey] 或 key lambda）
 * - 已经上报过的 key 进程内永久去重，只有调用 [clearDeduplication]、[resetExposure]
 *   或 [release] 后才能再次触发曝光（下拉刷新请用 [resetExposure]）
 * - 页面 PAUSE 时停止检测并清空停留计时，回到前台后重新累计
 *
 * @author shangmingchao
 */
class ExposureManager(private val reporter: ExposureReporter, private val config: ExposureConfig = ExposureConfig()) :
    EventTracker,
    DefaultLifecycleObserver {

    companion object {
        private const val TAG = "ExposureManager"
    }

    /** 单个被追踪的 View 条目 */
    private class TrackedItem(
        val viewRef: WeakReference<View>,
        var key: String,
        var data: Any?,
        /** 所在容器的索引（如外层列表的 adapter position），默认 0 */
        var parentIndex: Int = 0,
        /** 当前内容在容器内的索引（如内层列表的 adapter position / banner 页码），默认 0 */
        var childIndex: Int = 0,
    ) {
        /** 当前连续可见的起始时刻（elapsedRealtime），0 表示当前不可见 */
        var visibleSince: Long = 0L

        /** 本轮连续可见会话内是否已完成曝光判定（会话内只上报一次，会话结束会自动重置） */
        var reported: Boolean = false

        val view: View? get() = viewRef.get()
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val choreographer by lazy { Choreographer.getInstance() }

    /**
     * 追踪表
     */
    private val items = WeakHashMap<View, TrackedItem>()

    /** 去重缓存：key -> 最近一次上报的 elapsedRealtime */
    private val deduplicationCache = LruCache<String, Long>(config.deduplicationCacheSize.coerceAtLeast(1))

    /** 业务手动标记为「已曝光」的 key，一直抑制直到 [clearDeduplication] 解除 */
    private val markedKeys = HashSet<String>()

    private val pending = ArrayList<ExposureRecord>(16)

    /** 单帧批次内已入队的 key，保证同一批次（同一帧）内同 key 只上报一次 */
    private val pendingKeys = HashSet<String>()

    private val visibleRect = Rect()

    private var frameLoopRunning = false
    private var lastCheckTime = 0L
    private var resumed = true
    private var destroyed = false
    private var lifecycleRef: WeakReference<Lifecycle>? = null

    /** 是否存在「已可见但停留时长未达标」的 item：只有这类 item 才需要逐帧精度 */
    private var needsFramePrecision = false

    /** 空闲低频轮询是否已入队（无待判定 item 时代替帧回调，避免持续请求 VSYNC） */
    private var idleCheckScheduled = false

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!frameLoopRunning) return
            val now = SystemClock.elapsedRealtime()
            Log.d(TAG, "帧检测中...")
            if (now - lastCheckTime >= config.checkIntervalMillis) {
                lastCheckTime = now
                runCatching { needsFramePrecision = checkAll(now) }
                    .onFailure { Log.e(TAG, "曝光检测异常，已跳过本次检测") }
            }
            if (!needsFramePrecision) {
                stopFrameLoop()
            } else {
                choreographer.postFrameCallback(this)
            }
        }
    }

    /**
     * 空闲轮询：无待判定 item 时以 [ExposureConfig.idleCheckIntervalMillis] 的粒度检测。
     *
     * 只负责发现「有 item 变为可见」并交回帧循环，因此不需要帧级精度；
     * 代价是最多延迟一个空闲周期才开始累计停留时长（曝光会晚报，不会漏报，
     * 除非该 item 的可见时长短于一个空闲周期）。
     */
    private val idleCheckRunnable = object : Runnable {
        override fun run() {
            idleCheckScheduled = false
            if (destroyed || !resumed) return
            val now = SystemClock.elapsedRealtime()
            runCatching { needsFramePrecision = checkAll(now) }
                .onFailure { Log.e(TAG, "曝光检测异常，已跳过本次检测") }
            if (needsFramePrecision) {
                startFrameLoopIfNeeded()
            } else {
                scheduleIdleCheck()
            }
        }
    }

    /**
     * 可选：绑定 LifecycleOwner。
     *  - ON_RESUME ：开始/恢复曝光检测
     *  - ON_PAUSE  ：停止检测（退后台期间不检测、不上报）
     *  - ON_DESTROY：自动 [release]，解绑观察者并清空全部引用，防止内存泄漏
     */
    @MainThread
    fun bindLifecycle(owner: LifecycleOwner) {
        assertMainThread("bindLifecycle")
        lifecycleRef?.get()?.removeObserver(this) // 重复绑定时先解绑旧 owner，避免旧生命周期继续驱动
        lifecycleRef = WeakReference(owner.lifecycle)
        owner.lifecycle.addObserver(this)
        resumed = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        startFrameLoopIfNeeded()
    }

    /** 未绑定 LifecycleOwner 时手动驱动（等价于收到 ON_RESUME） */
    @MainThread
    @Suppress("unused")
    fun resume() {
        assertMainThread("resume")
        resumed = true
        startFrameLoopIfNeeded()
    }

    /** 未绑定 LifecycleOwner 时手动驱动（等价于收到 ON_PAUSE） */
    @MainThread
    @Suppress("unused")
    fun pause() {
        assertMainThread("pause")
        resumed = false
        onPaused()
    }

    /** 释放全部资源：解绑 Lifecycle、清空追踪与去重缓存。绑定 LifecycleOwner 时 ON_DESTROY 自动调用 */
    @MainThread
    fun release() {
        assertMainThread("release")
        if (destroyed) return
        destroyed = true
        stopFrameLoop()
        mainHandler.removeCallbacksAndMessages(null)
        lifecycleRef?.get()?.removeObserver(this)
        lifecycleRef = null
        items.clear()
        pending.clear()
        pendingKeys.clear()
        markedKeys.clear()
        deduplicationCache.evictAll()
    }

    override fun onResume(owner: LifecycleOwner) {
        resumed = true
        startFrameLoopIfNeeded()
    }

    override fun onPause(owner: LifecycleOwner) {
        resumed = false
        onPaused()
    }

    override fun onDestroy(owner: LifecycleOwner) = release()

    private fun onPaused() {
        stopFrameLoop()
        val iterator = items.values.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.view == null) {
                item.data = null
                iterator.remove()
            } else {
                resetDwell(item)
            }
        }
    }

    @AnyThread
    override fun <T> trackExposure(view: View?, data: T, parentIndex: Int, childIndex: Int, keyExtractor: (T) -> String) {
        bindInternal(view, data, keyExtractor(data), parentIndex, childIndex)
    }

    @AnyThread
    override fun trackExposure(view: View?, data: ExposureKeyable, parentIndex: Int, childIndex: Int) {
        bindInternal(view, data, data.exposureKey, parentIndex, childIndex)
    }

    /**
     * 手动解除某个 View 的曝光追踪
     */
    @MainThread
    @Suppress("unused")
    fun removeTracking(view: View) {
        assertMainThread("removeTracking")
        removeInternal(view)
    }

    private fun bindInternal(view: View?, data: Any?, key: String, parentIndex: Int, childIndex: Int) {
        if (destroyed) return
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { bindInternal(view, data, key, parentIndex, childIndex) }
            return
        }
        if (view == null) {
            if (passDeduplication(key, SystemClock.elapsedRealtime(), batchAware = false)) {
                reporter.onExposure(listOf(ExposureRecord(key, data, parentIndex, childIndex, null)))
            }
            return
        }
        val item = items[view]
            ?: TrackedItem(WeakReference(view), key, data, parentIndex, childIndex).also { items[view] = it }
        item.key = key
        item.data = data
        item.parentIndex = parentIndex
        item.childIndex = childIndex
        resetDwell(item)
        startFrameLoopIfNeeded()
    }

    @AnyThread
    override fun <T> trackClick(view: View?, data: T, parentIndex: Int, childIndex: Int) {
        reportClickInternal(view, data, parentIndex, childIndex)
    }

    private fun reportClickInternal(view: View?, data: Any?, parentIndex: Int, childIndex: Int) {
        if (destroyed) return
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { reportClickInternal(view, data, parentIndex, childIndex) }
            return
        }
        reporter.onClick(
            ExposureRecord(key = "", data = data, parentIndex = parentIndex, childIndex = childIndex, view = view),
        )
    }

    /**
     * 强制重新曝光：清空去重缓存，并重置所有已追踪 View 的停留计时与会话状态。
     *
     * 用于下拉刷新 / 重新拉取等场景，保证「无论数据是否变化」都能重新上报：
     * - 数据变化：新 key 本来就会上报，重置不影响结果
     * - 数据不变：同一个 key 被永久去重、且屏幕内的 item 其会话状态为「已上报」
     *   （ListAdapter 提交相同列表时 DiffUtil 判定 no-op，不会重新 onBindViewHolder），
     *   单靠 [clearDeduplication] 解不开会话状态，必须连同计时一起重置
     *
     * 调用时机：列表数据提交完成之后（ListAdapter 的 `submitList(list) {}` 回调内，
     * 或 `notifyDataSetChanged()` 之后 `recyclerView.post { }`）
     */
    @AnyThread
    fun resetExposure() {
        if (destroyed) return
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { resetExposure() }
            return
        }
        deduplicationCache.evictAll()
        markedKeys.clear()
        items.values.forEach(::resetDwell)
        startFrameLoopIfNeeded()
    }

    @AnyThread
    fun clearDeduplication(key: String? = null) {
        if (destroyed) return
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { clearDeduplication(key) }
            return
        }
        if (key == null) {
            deduplicationCache.evictAll()
            markedKeys.clear()
        } else {
            deduplicationCache.remove(key)
            markedKeys.remove(key)
        }
    }

    /**
     * 主动标记已曝光（如：服务端下发的历史数据，本地无需再报）
     *
     * 该标记会一直抑制该 key，直到 [clearDeduplication] 或 [release] 清除
     */
    @AnyThread
    fun markExposed(key: String) {
        if (destroyed) return
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { markExposed(key) }
            return
        }
        markedKeys.add(key)
        deduplicationCache.put(key, SystemClock.elapsedRealtime())
    }

    private fun startFrameLoopIfNeeded() {
        if (destroyed || frameLoopRunning || items.isEmpty()) return
        if (!resumed) return
        cancelIdleCheck()
        frameLoopRunning = true
        choreographer.postFrameCallback(frameCallback)
    }

    private fun stopFrameLoop() {
        if (!frameLoopRunning) return
        frameLoopRunning = false
        choreographer.removeFrameCallback(frameCallback)
        scheduleIdleCheck()
    }

    /** 空闲期不再挂帧回调（避免持续请求 VSYNC），改用 Handler 低频唤醒等待下一个可见时机 */
    private fun scheduleIdleCheck() {
        if (destroyed || !resumed || frameLoopRunning || idleCheckScheduled || items.isEmpty()) return
        idleCheckScheduled = true
        mainHandler.postDelayed(idleCheckRunnable, config.idleCheckIntervalMillis)
    }

    private fun cancelIdleCheck() {
        if (!idleCheckScheduled) return
        idleCheckScheduled = false
        mainHandler.removeCallbacks(idleCheckRunnable)
    }

    private fun checkAll(now: Long): Boolean {
        pending.clear()
        pendingKeys.clear()
        var needsFrame = false
        val iterator = items.values.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            val view = item.view
            if (view == null) {
                item.data = null
                iterator.remove()
                continue
            }
            if (isEffectivelyVisible(view)) {
                if (item.visibleSince == 0L) item.visibleSince = now
                // 一次连续可见会话内只判定一次曝光，避免每帧满足停留时长就重复上报
                if (!item.reported) {
                    if (now - item.visibleSince < config.minDwellMillis) {
                        // 停留时长还没攒够，需要继续逐帧跟踪
                        needsFrame = true
                    } else if (passDeduplication(item.key, now)) {
                        item.reported = true
                        pending.add(ExposureRecord(item.key, item.data, item.parentIndex, item.childIndex, view))
                    }
                }
            } else {
                resetDwell(item)
            }
        }
        if (pending.isNotEmpty()) {
            reporter.onExposure(ArrayList(pending))
        }
        return needsFrame
    }

    /**
     * 有效可见性判定：
     * 1. 自身及所有父 View 可见（isShown）
     * 2. Window 处于可见状态（退后台时 windowVisibility != VISIBLE）
     * 3. 已完成布局（宽高 > 0）
     * 4. getGlobalVisibleRect 计算被父布局裁剪后的可见面积占比 >= 阈值
     * 注：不计算被兄弟 View 完全遮挡的极端场景，与业界主流曝光方案保持一致
     */
    private fun isEffectivelyVisible(view: View): Boolean {
        if (!view.isShown) return false
        if (view.windowVisibility != View.VISIBLE) return false
        if (view.width <= 0 || view.height <= 0) return false
        visibleRect.setEmpty()
        if (!view.getGlobalVisibleRect(visibleRect)) return false
        val visibleArea = visibleRect.width().toLong() * visibleRect.height().toLong()
        val totalArea = view.width.toLong() * view.height.toLong()
        if (totalArea <= 0L) return false
        return visibleArea >= totalArea * config.visibleThreshold
    }

    /**
     * 去重判定；通过时记录本次上报时间。
     *
     * 会话内的重复由 [TrackedItem.reported] 兜住，这里只负责 key 维度的去重：
     * 1. 业务手动 [markExposed] 标记过的 key 直接抑制；
     * 2. 同一帧批次内相同 key 只上报一次；
     * 3. 上报过的 key 进程内永久抑制，直到 [clearDeduplication] 或 [release] ——
     *    **同一个 key 不会重复曝光，只有新 key 才会触发**。
     */
    private fun passDeduplication(key: String, now: Long, batchAware: Boolean = true): Boolean {
        if (!config.deduplicationEnabled) return true
        if (markedKeys.contains(key)) return false
        if (batchAware && !pendingKeys.add(key)) return false
        if (deduplicationCache.get(key) != null) return false // 进程内永久去重
        deduplicationCache.put(key, now)
        return true
    }

    private fun resetDwell(item: TrackedItem) {
        item.visibleSince = 0L
        item.reported = false
    }

    private fun removeInternal(view: View) {
        items.remove(view)?.data = null
    }

    private fun assertMainThread(api: String) {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "$api 必须在主线程调用（内部依赖 Choreographer / View 体系）"
        }
    }
}

/**
 * 埋点上报抽象
 */
interface EventTracker {

    /**
     * 绑定曝光 View 与数据 —— 在 RecyclerView#onBindViewHolder 中调用。
     *
     * - View 复用安全：同一 View 重新 bind 自动重置旧状态并替换 key/data，视为一次新的曝光会话
     * - 自动触发：bind 后布局完成，可见面积 >= [ExposureConfig.visibleThreshold]
     *   且连续停留 >= [ExposureConfig.minDwellMillis] 时批量回调 [ExposureReporter.onExposure]
     * - 会话去重：一次连续可见期间只上报一次
     * - key 去重：同一个 key 进程内只上报一次，直到 [ExposureManager.clearDeduplication]
     *   或 [ExposureManager.release] 解除
     * - 数据填充 / 刷新：网络成功 -> submitList / notifyDataSetChanged ->
     *   重新走 onBindViewHolder 即自动上报，业务无需手动触发
     * - [view] 为 null 时无可见性可判定：跳过追踪与停留计时，直接回调
     *   [ExposureReporter.onExposure]（仍受 [ExposureRecord.key] 去重约束）
     */
    @AnyThread
    fun <T> trackExposure(
        view: View?,
        data: T,
        parentIndex: Int = 0,
        childIndex: Int = 0,
        keyExtractor: (T) -> String = { d -> d.hashCode().toString() },
    )

    /** 便捷重载：数据实现 [ExposureKeyable] 时免去 key lambda */
    @AnyThread
    fun trackExposure(view: View?, data: ExposureKeyable, parentIndex: Int = 0, childIndex: Int = 0)

    /** 点击上报：每次点击必报，不做去重；[view] 可为 null */
    @AnyThread
    fun <T> trackClick(view: View?, data: T, parentIndex: Int = 0, childIndex: Int = 0)
}

/**
 * 业务数据实现该接口即可省去 [EventTracker.trackExposure] 的 key 提取 lambda
 */
interface ExposureKeyable {
    /** 曝光去重唯一 key */
    val exposureKey: String
}

/** 一条曝光/点击记录 */
class ExposureRecord(
    /** 曝光去重 key；点击不做去重，该字段恒为空字符串 */
    val key: String,
    /** bind 时携带的业务数据 */
    val data: Any?,
    /** 所在容器的索引（如外层列表的 adapter position） */
    val parentIndex: Int = 0,
    /** 当前内容在容器内的索引（如内层列表的 adapter position / banner 页码） */
    val childIndex: Int = 0,
    /** 触发事件的 View（临时引用，业务请勿长期持有）；无 View 的直接上报场景为 null */
    val view: View? = null,
)

/** 业务方实现该接口完成真正上报（网络 / 落库 / 转发埋点 SDK） */
interface ExposureReporter {
    /** 有效曝光上报：同一帧内新满足条件的曝光会合并为一批，便于批量上传 */
    fun onExposure(records: List<ExposureRecord>)

    /** 点击上报：每次点击必报，不做去重 */
    fun onClick(record: ExposureRecord)
}

/**
 * 曝光判定配置
 * 默认：可见面积 >= 1% 且连续停留 >= 100ms 判定为有效曝光，同一个 key 只上报一次
 */
data class ExposureConfig(
    /** 有效曝光要求的最小可见面积占比，取值 (0, 1] */
    val visibleThreshold: Float = 0.01f,
    /** 有效曝光要求的最小连续停留时长（毫秒） */
    val minDwellMillis: Long = 100L,
    /**
     * 曝光检测的节流间隔（毫秒）。帧回调按此粒度执行可见性检测，
     * 应明显小于 [minDwellMillis]（建议 <= 其 1/3），否则停留时长的判定精度会受影响
     */
    val checkIntervalMillis: Long = 32L,
    /**
     * 空闲期（没有任何 item 在累计停留时长）的轮询间隔（毫秒）。
     *
     * 此时不再挂帧回调、不请求 VSYNC，仅用 Handler 低频唤醒等待下一个可见时机。
     * 代价是最多延迟该时长才开始累计停留时长：曝光会**晚报但不会漏报**，
     * 除非某个 item 的可见时长短于该间隔（建议仍明显小于 [minDwellMillis]，或取默认值）
     */
    val idleCheckIntervalMillis: Long = 200L,
    /**
     * 是否开启 key 去重。开启后同一个 key 进程内只上报一次（永久去重），
     * 直到 [ExposureManager.clearDeduplication] 或 [ExposureManager.release] 解除
     */
    val deduplicationEnabled: Boolean = true,
    /** 去重缓存容量（LRU，超出后淘汰最久未上报的 key） */
    val deduplicationCacheSize: Int = 512,
)
