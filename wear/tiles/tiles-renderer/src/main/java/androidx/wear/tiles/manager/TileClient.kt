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
import androidx.core.content.ContextCompat
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TimelineBuilders
import androidx.wear.tiles.connection.TilesConnection
import androidx.wear.tiles.proto.DeviceParametersProto
import androidx.wear.tiles.proto.RequestProto
import androidx.wear.tiles.proto.StateProto
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
 * UI client for a single tile. This handles binding to a Tile Provider, and inflating the given
 * tile contents into the provided parentView. This also handles requested updates, re-fetching the
 * tile on-demand.
 *
 * After creation, you should call {@link #connect} to connect and start the initial fetch.
 * Likewise, when the owning activity is destroyed, you should call {@link #close} to disconnect
 * and release resources.
 */
public class TileClient(
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

    private val tilesConnection = TilesConnection(
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
     * the tile provider and request the first tile. It will also trigger any requested updates.
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
     * connection with the tile provider.
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
        state: StateProto.State? = StateProto.State.getDefaultInstance()
    ) = coroutineScope {
        withContext(Dispatchers.Main) {
            val tileRequest = RequestProto.TileRequest
                .newBuilder()
                .setState(state)
                .setDeviceParameters(buildDeviceParameters())
                .build()

            val tile = tilesConnection.tileRequest(tileRequest)
            val tileProto = tile.toProto()

            if (tileProto.resourcesVersion.isNotEmpty() && tileProto.resourcesVersion
                != tileResources?.toProto()?.version
            ) {
                val resourcesRequest = RequestProto.ResourcesRequest
                    .newBuilder()
                    .setVersion(tileProto.resourcesVersion)
                    .setDeviceParameters(buildDeviceParameters())
                    .build()

                tileResources = tilesConnection.resourcesRequest(resourcesRequest)
            }

            timelineManager?.apply {
                close()
            }

            val localTimelineManager = TilesTimelineManager(
                context.getSystemService(AlarmManager::class.java),
                System::currentTimeMillis,
                TimelineBuilders.Timeline.fromProto(tile.toProto().timeline),
                0,
                ContextCompat.getMainExecutor(context),
                { _, layout -> updateContents(layout) }
            )
            timelineManager = localTimelineManager

            val freshnessInterval = tile.toProto().freshnessIntervalMillis
            if (freshnessInterval > 0) {
                updateScheduler.scheduleUpdateAtTime(freshnessInterval)
            }

            // Last check; only init this if we haven't been cancelled.
            if (isActive) {
                localTimelineManager.init()
            }
        }
    }

    private fun updateContents(layout: LayoutElementBuilders.Layout) {
        parentView.removeAllViews()

        val renderer = TileRenderer(
            context,
            layout,
            tileResources!!,
            ContextCompat.getMainExecutor(context),
            { state -> coroutineScope.launch { requestTile(state.toProto()) } }
        )
        renderer.inflate(parentView)?.apply {
            (layoutParams as FrameLayout.LayoutParams).gravity = Gravity.CENTER
        }
    }

    private fun registerBroadcastReceiver() {
        val i = IntentFilter(Companion.ACTION_REQUEST_TILE_UPDATE)
        context.registerReceiver(updateReceiver, i)
    }

    private fun buildDeviceParameters(): DeviceParametersProto.DeviceParameters? {
        val displayMetrics: DisplayMetrics = context.resources.displayMetrics
        val isScreenRound: Boolean = context.resources.configuration.isScreenRound
        return DeviceParametersProto.DeviceParameters.newBuilder()
            .setScreenWidthDp(Math.round(displayMetrics.widthPixels / displayMetrics.density))
            .setScreenHeightDp(Math.round(displayMetrics.heightPixels / displayMetrics.density))
            .setScreenDensity(displayMetrics.density)
            .setScreenShape(
                if (isScreenRound) DeviceParametersProto.ScreenShape.SCREEN_SHAPE_ROUND
                else DeviceParametersProto.ScreenShape.SCREEN_SHAPE_RECT
            )
            .setDevicePlatform(DeviceParametersProto.DevicePlatform.DEVICE_PLATFORM_WEAR_OS)
            .build()
    }
}
