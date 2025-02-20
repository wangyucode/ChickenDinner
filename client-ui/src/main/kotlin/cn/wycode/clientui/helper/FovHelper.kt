package cn.wycode.clientui.helper

import cn.wycode.clientui.Connections
import cn.wycode.control.common.*
import org.springframework.stereotype.Component
import kotlin.math.abs

val SCREEN_FOV_EDGE = Position(500, 300)

@Component
class FovHelper(
    val connections: Connections,
) {
    var isFovAutoUp = false
    var movingFovId = TOUCH_ID_MOUSE
    lateinit var resetPosition: Position

    var lastFovX = 0.0
    var lastFovY = 0.0

    var sensitivityX = 1.0
    var sensitivityY = 1.0

    fun sendMoveFov(dx: Int, dy: Int) {
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
                true
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

        connections.sendTouch(HEAD_TOUCH_MOVE, movingFovId, lastFovX.toInt(), lastFovY.toInt(), false)
    }

    fun resetLastFov(position: Position) {
        lastFovX = position.x.toDouble()
        lastFovY = position.y.toDouble()
    }

    private fun checkFovEdge(): Boolean {
        return if (abs(lastFovX - resetPosition.x) > SCREEN_FOV_EDGE.x || abs(lastFovY - resetPosition.y) > SCREEN_FOV_EDGE.y) {
            connections.sendTouch(HEAD_TOUCH_UP, movingFovId, lastFovX.toInt(), lastFovY.toInt(), false)
            isFovAutoUp = true
            true
        } else {
            false
        }
    }
}