package cn.wycode.clientui.handler

import cn.wycode.clientui.CANVAS
import cn.wycode.clientui.Connections
import cn.wycode.clientui.OFFSET
import cn.wycode.clientui.RATIO
import cn.wycode.clientui.helper.SwitchMouseHelper
import cn.wycode.control.common.HEAD_TOUCH_DOWN
import cn.wycode.control.common.HEAD_TOUCH_MOVE
import cn.wycode.control.common.HEAD_TOUCH_UP
import cn.wycode.control.common.TOUCH_ID_MOUSE
import javafx.event.EventHandler
import javafx.scene.input.MouseEvent
import javafx.scene.robot.Robot
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

const val SCREEN_EDGE = 5.0

@Component
class MouseHandler(
    val connections: Connections,
    val mouseHelper: SwitchMouseHelper,
    val robot: Robot
) : EventHandler<MouseEvent> {

    var mouseVisibility = true

    @Bean
    fun robot(): Robot {
        return Robot()
    }

    override fun handle(event: MouseEvent) {
        when (event.eventType) {
            MouseEvent.MOUSE_PRESSED -> onMousePressed(event)
            MouseEvent.MOUSE_MOVED -> onMouseMoved(event)
            MouseEvent.MOUSE_RELEASED -> onMouseReleased(event)
            MouseEvent.MOUSE_DRAGGED -> onMouseDragged(event)
        }
    }

    private fun onMouseDragged(event: MouseEvent) {
        if (mouseVisibility && !connections.isOverlayClosed) {
            connections.sendMouseMove((event.x * RATIO).toInt(), (event.y * RATIO).toInt())
        }
        if (!connections.isControlClosed) {
            if (mouseHelper.mouseVisible) {
                connections.sendTouch(
                    HEAD_TOUCH_MOVE,
                    TOUCH_ID_MOUSE,
                    (event.x * RATIO).toInt(),
                    (event.y * RATIO).toInt(),
                    false
                )
            }
        }
        checkReachEdge(event.x, event.y)
    }

    fun checkReachEdge(x: Double, y: Double) {
        if (x < SCREEN_EDGE) {
            robot.mouseMove(OFFSET.x + SCREEN_EDGE, OFFSET.y + y)
        }
        if (x > CANVAS.x - SCREEN_EDGE) {
            robot.mouseMove(OFFSET.x + CANVAS.x - SCREEN_EDGE, OFFSET.y + y)
        }
        if (y < SCREEN_EDGE) {
            robot.mouseMove(OFFSET.x + x, OFFSET.y + SCREEN_EDGE)
        }
        if (y > CANVAS.y - SCREEN_EDGE) {
            robot.mouseMove(OFFSET.x + x, OFFSET.y + CANVAS.y - SCREEN_EDGE)
        }
    }

    private fun onMouseMoved(event: MouseEvent) {
        if (mouseVisibility && !connections.isOverlayClosed) {
            connections.sendMouseMove((event.x * RATIO).toInt(), (event.y * RATIO).toInt())
        }
        checkReachEdge(event.x, event.y)
    }

    private fun onMouseReleased(event: MouseEvent) {
        if (connections.isControlClosed) return
        if (mouseHelper.mouseVisible) {
            connections.sendTouch(
                HEAD_TOUCH_UP,
                TOUCH_ID_MOUSE,
                (event.x * RATIO).toInt(),
                (event.y * RATIO).toInt(),
                false
            )
        }
    }

    private fun onMousePressed(event: MouseEvent) {
        if (connections.isControlClosed) return
        // mouse touch down
        if (mouseHelper.mouseVisible) {
            connections.sendTouch(
                HEAD_TOUCH_DOWN,
                TOUCH_ID_MOUSE,
                (event.x * RATIO).toInt(),
                (event.y * RATIO).toInt(),
                false
            )
        }
    }
}