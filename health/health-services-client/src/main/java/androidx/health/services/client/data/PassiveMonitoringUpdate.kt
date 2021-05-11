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
import android.os.Parcel
import android.os.Parcelable

/**
 * Represents an update from Passive tracking.
 *
 * Provides [DataPoint] s associated with the Passive tracking, in addition to data related to the
 * user's [UserActivityState].
 */
public data class PassiveMonitoringUpdate(
    /** List of [DataPoint] s from Passive tracking. */
    val dataPoints: List<DataPoint>,

    /** The [UserActivityInfo] of the user from Passive tracking. */
    val userActivityInfoUpdates: List<UserActivityInfo>,
) : Parcelable {

    /**
     * Puts the state as an extra into a given [Intent]. The state can then be obtained from the
     * intent via [PassiveMonitoringUpdate.fromIntent].
     */
    public fun putToIntent(intent: Intent) {
        intent.putExtra(EXTRA_KEY, this)
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(dataPoints.size)
        dest.writeTypedArray(dataPoints.toTypedArray(), flags)

        dest.writeInt(userActivityInfoUpdates.size)
        dest.writeTypedArray(userActivityInfoUpdates.toTypedArray(), flags)
    }

    public companion object {
        private const val EXTRA_KEY = "hs.passive_monitoring_update"

        @JvmField
        public val CREATOR: Parcelable.Creator<PassiveMonitoringUpdate> =
            object : Parcelable.Creator<PassiveMonitoringUpdate> {
                override fun createFromParcel(source: Parcel): PassiveMonitoringUpdate? {
                    val dataPointsArray: Array<DataPoint?> = arrayOfNulls(source.readInt())
                    source.readTypedArray(dataPointsArray, DataPoint.CREATOR)

                    val userActivityInfoArray: Array<UserActivityInfo?> =
                        arrayOfNulls(source.readInt())
                    source.readTypedArray(userActivityInfoArray, UserActivityInfo.CREATOR)

                    return PassiveMonitoringUpdate(
                        dataPointsArray.filterNotNull().toList(),
                        userActivityInfoArray.filterNotNull().toList(),
                    )
                }

                override fun newArray(size: Int): Array<PassiveMonitoringUpdate?> {
                    return arrayOfNulls(size)
                }
            }

        /**
         * Creates a [PassiveMonitoringUpdate] from an [Intent]. Returns null if no
         * [PassiveMonitoringUpdate] is stored in the given intent.
         */
        @JvmStatic
        public fun fromIntent(intent: Intent): PassiveMonitoringUpdate? =
            intent.getParcelableExtra(EXTRA_KEY)
    }
}
