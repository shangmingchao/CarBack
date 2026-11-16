package cn.frank.carback.feature.home.demo

import cn.frank.carback.core.debug.scaffold.BaseDemoApplication
import cn.frank.carback.core.debug.scaffold.ScaffoldActivity
import dagger.hilt.android.HiltAndroidApp

/**
 * demo-home 薄壳：只依赖 :core + :feature:home:impl，独立调试首页。
 *
 * @author shangmingchao
 */
@HiltAndroidApp
class DemoApplication : BaseDemoApplication()

class DemoMainActivity : ScaffoldActivity() {
    override val startRoute: String get() = "home/main"
}
