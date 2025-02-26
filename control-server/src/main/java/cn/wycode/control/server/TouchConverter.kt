package cn.wycode.control.server

import android.os.SystemClock
import android.util.Log
import android.util.SparseArray
import android.view.InputDevice
import android.view.MotionEvent
import cn.wycode.control.common.HEAD_TOUCH_DOWN
import cn.wycode.control.common.HEAD_TOUCH_MOVE
import cn.wycode.control.common.HEAD_TOUCH_UP
import cn.wycode.control.server.utils.Ln
import java.util.*


const val MAX_POINTERS = 10

class TouchConverter {

    private var downTime = 0L
    private var touchScreenDeviceId = 6
    private val pointerProperties = arrayOfNulls<MotionEvent.PointerProperties>(MAX_POINTERS)
    private val pointerCoords = arrayOfNulls<MotionEvent.PointerCoords>(MAX_POINTERS)

    val localIdToEvent = SparseArray<Event>(10)

    private val unusedIds: Deque<Int> = ArrayDeque<Int>().apply {
        for (i in MAX_POINTERS - 1 downTo 0) push(i)
    }

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

        InputDevice.getDeviceIds().forEach {
            val device = InputDevice.getDevice(it)
            if (device != null && (device.sources and InputDevice.SOURCE_TOUCHSCREEN) == InputDevice.SOURCE_TOUCHSCREEN && device.name.contains("touch", true)) {
                Ln.d("Touch device id: $it, Name: ${device.name}")
                touchScreenDeviceId = it
                return@forEach
            }
        }
    }

    fun convert(input: Event): MotionEvent? {
        val now = SystemClock.uptimeMillis()
        var localId = -1
        var action = MotionEvent.ACTION_MOVE
        when (input.type) {
            HEAD_TOUCH_DOWN -> {
                // first touch down
                if (localIdToEvent.size() == 0) {
                    downTime = now
                    localId = getUnusedLocalId()
                    action = MotionEvent.ACTION_DOWN
                    localIdToEvent.put(localId, input.copy())
                } else {
                    localId = getLocalId(input)
                    if (localId != -1) {
                        Ln.w("already down $input, $localIdToEvent")
                        return null
                    } else {
                        localId = getUnusedLocalId()
                        if (localId == -1) {
                            Ln.w("no localId can use")
                            return null
                        }
                    }
                    localIdToEvent.put(localId, input.copy())
                    action = computeAction(MotionEvent.ACTION_POINTER_DOWN, localIdToEvent.indexOfKey(localId))
                }
            }
            HEAD_TOUCH_UP -> {
                localId = getLocalId(input)
                action = if (localIdToEvent.size() == 1) {
                    MotionEvent.ACTION_UP
                } else {
                    computeAction(MotionEvent.ACTION_POINTER_UP, localIdToEvent.indexOfKey(localId))
                }
                if (localId == -1) {
                    Ln.w("up id not found $input, $localIdToEvent")
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
                    val savedEvent = localIdToEvent.get(localId)
                    // ignore event if not actually moved
                    if (savedEvent.x == input.x && savedEvent.y == input.y) return null
                    localIdToEvent.put(localId, input.copy())
                }
            }
        }

        val pointerCount = localIdToEvent.size()

        updatePointerData()

        if (input.type == HEAD_TOUCH_UP) {
            localIdToEvent.remove(localId)
            returnLocalId(localId)
        }

        val event = MotionEvent.obtain(
            downTime,
            now,
            action,
            pointerCount,
            pointerProperties,
            pointerCoords,
            0,
            0,
            1f,
            1f,
            touchScreenDeviceId,
            0,
            InputDevice.SOURCE_TOUCHSCREEN,
            0
        )

        if (shouldLogEvent) {
            Ln.d("localIdToEvent->${localIdToEvent}")
            Ln.d("inject->${event}")
        }
        return event
    }

    fun computeAction(eventType: Int, index: Int): Int {
        return (index shl MotionEvent.ACTION_POINTER_INDEX_SHIFT) or eventType
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
        return if (unusedIds.isNotEmpty()) {
            unusedIds.pop()
        } else {
            -1
        }
    }

    private fun returnLocalId(id: Int) {
        unusedIds.push(id)
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
