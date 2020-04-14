package cn.wycode.control.common

const val TOUCH_ID_MOUSE: Byte = 1
const val TOUCH_ID_JOYSTICK: Byte = 2
const val TOUCH_ID_MOUSE_LEFT: Byte = 3
const val TOUCH_ID_MOUSE_RIGHT: Byte = 4

const val TOUCH_ID_BUTTON: Byte = 10

const val KEY_NAME_SWITCH = "switch"
const val KEY_NAME_BAG = "bag"
const val KEY_NAME_ONE = "one"
const val KEY_NAME_TWO = "two"
const val KEY_NAME_REPEAT = "repeat"

data class Keymap(
    val buttons: List<Button> = emptyList(),
    val joystick: Joystick,
    val mouse: Mouse
)

data class Button(val key: String, val position: Position, val name: String?)

data class Position(var x: Int, var y: Int)

data class Joystick(val center: Position, val radius: Int)

data class Mouse(
    val left: Position,
    val right: Position
)
