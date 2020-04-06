package cn.wycode.control

import android.graphics.Point
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.os.AsyncTask
import cn.wycode.control.common.MOUSE_SOCKET
import java.io.OutputStream
import java.nio.ByteBuffer

class MouseServer(private val callBack: ServerCallBack) : AsyncTask<Int, Point, Int>() {

    private lateinit var mouseSocket: LocalSocket
    private val pointBuffer = ByteBuffer.allocate(8)

    override fun doInBackground(vararg params: Int?): Int {
        val serverSocket = LocalServerSocket(MOUSE_SOCKET)
        mouseSocket = serverSocket.accept()
        val inputStream = mouseSocket.inputStream
        val outputStream = mouseSocket.outputStream
        outputStream.write(1)
        callBack.onConnected(outputStream)
        val point = Point()
        while (true) {
            if (inputStream.read(pointBuffer.array()) > 0) {
                point.x = pointBuffer.getInt(0)
                point.y = pointBuffer.getInt(4)
                publishProgress(point)
            }
        }
    }

    override fun onProgressUpdate(vararg points: Point) {
        callBack.onUpdate(points[0])
    }
}

interface ServerCallBack {
    fun onConnected(outputStream: OutputStream)
    fun onUpdate(point: Point)
}