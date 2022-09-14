/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.compose.material

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

/**
 * Scroll an item vertically in/out of view based on a [ScrollState].
 * Typically used to scroll a [TimeText] item out of view as the user starts to scroll a
 * vertically scrollable [Column] of items upwards and bring additional items into view.
 *
 * @param scrollState The [ScrollState] to used as the basis for the scroll-away.
 * @param offset Adjustment to the starting point for scrolling away. Positive values result in
 * the scroll away starting later.
 */
public fun Modifier.scrollAway(
    scrollState: ScrollState,
    offset: Int = 0,
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "scrollAway"
        properties["scrollState"] = scrollState
        properties["offset"] = offset
    }
) {
    val y = with(LocalDensity.current) {
        (scrollState.value - offset).toDp()
    }
    scrollAway(y)
}

/**
 * Scroll an item vertically in/out of view based on a [LazyListState].
 * Typically used to scroll a [TimeText] item out of view as the user starts to scroll
 * a [LazyColumn] of items upwards and bring additional items into view.
 *
 * @param scrollState The [LazyListState] to used as the basis for the scroll-away.
 * @param itemIndex The item for which the scroll offset will trigger scrolling away.
 * @param offset Adjustment to the starting point for scrolling away. Positive values result in
 * the scroll away starting later.
 */
public fun Modifier.scrollAway(
    scrollState: LazyListState,
    itemIndex: Int = 0,
    offset: Int = 0,
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "scrollAway"
        properties["scrollState"] = scrollState
        properties["itemIndex"] = itemIndex
        properties["offset"] = offset
    }
) {
    val targetItem by remember {
        derivedStateOf {
            scrollState.layoutInfo.visibleItemsInfo.find { it.index == itemIndex }
        }
    }
    if (targetItem != null) {
        val y = with(LocalDensity.current) {
            (-targetItem!!.offset - offset).toDp()
        }
        scrollAway(y)
    } else {
        ignore()
    }
}

/**
 * Scroll an item vertically in/out of view based on a [ScalingLazyListState].
 * Typically used to scroll a [TimeText] item out of view as the user starts to scroll
 * a [ScalingLazyColumn] of items upwards and bring additional items into view.
 *
 * @param scrollState The [ScalingLazyListState] to used as the basis for the scroll-away.
 * @param itemIndex The item for which the scroll offset will trigger scrolling away.
 * @param offset Adjustment to the starting point for scrolling away. Positive values result in
 * the scroll away starting later, negative values start scrolling away earlier.
 */
public fun Modifier.scrollAway(
    scrollState: ScalingLazyListState,
    itemIndex: Int = 1,
    offset: Int = 0,
): Modifier =
    composed(
        inspectorInfo = debugInspectorInfo {
            name = "scrollAway"
            properties["scrollState"] = scrollState
            properties["itemIndex"] = itemIndex
            properties["offset"] = offset
        }
    ) {
        val targetItem by remember {
            derivedStateOf {
                scrollState.layoutInfo.visibleItemsInfo.find { it.index == itemIndex }
            }
        }
        if (targetItem != null) {
            val y = with(LocalDensity.current) {
                (-targetItem!!.offset - offset).toDp()
            }
            scrollAway(y)
        } else {
            ignore()
        }
    }

private fun Modifier.scrollAway(y: Dp): Modifier {
    val progress: Float = (y / maxScrollOut).coerceIn(0f, 1f)
    val motionFraction: Float = lerp(minMotionOut, maxMotionOut, progress)
    val offset = -(maxOffset * progress)

    return this
        .offset(y = offset)
        .graphicsLayer {
            alpha = motionFraction
            scaleX = motionFraction
            scaleY = motionFraction
            transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0.0f)
        }
}

// Trivial modifier that neither measures nor places the content.
private fun Modifier.ignore(): Modifier = this.then(
    object : LayoutModifier {
        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints
        ): MeasureResult {
            return object : MeasureResult {
                override val width = 0
                override val height = 0
                override val alignmentLines = mapOf<AlignmentLine, Int>()
                override fun placeChildren() {}
            }
        }
    }
)

// The scroll motion effects take place between 0dp and 36dp.
internal val maxScrollOut = 36.dp

// The max offset to apply.
internal val maxOffset = 24.dp

// Fade and scale motion effects are between 100% and 50%.
internal const val minMotionOut = 1f
internal const val maxMotionOut = 0.5f
