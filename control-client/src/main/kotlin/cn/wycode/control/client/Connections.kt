package cn.wycode.control.client

import cn.wycode.control.common.HEAD_KEY
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.random.Random

class Connections {

    /**
     * 1 byte head, 1 byte id, 4 byte x , 4 byte y
     * head  id      x         y
     * | . | . | . . . . | . . . . |
     * touchDown -> head = 1
     * touchMove -> head = 2
     * touchUP   -> head = 3
     */
    private val touchBuffer = ByteBuffer.allocate(10)

    /**
     * 4 byte x , 4 byte y
     *      x         y
     * | . . . . | . . . . |
     */
    private val mouseMoveBuffer = ByteBuffer.allocate(8)

    /**
     * 1 byte head , 1 byte key
     * head  key
     * | . | . |
     */
    private val keyBuffer = ByteBuffer.allocate(2)

    private val mouseEventExecutor = Executors.newSingleThreadExecutor()
    private val controlEventExecutor = Executors.newSingleThreadExecutor()

    lateinit var mouseOutputStream: OutputStream
    lateinit var controlOutputStream: OutputStream

    fun sendKey(key: Byte) {
        println("sendKey::$key")
        controlEventExecutor.submit {
            keyBuffer.clear()
            keyBuffer.put(HEAD_KEY)
            keyBuffer.put(key)
            controlOutputStream.write(keyBuffer.array())
        }
    }

    fun sendMouseMove(x: Int, y: Int) {
        mouseEventExecutor.submit {
            mouseMoveBuffer.clear()
            mouseMoveBuffer.putInt(x)
            mouseMoveBuffer.putInt(y)
            mouseOutputStream.write(mouseMoveBuffer.array())
        }
    }

    fun sendTouch(head: Byte, id: Byte, x: Int, y: Int, shake: Boolean) {
        val shakeX = if (shake) x + Random.nextInt(-5, 5) else x
        val shakeY = if (shake) y + Random.nextInt(-5, 5) else y
        controlEventExecutor.submit {
            touchBuffer.clear()
            touchBuffer.put(head)
            touchBuffer.put(id)
            touchBuffer.putInt(shakeX)
            touchBuffer.putInt(shakeY)
            controlOutputStream.write(touchBuffer.array())
        }
    }
}