/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.health.connect.client.records

import android.os.Build
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.impl.platform.records.toPlatformRecord
import androidx.health.connect.client.records.metadata.Metadata
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

/**
 * Represents intensity of an activity.
 *
 * Intensity can be either moderate or vigorous.
 *
 * Each record requires the start time, the end time and the activity intensity type.
 *
 * The ability to insert or read this record type is dependent on the version of Health Connect
 * installed on the device. To check if available: call [HealthConnectFeatures.getFeatureStatus] and
 * pass [HealthConnectFeatures.FEATURE_ACTIVITY_INTENSITY] as an argument.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class ActivityIntensityRecord(
    override val startTime: Instant,
    override val startZoneOffset: ZoneOffset?,
    override val endTime: Instant,
    override val endZoneOffset: ZoneOffset?,
    override val metadata: Metadata,
    /** Type of activity intensity (moderate or vigorous). */
    @property:ActivityIntensityTypes val activityIntensityType: Int,
) : IntervalRecord {

    /*
     * Android U devices and later use the platform's validation instead of Jetpack validation.
     * See b/400965398 for more context.
     */
    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            this.toPlatformRecord()
        } else {
            require(startTime.isBefore(endTime)) { "startTime must be before endTime." }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActivityIntensityRecord) return false

        if (activityIntensityType != other.activityIntensityType) return false
        if (startTime != other.startTime) return false
        if (startZoneOffset != other.startZoneOffset) return false
        if (endTime != other.endTime) return false
        if (endZoneOffset != other.endZoneOffset) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = activityIntensityType.hashCode()
        result = 31 * result + (startZoneOffset?.hashCode() ?: 0)
        result = 31 * result + endTime.hashCode()
        result = 31 * result + (endZoneOffset?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }

    override fun toString(): String {
        return "ActivityIntensityRecord(startTime=$startTime, startZoneOffset=$startZoneOffset, endTime=$endTime, endZoneOffset=$endZoneOffset, activityIntensityType=$activityIntensityType, metadata=$metadata)"
    }

    companion object {
        /**
         * Metric identifier to retrieve the total duration of moderate activity intensity from
         * [androidx.health.connect.client.aggregate.AggregationResult]. To check if this metric is
         * available, use [HealthConnectFeatures.getFeatureStatus] with
         * [HealthConnectFeatures.FEATURE_ACTIVITY_INTENSITY] as the argument.
         */
        @JvmField
        val MODERATE_DURATION_TOTAL: AggregateMetric<Duration> =
            AggregateMetric.durationMetric(
                "ActivityIntensity",
                aggregationType = AggregateMetric.AggregationType.DURATION,
                fieldName = "moderateDuration",
            )

        /**
         * Metric identifier to retrieve the total duration of vigorous activity intensity from
         * [androidx.health.connect.client.aggregate.AggregationResult]. To check if this metric is
         * available, use [HealthConnectFeatures.getFeatureStatus] with
         * [HealthConnectFeatures.FEATURE_ACTIVITY_INTENSITY] as the argument.
         */
        @JvmField
        val VIGOROUS_DURATION_TOTAL: AggregateMetric<Duration> =
            AggregateMetric.durationMetric(
                "ActivityIntensity",
                aggregationType = AggregateMetric.AggregationType.DURATION,
                fieldName = "vigorousDuration",
            )

        /**
         * Metric identifier to retrieve the total duration of activity intensity regardless of the
         * type from [androidx.health.connect.client.aggregate.AggregationResult]. To check if this
         * metric is available, use [HealthConnectFeatures.getFeatureStatus] with
         * [HealthConnectFeatures.FEATURE_ACTIVITY_INTENSITY] as the argument.
         */
        @JvmField
        val DURATION_TOTAL: AggregateMetric<Duration> =
            AggregateMetric.durationMetric(
                "ActivityIntensity",
                aggregationType = AggregateMetric.AggregationType.DURATION,
                fieldName = "duration",
            )

        /**
         * Metric identifier to retrieve the number of weighted intensity minutes from
         * [androidx.health.connect.client.aggregate.AggregationResult]. To check if this metric is
         * available, use [HealthConnectFeatures.getFeatureStatus] with
         * [HealthConnectFeatures.FEATURE_ACTIVITY_INTENSITY] as the argument.
         */
        @JvmField
        val INTENSITY_MINUTES_TOTAL: AggregateMetric<Duration> =
            AggregateMetric.durationMetric(
                "ActivityIntensity",
                aggregationType = AggregateMetric.AggregationType.DURATION,
                fieldName = "intensityMinutes",
            )

        /** Moderate intensity activity */
        const val ACTIVITY_INTENSITY_TYPE_MODERATE = 0

        /** Vigorous intensity activity. */
        const val ACTIVITY_INTENSITY_TYPE_VIGOROUS = 1

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        val ACTIVITY_INTENSITY_TYPE_STRING_TO_INT_MAP =
            mapOf(
                "moderate" to ACTIVITY_INTENSITY_TYPE_MODERATE,
                "vigorous" to ACTIVITY_INTENSITY_TYPE_VIGOROUS,
            )

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        val ACTIVITY_INTENSITY_TYPE_INT_TO_STRING_MAP =
            ACTIVITY_INTENSITY_TYPE_STRING_TO_INT_MAP.reverse()
    }

    /** List of supported activity intensities. */
    @Retention(AnnotationRetention.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef(value = [ACTIVITY_INTENSITY_TYPE_MODERATE, ACTIVITY_INTENSITY_TYPE_VIGOROUS])
    annotation class ActivityIntensityTypes
}
