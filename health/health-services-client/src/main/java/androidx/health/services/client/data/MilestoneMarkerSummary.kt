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
import androidx.health.services.client.proto.DataProto.AchievedExerciseGoal
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

    /** The [ExerciseGoal] that triggered this milestone summary. */
    public val achievedGoal: ExerciseGoal<out Number>,

    /**
     * Returns the [DataPointContainer] for aggregated metrics tracked between [startTime] and
     * [endTime] i.e. during the duration of this milestone. This summary will only contain
     * [DataPoint]s for [AggregateDataType]s.
     */
    public val summaryMetrics: DataPointContainer,
) : ProtoParcelable<DataProto.MilestoneMarkerSummary>() {

    internal constructor(
        proto: DataProto.MilestoneMarkerSummary
    ) : this(
        Instant.ofEpochMilli(proto.startTimeEpochMs),
        Instant.ofEpochMilli(proto.endTimeEpochMs),
        Duration.ofMillis(proto.activeDurationMs),
        ExerciseGoal.fromProto(proto.achievedGoal.exerciseGoal),
        DataPointContainer(proto.summaryMetricsList.map {
                DataPoint.fromProto(it.aggregateDataPoint)
        })
    )

    /** @hide */
    override val proto: DataProto.MilestoneMarkerSummary by lazy {
        DataProto.MilestoneMarkerSummary.newBuilder()
            .setStartTimeEpochMs(startTime.toEpochMilli())
            .setEndTimeEpochMs(endTime.toEpochMilli())
            .setActiveDurationMs(activeDuration.toMillis())
            .setAchievedGoal(AchievedExerciseGoal.newBuilder().setExerciseGoal(achievedGoal.proto))
            .addAllSummaryMetrics(
                summaryMetrics.cumulativeDataPoints
                    .map {
                        SummaryMetricsEntry.newBuilder()
                            .setDataType(it.dataType.proto)
                            .setAggregateDataPoint(it.proto)
                            .build()
                    }
                    // Sorting to ensure equals() works correctly.
                    .sortedBy { it.dataType.name }
            )
            .addAllSummaryMetrics(
                summaryMetrics.statisticalDataPoints
                    .map {
                        SummaryMetricsEntry.newBuilder()
                            .setDataType(it.dataType.proto)
                            .setAggregateDataPoint(it.proto)
                            .build()
                    }
                    // Sorting to ensure equals() works correctly.
                    .sortedBy { it.dataType.name }
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
