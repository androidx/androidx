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

package androidx.core.telecom.extensions

import android.util.Log
import androidx.core.telecom.internal.CapabilityExchangeListenerRemote
import androidx.core.telecom.internal.MeetingSummaryStateListener
import androidx.core.telecom.util.ExperimentalAppActions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.properties.Delegates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Implementation of [MeetingSummaryRemote] for handling remote meeting summary interactions.
 *
 * This class manages the connection and communication with a remote extension providing meeting
 * summary data. It handles capability exchange and updates to the current speaker and participant
 * count through callbacks.
 *
 * @property callScope The [CoroutineScope] used for launching coroutines within this class. Should
 *   be tied to the lifecycle of the component using this class.
 * @property onCurrentSpeakerChanged A suspend function that is called when the current speaker
 *   changes. The function takes the new speaker's name/ID as a String parameter.
 * @property onParticipantCountChanged A suspend function that is called when the participant count
 *   changes. The function takes the new participant count as an Int parameter.
 */
@ExperimentalAppActions
internal class MeetingSummaryRemoteImpl(
    private val callScope: CoroutineScope,
    private val onCurrentSpeakerChanged: suspend (String) -> Unit,
    private val onParticipantCountChanged: suspend (Int) -> Unit,
) : MeetingSummaryRemote {

    /**
     * Indicates whether the remote meeting summary extension is supported.
     *
     * This value is set to `true` after a successful capability exchange indicates that the remote
     * extension is available and compatible, and `false` otherwise. Initialized using
     * [Delegates.notNull], ensuring that it's assigned a value before being read.
     */
    override var isSupported by Delegates.notNull<Boolean>()

    companion object {
        /** The tag used for logging. */
        val TAG: String = MeetingSummaryRemoteImpl::class.java.simpleName
    }

    /**
     * Returns an empty array of actions, as no actions are supported.
     *
     * This getter provides an IntArray, representing supported actions. In this current
     * implementation, no custom actions are handled.
     *
     * @return An empty [IntArray].
     */
    internal val actions
        get() = IntArray(0) // No actions supported

    /**
     * Called when the capability exchange is complete.
     *
     * This method determines whether the remote extension is supported based on the negotiated
     * capability and connects to the remote actions interface if supported.
     *
     * @param negotiatedCapability The negotiated capability.
     * @param remote The remote capability exchange listener.
     */
    internal suspend fun onExchangeComplete(
        negotiatedCapability: Capability?,
        remote: CapabilityExchangeListenerRemote?,
    ) {
        Log.i(TAG, "onExchangeComplete: in function")
        if (negotiatedCapability == null || remote == null) {
            Log.i(TAG, "onNegotiated: remote is not capable")
            isSupported = false
            return
        }
        Log.i(TAG, "onExchangeComplete: isSupported")
        isSupported = true
        connectToRemote(negotiatedCapability, remote)
    }

    /**
     * Connects to the remote meeting summary extension.
     *
     * This private function establishes a connection with the remote extension. It creates a
     * [MeetingSummaryStateListener] and passes it to the remote to receive updates on the meeting
     * summary state. Uses [suspendCancellableCoroutine] to suspend until the remote side finishes
     * syncing initial state.
     *
     * @param negotiatedCapability The [Capability] object resulting from the capability exchange.
     * @param remote The [CapabilityExchangeListenerRemote] instance for communicating with the
     *   remote side.
     */
    private suspend fun connectToRemote(
        negotiatedCapability: Capability,
        remote: CapabilityExchangeListenerRemote,
    ): Unit = suspendCancellableCoroutine { continuation ->
        Log.d(TAG, "connectToRemote:")
        val stateListener =
            MeetingSummaryStateListener(
                updateCurrentSpeaker = { callScope.launch { onCurrentSpeakerChanged(it) } },
                updateParticipantCount = { callScope.launch { onParticipantCountChanged(it) } },
                finishSync = { callScope.launch { continuation.resume(Unit) } },
            )
        try {
            remote.onCreateMeetingSummaryExtension(
                negotiatedCapability.featureVersion,
                stateListener,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to remote extension", e)
            continuation.resumeWithException(e) // Propagate the exception
        }
    }
}
