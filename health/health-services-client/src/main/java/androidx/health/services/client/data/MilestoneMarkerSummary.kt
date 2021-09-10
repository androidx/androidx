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
import androidx.health.services.client.proto.DataProto.MilestoneMarkerSummary.SummaryMetricsEntry
import java.time.Duration
import java.time.Instant

/**
 * The summary of metrics and state from the previously achieved milestone marker [ExerciseGoal].
 */
@Suppress("ParcelCreator")
public class MilestoneMarkerSummary(
    /** Returns the time at which this milestone marker started being tracked. */
    public val startTime: Instant,

    /** Returns the time at which this milestone marker was reached. */
    public val endTime: Instant,

    /**
     * Returns the total elapsed time for which the exercise was active during this milestone, i.e.
     * started but not paused.
     */
    public val activeDuration: Duration,

    /** The [AchievedExerciseGoal] that triggered this milestone summary. */
    public val achievedGoal: AchievedExerciseGoal,

    /**
     * Returns the [DataPoint] for each aggregated metric keyed by [DataType] tracked between
     * [startTime] and [endTime] i.e. during the duration of this milestone.
     */
    public val summaryMetrics: Map<DataType, AggregateDataPoint>,
) : ProtoParcelable<DataProto.MilestoneMarkerSummary>() {

    internal constructor(
        proto: DataProto.MilestoneMarkerSummary
    ) : this(
        Instant.ofEpochMilli(proto.startTimeEpochMs),
        Instant.ofEpochMilli(proto.endTimeEpochMs),
        Duration.ofMillis(proto.activeDurationMs),
        AchievedExerciseGoal(proto.achievedGoal),
        proto
            .summaryMetricsList
            .map { DataType(it.dataType) to AggregateDataPoint.fromProto(it.aggregateDataPoint) }
            .toMap()
    )

    /** @hide */
    override val proto: DataProto.MilestoneMarkerSummary by lazy {
        DataProto.MilestoneMarkerSummary.newBuilder()
            .setStartTimeEpochMs(startTime.toEpochMilli())
            .setEndTimeEpochMs(endTime.toEpochMilli())
            .setActiveDurationMs(activeDuration.toMillis())
            .setAchievedGoal(achievedGoal.proto)
            .addAllSummaryMetrics(
                summaryMetrics
                    .map {
                        SummaryMetricsEntry.newBuilder()
                            .setDataType(it.key.proto)
                            .setAggregateDataPoint(it.value.proto)
                            .build()
                    }
                    .sortedBy { entry ->
                        entry.dataType.name
                    } // Sorting to ensure equals() works correctly.
            )
            .build()
    }

    override fun toString(): String =
        "MilestoneMarkerSummary(" +
            "startTime=$startTime, " +
            "endTime=$endTime, " +
            "achievedGoal=$achievedGoal, " +
            "summaryMetrics=$summaryMetrics)"

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<MilestoneMarkerSummary> = newCreator { bytes ->
            val proto = DataProto.MilestoneMarkerSummary.parseFrom(bytes)
            MilestoneMarkerSummary(proto)
        }
    }
}
