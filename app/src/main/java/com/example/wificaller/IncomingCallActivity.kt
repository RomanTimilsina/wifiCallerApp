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

class IncomingCallActivity : ComponentActivity() {
//    private val callViewModel: CallViewModel by viewModels {
//        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
//    }

    private val callViewModel: CallViewModel by viewModels()
    companion object {
        var dropCall: (() -> Unit)? = null

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val host = intent.getStringExtra("HOST") ?: ""
        val port = intent.getIntExtra("PORT", 0)

        dropCall = {
            finish()
        }

        setContent {
            val callRequest by callViewModel.callRequest.collectAsState()

            IncomingCallScreen(
                host = host,
                port = port,
                onPickCall = { connectCall(host, port) },
                onDropCall = {
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