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
            if (keycode == KeyEvent.VK_Y) bagHelper.openPosition = button.position
            if (keycode == KeyEvent.VK_4) propsHelper.dropsPosition = button.position
            if (keycode == KeyEvent.VK_5) propsHelper.drugsPosition = button.position
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

    override fun keyTyped(e: KeyEvent) {
        e.consume()
    }

    override fun keyPressed(event: KeyEvent) {
        // fix long press
        if (event.keyCode == lastKeyDown) return
        lastKeyDown = event.keyCode

        when (event.keyCode) {
            KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D -> joystickHelper.pressed(event.keyCode)
            KeyEvent.VK_CONTROL, KeyEvent.VK_F2, KeyEvent.VK_F3 -> return // no need to touch
            KeyEvent.VK_4 -> {
                if (!switchMouseHelper.mouseVisible) {
                    connections.sendTouch(
                        HEAD_TOUCH_UP,
                        fovHelper.movingFovId,
                        fovHelper.lastFovX.toInt(),
                        fovHelper.lastFovY.toInt(),
                        false
                    )
                    fovHandler.isPropsSelecting = true
                }
                propsHelper.selectDrops()
            }

            KeyEvent.VK_5 -> {
                if (!switchMouseHelper.mouseVisible) {
                    connections.sendTouch(
                        HEAD_TOUCH_UP,
                        fovHelper.movingFovId,
                        fovHelper.lastFovX.toInt(),
                        fovHelper.lastFovY.toInt(),
                        false
                    )
                    fovHandler.isPropsSelecting = true
                }
                propsHelper.selectDrugs()
            }

            else -> sendTouch(event.keyCode, HEAD_TOUCH_DOWN)
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
                sendTouch(event.keyCode, HEAD_TOUCH_UP)
            }

            KeyEvent.VK_F -> {
                sendTouch(event.keyCode, HEAD_TOUCH_UP)
                val lastJoystickByte = joystickHelper.lastJoystickByte
                joystickHelper.sendJoystick(ZERO_BYTE)
                joystickHelper.sendJoystick(lastJoystickByte)
                if (!switchMouseHelper.mouseVisible) {
                    connections.sendTouch(
                        HEAD_TOUCH_UP,
                        fovHelper.movingFovId,
                        fovHelper.lastFovX.toInt(),
                        fovHelper.lastFovY.toInt(),
                        false
                    )
                    fovHelper.isFovAutoUp = true
                }
            }

            KeyEvent.VK_1 -> {
                weaponHelper.changeWeapon(1)
                sendTouch(event.keyCode, HEAD_TOUCH_UP)
            }

            KeyEvent.VK_2 -> {
                weaponHelper.changeWeapon(2)
                sendTouch(event.keyCode, HEAD_TOUCH_UP)
            }

            KeyEvent.VK_3 -> {
                weaponHelper.changeWeapon(3)
                sendTouch(event.keyCode, HEAD_TOUCH_UP)
            }

            KeyEvent.VK_4 -> {
                fovHandler.isPropsSelecting = false
                propsHelper.done()
                if (!switchMouseHelper.mouseVisible) {
                    connections.sendTouch(
                        HEAD_TOUCH_DOWN,
                        fovHelper.movingFovId,
                        fovHelper.lastFovX.toInt(),
                        fovHelper.lastFovY.toInt(),
                        false
                    )
                }
            }

            KeyEvent.VK_5 -> {
                fovHandler.isPropsSelecting = false
                propsHelper.done()
                if (!switchMouseHelper.mouseVisible) {
                    connections.sendTouch(
                        HEAD_TOUCH_DOWN,
                        fovHelper.movingFovId,
                        fovHelper.lastFovX.toInt(),
                        fovHelper.lastFovY.toInt(),
                        false
                    )
                }
            }

            KeyEvent.VK_F9 -> connections.sendKeymapVisible(true)
            KeyEvent.VK_F10 -> connections.sendKeymapVisible(false)
            KeyEvent.VK_F11 -> springContext.publishEvent(SpringEvent(EVENT_CLEAR_TEXT_AREA))
            else -> sendTouch(event.keyCode, HEAD_TOUCH_UP)
        }
        event.consume()
    }

    fun sendTouch(keyCode: Int, head: Byte) {
        val buttonWithId = buttonMap[keyCode]
        if (buttonWithId != null) {
            val position = buttonWithId.button.position
            connections.sendTouch(
                head,
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

