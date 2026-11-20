package cn.frank.carback.feature.home.impl.adapter

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import cn.frank.carback.feature.home.impl.model.home.Tab
import cn.frank.carback.feature.home.impl.recommend.RecommendFragment
import cn.frank.carback.route.Router

/**
 * 首页 Pager Adapter
 *
 * @author shangmingchao
 */
class HomePagerAdapter(
    fragment: Fragment,
    private val tabList: List<Tab>,
) : FragmentStateAdapter(fragment) {

    override fun createFragment(position: Int): Fragment =
        // tabs 已在 HomeViewModel 中经 Router.hasRoute 过滤，此处缺失仅可能是逻辑错误
        Router.fragment(tabList[position].id)
            ?: error("Tab 路由未装配：${tabList[position].id}")

    override fun getItemCount(): Int = tabList.size
}
