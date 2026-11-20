package cn.frank.carback.feature.home.impl

import androidx.recyclerview.widget.RecyclerView
import cn.frank.carback.feature.home.api.HomeApi
import cn.frank.carback.feature.home.api.TrackListAdapter
import cn.frank.carback.feature.home.impl.adapter.SongListAdapter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [HomeApi] 实现
 *
 * @author shangmingchao
 */
@Singleton
class HomeApiImpl @Inject constructor() : HomeApi {

    override fun createTrackAdapter(): TrackListAdapter = SongListAdapter()
}
