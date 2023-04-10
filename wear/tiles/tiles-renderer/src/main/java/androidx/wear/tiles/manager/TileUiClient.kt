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

package androidx.wear.tiles.manager

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.MainThread
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.wear.tiles.DeviceParametersBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.StateBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TimelineBuilders
import androidx.wear.tiles.checkers.TimelineChecker
import androidx.wear.tiles.connection.DefaultTileClient
import androidx.wear.tiles.renderer.TileRenderer
import androidx.wear.tiles.timeline.TilesTimelineManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * UI client for a single tile. This handles binding to a Tile Service, and inflating the given
 * tile contents into the provided parentView. This also handles requested updates, re-fetching the
 * tile on-demand.
 *
 * After creation, you should call {@link #connect} to connect and start the initial fetch.
 * Likewise, when the owning activity is destroyed, you should call {@link #close} to disconnect
 * and release resources.
 */
public class TileUiClient(
    private val context: Context,
    component: ComponentName,
    private val parentView: ViewGroup
) : AutoCloseable {
    private companion object {
        private const val ACTION_REQUEST_TILE_UPDATE =
            "androidx.wear.tiles.action.REQUEST_TILE_UPDATE"
    }

    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)
    private val timelineChecker = TimelineChecker()

    private val tilesConnection = DefaultTileClient(
        context = context,
        componentName = component,
        coroutineScope = coroutineScope,
        coroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    )

    private var timelineManager: TilesTimelineManager? = null
    private var tileResources: ResourceBuilders.Resources? = null
    private val updateScheduler = UpdateScheduler(
        context.getSystemService(AlarmManager::class.java),
        SystemClock::elapsedRealtime
    )

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateScheduler.updateNow(false)
        }
    }

    private var isRunning = false

    /**
     * Initialize this {@link TileManager}. This will cause the {@link TileManager} to connect to
     * the tile service and request the first tile. It will also trigger any requested updates.
     */
    @MainThread
    public fun connect() {
        if (isRunning) {
            return
        }

        coroutineScope.launch { requestTile() }
        updateScheduler.enableUpdates()
        updateScheduler.setUpdateReceiver {
            coroutineScope.launch { requestTile() }
        }
        registerBroadcastReceiver()

        isRunning = true
    }

    /**
     * Shut down this {@link TileManager}. This will cancel any scheduled updates, and close the
     * connection with the tile service.
     */
    @MainThread
    override fun close() {
        if (!isRunning) {
            // Technically not needed, as the below code should be idempotent anyway.
            return
        }

        context.unregisterReceiver(updateReceiver)
        job.cancel()
        updateScheduler.disableUpdates()
        timelineManager?.close()
        timelineManager = null
        isRunning = false
    }

    private suspend fun requestTile(
        state: StateBuilders.State = StateBuilders.State.Builder().build()
    ) = coroutineScope {
        withContext(Dispatchers.Main) {
            val tileRequest = RequestBuilders.TileRequest
                .Builder()
                .setState(androidx.wear.tiles.StateBuilders.State.fromProto(state.toProto()))
                .setDeviceParameters(buildDeviceParameters())
                .build()

            val tile = tilesConnection.requestTile(tileRequest).await()

            if (tile.resourcesVersion.isEmpty()) {
                tileResources = ResourceBuilders.Resources.Builder().build()
            } else if (tile.resourcesVersion != tileResources?.version) {
                val resourcesRequest = RequestBuilders.ResourcesRequest
                    .Builder()
                    .setVersion(tile.resourcesVersion)
                    .setDeviceParameters(buildDeviceParameters())
                    .build()

                tileResources = ResourceBuilders.Resources.fromProto(
                    tilesConnection.requestResources(resourcesRequest).await().toProto()
                )
            }

            timelineManager?.apply {
                close()
            }

            // Check the tile and raise any validation errors.
            if (tile.timeline != null) {
                timelineChecker.doCheck(tile.timeline!!)
            }

            val localTimelineManager = TilesTimelineManager(
                context.getSystemService(AlarmManager::class.java),
                System::currentTimeMillis,
                tile.timeline ?: TimelineBuilders.Timeline.Builder().build(),
                0,
                ContextCompat.getMainExecutor(context),
                { _, layout -> updateContents(layout) }
            )
            timelineManager = localTimelineManager

            val freshnessInterval = tile.freshnessIntervalMillis
            if (freshnessInterval > 0) {
                updateScheduler.scheduleUpdateAtTime(freshnessInterval)
            }

            // Last check; only init this if we haven't been cancelled.
            if (isActive) {
                localTimelineManager.init()
            }
        }
    }

    private fun updateContents(layout: androidx.wear.tiles.LayoutElementBuilders.Layout) {
        parentView.removeAllViews()

        val renderer = TileRenderer(
            context,
            ContextCompat.getMainExecutor(context),
            { state -> coroutineScope.launch { requestTile(state) } }
        )
        renderer.inflate(
            LayoutElementBuilders.Layout.fromProto(layout.toProto()),
            tileResources!!,
            parentView
        )?.apply {
            (layoutParams as FrameLayout.LayoutParams).gravity = Gravity.CENTER
        }
    }

    private fun registerBroadcastReceiver() {
        val i = IntentFilter(ACTION_REQUEST_TILE_UPDATE)
        context.registerReceiver(updateReceiver, i)
    }

    private fun buildDeviceParameters(): DeviceParametersBuilders.DeviceParameters {
        val displayMetrics: DisplayMetrics = context.resources.displayMetrics
        val isScreenRound: Boolean = context.resources.configuration.isScreenRound
        return DeviceParametersBuilders.DeviceParameters.Builder()
            .setScreenWidthDp(Math.round(displayMetrics.widthPixels / displayMetrics.density))
            .setScreenHeightDp(Math.round(displayMetrics.heightPixels / displayMetrics.density))
            .setScreenDensity(displayMetrics.density)
            .setScreenShape(
                if (isScreenRound) DeviceParametersBuilders.SCREEN_SHAPE_ROUND
                else DeviceParametersBuilders.SCREEN_SHAPE_RECT
            )
            .setDevicePlatform(DeviceParametersBuilders.DEVICE_PLATFORM_WEAR_OS)
            .build()
    }
}
