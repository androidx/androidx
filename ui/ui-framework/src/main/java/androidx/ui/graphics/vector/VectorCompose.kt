/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.graphics.vector

import androidx.compose.Composable
import androidx.compose.compositionReference
import androidx.compose.onPreCommit
import androidx.compose.remember
import androidx.ui.core.Alignment
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Draw
import androidx.ui.graphics.BlendMode
import androidx.ui.graphics.Brush
import androidx.ui.graphics.Color
import androidx.ui.graphics.ScaleFit
import androidx.ui.graphics.StrokeCap
import androidx.ui.graphics.StrokeJoin
import androidx.ui.graphics.withSave
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.Px
import androidx.ui.unit.PxSize
import kotlin.math.ceil

/**
 * Sentinel value used to indicate that a dimension is not provided
 */
private const val unset: Float = -1.0f

private val DefaultAlignment = Alignment.Center

/**
 * Draw a vector graphic with the provided width, height and viewport dimensions
 * @param[defaultWidth] Intrinsic width of the Vector in [Dp]
 * @param[defaultHeight] Intrinsic height of hte Vector in [Dp]
 * @param[viewportWidth] Width of the viewport space. The viewport is the virtual canvas where
 * paths are drawn on.
 *  This parameter is optional. Not providing it will use the [defaultWidth] converted to [Px]
 * @param[viewportHeight] Height of hte viewport space. The viewport is the virtual canvas where
 * paths are drawn on.
 *  This parameter is optional. Not providing it will use the [defaultHeight] converted to [Px]
 * @param[tintColor] Optional color used to tint this vector graphic
 * @param[tintBlendMode] Optional blend mode used with [tintColor], default is [BlendMode.srcIn]
 * @param[alignment] Specifies the placement of the vector within the drawing bounds
 * @param[scaleFit] Specifies how the vector is to be scaled within the parent bounds
 */
@Composable
fun DrawVector(
    defaultWidth: Dp,
    defaultHeight: Dp,
    viewportWidth: Float = unset,
    viewportHeight: Float = unset,
    tintColor: Color = DefaultTintColor,
    tintBlendMode: BlendMode = DefaultTintBlendMode,
    alignment: Alignment = DefaultAlignment,
    scaleFit: ScaleFit = ScaleFit.Fit,
    name: String = "",
    children: @Composable() VectorScope.(viewportWidth: Float, viewportHeight: Float) -> Unit
) {
    val density = DensityAmbient.current
    val widthPx = with(density) { defaultWidth.toPx() }
    val heightPx = with(density) { defaultHeight.toPx() }

    val vpWidth = if (viewportWidth == unset) widthPx.value else viewportWidth
    val vpHeight = if (viewportHeight == unset) heightPx.value else viewportHeight
    DrawVector(
        defaultWidth = widthPx,
        defaultHeight = heightPx,
        viewportWidth = vpWidth,
        viewportHeight = vpHeight,
        tintColor = tintColor,
        tintBlendMode = tintBlendMode,
        alignment = alignment,
        scaleFit = scaleFit,
        name = name,
        children = children
    )
}

/**
 * Draw a vector graphic with the provided width, height and viewport dimensions
 * @param[defaultWidth] Intrinsic width of the Vector in [Px]
 * @param[defaultHeight] Intrinsic height of hte Vector in [Px]
 * @param[viewportWidth] Width of the viewport space. The viewport is the virtual canvas
 *  where paths are drawn on. This parameter is optional. Not providing it will use the
 *  [defaultWidth]
 * @param[viewportHeight] Height of hte viewport space. The viewport is the virtual canvas
 *  where paths are drawn on. This parameter is optional. Not providing it will use the
 *  [defaultHeight]
 * @param[tintColor] Optional color used to tint this vector graphic
 * @param[tintBlendMode] Optional blend mode used with [tintColor], default is [BlendMode.srcIn]
 * @param[alignment] Specifies the placement of the vector within the drawing bounds
 * @param[scaleFit] Specifies how the vector is to be scaled within the parent bounds
 */
@Composable
fun DrawVector(
    defaultWidth: Px,
    defaultHeight: Px,
    viewportWidth: Float = defaultWidth.value,
    viewportHeight: Float = defaultHeight.value,
    tintColor: Color = DefaultTintColor,
    tintBlendMode: BlendMode = DefaultTintBlendMode,
    alignment: Alignment = DefaultAlignment,
    scaleFit: ScaleFit = ScaleFit.Fit,
    name: String = "",
    children: @Composable() VectorScope.(viewportWidth: Float, viewportHeight: Float) -> Unit
) {
    val vector =
        remember(name, viewportWidth, viewportHeight) {
            VectorComponent(
                name,
                viewportWidth,
                viewportHeight,
                defaultWidth,
                defaultHeight
            )
        }

    val ref = compositionReference()
    composeVector(vector, ref, children)
    onPreCommit(vector) {
        onDispose {
            disposeVector(vector, ref)
        }
    }

    val vectorWidth = defaultWidth.value
    val vectorHeight = defaultHeight.value
    val vectorPxSize = PxSize(Px(vectorWidth), Px(vectorHeight))

    Draw { canvas, parentSize ->
        val parentWidth = parentSize.width.value
        val parentHeight = parentSize.height.value
        val scale = scaleFit.scale(vectorPxSize, parentSize)

        val alignedPosition = alignment.align(
            IntPxSize(
                IntPx(ceil(parentWidth - (vectorWidth * scale)).toInt()),
                IntPx(ceil(parentHeight - (vectorHeight * scale)).toInt())
            )
        )

        val translateX = alignedPosition.x.value.toFloat()
        val translateY = alignedPosition.y.value.toFloat()

        // apply the scale to the root of the vector
        vector.root.scaleX = (vectorWidth / viewportWidth) * scale
        vector.root.scaleY = (vectorHeight / viewportHeight) * scale

        canvas.withSave {
            canvas.translate(translateX, translateY)
            vector.draw(canvas, tintColor, tintBlendMode)
        }
    }
}

@Composable
fun VectorScope.Group(
    name: String = DefaultGroupName,
    rotation: Float = DefaultRotation,
    pivotX: Float = DefaultPivotX,
    pivotY: Float = DefaultPivotY,
    scaleX: Float = DefaultScaleX,
    scaleY: Float = DefaultScaleY,
    translationX: Float = DefaultTranslationX,
    translationY: Float = DefaultTranslationY,
    clipPathData: List<PathNode> = EmptyPath,
    children: @Composable() VectorScope.() -> Unit
) {
    GroupComponent(
        name = name,
        rotation = rotation,
        pivotX = pivotX,
        pivotY = pivotY,
        scaleX = scaleX,
        scaleY = scaleY,
        translationX = translationX,
        translationY = translationY,
        clipPathData = clipPathData
    ) {
        children()
    }
}

@Composable
fun VectorScope.Path(
    pathData: List<PathNode>,
    name: String = DefaultPathName,
    fill: Brush? = null,
    fillAlpha: Float = DefaultAlpha,
    stroke: Brush? = null,
    strokeAlpha: Float = DefaultAlpha,
    strokeLineWidth: Float = DefaultStrokeLineWidth,
    strokeLineCap: StrokeCap = DefaultStrokeLineCap,
    strokeLineJoin: StrokeJoin = DefaultStrokeLineJoin,
    strokeLineMiter: Float = DefaultStrokeLineMiter
) {
    PathComponent(
        name = name,
        pathData = pathData,
        fill = fill,
        fillAlpha = fillAlpha,
        stroke = stroke,
        strokeAlpha = strokeAlpha,
        strokeLineWidth = strokeLineWidth,
        strokeLineJoin = strokeLineJoin,
        strokeLineCap = strokeLineCap,
        strokeLineMiter = strokeLineMiter
    )
}