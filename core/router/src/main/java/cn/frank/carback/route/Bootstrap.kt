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

    /**
     * @param lenientRoutes 宽松模式：路由未装配时优雅降级而非快速失败崩溃。
     *   仅 demo 壳（独立编译运行，classpath 上只有本 feature impl）应传 true；
     *   生产壳保持默认 false，路由缺失在第一跳快速失败。
     */
    fun install(context: Context, lenientRoutes: Boolean = false) {
        Router.lenient = lenientRoutes
        Router.init(context)
        ServiceRegistry.init(context)
    }
}
