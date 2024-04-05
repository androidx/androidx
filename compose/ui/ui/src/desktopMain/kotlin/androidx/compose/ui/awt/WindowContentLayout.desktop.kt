/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.awt

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.LocalWindow

/**
 * Base layout for full-window Compose content.
 */
@Composable
internal fun WindowContentLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val window = requireNotNull(LocalWindow.current)
    Layout(
        content = content,
        modifier = modifier,
        measurePolicy = { measurables, constraints ->
            val resizerMeasurable = measurables.lastOrNull()?.let {
                if (it.layoutId == "UndecoratedWindowResizer") it else null
            }
            val resizerPlaceable = resizerMeasurable?.let {
                val resizerWidth = (window.width * density).toInt()
                val resizerHeight = (window.height * density).toInt()
                it.measure(
                    Constraints(
                        minWidth = resizerWidth,
                        minHeight = resizerHeight,
                        maxWidth = resizerWidth,
                        maxHeight = resizerHeight
                    )
                )
            }

            val contentPlaceables = buildList(measurables.size) {
                measurables.fastForEach {
                    if (it != resizerMeasurable)
                        add(it.measure(constraints))
                }
            }

            val contentWidth = contentPlaceables.maxOfOrNull { it.measuredWidth } ?: 0
            val contentHeight = contentPlaceables.maxOfOrNull { it.measuredHeight } ?: 0
            layout(contentWidth, contentHeight) {
                contentPlaceables.fastForEach { placeable ->
                    placeable.place(0, 0)
                }
                resizerPlaceable?.place(0, 0)
            }
        }
    )
}
