package cn.wycode.clientui

import javafx.application.Application
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.stage.Stage
import javafx.stage.StageStyle
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationEvent
import org.springframework.context.ConfigurableApplicationContext

const val EVENT_STOP = "EVENT_STOP"
const val EVENT_CURSOR_VISIBLE = "EVENT_CURSOR_VISIBLE"
const val EVENT_CURSOR_INVISIBLE = "EVENT_CURSOR_INVISIBLE"
const val EVENT_OVERLAY_CONNECTED = "EVENT_OVERLAY_CONNECTED"
const val EVENT_CONTROL_CONNECTED = "EVENT_CONTROL_CONNECTED"
const val EVENT_SCREEN_CHANGE = "EVENT_SCREEN_CHANGE"

@SpringBootApplication
class ControllerClientUiApplication : Application() {

    lateinit var springContext: ConfigurableApplicationContext

    override fun init() {
        super.init()
        springContext = runApplication<ControllerClientUiApplication>(*parameters.raw.toTypedArray())
    }

    override fun start(primaryStage: Stage) {
        val loader = FXMLLoader(javaClass.classLoader.getResource("main.fxml"))
        loader.setControllerFactory { aClass -> springContext.getBean(aClass) }
        val root = loader.load<Parent>()
        val controller = loader.getController<Controller>()
        primaryStage.title = "Android Controller"
        primaryStage.scene = Scene(root)
        primaryStage.isResizable = false
        primaryStage.isAlwaysOnTop = true
        primaryStage.initStyle(StageStyle.TRANSPARENT)
        primaryStage.isFullScreen = true

        primaryStage.addEventHandler(KeyEvent.KEY_RELEASED) { event ->
            if (event.code == KeyCode.F12) primaryStage.isFullScreen = true
        }
        primaryStage.addEventHandler(KeyEvent.KEY_RELEASED) { event -> if (event.code == KeyCode.F11) controller.clearTextArea() }
        primaryStage.show()
    }

    override fun stop() {
        springContext.publishEvent(SpringEvent(EVENT_STOP))
        springContext.close()
        Platform.exit();
    }
}

class SpringEvent(source: String) : ApplicationEvent(source)

var ENABLE_LOG = false

fun main(args: Array<String>) {
    println(args.contentToString())
    ENABLE_LOG = args.isNotEmpty() && args[0] == "dev"
    System.setProperty("java.awt.headless", "false")
    Application.launch(ControllerClientUiApplication::class.java, *args)
}
