package cn.frank.carback.feature.home.impl.viewmodel

import androidx.lifecycle.ViewModel
import cn.frank.carback.feature.home.impl.model.home.Tab
import dagger.hilt.android.lifecycle.HiltViewModel

/**
 * 首页 Tab 数据（静态配置，后续可替换为接口下发）
 *
 * @author shangmingchao
 */
class HomeViewModel : ViewModel() {

    val tabs: List<Tab> = listOf(
        Tab("home/recommend", "首页"),
        Tab("mine/main", "我的"),
    )
}
