package cn.wycode.control.server

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import cn.wycode.control.common.HEAD_TOUCH_DOWN
import cn.wycode.control.common.HEAD_TOUCH_UP
import cn.wycode.control.server.model.Point
import cn.wycode.control.server.utils.Ln
import cn.wycode.control.server.utils.PointersState


const val MAX_POINTERS = 10

class TouchConverter {

    private var lastTouchDown = 0L
    private val pointerProperties = arrayOfNulls<MotionEvent.PointerProperties>(MAX_POINTERS)
    private val pointerCoords = arrayOfNulls<MotionEvent.PointerCoords>(MAX_POINTERS)

    private val pointersState = PointersState()


    init {
        for (i in 0 until MAX_POINTERS) {
            val props = MotionEvent.PointerProperties()
            props.toolType = MotionEvent.TOOL_TYPE_FINGER
            val coords = MotionEvent.PointerCoords()
            coords.orientation = -1f
            coords.size = 128f
            pointerProperties[i] = props
            pointerCoords[i] = coords
        }
    }

    fun convert(input: Event): MotionEvent? {
        val now = SystemClock.uptimeMillis()

        val pointerIndex: Int = pointersState.getPointerIndex(input.id.toLong())
        if (pointerIndex == -1) {
            Ln.w("Too many pointers for touch event");
            return null
        }

        val pointer = pointersState[pointerIndex]
        pointer.point = Point(input.x, input.y)
        pointer.pressure = 128f
        pointer.isUp = input.type == HEAD_TOUCH_UP

        val pointerCount = pointersState.update(pointerProperties, pointerCoords)

        var action: Int = when (input.type) {
            HEAD_TOUCH_DOWN -> MotionEvent.ACTION_DOWN
            HEAD_TOUCH_UP -> MotionEvent.ACTION_UP
            else -> MotionEvent.ACTION_MOVE
        }
        if (pointerCount == 1) {
            if (input.type == HEAD_TOUCH_DOWN) {
                lastTouchDown = now
            }
        } else {
            // secondary pointers must use ACTION_POINTER_* ORed with the pointerIndex
            if (input.type == HEAD_TOUCH_UP) {
                action = MotionEvent.ACTION_POINTER_UP or (pointerIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
            } else if (input.type == HEAD_TOUCH_DOWN) {
                action = MotionEvent.ACTION_POINTER_DOWN or (pointerIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
            }
        }

        val event = MotionEvent.obtain(
            lastTouchDown,
            now,
            action,
            pointerCount,
            pointerProperties,
            pointerCoords,
            0,
            0,
            1f,
            1f,
            -1,
            0,
            InputDevice.SOURCE_TOUCHSCREEN,
            0
        )
        return event
    }
}
