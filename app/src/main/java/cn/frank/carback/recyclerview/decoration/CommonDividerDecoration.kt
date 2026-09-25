package cn.frank.carback.recyclerview.decoration

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.frank.carback.utils.dp
import kotlin.math.roundToInt

/**
 * 通用 RecyclerView 分割线 ItemDecoration
 *
 * - 支持 LinearLayoutManager 和 GridLayoutManager item 之间的分割线
 * - 支持分割线颜色配置
 * - 支持分割线尺寸
 * - 支持分割线与 item 之间的间距配置，默认 12dp
 * - 支持分割线左右/上下的间距配置，默认 0dp
 *
 * @param color 分割线颜色
 * @param size 分割线尺寸，垂直列表为线高、水平列表为线宽
 * @param spacing 分割线与相邻 item 之间的间距，垂直列表为水平分割线距上/下 item 的间距，
 * 水平列表为垂直分割线距左/右 item 的间距，分割线始终居中于该间距
 * @param insetStart 分割线起始端的间距，垂直列表为左边、水平列表为上边
 * @param insetEnd 分割线结束端的间距，垂直列表为右边、水平列表为下边
 * @param includeLast 最后的 item 是否添加分割线，默认不添加
 *
 * @author shangmingchao
 */
class CommonDividerDecoration(
    private val color: Int = 0x14FFFFFF,
    private val size: Int = 0.25f.dp,
    private val spacing: Int = 10.dp,
    private val insetStart: Int = 0,
    private val insetEnd: Int = 0,
    private val includeLast: Boolean = false,
) : RecyclerView.ItemDecoration() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = this@CommonDividerDecoration.color
        style = Paint.Style.FILL
    }

    /** item 的装饰边界（含分割线占位与 margin），复用避免频繁创建对象 */
    private val bounds = Rect()

    /** 分割线矩形，复用避免频繁创建对象 */
    private val lineRect = Rect()

    /** 分割线在滚动方向上占据的空间：两侧 spacing + 分割线自身尺寸 */
    private val gap: Int = spacing * 2 + size

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (size <= 0 || position == RecyclerView.NO_POSITION) {
            outRect.set(0, 0, 0, 0)
            return
        }
        when (val lm = parent.layoutManager) {
            is GridLayoutManager -> applyGridOffsets(outRect, lm, position, state.itemCount)
            is LinearLayoutManager -> applyLinearOffsets(outRect, lm, position, state.itemCount)
            else -> outRect.set(0, 0, 0, 0)
        }
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (size <= 0) return
        when (val lm = parent.layoutManager) {
            is GridLayoutManager -> drawGrid(canvas, parent, lm, state.itemCount)
            is LinearLayoutManager -> drawLinear(canvas, parent, lm, state.itemCount)
            else -> Unit
        }
    }

    private fun applyLinearOffsets(
        outRect: Rect,
        llm: LinearLayoutManager,
        position: Int,
        itemCount: Int
    ) {
        outRect.set(0, 0, 0, 0)
        if (!hasDividerAfterItem(position, itemCount)) return
        if (llm.orientation == LinearLayoutManager.VERTICAL) {
            if (llm.reverseLayout) outRect.top = gap else outRect.bottom = gap
        } else {
            if (llm.reverseLayout) outRect.left = gap else outRect.right = gap
        }
    }

    private fun applyGridOffsets(
        outRect: Rect,
        glm: GridLayoutManager,
        position: Int,
        itemCount: Int
    ) {
        outRect.set(0, 0, 0, 0)
        val spanCount = glm.spanCount
        val lookup = glm.spanSizeLookup
        val spanIndex = lookup.getSpanIndex(position, spanCount)
        val spanSize = lookup.getSpanSize(position)
        val groupIndex = lookup.getSpanGroupIndex(position, spanCount)
        val lastGroupIndex = if (itemCount > 0) {
            lookup.getSpanGroupIndex(itemCount - 1, spanCount)
        } else {
            groupIndex
        }
        val along = hasDividerAfterGroup(groupIndex, lastGroupIndex)
        val cross = spanIndex + spanSize < spanCount
        if (glm.orientation == GridLayoutManager.VERTICAL) {
            if (along) {
                if (glm.reverseLayout) outRect.top = gap else outRect.bottom = gap
            }
            if (cross) outRect.right = gap
        } else {
            if (along) {
                if (glm.reverseLayout) outRect.left = gap else outRect.right = gap
            }
            if (cross) outRect.bottom = gap
        }
    }

    private fun drawLinear(
        canvas: Canvas,
        parent: RecyclerView,
        llm: LinearLayoutManager,
        itemCount: Int
    ) {
        val vertical = llm.orientation == LinearLayoutManager.VERTICAL
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(child)
            if (position == RecyclerView.NO_POSITION) continue
            if (!hasDividerAfterItem(position, itemCount)) continue
            parent.getDecoratedBoundsWithMargins(child, bounds)
            drawAlongLine(
                canvas = canvas,
                parent = parent,
                vertical = vertical,
                reverse = llm.reverseLayout,
                anchor = alongAnchor(child, vertical, llm.reverseLayout)
            )
        }
    }

    private fun drawGrid(
        canvas: Canvas,
        parent: RecyclerView,
        glm: GridLayoutManager,
        itemCount: Int
    ) {
        val vertical = glm.orientation == GridLayoutManager.VERTICAL
        val spanCount = glm.spanCount
        val lookup = glm.spanSizeLookup
        val lastGroupIndex = if (itemCount > 0) {
            lookup.getSpanGroupIndex(itemCount - 1, spanCount)
        } else {
            -1
        }
        // 沿滚动方向的分割线按 group 聚合后再绘制，避免同一行/列重复绘制导致颜色叠加
        val alongAnchors = HashMap<Int, Int>()
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(child)
            if (position == RecyclerView.NO_POSITION) continue
            parent.getDecoratedBoundsWithMargins(child, bounds)

            val spanIndex = lookup.getSpanIndex(position, spanCount)
            val spanSize = lookup.getSpanSize(position)
            val groupIndex = lookup.getSpanGroupIndex(position, spanCount)

            if (hasDividerAfterGroup(groupIndex, lastGroupIndex)) {
                val offset = alongAnchor(child, vertical, glm.reverseLayout)
                val anchor = alongAnchors[groupIndex]
                alongAnchors[groupIndex] = if (glm.reverseLayout) {
                    if (anchor == null) offset else minOf(anchor, offset)
                } else {
                    if (anchor == null) offset else maxOf(anchor, offset)
                }
            }

            // 交叉方向的分割线，每个 item 最多绘制一次，不会出现叠加
            if (spanIndex + spanSize < spanCount) {
                drawCrossLine(canvas, parent, child, vertical)
            }
        }

        for (anchor in alongAnchors.values) {
            drawAlongLine(
                canvas = canvas,
                parent = parent,
                vertical = vertical,
                reverse = glm.reverseLayout,
                anchor = anchor
            )
        }
    }

    /**
     * 沿滚动方向的分割线锚点坐标：垂直布局取 item 的上/下边界，水平布局取 item 的左/右边界
     */
    private fun alongAnchor(child: View, vertical: Boolean, reverse: Boolean): Int = if (vertical) {
        val ty = child.translationY.roundToInt()
        if (reverse) bounds.top + ty else bounds.bottom + ty
    } else {
        val tx = child.translationX.roundToInt()
        if (reverse) bounds.left + tx else bounds.right + tx
    }

    /**
     * 绘制垂直于滚动方向的分割线（垂直列表的横线、水平列表的竖线），横跨整个 RecyclerView，
     * 分割线距上一个 item 与下一个 item 均为 [spacing]
     *
     * @param anchor 分割线所在间距的外侧边界坐标
     */
    private fun drawAlongLine(
        canvas: Canvas,
        parent: RecyclerView,
        vertical: Boolean,
        reverse: Boolean,
        anchor: Int
    ) {
        if (vertical) {
            // reverse 时分割线位于 item 上方，否则位于 item 下方
            val top = if (reverse) anchor + spacing else anchor - spacing - size
            val (left, right) = insetRange(
                parent = parent,
                start = parent.paddingLeft,
                end = parent.width - parent.paddingRight,
                verticalLine = false
            )
            lineRect.set(left, top, right, top + size)
        } else {
            val left = if (reverse) anchor + spacing else anchor - spacing - size
            val (top, bottom) = insetRange(
                parent = parent,
                start = parent.paddingTop,
                end = parent.height - parent.paddingBottom,
                verticalLine = true
            )
            lineRect.set(left, top, left + size, bottom)
        }
        if (lineRect.width() <= 0 || lineRect.height() <= 0) return
        canvas.drawRect(lineRect, paint)
    }

    /**
     * 绘制平行于滚动方向的分割线（垂直网格的列间竖线、水平网格的行间横线），长度跟随当前 item，
     * 分割线距相邻两个 item 均为 [spacing]
     */
    private fun drawCrossLine(canvas: Canvas, parent: RecyclerView, child: View, vertical: Boolean) {
        val tx = child.translationX.roundToInt()
        val ty = child.translationY.roundToInt()
        if (vertical) {
            val right = bounds.right + tx - spacing
            val (top, bottom) = insetRange(
                parent = parent,
                start = bounds.top + ty,
                end = bounds.bottom + ty,
                verticalLine = true
            )
            lineRect.set(right - size, top, right, bottom)
        } else {
            val bottom = bounds.bottom + ty - spacing
            val (left, right) = insetRange(
                parent = parent,
                start = bounds.left + tx,
                end = bounds.right + tx,
                verticalLine = false
            )
            lineRect.set(left, bottom - size, right, bottom)
        }
        if (lineRect.width() <= 0 || lineRect.height() <= 0) return
        canvas.drawRect(lineRect, paint)
    }

    /**
     * 在分割线的延伸方向上叠加 [insetStart]/[insetEnd]，返回最终的起止坐标。
     *
     * @param verticalLine true 表示竖向分割线（取上下范围），false 表示横向分割线（取左右范围）
     */
    private fun insetRange(
        parent: RecyclerView,
        start: Int,
        end: Int,
        verticalLine: Boolean
    ): Pair<Int, Int> {
        return if (!verticalLine && parent.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
            (start + insetEnd) to (end - insetStart)
        } else {
            (start + insetStart) to (end - insetEnd)
        }
    }

    private fun hasDividerAfterItem(position: Int, itemCount: Int): Boolean =
        position in 0 until (itemCount - 1 + (if (includeLast) 1 else 0))

    private fun hasDividerAfterGroup(groupIndex: Int, lastGroupIndex: Int): Boolean =
        groupIndex <= lastGroupIndex - 1 + (if (includeLast) 1 else 0)
}
