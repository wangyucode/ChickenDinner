package cn.wycode.clientui.handler

import cn.wycode.clientui.AwtUi.Companion.robot
import cn.wycode.clientui.Connections
import cn.wycode.clientui.RATIO
import cn.wycode.clientui.CONTROL_AREA_BOUNDS
import cn.wycode.clientui.SCREEN
import cn.wycode.clientui.helper.SwitchMouseHelper
import cn.wycode.control.common.*
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
        var x = ((e.x - CONTROL_AREA_BOUNDS.x) * RATIO).toInt()
        var y = ((e.y - CONTROL_AREA_BOUNDS.y) * RATIO).toInt()

        x = x.coerceIn(0, SCREEN.x)
        y = y.coerceIn(0, SCREEN.y)

        if (e.x < CONTROL_AREA_BOUNDS.x) {
            robot.mouseMove(CONTROL_AREA_BOUNDS.x, e.y)
        } else if (e.x > CONTROL_AREA_BOUNDS.x + CONTROL_AREA_BOUNDS.width) {
            robot.mouseMove(CONTROL_AREA_BOUNDS.x + CONTROL_AREA_BOUNDS.width, e.y)
        }

        return Position(x, y)
    }

    override fun mouseDragged(e: MouseEvent) {
        if (mouseHelper.mouseVisible) {
            val position = normalizeMousePosition(e)
            if (!connections.isOverlayClosed) {
                connections.sendMouseMove(position.x, position.y)
            }
            if (!connections.isControlClosed) {
                connections.sendTouch(
                    HEAD_TOUCH_MOVE,
                    TOUCH_ID_MOUSE,
                    position.x,
                    position.y,
                    false
                )
            }
        }
        e.consume()
    }

    override fun mouseMoved(e: MouseEvent) {
        if (mouseHelper.mouseVisible && !connections.isOverlayClosed) {
            val position = normalizeMousePosition(e)
            connections.sendMouseMove(position.x, position.y)
        }
        e.consume()
    }


    override fun mouseClicked(e: MouseEvent) {
        e.consume()
    }

    override fun mousePressed(e: MouseEvent) {
        if (connections.isControlClosed) return
        // mouse touch down
        if (mouseHelper.mouseVisible) {
            connections.sendTouch(
                HEAD_TOUCH_DOWN,
                TOUCH_ID_MOUSE,
                ((e.x - CONTROL_AREA_BOUNDS.x) * RATIO).toInt(),
                ((e.y - CONTROL_AREA_BOUNDS.y) * RATIO).toInt(),
                false
            )
        }
        e.consume()
    }

    override fun mouseReleased(e: MouseEvent) {
        if (connections.isControlClosed) return
        if (mouseHelper.mouseVisible) {
            connections.sendTouch(
                HEAD_TOUCH_UP,
                TOUCH_ID_MOUSE,
                ((e.x - CONTROL_AREA_BOUNDS.x) * RATIO).toInt(),
                ((e.y - CONTROL_AREA_BOUNDS.y) * RATIO).toInt(),
                false
            )
        }
        e.consume()
    }

    override fun mouseEntered(e: MouseEvent) {
        e.consume()
    }

    override fun mouseExited(e: MouseEvent) {
        e.consume()
    }
}