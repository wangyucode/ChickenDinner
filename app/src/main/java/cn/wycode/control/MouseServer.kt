package cn.wycode.control

import android.graphics.Point
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.os.AsyncTask
import android.util.Log
import android.view.View
import cn.wycode.control.common.*
import com.alibaba.fastjson.JSON
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MouseServer(
    private val size: Point,
    private val pointerView: View,
    private val keymapView: KeymapView
) : AsyncTask<Int, Byte, Int>() {

    private lateinit var mouseSocket: LocalSocket
    private lateinit var outputStream: OutputStream

    private val inputPointBuffer = ByteBuffer.allocate(8)
    private val outputPointBuffer = ByteBuffer.allocate(8)

    val screenInfoExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val point = Point()

    private lateinit var keymap: Keymap

    override fun doInBackground(vararg params: Int?): Int {
        val serverSocket = LocalServerSocket(MOUSE_SOCKET)
        mouseSocket = serverSocket.accept()
        val inputStream = mouseSocket.inputStream
        outputStream = mouseSocket.outputStream
        outputStream.write(1)
        Log.d(LOG_TAG, "Mouse client connected!")

        screenInfoExecutor.submit { this.sendScreenInfo() }

        while (true) {
            val head = inputStream.read().toByte()
            when (head) {
                HEAD_MOUSE_MOVE -> {
                    inputStream.read(inputPointBuffer.array())
                    point.x = inputPointBuffer.getInt(0)
                    point.y = inputPointBuffer.getInt(4)
                }
                HEAD_KEYMAP -> {
                    val sizeBuffer = ByteBuffer.allocate(4)
                    inputStream.read(sizeBuffer.array())
                    val size = sizeBuffer.getInt(0)
                    val dataArray = ByteArray(size)
                    inputStream.read(dataArray)
                    val keymapString = dataArray.toString(StandardCharsets.UTF_8)
                    keymap = JSON.parseObject(keymapString, Keymap::class.java)
                }
            }
            publishProgress(head)
        }
    }

    override fun onProgressUpdate(vararg values: Byte?) {
        when (values[0]) {
            HEAD_MOUSE_MOVE -> {
                pointerView.x = point.x.toFloat()
                pointerView.y = point.y.toFloat()
            }
            HEAD_KEYMAP -> {
                keymapView.keymap = keymap
                keymapView.invalidate()
            }
            HEAD_MOUSE_VISIBLE -> pointerView.visibility = View.VISIBLE
            HEAD_MOUSE_INVISIBLE -> pointerView.visibility = View.INVISIBLE
        }
    }

    fun sendScreenInfo() {
        outputPointBuffer.clear()
        outputPointBuffer.putInt(size.x)
        outputPointBuffer.putInt(size.y)
        outputStream.write(outputPointBuffer.array())
        Log.d(LOG_TAG, "sendScreenInfo::$size")
    }
}
