package cn.frank.carback.feature.search.impl

import cn.frank.carback.feature.home.api.TrackUiItem
import cn.frank.carback.feature.search.api.SearchApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [SearchApi] 实现：内存 mock 搜索源。
 *
 * @author shangmingchao
 */
@Singleton
class SearchApiImpl @Inject constructor() : SearchApi {

    override fun search(keyword: String): List<TrackUiItem> =
        SEARCH_SOURCE
            .filter { keyword.isBlank() || it.contains(keyword, ignoreCase = true) }
            .map { source ->
                TrackUiItem(
                    id = "search_${source.hashCode()}",
                    title = source,
                    artist = "搜索结果",
                )
            }

    private companion object {
        val SEARCH_SOURCE = listOf(
            "海阔天空 - Beyond",
            "光辉岁月 - Beyond",
            "夜空中最亮的星 - 逃跑计划",
            "平凡之路 - 朴树",
            "晴天 - 周杰伦",
            "七里香 - 周杰伦",
            "告白气球 - 周杰伦",
            "青花瓷 - 周杰伦",
        )
    }
}
