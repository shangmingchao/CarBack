package cn.frank.carback.feature.home.impl.recommend.presenter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import cn.frank.carback.feature.home.impl.databinding.ItemImageTextBinding
import cn.frank.carback.feature.home.impl.detail.list.DetailListFragment
import cn.frank.carback.track.EventTracker
import cn.frank.carback.feature.home.impl.model.home.ImageTextModel
import cn.frank.carback.feature.mine.api.MineApi
import cn.frank.carback.navigation.FragmentRouter
import cn.frank.carback.recyclerview.ItemPresenter
import cn.frank.carback.route.Router
import cn.frank.carback.service.ServiceRegistry
import cn.frank.carback.shared.model.TrackModel

/**
 * ImageTextPresenter for displaying image and text items
 *
 * @author shangmingchao
 */
class ImageTextPresenter(private val eventTracker: EventTracker? = null) : ItemPresenter<ImageTextModel, ItemImageTextBinding> {
    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        attachToParent: Boolean
    ): ItemImageTextBinding {
        return ItemImageTextBinding.inflate(inflater, parent, attachToParent)
    }

    override fun bind(
        model: ImageTextModel,
        binding: ItemImageTextBinding,
        position: Int
    ) {
        eventTracker?.trackExposure(
            view = binding.root,
            data = model,
            parentIndex = 0,
            childIndex = position,
        )
        binding.tvTitle.text = model.data
        binding.root.setOnClickListener {
            ServiceRegistry.serviceOrNull<MineApi>()?.record(TrackModel(model.data.hashCode().toString(), model.data))
            eventTracker?.trackClick(
                view = binding.root,
                data = model,
                parentIndex = 0,
                childIndex = position,
            )
            Router.navigate("detail/list", args = bundleOf("id" to model.data))
        }
    }
}
