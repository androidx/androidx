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

package androidx.wear.compose.foundation.lazy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.trace
import androidx.wear.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import kotlinx.coroutines.CoroutineScope

internal fun interface MeasuredItemProvider {
    /**
     * Creates a [TransformingLazyColumnMeasuredItem] with the given index and offset with the
     * position calculated from top.
     */
    fun measuredItem(
        index: Int,
        offset: Int,
        measurementDirection: MeasurementDirection,
        progressProvider: (Int) -> TransformingLazyColumnItemScrollProgress,
    ): TransformingLazyColumnMeasuredItem
}

@Composable
internal fun rememberTransformingLazyColumnMeasurePolicy(
    itemProviderLambda: () -> TransformingLazyColumnItemProvider,
    state: TransformingLazyColumnState,
    coroutineScope: CoroutineScope,
    horizontalAlignment: Alignment.Horizontal,
    verticalArrangement: Arrangement.Vertical,
    measurementStrategy: TransformingLazyColumnMeasurementStrategy,
): LazyLayoutMeasureScope.(Constraints) -> MeasureResult =
    remember(
        itemProviderLambda,
        state,
        coroutineScope,
        horizontalAlignment,
        verticalArrangement,
        measurementStrategy,
    ) {
        { containerConstraints ->
            val childConstraints =
                Constraints(
                    maxHeight = Constraints.Infinity,
                    maxWidth =
                        containerConstraints.maxWidth -
                            measurementStrategy.leftContentPadding -
                            measurementStrategy.rightContentPadding,
                )
            val itemProvider = itemProviderLambda()

            val measuredItemProvider =
                MeasuredItemProvider { index, offset, measurementDirection, progressProvider ->
                    val placeables = measure(index, childConstraints)
                    // TODO(artemiy): Add support for multiple items.
                    val placeable = placeables.lastOrNull()
                    val key = itemProvider.getKey(index)
                    TransformingLazyColumnMeasuredItem(
                        index = index,
                        placeable = placeable,
                        offset = offset,
                        containerConstraints = containerConstraints,
                        measureScrollProgress = progressProvider(placeable?.height ?: 0),
                        measurementDirection = measurementDirection,
                        horizontalAlignment = horizontalAlignment,
                        layoutDirection = layoutDirection,
                        key = key,
                        spacing = verticalArrangement.spacing.roundToPx(),
                        leftPadding = measurementStrategy.leftContentPadding,
                        rightPadding = measurementStrategy.rightContentPadding,
                        animationProvider = { state.animator.getAnimation(key) },
                        contentType = itemProvider.getContentType(index),
                    )
                }

            val itemsCount = itemProviderLambda().itemCount

            val anchorItemKey: Any
            val anchorItemIndex: Int
            val anchorItemScrollOffset: Int
            val lastMeasuredAnchorItemHeight: Int
            val scrollToBeConsumed: Float
            Snapshot.withoutReadObservation {
                anchorItemKey = state.anchorItemKey
                anchorItemIndex =
                    if (itemsCount == 0) 0 else state.anchorItemIndex.coerceIn(0 until itemsCount)
                anchorItemScrollOffset = state.anchorItemScrollOffset
                lastMeasuredAnchorItemHeight = state.lastMeasuredAnchorItemHeight
                scrollToBeConsumed = state.scrollToBeConsumed
            }

            Snapshot.withMutableSnapshot {
                    trace("wear-compose:tlc:measure") {
                        measurementStrategy.measure(
                            itemsCount = itemsCount,
                            keyIndexMap = itemProvider.keyIndexMap,
                            measuredItemProvider = measuredItemProvider,
                            itemSpacing = verticalArrangement.spacing.roundToPx(),
                            containerConstraints = containerConstraints,
                            scrollToBeConsumed = scrollToBeConsumed,
                            anchorItemKey = anchorItemKey,
                            anchorItemIndex = anchorItemIndex,
                            anchorItemScrollOffset = anchorItemScrollOffset,
                            lastMeasuredAnchorItemHeight = lastMeasuredAnchorItemHeight,
                            coroutineScope = coroutineScope,
                            density = this,
                            layout = { width, height, placement ->
                                layout(
                                    containerConstraints.constrainWidth(width),
                                    containerConstraints.constrainHeight(height),
                                    emptyMap(),
                                    placement,
                                )
                            },
                        )
                    }
                }
                .also { state.applyMeasureResult(it) }
        }
    }

internal enum class MeasurementDirection {
    /**
     * Indicates that the item is being measured downward. This corresponds to using
     * [TransformingLazyColumnItemScrollProgress.downwardMeasuredItemScrollProgress].
     */
    DOWNWARD,

    /**
     * Indicates that the item is being measured upward This corresponds to using
     * [TransformingLazyColumnItemScrollProgress.upwardMeasuredItemScrollProgress].
     */
    UPWARD,
}
