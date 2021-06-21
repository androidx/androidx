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

/** Contains the latest updated state and metrics for the current exercise. */
public data class ExerciseUpdate(
    /** Returns the current status of the exercise. */
    val state: ExerciseState,

    /** Returns the time at which the exercise was started. */
    val startTime: Instant,

    /**
     * Returns the total elapsed time for which the exercise has been active, i.e. started but not
     * paused.
     */
    val activeDuration: Duration,

    /**
     * Returns the list of latest [DataPoint] for each metric keyed by data type name. This allows a
     * client to easily query for the "current" values of each metric since last call. There will
     * only be one value for an Aggregated DataType.
     */
    val latestMetrics: Map<DataType, List<DataPoint>>,

    /**
     * Returns the latest `#ONE_TIME_GOAL` [ExerciseGoal] s that have been achieved. `#MILESTONE`
     * [ExerciseGoal] s will be returned via `#getLatestMilestoneMarkerSummaries` below.
     */
    val latestAchievedGoals: Set<AchievedExerciseGoal>,

    /** Returns the latest [MilestoneMarkerSummary] s. */
    val latestMilestoneMarkerSummaries: Set<MilestoneMarkerSummary>,

    /**
     * Returns the [ExerciseConfig] used by the exercise when the [ExerciseUpdate] was dispatched.
     */
    val exerciseConfig: ExerciseConfig,
) : Parcelable {
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(state.id)
        dest.writeLong(startTime.toEpochMilli())
        dest.writeLong(activeDuration.toMillis())

        dest.writeInt(latestMetrics.size)
        for ((dataType, dataPoints) in latestMetrics) {
            dest.writeParcelable(dataType, flags)
            dest.writeInt(dataPoints.size)
            dest.writeTypedArray(dataPoints.toTypedArray(), flags)
        }

        dest.writeInt(latestAchievedGoals.size)
        dest.writeTypedArray(latestAchievedGoals.toTypedArray(), flags)

        dest.writeInt(latestMilestoneMarkerSummaries.size)
        dest.writeTypedArray(latestMilestoneMarkerSummaries.toTypedArray(), flags)

        dest.writeParcelable(exerciseConfig, flags)
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<ExerciseUpdate> =
            object : Parcelable.Creator<ExerciseUpdate> {
                override fun createFromParcel(source: Parcel): ExerciseUpdate? {
                    val exerciseState = ExerciseState.fromId(source.readInt()) ?: return null
                    val startTime = Instant.ofEpochMilli(source.readLong())
                    val activeDuration = Duration.ofMillis(source.readLong())

                    val numMetrics = source.readInt()
                    val latestMetrics = HashMap<DataType, List<DataPoint>>()
                    repeat(numMetrics) {
                        val dataType: DataType =
                            source.readParcelable(DataType::class.java.classLoader) ?: return null
                        val dataPointsArray = Array<DataPoint?>(source.readInt()) { null }
                        source.readTypedArray(dataPointsArray, DataPoint.CREATOR)
                        latestMetrics[dataType] = dataPointsArray.filterNotNull().toList()
                    }

                    val latestAchievedGoalsArray =
                        Array<AchievedExerciseGoal?>(source.readInt()) { null }
                    source.readTypedArray(latestAchievedGoalsArray, AchievedExerciseGoal.CREATOR)

                    val latestMilestoneMarkerSummariesArray =
                        Array<MilestoneMarkerSummary?>(source.readInt()) { null }
                    source.readTypedArray(
                        latestMilestoneMarkerSummariesArray,
                        MilestoneMarkerSummary.CREATOR
                    )

                    val exerciseConfig: ExerciseConfig =
                        source.readParcelable(ExerciseConfig::class.java.classLoader) ?: return null

                    return ExerciseUpdate(
                        exerciseState,
                        startTime,
                        activeDuration,
                        latestMetrics,
                        latestAchievedGoalsArray.filterNotNull().toSet(),
                        latestMilestoneMarkerSummariesArray.filterNotNull().toSet(),
                        exerciseConfig,
                    )
                }

                override fun newArray(size: Int): Array<ExerciseUpdate?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
