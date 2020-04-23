package cn.wycode.control

import android.graphics.Point
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.os.AsyncTask
import android.util.Log
import android.view.View
import cn.wycode.control.common.*
import com.alibaba.fastjson.JSON
import java.io.InputStream
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
    private var shutdown = false

    override fun doInBackground(vararg params: Int?): Int {
        val serverSocket = LocalServerSocket(MOUSE_SOCKET)
        mouseSocket = serverSocket.accept()
        val inputStream = mouseSocket.inputStream
        outputStream = mouseSocket.outputStream
        outputStream.write(1)
        Log.d(LOG_TAG, "Mouse client connected!")

        screenInfoExecutor.submit { this.sendScreenInfo() }
        while (!shutdown) {
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
            }
            publishProgress(head)
        }
        return 0
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
            HEAD_REPEAT_ENABLE -> {
                keymapView.repeat = true
                keymapView.invalidate()
            }
            HEAD_REPEAT_DISABLE -> {
                keymapView.repeat = false
                keymapView.invalidate()
            }
            HEAD_DROPS_OPEN -> {
                keymapView.dropsOpen = true
                keymapView.invalidate()
            }
            HEAD_DROPS_CLOSE -> {
                keymapView.dropsOpen = false
                keymapView.invalidate()
            }
            HEAD_DRUGS_OPEN -> {
                keymapView.drugsOpen = true
                keymapView.invalidate()
            }
            HEAD_DRUGS_CLOSE -> {
                keymapView.drugsOpen = false
                keymapView.invalidate()
            }
        }
    }

    override fun onPostExecute(result: Int?) {
        super.onPostExecute(result)
        mouseSocket.close()
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    fun sendScreenInfo() {
        outputPointBuffer.clear()
        outputPointBuffer.putInt(size.x)
        outputPointBuffer.putInt(size.y)
        outputStream.write(outputPointBuffer.array())
        Log.d(LOG_TAG, "sendScreenInfo::$size")
    }
}
