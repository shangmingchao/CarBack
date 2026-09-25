package cn.frank.carback.page

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.frank.carback.model.FIRST_PAGE_NO
import cn.frank.carback.model.RefreshTrigger
import cn.frank.carback.model.UiStateFlow
import cn.frank.carback.model.uiStateFlow
import cn.frank.carback.recyclerview.ItemModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.collections.isNotEmpty

/**
 * 通用列表页面 ViewModel
 *
 * @author shangmingchao
 */
abstract class CommonListViewModel : ViewModel() {

    protected var pageNo = FIRST_PAGE_NO

    /**
     * 已加载的全量数据
     */
    private val allData = mutableListOf<ItemModel>()

    open val listStateFlow: UiStateFlow<List<ItemModel>> = uiStateFlow(viewModelScope) { trigger ->
        getListFlow(if (trigger is RefreshTrigger.Refresh) FIRST_PAGE_NO else pageNo)
            .map { newList ->
                withContext(Dispatchers.Main.immediate) { accumulate(trigger, newList) }
            }
    }

    abstract fun getListFlow(pageNo: Int): Flow<List<ItemModel>>

    private fun accumulate(trigger: RefreshTrigger, newList: List<ItemModel>): List<ItemModel> {
        if (trigger is RefreshTrigger.Refresh) {
            pageNo = FIRST_PAGE_NO
            allData.clear()
        }
        allData.addAll(newList)
        if (newList.isNotEmpty()) {
            pageNo++
        }
        return allData.toList()
    }

    /**
     * 下拉刷新
     *
     * @param emitLoading 是否发送页面级加载中状态（由视图层决定，如列表为空时）
     */
    fun refresh(emitLoading: Boolean = false) {
        listStateFlow.refresh(emitLoading)
    }

    /**
     * 上拉加载更多
     */
    fun loadMore() {
        listStateFlow.loadMore()
    }
}
