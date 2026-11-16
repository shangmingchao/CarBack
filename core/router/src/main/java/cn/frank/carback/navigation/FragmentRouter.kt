package cn.frank.carback.navigation

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import cn.frank.carback.core.router.R

/**
 * 首页 Fragment 路由
 *
 * @author shangmingchao
 */
object FragmentRouter {

    private const val KEY_BACK_STACK_TAGS = "fragment_router_back_stack_tags"
    private const val KEY_TAB_TAGS = "fragment_router_tab_tags"
    private const val KEY_ACTIVE_TAB = "fragment_router_active_tab"

    var fragmentManager: FragmentManager? = null
        private set
    private var containerId: Int = 0

    /** 返回栈：仅存放 Tab 之上的普通页面（push 进入，pop/popTo 弹出） */
    private val backStack = ArrayDeque<FragmentStackEntry>()

    /** 常驻 Tab 页（hide/show 保留视图状态），不参与返回栈 */
    private val tabs = LinkedHashMap<String, Fragment>()
    private var activeTabTag: String? = null

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
     * 恢复 Tab 页与返回栈。
     *
     * Activity 销毁重建后，FragmentManager 中仍保留着之前添加的 Fragment（系统重新创建的实例），
     * 但内存中的 tabs / backStack 已被清空。此时根据持久化的 tag 列表，通过 findFragmentByTag
     * 找到系统恢复的 Fragment 实例，重建 tabs 与 backStack，并恢复"只显示栈顶或当前 Tab"的状态。
     */
    private fun restoreBackStackIfNeeded(
        fragmentManager: FragmentManager,
        savedInstanceState: Bundle?
    ) {
        // 已有状态说明无需恢复（如进程未死只是 Activity 重建）
        if (backStack.isNotEmpty() || tabs.isNotEmpty()) return

        // 恢复 Tab 页
        val savedTabTags = savedInstanceState?.getStringArrayList(KEY_TAB_TAGS).orEmpty()
        savedTabTags.forEach { tag ->
            fragmentManager.findFragmentByTag(tag)?.let { tabs[tag] = it }
        }
        activeTabTag = savedInstanceState?.getString(KEY_ACTIVE_TAB) ?: savedTabTags.lastOrNull()

        // 恢复返回栈：savedStackTags 是栈底到栈顶的顺序，用 addFirst 逐个添加，
        // 最终 last 为栈底、first 为栈顶，与 push 时的语义一致
        val savedStackTags = savedInstanceState?.getStringArrayList(KEY_BACK_STACK_TAGS).orEmpty()
        savedStackTags.forEach { tag ->
            fragmentManager.findFragmentByTag(tag)?.let {
                backStack.addFirst(FragmentStackEntry(fragment = it, tag = tag))
            }
        }

        if (tabs.isEmpty() && backStack.isEmpty()) {
            // 兜底：没有持久化的 tag 但 FragmentManager 中可能有残留 Fragment，
            // 仍然尝试恢复，避免界面上有 Fragment 但栈为空。
            // 注意：FragmentManager.fragments 不保证顺序，此为极端兜底路径。
            fragmentManager.fragments.forEach { fragment ->
                if (fragment != null && fragment.id == containerId) {
                    val tag = fragment.tag ?: fragment.javaClass.simpleName
                    backStack.addFirst(FragmentStackEntry(fragment = fragment, tag = tag))
                }
            }
        }

        // 恢复显示状态：优先显示栈顶（返回栈非空时），否则显示当前 Tab，其余全部隐藏
        val top = backStack.firstOrNull()?.fragment ?: tabs[activeTabTag] ?: return
        val transaction = fragmentManager.beginTransaction()
        (backStack.map { it.fragment } + tabs.values).forEach { fragment ->
            if (fragment !== top) {
                transaction.hide(fragment)
            }
        }
        transaction.show(top)
        transaction.commitAllowingStateLoss()
    }

    /**
     * 保存 Tab 页与返回栈状态，应在 Activity 的 onSaveInstanceState 中调用。
     * 持久化 Tab tag 列表、当前 Tab 与返回栈 tag（从栈底到栈顶的顺序），以便重建时恢复。
     */
    fun saveInstanceState(outState: Bundle) {
        if (tabs.isNotEmpty()) {
            outState.putStringArrayList(KEY_TAB_TAGS, ArrayList(tabs.keys))
            activeTabTag?.let { outState.putString(KEY_ACTIVE_TAB, it) }
        }
        if (backStack.isNotEmpty()) {
            // backStack 的 first 是栈顶，last 是栈底，保存时从栈底到栈顶
            outState.putStringArrayList(KEY_BACK_STACK_TAGS, ArrayList(backStack.reversed().map { it.tag }))
        }
    }

    /**
     * Activity 销毁时调用，避免持有已销毁的 FragmentManager 导致内存泄漏。
     */
    fun release() {
        fragmentManager = null
        backStack.clear()
        tabs.clear()
        activeTabTag = null
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

        val entry = FragmentStackEntry(fragment, entryTag)

        val transaction = fm.beginTransaction()
        applyAnimation(transaction, animation)

        // 隐藏当前可见页（栈顶，返回栈为空时为当前 Tab）
        currentVisibleFragment()?.let { transaction.hide(it) }

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

        // 隐藏当前可见页（栈顶，返回栈为空时为当前 Tab），避免动画过程中的视觉问题
        currentVisibleFragment()?.let { transaction.hide(it) }

        if (existingIndex != -1) {
            // 栈中已存在相同 tag 的旧实例，弹出并销毁从栈顶到该位置（含）的所有 Fragment
            val targetSize = existingIndex // 保留 [existingIndex+1, last] 即栈底部分
            while (backStack.size > targetSize) {
                val entry = backStack.removeFirst()
                transaction.remove(entry.fragment)
            }
        }

        val entry = FragmentStackEntry(fragment, entryTag)

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
        val entry = FragmentStackEntry(fragment, entryTag)

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
     * Tab 切换：Tab 页常驻（hide/show 保留视图状态），不进入返回栈。
     *
     * - 首次切换到该 Tab 时经 [createFragment] 懒创建页面，之后复用同一实例；
     * - 切换时丢弃返回栈中的普通页面（类似 CLEAR_TOP），回到 Tab 根页面；
     * - 重复点击当前 Tab 也会回到 Tab 根页面。
     *
     * @param tag Tab 唯一标识（通常为路由 path）
     * @param animation 切换动画
     * @param createFragment 首次切换时创建 Fragment
     */
    fun switchTab(
        tag: String,
        animation: FragmentAnimation = FragmentAnimation.NONE,
        createFragment: () -> Fragment,
    ) {
        val fm = fragmentManager ?: return
        val transaction = fm.beginTransaction()
        applyAnimation(transaction, animation)

        // 丢弃返回栈中的普通页面
        backStack.forEach { transaction.remove(it.fragment) }
        backStack.clear()

        // 首次切换则创建并添加，之后复用已有实例
        if (!tabs.containsKey(tag)) {
            val fragment = createFragment()
            transaction.add(containerId, fragment, tag)
            tabs[tag] = fragment
        }
        tabs.forEach { (tabTag, fragment) ->
            if (tabTag == tag) {
                transaction.show(fragment)
            } else {
                transaction.hide(fragment)
            }
        }
        activeTabTag = tag

        transaction.commitAllowingStateLoss()
    }

    /**
     * 弹出栈顶（类似 finish()）
     *
     * Tab 页常驻且不参与返回栈：栈空时返回 false，由宿主处理 finish；
     * 弹出最后一层普通页面后恢复显示当前 Tab。
     *
     * @return true 如果成功弹出，false 如果返回栈为空（由 Activity 处理 finish）
     */
    fun pop(): Boolean {
        if (backStack.isEmpty()) return false
        val fm = fragmentManager ?: return false

        val current = backStack.removeFirst()

        val transaction = fm.beginTransaction()
        applyAnimation(transaction, FragmentAnimation.SLIDE_OUT_RIGHT)

        transaction.remove(current.fragment)
        currentVisibleFragment()?.let { transaction.show(it) }

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
        // 显示保留的栈顶；栈被清空时（inclusive 且目标为栈底）回退显示当前 Tab
        currentVisibleFragment()?.let { transaction.show(it) }
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
     * 获取当前可见页的 tag（栈顶普通页面，返回栈为空时为当前 Tab）
     */
    fun currentTag(): String? = backStack.firstOrNull()?.tag ?: activeTabTag

    /**
     * 返回栈中是否存在指定 tag（用于 Tab 切换判断）
     */
    fun containsTag(tag: String): Boolean = backStack.any { it.tag == tag }

    /**
     * 栈是否为空
     */
    fun isEmpty(): Boolean = backStack.isEmpty()

    /**
     * 栈大小
     */
    fun size(): Int = backStack.size

    /**
     * 当前可见的普通页面：返回栈非空时为栈顶，否则为当前 Tab
     */
    private fun currentVisibleFragment(): Fragment? =
        backStack.firstOrNull()?.fragment ?: tabs[activeTabTag]

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
