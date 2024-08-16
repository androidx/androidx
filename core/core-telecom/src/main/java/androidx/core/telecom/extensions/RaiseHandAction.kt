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

import androidx.core.telecom.CallControlResult
import androidx.core.telecom.util.ExperimentalAppActions

/**
 * The action used to determine if the raise hand action is supported by the calling application and
 * notify the calling application when the user requests to raise or lower their hand.
 */
@ExperimentalAppActions
public interface RaiseHandAction {
    /**
     * Whether or not raising/lowering hands is supported by the calling application.
     *
     * if `true`, then updates about raised hands from the calling application will be notified. If
     * `false`, then the calling application doesn't support this action and state changes will not
     * be notified to the caller and [requestRaisedHandStateChange] requests will fail.
     *
     * Must not be queried until [CallExtensionScope.onConnected] is called or an error will be
     * thrown.
     */
    public var isSupported: Boolean

    /**
     * Request the calling application to raise or lower this user's hand.
     *
     * Whether or not this user's hand is currently raised is determined by inspecting whether or
     * not this [Participant] is currently included in the
     * [ParticipantExtensionRemote.addRaiseHandAction] `onRaisedHandsChanged` callback `List`.
     *
     * Note: A [CallControlResult.Success] result does not mean that the raised hand state of the
     * user has changed. It only means that the request was received by the remote application and
     * processed. This can be used to gray out UI until the request has processed.
     *
     * @param isRaised `true` if this user has raised their hand, `false` if they have lowered their
     *   hand
     * @return Whether or not the remote application received this event. This does not mean that
     *   the operation succeeded, but rather the remote received and processed the event
     *   successfully.
     */
    public suspend fun requestRaisedHandStateChange(isRaised: Boolean): CallControlResult
}
