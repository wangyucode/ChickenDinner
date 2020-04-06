package cn.wycode.control.server

import android.os.SystemClock
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyCharacterMap
import android.view.KeyEvent
import cn.wycode.control.common.*
import cn.wycode.control.server.wrappers.InputManager.INJECT_INPUT_EVENT_MODE_ASYNC
import cn.wycode.control.server.wrappers.ServiceManager
import java.io.InputStream
import java.nio.ByteBuffer


class Controller(private val inputStream: InputStream) : Thread() {

    private val event: Event = Event(0, 0, 0, 0)
    private val pointBuffer = ByteBuffer.allocate(8)
    private val serviceManager = ServiceManager()

    override fun run() {
        while (true) {
            readEvent()
            injectEvent()
        }
    }

    private fun injectEvent() {
        when (event.type) {
            HEAD_KEY -> injectKey()
        }
    }

    private fun injectKey() {
        when (event.key) {
            KEY_HOME -> injectKey(KeyEvent.KEYCODE_HOME)
            KEY_BACK -> injectKey(KeyEvent.KEYCODE_BACK)
            KEY_VOLUME_UP -> injectKey(KeyEvent.KEYCODE_VOLUME_UP)
            KEY_VOLUME_DOWN -> injectKey(KeyEvent.KEYCODE_VOLUME_DOWN)
        }
    }

    private fun injectKey(keyCode: Int) {
        if (injectKeyEvent(KeyEvent.ACTION_DOWN, keyCode, 0, 0))
            injectKeyEvent(KeyEvent.ACTION_UP, keyCode, 0, 0)

    }

    private fun injectKeyEvent(action: Int, keyCode: Int, repeat: Int, metaState: Int): Boolean {
        val now = SystemClock.uptimeMillis()
        val event = KeyEvent(
            now, now, action, keyCode, repeat, metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
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

data class Event(var type: Byte, var x: Int, var y: Int, var key: Byte)