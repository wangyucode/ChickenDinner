package cn.wycode.clientui

import cn.wycode.clientui.handler.KeyHandler
import cn.wycode.clientui.handler.MouseHandler
import cn.wycode.control.common.Position
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
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import java.awt.Robot

var RATIO = 3.0
var OFFSET = Position(0, 0)
var SCREEN = Position(0, 0)
var CANVAS = Position(0, 0)

@Component
class Controller(
    val initializer: Initializer,
    val mouseHandler: MouseHandler,
    val keyHandler: KeyHandler,
    val connections: Connections
) : ApplicationListener<SpringEvent> {

    @FXML
    lateinit var textArea: TextArea

    @FXML
    lateinit var controlPane: Pane

    @FXML
    fun initialize() {
        CoroutineScope(Dispatchers.Main).launch {
            initializer.initialize(textArea)
        }
        robot = Robot()
    }


    fun onScreenChange() {
        textArea.appendText("\nScreenChange::$SCREEN")
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

    fun changeCursor(visible: Boolean) {
        controlPane.cursor = if (visible) {
            Cursor.DEFAULT
        } else {
            Cursor.NONE
        }
    }

    fun onOverlayConnected() {
        controlPane.addEventHandler(MouseEvent.ANY, mouseHandler)
    }

    fun onControlConnected() {
        controlPane.scene.addEventHandler(KeyEvent.ANY, keyHandler)
        controlPane.scene.window.focusedProperty().addListener { _, _, newValue -> keyHandler.focusChange(newValue) }
    }

    override fun onApplicationEvent(event: SpringEvent) {
        when (event.source as String) {
            EVENT_STOP -> connections.closeAll()
            EVENT_SCREEN_CHANGE -> onScreenChange()
            EVENT_OVERLAY_CONNECTED -> onOverlayConnected()
            EVENT_CONTROL_CONNECTED -> onControlConnected()
            EVENT_CURSOR_VISIBLE -> changeCursor(true)
            EVENT_CURSOR_INVISIBLE -> changeCursor(false)
        }
    }

    fun showKeymap() {
        connections.sendKeymapVisible(true)
    }

    fun hideKeymap() {
        connections.sendKeymapVisible(false)
    }

    companion object {
        lateinit var robot: Robot
    }
}
