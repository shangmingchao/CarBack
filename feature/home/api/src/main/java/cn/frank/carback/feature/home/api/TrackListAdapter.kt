package cn.frank.carback.feature.home.api

import androidx.recyclerview.widget.RecyclerView

/**
 * 曲目列表 Adapter 抽象契约
 *
 * 消费方（如 :feature:search）只依赖本抽象：
 * 拿到 Adapter 直接 set 到 RecyclerView 上屏，零列表 UI 代码；
 * 条目布局、DiffUtil、点击行为全部由 :feature:home:impl 决定。
 *
 * @author shangmingchao
 */
abstract class TrackListAdapter<VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {

    /**
     * 提交一批曲目数据（含 diff 刷新契约）
     */
    abstract fun submitList(items: List<TrackUiItem>)
}
