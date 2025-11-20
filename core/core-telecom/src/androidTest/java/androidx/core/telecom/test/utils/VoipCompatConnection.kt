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

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.Connection
import androidx.annotation.RequiresApi
import androidx.core.telecom.extensions.ExtrasCallExtensionProcessor.Companion.EXTRA_CALL_IMAGE_URI
import androidx.core.telecom.extensions.ExtrasCallExtensionProcessor.Companion.EXTRA_CURRENT_SPEAKER
import androidx.core.telecom.extensions.ExtrasCallExtensionProcessor.Companion.EXTRA_PARTICIPANT_COUNT
import androidx.core.telecom.extensions.ExtrasCallExtensionProcessor.Companion.EXTRA_VOIP_API_VERSION

/** {@link Connection} with extensions for VOIP call functionality. */
@RequiresApi(Build.VERSION_CODES.O)
open class VoipCompatConnection : Connection() {
    /**
     * Version number of the VOIP call added API's to allow InCallService code to support future
     * updates.
     */
    val VOIP_API_VERSION = 1

    init {
        setApiVersion()
    }

    /** Sets the current speaker for the call. */
    fun setCurrentSpeaker(currentSpeaker: String) {
        setApiVersion()
        val extras = Bundle()
        extras.putString(EXTRA_CURRENT_SPEAKER, currentSpeaker)
        putExtras(extras)
    }

    /** Removes current speaker, the speaker will not be displayed in call UI's. */
    fun clearCurrentSpeaker() {
        removeExtras(EXTRA_CURRENT_SPEAKER)
    }

    /** Sets the participant count for the call. */
    fun setParticipantCount(participantCount: Int) {
        setApiVersion()
        val extras = Bundle()
        extras.putInt(EXTRA_PARTICIPANT_COUNT, participantCount)
        putExtras(extras)
    }

    /**
     * Clears the participant count for the call, the participant count will not be displayed in any
     * call UI's.
     */
    fun clearParticipantCount() {
        removeExtras(EXTRA_PARTICIPANT_COUNT)
    }

    /**
     * Sets the call image {@link Uri}. Supported URI types are resource URI’s and content provider
     * URI’s
     */
    fun setCallImageUri(callImageUri: Uri) {
        setApiVersion()
        val extras = Bundle()
        extras.putParcelable(EXTRA_CALL_IMAGE_URI, callImageUri)
        putExtras(extras)
    }

    /**
     * Clears the call image {@link Uri}. Surfaces showing the call will use a default image where
     * needed.
     */
    fun clearCallImageUri() {
        removeExtras(EXTRA_CALL_IMAGE_URI)
    }

    private fun setApiVersion() {
        if (
            getExtras() != null &&
                getExtras().getInt(EXTRA_VOIP_API_VERSION, -1) == VOIP_API_VERSION
        ) {
            return
        }

        val bundle = Bundle()
        bundle.putInt(EXTRA_VOIP_API_VERSION, VOIP_API_VERSION)
        putExtras(bundle)
    }
}
