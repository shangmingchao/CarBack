package cn.frank.carback.model

/**
 * 加载类型：区分下拉刷新与上拉加载更多
 *
 * @author shangmingchao
 */
enum class LoadType {
    /** 下拉刷新 / 首次加载 */
    Refresh,

    /** 上拉加载更多 */
    LoadMore,
}

/**
 * 首次页码
 */
const val FIRST_PAGE_NO = 1

/**
 * UI 状态
 */
sealed interface UiState<out T> {
    data class Success<T>(
        val data: T,
        val uniqueId: String? = null,
        val loadType: LoadType = LoadType.Refresh,
    ) : UiState<T>

    data class Error(
        val error: UiError,
        val loadType: LoadType = LoadType.Refresh,
    ) : UiState<Nothing>

    data class Loading<T>(
        val loadType: LoadType = LoadType.Refresh,
    ) : UiState<T>
}

sealed class UiError {
    data class NetworkError(val exception: Throwable? = null) : UiError()
    data class EmptyDataError(val msg: String = "暂无内容") : UiError()
    data class UnknownError(val exception: Throwable? = null) : UiError()
}
