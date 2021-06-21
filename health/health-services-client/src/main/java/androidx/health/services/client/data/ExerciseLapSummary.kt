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

import android.os.Parcel
import android.os.Parcelable
import java.time.Duration
import java.time.Instant

/** Describes a completed exercise lap. */
public data class ExerciseLapSummary(
    /** Returns the lap count of this summary. Lap count starts at 1 for the first lap. */
    val lapCount: Int,

    /** Returns the time at which the lap has started. */
    val startTime: Instant,

    /** Returns the time at which the lap has ended. */
    val endTime: Instant,

    /**
     * Returns the total elapsed time for which the exercise has been active during this lap, i.e.
     * started but not paused.
     */
    val activeDuration: Duration,

    /**
     * Returns the [DataPoint] s for each metric keyed by [DataType] tracked between [startTime] and
     * [endTime] i.e. during the duration of this lap. This will only contain aggregated [DataType]
     * s calculated over the duration of the lap.
     */
    val lapMetrics: Map<DataType, DataPoint>,
) : Parcelable {
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(lapCount)
        dest.writeLong(startTime.toEpochMilli())
        dest.writeLong(endTime.toEpochMilli())
        dest.writeLong(activeDuration.toMillis())

        dest.writeInt(lapMetrics.size)
        for ((dataType, dataPoint) in lapMetrics) {
            dest.writeParcelable(dataType, flags)
            dest.writeParcelable(dataPoint, flags)
        }
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<ExerciseLapSummary> =
            object : Parcelable.Creator<ExerciseLapSummary> {
                override fun createFromParcel(source: Parcel): ExerciseLapSummary? {
                    val lapCount = source.readInt()
                    val startTime = Instant.ofEpochMilli(source.readLong())
                    val endTime = Instant.ofEpochMilli(source.readLong())
                    val activeDuration = Duration.ofMillis(source.readLong())

                    val lapMetrics = HashMap<DataType, DataPoint>()
                    val numMetrics = source.readInt()
                    repeat(numMetrics) {
                        val dataType: DataType =
                            source.readParcelable(DataType::class.java.classLoader) ?: return null
                        lapMetrics[dataType] =
                            source.readParcelable(DataPoint::class.java.classLoader) ?: return null
                    }

                    return ExerciseLapSummary(
                        lapCount,
                        startTime,
                        endTime,
                        activeDuration,
                        lapMetrics
                    )
                }

                override fun newArray(size: Int): Array<ExerciseLapSummary?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
