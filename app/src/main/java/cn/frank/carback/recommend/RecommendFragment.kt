package cn.frank.carback.recommend

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import cn.frank.carback.page.CommonListFragment
import cn.frank.carback.recommend.presenter.ImageTextPresenter
import cn.frank.carback.recyclerview.CommonListAdapter
import cn.frank.carback.recyclerview.ItemModel
import cn.frank.carback.recyclerview.decoration.CommonSpacingDecoration
import cn.frank.carback.recyclerview.decoration.setItemDecoration
import dagger.hilt.android.AndroidEntryPoint

/**
 *
 *
 * @author shangmingchao
 */
@AndroidEntryPoint
class RecommendFragment : CommonListFragment() {

    override val viewModel: RecommendViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.setItemDecoration(CommonSpacingDecoration())
    }

    override fun registerPresenter(adapter: CommonListAdapter<ItemModel>) {
        adapter.register(ImageTextPresenter())
    }
}
