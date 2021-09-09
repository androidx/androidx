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

package androidx.health.services.client.impl.response

import android.os.Parcelable
import androidx.health.services.client.data.PassiveMonitoringCapabilities
import androidx.health.services.client.data.ProtoParcelable
import androidx.health.services.client.proto.ResponsesProto

/**
 * Response containing the [PassiveMonitoringCapabilities] of the device.
 *
 * @hide
 */
public class PassiveMonitoringCapabilitiesResponse(
    /** [PassiveMonitoringCapabilities] supported by this device. */
    public val passiveMonitoringCapabilities: PassiveMonitoringCapabilities,
) : ProtoParcelable<ResponsesProto.PassiveMonitoringCapabilitiesResponse>() {

    override val proto: ResponsesProto.PassiveMonitoringCapabilitiesResponse by lazy {
        ResponsesProto.PassiveMonitoringCapabilitiesResponse.newBuilder()
            .setCapabilities(passiveMonitoringCapabilities.proto)
            .build()
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<PassiveMonitoringCapabilitiesResponse> =
            newCreator { bytes ->
                val proto = ResponsesProto.PassiveMonitoringCapabilitiesResponse.parseFrom(bytes)
                PassiveMonitoringCapabilitiesResponse(
                    PassiveMonitoringCapabilities(proto.capabilities)
                )
            }
    }
}
