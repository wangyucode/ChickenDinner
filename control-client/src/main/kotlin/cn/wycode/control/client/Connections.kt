package cn.wycode.control.client

import cn.wycode.control.common.*
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
const val RANDOM_POSITION_MIN = -20
const val RANDOM_POSITION_MAX = 20

class Connections(val appendTextFun: (String) -> Unit) {

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

    var mouseVisible = true

    private var lastFovX = 0.0
    private var lastFovY = 0.0

    private val robot = Robot()

    private val fovHandler = FovHandler(this)

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
    private lateinit var leftMousePosition: Position
    private var sensitivityX = 1.0
    private var sensitivityY = 1.0
    private var repeatDelayMin = 0L
    private var repeatDelayMax = 0L

    private var isResetting = false

    fun initButtons(keymap: Keymap) {
        joystick = keymap.joystick
        sensitivityX = keymap.sensitivityX
        sensitivityY = keymap.sensitivityY
        repeatDelayMin = keymap.repeatDelayMin
        repeatDelayMax = keymap.repeatDelayMax
        leftMousePosition = keymap.mouse.left

        fovHandler.mouse = keymap.mouse
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
        var shakeX = x
        var shakeY = y
        if (shake) {
            val random = ThreadLocalRandom.current()
            shakeX = x + random.nextInt(RANDOM_POSITION_MIN, RANDOM_POSITION_MAX)
            shakeY = y + random.nextInt(RANDOM_POSITION_MIN, RANDOM_POSITION_MAX)
        }

        touchBuffer.clear()
        touchBuffer.put(head)
        touchBuffer.put(id)
        touchBuffer.putInt(shakeX)
        touchBuffer.putInt(shakeY)
        controlEventExecutor.execute(WriteRunnable(controlOutputStream, touchBuffer.array().copyOf()))
    }

    fun getRandomFirePosition(random: ThreadLocalRandom): Position {
        val randomY = random.nextInt(RANDOM_POSITION_MIN, RANDOM_POSITION_MAX) * 11
        val shakeX = leftMousePosition.x - randomY / 3 + random.nextInt(RANDOM_POSITION_MIN, RANDOM_POSITION_MAX)
        val shakeY = leftMousePosition.y + randomY
        return Position(shakeX, shakeY)
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
        if (isResetting) return
        // auto up after some time
        if (isFovAutoUp) {
            sendTouch(
                HEAD_TOUCH_DOWN,
                TOUCH_ID_MOUSE,
                resetPosition.x,
                resetPosition.y,
                false
            )
            fovHandler.stop()
            robot.mouseMove((OFFSET.x + resetPosition.x / RATIO).toInt(), (OFFSET.y + resetPosition.y / RATIO).toInt())
            fovHandler.start()
            isFovAutoUp = false
            resetLastFov(resetPosition)
            return
        }

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
            fovHandler.stop()
            robot.mouseMove((OFFSET.x + position.x / RATIO).toInt(), (OFFSET.y + position.y / RATIO).toInt())
            fovHandler.start()
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
        resetFuture?.cancel(false)
        if (!mouseVisible && !isFovAutoUp) {
            sendTouch(HEAD_TOUCH_UP, TOUCH_ID_MOUSE, lastFovX.toInt(), lastFovY.toInt(), false)
            isFovAutoUp = true
        }
        if (lastJoystickByte != ZERO_BYTE) {
            sendTouch(
                HEAD_TOUCH_UP,
                TOUCH_ID_JOYSTICK,
                lastJoystickX,
                lastJoystickY,
                false
            )
            resetLastJoystick()
        }

        resetEventExecutor.schedule({
            isResetting = false
            if (lastJoystickByte != ZERO_BYTE) sendTouch(
                HEAD_TOUCH_DOWN,
                TOUCH_ID_JOYSTICK,
                joystick.center.x,
                joystick.center.y,
                false
            )
        }, 100, TimeUnit.MILLISECONDS)
        isResetting = true
    }

    fun sendSwitchMouse() {
        resetFuture?.cancel(false)
        mouseVisible = !mouseVisible
        val head = if (mouseVisible) {
            sendClearTouch()
            sendMouseMove(resetPosition.x, resetPosition.y)
            fovHandler.stop()
            robot.mouseMove((OFFSET.x + resetPosition.x / RATIO).toInt(), (OFFSET.y + resetPosition.y / RATIO).toInt())
            HEAD_MOUSE_VISIBLE
        } else {
            sendTouch(HEAD_TOUCH_DOWN, TOUCH_ID_MOUSE, resetPosition.x, resetPosition.y, false)
            resetLastFov(resetPosition)
            robot.mouseMove((OFFSET.x + resetPosition.x / RATIO).toInt(), (OFFSET.y + resetPosition.y / RATIO).toInt())
            fovHandler.start()
            HEAD_MOUSE_INVISIBLE
        }
        mouseEventExecutor.execute(WriteRunnable(mouseOutputStream, byteArrayOf(head)))
    }

    fun sendBagOpen(mousePosition: Position) {
        fovHandler.stop()
        resetFuture?.cancel(false)
        mouseVisible = true
        sendTouch(HEAD_TOUCH_UP, TOUCH_ID_MOUSE, lastFovX.toInt(), lastFovY.toInt(), false)
        sendMouseMove(mousePosition.x, mousePosition.y)
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

    fun startRepeatFire() {
        isFireRepeating = true
        repeatFuture = repeatFireEventExecutor.scheduleAtFixedRate(
            WriteRepeatClickRunnable(),
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
            if (isResetting) return
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

    inner class WriteRepeatClickRunnable : Runnable {

        private val random = ThreadLocalRandom.current()

        override fun run() {
            Thread.sleep(random.nextLong(repeatDelayMin, repeatDelayMax))
            val position = getRandomFirePosition(random)
            repeatBuffer.clear()
            repeatBuffer.put(HEAD_TOUCH_DOWN)
            repeatBuffer.put(TOUCH_ID_MOUSE_LEFT)
            repeatBuffer.putInt(position.x)
            repeatBuffer.putInt(position.y)
            controlOutputStream.write(repeatBuffer.array())

            repeatBuffer.clear()
            repeatBuffer.put(HEAD_TOUCH_UP)
            repeatBuffer.put(TOUCH_ID_MOUSE_LEFT)
            repeatBuffer.putInt(position.x + random.nextInt(RANDOM_POSITION_MIN, RANDOM_POSITION_MAX) * 2)
            repeatBuffer.putInt(position.y + random.nextInt(RANDOM_POSITION_MIN, RANDOM_POSITION_MAX) * 2)
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





