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

package androidx.ui.core.vectorgraphics

import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.compositionReference
import androidx.compose.memo
import androidx.compose.onPreCommit
import androidx.compose.unaryPlus
import androidx.ui.core.Dp
import androidx.ui.core.Draw
import androidx.ui.core.Px
import androidx.ui.core.withDensity
import androidx.ui.graphics.Brush
import androidx.ui.graphics.EmptyBrush
import androidx.ui.graphics.obtainBrush
import androidx.ui.painting.StrokeCap
import androidx.ui.painting.StrokeJoin
import androidx.ui.vector.BrushType
import androidx.ui.vector.DefaultAlpha
import androidx.ui.vector.DefaultGroupName
import androidx.ui.vector.DefaultPathName
import androidx.ui.vector.DefaultPivotX
import androidx.ui.vector.DefaultPivotY
import androidx.ui.vector.DefaultRotation
import androidx.ui.vector.DefaultScaleX
import androidx.ui.vector.DefaultScaleY
import androidx.ui.vector.DefaultStrokeLineCap
import androidx.ui.vector.DefaultStrokeLineJoin
import androidx.ui.vector.DefaultStrokeLineMiter
import androidx.ui.vector.DefaultStrokeLineWidth
import androidx.ui.vector.DefaultTranslationX
import androidx.ui.vector.DefaultTranslationY
import androidx.ui.vector.EmptyPath
import androidx.ui.vector.GroupComponent
import androidx.ui.vector.PathComponent
import androidx.ui.vector.PathData
import androidx.ui.vector.VectorComponent
import androidx.ui.vector.VectorScope
import androidx.ui.vector.composeVector
import androidx.ui.vector.createPath
import androidx.ui.vector.disposeVector

/**
 * Sentinel value used to indicate that a dimension is not provided
 */
private const val unset: Float = -1.0f

/**
 * Draw a vector graphic with the provided width, height and viewport dimensions
 * [defaultWidth] Intrinsic width of the Vector in [Dp]
 * [defaultHeight] Intrinsic height of hte Vector in [Dp]
 * [vectorWidth] Width of the viewport space. The viewport is the virtual canvas where paths are drawn on.
 *  This parameter is optional. Not providing it will use the [defaultWidth] converted to [Px]
 * [vectorHeight] Height of hte viewport space. The viewport is the virtual canvas where paths are drawn on.
 *  This parameter is optional. Not providing it will use the [defaultHeight] converted to [Px]
 */
@Composable
fun DrawVector(
    defaultWidth: Dp,
    defaultHeight: Dp,
    viewportWidth: Float = unset,
    viewportHeight: Float = unset,
    name: String = "",
    @Children children: @Composable() VectorScope.(viewportWidth: Float, viewportHeight: Float) -> Unit
) {
    val widthPx = +withDensity { defaultWidth.toPx() }
    val heightPx = +withDensity { defaultHeight.toPx() }

    val vpWidth = if (viewportWidth == unset) widthPx.value else viewportWidth
    val vpHeight = if (viewportHeight == unset) heightPx.value else viewportHeight
    DrawVector(widthPx, heightPx, vpWidth, vpHeight, name, children)
}

/**
 * Draw a vector graphic with the provided width, height and viewport dimensions
 * [defaultWidth] Intrinsic width of the Vector in [Px]
 * [defaultHeight] Intrinsic height of hte Vector in [Px]
 * [vectorWidth] Width of the viewport space. The viewport is the virtual canvas where paths are drawn on.
 *  This parameter is optional. Not providing it will use the [defaultWidth]
 * [vectorHeight] Height of hte viewport space. The viewport is the virtual canvas where paths are drawn on.
 *  This parameter is optional. Not providing it will use the [defaultHeight]
 */
@Composable
fun DrawVector(
    defaultWidth: Px,
    defaultHeight: Px,
    viewportWidth: Float = defaultWidth.value,
    viewportHeight: Float = defaultHeight.value,
    name: String = "",
    @Children children: @Composable() VectorScope.(viewportWidth: Float, viewportHeight: Float) -> Unit
) {
    val vector =
        +memo(name, viewportWidth, viewportHeight) {
            VectorComponent(
                name,
                viewportWidth,
                viewportHeight,
                defaultWidth,
                defaultHeight
            )
        }

    val ref = +compositionReference()
    composeVector(vector, ref, children)
    +onPreCommit(vector) {
        onDispose {
            disposeVector(vector, ref)
        }
    }

    Draw { canvas, _ ->
        vector.draw(canvas)
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
    clipPathData: PathData = EmptyPath,
    children: @Composable() VectorScope.() -> Unit
) {

    val clipPathNodes = +memo(clipPathData) {
        createPath(clipPathData)
    }
    <GroupComponent
        name = name
        rotation = rotation
        pivotX = pivotX
        pivotY = pivotY
        scaleX = scaleX
        scaleY = scaleY
        translationX = translationX
        translationY = translationY
        clipPathNodes = clipPathNodes
    >
        children()
    </GroupComponent>
}

@Composable
fun VectorScope.Path(
    pathData: PathData,
    name: String = DefaultPathName,
    fill: BrushType = EmptyBrush,
    fillAlpha: Float = DefaultAlpha,
    stroke: BrushType = EmptyBrush,
    strokeAlpha: Float = DefaultAlpha,
    strokeLineWidth: Float = DefaultStrokeLineWidth,
    strokeLineCap: StrokeCap = DefaultStrokeLineCap,
    strokeLineJoin: StrokeJoin = DefaultStrokeLineJoin,
    strokeLineMiter: Float = DefaultStrokeLineMiter
) {
    val pathNodes = createPath(pathData)
    val fillBrush: Brush = obtainBrush(fill)
    val strokeBrush: Brush = obtainBrush(stroke)

    <PathComponent
        name
        pathNodes
        fill = fillBrush
        fillAlpha
        stroke = strokeBrush
        strokeAlpha
        strokeLineWidth
        strokeLineJoin
        strokeLineCap
        strokeLineMiter
    />
}