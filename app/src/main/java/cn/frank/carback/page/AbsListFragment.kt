package cn.frank.carback.page

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import cn.frank.carback.model.LoadType
import cn.frank.carback.recyclerview.CommonListAdapter
import cn.frank.carback.recyclerview.ItemModel
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener

/**
 * 抽象列表页面
 *
 * @author shangmingchao
 */
abstract class AbsListFragment<VB : ViewBinding> : BaseFragment<VB>() {

    abstract val viewModel: CommonListViewModel
    protected val adapter = CommonListAdapter<ItemModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerPresenter(adapter)
        getRecyclerView().adapter = adapter
        getRefreshLayout().setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
            override fun onRefresh(refreshLayout: RefreshLayout) {
                viewModel.refresh()
            }

            override fun onLoadMore(refreshLayout: RefreshLayout) {
                viewModel.loadMore()
            }
        })
        lazyLaunch {
            viewModel.listStateFlow.state.collect {
                composePageState(
                    it,
                    getRefreshLayout(),
                    getPageStateView(),
                    { viewModel.listStateFlow.refresh(true) },
                ) { success ->
                    onUpdateList(success.data, success.loadType == LoadType.Refresh)
                }
            }
        }
    }

    override fun onDestroyView() {
        getRecyclerView().adapter = null
        super.onDestroyView()
    }

    /**
     * 更新列表
     */
    open fun onUpdateList(list: List<ItemModel>, isRefresh: Boolean) {
        if (!isRefresh && list.isEmpty()) {
            return
        }
        adapter.submitList(list)
    }

    /**
     * 获取刷新布局
     */
    abstract fun getRefreshLayout(): SmartRefreshLayout

    /**
     * 获取 RecyclerView
     */
    abstract fun getRecyclerView(): RecyclerView

    /**
     * 获取页面状态视图
     */
    abstract fun getPageStateView(): PageStateView

    /**
     * 注册 Presenter
     */
    abstract fun registerPresenter(adapter: CommonListAdapter<ItemModel>)
}
