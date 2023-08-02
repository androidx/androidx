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

import androidx.health.services.client.proto.DataProto
import androidx.health.services.client.proto.DataProto.PassiveMonitoringUpdate as PassiveMonitoringUpdateProto

/**
 * Represents an update from Passive tracking.
 *
 * Provides [DataPoint]s associated with the Passive tracking, in addition to data related to the
 * user's [UserActivityState].
 */
@Suppress("ParcelCreator")
public class PassiveMonitoringUpdate(
    /** List of [DataPoint]s from Passive tracking. */
    public val dataPoints: DataPointContainer,

    /** The [UserActivityInfo] of the user from Passive tracking. */
    public val userActivityInfoUpdates: List<UserActivityInfo>,
) {

    internal constructor(
        proto: DataProto.PassiveMonitoringUpdate
    ) : this(
        DataPointContainer(proto.dataPointsList.map { DataPoint.fromProto(it) }),
        proto.userActivityInfoUpdatesList.map { UserActivityInfo(it) }
    )

    internal val proto: PassiveMonitoringUpdateProto =
        PassiveMonitoringUpdateProto.newBuilder()
            .addAllDataPoints(dataPoints.sampleDataPoints.map { it.proto })
            .addAllDataPoints(dataPoints.intervalDataPoints.map { it.proto })
            .addAllUserActivityInfoUpdates(userActivityInfoUpdates.map { it.proto })
            .build()

    override fun toString(): String =
        "PassiveMonitoringUpdate(" +
            "dataPoints=$dataPoints, " +
            "userActivityInfoUpdates=$userActivityInfoUpdates)"
}
