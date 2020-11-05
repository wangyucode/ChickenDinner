package cn.wycode.clientui

import cn.wycode.control.common.*
import com.alibaba.fastjson.JSON
import javafx.scene.control.TextArea
import javafx.scene.layout.Pane
import kotlinx.coroutines.*
import org.springframework.stereotype.Component
import java.lang.Runnable
import java.net.Socket

const val INIT_PROCESS_START_MOUSE_SERVICE = 1
const val INIT_PROCESS_ENABLE_MOUSE_TUNNEL = 2
const val INIT_PROCESS_READ_KEYMAP = 3
const val INIT_PROCESS_CONNECT_MOUSE_SERVICE = 4
const val INIT_PROCESS_ENABLE_CONTROL_TUNNEL = 5
const val INIT_PROCESS_CONNECT_CONTROL_SERVICE = 6

var ENABLE_LOG = false

@Component
class Initializer(val connections: Connections) {

    private val runtime = Runtime.getRuntime()

    lateinit var keymap: Keymap
    lateinit var keymapString: String
    lateinit var textArea: TextArea

    suspend fun initialize(textArea: TextArea, pane: Pane) {
        this.textArea = textArea
        startMouseService()

        textArea.appendText("enable overlay tunnel\n")
        enableTunnel(MOUSE_PORT, MOUSE_SOCKET)

        textArea.appendText("read keymap\n")
        keymapString = javaClass.classLoader.getResource("keymap.json")!!.readText()
        keymap = JSON.parseObject(keymapString, Keymap::class.java)
//        textArea.appendText("repeatMin=${REPEAT_INITIAL_DELAY + keymap.repeatDelayMin}, repeatMax=${REPEAT_INITIAL_DELAY + keymap.repeatDelayMax - 1}")

        textArea.appendText("connect to mouse\n")
        connections.connectToOverlayServer()

        textArea.appendText("enable control tunnel")
        enableTunnel(CONTROL_PORT, CONTROL_SOCKET)

        textArea.appendText("start control service\n")
//        Thread(Runnable {
//            startController()
//        }).start()
//
//        updateMessage("connecting to control")
//        while (true) {
//            controlSocket = Socket("localhost", CONTROL_PORT)
//            val signal = controlSocket.getInputStream().read()
//            appendText("control signal->$signal")
//            if (signal == 1) break
//            Thread.sleep(200)
//        }
//        updateValue(INIT_PROCESS_CONNECT_CONTROL_SERVICE)
//
//
//        return get()
    }

    suspend fun startMouseService() {
        try {
            var command = "adb shell am force-stop cn.wycode.control"
            textArea.appendText("$command\n")
            textArea.appendText("${executeCommand(command)}\n")

            command = "adb shell am start-activity cn.wycode.control/.MainActivity"
            textArea.appendText("$command\n")
            textArea.appendText("${executeCommand(command)}\n")
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

    private fun startController(): Boolean {
        try {
            var command = "adb shell killall $CONTROL_SERVER"
            appendText(command)
            var process = runtime.exec(command)
            process.waitFor()
            val result = process.inputStream.bufferedReader().readText()
            appendText(result)

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
            appendText(command)
            process = runtime.exec(command)
            while (process.isAlive && !isCancelled) {
                appendText(process.inputStream.bufferedReader().readLine())
            }
            appendText(process.inputStream.bufferedReader().readText())
            appendText(process.errorStream.bufferedReader().readText())

            mouseSocket.close()
            controlSocket.close()
            appendText("all closed!")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private suspend fun enableTunnel(port: Int, socketName: String): Boolean {
        try {
            val command = "adb forward tcp:$port localabstract:$socketName"
            textArea.appendText("$command\n")
            textArea.appendText("${executeCommand(command)}\n")
        } catch (e: Exception) {
            textArea.appendText(e.message)
        }
    }
}