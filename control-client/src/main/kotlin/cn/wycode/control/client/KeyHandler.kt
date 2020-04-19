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

    private val fovHandler = FovHandler(connections)

    fun initButtons(keymap: Keymap) {
        this.keymap = keymap
        for ((index, button) in keymap.buttons.withIndex()) {
            buttonMap[KeyCode.getKeyCode(button.key)] = ButtonWithId(index, button)
        }
        buttonMap[KeyCode.DIGIT4] = ButtonWithId(buttonMap.size, Button("4", keymap.drops.open, KEY_NAME_FOUR))
        buttonMap[KeyCode.DIGIT5] = ButtonWithId(buttonMap.size + 1, Button("5", keymap.drugs.open, KEY_NAME_FIVE))
        buttonMap[KeyCode.DIGIT6] = ButtonWithId(buttonMap.size + 2, Button("6", keymap.drugs.buttons[5], KEY_NAME_SIX))

        fovHandler.mouse = keymap.mouse
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
                    var position = buttonWithId.button.position
                    when (buttonWithId.button.name) {
                        KEY_NAME_SWITCH, KEY_NAME_REPEAT -> return
                        KEY_NAME_ONE, KEY_NAME_TWO, KEY_NAME_THREE -> {
                            val index = buttonWithId.button.name!!.toInt()
                            if (connections.isDropsOpen) {
                                position = keymap.drops.buttons[index]
                            } else if (connections.isDrugsOpen) {
                                position = keymap.drugs.buttons[index]
                            }
                        }
                        KEY_NAME_FOUR -> {
                            position = when {
                                connections.isDropsOpen -> keymap.drops.buttons[0]
                                connections.isDrugsOpen -> keymap.drugs.buttons[4]
                                else -> keymap.drops.open
                            }
                        }
                        KEY_NAME_FIVE -> {
                            position = when {
                                connections.isDropsOpen -> keymap.drops.buttons[4]
                                connections.isDrugsOpen -> keymap.drugs.buttons[0]
                                else -> keymap.drugs.open
                            }
                        }
                        KEY_NAME_SIX -> {
                            if (connections.isDrugsOpen) {
                                position = keymap.drugs.buttons[5]
                            } else {
                                return
                            }
                        }
                    }
                    connections.sendTouch(
                        HEAD_TOUCH_DOWN,
                        (TOUCH_ID_BUTTON + buttonWithId.id).toByte(),
                        position.x,
                        position.y,
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
                    var position = buttonWithId.button.position
                    when (buttonWithId.button.name) {
                        KEY_NAME_SWITCH -> {
                            if(connections.mouseVisible) fovHandler.start() else fovHandler.stop()
                            connections.sendSwitchMouse()
                            return
                        }
                        KEY_NAME_REPEAT -> {
                            connections.enableRepeatFire = !connections.enableRepeatFire
                            connections.sendEnableRepeat()
                            return
                        }
                        KEY_NAME_BAG -> if (!connections.mouseVisible){
                            fovHandler.stop()
                            connections.sendBagOpen(Position(2691, 267))
                        }
                        KEY_NAME_ONE, KEY_NAME_TWO, KEY_NAME_THREE -> {
                            val index = buttonWithId.button.name!!.toInt()
                            when {
                                connections.isDropsOpen -> {
                                    position = keymap.drops.buttons[index]
                                    connections.sendDropsOpen(false)
                                }
                                connections.isDrugsOpen -> {
                                    position = keymap.drugs.buttons[index]
                                    connections.sendDrugsOpen(false)
                                }
                                else -> connections.weaponNumber = index
                            }
                        }
                        KEY_NAME_FOUR -> {
                            position = when {
                                connections.isDropsOpen -> {
                                    connections.sendDropsOpen(false)
                                    keymap.drops.buttons[0]
                                }
                                connections.isDrugsOpen -> {
                                    connections.sendDrugsOpen(false)
                                    keymap.drugs.buttons[4]
                                }
                                else -> {
                                    connections.sendDropsOpen(true)
                                    connections.sendDrugsOpen(false)
                                    keymap.drops.open
                                }
                            }
                        }
                        KEY_NAME_FIVE -> {
                            position = when {
                                connections.isDropsOpen -> {
                                    connections.sendDropsOpen(false)
                                    keymap.drops.buttons[4]
                                }
                                connections.isDrugsOpen -> {
                                    connections.sendDrugsOpen(false)
                                    keymap.drugs.buttons[0]
                                }
                                else -> {
                                    connections.sendDrugsOpen(true)
                                    connections.sendDropsOpen(false)
                                    keymap.drugs.open
                                }
                            }
                        }
                        KEY_NAME_SIX -> {
                            if (connections.isDrugsOpen) {
                                connections.sendDrugsOpen(false)
                                position = keymap.drugs.buttons[5]
                            } else {
                                return
                            }
                        }
                    }
                    connections.sendTouch(
                        HEAD_TOUCH_UP,
                        (TOUCH_ID_BUTTON + buttonWithId.id).toByte(),
                        position.x,
                        position.y,
                        true
                    )
                }
            }
        }
    }

    fun focusChange(focus: Boolean) {
        if(!focus && !connections.mouseVisible){
            fovHandler.stop()
            connections.sendJoystick(JoystickDirection.NONE.joystickByte)
            connections.sendClearTouch()
            if(!connections.mouseVisible) connections.sendSwitchMouse()
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