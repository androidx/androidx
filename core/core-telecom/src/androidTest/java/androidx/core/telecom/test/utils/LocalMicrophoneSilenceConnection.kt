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

package androidx.core.telecom.test.utils

import android.os.Build
import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.annotation.RequiresApi
import androidx.core.telecom.extensions.ExtrasCallExtensionProcessor.Companion.EVENT_LOCAL_CALL_SILENCE_STATE_CHANGED
import androidx.core.telecom.extensions.ExtrasCallExtensionProcessor.Companion.EXTRA_CALL_SILENCE_AVAILABILITY
import androidx.core.telecom.extensions.ExtrasCallExtensionProcessor.Companion.EXTRA_LOCAL_CALL_SILENCE_STATE
import androidx.core.telecom.extensions.ExtrasCallExtensionProcessor.Companion.EXTRA_USE_LOCAL_CALL_SILENCE_CAPABILITY

/**
 * A class extending {@link Connection} which adds the ability to support a local microphone silence
 * state between the app and any other calling surfaces showing this call.
 */
@RequiresApi(Build.VERSION_CODES.O)
abstract class LocalMicrophoneSilenceConnection : VoipCompatConnection() {
    var currentlySilenced: Boolean = false
    var silenceStateChangeable: Boolean = true

    /**
     * Updates the call silence state and will be propagated to calling surfaces alerting them that
     * the call microphone is silenced. This should be called in response to a successful callback
     * from {@link #onCallMicrophoneSilenceStateChanged(boolean)}.
     */
    fun setCallMicrophoneSilenceState(silenced: Boolean) {
        // Always set call silence capability just in case it is cleared unintentionally.
        setCallMicrophoneSilenceCapabilities()

        currentlySilenced = silenced
        val currentExtras = getExtras()
        if (
            currentExtras != null &&
                currentExtras.getBoolean(EXTRA_LOCAL_CALL_SILENCE_STATE, false) == silenced
        ) {
            return
        }

        val bundle = Bundle()
        bundle.putBoolean(EXTRA_LOCAL_CALL_SILENCE_STATE, silenced)
        putExtras(bundle)
    }

    /**
     * Sets whether the call is able to currently be silenced or unsilenced. By default the call
     * will be assumed to be able to change silence states.
     */
    fun setCallMicrophoneSilenceAvailability(silenceStateChangeable: Boolean) {
        this.silenceStateChangeable = silenceStateChangeable
        setCallMicrophoneSilenceCapabilities()
    }

    /** Returns the current value for call silence state. */
    fun getCallMicrophoneSilenceState(): Boolean {
        val currentExtras = getExtras()
        // Update the value in the bundle if it has been unintentionally cleared.
        if (
            currentExtras == null ||
                currentExtras.getBoolean(EXTRA_LOCAL_CALL_SILENCE_STATE, false) != currentlySilenced
        ) {
            val bundle = Bundle()
            bundle.putBoolean(EXTRA_LOCAL_CALL_SILENCE_STATE, currentlySilenced)
            putExtras(bundle)
        }

        return currentlySilenced
    }

    @Override
    @CallSuper
    override fun onCallEvent(event: String, bundle: Bundle) {
        if (event == EVENT_LOCAL_CALL_SILENCE_STATE_CHANGED) {
            onCallMicrophoneSilenceStateChanged(
                bundle.getBoolean(EXTRA_LOCAL_CALL_SILENCE_STATE, false)
            )
        }
    }

    /**
     * Called when the call silence state is changed by a calling surface. The app should update
     * their internal call silence state to match. Then
     * {@link #setCallMicrophoneSilenceState(boolean)} should be called.
     */
    abstract fun onCallMicrophoneSilenceStateChanged(callSilenced: Boolean)

    private fun setCallMicrophoneSilenceCapabilities() {
        val bundle = Bundle()
        var capabilitiesUpdated = false
        val currentExtras = getExtras()

        if (
            currentExtras == null ||
                !currentExtras.getBoolean(EXTRA_USE_LOCAL_CALL_SILENCE_CAPABILITY, false)
        ) {
            capabilitiesUpdated = true
            bundle.putBoolean(EXTRA_USE_LOCAL_CALL_SILENCE_CAPABILITY, true)
        }

        if (
            currentExtras == null ||
                !currentExtras.containsKey(EXTRA_CALL_SILENCE_AVAILABILITY) ||
                currentExtras.getBoolean(EXTRA_CALL_SILENCE_AVAILABILITY, false) !=
                    silenceStateChangeable
        ) {
            capabilitiesUpdated = true
            bundle.putBoolean(EXTRA_CALL_SILENCE_AVAILABILITY, silenceStateChangeable)
        }

        if (capabilitiesUpdated) {
            putExtras(bundle)
        }
    }
}
