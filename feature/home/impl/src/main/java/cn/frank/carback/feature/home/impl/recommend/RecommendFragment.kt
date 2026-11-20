package cn.frank.carback.feature.home.impl.recommend

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import cn.frank.carback.feature.home.impl.adapter.SongListAdapter
import cn.frank.carback.feature.home.impl.recommend.viewmodel.RecommendViewModel
import cn.frank.carback.page.CommonListFragment
import cn.frank.carback.recyclerview.CommonListAdapter
import cn.frank.carback.recyclerview.ItemModel
import cn.frank.carback.recyclerview.decoration.CommonSpacingDecoration
import cn.frank.carback.recyclerview.decoration.setItemDecoration
import cn.frank.carback.track.ExposureManager
import cn.frank.carback.track.ExposureRecord
import cn.frank.carback.track.ExposureReporter
import dagger.hilt.android.AndroidEntryPoint

/**
 *
 *
 * @author shangmingchao
 */
@AndroidEntryPoint
class RecommendFragment : CommonListFragment() {

    private val exposureManager = ExposureManager(reporter = object : ExposureReporter {
        override fun onExposure(records: List<ExposureRecord>) {
            Log.e("ExposureManager", "onExposure: ${records.size} -> ${records.map { record -> "${record.childIndex}_${record.data}" }}")
        }

        override fun onClick(record: ExposureRecord) {
            Log.e("ExposureManager", "onClick: ${record.childIndex}_${record.data}")
        }
    })

    override val viewModel: RecommendViewModel by viewModels()

    override val adapter: CommonListAdapter<ItemModel> = SongListAdapter(exposureManager)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.setItemDecoration(CommonSpacingDecoration())
        exposureManager.bindLifecycle(safeLifecycleOwner)
    }

    override fun registerPresenter(adapter: CommonListAdapter<ItemModel>) = Unit
}
