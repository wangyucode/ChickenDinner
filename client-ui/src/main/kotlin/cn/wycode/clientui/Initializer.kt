package cn.wycode.clientui

import cn.wycode.clientui.handler.KeyHandler
import cn.wycode.control.common.*
import com.alibaba.fastjson2.JSON
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component


@Component
class Initializer(
    val connections: Connections,
    val keyHandler: KeyHandler
) {

    private val runtime = Runtime.getRuntime()

    lateinit var keymap: Keymap
    lateinit var keymapString: String
    lateinit var controlArea: ControlCanvas

    suspend fun initialize(controlArea: ControlCanvas) {
        this.controlArea = controlArea
        controlArea.append("\nget device name")
        val deviceName = getDeviceName()
        controlArea.append("\ndevice name: $deviceName")

        controlArea.append("\nstart control service")
        startMouseService()

        controlArea.append("\nenable overlay tunnel")
        enableTunnel(MOUSE_PORT, MOUSE_SOCKET)

        val keymapFileName = deviceName.trim() + ".json"

        controlArea.append("\nread keymap: $keymapFileName")
        keymapString = javaClass.classLoader.getResource(keymapFileName)!!.readText()
        keymap = JSON.parseObject(keymapString, Keymap::class.java)
        connections.keymapString = keymapString
        keyHandler.initButtons(keymap)

        controlArea.append("\nconnecting to mouse")
        connections.connectToOverlayServer()

        controlArea.append("\nenable control tunnel")
        enableTunnel(CONTROL_PORT, CONTROL_SOCKET)

        controlArea.append("\nstart control service")
        startController()

        controlArea.append("\nconnecting to control")
        connections.connectToControlServer()
        controlArea.append("\ninitialized!")
    }

    suspend fun getDeviceName(): String {
        val command = "adb shell getprop ro.product.model"
        controlArea.append("\n$command")
        return executeCommand(command)
    }

    suspend fun startMouseService() {
        try {
            var command = "adb shell am force-stop cn.wycode.control"
            controlArea.append("\n$command")
            controlArea.append("\n${executeCommand(command)}")

            command = "adb shell am start-activity cn.wycode.control/.MainActivity"
            controlArea.append("\n$command")
            controlArea.append("\n${executeCommand(command)}")
        } catch (e: Exception) {
            e.message?.let { controlArea.append(it) }
        }
    }

    private suspend fun executeCommand(command: String): String {
        return withContext(Dispatchers.IO) {
            val process = runtime.exec(command)
            process.waitFor()
            process.inputStream.bufferedReader().readText()
        }

    }

    private suspend fun startController(): Boolean {
        try {
            var command = "adb shell killall $CONTROL_SERVER"
            controlArea.append("\n$command")
            controlArea.append("\n${executeCommand(command)}")

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
            controlArea.append("\n$command")
            CoroutineScope(Dispatchers.IO).launch {
                val process = runtime.exec(command)
                while (process.isAlive) {
                    val input = process.inputStream.bufferedReader().readLine()
                    controlArea.append("\n$input")
                }
                val input = process.inputStream.bufferedReader().readLine()
                val error = process.errorStream.bufferedReader().readLine()
                withContext(Dispatchers.Unconfined) {
                    controlArea.append("\n$input")
                    controlArea.append("\n$error")
                }
                connections.closeAll()
                withContext(Dispatchers.Unconfined) {
                    controlArea.append("\nall closed!")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private suspend fun enableTunnel(port: Int, socketName: String) {
        try {
            val command = "adb forward tcp:$port localabstract:$socketName"
            controlArea.append("\n$command")
            controlArea.append("\n${executeCommand(command)}")
        } catch (e: Exception) {
            e.message?.let { controlArea.append(it) }
        }
    }
}