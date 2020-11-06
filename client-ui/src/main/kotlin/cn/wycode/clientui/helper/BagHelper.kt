package cn.wycode.clientui.helper

import cn.wycode.clientui.Connections
import cn.wycode.clientui.OFFSET
import cn.wycode.clientui.RATIO
import cn.wycode.clientui.handler.FovHandler
import cn.wycode.control.common.HEAD_MOUSE_VISIBLE
import cn.wycode.control.common.HEAD_TOUCH_UP
import cn.wycode.control.common.Position
import cn.wycode.control.common.TOUCH_ID_MOUSE
import javafx.scene.robot.Robot
import org.springframework.stereotype.Component

@Component
class BagHelper(
    val fovHandler: FovHandler,
    val mouseHelper: SwitchMouseHelper,
    val fovHelper: FovHelper,
    val connections: Connections,
    val robot: Robot
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
        robot.mouseMove(OFFSET.x + openPosition.x / RATIO, OFFSET.y + openPosition.y / RATIO)
    }
}