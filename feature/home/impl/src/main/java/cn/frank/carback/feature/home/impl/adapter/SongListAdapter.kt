package cn.frank.carback.feature.home.impl.adapter

import cn.frank.carback.feature.home.api.TrackListAdapter
import cn.frank.carback.feature.home.impl.model.home.ImageTextModel
import cn.frank.carback.feature.home.impl.recommend.presenter.ImageTextPresenter
import cn.frank.carback.shared.model.TrackModel
import cn.frank.carback.track.EventTracker

/**
 * 歌曲列表 Adapter
 *
 * @author shangmingchao
 */
class SongListAdapter(eventTracker: EventTracker? = null) : TrackListAdapter() {

    init {
        register(ImageTextPresenter(eventTracker))
    }

    override fun submitTracks(items: List<TrackModel>) {
        submitList(items.map { ImageTextModel("${it.title} - ${it.id}") })
    }
}
