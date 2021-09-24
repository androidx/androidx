/*
 * Copyright 2021 The Android Open Source Project
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

import android.content.Intent
import android.os.IBinder
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.glance.Applier
import androidx.glance.GlanceInternalApi
import androidx.glance.layout.EmittableBox
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.lifecycle.lifecycleScope
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import androidx.wear.tiles.TimelineBuilders
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch

/**
 * [TileService] which can consume a Glance composition, convert it to a Wear Tile, and
 * provide it to the system. You must implement the [Content] function to provide your Tile
 * layout, for example:
 *
 * ```
 * class MyTile: GlanceTileService() {
 *   @Composable
 *   override fun Content {
 *     Text("Hello World!")
 *   }
 * }
 * ```
 *
 * You must also register ComposeTileService instances in your manifest to allow the system to
 * discover them. Your service must include an intent filter for the action
 * `androidx.wear.tiles.action.BIND_TILE_PROVIDER`, and require the permission
 * `com.google.android.wearable.permission.BIND_TILE_PROVIDER`.
 */

@OptIn(GlanceInternalApi::class)
public abstract class GlanceTileService : TileService() {
    private val lifecycleOwner = object : LifecycleOwner {
        val localLifecycle = LifecycleRegistry(this)
        override fun getLifecycle(): Lifecycle = localLifecycle
    }

    private val lifecycleDispatcher = ServiceLifecycleDispatcher(lifecycleOwner)
    private val coroutineScope =
        CoroutineScope(
            lifecycleOwner.lifecycleScope.coroutineContext + BroadcastFrameClock()
        )

    override fun onCreate() {
        lifecycleDispatcher.onServicePreSuperOnCreate()
        super.onCreate()
    }

    override fun onBind(intent: Intent): IBinder? {
        lifecycleDispatcher.onServicePreSuperOnBind()
        return super.onBind(intent)
    }

    override fun onDestroy() {
        lifecycleDispatcher.onServicePreSuperOnDestroy()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    override fun onStart(intent: Intent?, startId: Int) {
        lifecycleDispatcher.onServicePreSuperOnStart()
        super.onStart(intent, startId)
    }

    private suspend fun runComposition(): LayoutElementBuilders.LayoutElement = coroutineScope {
        val root = EmittableBox()
        val applier = Applier(root)
        val recomposer = Recomposer(currentCoroutineContext())
        val composition = Composition(applier, recomposer)

        composition.setContent { Content() }

        launch { recomposer.runRecomposeAndApplyChanges() }

        recomposer.close()
        recomposer.join()

        translateComposition(this@GlanceTileService, root)
    }

    /** Implement with the layout to use in your Tile. */
    @Composable
    public abstract fun Content()

    /**
     * Called by the system to fetch a tile from this [GlanceTileService].
     *
     * Note that this call exists due to this class extending [TileService]; this should not be
     * called directly.
     */
    final override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> = coroutineScope.future {
        val content = runComposition()

        TileBuilders.Tile.Builder()
            .setResourcesVersion(ResourcesVersion)
            .setTimeline(
                TimelineBuilders.Timeline.Builder()
                    .addTimelineEntry(
                        TimelineBuilders.TimelineEntry.Builder()
                            .setLayout(
                                LayoutElementBuilders.Layout.Builder()
                                    .setRoot(content)
                                    .build()
                            ).build()
                    ).build()
            ).build()
    }

    /**
     * Called by the system to fetch a resources bundle from this [GlanceTileService].
     *
     * Note that this call exists due to this class extending [TileService]; this should not be
     * called directly.
     */
    final override fun onResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ListenableFuture<ResourceBuilders.Resources> = coroutineScope.future {
        ResourceBuilders.Resources.Builder().setVersion(ResourcesVersion).build()
    }

    @VisibleForTesting
    internal companion object {
        // We're not dealing with resources for now; don't bother with resource versioning for a bit
        @VisibleForTesting
        internal const val ResourcesVersion = "1"
    }
}