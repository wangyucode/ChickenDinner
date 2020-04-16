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
    var throwsOpen: Boolean = false
    var keymap: Keymap? = null

    private val mPaint = Paint()

    constructor(ctx: Context) : super(ctx)
    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs)

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        mPaint.color = 0x44ffffff
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

        val buttons = keymap!!.buttons
        mPaint.color = 0x4455ff55
        mPaint.style = Paint.Style.FILL_AND_STROKE
        for (button in buttons) {
            drawButton(canvas, button)
        }
        val drop = keymap!!.drops
        if (!throwsOpen) {
            drawButton(canvas, Button("4", drop.open, null))
        } else {
            mPaint.color = 0x44ffff55
            drawButton(canvas, Button("4", drop.buttons[0], null))
            for (i in 1 until drop.buttons.size) {
                val key = if (i < 4) i.toString() else (i + 1).toString()
                drawButton(canvas, Button(key, drop.buttons[i], null))
            }
        }

        mPaint.color = 0x445555ff
        val mouse = keymap!!.mouse
        drawButton(canvas, Button("RM", mouse.right, null))
        if (repeat) {
            mPaint.color = 0x44ff5555
        }
        drawButton(canvas, Button("LM", mouse.left, null))


    }

    private fun drawButton(canvas: Canvas, button: Button) {
        val x = button.position.x.toFloat()
        val y = button.position.y.toFloat()
        val text = if (button.name != null) button.name else button.key
        canvas.drawCircle(x, y, 10f, mPaint)
        canvas.drawText(text!!, x, y, mPaint)
    }
}