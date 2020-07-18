package cn.wycode.control.client.script

import java.lang.Thread.sleep
import java.net.Socket

fun main() {
    val initializer = Initializer().init()
    CanTing(initializer.controlSocket).start()
}

class CanTing(controlSocket: Socket) {

    private val sender = Sender(controlSocket.getOutputStream())

    fun start() {
        while (true) {

            repeat(30) {
                repeat(20) {
                    xuanChuan()
                }

                dianCan()
            }

            shouQian()

            right()
            chuFang()
            left()
        }

    }

    private fun xuanChuan() {
        sender.tap(1, 1280, 2879, 10)
        sleep(60)
    }

    private fun dianCan() {
        sender.tap(1, 518, 1293, 10)
        sleep(60)

        sender.tap(1, 880, 1257, 10)
        sleep(60)

        sender.tap(1, 1276, 1257, 10)
        sleep(60)

        sender.tap(1, 528, 1752, 10)
        sleep(60)

        sender.tap(1, 901, 1782, 10)
        sleep(60)

        sender.tap(1, 1296, 1782, 10)
        sleep(60)

    }

    private fun shouQian() {

        sender.tap(1, 592, 1494, 10)
        sleep(60)

        sender.tap(1, 116, 1327, 10)
        sleep(60)

        sender.tap(1, 950, 1477, 10)
        sleep(60)

        sender.tap(1, 1333, 1463, 10)
        sleep(60)

        sender.tap(1, 549, 1955, 10)
        sleep(60)

        sender.tap(1, 960, 1955, 10)
        sleep(60)

        sender.tap(1, 1323, 1955, 10)
        sleep(60)

        sender.tap(1, 499, 2386, 10)
        sleep(60)

        sender.tap(1, 642, 2686, 10)
        sleep(60)
    }

    private fun right() {
        sender.tap(1, 1353, 1635, 10)
        sleep(500)
    }

    private fun left() {
        sender.tap(1, 78, 1632, 10)
        sleep(500)
    }

    private fun chuFang() {
        sender.tap(1, 1292, 1240, 10)
        sleep(60)

        sender.tap(1, 132, 1140, 10)
        sleep(60)

        sender.tap(1, 602, 2435, 10)
        sleep(60)
    }

}
