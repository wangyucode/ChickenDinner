package cn.wycode.clientui.handler

import cn.wycode.clientui.Connections
import cn.wycode.clientui.RATIO
import cn.wycode.clientui.TEXTAREA_BOUNDS
import cn.wycode.clientui.helper.SwitchMouseHelper
import cn.wycode.control.common.HEAD_TOUCH_DOWN
import cn.wycode.control.common.HEAD_TOUCH_MOVE
import cn.wycode.control.common.HEAD_TOUCH_UP
import cn.wycode.control.common.Position
import cn.wycode.control.common.TOUCH_ID_MOUSE
import org.springframework.stereotype.Component
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener

@Component
class MouseHandler(
    val connections: Connections,
    val mouseHelper: SwitchMouseHelper
) : MouseMotionListener, MouseListener {

    private fun normalizeMousePosition(e: MouseEvent): Position {
        var x = (e.x * RATIO).toInt()
        var y = (e.y * RATIO).toInt()
        if (e.x < 0) {
            x = 0
        }
        if (e.y < 0) {
            y = 0
        }
        if (e.x > TEXTAREA_BOUNDS.width) {
            x = TEXTAREA_BOUNDS.width
        }
        if (e.y > TEXTAREA_BOUNDS.height) {
            y = TEXTAREA_BOUNDS.height
        }
        return Position(x, y)
    }

    override fun mouseDragged(e: MouseEvent) {
        val position = normalizeMousePosition(e)
        if (mouseHelper.mouseVisible && !connections.isOverlayClosed) {
            connections.sendMouseMove(position.x, position.y)
        }
        if (!connections.isControlClosed && mouseHelper.mouseVisible) {
            connections.sendTouch(
                HEAD_TOUCH_MOVE,
                TOUCH_ID_MOUSE,
                position.x,
                position.y,
                false
            )
        }
    }

    override fun mouseMoved(e: MouseEvent) {
        if (mouseHelper.mouseVisible && !connections.isOverlayClosed) {
            val position = normalizeMousePosition(e)
            connections.sendMouseMove(position.x, position.y)
        }
    }


    override fun mouseClicked(e: MouseEvent) {
    }

    override fun mousePressed(e: MouseEvent) {
        if (connections.isControlClosed) return
        // mouse touch down
        if (mouseHelper.mouseVisible) {
            connections.sendTouch(
                HEAD_TOUCH_DOWN,
                TOUCH_ID_MOUSE,
                (e.x * RATIO).toInt(),
                (e.y * RATIO).toInt(),
                false
            )
        }
    }

    override fun mouseReleased(e: MouseEvent) {
        if (connections.isControlClosed) return
        if (mouseHelper.mouseVisible) {
            connections.sendTouch(
                HEAD_TOUCH_UP,
                TOUCH_ID_MOUSE,
                (e.x * RATIO).toInt(),
                (e.y * RATIO).toInt(),
                false
            )
        }
    }

    override fun mouseEntered(e: MouseEvent) {
    }

    override fun mouseExited(e: MouseEvent) {
    }
}