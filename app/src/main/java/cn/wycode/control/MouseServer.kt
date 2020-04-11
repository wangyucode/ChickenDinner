package cn.wycode.control

import android.graphics.Point
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.os.AsyncTask
import android.util.Log
import android.view.View
import cn.wycode.control.common.HEAD_MOUSE_MOVE
import cn.wycode.control.common.LOG_TAG
import cn.wycode.control.common.MOUSE_SOCKET
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MouseServer(private val size: Point, private val pointerView: View) : AsyncTask<Int, Point, Int>() {

    private lateinit var mouseSocket: LocalSocket
    private val inputPointBuffer = ByteBuffer.allocate(8)
    private val outputPointBuffer = ByteBuffer.allocate(8)
    private lateinit var outputStream: OutputStream
    val screenInfoExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    override fun doInBackground(vararg params: Int?): Int {
        val serverSocket = LocalServerSocket(MOUSE_SOCKET)
        mouseSocket = serverSocket.accept()
        val inputStream = mouseSocket.inputStream
        outputStream = mouseSocket.outputStream
        outputStream.write(1)
        Log.d(LOG_TAG, "Mouse client connected!")
        screenInfoExecutor.submit { this.sendScreenInfo() }
        val point = Point()
        while (true) {
            when (inputStream.read().toByte()) {
                HEAD_MOUSE_MOVE -> {
                    inputStream.read(inputPointBuffer.array())
                    point.x = inputPointBuffer.getInt(0)
                    point.y = inputPointBuffer.getInt(4)
                    publishProgress(point)
                }
            }

        }
    }

    override fun onProgressUpdate(vararg points: Point) {
        val point = points[0]
        pointerView.x = point.x.toFloat()
        pointerView.y = point.y.toFloat()
    }

    fun sendScreenInfo() {
        outputPointBuffer.clear()
        outputPointBuffer.putInt(size.x)
        outputPointBuffer.putInt(size.y)
        outputStream.write(outputPointBuffer.array())
        Log.d(LOG_TAG, "sendScreenInfo::$size")
    }
}
