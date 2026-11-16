package cn.frank.carback.sdk

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * CarBack SDK 对外契约
 *
 * 第三方 APK 只需依赖 :sdk（对外发布时为独立 AAR），通过 [CarBackSdk]
 * 拉起宿主（CarBack）的指定页面或查询能力，无需依赖任何内部模块。
 *
 * @author shangmingchao
 */
object SdkContract {

    /** 打开宿主页面的 Action */
    const val ACTION_OPEN_PAGE = "cn.frank.carback.sdk.action.OPEN_PAGE"

    /** 页面路由 path，与宿主 @Route 声明一致，如 "home/main"、"mine/main"、"search/main" */
    const val EXTRA_PAGE_PATH = "cn.frank.carback.sdk.extra.PAGE_PATH"

    /** 宿主包名 */
    const val HOST_PACKAGE = "cn.frank.carback"

    /** 宿主 SDK 入口 Activity */
    const val HOST_ENTRY_ACTIVITY = "cn.frank.carback.SdkEntryActivity"
}

/**
 * CarBack SDK 对外门面
 *
 * 用法（第三方 APK）：
 * ```
 * // 打开宿主的搜索页
 * CarBackSdk.openPage(context, "search/main")
 * ```
 *
 * 宿主侧集成：:app 内的 [SdkEntryActivity]（exported）处理 [SdkContract.ACTION_OPEN_PAGE]，
 * 根据 [SdkContract.EXTRA_PAGE_PATH] 通过 Router 完成跳转。
 *
 * 若第三方希望直接内嵌页面/获取数据（同进程集成），可依赖 :core:router + 对应 feature 的
 * :impl（或 :api），并在 Application 中调用 Bootstrap.install 后使用 Router / ServiceRegistry /
 * EventBus；此方式适合与宿主同渠道分发的场景。
 *
 * @author shangmingchao
 */
object CarBackSdk {

    /**
     * 打开宿主的指定页面
     *
     * @param context 任意 Context
     * @param path 页面路由，如 "home/main"、"mine/main"、"search/main"
     * @param hostPackage 宿主包名，默认 CarBack
     * @return 宿主是否安装（Intent 是否可解析），实际页面由宿主处理
     */
    @JvmStatic
    @JvmOverloads
    fun openPage(context: Context, path: String, hostPackage: String = SdkContract.HOST_PACKAGE): Boolean {
        val intent = Intent(SdkContract.ACTION_OPEN_PAGE).apply {
            setClassName(
                hostPackage,
                SdkContract.HOST_ENTRY_ACTIVITY,
            )
            putExtra(SdkContract.EXTRA_PAGE_PATH, path)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (context.packageManager.resolveActivity(intent, 0) == null) {
            return false
        }
        context.startActivity(intent)
        return true
    }

    /**
     * 跳转到宿主应用商店/应用详情页（引导安装宿主）
     */
    @JvmStatic
    fun openHostMarket(context: Context, hostPackage: String = SdkContract.HOST_PACKAGE) {
        val marketIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("market://details?id=$hostPackage")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(marketIntent) }
    }
}
