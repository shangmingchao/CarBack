package cn.frank.carback.opensdk.impl

import android.content.Context
import cn.frank.carback.opensdk.api.SdkBridge
import cn.frank.carback.opensdk.api.SdkTrackProvider
import cn.frank.carback.opensdk.api.SdkUi
import cn.frank.carback.route.Bootstrap

/**
 * SDK 桥接实现（由 :opensdk:api 反射加载）
 *
 * install 统一完成宿主运行时装配（路由表 + 服务表），
 * 数据/UI 门面按需创建，内部依赖不向外泄漏。
 *
 * @author shangmingchao
 */
class SdkBridgeImpl : SdkBridge {

    override fun install(context: Context) {
        Bootstrap.install(context)
    }

    override fun trackProvider(): SdkTrackProvider = SdkTrackProviderImpl

    override fun ui(): SdkUi = SdkUiImpl
}
