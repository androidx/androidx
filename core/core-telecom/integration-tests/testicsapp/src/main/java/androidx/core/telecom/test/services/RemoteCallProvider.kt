/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.core.telecom.test.services

import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.getAndUpdate

data class LocalServiceConnection(
    val isConnected: Boolean,
    val context: Context? = null,
    val serviceConnection: ServiceConnection? = null,
    val connection: LocalIcsBinder? = null
)

/**
 * Manages the connection for the Provider of "remote" calls, which are calls from the app's
 * [InCallServiceImpl].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RemoteCallProvider {
    private companion object {
        const val LOG_TAG = "RemoteCallProvider"
    }

    private val connectedService: MutableStateFlow<LocalServiceConnection> =
        MutableStateFlow(LocalServiceConnection(false))

    /** Bind to the app's [LocalIcsBinder.Connector] Service implementation */
    fun connectService(context: Context) {
        if (connectedService.value.isConnected) return
        val intent = Intent(context, InCallServiceImpl::class.java)
        val serviceConnection =
            object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    if (service == null) return
                    val localService = service as LocalIcsBinder.Connector
                    connectedService.value =
                        LocalServiceConnection(true, context, this, localService.getService())
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    // Unlikely since the Service is in the same process. Re-evaluate if the service
                    // is moved to another process.
                    Log.w(LOG_TAG, "onServiceDisconnected: Unexpected call")
                }
            }
        Log.i(LOG_TAG, "connectToIcs: Binding to ICS locally")
        context.bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    /** Disconnect from the app;s [LocalIcsBinder.Connector] Service implementation */
    fun disconnectService() {
        val localConnection = connectedService.getAndUpdate { LocalServiceConnection(false) }
        localConnection.serviceConnection?.let { conn ->
            Log.i(LOG_TAG, "connectToIcs: Unbinding to ICS locally")
            localConnection.context?.unbindService(conn)
        }
    }

    /**
     * Stream the [CallData] representing each active Call on the device. The Flow will be empty
     * until the remote Service connects.
     */
    fun streamCallData(): Flow<List<CallData>> {
        return connectedService.flatMapConcat { conn ->
            if (!conn.isConnected) {
                emptyFlow()
            } else {
                conn.connection?.callData ?: emptyFlow()
            }
        }
    }

    /**
     * Stream the global mute state of the device. The Flow will be empty until the remote Service
     * connects.
     */
    fun streamMuteData(): Flow<Boolean> {
        return connectedService.flatMapConcat { conn ->
            if (!conn.isConnected) {
                emptyFlow()
            } else {
                conn.connection?.isMuted ?: emptyFlow()
            }
        }
    }

    /**
     * Stream the [CallAudioEndpoint] representing the current endpoint of the active call. The Flow
     * will be empty until the remote Service connects.
     */
    fun streamCurrentEndpointData(): Flow<CallAudioEndpoint?> {
        return connectedService.flatMapConcat { conn ->
            if (!conn.isConnected) {
                emptyFlow()
            } else {
                conn.connection?.currentAudioEndpoint ?: emptyFlow()
            }
        }
    }

    /**
     * Stream the List of [CallAudioEndpoint]s representing the available endpoints of the active
     * call. The Flow will be empty until the remote Service connects.
     */
    fun streamAvailableEndpointData(): Flow<List<CallAudioEndpoint>> {
        return connectedService.flatMapConcat { conn ->
            if (!conn.isConnected) {
                emptyFlow()
            } else {
                conn.connection?.availableAudioEndpoints ?: emptyFlow()
            }
        }
    }

    /** Request to change the global mute state of the device. */
    fun onChangeMuteState(isMuted: Boolean) {
        val service = connectedService.value
        if (!service.isConnected) return
        service.connection?.onChangeMuteState(isMuted)
    }

    /** Request to change the current audio route of the active call. */
    suspend fun onChangeAudioRoute(id: String) {
        val service = connectedService.value
        if (!service.isConnected) return
        service.connection?.onChangeAudioRoute(id)
    }
}
