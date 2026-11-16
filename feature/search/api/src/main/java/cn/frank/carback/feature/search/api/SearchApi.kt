package cn.frank.carback.feature.search.api

import cn.frank.carback.feature.home.api.TrackUiItem
import cn.frank.carback.service.FeatureApi

/**
 * 搜索能力契约（:feature:search 对外暴露的唯一入口）
 *
 * @author shangmingchao
 */
interface SearchApi : FeatureApi {

    /**
     * 按关键字搜索曲目
     */
    fun search(keyword: String): List<TrackUiItem>
}
