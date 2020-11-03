package cn.wycode.control

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.*
import cn.wycode.control.common.LOG_TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val CHANNEL_ID = "cn.wycode.control_channel"

class MouseService : Service() {


    private var currentOrientation = Configuration.ORIENTATION_PORTRAIT

    override fun onCreate() {
        super.onCreate()

        val channel = NotificationChannel(
            CHANNEL_ID,
            "control channel",
            NotificationManager.IMPORTANCE_LOW
        )

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)

        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Android Controller")
            .setContentText("running")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val size = Point()
        windowManager.defaultDisplay.getRealSize(size)
        currentOrientation = resources.configuration.orientation

        val layoutParams = WindowManager.LayoutParams()
        layoutParams.type = TYPE_APPLICATION_OVERLAY
        layoutParams.format = PixelFormat.RGBA_8888
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutParams.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        layoutParams.flags = FLAG_NOT_TOUCHABLE.or(FLAG_KEEP_SCREEN_ON).or(FLAG_NOT_FOCUSABLE).or(FLAG_NOT_TOUCH_MODAL)
        layoutParams.width = MATCH_PARENT
        layoutParams.height = MATCH_PARENT
        val overlay = LayoutInflater.from(this).inflate(R.layout.overlay, null) as ViewGroup
        windowManager.addView(overlay, layoutParams)
        val keymapView = overlay.getChildAt(0) as KeymapView
        val pointer = overlay.getChildAt(1)

        val server = MouseServer(size, pointer, keymapView)


        CoroutineScope(Dispatchers.IO).launch {

            server.start()
        }

        overlay.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (currentOrientation != resources.configuration.orientation) {
                currentOrientation = resources.configuration.orientation
                windowManager.defaultDisplay.getRealSize(size)

                CoroutineScope(Dispatchers.IO).launch {
                    server.sendScreenInfo()
                }
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.d(LOG_TAG, "onDestroy");
        stopForeground(true)
        super.onDestroy()
    }
}
