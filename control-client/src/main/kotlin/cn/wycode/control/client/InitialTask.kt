package cn.wycode.control.client

import cn.wycode.control.common.*
import com.alibaba.fastjson.JSON
import javafx.application.Platform
import javafx.concurrent.Task
import java.net.Socket

class InitialTask : Task<Int>() {

    private val runtime = Runtime.getRuntime()
    lateinit var mouseSocket: Socket
    lateinit var controlSocket: Socket
    lateinit var keymap: Keymap

    override fun call(): Int {
        updateMessage("start overlay service")
        if (!startMouseService()) return get()
        updateValue(1)

        updateMessage("enable overlay tunnel")
        if (!enableTunnel(MOUSE_PORT, MOUSE_SOCKET)) return get()
        updateValue(2)

        updateMessage("connect to overlay")
        while (true) {
            mouseSocket = Socket("localhost", MOUSE_PORT)
            val signal = mouseSocket.getInputStream().read()
            println("overlay signal->$signal")
            if (signal == 1) break
            Thread.sleep(200)
        }
        updateValue(3)

        updateMessage("enable control tunnel")
        if (!enableTunnel(CONTROL_PORT, CONTROL_SOCKET)) return get()
        updateValue(4)

        updateMessage("start control service")
        Thread(Runnable {
            startController()
        }).start()

        updateMessage("connecting to control")
        while (true) {
            controlSocket = Socket("localhost", CONTROL_PORT)
            val signal = controlSocket.getInputStream().read()
            println("control signal->$signal")
            if (signal == 1) break
            Thread.sleep(200)
        }
        updateValue(5)

        updateMessage("read keymap")
        keymap = JSON.parseObject(javaClass.classLoader.getResource("keymap.json")!!.readText(), Keymap::class.java)
        updateValue(6)
        return get()
    }

    private fun startMouseService(): Boolean {
        try {
            var command = "adb shell am force-stop cn.wycode.control"
            println(command)
            var process = runtime.exec(command)
            process.waitFor()
            var result = process.inputStream.bufferedReader().readText()
            println(result)

            command = "adb shell am start-activity cn.wycode.control/.MainActivity"
            println(command)
            process = runtime.exec(command)
            process.waitFor()
            result = process.inputStream.bufferedReader().readText()
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
            command = "adb shell CLASSPATH=$CONTROL_PATH app_process / --nice-name=$CONTROL_SERVER $CONTROL_SERVER"
            println(command)
            process = runtime.exec(command)
            while (process.isAlive) {
                println(process.inputStream.bufferedReader().readLine())
            }

            println(process.errorStream.bufferedReader().readText())

            Platform.exit()
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
            process.waitFor()
            val result = process.inputStream.bufferedReader().readText()
            println(result)
            return result.contains(port.toString()) || "" == result
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}