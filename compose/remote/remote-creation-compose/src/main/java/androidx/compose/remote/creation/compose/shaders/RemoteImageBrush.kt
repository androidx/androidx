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
import androidx.compose.remote.creation.compose.layout.RemoteContentScale
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.RemoteMatrix3x3
import androidx.compose.remote.creation.compose.state.RemoteMatrix3x3.Companion.createIdentity
import androidx.compose.remote.creation.compose.state.RemoteMatrix3x3.Companion.createScaleX
import androidx.compose.remote.creation.compose.state.RemoteMatrix3x3.Companion.createScaleY
import androidx.compose.remote.creation.compose.state.RemoteMatrix3x3.Companion.createTranslateXy
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.RemoteTileMode
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Immutable

internal class RemoteImageShader(
    public val bitmap: RemoteImageBitmap,
    public val tileModeX: RemoteTileMode,
    public val tileModeY: RemoteTileMode,
) : RemoteShader() {
    override fun apply(creationState: RemoteComposeCreationState, paintBundle: PaintBundle) {
        paintBundle.setTextureShader(
            bitmap.getIdForCreationState(creationState),
            tileModeX.value.toShort(),
            tileModeY.value.toShort(),
            Rc.Texture.FILTER_DEFAULT,
            0,
        )
    }
}

@Immutable
internal data class RemoteImageBrush(
    public val bitmap: RemoteImageBitmap,
    public val tileModeX: RemoteTileMode = RemoteTileMode.Clamp,
    public val tileModeY: RemoteTileMode = RemoteTileMode.Clamp,
    public val contentScale: RemoteContentScale = RemoteContentScale.None,
) : RemoteShaderBrush() {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun RemoteStateScope.createShader(size: RemoteSize): RemoteShader {
        return RemoteImageShader(bitmap, tileModeX, tileModeY).apply {
            remoteMatrix3x3 = createScaleMatrix(size)
        }
    }

    private fun createScaleMatrix(size: RemoteSize): RemoteMatrix3x3 {
        val intrinsicWidth = bitmap.width
        val intrinsicHeight = bitmap.height
        val scaleX = size.width / intrinsicWidth
        val scaleY = size.height / intrinsicHeight

        val finalScaleX: RemoteFloat
        val finalScaleY: RemoteFloat
        when (contentScale) {
            RemoteContentScale.Fit -> {
                val scale = scaleX.isLessThan(scaleY).select(scaleX, scaleY)
                finalScaleX = scale
                finalScaleY = scale
            }
            RemoteContentScale.Crop -> {
                val scale = scaleX.isGreaterThan(scaleY).select(scaleX, scaleY)
                finalScaleX = scale
                finalScaleY = scale
            }
            RemoteContentScale.FillBounds -> {
                finalScaleX = scaleX
                finalScaleY = scaleY
            }
            RemoteContentScale.FillWidth -> {
                finalScaleX = scaleX
                finalScaleY = scaleX
            }
            RemoteContentScale.FillHeight -> {
                finalScaleX = scaleY
                finalScaleY = scaleY
            }
            RemoteContentScale.Inside -> {
                val minScale = scaleX.isLessThan(scaleY).select(scaleX, scaleY)
                val scale = minScale.isLessThan(1.rf).select(minScale, 1.rf)
                finalScaleX = scale
                finalScaleY = scale
            }
            RemoteContentScale.None -> {
                finalScaleX = 1.rf
                finalScaleY = 1.rf
            }
            else -> return createIdentity()
        }

        val dx = (size.width - (intrinsicWidth * finalScaleX)) / 2f
        val dy = (size.height - (intrinsicHeight * finalScaleY)) / 2f

        return createTranslateXy(dx, dy) * createScaleX(finalScaleX) * createScaleY(finalScaleY)
    }
}
