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
 * The action used to determine if the calling application supports kicking participants and request
 * to kick [Participant]s in the call.
 */
@ExperimentalAppActions
public interface KickParticipantAction {

    /**
     * Whether or not kicking participants is supported by the calling application.
     *
     * if `true`, then requests to kick participants will be sent to the calling application. If
     * `false`, then the calling application doesn't support this action and requests will fail.
     *
     * Must not be queried until [CallExtensionScope.onConnected] is called.
     */
    public var isSupported: Boolean

    /**
     * Request to kick a [participant] in the call.
     *
     * Whether or not the [Participant] is allowed to be kicked is up to the calling application, so
     * requesting to kick a [Participant] may result in no action being taken. For example, the
     * calling application may choose not to complete a request to kick the host of the call or kick
     * the [Participant] representing this user.
     *
     * Note: This operation succeeding does not mean that the participant was kicked, it only means
     * that the request was received and processed by the remote application. If the [Participant]
     * is indeed kicked, the [CallExtensionScope.addParticipantExtension] `onParticipantsUpdated`
     * callback will be updated to remove the kicked [Participant].
     *
     * @param participant The [Participant] to kick from the call.
     * @return The result of whether or not this request was successfully sent to the remote
     *   application and processed.
     */
    public suspend fun requestKickParticipant(participant: Participant): CallControlResult
}
