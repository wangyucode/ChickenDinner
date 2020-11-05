package cn.wycode.control

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Point
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.*

class MouseServerWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as
                NotificationManager
    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as
                WindowManager

    private var currentOrientation = Configuration.ORIENTATION_PORTRAIT

    private val size = Point()
    private lateinit var keymapView: KeymapView
    private lateinit var pointer: View

    override suspend fun doWork(): Result {

        Log.d("wycs", Thread.currentThread().name)
        setForeground(createForegroundInfo())

        withContext(Dispatchers.Main) {
            addOverlay()
        }

        val server = MouseServer(size, pointer, keymapView)

        keymapView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val orientation = applicationContext.resources.configuration.orientation
            if (currentOrientation != orientation) {
                currentOrientation = orientation
                windowManager.defaultDisplay.getRealSize(size)

                CoroutineScope(Dispatchers.IO).launch {
                    server.sendScreenInfo()
                }
            }
        }

        server.start()

        return Result.success()
    }

    private fun addOverlay() {

        windowManager.defaultDisplay.getRealSize(size)
        currentOrientation = applicationContext.resources.configuration.orientation

        val layoutParams = WindowManager.LayoutParams()
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        layoutParams.format = PixelFormat.RGBA_8888
        layoutParams.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        layoutParams.flags =
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                .or(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                .or(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                .or(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT

        val overlay = LayoutInflater.from(applicationContext).inflate(R.layout.overlay, null) as ViewGroup
        windowManager.addView(overlay, layoutParams)

        keymapView = overlay.getChildAt(0) as KeymapView
        pointer = overlay.getChildAt(1)
    }

    // Creates an instance of ForegroundInfo which can be used to update the
    // ongoing notification.
    private fun createForegroundInfo(): ForegroundInfo {
        createChannel()

        val notification = Notification.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Overlay Service")
            .setTicker("Overlay Service")
            .setContentText("Overlay Service is running")
            .setSmallIcon(android.R.drawable.ic_secure)
            .setOngoing(true)
            .build()

        return ForegroundInfo(1, notification)
    }

    private fun createChannel() {
        // Create a Notification channel
        val channel = NotificationChannel(
            CHANNEL_ID,
            "control channel",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "CHANNEL_ID_STRING"
    }
}