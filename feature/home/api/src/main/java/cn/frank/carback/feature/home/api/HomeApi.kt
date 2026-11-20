package cn.frank.carback.feature.home.api

import cn.frank.carback.service.FeatureApi

/**
 * 首页能力契约
 *
 * @author shangmingchao
 */
interface HomeApi : FeatureApi {

    /**
     * 创建首页曲目列表 Adapter
     */
    fun createTrackAdapter(): TrackListAdapter
}
