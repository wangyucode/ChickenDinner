package cn.wycode.control

import android.graphics.Point
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.os.AsyncTask
import java.io.OutputStream
import java.nio.ByteBuffer

class MouseServer(private val callBack: ServerCallBack) : AsyncTask<Int, Point, Int>() {

    private lateinit var mouseSocket: LocalSocket

    override fun doInBackground(vararg params: Int?): Int {
        val serverSocket = LocalServerSocket(SOCKET_NAME)
        mouseSocket = serverSocket.accept()
        val inputStream = mouseSocket.inputStream
        val outputStream = mouseSocket.outputStream
        outputStream.write(1)
        callBack.onConnected(outputStream)
        val buffer = ByteArray(8)
        val point = Point()
        while (true) {
            if (inputStream.read(buffer) > 0) {
                point.x = ByteBuffer.wrap(buffer).getInt(0)
                point.y = ByteBuffer.wrap(buffer).getInt(4)
                println("MouseServer::$point")
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