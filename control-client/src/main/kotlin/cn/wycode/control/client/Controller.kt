package cn.wycode.control.client

import cn.wycode.control.common.*
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.canvas.Canvas
import javafx.scene.control.Label
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import java.io.OutputStream
import java.net.URL
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.HashMap


const val RATIO = 3.0

class Controller : Initializable {

    @FXML
    lateinit var canvas: Canvas

    @FXML
    lateinit var hBox: HBox

    @FXML
    lateinit var info: Label

    private val screenInfo = ScreenInfo(0, 0)

    private val mouseEventExecutor = Executors.newSingleThreadExecutor()
    private val controlEventExecutor = Executors.newSingleThreadExecutor()
    private lateinit var mouseOutputStream: OutputStream
    private lateinit var controlOutputStream: OutputStream

    private val buttonMap = HashMap<KeyCode, Button>()

    private var mouseConnected = false
    private var controlConnected = false

    private var lastKeyDown = KeyCode.UNDEFINED

    /**
     * 1 byte head, 1 byte id, 4 byte x , 4 byte y
     * head  id      x         y
     * | . | . | . . . . | . . . . |
     * touchDown -> head = 1
     * touchMove -> head = 2
     * touchUP   -> head = 3
     */
    private val touchBuffer = ByteBuffer.allocate(10)

    /**
     * 4 byte x , 4 byte y
     *      x         y
     * | . . . . | . . . . |
     */
    private val mouseMoveBuffer = ByteBuffer.allocate(8)

    /**
     * 1 byte head , 1 byte key
     * head  key
     * | . | . |
     */
    private val keyBuffer = ByteBuffer.allocate(2)

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        val initialTask = InitialTask()
        info.textProperty().bind(initialTask.messageProperty())
        initialTask.valueProperty().addListener { _, _, value ->
            when (value) {
                3 -> {
                    println("client::connected to mouse service!")
                    mouseConnected = true
                    mouseOutputStream = initialTask.mouseSocket.getOutputStream()
                    val readTask = ReadTask(initialTask.mouseSocket, screenInfo)
                    readTask.valueProperty().addListener { _, _, _ ->
                        canvas.width = screenInfo.width / RATIO
                        canvas.height = screenInfo.height.toDouble() / RATIO

                        canvas.scene.window.height = canvas.height
                        canvas.scene.window.width = canvas.width + 70

                    }
                    Thread(readTask).start()
                }
                5 -> {
                    println("client::connected to control service!")
                    controlConnected = true
                    controlOutputStream = initialTask.controlSocket.getOutputStream()
                    canvas.scene.setOnKeyPressed {
                        this.keyDown(it)
                    }

                    canvas.scene.setOnKeyReleased {
                        this.keyUp(it)
                    }
                }

                6 -> {
                    initButtons(initialTask.keymap, buttonMap)
                }
            }
        }

        Thread(initialTask).start()
    }


    @FXML
    fun onMousePressed(event: MouseEvent) {
        if (controlConnected) sendTouch(
            HEAD_TOUCH_DOWN,
            TOUCH_ID_MOUSE,
            (event.x * RATIO).toInt(),
            (event.y * RATIO).toInt()
        )
    }

    @FXML
    fun onMouseMoved(event: MouseEvent) {
        if (mouseConnected) sendMouseMove((event.x * RATIO).toInt(), (event.y * RATIO).toInt())
    }

    @FXML
    fun onMouseReleased(event: MouseEvent) {
        if (controlConnected) sendTouch(
            HEAD_TOUCH_UP,
            TOUCH_ID_MOUSE,
            (event.x * RATIO).toInt(),
            (event.y * RATIO).toInt()
        )
    }

    @FXML
    fun onMouseDragged(event: MouseEvent) {
        if (mouseConnected) sendMouseMove((event.x * RATIO).toInt(), (event.y * RATIO).toInt())
        if (controlConnected) sendTouch(
            HEAD_TOUCH_MOVE,
            TOUCH_ID_MOUSE,
            (event.x * RATIO).toInt(),
            (event.y * RATIO).toInt()
        )
    }

    @FXML
    fun home() {
        if (controlConnected) sendKey(KEY_HOME)
    }

    @FXML
    fun back() {
        if (controlConnected) sendKey(KEY_BACK)
    }

    private fun keyDown(keyEvent: KeyEvent) {
        if (!controlConnected) return
        // fix long press
        if (keyEvent.code == lastKeyDown) return
        lastKeyDown = keyEvent.code
        val button = buttonMap[keyEvent.code]
        if (button != null) sendTouch(HEAD_TOUCH_DOWN, TOUCH_ID_BUTTON, button.position.x, button.position.y)
    }

    private fun keyUp(keyEvent: KeyEvent) {
        if (!controlConnected) return
        // fix long press
        if (keyEvent.code == lastKeyDown) lastKeyDown = KeyCode.UNDEFINED
        when (keyEvent.code) {
            KeyCode.PAGE_UP -> sendKey(KEY_VOLUME_UP)
            KeyCode.PAGE_DOWN -> sendKey(KEY_VOLUME_DOWN)
            KeyCode.END -> sendKey(KEY_HOME)
            KeyCode.DELETE -> sendKey(KEY_BACK)
            else -> {
                val button = buttonMap[keyEvent.code]
                if (button != null) sendTouch(HEAD_TOUCH_UP, TOUCH_ID_BUTTON, button.position.x, button.position.y)
            }
        }

    }

    private fun sendKey(key: Byte) {
        println("sendKey::$key")
        controlEventExecutor.submit {
            keyBuffer.clear()
            keyBuffer.put(HEAD_KEY)
            keyBuffer.put(key)
            controlOutputStream.write(keyBuffer.array())
        }
    }

    private fun sendMouseMove(x: Int, y: Int) {
        mouseEventExecutor.submit {
            mouseMoveBuffer.clear()
            mouseMoveBuffer.putInt(x)
            mouseMoveBuffer.putInt(y)
            mouseOutputStream.write(mouseMoveBuffer.array())
        }
    }

    private fun sendTouch(head: Byte, id: Byte, x: Int, y: Int) {
        controlEventExecutor.submit {
            touchBuffer.clear()
            touchBuffer.put(head)
            touchBuffer.put(id)
            touchBuffer.putInt(x)
            touchBuffer.putInt(y)
            controlOutputStream.write(touchBuffer.array())
        }
    }

}
