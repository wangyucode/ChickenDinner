package cn.wycode.clientui.helper

import cn.wycode.clientui.Connections
import cn.wycode.control.common.*
import kotlinx.coroutines.*
import org.springframework.stereotype.Component

const val PROPS_UP_DOWN_DELAY = 7L

const val PROPS_SELECT_DELAY = 500L
const val PROPS_CLOSE_DELAY = 10000L

@Component
class PropsHelper(
    val connections: Connections
) {
    var id: Int = 0
    var isDropOpen = false
    var isDrugOpen = false
    lateinit var drops: Props
    lateinit var drugs: Props


    var selectJob: Job? = null
    var propIndex: Int = 0

    var openJob: Job? = null

    fun change(scroll: Int) {
        selectJob?.cancel()
        if (isDropOpen) sendSelect(PROP_TYPE_DROPS, scroll, drops, PROP_TYPE_DROPS_END)
        if (isDrugOpen) sendSelect(PROP_TYPE_DRUGS, scroll, drugs, PROP_TYPE_DRUGS_END)
    }

    private fun sendSelect(type: Byte, scroll: Int, props: Props, endType: Byte) {
        propIndex += scroll
        if (propIndex == props.buttons.size) propIndex = 0
        if (propIndex == -1) propIndex = props.buttons.size - 1
        connections.sendSelectedProp(type, propIndex.toByte())
        println("sendSelect->$type,$propIndex")
        selectJob = CoroutineScope(Dispatchers.IO).launch {
            delay(PROPS_SELECT_DELAY)
            connections.sendTouch(
                HEAD_TOUCH_DOWN,
                id.toByte(),
                props.buttons[propIndex].x,
                props.buttons[propIndex].y,
                true
            )
            delay(PROPS_UP_DOWN_DELAY)
            connections.sendTouch(
                HEAD_TOUCH_UP,
                id.toByte(),
                props.buttons[propIndex].x,
                props.buttons[propIndex].y,
                true
            )
            connections.sendSelectedProp(endType, propIndex.toByte())
            println("sendSelect->$endType, $propIndex")
            propIndex = 0
        }
    }

    fun openDrops() {
        isDropOpen = true
        openJob = CoroutineScope(Dispatchers.IO).launch {
            connections.sendTouch(HEAD_TOUCH_DOWN, id.toByte(), drops.open.x, drops.open.y, true)
            delay(PROPS_UP_DOWN_DELAY)
            connections.sendTouch(HEAD_TOUCH_UP, id.toByte(), drops.open.x, drops.open.y, true)
            delay(PROPS_CLOSE_DELAY)
            isDropOpen = false
        }
    }

    fun openDrugs() {
        isDrugOpen = true
        openJob = CoroutineScope(Dispatchers.IO).launch {
            connections.sendTouch(HEAD_TOUCH_DOWN, id.toByte(), drugs.open.x, drugs.open.y, true)
            delay(PROPS_UP_DOWN_DELAY)
            connections.sendTouch(HEAD_TOUCH_UP, id.toByte(), drugs.open.x, drugs.open.y, true)
            delay(PROPS_CLOSE_DELAY)
            isDrugOpen = false
        }
    }
}