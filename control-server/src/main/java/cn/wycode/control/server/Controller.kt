package cn.wycode.control.server

import android.os.SystemClock
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyCharacterMap
import android.view.KeyEvent
import cn.wycode.control.common.*
import cn.wycode.control.server.utils.Ln
import cn.wycode.control.server.wrappers.InputManager.INJECT_INPUT_EVENT_MODE_ASYNC
import cn.wycode.control.server.wrappers.ServiceManager
import java.io.InputStream
import java.nio.ByteBuffer

var shouldLogEvent = ENABLE_LOG

class Controller(private val inputStream: InputStream) {

    private val event: Event = Event(0, 0, 0, 0, 0)
    private var lastEvent = event.copy()

    /**
     * 1 byte id, 4 byte x , 4 byte y
     *   id      x         y
     * | . | . . . . | . . . . |
     * touchDown -> head = 1
     * touchMove -> head = 2
     * touchUP   -> head = 3
     */
    private val touchBuffer = ByteBuffer.allocate(9)
    private val serviceManager = ServiceManager()
    private val touchConverter = TouchConverter()
    private var shutdown = false

    fun run() {
        while (!shutdown) {
            readEvent()
            injectEvent()
        }
    }

    private fun injectEvent() {
        when (event.type) {
            HEAD_KEY -> injectKey()
            HEAD_CLEAR_TOUCH -> {
                for (i in touchConverter.localIdToEvent.size() - 1 downTo 0) {
                    val event = touchConverter.localIdToEvent.valueAt(i)
                    if (event.id == TOUCH_ID_JOYSTICK) continue
                    this.event.type = HEAD_TOUCH_UP
                    this.event.id = event.id
                    this.event.x = event.x
                    this.event.y = event.y
                    injectTouch()
                }
            }

            HEAD_SHUT_DOWN -> return
            else -> injectTouch()
        }
    }

    private fun injectTouch() {
        val motionEvent = touchConverter.convert(this.event)
        if (motionEvent != null) {
            injectEvent(motionEvent)
            motionEvent.recycle()
        } else if (shouldLogEvent) {
            lastEvent.id = 0
        }
    }


    private fun injectKey() {
        val keycode = when (event.key) {
            KEY_HOME -> KeyEvent.KEYCODE_HOME
            KEY_BACK -> KeyEvent.KEYCODE_BACK
            KEY_VOLUME_UP -> KeyEvent.KEYCODE_VOLUME_UP
            KEY_VOLUME_DOWN -> KeyEvent.KEYCODE_VOLUME_DOWN
            else -> KeyEvent.KEYCODE_UNKNOWN
        }
        if (injectKeyEvent(KeyEvent.ACTION_DOWN, keycode))
            injectKeyEvent(KeyEvent.ACTION_UP, keycode)
    }


    private fun injectKeyEvent(action: Int, keyCode: Int): Boolean {
        val now = SystemClock.uptimeMillis()
        val event = KeyEvent(
            now, now, action, keyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
            InputDevice.SOURCE_KEYBOARD
        )
        return injectEvent(event)
    }

    private fun injectEvent(event: InputEvent): Boolean {
        return serviceManager.inputManager.injectInputEvent(event, INJECT_INPUT_EVENT_MODE_ASYNC)
    }

    private fun readEvent() {
        event.type = inputStream.read().toByte()
        event.key = 0
        when (event.type) {
            HEAD_KEY -> {
                event.key = inputStream.read().toByte()
            }

            HEAD_SHUT_DOWN -> {
                shutdown = true
                return
            }

            HEAD_CLEAR_TOUCH -> {
                return
            }

            else -> {
                touchBuffer.clear()
                inputStream.read(touchBuffer.array())
                event.id = touchBuffer.get()
                event.x = touchBuffer.getInt(1)
                event.y = touchBuffer.getInt(5)
            }
        }
        shouldLogEvent = ENABLE_LOG && (
                event.type == HEAD_TOUCH_UP ||
                        event.type == HEAD_TOUCH_DOWN ||
                        lastEvent.type != event.type ||
                        lastEvent.id != event.id ||
                        event.key != ZERO_BYTE
                )
        if (shouldLogEvent) lastEvent = event.copy()
        if (shouldLogEvent) Ln.d("receive->${event}")
    }
}

data class Event(var type: Byte, var id: Byte, var x: Int, var y: Int, var key: Byte)