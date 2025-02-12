package cn.wycode.clientui.handler

import cn.wycode.clientui.Connections
import cn.wycode.clientui.EVENT_CLEAR_TEXT_AREA
import cn.wycode.clientui.SpringEvent
import cn.wycode.clientui.helper.*
import cn.wycode.control.common.*
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import kotlin.math.PI
import kotlin.math.sin

val SPECIAL_KEY_MAP = HashMap<String, Int>().apply {
    put("Shift", KeyEvent.VK_SHIFT)
    put("Ctrl", KeyEvent.VK_CONTROL)
    put("Tab", KeyEvent.VK_TAB)
    put("Space", KeyEvent.VK_SPACE)
    put("F1", KeyEvent.VK_F1)
    put("Back Quote", KeyEvent.VK_BACK_QUOTE)
}

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
    val weaponHelper: WeaponHelper,
    val springContext: ApplicationContext
) : KeyListener, FocusListener {
    private var lastKeyDown = KeyEvent.VK_UNDEFINED
    private val buttonMap = LinkedHashMap<Int, ButtonWithId>()
    private lateinit var keymap: Keymap
    private lateinit var resetPosition: Position

    fun initButtons(keymap: Keymap) {
        this.keymap = keymap

        for ((index, button) in keymap.buttons.withIndex()) {
            val keycode = keyNameToKeyCode(button.key)
            buttonMap[keycode] = ButtonWithId(index, button)
            if (keycode == KeyEvent.VK_CONTROL) resetPosition = button.position
            // move mouse to mark button position
            if (keycode == KeyEvent.VK_F1) bagHelper.openPosition = button.position
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
        propsHelper.id = buttonMap.size + 1
    }

    fun keyNameToKeyCode(keyName: String): Int {
        if (SPECIAL_KEY_MAP.containsKey(keyName)) {
            return SPECIAL_KEY_MAP[keyName]!!
        }

        // 如果是单个字符，尝试转换
        if (keyName.length == 1) {
            val c: Char = keyName[0]
            return KeyEvent.getExtendedKeyCodeForChar(c.code)
        }
        throw IllegalArgumentException("Unknown key name: $keyName")
    }

    override fun keyTyped(e: KeyEvent) {}

    override fun keyPressed(event: KeyEvent) {
        // fix long press
        if (event.keyCode == lastKeyDown) return
        lastKeyDown = event.keyCode

        when (event.keyCode) {
            KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D -> joystickHelper.pressed(event.keyCode)
            KeyEvent.VK_CONTROL, KeyEvent.VK_F2, KeyEvent.VK_F3, KeyEvent.VK_4 -> return // no need to touch
            else -> {
                // screen button
                val buttonWithId = buttonMap[event.keyCode]

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

    override fun keyReleased(event: KeyEvent) {
        if (event.keyCode == lastKeyDown) lastKeyDown = KeyEvent.VK_UNDEFINED
        when (event.keyCode) {
            KeyEvent.VK_PAGE_UP -> connections.sendKey(KEY_VOLUME_UP)
            KeyEvent.VK_PAGE_DOWN -> connections.sendKey(KEY_VOLUME_DOWN)
            KeyEvent.VK_END -> connections.sendKey(KEY_HOME)
            KeyEvent.VK_DELETE -> connections.sendKey(KEY_BACK)
            KeyEvent.VK_HOME -> connections.sendKey(KEY_HOME)
            KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D -> joystickHelper.released(event.keyCode)
            KeyEvent.VK_CONTROL -> switchMouseHelper.sendSwitchMouse()
            KeyEvent.VK_F2 -> {
                repeatHelper.stopRepeatFire()
                repeatHelper.enableRepeatFire = !repeatHelper.enableRepeatFire
                repeatHelper.repeatInitialDelay = REPEAT_INITIAL_DELAY
                connections.sendEnableRepeat(repeatHelper.enableRepeatFire)
            }
            KeyEvent.VK_F3 -> {
                repeatHelper.stopRepeatFire()
                repeatHelper.enableRepeatFire = !repeatHelper.enableRepeatFire
                repeatHelper.repeatInitialDelay = REPEAT_INITIAL_DELAY_1
                connections.sendEnableRepeat(repeatHelper.enableRepeatFire)
            }
            KeyEvent.VK_TAB -> {
                if (!switchMouseHelper.mouseVisible) bagHelper.sendBagOpen()
                sendTouchUp(event.keyCode)
            }
            KeyEvent.VK_F -> {
                if (!switchMouseHelper.mouseVisible) fovHelper.resetTouchAfterGetInCar()
                sendTouchUp(event.keyCode)
            }
            KeyEvent.VK_1 -> {
                weaponHelper.changeWeapon(1)
                sendTouchUp(event.keyCode)
            }
            KeyEvent.VK_2 -> {
                weaponHelper.changeWeapon(2)
                sendTouchUp(event.keyCode)
            }
            KeyEvent.VK_3 ->  {
                weaponHelper.changeWeapon(3)
                sendTouchUp(event.keyCode)
            }
            KeyEvent.VK_4 ->{
                if(!propsHelper.isDropOpen) propsHelper.openDrops()
            }
            KeyEvent.VK_5 ->{
                if(!propsHelper.isDrugOpen) propsHelper.openDrugs()
            }
            KeyEvent.VK_F9 -> {
                connections.sendKeymapVisible(true)
            }
            KeyEvent.VK_F10 -> {
                connections.sendKeymapVisible(false)
            }
            KeyEvent.VK_F11 -> {
                springContext.publishEvent(SpringEvent(EVENT_CLEAR_TEXT_AREA))
            }
            else -> {
                sendTouchUp(event.keyCode)
            }
        }
    }

    fun sendTouchUp(keyCode: Int) {
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

    override fun focusGained(e: FocusEvent) {}

    override fun focusLost(e: FocusEvent) {
        joystickHelper.sendJoystick(JoystickDirection.NONE.joystickByte)
        connections.sendClearTouch()
        if (!switchMouseHelper.mouseVisible) switchMouseHelper.sendSwitchMouse()
    }
}

data class ButtonWithId(val id: Int, val button: Button)

