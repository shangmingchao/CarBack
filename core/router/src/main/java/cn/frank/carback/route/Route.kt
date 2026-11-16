package cn.frank.carback.route

/**
 * Fragment 路由注解
 *
 * 被标注的 Fragment 必须：
 * - 继承 [androidx.fragment.app.Fragment]；
 * - 提供无参构造（参数通过 ServiceRegistry / EventBus / 自身默认值获取）。
 *
 * 编译期由 :core:router-compiler (KSP) 扫描并在 impl 模块内生成
 * "<Feature>ImplRouterModule" 装配代码（Hilt 多绑定，零反射），
 * 宿主启动时经 [Bootstrap.install] 聚合。
 *
 * 示例：
 * ```
 * @Route("search/main")
 * class SearchFragment : BaseFragment<FragmentSearchBinding>() { ... }
 *
 * Router.open("search/main")
 * ```
 *
 * @author shangmingchao
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Route(
    /**
     * 路由路径，如 "home/main"、"mine/main"、"search/main"
     */
    val path: String,
)
