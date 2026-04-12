/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.text.selection

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import kotlin.math.max

/**
 * Selection is transparent in terms of measurement and layout and passes the same constraints to
 * the children.
 */
@Composable
internal fun SimpleLayout(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        var width = 0
        var height = 0
        val placeables =
            measurables.fastMap { measurable ->
                val placeable = measurable.measure(constraints)
                width = max(width, placeable.width)
                height = max(height, placeable.height)
                placeable
            }
        layout(width, height) { placeables.fastForEach { placeable -> placeable.place(0, 0) } }
    }
}
