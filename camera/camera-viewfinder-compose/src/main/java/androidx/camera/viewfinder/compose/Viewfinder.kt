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
import android.util.Size
import android.view.Surface
import androidx.camera.viewfinder.compose.internal.SurfaceTransformationUtil
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.setFrom
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Displays a media stream with the given transformations for crop and rotation while maintaining
 * proper scaling.
 *
 * Provides a [Surface] for the given [ViewfinderSurfaceRequest], surface can be accessed through
 * [ViewfinderSurfaceRequest.getSurface].
 *
 * This has two underlying implementations either using an [AndroidEmbeddedExternalSurface] for
 * [ImplementationMode.COMPATIBLE] or an [AndroidExternalSurface] for
 * [ImplementationMode.PERFORMANCE].
 *
 * @param surfaceRequest Details about the surface being requested
 * @param implementationMode Determines the underlying implementation of the [Surface].
 * @param transformationInfo Specifies the required transformations for the media being displayed.
 * @param modifier Modifier to be applied to the [Viewfinder]
 *
 * TODO(b/322420487): Add a sample with `@sample`
 */
@Composable
fun Viewfinder(
    surfaceRequest: ViewfinderSurfaceRequest,
    implementationMode: ImplementationMode,
    transformationInfo: TransformationInfo,
    modifier: Modifier = Modifier
) {
    val resolution = surfaceRequest.resolution

    Box(
        modifier = modifier
            .clipToBounds()
            .fillMaxSize()
    ) {
        key(surfaceRequest) {
            TransformedSurface(
                resolution = resolution,
                transformationInfo = transformationInfo,
                implementationMode = implementationMode,
                onInit = {
                    onSurface { newSurface, _, _ ->
                        // TODO(b/322420450): Properly release surface when no longer needed
                        surfaceRequest.provideSurface(newSurface)
                    }

                    // TODO(b/322420176): Properly handle onSurfaceChanged()
                },
            )
        }
    }
}

// TODO(b/322420450): Properly release surface when this is cancelled
private suspend fun ViewfinderSurfaceRequest.provideSurface(surface: Surface): Surface =
    suspendCancellableCoroutine {
        this.provideSurface(surface, Runnable::run) { result: ViewfinderSurfaceRequest.Result? ->
            it.resume(requireNotNull(result) {
                "Expected non-null result from ViewfinderSurfaceRequest, but received null."
            }.surface)
        }
    }

@SuppressLint("RestrictedApi")
@Composable
private fun TransformedSurface(
    resolution: Size,
    transformationInfo: TransformationInfo,
    implementationMode: ImplementationMode,
    onInit: AndroidExternalSurfaceScope.() -> Unit,
) {
    // For TextureView, correct the orientation to match the target rotation.
    val correctionMatrix = Matrix()
    transformationInfo.let {
        correctionMatrix.setFrom(
            SurfaceTransformationUtil.getTextureViewCorrectionMatrix(
                it,
                resolution
            )
        )
    }

    val surfaceModifier = Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(
                Constraints.fixed(resolution.width, resolution.height)
            )

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
                    val surfaceRectInViewfinder =
                        SurfaceTransformationUtil.getTransformedSurfaceRect(
                            resolution,
                            transformationInfo,
                            Size(constraints.maxWidth, constraints.maxHeight)
                        )

                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = surfaceRectInViewfinder.width() / resolution.width
                    scaleY = surfaceRectInViewfinder.height() / resolution.height

                    translationX = surfaceRectInViewfinder.left
                    translationY = surfaceRectInViewfinder.top
                }
            }
        }

    when (implementationMode) {
        ImplementationMode.PERFORMANCE -> {
            AndroidExternalSurface(
                modifier = surfaceModifier,
                onInit = onInit
            )
        }
        ImplementationMode.COMPATIBLE -> {
            AndroidEmbeddedExternalSurface(
                modifier = surfaceModifier,
                transform = correctionMatrix,
                onInit = onInit
            )
        }
    }
}
