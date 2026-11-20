package cn.frank.carback.feature.home.impl.viewmodel

import androidx.lifecycle.ViewModel
import cn.frank.carback.feature.home.impl.model.home.Tab
import cn.frank.carback.route.Router
import dagger.hilt.android.lifecycle.HiltViewModel

/**
 * 首页 Tab 数据（静态配置，后续可替换为接口下发）
 *
 * 独立编译（demo 壳）时 classpath 上只有本 feature impl，
 * 未装配的路由（如 mine/main）通过 [Router.hasRoute] 过滤，优雅降级而非崩溃。
 *
 * @author shangmingchao
 */
class HomeViewModel : ViewModel() {

    val tabs: List<Tab> = buildList {
        add(Tab("home/recommend", "首页"))
        if (Router.hasRoute("mine/main")) {
            add(Tab("mine/main", "我的"))
        }
    }
}
