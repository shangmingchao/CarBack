package cn.frank.carback.core.debug.scaffold

import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.frank.carback.core.debug.scaffold.databinding.ActivityScaffoldBinding
import cn.frank.carback.navigation.FragmentAnimation
import cn.frank.carback.navigation.FragmentRouter
import cn.frank.carback.route.Router
import dagger.hilt.android.AndroidEntryPoint

/**
 * demo 壳通用宿主 Activity：路由容器 + 边缘到边缘 + 返回栈托管。
 *
 * 子类只需声明起始路由：
 * ```
 * class DemoMainActivity : ScaffoldActivity() {
 *     override val startRoute: String get() = "home/main"
 * }
 * ```
 *
 * @author shangmingchao
 */
@AndroidEntryPoint
abstract class ScaffoldActivity : AppCompatActivity() {

    /**
     * demo 启动时展示的起始路由
     */
    protected abstract val startRoute: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivityScaffoldBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.scaffoldContainer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        FragmentRouter.init(supportFragmentManager, R.id.scaffoldContainer, savedInstanceState)
        if (savedInstanceState == null) {
            Router.open(startRoute, animation = FragmentAnimation.NONE)
        }
        onBackPressedDispatcher.addCallback(this) {
            if (FragmentRouter.pop()) {
                return@addCallback
            }
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        FragmentRouter.saveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        FragmentRouter.release()
    }
}
