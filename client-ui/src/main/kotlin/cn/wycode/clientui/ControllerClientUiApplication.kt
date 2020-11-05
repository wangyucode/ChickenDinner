package cn.wycode.clientui

import javafx.application.Application
import javafx.application.Platform
import javafx.event.Event
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


@SpringBootApplication
class ControllerClientUiApplication: Application() {

	lateinit var springContext: ConfigurableApplicationContext

	override fun init() {
		super.init()
		springContext = runApplication<ControllerClientUiApplication>(*parameters.raw.toTypedArray())
	}

	override fun start(primaryStage: Stage) {
		val loader = FXMLLoader(javaClass.classLoader.getResource("main.fxml"))
		loader.setControllerFactory { aClass -> springContext.getBean(aClass) }
		val root = loader.load<Parent>()
		primaryStage.title = "Android Controller"
		primaryStage.scene = Scene(root)
		primaryStage.isResizable = false
		primaryStage.isAlwaysOnTop = true
		primaryStage.initStyle(StageStyle.TRANSPARENT)
		primaryStage.isFullScreen = true

		primaryStage.addEventHandler(KeyEvent.KEY_RELEASED) { event -> if (event.code == KeyCode.F12) primaryStage.isFullScreen = true}
		primaryStage.addEventHandler(KeyEvent.KEY_RELEASED) { event -> if (event.code == KeyCode.F11) springContext.publishEvent(SpringKeyEvent(event))}
		primaryStage.show()
	}

	override fun stop() {
		springContext.close();
		Platform.exit();
	}
}

class SpringKeyEvent(source: Event): ApplicationEvent(source)

fun main(args: Array<String>) {
	Application.launch(ControllerClientUiApplication::class.java, *args)
}