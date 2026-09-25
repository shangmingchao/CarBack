package cn.frank.carback.page

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

/**
 * 基础 Fragment
 *
 * - 修复了页面布局 ViewBinding 需要手动创建和销毁的问题
 * - 修复了 show/hide Fragment 时 onResume/onPause 没有按照预期执行的 Bug
 * - 修复了 Fragment 在 ViewPager 等场景下无法懒加载的问题
 *
 * 注意：使用 BaseFragment 后尽量不要重载其生命周期方法（其方法回调不再可靠），
 * 而是使用 safeLifecycleOwner.lifecycle.addObserver()进行生命周期监听
 *
 * @author shangmingchao
 */
abstract class BaseFragment<VB : ViewBinding> : Fragment(),
    FragmentBinding<VB> by FragmentBindingDelegate() {

    private val _visibilityFlow = MutableStateFlow<Boolean?>(null)

    /**
     * 收集 Flow 时的统一异常兜底
     */
    private val collectorExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(
            "BaseFragment",
            "collector error：${throwable.message}"
        )
    }

    private var _safeLifecycleOwner: SafeLifecycleOwner? = null

    /**
     * 真实的生命周期 LifecycleOwner（修复了 show/hide Fragment 时 onResume/onPause 没有按照预期执行的 Bug）。
     * 尽量不要直接使用 lifecycle 或者 viewLifecycleOwner.lifecycle
     */
    val safeLifecycleOwner: SafeLifecycleOwner
        get() = _safeLifecycleOwner ?: SafeLifecycleOwner(
            hostLifecycle = this.viewLifecycleOwner.lifecycle,
            visibilityFlow = _visibilityFlow.filterNotNull()
        ).also { _safeLifecycleOwner = it }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return createViewWithBinding(inflater, container)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            updateVisibility(isVisible = !hidden)
        }
    }

    override fun onResume() {
        super.onResume()
        updateVisibility(isVisible = !isHidden)
    }

    override fun onPause() {
        super.onPause()
        updateVisibility(isVisible = false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _safeLifecycleOwner = null
        _visibilityFlow.value = null
    }

    /**
     * 页面处于 [Lifecycle.State.RESUMED]（真正可见且可交互）时开始收集 Flow。
     *
     * 行为说明：
     * - 生命周期降到 RESUMED 以下（ON_PAUSE）时自动暂停收集；
     * - 再次回到 RESUMED 时自动重新收集；
     * - 生命周期进入 DESTROYED 后永久停止收集，不会造成协程泄漏。
     *
     * 支持同时并发收集多个 Flow：
     * ```
     * startCollect(
     *     { flow1.collect { ... } },
     *     { flow2.collect { ... } },
     * )
     * ```
     *
     * 每个 collector 之间相互隔离：若其中某个抛出异常，仅该 collector 自身结束，
     * 不会取消同批次其它 collector，也不会影响后续生命周期变化时的重新收集。
     * 未捕获的异常会被统一的 [collectorExceptionHandler] 捕获并记录日志，不会导致崩溃；
     * 若业务需要对异常做特定处理，仍建议在 collector 内部自行 try/catch。
     *
     * @param collectors 需要收集的挂起任务，每个都会被独立启动。
     * @see startRepeatCollect
     */
    protected fun startCollect(vararg collectors: suspend CoroutineScope.() -> Unit) {
        startRepeatCollect(Lifecycle.State.RESUMED, *collectors)
    }

    @Suppress("SameParameterValue")
    private fun startRepeatCollect(
        state: Lifecycle.State,
        vararg collectors: suspend CoroutineScope.() -> Unit,
    ) {
        safeLifecycleOwner.lifecycleScope.launch {
            safeLifecycleOwner.repeatOnLifecycle(state) {
                supervisorScope {
                    collectors.forEach { collector ->
                        launch(collectorExceptionHandler) { collector() }
                    }
                }
            }
        }
    }

    private fun updateVisibility(isVisible: Boolean) {
        if (_visibilityFlow.value != isVisible) {
            _visibilityFlow.value = isVisible
        }
    }

    class SafeLifecycleOwner(
        private val hostLifecycle: Lifecycle,
        private val visibilityFlow: Flow<Boolean>
    ) : LifecycleOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)

        override val lifecycle: Lifecycle = lifecycleRegistry

        private var collectionJob: Job? = null

        init {
            hostLifecycle.addObserver(object : DefaultLifecycleObserver {

                override fun onCreate(owner: LifecycleOwner) {
                    lifecycleRegistry.currentState = Lifecycle.State.CREATED
                }

                override fun onDestroy(owner: LifecycleOwner) {
                    collectionJob?.cancel()
                    lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
                    hostLifecycle.removeObserver(this)
                }
            })

            // 开始监听可见性变化
            collectionJob = hostLifecycle.coroutineScope.launch {
                visibilityFlow.collect { visible ->
                    if (visible) {
                        // 从当前状态逐步升至 RESUMED (会触发 onStart -> onResume)
                        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
                    } else {
                        // 降至 CREATED (会触发 onPause -> onStop)
                        lifecycleRegistry.currentState = Lifecycle.State.CREATED
                    }
                }
            }
        }
    }
}
