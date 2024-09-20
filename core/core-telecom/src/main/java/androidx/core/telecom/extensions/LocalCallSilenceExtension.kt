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

package androidx.core.telecom.extensions

import androidx.core.telecom.util.ExperimentalAppActions

/**
 * Add support for this remote surface to display information related to the local call silence
 * state for this call.
 *
 * Local Call Silence means that the call should be silenced at the application layer (local
 * silence) instead of the hardware layer (global silence). Using a local call silence over global
 * silence is advantageous when the application wants to still receive the audio input data while
 * not transmitting audio input data to remote users. This allows applications to do stuff like
 * nudge the user when they are silenced but talking into the microphone.
 *
 * @see ExtensionInitializationScope.addLocalCallSilenceExtension
 */
@ExperimentalAppActions
public interface LocalCallSilenceExtension {
    /**
     * Update all of the remote surfaces that the local call silence state of this call has changed.
     *
     * @param isSilenced The new local call silence state associated with this call.
     */
    public suspend fun updateIsLocallySilenced(isSilenced: Boolean)
}
