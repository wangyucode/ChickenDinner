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
        Thread {
            while (true) xuanChuan()
        }.start()

        Thread {
            while (true) {
                sleep(500)
                dianCan()
                sleep(500)
                shouQian()
            }
        }.start()

//        Thread {
//            while (true) {
//                sleep(30000)
//                shouQian()
//
//                right()
//                chuFang()
//                left()
//            }
//        }.start()


    }

    private fun xuanChuan() {
        sender.tap(1, 1300, 2879, 10)
        sleep(70)
    }

    private fun dianCan() {
        sender.tap(2, 378, 1473, 10)
        sleep(70)

        sender.tap(2, 758, 1473, 10)
        sleep(70)

        sender.tap(2, 1136, 1473, 10)
        sleep(70)

        sender.tap(2, 378, 1973, 10)
        sleep(70)

        sender.tap(2, 758, 1973, 10)
        sleep(70)

        sender.tap(2, 1136, 1973, 10)
        sleep(70)

    }

    private fun shouQian() {

        sender.tap(3, 592, 1494, 10)
        sleep(70)

        sender.tap(3, 116, 1327, 10)
        sleep(70)

        sender.tap(3, 950, 1477, 10)
        sleep(70)

        sender.tap(3, 1333, 1463, 10)
        sleep(70)

        sender.tap(3, 549, 1955, 10)
        sleep(70)

        sender.tap(3, 960, 1955, 10)
        sleep(70)

        sender.tap(3, 1323, 1955, 10)
        sleep(70)

        sender.tap(3, 499, 2386, 10)
        sleep(70)

        sender.tap(3, 642, 2686, 10)
        sleep(70)
    }

    private fun right() {
        sender.tap(3, 1353, 1635, 10)
        sleep(500)
    }

    private fun left() {
        sender.tap(3, 78, 1632, 10)
        sleep(500)
    }

    private fun chuFang() {
        sender.tap(3, 1292, 1240, 10)
        sleep(70)

        sender.tap(3, 132, 1140, 10)
        sleep(70)

        sender.tap(3, 602, 2435, 10)
        sleep(70)
    }

}
