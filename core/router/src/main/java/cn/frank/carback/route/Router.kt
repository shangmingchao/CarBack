package cn.frank.carback.route

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import cn.frank.carback.navigation.FragmentAnimation
import cn.frank.carback.navigation.FragmentRouter
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Fragment 路由器
 *
 * - 各 feature impl 用 @Route("xxx/main") 标注 Fragment，KSP 编译期生成
 *   "*ImplRouterModule"（Hilt 多绑定，零反射）；
 * - 宿主（:app 或各 demo 壳）启动时经 [Bootstrap.install] 聚合"当前 classpath 上
 *   所有 feature"的路由，feature 之间零依赖也能互相跳转；
 * - 路由路径冲突在启动第一刻快速失败；
 * - 实际页面管理复用 [FragmentRouter]（自维护返回栈 + hide/show）。
 *
 * @author shangmingchao
 */
object Router {

    private const val TAG = "Router"

    private val routes = mutableMapOf<String, RouteEntry>()

    /**
     * 宽松模式：仅 demo 壳（[Bootstrap.install] 传入）开启。
     *
     * 独立编译运行时 classpath 上只有本 feature 的 impl，未装配的路由（如 home demo
     * 里跳 "search/main"）在宽松模式下优雅降级（打日志、不跳转），而非抛
     * "No @Route registered for path" 崩溃；生产壳保持快速失败。
     */
    @Volatile
    internal var lenient: Boolean = false
        internal set

    /**
     * 初始化：收集所有 RouteEntry（Hilt 多绑定），并进行路径冲突快速失败检查。
     */
    fun init(context: Context) {
        if (routes.isNotEmpty()) {
            return
        }
        val entry = EntryPointAccessors.fromApplication(
            context.applicationContext,
            RouterEntryPoint::class.java,
        )
        val entries = entry.routeEntries()
        val conflicts = entries.groupBy { it.path }.filterValues { it.size > 1 }
        require(conflicts.isEmpty()) {
            "路由路径冲突：${conflicts.keys}。" +
                "冲突来源：${conflicts.values.map { list -> list.map { e -> e.fragmentClass.name } }}"
        }
        entries.forEach { entryItem -> routes[entryItem.path] = entryItem }
    }

    /**
     * 查询路由对应的 Fragment Class
     */
    fun routeClass(path: String): Class<out Fragment> =
        routes[path]?.fragmentClass ?: throw IllegalArgumentException(
            "No @Route registered for path \"$path\"。请确认对应 feature impl 已编入 classpath。",
        )

    /**
     * 是否存在指定路由
     */
    fun hasRoute(path: String): Boolean = routes.containsKey(path)

    /**
     * 所有已注册路由
     */
    fun routes(): Set<String> = routes.keys.toSet()

    /**
     * 创建路由对应的 Fragment 实例
     *
     * 路由未装配时：宽松模式下打日志并返回 null（demo 独立运行的优雅降级）；
     * 严格模式下抛出快速失败异常。
     *
     * @param args 传给 Fragment.arguments
     */
    fun fragment(path: String, args: Bundle? = null): Fragment? {
        val fragmentClass = routes[path]?.fragmentClass ?: run {
            if (lenient) {
                Log.w(TAG, "路由未装配（对应 feature impl 未编入 classpath），已降级忽略：$path")
                return null
            }
            throw IllegalArgumentException(
                "No @Route registered for path \"$path\"。请确认对应 feature impl 已编入 classpath。",
            )
        }
        return fragmentClass.getDeclaredConstructor().newInstance().apply {
            this.arguments = args
        }
    }

    /**
     * 跳转到指定路由页面（入栈）
     *
     * 路由未装配时：宽松模式下打日志并忽略（demo 独立运行的优雅降级）；
     * 严格模式下抛出快速失败异常。
     */
    fun navigate(
        path: String,
        args: Bundle? = null,
        animation: FragmentAnimation = FragmentAnimation.SLIDE_IN_RIGHT,
        singleInstance: Boolean = false,
    ) {
        val fragment = fragment(path, args) ?: return
        FragmentRouter.push(
            fragment = fragment,
            tag = path,
            animation = animation,
            singleInstance = singleInstance,
        )
    }

    /**
     * 返回上一个页面（出栈）
     */
    fun back(): Boolean {
        return FragmentRouter.pop()
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface RouterEntryPoint {
        fun routeEntries(): Set<@JvmSuppressWildcards RouteEntry>
    }
}
