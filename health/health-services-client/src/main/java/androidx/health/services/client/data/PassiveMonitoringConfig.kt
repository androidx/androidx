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

import android.content.ComponentName
import android.os.Parcelable
import androidx.health.services.client.proto.DataProto

/**
 * Defines configuration for a passive monitoring request using Health Services.
 *
 * @constructor Creates a new PassiveMonitoringConfig which defines a request for passive monitoring
 * using Health Services
 *
 * @property dataTypes set of [DataType]s which should be tracked
 * @property componentName [ComponentName] which [PassiveMonitoringUpdate] intents should be sent to
 * @property requestUserActivityState whether to request the [UserActivityState] to be included in
 * [PassiveMonitoringUpdate]s
 */
@Suppress("ParcelCreator")
public class PassiveMonitoringConfig
constructor(
    public val dataTypes: Set<DataType>,
    public val componentName: ComponentName,
    @get:JvmName("requestUserActivityState") public val requestUserActivityState: Boolean = true,
) : ProtoParcelable<DataProto.PassiveMonitoringConfig>() {

    internal constructor(
        proto: DataProto.PassiveMonitoringConfig
    ) : this(
        proto.dataTypesList.map { DataType(it) }.toSet(),
        ComponentName(proto.packageName, proto.receiverClassName),
        proto.includeUserActivityState
    )

    init {
        require(dataTypes.isNotEmpty()) { "Must specify the desired data types." }
    }

    /**
     * Builder for [PassiveMonitoringConfig] instances.
     *
     * @constructor Create empty Builder
     */
    public class Builder {
        private var dataTypes: Set<DataType> = emptySet()
        private var componentName: ComponentName? = null
        private var requestUserActivityState: Boolean = false

        /**
         * Sets the requested [DataType]s that should be passively tracked. It is required to
         * specify a set of [DataType]s to create a valid configuration. Failure to do so will
         * result in an exception thrown when [build] is called.
         */
        public fun setDataTypes(dataTypes: Set<DataType>): Builder {
            this.dataTypes = dataTypes.toSet()
            return this
        }

        /**
         * Sets the [ComponentName] that WHS should send the [PassiveMonitoringUpdate] intents to.
         */
        public fun setComponentName(componentName: ComponentName): Builder {
            this.componentName = componentName
            return this
        }

        /**
         * Sets whether to request the [UserActivityState] to be included in
         * [PassiveMonitoringUpdate]s. If not set they will not be included by default.
         * [UserActivityState] requires [android.Manifest.permission.ACTIVITY_RECOGNITION].
         */
        @Suppress("MissingGetterMatchingBuilder")
        public fun setRequestUserActivityState(requestUserActivityState: Boolean): Builder {
            this.requestUserActivityState = requestUserActivityState
            return this
        }

        /** Returns the built [PassiveMonitoringConfig]. */
        public fun build(): PassiveMonitoringConfig {
            require(dataTypes.isNotEmpty()) { "Must specify the desired data types." }
            return PassiveMonitoringConfig(
                dataTypes,
                checkNotNull(componentName) { "No component name specified." },
                requestUserActivityState,
            )
        }
    }

    /** @hide */
    override val proto: DataProto.PassiveMonitoringConfig by lazy {
        DataProto.PassiveMonitoringConfig.newBuilder()
            .addAllDataTypes(dataTypes.map { it.proto })
            .setPackageName(componentName.getPackageName())
            .setReceiverClassName(componentName.getClassName())
            .setIncludeUserActivityState(requestUserActivityState)
            .build()
    }

    public companion object {
        @JvmStatic public fun builder(): Builder = Builder()

        @JvmField
        public val CREATOR: Parcelable.Creator<PassiveMonitoringConfig> = newCreator { bytes ->
            val proto = DataProto.PassiveMonitoringConfig.parseFrom(bytes)
            PassiveMonitoringConfig(proto)
        }
    }
}
