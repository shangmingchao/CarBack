package cn.frank.carback.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * 带状态的结果（加载中/成功/失败）
 *
 * @author shangmingchao
 */
sealed interface StateResult<out T> {
    data class Success<T>(val data: T) : StateResult<T>
    data class Error(val exception: Throwable? = null) : StateResult<Nothing>
    object Loading : StateResult<Nothing>
}

/**
 * 将数据转换为带状态的结果
 */
inline fun <reified T> Flow<T>.asStateResult(emitLoading: Boolean = true): Flow<StateResult<T>> {
    return this
        .map<T, StateResult<T>> {
            StateResult.Success(it)
        }
        .onStart {
            if (emitLoading) {
                emit(StateResult.Loading)
            }
        }
        .catch {
            emit(StateResult.Error(it))
        }
}
