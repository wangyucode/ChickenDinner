package cn.wycode.control.server

import android.os.SystemClock
import android.util.SparseArray
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

    val localIdToEvent = SparseArray<Event>(10)

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
        var action = MotionEvent.ACTION_MOVE
        when (input.type) {
            HEAD_TOUCH_DOWN -> {
                lastTouchDown = now
                // first touch down
                if (localIdToEvent.size() == 0) {
                    action = MotionEvent.ACTION_DOWN
                    localIdToEvent.put(0, input.copy())
                } else {
                    localId = getLocalId(input)
                    if (localId != -1) {
                        Ln.w("already down $input")
                    } else {
                        localId = getUnusedLocalId()
                        if (localId == -1) {
                            Ln.w("no localId can use")
                            return null
                        }
                    }
                    localIdToEvent.put(localId, input.copy())
                    action =
                        MotionEvent.ACTION_POINTER_DOWN or (localIdToEvent.indexOfKey(localId) shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
                }
            }
            HEAD_TOUCH_UP -> {
                if (localIdToEvent.size() == 1) {
                    action = MotionEvent.ACTION_UP
                    localId = getLocalId(input)
                } else {
                    localId = getLocalId(input)
                    action =
                        MotionEvent.ACTION_POINTER_UP or (localIdToEvent.indexOfKey(localId) shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
                }
                if (localId == -1) {
                    Ln.w("up id not found")
                    return null
                } else {
                    localIdToEvent.put(localId, input.copy())
                }
            }

            HEAD_TOUCH_MOVE -> {
                localId = getLocalId(input)
                if (localId == -1) {
                    Ln.w("move id not found")
                    return null
                } else {
                    localIdToEvent.put(localId, input.copy())
                }
            }
        }

        val pointerCount = localIdToEvent.size()

        updatePointerData()

        if (input.type == HEAD_TOUCH_UP) {
            localIdToEvent.remove(localId)
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

        if (ENABLE_LOG && event.action != MotionEvent.ACTION_MOVE) {
            Ln.d("localIdToEvent->${localIdToEvent}")
            Ln.d("inject->${event}")
        }
        return event
    }

    private fun updatePointerData() {
        for (i in 0 until localIdToEvent.size()) {
            val localId = localIdToEvent.keyAt(i)
            val input = localIdToEvent.valueAt(i)

            pointerProperties[i]!!.id = localId
            pointerCoords[i]!!.x = input.x.toFloat()
            pointerCoords[i]!!.y = input.y.toFloat()
        }
    }

    private fun getUnusedLocalId(): Int {
        for (i in 0 until 10) {
            val index = localIdToEvent.indexOfKey(i)
            if (index < 0) return i
        }
        return -1
    }

    private fun getLocalId(input: Event): Int {
        for (i in 0 until localIdToEvent.size()) {
            if (localIdToEvent.valueAt(i).id == input.id) {
                return localIdToEvent.keyAt(i)
            }
        }
        return -1
    }
}
