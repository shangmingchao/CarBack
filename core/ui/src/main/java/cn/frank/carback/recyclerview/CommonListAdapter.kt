package cn.frank.carback.recyclerview

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * 通用列表适配器
 *
 * 泛型 M 为列表项基类型，需实现 [ItemModel]，支持按 M 的子类型注册 Presenter 并提交子类型列表
 *
 * 数据提交支持两种模式：
 *  - 未传入 [diffCallback]（默认）：全量刷新，与历史行为一致，适配"每次刷新都传入全新实例"的场景；
 *  - 传入 [DiffUtil.ItemCallback]：内部使用 [AsyncListDiffer] 在后台线程计算差异并增量刷新，
 *    需要精确局部更新（如播放状态、选中态、点赞等）时建议传入，避免整表重绑。
 *
 * @author shangmingchao
 */
open class CommonListAdapter<M : ItemModel>(
    diffCallback: DiffUtil.ItemCallback<M>? = null,
) : RecyclerView.Adapter<CommonListAdapter.ViewHolder>() {

    /**
     * 异步差异计算器，仅在传入 [diffCallback] 时创建
     */
    private val differ: AsyncListDiffer<M>? = diffCallback?.let {
        AsyncListDiffer(this, it)
    }

    /**
     * 未启用差异计算时的同步数据源
     */
    private val syncItems = mutableListOf<M>()

    private val presenterList = mutableListOf<ItemPresenter<M, ViewBinding>>()
    private val classToViewType = mutableMapOf<Class<*>, Int>()

    /**
     * 当前数据列表
     */
    private val items: List<M>
        get() = differ?.currentList ?: syncItems

    /**
     * 注册一个 Model 子类型对应的 Presenter
     *
     * @param modelClass Model 子类型的 Class 对象
     * @param presenter 对应的 Presenter 实例
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : M> register(modelClass: Class<T>, presenter: ItemPresenter<T, *>) {
        val viewType = presenterList.size
        classToViewType[modelClass] = viewType
        presenterList.add(presenter as ItemPresenter<M, ViewBinding>)
    }

    /**
     * 便捷的内联注册方法，自动推导 Class
     */
    inline fun <reified T : M> register(presenter: ItemPresenter<T, *>) {
        register(T::class.java, presenter)
    }

    /**
     * 以原始类型注册，供外部注册表 / 工厂批量登记使用，泛型擦除在此收敛
     *
     * @param modelClass Model 子类型的 Class 对象
     * @param presenter 对应的 Presenter 实例
     */
    @Suppress("UNCHECKED_CAST")
    fun registerRaw(modelClass: Class<*>, presenter: ItemPresenter<out M, out ViewBinding>) {
        register(modelClass as Class<M>, presenter as ItemPresenter<M, *>)
    }

    /**
     * 提交新的数据列表，支持 M 的子类型
     */
    fun submitList(newItems: List<M>) {
        submitList(newItems, null)
    }

    /**
     * 提交新的数据列表，并在提交完成后回调
     *
     * 启用差异计算（构造时传入 [DiffUtil.ItemCallback]）时由 [AsyncListDiffer] 在后台线程
     * 计算差异并增量刷新；未启用时退化为全量刷新。
     *
     * @param newItems 新的数据列表
     * @param commitCallback 数据提交完成后的回调，仅在启用差异计算时生效
     */
    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<M>, commitCallback: Runnable?) {
        val differ = differ
        if (differ != null) {
            differ.submitList(newItems, commitCallback)
            return
        }
        syncItems.clear()
        syncItems.addAll(newItems)
        notifyDataSetChanged()
        commitCallback?.run()
    }

    /**
     * 移除指定位置的数据
     */
    fun removeAt(position: Int, commitCallback: Runnable? = null) {
        if (position < 0 || position >= items.size) {
            return
        }
        val differ = differ
        if (differ != null) {
            differ.currentList.toMutableList().also {
                it.removeAt(position)
                differ.submitList(it, commitCallback)
            }
            return
        }
        syncItems.removeAt(position)
        notifyItemRemoved(position)
        commitCallback?.run()
    }

    /**
     * 清空列表
     */
    @SuppressLint("NotifyDataSetChanged")
    fun removeAll(commitCallback: Runnable? = null) {
        val differ = differ
        if (differ != null) {
            differ.currentList.toMutableList().also {
                it.clear()
                differ.submitList(it, commitCallback)
            }
            return
        }
        syncItems.clear()
        notifyDataSetChanged()
        commitCallback?.run()
    }

    /**
     * 获取数据
     */
    fun getDataList(): List<M> = items

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

    /**
     * 对 [recyclerView] 当前仍持有的 ViewHolder 逐个触发 bind
     *
     * 数据未变化但资源曾因宿主回收被解绑时（参见 [unbindAttached]），仅恢复当前
     * 附加的可见项监听等资源，不提交列表、不触发整表刷新，避免滑动卡顿。
     */
    fun rebindAttached(recyclerView: RecyclerView) {
        for (i in 0 until recyclerView.childCount) {
            val holder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i))
            if (holder !is ViewHolder) continue
            val position = holder.bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION || position >= items.size) continue
            // 仅当数据类型与当前占位类型一致时才恢复，避免脏数据错配
            if (getItemViewType(position) != holder.itemViewType) continue
            presenterList.getOrNull(holder.itemViewType)?.bind(items[position], holder.binding, position)
        }
    }

    class ViewHolder(val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root)
}
