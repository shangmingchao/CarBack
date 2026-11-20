package cn.frank.carback.feature.mine.impl.service

import cn.frank.carback.feature.mine.api.MineApi
import cn.frank.carback.shared.model.TrackModel
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

    private val _history = MutableStateFlow<List<TrackModel>>(emptyList())

    override val listenHistory: StateFlow<List<TrackModel>> = _history.asStateFlow()

    override fun record(track: TrackModel) {
        _history.update { current ->
            (listOf(track) + current.filterNot { it.id == track.id }).take(MAX_SIZE)
        }
    }

    override fun clearHistory() {
        _history.value = emptyList()
    }

    private companion object {
        const val MAX_SIZE = 20
    }
}
