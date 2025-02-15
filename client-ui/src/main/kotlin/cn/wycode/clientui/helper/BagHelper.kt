package cn.wycode.clientui.helper

import cn.wycode.clientui.*
import cn.wycode.clientui.AwtUi.Companion.robot
import cn.wycode.clientui.handler.FovHandler
import cn.wycode.control.common.HEAD_MOUSE_VISIBLE
import cn.wycode.control.common.HEAD_TOUCH_UP
import cn.wycode.control.common.Position
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
class BagHelper(
    val fovHandler: FovHandler,
    val mouseHelper: SwitchMouseHelper,
    val fovHelper: FovHelper,
    val connections: Connections,
    val springContext: ApplicationContext
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
        robot.mouseMove((openPosition.x / RATIO + CONTROL_AREA_BOUNDS.x).toInt(), (openPosition.y / RATIO + CONTROL_AREA_BOUNDS.y).toInt())
        connections.sendMouseMove(openPosition.x, openPosition.y)
        connections.sendOverlayData(byteArrayOf(HEAD_MOUSE_VISIBLE))
        springContext.publishEvent(SpringEvent(EVENT_CURSOR_VISIBLE))
    }
}