package cn.wycode.control.server

import cn.wycode.control.common.HEAD_KEY
import cn.wycode.control.common.HEAD_TOUCH_DOWN
import java.io.InputStream
import java.nio.ByteBuffer

class Controller(private val inputStream: InputStream) : Thread() {

    private val event: Event = Event(0, 0, 0, 0)
    private val pointBuffer = ByteBuffer.allocate(8)

    override fun run() {
        while (true) {
            readEvent()
            convertEvent()
            injectEvent()
        }
    }

    private fun injectEvent() {
    }

    private fun convertEvent() {
    }

    private fun readEvent() {
        event.type = inputStream.read().toByte()
        if (event.type == HEAD_KEY) {
            event.key = inputStream.read().toByte()
        } else {
            inputStream.read(pointBuffer.array())
            event.x = pointBuffer.getInt(0)
            event.y = pointBuffer.getInt(4)
        }
    }
}

data class Event(var type: Byte, var x: Int, var y: Int, var key: Byte)