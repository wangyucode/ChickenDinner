package cn.wycode.clientui

import cn.wycode.clientui.handler.KeyHandler
import cn.wycode.clientui.handler.MouseHandler
import cn.wycode.control.common.Position
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.Cursor
import javafx.scene.control.TextArea
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.stage.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component

var RATIO = 3.0
var OFFSET = Position(0, 0)
var SCREEN = Position(0, 0)
var CANVAS = Position(0, 0)

@Component
class Controller(
    val initializer: Initializer,
    val mouseHandler: MouseHandler,
    val keyHandler: KeyHandler
) {

    @FXML
    lateinit var textArea: TextArea

    @FXML
    lateinit var controlPane: Pane


    private val fovChangeFun = fun(visible: Boolean) {
        Platform.runLater {
            controlPane.cursor = if (visible) {
                Cursor.DEFAULT
            } else {
                Cursor.NONE
            }
        }
    }

//    private val connections = Connections(fovChangeFun)
//    private val mouseHandler = MouseHandler(connections)
//    private val keyHandler = KeyHandler(connections)

    @FXML
    fun initialize() {
        CoroutineScope(Dispatchers.Main).launch {
            initializer.initialize(textArea)
        }
//        initialTask.valueProperty().addListener { _, _, value ->
//            when (value) {
//                INIT_PROCESS_READ_KEYMAP -> {
//                    keyHandler.initButtons(initialTask.keymap)
//                    connections.initButtons(initialTask.keymap)
//                }
//                INIT_PROCESS_CONNECT_MOUSE_SERVICE -> onMouseServiceConnected()
//                INIT_PROCESS_CONNECT_CONTROL_SERVICE -> onControlServiceConnected()
//            }
//        }
//
//        Thread(initialTask).start()
    }


//    private fun onControlServiceConnected() {
//        println("client::connected to control service!")
//        mouseHandler.controlConnected = true
//        connections.controlOutputStream = initialTask.controlSocket.getOutputStream()
//        controlPane.scene.addEventHandler(KeyEvent.ANY, keyHandler)
//        controlPane.scene.window.focusedProperty().addListener { _, _, newValue -> keyHandler.focusChange(newValue) }
//    }


    fun onScreenChange() {
        val screen = Screen.getPrimary()
        val screenBounds = screen.bounds
        RATIO = if (screenBounds.width / screenBounds.height > SCREEN.x.toDouble() / SCREEN.y) {
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

    fun clearTextArea() {
        textArea.text = ""
    }

    fun onOverlayConnected() {
        controlPane.addEventHandler(MouseEvent.ANY, mouseHandler)
    }

    fun onControlConnected() {
        controlPane.scene.addEventHandler(KeyEvent.ANY, keyHandler)
    }

}
