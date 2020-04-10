package cn.wycode.control.client

import cn.wycode.control.common.*
import javafx.event.EventHandler
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

class KeyHandler(private val connections: Connections) : EventHandler<KeyEvent> {

    /**
     * 4 bit -> 4 direction
     * | . . . . | top | right | bottom | left |
     * | . . . . |  .  |   .   |    .   |  .   |
     */
    private var joystickByte: Byte = 0

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
            KeyCode.W -> {
                if (joystick == null) return
                joystickByte = joystickByte.or(JoystickDirection.TOP.joystickByte)
                connections.sendJoystick(joystick!!, joystickByte)
            }
            KeyCode.S -> {
                if (joystick == null) return
                joystickByte = joystickByte.or(JoystickDirection.BOTTOM.joystickByte)
                connections.sendJoystick(joystick!!, joystickByte)
            }
            KeyCode.A -> {
                if (joystick == null) return
                joystickByte = joystickByte.or(JoystickDirection.LEFT.joystickByte)
                connections.sendJoystick(joystick!!, joystickByte)
            }
            KeyCode.D -> {
                if (joystick == null) return
                joystickByte = joystickByte.or(JoystickDirection.RIGHT.joystickByte)
                connections.sendJoystick(joystick!!, joystickByte)
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
            KeyCode.W -> {
                if (joystick == null) return
                joystickByte = joystickByte.and(JoystickDirection.TOP.joystickByte.inv())
                connections.sendJoystick(joystick!!, joystickByte)
            }
            KeyCode.S -> {
                if (joystick == null) return
                joystickByte = joystickByte.and(JoystickDirection.BOTTOM.joystickByte.inv())
                connections.sendJoystick(joystick!!, joystickByte)
            }
            KeyCode.A -> {
                if (joystick == null) return
                joystickByte = joystickByte.and(JoystickDirection.LEFT.joystickByte.inv())
                connections.sendJoystick(joystick!!, joystickByte)
            }
            KeyCode.D -> {
                if (joystick == null) return
                joystickByte = joystickByte.and(JoystickDirection.RIGHT.joystickByte.inv())
                connections.sendJoystick(joystick!!, joystickByte)
            }
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

/**
 * 4 bit -> 4 direction
 * | . . . . | top | right | bottom | left |
 * | . . . . |  .  |   .   |    .   |  .   |
 */
enum class JoystickDirection(val joystickByte: Byte) {
    NONE(0),
    TOP(0b1000),
    TOP_RIGHT(0b1100),
    RIGHT(0b0100),
    RIGHT_BOTTOM(0b0110),
    BOTTOM(0b0010),
    BOTTOM_LEFT(0b0011),
    LEFT(0b0001),
    LEFT_TOP(0b1001);
}