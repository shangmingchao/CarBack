package cn.frank.carback.feature.home.impl.recommend.presenter

import android.view.LayoutInflater
import android.view.ViewGroup
import cn.frank.carback.feature.home.impl.databinding.ItemImageTextBinding
import cn.frank.carback.feature.home.impl.detail.list.DetailListFragment
import cn.frank.carback.feature.home.impl.manager.EventTracker
import cn.frank.carback.feature.home.impl.model.home.ImageTextModel
import cn.frank.carback.navigation.FragmentRouter
import cn.frank.carback.recyclerview.ItemPresenter

/**
 * ImageTextPresenter for displaying image and text items
 *
 * @author shangmingchao
 */
class ImageTextPresenter(private val eventTracker: EventTracker) : ItemPresenter<ImageTextModel, ItemImageTextBinding> {
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
        eventTracker.trackExposure(
            view = binding.root,
            data = model,
            parentIndex = 0,
            childIndex = position,
        )
        binding.tvTitle.text = model.data
        binding.root.setOnClickListener {
            eventTracker.trackClick(
                view = binding.root,
                data = model,
                parentIndex = 0,
                childIndex = position,
            )
            FragmentRouter.push(DetailListFragment.newInstance(model.data))
        }
    }
}
