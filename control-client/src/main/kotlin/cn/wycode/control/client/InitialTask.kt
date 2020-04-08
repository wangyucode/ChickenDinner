package cn.wycode.control.client

import cn.wycode.control.common.*
import javafx.concurrent.Task
import java.net.Socket

class InitialTask : Task<Int>() {

    private val runtime = Runtime.getRuntime()
    lateinit var mouseSocket: Socket
    lateinit var controlSocket: Socket

    override fun call(): Int {
        updateMessage("start mouse service")
        if (!startMouseService()) return get()
        updateValue(1)

        Thread.sleep(1000)

        updateMessage("enable mouse tunnel")
        if (!enableTunnel(MOUSE_PORT, MOUSE_SOCKET)) return get()
        updateValue(2)

        updateMessage("connect to mouse")
        mouseSocket = Socket("localhost", MOUSE_PORT)
        if (mouseSocket.getInputStream().read() < 0) return get()
        updateValue(3)

        Thread.sleep(1000)

        updateMessage("enable control tunnel")
        if (!enableTunnel(CONTROL_PORT, CONTROL_SOCKET)) return get()
        updateValue(4)

        updateMessage("start control service")
        Thread(Runnable {
            startController()
        }).start()

        Thread.sleep(2000)

        updateMessage("connect to control")
        controlSocket = Socket("localhost", CONTROL_PORT)
        if (controlSocket.getInputStream().read() < 0) return get()
        updateValue(5)

        return get()
    }

    private fun startMouseService(): Boolean {
        try {
            var command = "adb shell am force-stop cn.wycode.control"
            println(command)
            runtime.exec(command)

            Thread.sleep(1000)
            command = "adb shell am start-activity cn.wycode.control/.MainActivity"
            println(command)
            val process = runtime.exec(command)
            val result = process.inputStream.bufferedReader().readText()
            println(result)
            return !result.contains("Error")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun startController(): Boolean {
        try {
            var command = "adb shell killall $CONTROL_SERVER"
            println(command)
            val result = runtime.exec(command).inputStream.bufferedReader().readText()
            println(result)

            Thread.sleep(1000)
            //vm-options – VM 选项
            //cmd-dir –父目录 (/system/bin)
            //options –运行的参数 :
            //    –zygote
            //    –start-system-server
            //    –application (api>=14)
            //    –nice-name=nice_proc_name (api>=14)
            //start-class-name –包含main方法的主类  (com.android.commands.am.Am)
            //main-options –启动时候传递到main方法中的参数
            command = "adb shell CLASSPATH=$CONTROL_PATH app_process / --nice-name=$CONTROL_SERVER $CONTROL_SERVER"
            println(command)
            val process = runtime.exec(command)
            while (process.isAlive){
                println(process.inputStream.bufferedReader().readLine())
            }

            println(process.errorStream.bufferedReader().readText())
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