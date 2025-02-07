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

import android.content.Intent
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import android.util.Log
import androidx.core.telecom.InCallServiceCompat
import androidx.core.telecom.test.Compatibility
import androidx.core.telecom.util.ExperimentalAppActions
import androidx.lifecycle.lifecycleScope
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Implements the InCallService for this application as well as a local ICS binder for activities to
 * bind to this service locally and receive state changes.
 */
class InCallServiceImpl : LocalIcsBinder, InCallServiceCompat() {
    private companion object {
        const val LOG_TAG = "InCallServiceImpl"
    }

    private val localBinder =
        object : LocalIcsBinder.Connector, Binder() {
            override fun getService(): LocalIcsBinder {
                return this@InCallServiceImpl
            }
        }

    private val currId = AtomicInteger(1)
    private val mCallDataAggregator = CallDataAggregator()
    override val callData: StateFlow<List<CallData>> = mCallDataAggregator.callDataState
    private val mMuteStateResolver = MuteStateResolver()

    @Suppress("DEPRECATION")
    private val mCallAudioRouteResolver =
        CallAudioRouteResolver(
            lifecycleScope,
            callData,
            ::setAudioRoute,
            ::requestBluetoothAudio,
            onRequestEndpointChange = { ep, e, or ->
                Compatibility.requestCallEndpointChange(this@InCallServiceImpl, ep, e, or)
            }
        )
    override val isMuted: StateFlow<Boolean> = mMuteStateResolver.muteState
    override val currentAudioEndpoint: StateFlow<CallAudioEndpoint?> =
        mCallAudioRouteResolver.currentEndpoint
    override val availableAudioEndpoints: StateFlow<List<CallAudioEndpoint>> =
        mCallAudioRouteResolver.availableEndpoints

    override fun onBind(intent: Intent?): IBinder? {
        if (intent == null) {
            Log.w(LOG_TAG, "onBind: null intent, returning")
            return null
        }
        if (SERVICE_INTERFACE == intent.action) {
            Log.d(LOG_TAG, "onBind: Received telecom interface.")
            return super.onBind(intent)
        }
        Log.d(LOG_TAG, "onBind: Received bind request from ${intent.`package`}")
        return localBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(LOG_TAG, "onUnbind: Received unbind request from $intent")
        // work around a stupid bug where InCallService assumes that the unbind request can only
        // come from telecom
        if (intent?.action != null) {
            return super.onUnbind(intent)
        }
        return false
    }

    override fun onChangeMuteState(isMuted: Boolean) {
        setMuted(isMuted)
    }

    override suspend fun onChangeAudioRoute(id: String) {
        mCallAudioRouteResolver.onChangeAudioRoute(id)
    }

    private fun readCallIconUriFromFile(uri: Uri): Bitmap? {
        var parcelFileDescriptor: ParcelFileDescriptor? = null
        return try {
            parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r") // "r" for read mode
            parcelFileDescriptor?.let {
                val bitmap = BitmapFactory.decodeFileDescriptor(it.fileDescriptor)
                bitmap // Return the bitmap (Kotlin's last expression is the return value)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try {
                parcelFileDescriptor?.close() // ALWAYS close the ParcelFileDescriptor
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    inner class MyContentObserver(
        handler: Handler,
        private val mUri: Uri,
        private val mCallIconDataEmitter: CallIconExtensionDataEmitter
    ) : ContentObserver(handler) {

        override fun onChange(selfChange: Boolean) {
            onChange(selfChange, null)
        }

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            Log.d(LOG_TAG, "Content changed for URI: $mUri")
            if (uri != null) {
                val bitMap = readCallIconUriFromFile(uri)
                mCallIconDataEmitter.onVoipAppUpdate(bitMap!!)
            }
        }
    }

    @OptIn(ExperimentalAppActions::class)
    override fun onCallAdded(call: Call?) {
        if (call == null) return
        var callJob: Job? = null
        callJob =
            lifecycleScope.launch {
                connectExtensions(call) {
                    val participantsEmitter = ParticipantExtensionDataEmitter()
                    val participantExtension =
                        addParticipantExtension(
                            onActiveParticipantChanged =
                                participantsEmitter::onActiveParticipantChanged,
                            onParticipantsUpdated = participantsEmitter::onParticipantsChanged
                        )

                    val kickParticipantDataEmitter = KickParticipantDataEmitter()
                    val kickParticipantAction = participantExtension.addKickParticipantAction()

                    val raiseHandDataEmitter = RaiseHandDataEmitter()
                    val raiseHandAction =
                        participantExtension.addRaiseHandAction(
                            raiseHandDataEmitter::onRaisedHandsChanged
                        )

                    val localCallSilenceDataEmitter = LocalCallSilenceExtensionDataEmitter()
                    val localCallSilenceExtension =
                        addLocalCallSilenceExtension(
                            onIsLocallySilencedUpdated =
                                localCallSilenceDataEmitter::onVoipAppUpdate
                        )

                    val callIconDataEmitter = CallIconExtensionDataEmitter()

                    var contentObserver: MyContentObserver? = null
                    var observedUri: Uri? = null
                    addCallIconSupport { newUri ->
                        // Check if the URI has changed.  No need to do anything if it's the same.
                        if (newUri != observedUri) {
                            // Unregister the previous observer if it exists.  Use safe call ?. and
                            // let
                            // the platform handle nulls gracefully. No need for !!.
                            contentObserver?.let { contentResolver.unregisterContentObserver(it) }
                            // Create a new observer.  Use a local variable for clarity.
                            val newObserver =
                                MyContentObserver(
                                    Handler(Looper.getMainLooper()),
                                    newUri,
                                    callIconDataEmitter
                                )
                            // Register the new observer.
                            contentResolver.registerContentObserver(newUri, false, newObserver)
                            // Update the tracked observer and URI.
                            contentObserver = newObserver
                            observedUri = newUri
                            // Read the call icon and emit the update.  Use let for concise null
                            // check.
                            readCallIconUriFromFile(newUri)?.let { bitmap ->
                                callIconDataEmitter.onVoipAppUpdate(bitmap)
                            }
                        }
                    }

                    onConnected {
                        val callData = CallDataEmitter(IcsCall(currId.getAndAdd(1), call)).collect()

                        val participantData =
                            participantsEmitter.collect(
                                participantExtension.isSupported,
                                raiseHandDataEmitter.collect(raiseHandAction),
                                kickParticipantDataEmitter.collect(kickParticipantAction)
                            )

                        val localCallSilenceData =
                            localCallSilenceDataEmitter.collect(localCallSilenceExtension)

                        val callIconData = callIconDataEmitter.collect()

                        val fullData =
                            combine(
                                callData,
                                participantData,
                                localCallSilenceData,
                                callIconData
                            ) { cd, partData, silenceData, iconData ->
                                CallData(cd, partData, silenceData, iconData)
                            }
                        mCallDataAggregator.watch(this@launch, fullData)
                    }
                }
                callJob?.cancel("Call Disconnected")
                Log.d(LOG_TAG, "onCallAdded: connectedExtensions complete")
            }
    }

    @Deprecated("Deprecated in API 34")
    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        mMuteStateResolver.onCallAudioStateChanged(audioState)
        mCallAudioRouteResolver.onCallAudioStateChanged(audioState)
    }

    override fun onMuteStateChanged(isMuted: Boolean) {
        mMuteStateResolver.onMuteStateChanged(isMuted)
    }

    override fun onCallEndpointChanged(callEndpoint: CallEndpoint) {
        mCallAudioRouteResolver.onCallEndpointChanged(callEndpoint)
    }

    override fun onAvailableCallEndpointsChanged(availableEndpoints: MutableList<CallEndpoint>) {
        mCallAudioRouteResolver.onAvailableCallEndpointsChanged(availableEndpoints)
    }
}
