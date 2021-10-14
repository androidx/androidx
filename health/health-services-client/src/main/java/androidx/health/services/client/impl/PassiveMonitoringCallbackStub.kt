/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.health.services.client.impl

import android.util.Log
import androidx.health.services.client.PassiveMonitoringCallback
import androidx.health.services.client.impl.event.PassiveCallbackEvent
import androidx.health.services.client.impl.response.PassiveMonitoringUpdateResponse
import androidx.health.services.client.proto.EventsProto.PassiveCallbackEvent.EventCase.EVENT_NOT_SET
import androidx.health.services.client.proto.EventsProto.PassiveCallbackEvent.EventCase.PASSIVE_UPDATE_RESPONSE

/**
 * A stub implementation for IPassiveMonitoringCallback.
 *
 * @hide
 */
internal class PassiveMonitoringCallbackStub
internal constructor(private val callback: PassiveMonitoringCallback) :
    IPassiveMonitoringCallback.Stub() {

    override fun onPassiveCallbackEvent(event: PassiveCallbackEvent) {
        val proto = event.proto

        when (proto.eventCase) {
            PASSIVE_UPDATE_RESPONSE -> {
                val response = PassiveMonitoringUpdateResponse(proto.passiveUpdateResponse)
                callback.onPassiveMonitoringUpdate(response.passiveMonitoringUpdate)
            }
            null, EVENT_NOT_SET -> Log.w(TAG, "Received unknown event ${proto.eventCase}")
        }
    }

    private companion object {
        const val TAG = "PassiveCallbackStub"
    }
}
