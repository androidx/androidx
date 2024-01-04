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

/**
 * This class defines the Jetpack ICS layer which will be leveraged as part of supporting VOIP app
 * actions.
 */
@RequiresApi(Build.VERSION_CODES.M)
internal class InCallServiceCompat(context: Context) : InCallService() {
    private val mContext: Context = context

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
    @RequiresApi(Build.VERSION_CODES.O)
    @CapabilityExchangeType
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
}
