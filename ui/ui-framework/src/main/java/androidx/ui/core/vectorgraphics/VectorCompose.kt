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
import androidx.compose.onDispose
import androidx.compose.onPreCommit
import androidx.compose.unaryPlus

import androidx.ui.core.Draw
import androidx.ui.core.Px
import androidx.ui.graphics.vectorgraphics.Brush
import androidx.ui.graphics.vectorgraphics.BrushType
import androidx.ui.graphics.vectorgraphics.DefaultAlpha
import androidx.ui.graphics.vectorgraphics.DefaultGroupName
import androidx.ui.graphics.vectorgraphics.DefaultPathName
import androidx.ui.graphics.vectorgraphics.DefaultPivotX
import androidx.ui.graphics.vectorgraphics.DefaultPivotY
import androidx.ui.graphics.vectorgraphics.DefaultRotation
import androidx.ui.graphics.vectorgraphics.DefaultScaleX
import androidx.ui.graphics.vectorgraphics.DefaultScaleY
import androidx.ui.graphics.vectorgraphics.DefaultStrokeLineCap
import androidx.ui.graphics.vectorgraphics.DefaultStrokeLineJoin
import androidx.ui.graphics.vectorgraphics.DefaultStrokeLineMiter
import androidx.ui.graphics.vectorgraphics.DefaultStrokeLineWidth
import androidx.ui.graphics.vectorgraphics.DefaultTranslationX
import androidx.ui.graphics.vectorgraphics.DefaultTranslationY
import androidx.ui.graphics.vectorgraphics.EmptyBrush
import androidx.ui.graphics.vectorgraphics.EmptyPath
import androidx.ui.graphics.vectorgraphics.PathData
import androidx.ui.graphics.vectorgraphics.VectorComponent
import androidx.ui.graphics.vectorgraphics.GroupComponent
import androidx.ui.graphics.vectorgraphics.PathComponent
import androidx.ui.graphics.vectorgraphics.createPath
import androidx.ui.graphics.vectorgraphics.obtainBrush
import androidx.ui.painting.StrokeCap
import androidx.ui.painting.StrokeJoin
import androidx.ui.vector.VectorScope
import androidx.ui.vector.composeVector
import androidx.ui.vector.disposeVector
import java.util.Vector


@Composable
fun DrawVector(
    viewportWidth: Float,
    viewportHeight: Float,
    defaultWidth: Px = Px(viewportWidth),
    defaultHeight: Px = Px(viewportHeight),
    name: String = "",
    @Children children: @Composable() VectorScope.() -> Unit
) {
    val vector = +memo(name, viewportWidth, viewportHeight) {
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
        onDispose { disposeVector(vector, ref) }
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
    @Children children: @Composable() VectorScope.() -> Unit
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