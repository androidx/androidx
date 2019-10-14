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
import androidx.ui.graphics.StrokeCap
import androidx.ui.graphics.StrokeJoin

/**
 * Sentinel value used to indicate that a dimension is not provided
 */
private const val unset: Float = -1.0f

/**
 * Draw a vector graphic with the provided width, height and viewport dimensions
 * [defaultWidth] Intrinsic width of the Vector in [Dp]
 * [defaultHeight] Intrinsic height of hte Vector in [Dp]
 * [viewportWidth] Width of the viewport space. The viewport is the virtual canvas where paths are drawn on.
 *  This parameter is optional. Not providing it will use the [defaultWidth] converted to [Px]
 * [viewportHeight] Height of hte viewport space. The viewport is the virtual canvas where paths are drawn on.
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
 * [viewportWidth] Width of the viewport space. The viewport is the virtual canvas where paths are drawn on.
 *  This parameter is optional. Not providing it will use the [defaultWidth]
 * [viewportHeight] Height of hte viewport space. The viewport is the virtual canvas where paths are drawn on.
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
    clipPathData: Array<PathNode> = EmptyPath,
    children: @Composable() VectorScope.() -> Unit
) {
    <GroupComponent
        name = name
        rotation = rotation
        pivotX = pivotX
        pivotY = pivotY
        scaleX = scaleX
        scaleY = scaleY
        translationX = translationX
        translationY = translationY
        clipPathData = clipPathData
    >
        children()
    </GroupComponent>
}

@Composable
fun VectorScope.Path(
    pathData: Array<PathNode>,
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
    <PathComponent
        name
        pathData
        fill
        fillAlpha
        stroke
        strokeAlpha
        strokeLineWidth
        strokeLineJoin
        strokeLineCap
        strokeLineMiter
    />
}