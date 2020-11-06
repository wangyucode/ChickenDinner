package cn.wycode.clientui.handler

import cn.wycode.clientui.Connections
import cn.wycode.clientui.RATIO
import javafx.event.EventHandler
import javafx.scene.input.MouseEvent
import org.springframework.stereotype.Component

@Component
class MouseHandler(val connections: Connections) : EventHandler<MouseEvent> {

    var mouseVisibility = true

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
//        if (controlConnected) {
//            if (connections.mouseVisible) {
//                connections.sendTouch(
//                    HEAD_TOUCH_MOVE,
//                    TOUCH_ID_MOUSE,
//                    (event.x * RATIO).toInt(),
//                    (event.y * RATIO).toInt(),
//                    false
//                )
//            }
//        }
//        connections.checkReachEdge(event.x, event.y)
    }

    private fun onMouseMoved(event: MouseEvent) {
        if (mouseVisibility && !connections.isOverlayClosed) {
            connections.sendMouseMove((event.x * RATIO).toInt(), (event.y * RATIO).toInt())
        }
//        connections.checkReachEdge(event.x, event.y)
    }

    private fun onMouseReleased(event: MouseEvent) {
//        if (!controlConnected) return
//        if (connections.mouseVisible) {
//            connections.sendTouch(
//                HEAD_TOUCH_UP,
//                TOUCH_ID_MOUSE,
//                (event.x * RATIO).toInt(),
//                (event.y * RATIO).toInt(),
//                false
//            )
//        }
    }

    private fun onMousePressed(event: MouseEvent) {
//        if (!controlConnected) return
//        // mouse touch down
//        if (connections.mouseVisible) {
//            connections.sendTouch(
//                HEAD_TOUCH_DOWN,
//                TOUCH_ID_MOUSE,
//                (event.x * RATIO).toInt(),
//                (event.y * RATIO).toInt(),
//                false
//            )
//        }
    }
}