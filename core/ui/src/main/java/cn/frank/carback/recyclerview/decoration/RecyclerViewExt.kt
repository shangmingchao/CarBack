package cn.frank.carback.recyclerview.decoration

import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView 扩展
 *
 * @author shangmingchao
 */

/**
 * 设置 ItemDecoration
 */
fun RecyclerView.setItemDecoration(decoration: RecyclerView.ItemDecoration) {
    if (itemDecorationCount > 0) {
        removeItemDecorationAt(0)
    }
    addItemDecoration(decoration)
}
