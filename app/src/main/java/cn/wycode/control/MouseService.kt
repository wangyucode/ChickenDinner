package cn.wycode.control

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.IBinder
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.*
import java.io.OutputStream
import java.nio.ByteBuffer

const val CHANNEL_ID = "cn.wycode.control_channel"

class MouseService : Service() {


    override fun onCreate() {
        super.onCreate()

        val channel = NotificationChannel(
            CHANNEL_ID,
            "control channel",
            NotificationManager.IMPORTANCE_HIGH
        )

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)

        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Android Controller")
            .setContentText("running").build()

        startForeground(1, notification)

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val size = Point()

        windowManager.defaultDisplay.getRealSize(size)

        val layoutParams = WindowManager.LayoutParams()
        layoutParams.type = TYPE_APPLICATION_OVERLAY
        layoutParams.format = PixelFormat.RGBA_8888
        layoutParams.flags = FLAG_NOT_TOUCHABLE.or(FLAG_KEEP_SCREEN_ON).or(FLAG_NOT_FOCUSABLE).or(FLAG_NOT_TOUCH_MODAL)
        layoutParams.width = MATCH_PARENT
        layoutParams.height = MATCH_PARENT
        val mouse = LayoutInflater.from(this).inflate(R.layout.mouse, null) as ViewGroup
        windowManager.addView(mouse, layoutParams)
        val pointer = mouse.getChildAt(0)
        MouseServer(object : ServerCallBack {
            override fun onConnected(outputStream: OutputStream) {
                val buffer = ByteBuffer.allocate(8)

                buffer.putInt(size.x)
                buffer.putInt(size.y)

                outputStream.write(buffer.array())
            }

            override fun onUpdate(point: Point) {
                pointer.x = point.x.toFloat()
                pointer.y = point.y.toFloat()
            }
        }).execute()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

}
