package cn.frank.carback.feature.home.impl.detail.list

import android.os.Bundle
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import cn.frank.carback.ext.dp
import cn.frank.carback.feature.home.impl.R
import cn.frank.carback.feature.home.impl.adapter.SongListAdapter
import cn.frank.carback.navigation.FragmentRouter
import cn.frank.carback.page.CommonListFragment
import cn.frank.carback.recyclerview.CommonListAdapter
import cn.frank.carback.recyclerview.ItemModel
import cn.frank.carback.recyclerview.decoration.CommonSpacingDecoration
import cn.frank.carback.recyclerview.decoration.setItemDecoration
import cn.frank.carback.route.Route
import cn.frank.carback.route.Router
import dagger.hilt.android.AndroidEntryPoint

/**
 *
 *
 * @author shangmingchao
 */
@AndroidEntryPoint
@Route("detail/list")
class DetailListFragment : CommonListFragment() {

    override val viewModel: DetailListViewModel by viewModels()

    override val adapter: CommonListAdapter<ItemModel> = SongListAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.setItemDecoration(CommonSpacingDecoration())
        binding.titleBar.updatePadding(16.dp)
        binding.titleBar.title = getString(R.string.home_detail)
        binding.titleBar.visibility = View.VISIBLE
        binding.titleBar.navBackAction = { Router.back() }
    }

    override fun registerPresenter(adapter: CommonListAdapter<ItemModel>) = Unit
}
