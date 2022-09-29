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

import androidx.health.services.client.proto.DataProto.ExerciseUpdate.LatestMetricsEntry as LatestMetricsEntryProto
import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.health.services.client.data.ExerciseEndReason.Companion.toProto
import androidx.health.services.client.data.ExerciseUpdate.ActiveDurationCheckpoint
import androidx.health.services.client.proto.DataProto
import androidx.health.services.client.proto.DataProto.AchievedExerciseGoal
import java.time.Duration
import java.time.Instant

/** Contains the latest updated state and metrics for the current exercise. */
@Suppress("ParcelCreator")
public class ExerciseUpdate(
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

    /** Returns the list of the latest [DataPoint]s. */
    public val latestMetrics: DataPointContainer,

    /**
     * Returns the latest [ExerciseGoalType.ONE_TIME_GOAL] [ExerciseGoal]s that have been achieved.
     * [ExerciseGoalType.MILESTONE] [ExerciseGoal]s will be returned via
     * [latestMilestoneMarkerSummaries].
     */
    public val latestAchievedGoals: Set<ExerciseGoal<out Number>>,

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

    /**
     * Returns the [ExerciseStateInfo] containing the current [ExerciseState] and
     * [ExerciseEndReason], if applicable.
     */
    public val exerciseStateInfo: ExerciseStateInfo,
) : ProtoParcelable<DataProto.ExerciseUpdate>() {
    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public constructor(
        proto: DataProto.ExerciseUpdate
    ) : this(
        if (proto.hasStartTimeEpochMs()) Instant.ofEpochMilli(proto.startTimeEpochMs) else null,
        Duration.ofMillis(proto.activeDurationMs),
        if (proto.hasUpdateDurationFromBootMs()) {
            Duration.ofMillis(proto.updateDurationFromBootMs)
        } else {
            null
        },
        exerciseUpdateProtoToDataPointContainer(proto),
        proto.latestAchievedGoalsList.map { ExerciseGoal.fromProto(it.exerciseGoal) }.toSet(),
        proto.mileStoneMarkerSummariesList.map { MilestoneMarkerSummary(it) }.toSet(),
        if (proto.hasExerciseConfig()) ExerciseConfig(proto.exerciseConfig) else null,
        if (proto.hasActiveDurationCheckpoint()) {
            ActiveDurationCheckpoint.fromProto(proto.activeDurationCheckpoint)
        } else {
            null
        },
        ExerciseStateInfo(
            ExerciseState.fromProto(proto.state)
                ?: throw IllegalStateException("Invalid ExerciseState: ${proto.state}"),
            ExerciseEndReason.fromProto(proto.exerciseEndReason)
        ),
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
    override val proto: DataProto.ExerciseUpdate = getExerciseUpdateProto()

    private fun getExerciseUpdateProto(): DataProto.ExerciseUpdate {
        val builder =
            DataProto.ExerciseUpdate.newBuilder()
                .setState(exerciseStateInfo.state.toProto())
                .setActiveDurationMs(activeDuration.toMillis())
                .addAllLatestMetrics(
                    latestMetrics.sampleDataPoints
                        .groupBy { it.dataType }
                        .map {
                            LatestMetricsEntryProto.newBuilder()
                                .setDataType(it.key.proto)
                                .addAllDataPoints(it.value.map(SampleDataPoint<*>::proto))
                                .build()
                        }
                        // If we don't sort, equals() may not work.
                        .sortedBy { entry -> entry.dataType.name }
                )
                .addAllLatestMetrics(
                    latestMetrics.intervalDataPoints
                        .groupBy { it.dataType }
                        .map {
                            LatestMetricsEntryProto.newBuilder()
                                .setDataType(it.key.proto)
                                .addAllDataPoints(it.value.map(IntervalDataPoint<*>::proto))
                                .build()
                        }
                        // If we don't sort, equals() may not work.
                        .sortedBy { entry -> entry.dataType.name }
                )
                .addAllLatestAggregateMetrics(
                    latestMetrics.statisticalDataPoints
                        .map { it.proto }
                        // If we don't sort, equals() may not work.
                        .sortedBy { entry -> entry.statisticalDataPoint.dataType.name }
                ).addAllLatestAggregateMetrics(latestMetrics.cumulativeDataPoints
                    .map { it.proto }
                    // If we don't sort, equals() may not work.
                    .sortedBy { entry -> entry.cumulativeDataPoint.dataType.name })
                .addAllLatestAchievedGoals(latestAchievedGoals.map {
                    AchievedExerciseGoal.newBuilder().setExerciseGoal(it.proto).build()
                }).addAllMileStoneMarkerSummaries(latestMilestoneMarkerSummaries.map { it.proto })
                .setExerciseEndReason((exerciseStateInfo.endReason).toProto())

        startTime?.let { builder.setStartTimeEpochMs(startTime.toEpochMilli()) }
        updateDurationFromBoot?.let {
            builder.setUpdateDurationFromBootMs(updateDurationFromBoot.toMillis())
        }
        exerciseConfig?.let { builder.setExerciseConfig(exerciseConfig.toProto()) }
        activeDurationCheckpoint?.let {
            builder.setActiveDurationCheckpoint(activeDurationCheckpoint.toProto())
        }

        return builder.build()
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
     * Returns the ActiveDuration of the exercise at the time of the provided [IntervalDataPoint].
     * The provided [IntervalDataPoint] should be present in this [ExerciseUpdate].
     *
     * @throws IllegalArgumentException if [dataPoint] is not present in this [ExerciseUpdate]
     * @throws IllegalStateException if this [ExerciseUpdate] does not contain a valid
     * `updateDurationFromBoot` which may happen if the Health Services app is out of date
     */
    public fun getActiveDurationAtDataPoint(dataPoint: IntervalDataPoint<*>): Duration =
        getActiveDurationAtDataPoint(dataPoint, dataPoint.endDurationFromBoot)

    /**
     * Returns the ActiveDuration of the exercise at the time of the provided [SampleDataPoint].
     * The provided [SampleDataPoint] should be present in this [ExerciseUpdate].
     *
     * @throws IllegalArgumentException if [dataPoint] is not present in this [ExerciseUpdate]
     * @throws IllegalStateException if this [ExerciseUpdate] does not contain a valid
     * `updateDurationFromBoot` which may happen if the Health Services app is out of date
     */
    public fun getActiveDurationAtDataPoint(dataPoint: SampleDataPoint<*>): Duration =
        getActiveDurationAtDataPoint(dataPoint, dataPoint.timeDurationFromBoot)

    private fun getActiveDurationAtDataPoint(
        dataPoint: DataPoint<*>,
        durationFromBoot: Duration
    ): Duration {
        val dataPointList = latestMetrics.dataPoints[dataPoint.dataType]
        if (dataPointList?.indexOf(dataPoint) == -1) {
            throw IllegalArgumentException("dataPoint not found in ExerciseUpdate")
        }

        // If we are paused then the last active time applies to all updates.
        if (exerciseStateInfo.state == ExerciseState.USER_PAUSED ||
            exerciseStateInfo.state == ExerciseState.AUTO_PAUSED
        ) {
            return activeDuration
        }

        // Active duration applies to when this update was generated so calculate for the given time
        // by working backwards.
        // First find time since this point was generated.
        val durationSinceProvidedTime = getUpdateDurationFromBoot().minus(durationFromBoot)
        return activeDuration.minus(durationSinceProvidedTime)
    }

    override fun toString(): String =
        "ExerciseUpdate(" +
            "state=$exerciseStateInfo.state, " +
            "startTime=$startTime, " +
            "activeDuration=$activeDuration, " +
            "updateDurationFromBoot=$updateDurationFromBoot, " +
            "latestMetrics=$latestMetrics, " +
            "latestAchievedGoals=$latestAchievedGoals, " +
            "latestMilestoneMarkerSummaries=$latestMilestoneMarkerSummaries, " +
            "exerciseConfig=$exerciseConfig, " +
            "activeDurationCheckpoint=$activeDurationCheckpoint, " +
            "exerciseEndReason=${exerciseStateInfo.endReason}" +
            ")"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExerciseUpdate) return false

        if (startTime != other.startTime) return false
        if (activeDuration != other.activeDuration) return false
        if (latestMetrics != other.latestMetrics) return false
        if (latestAchievedGoals != other.latestAchievedGoals) return false
        if (latestMilestoneMarkerSummaries != other.latestMilestoneMarkerSummaries) return false
        if (exerciseConfig != other.exerciseConfig) return false
        if (activeDurationCheckpoint != other.activeDurationCheckpoint) return false
        if (exerciseStateInfo != other.exerciseStateInfo) return false
        if (updateDurationFromBoot != other.updateDurationFromBoot) return false

        return true
    }

    override fun hashCode(): Int {
        var result = startTime?.hashCode() ?: 0
        result = 31 * result + activeDuration.hashCode()
        result = 31 * result + latestMetrics.hashCode()
        result = 31 * result + latestAchievedGoals.hashCode()
        result = 31 * result + latestMilestoneMarkerSummaries.hashCode()
        result = 31 * result + (exerciseConfig?.hashCode() ?: 0)
        result = 31 * result + (activeDurationCheckpoint?.hashCode() ?: 0)
        result = 31 * result + exerciseStateInfo.hashCode()
        result = 31 * result + (updateDurationFromBoot?.hashCode() ?: 0)
        return result
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<ExerciseUpdate> = newCreator { bytes ->
            val proto = DataProto.ExerciseUpdate.parseFrom(bytes)
            ExerciseUpdate(proto)
        }

        internal fun exerciseUpdateProtoToDataPointContainer(
            proto: DataProto.ExerciseUpdate
        ): DataPointContainer {
            val dataPoints = mutableListOf<DataPoint<*>>()

            proto.latestMetricsList
                .flatMap { it.dataPointsList }
                .forEach {
                    dataPoints += DataPoint.fromProto(it)
                }
            proto.latestAggregateMetricsList
                .forEach {
                    dataPoints += DataPoint.fromProto(it)
                }

            return DataPointContainer(dataPoints)
        }
    }
}
