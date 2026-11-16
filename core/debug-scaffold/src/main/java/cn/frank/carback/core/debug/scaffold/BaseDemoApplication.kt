package cn.frank.carback.core.debug.scaffold

import android.app.Application
import cn.frank.carback.route.Bootstrap
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout

/**
 * demo 壳通用 Application 基类：Bootstrap.install（路由/服务装配）+ SmartRefresh 默认样式。
 *
 * 注意：@HiltAndroidApp 由各 demo 壳的具体 Application 自行标注
 * （Hilt 要求 Application 定义在 application 模块中）。
 *
 * @author shangmingchao
 */
abstract class BaseDemoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        SmartRefreshLayout.setDefaultRefreshHeaderCreator { _, _ ->
            ClassicsHeader(this@BaseDemoApplication)
        }
        SmartRefreshLayout.setDefaultRefreshFooterCreator { _, _ ->
            ClassicsFooter(this@BaseDemoApplication)
        }
        Bootstrap.install(this)
    }
}
