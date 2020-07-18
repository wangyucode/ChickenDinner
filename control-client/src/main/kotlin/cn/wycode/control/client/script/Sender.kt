package cn.wycode.control.client.script

import cn.wycode.control.client.Connections
import cn.wycode.control.common.HEAD_KEY
import cn.wycode.control.common.HEAD_TOUCH_DOWN
import cn.wycode.control.common.HEAD_TOUCH_UP
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.ThreadLocalRandom

class Sender(private val outputStream: OutputStream) {

    /**
     * 1 byte head, 1 byte id, 4 byte x , 4 byte y
     * | head | id |    x    |    y    |
     * |  .   |  . | . . . . | . . . . |
     * touchDown -> head = 1
     * touchMove -> head = 2
     * touchUP   -> head = 3
     */
    private val touchBuffer = ByteBuffer.allocate(10)

    /**
     * 1 byte head , 1 byte key
     * | head | key |
     * |  .   |  .  |
     */
    private val keyBuffer = ByteBuffer.allocate(2)

    private val controlEventExecutor = Executors.newSingleThreadExecutor()
    private val actionEventExecutor = Executors.newSingleThreadExecutor()

    fun sendKey(key: Byte) {
        keyBuffer.clear()
        keyBuffer.put(HEAD_KEY)
        keyBuffer.put(key)
        controlEventExecutor.execute(Connections.WriteRunnable(outputStream, keyBuffer.array().copyOf()))
    }

    fun sendTouch(head: Byte, id: Byte, x: Int, y: Int, shake: Int) {
        var shakeX = x
        var shakeY = y
        if (shake > 0) {
            val random = ThreadLocalRandom.current()
            shakeX = x + random.nextInt(-shake, shake)
            shakeY = y + random.nextInt(-shake, shake)
        }

        touchBuffer.clear()
        touchBuffer.put(head)
        touchBuffer.put(id)
        touchBuffer.putInt(shakeX)
        touchBuffer.putInt(shakeY)
        controlEventExecutor.execute(Connections.WriteRunnable(outputStream, touchBuffer.array().copyOf()))
    }

    fun tap(id: Byte, x: Int, y: Int, shake: Int) {
        actionEventExecutor.execute {
            sendTouch(HEAD_TOUCH_DOWN, id, x, y, shake)
            Thread.sleep(50)
            sendTouch(HEAD_TOUCH_UP, id, x, y, shake)
        }
    }
}