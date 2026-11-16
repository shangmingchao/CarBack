package cn.frank.carback.feature.home.impl

import androidx.lifecycle.ViewModel
import cn.frank.carback.feature.home.impl.model.home.Tab

/**
 * 首页 Tab 数据（静态配置，后续可替换为接口下发）
 *
 * @author shangmingchao
 */
class HomeViewModel : ViewModel() {

    val tabs: List<Tab> = listOf(
        Tab("recommend", "推荐"),
        Tab("news", "资讯"),
        Tab("radio", "广播"),
        Tab("all", "全部"),
    )
}
