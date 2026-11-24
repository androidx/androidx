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
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Matrix
import android.graphics.PointF
import android.view.Surface
import androidx.annotation.VisibleForTesting
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraState
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.SurfaceRequest.Result.RESULT_SURFACE_ALREADY_PROVIDED
import androidx.camera.core.SurfaceRequest.TransformationInfo as CXTransformationInfo
import androidx.camera.core.impl.CameraCaptureCallback
import androidx.camera.core.impl.CameraCaptureResult
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.CameraSessionLifecycleCallback
import androidx.camera.core.impl.utils.Threads
import androidx.camera.core.impl.utils.TransformUtils
import androidx.camera.core.streamsharing.VirtualCameraInfo
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.camera.viewfinder.compose.Viewfinder
import androidx.camera.viewfinder.core.FocusState
import androidx.camera.viewfinder.core.FocusStateValue
import androidx.camera.viewfinder.core.FrameRenderedListener
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.camera.viewfinder.core.TransformationInfo
import androidx.camera.viewfinder.core.TransformationMode
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest
import androidx.camera.viewfinder.core.ViewfinderSurfaceSessionScope
import androidx.camera.viewfinder.core.ZoomGestureDetector
import androidx.camera.viewfinder.core.ZoomGestureDetector.ZoomEvent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.concurrent.futures.await
import androidx.lifecycle.Observer
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

private const val SCREEN_FLASH_ANIMATION_DURATION_MILLIS = 1000

private val ImmediateMainExecutor = Executor(Threads::runOnMain)

/**
 * An adapter composable that displays frames from CameraX by completing provided [SurfaceRequest]s.
 *
 * This is a wrapper around [Viewfinder] that will convert a CameraX [SurfaceRequest] internally
 * into a [ViewfinderSurfaceRequest]. Additionally, all interactions normally handled through the
 * [ViewfinderSurfaceRequest] will be derived from the [SurfaceRequest].
 *
 * [CameraXViewfinder] always orients the preview upright relative to the display, regardless of the
 * target rotation set on [Preview]. To apply a custom UI rotation to the viewfinder, apply a
 * Compose rotation modifier (such as [androidx.compose.ui.graphics.graphicsLayer]) when using
 * [ImplementationMode.EMBEDDED].
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
 *   if Extensions are enabled on API level 33 and below, or on devices with other known
 *   compatibility issues (such as on API level 24 and below). Explicitly setting a mode will
 *   override this compatibility logic and may have performance or correctness implications on some
 *   devices. For example, explicitly setting [ImplementationMode.EXTERNAL] while Extensions are
 *   enabled on API level 33 and below will cause the camera session to fail to configure. On API
 *   levels below 29, [ImplementationMode.EMBEDDED] is always used for streams that the camera does
 *   not transform itself (e.g. when a [androidx.camera.core.CameraEffect] or stream sharing is
 *   active), since [ImplementationMode.EXTERNAL] cannot apply their remaining rotation on those API
 *   levels.
 * @param coordinateTransformer The [MutableCoordinateTransformer] used to map offsets of this
 *   viewfinder to the source coordinates of the data being provided to the surface that fulfills
 *   [surfaceRequest]. It is updated during layout, so in the frame the viewfinder is resized, e.g.
 *   when the display rotates, composition still sees the previous transform. Read it during layout,
 *   drawing or input handling, for example in a `Modifier.offset {}` lambda, rather than during
 *   composition.
 * @param alignment Optional alignment parameter used to place the camera feed in the given bounds
 *   of the [CameraXViewfinder]. Defaults to [Alignment.Center].
 * @param contentScale Optional scale parameter used to determine the aspect ratio scaling to be
 *   used to fit the camera feed in the bounds of the [CameraXViewfinder]. Defaults to
 *   [ContentScale.Crop].
 */
@Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
@Composable
public fun CameraXViewfinder(
    surfaceRequest: SurfaceRequest,
    modifier: Modifier = Modifier,
    implementationMode: ImplementationMode =
        CameraImplementationModeCompat.chooseCompatibleMode(surfaceRequest.camera),
    coordinateTransformer: MutableCoordinateTransformer? = null,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Crop,
) {
    CameraXViewfinder(
        surfaceRequest = surfaceRequest,
        modifier = modifier,
        implementationMode = implementationMode,
        coordinateTransformer = coordinateTransformer,
        alignment = alignment,
        contentScale = contentScale,
        onStreamStateChanged = {},
        isTapToFocusEnabled = false,
        onTapToFocus = { _, _ -> },
        autoCancelDurationMillis = 5000L,
        isPinchToZoomEnabled = false,
        onZoomRatioChanged = {},
        onScreenFlashReady = {},
    )
}

/**
 * An adapter composable that displays frames from CameraX by completing provided [SurfaceRequest]s.
 *
 * This is a wrapper around [Viewfinder] that will convert a CameraX [SurfaceRequest] internally
 * into a [ViewfinderSurfaceRequest]. Additionally, all interactions normally handled through the
 * [ViewfinderSurfaceRequest] will be derived from the [SurfaceRequest].
 *
 * [CameraXViewfinder] always orients the preview upright relative to the display, regardless of the
 * target rotation set on [Preview]. To apply a custom UI rotation to the viewfinder, apply a
 * Compose rotation modifier (such as [androidx.compose.ui.graphics.graphicsLayer]) when using
 * [ImplementationMode.EMBEDDED].
 *
 * If [implementationMode] is changed while the provided [surfaceRequest] has been fulfilled, the
 * surface request will be invalidated as if [SurfaceRequest.invalidate] has been called. This will
 * allow CameraX to know that a new surface request is required since the underlying viewfinder
 * implementation will be providing a new surface.
 *
 * This specific overload provides full control over advanced viewfinder features such as
 * pinch-to-zoom, tap-to-focus gestures, and screen flash.
 *
 * Example usage:
 *
 * @sample androidx.camera.compose.samples.CameraXViewfinderAdvancedSample
 * @param surfaceRequest The surface request from CameraX
 * @param modifier The [Modifier] to be applied to this viewfinder
 * @param implementationMode The [ImplementationMode] to be used by this viewfinder. By default,
 *   this is chosen automatically based on the device's capabilities. The default behavior prefers
 *   the higher-performance [ImplementationMode.EXTERNAL] mode, but will fall back to
 *   [ImplementationMode.EMBEDDED] if the camera hardware level is
 *   [LEGACY][android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY],
 *   if Extensions are enabled on API level 33 and below, or on devices with other known
 *   compatibility issues (such as on API level 24 and below). Explicitly setting a mode will
 *   override this compatibility logic and may have performance or correctness implications on some
 *   devices. For example, explicitly setting [ImplementationMode.EXTERNAL] while Extensions are
 *   enabled on API level 33 and below will cause the camera session to fail to configure. On API
 *   levels below 29, [ImplementationMode.EMBEDDED] is always used for streams that the camera does
 *   not transform itself (e.g. when a [androidx.camera.core.CameraEffect] or stream sharing is
 *   active), since [ImplementationMode.EXTERNAL] cannot apply their remaining rotation on those API
 *   levels.
 * @param coordinateTransformer The [MutableCoordinateTransformer] used to map offsets of this
 *   viewfinder to the source coordinates of the data being provided to the surface that fulfills
 *   [surfaceRequest]. It is updated during layout, so in the frame the viewfinder is resized, e.g.
 *   when the display rotates, composition still sees the previous transform. Read it during layout,
 *   drawing or input handling, for example in a `Modifier.offset {}` lambda, rather than during
 *   composition.
 * @param alignment Optional alignment parameter used to place the camera feed in the given bounds
 *   of the [CameraXViewfinder]. Defaults to [Alignment.Center].
 * @param contentScale Optional scale parameter used to determine the aspect ratio scaling to be
 *   used to fit the camera feed in the bounds of the [CameraXViewfinder]. Defaults to
 *   [ContentScale.Crop].
 * @param onStreamStateChanged Callback invoked when the preview stream state changes. Provides the
 *   current [Preview.StreamState].
 * @param isTapToFocusEnabled Whether the tap-to-focus gesture is enabled.
 * @param onTapToFocus A callback invoked when a tap-to-focus action is triggered. It provides the
 *   tap [Offset] and an integer representing the current focus state. See [FocusState] for possible
 *   values.
 * @param autoCancelDurationMillis The auto-cancel duration of focus/metering in milliseconds.
 *   Defaults to 5000L.
 * @param isPinchToZoomEnabled Whether the pinch-to-zoom gesture is enabled.
 * @param onZoomRatioChanged A callback invoked when the [CameraXViewfinder]'s pinch-to-zoom gesture
 *   scales the zoom ratio, providing the updated zoom ratio. This callback is only invoked during
 *   the active zooming state.
 * @param onScreenFlashReady A callback invoked when the screen flash feature is ready to apply,
 *   providing the [ImageCapture.ScreenFlash] implementation to be used with ImageCapture.
 * @param onRelease A callback invoked when the [CameraXViewfinder] is permanently removed from the
 *   composition, indicating that any references to resources (like [ImageCapture.ScreenFlash])
 *   should be cleared.
 */
@Composable
public fun CameraXViewfinder(
    surfaceRequest: SurfaceRequest,
    modifier: Modifier = Modifier,
    implementationMode: ImplementationMode =
        CameraImplementationModeCompat.chooseCompatibleMode(surfaceRequest.camera),
    coordinateTransformer: MutableCoordinateTransformer? = null,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Crop,
    onStreamStateChanged: (@Preview.StreamState Int) -> Unit = {},
    isTapToFocusEnabled: Boolean = false,
    onTapToFocus: (Offset, @FocusStateValue Int) -> Unit = { _, _ -> },
    autoCancelDurationMillis: Long = 5000L,
    isPinchToZoomEnabled: Boolean = false,
    onZoomRatioChanged: (Float) -> Unit = {},
    onScreenFlashReady: (ImageCapture.ScreenFlash) -> Unit = {},
    onRelease: () -> Unit = {},
) {
    val pendingScreenFlashListener = remember { mutableStateOf<ScreenFlashState?>(null) }
    val screenFlash = remember {
        object : ImageCapture.ScreenFlash {
            override fun apply(
                expirationTimeMillis: Long,
                listener: ImageCapture.ScreenFlashListener,
            ) {
                // TODO(b/355168952): Clarify expirationTimeMillis implementation mismatch with doc
                // description.
                pendingScreenFlashListener.value = ScreenFlashState(listener)
            }

            override fun clear() {
                pendingScreenFlashListener.value = null
            }
        }
    }

    val currentOnScreenFlashReady by rememberUpdatedState(onScreenFlashReady)
    val currentOnRelease = rememberUpdatedState(onRelease)
    DisposableEffect(screenFlash) {
        currentOnScreenFlashReady(screenFlash)
        onDispose { currentOnRelease.value() }
    }

    val currentImplementationMode by rememberUpdatedState(implementationMode)
    val currentOnStreamStateChanged = rememberUpdatedState(onStreamStateChanged)

    // Not keyed by the SurfaceRequest: the previous arguments keep the Viewfinder (and its Surface)
    // in place until the first TransformationInfo of a new SurfaceRequest arrives, so that the same
    // Surface can be provided to the new SurfaceRequest.
    var viewfinderArgs by remember { mutableStateOf<ViewfinderArgs?>(null) }
    var lastTransformationInfo by
        remember(surfaceRequest) { mutableStateOf<CXTransformationInfo?>(null) }
    var lockedImplementationMode by
        remember(surfaceRequest) { mutableStateOf<ImplementationMode?>(null) }
    var isInvalidated by remember(surfaceRequest) { mutableStateOf(false) }

    DisposableEffect(surfaceRequest) {
        // TransformationInfo is delivered on the main thread, but request cancellation can be
        // signaled from any thread. Run both on the main thread so this state is only accessed
        // there.
        var isDisposed = false
        var isCancelled = false
        // If a pending replacement SurfaceRequest is cancelled before producing ViewfinderArgs,
        // clear any previous SurfaceRequest's ViewfinderArgs so a stale Viewfinder is not stranded.
        // If the cancelled SurfaceRequest already owns viewfinderArgs, keep viewfinderArgs intact
        // so an incoming replacement SurfaceRequest with matching resolution can reuse its Surface.
        surfaceRequest.addRequestCancellationListener(ImmediateMainExecutor) {
            if (isDisposed) return@addRequestCancellationListener
            Snapshot.withMutableSnapshot {
                isCancelled = true
                if (viewfinderArgs?.surfaceRequest !== surfaceRequest) {
                    viewfinderArgs = null
                }
            }
        }
        surfaceRequest.setTransformationInfoListener(ImmediateMainExecutor) { transformInfo ->
            if (isDisposed || isCancelled || isInvalidated) return@setTransformationInfoListener
            val resolvedMode =
                CameraImplementationModeCompat.resolveImplementationMode(
                    currentImplementationMode,
                    transformInfo,
                )
            val lockedMode = lockedImplementationMode
            if (lockedMode != null && resolvedMode != lockedMode) {
                // ImplementationMode changed after being locked in; invalidate the SurfaceRequest
                // so a new SurfaceRequest will be sent.
                Snapshot.withMutableSnapshot {
                    lastTransformationInfo = transformInfo
                    isInvalidated = true
                }
                surfaceRequest.invalidate()
            } else {
                Snapshot.withMutableSnapshot {
                    lastTransformationInfo = transformInfo
                    lockedImplementationMode = resolvedMode
                    viewfinderArgs =
                        ViewfinderArgs(
                            surfaceRequest = surfaceRequest,
                            implementationMode = resolvedMode,
                            sensorToBufferTransform = Matrix(transformInfo.sensorToBufferTransform),
                            transformationInfo = transformInfo,
                        )
                }
            }
        }
        onDispose {
            isDisposed = true
            surfaceRequest.clearTransformationInfoListener()
        }
    }

    val currentLockedMode = lockedImplementationMode
    val currentTransformInfo = lastTransformationInfo
    if (
        !isInvalidated &&
            currentLockedMode != null &&
            currentTransformInfo != null &&
            CameraImplementationModeCompat.resolveImplementationMode(
                implementationMode,
                currentTransformInfo,
            ) != currentLockedMode
    ) {
        DisposableEffect(surfaceRequest, implementationMode) {
            if (!isInvalidated) {
                isInvalidated = true
                surfaceRequest.invalidate()
            }
            onDispose {}
        }
    }

    viewfinderArgs?.let { args ->
        val currentArgs by rememberUpdatedState(args)
        // When the display rotates, the TransformationInfo for the new rotation can arrive after
        // the viewfinder is laid out in the new orientation. Derive the source rotation from the
        // display rotation during composition so that it is consistent with the layout of the same
        // frame. See toViewfinderTransformationInfo().
        val hasCameraTransform = args.transformationInfo.hasCameraTransform()
        val displayRotation =
            rememberCameraDisplayRotation(
                implementationMode = args.implementationMode,
                transformationMode =
                    if (hasCameraTransform) {
                        TransformationMode.DEFERRED
                    } else {
                        TransformationMode.PRE_APPLIED
                    },
            )
        val transformationInfo =
            remember(args, displayRotation) {
                args.transformationInfo.toViewfinderTransformationInfo(
                    cameraInfo = args.surfaceRequest.camera.cameraInfoInternal,
                    displayRotation = displayRotation,
                    sensorToBufferTransform = args.sensorToBufferTransform,
                )
            }
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

        val currentOnTapToFocus by rememberUpdatedState(onTapToFocus)

        val internalCoordinateTransformer =
            coordinateTransformer ?: remember { MutableCoordinateTransformer() }

        val passthroughMeteringPointFactory = remember {
            object : MeteringPointFactory() {
                override fun convertPoint(x: Float, y: Float): PointF {
                    return PointF(x, y)
                }
            }
        }

        val gestureModifier =
            Modifier.tapToFocusGesture(
                    camera = args.surfaceRequest.camera,
                    isTapToFocusEnabled = isTapToFocusEnabled,
                    autoCancelDurationMillis = autoCancelDurationMillis,
                    sensorToBufferTransform = args.sensorToBufferTransform,
                    coordinateTransformer = internalCoordinateTransformer,
                    meteringPointFactory = passthroughMeteringPointFactory,
                    onTapToFocus = currentOnTapToFocus,
                )
                .pinchToZoomGesture(
                    camera = args.surfaceRequest.camera,
                    isPinchToZoomEnabled = isPinchToZoomEnabled,
                    onZoomRatioChanged = onZoomRatioChanged,
                )

        surfaceRequestScope?.let { scope ->
            DisposableEffect(scope) { onDispose { scope.complete() } }
            Box(modifier = modifier) {
                Viewfinder(
                    surfaceRequest = scope.viewfinderSurfaceRequest,
                    transformationInfo = transformationInfo,
                    modifier = Modifier.fillMaxSize().then(gestureModifier),
                    coordinateTransformer = internalCoordinateTransformer,
                    alignment = alignment,
                    contentScale = contentScale,
                ) {
                    onSurfaceSession {
                        with(scope) {
                            for (surfaceRequest in requestChannel) {
                                val cameraInfo =
                                    surfaceRequest.camera.cameraInfo as? CameraInfoInternal
                                val monitorJob =
                                    if (cameraInfo != null) {
                                        launch {
                                            monitorStreamState(
                                                surfaceRequest,
                                                cameraInfo,
                                                currentArgs.implementationMode,
                                                this@onSurfaceSession,
                                                currentOnStreamStateChanged,
                                            )
                                        }
                                    } else {
                                        currentOnStreamStateChanged.value(Preview.STREAM_STATE_IDLE)
                                        null
                                    }
                                // Since we provide the surface in a NonCancellable context, we want
                                // to add a job outside that context to check if the surface is
                                // being replaced.
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
                                    // cancellation watcher job and monitor job so the coroutine can
                                    // complete.
                                    cancellationWatcherJob.cancelAndJoin()
                                    monitorJob?.cancelAndJoin()

                                    when (result.resultCode) {
                                        // If the surface request is already fulfilled, we need to
                                        // invalidate it so that a new surface request will be
                                        // produced
                                        RESULT_SURFACE_ALREADY_PROVIDED ->
                                            surfaceRequest.invalidate()
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
                ScreenFlashOverlay(pendingScreenFlashListener = pendingScreenFlashListener)
            }
        }
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

private suspend fun monitorStreamState(
    surfaceRequest: SurfaceRequest,
    cameraInfo: CameraInfoInternal,
    implementationMode: ImplementationMode,
    sessionScope: ViewfinderSurfaceSessionScope,
    currentOnStreamStateChanged: State<(@Preview.StreamState Int) -> Unit>,
) {
    val isRequestCancelled = AtomicBoolean(false)

    // Ensure the stream state is IDLE when the session starts.
    withContext(Dispatchers.Main.immediate) {
        currentOnStreamStateChanged.value(Preview.STREAM_STATE_IDLE)
    }

    coroutineScope {
        val outerScope = this
        surfaceRequest.addRequestCancellationListener(Runnable::run) {
            isRequestCancelled.set(true)
            outerScope.cancel()
        }

        // Loop to monitor stream state transitions across session restarts (e.g., during zoom)
        // where the SurfaceRequest remains valid but the camera pipeline reconfigures.
        while (isActive) {
            try {
                // Signal 1 (Capture Start): The first onCaptureCompleted event
                waitForNextFrame(cameraInfo)

                if (implementationMode != ImplementationMode.EXTERNAL) {
                    // Signal 2 (Internal Surface Update API): wait for surface update
                    sessionScope.waitForSurfaceUpdate()
                }

                withContext(Dispatchers.Main.immediate) {
                    currentOnStreamStateChanged.value(Preview.STREAM_STATE_STREAMING)
                }

                // Wait for the session to close or the camera to be closed.
                cameraInfo.waitForSessionClosure()
            } catch (e: CancellationException) {
                if (isRequestCancelled.get() || !isActive) {
                    throw e
                }
                // Otherwise it was session closure, loop back to wait for next session
            } finally {
                // Handle both session-to-session transitions and final cleanup when cancelled.
                withContext(Dispatchers.Main.immediate + NonCancellable) {
                    currentOnStreamStateChanged.value(Preview.STREAM_STATE_IDLE)
                }
            }
        }
    }
}

private suspend fun ViewfinderSurfaceSessionScope.waitForSurfaceUpdate() {
    suspendCancellableCoroutine<Unit> { continuation ->
        val listener = FrameRenderedListener { _ ->
            if (continuation.isActive) continuation.resume(Unit)
        }
        addFrameRenderedListener(Runnable::run, listener)
        continuation.invokeOnCancellation { removeFrameRenderedListener(listener) }
    }
}

private suspend fun CameraInfoInternal.waitForSessionClosure() {
    coroutineScope {
        val innerScope = this
        val observer =
            Observer<CameraState> { state ->
                if (state.type != CameraState.Type.OPEN && state.type != CameraState.Type.OPENING) {
                    innerScope.cancel()
                }
            }

        val sessionLifecycleCallback =
            object : CameraSessionLifecycleCallback() {
                override fun onSessionStopped() {
                    innerScope.cancel()
                }
            }

        withContext(Dispatchers.Main.immediate) {
            cameraState.observeForever(observer)
            addSessionLifecycleCallback(Runnable::run, sessionLifecycleCallback)
        }

        try {
            awaitCancellation()
        } finally {
            withContext(Dispatchers.Main.immediate + NonCancellable) {
                cameraState.removeObserver(observer)
                removeSessionLifecycleCallback(sessionLifecycleCallback)
            }
        }
    }
}

private suspend fun waitForNextFrame(cameraInfo: CameraInfoInternal) {
    suspendCancellableCoroutine<Unit> { continuation ->
        val callback =
            object : CameraCaptureCallback() {
                override fun onCaptureCompleted(captureConfigId: Int, result: CameraCaptureResult) {
                    cameraInfo.removeSessionCaptureCallback(this)
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }
        continuation.invokeOnCancellation { cameraInfo.removeSessionCaptureCallback(callback) }
        cameraInfo.addSessionCaptureCallback(Runnable::run, callback)
    }
}

@Immutable
private data class ViewfinderArgs(
    val surfaceRequest: SurfaceRequest,
    val implementationMode: ImplementationMode,
    /** Defensive copy of the sensor-to-buffer transform for coordinate mapping and rotation. */
    val sensorToBufferTransform: Matrix,
    val transformationInfo: CXTransformationInfo,
)

/**
 * Converts the CameraX [SurfaceRequest.TransformationInfo] to a viewfinder [TransformationInfo]
 * whose source rotation orients the stream upright relative to [displayRotation]: from the sensor
 * orientation for streams with the camera transform, and from [sensorToBufferTransform] (see
 * [preAppliedSourceRotation]) otherwise.
 */
@VisibleForTesting
internal fun CXTransformationInfo.toViewfinderTransformationInfo(
    cameraInfo: CameraInfoInternal,
    displayRotation: Int,
    sensorToBufferTransform: Matrix = this.sensorToBufferTransform,
): TransformationInfo =
    TransformationInfo(
        sourceRotation =
            if (hasCameraTransform()) {
                cameraInfo.getSensorRotationDegrees(displayRotation)
            } else {
                preAppliedSourceRotation(sensorToBufferTransform, cameraInfo, displayRotation)
            },
        isSourceMirroredHorizontally = isMirroring,
        isSourceMirroredVertically = false,
        cropRectLeft = cropRect.left.toFloat(),
        cropRectTop = cropRect.top.toFloat(),
        cropRectRight = cropRect.right.toFloat(),
        cropRectBottom = cropRect.bottom.toFloat(),
        transformationMode =
            if (hasCameraTransform()) {
                TransformationMode.DEFERRED
            } else {
                TransformationMode.PRE_APPLIED
            },
    )

/**
 * Returns the remaining clockwise source rotation of a [TransformationMode.PRE_APPLIED] stream for
 * [displayRotation], derived statelessly from [sensorToBufferTransform] and [cameraInfo].
 *
 * When CameraX copies a stream (via stream sharing, a [androidx.camera.core.CameraEffect], or
 * both), [sensorToBufferTransform] records the total rotation and horizontal mirroring already
 * baked into the buffer relative to the physical camera sensor. Subtracting that baked rotation
 * from the sensor rotation for [displayRotation] (and negating when the buffer is mirrored) yields
 * the exact remaining rotation to orient the preview upright on the display.
 *
 * @param sensorToBufferTransform The transform from sensor coordinates to buffer coordinates.
 * @param cameraInfo The camera reported on the [SurfaceRequest].
 * @param displayRotation The display rotation to calculate the source rotation for.
 * @return The clockwise rotation in degrees in `[0, 360)`.
 */
@VisibleForTesting
internal fun preAppliedSourceRotation(
    sensorToBufferTransform: Matrix,
    cameraInfo: CameraInfoInternal,
    displayRotation: Int,
): Int {
    val isMirrored = TransformUtils.isMirrored(sensorToBufferTransform)
    val unmirrored =
        if (isMirrored) {
            Matrix(sensorToBufferTransform).apply { postScale(-1f, 1f) }
        } else {
            sensorToBufferTransform
        }
    val bakedRotation = TransformUtils.getRotationDegrees(unmirrored)
    val sensorRotation =
        cameraInfo.physicalOrParentCameraInfo().getSensorRotationDegrees(displayRotation)
    val remaining = sensorRotation - bakedRotation
    return TransformUtils.within360(if (isMirrored) -remaining else remaining)
}

private fun CameraInfoInternal.physicalOrParentCameraInfo(): CameraInfo =
    if (this !is VirtualCameraInfo) {
        this
    } else {
        cameraSelector.physicalCameraId?.let { id ->
            implementation.physicalCameraInfos.firstOrNull {
                it.cameraSelector.physicalCameraId == id
            }
        } ?: implementation
    }

@SuppressLint("RestrictedApiAndroidX")
@Composable
private fun Modifier.tapToFocusGesture(
    camera: Camera,
    isTapToFocusEnabled: Boolean,
    autoCancelDurationMillis: Long,
    sensorToBufferTransform: Matrix?,
    coordinateTransformer: MutableCoordinateTransformer,
    meteringPointFactory: MeteringPointFactory,
    onTapToFocus: (Offset, @FocusStateValue Int) -> Unit,
): Modifier {
    if (!isTapToFocusEnabled) return this
    val coroutineScope = rememberCoroutineScope()
    val currentSensorToBufferTransform by rememberUpdatedState(sensorToBufferTransform)
    val currentCoordinateTransformer by rememberUpdatedState(coordinateTransformer)
    val currentOnTapToFocus by rememberUpdatedState(onTapToFocus)
    val currentAutoCancelDurationMillis by rememberUpdatedState(autoCancelDurationMillis)

    return this.pointerInput(camera) {
        detectTapGestures { localOffset ->
            val sensorMatrix = currentSensorToBufferTransform ?: return@detectTapGestures
            val sensorRect =
                (camera.cameraInfo as? CameraInfoInternal)?.sensorRect ?: return@detectTapGestures

            coroutineScope.launch {
                val surfaceOffset = with(currentCoordinateTransformer) { localOffset.transform() }

                val bufferToSensor = Matrix().apply { sensorMatrix.invert(this) }

                val sensorPoint = floatArrayOf(surfaceOffset.x, surfaceOffset.y)
                bufferToSensor.mapPoints(sensorPoint)

                val normalizedX = sensorPoint[0] / sensorRect.width().toFloat()
                val normalizedY = sensorPoint[1] / sensorRect.height().toFloat()

                if (normalizedX !in 0f..1f || normalizedY !in 0f..1f) return@launch

                val meteringPoint = meteringPointFactory.createPoint(normalizedX, normalizedY)
                val focusMeteringAction =
                    FocusMeteringAction.Builder(meteringPoint)
                        .setAutoCancelDuration(
                            currentAutoCancelDurationMillis,
                            TimeUnit.MILLISECONDS,
                        )
                        .build()

                fun notifyTapToFocus(@FocusStateValue status: Int) {
                    currentOnTapToFocus(localOffset, status)
                }

                notifyTapToFocus(FocusState.STARTED)

                try {
                    val result =
                        camera.cameraControl.startFocusAndMetering(focusMeteringAction).await()
                    if (result.isFocusSuccessful) {
                        notifyTapToFocus(FocusState.FOCUSED)
                    } else {
                        notifyTapToFocus(FocusState.NOT_FOCUSED)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (e !is CameraControl.OperationCanceledException) {
                        notifyTapToFocus(FocusState.FAILED)
                    }
                }
            }
        }
    }
}

@Composable
private fun Modifier.pinchToZoomGesture(
    camera: Camera,
    isPinchToZoomEnabled: Boolean,
    onZoomRatioChanged: (Float) -> Unit,
): Modifier {
    if (!isPinchToZoomEnabled) return this
    val context = LocalContext.current
    val isZooming = remember { mutableStateOf(false) }
    val currentOnZoomRatioChanged by rememberUpdatedState(onZoomRatioChanged)

    var currentRatio by remember {
        mutableFloatStateOf(camera.cameraInfo.zoomState.value?.zoomRatio ?: 1f)
    }
    val zoomGestureDetector =
        remember(camera, context) {
            ZoomGestureDetector(context) { zoomEvent ->
                val zoomState = camera.cameraInfo.zoomState.value
                if (zoomState != null) {
                    when (zoomEvent) {
                        is ZoomEvent.Begin -> {
                            isZooming.value = true
                            currentRatio = zoomState.zoomRatio
                        }
                        is ZoomEvent.Move -> {
                            val incremental = zoomEvent.incrementalScaleFactor
                            val speedUp = speedUpZoomBy2X(incremental)
                            val targetRatio =
                                (currentRatio * speedUp).coerceIn(
                                    zoomState.minZoomRatio,
                                    zoomState.maxZoomRatio,
                                )
                            currentRatio = targetRatio
                            camera.cameraControl.setZoomRatio(targetRatio)
                            currentOnZoomRatioChanged(targetRatio)
                        }
                        is ZoomEvent.End -> {
                            isZooming.value = false
                        }
                    }
                }
                true
            }
        }

    return this.pointerInput(zoomGestureDetector) {
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
}

private fun speedUpZoomBy2X(scaleFactor: Float): Float {
    return (2 * scaleFactor - 1.0f).coerceAtLeast(0.01f)
}

@Composable
private fun ScreenFlashOverlay(pendingScreenFlashListener: State<ScreenFlashState?>) {
    val context = LocalContext.current

    val flashListenerState = pendingScreenFlashListener.value
    val isScreenFlashActive = flashListenerState != null

    DisposableEffect(isScreenFlashActive, context) {
        val activity = context.findActivity()
        val targetWindow = activity?.window
        val isActivityValid = activity == null || (!activity.isFinishing && !activity.isDestroyed)
        if (isScreenFlashActive && targetWindow != null && isActivityValid) {
            val params = targetWindow.attributes
            val originalBrightness = params.screenBrightness
            params.screenBrightness = 1.0f
            targetWindow.attributes = params
            onDispose {
                val isActivityStillValid = !activity.isFinishing && !activity.isDestroyed
                if (isActivityStillValid) {
                    val currentParams = targetWindow.attributes
                    currentParams.screenBrightness = originalBrightness
                    targetWindow.attributes = currentParams
                }
            }
        } else {
            onDispose {}
        }
    }

    val alphaAnimatable = remember { Animatable(0f) }
    LaunchedEffect(flashListenerState) {
        if (flashListenerState != null) {
            try {
                alphaAnimatable.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = SCREEN_FLASH_ANIMATION_DURATION_MILLIS),
                )
            } finally {
                flashListenerState.listener.onCompleted()
            }
        } else {
            alphaAnimatable.animateTo(0f)
        }
    }
    val alpha = alphaAnimatable.value

    if (alpha > 0f) {
        // TODO(b/355168952): Allow apps to configure the screen flash background
        // overlay color.
        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = alpha)))
    }
}

private data class ScreenFlashState(val listener: ImageCapture.ScreenFlashListener)

private fun Context.findActivity(): Activity? {
    var innerContext = this
    while (innerContext is ContextWrapper) {
        if (innerContext is Activity) {
            return innerContext
        }
        innerContext = innerContext.baseContext
    }
    return null
}
