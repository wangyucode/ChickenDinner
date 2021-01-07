package cn.wycode.clientui.helper

import cn.wycode.clientui.Connections
import cn.wycode.clientui.RANDOM_POSITION_MAX
import cn.wycode.clientui.RANDOM_POSITION_MIN
import cn.wycode.control.common.*
import javafx.scene.input.KeyCode
import kotlinx.coroutines.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.ThreadLocalRandom
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or
import kotlin.math.abs


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

    /**
     * 4 bit -> 4 direction
     * | . . . . | top | right | bottom | left |
     * | . . . . |  .  |   .   |    .   |  .   |
     */
    private var joystickByte: Byte = 0

    var lastJoystickX = -1
    var lastJoystickY = -1
    private var destJoystickX = 0
    private var destJoystickY = 0

    var lastJoystickByte: Byte = 0
    lateinit var joystick: Joystick
    var sin45: Int = 0

    private var stepJob: Job? = null

    @Autowired
    lateinit var fovHelper: FovHelper

    fun pressed(keyCode: KeyCode) {
        joystickByte = when (keyCode) {
            KeyCode.W -> joystickByte.or(JoystickDirection.TOP.joystickByte)
            KeyCode.S -> joystickByte.or(JoystickDirection.BOTTOM.joystickByte)
            KeyCode.A -> joystickByte.or(JoystickDirection.LEFT.joystickByte)
            KeyCode.D -> joystickByte.or(JoystickDirection.RIGHT.joystickByte)
            else -> return
        }
        sendJoystick(joystickByte)
    }

    fun released(keyCode: KeyCode) {
        joystickByte = when (keyCode) {
            KeyCode.W -> joystickByte.and(JoystickDirection.TOP.joystickByte.inv())
            KeyCode.S -> joystickByte.and(JoystickDirection.BOTTOM.joystickByte.inv())
            KeyCode.A -> joystickByte.and(JoystickDirection.LEFT.joystickByte.inv())
            KeyCode.D -> joystickByte.and(JoystickDirection.RIGHT.joystickByte.inv())
            else -> return
        }
        sendJoystick(joystickByte)
    }

    fun sendJoystick(joystickByte: Byte) {
        if (lastJoystickX < 0 || lastJoystickY < 0) resetLastJoystick()
        // no changes
        if (lastJoystickByte == joystickByte) return

        // no touch
        if (joystickByte == ZERO_BYTE) {
            connections.sendTouch(HEAD_TOUCH_UP, TOUCH_ID_JOYSTICK, lastJoystickX, lastJoystickY, false)
            resetLastJoystick()
            lastJoystickByte = ZERO_BYTE
            // cancel move
            stepJob?.cancel()
            return
        }

        destJoystickX = joystick.center.x
        destJoystickY = joystick.center.y

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
        }
        stepJob?.cancel()
        stepJob = CoroutineScope(Dispatchers.IO).launch {
            var dx: Int
            var dy: Int
            do {
                dx = destJoystickX - lastJoystickX
                dy = destJoystickY - lastJoystickY
                sendStep(dx.coerceIn(-JOYSTICK_STEP, JOYSTICK_STEP), dy.coerceIn(-JOYSTICK_STEP, JOYSTICK_STEP))
                delay(JOYSTICK_STEP_DELAY)
            } while (abs(dx) > JOYSTICK_STEP || abs(dy) > JOYSTICK_STEP)
        }

        lastJoystickByte = joystickByte
    }

    private fun sendStep(dx: Int, dy: Int) {
        if (fovHelper.isResetting) return

        lastJoystickX += dx
        lastJoystickY += dy

        lastJoystickX += ThreadLocalRandom.current().nextInt(RANDOM_POSITION_MIN, RANDOM_POSITION_MAX)
        lastJoystickY += ThreadLocalRandom.current().nextInt(RANDOM_POSITION_MIN, RANDOM_POSITION_MAX)

        connections.sendTouch(HEAD_TOUCH_MOVE, TOUCH_ID_JOYSTICK, lastJoystickX, lastJoystickY, false)
    }

    fun resetLastJoystick() {
        lastJoystickX = joystick.center.x
        lastJoystickY = joystick.center.y
    }
}