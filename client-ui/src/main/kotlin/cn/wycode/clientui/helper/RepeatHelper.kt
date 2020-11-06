package cn.wycode.clientui.helper

import cn.wycode.clientui.Connections
import cn.wycode.clientui.RANDOM_POSITION_MAX
import cn.wycode.clientui.RANDOM_POSITION_MIN
import cn.wycode.control.common.HEAD_TOUCH_DOWN
import cn.wycode.control.common.HEAD_TOUCH_UP
import cn.wycode.control.common.Position
import cn.wycode.control.common.TOUCH_ID_MOUSE_LEFT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component
import java.util.concurrent.ThreadLocalRandom

const val REPEAT_INITIAL_DELAY = 67L

@Component
class RepeatHelper(val connections: Connections) {
    var enableRepeatFire = false
    var isFireRepeating = false
    var repeatDelayMin = 0L
    var repeatDelayMax = 0L
    lateinit var leftMousePosition: Position

    fun startRepeatFire() {
        isFireRepeating = true
        CoroutineScope(Dispatchers.IO).launch {
            while (isFireRepeating) {
                val random = ThreadLocalRandom.current()
                val position = getRandomFirePosition(random)
                connections.sendTouch(HEAD_TOUCH_DOWN, TOUCH_ID_MOUSE_LEFT, position.x, position.y, false)

                val upX = position.x + random.nextInt(RANDOM_POSITION_MIN, RANDOM_POSITION_MAX) * 2
                val upY = position.y + random.nextInt(RANDOM_POSITION_MIN, RANDOM_POSITION_MAX) * 2
                delay(random.nextLong(repeatDelayMin, repeatDelayMax))
                connections.sendTouch(HEAD_TOUCH_UP, TOUCH_ID_MOUSE_LEFT, upX, upY, false)

                delay(REPEAT_INITIAL_DELAY)
            }
        }
    }

    fun stopRepeatFire() {
        isFireRepeating = false
    }

    fun getRandomFirePosition(random: ThreadLocalRandom): Position {
        val randomY = random.nextInt(RANDOM_POSITION_MIN, RANDOM_POSITION_MAX) * 4
        val shakeX = leftMousePosition.x - randomY / 2 + random.nextInt(RANDOM_POSITION_MIN, RANDOM_POSITION_MAX)
        val shakeY = leftMousePosition.y + randomY
        return Position(shakeX, shakeY)
    }
}