package cn.wycode.control.client.script

import java.lang.Thread.sleep
import java.net.Socket


fun main() {
    val initializer = Initializer().init()
    ZhuGuang(initializer.controlSocket).start()
}

class ZhuGuang(controlSocket: Socket) {

    private val sender = Sender(controlSocket.getOutputStream())

    fun start() {
        Thread {
            while (true) nengLiang()
        }.start()

        Thread {
            while (true) {
                shengJi()
                sleep(10000)
            }
        }.start()

    }


    fun nengLiang(){
        sender.tap(1, 720, 2950, 10)
        sleep(60)
    }

    fun shengJi(){
        repeat(2) {
            sender.tap(1, 1240, 680, 0)
            sleep(500)
        }
        repeat(2) {
            sender.tap(1, 1240, 1070, 0)
            sleep(500)
        }
        repeat(2) {
            sender.tap(1, 1240, 1470, 0)
            sleep(500)
        }
        repeat(2) {
            sender.tap(1, 1240, 1870, 0)
            sleep(500)
        }
    }

}
