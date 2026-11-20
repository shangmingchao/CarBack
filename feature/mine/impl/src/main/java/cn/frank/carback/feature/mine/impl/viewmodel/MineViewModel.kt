package cn.frank.carback.feature.mine.impl.viewmodel

import androidx.lifecycle.ViewModel
import cn.frank.carback.feature.mine.api.MineApi
import cn.frank.carback.feature.mine.impl.model.HistoryItemModel
import cn.frank.carback.service.ServiceRegistry
import kotlinx.coroutines.flow.map

/**
 * 我的页面 ViewModel：收听历史数据经 [ServiceRegistry] 按契约解析
 *
 * @author shangmingchao
 */
class MineViewModel : ViewModel() {

    private val mineApi = ServiceRegistry.service<MineApi>()

    val history = mineApi.listenHistory.map { tracks -> tracks.map { track -> HistoryItemModel(track) } }

    fun clearHistory() {
        mineApi.clearHistory()
    }
}
