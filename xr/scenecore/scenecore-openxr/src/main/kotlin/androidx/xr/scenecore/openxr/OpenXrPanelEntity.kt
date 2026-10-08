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

package androidx.xr.scenecore.openxr

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.SurfaceControlViewHost
import android.view.View
import android.window.InputTransferToken
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.FieldOfView
import androidx.xr.runtime.math.Vector2
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.openxr.OpenXrSceneRuntime.Companion.VIRTUAL_PIXEL_DENSITY
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.PanelEntity
import androidx.xr.scenecore.runtime.PerceivedResolutionResult
import androidx.xr.scenecore.runtime.PixelDimensions
import androidx.xr.scenecore.runtime.ScenePose
import androidx.xr.scenecore.runtime.Space
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

private const val TAG = "OpenXrPanelEntity"

/** Gives up on the display-ID poll after 100 attempts * 10 ms = 1000 ms (one second). */
private const val DISPLAY_ID_POLL_ATTEMPTS = 100
private const val DISPLAY_ID_POLL_INTERVAL_MS = 10L

/**
 * OpenXR implementation of [PanelEntity] that attaches an Android View via SurfaceControlViewHost
 * to an OpenXR spatial scene entity.
 *
 * The runtime backs the panel with a virtual display whose ID is delivered asynchronously, so the
 * view is hosted and its surface handed to the runtime in the background after construction, see
 * [setupViewPanel]. Until then the entity is part of the scene graph but renders nothing.
 */
@RequiresApi(Build.VERSION_CODES.R)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class OpenXrPanelEntity
internal constructor(
    context: Context,
    entityHandle: Long,
    internal val view: View,
    pixelDimensions: PixelDimensions,
    nativeWrapper: SceneCoreOpenXrNative,
    sceneNodeRegistry: OpenXrSceneNodeRegistry,
    executor: ScheduledExecutorService,
) : OpenXrEntity(context, entityHandle, nativeWrapper, sceneNodeRegistry, executor), PanelEntity {

    private var _pixelDimensions: PixelDimensions = pixelDimensions
    private var _cornerRadius: Float = 0f

    /**
     * Hosts [view] on the runtime's virtual display; null until that display is available and after
     * [dispose].
     */
    private var viewHost: SurfaceControlViewHost? = null

    /**
     * Resizes the hosted view right away once [viewHost] exists; before that it is only recorded
     * and used when the view is first hosted.
     */
    override var sizeInPixels: PixelDimensions
        get() = _pixelDimensions
        set(value) {
            _pixelDimensions = value
            viewHost?.relayout(value.width, value.height)
        }

    /**
     * Stored only: the OpenXR ViewPanel has no corner radius control, so this value does not affect
     * rendering.
     */
    override var cornerRadius: Float
        get() = _cornerRadius
        set(value) {
            _cornerRadius = value
        }

    /** Derived from [sizeInPixels] at [VIRTUAL_PIXEL_DENSITY]; the panel has no depth. */
    override var size: Dimensions
        get() {
            val widthMeters = _pixelDimensions.width / VIRTUAL_PIXEL_DENSITY
            val heightMeters = _pixelDimensions.height / VIRTUAL_PIXEL_DENSITY
            return Dimensions(widthMeters, heightMeters, 0f)
        }
        set(value) {
            _pixelDimensions =
                PixelDimensions(
                    (value.width * VIRTUAL_PIXEL_DENSITY).toInt(),
                    (value.height * VIRTUAL_PIXEL_DENSITY).toInt(),
                )
            viewHost?.relayout(_pixelDimensions.width, _pixelDimensions.height)
        }

    init {
        setupViewPanel(context)
    }

    /**
     * Attaches [view] to the runtime's ViewPanel:
     * 1. asks the runtime for a virtual display sized to [sizeInPixels] at the context's density,
     * 2. polls for the answer on [executor], as the runtime offers no completion callback,
     * 3. on the main thread, hosts [view] on that display in a [SurfaceControlViewHost] and hands
     *    its surface package to the runtime,
     * 4. replays the parent and transform recorded on this entity while it had no surface.
     *
     * A failure at any step leaves the entity in the scene graph without content.
     */
    private fun setupViewPanel(context: Context) {
        if (entityHandle == INVALID_HANDLE || nativeWrapper.nativeScenecore == INVALID_HANDLE) {
            return
        }

        val dpi = context.resources.displayMetrics.densityDpi
        val futureHandle =
            nativeWrapper.requestAndroidViewPanelDisplayId(
                entityHandle,
                _pixelDimensions.width,
                _pixelDimensions.height,
                dpi,
            )
        if (futureHandle == 0L) {
            Log.e(TAG, "requestAndroidViewPanelDisplayId failed for entity $entityHandle")
            return
        }

        // The runtime answers the display-ID request asynchronously and exposes no completion
        // callback, so the future is polled from the executor until it resolves.
        executor.schedule(
            { pollDisplayId(context, futureHandle, attempt = 0) },
            0,
            TimeUnit.MILLISECONDS,
        )
    }

    /**
     * Polls the display-ID future once and re-schedules itself every [DISPLAY_ID_POLL_INTERVAL_MS]
     * until the runtime answers, [DISPLAY_ID_POLL_ATTEMPTS] is reached or the entity is disposed.
     */
    private fun pollDisplayId(context: Context, futureHandle: Long, attempt: Int) {
        if (entityHandle == INVALID_HANDLE || nativeWrapper.nativeScenecore == INVALID_HANDLE) {
            return
        }
        val result = nativeWrapper.pollAndroidViewPanelDisplayId(entityHandle, futureHandle)
        if (result == null) {
            if (attempt + 1 >= DISPLAY_ID_POLL_ATTEMPTS) {
                Log.e(
                    TAG,
                    "Timeout waiting for AndroidViewPanel display ID on entity $entityHandle",
                )
                return
            }
            executor.schedule(
                { pollDisplayId(context, futureHandle, attempt + 1) },
                DISPLAY_ID_POLL_INTERVAL_MS,
                TimeUnit.MILLISECONDS,
            )
            return
        }
        Handler(Looper.getMainLooper()).post {
            hostView(context, result.displayId, result.inputToken)
        }
    }

    /**
     * Hosts [view] on the runtime's virtual display [displayId] and hands the resulting surface
     * package to the runtime. [inputToken] is what the host needs to route input to the view: an
     * [InputTransferToken] on Android 15+, an [IBinder] host token before that. Main thread only.
     */
    @SuppressLint("NewApi")
    private fun hostView(context: Context, displayId: Int, inputToken: Any?) {
        // The entity or the runtime may have been disposed while the display was being set up.
        if (entityHandle == INVALID_HANDLE || nativeWrapper.nativeScenecore == INVALID_HANDLE) {
            return
        }

        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        val virtualDisplay = displayManager?.getDisplay(displayId)
        if (virtualDisplay == null) {
            Log.e(TAG, "Failed to get virtual display for display ID $displayId")
            return
        }
        val themedContext =
            ContextThemeWrapper(
                context.applicationContext.createDisplayContext(virtualDisplay),
                android.R.style.Theme_Material_Light_NoActionBar,
            )

        val host =
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM &&
                    inputToken is InputTransferToken
            ) {
                SurfaceControlViewHost(themedContext, virtualDisplay, inputToken)
            } else {
                SurfaceControlViewHost(themedContext, virtualDisplay, inputToken as? IBinder)
            }
        host.setView(view, _pixelDimensions.width, _pixelDimensions.height)

        val surfacePackage = host.surfacePackage
        if (
            surfacePackage == null ||
                !nativeWrapper.setAndroidViewPanelSurfacePackage(entityHandle, surfacePackage)
        ) {
            Log.e(TAG, "setAndroidViewPanelSurfacePackage failed for entity $entityHandle")
            host.release()
            return
        }
        viewHost = host

        // The parent and transform were recorded on the Kotlin entity before the surface existed,
        // so push them to the runtime now that it has something to render.
        OpenXrTransaction(nativeWrapper, nativeWrapper.createSceneTransaction()).use { tx ->
            if (tx.isAvailable) {
                val parentHandle = (parent as? OpenXrEntity)?.entityHandle
                if (parentHandle != null && parentHandle != INVALID_HANDLE) {
                    tx.setParent(entityHandle, parentHandle)
                }
                tx.setTransform(entityHandle, getPose(Space.PARENT), getScale(Space.PARENT))
                tx.commit()
            }
        }
    }

    /**
     * Reports [sizeInPixels] as the perceived resolution; the distance between the panel and the
     * render view is not taken into account for OpenXR panels.
     */
    override fun getPerceivedResolution(
        renderViewScenePose: ScenePose,
        renderViewFov: FieldOfView,
    ): PerceivedResolutionResult {
        return PerceivedResolutionResult.Success(_pixelDimensions)
    }

    /**
     * Pixel coordinates have their origin at the top-left corner of the panel with +Y pointing
     * down, whereas the local position is measured in meters from the center of the panel with +Y
     * pointing up: the X offset is shifted by half the width and the Y offset is flipped around
     * half the height.
     */
    override fun transformPixelCoordinatesToLocalPosition(coordinates: Vector2): Vector3 {
        val halfW = _pixelDimensions.width / (2 * VIRTUAL_PIXEL_DENSITY)
        val halfH = _pixelDimensions.height / (2 * VIRTUAL_PIXEL_DENSITY)
        val localX = (coordinates.x / VIRTUAL_PIXEL_DENSITY) - halfW
        val localY = halfH - (coordinates.y / VIRTUAL_PIXEL_DENSITY)
        return Vector3(localX, localY, 0f)
    }

    /**
     * Normalized coordinates are in [-1, 1] from the center of the panel with +Y pointing up, which
     * already matches the local position axes, so they are only scaled by half the panel size in
     * meters.
     */
    override fun transformNormalizedCoordinatesToLocalPosition(coordinates: Vector2): Vector3 {
        val halfW = _pixelDimensions.width / (2 * VIRTUAL_PIXEL_DENSITY)
        val halfH = _pixelDimensions.height / (2 * VIRTUAL_PIXEL_DENSITY)
        return Vector3(coordinates.x * halfW, coordinates.y * halfH, 0f)
    }

    override fun dispose() {
        viewHost?.let { host ->
            // SurfaceControlViewHost is bound to the thread that created it (the main thread, see
            // hostView), so release it there.
            if (Looper.myLooper() == Looper.getMainLooper()) {
                host.release()
            } else {
                Handler(Looper.getMainLooper()).post { host.release() }
            }
        }
        viewHost = null
        super.dispose()
    }
}
