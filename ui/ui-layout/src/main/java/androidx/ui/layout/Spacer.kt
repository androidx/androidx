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

package androidx.ui.layout

import androidx.compose.Composable
import androidx.compose.emptyContent
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.hasFixedHeight
import androidx.ui.core.hasFixedWidth
import androidx.ui.unit.ipx
import androidx.ui.unit.isFinite

/**
 * Component that represents an empty space layout, whose size can be defined using the [LayoutWidth],
 * [LayoutHeight] and [LayoutSize] modifiers.
 *
 * @sample androidx.ui.layout.samples.SpacerExample
 *
 * @param modifier modifiers to set to this spacer
 */
@Composable
fun Spacer(modifier: Modifier) {
    Layout(emptyContent(), modifier) { _, constraints, _ ->
        with(constraints) {
            val width = if (hasFixedWidth && maxWidth.isFinite()) maxWidth else 0.ipx
            val height = if (hasFixedHeight && maxHeight.isFinite()) maxHeight else 0.ipx
            layout(width, height) {}
        }
    }
}