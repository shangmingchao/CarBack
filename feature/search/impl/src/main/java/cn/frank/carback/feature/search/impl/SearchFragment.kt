package cn.frank.carback.feature.search.impl

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import cn.frank.carback.feature.home.api.HomeApi
import cn.frank.carback.feature.home.api.TrackListAdapter
import cn.frank.carback.feature.search.api.SearchApi
import cn.frank.carback.feature.search.impl.databinding.FragmentSearchBinding
import cn.frank.carback.page.BaseFragment
import cn.frank.carback.route.Route
import cn.frank.carback.service.ServiceRegistry
import dagger.hilt.android.AndroidEntryPoint

/**
 * 搜索页面
 *
 * - 通过 [Router] 路由挂载：@Route("search/main")
 * - View 级契约示例：只依赖 :feature:home:api，经 [ServiceRegistry] 拿到 home
 *   实现的 TrackListAdapter 直接上屏——条目长什么样、怎么 diff、点击干什么，
 *   全部由 home 决定，搜索页零列表 UI 代码；
 * - 点击条目即播放：HomeApiImpl.play() 内部回写 mine 收听历史并发送事件。
 *
 * @author shangmingchao
 */
@Route("search/main")
@AndroidEntryPoint
class SearchFragment : BaseFragment<FragmentSearchBinding>() {

    private val searchApi: SearchApi by lazy {
        ServiceRegistry.service<SearchApi>()
    }

    /**
     * home 未装配时为 null（宿主可不装 home，优雅降级为空态）
     */
    private val homeApi: HomeApi? by lazy {
        ServiceRegistry.serviceOrNull<HomeApi>()
    }

    private var trackAdapter: TrackListAdapter<*>? = null
    private var keyword: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        trackAdapter = homeApi?.createTrackAdapter()
        if (trackAdapter != null) {
            binding.emptyHint.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
            binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerView.adapter = trackAdapter
        } else {
            binding.emptyHint.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        }
        binding.etKeyword.doAfterTextChanged { text ->
            keyword = text?.toString().orEmpty()
        }
        binding.btnSearch.setOnClickListener {
            search(keyword)
        }
        binding.etKeyword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search(keyword)
                true
            } else {
                false
            }
        }
        search(keyword)
    }

    private fun search(keyword: String) {
        val results = searchApi.search(keyword)
        trackAdapter?.submitList(results)
        Toast.makeText(requireContext(), "已搜索：$keyword（${results.size} 条）", Toast.LENGTH_SHORT).show()
    }
}
