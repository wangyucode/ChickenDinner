package cn.wycode.clientui.helper

import cn.wycode.clientui.Connections
import cn.wycode.clientui.handler.FovHandler
import cn.wycode.control.common.HEAD_MOUSE_VISIBLE
import cn.wycode.control.common.HEAD_TOUCH_UP
import cn.wycode.control.common.Position
import cn.wycode.control.common.TOUCH_ID_MOUSE
import org.springframework.stereotype.Component

@Component
class BagHelper(
    val fovHandler: FovHandler,
    val mouseHelper: SwitchMouseHelper,
    val connections: Connections
) {

    private var movingFovId = TOUCH_ID_MOUSE

    fun sendBagOpen(position: Position) {
        fovHandler.stop()
        mouseHelper.mouseVisible = true
        connections.sendTouch(HEAD_TOUCH_UP, movingFovId, lastFovX.toInt(), lastFovY.toInt(), false)
        sendMouseMove(mousePosition.x, mousePosition.y)
        mouseEventExecutor.execute(WriteRunnable(mouseOutputStream, byteArrayOf(HEAD_MOUSE_VISIBLE)))
        robot.mouseMove((OFFSET.x + mousePosition.x / RATIO).toInt(), (OFFSET.y + mousePosition.y / RATIO).toInt())
    }
}