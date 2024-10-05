package com.aditya.socialguru.domain_layer.manager

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import com.aditya.socialguru.domain_layer.helper.launchCoroutineInIOThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

object NetworkManager {
    private lateinit var connectivityManager: ConnectivityManager
    private val internetState = MutableSharedFlow<Boolean>(
        replay = 1, // Replay the last value for new subscribers
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            CoroutineScope(Dispatchers.Main).launch {
                internetState.emit(true) // Emit "true" when network is available
            }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            CoroutineScope(Dispatchers.Main).launch {
                internetState.emit(false) // Emit "false" when network is lost
            }
        }
    }



    // Register the network callback in the Application class
    fun registerNetworkCallback(context: Context) {
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerDefaultNetworkCallback(callback)
        launchCoroutineInIOThread {
            internetState.emit(SoftwareManager.isNetworkAvailable(context))
        }
    }

    // Unregister the network callback to avoid leaks
    fun unregisterNetworkCallback() {
        connectivityManager.unregisterNetworkCallback(callback)
    }

    // Shared flow to observe the network status
    fun Context.monitorInternet(): SharedFlow<Boolean> {
        return internetState
    }
}
