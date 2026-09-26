package cn.frank.carback.detail.list

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import cn.frank.carback.model.home.ImageTextModel
import cn.frank.carback.page.CommonListViewModel
import cn.frank.carback.recyclerview.ItemModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.random.Random

/**
 *
 *
 * @author shangmingchao
 */
@HiltViewModel
class DetailListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : CommonListViewModel() {

    private val menuId: String = savedStateHandle["id"] ?: ""

    override fun getListFlow(pageNo: Int): Flow<List<ItemModel>> =
        mockData(menuId, pageNo)
            .map { list ->
                list.map { ImageTextModel(it) }
            }.flowOn(Dispatchers.IO)

    private fun mockData(id: String, pageNo: Int): Flow<List<String>> = flow {
        delay(3000)
        val data = (1..20).map { "$id-$pageNo-$it-${Random.nextInt(1000000000)}" }
        Log.d("aaaa", "emit: $data")
        emit(data)
    }
}
