package cn.frank.carback.feature.home.impl.adapter

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import cn.frank.carback.feature.home.impl.HomeFragment
import cn.frank.carback.feature.home.impl.model.home.Tab
import cn.frank.carback.feature.home.impl.recommend.RecommendFragment

/**
 * 为你推荐列表页
 *
 * @author shangmingchao
 */
class RecommendPagerAdapter(
    fragment: Fragment,
    private val tabList: List<Tab>,
) : FragmentStateAdapter(fragment) {

    override fun createFragment(position: Int): Fragment = RecommendFragment().apply {
        arguments = Bundle().apply {
            putString("id", tabList[position].id)
        }
    }

    override fun getItemCount(): Int = tabList.size
}
