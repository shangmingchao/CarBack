package cn.frank.carback

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import cn.frank.carback.adapter.HomePagerAdapter
import cn.frank.carback.databinding.FragmentHomeBinding
import cn.frank.carback.ext.dropSticky
import cn.frank.carback.page.BaseFragment
import cn.frank.carback.viewmodel.HomeViewModel
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

/**
 * 主页 Fragment
 *
 * @author shangmingchao
 */
@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private val viewModel by viewModels<HomeViewModel>()
    private var tabLayoutMediator: TabLayoutMediator? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tabFlow = viewModel.tabList.dropSticky()
        startCollect(
            {
                tabFlow.collect { tabs ->
                    tabLayoutMediator?.detach()
                    binding.viewPager.adapter = HomePagerAdapter(this@HomeFragment, tabs)
                    tabLayoutMediator =
                        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                            tab.text = tabs[position].name
                        }.apply {
                            attach()
                        }
                }
            },
        )
    }
}
