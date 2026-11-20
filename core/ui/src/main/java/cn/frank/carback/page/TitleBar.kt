package cn.frank.carback.page

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.withStyledAttributes
import cn.frank.carback.core.ui.R

/**
 * 标题栏
 *
 * @author shangmingchao
 */
class TitleBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val titleView: TextView
    private val navBackBtn: ImageView

    var title: String? = null
        set(value) {
            field = value
            titleView.text = value
        }
    var navBackBtnDrawable: Drawable? = null
        set(value) {
            field = value
            navBackBtn.setImageDrawable(value)
        }
    var navBackAction: (() -> Unit)? = null
        set(value) {
            field = value
            navBackBtn.setOnClickListener { value?.invoke() }
        }

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_title_bar, this)
        titleView = findViewById(R.id.title)
        navBackBtn = findViewById(R.id.navBackBtn)
        context.withStyledAttributes(attrs, R.styleable.TitleBar) {
            title = getString(R.styleable.TitleBar_title)
            getDrawable(R.styleable.TitleBar_navBackIcon)?.let { navBackBtnDrawable = it }
        }
    }
}
