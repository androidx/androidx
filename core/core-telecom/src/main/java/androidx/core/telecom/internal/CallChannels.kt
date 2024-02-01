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

package androidx.core.telecom.internal

import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.extensions.voip.VoipParticipantActionRequest
import kotlinx.coroutines.channels.Channel

internal class CallChannels(
    val currentEndpointChannel: Channel<CallEndpointCompat> = Channel(Channel.UNLIMITED),
    val availableEndpointChannel: Channel<List<CallEndpointCompat>> = Channel(Channel.UNLIMITED),
    val isMutedChannel: Channel<Boolean> = Channel(Channel.UNLIMITED),
    val voipParticipantActionRequestsChannel: Channel<VoipParticipantActionRequest> =
        Channel(Channel.UNLIMITED)
) {
    fun closeAllChannels() {
        currentEndpointChannel.close()
        availableEndpointChannel.close()
        isMutedChannel.close()
        voipParticipantActionRequestsChannel.close()
    }
}
