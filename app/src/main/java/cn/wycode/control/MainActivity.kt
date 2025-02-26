package cn.wycode.control

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import cn.wycode.control.common.Keymap
import com.alibaba.fastjson2.JSON

class MainActivity : Activity(), View.OnTouchListener {

    private lateinit var content: View

    var lastTouchCount = 0
    var lastTouchAction = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        content = LayoutInflater.from(this).inflate(R.layout.activity_main, null)
        setContentView(content)
        window.setDecorFitsSystemWindows(false)
        window.insetsController.let {
            it?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            it?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent()
            intent.action = Settings.ACTION_MANAGE_OVERLAY_PERMISSION
            intent.data = Uri.parse("package:$packageName")
            startActivityForResult(intent, 1)
        }
        content.setOnTouchListener(this)
    }

    override fun onStart() {
        super.onStart()
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "没有权限！", Toast.LENGTH_SHORT).show()
        } else {
            startForegroundService(Intent(this, MouseService::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, MouseService::class.java))
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if(event.action == MotionEvent.ACTION_MOVE && lastTouchAction == MotionEvent.ACTION_MOVE && event.pointerCount==lastTouchCount){
            return true
        }
        Log.d("MainActivity, [wycs]", "onTouch: $event")
        lastTouchCount = event.pointerCount
        lastTouchAction = event.action
        return true
    }
}
