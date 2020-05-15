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
    var dropsOpen: Boolean = false
    var drugsOpen: Boolean = false
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

        val drops = keymap!!.drops
        if (!dropsOpen) {
            drawButton(canvas, Button("4", drops.open))
        } else {
            mPaint.color = 0x44ffff55
            drawButton(canvas, Button("4", drops.buttons[0]))
            for (i in 1 until drops.buttons.size) {
                val key = if (i < 4) i.toString() else (i + 1).toString()
                drawButton(canvas, Button(key, drops.buttons[i]))
            }
        }

        val drugs = keymap!!.drugs
        if (!drugsOpen) {
            drawButton(canvas, Button("5", drugs.open))
        } else {
            mPaint.color = 0x44ffff55
            drawButton(canvas, Button("5", drugs.buttons[0]))
            for (i in 1 until drugs.buttons.size) {
                val key = if (i < 5) i.toString() else (i + 1).toString()
                drawButton(canvas, Button(key, drugs.buttons[i]))
            }
        }

        mPaint.color = 0x445555ff
        val mouse = keymap!!.mouse
        drawButton(canvas, Button("RM", mouse.right))
        if (repeat) {
            mPaint.color = 0x44ff5555
        }
        drawButton(canvas, Button("LM", mouse.left))
    }

    private fun drawButton(canvas: Canvas, button: Button) {
        val x = button.position.x.toFloat()
        val y = button.position.y.toFloat()
        val text = if (button.name != null) button.name else button.key
        canvas.drawCircle(x, y, 10f, mPaint)
        canvas.drawText(text!!, x, y, mPaint)
    }
}