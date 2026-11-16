package cn.frank.carback

import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.frank.carback.navigation.FragmentAnimation
import cn.frank.carback.navigation.FragmentRouter
import cn.frank.carback.route.Router
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

/**
 * 主壳 Activity：三 Tab 装配（首页 / 我的 / 搜索）
 *
 * Tab 切换走 [Router.openTab]：Tab 页常驻且不进入返回栈（hide/show 保留页面状态），
 * 返回键只在弹出普通压栈页面后才会退出应用。
 *
 * @author shangmingchao
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        FragmentRouter.init(supportFragmentManager, R.id.homeContentContainer, savedInstanceState)
        if (savedInstanceState == null) {
            Router.openTab("home/main")
        }
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.tabHome -> {
                    Router.openTab("home/main")
                    true
                }

                R.id.tabMine -> {
                    Router.openTab("mine/main")
                    true
                }

                R.id.tabSearch -> {
                    Router.openTab("search/main")
                    true
                }

                else -> false
            }
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
