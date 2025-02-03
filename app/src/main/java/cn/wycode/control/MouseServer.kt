package cn.wycode.control

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Point
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import cn.wycode.control.common.*
import com.alibaba.fastjson2.JSON
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

class MouseServer(
    private val context: Context
) {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as
                WindowManager

    private val size = Point()
    private lateinit var overlay: ViewGroup
    private lateinit var keymapView: KeymapView
    private lateinit var pointer: View

    private lateinit var mouseSocket: LocalSocket
    private lateinit var serverSocket: LocalServerSocket
    private lateinit var outputStream: OutputStream
    private lateinit var inputStream: InputStream
    private var selectedType = 0
    private var selectedIndex = 0

    private val inputPointBuffer = ByteBuffer.allocate(8)
    private val outputPointBuffer = ByteBuffer.allocate(8)

    private val point = Point()

    private lateinit var keymap: Keymap
    private var shutdown = false

    private var currentOrientation = Configuration.ORIENTATION_PORTRAIT

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun start() {
        serverSocket = LocalServerSocket(MOUSE_SOCKET)
        mouseSocket = serverSocket.accept()
        inputStream = mouseSocket.inputStream
        outputStream = mouseSocket.outputStream
        outputStream.write(1)
        Log.d(LOG_TAG, "Mouse client connected!")

        withContext(Dispatchers.Main) {
            addOverlay()
            withContext(Dispatchers.IO) {
                sendScreenInfo()
            }
        }

        while (!shutdown) {
            val head = read(inputStream)
            withContext(Dispatchers.Main) {
                updateUi(head)
            }
        }

        mouseSocket.close()
        serverSocket.close()
        withContext(Dispatchers.Main) {
            removeOverlay()
        }
        context.stopService(Intent(context, MouseService::class.java))
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private fun read(inputStream: InputStream): Byte {
        val head = inputStream.read().toByte()
        when (head) {
            HEAD_MOUSE_MOVE -> {
                inputStream.read(inputPointBuffer.array())
                point.x = inputPointBuffer.getInt(0)
                point.y = inputPointBuffer.getInt(4)
            }
            HEAD_SHUT_DOWN -> shutdown = true
            HEAD_KEYMAP -> {
                val sizeBuffer = ByteBuffer.allocate(4)
                inputStream.read(sizeBuffer.array())
                val size = sizeBuffer.getInt(0)
                val dataArray = ByteArray(size)
                inputStream.read(dataArray)
                val keymapString = dataArray.toString(StandardCharsets.UTF_8)
                keymap = JSON.parseObject(keymapString, Keymap::class.java)
            }
            HEAD_SELECTED_PROP -> {
                selectedType = inputStream.read()
                selectedIndex = inputStream.read()
            }
        }
        return head
    }

    private fun updateUi(head: Byte) {
        when (head) {
            HEAD_MOUSE_MOVE -> {
                pointer.x = point.x.toFloat()
                pointer.y = point.y.toFloat()
            }
            HEAD_KEYMAP -> {
                keymapView.keymap = keymap
                keymapView.invalidate()
            }
            HEAD_MOUSE_VISIBLE -> pointer.visibility = View.VISIBLE
            HEAD_MOUSE_INVISIBLE -> pointer.visibility = View.INVISIBLE
            HEAD_REPEAT_ENABLE -> {
                keymapView.repeat = true
                keymapView.invalidate()
            }
            HEAD_REPEAT_DISABLE -> {
                keymapView.repeat = false
                keymapView.invalidate()
            }
            HEAD_KEYMAP_VISIBLE -> {
                keymapView.keymapVisible = true
                keymapView.invalidate()
            }
            HEAD_KEYMAP_INVISIBLE -> {
                keymapView.keymapVisible = false
                keymapView.invalidate()
            }
            HEAD_SELECTED_PROP -> {
                keymapView.selectedType = selectedType
                keymapView.selectedIndex = selectedIndex
                keymapView.invalidate()
            }
        }
    }

    private fun sendScreenInfo() {
        outputPointBuffer.clear()
        outputPointBuffer.putInt(size.x)
        outputPointBuffer.putInt(size.y)
        try {
            outputStream.write(outputPointBuffer.array())
        } catch (e: IOException) {
            Log.e(LOG_TAG, "sendScreenInfo", e)
            shutdown = true
        }
        Log.d(LOG_TAG, "sendScreenInfo::$size")
    }


    private fun addOverlay() {
        windowManager.defaultDisplay.getRealSize(size)
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

        overlay = LayoutInflater.from(context.applicationContext).inflate(R.layout.overlay, null) as ViewGroup
        windowManager.addView(overlay, layoutParams)

        keymapView = overlay.getChildAt(0) as KeymapView
        pointer = overlay.getChildAt(1)

        currentOrientation = context.resources.configuration.orientation

        overlay.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val orientation = context.resources.configuration.orientation
            if (currentOrientation != orientation) {
                currentOrientation = orientation
                windowManager.defaultDisplay.getRealSize(size)

                CoroutineScope(Dispatchers.IO).launch {
                    sendScreenInfo()
                }
            }
        }
    }

    private fun removeOverlay() {
        windowManager.removeView(overlay)
    }
}
