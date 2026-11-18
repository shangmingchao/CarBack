package cn.frank.carback.opensdk.api

import kotlinx.coroutines.flow.StateFlow

/**
 * SDK 对外数据门面（只读契约）
 *
 * 第三方通过本接口消费宿主的曲目能力，看不到内部 MineApi / SearchApi /
 * Repository，也拿不到写入口（记录历史、清空历史等写操作不在 SDK 契约中）。
 *
 * @author shangmingchao
 */
interface SdkTrackProvider {

    /**
     * 收听历史（响应式，数据变化自动通知订阅方）
     */
    val listenHistory: StateFlow<List<SdkTrack>>

    /**
     * 按关键字搜索曲目；宿主未装配搜索能力时返回空列表（优雅降级）
     */
    fun search(keyword: String): List<SdkTrack>
}
