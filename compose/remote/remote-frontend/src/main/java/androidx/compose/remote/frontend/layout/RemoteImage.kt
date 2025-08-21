/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.frontend.layout

import androidx.compose.foundation.Image
import androidx.compose.remote.core.operations.utilities.ImageScaling
import androidx.compose.remote.frontend.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.frontend.capture.NoRemoteCompose
import androidx.compose.remote.frontend.capture.RecordingCanvas
import androidx.compose.remote.frontend.modifier.RemoteModifier
import androidx.compose.remote.frontend.modifier.toComposeUi
import androidx.compose.remote.frontend.modifier.toComposeUiLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultFilterQuality
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale

class RemoteComposeImageModifier(
    private val modifier: RemoteModifier,
    val bitmap: ImageBitmap,
    val contentDescription: String?,
    val alignment: Alignment,
    val contentScale: ContentScale,
    val alpha: Float,
    val colorFilter: ColorFilter?,
    val filterQuality: FilterQuality,
) : DrawModifier {
    override fun ContentDrawScope.draw() {
        drawIntoCanvas {
            if (it.nativeCanvas is RecordingCanvas) {
                (it.nativeCanvas as RecordingCanvas).let {
                    val bitmapId = it.document.addBitmap(bitmap.asAndroidBitmap())
                    val scaleType =
                        when (contentScale) {
                            ContentScale.Fit -> ImageScaling.SCALE_FIT
                            ContentScale.Crop -> ImageScaling.SCALE_CROP
                            ContentScale.None -> ImageScaling.SCALE_NONE
                            ContentScale.Inside -> ImageScaling.SCALE_INSIDE
                            ContentScale.FillWidth -> ImageScaling.SCALE_FILL_WIDTH
                            ContentScale.FillHeight -> ImageScaling.SCALE_FILL_HEIGHT
                            ContentScale.FillBounds -> ImageScaling.SCALE_FILL_BOUNDS
                            else -> ImageScaling.SCALE_NONE
                        }
                    it.document.image(modifier.toRemoteCompose(), bitmapId, scaleType, alpha)
                }
            }
        }
    }
}

@Composable
@NonRestartableComposable
fun RemoteImage(
    bitmap: ImageBitmap,
    contentDescription: String?,
    modifier: RemoteModifier = RemoteModifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    filterQuality: FilterQuality = DefaultFilterQuality,
) {
    val captureMode = LocalRemoteComposeCreationState.current
    if (captureMode is NoRemoteCompose) {
        Image(
            bitmap,
            contentDescription,
            modifier.toComposeUi(),
            alignment,
            contentScale,
            alpha,
            colorFilter,
            filterQuality,
        )
    } else {
        androidx.compose.foundation.layout.Box(
            modifier =
                RemoteComposeImageModifier(
                        modifier,
                        bitmap,
                        contentDescription,
                        alignment,
                        contentScale,
                        alpha,
                        colorFilter,
                        filterQuality,
                    )
                    .then(modifier.toComposeUiLayout())
        )
    }
}
