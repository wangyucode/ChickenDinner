package cn.wycode.clientui.helper

import cn.wycode.clientui.Connections
import cn.wycode.control.common.*
import kotlinx.coroutines.*
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

const val PROPS_CLOSE_DELAY = 3000L

@Component
class PropsHelper(
    val connections: Connections,
    val weaponHelper: WeaponHelper
) {

    var isDropsOpen = false
    var isDrugsOpen = false
    lateinit var drops: Props
    lateinit var drugs: Props
    var closeDropsJob: Job? = null
    var closeDrugsJob: Job? = null

    fun changeDownPosition(key: String?, position: Position) {
        when (key) {
            KEY_NAME_ONE, KEY_NAME_TWO, KEY_NAME_THREE -> {
                val index = key.toInt()
                if (isDropsOpen) {
                    changePosition(position, drops.buttons[index])
                } else if (isDrugsOpen) {
                    changePosition(position, drugs.buttons[index])
                }
            }
            KEY_NAME_FOUR -> {
                when {
                    isDropsOpen -> changePosition(position, drops.buttons[0])
                    isDrugsOpen -> changePosition(position, drugs.buttons[4])
                    else -> changePosition(position, drops.open)
                }
            }
            KEY_NAME_FIVE -> {
                if (isDrugsOpen) { //use default
                    changePosition(position, drugs.buttons[0])
                } else {
                    changePosition(position, drugs.open)
                }
            }
        }
    }

    fun changeUpPosition(key: String?, position: Position) {
        when (key) {
            KEY_NAME_ONE, KEY_NAME_TWO, KEY_NAME_THREE -> {
                val index = key.toInt()
                when {
                    isDropsOpen -> {
                        changePosition(position, drops.buttons[index])
                        sendDropsStatus(false)
                        weaponHelper.weaponNumber = 4
                    }
                    isDrugsOpen -> {
                        changePosition(position, drugs.buttons[index])
                        sendDrugsStatus(false)
                    }
                    else -> weaponHelper.weaponNumber = index
                }
            }
            KEY_NAME_FOUR -> {
                when {
                    isDropsOpen -> {
                        sendDropsStatus(false)
                        changePosition(position, drops.buttons[0])
                        weaponHelper.weaponNumber = 4
                    }
                    isDrugsOpen -> {
                        sendDrugsStatus(false)
                        changePosition(position, drugs.buttons[4])
                    }
                    else -> {
                        sendDropsStatus(true)
                        sendDrugsStatus(false)
                        changePosition(position, drops.open)
                    }
                }
            }
            KEY_NAME_FIVE -> {
                if (isDrugsOpen) {
                    sendDrugsStatus(false)
                    changePosition(position, drugs.buttons[0])
                } else {
                    sendDrugsStatus(true)
                    sendDropsStatus(false)
                    changePosition(position, drugs.open)
                }
            }
        }
    }

    fun changePosition(from: Position, to: Position) {
        from.x = to.x
        from.y = to.y
    }

    fun sendDropsStatus(open: Boolean) {
        val head = if (open) {
            closeDropsJob = CoroutineScope(Dispatchers.IO).launch {
                delay(PROPS_CLOSE_DELAY)
                sendDropsStatus(false)
            }
            HEAD_DROPS_OPEN
        } else {
            closeDropsJob?.cancel()
            HEAD_DROPS_CLOSE
        }
        isDropsOpen = open
        connections.sendOverlayData(byteArrayOf(head))
    }

    fun sendDrugsStatus(open: Boolean) {
        val head = if (open) {
            closeDrugsJob = CoroutineScope(Dispatchers.IO).launch {
                delay(PROPS_CLOSE_DELAY)
                sendDrugsStatus(false)
            }
            HEAD_DRUGS_OPEN
        } else {
            closeDrugsJob?.cancel()
            HEAD_DRUGS_CLOSE
        }
        isDrugsOpen = open
        connections.sendOverlayData(byteArrayOf(head))
    }


}