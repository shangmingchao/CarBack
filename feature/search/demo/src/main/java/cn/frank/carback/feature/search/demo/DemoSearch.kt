package cn.frank.carback.feature.search.demo

import cn.frank.carback.core.debug.scaffold.BaseDemoApplication
import cn.frank.carback.core.debug.scaffold.ScaffoldActivity
import dagger.hilt.android.HiltAndroidApp

/**
 * demo-search 薄壳：独立调试搜索页。
 *
 * 搜索页的列表 Adapter 由 :feature:home:impl 提供（View 级契约），
 * 因此本 demo 额外装配 home impl；若不装配，搜索页自动降级为空态提示。
 *
 * @author shangmingchao
 */
@HiltAndroidApp
class DemoApplication : BaseDemoApplication()

class DemoMainActivity : ScaffoldActivity() {
    override val startRoute: String get() = "search/main"
}
