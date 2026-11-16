package cn.frank.carback.bus

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance

/**
 * 轻量事件总线（基于 SharedFlow）
 *
 * - 跨 feature 的事件定义为实现 [Event] 的数据类（放在各 feature 的 api 契约模块中）；
 * - 不持有历史事件（非粘性），接收方需先订阅再等待发送；
 * - [extraBufferCapacity] 保证发送方在无订阅者时也不挂起（事件丢弃）。
 *
 * @author shangmingchao
 */
object EventBus {

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 64)

    val events: SharedFlow<Event> = _events.asSharedFlow()

    /**
     * 发送事件（非挂起，无订阅者时事件被丢弃）
     */
    fun post(event: Event) {
        _events.tryEmit(event)
    }

    /**
     * 按类型订阅事件（非粘性，订阅之后发送的事件才会收到）
     */
    inline fun <reified T : Event> on(): Flow<T> = events.filterIsInstance<T>()
}
