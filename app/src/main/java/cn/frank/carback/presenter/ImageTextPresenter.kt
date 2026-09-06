package cn.frank.carback.presenter

import android.view.LayoutInflater
import android.view.ViewGroup
import cn.frank.carback.databinding.ItemImageTextBinding
import cn.frank.carback.model.home.ImageTextModel
import cn.frank.carback.recyclerview.ItemPresenter

/**
 *
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
    }
}
