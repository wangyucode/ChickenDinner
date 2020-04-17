package cn.wycode.control.client

import lc.kra.system.mouse.GlobalMouseHook
import lc.kra.system.mouse.event.GlobalMouseAdapter
import lc.kra.system.mouse.event.GlobalMouseEvent

class FovHandler(private val connections: Connections) {

    private lateinit var mouseHook: GlobalMouseHook

    private val mouseListener = MouseListener(connections)

    fun start() {
        mouseHook = GlobalMouseHook(true)
        mouseHook.addMouseListener(mouseListener)
    }

    fun stop() {
        mouseHook.shutdownHook()
    }
}

class MouseListener(private val connections: Connections) : GlobalMouseAdapter() {

    override fun mouseMoved(event: GlobalMouseEvent) {
        connections.sendMoveFov(event.x, event.y)
    }
}

