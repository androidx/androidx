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
import androidx.health.services.client.proto.DataProto.DataPointAccuracy.HrAccuracy as HrAccuracyProto
import androidx.health.services.client.proto.DataProto.DataPointAccuracy.HrAccuracy.SensorStatus as SensorStatusProto

/** Accuracy for a [DataType.HEART_RATE_BPM] data point. */
@Suppress("ParcelCreator")
public class HrAccuracy(public val sensorStatus: SensorStatus) : DataPointAccuracy() {

    internal constructor(
        proto: DataProto.DataPointAccuracy
    ) : this(SensorStatus.fromProto(proto.hrAccuracy.sensorStatus))

    public enum class SensorStatus(public val id: Int) {
        UNKNOWN(0),
        NO_CONTACT(1),
        UNRELIABLE(2),
        ACCURACY_LOW(3),
        ACCURACY_MEDIUM(4),
        ACCURACY_HIGH(5);

        /** @hide */
        internal fun toProto(): SensorStatusProto =
            SensorStatusProto.forNumber(id) ?: SensorStatusProto.HR_ACCURACY_SENSOR_STATUS_UNKNOWN

        /** @hide */
        public companion object {
            /** @hide */
            public fun fromProto(proto: SensorStatusProto): SensorStatus =
                values().firstOrNull { it.id == proto.number } ?: UNKNOWN
        }
    }

    /** @hide */
    override val proto: DataProto.DataPointAccuracy by lazy {
        DataProto.DataPointAccuracy.newBuilder()
            .setHrAccuracy(HrAccuracyProto.newBuilder().setSensorStatus(sensorStatus.toProto()))
            .build()
    }

    override fun toString(): String = "HrAccuracy(sensorStatus=$sensorStatus)"

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<HrAccuracy> = newCreator {
            val proto = DataProto.DataPointAccuracy.parseFrom(it)
            HrAccuracy(proto)
        }
    }
}
