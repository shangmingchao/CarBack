package cn.frank.carback.feature.home.impl.service

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cn.frank.carback.feature.home.api.TrackListAdapter
import cn.frank.carback.feature.home.api.TrackUiItem
import cn.frank.carback.feature.home.impl.databinding.ItemImageTextBinding

/**
 * [TrackListAdapter] 契约的 home 实现：
 * 条目布局（item_image_text）、DiffUtil、点击行为全部由 home 决定，
 * 消费方（search 等）零列表 UI 代码。
 *
 * @author shangmingchao
 */
class HomeTrackAdapter(
    private val onItemClick: (TrackUiItem) -> Unit,
) : TrackListAdapter<HomeTrackAdapter.TrackViewHolder>() {

    private val differ = AsyncListDiffer(
        this,
        object : DiffUtil.ItemCallback<TrackUiItem>() {
            override fun areItemsTheSame(oldItem: TrackUiItem, newItem: TrackUiItem): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: TrackUiItem, newItem: TrackUiItem): Boolean =
                oldItem == newItem
        },
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val binding = ItemImageTextBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TrackViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val item = differ.currentList[position]
        holder.binding.tvTitle.text = item.title
        holder.binding.ivImage.tag = item.id
        holder.binding.root.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = differ.currentList.size

    override fun submitList(items: List<TrackUiItem>) {
        differ.submitList(items)
    }

    class TrackViewHolder(
        val binding: ItemImageTextBinding,
    ) : RecyclerView.ViewHolder(binding.root)
}
