package cn.wycode.control.server

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import cn.wycode.control.common.HEAD_TOUCH_DOWN
import cn.wycode.control.common.HEAD_TOUCH_UP

const val MAX_POINTERS = 10

class TouchConverter() {

    private var lastTouchDown = 0L
    private val pointerProperties = arrayOfNulls<MotionEvent.PointerProperties>(MAX_POINTERS)
    private val pointerCoords = arrayOfNulls<MotionEvent.PointerCoords>(MAX_POINTERS)

    init {
        for (i in 0 until MAX_POINTERS) {
            val props = MotionEvent.PointerProperties()
            props.id = i
            props.toolType = MotionEvent.TOOL_TYPE_FINGER
            val coords = MotionEvent.PointerCoords()
            coords.orientation = -1f
            coords.size = 128f
            pointerProperties[i] = props
            pointerCoords[i] = coords
        }
    }

    fun convert(input: Event): MotionEvent {
        val now = SystemClock.uptimeMillis()
        var action = MotionEvent.ACTION_MOVE
        if (input.type == HEAD_TOUCH_DOWN) {
            action = MotionEvent.ACTION_DOWN
            lastTouchDown = now
        } else if (input.type == HEAD_TOUCH_UP) {
            action = MotionEvent.ACTION_UP
        }
        val event =  MotionEvent.obtain(
            lastTouchDown,
            now,
            action,
            input.x.toFloat(),
            input.y.toFloat(),
            0
        )
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        return event
    }
}