/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.health.services.client.impl.event

import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.health.services.client.PassiveListenerCallback
import androidx.health.services.client.PassiveListenerService
import androidx.health.services.client.data.ProtoParcelable
import androidx.health.services.client.impl.response.HealthEventResponse
import androidx.health.services.client.impl.response.PassiveMonitoringGoalResponse
import androidx.health.services.client.impl.response.PassiveMonitoringUpdateResponse
import androidx.health.services.client.proto.EventsProto.PassiveListenerEvent as EventProto
import androidx.health.services.client.proto.ResponsesProto.PermissionLostResponse

/** An event representing a [PassiveListenerCallback] or [PassiveListenerService] invocation. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class PassiveListenerEvent(public override val proto: EventProto) :
    ProtoParcelable<EventProto>() {

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<PassiveListenerEvent> = newCreator {
            PassiveListenerEvent(EventProto.parseFrom(it))
        }

        @JvmStatic
        public fun createPermissionLostResponse(): PassiveListenerEvent =
            PassiveListenerEvent(
                EventProto.newBuilder()
                    .setPermissionLostResponse(PermissionLostResponse.newBuilder().build())
                    .build()
            )

        @JvmStatic
        public fun createPassiveUpdateResponse(
            response: PassiveMonitoringUpdateResponse
        ): PassiveListenerEvent =
            PassiveListenerEvent(
                EventProto.newBuilder().setPassiveUpdateResponse(response.proto).build()
            )

        @JvmStatic
        public fun createPassiveGoalResponse(
            response: PassiveMonitoringGoalResponse
        ): PassiveListenerEvent =
            PassiveListenerEvent(
                EventProto.newBuilder().setPassiveGoalResponse(response.proto).build()
            )

        @JvmStatic
        public fun createHealthEventResponse(response: HealthEventResponse): PassiveListenerEvent =
            PassiveListenerEvent(
                EventProto.newBuilder().setHealthEventResponse(response.proto).build()
            )
    }
}
