package cn.wycode.clientui

import cn.wycode.control.common.*
import kotlinx.coroutines.*
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.ThreadLocalRandom

const val RANDOM_POSITION_MIN = -10
const val RANDOM_POSITION_MAX = 10

@Component
class Connections(val springContext: ApplicationContext) {

    lateinit var keymapString: String
    lateinit var overlaySocket: Socket
    lateinit var controlSocket: Socket

    /**
     * 1 byte head , 4 byte x , 4 byte y
     * | head |    x    |    y    |
     * |  .   | . . . . | . . . . |
     */
    private val mouseMoveBuffer = ByteBuffer.allocate(9)

    /**
     * 1 byte head, 1 byte id, 4 byte x , 4 byte y
     * | head | id |    x    |    y    |
     * |  .   |  . | . . . . | . . . . |
     * touchDown -> head = 1
     * touchMove -> head = 2
     * touchUP   -> head = 3
     */
    private val touchBuffer = ByteBuffer.allocate(10)

    /**
     * 1 byte head, 1 byte key
     * | head | key |
     * |  .   |  .  |
     */
    private val keyBuffer = ByteBuffer.allocate(2)


    @Volatile
    var isOverlayClosed = false

    @Volatile
    var isControlClosed = false

    fun connectToOverlayServer() {
        CoroutineScope(Dispatchers.IO).launch {
            var signal = 0
            while (!isControlClosed && signal != 1) {
                overlaySocket = Socket("localhost", MOUSE_PORT)
                signal = overlaySocket.inputStream.read()
                delay(200)
            }

            if (signal == 1) {
                springContext.publishEvent(SpringEvent(EVENT_OVERLAY_CONNECTED))

                sendKeymap()

                // read screen change
                val buffer = ByteArray(8)
                while (!isOverlayClosed) {
                    overlaySocket.inputStream.read(buffer)
                    SCREEN.x = ByteBuffer.wrap(buffer).getInt(0)
                    SCREEN.y = ByteBuffer.wrap(buffer).getInt(4)
                    springContext.publishEvent(SpringEvent(EVENT_SCREEN_CHANGE))
                }
            }
        }
    }

    private fun sendKeymap() {
        val data = keymapString.toByteArray()
        // |   .  | . . . . | . . . (...) . . . |
        // | head |  size   |        data       |
        val buffer = ByteBuffer.allocate(data.size + 5)
        buffer.put(HEAD_KEYMAP)
        buffer.putInt(data.size)
        buffer.put(data)
        sendOverlayData(buffer.array())
    }

    suspend fun connectToControlServer() {
        withContext(Dispatchers.IO) {
            var signal = 0
            while (!isControlClosed && signal != 1) {
                controlSocket = Socket("localhost", CONTROL_PORT)
                signal = controlSocket.getInputStream().read()
                delay(200)
            }

            if (signal == 1) {
                val version = controlSocket.getInputStream().read()
                springContext.publishEvent(SpringEvent(EVENT_CONTROL_CONNECTED, "Control Server Version: $version"))
            }
        }
    }

    fun closeAll() {
        isOverlayClosed = true
        isControlClosed = true
        overlaySocket.outputStream.write(byteArrayOf(HEAD_SHUT_DOWN))
        controlSocket.outputStream.write(byteArrayOf(HEAD_SHUT_DOWN))
        overlaySocket.close()
        controlSocket.close()
    }

    fun sendMouseMove(x: Int, y: Int) {
        mouseMoveBuffer.clear()
        mouseMoveBuffer.put(HEAD_MOUSE_MOVE)
        mouseMoveBuffer.putInt(x)
        mouseMoveBuffer.putInt(y)
        sendOverlayData(mouseMoveBuffer.array().copyOf())
    }

    fun sendOverlayData(data: ByteArray) {
        CoroutineScope(Dispatchers.IO).launch {
            overlaySocket.outputStream.write(data)
        }
    }

    fun sendControlData(data: ByteArray) {
        CoroutineScope(Dispatchers.IO).launch {
            controlSocket.outputStream.write(data)
        }
    }


    fun sendTouch(head: Byte, id: Byte, x: Int, y: Int, shake: Boolean) {
        var shakeX = x
        var shakeY = y
        if (shake) {
            val random = ThreadLocalRandom.current()
            shakeX = x + random.nextInt(RANDOM_POSITION_MIN, RANDOM_POSITION_MAX)
            shakeY = y + random.nextInt(RANDOM_POSITION_MIN, RANDOM_POSITION_MAX)
        }

        touchBuffer.clear()
        touchBuffer.put(head)
        touchBuffer.put(id)
        touchBuffer.putInt(shakeX)
        touchBuffer.putInt(shakeY)
        sendControlData(touchBuffer.array().copyOf())
    }

    fun sendKey(key: Byte) {
        keyBuffer.clear()
        keyBuffer.put(HEAD_KEY)
        keyBuffer.put(key)
        sendControlData(keyBuffer.array().copyOf())
    }

    fun sendClearTouch() {
        if (!isControlClosed) {
            sendControlData(byteArrayOf(HEAD_CLEAR_TOUCH))
        }
    }

    fun sendEnableRepeat(enableRepeatFire: Boolean) {
        val head = if (enableRepeatFire) HEAD_REPEAT_ENABLE else HEAD_REPEAT_DISABLE
        sendOverlayData(byteArrayOf(head))
    }

    fun sendKeymapVisible(visible: Boolean) {
        val head = if (visible) HEAD_KEYMAP_VISIBLE else HEAD_KEYMAP_INVISIBLE
        sendOverlayData(byteArrayOf(head))
    }


}