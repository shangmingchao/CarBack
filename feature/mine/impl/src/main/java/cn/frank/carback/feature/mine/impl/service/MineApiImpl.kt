package cn.frank.carback.feature.mine.impl.service

import cn.frank.carback.feature.mine.api.MineApi
import cn.frank.carback.feature.mine.api.TrackItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [MineApi] 实现：内存收听历史仓库。
 *
 * home/search 播放时 record() 回写，首页与"我的"页通过 listenHistory 响应式刷新。
 *
 * @author shangmingchao
 */
@Singleton
class MineApiImpl @Inject constructor() : MineApi {

    private val history = MutableStateFlow(
        listOf(
            TrackItem("mock-1", "夜空中最亮的星", "逃跑计划"),
            TrackItem("mock-2", "平凡之路", "朴树"),
            TrackItem("mock-3", "海阔天空", "Beyond"),
        ),
    )

    override val listenHistory: StateFlow<List<TrackItem>> = history.asStateFlow()

    override fun record(track: TrackItem) {
        history.update { current ->
            (listOf(track) + current.filterNot { it.id == track.id }).take(MAX_SIZE)
        }
    }

    override fun clearHistory() {
        history.value = emptyList()
    }

    private companion object {
        const val MAX_SIZE = 20
    }
}
