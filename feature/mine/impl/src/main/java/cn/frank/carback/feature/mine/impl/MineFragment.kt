package cn.frank.carback.feature.mine.impl

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import cn.frank.carback.feature.mine.api.MineApi
import cn.frank.carback.feature.mine.impl.databinding.FragmentMineBinding
import cn.frank.carback.feature.mine.impl.databinding.ItemTrackBinding
import cn.frank.carback.feature.mine.impl.model.HistoryItemModel
import cn.frank.carback.feature.mine.impl.presenter.HistoryItemPresenter
import cn.frank.carback.feature.mine.impl.viewmodel.MineViewModel
import cn.frank.carback.page.BaseFragment
import cn.frank.carback.recyclerview.CommonListAdapter
import cn.frank.carback.recyclerview.ItemModel
import cn.frank.carback.recyclerview.ItemPresenter
import cn.frank.carback.route.Route
import cn.frank.carback.route.Router
import cn.frank.carback.service.ServiceRegistry
import cn.frank.carback.shared.model.TrackModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * 我的页面
 *
 * @author shangmingchao
 */
@Route("mine/main")
@AndroidEntryPoint
class MineFragment : BaseFragment<FragmentMineBinding>() {

    private val viewModel by viewModels<MineViewModel>()

    private val adapter = CommonListAdapter<ItemModel>().apply {
        register(HistoryItemPresenter())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.ivClear.setOnClickListener {
            viewModel.clearHistory()
        }
        startCollect(
            {
                viewModel.history.collect {
                    adapter.submitList(it)
                }
            },
        )
    }
}
