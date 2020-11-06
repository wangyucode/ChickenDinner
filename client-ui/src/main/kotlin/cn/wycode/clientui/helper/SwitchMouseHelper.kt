package cn.wycode.clientui.helper

import cn.wycode.clientui.Connections
import cn.wycode.control.common.*
import org.springframework.stereotype.Component

@Component
class SwitchMouseHelper(val connections: Connections) {

    var mouseVisible = false

    lateinit var resetPosition: Position

    fun sendSwitchMouse() {
        mouseVisible = !mouseVisible
        val head = if (mouseVisible) {
            connections.sendClearTouch()
            connections.sendMouseMove(resetPosition.x, resetPosition.y)
            fovHandler.stop()
            robot.mouseMove((OFFSET.x + resetPosition.x / RATIO).toInt(), (OFFSET.y + resetPosition.y / RATIO).toInt())
            fovChangeFun(true)
            HEAD_MOUSE_VISIBLE
        } else {
            sendTouch(HEAD_TOUCH_DOWN, movingFovId, resetPosition.x, resetPosition.y, false)
            resetLastFov(resetPosition)
            robot.mouseMove((OFFSET.x + resetPosition.x / RATIO).toInt(), (OFFSET.y + resetPosition.y / RATIO).toInt())
            fovHandler.start()
            fovChangeFun(false)
            HEAD_MOUSE_INVISIBLE
        }
        mouseEventExecutor.execute(WriteRunnable(mouseOutputStream, byteArrayOf(head)))
    }

    fun sendBagOpen(mousePosition: Position) {
        fovHandler.stop()
        mouseVisible = true
        sendTouch(HEAD_TOUCH_UP, movingFovId, lastFovX.toInt(), lastFovY.toInt(), false)
        sendMouseMove(mousePosition.x, mousePosition.y)
        mouseEventExecutor.execute(WriteRunnable(mouseOutputStream, byteArrayOf(HEAD_MOUSE_VISIBLE)))
        robot.mouseMove((OFFSET.x + mousePosition.x / RATIO).toInt(), (OFFSET.y + mousePosition.y / RATIO).toInt())
    }
}