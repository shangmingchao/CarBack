package cn.frank.carback.opensdk.api

import android.content.Context

/**
 * SDK 桥接契约（仅由 :opensdk:impl 实现，第三方不感知）
 *
 * :opensdk:api 通过反射实例化 [SDK_BRIDGE_CLASS] 完成绑定，
 * 实现类必须提供公共无参构造。
 *
 * @author shangmingchao
 */
interface SdkBridge {

    /**
     * 安装宿主运行时（路由表 + 服务表），同进程集成的初始化入口
     */
    fun install(context: Context)

    /**
     * 提供数据门面实现
     */
    fun trackProvider(): SdkTrackProvider

    /**
     * 提供UI 门面实现
     */
    fun ui(): SdkUi
}
