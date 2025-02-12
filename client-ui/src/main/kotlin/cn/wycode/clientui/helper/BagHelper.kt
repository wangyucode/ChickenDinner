package cn.wycode.clientui.helper

import cn.wycode.clientui.AwtUi.Companion.robot
import cn.wycode.clientui.Connections
import cn.wycode.clientui.RATIO
import cn.wycode.clientui.TEXTAREA_BOUNDS
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
    lateinit var openPosition: Position

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
        robot.mouseMove((openPosition.x / RATIO + TEXTAREA_BOUNDS.x).toInt(), (openPosition.y / RATIO + TEXTAREA_BOUNDS.y).toInt())
        connections.sendMouseMove(openPosition.x, openPosition.y)
        connections.sendOverlayData(byteArrayOf(HEAD_MOUSE_VISIBLE))
    }
}