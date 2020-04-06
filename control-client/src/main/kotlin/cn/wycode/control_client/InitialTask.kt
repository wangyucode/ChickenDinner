package cn.wycode.control_client

import javafx.concurrent.Task
import java.net.Socket

const val MOUSE_PORT = 15940
const val CONTROL_PORT = 15941
const val MOUSE_SOCKET = "mouse-socket"
const val CONTROL_SOCKET = "control-socket"
const val CONTROL_PATH = "/data/local/tmp/controller.jar"
const val CONTROL_SERVER = "cn.wycode.control_server.ControlServer"

class InitialTask : Task<Int>() {

    private val runtime = Runtime.getRuntime()
    lateinit var mouseSocket: Socket
    lateinit var controlSocket: Socket

    override fun call(): Int {
        updateMessage("enable mouse tunnel")
        if (!enableTunnel(MOUSE_PORT, MOUSE_SOCKET)) return get()
        updateValue(1)

        updateMessage("connect to mouse")
        mouseSocket = Socket("localhost", MOUSE_PORT)
        if (mouseSocket.getInputStream().read() < 0) return get()
        updateValue(2)

        Thread.sleep(1000)

        updateMessage("enable control tunnel")
        if (!enableTunnel(CONTROL_PORT, CONTROL_SOCKET)) return get()
        updateValue(3)

        updateMessage("start controller")
        Thread(Runnable {
            startController()
        }).start()

        Thread.sleep(2000)

        updateMessage("connect to control")
        controlSocket = Socket("localhost", CONTROL_PORT)
        if (controlSocket.getInputStream().read() < 0) return get()
        updateValue(4)

        return get()
    }

    private fun startController(): Boolean {
        try {
            val command = "adb shell CLASSPATH=$CONTROL_PATH app_process / $CONTROL_SERVER"
            println(command)
            val process = runtime.exec(command)
            val error = process.errorStream.bufferedReader().readText()
            val result = process.inputStream.bufferedReader().readText()
            println(result)
            println(error)
            return !result.contains("ERROR")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun enableTunnel(port: Int, socketName: String): Boolean {
        try {
            val command = "adb forward tcp:$port localabstract:$socketName"
            println(command)
            val process = runtime.exec(command)
            val error = process.errorStream.bufferedReader().readText()
            val result = process.inputStream.bufferedReader().readText()
            println(result)
            println(error)
            return result.contains(port.toString()) || "" == result
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}