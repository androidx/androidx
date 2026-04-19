/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.glance.wear

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.OutcomeReceiver
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.glance.wear.cache.WearWidgetCache
import androidx.glance.wear.core.ActiveWearWidgetHandle
import androidx.glance.wear.core.ContainerInfo
import androidx.glance.wear.core.WearWidgetProviderInfo
import androidx.glance.wear.core.WidgetInstanceId
import androidx.glance.wear.util.isRobolectricBuild
import androidx.wear.utils.WearApiVersionHelper
import com.google.wear.Sdk
import com.google.wear.services.tiles.TileInstance
import com.google.wear.services.tiles.TileProvider
import com.google.wear.services.tiles.TilesManager
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.reflect.KClass
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Manager for Glance Wear Widgets.
 *
 * This is used to query the wear widgets currently active on the system.
 */
public class GlanceWearWidgetManager {
    private val context: Context
    /** The [TilesManager] used to query for active tiles/widgets on API 34+. */
    private val tilesManager: Lazy<TilesManager>
    /** The [ActiveWidgetStore] used to query for active widgets on API < 34. */
    private val activeWidgetStore: Lazy<ActiveWidgetStore>
    private val state: State

    /**
     * Creates a new [GlanceWearWidgetManager].
     *
     * @param context The application context.
     * @param tilesManager The [TilesManager] used to query for active tiles/widgets on API 34+.
     * @param activeWidgetStore The [ActiveWidgetStore] used to query for active widgets on API
     *   < 34.
     * @param widgetCache The [WearWidgetCache] used to store widget information.
     */
    @VisibleForTesting
    internal constructor(
        context: Context,
        tilesManager: TilesManager,
        activeWidgetStore: ActiveWidgetStore,
        widgetCache: WearWidgetCache,
    ) {
        this.context = context
        this.tilesManager = lazyOf(tilesManager)
        this.activeWidgetStore = lazyOf(activeWidgetStore)
        this.state = State(context, widgetCache)
    }

    /**
     * Creates a new [GlanceWearWidgetManager], providing an interface to check which tiles or
     * widgets are currently present on the system surfaces.
     *
     * This instance is designed for reuse and can be maintained as a singleton within your
     * application.
     *
     * @param context The application context.
     */
    public constructor(context: Context) {
        this.context = context
        this.tilesManager =
            lazy(LazyThreadSafetyMode.PUBLICATION) {
                Sdk.getWearManager(context, TilesManager::class.java)
            }
        this.activeWidgetStore = lazy { ActiveWidgetStore(context) }
        this.state = State(context, WearWidgetCache(context))
    }

    /**
     * Returns all currently active widgets instances associated with the calling package.
     *
     * A widget instance is active when it was added by the user to a widget surface.
     *
     * **Legacy Behavior (Pre-API 34):** On SDKs prior to Android 14 (U), this method uses a
     * best-effort approach to approximate platform behavior and may be incomplete. Results may omit
     * pre-installed widgets, widgets not visited within the last 60 days, or all widgets if the
     * user has cleared app data. Conversely, widgets removed via an app update may incorrectly
     * persist as "active" for up to 60 days post-removal.
     */
    public suspend fun fetchActiveWidgets(): List<ActiveWearWidgetHandle> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Api34Impl.fetchActiveTiles(tilesManager.value)
        } else {
            activeWidgetStore.value.getActiveWidgets()
        }

    /**
     * Returns all currently active widgets instances associated with the specified [widget] class.
     *
     * A widget instance is active when it was added by the user to a widget surface.
     *
     * **Legacy Behavior (Pre-API 34):** On SDKs prior to Android 14 (U), this method uses a
     * best-effort approach to approximate platform behavior and may be incomplete. Results may omit
     * pre-installed widgets, widgets not visited within the last 60 days, or all widgets if the
     * user has cleared app data. Conversely, widgets removed via an app update may incorrectly
     * persist as "active" for up to 60 days post-removal.
     */
    public suspend fun fetchActiveWidgets(
        widget: KClass<out GlanceWearWidget>
    ): List<ActiveWearWidgetHandle> {
        val serviceToWidgetMapping = state.getServiceToWidgetMapping()
        return fetchActiveWidgets().filter {
            serviceToWidgetMapping[it.provider] == widget.qualifiedName
        }
    }

    internal suspend fun updateServiceMapping(
        service: GlanceWearWidgetService,
        widget: GlanceWearWidget,
    ) {
        val serviceComponentName = ComponentName(context, service.javaClass)
        val widgetName = widget.canonicalName()
        state.updateServiceMapping(serviceComponentName.className, widgetName)
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private object Api34Impl {
        @DoNotInline
        suspend fun fetchActiveTiles(tilesManager: TilesManager): List<ActiveWearWidgetHandle> =
            suspendCancellableCoroutine { continuation ->
                val receiver = ContinuationOutcomeReceiver(continuation)
                tilesManager.getActiveTiles(Runnable::run, receiver)
            }

        private class ContinuationOutcomeReceiver(
            private val continuation: CancellableContinuation<List<ActiveWearWidgetHandle>>
        ) : OutcomeReceiver<List<TileInstance>, Exception> {
            override fun onResult(result: List<TileInstance>) {
                val widgets =
                    result.map { instance ->
                        val provider = instance.tileProvider
                        ActiveWearWidgetHandle(
                            provider = provider.componentName,
                            instanceId =
                                WidgetInstanceId(
                                    WidgetInstanceId.WIDGET_CAROUSEL_NAMESPACE,
                                    instance.id,
                                ),
                            containerType = provider.getContainerTypeCompat(),
                        )
                    }
                continuation.resume(widgets)
            }

            override fun onError(error: Exception) {
                continuation.resumeWithException(error)
            }
        }
    }

    /**
     * State for this Manager class, holds mappings between [GlanceWearWidgetService] and
     * [GlanceWearWidget].
     */
    private class State(private val context: Context, private val widgetCache: WearWidgetCache) {
        suspend fun getServiceToWidgetMapping(): Map<ComponentName, String> {
            val mapping =
                widgetCache.getServiceToWidgetMapping().takeIf { it.isNotEmpty() }
                    ?: recoverServiceToWidgetMapping()
            return mapping.mapKeys { (serviceName, _) -> ComponentName(context, serviceName) }
        }

        suspend fun updateServiceMapping(serviceName: String, widgetName: String) {
            widgetCache.update { putServiceToWidgetMapping(serviceName, widgetName) }
        }

        /**
         * Rebuilds the mapping between widget services and their associated widget classes by
         * querying the package manager for all services that handle the
         * [WearWidgetProviderInfo.ACTION_BIND_WIDGET_PROVIDER] intent.
         *
         * This should be used only after App data is cleared, which deletes all cached data.
         *
         * The result is also cached with [WearWidgetCache].
         */
        @SuppressLint("ListIterator")
        private suspend fun recoverServiceToWidgetMapping(): Map<String, String> {
            val availableWidgetServices =
                context.packageManager
                    .queryIntentServices(
                        Intent(WearWidgetProviderInfo.ACTION_BIND_WIDGET_PROVIDER)
                            .setPackage(context.packageName),
                        PackageManager.GET_META_DATA,
                    )
                    .mapNotNull { it.maybeGlanceWearWidgetService() }

            val serviceToWidgetMapping =
                availableWidgetServices.associate { it.serviceName() to it.widget.canonicalName() }
            widgetCache.update {
                serviceToWidgetMapping.forEach { (serviceName, widgetName) ->
                    putServiceToWidgetMapping(serviceName, widgetName)
                }
            }

            return serviceToWidgetMapping
        }

        companion object {
            /**
             * Returns the [GlanceWearWidgetService] instance for this [ResolveInfo], or null if
             * it's not a valid widget service or cannot be instantiated.
             */
            private fun ResolveInfo.maybeGlanceWearWidgetService(): GlanceWearWidgetService? =
                runCatching {
                        Class.forName(serviceInfo.name).getDeclaredConstructor().newInstance()
                            as? GlanceWearWidgetService
                    }
                    .getOrNull()
        }
    }
}

private fun GlanceWearWidgetService.serviceName() = this.javaClass.name

private fun GlanceWearWidget.canonicalName() = this.javaClass.canonicalName ?: this.javaClass.name

@ContainerInfo.ContainerType
private fun TileProvider.getContainerTypeCompat(): Int {
    if (
        isRobolectricBuild() ||
            WearApiVersionHelper.isApiVersionAtLeast(WearApiVersionHelper.WEAR_CINNAMON_BUN_0)
    ) {
        return when (containerType) {
            TilesManager.WIDGET_CONTAINER_TYPE_LARGE -> ContainerInfo.CONTAINER_TYPE_LARGE
            TilesManager.WIDGET_CONTAINER_TYPE_SMALL -> ContainerInfo.CONTAINER_TYPE_SMALL
            TilesManager.WIDGET_CONTAINER_TYPE_FULLSCREEN ->
                ContainerInfo.CONTAINER_TYPE_TILE_COMPAT
            else -> throw IllegalArgumentException("Unknown containerType $containerType")
        }
    }
    return ContainerInfo.CONTAINER_TYPE_TILE_COMPAT
}
