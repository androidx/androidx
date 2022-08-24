/*
 * Copyright (C) 2022 The Android Open Source Project
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
import androidx.health.services.client.PassiveListenerCallback
import androidx.health.services.client.proto.DataProto

/**
 * Defines configuration for a passive monitoring listener request using Health Services.
 *
 * @constructor Creates a new [PassiveListenerConfig] which defines a request for passive monitoring
 * using Health Services
 *
 * @property dataTypes set of [DataType]s which should be tracked. Requested data will be returned
 * by [PassiveListenerCallback.onNewDataPointsReceived].
 * @property shouldRequestUserActivityState whether to request [UserActivityInfo] updates. Data will
 * be returned by [PassiveListenerCallback.onUserActivityInfoReceived]. If set to true, calling app
 * must have [android.Manifest.permission.ACTIVITY_RECOGNITION].
 * @property dailyGoals set of daily [PassiveGoal]s which should be tracked. Achieved goals will be
 * returned by [PassiveListenerCallback.onGoalCompleted].
 * @property healthEventTypes set of [HealthEvent.Type] which should be tracked. Detected health
 * events will be returned by [PassiveListenerCallback.onHealthEventReceived].
 */
@Suppress("ParcelCreator")
public class PassiveListenerConfig(
    public val dataTypes: Set<DataType<out Any, out DataPoint<out Any>>>,
    @get:JvmName("shouldRequestUserActivityState")
    public val shouldRequestUserActivityState: Boolean,
    public val dailyGoals: Set<PassiveGoal>,
    public val healthEventTypes: Set<HealthEvent.Type>
) : ProtoParcelable<DataProto.PassiveListenerConfig>() {

    internal constructor(
        proto: DataProto.PassiveListenerConfig
    ) : this(
        proto.dataTypesList.map { DataType.deltaFromProto(it) }.toSet(),
        proto.includeUserActivityState,
        proto.passiveGoalsList.map { PassiveGoal(it) }.toSet(),
        proto.healthEventTypesList
            .map { HealthEvent.Type.fromProto(it) }
            .toSet()
    )

    /** Builder for [PassiveListenerConfig] instances. */
    public class Builder {
        private var dataTypes: Set<DataType<*, *>> = emptySet()
        private var requestUserActivityState: Boolean = false
        private var dailyGoals: Set<PassiveGoal> = emptySet()
        private var healthEventTypes: Set<HealthEvent.Type> = emptySet()

        /** Sets the requested [DataType]s that should be passively tracked. */
        public fun setDataTypes(dataTypes: Set<DataType<*, *>>): Builder {
            this.dataTypes = dataTypes.toSet()
            return this
        }

        /**
         * Sets whether to request the [UserActivityState] updates. If not set they will not be
         * included by default and [PassiveListenerCallback.onUserActivityInfoReceived] will not be invoked.
         * [UserActivityState] requires [android.Manifest.permission.ACTIVITY_RECOGNITION].
         *
         * @param requestUserActivityState whether to request user activity state tracking
         */
        @Suppress("MissingGetterMatchingBuilder")
        public fun setShouldRequestUserActivityState(requestUserActivityState: Boolean): Builder {
            this.requestUserActivityState = requestUserActivityState
            return this
        }

        /**
         * Sets the requested daily [PassiveGoal]s that should be passively tracked.
         *
         * @param dailyGoals the daily [PassiveGoal]s that should be tracked passively
         */
        public fun setDailyGoals(dailyGoals: Set<PassiveGoal>): Builder {
            this.dailyGoals = dailyGoals.toSet()
            return this
        }

        /**
         * Sets the requested [HealthEvent.Type]s that should be passively tracked.
         *
         * @param healthEventTypes the [HealthEvent.Type]s that should be tracked passively
         */
        public fun setHealthEventTypes(healthEventTypes: Set<HealthEvent.Type>): Builder {
            this.healthEventTypes = healthEventTypes.toSet()
            return this
        }

        /** Returns the built [PassiveListenerConfig]. */
        public fun build(): PassiveListenerConfig {
            return PassiveListenerConfig(
                dataTypes,
                requestUserActivityState,
                dailyGoals,
                healthEventTypes
            )
        }
    }

    /** @hide */
    override val proto: DataProto.PassiveListenerConfig by lazy {
        DataProto.PassiveListenerConfig.newBuilder()
            .addAllDataTypes(dataTypes.map { it.proto })
            .setIncludeUserActivityState(shouldRequestUserActivityState)
            .addAllPassiveGoals(dailyGoals.map { it.proto })
            .addAllHealthEventTypes(healthEventTypes.map { it.toProto() })
            .build()
    }

    public companion object {
        @JvmStatic
        public fun builder(): Builder = Builder()

        @JvmField
        public val CREATOR: Parcelable.Creator<PassiveListenerConfig> = newCreator { bytes ->
            val proto = DataProto.PassiveListenerConfig.parseFrom(bytes)
            PassiveListenerConfig(proto)
        }
    }
}
