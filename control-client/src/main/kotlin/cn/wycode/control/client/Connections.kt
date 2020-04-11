package cn.wycode.control.client

import cn.wycode.control.common.*
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class Connections {

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
     * 4 byte x , 4 byte y
     * |    x    |    y    |
     * | . . . . | . . . . |
     */
    private val mouseMoveBuffer = ByteBuffer.allocate(8)

    /**
     * 1 byte head , 1 byte key
     * | head | key |
     * |  .   |  .  |
     */
    private val keyBuffer = ByteBuffer.allocate(2)


    private val mouseEventExecutor = Executors.newSingleThreadExecutor()
    private val controlEventExecutor = Executors.newSingleThreadScheduledExecutor()

    lateinit var mouseOutputStream: OutputStream
    lateinit var controlOutputStream: OutputStream

    private var lastJoystickByte: Byte = 0

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

    fun sendJoystick(joystick: Joystick, joystickByte: Byte) {
        var x = joystick.center.x + Random.nextInt(-5, 5)
        var y = joystick.center.y + Random.nextInt(-5, 5)
        when {
            lastJoystickByte == joystickByte -> {
                // no change
                return
            }
            joystickByte == ZERO_BYTE -> {
                // up
                controlEventExecutor.submit {
                    touchBuffer.clear()
                    touchBuffer.put(HEAD_TOUCH_UP)
                    touchBuffer.put(TOUCH_ID_JOYSTICK)
                    touchBuffer.putInt(x)
                    touchBuffer.putInt(y)
                    controlOutputStream.write(touchBuffer.array())
                }
                lastJoystickByte = joystickByte
                return
            }
            lastJoystickByte == ZERO_BYTE -> {
                // down
                controlEventExecutor.submit {
                    touchBuffer.clear()
                    touchBuffer.put(HEAD_TOUCH_DOWN)
                    touchBuffer.put(TOUCH_ID_JOYSTICK)
                    touchBuffer.putInt(x)
                    touchBuffer.putInt(y)
                    controlOutputStream.write(touchBuffer.array())
                }
            }
        }
        lastJoystickByte = joystickByte

        val sin45 = (joystick.radius * sin(PI / 4)).toInt()
        when (joystickByte) {
            JoystickDirection.TOP.joystickByte -> y -= joystick.radius
            JoystickDirection.TOP_RIGHT.joystickByte -> {
                x += sin45
                y -= sin45
            }
            JoystickDirection.RIGHT.joystickByte -> x += joystick.radius
            JoystickDirection.RIGHT_BOTTOM.joystickByte -> {
                x += sin45
                y += sin45
            }
            JoystickDirection.BOTTOM.joystickByte -> y += joystick.radius
            JoystickDirection.BOTTOM_LEFT.joystickByte -> {
                x -= sin45
                y += sin45
            }
            JoystickDirection.LEFT.joystickByte -> x -= joystick.radius
            JoystickDirection.LEFT_TOP.joystickByte -> {
                x -= sin45
                y -= sin45
            }
        }
        controlEventExecutor.submit {
            touchBuffer.clear()
            touchBuffer.put(HEAD_TOUCH_MOVE)
            touchBuffer.put(TOUCH_ID_JOYSTICK)
            touchBuffer.putInt(x)
            touchBuffer.putInt(y)
            controlOutputStream.write(touchBuffer.array())
        }
    }
}