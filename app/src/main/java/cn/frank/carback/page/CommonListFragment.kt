package cn.frank.carback.page

import androidx.recyclerview.widget.RecyclerView
import cn.frank.carback.databinding.CommonRefreshListPageBinding
import com.scwang.smart.refresh.layout.SmartRefreshLayout

/**
 * 通用列表页面
 *
 * @author shangmingchao
 */
abstract class CommonListFragment : AbsListFragment<CommonRefreshListPageBinding>() {

    override fun getRefreshLayout(): SmartRefreshLayout = binding.refreshLayout

    override fun getRecyclerView(): RecyclerView = binding.recyclerView

    override fun getPageStateView(): PageStateView = binding.pageStateView
}
