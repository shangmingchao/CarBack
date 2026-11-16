package cn.frank.carback

import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import cn.frank.carback.navigation.FragmentAnimation
import cn.frank.carback.navigation.FragmentRouter
import cn.frank.carback.route.Router
import cn.frank.carback.sdk.SdkContract

/**
 * SDK 对外入口 Activity（exported）
 *
 * 第三方 APK 通过 :sdk 的 [cn.frank.carback.sdk.CarBackSdk.openPage] 拉起本页面，
 * 本页面根据 [SdkContract.EXTRA_PAGE_PATH] 通过 [Router] 跳转到指定 feature 页面。
 *
 * @author shangmingchao
 */
class SdkEntryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = intent?.getStringExtra(SdkContract.EXTRA_PAGE_PATH)
        if (path.isNullOrBlank() || !Router.hasRoute(path)) {
            Toast.makeText(this, "无效的页面路由：$path", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        FragmentRouter.init(supportFragmentManager, android.R.id.content, savedInstanceState)
        if (savedInstanceState == null) {
            Router.open(path, animation = FragmentAnimation.NONE)
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
