package cn.frank.carback

import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.frank.carback.feature.home.impl.HomeFragment
import cn.frank.carback.navigation.FragmentAnimation
import cn.frank.carback.navigation.FragmentRouter
import cn.frank.carback.route.Router
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

/**
 * 主壳 Activity
 *
 * @author shangmingchao
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.homeContentContainer)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        FragmentRouter.init(supportFragmentManager, R.id.homeContentContainer, savedInstanceState)
        if (savedInstanceState == null) {
            Router.open("home/main", animation = FragmentAnimation.NONE)
        }
        onBackPressedDispatcher.addCallback(this) {
            if (FragmentRouter.pop()) {
                return@addCallback
            }
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        FragmentRouter.saveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        FragmentRouter.release()
    }
}
