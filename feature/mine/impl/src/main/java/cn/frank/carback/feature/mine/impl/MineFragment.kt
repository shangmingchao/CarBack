package cn.frank.carback.feature.mine.impl

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import cn.frank.carback.feature.mine.api.MineApi
import cn.frank.carback.feature.mine.api.TrackItem
import cn.frank.carback.feature.mine.impl.databinding.FragmentMineBinding
import cn.frank.carback.feature.mine.impl.databinding.ItemTrackBinding
import cn.frank.carback.page.BaseFragment
import cn.frank.carback.recyclerview.CommonListAdapter
import cn.frank.carback.recyclerview.ItemModel
import cn.frank.carback.recyclerview.ItemPresenter
import cn.frank.carback.route.Route
import cn.frank.carback.route.Router
import cn.frank.carback.service.ServiceRegistry
import dagger.hilt.android.AndroidEntryPoint

/**
 * 列表行包装：[TrackItem] 为 api 契约模型（不依赖 UI 层），展示时包装为 [ItemModel]
 *
 * @author shangmingchao
 */
data class TrackRow(
    val track: TrackItem,
) : ItemModel

/**
 * 我的页面
 *
 * - 通过 [Router] 路由挂载：@Route("mine/main")
 * - 对外提供 [MineApi]（收听历史数据），首页/搜索页播放时回写，
 *   本页经 StateFlow 实时刷新
 *
 * @author shangmingchao
 */
@Route("mine/main")
@AndroidEntryPoint
class MineFragment : BaseFragment<FragmentMineBinding>() {

    private val viewModel by viewModels<MineViewModel>()

    private val adapter = CommonListAdapter<TrackRow>().apply {
        register(TrackItemPresenter())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.btnSearch.setOnClickListener {
            Router.open("search/main")
        }
        binding.btnClear.setOnClickListener {
            viewModel.clearHistory()
        }
        startCollect(
            {
                // 收听历史（StateFlow 响应式，播放回写后自动刷新）
                viewModel.history.collect { tracks ->
                    adapter.submitList(tracks.map { track -> TrackRow(track) })
                }
            },
        )
    }
}

/**
 * 我的页面 ViewModel：收听历史数据经 [ServiceRegistry] 按契约解析
 *
 * @author shangmingchao
 */
class MineViewModel : ViewModel() {

    private val mineApi = ServiceRegistry.service<MineApi>()

    val history = mineApi.listenHistory

    fun clearHistory() {
        mineApi.clearHistory()
    }
}

/**
 * 收听历史条目 Presenter
 *
 * @author shangmingchao
 */
class TrackItemPresenter : ItemPresenter<TrackRow, ItemTrackBinding> {

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        attachToParent: Boolean,
    ): ItemTrackBinding = ItemTrackBinding.inflate(inflater, parent, attachToParent)

    override fun bind(model: TrackRow, binding: ItemTrackBinding, position: Int) {
        binding.tvTitle.text = model.track.title
        binding.tvArtist.text = model.track.artist
    }
}
