package cn.frank.carback.feature.mine.api

import cn.frank.carback.service.FeatureApi
import kotlinx.coroutines.flow.StateFlow

/**
 * 我的页面能力契约（收听历史，:feature:mine 对外暴露的唯一入口）
 *
 * home/search 播放时调用 [record] 回写，
 * 首页与"我的"页通过 [listenHistory] 响应式刷新。
 *
 * @author shangmingchao
 */
interface MineApi : FeatureApi {

    /**
     * 收听历史（响应式，数据变化自动通知所有订阅方）
     */
    val listenHistory: StateFlow<List<TrackItem>>

    /**
     * 记录一条播放历史
     */
    fun record(track: TrackItem)

    /**
     * 清空收听历史
     */
    fun clearHistory()
}
