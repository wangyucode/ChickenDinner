package cn.wycode.clientui


import cn.wycode.clientui.handler.KeyHandler
import cn.wycode.clientui.handler.MouseHandler
import cn.wycode.control.common.Position
import kotlinx.coroutines.*
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage
import kotlin.system.exitProcess

var RATIO = 3.0
var SCREEN = Position(0, 0)
var CONTROL_AREA_BOUNDS = Rectangle(0, 0, 0, 0)

@Component
class AwtUi(
    val initializer: Initializer,
    val mouseHandler: MouseHandler,
    val keyHandler: KeyHandler,
    val connections: Connections
) : ApplicationListener<SpringEvent> {

    private final val frame: Frame
    private final val controlCanvas: ControlCanvas
    private final val graphicsDevice: GraphicsDevice
    var isStopping = false
    private final val dotCursor: Cursor

    init {
        System.setProperty("java.awt.headless", "false")
        graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice

        dotCursor = createDotCursor()

        frame = Frame("Android Controller")
        frame.isUndecorated = true
        frame.layout = FlowLayout(FlowLayout.CENTER, 0, 0)
        frame.preferredSize = graphicsDevice.defaultConfiguration.bounds.size
        frame.isFocusable = false
        frame.isResizable = false

        controlCanvas = ControlCanvas()
        controlCanvas.preferredSize = frame.preferredSize
        controlCanvas.isFocusable = true
        controlCanvas.focusTraversalKeysEnabled = false

        frame.add(controlCanvas)
        frame.pack()

        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                println("Window closing")
                frame.dispose()
            }
        })

        robot = Robot()
    }

    private final fun createDotCursor(): Cursor {
        val size = 48
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()

        // Draw a small dot in the center
        g.color = Color.GREEN
        g.fillRect(size / 2 - 1, size / 2 - 1, 2, 2)
        g.dispose()

        val toolkit = Toolkit.getDefaultToolkit()
        val cursor = toolkit.createCustomCursor(image, Point(size / 2, size / 2), "dotCursor")
        return cursor
    }


    fun show() {
        frame.isVisible = true
        controlCanvas.append("start")
        CoroutineScope(Dispatchers.Unconfined + CoroutineExceptionHandler { context, throwable -> onStop("unhandled exception in $context: $throwable") }).launch {
            initializer.initialize(controlCanvas)
        }
    }

    fun onScreenChange() {
        controlCanvas.append("\nScreenChange::$SCREEN")
        // get screen size
        val screenBounds = frame.preferredSize
        RATIO = if (screenBounds.width.toDouble() / screenBounds.height > SCREEN.x.toDouble() / SCREEN.y) {
            SCREEN.y / screenBounds.height.toDouble()
        } else {
            SCREEN.x / screenBounds.width.toDouble()
        }
        val controlWidth = (SCREEN.x / RATIO).toInt()
        val controlHeight = (SCREEN.y / RATIO).toInt()
        val x = (screenBounds.width / 2.0 - controlWidth / 2.0).toInt()
        val y = 0

        CONTROL_AREA_BOUNDS = Rectangle(x, y, controlWidth, controlHeight)
        controlCanvas.drawRect()
    }

    fun onOverlayConnected() {
        controlCanvas.addMouseMotionListener(mouseHandler)
        controlCanvas.addMouseListener(mouseHandler)
        controlCanvas.addFocusListener(keyHandler)
    }

    fun onControlConnected(message: String?) {
        controlCanvas.addKeyListener(keyHandler)
        controlCanvas.append("\n$message")
    }

    fun changeCursor(visible: Boolean) {
        controlCanvas.cursor = if (visible) {
            Cursor.getDefaultCursor()
        } else {
            dotCursor
        }
    }

    fun onStop(message: String?) {
        if (isStopping) return
        isStopping = true
        try {
            connections.closeAll()
        } catch (_: Exception) {
        }
        if (message != null) {
            controlCanvas.append(message)
        }
        CoroutineScope(Dispatchers.Unconfined).launch {
            controlCanvas.append("will exit in 30 seconds")
            delay(30000)
            exitProcess(0)
        }
    }


    override fun onApplicationEvent(event: SpringEvent) {
        when (event.source as String) {
            EVENT_STOP -> onStop(event.message)
            EVENT_SCREEN_CHANGE -> onScreenChange()
            EVENT_OVERLAY_CONNECTED -> onOverlayConnected()
            EVENT_CONTROL_CONNECTED -> onControlConnected(event.message)
            EVENT_CURSOR_VISIBLE -> changeCursor(true)
            EVENT_CURSOR_INVISIBLE -> changeCursor(false)
            EVENT_CLEAR_TEXT_AREA -> controlCanvas.clean()
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