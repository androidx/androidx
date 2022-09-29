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
import androidx.annotation.RestrictTo
import androidx.health.services.client.proto.DataProto
import androidx.health.services.client.proto.DataProto.DataPointAccuracy.HrAccuracy as HrAccuracyProto
import androidx.health.services.client.proto.DataProto.DataPointAccuracy.HrAccuracy.SensorStatus as SensorStatusProto

/** Accuracy for a [DataType.HEART_RATE_BPM] data point. */
@Suppress("ParcelCreator")
public class HeartRateAccuracy(public val sensorStatus: SensorStatus) : DataPointAccuracy() {

    internal constructor(
        proto: DataProto.DataPointAccuracy
    ) : this(SensorStatus.fromProto(proto.hrAccuracy.sensorStatus))

    /** Status of the Heart Rate sensor in terms of accuracy. */
    public class SensorStatus private constructor(public val id: Int, public val name: String) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SensorStatus) return false
            if (id != other.id) return false

            return true
        }

        override fun hashCode(): Int = id

        override fun toString(): String = name

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        internal fun toProto(): SensorStatusProto =
            SensorStatusProto.forNumber(id) ?: SensorStatusProto.HR_ACCURACY_SENSOR_STATUS_UNKNOWN

        public companion object {
            /**
             * The availability is unknown, or is represented by a value too new for this library
             * version to parse.
             */
            @JvmField
            public val UNKNOWN: SensorStatus = SensorStatus(0, "UNKNOWN")

            /**
             * The heart rate cannot be acquired because the sensor is not properly contacting skin.
             */
            @JvmField
            public val NO_CONTACT: SensorStatus = SensorStatus(1, "NO_CONTACT")

            /** Heart rate data is currently too unreliable to be used. */
            @JvmField
            public val UNRELIABLE: SensorStatus = SensorStatus(2, "UNRELIABLE")

            /** Heart rate data is available but the accuracy is low. */
            @JvmField
            public val ACCURACY_LOW: SensorStatus = SensorStatus(3, "ACCURACY_LOW")

            /** Heart rate data is available and the accuracy is medium. */
            @JvmField
            public val ACCURACY_MEDIUM: SensorStatus = SensorStatus(4, "ACCURACY_MEDIUM")

            /** Heart rate data is available with high accuracy. */
            @JvmField
            public val ACCURACY_HIGH: SensorStatus = SensorStatus(5, "ACCURACY_HIGH")

            @RestrictTo(RestrictTo.Scope.LIBRARY)
            @JvmField
            public val VALUES: List<SensorStatus> =
                listOf(
                    UNKNOWN,
                    NO_CONTACT,
                    UNRELIABLE,
                    ACCURACY_LOW,
                    ACCURACY_MEDIUM,
                    ACCURACY_HIGH,
                )

            /** @hide */
            @RestrictTo(RestrictTo.Scope.LIBRARY)
            public fun fromProto(proto: SensorStatusProto): SensorStatus =
                VALUES.firstOrNull { it.id == proto.number } ?: UNKNOWN
        }
    }

    /** @hide */
    override val proto: DataProto.DataPointAccuracy =
        DataProto.DataPointAccuracy.newBuilder()
            .setHrAccuracy(HrAccuracyProto.newBuilder().setSensorStatus(sensorStatus.toProto()))
            .build()

    override fun toString(): String = "HrAccuracy(sensorStatus=$sensorStatus)"

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<HeartRateAccuracy> = newCreator {
            val proto = DataProto.DataPointAccuracy.parseFrom(it)
            HeartRateAccuracy(proto)
        }
    }
}
