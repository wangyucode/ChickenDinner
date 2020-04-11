package cn.wycode.control

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import cn.wycode.control.common.Keymap

class KeymapView : View {

    var keymap: Keymap? = null

    private val mPaint = Paint()

    constructor(ctx: Context) : super(ctx)
    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs)

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        mPaint.color = 0x33ffffff
        mPaint.strokeWidth = 2f
        mPaint.style = Paint.Style.STROKE
        mPaint.textSize = 30f

        if (keymap == null) return

        val joystick = keymap!!.joystick
        if (joystick != null) {
            canvas.drawCircle(
                joystick.center.x.toFloat(),
                joystick.center.y.toFloat(),
                joystick.radius.toFloat(),
                mPaint
            )
        }

        val buttons = keymap!!.buttons
        mPaint.color = 0x3355ff55
        mPaint.style = Paint.Style.FILL_AND_STROKE
        for (button in buttons) {
            canvas.drawText(button.key, button.position.x.toFloat(), button.position.y.toFloat(), mPaint)
        }
    }
}