package cn.wycode.control.common

const val TOUCH_ID_MOUSE: Byte = 0
const val TOUCH_ID_BUTTON: Byte = 1
const val TOUCH_ID_JOYSTICK: Byte = 2

data class Keymap(val buttons: List<Button> = emptyList(), val joystick: Joystick?)

data class Button(val key: String, val position: Position)

data class Position(val x: Int, val y: Int)

data class Joystick(val center: Position, val radius: Int)



