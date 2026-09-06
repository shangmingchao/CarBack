package cn.frank.carback.viewmodel

import androidx.lifecycle.SavedStateHandle
import cn.frank.carback.model.home.ImageTextModel
import cn.frank.carback.page.CommonListViewModel
import cn.frank.carback.recyclerview.ItemModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 *
 *
 * @author shangmingchao
 */
@HiltViewModel
class RecommendViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : CommonListViewModel() {

    private val menuId: String = savedStateHandle["id"] ?: ""

    override fun getListFlow(pageNo: Int): Flow<List<ItemModel>> =
        flowOf((1..20).map { "$menuId-$it" })
            .map { list ->
                list.map { ImageTextModel(it) }
            }
}
