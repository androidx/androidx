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

import androidx.annotation.IntRange
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

public actual class GraphicsLayer {
    public actual var compositingStrategy: CompositingStrategy = implementedInJetBrainsFork()
    public actual var topLeft: IntOffset = implementedInJetBrainsFork()
    public actual var size: IntSize = implementedInJetBrainsFork()
    public actual var alpha: Float = implementedInJetBrainsFork()
    public actual var scaleX: Float = implementedInJetBrainsFork()
    public actual var scaleY: Float = implementedInJetBrainsFork()
    public actual var translationX: Float = implementedInJetBrainsFork()
    public actual var translationY: Float = implementedInJetBrainsFork()
    public actual var shadowElevation: Float = implementedInJetBrainsFork()
    public actual var rotationX: Float = implementedInJetBrainsFork()
    public actual var rotationY: Float = implementedInJetBrainsFork()
    public actual var rotationZ: Float = implementedInJetBrainsFork()
    public actual var cameraDistance: Float = implementedInJetBrainsFork()
    public actual var renderEffect: RenderEffect? = implementedInJetBrainsFork()

    public actual fun record(
        density: Density,
        layoutDirection: LayoutDirection,
        size: IntSize,
        block: DrawScope.() -> Unit,
    ): Unit = implementedInJetBrainsFork()

    public actual var clip: Boolean = implementedInJetBrainsFork()

    internal actual fun draw(canvas: Canvas, parentLayer: GraphicsLayer?): Unit =
        implementedInJetBrainsFork()

    public actual var pivotOffset: Offset = implementedInJetBrainsFork()
    public actual var blendMode: BlendMode = implementedInJetBrainsFork()
    public actual var colorFilter: ColorFilter? = implementedInJetBrainsFork()

    public actual fun setRoundRectOutline(topLeft: Offset, size: Size, cornerRadius: Float): Unit =
        implementedInJetBrainsFork()

    public actual fun setPathOutline(path: Path): Unit = implementedInJetBrainsFork()

    public actual val outline: Outline = implementedInJetBrainsFork()

    public actual fun setRectOutline(topLeft: Offset, size: Size): Unit =
        implementedInJetBrainsFork()

    public actual var isReleased: Boolean = implementedInJetBrainsFork()
    public actual var ambientShadowColor: Color = implementedInJetBrainsFork()
    public actual var spotShadowColor: Color = implementedInJetBrainsFork()

    public actual suspend fun toImageBitmap(): ImageBitmap = implementedInJetBrainsFork()

    public actual fun setOutsets(
        @IntRange(from = 0) left: Int,
        @IntRange(from = 0) top: Int,
        @IntRange(from = 0) right: Int,
        @IntRange(from = 0) bottom: Int,
    ): Unit = implementedInJetBrainsFork()
}
