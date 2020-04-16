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
    private val buttonMap = LinkedHashMap<KeyCode, ButtonWithId>()
    private lateinit var keymap: Keymap

    fun initButtons(keymap: Keymap) {
        this.keymap = keymap
        for ((index, button) in keymap.buttons.withIndex()) {
            buttonMap[KeyCode.getKeyCode(button.key)] = ButtonWithId(index, button)
        }
    }

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
                joystickByte = joystickByte.or(JoystickDirection.TOP.joystickByte)
                connections.sendJoystick(joystickByte)
            }
            KeyCode.S -> {
                joystickByte = joystickByte.or(JoystickDirection.BOTTOM.joystickByte)
                connections.sendJoystick(joystickByte)
            }
            KeyCode.A -> {
                joystickByte = joystickByte.or(JoystickDirection.LEFT.joystickByte)
                connections.sendJoystick(joystickByte)
            }
            KeyCode.D -> {
                joystickByte = joystickByte.or(JoystickDirection.RIGHT.joystickByte)
                connections.sendJoystick(joystickByte)
            }
            else -> {
                // screen button
                val buttonWithId = buttonMap[event.code]
                if (buttonWithId != null) {
                    when (buttonWithId.button.name) {
                        KEY_NAME_SWITCH, KEY_NAME_REPEAT -> return
                    }
                    connections.sendTouch(
                        HEAD_TOUCH_DOWN,
                        (TOUCH_ID_BUTTON + buttonWithId.id).toByte(),
                        buttonWithId.button.position.x,
                        buttonWithId.button.position.y,
                        true
                    )
                }
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
            KeyCode.DIGIT4 -> {
                if (!connections.isDropsOpen) {
                    connections.sendDropsOpen(true)
                } else {
                    val mainPosition = keymap.drops.buttons[0]
                    connections.sendTouch(HEAD_TOUCH_DOWN, TOUCH_ID_DROPS, mainPosition.x, mainPosition.y, true)
                    connections.sendTouch(HEAD_TOUCH_UP, TOUCH_ID_DROPS, mainPosition.x, mainPosition.y, true)
                    connections.sendDropsOpen(false)
                }
            }
            KeyCode.W -> {
                joystickByte = joystickByte.and(JoystickDirection.TOP.joystickByte.inv())
                connections.sendJoystick(joystickByte)
            }
            KeyCode.S -> {
                joystickByte = joystickByte.and(JoystickDirection.BOTTOM.joystickByte.inv())
                connections.sendJoystick(joystickByte)
            }
            KeyCode.A -> {
                joystickByte = joystickByte.and(JoystickDirection.LEFT.joystickByte.inv())
                connections.sendJoystick(joystickByte)
            }
            KeyCode.D -> {
                joystickByte = joystickByte.and(JoystickDirection.RIGHT.joystickByte.inv())
                connections.sendJoystick(joystickByte)
            }
            else -> {
                val buttonWithId = buttonMap[event.code]
                if (buttonWithId != null) {
                    when (buttonWithId.button.name) {
                        KEY_NAME_SWITCH -> {
                            connections.sendSwitchMouse()
                            return
                        }
                        KEY_NAME_REPEAT -> {
                            connections.enableRepeatFire = !connections.enableRepeatFire
                            connections.sendEnableRepeat()
                            return
                        }
                        KEY_NAME_BAG -> if (!connections.mouseVisible) connections.sendBagOpen(Position(2103, 359))
                        KEY_NAME_ONE -> connections.weaponNumber = 1
                        KEY_NAME_TWO -> connections.weaponNumber = 2
                    }
                    connections.sendTouch(
                        HEAD_TOUCH_UP,
                        (TOUCH_ID_BUTTON + buttonWithId.id).toByte(),
                        buttonWithId.button.position.x,
                        buttonWithId.button.position.y,
                        true
                    )
                }
            }
        }
    }
}

data class ButtonWithId(val id: Int, val button: Button)

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