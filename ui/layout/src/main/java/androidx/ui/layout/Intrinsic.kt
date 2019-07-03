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

import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.ui.core.ComplexLayout
import androidx.ui.core.Constraints
import androidx.ui.core.enforce
import androidx.ui.core.ipx

/**
 * Layout widget that forces its child to be as wide as its min intrinsic width.
 * If incoming constraints do not allow this, the closest possible width will be used.
 */
@Composable
fun MinIntrinsicWidth(@Children children: @Composable() () -> Unit) {
    ComplexLayout(children) {
        layout { measurables, constraints ->
            val measurable = measurables.firstOrNull()
            val width = measurable?.minIntrinsicWidth(constraints.maxHeight) ?: 0.ipx
            val placeable = measurable?.measure(
                Constraints.tightConstraintsForWidth(width).enforce(constraints)
            )
            layoutResult(placeable?.width ?: 0.ipx, placeable?.height ?: 0.ipx) {
                placeable?.place(0.ipx, 0.ipx)
            }
        }
        minIntrinsicWidth { measurables, h ->
            measurables.firstOrNull()?.minIntrinsicWidth(h) ?: 0.ipx
        }
        minIntrinsicHeight { measurables, w ->
            measurables.firstOrNull()?.minIntrinsicHeight(w) ?: 0.ipx
        }
        maxIntrinsicWidth { measurables, h ->
            measurables.firstOrNull()?.minIntrinsicWidth(h) ?: 0.ipx
        }
        maxIntrinsicHeight { measurables, w ->
            measurables.firstOrNull()?.maxIntrinsicHeight(w) ?: 0.ipx
        }
    }
}

/**
 * Layout widget that forces its child to be as tall as its min intrinsic height.
 * If incoming constraints do not allow this, the closest possible height will be used.
 */
@Composable
fun MinIntrinsicHeight(@Children children: @Composable() () -> Unit) {
    ComplexLayout(children) {
        layout { measurables, constraints ->
            val measurable = measurables.firstOrNull()
            val height = measurable?.minIntrinsicHeight(constraints.maxWidth) ?: 0.ipx
            val placeable = measurable?.measure(
                Constraints.tightConstraintsForHeight(height).enforce(constraints)
            )
            layoutResult(placeable?.width ?: 0.ipx, placeable?.height ?: 0.ipx) {
                placeable?.place(0.ipx, 0.ipx)
            }
        }
        minIntrinsicWidth { measurables, h ->
            measurables.firstOrNull()?.minIntrinsicWidth(h) ?: 0.ipx
        }
        minIntrinsicHeight { measurables, w ->
            measurables.firstOrNull()?.minIntrinsicHeight(w) ?: 0.ipx
        }
        maxIntrinsicWidth { measurables, h ->
            measurables.firstOrNull()?.maxIntrinsicWidth(h) ?: 0.ipx
        }
        maxIntrinsicHeight { measurables, w ->
            measurables.firstOrNull()?.minIntrinsicHeight(w) ?: 0.ipx
        }
    }
}

/**
 * Layout widget that forces its child to be as wide as its max intrinsic width.
 * If incoming constraints do not allow this, the closest possible width will be used.
 */
@Composable
fun MaxIntrinsicWidth(@Children children: @Composable() () -> Unit) {
    ComplexLayout(children) {
        layout { measurables, constraints ->
            val measurable = measurables.firstOrNull()
            val width = measurable?.maxIntrinsicWidth(constraints.maxHeight) ?: 0.ipx
            val placeable = measurable?.measure(
                Constraints.tightConstraintsForWidth(width).enforce(constraints)
            )
            layoutResult(placeable?.width ?: 0.ipx, placeable?.height ?: 0.ipx) {
                placeable?.place(0.ipx, 0.ipx)
            }
        }
        minIntrinsicWidth { measurables, h ->
            measurables.firstOrNull()?.maxIntrinsicWidth(h) ?: 0.ipx
        }
        minIntrinsicHeight { measurables, w ->
            measurables.firstOrNull()?.minIntrinsicHeight(w) ?: 0.ipx
        }
        maxIntrinsicWidth { measurables, h ->
            measurables.firstOrNull()?.maxIntrinsicWidth(h) ?: 0.ipx
        }
        maxIntrinsicHeight { measurables, w ->
            measurables.firstOrNull()?.maxIntrinsicHeight(w) ?: 0.ipx
        }
    }
}

/**
 * Layout widget that forces its child to be as tall as its max intrinsic height.
 * If incoming constraints do not allow this, the closest possible height will be used.
 */
@Composable
fun MaxIntrinsicHeight(@Children children: @Composable() () -> Unit) {
    ComplexLayout(children) {
        layout { measurables, constraints ->
            val measurable = measurables.firstOrNull()
            val height = measurable?.maxIntrinsicHeight(constraints.maxHeight) ?: 0.ipx
            val placeable = measurable?.measure(
                Constraints.tightConstraintsForHeight(height).enforce(constraints)
            )
            layoutResult(placeable?.width ?: 0.ipx, placeable?.height ?: 0.ipx) {
                placeable?.place(0.ipx, 0.ipx)
            }
        }
        minIntrinsicWidth { measurables, h ->
            measurables.firstOrNull()?.minIntrinsicWidth(h) ?: 0.ipx
        }
        minIntrinsicHeight { measurables, w ->
            measurables.firstOrNull()?.maxIntrinsicHeight(w) ?: 0.ipx
        }
        maxIntrinsicWidth { measurables, h ->
            measurables.firstOrNull()?.maxIntrinsicWidth(h) ?: 0.ipx
        }
        maxIntrinsicHeight { measurables, w ->
            measurables.firstOrNull()?.maxIntrinsicHeight(w) ?: 0.ipx
        }
    }
}
