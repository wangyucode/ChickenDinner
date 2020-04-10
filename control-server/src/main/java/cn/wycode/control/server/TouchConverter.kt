package cn.wycode.control.server

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import cn.wycode.control.common.HEAD_TOUCH_DOWN
import cn.wycode.control.common.HEAD_TOUCH_MOVE
import cn.wycode.control.common.HEAD_TOUCH_UP
import cn.wycode.control.server.utils.Ln
import java.util.*

const val MAX_POINTERS = 10

class TouchConverter {

    private var lastTouchDown = 0L
    private val pointerProperties = arrayOfNulls<MotionEvent.PointerProperties>(MAX_POINTERS)
    private val pointerCoords = arrayOfNulls<MotionEvent.PointerCoords>(MAX_POINTERS)

    private val pointerIdsPool = LinkedList<Int>()

    private val inputIdToPointerIdMap = LinkedHashMap<Byte, Int>()

    init {
        for (i in 0 until MAX_POINTERS) {
            val props = MotionEvent.PointerProperties()
            props.toolType = MotionEvent.TOOL_TYPE_FINGER
            val coords = MotionEvent.PointerCoords()
            coords.orientation = -1f
            coords.size = 128f
            pointerProperties[i] = props
            pointerCoords[i] = coords
            // 0 is default touch(first touch), second touch should start at 1
            pointerIdsPool.add((i + 1))
        }
        pointerIdsPool.removeLast()
    }

    fun convert(input: Event): MotionEvent? {
        val now = SystemClock.uptimeMillis()

        val action: Int
        var touchCount = inputIdToPointerIdMap.size
        when (input.type) {
            HEAD_TOUCH_DOWN -> {
                lastTouchDown = now
                if (pointerIdsPool.size == 0) {
                    Ln.w("pointerIdsPool is empty!")
                    return null
                }
                val pointerId: Int
                if (touchCount == 0) {
                    action = MotionEvent.ACTION_DOWN
                    pointerId = 0
                } else {
                    pointerId = pointerIdsPool.removeFirst()
                    action = MotionEvent.ACTION_POINTER_DOWN or (pointerId shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
                }
                inputIdToPointerIdMap[input.id] = pointerId
                pointerProperties[touchCount]!!.id = pointerId
                pointerCoords[touchCount]!!.x = input.x.toFloat()
                pointerCoords[touchCount]!!.y = input.y.toFloat()
                touchCount++
            }
            HEAD_TOUCH_MOVE -> {
                //no record, drop the event
                if (!inputIdToPointerIdMap.containsKey(input.id)) return null
                action = MotionEvent.ACTION_MOVE
                var i = 0 //the index of element need to update
                for (entry in inputIdToPointerIdMap.entries) {
                    if (entry.key == input.id) {
                        pointerProperties[i]!!.id = entry.value
                        pointerCoords[i]!!.x = input.x.toFloat()
                        pointerCoords[i]!!.y = input.y.toFloat()
                        break
                    }
                    i++
                }
            }
            HEAD_TOUCH_UP -> {
                // no record, drop the event
                if (touchCount == 0 || !inputIdToPointerIdMap.containsKey(input.id)) return null
                action = if (touchCount == 1) {
                    MotionEvent.ACTION_UP
                } else {
                    MotionEvent.ACTION_POINTER_UP or (inputIdToPointerIdMap[input.id]!! shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
                }
                var i = 0 //the index of element need to update
                val iterator = inputIdToPointerIdMap.entries.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    if (entry.key == input.id) {
                        pointerProperties[i]!!.id = entry.value
                        pointerCoords[i]!!.x = input.x.toFloat()
                        pointerCoords[i]!!.y = input.y.toFloat()
                        //recycle pointerId
                        if (entry.value != 0) pointerIdsPool.addFirst(entry.value)
                        //remove the id map
                        iterator.remove()
                        break
                    }
                    i++
                }

            }
            else -> {
                return null
            }
        }

        val event = MotionEvent.obtain(
            lastTouchDown,
            now,
            action,
            touchCount,
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
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        return event
    }
}
