package cn.frank.carback.recommend.presenter

import android.view.LayoutInflater
import android.view.ViewGroup
import cn.frank.carback.databinding.ItemImageTextBinding
import cn.frank.carback.detail.list.DetailListFragment
import cn.frank.carback.model.home.ImageTextModel
import cn.frank.carback.navigation.FragmentRouter
import cn.frank.carback.recyclerview.ItemPresenter

/**
 * ImageTextPresenter for displaying image and text items
 *
 * @author shangmingchao
 */
class ImageTextPresenter : ItemPresenter<ImageTextModel, ItemImageTextBinding> {
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
        binding.tvTitle.text = model.data
        binding.root.setOnClickListener {
            FragmentRouter.push(DetailListFragment.newInstance(model.data))
        }
    }
}
