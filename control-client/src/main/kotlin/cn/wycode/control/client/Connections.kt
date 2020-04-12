package cn.wycode.control.client

import cn.wycode.control.common.*
import java.awt.Robot
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

const val JOYSTICK_STEP_COUNT = 8
const val JOYSTICK_STEP_DELAY = 40L

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
     * 1 byte head , 4 byte x , 4 byte y
     * | head |    x    |    y    |
     * |  .   | . . . . | . . . . |
     */
    private val mouseMoveBuffer = ByteBuffer.allocate(9)

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

    @Volatile
    private var lastJoystickX = 0

    @Volatile
    private var lastJoystickY = 0

    @Volatile
    private var joystickId = 0

    var mouseVisible = true

    private val robot = Robot()

    fun sendKey(key: Byte) {
        keyBuffer.clear()
        keyBuffer.put(HEAD_KEY)
        keyBuffer.put(key)
        controlEventExecutor.submit(WriteRunnable(controlOutputStream, keyBuffer.array().copyOf()))
    }

    fun sendMouseMove(x: Int, y: Int) {
        mouseMoveBuffer.clear()
        mouseMoveBuffer.put(HEAD_MOUSE_MOVE)
        mouseMoveBuffer.putInt(x)
        mouseMoveBuffer.putInt(y)
        mouseEventExecutor.submit(WriteRunnable(mouseOutputStream, mouseMoveBuffer.array().copyOf()))
    }

    fun sendTouch(head: Byte, id: Byte, x: Int, y: Int, shake: Boolean) {
        val shakeX = if (shake) x + Random.nextInt(-5, 5) else x
        val shakeY = if (shake) y + Random.nextInt(-5, 5) else y
        touchBuffer.clear()
        touchBuffer.put(head)
        touchBuffer.put(id)
        touchBuffer.putInt(shakeX)
        touchBuffer.putInt(shakeY)
        controlEventExecutor.submit(WriteRunnable(controlOutputStream, touchBuffer.array().copyOf()))
    }


    fun sendMoveFov(x: Int, y: Int, canvasX: Double, canvasY: Double, reset: Position) {

        if (canvasX < 100 || canvasX > CANVAS.x - 100 || canvasY < 100 || canvasY > CANVAS.y - 100) {
            sendTouch(
                HEAD_TOUCH_UP,
                TOUCH_ID_MOUSE,
                x,
                y,
                false
            )
            robot.mouseMove((OFFSET.x + reset.x / RATIO).toInt(), (OFFSET.y + reset.y / RATIO).toInt())

            sendTouch(
                HEAD_TOUCH_DOWN,
                TOUCH_ID_MOUSE,
                reset.x,
                reset.y,
                false
            )
            return
        }

        touchBuffer.clear()
        touchBuffer.put(HEAD_TOUCH_MOVE)
        touchBuffer.put(TOUCH_ID_MOUSE)
        touchBuffer.putInt(x)
        touchBuffer.putInt(y)
        controlEventExecutor.submit(WriteRunnable(controlOutputStream, touchBuffer.array().copyOf()))
    }

    fun sendJoystick(joystick: Joystick, joystickByte: Byte) {
        joystickId++
        val joystickCenterX = joystick.center.x
        val joystickCenterY = joystick.center.y

        if (lastJoystickX == 0) lastJoystickX = joystickCenterX
        if (lastJoystickY == 0) lastJoystickY = joystickCenterY

        when (lastJoystickByte) {
            joystickByte -> {
                // no change
                return
            }
            ZERO_BYTE -> {
                // down
                touchBuffer.clear()
                touchBuffer.put(HEAD_TOUCH_DOWN)
                touchBuffer.put(TOUCH_ID_JOYSTICK)
                touchBuffer.putInt(joystickCenterX)
                touchBuffer.putInt(joystickCenterY)
                controlEventExecutor.submit(
                    JoystickWriteRunnable(
                        joystickId,
                        joystickCenterX,
                        joystickCenterY,
                        controlOutputStream,
                        touchBuffer.array().copyOf()
                    )
                )
            }
        }

        if (joystickByte == ZERO_BYTE) {
            touchBuffer.clear()
            touchBuffer.put(HEAD_TOUCH_UP)
            touchBuffer.put(TOUCH_ID_JOYSTICK)
            touchBuffer.putInt(joystickCenterX)
            touchBuffer.putInt(joystickCenterY)
            controlEventExecutor.submit(
                JoystickWriteRunnable(
                    joystickId,
                    joystickCenterX + Random.nextInt(-5, 5),
                    joystickCenterY + Random.nextInt(-5, 5),
                    controlOutputStream,
                    touchBuffer.array().copyOf()
                )
            )
            lastJoystickByte = joystickByte
            return
        }

        var destX = joystickCenterX
        var destY = joystickCenterY

        val sin45 = (joystick.radius * sin(PI / 4)).toInt()
        when (joystickByte) {
            JoystickDirection.TOP.joystickByte -> destY -= joystick.radius
            JoystickDirection.TOP_RIGHT.joystickByte -> {
                destX += sin45
                destY -= sin45
            }
            JoystickDirection.RIGHT.joystickByte -> destX += joystick.radius
            JoystickDirection.RIGHT_BOTTOM.joystickByte -> {
                destX += sin45
                destY += sin45
            }
            JoystickDirection.BOTTOM.joystickByte -> destY += joystick.radius
            JoystickDirection.BOTTOM_LEFT.joystickByte -> {
                destX -= sin45
                destY += sin45
            }
            JoystickDirection.LEFT.joystickByte -> destX -= joystick.radius
            JoystickDirection.LEFT_TOP.joystickByte -> {
                destX -= sin45
                destY -= sin45
            }
        }

        val dx = (destX - lastJoystickX) / JOYSTICK_STEP_COUNT
        val dy = (destY - lastJoystickY) / JOYSTICK_STEP_COUNT
        for (i in 1..JOYSTICK_STEP_COUNT) {
            touchBuffer.clear()
            touchBuffer.put(HEAD_TOUCH_MOVE)
            touchBuffer.put(TOUCH_ID_JOYSTICK)
            val x = lastJoystickX + dx * i + Random.nextInt(-5, 5)
            val y = lastJoystickY + dy * i + Random.nextInt(-5, 5)
            touchBuffer.putInt(x)
            touchBuffer.putInt(y)
            controlEventExecutor.schedule(
                JoystickWriteRunnable(joystickId, x, y, controlOutputStream, touchBuffer.array().copyOf()),
                i * JOYSTICK_STEP_DELAY,
                TimeUnit.MILLISECONDS
            )
        }
        lastJoystickByte = joystickByte
    }

    fun sendKeymap(keymapString: String) {
        val data = keymapString.toByteArray()
        // |   .  | . . . . | . . . (...) . . . |
        // | head |  size   |        data       |
        val buffer = ByteBuffer.allocate(data.size + 5)
        buffer.put(HEAD_KEYMAP)
        buffer.putInt(data.size)
        buffer.put(data)
        mouseEventExecutor.submit(WriteRunnable(mouseOutputStream, buffer.array()))
    }

    fun sendSwitchMouse(reset: Position) {
        mouseVisible = !mouseVisible
        val head = if (mouseVisible) {
            sendTouch(HEAD_TOUCH_UP, TOUCH_ID_MOUSE, reset.x, reset.y, false)
            sendMouseMove(reset.x, reset.y)
            HEAD_MOUSE_VISIBLE
        } else {
            sendTouch(HEAD_TOUCH_DOWN, TOUCH_ID_MOUSE, reset.x, reset.y, false)
            HEAD_MOUSE_INVISIBLE
        }
        mouseEventExecutor.submit(WriteRunnable(mouseOutputStream, byteArrayOf(head)))
        robot.mouseMove((OFFSET.x + reset.x / RATIO).toInt(), (OFFSET.y + reset.y / RATIO).toInt())
    }

    fun checkReachEdge(x: Double, y: Double) {
        if (x < 10) {
            robot.mouseMove(OFFSET.x + 10, (OFFSET.y + y).toInt())
        }
        if (x > CANVAS.x - 10) {
            robot.mouseMove(OFFSET.x + CANVAS.x - 10, (OFFSET.y + y).toInt())
        }
        if (y < 10) {
            robot.mouseMove((OFFSET.x + x).toInt(), OFFSET.y + 10)
        }
        if (y > CANVAS.y - 10) {
            robot.mouseMove((OFFSET.x + x).toInt(), OFFSET.y + CANVAS.y - 10)
        }
    }

    inner class JoystickWriteRunnable(
        private val id: Int,
        private val x: Int,
        private val y: Int,
        private val outputStream: OutputStream,
        private val data: ByteArray
    ) : WriteRunnable(outputStream, data) {
        override fun run() {
            if (joystickId == id) {
                outputStream.write(data)
                lastJoystickX = x
                lastJoystickY = y
            }
        }
    }
}

open class WriteRunnable(private val outputStream: OutputStream, private val data: ByteArray) : Runnable {
    override fun run() {
        outputStream.write(data)
    }
}

