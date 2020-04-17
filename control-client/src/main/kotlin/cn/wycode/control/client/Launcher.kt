package cn.wycode.control.client

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage


class Launcher : Application() {
    override fun start(primaryStage: Stage) {
        val root = FXMLLoader(javaClass.classLoader.getResource("main.fxml")).load<Parent>()
        primaryStage.title = "Android Controller"
        primaryStage.scene = Scene(root)
        primaryStage.isResizable = false
        primaryStage.show()
    }
}

fun main(args: Array<String>) {
    Application.launch(Launcher::class.java, *args)
}

