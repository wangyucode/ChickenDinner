package cn.wycode.clientui

import cn.wycode.control.common.MOUSE_PORT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import java.net.Socket

@Component
class Connections {

    lateinit var mouseSocket: Socket
    lateinit var controlSocket: Socket

    suspend fun connectToOverlayServer(){
        withContext(Dispatchers.IO){
            while (true) {
                mouseSocket = Socket("localhost", MOUSE_PORT)
                val signal = mouseSocket.inputStream.read()
                if (signal == 1) break
                delay(200)
            }
        }
    }
}