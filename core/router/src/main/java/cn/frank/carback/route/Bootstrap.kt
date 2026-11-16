package cn.frank.carback.route

import android.content.Context
import cn.frank.carback.service.ServiceRegistry

/**
 * 宿主启动引导：统一注册路由表与服务表。
 *
 * 应在 HiltAndroidApp 的 Application#onCreate 中调用：
 * ```
 * class App : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         Bootstrap.install(this)
 *     }
 * }
 * ```
 *
 * 路由路径冲突 / 服务重复注册在此处第一时刻快速失败，不带到线上。
 *
 * @author shangmingchao
 */
object Bootstrap {

    fun install(context: Context) {
        Router.init(context)
        ServiceRegistry.init(context)
    }
}
