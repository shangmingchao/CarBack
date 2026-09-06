package cn.frank.carback.page

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator.INFINITE
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import cn.frank.carback.R
import cn.frank.carback.databinding.CommonPageEmptyBinding
import cn.frank.carback.databinding.CommonPageErrorNetworkBinding
import cn.frank.carback.databinding.CommonPageLoadingBinding

/**
 * 页面状态视图，用于在不同状态间切换：Loading、Empty、NetworkError、UnknownError。
 *
 * 可以通过 XML 属性自定义：
 * - `app:loadingLayout`：Loading 状态的自定义布局资源 ID。
 * - `app:emptyIcon`：Empty 状态的图标资源 ID。
 * - `app:emptyText`：Empty 状态的文案。
 * - `app:errorIcon`：错误状态的图标资源 ID。
 * - `app:errorText`：错误状态的文案。
 * - `app:retryButtonText`：重试按钮文字。
 *
 * 也可以通过代码动态设置：
 * - `setLoadingView(view)`
 * - `setEmptyView(view)`
 * - `setOnRetryClickListener(listener)`
 *
 * @author shangmingchao
 */
class PageStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    /**
     * 页面状态枚举
     */
    enum class State {
        LOADING, EMPTY, NETWORK_ERROR, UNKNOWN_ERROR
    }

    /**
     * 当前状态
     */
    private var currentState: State = State.LOADING

    private var loadingView: View? = null
    private var emptyView: View? = null
    private var networkErrorView: View? = null
    private var unknownErrorView: View? = null
    private var retryClickListener: OnClickListener? = null
    private var loadingAnimator: Animator? = null

    init {
        if (attrs != null) {
            val typedArray =
                context.obtainStyledAttributes(attrs, R.styleable.PageStateView, defStyleAttr, 0)
            try {
                // 获取自定义属性
                val loadingLayoutRes =
                    typedArray.getResourceId(R.styleable.PageStateView_loadingLayout, 0)
                val emptyIconRes = typedArray.getResourceId(R.styleable.PageStateView_emptyIcon, 0)
                val emptyText = typedArray.getString(R.styleable.PageStateView_emptyText)
                val errorIconRes = typedArray.getResourceId(R.styleable.PageStateView_errorIcon, 0)
                val errorText = typedArray.getString(R.styleable.PageStateView_errorText)
                val retryButtonText =
                    typedArray.getString(R.styleable.PageStateView_retryButtonText)
                // 创建各状态视图
                createLoadingView(loadingLayoutRes)
                createEmptyView(
                    if (typedArray.hasValue(R.styleable.PageStateView_emptyIcon)) emptyIconRes else null,
                    if (typedArray.hasValue(R.styleable.PageStateView_emptyText)) emptyText else null,
                )
                createNetworkErrorView(
                    if (typedArray.hasValue(R.styleable.PageStateView_errorIcon)) errorIconRes else null,
                    if (typedArray.hasValue(R.styleable.PageStateView_errorText)) errorText else null,
                    if (typedArray.hasValue(R.styleable.PageStateView_retryButtonText)) retryButtonText else null,
                )
                createUnknownErrorView(
                    if (typedArray.hasValue(R.styleable.PageStateView_errorIcon)) errorIconRes else null,
                    if (typedArray.hasValue(R.styleable.PageStateView_errorText)) errorText else null,
                    if (typedArray.hasValue(R.styleable.PageStateView_retryButtonText)) retryButtonText else null,
                )
            } finally {
                typedArray.recycle()
            }
        } else {
            // 无属性时创建默认视图
            createLoadingView(0)
            createEmptyView()
            createNetworkErrorView()
            createUnknownErrorView()
        }
        showState(currentState)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        loadingAnimator?.cancel()
    }

    /**
     * 显示 Loading 状态
     */
    fun showLoading() {
        showState(State.LOADING)
        showOrHide(true)
    }

    /**
     * 显示 Empty 状态
     */
    fun showEmpty() {
        showState(State.EMPTY)
        showOrHide(true)
    }

    /**
     * 显示 NetworkError 状态
     */
    fun showNetworkError() {
        showState(State.NETWORK_ERROR)
        showOrHide(true)
    }

    /**
     * 显示 UnknownError 状态
     */
    fun showUnknownError() {
        showState(State.UNKNOWN_ERROR)
        showOrHide(true)
    }

    /**
     * 展示、隐藏
     */
    fun showOrHide(show: Boolean) {
        visibility = if (show) VISIBLE else GONE
        if (show && currentState == State.LOADING) {
            loadingAnimator?.start()
        }
        if (!show) {
            loadingAnimator?.cancel()
        }
    }

    /**
     * 设置 Empty 图片/文案
     */
    fun setEmptyIconText(iconRes: Int? = null, text: String? = null) {
        emptyView?.let {
            val binding = CommonPageEmptyBinding.bind(it)
            iconRes?.let { ic -> binding.imgEmpty.setImageResource(ic) }
            text?.let { t -> binding.textEmpty.text = t }
        }
    }

    /**
     * 设置自定义 Loading 视图（覆盖默认/XML 设置）
     */
    fun setLoadingView(view: View) {
        loadingView?.let { removeView(it) }
        loadingView = view
        addView(view)
        showState(currentState)
    }

    /**
     * 设置自定义 Empty 视图
     */
    fun setEmptyView(view: View) {
        emptyView?.let { removeView(it) }
        emptyView = view
        addView(view)
        showState(currentState)
    }

    /**
     * 设置重试按钮点击监听器
     */
    fun setOnRetryClickListener(listener: OnClickListener?) {
        retryClickListener = listener
        networkErrorView?.let { v ->
            val binding = CommonPageErrorNetworkBinding.bind(v)
            binding.textRetry.setOnClickListener {
                retryClickListener?.onClick(it)
            }
        }
    }

    /**
     * 创建 Loading 视图
     *
     * @param customLayoutRes 自定义布局资源 ID，0 表示使用默认 ProgressBar
     */
    private fun createLoadingView(customLayoutRes: Int) {
        loadingView = LayoutInflater.from(context).inflate(
            if (customLayoutRes != 0) customLayoutRes else R.layout.common_page_loading,
            this,
            false,
        ).apply {
            if (customLayoutRes != 0) {
                return@apply
            }
            val binding = CommonPageLoadingBinding.bind(this)
            loadingAnimator =
                ObjectAnimator.ofFloat(binding.imgLoading, "rotation", 0f, 360f).apply {
                    repeatCount = INFINITE
                    duration = 1000L
                    interpolator = LinearInterpolator()
                    start()
                }
        }
        loadingView?.let { addView(it) }
    }

    /**
     * 创建 Empty 视图
     */
    private fun createEmptyView(iconRes: Int? = null, text: String? = null) {
        emptyView = LayoutInflater.from(context).inflate(R.layout.common_page_empty, this, false)
        setEmptyIconText(iconRes = iconRes, text = text)
        emptyView?.let { addView(it) }
    }

    /**
     * 创建 NetworkError 视图（包含重试按钮）
     */
    private fun createNetworkErrorView(
        iconRes: Int? = null,
        text: String? = null,
        retryText: String? = null,
    ) {
        networkErrorView =
            LayoutInflater.from(context).inflate(R.layout.common_page_error_network, this, false)
                .apply {
                    val binding = CommonPageErrorNetworkBinding.bind(this)
                    iconRes?.let { binding.imgError.setImageResource(it) }
                    text?.let { binding.textError.text = it }
                    retryText?.let { binding.textRetry.text = it }
                    binding.textRetry.setOnClickListener {
                        retryClickListener?.onClick(this)
                    }
                }
        networkErrorView?.let { addView(it) }
    }

    /**
     * 创建 UnknownError 视图
     */
    private fun createUnknownErrorView(
        iconRes: Int? = null,
        text: String? = null,
        retryText: String? = null,
    ) {
        unknownErrorView =
            LayoutInflater.from(context).inflate(R.layout.common_page_error_network, this, false)
                .apply {
                    val binding = CommonPageErrorNetworkBinding.bind(this)
                    iconRes?.let { binding.imgError.setImageResource(it) }
                    text?.let { binding.textError.text = it }
                    retryText?.let { binding.textRetry.text = it }
                    binding.textRetry.setOnClickListener {
                        retryClickListener?.onClick(this)
                    }
                }
        unknownErrorView?.let { addView(it) }
    }

    /**
     * 切换状态，仅显示当前状态对应的视图，隐藏其他
     */
    private fun showState(state: State) {
        currentState = state
        loadingView?.visibility = if (state == State.LOADING) VISIBLE else GONE
        emptyView?.visibility = if (state == State.EMPTY) VISIBLE else GONE
        networkErrorView?.visibility = if (state == State.NETWORK_ERROR) VISIBLE else GONE
        unknownErrorView?.visibility = if (state == State.UNKNOWN_ERROR) VISIBLE else GONE
    }
}
