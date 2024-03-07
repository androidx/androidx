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

package androidx.compose.ui.graphics.layer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

fun GraphicsContext(): GraphicsContext = DesktopGraphicsContext()

private class DesktopGraphicsContext : GraphicsContext {
    override fun createGraphicsLayer(): GraphicsLayer {
        return GraphicsLayer()
    }

    override fun releaseGraphicsLayer(layer: GraphicsLayer) {
        // TODO
    }
}

actual class GraphicsLayer internal constructor() {

    actual var topLeft: IntOffset = IntOffset.Zero

    actual var size: IntSize = IntSize.Zero

    actual var alpha: Float = 1f

    actual var scaleX: Float = 1f

    actual var scaleY: Float = 1f

    actual var translationX: Float = 0f

    actual var translationY: Float = 0f

    actual var shadowElevation: Float = 0f

    actual var rotationX: Float = 0f

    actual var rotationY: Float = 0f

    actual var rotationZ: Float = 0f

    actual var cameraDistance: Float = DefaultCameraDistance

    actual var renderEffect: RenderEffect? = null

    actual fun buildLayer(
        density: Density,
        layoutDirection: LayoutDirection,
        size: IntSize,
        block: DrawScope.() -> Unit
    ): GraphicsLayer {
        // TODO
        return this
    }

    actual var clip: Boolean = false

    actual fun draw(canvas: Canvas) {
        // TODO
    }

    actual var pivotOffset: Offset = Offset.Unspecified

    actual var blendMode: BlendMode = BlendMode.SrcOver
    actual var colorFilter: ColorFilter? = null

    actual var isReleased: Boolean = false

    actual var ambientShadowColor: Color = Color.Black

    actual var spotShadowColor: Color = Color.Black

    actual var compositingStrategy: CompositingStrategy = CompositingStrategy.Auto

    actual fun setRoundRectOutline(
        topLeft: IntOffset,
        size: IntSize,
        cornerRadius: Float
    ) {
        // TODO
    }

    actual fun setPathOutline(path: Path) {
        // TODO
    }

    actual fun setRectOutline(
        topLeft: IntOffset,
        size: IntSize
    ) {
        // TODO
    }

    actual val outline: Outline = Outline.Rectangle(Rect(0f, 0f, 0f, 0f))

    actual companion object {
        actual val UnsetOffset: IntOffset = IntOffset(Int.MIN_VALUE, Int.MIN_VALUE)
        actual val UnsetSize: IntSize = IntSize(Int.MIN_VALUE, Int.MIN_VALUE)
    }
}
