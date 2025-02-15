package cn.wycode.clientui

import java.awt.*

const val TEXT_SIZE = 16

class ControlCanvas : Canvas() {

    private var lastLogY = TEXT_SIZE / 2
    private var currentLog = ""
    private var isClean = true
    private var isDrawRect = false
    private val padding = TEXT_SIZE / 2
    private var lastRect= Rectangle(0,0,0,0)

    init {
        background = Color(0, 44, 34)
    }

    fun append(log: String) {
        currentLog = log
        repaint(padding, lastLogY - TEXT_SIZE + 4, width - padding * 2, TEXT_SIZE)
    }

    fun clean() {
        isClean = true
        repaint()
    }

    fun drawRect() {
        isDrawRect = true
        repaint()
    }

    override fun paint(g: Graphics) {
        if (isClean) {
            g.clearRect(0, 0, width, height)
            isClean = false
            lastLogY = TEXT_SIZE
            g.color = Color(0,0,0,0x55)
            g.fillRect(lastRect.x, lastRect.y, lastRect.width, lastRect.height)
            return
        }
        if (isDrawRect) {
            g.clip = Rectangle(0,0,width,height)
            g.color = background
            g.fillRect(lastRect.x, lastRect.y, lastRect.width, lastRect.height)
            lastRect = CONTROL_AREA_BOUNDS
            g.color = Color(0,0,0,0x55)
            g.fillRect(lastRect.x, lastRect.y, lastRect.width, lastRect.height)
            isDrawRect = false
            return
        }
        if (currentLog.isNotEmpty()) {
            g.color = Color.WHITE
            g.drawString(currentLog, padding, lastLogY)
            lastLogY += TEXT_SIZE
            currentLog = ""
        }
    }

    override fun update(g: Graphics) {
        paint(g)
    }
}