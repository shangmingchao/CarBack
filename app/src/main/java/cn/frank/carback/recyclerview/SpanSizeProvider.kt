package cn.frank.carback.recyclerview

/**
 * 可占据多列的 Item 数据模型
 *
 * @author shangmingchao
 */
interface SpanSizeProvider {
    /**
     * 占据的列数，小于 1 时按 1 列处理，超过总列数时按总列数处理
     */
    val spanSize: Int
}
