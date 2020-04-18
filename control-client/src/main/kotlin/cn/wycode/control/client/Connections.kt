package cn.wycode.control.client

import cn.wycode.control.common.*
import javafx.scene.Cursor
import javafx.scene.Scene
import java.awt.Robot
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

const val JOYSTICK_STEP_COUNT = 8
const val JOYSTICK_STEP_DELAY = 40L
const val SCREEN_EDGE = 5
const val SENSITIVITY_X = 0.6
const val SENSITIVITY_Y = 0.6
const val REPEAT_TIME_MIN = 15L

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
    private val repeatBuffer = ByteBuffer.allocate(10)

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
    private val controlEventExecutor = Executors.newSingleThreadExecutor()
    private val resetEventExecutor = Executors.newSingleThreadScheduledExecutor()
    private val joystickEventExecutor = Executors.newSingleThreadScheduledExecutor()
    private val repeatFireEventExecutor = Executors.newSingleThreadScheduledExecutor()

    lateinit var mouseOutputStream: OutputStream
    lateinit var controlOutputStream: OutputStream

    private var lastJoystickByte: Byte = 0

    @Volatile
    private var lastJoystickX = 0

    @Volatile
    private var lastJoystickY = 0

    @Volatile
    private var joystickId = 0

    @Volatile
    private var isFovAutoUp = false

    @Volatile
    var isDropsOpen = false

    @Volatile
    var isDrugsOpen = false

    lateinit var scene: Scene

    var mouseVisible = true

    var lastFovX = 0.0
    var lastFovY = 0.0

    private val robot = Robot()

    private var resetFuture: ScheduledFuture<*>? = null
    private var repeatFuture: ScheduledFuture<*>? = null
    private var closeDropFuture: ScheduledFuture<*>? = null
    private var closeDrugFuture: ScheduledFuture<*>? = null

    var enableRepeatFire = false

    var weaponNumber = 1

    private lateinit var resetPosition: Position
    private lateinit var joystick: Joystick

    fun initButtons(keymap: Keymap) {
        joystick = keymap.joystick
        resetPosition = keymap.buttons.find { it.name == KEY_NAME_SWITCH }!!.position
    }

    fun sendKey(key: Byte) {
        keyBuffer.clear()
        keyBuffer.put(HEAD_KEY)
        keyBuffer.put(key)
        controlEventExecutor.execute(WriteRunnable(controlOutputStream, keyBuffer.array().copyOf()))
    }

    fun sendMouseMove(x: Int, y: Int) {
        mouseMoveBuffer.clear()
        mouseMoveBuffer.put(HEAD_MOUSE_MOVE)
        mouseMoveBuffer.putInt(x)
        mouseMoveBuffer.putInt(y)
        mouseEventExecutor.execute(WriteRunnable(mouseOutputStream, mouseMoveBuffer.array().copyOf()))
    }

    fun sendTouch(head: Byte, id: Byte, x: Int, y: Int, shake: Boolean) {
        val shakeX = if (shake) x + Random.nextInt(-5, 5) else x
        val shakeY = if (shake) y + Random.nextInt(-5, 5) else y
        touchBuffer.clear()
        touchBuffer.put(head)
        touchBuffer.put(id)
        touchBuffer.putInt(shakeX)
        touchBuffer.putInt(shakeY)
        controlEventExecutor.execute(WriteRunnable(controlOutputStream, touchBuffer.array().copyOf()))
    }


    fun sendMoveFov(dx: Int, dy: Int) {
        resetFuture?.cancel(false)
        if (isFovAutoUp) {
            sendTouch(
                HEAD_TOUCH_DOWN,
                TOUCH_ID_MOUSE,
                resetPosition.x,
                resetPosition.y,
                false
            )

            robot.mouseMove((OFFSET.x + resetPosition.x / RATIO).toInt(), (OFFSET.y + resetPosition.y / RATIO).toInt())
            isFovAutoUp = false
            resetLastFov()
            return
        }

        if (abs(dx) > 30 || abs(dy) > 30) return

        if (lastFovX == 0.0 || lastFovY == 0.0) {
            resetLastFov()
        }

        lastFovX += dx * SENSITIVITY_X
        lastFovY += dy * SENSITIVITY_Y

        //reach the device edge
        if (lastFovX < 100 || lastFovX > CANVAS.x - 100 || lastFovY < 100 || lastFovY > CANVAS.y - 100) {
            // up from current position
            sendTouch(
                HEAD_TOUCH_UP,
                TOUCH_ID_MOUSE,
                lastFovX.toInt(),
                lastFovY.toInt(),
                false
            )
            // reset mouse position
            robot.mouseMove((OFFSET.x + resetPosition.x / RATIO).toInt(), (OFFSET.y + resetPosition.y / RATIO).toInt())
            // down from reset position
            sendTouch(
                HEAD_TOUCH_DOWN,
                TOUCH_ID_MOUSE,
                resetPosition.x,
                resetPosition.y,
                false
            )
            // reset last fov
            resetLastFov()
            // ignore this move
            return
        }

        sendTouch(HEAD_TOUCH_MOVE, TOUCH_ID_MOUSE, lastFovX.toInt(), lastFovY.toInt(), false)

        touchBuffer.clear()
        touchBuffer.put(HEAD_TOUCH_UP)
        touchBuffer.put(TOUCH_ID_MOUSE)
        touchBuffer.putInt(lastFovX.toInt())
        touchBuffer.putInt(lastFovY.toInt())
        resetFuture = resetEventExecutor.schedule(
            ResetMouseRunnable(touchBuffer.array().copyOf()),
            1000L,
            TimeUnit.MILLISECONDS
        )
    }

    private fun resetLastFov() {
        lastFovX = resetPosition.x.toDouble()
        lastFovY = resetPosition.y.toDouble()
    }

    fun sendJoystick(joystickByte: Byte) {
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
                controlEventExecutor.execute(
                    JoystickWriteRunnable(
                        joystickId,
                        joystickCenterX,
                        joystickCenterY,
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
            controlEventExecutor.execute(
                JoystickWriteRunnable(
                    joystickId,
                    joystickCenterX + Random.nextInt(-5, 5),
                    joystickCenterY + Random.nextInt(-5, 5),
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
            joystickEventExecutor.schedule(
                JoystickWriteRunnable(joystickId, x, y, touchBuffer.array().copyOf()),
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
        mouseEventExecutor.execute(WriteRunnable(mouseOutputStream, buffer.array()))
    }

    fun sendSwitchMouse() {
        resetFuture?.cancel(false)
        mouseVisible = !mouseVisible
        val head = if (mouseVisible) {
            sendTouch(HEAD_TOUCH_UP, TOUCH_ID_MOUSE, lastFovX.toInt(), lastFovY.toInt(), false)
            sendMouseMove(resetPosition.x, resetPosition.y)
            scene.cursor = Cursor.DEFAULT
            HEAD_MOUSE_VISIBLE
        } else {
            sendTouch(HEAD_TOUCH_DOWN, TOUCH_ID_MOUSE, resetPosition.x, resetPosition.y, false)
            scene.cursor = Cursor.NONE
            resetLastFov()
            HEAD_MOUSE_INVISIBLE
        }
        mouseEventExecutor.execute(WriteRunnable(mouseOutputStream, byteArrayOf(head)))
        robot.mouseMove((OFFSET.x + resetPosition.x / RATIO).toInt(), (OFFSET.y + resetPosition.y / RATIO).toInt())
    }

    fun sendBagOpen(mousePosition: Position) {
        resetFuture?.cancel(false)
        mouseVisible = true
        sendTouch(HEAD_TOUCH_UP, TOUCH_ID_MOUSE, lastFovX.toInt(), lastFovY.toInt(), false)
        sendMouseMove(mousePosition.x, mousePosition.y)
        scene.cursor = Cursor.DEFAULT
        mouseEventExecutor.execute(WriteRunnable(mouseOutputStream, byteArrayOf(HEAD_MOUSE_VISIBLE)))
        robot.mouseMove((OFFSET.x + mousePosition.x / RATIO).toInt(), (OFFSET.y + mousePosition.y / RATIO).toInt())
    }

    fun checkReachEdge(x: Double, y: Double) {
        if (x < SCREEN_EDGE) {
            robot.mouseMove(OFFSET.x + SCREEN_EDGE, (OFFSET.y + y).toInt())
        }
        if (x > CANVAS.x - SCREEN_EDGE) {
            robot.mouseMove(OFFSET.x + CANVAS.x - SCREEN_EDGE, (OFFSET.y + y).toInt())
        }
        if (y < SCREEN_EDGE) {
            robot.mouseMove((OFFSET.x + x).toInt(), OFFSET.y + SCREEN_EDGE)
        }
        if (y > CANVAS.y - SCREEN_EDGE) {
            robot.mouseMove((OFFSET.x + x).toInt(), OFFSET.y + CANVAS.y - SCREEN_EDGE)
        }
    }

    fun startRepeatFire(left: Position) {
        repeatFuture = repeatFireEventExecutor.scheduleAtFixedRate(
            WriteRepeatClickRunnable(left.x, left.y),
            0,
            100,
            TimeUnit.MILLISECONDS
        )
    }

    fun stopRepeatFire() {
        repeatFuture?.cancel(false)
    }

    fun sendEnableRepeat() {
        val head = if (enableRepeatFire) HEAD_REPEAT_ENABLE else HEAD_REPEAT_DISABLE
        mouseEventExecutor.execute(WriteRunnable(mouseOutputStream, byteArrayOf(head)))
    }

    fun sendDropsOpen(open: Boolean) {
        val head = if (open) {
            closeDropFuture = resetEventExecutor.schedule({ sendDropsOpen(false) }, 3000, TimeUnit.MILLISECONDS)
            HEAD_DROPS_OPEN
        } else {
            closeDropFuture?.cancel(false)
            HEAD_DROPS_CLOSE
        }
        isDropsOpen = open
        mouseEventExecutor.execute(WriteRunnable(mouseOutputStream, byteArrayOf(head)))
    }

    fun sendDrugsOpen(open: Boolean) {
        val head = if (open) {
            closeDrugFuture = resetEventExecutor.schedule({ sendDrugsOpen(false) }, 3000, TimeUnit.MILLISECONDS)
            HEAD_DRUGS_OPEN
        } else {
            closeDrugFuture?.cancel(false)
            HEAD_DRUGS_CLOSE
        }
        isDrugsOpen = open
        mouseEventExecutor.execute(WriteRunnable(mouseOutputStream, byteArrayOf(head)))
    }

    inner class JoystickWriteRunnable(
        private val id: Int,
        private val x: Int,
        private val y: Int,
        private val data: ByteArray
    ) : Runnable {
        override fun run() {
            if (joystickId == id) {
                controlOutputStream.write(data)
                lastJoystickX = x
                lastJoystickY = y
            }
        }
    }

    inner class ResetMouseRunnable(private val data: ByteArray) : Runnable {
        override fun run() {
            controlOutputStream.write(data)
            isFovAutoUp = true
        }
    }

    inner class WriteRepeatClickRunnable(private val x: Int, private val y: Int) :
        Runnable {

        override fun run() {
            Thread.sleep(Random.nextLong(REPEAT_TIME_MIN, REPEAT_TIME_MIN + 10))
            val shakeX = x + Random.nextInt(-10, 10)
            val shakeY = y + Random.nextInt(-10, 10)
            repeatBuffer.clear()
            repeatBuffer.put(HEAD_TOUCH_DOWN)
            repeatBuffer.put(TOUCH_ID_MOUSE_LEFT)
            repeatBuffer.putInt(shakeX)
            repeatBuffer.putInt(shakeY)
            controlOutputStream.write(repeatBuffer.array())

            repeatBuffer.put(0, HEAD_TOUCH_UP)
            Thread.sleep(Random.nextInt(20, 30).toLong())

            controlOutputStream.write(repeatBuffer.array())
        }
    }

    class WriteRunnable(private val outputStream: OutputStream, private val data: ByteArray) : Runnable {
        override fun run() {
            outputStream.write(data)
        }
    }
}





