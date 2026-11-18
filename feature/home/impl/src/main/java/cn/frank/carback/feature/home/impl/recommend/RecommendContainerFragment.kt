package cn.frank.carback.feature.home.impl.recommend

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import cn.frank.carback.feature.home.impl.R
import cn.frank.carback.feature.home.impl.adapter.RecommendPagerAdapter
import cn.frank.carback.feature.home.impl.databinding.FragmentRecommendContainerBinding
import cn.frank.carback.feature.home.impl.recommend.viewmodel.RecommendContainerViewModel
import cn.frank.carback.feature.mine.api.MineApi
import cn.frank.carback.page.BaseFragment
import cn.frank.carback.route.Route
import cn.frank.carback.service.ServiceRegistry
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue

/**
 * 推荐容器 Fragment
 *
 * @author shangmingchao
 */
@AndroidEntryPoint
@Route("home/recommend")
class RecommendContainerFragment : BaseFragment<FragmentRecommendContainerBinding>() {

    private val viewModel by viewModels<RecommendContainerViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tabs = viewModel.tabs
        binding.viewPager.adapter = RecommendPagerAdapter(this, tabs)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabs[position].name
        }.attach()
        startCollect(
            {
                ServiceRegistry.serviceOrNull<MineApi>()?.listenHistory?.collect { tracks ->
                    if (tracks.isEmpty()) {
                        binding.historyText.visibility = View.GONE
                    } else {
                        binding.historyText.visibility = View.VISIBLE
                        binding.historyText.text = getString(
                            R.string.home_recent_listen,
                            tracks.take(3).joinToString("、") { it.title },
                        )
                    }
                }
            },
        )
    }
}
