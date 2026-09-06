package cn.frank.carback.fragment

import androidx.fragment.app.viewModels
import cn.frank.carback.page.CommonListFragment
import cn.frank.carback.presenter.ImageTextPresenter
import cn.frank.carback.recyclerview.CommonListAdapter
import cn.frank.carback.recyclerview.ItemModel
import cn.frank.carback.viewmodel.RecommendViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 *
 *
 * @author shangmingchao
 */
@AndroidEntryPoint
class RecommendFragment : CommonListFragment() {

    override val viewModel: RecommendViewModel by viewModels()

    override fun registerPresenter(adapter: CommonListAdapter<ItemModel>) {
        adapter.register(ImageTextPresenter())
    }
}
