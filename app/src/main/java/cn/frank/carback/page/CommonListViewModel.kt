package cn.frank.carback.page

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.frank.carback.model.FIRST_PAGE_NO
import cn.frank.carback.model.RefreshTrigger
import cn.frank.carback.model.UiStateFlow
import cn.frank.carback.model.uiStateFlow
import cn.frank.carback.recyclerview.ItemModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.collections.isNotEmpty

/**
 * 通用列表页面 ViewModel
 *
 * @author shangmingchao
 */
abstract class CommonListViewModel : ViewModel() {

    protected var pageNo = FIRST_PAGE_NO

    private val allData = mutableListOf<ItemModel>()

    open val listStateFlow: UiStateFlow<List<ItemModel>> = uiStateFlow(viewModelScope) { trigger ->
        getListFlow(if (trigger is RefreshTrigger.Refresh) FIRST_PAGE_NO else pageNo).map { newList ->
            if (trigger is RefreshTrigger.Refresh) {
                pageNo = FIRST_PAGE_NO
                allData.clear()
            }
            allData.addAll(newList)
            if (newList.isNotEmpty()) {
                pageNo++
            }
            allData.toList()
        }
    }

    abstract fun getListFlow(pageNo: Int): Flow<List<ItemModel>>

    /**
     * 下拉刷新
     */
    fun refresh() {
        listStateFlow.refresh(allData.isEmpty())
    }

    /**
     * 上拉加载更多
     */
    fun loadMore() {
        listStateFlow.loadMore()
    }
}
