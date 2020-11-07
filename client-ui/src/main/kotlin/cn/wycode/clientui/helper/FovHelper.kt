package cn.wycode.clientui.helper

import cn.wycode.clientui.Connections
import cn.wycode.control.common.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component
import kotlin.math.abs

const val SCREEN_FOV_EDGE = 100

@Component
class FovHelper(
    val connections: Connections,
    val joystickHelper: JoystickHelper
) {

    var isResetting = false
    var isFovAutoUp = false
    var lastFovMoveTime = 0L
    var movingFovId = TOUCH_ID_MOUSE
    lateinit var resetPosition: Position

    var lastFovX = 0.0
    var lastFovY = 0.0

    var sensitivityX = 1.0
    var sensitivityY = 1.0

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

        connections.sendTouch(HEAD_TOUCH_MOVE, movingFovId, lastFovX.toInt(), lastFovY.toInt(), false)
        lastFovMoveTime = System.currentTimeMillis()
    }

    fun resetTouchAfterGetInCar() {
        if (!isFovAutoUp) {
            connections.sendTouch(HEAD_TOUCH_UP, movingFovId, lastFovX.toInt(), lastFovY.toInt(), false)
            isFovAutoUp = true
        }

        val lastJoystickByte = joystickHelper.lastJoystickByte
        joystickHelper.sendJoystick(ZERO_BYTE)

        CoroutineScope(Dispatchers.IO).launch {
            delay(100)
            isResetting = false
            joystickHelper.sendJoystick(lastJoystickByte)
        }
        isResetting = true
    }

    fun resetLastFov(position: Position) {
        lastFovX = position.x.toDouble()
        lastFovY = position.y.toDouble()
    }

    private fun checkFovEdge(): Boolean {
        return if (abs(lastFovX - resetPosition.x) > resetPosition.x / 2 || abs(lastFovY - resetPosition.y) > resetPosition.y - SCREEN_FOV_EDGE) {
            // up from current position
            val x = lastFovX.toInt()
            val y = lastFovY.toInt()
            val id = movingFovId
            CoroutineScope(Dispatchers.IO).launch {
                delay(100)
                connections.sendTouch(HEAD_TOUCH_UP, id, x, y, false)
            }
            isFovAutoUp = true
            true
        } else {
            false
        }
    }
}