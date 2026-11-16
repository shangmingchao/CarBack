package cn.frank.carback.recyclerview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding

/**
 * 用于处理特定 Model 类型的 Presenter
 *
 * @param M Model 类型
 * @param VB 对应的 ViewBinding 类型
 *
 * @author shangmingchao
 */
interface ItemPresenter<M : Any, VB : ViewBinding> {
    /**
     * 创建 ViewBinding 实例
     */
    fun createBinding(inflater: LayoutInflater, parent: ViewGroup, attachToParent: Boolean): VB

    /**
     * 将 Model 数据绑定到 ViewBinding
     */
    fun bind(model: M, binding: VB, position: Int)

    /**
     * 处理局部刷新
     */
    fun onPayload(model: M, binding: VB, position: Int, payloads: List<Any>) {
        // ignore
    }

    /**
     * item 被回收（滑出屏幕或复用）时调用，用于清理资源，如取消异步任务、解绑监听器等
     *
     * 默认空实现，只有需要清理资源的 Presenter 才需要覆写
     */
    fun unbind(binding: VB) {
        // ignore
    }
}
