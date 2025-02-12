package cn.wycode.clientui.handler

import cn.wycode.clientui.Connections
import cn.wycode.clientui.helper.FovHelper
import cn.wycode.clientui.helper.PropsHelper
import cn.wycode.clientui.helper.RepeatHelper
import cn.wycode.clientui.helper.WeaponHelper
import cn.wycode.control.common.*
import lc.kra.system.mouse.GlobalMouseHook
import lc.kra.system.mouse.event.GlobalMouseAdapter
import lc.kra.system.mouse.event.GlobalMouseEvent
import org.springframework.stereotype.Component
import java.util.concurrent.ThreadLocalRandom

@Component
class FovHandler(
    val connections: Connections,
    val fovHelper: FovHelper,
    val weaponHelper: WeaponHelper,
    val repeatHelper: RepeatHelper,
    val propsHelper: PropsHelper
) {
    private lateinit var mouseHook: GlobalMouseHook

    lateinit var mouse: Mouse
    lateinit var lastFirePosition: Position
    var isPropsSelecting = false

    private val mouseListener = MouseListener()

    fun start() {
        mouseHook = GlobalMouseHook(true)
        mouseHook.addMouseListener(mouseListener)
    }

    fun stop() {
        mouseHook.shutdownHook()
    }

    inner class MouseListener : GlobalMouseAdapter() {
        override fun mouseMoved(event: GlobalMouseEvent) {
            if(isPropsSelecting){
                propsHelper.moveMouse(event.x, event.y)
            }else{
                fovHelper.sendMoveFov(event.x, event.y)
            }

        }

        override fun mousePressed(event: GlobalMouseEvent) {
            // fire
            if (event.button == GlobalMouseEvent.BUTTON_LEFT) {
                if (repeatHelper.enableRepeatFire && weaponHelper.weaponNumber == 1) {
                    repeatHelper.startRepeatFire()
                    return
                }
                lastFirePosition = repeatHelper.getRandomFirePosition(ThreadLocalRandom.current())
                connections.sendTouch(
                    HEAD_TOUCH_DOWN,
                    TOUCH_ID_MOUSE_LEFT,
                    lastFirePosition.x,
                    lastFirePosition.y,
                    false
                )
            } else if (event.button == GlobalMouseEvent.BUTTON_RIGHT) {
                // aim
                val position = mouse.right
                connections.sendTouch(
                    HEAD_TOUCH_DOWN,
                    TOUCH_ID_MOUSE_RIGHT,
                    position.x,
                    position.y,
                    true
                )
            }
        }

        override fun mouseReleased(event: GlobalMouseEvent) {
            val position: Position
            val id: Byte
            when (event.button) {
                GlobalMouseEvent.BUTTON_LEFT -> {
                    // repeat stop
                    if (repeatHelper.isFireRepeating) {
                        repeatHelper.stopRepeatFire()
                        return
                    }
                    position = lastFirePosition
                    id = TOUCH_ID_MOUSE_LEFT
                }
                GlobalMouseEvent.BUTTON_RIGHT -> {
                    position = mouse.right
                    id = TOUCH_ID_MOUSE_RIGHT
                }
                else -> {
                    return
                }
            }
            // normal click
            connections.sendTouch(
                HEAD_TOUCH_UP,
                id,
                position.x,
                position.y,
                true
            )
        }

        override fun mouseWheel(event: GlobalMouseEvent) {}
    }
}


