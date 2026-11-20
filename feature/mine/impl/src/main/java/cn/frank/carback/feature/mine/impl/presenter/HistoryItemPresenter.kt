package cn.frank.carback.feature.mine.impl.presenter

import android.view.LayoutInflater
import android.view.ViewGroup
import cn.frank.carback.feature.mine.impl.databinding.ItemTrackBinding
import cn.frank.carback.feature.mine.impl.model.HistoryItemModel
import cn.frank.carback.recyclerview.ItemPresenter

/**
 *
 *
 * @author shangmingchao
 */
class HistoryItemPresenter : ItemPresenter<HistoryItemModel, ItemTrackBinding> {

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        attachToParent: Boolean,
    ): ItemTrackBinding = ItemTrackBinding.inflate(inflater, parent, attachToParent)

    override fun bind(model: HistoryItemModel, binding: ItemTrackBinding, position: Int) {
        binding.tvTitle.text = model.track.title
        binding.tvArtist.text = model.track.id
    }
}
