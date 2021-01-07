package cn.wycode.clientui.helper

import cn.wycode.clientui.Connections
import cn.wycode.control.common.HEAD_TOUCH_DOWN
import cn.wycode.control.common.HEAD_TOUCH_UP
import cn.wycode.control.common.Props
import javafx.scene.input.KeyCode
import kotlinx.coroutines.*
import org.springframework.stereotype.Component

const val PROPS_UP_DOWN_DELAY = 7L
const val PROPS_CHANGE_DELAY = 23L

@Component
class PropsHelper(
    val connections: Connections
) {
    var id: Int = 0
    lateinit var drops: Props
    lateinit var drugs: Props

    var changeJob: Job? = null

    fun change(code: KeyCode) {
        changeJob?.cancel()
        when (code) {
            KeyCode.DIGIT6 -> {
                changeJob = CoroutineScope(Dispatchers.IO).launch {
                    connections.sendTouch(HEAD_TOUCH_DOWN, id.toByte(), drops.open.x, drops.open.y, true)
                    delay(PROPS_UP_DOWN_DELAY)
                    connections.sendTouch(HEAD_TOUCH_UP, id.toByte(), drops.open.x, drops.open.y, true)

                    delay(PROPS_CHANGE_DELAY)

                    connections.sendTouch(HEAD_TOUCH_DOWN, id.toByte(), drops.buttons[0].x, drops.buttons[0].y, true)
                    delay(PROPS_UP_DOWN_DELAY)
                    connections.sendTouch(HEAD_TOUCH_UP, id.toByte(), drops.buttons[0].x, drops.buttons[0].y, true)
                }
            }
            KeyCode.DIGIT7 -> {
                changeJob = CoroutineScope(Dispatchers.IO).launch {
                    connections.sendTouch(HEAD_TOUCH_DOWN, id.toByte(), drugs.open.x, drugs.open.y, true)
                    delay(PROPS_UP_DOWN_DELAY)
                    connections.sendTouch(HEAD_TOUCH_UP, id.toByte(), drugs.open.x, drugs.open.y, true)

                    delay(PROPS_CHANGE_DELAY)

                    connections.sendTouch(HEAD_TOUCH_DOWN, id.toByte(), drugs.buttons[0].x, drugs.buttons[0].y, true)
                    delay(PROPS_UP_DOWN_DELAY)
                    connections.sendTouch(HEAD_TOUCH_UP, id.toByte(), drugs.buttons[0].x, drugs.buttons[0].y, true)
                }
            }
            KeyCode.DIGIT8 -> {
                changeJob = CoroutineScope(Dispatchers.IO).launch {
                    connections.sendTouch(HEAD_TOUCH_DOWN, id.toByte(), drops.open.x, drops.open.y, true)
                    delay(PROPS_UP_DOWN_DELAY)
                    connections.sendTouch(HEAD_TOUCH_UP, id.toByte(), drops.open.x, drops.open.y, true)

                    delay(PROPS_CHANGE_DELAY)

                    connections.sendTouch(HEAD_TOUCH_DOWN, id.toByte(), drops.buttons[1].x, drops.buttons[1].y, true)
                    delay(PROPS_UP_DOWN_DELAY)
                    connections.sendTouch(HEAD_TOUCH_UP, id.toByte(), drops.buttons[1].x, drops.buttons[1].y, true)
                }
            }
            KeyCode.DIGIT9 -> {
                changeJob = CoroutineScope(Dispatchers.IO).launch {
                    connections.sendTouch(HEAD_TOUCH_DOWN, id.toByte(), drugs.open.x, drugs.open.y, true)
                    delay(PROPS_UP_DOWN_DELAY)
                    connections.sendTouch(HEAD_TOUCH_UP, id.toByte(), drugs.open.x, drugs.open.y, true)

                    delay(PROPS_CHANGE_DELAY)

                    connections.sendTouch(HEAD_TOUCH_DOWN, id.toByte(), drugs.buttons[1].x, drugs.buttons[1].y, true)
                    delay(PROPS_UP_DOWN_DELAY)
                    connections.sendTouch(HEAD_TOUCH_UP, id.toByte(), drugs.buttons[1].x, drugs.buttons[1].y, true)
                }
            }
            else -> return
        }
    }

}