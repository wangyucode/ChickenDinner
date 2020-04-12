package cn.wycode.control.common

const val TOUCH_ID_MOUSE: Byte = 0
const val TOUCH_ID_JOYSTICK: Byte = 1
const val TOUCH_ID_MOUSE_LEFT: Byte = 2
const val TOUCH_ID_MOUSE_RIGHT: Byte = 3
const val TOUCH_ID_BUTTON: Byte = 50

data class Keymap(val buttons: List<Button> = emptyList(), val joystick: Joystick?, val mouse: Mouse)

data class Button(val key: String, val position: Position)

data class Position(var x: Int, var y: Int)

data class Joystick(val center: Position, val radius: Int)

data class Mouse(val switch: String, val reset: Position, val left: Position, val right: Position)
