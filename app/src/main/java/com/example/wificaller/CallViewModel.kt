package com.example.wificaller
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class CallRequest(
    val host: String,
    val port: Int
)

object CallRepository {

    var myListeningPort: Int = 0

    private val _callPicked = MutableStateFlow(false)
    val callPicked: StateFlow<Boolean> = _callPicked

    fun setCallPicked(value: Boolean) {
        _callPicked.value = value
    }

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
}

class CallViewModel : ViewModel() {

    val callRequest = CallRepository.callRequest

    fun requestCall(host: String, port: Int) {
        CallRepository.requestCall(host, port)
    }

    fun clearCallRequest() {
        CallRepository.clearCallRequest()
    }

    fun sendCallRequest(host: String, port: Int, command: String, myPort:Int) {
        Thread {
            try {
                val socket = java.net.Socket(host, port)
                val writer = socket.getOutputStream().bufferedWriter()

                writer.write("$command|$myPort\n")
                writer.flush()

                socket.close()
            } catch (e: Exception) {
            }
        }.start()
    }
}