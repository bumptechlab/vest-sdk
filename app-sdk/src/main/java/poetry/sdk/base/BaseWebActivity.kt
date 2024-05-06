package poetry.sdk.base

import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity

abstract class BaseWebActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFullScreen()
        initView()
    }

    override fun onResume() {
        super.onResume()
        doResume()
    }

    private fun setFullScreen() {
        //屏幕保持常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        //设置无标题
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        //设置全屏
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }

    protected abstract fun initView()
    protected abstract fun doResume()
}
