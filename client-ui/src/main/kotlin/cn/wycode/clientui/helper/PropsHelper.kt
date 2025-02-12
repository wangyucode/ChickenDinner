package cn.wycode.clientui.helper

import cn.wycode.clientui.Connections
import cn.wycode.control.common.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component

@Component
class PropsHelper(
    val connections: Connections,
    val fovHelper: FovHelper
) {
    var id: Int = 0

    lateinit var dropsPosition: Position
    lateinit var drugsPosition: Position

    var lastX =0.0
    var lastY =0.0

    fun selectDrops() {
        connections.sendTouch(HEAD_TOUCH_DOWN, id.toByte(), dropsPosition.x, dropsPosition.y, true)
        lastX = dropsPosition.x.toDouble()
        lastY = dropsPosition.y.toDouble()
    }

    fun moveMouse(dx: Int, dy: Int) {
        lastX += dx * fovHelper.sensitivityX
        lastY += dy * fovHelper.sensitivityY
        connections.sendTouch(HEAD_TOUCH_MOVE, id.toByte(), lastX.toInt(), lastY.toInt(), false)
    }

    fun selectDrugs() {
        connections.sendTouch(HEAD_TOUCH_DOWN, id.toByte(), drugsPosition.x, drugsPosition.y, true)
        lastX = drugsPosition.x.toDouble()
        lastY = drugsPosition.y.toDouble()
    }

    fun done() {
        CoroutineScope(Dispatchers.IO).launch {
            delay(100)
            connections.sendTouch(HEAD_TOUCH_UP, id.toByte(), lastX.toInt(), lastY.toInt(), false)
        }
    }
}