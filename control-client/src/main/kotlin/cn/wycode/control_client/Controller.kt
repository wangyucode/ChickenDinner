package cn.wycode.control_client

import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.canvas.Canvas
import javafx.scene.control.Label
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import java.io.OutputStream
import java.net.URL
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.Executors


const val RATIO = 4.0

class Controller : Initializable {

    @FXML
    lateinit var canvas: Canvas

    @FXML
    lateinit var hBox: HBox

    @FXML
    lateinit var info: Label

    private val mouseEventExecutor = Executors.newSingleThreadExecutor()
    private val controlEventExecutor = Executors.newSingleThreadExecutor()
    private lateinit var mouseOutputStream: OutputStream

    var mouseConnected = false

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        val initialTask = InitialTask()
        info.textProperty().bind(initialTask.messageProperty())
        initialTask.valueProperty().addListener { _, old, value ->
            println("valueProperty-->$old,$value")
            when (value) {
                2 -> {
                    mouseConnected = true
                    mouseOutputStream = initialTask.mouseSocket.getOutputStream()
                    val readTask = ReadTask(initialTask.mouseSocket)
                    readTask.valueProperty().addListener { _, _, screenInfo ->
                        canvas.width = screenInfo.width / RATIO
                        canvas.height = screenInfo.height.toDouble() / RATIO

                        canvas.scene.window.height = canvas.height
                        canvas.scene.window.width = canvas.width + 65
                    }
                    Thread(readTask).start()
                }
                4 -> {
                    println("success!")
                }
            }
        }

        Thread(initialTask).start()
    }

    @FXML
    fun onMousePressed(event: MouseEvent) {
        println(event)
    }

    @FXML
    fun onMouseMoved(event: MouseEvent) {
        if (!mouseConnected) return
        mouseEventExecutor.submit {
            val buffer = ByteBuffer.allocate(8)
            buffer.putInt((event.x * RATIO).toInt())
            buffer.putInt((event.y * RATIO).toInt())
            mouseOutputStream.write(buffer.array())
        }
    }

    @FXML
    fun onMouseReleased(event: MouseEvent) {
        println(event)
    }

    @FXML
    fun onMouseDragged(event: MouseEvent) {
        if (!mouseConnected) return
        mouseEventExecutor.submit {
            val buffer = ByteBuffer.allocate(8)
            buffer.putInt((event.x * RATIO).toInt())
            buffer.putInt((event.y * RATIO).toInt())
            mouseOutputStream.write(buffer.array())
        }
    }

}
