package cn.wycode.control

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import cn.wycode.control.common.Keymap
import com.alibaba.fastjson2.JSON

class MainActivity : Activity() {

    private lateinit var content: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        content = LayoutInflater.from(this).inflate(R.layout.activity_main, null)
        setContentView(content)
        content.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent()
            intent.action = Settings.ACTION_MANAGE_OVERLAY_PERMISSION
            intent.data = Uri.parse("package:$packageName")
            startActivityForResult(intent, 1)
        }
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
}
