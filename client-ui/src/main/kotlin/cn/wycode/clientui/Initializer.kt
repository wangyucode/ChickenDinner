package cn.wycode.clientui

import cn.wycode.clientui.handler.KeyHandler
import cn.wycode.control.common.*
import com.alibaba.fastjson.JSON
import javafx.scene.control.TextArea
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
    lateinit var textArea: TextArea

    suspend fun initialize(textArea: TextArea) {
        this.textArea = textArea
        textArea.appendText("\nstart control service")
        startMouseService()

        textArea.appendText("\nenable overlay tunnel")
        enableTunnel(MOUSE_PORT, MOUSE_SOCKET)

        textArea.appendText("\nread keymap")
        keymapString = javaClass.classLoader.getResource("keymap.json")!!.readText()
        keymap = JSON.parseObject(keymapString, Keymap::class.java)
        connections.keymapString = keymapString
        keyHandler.initButtons(keymap)

//        textArea.appendText("repeatMin=${REPEAT_INITIAL_DELAY + keymap.repeatDelayMin}, repeatMax=${REPEAT_INITIAL_DELAY + keymap.repeatDelayMax - 1}")

        textArea.appendText("\nconnecting to mouse")
        connections.connectToOverlayServer()

        textArea.appendText("\nenable control tunnel")
        enableTunnel(CONTROL_PORT, CONTROL_SOCKET)

        textArea.appendText("\nstart control service")
        startController()

        textArea.appendText("\nconnecting to control")
        connections.connectToControlServer()
        textArea.appendText("\ninitialized!")
    }

    suspend fun startMouseService() {
        try {
            var command = "adb shell am force-stop cn.wycode.control"
            textArea.appendText("\n$command")
            textArea.appendText("\n${executeCommand(command)}")

            command = "adb shell am start-activity cn.wycode.control/.MainActivity"
            textArea.appendText("\n$command")
            textArea.appendText("\n${executeCommand(command)}")
        } catch (e: Exception) {
            textArea.appendText(e.message)
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
            textArea.appendText("\n$command")
            textArea.appendText("\n${executeCommand(command)}")

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
            textArea.appendText("\n$command")
            CoroutineScope(Dispatchers.IO).launch {
                val process = runtime.exec(command)
                while (process.isAlive) {
                    val input = process.inputStream.bufferedReader().readLine()
                    withContext(Dispatchers.Main) {
                        textArea.appendText("\n$input")
                    }
                }
                val input = process.inputStream.bufferedReader().readLine()
                val error = process.errorStream.bufferedReader().readLine()
                withContext(Dispatchers.Main) {
                    textArea.appendText("\n$input")
                    textArea.appendText("\n$error")
                }
                connections.closeAll()
                withContext(Dispatchers.Main) {
                    textArea.appendText("\nall closed!")
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
            textArea.appendText("\n$command")
            textArea.appendText("\n${executeCommand(command)}")
        } catch (e: Exception) {
            textArea.appendText(e.message)
        }
    }
}