package cn.wycode.clientui.helper

import cn.wycode.clientui.Connections
import cn.wycode.clientui.Controller.Companion.robot
import cn.wycode.clientui.OFFSET
import cn.wycode.clientui.RATIO
import cn.wycode.clientui.handler.FovHandler
import cn.wycode.control.common.HEAD_MOUSE_VISIBLE
import cn.wycode.control.common.HEAD_TOUCH_UP
import cn.wycode.control.common.Position
import org.springframework.stereotype.Component

@Component
class BagHelper(
    val fovHandler: FovHandler,
    val mouseHelper: SwitchMouseHelper,
    val fovHelper: FovHelper,
    val connections: Connections
) {
    private val openPosition = Position(2691, 267)

    fun sendBagOpen() {
        fovHandler.stop()
        mouseHelper.mouseVisible = true
        connections.sendTouch(
            HEAD_TOUCH_UP,
            fovHelper.movingFovId,
            fovHelper.lastFovX.toInt(),
            fovHelper.lastFovY.toInt(),
            false
        )

        connections.sendMouseMove(openPosition.x, openPosition.y)
        connections.sendOverlayData(byteArrayOf(HEAD_MOUSE_VISIBLE))
        robot.mouseMove((OFFSET.x + openPosition.x / RATIO).toInt(), (OFFSET.y + openPosition.y / RATIO).toInt())
    }
}