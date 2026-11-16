package cn.frank.carback.recyclerview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding

/**
 * 简单适配器，用于简单单类型数据列表，只需要匿名类和绑定逻辑
 *
 * @author shangmingchao
 */
class SimpleAdapter<T, VB : ViewBinding>(
    presenter: ItemPresenter<SimpleItemModel<T>, VB>
) : CommonListAdapter<ItemModel>() {

    init {
        @Suppress("UNCHECKED_CAST")
        register(
            SimpleItemModel::class.java,
            SimpleItemPresenter(presenter) as ItemPresenter<SimpleItemModel<*>, *>
        )
    }

    class SimpleItemModel<T>(val data: T) : ItemModel

    class SimpleItemPresenter<T, VB : ViewBinding>(
        private val presenter: ItemPresenter<SimpleItemModel<T>, VB>
    ) : ItemPresenter<SimpleItemModel<T>, VB> {

        override fun createBinding(
            inflater: LayoutInflater,
            parent: ViewGroup,
            attachToParent: Boolean
        ): VB = presenter.createBinding(inflater, parent, attachToParent)

        override fun bind(model: SimpleItemModel<T>, binding: VB, position: Int) {
            presenter.bind(model, binding, position)
        }

        override fun onPayload(
            model: SimpleItemModel<T>,
            binding: VB,
            position: Int,
            payloads: List<Any>
        ) {
            presenter.onPayload(model, binding, position, payloads)
        }

        override fun unbind(binding: VB) {
            presenter.unbind(binding)
        }
    }

    /**
     * 提交原始数据列表，内部自动包装为 [SimpleItemModel]
     */
    fun submitSimpleList(data: List<T>) {
        super.submitList(data.map { SimpleItemModel(it) })
    }
}
