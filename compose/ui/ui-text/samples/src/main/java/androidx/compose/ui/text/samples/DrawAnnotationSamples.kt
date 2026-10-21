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

package androidx.compose.ui.text.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Sampled
@Composable
fun TextLayoutResultGetBoundingBoxesForRangeSample() {
    val textMeasurer = rememberTextMeasurer()
    val highlightRange = TextRange(10, 25)
    var boundingBoxes by remember { mutableStateOf<List<Rect>>(emptyList()) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Canvas(
        Modifier.size(200.dp, 80.dp).layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            val result =
                textMeasurer.measure(
                    text = "Highlight a range of text",
                    style = TextStyle(fontSize = 18.sp),
                    constraints = Constraints(maxWidth = placeable.width),
                )
            textLayoutResult = result
            boundingBoxes =
                result.getBoundingBoxesForRange(
                    start = highlightRange.start,
                    end = highlightRange.end,
                )
            layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
        }
    ) {
        textLayoutResult?.let { result ->
            for (i in boundingBoxes.indices) {
                val box = boundingBoxes[i]
                drawRect(
                    color = Color.Yellow,
                    topLeft = box.topLeft,
                    size = box.size,
                )
            }
            drawText(result)
        }
    }
}

@Sampled
@Composable
fun TextLayoutResultFillPathForRangeSample() {
    val textMeasurer = rememberTextMeasurer()
    val highlightRange = TextRange(12, 36)
    val cachedPath = remember { Path() }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Canvas(
        Modifier.size(200.dp, 100.dp).layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            val result =
                textMeasurer.measure(
                    text = "Highlight a multi-line range of text with a merged rounded path",
                    style = TextStyle(fontSize = 18.sp),
                    constraints = Constraints(maxWidth = placeable.width),
                )
            textLayoutResult = result
            result.fillPathForRange(
                start = highlightRange.start,
                end = highlightRange.end,
                target = cachedPath,
                cornerRadius = CornerRadius(6.dp.toPx()),
                mergeLines = true,
            )
            layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
        }
    ) {
        textLayoutResult?.let { result ->
            drawPath(path = cachedPath, color = Color.Yellow)
            drawText(result)
        }
    }
}
