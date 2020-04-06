@file:JvmName("ControlServer")
package cn.wycode.control_server

import android.net.LocalServerSocket
import android.net.LocalSocket
import cn.wycode.control_server.utils.Ln
import java.io.File
import java.io.IOException
import java.net.Socket

const val CONTROL_PATH = "/data/local/tmp/controller.jar"
const val CONTROL_SOCKET = "control-socket"

class Server {

    private lateinit var controlSocket: LocalSocket

    @Throws(IOException::class)
    fun start() {
        val serverSocket = LocalServerSocket(CONTROL_SOCKET)
        controlSocket = serverSocket.accept()
        controlSocket.outputStream.write(1)
    }

}

fun main(args: Array<String>) {
    Thread.setDefaultUncaughtExceptionHandler { t, e ->
        Ln.e("Exception on thread $t", e)
    }
    //deleteSelf()
    Server().start()
}

fun deleteSelf() {
    File(CONTROL_PATH).delete();
}


