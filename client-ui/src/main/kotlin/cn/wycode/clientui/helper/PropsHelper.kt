package cn.wycode.clientui.helper

import cn.wycode.clientui.Connections
import org.springframework.stereotype.Component

const val PROPS_SELECT_DELAY = 200L
const val PROPS_UP_DOWN_DELAY = 7L
const val PROPS_CHANGE_DELAY = 23L

@Component
class PropsHelper(
    val connections: Connections
) {

}