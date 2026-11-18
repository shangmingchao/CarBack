package cn.frank.carback.opensdk.impl

import android.os.Bundle
import androidx.fragment.app.Fragment
import cn.frank.carback.opensdk.api.SdkUi
import cn.frank.carback.route.Router

/**
 * UI 门面实现：按路由创建宿主页面 Fragment
 *
 * 通过 Router 查表拿到 Fragment Class 反射实例化，第三方只拿到
 * [Fragment] 基类引用，看不到 feature Fragment 的具体类型。
 *
 * @author shangmingchao
 */
internal object SdkUiImpl : SdkUi {

    override fun createPageFragment(path: String, args: Bundle?): Fragment =
        Router.routeClass(path)
            .getDeclaredConstructor()
            .newInstance()
            .apply { arguments = args }
}
