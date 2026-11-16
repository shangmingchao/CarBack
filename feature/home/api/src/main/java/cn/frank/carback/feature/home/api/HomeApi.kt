package cn.frank.carback.feature.home.api

import cn.frank.carback.service.FeatureApi

/**
 * 首页能力契约（:feature:home 对外暴露的唯一入口）
 *
 * @author shangmingchao
 */
interface HomeApi : FeatureApi {

    /**
     * 创建首页曲目列表 Adapter（条目样式 / DiffUtil / 点击行为全部由 home 决定）
     */
    fun createTrackAdapter(): TrackListAdapter<*>

    /**
     * 播放一首曲目（内部回写 mine 收听历史 + 发送 TrackPlayedEvent）
     */
    fun play(item: TrackUiItem)
}
