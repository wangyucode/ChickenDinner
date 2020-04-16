package cn.wycode.control.common

const val TOUCH_ID_MOUSE: Byte = 1
const val TOUCH_ID_JOYSTICK: Byte = 2
const val TOUCH_ID_MOUSE_LEFT: Byte = 3
const val TOUCH_ID_MOUSE_RIGHT: Byte = 4
const val TOUCH_ID_PROPS: Byte = 5

const val TOUCH_ID_BUTTON: Byte = 10

const val KEY_NAME_SWITCH = "switch"
const val KEY_NAME_BAG = "bag"
const val KEY_NAME_ONE = "1"
const val KEY_NAME_TWO = "2"
const val KEY_NAME_THREE = "3"
const val KEY_NAME_FOUR = "4"
const val KEY_NAME_FIVE = "5"
const val KEY_NAME_SIX = "6"
const val KEY_NAME_REPEAT = "repeat"

data class Keymap(
    val buttons: List<Button> = emptyList(),
    val joystick: Joystick,
    val mouse: Mouse,
    val drops: Props,
    val drugs: Props
)

data class Props(val open: Position, val buttons: List<Position>)

data class Button(val key: String, var position: Position, val name: String? = null)

data class Position(var x: Int, var y: Int)

data class Joystick(val center: Position, val radius: Int)

data class Mouse(val left: Position, val right: Position)
