package cn.frank.carback.feature.home.impl

import cn.frank.carback.bus.EventBus
import cn.frank.carback.feature.home.api.HomeApi
import cn.frank.carback.feature.home.api.TrackListAdapter
import cn.frank.carback.feature.home.api.TrackUiItem
import cn.frank.carback.feature.home.impl.service.HomeTrackAdapter
import cn.frank.carback.feature.mine.api.MineApi
import cn.frank.carback.feature.mine.api.TrackItem
import cn.frank.carback.feature.mine.api.TrackPlayedEvent
import cn.frank.carback.service.ServiceRegistry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [HomeApi] 实现。
 *
 * 播放链路：play() -> 回写 MineApi.record（mine 未装配时优雅降级）-> 发送 TrackPlayedEvent。
 * 首页/搜索页任一处点击播放 -> mine 记录 -> 首页与"我的"页实时刷新。
 *
 * @author shangmingchao
 */
@Singleton
class HomeApiImpl @Inject constructor() : HomeApi {

    override fun createTrackAdapter(): TrackListAdapter<*> =
        HomeTrackAdapter(onItemClick = ::play)

    override fun play(item: TrackUiItem) {
        val track = TrackItem(id = item.id, title = item.title, artist = item.artist)
        ServiceRegistry.serviceOrNull<MineApi>()?.record(track)
        EventBus.post(TrackPlayedEvent(track))
    }
}
