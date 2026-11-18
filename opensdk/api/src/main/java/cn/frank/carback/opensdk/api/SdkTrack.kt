package cn.frank.carback.opensdk.api

/**
 * SDK 对外曲目模型（内部 TrackItem / TrackUiItem 的显式映射子集）
 *
 * 第三方只能看到本模型声明的字段；内部模型的字段变更不会泄漏到 SDK 契约。
 *
 * @author shangmingchao
 */
data class SdkTrack(
    val id: String,
    val title: String,
    val artist: String,
)
