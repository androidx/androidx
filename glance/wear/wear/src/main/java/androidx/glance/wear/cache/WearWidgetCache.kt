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

package androidx.glance.wear.cache

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.glance.wear.core.ContainerInfo
import androidx.glance.wear.core.WidgetInstanceId
import androidx.glance.wear.proto.WearWidgetCacheProto
import androidx.glance.wear.proto.WidgetContainerSpecProto
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.flow.first

private const val DEFAULT_FILE_NAME = "androidx_glance_wear_widget_cache.pb"
private val Context.dataStore: DataStore<WearWidgetCacheProto> by
    dataStore(fileName = DEFAULT_FILE_NAME, serializer = WearWidgetCacheSerializer)

/**
 * Caches widget information, including container specs and instance-to-type mappings. The cache is
 * backed by a [DataStore] file.
 *
 * @param dataStore The [DataStore] to use for the cache.
 */
internal class WearWidgetCache
@VisibleForTesting
internal constructor(private val dataStore: DataStore<WearWidgetCacheProto>) {

    /** Creates a new [WearWidgetCache] instance. */
    constructor(context: Context) : this(context.dataStore)

    /**
     * Updates the cache atomically.
     *
     * @param block The block of code to run within the update scope.
     * @return `true` if the update was successful, `false` otherwise.
     */
    suspend fun update(block: WidgetCacheUpdateScope.() -> Unit): Boolean {
        return try {
            dataStore.updateData { cacheProto ->
                val scope = WidgetCacheUpdateScope(cacheProto)
                scope.block()
                scope.toProto()
            }
            true
        } catch (e: IOException) {
            Log.e(TAG, "Failed to update cache", e)
            false
        }
    }

    /**
     * Reads the container spec for a given container type from the cache.
     *
     * @param containerType The container type to read the spec for.
     * @return The [WidgetContainerSpec], or `null` if it doesn't exist in the cache.
     */
    suspend fun getContainerSpec(
        @ContainerInfo.ContainerType containerType: Int
    ): WidgetContainerSpec? {
        val cacheProto = dataStore.data.first()
        return cacheProto.container_type_to_spec[containerType]?.let {
            WidgetContainerSpec.fromProto(it)
        }
    }

    /**
     * Reads the container type for a given widget instance from the cache.
     *
     * @param instanceId The instance id of the widget to read the container type for.
     * @return The container type, or `null` if it doesn't exist in the cache.
     */
    suspend fun getInstanceType(instanceId: WidgetInstanceId): Int? {
        val cacheProto = dataStore.data.first()
        return cacheProto.instance_id_to_type[instanceId.flattenToString()]
    }

    /** Scope for updating the widget cache. */
    @Suppress("PrimitiveInCollection") // Underlying proto code already creates the maps.
    class WidgetCacheUpdateScope(private val initialProto: WearWidgetCacheProto) {
        private val instanceIdToType = initialProto.instance_id_to_type.toMutableMap()
        private val containerTypeToSpec = initialProto.container_type_to_spec.toMutableMap()

        /**
         * Sets the container type for a given widget instance. Overwrites any existing entry for
         * the same [instanceId].
         *
         * @param instanceId The instance id of the widget.
         * @param containerType The container type to associate with the instance.
         */
        fun setInstanceType(
            instanceId: WidgetInstanceId,
            @ContainerInfo.ContainerType containerType: Int,
        ) {
            instanceIdToType[instanceId.flattenToString()] = containerType
        }

        /**
         * Sets the container spec for a given container type. Overwrites any existing entry for the
         * same [containerType].
         *
         * @param containerType The container type.
         * @param spec The [WidgetContainerSpec] for the container type.
         */
        fun setContainerSpec(
            @ContainerInfo.ContainerType containerType: Int,
            spec: WidgetContainerSpec,
        ) {
            containerTypeToSpec[containerType] = spec.toProto()
        }

        internal fun toProto(): WearWidgetCacheProto {
            return initialProto.copy(
                instance_id_to_type = instanceIdToType,
                container_type_to_spec = containerTypeToSpec,
            )
        }
    }

    internal companion object {
        private const val TAG = "WearWidgetCache"
    }
}

/**
 * Represents the container spec for widget containers.
 *
 * @property widthDp The width of the content area in dp.
 * @property heightDp The height of the content area in dp.
 */
internal data class WidgetContainerSpec(val widthDp: Float, val heightDp: Float) {
    /** Converts this [WidgetContainerSpec] to a [WidgetContainerSpecProto]. */
    internal fun toProto() = WidgetContainerSpecProto(width_dp = widthDp, height_dp = heightDp)

    internal companion object {
        /** Converts a [WidgetContainerSpecProto] to a [WidgetContainerSpec]. */
        internal fun fromProto(specProto: WidgetContainerSpecProto) =
            WidgetContainerSpec(widthDp = specProto.width_dp, heightDp = specProto.height_dp)
    }
}

internal object WearWidgetCacheSerializer : Serializer<WearWidgetCacheProto> {
    override val defaultValue: WearWidgetCacheProto = WearWidgetCacheProto()

    override suspend fun readFrom(input: InputStream): WearWidgetCacheProto {
        return WearWidgetCacheProto.ADAPTER.decode(input)
    }

    override suspend fun writeTo(t: WearWidgetCacheProto, output: OutputStream) {
        t.encode(output)
    }
}
