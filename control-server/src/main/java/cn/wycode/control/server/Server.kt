@file:JvmName("ControlServer")

package cn.wycode.control.server

import android.net.LocalServerSocket
import android.net.LocalSocket
import cn.wycode.control.server.utils.Ln
import java.io.File
import java.io.IOException

const val CONTROL_PATH = "/data/local/tmp/controller.jar"
const val CONTROL_SOCKET = "control-socket"

var ENABLE_LOG = false

class Server {

    private lateinit var controlSocket: LocalSocket

    @Throws(IOException::class)
    fun start() {
        val serverSocket = LocalServerSocket(CONTROL_SOCKET)
        Ln.d("started!")
        controlSocket = serverSocket.accept()
        controlSocket.outputStream.write(1)

        Controller(controlSocket.inputStream).run()
    }

}

fun main(args: Array<String>) {
    Thread.setDefaultUncaughtExceptionHandler { t, e ->
        Ln.e("Exception on thread $t", e)
    }
    //deleteSelf()
    resolveArguments(args)
    Server().start()
}

fun resolveArguments(args: Array<String>) {
    if (args.isEmpty()) return
    if (args[0] == "--debug") ENABLE_LOG = true

}

fun deleteSelf() {
    File(CONTROL_PATH).delete();
}

//        Log.d("wy---->", "start!"+ Arrays.toString(args));
//        long now = SystemClock.uptimeMillis();
//        try {
//            Class inputManagerClass = InputManager.class;
//            Method getInstance = inputManagerClass.getDeclaredMethod("getInstance");
//            InputManager inputManager = (InputManager) getInstance.invoke(inputManagerClass);
//            Method injectInputEvent = inputManagerClass.getMethod("injectInputEvent", InputEvent.class, int.class);
//            MotionEvent.class.getDeclaredMethod("obtain").setAccessible(true);
//            MotionEvent event1 = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 100,100, 0);
//            MotionEvent event2 = MotionEvent.obtain(now+10, now+10, MotionEvent.ACTION_MOVE, 200,200, 0);
//            MotionEvent event3 = MotionEvent.obtain(now+20, now+20, MotionEvent.ACTION_UP, 200,200, 0);
//            event1.setSource(4098);
//            event2.setSource(4098);
//            event3.setSource(4098);
//            injectInputEvent.invoke(inputManager, event1, 0);
//            injectInputEvent.invoke(inputManager, event2, 0);
//            injectInputEvent.invoke(inputManager, event3, 0);
//            Log.d("wy---->", "invoke!");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }


