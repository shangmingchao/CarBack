package cn.frank.carback.feature.home.api

/**
 * 曲目列表条目（View 级契约的展示模型）
 *
 * @author shangmingchao
 */
data class TrackUiItem(
    val id: String,
    val title: String,
    val artist: String,
)
