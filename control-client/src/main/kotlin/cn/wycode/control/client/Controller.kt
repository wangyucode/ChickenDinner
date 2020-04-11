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
import javafx.scene.paint.Color
import javafx.stage.Screen
import java.net.URL
import java.util.*

var RATIO = 3.0

const val PANEL_WIDTH = 70

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
                INIT_PROCESS_READ_KEYMAP -> keyHandler.initButtons(initialTask.keymap)
                INIT_PROCESS_CONNECT_MOUSE_SERVICE -> onMouseServiceConnected()
                INIT_PROCESS_CONNECT_CONTROL_SERVICE -> onControlServiceConnected()
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
        println("client::connected to overlay service!")
        mouseHandler.mouseConnected = true
        connections.mouseOutputStream = initialTask.mouseSocket.getOutputStream()

        val screen = Screen.getPrimary()
        val visualBounds = screen.visualBounds

        val graphics = canvas.graphicsContext2D
        graphics.fill = Color.DARKOLIVEGREEN

        val readTask = ReadTask(initialTask.mouseSocket, screenInfo)
        readTask.valueProperty().addListener { _, _, _ ->
            // The client screen is wider than the server
            RATIO =
                if (visualBounds.width - PANEL_WIDTH / visualBounds.height > screenInfo.width.toDouble() / screenInfo.height) {
                    screenInfo.height / visualBounds.height
                } else {
                    screenInfo.width - PANEL_WIDTH / visualBounds.width
                }

            canvas.width = screenInfo.width / RATIO
            canvas.height = screenInfo.height / RATIO

            val window = canvas.scene.window
            window.height = canvas.height
            window.width = canvas.width + PANEL_WIDTH
            window.y = 0.0
            window.x = visualBounds.width / 2 - window.width / 2

            graphics.fillRect(0.0, 0.0, canvas.width, canvas.height)
        }
        Thread(readTask).start()
        canvas.addEventHandler(MouseEvent.ANY, mouseHandler)
    }

}
