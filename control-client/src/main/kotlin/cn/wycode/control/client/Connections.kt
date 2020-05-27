package cn.wycode.control.client

import cn.wycode.control.common.*
import javafx.scene.Cursor
import javafx.scene.Scene
import java.awt.Robot
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

const val JOYSTICK_STEP = 50
const val JOYSTICK_STEP_DELAY = 40L
const val SCREEN_EDGE = 5
const val SCREEN_FOV_EDGE = 100
const val REPEAT_INITIAL_DELAY = 67L
const val RANDOM_POSITION_MIN = -15
const val RANDOM_POSITION_MAX = 15

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
    private val joystickBuffer = ByteBuffer.allocate(10)

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
    private var lastJoystickX = -1

    @Volatile
    private var lastJoystickY = -1

    @Volatile
    private var destJoystickX = 0

    @Volatile
    private var destJoystickY = 0

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
    private var joystickFuture: ScheduledFuture<*>? = null
    private var closeDropFuture: ScheduledFuture<*>? = null
    private var closeDrugFuture: ScheduledFuture<*>? = null

    var enableRepeatFire = false
        private set
    var isFireRepeating = false
        private set

    var weaponNumber = 1

    lateinit var resetPosition: Position
    private lateinit var joystick: Joystick
    private var sensitivityX = 1.0
    private var sensitivityY = 1.0
    private var repeatDelayMin = 0L
    private var repeatDelayMax = 0L

    fun initButtons(keymap: Keymap) {
        joystick = keymap.joystick
        sensitivityX = keymap.sensitivityX
        sensitivityY = keymap.sensitivityY
        repeatDelayMin = keymap.repeatDelayMin
        repeatDelayMax = keymap.repeatDelayMax
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
        val shakeX = if (shake) x + ThreadLocalRandom.current().nextInt(RANDOM_POSITION_MIN, RANDOM_POSITION_MAX) else x
        val shakeY = if (shake) y + ThreadLocalRandom.current().nextInt(RANDOM_POSITION_MIN, RANDOM_POSITION_MAX) else y
        touchBuffer.clear()
        touchBuffer.put(head)
        touchBuffer.put(id)
        touchBuffer.putInt(shakeX)
        touchBuffer.putInt(shakeY)
        controlEventExecutor.execute(WriteRunnable(controlOutputStream, touchBuffer.array().copyOf()))
    }

    fun sendJoystick(joystickByte: Byte) {

        if (lastJoystickX < 0 || lastJoystickY < 0) resetLastJoystick()
        // no changes
        if (lastJoystickByte == joystickByte) return

        // no touch
        if (joystickByte == ZERO_BYTE) {
            sendTouch(HEAD_TOUCH_UP, TOUCH_ID_JOYSTICK, joystick.center.x, joystick.center.y, false)
            resetLastJoystick()
            lastJoystickByte = joystickByte
            // cancel move
            joystickFuture?.cancel(false)
            return
        }

        destJoystickX = joystick.center.x
        destJoystickY = joystick.center.y

        val sin45 = (joystick.radius * sin(PI / 4)).toInt()
        when (joystickByte) {
            JoystickDirection.TOP.joystickByte -> destJoystickY -= joystick.radius
            JoystickDirection.TOP_RIGHT.joystickByte -> {
                destJoystickX += sin45
                destJoystickY -= sin45
            }
            JoystickDirection.RIGHT.joystickByte -> destJoystickX += joystick.radius
            JoystickDirection.RIGHT_BOTTOM.joystickByte -> {
                destJoystickX += sin45
                destJoystickY += sin45
            }
            JoystickDirection.BOTTOM.joystickByte -> destJoystickY += joystick.radius
            JoystickDirection.BOTTOM_LEFT.joystickByte -> {
                destJoystickX -= sin45
                destJoystickY += sin45
            }
            JoystickDirection.LEFT.joystickByte -> destJoystickX -= joystick.radius
            JoystickDirection.LEFT_TOP.joystickByte -> {
                destJoystickX -= sin45
                destJoystickY -= sin45
            }
        }

        if (lastJoystickByte == ZERO_BYTE) {
            sendTouch(
                HEAD_TOUCH_DOWN,
                TOUCH_ID_JOYSTICK,
                joystick.center.x,
                joystick.center.y,
                true
            )
            resetLastJoystick()
            // start move
            joystickFuture = joystickEventExecutor.scheduleAtFixedRate(
                JoystickWriteRunnable(),
                JOYSTICK_STEP_DELAY,
                JOYSTICK_STEP_DELAY,
                TimeUnit.MILLISECONDS
            )
        }

        lastJoystickByte = joystickByte
    }

    private fun resetLastJoystick() {
        lastJoystickX = joystick.center.x
        lastJoystickY = joystick.center.y
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

    fun sendMoveFov(dx: Int, dy: Int) {
        resetFuture?.cancel(false)
        // auto up after some time
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
            resetLastFov(resetPosition)
            return
        }

        if (abs(dx) > 30 || abs(dy) > 30) return

        if (lastFovX == 0.0 || lastFovY == 0.0) {
            resetLastFov(resetPosition)
            return
        }

        lastFovX += dx * sensitivityX
        lastFovY += dy * sensitivityY

        //reach the device edge
        val reachEdge = checkFovEdge(resetPosition)
        // ignore this move
        if (reachEdge) return

        sendTouch(HEAD_TOUCH_MOVE, TOUCH_ID_MOUSE, lastFovX.toInt(), lastFovY.toInt(), false)
        // schedule auto up
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

    private fun checkFovEdge(position: Position): Boolean {
        return if (abs(lastFovX - position.x) > position.x / 2 || abs(lastFovY - position.y) > position.y - SCREEN_FOV_EDGE) {
            // up from current position
            sendTouch(
                HEAD_TOUCH_UP,
                TOUCH_ID_MOUSE,
                lastFovX.toInt(),
                lastFovY.toInt(),
                false
            )
            // reset mouse position
            robot.mouseMove((OFFSET.x + position.x / RATIO).toInt(), (OFFSET.y + position.y / RATIO).toInt())
            // down from reset position
            sendTouch(
                HEAD_TOUCH_DOWN,
                TOUCH_ID_MOUSE,
                position.x,
                position.y,
                false
            )
            // reset last fov
            resetLastFov(position)

            true
        } else {
            false
        }
    }

    private fun resetLastFov(position: Position) {
        lastFovX = position.x.toDouble()
        lastFovY = position.y.toDouble()
    }

    fun resetTouch() {
        sendTouch(HEAD_TOUCH_UP, TOUCH_ID_MOUSE, lastFovX.toInt(), lastFovY.toInt(), false)
        resetLastFov(resetPosition)
        sendTouch(HEAD_TOUCH_UP, TOUCH_ID_JOYSTICK, lastJoystickX, lastJoystickY, false)
        resetLastJoystick()
        resetEventExecutor.schedule({
            if (!mouseVisible) sendTouch(HEAD_TOUCH_DOWN, TOUCH_ID_MOUSE, lastFovX.toInt(), lastFovY.toInt(), false)
            if (lastJoystickByte != ZERO_BYTE) sendTouch(
                HEAD_TOUCH_DOWN,
                TOUCH_ID_JOYSTICK,
                joystick.center.x,
                joystick.center.y,
                false
            )
        }, 120, TimeUnit.MILLISECONDS)
    }

    fun sendSwitchMouse() {
        resetFuture?.cancel(false)
        mouseVisible = !mouseVisible
        val head = if (mouseVisible) {
            sendClearTouch()
            sendMouseMove(resetPosition.x, resetPosition.y)
            scene.cursor = Cursor.DEFAULT
            HEAD_MOUSE_VISIBLE
        } else {
            sendTouch(HEAD_TOUCH_DOWN, TOUCH_ID_MOUSE, resetPosition.x, resetPosition.y, false)
            scene.cursor = Cursor.NONE
            resetLastFov(resetPosition)
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
        isFireRepeating = true
        repeatFuture = repeatFireEventExecutor.scheduleAtFixedRate(
            WriteRepeatClickRunnable(left.x, left.y),
            0,
            REPEAT_INITIAL_DELAY,
            TimeUnit.MILLISECONDS
        )
    }

    fun stopRepeatFire() {
        isFireRepeating = false
        repeatFuture?.cancel(false)
    }

    fun sendEnableRepeat() {
        enableRepeatFire = !enableRepeatFire
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

    fun sendClearTouch() {
        controlOutputStream.write(byteArrayOf(HEAD_CLEAR_TOUCH))
    }

    fun close() {
        joystickEventExecutor.shutdown()
        resetEventExecutor.shutdown()
        repeatFireEventExecutor.shutdown()
        controlEventExecutor.shutdown()
        mouseEventExecutor.shutdown()
        controlOutputStream.write(byteArrayOf(HEAD_SHUT_DOWN))
        controlOutputStream.flush()
        controlOutputStream.close()
        mouseOutputStream.write(byteArrayOf(HEAD_SHUT_DOWN))
        mouseOutputStream.flush()
        mouseOutputStream.close()
    }

    inner class JoystickWriteRunnable : Runnable {

        override fun run() {
            var dx = destJoystickX - lastJoystickX
            var dy = destJoystickY - lastJoystickY

            if (dx > -JOYSTICK_STEP && dx < JOYSTICK_STEP && dy > -JOYSTICK_STEP && dy < JOYSTICK_STEP) return

            if (dx < -JOYSTICK_STEP) {
                dx = -JOYSTICK_STEP
            } else if (dx > JOYSTICK_STEP) {
                dx = JOYSTICK_STEP
            }

            if (dy < -JOYSTICK_STEP) {
                dy = -JOYSTICK_STEP
            } else if (dy > JOYSTICK_STEP) {
                dy = JOYSTICK_STEP
            }

            lastJoystickX += dx
            lastJoystickY += dy

            lastJoystickX += ThreadLocalRandom.current().nextInt(RANDOM_POSITION_MIN, RANDOM_POSITION_MAX)
            lastJoystickY += ThreadLocalRandom.current().nextInt(RANDOM_POSITION_MIN, RANDOM_POSITION_MAX)

            joystickBuffer.clear()
            joystickBuffer.put(HEAD_TOUCH_MOVE)
            joystickBuffer.put(TOUCH_ID_JOYSTICK)
            joystickBuffer.putInt(lastJoystickX)
            joystickBuffer.putInt(lastJoystickY)
            controlOutputStream.write(joystickBuffer.array())

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

        private val random = ThreadLocalRandom.current()

        override fun run() {
            Thread.sleep(random.nextLong(repeatDelayMin, repeatDelayMax))
            val randomY = random.nextInt(RANDOM_POSITION_MIN, RANDOM_POSITION_MAX) * 12
            val shakeX = x - randomY / 6 + random.nextInt(RANDOM_POSITION_MIN, RANDOM_POSITION_MAX)
            val shakeY = y + randomY
            repeatBuffer.clear()
            repeatBuffer.put(HEAD_TOUCH_DOWN)
            repeatBuffer.put(TOUCH_ID_MOUSE_LEFT)
            repeatBuffer.putInt(shakeX)
            repeatBuffer.putInt(shakeY)
            controlOutputStream.write(repeatBuffer.array())

            repeatBuffer.clear()
            repeatBuffer.put(HEAD_TOUCH_UP)
            repeatBuffer.put(TOUCH_ID_MOUSE_LEFT)
            repeatBuffer.putInt(shakeX + random.nextInt(RANDOM_POSITION_MIN, RANDOM_POSITION_MAX))
            repeatBuffer.putInt(shakeY + random.nextInt(RANDOM_POSITION_MIN, RANDOM_POSITION_MAX))
            Thread.sleep(random.nextLong(repeatDelayMin, repeatDelayMax))

            controlOutputStream.write(repeatBuffer.array())
        }
    }

    class WriteRunnable(private val outputStream: OutputStream, private val data: ByteArray) : Runnable {
        override fun run() {
            outputStream.write(data)
        }
    }
}





