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

import android.content.Intent
import android.os.Parcelable
import androidx.health.services.client.proto.DataProto
import androidx.health.services.client.proto.DataProto.PassiveMonitoringUpdate as PassiveMonitoringUpdateProto

/**
 * Represents an update from Passive tracking.
 *
 * Provides [DataPoint] s associated with the Passive tracking, in addition to data related to the
 * user's [UserActivityState].
 */
@Suppress("ParcelCreator")
public class PassiveMonitoringUpdate(
    /** List of [DataPoint] s from Passive tracking. */
    public val dataPoints: List<DataPoint>,

    /** The [UserActivityInfo] of the user from Passive tracking. */
    public val userActivityInfoUpdates: List<UserActivityInfo>,
) : ProtoParcelable<PassiveMonitoringUpdateProto>() {

    internal constructor(
        proto: DataProto.PassiveMonitoringUpdate
    ) : this(
        proto.dataPointsList.map { DataPoint(it) },
        proto.userActivityInfoUpdatesList.map { UserActivityInfo(it) }
    )

    /**
     * Puts the state as an extra into a given [Intent]. The state can then be obtained from the
     * intent via [PassiveMonitoringUpdate.fromIntent].
     */
    public fun putToIntent(intent: Intent) {
        intent.putExtra(EXTRA_KEY, this)
    }

    /** @hide */
    override val proto: PassiveMonitoringUpdateProto by lazy {
        PassiveMonitoringUpdateProto.newBuilder()
            .addAllDataPoints(dataPoints.map { it.proto })
            .addAllUserActivityInfoUpdates(userActivityInfoUpdates.map { it.proto })
            .build()
    }

    override fun toString(): String =
        "PassiveMonitoringUpdate(" +
            "dataPoints=$dataPoints, " +
            "userActivityInfoUpdates=$userActivityInfoUpdates)"

    public companion object {
        private const val EXTRA_KEY = "hs.passive_monitoring_update"
        @Suppress("ActionValue") public const val ACTION_DATA: String = "hs.passivemonitoring.DATA"

        @JvmField
        public val CREATOR: Parcelable.Creator<PassiveMonitoringUpdate> = newCreator { bytes ->
            val proto = PassiveMonitoringUpdateProto.parseFrom(bytes)
            PassiveMonitoringUpdate(proto)
        }

        /**
         * Creates a [PassiveMonitoringUpdate] from an [Intent]. Returns null if no
         * [PassiveMonitoringUpdate] is stored in the given intent.
         */
        @Suppress("DEPRECATION")
        @JvmStatic
        public fun fromIntent(intent: Intent): PassiveMonitoringUpdate? =
            intent.getParcelableExtra(EXTRA_KEY)
    }
}
