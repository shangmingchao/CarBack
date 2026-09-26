package cn.frank.carback.detail.list

import android.os.Bundle
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.viewModels
import cn.frank.carback.navigation.FragmentRouter
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
class DetailListFragment : CommonListFragment() {

    override val viewModel: DetailListViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.setItemDecoration(CommonSpacingDecoration())
        binding.titleBar.visibility = View.VISIBLE
        binding.titleBar.title = "DetailList"
        binding.titleBar.navigationIcon = ResourcesCompat.getDrawable(resources, android.R.drawable.arrow_down_float, null)
        binding.titleBar.setNavigationOnClickListener {
            FragmentRouter.pop()
        }
    }
    override fun registerPresenter(adapter: CommonListAdapter<ItemModel>) {
        adapter.register(ImageTextPresenter())
    }

    companion object {
        fun newInstance(id: String) = DetailListFragment().apply {
            arguments = Bundle().apply {
                putString("id", id)
            }
        }
    }
}
