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

/**
 * The summary of metrics and state from the previously achieved milestone marker [ExerciseGoal].
 */
public data class MilestoneMarkerSummary(
    /** Returns the time at which this milestone marker started being tracked. */
    val startTime: Instant,

    /** Returns the time at which this milestone marker was reached. */
    val endTime: Instant,

    /**
     * Returns the total elapsed time for which the exercise was active during this milestone, i.e.
     * started but not paused.
     */
    val activeDuration: Duration,

    /** The [AchievedExerciseGoal] that triggered this milestone summary. */
    val achievedGoal: AchievedExerciseGoal,

    /**
     * Returns the [DataPoint] for each aggregated metric keyed by [DataType] tracked between
     * [startTime] and [endTime] i.e. during the duration of this milestone.
     */
    val summaryMetrics: Map<DataType, DataPoint>,
) : Parcelable {
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(startTime.toEpochMilli())
        dest.writeLong(endTime.toEpochMilli())
        dest.writeLong(activeDuration.toMillis())
        dest.writeParcelable(achievedGoal, flags)

        dest.writeInt(summaryMetrics.size)
        for ((dataType, dataPoint) in summaryMetrics) {
            dest.writeParcelable(dataType, flags)
            dest.writeParcelable(dataPoint, flags)
        }
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<MilestoneMarkerSummary> =
            object : Parcelable.Creator<MilestoneMarkerSummary> {
                override fun createFromParcel(source: Parcel): MilestoneMarkerSummary? {
                    val startTime = Instant.ofEpochMilli(source.readLong())
                    val endTime = Instant.ofEpochMilli(source.readLong())
                    val activeDuration = Duration.ofMillis(source.readLong())
                    val achievedGoal: AchievedExerciseGoal =
                        source.readParcelable(AchievedExerciseGoal::class.java.classLoader)
                            ?: return null

                    val summaryMetrics = HashMap<DataType, DataPoint>()
                    repeat(source.readInt()) {
                        val dataType: DataType =
                            source.readParcelable(DataType::class.java.classLoader) ?: return null
                        val dataPoint: DataPoint =
                            source.readParcelable(DataPoint::class.java.classLoader) ?: return null
                        summaryMetrics[dataType] = dataPoint
                    }

                    return MilestoneMarkerSummary(
                        startTime = startTime,
                        endTime = endTime,
                        activeDuration = activeDuration,
                        achievedGoal = achievedGoal,
                        summaryMetrics = summaryMetrics
                    )
                }

                override fun newArray(size: Int): Array<MilestoneMarkerSummary?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
