package cn.frank.carback.feature.home.impl

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import cn.frank.carback.feature.home.impl.adapter.HomePagerAdapter
import cn.frank.carback.feature.home.impl.databinding.FragmentHomeBinding
import cn.frank.carback.feature.home.impl.viewmodel.HomeViewModel
import cn.frank.carback.page.BaseFragment
import cn.frank.carback.route.Route
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

/**
 * 主页 Fragment
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
        binding.viewPager.adapter = HomePagerAdapter(this, tabs)
        binding.viewPager.isUserInputEnabled = false
        TabLayoutMediator(binding.tab, binding.viewPager, true, false) { tab, position ->
            tab.text = tabs[position].name
            tab.view.setOnLongClickListener { true }
        }.also { it.attach() }
    }
}
