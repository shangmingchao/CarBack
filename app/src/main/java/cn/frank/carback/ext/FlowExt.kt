package cn.frank.carback.ext

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

private object Unset

/**
 * 过滤掉重复发射的值
 */
fun <T> Flow<T>.dropSticky(): Flow<T> {
    var last: Any? = Unset
    return transform { value ->
        if (last === Unset || last != value) {
            last = value
            emit(value)
        }
    }
}
