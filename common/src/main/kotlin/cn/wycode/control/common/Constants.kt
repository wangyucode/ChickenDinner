package cn.wycode.control.common

const val LOG_TAG = "[wycs]"

const val ZERO_BYTE: Byte = 0

const val MOUSE_PORT = 15940
const val CONTROL_PORT = 15941
const val MOUSE_SOCKET = "overlay-socket"
const val CONTROL_SOCKET = "control-socket"
const val CONTROL_PATH = "/sdcard/controller.jar"
const val CONTROL_SERVER = "cn.wycode.control.server.ControlServer"

const val HEAD_TOUCH_DOWN: Byte = 1
const val HEAD_TOUCH_MOVE: Byte = 2
const val HEAD_TOUCH_UP: Byte = 3
const val HEAD_KEY: Byte = 4
const val HEAD_MOUSE_MOVE: Byte = 5
const val HEAD_KEYMAP: Byte = 6
const val HEAD_MOUSE_VISIBLE: Byte = 7
const val HEAD_MOUSE_INVISIBLE: Byte = 8
const val HEAD_REPEAT_ENABLE: Byte = 9
const val HEAD_REPEAT_DISABLE: Byte = 10
const val HEAD_KEYMAP_VISIBLE: Byte = 11
const val HEAD_KEYMAP_INVISIBLE: Byte = 12

const val HEAD_CLEAR_TOUCH: Byte = 15
const val HEAD_SHUT_DOWN: Byte = 16

const val KEY_HOME: Byte = 1
const val KEY_BACK: Byte = 2
const val KEY_VOLUME_UP: Byte = 3
const val KEY_VOLUME_DOWN: Byte = 4