package cn.frank.carback.page

import android.content.Context
import android.os.Bundle
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
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * 基础 Fragment，支持安全的 Lifecycle（修复了 show/hide Fragment 时 onResume/onPause 没有按照预期执行的 Bug）
 *
 * @author shangmingchao
 */
abstract class BaseFragment<VB : ViewBinding> : Fragment(),
    FragmentBinding<VB> by FragmentBindingDelegate() {

    private val _visibilityFlow = MutableStateFlow<Boolean?>(null)
    private var isLazyLoaded = false

    protected lateinit var baseContext: Context

    /**
     * 真实的生命周期 LifecycleOwner（修复了 show/hide Fragment 时 onResume/onPause 没有按照预期执行的 Bug）。
     * 尽量不要直接使用 lifecycle 或者 viewLifecycleOwner.lifecycle
     *
     */
    val safeLifecycleOwner by lazy {
        SafeLifecycleOwner(
            hostLifecycle = this.viewLifecycleOwner.lifecycle,
            visibilityFlow = _visibilityFlow.filterNotNull()
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        baseContext = context
    }

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

    /**
     * 页面可见时加载，使用于 ViewPager 数据懒加载，只加载一次
     */
    protected fun lazyLoad(block: SafeLifecycleOwner.() -> Unit) {
        safeLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                if (!isLazyLoaded) {
                    isLazyLoaded = true
                    block(safeLifecycleOwner)
                }
            }

            override fun onDestroy(owner: LifecycleOwner) {
                isLazyLoaded = false
            }
        })
    }

    /**
     * 页面可见时启动协程，使用于 ViewPager 数据懒加载，只启动一次
     */
    protected inline fun lazyLaunch(crossinline block: suspend () -> Unit) {
        lazyLoad {
            lifecycleScope.launch {
                block()
            }
        }
    }

    protected fun startCollect(vararg collectors: suspend CoroutineScope.() -> Unit) {
        safeLifecycleOwner.lifecycleScope.launch {
            collectors.forEach { collector ->
                launch { collector() }
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
