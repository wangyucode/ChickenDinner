package cn.wycode.control.client

import cn.wycode.control.common.*
import lc.kra.system.mouse.GlobalMouseHook
import lc.kra.system.mouse.event.GlobalMouseAdapter
import lc.kra.system.mouse.event.GlobalMouseEvent

class FovHandler(private val connections: Connections) {

    private lateinit var mouseHook: GlobalMouseHook

    lateinit var mouse: Mouse

    private val mouseListener = MouseListener()

    fun start() {
        mouseHook = GlobalMouseHook(true)
        mouseHook.addMouseListener(mouseListener)
    }

    fun stop() {
        mouseHook.shutdownHook()
    }

    inner class MouseListener : GlobalMouseAdapter() {
        override fun mouseMoved(event: GlobalMouseEvent) {
            connections.sendMoveFov(event.x, event.y)
        }

        override fun mousePressed(event: GlobalMouseEvent) {
            // fire
            val position: Position
            val id: Byte
            if (event.button == GlobalMouseEvent.BUTTON_LEFT) {
                position = mouse.left
                id = TOUCH_ID_MOUSE_LEFT
                if (connections.enableRepeatFire && connections.weaponNumber == 1) {
                    connections.startRepeatFire(mouse.left)
                    return
                }
            } else if (event.button == GlobalMouseEvent.BUTTON_RIGHT) {
                position = mouse.right
                id = TOUCH_ID_MOUSE_RIGHT
            } else {
                return
            }
            // normal click
            connections.sendTouch(
                HEAD_TOUCH_DOWN,
                id,
                position.x,
                position.y,
                true
            )
        }

        override fun mouseReleased(event: GlobalMouseEvent) {
            val position: Position
            val id: Byte
            when (event.button) {
                GlobalMouseEvent.BUTTON_LEFT -> {
                    position = mouse.left
                    id = TOUCH_ID_MOUSE_LEFT
                    // repeat stop
                    connections.stopRepeatFire()
                }
                GlobalMouseEvent.BUTTON_RIGHT -> {
                    position = mouse.right
                    id = TOUCH_ID_MOUSE_RIGHT
                }
                else -> {
                    return
                }
            }
            // normal click
            connections.sendTouch(
                HEAD_TOUCH_UP,
                id,
                position.x,
                position.y,
                false
            )
        }
    }
}


