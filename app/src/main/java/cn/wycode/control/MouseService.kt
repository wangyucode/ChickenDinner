package cn.wycode.control

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import cn.wycode.control.common.LOG_TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MouseService : Service() {

    override fun onCreate() {
        super.onCreate()

        startForeground(1, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)

        val context = this
        CoroutineScope(Dispatchers.IO).launch {
            MouseServer(context).start()
        }
    }


    private fun createNotification(): Notification {
        val channel = NotificationChannel(
            "CONTROL_CHANNEL_ID",
            "control channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager =
            this.getSystemService(Context.NOTIFICATION_SERVICE) as
                    NotificationManager
        notificationManager.createNotificationChannel(channel)

        return Notification.Builder(applicationContext, channel.id)
            .setContentTitle("Overlay Service")
            .setTicker("Overlay Service")
            .setContentText("Overlay Service is running")
            .setSmallIcon(android.R.drawable.ic_secure)
            .setOngoing(true)
            .build()
    }


    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.d(LOG_TAG, "onDestroy");
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}