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

    /** The duration since boot when this ExerciseUpdate was created. */
    private val updateDurationFromBoot: Duration?,

    /**
     * Returns the list of latest [DataPoint] for each metric keyed by data type name. This allows a
     * client to easily query for the "current" values of each metric since last call.
     */
    public val latestMetrics: Map<DataType, List<DataPoint>>,

    /** Returns the latest aggregated values for each metric keyed by [DataType.name]. */
    public val latestAggregateMetrics: Map<DataType, AggregateDataPoint>,

    /**
     * Returns the latest [ExerciseGoalType.ONE_TIME_GOAL] [ExerciseGoal]s that have been achieved.
     * [ExerciseGoalType.MILESTONE] [ExerciseGoal]s will be returned via
     * [latestMilestoneMarkerSummaries].
     */
    public val latestAchievedGoals: Set<AchievedExerciseGoal>,

    /** Returns the latest [MilestoneMarkerSummary]s. */
    public val latestMilestoneMarkerSummaries: Set<MilestoneMarkerSummary>,

    /**
     * Returns the [ExerciseConfig] used by the exercise when the [ExerciseUpdate] was dispatched
     * and returns `null` if the exercise is in prepare phase and hasn't been started yet.
     */
    public val exerciseConfig: ExerciseConfig?,

    /**
     * Returns the [ActiveDurationCheckpoint] which can be used to determine the active duration of
     * the exercise in a way that is consistent with Health Services. Clients can anchor their
     * application timers against this to ensure their view of the active duration matches the view
     * of Health Services.
     */
    public val activeDurationCheckpoint: ActiveDurationCheckpoint?,
) : ProtoParcelable<DataProto.ExerciseUpdate>() {

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public constructor(
        proto: DataProto.ExerciseUpdate
    ) : this(
        ExerciseState.fromProto(proto.state)
            ?: throw IllegalStateException("Invalid ExerciseState: ${proto.state}"),
        if (proto.hasStartTimeEpochMs()) Instant.ofEpochMilli(proto.startTimeEpochMs) else null,
        Duration.ofMillis(proto.activeDurationMs),
        if (proto.hasUpdateDurationFromBootMs()) {
            Duration.ofMillis(proto.updateDurationFromBootMs)
        } else {
            null
        },
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
        if (proto.hasExerciseConfig()) ExerciseConfig(proto.exerciseConfig) else null,
        if (proto.hasActiveDurationCheckpoint()) {
            ActiveDurationCheckpoint.fromProto(proto.activeDurationCheckpoint)
        } else {
            null
        }
    )

    /**
     * This records the last time the exercise transitioned from an active to an inactive state or
     * from an inactive to an active state, where inactive states match those found in
     * [ExerciseState.isPaused] or [ExerciseState.isEnded]. This can be used to calculate the
     * exercise active duration in a way that is consistent with Health Service's view of the
     * exercise.
     *
     * If the exercise is currently inactive, the exercise’s active duration will match
     * [activeDuration] below. If the exercise is active, the active duration can be calculated by
     * `activeDuration + (now() - time)`.
     */
    public class ActiveDurationCheckpoint(
        /**
         * Returns the time at which the exercise last transitioned between the active or inactive
         * states.
         */
        public val time: Instant,

        /**
         * Returns the active duration of the exercise at the time it last transitioned to "active",
         * or the duration when it transitioned to inactive if it's currently paused or stopped.
         */
        public val activeDuration: Duration,
    ) {

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        internal fun toProto(): DataProto.ExerciseUpdate.ActiveDurationCheckpoint =
            DataProto.ExerciseUpdate.ActiveDurationCheckpoint.newBuilder()
                .setTimeEpochMs(time.toEpochMilli())
                .setActiveDurationMs(activeDuration.toMillis())
                .build()

        override fun toString(): String =
            "ActiveDurationCheckpoint(time=$time, activeDuration=$activeDuration)"

        internal companion object {
            /** @hide */
            @RestrictTo(RestrictTo.Scope.LIBRARY)
            internal fun fromProto(
                proto: DataProto.ExerciseUpdate.ActiveDurationCheckpoint
            ): ActiveDurationCheckpoint? =
                ActiveDurationCheckpoint(
                    Instant.ofEpochMilli(proto.timeEpochMs),
                    Duration.ofMillis(proto.activeDurationMs)
                )
        }
    }

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
        updateDurationFromBoot?.let {
            builder.setUpdateDurationFromBootMs(updateDurationFromBoot.toMillis())
        }
        exerciseConfig?.let { builder.setExerciseConfig(exerciseConfig.proto) }
        activeDurationCheckpoint?.let {
            builder.setActiveDurationCheckpoint(activeDurationCheckpoint.toProto())
        }

        builder.build()
    }

    /**
     * Returns the duration since boot when this ExerciseUpdate was created.
     *
     * @throws IllegalStateException if this [ExerciseUpdate] does not contain a valid
     * `updateDurationFromBoot` which may happen if the Health Services app is out of date
     */
    public fun getUpdateDurationFromBoot(): Duration =
        updateDurationFromBoot
            ?: error(
                "updateDurationFromBoot unavailable; is the Health Services APK out of date?"
            )

    /**
     * Returns the ActiveDuration of the exercise at the time of the provided [DataPoint]. The
     * provided [DataPoint] should be present in this [ExerciseUpdate].
     *
     * @throws IllegalArgumentException if [dataPoint] is not present in this [ExerciseUpdate]
     * @throws IllegalStateException if this [ExerciseUpdate] does not contain a valid
     * `updateDurationFromBoot` which may happen if the Health Services app is out of date
     */
    public fun getActiveDurationAtDataPoint(dataPoint: DataPoint): Duration {
        val dataPointList = latestMetrics[dataPoint.dataType]
        if (dataPointList == null || dataPointList.indexOf(dataPoint) == -1) {
            throw IllegalArgumentException("dataPoint not found in ExerciseUpdate")
        }

        // If we are paused then the last active time applies to all updates.
        if (state == ExerciseState.USER_PAUSED || state == ExerciseState.AUTO_PAUSED) {
            return activeDuration
        }
        // Active duration applies to when this update was generated so calculate for the given time
        // by working backwards.
        // First find time since this point was generated.
        val durationSinceProvidedTime =
            getUpdateDurationFromBoot().minus(dataPoint.endDurationFromBoot)
        return activeDuration.minus(durationSinceProvidedTime)
    }

    override fun toString(): String =
        "ExerciseUpdate(" +
            "state=$state, " +
            "startTime=$startTime, " +
            "activeDuration=$activeDuration, " +
            "updateDurationFromBoot=$updateDurationFromBoot, " +
            "latestMetrics=$latestMetrics, " +
            "latestAggregateMetrics=$latestAggregateMetrics, " +
            "latestAchievedGoals=$latestAchievedGoals, " +
            "latestMilestoneMarkerSummaries=$latestMilestoneMarkerSummaries, " +
            "exerciseConfig=$exerciseConfig, " +
            "activeDurationCheckpoint=$activeDurationCheckpoint)"

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<ExerciseUpdate> = newCreator { bytes ->
            val proto = DataProto.ExerciseUpdate.parseFrom(bytes)
            ExerciseUpdate(proto)
        }
    }
}
