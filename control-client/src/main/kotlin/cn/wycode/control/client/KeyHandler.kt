package cn.wycode.control.client

import cn.wycode.control.common.*
import javafx.event.EventHandler
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent

class KeyHandler(private val connections: Connections) : EventHandler<KeyEvent> {

    private var lastKeyDown = KeyCode.UNDEFINED
    val buttonMap = HashMap<KeyCode, Button>()
    var joystick: Joystick? = null

    override fun handle(event: KeyEvent) {
        when (event.eventType) {
            KeyEvent.KEY_PRESSED -> keyPressed(event)
            KeyEvent.KEY_RELEASED -> keyReleased(event)
        }
    }

    private fun keyPressed(event: KeyEvent) {
        // fix long press
        if (event.code == lastKeyDown) return
        lastKeyDown = event.code

        when (event.code) {
            KeyCode.W, KeyCode.S, KeyCode.A, KeyCode.D -> {
                if(joystick==null) return
                //TODO
            }
            else -> {
                // screen button
                val button = buttonMap[event.code]
                if (button != null) return connections.sendTouch(
                    HEAD_TOUCH_DOWN,
                    TOUCH_ID_BUTTON,
                    button.position.x,
                    button.position.y,
                    true
                )
            }
        }
    }

    private fun keyReleased(event: KeyEvent) {
        // fix long press
        if (event.code == lastKeyDown) lastKeyDown = KeyCode.UNDEFINED
        when (event.code) {
            KeyCode.PAGE_UP -> connections.sendKey(KEY_VOLUME_UP)
            KeyCode.PAGE_DOWN -> connections.sendKey(KEY_VOLUME_DOWN)
            KeyCode.END -> connections.sendKey(KEY_HOME)
            KeyCode.DELETE -> connections.sendKey(KEY_BACK)
            KeyCode.HOME -> connections.sendKey(KEY_HOME)
            else -> {
                val button = buttonMap[event.code]
                if (button != null) connections.sendTouch(
                    HEAD_TOUCH_UP,
                    TOUCH_ID_BUTTON,
                    button.position.x,
                    button.position.y,
                    true
                )
            }
        }
    }
}