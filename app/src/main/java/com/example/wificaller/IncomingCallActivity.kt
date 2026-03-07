package com.example.wificaller


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModelProvider

import android.media.AudioRecord
import android.media.AudioTrack
import android.media.AudioFormat
import android.media.AudioManager
import android.media.MediaRecorder
import java.net.Socket

import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

import kotlinx.coroutines.delay

class IncomingCallActivity : ComponentActivity() {
//    private val callViewModel: CallViewModel by viewModels {
//        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
//    }

    private val callViewModel: CallViewModel by viewModels()
    companion object {
        var dropCall: (() -> Unit)? = null

    }

    var isCalling = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val host = intent.getStringExtra("HOST") ?: ""
        val port = intent.getIntExtra("PORT", 0)

        dropCall = {
            finish()
        }

        fun startSendingAudio(host: String, port: Int) {

            Thread {

                val socket = java.net.Socket(host, port)

                val audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    8000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    1024
                )

                val buffer = ByteArray(1024)

                val output = socket.getOutputStream()

                audioRecord.startRecording()

                while (CallRepository.callPicked.value) {
                    val read = audioRecord.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        output.write(buffer, 0, read)
                    }
                }

            }.start()
        }

        fun startReceivingAudio(socket: Socket) {

            Thread {

                val input = socket.getInputStream()

                val audioTrack = AudioTrack(
                    android.media.AudioManager.STREAM_VOICE_CALL,
                    8000,
                    android.media.AudioFormat.CHANNEL_OUT_MONO,
                    android.media.AudioFormat.ENCODING_PCM_16BIT,
                    1024,
                    android.media.AudioTrack.MODE_STREAM
                )

                val buffer = ByteArray(1024)

                audioTrack.play()

                while (CallRepository.callPicked.value) {

                    val read = input.read(buffer)

                    if (read > 0) {
                        audioTrack.write(buffer, 0, read)
                    }

                }

            }.start()
        }



        setContent {
            val callRequest by callViewModel.callRequest.collectAsState()
            val callPicked by CallRepository.callPicked.collectAsState()
            IncomingCallScreen(
                callPicked = callPicked,
                host = host,
                port = port,
                onPickCall = {
                    CallRepository.setCallPicked(true)

                    Thread {
                        val socket = Socket(host, 8000)

                        startReceivingAudio(socket)

                    }.start()

                    startSendingAudio(host, 8000)
                    callViewModel.sendCallRequest(host, port, MainActivity.constants.CALL_ACCEPT, CallRepository.myListeningPort)

                    connectCall(host, port)
                             },
                onDropCall = {
                    CallRepository.setCallPicked(false)
                    finish()
                    // Send CALL_DROP to the other device
                    callViewModel.sendCallRequest(host, port, MainActivity.constants.CALL_DROP, CallRepository.myListeningPort)

                    // Clear local call state
                    callViewModel.clearCallRequest()

                    // Close this activity only

                }
            )

            // Auto-close if the call is dropped remotely
            LaunchedEffect(callRequest) {
                if (callRequest == null) {
                    finish()
                }
            }

        }
    }

    private fun connectCall(host: String, port: Int) {
        // TODO: start socket connection here
        println("Connecting to $host:$port")

    }
}

@Composable
fun IncomingCallScreen(
    callPicked:Boolean,
    host: String,
    port: Int,
    onPickCall: () -> Unit,
    onDropCall: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Incoming Call",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Host: $host")
        Text("Port: $port")

        Spacer(modifier = Modifier.height(40.dp))

        if (callPicked) {

            var seconds by remember { mutableStateOf(0) }

            LaunchedEffect(Unit) {
                while (true) {
                    kotlinx.coroutines.delay(1000)
                    seconds++
                }
            }

            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            val timeText = String.format("%02d:%02d", minutes, remainingSeconds)

            Text(
                text = timeText,
                style = MaterialTheme.typography.headlineMedium
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            Button(
                onClick = onPickCall
            ) {
                Text("Pick Call")
            }

            Button(
                onClick = onDropCall
            ) {
                Text("Drop Call")
            }
        }
    }
}