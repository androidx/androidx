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

package androidx.r4a

import android.content.res.Resources
import android.view.View
import androidx.ui.core.vectorgraphics.VectorKt
import androidx.ui.core.vectorgraphics.compat.VectorResourceKt
import androidx.ui.painting.StrokeCap
import androidx.ui.painting.StrokeJoin
import androidx.ui.vectorgraphics.BrushType
import androidx.ui.vectorgraphics.DEFAULT_ALPHA
import androidx.ui.vectorgraphics.DEFAULT_GROUP_NAME
import androidx.ui.vectorgraphics.DEFAULT_PATH_NAME
import androidx.ui.vectorgraphics.DEFAULT_PIVOT_X
import androidx.ui.vectorgraphics.DEFAULT_PIVOT_Y
import androidx.ui.vectorgraphics.DEFAULT_ROTATE
import androidx.ui.vectorgraphics.DEFAULT_SCALE_X
import androidx.ui.vectorgraphics.DEFAULT_SCALE_Y
import androidx.ui.vectorgraphics.DEFAULT_STROKE_LINE_CAP
import androidx.ui.vectorgraphics.DEFAULT_STROKE_LINE_JOIN
import androidx.ui.vectorgraphics.DEFAULT_STROKE_LINE_MITER
import androidx.ui.vectorgraphics.DEFAULT_STROKE_LINE_WIDTH
import androidx.ui.vectorgraphics.DEFAULT_TRANSLATE_X
import androidx.ui.vectorgraphics.DEFAULT_TRANSLATE_Y
import androidx.ui.vectorgraphics.EMPTY_BRUSH
import androidx.ui.vectorgraphics.EMPTY_PATH
import androidx.ui.vectorgraphics.PathData
import com.google.r4a.Children
import com.google.r4a.Composable

// Temporary file to properly import R4A dependencies from other libraries
// This should go away when the meta-data fix to preserve Kotlin code lands

@SuppressWarnings("PLUGIN_ERROR")
fun adoptVectorGraphic(parent: Any?, child: Any?): View? {
    return VectorKt.adoptVectorGraphic(parent, child)
}

@Composable
@Suppress("PLUGIN_ERROR")
fun vectorResource(res: Resources, resId: Int) {
    VectorResourceKt.vectorResource(res, resId)
}

@Composable
@Suppress("PLUGIN_ERROR")
fun vector(
    @Children children: () -> Unit,
    name: String = "",
    defaultWidth: Float,
    defaultHeight: Float,
    viewportWidth: Float,
    viewportHeight: Float
) {
    VectorKt.vector(name, defaultWidth, defaultHeight, viewportWidth, viewportHeight, children)
}

@Composable
@Suppress("PLUGIN_ERROR")
fun group(
    @Children childNodes: () -> Unit,
    name: String = DEFAULT_GROUP_NAME,
    rotate: Float = DEFAULT_ROTATE,
    pivotX: Float = DEFAULT_PIVOT_X,
    pivotY: Float = DEFAULT_PIVOT_Y,
    scaleX: Float = DEFAULT_SCALE_X,
    scaleY: Float = DEFAULT_SCALE_Y,
    translateX: Float = DEFAULT_TRANSLATE_X,
    translateY: Float = DEFAULT_TRANSLATE_Y,
    clipPath: PathData = EMPTY_PATH
) {
    VectorKt.group(name, rotate, pivotX, pivotY, scaleX,
        scaleY, translateX, translateY, clipPath, childNodes)
}

@Composable
@Suppress("PLUGIN_ERROR")
fun path(
    pathData: PathData,
    name: String = DEFAULT_PATH_NAME,
    fill: BrushType = EMPTY_BRUSH,
    fillAlpha: Float = DEFAULT_ALPHA,
    strokeAlpha: Float = DEFAULT_ALPHA,
    strokeLineWidth: Float = DEFAULT_STROKE_LINE_WIDTH,
    stroke: BrushType = EMPTY_BRUSH,
    strokeLineCap: StrokeCap = DEFAULT_STROKE_LINE_CAP,
    strokeLineJoin: StrokeJoin = DEFAULT_STROKE_LINE_JOIN,
    strokeLineMiter: Float = DEFAULT_STROKE_LINE_MITER
) {
    VectorKt.path(pathData, name, fill, fillAlpha, stroke, strokeAlpha, strokeLineWidth,
        strokeLineCap, strokeLineJoin, strokeLineMiter)
}