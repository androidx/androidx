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

package androidx.ui.layout.samples

import androidx.compose.Composable
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.VerticalAlignmentLine
import androidx.ui.foundation.DrawBackground
import androidx.ui.graphics.Color
import androidx.ui.unit.Dp
import androidx.ui.unit.ipx
import androidx.ui.unit.max
import androidx.ui.unit.min

/**
 * Rect with two alignment lines [Start] and [End].
 */
// TODO (popam): remove this and make FlexSamples where it's being used copy-pastable
@Composable
fun SizedRectangleWithLines(
    modifier: Modifier = Modifier.None,
    color: Color,
    width: Dp? = null,
    height: Dp? = null
) {
    Layout(
        children = { },
        modifier = modifier + DrawBackground(color = color)
    ) { _, constraints, _ ->
        val widthPx = max(width?.toIntPx() ?: constraints.maxWidth, constraints.minWidth)
        val heightPx = max(height?.toIntPx() ?: constraints.maxHeight, constraints.minHeight)
        layout(widthPx, heightPx, mapOf(Start to 0.ipx, End to widthPx)) {}
    }
}

/**
 * Alignment lines for [SizedRectangleWithLines].
 */
internal val Start = VerticalAlignmentLine(::min)
internal val End = VerticalAlignmentLine(::min)