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

package androidx.core.telecom.extensions.voip

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallsManager
import androidx.core.telecom.extensions.Capability
import androidx.core.telecom.extensions.CapabilityExchange
import androidx.core.telecom.internal.CallChannels
import kotlin.coroutines.CoroutineContext

@RequiresApi(Build.VERSION_CODES.O)
@androidx.annotation.OptIn(androidx.core.telecom.util.ExperimentalAppActions::class)
internal class VoipExtensionManager(
    private val context: Context,
    private val coroutineContext: CoroutineContext?,
    private val callChannels: CallChannels,
    // Capabilities to be included as specified on VOIP side.
    private val extensionsToAdd: List<Capability>
) {
    // The current call control session scope.
    private lateinit var session: CallControlScope
    // Handles participant extension updates from VOIP side to ICS.
    internal var participantExtensionManager: VoipParticipantExtensionManager? = null
    // Todo: re-enable once call icon impl. is complete.
    // Handles call detail extension updates from VOIP side to ICS.
//    internal var callDetailsExtensionManager: VoipCallDetailsExtensionManager? = null

    // Track each ICS with a unique id so that it can be used to distinguish the different
    // subscribers when sending updates from the VOIP side.
    private var currentIcsId = 0

    companion object {
        private val TAG = VoipExtensionManager::class.simpleName

        /**
         * Todo: VERSION HANDLING
         * List of all possible supported actions for the Participant extension. This will be
         * modified to include other actions for future versions.
         */
        internal val PARTICIPANT_SUPPORTED_ACTIONS = setOf(
            CallsManager.RAISE_HAND_ACTION, CallsManager.KICK_PARTICIPANT_ACTION)
        internal val CALL_DETAILS_SUPPORTED_ACTIONS:
            Set<@CallsManager.Companion.ExtensionSupportedActions Int> = setOf()
        internal val EXTENSION_SUPPORTED_ACTIONS_MAPPING:
            MutableMap<@CallsManager.Companion.ExtensionType Int,
                Set<@CallsManager.Companion.ExtensionSupportedActions Int>> = hashMapOf(
                    CallsManager.PARTICIPANT to PARTICIPANT_SUPPORTED_ACTIONS,
                    CallsManager.CALL_ICON to CALL_DETAILS_SUPPORTED_ACTIONS
                )

        /**
         * Static helper to determine if the ICS supports a given action.
         */
        internal fun isActionSupportedByIcs(
            actions: IntArray,
            actionToCheck: @CallsManager.Companion.ExtensionSupportedActions Int
        ): Boolean {
            for (action in actions) {
                if (action == actionToCheck) {
                    return true
                }
            }
            return false
        }
    }

    /**
     * Initialize the call session once it becomes available (CallSession / CallSessionLegacy).
     */
    internal fun initializeSession(currentSession: CallControlScope) {
        session = currentSession
    }

    /**
     * Initialize capabilities to be included as specified by the VOIP app.
     */
    internal fun initializeExtensions() {
        for (capability in extensionsToAdd) {
            addExtension(capability)
        }
    }

    /**
     * Internal helper to being capability negotiation between the VOIP app and ICS. This helper is
     * invoked on the VOIP side where negotiation begins when we are notified via a call event
     * (containing [CallsManager.EVENT_JETPACK_CAPABILITY_EXCHANGE]). The VOIP side is responsible
     * for informing the ICS of its supported capabilities and providing it with a listener with
     * which the ICS can leverage to set up extensions support with the VOIP app.
     *
     * @param extras received from call event.
     * @param supportedCapabilities for the VOIP app.
     * @param logTag
     */
    internal fun initiateVoipAppCapabilityExchange(
        extras: Bundle,
        supportedCapabilities: MutableList<Capability>,
        logTag: String? = TAG
    ) {
        Log.i(logTag, "initiateVoipAppCapabilityExchange: Begin capability exchange")
        // Retrieve binder from ICS.
        val capabilityExchange: CapabilityExchange? = extras.getBinder(
            CallsManager.EXTRA_CAPABILITY_EXCHANGE_BINDER) as CapabilityExchange?

        // Initialize capability exchange listener and set it on binder
        val capabilityExchangeListener = CapabilityExchangeListener(
            this@VoipExtensionManager, currentIcsId++)
        try {
            capabilityExchange?.let {
                capabilityExchange.beginExchange(supportedCapabilities, capabilityExchangeListener)
            }
        } catch (e: RemoteException) {
            Log.w(logTag, "initiateVoipAppCapabilityExchange: Remote exception occurred " +
                "while starting capability exchange with ICS.", e)
        } catch (e: Exception) {
            Log.w(logTag, "initiateVoipAppCapabilityExchange: Exception occurred", e)
        }
    }

    /**
     * Tear down all extensions and stop collecting updates when the call session is terminated.
     */
    internal fun tearDownExtensions() {
        participantExtensionManager?.tearDown()
//        callDetailsExtensionManager?.tearDown()
    }

    /***********************************************************************************************
     *                           Private Helpers
     *********************************************************************************************/

    /**
     * Private helper to register a specified extension on the VOIP side.
     *
     * @param capability (extension) to register.
     */
    private fun addExtension(capability: Capability) {
        when (capability.featureId) {
            CallsManager.PARTICIPANT -> {
                participantExtensionManager = VoipParticipantExtensionManager(session,
                    coroutineContext!!, callChannels, capability)
            }
            CallsManager.CALL_ICON -> {
                // Todo: Re-enable once call icon impl. is ready.
//                callDetailsExtensionManager = VoipCallDetailsExtensionManager(context, session,
//                    coroutineContext!!, capability)
            }
            CallsManager.CALL_SILENCE -> {
                // Todo
            }
            else -> Log.i(TAG, "Attempted to add incompatible extension")
        }
    }
}
