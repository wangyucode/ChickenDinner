package cn.wycode.clientui.helper

import cn.wycode.clientui.*
import cn.wycode.clientui.AwtUi.Companion.robot
//import cn.wycode.clientui.Controller.Companion.robot
import cn.wycode.clientui.handler.FovHandler
import cn.wycode.control.common.HEAD_MOUSE_INVISIBLE
import cn.wycode.control.common.HEAD_MOUSE_VISIBLE
import cn.wycode.control.common.HEAD_TOUCH_DOWN
import cn.wycode.control.common.Position
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
class SwitchMouseHelper(
    val connections: Connections,
    val fovHandler: FovHandler,
    val fovHelper: FovHelper,
    val springContext: ApplicationContext
) {

    var mouseVisible = true

    lateinit var resetPosition: Position

    fun sendSwitchMouse() {
        mouseVisible = !mouseVisible
        val head = if (mouseVisible) {
            fovHandler.stop()
            connections.sendClearTouch()
            connections.sendMouseMove(resetPosition.x, resetPosition.y)
            robot.mouseMove((TEXTAREA_BOUNDS.x + resetPosition.x / RATIO).toInt(), (TEXTAREA_BOUNDS.y + resetPosition.y / RATIO).toInt())
            springContext.publishEvent(SpringEvent(EVENT_CURSOR_VISIBLE))
            HEAD_MOUSE_VISIBLE
        } else {
            fovHelper.resetLastFov(resetPosition)
            robot.mouseMove((TEXTAREA_BOUNDS.x + resetPosition.x / RATIO).toInt(), (TEXTAREA_BOUNDS.y + resetPosition.y / RATIO).toInt())
            connections.sendTouch(HEAD_TOUCH_DOWN, fovHelper.movingFovId, resetPosition.x, resetPosition.y, true)
            fovHandler.start()
            springContext.publishEvent(SpringEvent(EVENT_CURSOR_INVISIBLE))
            HEAD_MOUSE_INVISIBLE
        }
        connections.sendOverlayData(byteArrayOf(head))
    }

}