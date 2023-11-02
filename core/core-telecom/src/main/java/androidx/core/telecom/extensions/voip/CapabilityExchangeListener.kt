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

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.core.telecom.extensions.ICallDetailsListener
import androidx.core.telecom.extensions.ICapabilityExchangeListener
import androidx.core.telecom.extensions.IParticipantStateListener
import androidx.core.telecom.util.ExperimentalAppActions

@ExperimentalAppActions
@RequiresApi(Build.VERSION_CODES.O)
@RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)
internal class CapabilityExchangeListener(
    private val voipExtensionManager: VoipExtensionManager,
    private val icsId: Int
) : ICapabilityExchangeListener.Stub() {
    // Participant extension
    internal lateinit var participantVoipSupportedActions: IntArray
    internal lateinit var participantStateListener: IParticipantStateListener
    // Call details extension
    internal lateinit var callDetailsVoipSupportedActions: IntArray
    internal lateinit var callDetailsListener: ICallDetailsListener

    override fun onCreateParticipantExtension(
        version: Int,
        actions: IntArray?,
        l: IParticipantStateListener?
    ) {
        actions?.let {
            participantVoipSupportedActions = actions
        }
        l?.let { participantStateListener = l }
        // Subscribe to updates if the VOIP app supports the extension.
        voipExtensionManager.participantExtensionManager?.subscribeToVoipUpdates(icsId,
            participantStateListener, participantVoipSupportedActions, version)
    }

    override fun onCreateCallDetailsExtension(
        version: Int,
        actions: IntArray?,
        l: ICallDetailsListener?,
        packageName: String
    ) {
        actions?.let {
            callDetailsVoipSupportedActions = actions
        }
        l?.let { callDetailsListener = l }
//        voipExtensionManager.callDetailsExtensionManager?.subscribeToVoipUpdates(icsId,
//            callDetailsListener, callDetailsVoipSupportedActions, version, packageName)
    }

    override fun onRemoveExtensions() {
        voipExtensionManager.participantExtensionManager?.unsubscribeFromUpdates(icsId)
//        voipExtensionManager.callDetailsExtensionManager?.unsubscribeFromUpdates(icsId)
    }
}
