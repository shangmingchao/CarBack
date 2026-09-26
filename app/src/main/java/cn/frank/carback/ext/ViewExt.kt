package cn.frank.carback.ext

import android.content.res.Resources

/**
 * View 扩展
 *
 * @author shangmingchao
 */

/**
 * 转换为 dp
 */
val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density + 0.5f).toInt()

val Float.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density + 0.5f).toInt()
