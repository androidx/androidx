/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.core.selection

import androidx.compose.Composable
import androidx.ui.core.Alignment
import androidx.ui.core.Constraints
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.Placeable
import androidx.ui.core.Popup
import androidx.ui.core.enforce
import androidx.ui.core.hasFixedHeight
import androidx.ui.core.hasFixedWidth
import androidx.ui.geometry.Offset
import androidx.ui.text.InternalTextApi
import androidx.ui.text.style.ResolvedTextDirection
import androidx.ui.unit.Dp
import androidx.ui.unit.IntOffset
import androidx.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * @suppress
 */
@InternalTextApi
@Composable
fun SelectionHandleLayout(
    startHandlePosition: Offset?,
    endHandlePosition: Offset?,
    isStartHandle: Boolean,
    directions: Pair<ResolvedTextDirection, ResolvedTextDirection>,
    handlesCrossed: Boolean,
    handle: @Composable () -> Unit
) {
    val offset = (if (isStartHandle) startHandlePosition else endHandlePosition) ?: return

    SelectionLayout {
        val left = isLeft(
            isStartHandle = isStartHandle,
            directions = directions,
            handlesCrossed = handlesCrossed
        )
        val alignment = if (left) Alignment.TopEnd else Alignment.TopStart

        Popup(
            alignment = alignment,
            offset = IntOffset(offset.x.roundToInt(), offset.y.roundToInt()),
            children = handle
        )
    }
}

/**
 * Selection is transparent in terms of measurement and layout and passes the same constraints to
 * the children.
 * @suppress
 */
@InternalTextApi
@Composable
fun SelectionLayout(modifier: Modifier = Modifier, children: @Composable () -> Unit) {
    Layout(modifier = modifier, children = children) { measurables, constraints, _ ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }

        val width = placeables.fold(0) { maxWidth, placeable ->
            max(maxWidth, (placeable.width))
        }

        val height = placeables.fold(0) { minWidth, placeable ->
            max(minWidth, (placeable.height))
        }

        layout(width, height) {
            placeables.forEach { placeable ->
                placeable.placeAbsolute(0, 0)
            }
        }
    }
}

/**
 * A Container Box implementation used for selection children and handle layout
 */
@Composable
internal fun SimpleContainer(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp? = null,
    children: @Composable () -> Unit
) {
    Layout(children, modifier) { measurables, incomingConstraints, _ ->
        val containerConstraints = Constraints()
            .copy(
                width?.toIntPx() ?: 0,
                width?.toIntPx() ?: Constraints.Infinity,
                height?.toIntPx() ?: 0,
                height?.toIntPx() ?: Constraints.Infinity
            )
            .enforce(incomingConstraints)
        val childConstraints = containerConstraints.copy(minWidth = 0, minHeight = 0)
        var placeable: Placeable? = null
        val containerWidth = if (
            containerConstraints.hasFixedWidth
        ) {
            containerConstraints.maxWidth
        } else {
            placeable = measurables.firstOrNull()?.measure(childConstraints)
            max((placeable?.width ?: 0), containerConstraints.minWidth)
        }
        val containerHeight = if (
            containerConstraints.hasFixedHeight
        ) {
            containerConstraints.maxHeight
        } else {
            if (placeable == null) {
                placeable = measurables.firstOrNull()?.measure(childConstraints)
            }
            max((placeable?.height ?: 0), containerConstraints.minHeight)
        }
        layout(containerWidth, containerHeight) {
            val p = placeable ?: measurables.firstOrNull()?.measure(childConstraints)
            p?.let {
                val position = Alignment.Center.align(
                    IntSize(
                        containerWidth - it.width,
                        containerHeight - it.height
                    )
                )
                it.place(
                    position.x,
                    position.y
                )
            }
        }
    }
}
