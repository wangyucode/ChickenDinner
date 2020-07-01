package cn.wycode.control.client

import cn.wycode.control.common.Position
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.Cursor
import javafx.scene.control.TextArea
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.stage.Screen
import java.net.URL
import java.util.*

var RATIO = 3.0
var OFFSET = Position(0, 0)
var SCREEN = Position(0, 0)
var CANVAS = Position(0, 0)

class Controller : Initializable {

    @FXML
    lateinit var textArea: TextArea

    @FXML
    lateinit var controlPane: Pane

    private val appendTextFun = fun(text: String) {
        Platform.runLater {
            textArea.appendText("\n" + text)
        }
    }

    private val initialTask = InitialTask(appendTextFun)
    private lateinit var readTask: ReadTask
    private val connections = Connections(appendTextFun)
    private val mouseHandler = MouseHandler(connections)
    private val keyHandler = KeyHandler(connections)

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        initialTask.valueProperty().addListener { _, _, value ->
            when (value) {
                INIT_PROCESS_READ_KEYMAP -> {
                    keyHandler.initButtons(initialTask.keymap)
                    connections.initButtons(initialTask.keymap)
                }
                INIT_PROCESS_CONNECT_MOUSE_SERVICE -> onMouseServiceConnected()
                INIT_PROCESS_CONNECT_CONTROL_SERVICE -> onControlServiceConnected()
            }
        }

        Thread(initialTask).start()
    }

    private fun onControlServiceConnected() {
        println("client::connected to control service!")
        mouseHandler.controlConnected = true
        connections.controlOutputStream = initialTask.controlSocket.getOutputStream()
        controlPane.scene.addEventHandler(KeyEvent.ANY, keyHandler)
        controlPane.scene.window.focusedProperty().addListener { _, _, newValue -> keyHandler.focusChange(newValue) }
    }

    private fun onMouseServiceConnected() {
        println("client::connected to overlay service!")
        mouseHandler.mouseConnected = true
        connections.mouseOutputStream = initialTask.mouseSocket.getOutputStream()

        connections.sendKeymap(initialTask.keymapString)

        val screen = Screen.getPrimary()
        val screenBounds = screen.bounds

        readTask = ReadTask(initialTask.mouseSocket, appendTextFun)
        readTask.valueProperty().addListener { _, _, _ ->
            // The client screen is wider than the server
            RATIO =
                if (screenBounds.width / screenBounds.height > SCREEN.x.toDouble() / SCREEN.y) {
                    SCREEN.y / screenBounds.height
                } else {
                    SCREEN.x / screenBounds.width
                }
            textArea.prefWidth = SCREEN.x / RATIO
            textArea.prefHeight = SCREEN.y / RATIO

            controlPane.prefWidth = textArea.prefWidth
            controlPane.prefHeight = textArea.prefHeight

            CANVAS.x = textArea.prefWidth.toInt()
            CANVAS.y = textArea.prefHeight.toInt()

            textArea.layoutX = screenBounds.width / 2 - textArea.prefWidth / 2
            textArea.layoutY = 0.0

            controlPane.layoutX = screenBounds.width / 2 - textArea.prefWidth / 2
            controlPane.layoutY = 0.0

            val window = textArea.scene.window
            window.y = 0.0
            window.x = screenBounds.width / 2 - window.width / 2

            OFFSET.x = (window.x + textArea.layoutX).toInt()
            OFFSET.y = (window.y + textArea.layoutY).toInt()

        }
        Thread(readTask).start()
        controlPane.cursor = Cursor.CROSSHAIR
        controlPane.addEventHandler(MouseEvent.ANY, mouseHandler)
    }

    fun stop() {
        readTask.cancel()
        connections.close()
        initialTask.cancel()
    }

}
