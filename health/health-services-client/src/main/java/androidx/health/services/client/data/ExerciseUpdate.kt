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
import androidx.health.services.client.proto.DataProto.AggregateDataPoint.AggregateCase.AGGREGATE_NOT_SET
import androidx.health.services.client.proto.DataProto.AggregateDataPoint.AggregateCase.CUMULATIVE_DATA_POINT
import androidx.health.services.client.proto.DataProto.AggregateDataPoint.AggregateCase.STATISTICAL_DATA_POINT
import androidx.health.services.client.proto.DataProto.ExerciseUpdate.LatestMetricsEntry as LatestMetricsEntryProto
import java.lang.IllegalStateException
import java.time.Duration
import java.time.Instant

/** Contains the latest updated state and metrics for the current exercise. */
@Suppress("ParcelCreator")
public class ExerciseUpdate(
    /** Returns the current status of the exercise. */
    public val state: ExerciseState,

    /**
     * Returns the time at which the exercise was started or `null` if the exercise is in prepare
     * phase and hasn't started yet.
     */
    public val startTime: Instant?,

    /**
     * Returns the total elapsed time for which the exercise has been active, i.e. started but not
     * paused. The active duration is zero in prepare phase.
     */
    public val activeDuration: Duration,

    /**
     * Returns the list of latest [DataPoint] for each metric keyed by data type name. This allows a
     * client to easily query for the "current" values of each metric since last call.
     */
    public val latestMetrics: Map<DataType, List<DataPoint>>,

    /** Returns the latest aggregated values for each metric keyed by [DataType#name]. */
    public val latestAggregateMetrics: Map<DataType, AggregateDataPoint>,

    /**
     * Returns the latest `#ONE_TIME_GOAL` [ExerciseGoal] s that have been achieved. `#MILESTONE`
     * [ExerciseGoal] s will be returned via `#getLatestMilestoneMarkerSummaries` below.
     */
    public val latestAchievedGoals: Set<AchievedExerciseGoal>,

    /** Returns the latest [MilestoneMarkerSummary] s. */
    public val latestMilestoneMarkerSummaries: Set<MilestoneMarkerSummary>,

    /**
     * Returns the [ExerciseConfig] used by the exercise when the [ExerciseUpdate] was dispatched
     * and returns `null` if the exercise is in prepare phase and hasn't been started yet.
     */
    public val exerciseConfig: ExerciseConfig?,
) : ProtoParcelable<DataProto.ExerciseUpdate>() {

    /** @hide */
    public constructor(
        proto: DataProto.ExerciseUpdate
    ) : this(
        ExerciseState.fromProto(proto.state)
            ?: throw IllegalStateException("Invalid ExerciseState: ${proto.state}"),
        if (proto.hasStartTimeEpochMs()) Instant.ofEpochMilli(proto.startTimeEpochMs) else null,
        Duration.ofMillis(proto.activeDurationMs),
        proto
            .latestMetricsList
            .map { metric ->
                DataType(metric.dataType) to metric.dataPointsList.map { DataPoint(it) }
            }
            .toMap(),
        proto
            .latestAggregateMetricsList
            .map { metric ->
                when (metric.aggregateCase) {
                    CUMULATIVE_DATA_POINT -> DataType(metric.cumulativeDataPoint.dataType)
                    STATISTICAL_DATA_POINT -> DataType(metric.statisticalDataPoint.dataType)
                    null, AGGREGATE_NOT_SET ->
                        throw IllegalStateException("Aggregate not set on $metric")
                } to AggregateDataPoint.fromProto(metric)
            }
            .toMap(),
        proto.latestAchievedGoalsList.map { AchievedExerciseGoal(it) }.toSet(),
        proto.mileStoneMarkerSummariesList.map { MilestoneMarkerSummary(it) }.toSet(),
        if (proto.hasExerciseConfig()) ExerciseConfig(proto.exerciseConfig) else null
    )

    /** @hide */
    override val proto: DataProto.ExerciseUpdate by lazy {
        val builder =
            DataProto.ExerciseUpdate.newBuilder()
                .setState(state.toProto())
                .setActiveDurationMs(activeDuration.toMillis())
                .addAllLatestMetrics(
                    latestMetrics
                        .map {
                            LatestMetricsEntryProto.newBuilder()
                                .setDataType(it.key.proto)
                                .addAllDataPoints(it.value.map { dataPoint -> dataPoint.proto })
                                .build()
                        }
                        .sortedBy { entry ->
                            entry.dataType.name
                        } // If we don't sort, equals() may not work.
                )
                .addAllLatestAggregateMetrics(
                    latestAggregateMetrics.map { it.value.proto }.sortedBy { entry ->
                        when (entry.aggregateCase) {
                            CUMULATIVE_DATA_POINT -> entry.cumulativeDataPoint.dataType.name
                            STATISTICAL_DATA_POINT -> entry.statisticalDataPoint.dataType.name
                            null, AGGREGATE_NOT_SET ->
                                throw IllegalStateException("Aggregate not set on $entry")
                        }
                    }
                ) // If we don't sort, equals() may not work.
                .addAllLatestAchievedGoals(latestAchievedGoals.map { it.proto })
                .addAllMileStoneMarkerSummaries(latestMilestoneMarkerSummaries.map { it.proto })

        startTime?.let { builder.setStartTimeEpochMs(startTime.toEpochMilli()) }
        exerciseConfig?.let { builder.setExerciseConfig(exerciseConfig.proto) }

        builder.build()
    }

    override fun toString(): String =
        "ExerciseUpdate(" +
            "state=$state, " +
            "startTime=$startTime, " +
            "activeDuration=$activeDuration, " +
            "latestMetrics=$latestMetrics, " +
            "latestAggregateMetrics=$latestAggregateMetrics, " +
            "latestAchievedGoals=$latestAchievedGoals, " +
            "latestMilestoneMarkerSummaries=$latestMilestoneMarkerSummaries, " +
            "exerciseConfig=$exerciseConfig)"

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<ExerciseUpdate> = newCreator { bytes ->
            val proto = DataProto.ExerciseUpdate.parseFrom(bytes)
            ExerciseUpdate(proto)
        }
    }
}
