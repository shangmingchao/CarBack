package cn.frank.carback.recyclerview

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * 通用列表适配器
 *
 * 泛型 M 为列表项基类型，需实现 [ItemModel]，支持按 M 的子类型注册 Presenter 并提交子类型列表
 *
 * @author shangmingchao
 */
open class CommonListAdapter<M : ItemModel> : RecyclerView.Adapter<CommonListAdapter.ViewHolder>() {

    private val items = mutableListOf<M>()
    private val presenterList = mutableListOf<ItemPresenter<M, ViewBinding>>()
    private val classToViewType = mutableMapOf<Class<*>, Int>()

    /**
     * 注册一个 Model 子类型对应的 Presenter
     *
     * @param modelClass Model 子类型的 Class 对象
     * @param presenter 对应的 Presenter 实例
     */
    fun <T : M> register(modelClass: Class<T>, presenter: ItemPresenter<T, *>) {
        val viewType = presenterList.size
        classToViewType[modelClass] = viewType
        @Suppress("UNCHECKED_CAST")
        presenterList.add(presenter as ItemPresenter<M, ViewBinding>)
    }

    /**
     * 便捷的内联注册方法，自动推导 Class
     */
    inline fun <reified T : M> register(presenter: ItemPresenter<T, *>) {
        register(T::class.java, presenter)
    }

    /**
     * 提交新的数据列表，支持 M 的子类型
     */
    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<M>) {
        items.clear()
        newItems.forEach { items.add(it) }
        notifyDataSetChanged()
    }

    /**
     * 获取数据
     */
    fun getDataList() = items

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        val item = items[position]
        return classToViewType[item::class.java]
            ?: throw IllegalArgumentException(
                "No presenter registered for ${item::class.java.simpleName}"
            )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val presenter = presenterList[viewType]
        val binding = presenter.createBinding(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        val item = items[position]
        val presenter = presenterList[getItemViewType(position)]
        if (payloads.isEmpty()) {
            presenter.bind(item, holder.binding, position)
        } else {
            presenter.onPayload(item, holder.binding, position, payloads)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        onBindViewHolder(holder, position, emptyList())
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        presenterList[holder.itemViewType].unbind(holder.binding)
    }

    /**
     * 对 [recyclerView] 当前仍持有的 ViewHolder 逐个触发 unbind
     *
     * 嵌套列表场景下，宿主被回收时内层列表的 item 不会被自动回收，
     * 需要在宿主 unbind 时主动调用一次，避免其继续持有图片 / 监听等资源。
     */
    fun unbindAttached(recyclerView: RecyclerView) {
        for (i in 0 until recyclerView.childCount) {
            val holder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i))
            if (holder is ViewHolder) {
                presenterList.getOrNull(holder.itemViewType)?.unbind(holder.binding)
            }
        }
    }

    class ViewHolder(val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root)
}
