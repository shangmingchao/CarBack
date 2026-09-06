package cn.frank.carback.adapter

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import cn.frank.carback.HomeFragment
import cn.frank.carback.fragment.RecommendFragment
import cn.frank.carback.model.home.Tab

/**
 *
 *
 * @author shangmingchao
 */
class HomePagerAdapter(
    fragment: HomeFragment,
    private val tabList: List<Tab>,
) : FragmentStateAdapter(fragment) {

    override fun createFragment(position: Int): Fragment = RecommendFragment().apply {
        arguments = Bundle().apply {
            putString("id", tabList[position].id)
        }
    }

    override fun getItemCount(): Int = tabList.size
}
