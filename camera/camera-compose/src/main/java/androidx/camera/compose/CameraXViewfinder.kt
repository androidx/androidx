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

import androidx.camera.core.SurfaceRequest
import androidx.camera.core.SurfaceRequest.TransformationInfo as CXTransformationInfo
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.camera.viewfinder.compose.Viewfinder
import androidx.camera.viewfinder.surface.ImplementationMode
import androidx.camera.viewfinder.surface.TransformationInfo
import androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

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
 * @param implementationMode The [ImplementationMode] to be used by this viewfinder.
 * @param coordinateTransformer The [MutableCoordinateTransformer] used to map offsets of this
 *   viewfinder to the source coordinates of the data being provided to the surface that fulfills
 *   [surfaceRequest]
 */
@Composable
public fun CameraXViewfinder(
    surfaceRequest: SurfaceRequest,
    modifier: Modifier = Modifier,
    implementationMode: ImplementationMode = ImplementationMode.EXTERNAL,
    coordinateTransformer: MutableCoordinateTransformer? = null
) {
    val currentImplementationMode by rememberUpdatedState(implementationMode)

    val viewfinderArgs by
        produceState<ViewfinderArgs?>(initialValue = null, surfaceRequest) {
            // Convert the CameraX SurfaceRequest to ViewfinderSurfaceRequest. There should
            // always be a 1:1 mapping of CameraX SurfaceRequest to ViewfinderSurfaceRequest.
            val viewfinderSurfaceRequest =
                ViewfinderSurfaceRequest.Builder(surfaceRequest.resolution).build()

            // Launch undispatched so we always reach the try/finally in this coroutine
            launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    // Forward request cancellation to the ViewfinderSurfaceRequest by marking it
                    // safe to release and cancelling this produceScope in case we haven't yet
                    // produced a complete ViewfinderArgs.
                    surfaceRequest.addRequestCancellationListener(Runnable::run) {
                        // This SurfaceRequest doesn't need to be completed, so let the
                        // Viewfinder know in case it has already generated a Surface.
                        viewfinderSurfaceRequest.markSurfaceSafeToRelease()
                        // Also complete the ViewfinderSurfaceRequest from the producer side
                        // in case we never sent it to the Viewfinder.
                        viewfinderSurfaceRequest.willNotProvideSurface()
                        this@produceState.cancel()
                    }

                    // Suspend until we retrieve the Surface
                    val surface = viewfinderSurfaceRequest.getSurface()
                    // Provide the surface and mark safe to release once the
                    // frame producer is finished.
                    surfaceRequest.provideSurface(surface, Runnable::run) {
                        viewfinderSurfaceRequest.markSurfaceSafeToRelease()
                    }
                } finally {
                    // If we haven't provided the surface, such as if we're cancelled
                    // while suspending on getSurface(), this call will succeed. Otherwise
                    // it will be a no-op.
                    surfaceRequest.willNotProvideSurface()
                }
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
                            viewfinderSurfaceRequest,
                            implMode,
                            TransformationInfo(
                                sourceRotation = transformInfo.rotationDegrees,
                                isSourceMirroredHorizontally = transformInfo.isMirroring,
                                isSourceMirroredVertically = false,
                                cropRectLeft = transformInfo.cropRect.left,
                                cropRectTop = transformInfo.cropRect.top,
                                cropRectRight = transformInfo.cropRect.right,
                                cropRectBottom = transformInfo.cropRect.bottom
                            )
                        )
                }
        }

    viewfinderArgs?.let { args ->
        Viewfinder(
            surfaceRequest = args.viewfinderSurfaceRequest,
            implementationMode = args.implementationMode,
            transformationInfo = args.transformationInfo,
            modifier = modifier.fillMaxSize(),
            coordinateTransformer = coordinateTransformer
        )
    }
}

@Immutable
private data class ViewfinderArgs(
    val viewfinderSurfaceRequest: ViewfinderSurfaceRequest,
    val implementationMode: ImplementationMode,
    val transformationInfo: TransformationInfo
)
