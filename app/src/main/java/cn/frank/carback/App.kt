package cn.frank.carback

import android.app.Application
import cn.frank.carback.route.Bootstrap
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import dagger.hilt.android.HiltAndroidApp

/**
 * 主壳 Application：只做启动装配（路由表 + 服务表 + 刷新样式默认值）
 *
 * @author shangmingchao
 */
@HiltAndroidApp
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        SmartRefreshLayout.setDefaultRefreshHeaderCreator { _, _ ->
            ClassicsHeader(this@App)
        }
        SmartRefreshLayout.setDefaultRefreshFooterCreator { _, _ ->
            ClassicsFooter(this@App)
        }
        Bootstrap.install(this)
    }
}
