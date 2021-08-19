package cn.wycode.clientui.handler

import cn.wycode.clientui.Connections
import cn.wycode.clientui.helper.*
import cn.wycode.control.common.*
import javafx.event.EventHandler
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import org.springframework.stereotype.Component
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
    val repeatHelper: RepeatHelper,
    val weaponHelper: WeaponHelper
) : EventHandler<KeyEvent> {

    private var lastKeyDown = KeyCode.UNDEFINED
    private val buttonMap = LinkedHashMap<KeyCode, ButtonWithId>()
    private lateinit var keymap: Keymap
    private lateinit var resetPosition: Position

    fun initButtons(keymap: Keymap) {
        this.keymap = keymap

        for ((index, button) in keymap.buttons.withIndex()) {
            val keycode = KeyCode.getKeyCode(button.key)
            buttonMap[keycode] = ButtonWithId(index, button)
            if (keycode == KeyCode.CONTROL) resetPosition = button.position
            // move mouse to mark button position
            if (keycode == KeyCode.F1) bagHelper.openPosition = button.position
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
        propsHelper.id = buttonMap.size + 1

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
            KeyCode.W, KeyCode.S, KeyCode.A, KeyCode.D -> joystickHelper.pressed(event.code)
            KeyCode.CONTROL, KeyCode.F2, KeyCode.F3 -> return // no need to touch
            else -> {
                // screen button
                val buttonWithId = buttonMap[event.code]
                if (buttonWithId != null) {
                    val position = buttonWithId.button.position
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
            KeyCode.W, KeyCode.S, KeyCode.A, KeyCode.D -> joystickHelper.released(event.code)
            KeyCode.CONTROL -> switchMouseHelper.sendSwitchMouse()
            KeyCode.F2 -> {
                repeatHelper.stopRepeatFire()
                repeatHelper.enableRepeatFire = !repeatHelper.enableRepeatFire
                repeatHelper.repeatInitialDelay = REPEAT_INITIAL_DELAY
                connections.sendEnableRepeat(repeatHelper.enableRepeatFire)
            }
            KeyCode.F3 -> {
                repeatHelper.stopRepeatFire()
                repeatHelper.enableRepeatFire = !repeatHelper.enableRepeatFire
                repeatHelper.repeatInitialDelay = REPEAT_INITIAL_DELAY_1
                connections.sendEnableRepeat(repeatHelper.enableRepeatFire)
            }
            KeyCode.TAB -> {
                if (!switchMouseHelper.mouseVisible) bagHelper.sendBagOpen()
                sendTouchUp(event.code)
            }
            KeyCode.F -> {
                if (!switchMouseHelper.mouseVisible) fovHelper.resetTouchAfterGetInCar()
                sendTouchUp(event.code)
            }
            KeyCode.DIGIT1 -> {
                weaponHelper.changeWeapon(1)
                sendTouchUp(event.code)
            }
            KeyCode.DIGIT2 -> {
                weaponHelper.changeWeapon(2)
                sendTouchUp(event.code)
            }
            KeyCode.DIGIT3 ->  {
                weaponHelper.changeWeapon(3)
                sendTouchUp(event.code)
            }
            KeyCode.DIGIT4 ->{
                if(!propsHelper.isDropOpen) propsHelper.openDrops()
                sendTouchUp(event.code)
            }
            KeyCode.DIGIT5 ->{
                if(!propsHelper.isDrugOpen) propsHelper.openDrugs()
                sendTouchUp(event.code)
            }
            else -> {
                sendTouchUp(event.code)
            }
        }
    }

    fun sendTouchUp(keyCode: KeyCode) {
        val buttonWithId = buttonMap[keyCode]
        if (buttonWithId != null) {
            val position = buttonWithId.button.position
            connections.sendTouch(
                HEAD_TOUCH_UP,
                (TOUCH_ID_BUTTON + buttonWithId.id).toByte(),
                position.x,
                position.y,
                true
            )
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

