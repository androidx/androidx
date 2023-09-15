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

package androidx.core.telecom.internal.utils

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallsManager
import androidx.core.telecom.extensions.Capability
import androidx.core.telecom.extensions.CapabilityExchange
import androidx.core.telecom.extensions.CapabilityExchangeListener
import androidx.core.telecom.util.ExperimentalAppActions
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

@ExperimentalAppActions
@RequiresApi(Build.VERSION_CODES.O)
internal class CapabilityExchangeUtils {
    companion object {
        private val TAG = Companion::class.java.simpleName

        /**
         * Timeouts to help facilitate capability exchange negotiation between ICS and VOIP app.
         */
        internal const val CAPABILITY_EXCHANGE_TIMEOUT = 1000L
        internal const val CAPABILITY_NEGOTIATION_COROUTINE_TIMEOUT = 3000L

        /**
         * Internal helper to help facilitate acknowledgement for capability negotiation between
         * the VOIP app and ICS. This helper is invoked on the VOIP side where negotiation begins
         * when we are notified via a call event (containing
         * [CallsManager.EVENT_JETPACK_CAPABILITY_EXCHANGE]). The VOIP side is responsible for
         * informing the ICS of its supported capabilities, receiving the ICS's supported
         * capabilities (ACK), and informing the ICS that negotiation has completed. If the VOIP
         * side is unable to receive the ICS supported capabilities, feature setup will fail and
         * the ICS will report the status for negotiation as successful.
         *
         * @param extras received from call event.
         * @param supportedCapabilities for the VOIP app.
         * @param logTag to help identify if legacy or v2 APIs are being used.
         */
        internal suspend fun initiateVoipAppCapabilityExchange(
            extras: Bundle,
            supportedCapabilities: MutableList<Capability>,
            logTag: String? = TAG
        ) {
            try {
                withTimeout(CAPABILITY_NEGOTIATION_COROUTINE_TIMEOUT) {
                    Log.i(logTag, "Starting capability negotiation with ICS...")
                    var isFeatureSetupComplete = false
                    // Retrieve binder from ICS.
                    val capabilityExchange: CapabilityExchange? = extras.getBinder(
                        CallsManager.EXTRA_CAPABILITY_EXCHANGE_BINDER) as CapabilityExchange?

                    // Initialize capability exchange listener and set it on binder
                    val capabilityExchangeListener = CapabilityExchangeListener()
                    capabilityExchange?.let {
                        capabilityExchange.setListener(capabilityExchangeListener)
                        // Negotiate the supported VOIP app capabilities to the ICS (stub with empty
                        // capabilities until the implementation is supported).
                        capabilityExchange.negotiateCapabilities(supportedCapabilities)
                        // Wait for the ICS to return its supported capabilities and notify that the
                        // setup is complete.
                        if (capabilityExchangeListener.onCapabilitiesNegotiatedLatch
                                .await(CAPABILITY_EXCHANGE_TIMEOUT, TimeUnit.MILLISECONDS)) {
                            capabilityExchange.featureSetupComplete()
                            isFeatureSetupComplete = true
                            Log.i(logTag, "Capability negotiation with ICS has completed.")
                        }
                    }

                    if (!isFeatureSetupComplete) {
                        Log.i(logTag, "Unable to receive supported capabilities from (ICS) client.")
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.i(logTag, "Capability negotiation job timed out in VOIP app side.")
            }
        }
    }
}
