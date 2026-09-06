package cn.frank.carback.navigation

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import cn.frank.carback.R

/**
 * 首页 Fragment 路由
 *
 * @author shangmingchao
 */
object FragmentRouter {

    private const val KEY_BACK_STACK_TAGS = "fragment_router_back_stack_tags"

    var fragmentManager: FragmentManager? = null
        private set
    private var containerId: Int = 0
    private val backStack = ArrayDeque<FragmentStackEntry>()

    /**
     * 初始化路由，绑定 Activity 的 FragmentManager。
     * 应在 Activity 的 onCreate 中调用。
     *
     * 当 Activity 被系统在后台杀死后销毁重建时，FragmentManager 会自动恢复之前添加的
     * Fragment（系统会重新创建新的 Fragment 实例），但 FragmentRouter 内存中的 backStack
     * 会随进程死亡而丢失。此方法会从 FragmentManager 恢复回退栈，确保两者状态同步。
     *
     * [savedInstanceState] 是 Activity 的 savedInstanceState，用于读取持久化的栈 tag 列表。
     */
    fun init(
        fragmentManager: FragmentManager,
        containerId: Int,
        savedInstanceState: Bundle? = null
    ) {
        this.fragmentManager = fragmentManager
        this.containerId = containerId
        restoreBackStackIfNeeded(fragmentManager, savedInstanceState)
    }

    /**
     * 从 FragmentManager 恢复回退栈。
     *
     * Activity 销毁重建后，FragmentManager 中仍保留着之前添加的 Fragment（系统重新创建的实例），
     * 但 backStack 已被清空。此时需要根据持久化的 tag 列表，通过 findFragmentByTag 找到
     * 系统恢复的 Fragment 实例，按正确的顺序重建 backStack。
     *
     * 如果没有持久化的 tag 列表（极端兜底场景），则直接从 FragmentManager 的 fragments 恢复，
     * 但此方式不保证顺序。
     */
    private fun restoreBackStackIfNeeded(
        fragmentManager: FragmentManager,
        savedInstanceState: Bundle?
    ) {
        // backStack 不为空说明无需恢复（如进程未死只是 Activity 重建）
        if (backStack.isNotEmpty()) return

        val savedTags = savedInstanceState?.getStringArrayList(KEY_BACK_STACK_TAGS)

        if (!savedTags.isNullOrEmpty()) {
            // savedTags 是栈底到栈顶的顺序，用 addFirst 逐个添加，
            // 最终 last 为栈底、first 为栈顶，与 push 时的语义一致
            savedTags.forEach { tag ->
                val fragment = fragmentManager.findFragmentByTag(tag) ?: return@forEach
                backStack.addFirst(
                    FragmentStackEntry(
                        fragment = fragment,
                        tag = tag,
                        animation = FragmentAnimation.SLIDE_IN_RIGHT
                    )
                )
            }
        } else {
            // 兜底：没有持久化的 tag 但 FragmentManager 中可能有残留 Fragment，
            // 仍然尝试恢复，避免界面上有 Fragment 但栈为空。
            // 注意：FragmentManager.fragments 不保证顺序，此为极端兜底路径。
            fragmentManager.fragments.forEach { fragment ->
                if (fragment != null && fragment.id == containerId) {
                    val tag = fragment.tag ?: fragment.javaClass.simpleName
                    backStack.addFirst(
                        FragmentStackEntry(
                            fragment = fragment,
                            tag = tag,
                            animation = FragmentAnimation.SLIDE_IN_RIGHT
                        )
                    )
                }
            }
        }

        if (backStack.isEmpty()) return

        // 恢复正确的显示/隐藏状态：仅显示栈顶（first），隐藏其余
        val transaction = fragmentManager.beginTransaction()
        var isTop = true
        backStack.forEach { entry ->
            if (isTop) {
                transaction.show(entry.fragment)
                isTop = false
            } else {
                transaction.hide(entry.fragment)
            }
        }
        transaction.commitAllowingStateLoss()
    }

    /**
     * 保存回退栈状态，应在 Activity 的 onSaveInstanceState 中调用。
     * 持久化栈中所有 Fragment 的 tag（从栈底到栈顶的顺序），以便重建时恢复。
     */
    fun saveInstanceState(outState: Bundle) {
        if (backStack.isEmpty()) {
            return
        }
        // backStack 的 first 是栈顶，last 是栈底，保存时从栈底到栈顶
        val tags = ArrayList<String>()
        backStack.reversed().forEach { tags.add(it.tag) }
        outState.putStringArrayList(KEY_BACK_STACK_TAGS, tags)
    }

    /**
     * Activity 销毁时调用，避免持有已销毁的 FragmentManager 导致内存泄漏。
     */
    fun release() {
        fragmentManager = null
        backStack.clear()
    }

    /**
     * 压入新的 Fragment（类似 startActivity）
     *
     * @param singleInstance 是否保证 backStack 中只有一个对应 tag 的 Fragment 实例。
     * 当为 true 时（类似 Activity 的 singleInstance 启动模式）：
     * - 如果栈中已存在相同 tag 的 Fragment，会先弹出并销毁所有在其之上的 Fragment 以及该旧实例，
     *   然后将新 Fragment 压入栈顶；
     * - 如果不存在相同 tag 的 Fragment，行为与普通 push 一致。
     */
    fun push(
        fragment: Fragment,
        tag: String? = null,
        animation: FragmentAnimation = FragmentAnimation.SLIDE_IN_RIGHT,
        singleInstance: Boolean = false
    ) {
        val fm = fragmentManager ?: return
        val entryTag = tag ?: fragment.javaClass.simpleName

        if (singleInstance) {
            pushSingleInstance(fm, fragment, entryTag, animation)
            return
        }

        val entry = FragmentStackEntry(fragment, entryTag, animation)

        val transaction = fm.beginTransaction()
        applyAnimation(transaction, animation)

        // 隐藏当前栈顶
        backStack.firstOrNull()?.let { current ->
            transaction.hide(current.fragment)
        }

        // 添加新 Fragment
        if (fragment.isAdded) {
            transaction.show(fragment)
        } else {
            transaction.add(containerId, fragment, entryTag)
        }

        transaction.commitAllowingStateLoss()
        backStack.addFirst(entry)
    }

    /**
     * SingleInstance 模式的 push 实现。
     *
     * 保证 backStack 中对应 tag 只存在一个实例。若栈中已存在相同 tag 的 Fragment，
     * 则弹出并销毁从栈顶到该实例（含）的所有 Fragment，然后将新 Fragment 压入栈顶。
     */
    private fun pushSingleInstance(
        fm: FragmentManager,
        fragment: Fragment,
        entryTag: String,
        animation: FragmentAnimation
    ) {
        val existingIndex = backStack.indexOfFirst { it.tag == entryTag }

        val transaction = fm.beginTransaction()
        applyAnimation(transaction, animation)

        if (existingIndex != -1) {
            // 栈中已存在相同 tag 的旧实例，弹出并销毁从栈顶到该位置（含）的所有 Fragment
            // 隐藏当前栈顶（即将被移除的），避免动画过程中的视觉问题
            backStack.firstOrNull()?.let { current ->
                transaction.hide(current.fragment)
            }

            val targetSize = existingIndex // 保留 [existingIndex+1, last] 即栈底部分
            while (backStack.size > targetSize) {
                val entry = backStack.removeFirst()
                transaction.remove(entry.fragment)
            }
        } else {
            // 栈中不存在相同 tag，隐藏当前栈顶
            backStack.firstOrNull()?.let { current ->
                transaction.hide(current.fragment)
            }
        }

        val entry = FragmentStackEntry(fragment, entryTag, animation)

        // 添加新 Fragment
        if (fragment.isAdded) {
            transaction.show(fragment)
        } else {
            transaction.add(containerId, fragment, entryTag)
        }

        transaction.commitAllowingStateLoss()
        backStack.addFirst(entry)
    }

    /**
     * 替换当前 Fragment（类似 startActivity + FLAG_ACTIVITY_REPLACE）
     */
    fun replace(
        fragment: Fragment,
        tag: String? = null,
        animation: FragmentAnimation = FragmentAnimation.NONE
    ) {
        val fm = fragmentManager ?: return
        val entryTag = tag ?: fragment.javaClass.simpleName
        val entry = FragmentStackEntry(fragment, entryTag, animation)

        val transaction = fm.beginTransaction()
        applyAnimation(transaction, animation)

        // 移除栈中所有现存 Fragment
        backStack.forEach { transaction.remove(it.fragment) }
        transaction.add(containerId, fragment, entryTag)

        transaction.commitAllowingStateLoss()
        backStack.clear()
        backStack.addFirst(entry)
    }

    /**
     * 弹出栈顶（类似 finish()）
     *
     * @return true 如果成功弹出，false 如果栈中只剩一个或为空（由 Activity 处理 finish）
     */
    fun pop(): Boolean {
        if (backStack.size <= 1) return false
        val fm = fragmentManager ?: return false

        val current = backStack.removeFirst()
        val previous = backStack.firstOrNull() ?: return false

        val transaction = fm.beginTransaction()
        applyAnimation(transaction, FragmentAnimation.SLIDE_OUT_RIGHT)

        transaction.remove(current.fragment)
        transaction.show(previous.fragment)

        transaction.commitAllowingStateLoss()
        return true
    }

    /**
     * 弹出到指定 tag 的层级
     *
     * @param tag 目标 Fragment 的 tag
     * @param inclusive 是否连同目标一起弹出
     * @return true 如果找到目标并执行了弹出操作
     */
    fun popTo(tag: String, inclusive: Boolean = false): Boolean {
        val fm = fragmentManager ?: return false
        // ArrayDeque:first 是栈顶，last 是栈底
        val targetIndex = backStack.indexOfFirst { it.tag == tag }
        if (targetIndex == -1) return false
        val transaction = fm.beginTransaction()
        val keepIndex = if (inclusive) targetIndex - 1 else targetIndex
        // 从栈顶开始弹出，直到保留位置
        while (backStack.size - 1 > keepIndex) {
            val entry = backStack.removeFirst()
            transaction.remove(entry.fragment)
        }
        backStack.firstOrNull()?.let { transaction.show(it.fragment) }
        transaction.commitAllowingStateLoss()
        return true
    }

    /**
     * 清空栈并跳转到指定 Fragment（类似 FLAG_ACTIVITY_CLEAR_TASK）
     */
    fun popAllAndPush(fragment: Fragment, tag: String? = null) {
        val fm = fragmentManager ?: return
        val transaction = fm.beginTransaction()
        backStack.forEach { transaction.remove(it.fragment) }
        backStack.clear()
        transaction.commitAllowingStateLoss()

        push(fragment, tag)
    }

    /**
     * 获取当前栈顶的 tag（用于路由判断）
     */
    fun currentTag(): String? = backStack.firstOrNull()?.tag

    /**
     * 栈是否为空
     */
    fun isEmpty(): Boolean = backStack.isEmpty()

    /**
     * 栈大小
     */
    fun size(): Int = backStack.size

    private fun applyAnimation(
        transaction: FragmentTransaction,
        animation: FragmentAnimation,
    ) {
        when (animation) {
            FragmentAnimation.SLIDE_IN_RIGHT -> {
                transaction.setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.fade_out,
                    0,
                    0
                )
            }
            FragmentAnimation.SLIDE_OUT_RIGHT -> {
                transaction.setCustomAnimations(
                    R.anim.fade_in,
                    R.anim.slide_out_right,
                    0,
                    0
                )
            }
            FragmentAnimation.NONE -> {
            }
        }
    }
}
