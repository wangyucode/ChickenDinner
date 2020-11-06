package cn.wycode.clientui.helper

import cn.wycode.clientui.Connections
import cn.wycode.clientui.RANDOM_POSITION_MAX
import cn.wycode.clientui.RANDOM_POSITION_MIN
import cn.wycode.control.common.*
import kotlinx.coroutines.*
import org.springframework.stereotype.Component
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin


const val JOYSTICK_STEP_DELAY = 40L
const val JOYSTICK_STEP = 50

/**
 * 4 bit -> 4 direction
 * | . . . . | top | right | bottom | left |
 * | . . . . |  .  |   .   |    .   |  .   |
 */
enum class JoystickDirection(val joystickByte: Byte) {
    NONE(0),
    TOP(0b1000),
    TOP_RIGHT(0b1100),
    RIGHT(0b0100),
    RIGHT_BOTTOM(0b0110),
    BOTTOM(0b0010),
    BOTTOM_LEFT(0b0011),
    LEFT(0b0001),
    LEFT_TOP(0b1001);
}

@Component
class JoystickHelper(val connections: Connections) {

    private var lastJoystickX = -1
    private var lastJoystickY = -1
    private var destJoystickX = 0
    private var destJoystickY = 0

    private var isResetting = false

    private var lastJoystickByte: Byte = 0
    lateinit var joystick: Joystick

    private var stepJob: Job? = null

    fun sendJoystick(joystickByte: Byte) {
        if (lastJoystickX < 0 || lastJoystickY < 0) resetLastJoystick()
        // no changes
        if (lastJoystickByte == joystickByte) return

        // no touch
        if (joystickByte == ZERO_BYTE) {
            connections.sendTouch(HEAD_TOUCH_UP, TOUCH_ID_JOYSTICK, joystick.center.x, joystick.center.y, false)
            resetLastJoystick()
            lastJoystickByte = joystickByte
            // cancel move
            stepJob?.cancel()
            return
        }

        destJoystickX = joystick.center.x
        destJoystickY = joystick.center.y

        val sin45 = (joystick.radius * sin(PI / 4)).toInt()
        when (joystickByte) {
            JoystickDirection.TOP.joystickByte -> destJoystickY -= joystick.radius
            JoystickDirection.TOP_RIGHT.joystickByte -> {
                destJoystickX += sin45
                destJoystickY -= sin45
            }
            JoystickDirection.RIGHT.joystickByte -> destJoystickX += joystick.radius
            JoystickDirection.RIGHT_BOTTOM.joystickByte -> {
                destJoystickX += sin45
                destJoystickY += sin45
            }
            JoystickDirection.BOTTOM.joystickByte -> destJoystickY += joystick.radius
            JoystickDirection.BOTTOM_LEFT.joystickByte -> {
                destJoystickX -= sin45
                destJoystickY += sin45
            }
            JoystickDirection.LEFT.joystickByte -> destJoystickX -= joystick.radius
            JoystickDirection.LEFT_TOP.joystickByte -> {
                destJoystickX -= sin45
                destJoystickY -= sin45
            }
        }

        if (lastJoystickByte == ZERO_BYTE) {
            connections.sendTouch(
                HEAD_TOUCH_DOWN,
                TOUCH_ID_JOYSTICK,
                joystick.center.x,
                joystick.center.y,
                true
            )
            resetLastJoystick()

            stepJob = CoroutineScope(Dispatchers.IO).launch {
                val dx = destJoystickX - lastJoystickX
                val dy = destJoystickY - lastJoystickY
                while (abs(dx) > JOYSTICK_STEP || abs(dy) > JOYSTICK_STEP) {
                    delay(JOYSTICK_STEP_DELAY)
                    sendStep(dx.coerceIn(-JOYSTICK_STEP, JOYSTICK_STEP), dy.coerceIn(-JOYSTICK_STEP, JOYSTICK_STEP))
                    delay(JOYSTICK_STEP_DELAY)
                }
            }
        }

        lastJoystickByte = joystickByte
    }

    private fun sendStep(dx: Int, dy: Int) {
        if (isResetting) return

        lastJoystickX += dx
        lastJoystickY += dy

        lastJoystickX += ThreadLocalRandom.current().nextInt(RANDOM_POSITION_MIN, RANDOM_POSITION_MAX)
        lastJoystickY += ThreadLocalRandom.current().nextInt(RANDOM_POSITION_MIN, RANDOM_POSITION_MAX)

        connections.sendTouch(HEAD_TOUCH_MOVE, TOUCH_ID_JOYSTICK, lastJoystickX, lastJoystickY, false)
    }

    private fun resetLastJoystick() {
        lastJoystickX = joystick.center.x
        lastJoystickY = joystick.center.y
    }
}