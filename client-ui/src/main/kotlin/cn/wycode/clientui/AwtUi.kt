package cn.wycode.clientui


import cn.wycode.clientui.handler.KeyHandler
import cn.wycode.clientui.handler.MouseHandler
import cn.wycode.control.common.Position
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import java.awt.*
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import kotlin.system.exitProcess

var RATIO = 3.0
var SCREEN = Position(0, 0)
var TEXTAREA_BOUNDS = Rectangle(0, 0, 0, 0)

@Component
class AwtUi(
    val initializer: Initializer,
    val mouseHandler: MouseHandler,
    val keyHandler: KeyHandler,
    val connections: Connections
) : ApplicationListener<SpringEvent> {

    final var frame: Frame
    final var textArea: TextArea
    final var graphicsDevice: GraphicsDevice

    init {
        System.setProperty("java.awt.headless", "false")
        graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
        frame = Frame("Android Controller")
        frame.isUndecorated = true
        frame.background = Color(0, 44, 34)
        frame.layout = FlowLayout()
        frame.preferredSize = graphicsDevice.defaultConfiguration.bounds.size
        frame.focusTraversalKeysEnabled = false

        textArea = TextArea()
        textArea.preferredSize = Dimension(1280, 720)
        textArea.background = Color(0, 188, 125)
        textArea.isEditable = false
        textArea.isFocusable = false
        textArea.cursor = Cursor.getDefaultCursor()

        frame.add(textArea)
        frame.pack()

        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                println("Window closing")
                frame.dispose()
            }
        })

        robot = Robot()
    }

    fun onScreenChange() {
        textArea.append("\nScreenChange::$SCREEN")
        // get screen size
        val screenBounds = graphicsDevice.defaultConfiguration.bounds
        RATIO = if (screenBounds.width.toDouble() / screenBounds.height > SCREEN.x.toDouble() / SCREEN.y) {
            SCREEN.y / (screenBounds.height - 10.0) // 10 is the width of the border
        } else {
            SCREEN.x / (screenBounds.width - 10.0)
        }
        textArea.preferredSize = Dimension((SCREEN.x / RATIO).toInt(), (SCREEN.y / RATIO).toInt())

//
//        controlPane.prefWidth = textArea.prefWidth
//        controlPane.prefHeight = textArea.prefHeight
//
//        CANVAS.x = textArea.prefWidth.toInt()
//        CANVAS.y = textArea.prefHeight.toInt()
//
//        textArea.layoutX = screenBounds.width / 2 - textArea.prefWidth / 2
//        textArea.layoutY = 0.0
//
//        controlPane.layoutX = screenBounds.width / 2 - textArea.prefWidth / 2
//        controlPane.layoutY = 0.0
//
//        val window = textArea.scene.window
//        window.y = 0.0
//        window.x = screenBounds.width / 2 - window.width / 2
//
//        OFFSET.x = (window.x + textArea.layoutX).toInt()
//        OFFSET.y = (window.y + textArea.layoutY).toInt()
        frame.pack()
        frame.revalidate()
        TEXTAREA_BOUNDS = textArea.bounds
    }

    fun onOverlayConnected() {
        textArea.addMouseMotionListener(mouseHandler)
        textArea.addMouseListener(mouseHandler)
        frame.addFocusListener(keyHandler)
        frame.addMouseMotionListener(object : MouseMotionListener {
            fun checkEdge(e: MouseEvent) {
                if (e.x < textArea.bounds.x) {
                    robot.mouseMove(textArea.bounds.x, e.y)
                }
                if (e.x > textArea.bounds.x + textArea.bounds.width) {
                    robot.mouseMove(textArea.bounds.x + textArea.bounds.width, e.y)
                }
                if (e.y < textArea.bounds.y) {
                    robot.mouseMove(e.x, textArea.bounds.y)
                }
                if (e.y > textArea.bounds.y + textArea.bounds.height) {
                    robot.mouseMove(e.x, textArea.bounds.y + textArea.bounds.height)
                }
            }

            override fun mouseDragged(e: MouseEvent) {

            }

            override fun mouseMoved(e: MouseEvent) {
                if (mouseHandler.mouseHelper.mouseVisible) checkEdge(e)
            }
        })
    }

    fun onControlConnected() {
        frame.addKeyListener(keyHandler)
    }

    fun changeCursor(visible: Boolean) {
        textArea.cursor = if (visible) {
            Cursor.getDefaultCursor()
        } else {
            Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
        }
    }

    fun onStop(message: String?) {
        try { connections.closeAll() } catch (_: Exception) {}
        textArea.append("\n")
        textArea.append(message)
        textArea.append("\n")
        CoroutineScope(Dispatchers.Unconfined).launch {
            for (i in 10 downTo 1) {
                textArea.append("\nwill exit in $i seconds")
                Thread.sleep(1000)
            }
            exitProcess(0)
        }
    }

    fun show() {
        frame.isVisible = true
        CoroutineScope(Dispatchers.Unconfined + CoroutineExceptionHandler { context, throwable -> onStop("unhandled exception in $context: $throwable") }).launch {
            initializer.initialize(textArea)
        }
    }

    override fun onApplicationEvent(event: SpringEvent) {
        when (event.source as String) {
            EVENT_STOP -> onStop(event.message)
            EVENT_SCREEN_CHANGE -> onScreenChange()
            EVENT_OVERLAY_CONNECTED -> onOverlayConnected()
            EVENT_CONTROL_CONNECTED -> onControlConnected()
            EVENT_CURSOR_VISIBLE -> changeCursor(true)
            EVENT_CURSOR_INVISIBLE -> changeCursor(false)
            EVENT_CLEAR_TEXT_AREA -> textArea.text = ""
        }
    }

    companion object {
        lateinit var robot: Robot
    }
}

class SpringEvent(source: String, val message: String? = null) : ApplicationEvent(source)

const val EVENT_STOP = "EVENT_STOP"
const val EVENT_CURSOR_VISIBLE = "EVENT_CURSOR_VISIBLE"
const val EVENT_CURSOR_INVISIBLE = "EVENT_CURSOR_INVISIBLE"
const val EVENT_OVERLAY_CONNECTED = "EVENT_OVERLAY_CONNECTED"
const val EVENT_CONTROL_CONNECTED = "EVENT_CONTROL_CONNECTED"
const val EVENT_SCREEN_CHANGE = "EVENT_SCREEN_CHANGE"
const val EVENT_CLEAR_TEXT_AREA = "EVENT_CLEAR_TEXT_AREA"