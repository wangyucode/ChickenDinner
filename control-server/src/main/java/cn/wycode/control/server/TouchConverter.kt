package cn.wycode.control.server

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import cn.wycode.control.common.HEAD_TOUCH_DOWN
import cn.wycode.control.common.HEAD_TOUCH_MOVE
import cn.wycode.control.common.HEAD_TOUCH_UP
import cn.wycode.control.server.utils.Ln


const val MAX_POINTERS = 10

class TouchConverter {

    private var lastTouchDown = 0L
    private val pointerProperties = arrayOfNulls<MotionEvent.PointerProperties>(MAX_POINTERS)
    private val pointerCoords = arrayOfNulls<MotionEvent.PointerCoords>(MAX_POINTERS)

    val events = ArrayList<Event>(10)

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
        var localId = -1
        var index = -1
        var action = MotionEvent.ACTION_MOVE
        // find saved index and localId
        for (i in 0 until events.size) {
            if (events[i].id == input.id) {
                localId = events[i].localId
                index = i
                break
            }
        }

        when (input.type) {
            HEAD_TOUCH_DOWN -> {
                lastTouchDown = now
                // first touch down
                if (events.size == 0) {
                    action = MotionEvent.ACTION_DOWN
                    events.add(input.copy(localId = 0))
                } else {
                    if (index != -1) {
                        Ln.w("already down $input, $events")
                    } else {
                        localId = getUnusedLocalId()
                        if (localId == -1) {
                            Ln.w("no localId can use")
                            return null
                        }
                    }
                    events.add(input.copy(localId = localId))
                    action =
                        MotionEvent.ACTION_POINTER_DOWN or (events.size - 1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
                }
            }
            HEAD_TOUCH_UP -> {
                if (index == -1) {
                    Ln.w("up id not found $input, $events")
                    return null
                } else {
                    action = if (events.size == 1) {
                        MotionEvent.ACTION_UP
                    } else {
                        MotionEvent.ACTION_POINTER_UP or (index shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
                    }
                    events[index] = input.copy(localId = localId)
                }
            }

            HEAD_TOUCH_MOVE -> {
                if (index == -1) {
                    Ln.w("move id not found $input, $events")
                    return null
                } else {
                    // not moved, ignore this event
                    if (input.x == events[index].x && input.y == events[index].y) return null
                    events.removeAt(index)
                    events.add(input.copy(localId = localId))
                }
            }
        }

        val pointerCount = events.size

        updatePointerData()

        if (input.type == HEAD_TOUCH_UP) {
            events.removeAt(index)
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

        if (shouldLogEvent) {
            Ln.d("localIdToEvent->${events}")
            Ln.d("inject->${event}")
        }
        return event
    }

    private fun updatePointerData() {
        for (i in 0 until events.size) {
            val input = events[i]
            pointerProperties[i]!!.id = input.localId
            pointerCoords[i]!!.x = input.x.toFloat()
            pointerCoords[i]!!.y = input.y.toFloat()
        }
    }

    private fun getUnusedLocalId(): Int {
        for (i in 0 until 10) {
            var used = false
            for (j in 0 until events.size) {
                if (events[j].localId == i) {
                    used = true
                    break
                }
            }
            if (!used) return i
        }
        return -1
    }

}
