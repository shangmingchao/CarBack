package cn.frank.carback.recyclerview.decoration

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * 通用 RecyclerView 间距 ItemDecoration
 *
 * - 默认 RecyclerView 边缘没有间距，只有 item 之间有间距
 * - 默认间距是 12dp
 * - 支持设置 item 之间的间距
 * - 支持 GridLayoutManager 和 LinearLayoutManager
 *
 * @param spacing item 之间的间距，默认 12dp
 * @param includeEdge 是否包含 RecyclerView 边缘间距，默认 false
 *
 * @author shangmingchao
 */
class CommonSpacingDecoration(
    private val spacing: Int = 12,
    private val includeEdge: Boolean = false,
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) {
            return
        }
        val layoutManager = parent.layoutManager
        when (layoutManager) {
            is GridLayoutManager -> {
                val spanCount = layoutManager.spanCount
                val column = position % spanCount
                if (includeEdge) {
                    outRect.left = spacing - column * spacing / spanCount
                    outRect.right = (column + 1) * spacing / spanCount
                    if (position < spanCount) {
                        outRect.top = spacing
                    }
                    outRect.bottom = spacing
                } else {
                    outRect.left = column * spacing / spanCount
                    outRect.right = spacing - (column + 1) * spacing / spanCount
                    if (position >= spanCount) {
                        outRect.top = spacing
                    }
                    outRect.bottom = 0
                }
            }
            is LinearLayoutManager -> {
                val isVertical = layoutManager.orientation == RecyclerView.VERTICAL
                val itemCount = parent.adapter?.itemCount ?: 0
                val isFirst = position == 0
                val isLast = position == itemCount - 1
                if (isVertical) {
                    outRect.top = if (isFirst && !includeEdge) 0 else spacing
                    outRect.bottom = if (isLast && includeEdge) spacing else 0
                } else {
                    outRect.left = if (isFirst && !includeEdge) 0 else spacing
                    outRect.right = if (isLast && includeEdge) spacing else 0
                }
            }
        }
    }
}
