package cn.frank.carback.route

import androidx.fragment.app.Fragment

/**
 * 一条路由注册项（由 KSP 生成的 *ImplRouterModule 装配，零反射）
 *
 * @author shangmingchao
 */
class RouteEntry(
    val path: String,
    val fragmentClass: Class<out Fragment>,
)
