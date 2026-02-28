package com.example.wificaller

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

class NsdHelper(
    context: Context,
    private val onDeviceFound: (WifiDevice) -> Unit
) {

    private val TAG = "NSD_HELPER"

    private val SERVICE_TYPE = "_wificaller._tcp"

    private val nsdManager =
        context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private var isDiscovering = false
    private var serviceName: String = ""

    // =========================
    // DISCOVERY
    // =========================

    fun startDiscovery() {

        if (isDiscovering) {
            stopDiscovery()
        }

        nsdManager.discoverServices(
            SERVICE_TYPE,
            NsdManager.PROTOCOL_DNS_SD,
            discoveryListener
        )

        isDiscovering = true
    }

    fun stopDiscovery() {
        try {
            nsdManager.stopServiceDiscovery(discoveryListener)
        } catch (_: Exception) {}

        isDiscovering = false
    }

    // =========================
    // RESOLVE
    // =========================

    private fun resolveService(service: NsdServiceInfo) {

        val resolveListener =
            object : NsdManager.ResolveListener {

                override fun onResolveFailed(
                    serviceInfo: NsdServiceInfo,
                    errorCode: Int
                ) {
                    Log.e(TAG, "Resolve failed: $errorCode")
                }

                override fun onServiceResolved(
                    serviceInfo: NsdServiceInfo
                ) {

                    val host =
                        serviceInfo.host?.hostAddress ?: return

                    val device = WifiDevice(
                        name = serviceInfo.serviceName,
                        host = host,
                        port = serviceInfo.port
                    )

                    onDeviceFound(device)
                }
            }

        nsdManager.resolveService(service, resolveListener)
    }

    // =========================
    // DISCOVERY LISTENER
    // =========================

    private val discoveryListener =
        object : NsdManager.DiscoveryListener {

            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {

                if (!service.serviceType.contains(SERVICE_TYPE))
                    return

                if (service.serviceName == serviceName)
                    return

                resolveService(service)
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.d(TAG, "Service lost")
            }

            override fun onDiscoveryStopped(serviceType: String) {}

            override fun onStartDiscoveryFailed(
                serviceType: String,
                errorCode: Int
            ) {
                stopDiscovery()
            }

            override fun onStopDiscoveryFailed(
                serviceType: String,
                errorCode: Int
            ) {
                stopDiscovery()
            }
        }

    // =========================
    // REGISTER SERVICE
    // =========================

    fun registerService(port: Int) {

        val deviceName = android.os.Build.MODEL

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "WifiCaller_$deviceName"
            serviceType = SERVICE_TYPE
            setPort(port)
        }

        nsdManager.registerService(
            serviceInfo,
            NsdManager.PROTOCOL_DNS_SD,
            registrationListener
        )
    }

    private val registrationListener =
        object : NsdManager.RegistrationListener {

            override fun onServiceRegistered(info: NsdServiceInfo) {
                serviceName = info.serviceName
                Log.d(TAG, "Registered: $serviceName")
            }

            override fun onRegistrationFailed(
                serviceInfo: NsdServiceInfo,
                errorCode: Int
            ) {}

            override fun onServiceUnregistered(
                serviceInfo: NsdServiceInfo
            ) {}

            override fun onUnregistrationFailed(
                serviceInfo: NsdServiceInfo,
                errorCode: Int
            ) {}
        }
}