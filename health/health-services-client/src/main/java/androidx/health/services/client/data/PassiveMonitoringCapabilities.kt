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

package androidx.health.services.client.data

import android.os.Parcelable
import androidx.health.services.client.proto.DataProto
import androidx.health.services.client.proto.DataProto.PassiveMonitoringCapabilities as PassiveMonitoringCapabilitiesProto

/**
 * A place holder class that represents the capabilities of the
 * [androidx.health.services.client.PassiveMonitoringClient] on the device.
 */
@Suppress("ParcelCreator")
public class PassiveMonitoringCapabilities(

    /**
     * Set of supported [DataType] s for background capture on this device.
     *
     * Some data types are only available during exercise (e.g. location) or for measurements.
     */
    public val supportedDataTypesPassiveMonitoring: Set<DataType>,

    /** Set of supported [DataType] s for event callbacks on this device. */
    public val supportedDataTypesEvents: Set<DataType>,
) : ProtoParcelable<PassiveMonitoringCapabilitiesProto>() {
    internal constructor(
        proto: DataProto.PassiveMonitoringCapabilities
    ) : this(
        proto.supportedDataTypesPassiveMonitoringList.map { DataType(it) }.toSet(),
        proto.supportedDataTypesEventsList.map { DataType(it) }.toSet()
    )

    /** @hide */
    override val proto: PassiveMonitoringCapabilitiesProto by lazy {
        PassiveMonitoringCapabilitiesProto.newBuilder()
            .addAllSupportedDataTypesPassiveMonitoring(
                supportedDataTypesPassiveMonitoring.map { it.proto }
            )
            .addAllSupportedDataTypesEvents(supportedDataTypesEvents.map { it.proto })
            .build()
    }

    override fun toString(): String =
        "PassiveMonitoringCapabilities(" +
            "supportedDataTypesPassiveMonitoring=$supportedDataTypesPassiveMonitoring, " +
            "supportedDataTypesEvents=$supportedDataTypesEvents)"

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<PassiveMonitoringCapabilities> =
            newCreator { bytes ->
                val proto = PassiveMonitoringCapabilitiesProto.parseFrom(bytes)
                PassiveMonitoringCapabilities(proto)
            }
    }
}
