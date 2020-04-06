package cn.wycode.control_server

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class Pointer(context: Context) : View(context) {

    private val paint = Paint()

    init {
        paint.color = Color.RED
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(100,100)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.RED)
    }

}