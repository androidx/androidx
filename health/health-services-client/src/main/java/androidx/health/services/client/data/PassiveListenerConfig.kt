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
 * by [PassiveListenerCallback.onNewDataPoints].
 * @property shouldRequestUserActivityState whether to request [UserActivityInfo] updates. Data will
 * be returned by [PassiveListenerCallback.onUserActivityInfo]. If set to true, calling app must
 * have [android.Manifest.permission.ACTIVITY_RECOGNITION].
 * @property passiveGoals set of [PassiveGoal]s which should be tracked. Achieved goals will be
 * returned by [PassiveListenerCallback.onGoalCompleted].
 */
@Suppress("ParcelCreator")
// TODO(b/227475943): open up visibility
internal class PassiveListenerConfig
public constructor(
    public val dataTypes: Set<DataType>,
    @get:JvmName("shouldRequestUserActivityState")
    public val shouldRequestUserActivityState: Boolean,
    public val passiveGoals: Set<PassiveGoal>,
) : ProtoParcelable<DataProto.PassiveListenerConfig>() {

    internal constructor(
        proto: DataProto.PassiveListenerConfig
    ) : this(
        proto.dataTypesList.map { DataType(it) }.toSet(),
        proto.includeUserActivityState,
        proto.passiveGoalsList.map { PassiveGoal(it) }.toSet(),
    )

    /** Builder for [PassiveListenerConfig] instances. */
    // TODO(b/227475943): open up visibility
    internal class Builder {
        private var dataTypes: Set<DataType> = emptySet()
        private var requestUserActivityState: Boolean = false
        private var passiveGoals: Set<PassiveGoal> = emptySet()

        /** Sets the requested [DataType]s that should be passively tracked. */
        public fun setDataTypes(dataTypes: Set<DataType>): Builder {
            this.dataTypes = dataTypes.toSet()
            return this
        }

        /**
         * Sets whether to request the [UserActivityState] updates. If not set they will not be
         * included by default and [PassiveListenerCallback.onUserActivityInfo] will not be invoked.
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
         * Sets the requested [PassiveGoal]s that should be passively tracked.
         *
         * @param passiveGoals the [PassiveGoal]s that should be tracked passively
         */
        public fun setPassiveGoals(passiveGoals: Set<PassiveGoal>): Builder {
            this.passiveGoals = passiveGoals.toSet()
            return this
        }

        /** Returns the built [PassiveListenerConfig]. */
        public fun build(): PassiveListenerConfig {
            return PassiveListenerConfig(
                dataTypes,
                requestUserActivityState,
                passiveGoals
            )
        }
    }

    /** @hide */
    override val proto: DataProto.PassiveListenerConfig by lazy {
        DataProto.PassiveListenerConfig.newBuilder()
            .addAllDataTypes(dataTypes.map { it.proto })
            .setIncludeUserActivityState(shouldRequestUserActivityState)
            .addAllPassiveGoals(passiveGoals.map { it.proto })
            .build()
    }

    // TODO(b/227475943): open up visibility
    internal companion object {
        @JvmStatic public fun builder(): Builder = Builder()

        @JvmField
        public val CREATOR: Parcelable.Creator<PassiveListenerConfig> = newCreator { bytes ->
            val proto = DataProto.PassiveListenerConfig.parseFrom(bytes)
            PassiveListenerConfig(proto)
        }
    }
}
