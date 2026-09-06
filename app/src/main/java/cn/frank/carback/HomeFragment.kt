package cn.frank.carback

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import cn.frank.carback.adapter.HomePagerAdapter
import cn.frank.carback.databinding.FragmentHomeBinding
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
        startCollect(
            {
                viewModel.tabList.collect {
                    tabLayoutMediator?.detach()
                    binding.viewPager.adapter = HomePagerAdapter(this@HomeFragment, it)
                    tabLayoutMediator =
                        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                            tab.text = it[position].name
                        }.apply {
                            attach()
                        }
                }
            },
        )
    }
}
