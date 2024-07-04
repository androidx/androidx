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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.implementedInJetBrainsFork
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

actual class GraphicsLayer {
    actual var compositingStrategy: CompositingStrategy = implementedInJetBrainsFork()
    actual var topLeft: IntOffset = implementedInJetBrainsFork()
    actual var size: IntSize = implementedInJetBrainsFork()
    actual var alpha: Float = implementedInJetBrainsFork()
    actual var scaleX: Float = implementedInJetBrainsFork()
    actual var scaleY: Float = implementedInJetBrainsFork()
    actual var translationX: Float = implementedInJetBrainsFork()
    actual var translationY: Float = implementedInJetBrainsFork()
    actual var shadowElevation: Float = implementedInJetBrainsFork()
    actual var rotationX: Float = implementedInJetBrainsFork()
    actual var rotationY: Float = implementedInJetBrainsFork()
    actual var rotationZ: Float = implementedInJetBrainsFork()
    actual var cameraDistance: Float = implementedInJetBrainsFork()
    actual var renderEffect: RenderEffect? = implementedInJetBrainsFork()

    actual fun record(
        density: Density,
        layoutDirection: LayoutDirection,
        size: IntSize,
        block: DrawScope.() -> Unit
    ): Unit = implementedInJetBrainsFork()

    actual var clip: Boolean = implementedInJetBrainsFork()

    internal actual fun draw(canvas: Canvas, parentLayer: GraphicsLayer?): Unit =
        implementedInJetBrainsFork()

    actual var pivotOffset: Offset = implementedInJetBrainsFork()
    actual var blendMode: BlendMode = implementedInJetBrainsFork()
    actual var colorFilter: ColorFilter? = implementedInJetBrainsFork()

    actual fun setRoundRectOutline(topLeft: Offset, size: Size, cornerRadius: Float): Unit =
        implementedInJetBrainsFork()

    actual fun setPathOutline(path: Path): Unit = implementedInJetBrainsFork()

    actual val outline: Outline = implementedInJetBrainsFork()

    actual fun setRectOutline(topLeft: Offset, size: Size): Unit = implementedInJetBrainsFork()

    actual var isReleased: Boolean = implementedInJetBrainsFork()
    actual var ambientShadowColor: Color = implementedInJetBrainsFork()
    actual var spotShadowColor: Color = implementedInJetBrainsFork()

    actual suspend fun toImageBitmap(): ImageBitmap = implementedInJetBrainsFork()
}
