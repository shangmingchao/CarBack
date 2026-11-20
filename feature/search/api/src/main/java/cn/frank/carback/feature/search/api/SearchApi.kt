package cn.frank.carback.feature.search.api

import cn.frank.carback.service.FeatureApi
import cn.frank.carback.shared.model.TrackModel

/**
 * 搜索能力契约（:feature:search 对外暴露的唯一入口）
 *
 * @author shangmingchao
 */
interface SearchApi : FeatureApi {

    /**
     * 按关键字搜索曲目
     */
    fun search(keyword: String): List<TrackModel>
}
