package cn.frank.carback.ext

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

private object Unset

/**
 * 过滤掉重复发射的值，**跨收集重启生效**。
 *
 * 解决的问题：StateFlow 在配合 repeatOnLifecycle（如 [cn.frank.carback.page.BaseFragment.startCollect]）
 * 使用时，每次生命周期回到 RESUMED 都会重新收集并重发当前值。如果 collect 回调里有
 * 重建 Adapter / 重置 UI 等操作，会导致页面状态被意外重置。
 *
 * 与 [kotlinx.coroutines.flow.distinctUntilChanged] 的区别：
 * 后者只在一次收集（同一个 collector）内部去重，收集重启后状态丢失；
 * 本算子把“上一次发射的值”记录在算子实例中，重启收集后依然会过滤掉未变化的值。
 *
 * 注意：必须在 collector lambda **外部**调用（如 onViewCreated 中构建流时），
 * 保证多次重启收集复用同一个算子实例；若写在 collector 内部，每次重启收集
 * 都会新建算子实例导致过滤失效。例如：
 * ```
 * val stateFlow = viewModel.someState.dropSticky()
 * startCollect({
 *     stateFlow.collect { value -> ... }
 * })
 * ```
 * 视图销毁重建时会创建新的算子实例，首值正常发射，不会误过滤。
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
