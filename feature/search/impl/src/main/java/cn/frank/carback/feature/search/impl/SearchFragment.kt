package cn.frank.carback.feature.search.impl

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.frank.carback.feature.home.api.HomeApi
import cn.frank.carback.feature.home.api.TrackListAdapter
import cn.frank.carback.feature.search.api.SearchApi
import cn.frank.carback.feature.search.impl.databinding.FragmentSearchBinding
import cn.frank.carback.page.BaseFragment
import cn.frank.carback.route.Route
import cn.frank.carback.route.Router
import cn.frank.carback.service.ServiceRegistry
import dagger.hilt.android.AndroidEntryPoint

/**
 * 搜索页面
 *
 * @author shangmingchao
 */
@Route("search/main")
@AndroidEntryPoint
class SearchFragment : BaseFragment<FragmentSearchBinding>() {

    private val searchApi: SearchApi by lazy {
        ServiceRegistry.service<SearchApi>()
    }

    private val homeApi: HomeApi? by lazy {
        ServiceRegistry.serviceOrNull<HomeApi>()
    }

    private var trackAdapter: TrackListAdapter? = null
    private var keyword: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        trackAdapter = homeApi?.createTrackAdapter()
        binding.titleBar.navBackAction = {
            Router.back()
        }
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
        trackAdapter?.submitTracks(results)
    }
}
