package cn.wycode.control

import android.content.Context
import android.graphics.*
import android.os.Debug
import android.util.AttributeSet
import android.util.Log
import android.view.View
import cn.wycode.control.common.*

class KeymapView : View {

    var keymapVisible: Boolean = true
    var repeat: Boolean = false
    var selectedType = 0
    var selectedIndex = 0
    var keymap: Keymap? = null

    private val mPaint = Paint()

    constructor(ctx: Context) : super(ctx)
    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs)

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        if (keymap == null) return
        //clear previous drawings
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        Log.d(LOG_TAG, "draw-> $selectedType, $selectedIndex");
        when (selectedType.toByte()) {
            PROP_TYPE_DROPS -> {
                val position = keymap!!.drops.buttons[selectedIndex]
                mPaint.color = 0x88ff5555.toInt()
                canvas.drawCircle(
                    position.x.toFloat(),
                    position.y.toFloat(),
                    15f,
                    mPaint
                )
            }
            PROP_TYPE_DRUGS -> {
                val position = keymap!!.drugs.buttons[selectedIndex]
                mPaint.color = 0x88ff5555.toInt()
                canvas.drawCircle(
                    position.x.toFloat(),
                    position.y.toFloat(),
                    15f,
                    mPaint
                )
            }
            PROP_TYPE_DROPS_END -> {
                val position = keymap!!.drops.buttons[selectedIndex]
                mPaint.color = 0x88ff0000.toInt()
                canvas.drawCircle(
                    position.x.toFloat(),
                    position.y.toFloat(),
                    20f,
                    mPaint
                )
                selectedType = 0
                postInvalidateDelayed(250)
            }
            PROP_TYPE_DRUGS_END -> {
                val position = keymap!!.drugs.buttons[selectedIndex]
                mPaint.color = 0x88ff0000.toInt()
                canvas.drawCircle(
                    position.x.toFloat(),
                    position.y.toFloat(),
                    20f,
                    mPaint
                )
                selectedType = 0
                postInvalidateDelayed(250)
            }
        }

        if(!keymapVisible) return
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
        drawProps(canvas)

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

    private fun drawProps(canvas: Canvas) {
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
        mPaint.color = 0x88ffff55.toInt()
        for (p in keymap!!.drops.buttons) {
            canvas.drawCircle(
                p.x.toFloat(),
                p.y.toFloat(),
                10f,
                mPaint
            )
        }

        for(p in keymap!!.drugs.buttons){
            canvas.drawCircle(
                p.x.toFloat(),
                p.y.toFloat(),
                10f,
                mPaint
            )
        }
    }

    private fun drawButton(canvas: Canvas, button: Button) {
        val x = button.position.x.toFloat()
        val y = button.position.y.toFloat()
        canvas.drawCircle(x, y, 10f, mPaint)
        canvas.drawText(button.key, x, y, mPaint)
    }
}