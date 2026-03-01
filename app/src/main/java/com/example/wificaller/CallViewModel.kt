package com.example.wificaller
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class CallRequest(
    val host: String,
    val port: Int
)

class CallViewModel : ViewModel() {

    private val _callRequest =
        MutableStateFlow<CallRequest?>(null)

    val callRequest: StateFlow<CallRequest?> =
        _callRequest

    fun requestCall(host: String, port: Int) {
        _callRequest.value = CallRequest(host, port)
    }

    fun clearCallRequest() {
        _callRequest.value = null
    }

    fun sendCallRequest(host: String, port: Int, command: String) {
        Thread {
            try {
                val socket = java.net.Socket(host, port)
                val writer = socket.getOutputStream().bufferedWriter()

                writer.write("$command\n")
                writer.flush()

                socket.close()
            } catch (e: Exception) {
            }
        }.start()
    }
}