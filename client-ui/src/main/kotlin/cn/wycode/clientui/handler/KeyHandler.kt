package cn.wycode.clientui.handler

import cn.wycode.clientui.Connections
import cn.wycode.clientui.helper.*
import cn.wycode.control.common.*
import javafx.event.EventHandler
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import org.springframework.stereotype.Component
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or
import kotlin.math.PI
import kotlin.math.sin

@Component
class KeyHandler(
    val joystickHelper: JoystickHelper,
    val switchMouseHelper: SwitchMouseHelper,
    val connections: Connections,
    val bagHelper: BagHelper,
    val fovHandler: FovHandler,
    val fovHelper: FovHelper,
    val propsHelper: PropsHelper,
    val repeatHelper: RepeatHelper
) : EventHandler<KeyEvent> {

    /**
     * 4 bit -> 4 direction
     * | . . . . | top | right | bottom | left |
     * | . . . . |  .  |   .   |    .   |  .   |
     */
    private var joystickByte: Byte = 0

    private var lastKeyDown = KeyCode.UNDEFINED
    private val buttonMap = LinkedHashMap<KeyCode, ButtonWithId>()
    private lateinit var keymap: Keymap
    private lateinit var resetPosition: Position

    fun initButtons(keymap: Keymap) {
        this.keymap = keymap

        for ((index, button) in keymap.buttons.withIndex()) {
            buttonMap[KeyCode.getKeyCode(button.key)] =
                ButtonWithId(index, button)
            if (button.name == KEY_NAME_SWITCH) resetPosition = button.position
        }
        joystickHelper.joystick = keymap.joystick
        joystickHelper.sin45 = (keymap.joystick.radius * sin(PI / 4)).toInt()
        switchMouseHelper.resetPosition = resetPosition
        fovHelper.resetPosition = resetPosition
        fovHelper.sensitivityX = keymap.sensitivityX
        fovHelper.sensitivityY = keymap.sensitivityY
        fovHandler.mouse = keymap.mouse
        repeatHelper.repeatDelayMin = keymap.repeatDelayMin
        repeatHelper.repeatDelayMax = keymap.repeatDelayMax
        repeatHelper.leftMousePosition = keymap.mouse.left
        propsHelper.drops = keymap.drops
        propsHelper.drugs = keymap.drugs

        buttonMap[KeyCode.DIGIT4] = ButtonWithId(
            buttonMap.size,
            Button("4", keymap.drops.open, KEY_NAME_FOUR)
        )
        buttonMap[KeyCode.DIGIT5] = ButtonWithId(
            buttonMap.size + 1,
            Button("5", keymap.drugs.open, KEY_NAME_FIVE)
        )
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
                joystickHelper.sendJoystick(joystickByte)
            }
            KeyCode.S -> {
                joystickByte = joystickByte.or(JoystickDirection.BOTTOM.joystickByte)
                joystickHelper.sendJoystick(joystickByte)
            }
            KeyCode.A -> {
                joystickByte = joystickByte.or(JoystickDirection.LEFT.joystickByte)
                joystickHelper.sendJoystick(joystickByte)
            }
            KeyCode.D -> {
                joystickByte = joystickByte.or(JoystickDirection.RIGHT.joystickByte)
                joystickHelper.sendJoystick(joystickByte)
            }
            else -> {
                // screen button
                val buttonWithId = buttonMap[event.code]
                if (buttonWithId != null) {
                    val position = buttonWithId.button.position
                    when (buttonWithId.button.name) {
                        KEY_NAME_SWITCH, KEY_NAME_REPEAT -> return
                        else -> {
                            propsHelper.changeDownPosition(buttonWithId.button.name, position)
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
                joystickHelper.sendJoystick(joystickByte)
            }
            KeyCode.S -> {
                joystickByte = joystickByte.and(JoystickDirection.BOTTOM.joystickByte.inv())
                joystickHelper.sendJoystick(joystickByte)
            }
            KeyCode.A -> {
                joystickByte = joystickByte.and(JoystickDirection.LEFT.joystickByte.inv())
                joystickHelper.sendJoystick(joystickByte)
            }
            KeyCode.D -> {
                joystickByte = joystickByte.and(JoystickDirection.RIGHT.joystickByte.inv())
                joystickHelper.sendJoystick(joystickByte)
            }
            else -> {
                val buttonWithId = buttonMap[event.code]
                if (buttonWithId != null) {
                    val position = buttonWithId.button.position
                    when (buttonWithId.button.name) {
                        KEY_NAME_SWITCH -> {
                            switchMouseHelper.sendSwitchMouse()
                            return
                        }
                        KEY_NAME_REPEAT -> {
                            repeatHelper.stopRepeatFire()
                            repeatHelper.enableRepeatFire = !repeatHelper.enableRepeatFire
                            connections.sendEnableRepeat(repeatHelper.enableRepeatFire)
                            return
                        }
                        KEY_NAME_BAG -> if (!switchMouseHelper.mouseVisible) bagHelper.sendBagOpen()
                        KEY_NAME_F -> if (!switchMouseHelper.mouseVisible) fovHelper.resetTouchAfterGetInCar()
                        else -> propsHelper.changeUpPosition(buttonWithId.button.name, position)


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
        if (!focus) {
            joystickHelper.sendJoystick(JoystickDirection.NONE.joystickByte)
            connections.sendClearTouch()
            if (!switchMouseHelper.mouseVisible) switchMouseHelper.sendSwitchMouse()
        }
    }
}

data class ButtonWithId(val id: Int, val button: Button)

