package cn.wycode.control

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import cn.wycode.control.common.*

class KeymapView : View {

    var keymapVisible: Boolean = true
    var repeat: Boolean = false
    var keymap: Keymap? = null

    private val mPaint = Paint()

    constructor(ctx: Context) : super(ctx)
    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs)

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        if (keymap == null) return
        //clear previous drawings
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        if (!keymapVisible) return
        mPaint.color = 0x88FFFFFF.toInt()
        mPaint.strokeWidth = 2f
        mPaint.style = Paint.Style.STROKE
        mPaint.textSize = 30f

        val joystick = keymap!!.joystick
        canvas.drawCircle(
            joystick.center.x.toFloat(),
            joystick.center.y.toFloat(),
            joystick.radius.toFloat(),
            mPaint
        )

        mPaint.style = Paint.Style.FILL_AND_STROKE
        canvas.drawCircle(
            joystick.center.x.toFloat(),
            joystick.center.y.toFloat(),
            4f,
            mPaint
        )

        val buttons = keymap!!.buttons
        mPaint.color = 0x8855ff55.toInt()
        for (button in buttons) {
            drawButton(canvas, button)
        }

        mPaint.color = 0x885555ff.toInt()
        val mouse = keymap!!.mouse
        drawButton(canvas, Button("RM", mouse.right))
        if (repeat) {
            mPaint.color = 0x88ff5555.toInt()
        }
        drawButton(canvas, Button("LM", mouse.left))

        mPaint.color = 0x88dd0000.toInt()
        canvas.drawCircle(width / 2f, height / 2f, 2f, mPaint)
    }

    private fun drawButton(canvas: Canvas, button: Button) {
        val x = button.position.x.toFloat()
        val y = button.position.y.toFloat()
        canvas.drawCircle(x, y, 10f, mPaint)
        canvas.drawText(button.key, x, y, mPaint)
    }
}