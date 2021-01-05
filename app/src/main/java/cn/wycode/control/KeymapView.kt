package cn.wycode.control

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import cn.wycode.control.common.Button
import cn.wycode.control.common.Keymap

class KeymapView : View {

    var repeat: Boolean = false
    var keymap: Keymap? = null

    private val mPaint = Paint()

    constructor(ctx: Context) : super(ctx)
    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs)

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        mPaint.color = 0x33ffffff
        mPaint.strokeWidth = 1f
        mPaint.style = Paint.Style.STROKE
        mPaint.textSize = 30f

        if (keymap == null) return

        val joystick = keymap!!.joystick
        canvas.drawCircle(
            joystick.center.x.toFloat(),
            joystick.center.y.toFloat(),
            joystick.radius.toFloat(),
            mPaint
        )

        mPaint.style = Paint.Style.FILL_AND_STROKE
        drawProps(canvas)

        val buttons = keymap!!.buttons
        mPaint.color = 0x3355ff55
        for (button in buttons) {
            drawButton(canvas, button)
        }

        mPaint.color = 0x335555ff
        val mouse = keymap!!.mouse
        drawButton(canvas, Button("RM", mouse.right))
        if (repeat) {
            mPaint.color = 0x33ff5555
        }
        drawButton(canvas, Button("LM", mouse.left))

        mPaint.color = 0x66dd0000
        canvas.drawCircle(width / 2f, height / 2f, 2f, mPaint)
    }

    private fun drawProps(canvas: Canvas){
        canvas.drawCircle(
            keymap!!.drops.open.x.toFloat(),
            keymap!!.drops.open.y.toFloat(),
            10f,
            mPaint
        )

        canvas.drawCircle(
            keymap!!.drugs.open.x.toFloat(),
            keymap!!.drugs.open.y.toFloat(),
            10f,
            mPaint
        )
    }

    private fun drawButton(canvas: Canvas, button: Button) {
        val x = button.position.x.toFloat()
        val y = button.position.y.toFloat()
        val text = if (button.name != null) button.name else button.key
        canvas.drawCircle(x, y, 10f, mPaint)
        canvas.drawText(text!!, x, y, mPaint)
    }
}