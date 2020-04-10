package cn.wycode.control.client

import cn.wycode.control.common.KEY_BACK
import cn.wycode.control.common.KEY_HOME
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.canvas.Canvas
import javafx.scene.control.Label
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import java.net.URL
import java.util.*


const val RATIO = 3.0

class Controller : Initializable {

    @FXML
    lateinit var canvas: Canvas

    @FXML
    lateinit var hBox: HBox

    @FXML
    lateinit var info: Label

    private val screenInfo = ScreenInfo(0, 0)

    private val initialTask = InitialTask()
    private val connections = Connections()
    private val mouseHandler = MouseHandler(connections)
    private val keyHandler = KeyHandler(connections)

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        info.textProperty().bind(initialTask.messageProperty())

        initialTask.valueProperty().addListener { _, _, value ->
            when (value) {
                3 -> onMouseServiceConnected()
                5 -> onControlServiceConnected()
                6 -> {
                    keyHandler.joystick = initialTask.keymap.joystick
                    initButtons(initialTask.keymap, keyHandler.buttonMap)
                }
            }
        }

        Thread(initialTask).start()
    }

    @FXML
    fun home() {
        if (mouseHandler.controlConnected) connections.sendKey(KEY_HOME)
    }

    @FXML
    fun back() {
        if (mouseHandler.controlConnected) connections.sendKey(KEY_BACK)
    }

    private fun onControlServiceConnected() {
        println("client::connected to control service!")
        mouseHandler.controlConnected = true
        connections.controlOutputStream = initialTask.controlSocket.getOutputStream()
        canvas.scene.addEventHandler(KeyEvent.ANY, keyHandler)
    }

    private fun onMouseServiceConnected() {
        println("client::connected to mouse service!")
        mouseHandler.mouseConnected = true
        connections.mouseOutputStream = initialTask.mouseSocket.getOutputStream()
        val readTask = ReadTask(initialTask.mouseSocket, screenInfo)
        readTask.valueProperty().addListener { _, _, _ ->
            canvas.width = screenInfo.width / RATIO
            canvas.height = screenInfo.height.toDouble() / RATIO

            canvas.scene.window.height = canvas.height
            canvas.scene.window.width = canvas.width + 70

        }
        Thread(readTask).start()
        canvas.addEventHandler(MouseEvent.ANY, mouseHandler)
    }

}
