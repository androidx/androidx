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

package androidx.compose.remote.creation.compose.shaders

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.RemoteMatrix3x3
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.TileMode as ComposeTileMode
import androidx.compose.ui.graphics.toAndroidTileMode
import androidx.compose.ui.layout.ContentScale

internal class RemoteImageShader(
    public val bitmap: RemoteImageBitmap,
    public val tileModeX: ComposeTileMode,
    public val tileModeY: ComposeTileMode,
) : RemoteShader() {
    override fun apply(creationState: RemoteComposeCreationState, paintBundle: PaintBundle) {
        paintBundle.setTextureShader(
            bitmap.getIdForCreationState(creationState),
            tileModeX.toAndroidTileMode().ordinal.toShort(),
            tileModeY.toAndroidTileMode().ordinal.toShort(),
            Rc.Texture.FILTER_DEFAULT,
            0,
        )
    }

    override var remoteMatrix3x3: RemoteMatrix3x3? = null
}

@Immutable
internal data class RemoteImageBrush(
    public val bitmap: RemoteImageBitmap,
    public val tileModeX: ComposeTileMode = ComposeTileMode.Clamp,
    public val tileModeY: ComposeTileMode = ComposeTileMode.Clamp,
    public val contentScale: ContentScale = ContentScale.None,
) : RemoteBrush() {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun RemoteStateScope.createShader(size: RemoteSize): RemoteShader {
        return RemoteImageShader(bitmap, tileModeX, tileModeY).apply {
            remoteMatrix3x3 = createScaleMatrix(size)
        }
    }

    private fun createScaleMatrix(size: RemoteSize): RemoteMatrix3x3? {
        val intrinsicWidth = bitmap.width
        val intrinsicHeight = bitmap.height
        val scaleX = size.width / intrinsicWidth
        val scaleY = size.height / intrinsicHeight

        val finalScaleX: RemoteFloat
        val finalScaleY: RemoteFloat
        when (contentScale) {
            ContentScale.Fit -> {
                val scale = scaleX.isLessThan(scaleY).select(scaleX, scaleY)
                finalScaleX = scale
                finalScaleY = scale
            }
            ContentScale.Crop -> {
                val scale = scaleX.isGreaterThan(scaleY).select(scaleX, scaleY)
                finalScaleX = scale
                finalScaleY = scale
            }
            ContentScale.FillBounds -> {
                finalScaleX = scaleX
                finalScaleY = scaleY
            }
            ContentScale.FillWidth -> {
                finalScaleX = scaleX
                finalScaleY = scaleX
            }
            ContentScale.FillHeight -> {
                finalScaleX = scaleY
                finalScaleY = scaleY
            }
            ContentScale.Inside -> {
                val minScale = scaleX.isLessThan(scaleY).select(scaleX, scaleY)
                val scale = minScale.isLessThan(1.rf).select(minScale, 1.rf)
                finalScaleX = scale
                finalScaleY = scale
            }
            ContentScale.None -> {
                finalScaleX = 1.rf
                finalScaleY = 1.rf
            }
            else -> return null
        }

        val dx = (size.width - (intrinsicWidth * finalScaleX)) / 2f
        val dy = (size.height - (intrinsicHeight * finalScaleY)) / 2f

        return RemoteMatrix3x3.createTranslateXy(dx, dy) *
            RemoteMatrix3x3.createScaleX(finalScaleX) *
            RemoteMatrix3x3.createScaleY(finalScaleY)
    }
}

/**
 * Creates a texture brush with a specified [image].
 *
 * @param image The [RemoteImageBitmap] to use
 * @param tileModeX The [ComposeTileMode] to use for the bitmap in the x-axis. Defaults to
 *   [ComposeTileMode.Clamp] to repeat the edge pixels
 * @param tileModeY The [ComposeTileMode] to use for the bitmap in the y-axis. Defaults to
 *   [ComposeTileMode.Clamp] to repeat the edge pixels
 * @param contentScale The [ContentScale] to use when scaling the bitmap.
 */
@Stable
public fun RemoteBrush.Companion.image(
    image: RemoteImageBitmap,
    tileModeX: ComposeTileMode = ComposeTileMode.Clamp,
    tileModeY: ComposeTileMode = ComposeTileMode.Clamp,
    contentScale: ContentScale = ContentScale.None,
): RemoteBrush = RemoteImageBrush(image, tileModeX, tileModeY, contentScale)
