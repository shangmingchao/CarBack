package cn.frank.carback.feature.mine.demo

import cn.frank.carback.core.debug.scaffold.BaseDemoApplication
import cn.frank.carback.core.debug.scaffold.ScaffoldActivity
import dagger.hilt.android.HiltAndroidApp

/**
 * demo-mine 薄壳：只依赖 :core + :feature:mine:impl，独立调试收听历史页。
 *
 * @author shangmingchao
 */
@HiltAndroidApp
class DemoApplication : BaseDemoApplication()

class DemoMainActivity : ScaffoldActivity() {
    override val startRoute: String get() = "mine/main"
}
