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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded.layout

import androidx.compose.remote.core.operations.layout.managers.FitBoxLayout
import androidx.compose.remote.player.compose.embedded.RcPlayerChildren
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth

/**
 * Renders a [FitBoxLayout]: its children are *alternatives* and only the first one whose natural
 * size fits the available space is displayed (the rest are measured but not placed) — matching
 * remote-core's FitBoxLayout ("only display the child that fits in the available space"). The
 * chosen child is centered, like a Box.
 *
 * Children are measured unbounded to get their natural size, then the first with `width <= maxWidth
 * && height <= maxHeight` wins. (Core hides the FitBox entirely when nothing fits; here we fall
 * back to the first child so the player isn't blank.) FitBox children are normally wrap/fixed-size
 * alternatives; a child that wants to fill its parent has no natural size and isn't a meaningful
 * FitBox candidate.
 */
@Composable
internal fun RcPlayerFitBoxLayout(layout: FitBoxLayout, modifier: Modifier) {
    Layout(content = { RcPlayerChildren(layout) }, modifier = modifier) { measurables, constraints
        ->
        val placeables = measurables.map { it.measure(Constraints()) }
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        val maxHeight = if (constraints.hasBoundedHeight) constraints.maxHeight else Int.MAX_VALUE

        var chosen = placeables.indexOfFirst { it.width <= maxWidth && it.height <= maxHeight }
        if (chosen < 0) chosen = if (placeables.isEmpty()) -1 else 0

        val picked = placeables.getOrNull(chosen)
        val width = constraints.constrainWidth(picked?.width ?: 0)
        val height = constraints.constrainHeight(picked?.height ?: 0)
        layout(width, height) {
            if (picked != null) {
                val x = ((width - picked.width) / 2).coerceAtLeast(0)
                val y = ((height - picked.height) / 2).coerceAtLeast(0)
                picked.placeRelative(x, y)
            }
        }
    }
}
