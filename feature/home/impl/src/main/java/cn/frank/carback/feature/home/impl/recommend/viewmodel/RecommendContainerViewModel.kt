package cn.frank.carback.feature.home.impl.recommend.viewmodel

import androidx.lifecycle.ViewModel
import cn.frank.carback.feature.home.impl.model.home.Tab
import dagger.hilt.android.lifecycle.HiltViewModel

/**
 *
 *
 * @author shangmingchao
 */
class RecommendContainerViewModel : ViewModel() {
    val tabs: List<Tab> = listOf(
        Tab("recommend", "推荐"),
        Tab("news", "资讯"),
        Tab("radio", "广播"),
        Tab("all", "全部"),
    )
}
