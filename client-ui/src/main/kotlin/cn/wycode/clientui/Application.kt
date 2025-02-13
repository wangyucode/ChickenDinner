package cn.wycode.clientui

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

var ENABLE_LOG = false

@SpringBootApplication
class Application(val awtUi: AwtUi): CommandLineRunner {

    override fun run(vararg args: String) {
        awtUi.show()
    }
}

fun main(args: Array<String>) {
    println(args.contentToString())
    ENABLE_LOG = args.isNotEmpty() && args[0] == "dev"
    val app = SpringApplication.run(Application::class.java, *args)
    Thread.setDefaultUncaughtExceptionHandler { t, e ->
        e.printStackTrace()
        app.publishEvent(SpringEvent(EVENT_STOP, "Uncaught exception in thread $t: $e"))
    }
}