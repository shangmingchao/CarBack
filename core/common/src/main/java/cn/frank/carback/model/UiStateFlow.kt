package cn.frank.carback.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.channels.BufferOverflow
import java.util.concurrent.atomic.AtomicLong

/**
 * 刷新触发类型：区分下拉刷新与上拉加载更多
 *
 * @author shangmingchao
 */
sealed interface RefreshTrigger {

    /**
     * 下拉刷新
     *
     * @param emitLoading 是否发送加载中状态
     */
    data class Refresh(val emitLoading: Boolean = true) : RefreshTrigger

    /**
     * 上拉加载更多
     */
    data object LoadMore : RefreshTrigger
}

/**
 * Success 状态的唯一 id 计数器，用于绕过 StateFlow 的去重
 */
private val UNIQUE_ID = AtomicLong(0L)

/**
 * UI 状态封装
 */
class UiStateFlow<T>(
    val state: StateFlow<UiState<T>>,
    private val refreshTrigger: MutableSharedFlow<RefreshTrigger>,
) {

    /**
     * 刷新第一页数据
     */
    fun refresh(emitLoading: Boolean = false) {
        refreshTrigger.tryEmit(RefreshTrigger.Refresh(emitLoading))
    }

    /**
     * 请求下一页数据
     */
    fun loadMore() {
        refreshTrigger.tryEmit(RefreshTrigger.LoadMore)
    }
}

/**
 * 创建 UI 状态流
 *
 * @param scope 协程作用域
 * @param emitLoadingOnStart 首次订阅时是否立即发送加载中状态
 * @param checkEmptyData 检查数据是否为空，为空时返回 UiState.Error(UiError.EmptyDataError(), loadType)
 * @param request 请求数据，参数为 [RefreshTrigger]，返回 [Flow] 数据
 *
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> uiStateFlow(
    scope: CoroutineScope,
    emitLoadingOnStart: Boolean = true,
    checkEmptyData: (T) -> Boolean = { it == null || (it is List<*> && it.isEmpty()) },
    request: suspend (RefreshTrigger) -> Flow<T>,
): UiStateFlow<T> {
    val refreshTrigger = MutableSharedFlow<RefreshTrigger>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val state = refreshTrigger
        .onStart { emit(RefreshTrigger.Refresh(emitLoadingOnStart)) }
        .flatMapLatest { trigger ->
            request(trigger)
                .asStateResult((trigger as? RefreshTrigger.Refresh)?.emitLoading ?: false)
                .map { result ->
                    val loadType =
                        if (trigger is RefreshTrigger.Refresh) LoadType.Refresh else LoadType.LoadMore
                    when (result) {
                        is StateResult.Success -> {
                            if (checkEmptyData(result.data)) {
                                UiState.Error(UiError.EmptyDataError(), loadType)
                            } else {
                                UiState.Success(
                                    result.data,
                                    uniqueId = UNIQUE_ID.incrementAndGet().toString(),
                                    loadType = loadType,
                                )
                            }
                        }

                        is StateResult.Loading -> UiState.Loading(
                            loadType = loadType,
                        )

                        is StateResult.Error -> UiState.Error(
                            error = UiError.NetworkError(result.exception),
                            loadType = loadType,
                        )
                    }
                }
        }
        .stateIn(scope, SharingStarted.Lazily, UiState.Loading())
    return UiStateFlow(state, refreshTrigger)
}
