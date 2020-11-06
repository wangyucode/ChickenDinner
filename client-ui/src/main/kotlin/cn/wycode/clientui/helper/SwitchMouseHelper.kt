package cn.wycode.clientui.helper

import cn.wycode.clientui.Connections
import cn.wycode.clientui.Controller
import cn.wycode.clientui.OFFSET
import cn.wycode.clientui.RATIO
import cn.wycode.clientui.handler.FovHandler
import cn.wycode.control.common.*
import javafx.scene.robot.Robot
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class SwitchMouseHelper(
    val connections: Connections,
    val fovHandler: FovHandler,
    val fovHelper: FovHelper,
    val robot: Robot
) {
    @Autowired
    lateinit var controller: Controller
    var mouseVisible = false

    lateinit var resetPosition: Position

    fun sendSwitchMouse() {
        mouseVisible = !mouseVisible
        val head = if (mouseVisible) {
            connections.sendClearTouch()
            connections.sendMouseMove(resetPosition.x, resetPosition.y)
            fovHandler.stop()
            robot.mouseMove(OFFSET.x + resetPosition.x / RATIO, OFFSET.y + resetPosition.y / RATIO)
            controller.changeCursor(true)
            HEAD_MOUSE_VISIBLE
        } else {
            connections.sendTouch(HEAD_TOUCH_DOWN, fovHelper.movingFovId, resetPosition.x, resetPosition.y, false)
            fovHelper.resetLastFov(resetPosition)
            robot.mouseMove(OFFSET.x + resetPosition.x / RATIO, OFFSET.y + resetPosition.y / RATIO)
            fovHandler.start()
            controller.changeCursor(false)
            HEAD_MOUSE_INVISIBLE
        }
        connections.sendOverlayData(byteArrayOf(head))
    }

}