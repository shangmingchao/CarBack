package cn.frank.carback.navigation

import androidx.fragment.app.Fragment

/**
 * Fragment 栈项
 *
 * @author shangmingchao
 */
data class FragmentStackEntry(
 val fragment: Fragment,
 val tag: String,
 val animation: FragmentAnimation = FragmentAnimation.SLIDE_IN_RIGHT
)
