package cn.wycode.control.common

const val TOUCH_ID_MOUSE: Byte = 1
const val TOUCH_ID_JOYSTICK: Byte = 2
const val TOUCH_ID_MOUSE_LEFT: Byte = 3
const val TOUCH_ID_MOUSE_RIGHT: Byte = 4
const val TOUCH_ID_MOUSE_BACKUP: Byte = 5

const val TOUCH_ID_BUTTON: Byte = 10

const val KEY_NAME_SWITCH = "switch"
const val KEY_NAME_F = "F"
const val KEY_NAME_BAG = "bag"
const val KEY_NAME_1 = "1"
const val KEY_NAME_2 = "2"
const val KEY_NAME_3 = "3"
const val KEY_NAME_4 = "4"
const val KEY_NAME_5 = "5"
const val KEY_NAME_REPEAT = "repeat"
const val KEY_NAME_REPEAT_1 = "repeat_1"

data class Keymap(
    val buttons: List<Button> = emptyList(),
    val joystick: Joystick,
    val mouse: Mouse,
    val sensitivityX: Double,
    val sensitivityY: Double,
    val repeatDelayMin: Long,
    val repeatDelayMax: Long,
    val drops: Props,
    val drugs: Props
)

data class Props(val open: Position, val buttons: List<Position>)

data class Button(val key: String, var position: Position, val name: String? = null)

data class Position(var x: Int, var y: Int)

data class Joystick(val center: Position, val radius: Int)

data class Mouse(val left: Position, val right: Position)
