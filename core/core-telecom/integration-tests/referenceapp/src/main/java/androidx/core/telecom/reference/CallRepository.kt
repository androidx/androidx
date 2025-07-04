/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.core.telecom.reference

import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.reference.model.CallData
import androidx.core.telecom.reference.service.LocalServiceBinder
import androidx.core.telecom.reference.service.TelecomVoipService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CallRepository {

    companion object {
        const val LOG_TAG = "CallRepository"
    }

    // --- State Management ---
    private var mIsBound = false
    private var mBinder: LocalServiceBinder? = null // Store the Binder interface directly
    private var mBoundContext: Context? = null // Keep track of the context used for unbinding

    // --- Coroutine Scope & Job ---
    // Use SupervisorJob so failure in collection doesn't cancel the whole scope
    private val mRepositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var mServiceCollectionJob: Job? = null // Keep track of the collection job

    // --- Service Connection ---
    // Define ServiceConnection as a stable member variable
    private val mServiceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                Log.i(LOG_TAG, "Service connected: $name")
                if (service == null) {
                    Log.w(LOG_TAG, "onServiceConnected: IBinder service is null!")
                    // Handle error? Maybe attempt rebind?
                    mIsBound = false
                    return
                }
                try {
                    // Cast to the Binder interface defined in your service
                    mBinder = (service as LocalServiceBinder.Connector).getService()
                    mIsBound = true
                    Log.i(LOG_TAG, "Service bound successfully.")

                    // Start collecting updates from the service's StateFlow
                    startCollectingFromService()
                } catch (e: ClassCastException) {
                    Log.e(
                        LOG_TAG,
                        "onServiceConnected: Error casting IBinder to LocalIcsBinder.Connector",
                        e,
                    )
                    mIsBound = false // Failed to get binder
                    // Might need to unbind here if appropriate
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                // Called when the connection is unexpectedly lost (e.g., service crashed).
                // NOT usually called on explicit unbindService().
                Log.w(LOG_TAG, "Service unexpectedly disconnected: $name")
                mIsBound = false
                mBinder = null
                mServiceCollectionJob?.cancel() // Stop collecting if service dies
                mServiceCollectionJob = null
                mBoundContext = null
            }
        }

    /** Private backing property for the callDataFlow. */
    private val _callDataFlow = MutableStateFlow<List<CallData>>(emptyList())

    /** Public read-only property to access the StateFlow of CallData updates. */
    val callDataFlow: StateFlow<List<CallData>> = _callDataFlow.asStateFlow()

    /**
     * Ensures the TelecomVoipService is started and binds to it. This method is idempotent; safe to
     * call multiple times.
     */
    fun maybeConnectService(context: Context) {
        if (mIsBound) {
            Log.d(LOG_TAG, "connectService: Already bound.")
            return // Already connected
        }

        val applicationContext = context.applicationContext
        val intent = Intent(applicationContext, TelecomVoipService::class.java)
        Log.i(LOG_TAG, "connectService: Attempting to start and bind to VoipService.")

        try {
            // 1. Ensure the service is started using startService.
            // This keeps the service running even if all clients unbind,
            // until stopSelf() or stopService() is called.
            applicationContext.startService(intent)
            Log.d(LOG_TAG, "startService called.")

            // 2. Bind to the service
            val didBind =
                applicationContext.bindService(
                    intent,
                    mServiceConnection, // Use the stable member variable
                    BIND_AUTO_CREATE,
                )

            if (didBind) {
                Log.d(LOG_TAG, "bindService call initiated successfully.")
                mBoundContext = applicationContext // Store context for unbinding
                // Note: isBound will be set true in onServiceConnected
            } else {
                Log.e(
                    LOG_TAG,
                    "bindService call returned false. Service might not be" + " available.",
                )
                // Failed to initiate binding. Stop service
                applicationContext.stopService(intent)
            }
        } catch (e: SecurityException) {
            Log.e(
                LOG_TAG,
                "connectService: Failed to start/bind service due to" +
                    " SecurityException. Check permissions.",
                e,
            )
            // Handle lack of permissions (rare for same-app service)
        } catch (e: IllegalStateException) {
            Log.e(
                LOG_TAG,
                "connectService: Failed to start/bind service due to" + " IllegalStateException.",
                e,
            )
            // Can happen if trying to startForegroundService from background without permission
        } catch (e: Exception) {
            Log.e(LOG_TAG, "connectService: Unexpected error starting/binding" + " service.", e)
        }
    }

    /**
     * Unbinds from the TelecomVoipService if currently bound and no active calls exist. Does NOT
     * stop the service; the service should manage its own shutdown via stopSelf().
     */
    fun maybeDisconnectService() {
        // Prevent disconnecting if there are active calls being tracked
        if (_callDataFlow.value.isNotEmpty()) {
            Log.w(LOG_TAG, "disconnectService: Skipping disconnect - active calls" + " detected.")
            return
        }

        if (mIsBound && mBoundContext != null) {
            Log.i(LOG_TAG, "disconnectService: Unbinding from VoipService.")
            try {
                mBoundContext?.unbindService(mServiceConnection)
            } catch (e: IllegalArgumentException) {
                Log.w(
                    LOG_TAG,
                    "disconnectService: ServiceConnection not registered?" + " Already unbound?",
                    e,
                )
            }
            mIsBound = false
            mBinder = null
            mServiceCollectionJob?.cancel() // Ensure collection stops
            mServiceCollectionJob = null
            mBoundContext = null
        } else {
            Log.d(LOG_TAG, "disconnectService: Not currently bound.")
        }
    }

    /** Starts collecting updates from the service's StateFlow. */
    private fun startCollectingFromService() {
        val serviceBinder = mBinder
        if (serviceBinder == null) {
            Log.w(LOG_TAG, "startCollectingFromService: Binder is null, cannot collect.")
            return
        }

        // Cancel any previous job before starting a new one
        mServiceCollectionJob?.cancel()

        mServiceCollectionJob =
            mRepositoryScope.launch {
                Log.i(LOG_TAG, "Starting collection from service flow")
                serviceBinder.callDataUpdates.collect { dataList ->
                    Log.v(LOG_TAG, "Received data update from service: ${dataList.size} calls")
                    _callDataFlow.value = dataList // Update the repository's StateFlow
                }
            }
        Log.d(LOG_TAG, "Collection job started: $mServiceCollectionJob")
    }

    // --- Service Interaction Methods ---

    fun addOutgoingCall(callAttributesCompat: CallAttributesCompat) {
        if (!mIsBound || mBinder == null) {
            Log.w(LOG_TAG, "addOutgoingCall: Service is not connected/bound.")
            return
        }
        mBinder?.addCall(callAttributesCompat, getNextNotificationId())
    }

    fun onIncomingCallDetected(attributes: CallAttributesCompat, id: Int) {
        if (!mIsBound || mBinder == null) {
            Log.w(LOG_TAG, "onIncomingCallDetected: Service is not connected/bound.")
            return
        }
        Log.i(LOG_TAG, "onIncomingCallDetected: ")
        mBinder?.addCall(attributes, id)
    }

    fun setCallActive(callId: String) {
        if (!mIsBound || mBinder == null) {
            Log.w(LOG_TAG, "setCallActive: Service is not connected/bound.")
            return
        }
        mBinder?.setCallActive(callId)
    }

    fun setCallInactive(callId: String) {
        if (!mIsBound || mBinder == null) {
            Log.w(LOG_TAG, "setCallInactive: Service is not connected/bound.")
            return
        }
        mBinder?.setCallInactive(callId)
    }

    fun endCall(callId: String) {
        if (!mIsBound || mBinder == null) {
            Log.w(LOG_TAG, "endCall: Service is not connected/bound.")
            return
        }
        mBinder?.endCall(callId)
    }

    fun switchCallEndpoint(callId: String, endpointCompat: CallEndpointCompat) {
        if (!mIsBound || mBinder == null) {
            Log.w(LOG_TAG, "switchCallEndpoint: Service is not connected/bound.")
            return
        }
        mBinder?.switchCallEndpoint(callId, endpointCompat)
    }

    fun toggleGlobalMute(isMuted: Boolean) {
        if (!mIsBound || mBinder == null) {
            Log.w(LOG_TAG, "toggleGlobalMute: Service is not connected/bound.")
            return
        }
        mBinder?.toggleGlobalMute(isMuted)
    }

    fun toggleLocalCallSilence(callId: String, isMuted: Boolean) {
        if (!mIsBound || mBinder == null) {
            Log.w(LOG_TAG, "toggleLocalCallSilence: Service is not connected/bound.")
            return
        }
        mBinder?.toggleLocalCallSilence(callId, isMuted)
    }

    fun addParticipant(callId: String) {
        if (!mIsBound || mBinder == null) {
            Log.w(LOG_TAG, "addParticipant: Service is not connected/bound.")
            return
        }
        mBinder?.addParticipant(callId)
    }

    fun removeParticipant(callId: String) {
        if (!mIsBound || mBinder == null) {
            Log.w(LOG_TAG, "kickParticipant: Service is not connected/bound.")
            return
        }
        mBinder?.removeParticipant(callId)
    }

    fun changeCallIcon(callId: String) {
        if (!mIsBound || mBinder == null) {
            Log.w(LOG_TAG, "changeCallIcon: Service is not connected/bound.")
            return
        }
        mBinder?.changeCallIcon(callId)
    }
}
