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

package androidx.camera.viewfinder.compose

import android.annotation.SuppressLint
import android.graphics.RectF
import android.util.Size
import android.view.Surface
import androidx.camera.viewfinder.compose.internal.RefCounted
import androidx.camera.viewfinder.compose.internal.SurfaceTransformationUtil
import androidx.camera.viewfinder.compose.internal.TransformUtil.surfaceRotationToRotationDegrees
import androidx.camera.viewfinder.surface.ImplementationMode
import androidx.camera.viewfinder.surface.TransformationInfo
import androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest
import androidx.compose.foundation.AndroidEmbeddedExternalSurface
import androidx.compose.foundation.AndroidExternalSurface
import androidx.compose.foundation.AndroidExternalSurfaceScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.setFrom
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel

/**
 * Displays a media stream with the given transformations for crop and rotation while maintaining
 * proper scaling.
 *
 * Provides a [Surface] for the given [ViewfinderSurfaceRequest], surface can be accessed through
 * [ViewfinderSurfaceRequest.getSurface].
 *
 * This has two underlying implementations either using an [AndroidEmbeddedExternalSurface] for
 * [ImplementationMode.EMBEDDED] or an [AndroidExternalSurface] for [ImplementationMode.EXTERNAL].
 *
 * @param surfaceRequest Details about the surface being requested
 * @param implementationMode Determines the underlying implementation of the [Surface].
 * @param transformationInfo Specifies the required transformations for the media being displayed.
 * @param modifier Modifier to be applied to the [Viewfinder]
 * @param coordinateTransformer Coordinate transformer that can be used to convert Compose space
 *   coordinates such as touch coordinates to surface space coordinates. When the Viewfinder is
 *   displaying content from the camera, this transformer can be used to translate touch events into
 *   camera sensor coordinates for focus and metering actions.
 *
 * TODO(b/322420487): Add a sample with `@sample`
 */
@Composable
fun Viewfinder(
    surfaceRequest: ViewfinderSurfaceRequest,
    implementationMode: ImplementationMode,
    transformationInfo: TransformationInfo,
    modifier: Modifier = Modifier,
    coordinateTransformer: MutableCoordinateTransformer? = null,
) {
    val resolution = surfaceRequest.resolution

    Box(modifier = modifier.clipToBounds().fillMaxSize()) {
        key(surfaceRequest) {
            TransformedSurface(
                resolution = resolution,
                transformationInfo = transformationInfo,
                implementationMode = implementationMode,
                onInit = {
                    onSurface { newSurface, _, _ ->
                        val refCountedSurface = RefCounted<Surface> { it.release() }

                        refCountedSurface.initialize(newSurface)
                        newSurface.onDestroyed { refCountedSurface.release() }

                        refCountedSurface.acquire()?.let {
                            surfaceRequest.provideSurface(it, Runnable::run) {
                                refCountedSurface.release()
                                this@onSurface.cancel()
                            }
                            awaitCancellation()
                        } ?: run { this@onSurface.cancel() }
                    }
                    // TODO(b/322420176): Properly handle onSurfaceChanged()
                },
                coordinateTransformer,
            )
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun TransformedSurface(
    resolution: Size,
    transformationInfo: TransformationInfo,
    implementationMode: ImplementationMode,
    onInit: AndroidExternalSurfaceScope.() -> Unit,
    coordinateTransformer: MutableCoordinateTransformer?,
) {
    val surfaceModifier =
        Modifier.layout { measurable, constraints ->
            val placeable =
                measurable.measure(Constraints.fixed(resolution.width, resolution.height))

            // When the child placeable is larger than the parent's constraints, rather
            // than the child overflowing through the right or bottom of the parent, it overflows
            // evenly on all sides, as if it's placed exactly in the center of the parent.
            // To compensate for this, we must offset the child by the amount it overflows
            // so it is consistently placed in the top left corner of the parent before
            // we apply scaling and translation in the graphics layer.
            val widthOffset = 0.coerceAtLeast((placeable.width - constraints.maxWidth) / 2)
            val heightOffset = 0.coerceAtLeast((placeable.height - constraints.maxHeight) / 2)
            layout(placeable.width, placeable.height) {
                placeable.placeWithLayer(widthOffset, heightOffset) {
                    val surfaceToViewFinderMatrix =
                        SurfaceTransformationUtil.getTransformedSurfaceMatrix(
                            transformationInfo,
                            Size(constraints.maxWidth, constraints.maxHeight)
                        )

                    coordinateTransformer?.transformMatrix =
                        Matrix().apply {
                            setFrom(surfaceToViewFinderMatrix)
                            invert()
                        }

                    val surfaceRectInViewfinder =
                        RectF(0f, 0f, resolution.width.toFloat(), resolution.height.toFloat())
                    surfaceToViewFinderMatrix.mapRect(surfaceRectInViewfinder)

                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = surfaceRectInViewfinder.width() / resolution.width
                    scaleY = surfaceRectInViewfinder.height() / resolution.height

                    translationX = surfaceRectInViewfinder.left
                    translationY = surfaceRectInViewfinder.top
                }
            }
        }

    when (implementationMode) {
        ImplementationMode.EXTERNAL -> {
            AndroidExternalSurface(modifier = surfaceModifier, onInit = onInit)
        }
        ImplementationMode.EMBEDDED -> {
            val displayRotationDegrees =
                key(LocalConfiguration.current) {
                    surfaceRotationToRotationDegrees(LocalView.current.display.rotation)
                }

            // For TextureView, correct the orientation to match the display rotation.
            val correctionMatrix = remember { Matrix() }

            transformationInfo.let {
                correctionMatrix.setFrom(
                    SurfaceTransformationUtil.getTextureViewCorrectionMatrix(
                        displayRotationDegrees,
                        resolution
                    )
                )
            }

            AndroidEmbeddedExternalSurface(
                modifier = surfaceModifier,
                transform = correctionMatrix,
                onInit = onInit
            )
        }
    }
}
