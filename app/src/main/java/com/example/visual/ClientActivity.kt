package com.example.visual
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.zeromq.ZMQ

class ClientActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val button = Button(this).apply {
            text = "Send via ZMQ"
            setOnClickListener {
                Thread {
                    try {
                        val context = ZMQ.context(1)
                        val socket = context.socket(ZMQ.REQ)
                        socket.connect("tcp://192.168.56.1:5555")
                        socket.send("Hello from Android!", 0)
                        val reply = socket.recvStr()
                        runOnUiThread { Toast.makeText(this@ClientActivity, reply, Toast.LENGTH_SHORT).show() }
                        socket.close()
                        context.term()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        runOnUiThread { Toast.makeText(this@ClientActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
                    }
                }.start()
            }
        }
        setContentView(button)
    }
}