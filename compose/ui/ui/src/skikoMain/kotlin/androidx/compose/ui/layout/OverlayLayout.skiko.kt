/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Creates an overlay layout that places the content on top of each other.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param content The composable content to be placed in the overlay layout.
 */
@Composable
internal fun OverlayLayout(modifier: Modifier, content: @Composable () -> Unit) = Layout(
    content = content,
    modifier = modifier,
    measurePolicy = { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        layout(
            placeables.maxOfOrNull { it.width } ?: constraints.minWidth,
            placeables.maxOfOrNull { it.height } ?: constraints.minHeight
        ) {
            placeables.forEach {
                it.place(0, 0)
            }
        }
    }
)
