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

import android.content.Context
import android.os.Build
import android.os.OutcomeReceiver
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.glance.wear.core.ActiveWearWidgetHandle
import androidx.glance.wear.core.ContainerInfo
import androidx.glance.wear.core.WidgetInstanceId
import com.google.wear.Sdk
import com.google.wear.services.tiles.TileInstance
import com.google.wear.services.tiles.TilesManager
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Manager for Glance Wear Widgets.
 *
 * This is used to query the wear widgets currently active on the system.
 */
public class GlanceWearWidgetManager {

    /** The [TilesManager] used to query for active tiles/widgets on API 34+. */
    private val tilesManager: Lazy<TilesManager>
    /** The [ActiveWidgetStore] used to query for active widgets on API < 34. */
    private val activeWidgetStore: Lazy<ActiveWidgetStore>

    /**
     * Creates a new [GlanceWearWidgetManager].
     *
     * @param tilesManager The [TilesManager] used to query for active tiles/widgets on API 34+.
     * @param activeWidgetStore The [ActiveWidgetStore] used to query for active widgets on API
     *   < 34.
     */
    @VisibleForTesting
    internal constructor(tilesManager: TilesManager, activeWidgetStore: ActiveWidgetStore) {
        this.tilesManager = lazyOf(tilesManager)
        this.activeWidgetStore = lazyOf(activeWidgetStore)
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
        this.tilesManager =
            lazy(LazyThreadSafetyMode.PUBLICATION) {
                Sdk.getWearManager(context, TilesManager::class.java)
            }
        this.activeWidgetStore = lazy { ActiveWidgetStore(context) }
    }

    /**
     * Returns all currently active widgets and tiles associated with the calling package.
     *
     * A widget or a tile instance is active when it was added by the user to a widget or tile
     * surface.
     *
     * This includes:
     * * **Tiles** provided via `androidx.wear.tiles.TileService`.
     * * **Widgets** provided via [GlanceWearWidgetService].
     *
     * **Note:** A [ContainerInfo.CONTAINER_TYPE_FULLSCREEN] result represents either a standard
     * `TileService` or a [GlanceWearWidgetService] running in compatibility mode.
     *
     * **Legacy Behavior (Pre-API 34):** On SDKs prior to Android 14 (U), this method uses a
     * best-effort approach to approximate platform behavior and may be incomplete. Results may omit
     * pre-installed tiles or widgets, tiles or widgets not visited within the last 60 days, or all
     * tiles or widgets if the user has cleared app data. Conversely, tiles or widgets removed via
     * an app update may incorrectly persist as "active" for up to 60 days post-removal.
     */
    public suspend fun fetchActiveWidgets(): List<ActiveWearWidgetHandle> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Api34Impl.fetchActiveTiles(tilesManager.value)
        } else {
            activeWidgetStore.value.getActiveWidgets()
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
                            // TODO: b/485487815 - Set container type correctly when Wear 7 SDK will
                            // be available
                            containerType = ContainerInfo.CONTAINER_TYPE_FULLSCREEN,
                        )
                    }
                continuation.resume(widgets)
            }

            override fun onError(error: Exception) {
                continuation.resumeWithException(error)
            }
        }
    }
}
