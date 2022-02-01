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

package androidx.glance.wear.tiles

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.annotation.CallSuper
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.Applier
import androidx.glance.GlanceModifier
import androidx.glance.layout.EmittableBox
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.LocalState
import androidx.glance.layout.Alignment
import androidx.glance.layout.fillMaxSize
import androidx.glance.state.GlanceState
import androidx.glance.state.GlanceStateDefinition
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.lifecycle.lifecycleScope
import androidx.wear.tiles.EventBuilders.TileRemoveEvent
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import androidx.wear.tiles.TimelineBuilders
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch
import java.util.Arrays

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
 *
 * @param errorUiLayout If not null and an error occurs within this glance wear tile, the tile is
 * updated with an error UI using the provided layout.
 */

public abstract class GlanceTileService(
    private val errorUiLayout: LayoutElementBuilders.LayoutElement? = errorUiLayout()
) : TileService() {
    private val lifecycleOwner = object : LifecycleOwner {
        val localLifecycle = LifecycleRegistry(this)
        override fun getLifecycle(): Lifecycle = localLifecycle
    }

    private val lifecycleDispatcher = ServiceLifecycleDispatcher(lifecycleOwner)
    private val coroutineScope =
        CoroutineScope(
            lifecycleOwner.lifecycleScope.coroutineContext + BroadcastFrameClock()
        )

    private var resources: ResourceBuilders.Resources? = null

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
        resources = null
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    override fun onStart(intent: Intent?, startId: Int) {
        lifecycleDispatcher.onServicePreSuperOnStart()
        super.onStart(intent, startId)
    }

    private fun getStateIdentifier(): String =
        ComponentName(this, javaClass).flattenToString()

    /**
     * Retrieve the state of the wear tile provided by this service
     */
    @Suppress("UNCHECKED_CAST")
    public suspend fun <T> getTileState(): T =
        GlanceState.getValue(
            this,
            checkNotNull(stateDefinition) { "No state defined in this service" },
            getStateIdentifier()
        ) as T

    /**
     * Update the state of the wear tile provided by this service
     */
    @Suppress("UNCHECKED_CAST")
    public suspend fun <T> updateTileState(updateState: suspend (T) -> T): T =
        GlanceState.updateValue(
            this,
            checkNotNull(stateDefinition as GlanceStateDefinition<T>?) {
                "No state defined in this service"
            },
            getStateIdentifier(),
            updateState
        )

    /**
     * Run the composition on the given time interval
     * When the timeline mode is singleEntry, pass in null argument
     */
    private suspend fun runComposition(
        screenSize: DpSize,
        state: Any?,
        timeInterval: TimeInterval? = null
    ): CompositionResult =
        coroutineScope {
            val root = EmittableBox()
            root.modifier = GlanceModifier.fillMaxSize()
            root.contentAlignment = Alignment.Center
            val applier = Applier(root)
            val recomposer = Recomposer(currentCoroutineContext())
            val composition = Composition(applier, recomposer)

            composition.setContent {
                CompositionLocalProvider(
                    LocalContext provides this@GlanceTileService,
                    LocalSize provides screenSize,
                    LocalState provides state,
                    LocalTimeInterval provides timeInterval
                ) { Content() }
            }

            launch { recomposer.runRecomposeAndApplyChanges() }

            recomposer.close()
            recomposer.join()

            normalizeCompositionTree(this@GlanceTileService, root)

            try {
                translateTopLevelComposition(this@GlanceTileService, root)
            } catch (ex: CancellationException) {
                throw ex
            } catch (throwable: Throwable) {
                if (errorUiLayout == null) {
                    throw throwable
                }
                Log.e(GlanceWearTileTag, throwable.toString())
                CompositionResult(errorUiLayout, ResourceBuilders.Resources.Builder())
            }
        }

    internal class GlanceTile(
        val tile: TileBuilders.Tile?,
        val resources: ResourceBuilders.Resources?
    )

    /**
     * Run the composition to build the resources, and, if required, tile as well
     */
    private suspend fun runComposition(
        screenSize: DpSize,
        resourcesOnly: Boolean
    ): GlanceTile = coroutineScope {
        val timelineBuilders = if (resourcesOnly) null else TimelineBuilders.Timeline.Builder()
        var resourcesBuilder: ResourceBuilders.Resources.Builder

        val state = if (stateDefinition != null) getTileState<Any>() else null
        if (timelineMode === TimelineMode.SingleEntry) {
            val content = runComposition(screenSize, state)

            timelineBuilders?.let {
                timelineBuilders.addTimelineEntry(
                    TimelineBuilders.TimelineEntry.Builder()
                        .setLayout(
                            LayoutElementBuilders.Layout.Builder()
                                .setRoot(content.layout)
                                .build()
                        ).build()
                )
            }

            resourcesBuilder = content.resources
        } else { // timelineMode is TimelineMode.TimeBoundEntries
            val timeIntervals = (timelineMode as TimelineMode.TimeBoundEntries).timeIntervals
            resourcesBuilder = ResourceBuilders.Resources.Builder()

            timeIntervals.forEach { interval ->
                val content = runComposition(screenSize, state, interval)
                timelineBuilders?.let {
                    timelineBuilders
                        .addTimelineEntry(
                            TimelineBuilders.TimelineEntry.Builder()
                                .setValidity(
                                    TimelineBuilders.TimeInterval.Builder()
                                        .setStartMillis(interval.start.toEpochMilli())
                                        .setEndMillis(interval.end.toEpochMilli())
                                        .build()
                                )
                                .setLayout(
                                    LayoutElementBuilders.Layout.Builder()
                                        .setRoot(content.layout)
                                        .build()
                                ).build()
                        )
                }

                content.resources.build().idToImageMapping.forEach { res ->
                    resourcesBuilder.addIdToImageMapping(res.key, res.value)
                }
            }
        }

        val resourcesVersion =
            Arrays.hashCode(
                resourcesBuilder.build().idToImageMapping.keys.toTypedArray()
            ).toString()
        resources = resourcesBuilder.setVersion(resourcesVersion).build()

        var tile: TileBuilders.Tile? = null
        timelineBuilders?.let {
            tile = TileBuilders.Tile.Builder()
                .setResourcesVersion(resourcesVersion)
                .setTimeline(timelineBuilders.build())
                .build()
        }

        GlanceTile(tile, resources)
    }

    /**
     * Data store for tile data specific to this service
     */
    public open val stateDefinition: GlanceStateDefinition<*>? = null

    /**
     * Defines the handling of timeline
     */
    public open val timelineMode: TimelineMode = TimelineMode.SingleEntry

    /** Override this method to set the layout to use in your Tile. */
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
        runComposition(
            if (requestParams.deviceParameters != null)
                DpSize(
                    requestParams.deviceParameters!!.screenWidthDp.dp,
                    requestParams.deviceParameters!!.screenHeightDp.dp
                ) else DpSize(0.dp, 0.dp),
            false
        ).tile!!
    }

    /**
     * Called by the system to fetch a resources bundle from this [GlanceTileService].
     *
     * Note that this call exists due to this class extending [TileService]; this should not be
     * called directly.
     */
    final override fun onResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ListenableFuture<ResourceBuilders.Resources> {
        resources?.let {
            if (requestParams.version !== resources!!.version) {
                Futures.immediateFailedFuture<ResourceBuilders.Resources>(
                    IllegalArgumentException(
                        "resource version is not matched " +
                            "between the request and the resources composed during onTileRequest"
                    )
                )
            }

            return Futures.immediateFuture(resources)
        }
        return coroutineScope.future {
            runComposition(
                if (requestParams.deviceParameters != null)
                    DpSize(
                        requestParams.deviceParameters!!.screenWidthDp.dp,
                        requestParams.deviceParameters!!.screenHeightDp.dp
                    )
                else DpSize(0.dp, 0.dp),
                true
            ).resources!!
        }
    }

    @CallSuper
    override fun onTileRemoveEvent(requestParams: TileRemoveEvent) {
        coroutineScope.launch {
            stateDefinition?.let {
                GlanceState.deleteStore(this as Context, it, getStateIdentifier())
            }
        }
    }
}