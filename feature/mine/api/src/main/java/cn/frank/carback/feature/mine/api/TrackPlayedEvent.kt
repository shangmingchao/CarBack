package cn.frank.carback.feature.mine.api

import cn.frank.carback.bus.Event

/**
 * 收听了某条曲目的事件
 *
 * 由 :feature:home:impl 播放时发送（EventBus），供任意 feature 订阅。
 *
 * @author shangmingchao
 */
data class TrackPlayedEvent(
    val track: TrackItem,
) : Event
