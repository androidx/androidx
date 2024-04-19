/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.core.telecom.internal

import android.os.Build
import android.os.Bundle
import android.telecom.Call
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.core.telecom.CallsManager
import androidx.core.telecom.extensions.Capability
import androidx.core.telecom.extensions.CapabilityExchange
import androidx.core.telecom.extensions.ParticipantClientActions
import androidx.core.telecom.extensions.ParticipantClientActionsImpl
import androidx.core.telecom.internal.utils.CapabilityExchangeUtils
import androidx.core.telecom.util.ExperimentalAppActions
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout

@ExperimentalAppActions
@RequiresApi(Build.VERSION_CODES.O)
internal class CallCompat(
    private val call: Call,
    val scope: CoroutineScope,
) {
    internal val icsCapabilities = mutableListOf<Capability>()

    @VisibleForTesting
    internal var capExchangeSetupComplete = false

    internal lateinit var onParticipantInitializationComplete: (ParticipantClientActions) -> Unit
    internal lateinit var participantStateListener: ParticipantClientActionsImpl

    companion object {
        /**
         * Current capability exchange version
         */
        internal const val CAPABILITY_EXCHANGE_VERSION = 1

        private val TAG = CallCompat::class.simpleName

        fun toCallCompat(call: Call, scope: CoroutineScope, init: CallCompat.() -> Unit):
            CallCompat {
            Log.i(TAG, "toCallCompat; call = $call")
            val callCompat = CallCompat(call, scope)
            callCompat.init()
            return callCompat
        }
    }

    fun toCall(): Call {
        return call
    }

    internal fun getParticipantClientActions(): Result<ParticipantClientActions> {
        Log.i(TAG, "getParticipantClientActions")
        return if (this::participantStateListener.isInitialized) {
            if (participantStateListener.mIsInitializationComplete) {
                Result.success(participantStateListener)
            } else {
                Result.failure(IllegalAccessException("ParticipantClientActions not setup yet."))
            }
        } else {
            Result.failure(IllegalAccessException("The participantStateListener field in " +
                "CallCompat was not initialized."))
        }
    }

    internal fun addCapability(capability: Capability) {
        Log.i(TAG, "addCapability capability = $capability")
        // This is called by extensions to include their capabilities to the call.
        icsCapabilities.add(capability)
    }

    internal fun addExtension(onInitializationComplete: (pca: ParticipantClientActions) -> Unit) {
        Log.i(TAG, "addExtension")
        onParticipantInitializationComplete = onInitializationComplete
    }

    /**
     * Initiate capability exchange negotiation between ICS and VOIP app. The acknowledgement begins
     * when the ICS sends a call event with [CallsManager.EVENT_JETPACK_CAPABILITY_EXCHANGE] to
     * notify the VOIP app to begin capability exchange negotiation. At that point, 3 stages of
     * acknowledgement are required between the two parties in order for negotiation to succeed.
     *
     * This entails the ICS side waiting for the VOIP app to communicate its supported capabilities,
     * the VOIP side waiting for the ICS side to communicate its supported capabilities, and the
     * VOIP side signaling the ICS side that feature setup (negotiation) is complete. If any one of
     * the aforementioned stages of ACK fails (i.e. timeout), the negotiation will fail.
     *
     * Note: Negotiation is only supported by InCallServices that support capability exchange
     * ([InCallServiceCompat.CAPABILITY_EXCHANGE]).
     *
     * @return the capability negotiation status.
     * between the ICS and VOIP app.
     */
    internal suspend fun startCapabilityExchange() {
        Log.i(TAG, "startCapabilityExchange: Starting capability negotiation with VOIP app...")

        // Initialize binder for facilitating IPC (capability exchange) between ICS and VOIP app
        // and notify VOIP app via a call event.
        val capExchange = CapabilityExchange()
        val extras = Bundle()
        extras.putBinder(CallsManager.EXTRA_CAPABILITY_EXCHANGE_BINDER, capExchange)
        extras.putInt(
            CallsManager.EXTRA_CAPABILITY_EXCHANGE_VERSION,
            CAPABILITY_EXCHANGE_VERSION
        )
        call.sendCallEvent(CallsManager.EVENT_JETPACK_CAPABILITY_EXCHANGE, extras)

        // Launch a new coroutine from the context of the current coroutine and wait for task to
        // complete.
        scope.async {
            beginCapabilityNegotiationAck(capExchange)
        }.await()
    }

    /**
     * Helper to start acknowledgement process for capability negotiation between the ICS and VOIP
     * app.
     */
    private suspend fun beginCapabilityNegotiationAck(capExchange: CapabilityExchange) {
        Log.i(TAG, "beginCapabilityNegotiationAck")

        try {
            withTimeout(CapabilityExchangeUtils.CAPABILITY_NEGOTIATION_COROUTINE_TIMEOUT) {
                // Wait for VOIP app to return its supported capabilities.
                if (capExchange.beginExchangeLatch.await(
                        CapabilityExchangeUtils.CAPABILITY_EXCHANGE_TIMEOUT,
                        TimeUnit.MILLISECONDS)) {
                    Log.i(TAG, "beginCapabilityNegotiationAck beginExchange returned from " +
                        "the VOIP side.")

                    setupSupportedCapabilities(capExchange)

                    Log.i(TAG, "beginCapabilityNegotiationAck: " +
                        "Completed capability exchange feature set up.")
                    capExchangeSetupComplete = true
                }
            }
        } catch (e: Exception) {
            when (e) {
                is CancellationException -> {
                    Log.i(
                        TAG, "beginCapabilityNegotiationAck: Capability negotiation job " +
                            "timed out in ICS side."
                    )
                    completeParticipantCapExchangeUnsupported()
                    // Todo: complete other extensions exceptionally
                }
                else -> {
                    // Handle the case where the VOIP app dies:
                    Log.i(
                        TAG, "beginCapabilityNegotiationAck: Remote party threw exception = $e"
                    )
                    completeParticipantCapExchangeUnsupported()
                    // Todo: complete other extensions exceptionally
                }
            }
        }
    }

    /***********************************************************************************************
     *                           Helpers
     *********************************************************************************************/

    internal fun setupSupportedCapabilities(capExchange: CapabilityExchange) {
        val voipCaps: Set<Capability> = capExchange.voipCapabilities.toSet()
        for (icsCap in icsCapabilities) {
            // Check if the VoIP app supports this capability:
            val voipCap: Capability? = voipCaps.find {
                it.featureId == icsCap.featureId
            }

            // If so, then initialize the listener and send the relevant callback:
            if (voipCap != null) {
                val negotiatedActions = icsCap.supportedActions
                    .intersect(voipCap.supportedActions.toSet())
                val minExtVersion = min(icsCap.featureVersion,
                    voipCap.featureVersion)

                when (icsCap.featureId) {
                    CallsManager.PARTICIPANT -> initializeParticipantListenerAndInformVoipApp(
                        negotiatedActions, minExtVersion, capExchange)
                    CallsManager.CALL_ICON -> initializeCallIconListenerAndInformVoipApp(
                        negotiatedActions, minExtVersion, capExchange)
                }
            } else {
                when (icsCap.featureId) {
                    CallsManager.PARTICIPANT -> completeParticipantCapExchangeUnsupported()
                    CallsManager.CALL_ICON -> completeCallIconCapExchangeUnsupported()
                }
            }
        }
    }

    private fun initializeParticipantListenerAndInformVoipApp(
        negotiatedParticipantActions: Set<Int>,
        minVersion: Int,
        capExchange: CapabilityExchange
    ) {
        participantStateListener = ParticipantClientActionsImpl(scope, negotiatedParticipantActions,
            onParticipantInitializationComplete)
        capExchange.capabilityExchangeListener.onCreateParticipantExtension(
            minVersion,
            negotiatedParticipantActions.toIntArray(),
            participantStateListener)
    }

    private fun initializeCallIconListenerAndInformVoipApp(
        negotiatedCallIconActions: Set<Int>,
        minVersion: Int,
        capExchange: CapabilityExchange
    ) {
        Log.i(TAG, "initializeCallIconListenerAndInformVoipApp: size of negotiatedActions" +
            " = ${negotiatedCallIconActions.size}, version = $minVersion, " +
            "capExchange = $capExchange")
        // Todo: initialize ICallDetailsListener and send onCreateCallDetailsExtension.
    }

    private fun completeParticipantCapExchangeUnsupported() {
        // complete the call cap exchange exceptionally and let Telecom take care of the cleanup:
        participantStateListener = ParticipantClientActionsImpl(scope, emptySet()) {}
        participantStateListener.mIsParticipantExtensionSupported = false
        onParticipantInitializationComplete(participantStateListener)
    }

    private fun completeCallIconCapExchangeUnsupported() {
        Log.i(TAG, "completeCallIconCapExchangeUnsupported")
        // Todo: inform the ICS app that voip doesn't support CallDetails extension
    }
}
