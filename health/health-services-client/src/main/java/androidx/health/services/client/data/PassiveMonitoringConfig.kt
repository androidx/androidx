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

/** Configuration that defines a request for passive monitoring using HealthServices. */
@Suppress("DataClassPrivateConstructor", "ParcelCreator")
public class PassiveMonitoringConfig
protected constructor(
    public val dataTypes: Set<DataType>,
    public val componentName: ComponentName,
    @get:JvmName("shouldIncludeUserActivityState")
    public val shouldIncludeUserActivityState: Boolean,
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

    /** Builder for [PassiveMonitoringConfig] instances. */
    public class Builder {
        private var dataTypes: Set<DataType> = emptySet()
        private var componentName: ComponentName? = null
        private var shouldIncludeUserActivityState: Boolean = false

        /**
         * Sets the requested [DataType] s that should be passively tracked. It is required to
         * specify a set of [DataType]s to create a valid configuration. Failure to do so will
         * result in an exception thrown when `build` is called.
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
         * Sets whether to include the [UserActivityState] with the [PassiveMonitoringUpdate]s. If
         * not set they will not be included by default. [UserActivityState] requires
         * [permission.ACTIVITY_RECOGNITION]
         */
        @Suppress("MissingGetterMatchingBuilder")
        public fun setShouldIncludeUserActivityState(
            shouldIncludeUserActivityState: Boolean
        ): Builder {
            this.shouldIncludeUserActivityState = shouldIncludeUserActivityState
            return this
        }

        /** Returns the built [PassiveMonitoringConfig]. */
        public fun build(): PassiveMonitoringConfig {
            require(dataTypes.isNotEmpty()) { "Must specify the desired data types." }
            return PassiveMonitoringConfig(
                dataTypes,
                checkNotNull(componentName) { "No component name specified." },
                shouldIncludeUserActivityState,
            )
        }
    }

    /** @hide */
    override val proto: DataProto.PassiveMonitoringConfig by lazy {
        DataProto.PassiveMonitoringConfig.newBuilder()
            .addAllDataTypes(dataTypes.map { it.proto })
            .setPackageName(componentName.getPackageName())
            .setReceiverClassName(componentName.getClassName())
            .setIncludeUserActivityState(shouldIncludeUserActivityState)
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
