package cn.frank.carback.opensdk.api

import android.os.Bundle
import androidx.fragment.app.Fragment

/**
 * SDK 对外 UI 门面
 *
 * 第三方只拿到 [Fragment] 基类引用，看不到 feature Fragment 的具体类型，
 * 也无法引用 feature 模块的其他页面。
 *
 * @author shangmingchao
 */
interface SdkUi {

    /**
     * 按路由 path 创建宿主页面 Fragment
     *
     * @param path 页面路由，如 "home/main"、"mine/main"、"search/main"
     * @param args 页面参数（写入 Fragment.arguments，key 由目标页面约定）
     * @return 宿主页面的 [Fragment] 基类实例
     * @throws IllegalArgumentException 路由未注册（对应 feature 未编入宿主）
     */
    fun createPageFragment(path: String, args: Bundle? = null): Fragment
}
