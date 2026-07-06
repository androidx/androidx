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
import androidx.glance.wear.core.RendererVersion
import androidx.glance.wear.core.WearWidgetParams
import androidx.glance.wear.core.WidgetInstanceId
import androidx.glance.wear.core.mapToList
import androidx.glance.wear.core.toIntSet
import androidx.glance.wear.proto.CachedPlayerOperation
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
internal open class WearWidgetCache
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
    open suspend fun update(block: WidgetCacheUpdateScope.() -> Unit): Boolean {
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
     * @param instanceId The instance id to use for the returned [WearWidgetParams].
     * @return The reconstructed [WearWidgetParams].
     * @throws [WidgetCacheMissException] if the requested cache entry can't be found.
     */
    open suspend fun getWidgetParams(
        @ContainerInfo.ContainerType containerType: Int,
        instanceId: WidgetInstanceId,
    ): WearWidgetParams {
        val cacheProto = dataStore.data.first()
        return cacheProto.container_type_to_spec[containerType]?.let { specProto ->
            WearWidgetParams(
                instanceId = instanceId,
                containerType = containerType,
                widthDp = specProto.width_dp,
                heightDp = specProto.height_dp,
                horizontalPaddingDp = specProto.horizontal_padding_dp,
                verticalPaddingDp = specProto.vertical_padding_dp,
                cornerRadiusDp = specProto.corner_radius_dp,
                rendererVersion =
                    run {
                        val supportedOps =
                            if (specProto.renderer_supported_operations.isNotEmpty()) {
                                specProto.renderer_supported_operations.toIntSet { it.op_code }
                            } else {
                                RendererVersion.DEFAULT_SUPPORTED_OPERATIONS
                            }
                        if (specProto.renderer_version_major == 0) {
                            RendererVersion(supportedOperations = supportedOps)
                        } else {
                            RendererVersion(
                                major = specProto.renderer_version_major,
                                minor = specProto.renderer_version_minor,
                                revision = specProto.renderer_version_revision,
                                supportedOperations = supportedOps,
                            )
                        }
                    },
            )
        } ?: throw WidgetCacheMissException("No params found for container type $containerType")
    }

    /**
     * Reads the container type for a given widget instance from the cache.
     *
     * @param instanceId The instance id of the widget to read the container type for.
     * @return The container type.
     * @throws [WidgetCacheMissException] if the requested cache entry can't be found.
     */
    open suspend fun getContainerTypeForInstance(instanceId: WidgetInstanceId): Int {
        val cacheProto = dataStore.data.first()
        return cacheProto.instance_id_to_type[instanceId.flattenToString()]
            ?: throw WidgetCacheMissException("No container type found for instance $instanceId")
    }

    /**
     * Reads the service-to-widget class mapping from the cache.
     *
     * @return The mapping from service class name to widget class name.
     */
    open suspend fun getServiceToWidgetMapping(): Map<String, String> {
        val cacheProto = dataStore.data.first()
        return cacheProto.service_to_widget_name
    }

    /** Scope for updating the widget cache. */
    @Suppress("PrimitiveInCollection") // Underlying proto code already creates the maps.
    class WidgetCacheUpdateScope(private val initialProto: WearWidgetCacheProto) {
        private val instanceIdToType = initialProto.instance_id_to_type.toMutableMap()
        private val containerTypeToSpec = initialProto.container_type_to_spec.toMutableMap()
        private val serviceToWidgetName = initialProto.service_to_widget_name.toMutableMap()

        /**
         * Sets the container type for a given widget instance. Overwrites any existing entry for
         * the same [instanceId].
         *
         * @param instanceId The instance id of the widget.
         * @param containerType The container type to associate with the instance.
         */
        fun setContainerTypeForInstance(
            instanceId: WidgetInstanceId,
            @ContainerInfo.ContainerType containerType: Int,
        ) {
            instanceIdToType[instanceId.flattenToString()] = containerType
        }

        /**
         * Sets the params for a given container type. Overwrites any existing entry for the same
         * container type in [params].
         *
         * @param params The parameters to be updated in the cache.
         */
        fun setWidgetParams(params: WearWidgetParams) {
            containerTypeToSpec[params.containerType] =
                WidgetContainerSpecProto(
                    width_dp = params.widthDp,
                    height_dp = params.heightDp,
                    horizontal_padding_dp = params.horizontalPaddingDp,
                    vertical_padding_dp = params.verticalPaddingDp,
                    corner_radius_dp = params.cornerRadiusDp,
                    renderer_version_major = params.rendererVersion.major,
                    renderer_version_minor = params.rendererVersion.minor,
                    renderer_version_revision = params.rendererVersion.revision,
                    renderer_supported_operations =
                        params.rendererVersion.supportedOperations.mapToList {
                            CachedPlayerOperation(it)
                        },
                )
        }

        /**
         * Puts the widget class name for a given service class name, overriding existing values.
         *
         * @param serviceName The class name of the service.
         * @param widgetName The class name of the widget.
         */
        fun putServiceToWidgetMapping(serviceName: String, widgetName: String) {
            serviceToWidgetName[serviceName] = widgetName
        }

        internal fun toProto(): WearWidgetCacheProto {
            return initialProto.copy(
                instance_id_to_type = instanceIdToType,
                container_type_to_spec = containerTypeToSpec,
                service_to_widget_name = serviceToWidgetName,
            )
        }
    }

    /** Exception thrown when a requested cache entry can't be found. */
    internal class WidgetCacheMissException(message: String) : Exception(message)

    internal companion object {
        private const val TAG = "WearWidgetCache"
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
