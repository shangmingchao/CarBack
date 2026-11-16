package cn.frank.carback.feature.home.impl

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import cn.frank.carback.feature.home.impl.adapter.HomePagerAdapter
import cn.frank.carback.feature.home.impl.databinding.FragmentHomeBinding
import cn.frank.carback.feature.mine.api.MineApi
import cn.frank.carback.page.BaseFragment
import cn.frank.carback.route.Route
import cn.frank.carback.service.ServiceRegistry
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

/**
 * 主页 Fragment
 *
 * - 通过 [Router] 路由挂载：@Route("home/main")
 * - 跨 feature 数据流示例：只依赖 :feature:mine:api，
 *   经 [ServiceRegistry] 订阅收听历史（StateFlow 响应式刷新"最近收听"）；
 *   宿主没装配 mine 时优雅降级（serviceOrNull），历史条隐藏。
 *
 * @author shangmingchao
 */
@Route("home/main")
@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private val viewModel by viewModels<HomeViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tabs = viewModel.tabs
        binding.viewPager.adapter = HomePagerAdapter(this@HomeFragment, tabs)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabs[position].name
        }.attach()
        startCollect(
            {
                // 跨 feature 订阅收听历史：MineApi 由 :feature:mine:impl 注册，本模块只认 mine/api 契约
                ServiceRegistry.serviceOrNull<MineApi>()?.listenHistory?.collect { tracks ->
                    if (tracks.isEmpty()) {
                        binding.historyText.visibility = View.GONE
                    } else {
                        binding.historyText.visibility = View.VISIBLE
                        binding.historyText.text = getString(
                            R.string.home_recent_listen,
                            tracks.take(RECENT_COUNT).joinToString("、") { it.title },
                        )
                    }
                }
            },
        )
    }

    private companion object {
        const val RECENT_COUNT = 3
    }
}
