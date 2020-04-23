package cn.wycode.control.client

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage
import kotlin.system.exitProcess


class Launcher : Application() {

    lateinit var controller: Controller

    override fun start(primaryStage: Stage) {
        val loader = FXMLLoader(javaClass.classLoader.getResource("main.fxml"))
        val root = loader.load<Parent>()
        controller = loader.getController()
        primaryStage.title = "Android Controller"
        primaryStage.scene = Scene(root)
        primaryStage.isResizable = false
        primaryStage.show()
    }

    override fun stop() {
        controller.stop()
        super.stop()
        exitProcess(0)
    }
}

fun main(args: Array<String>) {
    Application.launch(Launcher::class.java, *args)
}

