package cn.frank.carback.recyclerview.decoration

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.frank.carback.ext.dp
import kotlin.math.roundToInt

/**
 * 通用 RecyclerView 间距 ItemDecoration
 *
 * 间距统一加在 item 之后（垂直列表为下方，水平列表为右方），首项不额外加间距。
 *
 * @param spacing item 之间的间距，默认 12dp
 * @param includeLast 最后一个 item 是否也添加尾部间距，默认 false。
 * 分页加载场景建议传 true：RecyclerView 会缓存已 attach 子 View 的 decoration 偏移，
 * 追加下一页（notifyItemRangeInserted）时不会为旧 View 重算偏移，若最后一个 item 的
 * 尾部间距为 0，加载下一页后它依然是 0，导致两页之间没有间距；传 true 后偏移不再依赖
 * itemCount，列表底部也会保留间距。
 *
 * @author shangmingchao
 */
class CommonSpacingDecoration(
    private val spacing: Int = 12.dp,
    private val includeLast: Boolean = false,
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) {
            outRect.set(0, 0, 0, 0)
            return
        }

        when (val lm = parent.layoutManager) {
            is GridLayoutManager -> applyGridOffsets(outRect, lm, position, state.itemCount)
            is LinearLayoutManager -> applyLinearOffsets(outRect, lm, position, state.itemCount)
            else -> outRect.set(0, 0, 0, 0)
        }
    }

    private fun applyGridOffsets(
        outRect: Rect,
        glm: GridLayoutManager,
        position: Int,
        itemCount: Int
    ) {
        val spanCount = glm.spanCount
        val lookup = glm.spanSizeLookup
        val spanSize = lookup.getSpanSize(position)
        val spanIndex = lookup.getSpanIndex(position, spanCount)
        val spanGroupIndex = lookup.getSpanGroupIndex(position, spanCount)

        // 是否是最后一个 span group（垂直网格即最后一行，水平网格即最后一列）
        val lastSpanGroupIndex = lastSpanGroupIndex(lookup, spanCount, itemCount, spanGroupIndex)
        val isLastGroup = spanGroupIndex >= lastSpanGroupIndex

        // 用统一的网格线函数计算左右（或上下）偏移：
        //   startPad = f(startSpan)
        //   endPad   = g - f(endSpan)
        // 这样相邻间距恒等于 g，且所有单元格内容尺寸一致。
        val startSpan = spanIndex
        val endSpan = spanIndex + spanSize
        val startPad = gridLineOffset(startSpan, spacing, spanCount)
        val endPad = spacing - gridLineOffset(endSpan, spacing, spanCount)

        // 滚动方向上的尾部间距，includeLast 时与是否为最后一组无关，
        // 保证偏移不随 itemCount 变化，避免追加数据后旧 View 仍沿用缓存偏移
        val alongSpacing = if (includeLast || !isLastGroup) spacing else 0

        if (glm.orientation == GridLayoutManager.VERTICAL) {
            outRect.left = startPad
            outRect.right = endPad
            outRect.top = 0
            outRect.bottom = alongSpacing
        } else {
            outRect.top = startPad
            outRect.bottom = endPad
            outRect.left = 0
            outRect.right = alongSpacing
        }
    }

    /** 最后一组的缓存入参，避免同一 layout 过程中对每个子 View 都重算一次 */
    private var cacheLookup: GridLayoutManager.SpanSizeLookup? = null
    private var cacheSpanCount = -1
    private var cacheItemCount = -1
    private var cacheLastSpanGroupIndex = 0

    /**
     * 最后一组（itemCount - 1 所在组）的索引。
     *
     * [GridLayoutManager.SpanSizeLookup.getSpanGroupIndex] 默认实现会线性扫描到目标 position，
     * 逐个子 View 计算会退化成 O(itemCount × childCount)；这里按 (lookup, spanCount, itemCount)
     * 缓存结果，一次 layout 过程内只计算一次。
     */
    private fun lastSpanGroupIndex(
        lookup: GridLayoutManager.SpanSizeLookup,
        spanCount: Int,
        itemCount: Int,
        fallback: Int
    ): Int {
        if (itemCount <= 0) {
            return fallback
        }
        if (cacheLookup === lookup && cacheSpanCount == spanCount && cacheItemCount == itemCount) {
            return cacheLastSpanGroupIndex
        }
        val value = lookup.getSpanGroupIndex(itemCount - 1, spanCount)
        cacheLookup = lookup
        cacheSpanCount = spanCount
        cacheItemCount = itemCount
        cacheLastSpanGroupIndex = value
        return value
    }

    /**
     * 第 [span] 条网格线到容器边缘的偏移，等价于 round(span * spacing / spanCount)。
     * 保证 f(0) = 0、f(spanCount) = spacing。
     */
    private fun gridLineOffset(span: Int, spacing: Int, spanCount: Int): Int =
        (span * spacing.toFloat() / spanCount).roundToInt()

    private fun applyLinearOffsets(
        outRect: Rect,
        llm: LinearLayoutManager,
        position: Int,
        itemCount: Int
    ) {
        val isLast = position == itemCount - 1
        // 滚动方向上的尾部间距，includeLast 时与是否为最后一项无关，
        // 保证偏移不随 itemCount 变化，避免追加数据后旧 View 仍沿用缓存偏移
        val alongSpacing = if (includeLast || !isLast) spacing else 0
        if (llm.orientation == LinearLayoutManager.VERTICAL) {
            if (llm.reverseLayout) {
                outRect.set(0, alongSpacing, 0, 0)
            } else {
                outRect.set(0, 0, 0, alongSpacing)
            }
        } else {
            if (llm.reverseLayout) {
                outRect.set(alongSpacing, 0, 0, 0)
            } else {
                outRect.set(0, 0, alongSpacing, 0)
            }
        }
    }
}
