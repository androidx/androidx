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
import androidx.ui.graphics.Brush
import androidx.ui.graphics.StrokeCap
import androidx.ui.graphics.StrokeJoin

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
    children: @Composable VectorScope.() -> Unit
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