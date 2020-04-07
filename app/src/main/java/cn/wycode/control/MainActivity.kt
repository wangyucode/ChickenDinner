package cn.wycode.control

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
const val OVERLAY_PERMISSION_REQUEST_CODE = 300

class MainActivity : Activity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent()
            intent.action = Settings.ACTION_MANAGE_OVERLAY_PERMISSION
            intent.data = Uri.parse("package:$packageName")
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
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
}
