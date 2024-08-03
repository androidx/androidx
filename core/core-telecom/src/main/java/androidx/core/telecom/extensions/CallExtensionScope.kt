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

import android.telecom.Call
import androidx.core.telecom.util.ExperimentalAppActions

/**
 * Provides a scope where extensions can be first initialized and next managed for a [Call] once
 * [onConnected] is called.
 *
 * The following extension is supported on a call:
 * - [addParticipantExtension] - Show the user more information about the [Participant]s in the
 *   call.
 *
 * ```
 * class InCallServiceImpl : InCallServiceCompat() {
 * ...
 *   override fun onCallAdded(call: Call) {
 *     lifecycleScope.launch {
 *       connectExtensions(context, call) {
 *         // Initialize extensions
 *         onConnected { call ->
 *           // change call states & listen for extension updates/send extension actions
 *         }
 *       }
 *       // Once the call is destroyed, control flow will resume again
 *     }
 *   }
 *  ...
 * }
 * ```
 */
@ExperimentalAppActions
public interface CallExtensionScope {

    /**
     * Called when the [Call] extensions have been successfully set up and are ready to be used.
     *
     * @param block Called when the [Call] and initialized extensions are ready to be used.
     */
    public fun onConnected(block: suspend (Call) -> Unit)

    /**
     * Add support for this remote surface to display information related to the [Participant]s in
     * this call.
     *
     * ```
     * connectExtensions(call) {
     *     val participantExtension = addParticipantExtension(
     *         // consume participant changed events
     *     )
     *     onConnected {
     *         // extensions have been negotiated and actions are ready to be used
     *     }
     * }
     * ```
     *
     * @param onActiveParticipantChanged Called with the active [Participant] in the call has
     *   changed. If this method is called with a `null` [Participant], there is no active
     *   [Participant]. The active [Participant] in the call is the [Participant] that should take
     *   focus and be either more prominent on the screen or otherwise featured as active in UI. For
     *   example, this could be the [Participant] that is actively talking or presenting.
     * @param onParticipantsUpdated Called when the [Participant]s in the [Call] have changed and
     *   the UI should be updated.
     * @return The interface that is used to set up additional actions for this extension.
     */
    public fun addParticipantExtension(
        onActiveParticipantChanged: suspend (Participant?) -> Unit,
        onParticipantsUpdated: suspend (Set<Participant>) -> Unit
    ): ParticipantExtensionRemote
}
