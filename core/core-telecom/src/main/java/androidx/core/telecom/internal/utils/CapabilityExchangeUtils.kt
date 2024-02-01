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
import android.os.RemoteException
import android.util.Log
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.core.telecom.CallsManager
import androidx.core.telecom.extensions.voip.VoipExtensionManager
import androidx.core.telecom.util.ExperimentalAppActions

@ExperimentalAppActions
@RequiresApi(Build.VERSION_CODES.O)
internal class CapabilityExchangeUtils {
    companion object {
        /**
         * Timeouts to help facilitate capability exchange negotiation between ICS and VOIP app.
         */
        internal const val CAPABILITY_EXCHANGE_TIMEOUT = 1000L
        internal const val CAPABILITY_NEGOTIATION_COROUTINE_TIMEOUT = 3000L
        internal const val ACTION_RESULT_RESPONSE_TIMEOUT = 1000L

        /**
         * Constants used to denote the types of error codes that can be returned from the provided
         * action callbacks. Used by the VOIP side.
         */
        @Target(AnnotationTarget.TYPE)
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(VOIP_SERVER_ERROR, VOIP_ACTION_NOT_SUPPORTED_ERROR, PARTICIPANT_NOT_FOUND_ERROR)
        annotation class ExtensionCallbackErrorCode

        internal const val VOIP_SERVER_ERROR = 1
        internal const val VOIP_ACTION_NOT_SUPPORTED_ERROR = 2
        internal const val PARTICIPANT_NOT_FOUND_ERROR = 3

        // Id to be used to represent a null Participant. This would be used for sending updates
        // to the ICS when the active participant is not defined.
        internal const val NULL_PARTICIPANT_ID = -1

        // Logging utils
        internal const val PARTICIPANT_TAG = "participants"
        internal const val CALL_DETAILS_TAG = "call details"

        /**
         * Static helper to sanitize the passed in actions to make sure no duplicates are found and
         * only actions supported by the Participant are defined.
         */
        internal fun preprocessSupportedActions(
            featureId: @CallsManager.Companion.ExtensionType Int,
            rawActions: IntArray
        ): MutableSet<Int> {
            val processedActions:
                MutableSet<@CallsManager.Companion.ExtensionSupportedActions Int> = mutableSetOf()
            // Track currently added actions so we don't end accounting for duplicates (user error).
            for (action in rawActions) {
                VoipExtensionManager.EXTENSION_SUPPORTED_ACTIONS_MAPPING[featureId]?.let {
                    if (it.contains(action) && !processedActions.contains(action)) {
                        processedActions.add(action)
                    }
                }
            }
            return processedActions
        }

        internal fun handleVoipSideUpdateExceptions(
            logTag: String,
            prefixTag: String,
            extensionMsg: String,
            e: Exception
        ) {
            when (e) {
                is RemoteException -> {
                    Log.w(logTag, "$prefixTag: Remote exception occurred while sending " +
                        "$extensionMsg updates to the ICS.", e)
                }
                else -> {
                    Log.w(logTag, "$prefixTag: Exception occurred while sending " +
                        "$extensionMsg updates to the ICS.", e)
                }
            }
        }
    }
}
