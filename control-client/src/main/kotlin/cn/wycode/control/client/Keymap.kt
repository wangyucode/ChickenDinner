package cn.wycode.control.client

import javafx.scene.input.KeyCode

data class Keymap(val buttons: List<Button> = emptyList(), val joystick: Joystick?)

data class Button(val key: String, val position: Position)

data class Position(val x: Int, val y: Int)

data class Joystick(val left: Button, val right: Button, val up: Button, val down: Button)

fun initButtons(keymap: Keymap, buttonMap: HashMap<KeyCode, Button>) {
    for (button in keymap.buttons) {
        buttonMap[KeyCode.getKeyCode(button.key)] = button
    }
}

