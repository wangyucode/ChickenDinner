package cn.wycode.control.common

const val TOUCH_ID_MOUSE: Byte = 1
const val TOUCH_ID_JOYSTICK: Byte = 2
const val TOUCH_ID_MOUSE_LEFT: Byte = 3
const val TOUCH_ID_MOUSE_RIGHT: Byte = 4
const val TOUCH_ID_MOUSE_BACKUP: Byte = 5

const val TOUCH_ID_BUTTON: Byte = 10

data class Keymap(
    val buttons: List<Button> = emptyList(),
    val joystick: Joystick,
    val mouse: Mouse,
    val sensitivityX: Double,
    val sensitivityY: Double,
    val repeatDelayMin: Long,
    val repeatDelayMax: Long
)

data class Button(val key: String, var position: Position)

data class Position(var x: Int, var y: Int)

data class Joystick(val center: Position, val radius: Int)

data class Mouse(val left: Position, val right: Position)
