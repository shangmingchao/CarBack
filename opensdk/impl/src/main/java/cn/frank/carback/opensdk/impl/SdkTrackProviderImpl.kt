package cn.frank.carback.opensdk.impl

import cn.frank.carback.feature.home.api.TrackUiItem
import cn.frank.carback.feature.mine.api.MineApi
import cn.frank.carback.feature.mine.api.TrackItem
import cn.frank.carback.feature.search.api.SearchApi
import cn.frank.carback.opensdk.api.SdkTrack
import cn.frank.carback.opensdk.api.SdkTrackProvider
import cn.frank.carback.service.ServiceRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * 数据门面实现：内部 MineApi / SearchApi → SDK 只读子集
 *
 * `toSdkTrack()` 是编译期强制执行的子集裁剪：第三方拿到的 [SdkTrack]
 * 不可能包含内部模型未显式映射的字段；写操作（record / clearHistory）
 * 不在 [SdkTrackProvider] 契约中，第三方编译期就没有写入口。
 *
 * @author shangmingchao
 */
internal object SdkTrackProviderImpl : SdkTrackProvider {

    private val providerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val listenHistory: StateFlow<List<SdkTrack>> by lazy {
        val source = ServiceRegistry.require(MineApi::class.java).listenHistory
        source.map { history ->
            history.map { it.toSdkTrack() }
        }.stateIn(
            scope = providerScope,
            started = SharingStarted.Eagerly,
            initialValue = source.value.map { it.toSdkTrack() },
        )
    }

    override fun search(keyword: String): List<SdkTrack> =
        ServiceRegistry.find(SearchApi::class.java)
            ?.search(keyword)
            ?.map { it.toSdkTrack() }
            ?: emptyList()
}

private fun TrackItem.toSdkTrack(): SdkTrack = SdkTrack(
    id = id,
    title = title,
    artist = artist,
)

private fun TrackUiItem.toSdkTrack(): SdkTrack = SdkTrack(
    id = id,
    title = title,
    artist = artist,
)
