package cn.frank.carback.viewmodel

import androidx.lifecycle.ViewModel
import cn.frank.carback.model.home.Tab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 *
 *
 * @author shangmingchao
 */
class HomeViewModel : ViewModel() {

    private val _tabList = MutableStateFlow<List<Tab>>(emptyList())
    val tabList = _tabList.asStateFlow()

    init {
        _tabList.value = listOf(
            Tab("recommend", "推荐"),
            Tab("news", "资讯"),
            Tab("radio", "广播"),
            Tab("all", "全部"),
        )
    }
}
