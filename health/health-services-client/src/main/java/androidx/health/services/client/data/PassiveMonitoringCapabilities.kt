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
 * Contains the capabilities supported by [androidx.health.services.client.PassiveMonitoringClient]
 * on this device.
 */
@Suppress("ParcelCreator")
public class PassiveMonitoringCapabilities(

    /**
     * Set of supported [DataType]s for background capture on this device.
     *
     * Some data types are only available during exercise (e.g. location) or for measurements.
     */
    public val supportedDataTypesPassiveMonitoring: Set<DataType<*, *>>,

    /** Set of supported [DataType]s for goal callbacks on this device. */
    public val supportedDataTypesPassiveGoals: Set<DataType<*, *>>,

    /** Set of supported [HealthEvent.Type]s on this device. */
    public val supportedHealthEventTypes: Set<HealthEvent.Type>,

    /** Set of supported [UserActivityState]s on this device. */
    public val supportedUserActivityStates: Set<UserActivityState>,
) : ProtoParcelable<PassiveMonitoringCapabilitiesProto>() {
    internal constructor(
        proto: DataProto.PassiveMonitoringCapabilities
    ) : this(
        proto.supportedDataTypesPassiveMonitoringList.map { DataType.deltaFromProto(it) }.toSet(),
        proto.supportedDataTypesPassiveGoalsList.map { DataType.deltaFromProto(it) }.toSet(),
        proto.supportedHealthEventTypesList.mapNotNull { HealthEvent.Type.fromProto(it) }.toSet(),
        proto.supportedUserActivityStatesList.mapNotNull { UserActivityState.fromProto(it) }.toSet()
    )

    /** @hide */
    override val proto: PassiveMonitoringCapabilitiesProto by lazy {
        PassiveMonitoringCapabilitiesProto.newBuilder()
            .addAllSupportedDataTypesPassiveMonitoring(
                supportedDataTypesPassiveMonitoring.map { it.proto }
            )
            .addAllSupportedDataTypesPassiveGoals(supportedDataTypesPassiveGoals.map { it.proto })
            .addAllSupportedHealthEventTypes(supportedHealthEventTypes.map { it.toProto() })
            .addAllSupportedUserActivityStates(supportedUserActivityStates.map { it.toProto() })
            .build()
    }

    override fun toString(): String =
        "PassiveMonitoringCapabilities(" +
            "supportedDataTypesPassiveMonitoring=$supportedDataTypesPassiveMonitoring, " +
            "supportedDataTypesPassiveGoals=$supportedDataTypesPassiveGoals, " +
            "supportedHealthEventTypes=$supportedHealthEventTypes, " +
            "supportedUserActivityStates=$supportedUserActivityStates)"

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<PassiveMonitoringCapabilities> =
            newCreator { bytes ->
                val proto = PassiveMonitoringCapabilitiesProto.parseFrom(bytes)
                PassiveMonitoringCapabilities(proto)
            }
    }
}
