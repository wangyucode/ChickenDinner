package cn.wycode.control.server

import android.os.SystemClock
import android.view.*
import android.view.MotionEvent.PointerCoords
import android.view.MotionEvent.PointerProperties
import cn.wycode.control.common.*
import cn.wycode.control.server.wrappers.InputManager.INJECT_INPUT_EVENT_MODE_ASYNC
import cn.wycode.control.server.wrappers.ServiceManager
import java.io.InputStream
import java.nio.ByteBuffer


const val MAX_POINTERS = 10

class Controller(private val inputStream: InputStream) : Thread() {

    private val event: Event = Event(0, 0, 0, 0, 0)
    private val pointBuffer = ByteBuffer.allocate(8)
    private val serviceManager = ServiceManager()
    private var touchCount = 1
    private var lastTouchDown = 0L
    private val pointerProperties = arrayOfNulls<MotionEvent.PointerProperties>(MAX_POINTERS)
    private val pointerCoords = arrayOfNulls<MotionEvent.PointerCoords>(MAX_POINTERS)

    init {
        for (i in 0 until MAX_POINTERS) {
            val props = PointerProperties()
            props.id = i
            props.toolType = MotionEvent.TOOL_TYPE_FINGER
            val coords = PointerCoords()
            coords.orientation = -1f
            coords.size = 128f
            pointerProperties[i] = props
            pointerCoords[i] = coords
        }
    }

    override fun run() {
        while (true) {
            readEvent()
            injectEvent()
        }
    }

    private fun injectEvent() {
        when (event.type) {
            HEAD_KEY -> injectKey()
            else -> injectTouch()
        }
    }

    private fun injectTouch() {
        val now = SystemClock.uptimeMillis()

        var action = MotionEvent.ACTION_MOVE
        if (event.type == HEAD_TOUCH_DOWN) {
            if (touchCount == 1) {
                lastTouchDown = now
                action = MotionEvent.ACTION_DOWN
            } else {
                action = MotionEvent.ACTION_POINTER_DOWN or (event.index.toInt() shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
            }
            touchCount++
        } else if (event.type == HEAD_TOUCH_UP) {
            // secondary pointers must use ACTION_POINTER_* ORed with the pointerIndex
            action = if (touchCount > 1) {
                MotionEvent.ACTION_POINTER_UP or (event.index.toInt() shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
            } else {
                MotionEvent.ACTION_UP
            }
            touchCount--
        }

        val coords = pointerCoords[event.index.toInt()]
        coords!!.x = event.x.toFloat()
        coords.y = event.y.toFloat()

        val event = MotionEvent.obtain(
            lastTouchDown,
            now,
            action,
            touchCount,
            pointerProperties,
            pointerCoords,
            0,
            0,
            1f,
            1f,
            -1,
            0,
            InputDevice.SOURCE_TOUCHSCREEN,
            0
        )

        injectEvent(event)
        event.recycle()
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
        return serviceManager.inputManager.injectInputEvent(event, INJECT_INPUT_EVENT_MODE_ASYNC);
    }

    private fun readEvent() {
        event.type = inputStream.read().toByte()
        if (event.type == HEAD_KEY) {
            event.key = inputStream.read().toByte()
        } else {
            inputStream.read(pointBuffer.array())
            event.x = pointBuffer.getInt(0)
            event.y = pointBuffer.getInt(4)
        }
    }
}

data class Event(var type: Byte, var index: Byte, var x: Int, var y: Int, var key: Byte)