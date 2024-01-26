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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.setFrom
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Composable Viewfinder.
 * TODO(b/322420487): KDOC
 */
@Composable
fun Viewfinder(
    surfaceRequest: ViewfinderSurfaceRequest,
    implementationMode: ImplementationMode,
    transformationInfo: TransformationInfo,
    modifier: Modifier = Modifier
) {
    var parentViewSize by remember { mutableStateOf(IntSize.Zero) }
    val resolution = surfaceRequest.resolution

    IntSize(transformationInfo.cropRectRight - transformationInfo.cropRectLeft,
        transformationInfo.cropRectTop - transformationInfo.cropRectBottom)

    Box(
        modifier = modifier
            .onSizeChanged {
                parentViewSize = it
            }
            .clipToBounds()
            .wrapContentSize(unbounded = true, align = Alignment.Center)
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
                getParentSize = { parentViewSize },
            )
        }
    }
}

// TODO(b/322420450): Properly release surface when this is cancelled
private suspend fun ViewfinderSurfaceRequest.provideSurface(surface: Surface): Surface =
    suspendCancellableCoroutine {
        this.provideSurface(surface, Runnable::run) { result ->
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
    getParentSize: () -> IntSize
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

    val getSurfaceRectInViewFinder = {
        SurfaceTransformationUtil.getTransformedSurfaceRect(
            resolution,
            transformationInfo,
            getParentSize().toSize()
        )
    }

    val getViewFinderScaleX = { getSurfaceRectInViewFinder().width() / resolution.width }
    val getViewFinderScaleY = { getSurfaceRectInViewFinder().height() / resolution.height }

    val heightDp = with(LocalDensity.current) { resolution.height.toDp() }
    val widthDp = with(LocalDensity.current) { resolution.width.toDp() }

    val getModifier: () -> Modifier = {
        Modifier
            .height(heightDp)
            .width(widthDp)
            .scale(getViewFinderScaleX(), getViewFinderScaleY())
    }

    when (implementationMode) {
        ImplementationMode.PERFORMANCE -> {
            AndroidExternalSurface(
                modifier = getModifier(),
                onInit = onInit
            )
        }
        ImplementationMode.COMPATIBLE -> {
            AndroidEmbeddedExternalSurface(
                modifier = getModifier(),
                transform = correctionMatrix,
                onInit = onInit
            )
        }
    }
}

private fun IntSize.toSize() = Size(this.width, this.height)
