package cn.wycode.control.client

import cn.wycode.control.common.*
import lc.kra.system.mouse.GlobalMouseHook
import lc.kra.system.mouse.event.GlobalMouseAdapter
import lc.kra.system.mouse.event.GlobalMouseEvent
import java.util.concurrent.ThreadLocalRandom

class FovHandler(private val connections: Connections) {

    private lateinit var mouseHook: GlobalMouseHook

    lateinit var mouse: Mouse
    lateinit var lastFirePosition: Position

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
            if (event.button == GlobalMouseEvent.BUTTON_LEFT) {
                if (connections.enableRepeatFire && connections.weaponNumber == 1) {
                    connections.startRepeatFire()
                    return
                }
                lastFirePosition = connections.getRandomFirePosition(ThreadLocalRandom.current())
                connections.sendTouch(
                    HEAD_TOUCH_DOWN,
                    TOUCH_ID_MOUSE_LEFT,
                    lastFirePosition.x,
                    lastFirePosition.y,
                    false
                )
            } else if (event.button == GlobalMouseEvent.BUTTON_RIGHT) {
                // aim
                val position = mouse.right
                connections.sendTouch(
                    HEAD_TOUCH_DOWN,
                    TOUCH_ID_MOUSE_RIGHT,
                    position.x,
                    position.y,
                    true
                )
            }
        }

        override fun mouseReleased(event: GlobalMouseEvent) {
            val position: Position
            val id: Byte
            when (event.button) {
                GlobalMouseEvent.BUTTON_LEFT -> {
                    // repeat stop
                    if (connections.isFireRepeating) {
                        connections.stopRepeatFire()
                        return
                    }
                    position = lastFirePosition
                    id = TOUCH_ID_MOUSE_LEFT
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
                true
            )
        }
    }
}


