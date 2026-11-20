package cn.frank.carback.feature.home.api

import cn.frank.carback.recyclerview.CommonListAdapter
import cn.frank.carback.recyclerview.ItemModel
import cn.frank.carback.shared.model.TrackModel

/**
 * 曲目列表 Adapter 抽象契约
 *
 * 消费方（如 :feature:search）只依赖本抽象：
 * 拿到 Adapter 直接 set 到 RecyclerView 上屏，零列表 UI 代码；
 * 条目布局、DiffUtil、点击行为全部由 :feature:home:impl 决定。
 *
 * 提交 [TrackModel]（跨模块数据契约）而非 [ItemModel]（UI 层标记），
 * 数据模型到 UI 条目的转换收敛在 home 实现内部。
 *
 * @author shangmingchao
 */
abstract class TrackListAdapter : CommonListAdapter<ItemModel>() {

    /**
     * 提交一批曲目数据
     */
    abstract fun submitTracks(items: List<TrackModel>)
}
