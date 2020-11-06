package cn.wycode.clientui.helper

import cn.wycode.clientui.Connections
import cn.wycode.control.common.HEAD_TOUCH_DOWN
import cn.wycode.control.common.HEAD_TOUCH_MOVE
import cn.wycode.control.common.TOUCH_ID_MOUSE
import cn.wycode.control.common.TOUCH_ID_MOUSE_BACKUP
import org.springframework.stereotype.Component

@Component
class FovHelper(val connections: Connections) {

    var isResetting = false
    var isFovAutoUp = false
    var lastFovMoveTime = 0L
    var movingFovId = TOUCH_ID_MOUSE

    fun sendMoveFov(dx: Int, dy: Int) {
        if (isResetting) return
        // auto up after some time
        if (isFovAutoUp) {

            movingFovId = if (movingFovId == TOUCH_ID_MOUSE) {
                TOUCH_ID_MOUSE_BACKUP
            } else {
                TOUCH_ID_MOUSE
            }

            connections.sendTouch(
                HEAD_TOUCH_DOWN,
                movingFovId,
                resetPosition.x,
                resetPosition.y,
                false
            )

            isFovAutoUp = false
            resetLastFov(resetPosition)
            return
        }

        if (lastFovX == 0.0 || lastFovY == 0.0) {
            resetLastFov(resetPosition)
            return
        }

        lastFovX += dx * sensitivityX
        lastFovY += dy * sensitivityY

        //reach the device edge, ignore this move
        if (checkFovEdge()) return

        sendTouch(HEAD_TOUCH_MOVE, movingFovId, lastFovX.toInt(), lastFovY.toInt(), false)
        lastFovMoveTime = System.currentTimeMillis()
    }
}