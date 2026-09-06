package cn.frank.carback.page

import android.view.View
import cn.frank.carback.model.LoadType
import cn.frank.carback.model.UiError
import cn.frank.carback.model.UiState
import com.scwang.smart.refresh.layout.SmartRefreshLayout

/**
 * 绑定页面状态
 *
 * @author shangmingchao
 */
fun <T> composePageState(
    state: UiState<T>,
    contentView: View,
    pageStateView: PageStateView,
    retryAction: (() -> Unit)? = null,
    successAction: ((UiState.Success<T>) -> Unit)? = null,
) {
    pageStateView.setOnRetryClickListener { v ->
        retryAction?.invoke()
    }
    when (state) {
        is UiState.Loading -> {
            contentView.visibility = View.GONE
            pageStateView.showLoading()
        }

        is UiState.Success -> {
            contentView.visibility = View.VISIBLE
            pageStateView.showOrHide(false)
            successAction?.invoke(state)
            if (contentView is SmartRefreshLayout) {
                if (state.loadType == LoadType.Refresh) {
                    contentView.finishRefresh()
                } else {
                    contentView.finishLoadMore()
                }
            }
        }

        is UiState.Error -> {
            if (contentView is SmartRefreshLayout) {
                if (state.loadType == LoadType.Refresh) {
                    contentView.finishRefresh()
                } else {
                    contentView.finishLoadMore()
                }
            }
            if (state.loadType == LoadType.LoadMore) {
                return
            }
            contentView.visibility = View.GONE
            when (state.error) {
                is UiError.NetworkError -> {
                    pageStateView.showNetworkError()
                }

                is UiError.EmptyDataError -> {
                    pageStateView.showEmpty()
                }

                is UiError.UnknownError -> {
                    pageStateView.showUnknownError()
                }
            }
        }
    }
}
