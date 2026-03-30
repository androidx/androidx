/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.compose

import android.annotation.SuppressLint
import android.graphics.PointF
import android.view.Surface
import androidx.annotation.RestrictTo
import androidx.camera.core.CameraControl
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.SurfaceRequest.Result.RESULT_SURFACE_ALREADY_PROVIDED
import androidx.camera.core.SurfaceRequest.TransformationInfo as CXTransformationInfo
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.camera.viewfinder.compose.Viewfinder
import androidx.camera.viewfinder.core.FocusMeteringState
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.camera.viewfinder.core.TransformationInfo
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest
import androidx.camera.viewfinder.core.ZoomGestureDetector
import androidx.camera.viewfinder.core.ZoomGestureDetector.ZoomEvent
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * An adapter composable that displays frames from CameraX by completing provided [SurfaceRequest]s.
 *
 * This is a wrapper around [Viewfinder] that will convert a CameraX [SurfaceRequest] internally
 * into a [ViewfinderSurfaceRequest]. Additionally, all interactions normally handled through the
 * [ViewfinderSurfaceRequest] will be derived from the [SurfaceRequest].
 *
 * If [implementationMode] is changed while the provided [surfaceRequest] has been fulfilled, the
 * surface request will be invalidated as if [SurfaceRequest.invalidate] has been called. This will
 * allow CameraX to know that a new surface request is required since the underlying viewfinder
 * implementation will be providing a new surface.
 *
 * Example usage:
 *
 * @sample androidx.camera.compose.samples.CameraXViewfinderSample
 * @param surfaceRequest The surface request from CameraX
 * @param modifier The [Modifier] to be applied to this viewfinder
 * @param implementationMode The [ImplementationMode] to be used by this viewfinder. By default,
 *   this is chosen automatically based on the device's capabilities. The default behavior prefers
 *   the higher-performance [ImplementationMode.EXTERNAL] mode, but will fall back to
 *   [ImplementationMode.EMBEDDED] if the camera hardware level is
 *   [LEGACY][android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY],
 *   or on devices with other known compatibility issues (such as on API level 24 and below).
 *   Explicitly setting a mode will override this compatibility logic and may have performance or
 *   correctness implications on some devices.
 * @param coordinateTransformer The [MutableCoordinateTransformer] used to map offsets of this
 *   viewfinder to the source coordinates of the data being provided to the surface that fulfills
 *   [surfaceRequest]
 * @param alignment Optional alignment parameter used to place the camera feed in the given bounds
 *   of the [CameraXViewfinder]. Defaults to [Alignment.Center].
 * @param contentScale Optional scale parameter used to determine the aspect ratio scaling to be
 *   used to fit the camera feed in the bounds of the [CameraXViewfinder]. Defaults to
 *   [ContentScale.Crop].
 */
@SuppressLint("RestrictedApiAndroidX")
@Composable
public fun CameraXViewfinder(
    surfaceRequest: SurfaceRequest,
    modifier: Modifier = Modifier,
    implementationMode: ImplementationMode =
        CameraImplementationModeCompat.chooseCompatibleMode(surfaceRequest.camera.cameraInfo),
    coordinateTransformer: MutableCoordinateTransformer? = null,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val currentImplementationMode by rememberUpdatedState(implementationMode)

    val viewfinderArgs by
        produceState<ViewfinderArgs?>(initialValue = null, surfaceRequest) {
            // Cancel this produceScope in case we haven't yet produced a complete
            // ViewfinderArgs.
            surfaceRequest.addRequestCancellationListener(Runnable::run) {
                this@produceState.cancel()
            }

            // Convert the CameraX TransformationInfo callback into a StateFlow
            val transformationInfoFlow: StateFlow<CXTransformationInfo?> =
                MutableStateFlow<CXTransformationInfo?>(null)
                    .also { stateFlow ->
                        // Set a callback to update this state flow
                        surfaceRequest.setTransformationInfoListener(Runnable::run) { transformInfo
                            ->
                            // Set the next value of the flow
                            stateFlow.value = transformInfo
                        }
                    }
                    .asStateFlow()

            // The ImplementationMode that will be used for all TransformationInfo updates.
            // This is locked in once we have updated ViewfinderArgs and won't change until
            // this produceState block is cancelled and restarted.
            var snapshotImplementationMode: ImplementationMode? = null
            snapshotFlow { currentImplementationMode }
                .combine(transformationInfoFlow.filterNotNull()) { implMode, transformInfo ->
                    Pair(implMode, transformInfo)
                }
                .takeWhile { (implMode, _) ->
                    val shouldAbort =
                        snapshotImplementationMode != null && implMode != snapshotImplementationMode
                    if (shouldAbort) {
                        // Abort flow and invalidate SurfaceRequest so a new SurfaceRequest will
                        // be sent.
                        surfaceRequest.invalidate()
                    } else {
                        // Got the first ImplementationMode. This will be used until this
                        // produceState is cancelled.
                        snapshotImplementationMode = implMode
                    }
                    !shouldAbort
                }
                .collect { (implMode, transformInfo) ->
                    value =
                        ViewfinderArgs(
                            surfaceRequest,
                            implMode,
                            TransformationInfo(
                                sourceRotation = transformInfo.rotationDegrees,
                                isSourceMirroredHorizontally = transformInfo.isMirroring,
                                isSourceMirroredVertically = false,
                                cropRectLeft = transformInfo.cropRect.left.toFloat(),
                                cropRectTop = transformInfo.cropRect.top.toFloat(),
                                cropRectRight = transformInfo.cropRect.right.toFloat(),
                                cropRectBottom = transformInfo.cropRect.bottom.toFloat(),
                            ),
                        )
                }
        }

    viewfinderArgs?.let { args ->
        val currentArgs by rememberUpdatedState(args)
        val surfaceRequestScope by
            produceState<SurfaceRequestScope?>(null) {
                snapshotFlow { Pair(currentArgs.surfaceRequest, currentArgs.implementationMode) }
                    .collectLatest { (surfaceRequest, implementationMode) ->
                        if (!value.canSupport(surfaceRequest, implementationMode)) {
                            // Create a new session if the new surface request and implementation
                            // mode
                            // do not match the current session.
                            value =
                                SurfaceRequestScope.createFrom(surfaceRequest, implementationMode)
                        }

                        // Send along the surface requests until one completes or a request is
                        // cancelled.
                        // We want to continue to use the same Surface until it is sent to a
                        // SurfaceRequest so we don't unnecessarily recreate the underlying
                        // SurfaceView or TextureView.
                        try {
                            val channel =
                                checkNotNull(value?.requestChannel) {
                                    "Surface request channel should not be null"
                                }
                            channel.send(surfaceRequest)
                        } catch (_: ClosedSendChannelException) {
                            // Channel was closed. The SurfaceRequest will have
                            // willNotProvideSurface()
                            // called on it by the channel's onUndeliveredElement callback.
                        }
                    }
            }

        surfaceRequestScope?.let { scope ->
            DisposableEffect(scope) { onDispose { scope.complete() } }
            Viewfinder(
                surfaceRequest = scope.viewfinderSurfaceRequest,
                transformationInfo = args.transformationInfo,
                modifier = modifier.fillMaxSize(),
                coordinateTransformer = coordinateTransformer,
                alignment = alignment,
                contentScale = contentScale,
            ) {
                onSurfaceSession {
                    with(scope) {
                        for (surfaceRequest in requestChannel) {
                            // Since we provide the surface in a NonCancellable context, we want
                            // to add a job outside that context to check if the surface is being
                            // replaced.
                            val cancellationWatcherJob = launch {
                                try {
                                    awaitCancellation()
                                } catch (e: CancellationException) {
                                    if (e.message?.contains("Surface replaced") == true) {
                                        surfaceRequest.invalidate()
                                    }
                                }
                            }

                            // If we're providing a surface, we must wait for the source to be
                            // finished with the surface before we allow the surface session to
                            // complete, so always run inside a non-cancellable context
                            withContext(NonCancellable) {
                                val result =
                                    surfaceRequest.provideSurfaceAndWaitForCompletion(surface)

                                // Now that we're done with the Surface, we need to cancel the
                                // cancellation watcher job so the coroutine can complete.
                                cancellationWatcherJob.cancel()

                                when (result.resultCode) {
                                    // If the surface request is already fulfilled, we need to
                                    // invalidate it so that a new surface request will be produced
                                    RESULT_SURFACE_ALREADY_PROVIDED -> surfaceRequest.invalidate()
                                    else -> {
                                        // The surface is no longer in use. It can be reused for
                                        // any future requests.
                                    }
                                }
                            }

                            if (!isActive) {
                                // If the coroutine is no longer active, break out of the loop
                                // before we try to dequeue another SurfaceRequest. If
                                // onSurfaceSession is simply called again with a new
                                // ViewfinderSurfaceSessionScope, we could potentially use the
                                // SurfaceRequest currently enqueued in the Channel.
                                break
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * An adapter composable that displays frames from CameraX by completing provided [SurfaceRequest]s.
 *
 * This is a wrapper around [Viewfinder] that will convert a CameraX [SurfaceRequest] internally
 * into a [ViewfinderSurfaceRequest]. Additionally, all interactions normally handled through the
 * [ViewfinderSurfaceRequest] will be derived from the [SurfaceRequest].
 *
 * If [implementationMode] is changed while the provided [surfaceRequest] has been fulfilled, the
 * surface request will be invalidated as if [SurfaceRequest.invalidate] has been called. This will
 * allow CameraX to know that a new surface request is required since the underlying viewfinder
 * implementation will be providing a new surface.
 *
 * Example usage:
 *
 * @sample androidx.camera.compose.samples.CameraXViewfinderSample
 * @param surfaceRequest The surface request from CameraX
 * @param state The [CameraXViewfinderState] to be used by this viewfinder.
 * @param modifier The [Modifier] to be applied to this viewfinder
 * @param onZoomRatioChanged A callback to be invoked when the zoom ratio changes.
 * @param onTapToFocus A callback to be invoked when a tap-to-focus gesture occurs. The callback
 *   receives a [ComposeTapToFocusInfo] containing the status and coordinate of the tap.
 * @param implementationMode The [ImplementationMode] to be used by this viewfinder. By default,
 *   this is chosen automatically based on the device's capabilities. The default behavior prefers
 *   the higher-performance [ImplementationMode.EXTERNAL] mode, but will fall back to
 *   [ImplementationMode.EMBEDDED] if the camera hardware level is
 *   [LEGACY][android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY],
 *   or on devices with other known compatibility issues (such as on API level 24 and below).
 *   Explicitly setting a mode will override this compatibility logic and may have performance or
 *   correctness implications on some devices.
 * @param coordinateTransformer The [MutableCoordinateTransformer] used to map offsets of this
 *   viewfinder to the source coordinates of the data being provided to the surface that fulfills
 *   [surfaceRequest]
 * @param alignment Optional alignment parameter used to place the camera feed in the given bounds
 *   of the [CameraXViewfinder]. Defaults to [Alignment.Center].
 * @param contentScale Optional scale parameter used to determine the aspect ratio scaling to be
 *   used to fit the camera feed in the bounds of the [CameraXViewfinder]. Defaults to
 *   [ContentScale.Crop].
 */
@SuppressLint("RestrictedApiAndroidX")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun CameraXViewfinder(
    surfaceRequest: SurfaceRequest,
    state: CameraXViewfinderState,
    modifier: Modifier = Modifier,
    onZoomRatioChanged: (Float) -> Unit = {},
    onTapToFocus: (ComposeTapToFocusInfo) -> Unit = {},
    implementationMode: ImplementationMode =
        CameraImplementationModeCompat.chooseCompatibleMode(surfaceRequest.camera.cameraInfo),
    coordinateTransformer: MutableCoordinateTransformer? = null,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val camera = remember(surfaceRequest) { surfaceRequest.camera }
    val currentOnZoomRatioChanged by rememberUpdatedState(onZoomRatioChanged)
    val currentOnTapToFocus by rememberUpdatedState(onTapToFocus)

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val effectiveCoordinateTransformer =
        coordinateTransformer ?: remember { MutableCoordinateTransformer() }

    val context = LocalContext.current
    val isZooming = remember { mutableStateOf(false) }
    val zoomGestureDetector =
        remember(camera) {
            ZoomGestureDetector(context) { zoomEvent ->
                when (zoomEvent) {
                    is ZoomEvent.Move -> {
                        val zoomState =
                            camera.cameraInfo.zoomState.value ?: return@ZoomGestureDetector true
                        val incremental = zoomEvent.incrementalScaleFactor
                        val speedUp = speedUpZoomBy2X(incremental)
                        val currentCameraRatio = zoomState.zoomRatio
                        val targetRatio = currentCameraRatio * speedUp
                        val minRatio = zoomState.minZoomRatio
                        val maxRatio = zoomState.maxZoomRatio
                        val clampedRatio = min(max(targetRatio, minRatio), maxRatio)
                        camera.cameraControl.setZoomRatio(clampedRatio)
                        currentOnZoomRatioChanged.invoke(clampedRatio)
                    }
                    is ZoomEvent.Begin -> {
                        isZooming.value = true
                    }
                    is ZoomEvent.End -> {
                        isZooming.value = false
                    }
                }
                true
            }
        }

    val meteringPointFactory =
        remember(surfaceRequest) {
            SurfaceOrientedMeteringPointFactory(
                surfaceRequest.resolution.width.toFloat(),
                surfaceRequest.resolution.height.toFloat(),
            )
        }

    val pinchToZoomEnabled = state.isPinchToZoomEnabled
    val tapToFocusEnabled = state.isTapToFocusEnabled

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .pointerInput(camera, tapToFocusEnabled) {
                    if (!tapToFocusEnabled) return@pointerInput
                    detectTapGestures { localOffset ->
                        coroutineScope.launch {
                            val surfaceCoords =
                                effectiveCoordinateTransformer.transformMatrix.map(localOffset)

                            val focusMeteringAction =
                                createFocusMeteringAction(
                                    meteringPointFactory,
                                    PointF(surfaceCoords.x, surfaceCoords.y),
                                    state.tapToFocusAutoCancelDurationMillis,
                                )

                            fun notifyTapToFocus(status: Int) {
                                currentOnTapToFocus.invoke(
                                    ComposeTapToFocusInfo(
                                        status = status,
                                        tapCoordinate = localOffset,
                                    )
                                )
                            }

                            notifyTapToFocus(FocusMeteringState.FOCUS_METERING_STARTED)

                            try {
                                val result =
                                    camera.cameraControl
                                        .startFocusAndMetering(focusMeteringAction)
                                        .await()
                                if (result.isFocusSuccessful) {
                                    notifyTapToFocus(FocusMeteringState.FOCUS_METERING_FOCUSED)
                                } else {
                                    notifyTapToFocus(FocusMeteringState.FOCUS_METERING_NOT_FOCUSED)
                                }
                            } catch (e: Exception) {
                                if (e !is CameraControl.OperationCanceledException) {
                                    notifyTapToFocus(FocusMeteringState.FOCUS_METERING_FAILED)
                                }
                            }
                        }
                    }
                }
                .pointerInput(zoomGestureDetector, pinchToZoomEnabled) {
                    if (!pinchToZoomEnabled) return@pointerInput
                    awaitEachGesture {
                        while (true) {
                            val event = awaitPointerEvent()
                            val motionEvent = event.motionEvent
                            if (motionEvent != null) {
                                zoomGestureDetector.onTouchEvent(motionEvent)
                                if (isZooming.value) {
                                    event.changes.fastForEach { it.consume() }
                                }
                            }
                            if (!event.changes.fastAny { it.pressed }) break
                        }
                    }
                }
    ) {
        CameraXViewfinder(
            surfaceRequest = surfaceRequest,
            modifier = Modifier.fillMaxSize(),
            implementationMode = implementationMode,
            coordinateTransformer = effectiveCoordinateTransformer,
            alignment = alignment,
            contentScale = contentScale,
        )
    }
}

@Stable
private class SurfaceRequestScope(val viewfinderSurfaceRequest: ViewfinderSurfaceRequest) {
    val requestChannel =
        Channel<SurfaceRequest>(Channel.RENDEZVOUS) {
            // If a surface hasn't yet been provided, this call will succeed. Otherwise
            // it will be a no-op.
            it.willNotProvideSurface()
        }

    suspend fun SurfaceRequest.provideSurfaceAndWaitForCompletion(
        surface: Surface
    ): SurfaceRequest.Result = suspendCancellableCoroutine { continuation ->
        provideSurface(surface, Runnable::run) { continuation.resume(it) }

        continuation.invokeOnCancellation {
            assert(false) {
                "provideSurfaceAndWaitForCompletion should always be called in a " +
                    "NonCancellable context to ensure the Surface is not closed before the " +
                    "frame source has finished using it."
            }
        }
    }

    fun complete() {
        // Ensure the surface session can exit the for-loop and finish
        requestChannel.close()
    }

    fun canSupport(surfaceRequest: SurfaceRequest, implementationMode: ImplementationMode) =
        viewfinderSurfaceRequest.width == surfaceRequest.resolution.width &&
            viewfinderSurfaceRequest.height == surfaceRequest.resolution.height &&
            viewfinderSurfaceRequest.implementationMode == implementationMode

    companion object {
        fun createFrom(surfaceRequest: SurfaceRequest, implementationMode: ImplementationMode) =
            SurfaceRequestScope(
                ViewfinderSurfaceRequest(
                    width = surfaceRequest.resolution.width,
                    height = surfaceRequest.resolution.height,
                    implementationMode = implementationMode,
                    requestId = "CXSurfaceRequest-${"%x".format(surfaceRequest.hashCode())}",
                )
            )
    }
}

private fun SurfaceRequestScope?.canSupport(
    surfaceRequest: SurfaceRequest,
    implementationMode: ImplementationMode,
) = this != null && canSupport(surfaceRequest, implementationMode)

@Immutable
private data class ViewfinderArgs(
    val surfaceRequest: SurfaceRequest,
    val implementationMode: ImplementationMode,
    val transformationInfo: TransformationInfo,
)

private fun speedUpZoomBy2X(scaleFactor: Float): Float {
    return if (scaleFactor > 1f) {
        1.0f + (scaleFactor - 1.0f) * 2
    } else {
        1.0f - (1.0f - scaleFactor) * 2
    }
}

private fun createFocusMeteringAction(
    meteringPointFactory: MeteringPointFactory,
    tapPoint: PointF,
    autoCancelDurationMillis: Long,
): FocusMeteringAction {
    return FocusMeteringAction.create(meteringPointFactory, tapPoint, autoCancelDurationMillis)
}
