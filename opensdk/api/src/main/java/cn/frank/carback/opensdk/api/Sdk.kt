package cn.frank.carback.opensdk.api

import android.content.Context

/** 反射加载的桥接实现类（:opensdk:impl 提供） */
internal const val SDK_BRIDGE_CLASS = "cn.frank.carback.opensdk.impl.SdkBridgeImpl"

/**
 * CarBack SDK 统一入口（同进程集成）
 *
 * 用法（第三方 APK，Application 为 @HiltAndroidApp 时）：
 * ```
 * class App : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         Sdk.init(this)
 *     }
 * }
 *
 * // 消费宿主数据（只读）
 * val provider: SdkTrackProvider = Sdk.trackProvider()
 * lifecycleScope.launch {
 *     provider.listenHistory.collect { tracks -> ... }
 * }
 *
 * // 内嵌宿主页面
 * val fragment = Sdk.ui().createPageFragment("search/main", bundleOf("keyword" to "abc"))
 * ```
 *
 * 实现绑定：[init] 通过反射加载 [SDK_BRIDGE_CLASS]（由 :opensdk:impl 提供），
 * 第三方编译期只看到本文件中的门面契约，内部模块全部隐藏。
 *
 * @author shangmingchao
 */
object Sdk {

    @Volatile
    private var bridge: SdkBridge? = null

    /**
     * 初始化 SDK（幂等）
     *
     * @param context 任意 Context（内部会取 applicationContext）
     * @throws IllegalStateException :opensdk:impl 未编入 classpath
     */
    @JvmStatic
    fun init(context: Context) {
        if (bridge == null) {
            synchronized(this) {
                if (bridge == null) {
                    bridge = loadBridge()
                }
            }
        }
        bridge?.install(context.applicationContext)
    }

    /**
     * 数据门面（只读）
     */
    @JvmStatic
    fun trackProvider(): SdkTrackProvider =
        requireBridge().trackProvider()

    /**
     * UI 门面（Fragment 工厂）
     */
    @JvmStatic
    fun ui(): SdkUi =
        requireBridge().ui()

    private fun loadBridge(): SdkBridge {
        val impl = runCatching {
            Class.forName(SDK_BRIDGE_CLASS)
                .getDeclaredConstructor()
                .newInstance()
        }.getOrElse {
            throw IllegalStateException(
                "SdkBridge 实现未找到（$SDK_BRIDGE_CLASS），请确认 :opensdk:impl 已编入 classpath",
                it,
            )
        }
        return impl as SdkBridge
    }

    private fun requireBridge(): SdkBridge =
        bridge ?: throw IllegalStateException("Sdk.init(context) not called")
}
