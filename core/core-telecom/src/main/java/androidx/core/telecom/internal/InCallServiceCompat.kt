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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.PhoneAccount
import android.telecom.TelecomManager
import android.util.Log
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.telecom.CallsManager
import androidx.core.telecom.extensions.Capability
import androidx.core.telecom.extensions.CapabilityExchange
import androidx.core.telecom.internal.utils.CapabilityExchangeUtils
import androidx.core.telecom.util.ExperimentalAppActions
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout

/**
 * This class defines the Jetpack ICS layer which will be leveraged as part of supporting VOIP app
 * actions.
 */
@ExperimentalAppActions
@RequiresApi(Build.VERSION_CODES.O)
internal class InCallServiceCompat(context: Context) : InCallService() {
    private val mContext: Context = context
    private val mSupportedCapabilities = mutableListOf(Capability())

    companion object {
        /**
         * Constants used to denote the extension level supported by the VOIP app.
         */
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(NONE, EXTRAS, CAPABILITY_EXCHANGE, UNKNOWN)
        internal annotation class CapabilityExchangeType

        internal const val NONE = 0
        internal const val EXTRAS = 1
        internal const val CAPABILITY_EXCHANGE = 2
        internal const val UNKNOWN = 3

        /**
         * Current capability exchange version
         */
        internal const val CAPABILITY_EXCHANGE_VERSION = 1

        private val TAG = InCallServiceCompat::class.simpleName
    }

    fun onCreateCall(call: Call): CallCompat {
        Log.d(TAG, "onCreateCall: call = $call")
        return with(this) {
            CallCompat(call) {
            }
        }
    }

    fun onRemoveCall(call: CallCompat) {
        Log.d(TAG, "onRemoveCall: call = $call")
    }

    /**
     * Internal helper used by the [InCallService] to help resolve the call extension type. This
     * is invoked before capability exchange between the [InCallService] and VOIP app starts to
     * ensure the necessary features are enabled to support it.
     *
     * If the call is placed using the V1.5 ConnectionService + Extensions Library (Auto Case), the
     * call will have the [CallsManager.EXTRA_VOIP_API_VERSION] defined in the extras. The call
     * extension would be resolved as [InCallServiceCompat.EXTRAS].
     *
     * If the call is using the v2 APIs and the phone account associated with the call supports
     * transactional ops (U+) or the call has the [CallsManager.PROPERTY_IS_TRANSACTIONAL] property
     * defined (on V devices), then the extension type is [InCallServiceCompat.CAPABILITY_EXCHANGE].
     *
     * If the call is added via CallsManager#addCall on pre-U devices and the
     * [CallsManager.EXTRA_VOIP_BACKWARDS_COMPATIBILITY_SUPPORTED] is present in the call extras,
     * the extension type also resolves to [InCallServiceCompat.CAPABILITY_EXCHANGE].
     *
     * In the case that none of the cases above apply and the phone account is found not to support
     * transactional ops (assumes that caller has [android.Manifest.permission.READ_PHONE_NUMBERS]
     * permission), then the extension type is [InCallServiceCompat.NONE].
     *
     * If the caller does not have the required permission to retrieve the phone account, then
     * the extension type will be [InCallServiceCompat.UNKNOWN], until it can be resolved.
     *
     * @param call to resolve the extension type for.
     * @return the extension type [InCallServiceCompat.CapabilityExchangeType] resolved for the
     * call.
     */
    internal fun resolveCallExtensionsType(call: Call): Int {
        var callDetails = call.details
        val callExtras = callDetails?.extras ?: Bundle()

        if (callExtras.containsKey(CallsManager.EXTRA_VOIP_API_VERSION)) {
            return EXTRAS
        }
        if (callDetails?.hasProperty(CallsManager.PROPERTY_IS_TRANSACTIONAL) == true || callExtras
            .containsKey(CallsManager.EXTRA_VOIP_BACKWARDS_COMPATIBILITY_SUPPORTED)) {
            return CAPABILITY_EXCHANGE
        }
        // Verify read phone numbers permission to see if phone account supports transactional ops.
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_PHONE_NUMBERS)
            == PackageManager.PERMISSION_GRANTED) {
            var telecomManager = mContext.getSystemService(Context.TELECOM_SERVICE)
                as TelecomManager
            var phoneAccount = telecomManager.getPhoneAccount(callDetails?.accountHandle)
            if (phoneAccount?.hasCapabilities(
                    PhoneAccount.CAPABILITY_SUPPORTS_TRANSACTIONAL_OPERATIONS) == true) {
                return CAPABILITY_EXCHANGE
            } else {
                return NONE
            }
        }

        Log.i(TAG, "Unable to resolve call extension type. Returning $UNKNOWN.")
        return UNKNOWN
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
     * @param call to initiate capability exchange for.
     * @return the capability negotiation status.
     * between the ICS and VOIP app.
     */
    internal suspend fun initiateICSCapabilityExchange(call: Call): Boolean {
        Log.i(TAG, "initiateICSCapabilityExchange: " +
            "Starting capability negotiation with VOIP app...")

        // Initialize binder for facilitating IPC (capability exchange) between ICS and VOIP app
        // and notify VOIP app via a call event.
        val capExchange = CapabilityExchange()
        val extras = Bundle()
        extras.putBinder(CallsManager.EXTRA_CAPABILITY_EXCHANGE_BINDER, capExchange)
        extras.putInt(CallsManager.EXTRA_CAPABILITY_EXCHANGE_VERSION, CAPABILITY_EXCHANGE_VERSION)
        call.sendCallEvent(CallsManager.EVENT_JETPACK_CAPABILITY_EXCHANGE, extras)

        // Launch a new coroutine from the context of the current coroutine and wait for task to
        // complete.
        return CoroutineScope(coroutineContext).async {
            beginCapabilityNegotiationAck(capExchange)
        }.await()
    }

    /**
     * Helper to start acknowledgement process for capability negotiation between the ICS and VOIP
     * app.
     */
    private suspend fun beginCapabilityNegotiationAck(capExchange: CapabilityExchange): Boolean {
        var negotiationAckStatus = false
        try {
            withTimeout(CapabilityExchangeUtils.CAPABILITY_NEGOTIATION_COROUTINE_TIMEOUT) {
                // Wait for VOIP app to return its supported capabilities.
                if (capExchange.negotiatedCapabilitiesLatch.await(
                        CapabilityExchangeUtils.CAPABILITY_EXCHANGE_TIMEOUT,
                        TimeUnit.MILLISECONDS)) {
                    // Respond back to the VOIP app with the InCallService's supported
                    // capabilities (stub empty capabilities until implementation is supported).
                    capExchange.capabilityExchangeListener
                        .onCapabilitiesNegotiated(mSupportedCapabilities)
                    // Ensure that feature setup is signaled from VOIP app side.
                    if (capExchange.featureSetUpCompleteLatch.await(
                            CapabilityExchangeUtils.CAPABILITY_EXCHANGE_TIMEOUT,
                            TimeUnit.MILLISECONDS)) {
                        Log.i(TAG, "initiateICSCapabilityExchange: " +
                            "Completed capability exchange feature set up.")
                        negotiationAckStatus = true
                    }
                }

                // Report negotiation acknowledgement failure, if it occurred.
                if (!negotiationAckStatus) {
                    Log.i(TAG, "initiateICSCapabilityExchange: Unable to complete capability " +
                            "exchange feature set up.")
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.i(TAG, "initiateICSCapabilityExchange: Capability negotiation job timed " +
                "out in ICS side.")
        }
        return negotiationAckStatus
    }
}
