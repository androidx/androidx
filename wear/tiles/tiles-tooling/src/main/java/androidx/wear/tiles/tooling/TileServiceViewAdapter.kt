/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.tiles.tooling

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.StateBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import androidx.wear.tiles.renderer.TileRenderer
import androidx.wear.tiles.timeline.TilesTimelineCache
import com.google.common.util.concurrent.ListenableFuture
import java.lang.reflect.Method
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

private const val TOOLS_NS_URI = "http://schemas.android.com/tools"

/**
 * A method extending functionality of [Class.getDeclaredMethod] allowing to finding the methods
 * (including non-public) declared in the superclasses as well.
 */
internal fun Class<out Any>.findMethod(
    name: String,
    vararg parameterTypes: Class<out Any>
): Method {
    var currentClass: Class<out Any>? = this
    while (currentClass != null) {
        try {
            return currentClass.getDeclaredMethod(name, *parameterTypes)
        } catch (_: NoSuchMethodException) {}
        currentClass = currentClass.superclass
    }
    val methodSignature = "$name(${parameterTypes.joinToString { ", " }})"
    throw NoSuchMethodException(
        "Could not find method $methodSignature neither in $this nor in its superclasses."
    )
}

/**
 * View adapter that renders a class inheriting [TileService]. The class is found by reading the
 * `tools:tileServiceName` attribute that contains the FQCN.
 */
internal class TileServiceViewAdapter(context: Context, attrs: AttributeSet) :
    FrameLayout(context, attrs) {
    init {
        init(attrs)
    }

    private fun init(attrs: AttributeSet) {
        val tileServiceName = attrs.getAttributeValue(TOOLS_NS_URI, "tileServiceName") ?: return

        init(tileServiceName)
    }

    @SuppressLint("BanUncheckedReflection")
    @Suppress("UNCHECKED_CAST", "deprecation") // TODO(b/276343540): Use protolayout types
    internal fun init(tileServiceName: String) {
        val tileServiceClass = Class.forName(tileServiceName)

        // val tileService = <TileServiceClassName>()
        val tileService = tileServiceClass.getConstructor().newInstance() as TileService

        // tileService.attachBaseContext(context)
        val attachBaseContextMethod =
            tileServiceClass.findMethod("attachBaseContext", Context::class.java).apply {
                isAccessible = true
            }
        attachBaseContextMethod.invoke(tileService, context)

        val deviceParams = context.buildDeviceParameters()
        val tileRequest = RequestBuilders.TileRequest
            .Builder()
            .setCurrentState(StateBuilders.State.Builder().build())
            .setDeviceConfiguration(deviceParams)
            .build()

        // val tile = tileService.onTileRequest(tileRequest)
        val onTileRequestMethod =
            tileServiceClass
                .findMethod("onTileRequest", RequestBuilders.TileRequest::class.java)
                .apply { isAccessible = true }
        val tile =
            (onTileRequestMethod.invoke(tileService, tileRequest)
                    as ListenableFuture<TileBuilders.Tile>)
                .get(1, TimeUnit.SECONDS)

        val resourceRequest = RequestBuilders.ResourcesRequest
            .Builder()
            .setVersion(tile.resourcesVersion)
            .setDeviceConfiguration(deviceParams)
            .build()

        // val resources = tileService.onTileResourcesRequest(resourceRequest).get(1, TimeUnit.SECONDS)
        val onTileResourcesRequestMethod =
            tileServiceClass
                .findMethod("onTileResourcesRequest", RequestBuilders.ResourcesRequest::class.java)
                .apply { isAccessible = true }
        val resources =
            ResourceBuilders.Resources.fromProto(
                (onTileResourcesRequestMethod.invoke(tileService, resourceRequest) as
                    ListenableFuture<ResourceBuilders.Resources>)
                    .get(1, TimeUnit.SECONDS).toProto()
            )

        val layout = tile.timeline?.getCurrentLayout()
        if (layout != null) {
            val renderer = TileRenderer(context, ContextCompat.getMainExecutor(context)) {}
            val result = renderer.inflateAsync(layout, resources, this)
            result.addListener(
                {
                    (result.get().layoutParams as FrameLayout.LayoutParams).gravity = Gravity.CENTER
                },
                ContextCompat.getMainExecutor(context)
            )
        }
    }
}

@Suppress("deprecation") // For backwards compatibility.
internal fun androidx.wear.tiles.TimelineBuilders.Timeline?.getCurrentLayout():
    LayoutElementBuilders.Layout? {
    val now = System.currentTimeMillis()
    return this?.let {
            val cache = TilesTimelineCache(it)
            cache.findTimelineEntryForTime(now) ?: cache.findClosestTimelineEntry(now)
        }
        ?.layout
        ?.let { LayoutElementBuilders.Layout.fromProto(it.toProto()) }
}

/** Creates an instance of [DeviceParametersBuilders.DeviceParameters] from the [Context]. */
internal fun Context.buildDeviceParameters(): DeviceParametersBuilders.DeviceParameters {
    val displayMetrics = resources.displayMetrics
    val isScreenRound = resources.configuration.isScreenRound
    return DeviceParametersBuilders.DeviceParameters.Builder()
        .setScreenWidthDp((displayMetrics.widthPixels / displayMetrics.density).roundToInt())
        .setScreenHeightDp((displayMetrics.heightPixels / displayMetrics.density).roundToInt())
        .setScreenDensity(displayMetrics.density)
        .setScreenShape(
            if (isScreenRound) DeviceParametersBuilders.SCREEN_SHAPE_ROUND
            else DeviceParametersBuilders.SCREEN_SHAPE_RECT
        )
        .setDevicePlatform(DeviceParametersBuilders.DEVICE_PLATFORM_WEAR_OS)
        .build()
}
