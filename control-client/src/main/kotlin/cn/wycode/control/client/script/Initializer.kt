package cn.wycode.control.client.script

import cn.wycode.control.client.ENABLE_LOG
import cn.wycode.control.common.CONTROL_PATH
import cn.wycode.control.common.CONTROL_PORT
import cn.wycode.control.common.CONTROL_SERVER
import cn.wycode.control.common.CONTROL_SOCKET
import java.net.Socket

class Initializer {

    private val runtime = Runtime.getRuntime()
    lateinit var controlSocket: Socket

    fun init(): Initializer {
        println("stop control tunnel")
        stopService()

        println("enable control tunnel")
        enableTunnel(CONTROL_PORT, CONTROL_SOCKET)

        startControlService()

        connectControlService()
        return this
    }

    private fun stopService() {
        try {
            val command = "adb shell am force-stop cn.wycode.control"
            println(command)
            val process = runtime.exec(command)
            process.waitFor()
            val result = process.inputStream.bufferedReader().readText()
            println(result)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun enableTunnel(port: Int, socketName: String): Boolean {
        try {
            val command = "adb forward tcp:$port localabstract:$socketName"
            println(command)
            val process = runtime.exec(command)
            process.waitFor()
            val result = process.inputStream.bufferedReader().readText()
            println(result)
            return result.contains(port.toString()) || "" == result
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun startControlService(): Boolean {
        try {
            var command = "adb shell killall $CONTROL_SERVER"
            println(command)
            var process = runtime.exec(command)
            process.waitFor()
            val result = process.inputStream.bufferedReader().readText()
            println(result)

            //vm-options – VM 选项
            //cmd-dir –父目录 (/system/bin)
            //options –运行的参数 :
            //    –zygote
            //    –start-system-server
            //    –application (api>=14)
            //    –nice-name=nice_proc_name (api>=14)
            //start-class-name –包含main方法的主类  (com.android.commands.am.Am)
            //main-options –启动时候传递到main方法中的参数
            val args = if (ENABLE_LOG) "--debug" else ""
            command =
                "adb shell CLASSPATH=$CONTROL_PATH app_process / --nice-name=$CONTROL_SERVER $CONTROL_SERVER $args"
            println(command)
            process = runtime.exec(command)
            Thread {
                while (process.isAlive) {
                    println(process.inputStream.bufferedReader().readLine())
                }
                println(process.inputStream.bufferedReader().readText())
                println(process.errorStream.bufferedReader().readText())
                controlSocket.close()
                println("all closed!")
            }.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun connectControlService() {
        while (true) {
            controlSocket = Socket("localhost", CONTROL_PORT)
            val signal = controlSocket.getInputStream().read()
            println("control signal->$signal")
            if (signal == 1) break
            Thread.sleep(200)
        }
    }
}