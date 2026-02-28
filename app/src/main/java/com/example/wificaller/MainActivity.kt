package com.example.wificaller

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import java.net.ServerSocket
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import android.util.Log
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
data class WifiDevice(
    val name: String,
    val host: String,
    val port: Int
)
class MainActivity : ComponentActivity() {

    private lateinit var nsdManager: NsdManager
    private var serverSocket: ServerSocket? = null
    private var localPort: Int = 0
    private var serviceName: String = "WifiCaller"
    private val SERVICE_TYPE = "_wificaller._tcp"
    private val TAG = "NSD_TEST"

    private var isDiscovering = false
    private lateinit var nsdHelper: NsdHelper


    private val discoveredDevices = mutableStateListOf<WifiDevice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager

        nsdHelper = NsdHelper(this) { device ->

            runOnUiThread {
                if (discoveredDevices.none { it.host == device.host }) {
                    discoveredDevices.add(device)
                }
            }
        }
//        initializeServerSocket()

        nsdHelper.startDiscovery()
        startServer()

        setContent {
            WifiUI(
                devices = discoveredDevices,
                onCallClick = { },
                onConnectClick = { },
                connectToDevice = { host, port -> connectToServer(host, port)}
            )
        }
    }

    // ✅ Create server socket

//    private fun initializeServerSocket() {
//        try {
//            serverSocket = ServerSocket(0).also { socket ->
//                localPort = socket.localPort
//                Log.d(TAG, "Server started on port: $localPort")
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Server socket error: ${e.message}")
//        }
//    }

    //
    private var serverThread: Thread? = null

    //
    private fun startServer() {
        if (serverThread?.isAlive == true) return  // Already running

        serverThread = Thread {
            try {
                if (serverSocket == null) {
                    serverSocket = ServerSocket(0)
                    localPort = serverSocket!!.localPort
                }

//                registerService(localPort)
                nsdHelper.registerService(localPort)
                while (true) {
                    val client = serverSocket!!.accept()
                    val writer = client.getOutputStream().bufferedWriter()
                    var message = android.os.Build.MODEL
                    writer.write("$message\n")
                    writer.flush()
                    client.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server error: ${e.message}")
            }
        }.apply { start() }
    }

    //
    private fun connectToServer(host: String, port: Int) {
        Thread {
            try {
                val socket = java.net.Socket(host, port)
                val reader = socket.getInputStream().bufferedReader()
                val message = reader.readLine()
                Log.d(TAG, "Received from server: $message")
                socket.close()
            } catch (e: Exception) {
                Log.e(TAG, "Client error: ${e.message}")
            }
        }.start()
    }
//
//    private fun discoverServices() {
//        if (isDiscovering) {
//            nsdManager.stopServiceDiscovery(discoveryListener)
//            isDiscovering = false
//        }
//
//        nsdManager.discoverServices(
//            SERVICE_TYPE,
//            NsdManager.PROTOCOL_DNS_SD,
//            discoveryListener
//        )
//        isDiscovering = true
//    }
//
//    private fun resolveService(service: NsdServiceInfo) {
//        val listener = object : NsdManager.ResolveListener {
//            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
//                Log.e(TAG, "Resolve failed: $errorCode for ${serviceInfo.serviceName}")
//            }
//
//            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
//                val host = serviceInfo.host?.hostAddress ?: return
//                val port = serviceInfo.port
//                val name = serviceInfo.serviceName
//
//                Log.d(TAG, "Resolved server: $name at $host:$port")
//
//                runOnUiThread {
//                    if (discoveredDevices.none { it.host == host }) {
//                        discoveredDevices.add(WifiDevice(name, host, port))
//                    }
//                }
//            }
//        }
//        nsdManager.resolveService(service, listener)
//    }
//
//    private val discoveryListener = object : NsdManager.DiscoveryListener {
//
//        override fun onDiscoveryStarted(regType: String) {
//            Log.d(TAG, "Discovery started")
//        }
//
//        override fun onServiceFound(service: NsdServiceInfo) {
//            if (service.serviceType.contains(SERVICE_TYPE) && service.serviceName != serviceName) {
//                resolveService(service) // creates a new listener for each resolve
//            }
//        }
//
//        override fun onServiceLost(service: NsdServiceInfo) {
//            Log.e(TAG, "Service lost: $service")
//        }
//
//        override fun onDiscoveryStopped(serviceType: String) {
//            Log.d(TAG, "Discovery stopped")
//        }
//
//        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
//            Log.e(TAG, "Discovery failed: $errorCode")
//            nsdManager.stopServiceDiscovery(this)
//        }
//
//        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
//            Log.e(TAG, "Stop discovery failed: $errorCode")
//            nsdManager.stopServiceDiscovery(this)
//        }
//    }
//
//    // ✅ Registration Listener
//    private val registrationListener = object : NsdManager.RegistrationListener {
//
//        override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
//            serviceName = serviceInfo.serviceName
//            Log.d("NSD_TEST", "Service registered as: $serviceName")
//
//        }
//
//        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
//            Log.e("NSD_TEST", "Registration failed: $errorCode")
//
//        }
//
//        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {}
//
//        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
//    }
//
//    // ✅ Register service
//    private fun registerService(port: Int) {
//        val deviceName = android.os.Build.MODEL
//        val uniqueServiceName = "WifiCaller_$deviceName"
//
//        val serviceInfo = NsdServiceInfo().apply {
//            serviceName = uniqueServiceName
//            serviceType = "_wificaller._tcp"
//            setPort(port)
//        }
//
////        nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager
//        nsdManager.registerService(
//            serviceInfo,
//            NsdManager.PROTOCOL_DNS_SD,
//            registrationListener
//        )
//    }
//}

    @Composable
    fun WifiUI(
        devices: List<WifiDevice>,
        connectToDevice: (String, Int) -> Unit,
        onConnectClick: () -> Unit,
        onCallClick: () -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (devices.isEmpty()) {

                Text(
                    text = "No devices found",
                    style = MaterialTheme.typography.bodyLarge
                )

            }
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(devices) { device ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(device.name)

                        Button(onClick = { connectToDevice(device.host, device.port) }) {
                            Text("Connect")
                        }
                    }
                }
            }
        }

    }
}