package cn.frank.carback.page

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.AbsSavedState
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.core.content.res.use
import androidx.core.view.NestedScrollingChild3
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.NestedScrollingParent3
import androidx.core.view.NestedScrollingParentHelper
import androidx.core.view.ViewCompat
import cn.frank.carback.R
import cn.frank.carback.databinding.CommonPageEmptyBinding
import cn.frank.carback.databinding.CommonPageErrorNetworkBinding

/**
 * 页面状态视图，用于在不同状态间切换：Loading、Empty、NetworkError、UnknownError。
 *
 * 设计要点：
 * - **按需创建**：状态视图在首次展示时才 inflate 并缓存，避免一次性创建所有状态布局；
 *   `NETWORK_ERROR` 与 `UNKNOWN_ERROR` 共用同一个视图实例，仅文案不同。
 * - **配置可延后**：图标/文案等配置在视图创建前设置也会缓存下来，创建后自动应用。
 * - **状态可恢复**：横竖屏等配置变更后自动恢复当前状态与显示/隐藏。
 * - **嵌套滚动转发**：状态页自身会消费触摸事件却不产生滚动，因此同时扮演两种角色：
 *   - [NestedScrollingChild3]：没有子 View 接力时，把手指竖向位移直接派发给父级；
 *   - [NestedScrollingParent3]：状态视图内部有可滚动 View（自定义 RecyclerView 等）时，
 *     接收它派发上来的滚动并继续向上转发。
 *   两者保证外层（AppBarLayout 折叠、下拉刷新）在滑动状态页时依然能跟随响应，
 *   且同一手势只会有一条转发链路，位移不会被重复计算。
 *
 * 可以通过 XML 属性自定义：
 * - `app:loadingLayout`：Loading 状态的自定布局资源 ID。
 * - `app:emptyLayout`：Empty 状态的自定布局资源 ID。
 * - `app:emptyIcon`：Empty 状态的图标资源 ID（0 表示隐藏图标）。
 * - `app:emptyText`：Empty 状态的文案。
 * - `app:errorLayout`：错误状态的自定布局资源 ID。
 * - `app:errorIcon`：错误状态的图标资源 ID。
 * - `app:errorText`：网络错误状态的文案。
 * - `app:unknownErrorText`：未知错误状态的文案，未设置时复用 `app:errorText`。
 * - `app:retryButtonText`：重试按钮文字。
 * - `app:retryButtonVisible`：是否展示重试按钮。
 *
 * 也可以通过代码动态设置：
 * - `setEmptyIconText(iconRes, text)`
 * - `setErrorIconText(errorIconRes, errorText, retryButtonText)`
 * - `setUnknownErrorText(text)`
 * - `setLoadingView(view)` / `setEmptyView(view)` / `setErrorView(view)`
 * - `setLoadingLayoutRes(resId)` / `setEmptyLayoutRes(resId)` / `setErrorLayoutRes(resId)`
 * - `setOnRetryClickListener(listener)`
 *
 * @author shangmingchao
 */
class PageStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), NestedScrollingChild3, NestedScrollingParent3 {

    /**
     * 页面状态枚举
     */
    enum class State {
        LOADING, EMPTY, NETWORK_ERROR, UNKNOWN_ERROR
    }

    /**
     * 单个状态对应的视图及其 ViewBinding 缓存。
     * `NETWORK_ERROR` 与 `UNKNOWN_ERROR` 共用同一个实例，切换时按状态应用不同文案。
     *
     * @param view 状态视图
     * @param custom true 表示由外部通过 `setXxxView()` 传入，不再套用默认配置
     */
    private class StateViewHolder(
        val view: View,
        var custom: Boolean = false,
    ) {
        var emptyBinding: CommonPageEmptyBinding? = null
        var errorBinding: CommonPageErrorNetworkBinding? = null
    }

    private val inflater = LayoutInflater.from(context)

    /**
     * 已创建的状态视图，按状态按需创建并缓存
     */
    private val stateHolders = LinkedHashMap<State, StateViewHolder>()

    /**
     * 当前状态
     */
    var currentState: State = State.LOADING
        private set

    private var retryClickListener: OnClickListener? = null

    @LayoutRes
    private var loadingLayoutRes = 0

    @LayoutRes
    private var emptyLayoutRes = 0

    @LayoutRes
    private var errorLayoutRes = 0

    /** null 表示未设置（保留布局默认图标）；0 表示隐藏图标 */
    @DrawableRes
    private var emptyIconRes: Int? = null

    private var emptyText: CharSequence? = null

    @DrawableRes
    private var errorIconRes: Int? = null

    private var errorText: CharSequence? = null

    private var unknownErrorText: CharSequence? = null

    private var retryButtonText: CharSequence? = null

    private var retryButtonVisible = true

    /**
     * 作为嵌套滚动子 View：把（自己或内层 View 的）滚动继续派发给父级
     */
    private val nestedScrollingChildHelper = NestedScrollingChildHelper(this)

    /**
     * 作为嵌套滚动父 View：记录子 View 发起的嵌套滚动轴向
     */
    private val nestedScrollingParentHelper = NestedScrollingParentHelper(this)

    /**
     * 本轮手势是否由自身负责向上转发。
     *
     * 若状态视图内部有 View 发起嵌套滚动（它会先把滚动派发给我们，再由我们向上转发），
     * 则置为 false，自身的触摸转发让位，避免同一段位移被重复派发导致外层滚动两倍距离。
     */
    private var forwardingByTouch = true

    /**
     * 上一次触摸的 Y 坐标（已按父级滚动导致的自身位移修正）
     */
    private var lastTouchY = 0

    /**
     * 父级在 pre-scroll 阶段消费的滚动量
     */
    private val scrollConsumed = IntArray(2)

    /**
     * 父级在 scroll 阶段消费的滚动量
     */
    private val scrollConsumedByParent = IntArray(2)

    /**
     * 嵌套滚动期间本视图在窗口中的位移量（由父级下发）
     */
    private val scrollOffset = IntArray(2)

    /**
     * 累计的窗口位移。
     *
     * AppBarLayout 折叠/展开时是由父级移动本视图实现的，此时同一根手指在本视图坐标系中的
     * Y 值会随之变化；若不扣除这段位移，就会被当成新的滚动距离重复上报，表现为折叠时抖动。
     */
    private var nestedYOffset = 0

    init {
        parseAttributes(attrs, defStyleAttr)
        // 状态页展示时拦截触摸事件，避免穿透到下层的内容/列表
        isClickable = true
        // 状态页自身不滚动，但要把竖向滑动转发给外层（折叠头部 / 下拉刷新）
        isNestedScrollingEnabled = true
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            // 新一轮手势：默认由自身转发，若接下来有子 View 发起嵌套滚动再让位
            forwardingByTouch = true
        }
        // 先分发给子 View：子 View 若发起嵌套滚动，会置 forwardingByTouch = false
        val handled = super.dispatchTouchEvent(event)
        if (forwardingByTouch) {
            dispatchNestedScrollByTouch(event)
        }
        return handled
    }

    /**
     * 把手指的竖向位移以嵌套滚动的方式派发给父级。
     *
     * 约定与 RecyclerView 一致：`dy > 0` 表示手指向上滑（内容下移，头部折叠），
     * `dy < 0` 表示手指向下滑（内容到顶后头部展开，再继续则触发外层下拉刷新）。
     */
    private fun dispatchNestedScrollByTouch(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                nestedYOffset = 0
                lastTouchY = motionY(event)
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH)
            }

            MotionEvent.ACTION_MOVE -> {
                val y = motionY(event)
                var dy = lastTouchY - y
                if (dy != 0) {
                    scrollConsumed[0] = 0
                    scrollConsumed[1] = 0
                    scrollOffset[0] = 0
                    scrollOffset[1] = 0
                    if (dispatchNestedPreScroll(
                            0, dy, scrollConsumed, scrollOffset, ViewCompat.TYPE_TOUCH
                        )
                    ) {
                        dy -= scrollConsumed[1]
                        nestedYOffset += scrollOffset[1]
                    }
                    if (dy != 0) {
                        scrollConsumedByParent[0] = 0
                        scrollConsumedByParent[1] = 0
                        scrollOffset[0] = 0
                        scrollOffset[1] = 0
                        dispatchNestedScroll(
                            0,
                            0,
                            0,
                            dy,
                            scrollOffset,
                            ViewCompat.TYPE_TOUCH,
                            scrollConsumedByParent
                        )
                        nestedYOffset += scrollOffset[1]
                    }
                }
                // 父级消费滚动时会移动本视图，需从手指坐标中扣除该位移，否则会重复计算抖动
                lastTouchY = y - nestedYOffset
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                nestedYOffset = 0
                stopNestedScroll(ViewCompat.TYPE_TOUCH)
            }
        }
    }

    /**
     * 触摸点的 Y 坐标（四舍五入取整，避免截断带来的 ±1px 抖动）
     */
    private fun motionY(event: MotionEvent): Int = (event.y + 0.5f).toInt()

    /**
     * 显示 Loading 状态
     */
    fun showLoading() {
        show(State.LOADING)
    }

    /**
     * 显示 Empty 状态
     *
     * @param iconRes 临时指定的图标，null 表示不修改（0 表示隐藏图标）
     * @param text 临时指定的文案，null 表示不修改
     */
    fun showEmpty(
        @DrawableRes iconRes: Int? = null,
        text: CharSequence? = null,
    ) {
        if (iconRes != null || text != null) {
            setEmptyIconText(iconRes, text)
        }
        show(State.EMPTY)
    }

    /**
     * 显示 NetworkError 状态
     *
     * @param text 临时指定的错误文案，null 表示不修改
     */
    fun showNetworkError(text: CharSequence? = null) {
        text?.let { setErrorIconText(errorText = it) }
        show(State.NETWORK_ERROR)
    }

    /**
     * 显示 UnknownError 状态
     *
     * @param text 临时指定的错误文案，null 表示不修改
     */
    fun showUnknownError(text: CharSequence? = null) {
        text?.let { setUnknownErrorText(it) }
        show(State.UNKNOWN_ERROR)
    }

    /**
     * 切换到指定状态并展示（沿用已有配置）
     */
    fun show(state: State) {
        switchState(state)
        showOrHide(true)
    }

    /**
     * 展示、隐藏
     */
    fun showOrHide(show: Boolean) {
        if (show) {
            // 保证当前状态视图已创建（例如外部直接调用 showOrHide(true)）
            switchState(currentState)
        }
        visibility = if (show) VISIBLE else GONE
    }

    /**
     * 设置 Empty 图片/文案（Empty 视图尚未创建时先缓存，创建后自动应用）
     *
     * @param iconRes 图标资源，null 表示不修改，0 表示隐藏图标
     * @param text 文案，null 表示不修改
     */
    fun setEmptyIconText(
        @DrawableRes iconRes: Int? = null,
        text: CharSequence? = null,
    ) {
        if (iconRes != null) {
            emptyIconRes = iconRes
        }
        if (text != null) {
            emptyText = text
        }
        stateHolders[State.EMPTY]?.let(::applyEmptyConfig)
    }

    /**
     * 设置 Error 图片/文案（错误视图尚未创建时先缓存，创建后自动应用）
     *
     * @param errorIconRes 错误图标资源，null 表示不修改
     * @param errorText 网络错误文案，null 表示不修改
     * @param retryButtonText 重试按钮文案，null 表示不修改
     */
    fun setErrorIconText(
        @DrawableRes errorIconRes: Int? = null,
        errorText: CharSequence? = null,
        retryButtonText: CharSequence? = null,
    ) {
        if (errorIconRes != null) {
            this.errorIconRes = errorIconRes
        }
        if (errorText != null) {
            this.errorText = errorText
        }
        if (retryButtonText != null) {
            this.retryButtonText = retryButtonText
        }
        applyErrorConfigToErrorViews()
    }

    /**
     * 设置未知错误（[State.UNKNOWN_ERROR]）文案，未设置时复用 [setErrorIconText] 的文案
     */
    fun setUnknownErrorText(text: CharSequence?) {
        unknownErrorText = text
        applyErrorConfigToErrorViews()
    }

    /**
     * 设置重试按钮是否展示
     */
    fun setRetryButtonVisible(visible: Boolean) {
        retryButtonVisible = visible
        applyErrorConfigToErrorViews()
    }

    /**
     * 设置自定义 Loading 视图（覆盖默认/XML 设置）
     */
    fun setLoadingView(view: View) {
        replaceStateView(State.LOADING, view)
    }

    /**
     * 设置自定义 Empty 视图
     */
    fun setEmptyView(view: View) {
        replaceStateView(State.EMPTY, view)
    }

    /**
     * 设置自定义错误视图（网络错误与未知错误共用）
     */
    fun setErrorView(view: View) {
        removeStateView(State.NETWORK_ERROR)
        removeStateView(State.UNKNOWN_ERROR)
        val holder = StateViewHolder(view, custom = true)
        addStateView(holder)
        stateHolders[State.NETWORK_ERROR] = holder
        stateHolders[State.UNKNOWN_ERROR] = holder
        switchState(currentState)
    }

    /**
     * 动态替换 Loading 布局资源（已创建的视图会被重建）
     */
    fun setLoadingLayoutRes(@LayoutRes resId: Int) {
        if (loadingLayoutRes == resId) return
        loadingLayoutRes = resId
        recreateStateView(State.LOADING)
    }

    /**
     * 动态替换 Empty 布局资源（已创建的视图会被重建）
     */
    fun setEmptyLayoutRes(@LayoutRes resId: Int) {
        if (emptyLayoutRes == resId) return
        emptyLayoutRes = resId
        recreateStateView(State.EMPTY)
    }

    /**
     * 动态替换错误布局资源（已创建的视图会被重建）
     */
    fun setErrorLayoutRes(@LayoutRes resId: Int) {
        if (errorLayoutRes == resId) return
        errorLayoutRes = resId
        recreateStateView(State.NETWORK_ERROR)
        recreateStateView(State.UNKNOWN_ERROR)
    }

    /**
     * 设置重试按钮点击监听器
     */
    fun setOnRetryClickListener(listener: OnClickListener?) {
        retryClickListener = listener
    }

    private fun parseAttributes(attrs: AttributeSet?, defStyleAttr: Int) {
        attrs ?: return
        context.obtainStyledAttributes(attrs, R.styleable.PageStateView, defStyleAttr, 0)
            .use { typedArray ->
                loadingLayoutRes =
                    typedArray.getResourceId(R.styleable.PageStateView_loadingLayout, 0)
                emptyLayoutRes = typedArray.getResourceId(R.styleable.PageStateView_emptyLayout, 0)
                errorLayoutRes = typedArray.getResourceId(R.styleable.PageStateView_errorLayout, 0)
                if (typedArray.hasValue(R.styleable.PageStateView_emptyIcon)) {
                    emptyIconRes = typedArray.getResourceId(R.styleable.PageStateView_emptyIcon, 0)
                }
                emptyText = typedArray.getText(R.styleable.PageStateView_emptyText)
                if (typedArray.hasValue(R.styleable.PageStateView_errorIcon)) {
                    errorIconRes = typedArray.getResourceId(R.styleable.PageStateView_errorIcon, 0)
                }
                errorText = typedArray.getText(R.styleable.PageStateView_errorText)
                unknownErrorText = typedArray.getText(R.styleable.PageStateView_unknownErrorText)
                retryButtonText = typedArray.getText(R.styleable.PageStateView_retryButtonText)
                retryButtonVisible =
                    typedArray.getBoolean(R.styleable.PageStateView_retryButtonVisible, true)
            }
    }

    /**
     * 切换状态：只确保目标状态的视图已创建，并仅显示该状态视图
     */
    private fun switchState(state: State) {
        currentState = state
        val target = stateView(state)
        // 错误状态共用视图，切换时按状态刷新文案
        if (state == State.NETWORK_ERROR || state == State.UNKNOWN_ERROR) {
            stateHolders[state]?.let { applyErrorConfig(it, state) }
        }
        stateHolders.values.distinct().forEach { holder ->
            holder.view.visibility = if (holder.view === target) VISIBLE else GONE
        }
    }

    private fun stateView(state: State): View = stateHolders[state]?.view ?: createStateView(state)

    private fun createStateView(state: State): View {
        val holder = when (state) {
            State.LOADING -> StateViewHolder(
                inflateStateView(loadingLayoutRes, R.layout.common_page_loading)
            )

            State.EMPTY -> StateViewHolder(
                inflateStateView(emptyLayoutRes, R.layout.common_page_empty)
            ).also(::applyEmptyConfig)

            State.NETWORK_ERROR, State.UNKNOWN_ERROR -> errorHolder().also {
                applyErrorConfig(it, state)
            }
        }
        stateHolders[state] = holder
        addStateView(holder)
        return holder.view
    }

    /**
     * 错误状态共用同一个视图实例，避免重复 inflate
     */
    private fun errorHolder(): StateViewHolder {
        stateHolders[State.NETWORK_ERROR]?.let { return it }
        stateHolders[State.UNKNOWN_ERROR]?.let { return it }
        return StateViewHolder(inflateStateView(errorLayoutRes, R.layout.common_page_error_network))
    }

    private fun replaceStateView(state: State, view: View) {
        removeStateView(state)
        val holder = StateViewHolder(view, custom = true)
        stateHolders[state] = holder
        addStateView(holder)
        switchState(currentState)
    }

    /**
     * 移除指定状态的视图：仅当没有其他状态仍引用该视图时才从容器中移除
     */
    private fun removeStateView(state: State) {
        val holder = stateHolders.remove(state) ?: return
        if (stateHolders.values.none { it.view === holder.view }) {
            removeView(holder.view)
        }
    }

    private fun addStateView(holder: StateViewHolder) {
        if (holder.view.parent !== this) {
            addView(holder.view)
        }
    }

    /**
     * 布局资源变更时重建对应状态视图（未创建的保持懒加载）
     */
    private fun recreateStateView(state: State) {
        if (stateHolders[state] == null) return
        removeStateView(state)
        switchState(currentState)
    }

    private fun inflateStateView(@LayoutRes customRes: Int, @LayoutRes defaultRes: Int): View =
        inflater.inflate(if (customRes != 0) customRes else defaultRes, this, false)

    private fun applyEmptyConfig(holder: StateViewHolder) {
        val binding = emptyBinding(holder) ?: return
        when (val icon = emptyIconRes) {
            null -> Unit // 未设置，保留布局默认图标
            0 -> binding.imgEmpty.visibility = GONE
            else -> {
                binding.imgEmpty.setImageResource(icon)
                binding.imgEmpty.visibility = VISIBLE
            }
        }
        emptyText?.let { binding.textEmpty.text = it }
    }

    private fun applyErrorConfigToErrorViews() {
        stateHolders[State.NETWORK_ERROR]?.let { applyErrorConfig(it, State.NETWORK_ERROR) }
        stateHolders[State.UNKNOWN_ERROR]?.let { applyErrorConfig(it, State.UNKNOWN_ERROR) }
    }

    private fun applyErrorConfig(holder: StateViewHolder, state: State) {
        val binding = errorBinding(holder) ?: return
        errorIconRes?.takeIf { it != 0 }?.let { binding.imgError.setImageResource(it) }
        val text = if (state == State.UNKNOWN_ERROR) {
            unknownErrorText ?: errorText
        } else {
            errorText
        }
        text?.let { binding.textError.text = it }
        retryButtonText?.let { binding.textRetry.text = it }
        binding.textRetry.visibility = if (retryButtonVisible) VISIBLE else GONE
        binding.textRetry.setOnClickListener { retryClickListener?.onClick(it) }
    }

    /**
     * 获取（并缓存）Empty 视图的 Binding；自定义视图或缺少对应 id 时返回 null
     */
    private fun emptyBinding(holder: StateViewHolder): CommonPageEmptyBinding? {
        if (holder.custom) return null
        holder.emptyBinding?.let { return it }
        return runCatching { CommonPageEmptyBinding.bind(holder.view) }
            .onSuccess { holder.emptyBinding = it }
            .getOrNull()
    }

    /**
     * 获取（并缓存）错误视图的 Binding；自定义视图或缺少对应 id 时返回 null
     */
    private fun errorBinding(holder: StateViewHolder): CommonPageErrorNetworkBinding? {
        if (holder.custom) return null
        holder.errorBinding?.let { return it }
        return runCatching { CommonPageErrorNetworkBinding.bind(holder.view) }
            .onSuccess { holder.errorBinding = it }
            .getOrNull()
    }

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        nestedScrollingChildHelper.isNestedScrollingEnabled = enabled
    }

    override fun isNestedScrollingEnabled(): Boolean = nestedScrollingChildHelper.isNestedScrollingEnabled

    override fun startNestedScroll(axes: Int): Boolean =
        nestedScrollingChildHelper.startNestedScroll(axes)

    override fun startNestedScroll(axes: Int, type: Int): Boolean =
        nestedScrollingChildHelper.startNestedScroll(axes, type)

    override fun stopNestedScroll() {
        nestedScrollingChildHelper.stopNestedScroll()
    }

    override fun stopNestedScroll(type: Int) {
        nestedScrollingChildHelper.stopNestedScroll(type)
    }

    override fun hasNestedScrollingParent(): Boolean =
        nestedScrollingChildHelper.hasNestedScrollingParent()

    override fun hasNestedScrollingParent(type: Int): Boolean =
        nestedScrollingChildHelper.hasNestedScrollingParent(type)

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?
    ): Boolean = nestedScrollingChildHelper.dispatchNestedScroll(
        dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow
    )

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean = nestedScrollingChildHelper.dispatchNestedScroll(
        dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type
    )

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int,
        consumed: IntArray
    ) {
        nestedScrollingChildHelper.dispatchNestedScroll(
            dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type, consumed
        )
    }

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?
    ): Boolean = nestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean =
        nestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)

    override fun dispatchNestedFling(velocityX: Float, velocityY: Float, consumed: Boolean): Boolean =
        nestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed)

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean =
        nestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY)

    // ---------------- NestedScrollingParent3 ----------------
    // 状态视图内部的可滚动 View 会把嵌套滚动派发到这里，这里再向上转发出去，
    // 保证外层（AppBarLayout 折叠 / 下拉刷新）也能收到。

    override fun onStartNestedScroll(child: View, target: View, axes: Int): Boolean =
        onStartNestedScroll(child, target, axes, ViewCompat.TYPE_TOUCH)

    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean =
        (axes and ViewCompat.SCROLL_AXIS_VERTICAL) != 0

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int) =
        onNestedScrollAccepted(child, target, axes, ViewCompat.TYPE_TOUCH)

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) {
        nestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes, type)
        startNestedScroll(axes and ViewCompat.SCROLL_AXIS_VERTICAL, type)
        if (type == ViewCompat.TYPE_TOUCH) {
            forwardingByTouch = false
        }
    }

    override fun onStopNestedScroll(target: View) = onStopNestedScroll(target, ViewCompat.TYPE_TOUCH)

    override fun onStopNestedScroll(target: View, type: Int) {
        nestedScrollingParentHelper.onStopNestedScroll(target, type)
        stopNestedScroll(type)
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) =
        onNestedPreScroll(target, dx, dy, consumed, ViewCompat.TYPE_TOUCH)

    override fun onNestedPreScroll(
        target: View,
        dx: Int,
        dy: Int,
        consumed: IntArray,
        type: Int
    ) {
        dispatchNestedPreScroll(dx, dy, consumed, null, type)
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int
    ) = onNestedScroll(
        target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, ViewCompat.TYPE_TOUCH
    )

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int
    ) {
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, null, type)
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        dispatchNestedScroll(
            dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, null, type, consumed
        )
    }

    override fun onNestedFling(
        target: View,
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean = dispatchNestedFling(velocityX, velocityY, consumed)

    override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean =
        dispatchNestedPreFling(velocityX, velocityY)

    override fun getNestedScrollAxes(): Int = nestedScrollingParentHelper.nestedScrollAxes

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        return SavedState(superState, currentState, visibility == VISIBLE)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        switchState(state.pageState)
        visibility = if (state.visible) VISIBLE else GONE
    }

    /**
     * 保存当前状态与显示/隐藏，便于配置变更后恢复
     */
    private class SavedState : AbsSavedState {

        val pageState: State
        val visible: Boolean

        constructor(superState: Parcelable?, pageState: State, visible: Boolean) : super(superState) {
            this.pageState = pageState
            this.visible = visible
        }

        constructor(source: Parcel) : super(source) {
            pageState = State.values()[source.readInt()]
            visible = source.readInt() == 1
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeInt(pageState.ordinal)
            dest.writeInt(if (visible) 1 else 0)
        }

        companion object {
            @JvmField
            val CREATOR = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel): SavedState = SavedState(source)

                override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
            }
        }
    }
}
