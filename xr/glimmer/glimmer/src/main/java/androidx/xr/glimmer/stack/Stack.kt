/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer.stack

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex

/**
 * [VerticalStack] is a lazy scrollable layout that displays its children in a form of a stack where
 * the item on top of the stack is prominently displayed. [VerticalStack] implements the item
 * traversal in a vertical direction.
 *
 * @sample androidx.xr.glimmer.samples.VerticalStackSample
 * @param modifier the modifier to apply to this layout.
 * @param state the state of the stack.
 * @param content a block that describes the content. Inside this block you can use methods like
 *   [StackScope.item] to add a single item or [StackScope.items] to add a collection of items.
 */
@Composable
public fun VerticalStack(
    modifier: Modifier = Modifier,
    state: StackState = rememberStackState(),
    content: StackScope.() -> Unit,
) {
    val latestContent = rememberUpdatedState(content)
    val stackItemHolderState =
        remember(state) {
            // Re-run the DSL to parse items only when the content lambda instance changes.
            derivedStateOf(referentialEqualityPolicy()) {
                    StackItemHolder(state, latestContent.value).also {
                        // Set the item count on the StackState immediately when the derived state
                        // re-evaluates (i.e., when content changes), even before recomposition.
                        state.itemCount = it.itemCount
                    }
                }
                .also {
                    // This second assignment is necessary to force the derivedStateOf lambda above
                    // (which is executed lazily) to execute synchronously inside this block.
                    state.itemCount = it.value.itemCount
                }
        }

    VerticalPager(
        state = state.pagerState,
        modifier = modifier.onFocusChanged(state::onTopLevelFocusChanged).stackScrim(),
        contentPadding = PaddingValues(bottom = RevealAreaSize),
        key = { page -> stackItemHolderState.value.getKey(page) },
        beyondViewportPageCount = MaxNextVisibleItemCount,
    ) { page ->
        val stackItemHolder = stackItemHolderState.value
        stackItemHolder.withInterval(page) { localIndex, itemInterval ->
            val key =
                itemInterval.getKeyOrDefault(globalIndex = page, localIntervalIndex = localIndex)
            val itemScope = itemInterval.getOrCreateItemScope(key)
            itemScope.index = page
            StackItemLayout(
                page = page,
                state = state,
                stackItemHolder = stackItemHolder,
                itemScope = itemScope,
            ) {
                itemInterval.item(itemScope, localIndex)
            }
        }
    }
}

@Composable
private fun StackItemLayout(
    page: Int,
    state: StackState,
    stackItemHolder: StackItemHolder,
    itemScope: StackItemScopeImpl,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    Layout(
        content = content,
        modifier =
            modifier
                .zIndex(-page.toFloat())
                .clipToItemAbove(page, state, stackItemHolder)
                .focusRequester(focusRequester)
                .onFocusChanged { state.onItemFocusChanged(page, it) },
    ) { measurables, constraints ->
        var maxWidth = 0
        var maxHeight = 0
        val placeables =
            measurables.fastMap {
                it.measure(constraints).also { placeable ->
                    maxWidth = maxOf(maxWidth, placeable.width)
                    maxHeight = maxOf(maxHeight, placeable.height)
                }
            }

        if (!isLookingAhead) {
            state.layoutInfoInternal.updateMeasuredHeight(index = page, height = maxHeight)
        }

        layout(maxWidth, maxHeight) {
            placeables.fastForEach { placeable ->
                placeable.placeRelativeWithLayer(x = 0, y = 0) {
                    val revealHeight = RevealAreaSize.roundToPx()
                    val topItem = state.topItem
                    when {
                        page.isTopItem(topItem = topItem) -> {
                            state
                                .topItemTranslationY(
                                    revealHeight = revealHeight,
                                    topItemHeight = placeable.height,
                                )
                                .ifNonNaN { translationY = it }

                            state.notifyAutoFocus(page, focusRequester)
                        }

                        page.isNextItem(topItem = topItem) -> {
                            state
                                .nextItemTranslationY(
                                    revealHeight = revealHeight,
                                    topItem = topItem,
                                    nextItemHeight = placeable.height,
                                )
                                .ifNonNaN { translationY = it }
                            val scale = state.nextItemScale()
                            scaleX = scale
                            scaleY = scale

                            state.notifyAutoFocus(page, focusRequester)
                        }

                        page.isNextNextItem(topItem = topItem) -> {
                            state
                                .nextNextItemTranslationY(
                                    revealHeight = revealHeight,
                                    topItem = topItem,
                                    nextNextItemHeight = placeable.height,
                                )
                                .ifNonNaN { translationY = it }
                            scaleX = NextItemMinScale
                            scaleY = NextItemMinScale
                        }
                    }
                }
            }
        }
    }
}

/** Clips the item at the given index such that only the part below the item above is displayed. */
private fun Modifier.clipToItemAbove(
    index: Int,
    state: StackState,
    stackItemHolder: StackItemHolder,
): Modifier =
    this.drawWithCache {
        if (index == 0) {
            // The first item is never clipped.
            return@drawWithCache onDrawWithContent { drawContent() }
        }

        val clipPath = Path() // Ensure the clip path is cached.
        var previousClipOffset = Float.NaN
        var cachedItemAboveSize: Size? = null
        var cachedItemAboveShape: Shape? = null

        onDrawWithContent {
            val clipOffset =
                state.calculateClipOffset(
                    index = index,
                    topItem = state.topItem,
                    revealHeight = RevealAreaSize.roundToPx(),
                )
            if (clipOffset.isNaN()) {
                drawContent()
                return@onDrawWithContent
            }

            val clipOffsetDiff =
                if (previousClipOffset.isNaN()) clipOffset else clipOffset - previousClipOffset
            previousClipOffset = clipOffset

            val itemAboveScope = stackItemHolder.getItemScope(index - 1)
            if (itemAboveScope == null) {
                drawContent()
                return@onDrawWithContent
            }

            // TODO(b/446933128): add support for multiple item decorations.
            val itemAboveDecoration = itemAboveScope.firstDecoration()
            val itemAboveSize = itemAboveDecoration?.size
            val itemAboveShape = itemAboveDecoration?.shape

            if (cachedItemAboveSize != itemAboveSize || cachedItemAboveShape != itemAboveShape) {
                cachedItemAboveSize = itemAboveSize
                cachedItemAboveShape = itemAboveShape

                // The size or shape of the item above changed, we have to update the clip path.
                clipPath.apply {
                    reset()
                    if (itemAboveSize != null && itemAboveShape != null) {
                        addOutline(
                            itemAboveShape.createOutline(
                                size = itemAboveSize,
                                layoutDirection = this@drawWithCache.layoutDirection,
                                density = this@drawWithCache,
                            )
                        )
                        // Extend the clip region from the middle of the item above to the top of
                        // the viewport so that larger items are fully clipped.
                        addRect(
                            Rect(
                                left = 0f,
                                top = (itemAboveSize.height / 2f) - size.height,
                                right = itemAboveSize.width,
                                bottom = itemAboveSize.height / 2f,
                            )
                        )
                    }
                }
            }

            if (clipPath.isEmpty) {
                drawContent()
                return@onDrawWithContent
            }

            clipPath.translate(Offset(x = 0f, y = -clipOffsetDiff))
            clipPath(path = clipPath, clipOp = ClipOp.Difference) {
                this@onDrawWithContent.drawContent()
            }
        }
    }

/**
 * The translation Y of the top item in pixels, which is always equal to the snapped position
 * offset.
 */
private fun StackState.topItemTranslationY(revealHeight: Int, topItemHeight: Int) =
    calculateTopPositionOffset(topItemHeight, revealHeight)

/** The current translation Y of the next item in pixels. */
private fun StackState.nextItemTranslationY(
    revealHeight: Int,
    topItem: Int,
    nextItemHeight: Int,
): Float {
    val nextPageOffset = pagerState.pageOffset(topItem + 1) ?: return Float.NaN
    val progress = topItemOffsetFraction
    val topItemHeight = layoutInfoInternal.measuredTopItemHeight

    val topPageTopPositionOffset = calculateTopPositionOffset(topItemHeight, revealHeight)
    val nextPageTopPositionOffset = calculateTopPositionOffset(nextItemHeight, revealHeight)

    val topItemBottom = topPageTopPositionOffset + topItemHeight
    // startOffset is the initial offset of the next item when it's revealed below the top item.
    val startOffset =
        calculateInitialBehindPosition(
            itemAboveBottom = topItemBottom + revealHeight,
            itemBehindHeight = nextItemHeight,
        )

    val currentOffset =
        lerp(start = startOffset, stop = nextPageTopPositionOffset, fraction = progress)

    return currentOffset - nextPageOffset
}

/** The current scale of the next item. */
private fun StackState.nextItemScale(): Float {
    val progress = topItemOffsetFraction
    return lerp(start = NextItemMinScale, stop = 1f, fraction = progress)
}

/** The current translation Y of the next-next item in pixels. */
private fun StackState.nextNextItemTranslationY(
    revealHeight: Int,
    topItem: Int,
    nextNextItemHeight: Int,
): Float {
    val nextNextPageOffset = pagerState.pageOffset(topItem + 2) ?: return Float.NaN
    val viewportHeight = layoutInfoInternal.viewportSize.height
    val nextItemHeight = layoutInfoInternal.measuredNextItemHeight

    // How much of the reveal area the item has scrolled into, as it moves up from the bottom.
    val progress = ((viewportHeight - nextNextPageOffset) / revealHeight.toFloat()).coerceIn(0f, 1f)

    val nextPageTopPositionOffset = calculateTopPositionOffset(nextItemHeight, revealHeight)
    val nextItemBottom = nextPageTopPositionOffset + nextItemHeight

    // startOffset is the initial offset of the next-next item.
    val startOffset =
        calculateInitialBehindPosition(
            itemAboveBottom = nextItemBottom,
            itemBehindHeight = nextNextItemHeight,
        )
    val currentOffset = startOffset + revealHeight * progress

    return currentOffset - nextNextPageOffset
}

/**
 * Calculates the offset from the top of the viewport for an item of a given height for the item's
 * top snapped position. Items are aligned to the bottom of the stack layout.
 */
private fun StackState.calculateTopPositionOffset(itemHeight: Int, revealHeight: Int): Float {
    val viewportHeight = layoutInfoInternal.viewportSize.height
    return (viewportHeight - itemHeight - revealHeight).coerceAtLeast(0).toFloat()
}

/** Calculates the initial offset for an item when it is positioned behind the item above it. */
private fun calculateInitialBehindPosition(itemAboveBottom: Float, itemBehindHeight: Int): Float =
    itemAboveBottom - itemBehindHeight * NextItemPositioningScale

/** Calculates the offset for the clip shape to clip the item at the given index. */
private fun StackState.calculateClipOffset(index: Int, topItem: Int, revealHeight: Int): Float =
    when {
        index.isNextItem(topItem = topItem) -> {
            val topItemTranslationY =
                topItemTranslationY(
                    revealHeight = revealHeight,
                    topItemHeight = layoutInfoInternal.measuredTopItemHeight,
                )
            val maxItemHeight = layoutInfoInternal.viewportSize.height - revealHeight
            maxItemHeight - topItemTranslationY
        }

        index.isNextNextItem(topItem = topItem) -> {
            val nextItemTranslationY =
                nextItemTranslationY(
                    revealHeight = revealHeight,
                    topItem = topItem,
                    nextItemHeight = layoutInfoInternal.measuredNextItemHeight,
                )
            if (nextItemTranslationY.isNaN()) Float.NaN
            else {
                val maxItemHeight = layoutInfoInternal.viewportSize.height - revealHeight
                maxItemHeight - nextItemTranslationY
            }
        }

        else -> Float.NaN
    }

/** The main axis offset of the item in pixels from the top of the viewport. */
private fun PagerState.pageOffset(page: Int): Int? =
    layoutInfo.visiblePagesInfo.fastFirstOrNull { it.index == page }?.offset

private inline fun Float.ifNonNaN(block: (Float) -> Unit) {
    if (!isNaN()) block.invoke(this)
}

/** The size of the area where the items beneath the top of the stack item are revealed. */
internal val RevealAreaSize = 18.dp

/** The maximum number of items that can be visible at the same time in addition to the top item. */
private const val MaxNextVisibleItemCount = 2

/**
 * The scale of the next item when it is fully behind the top item, and the default scale of the
 * item behind it (the next-next item).
 */
private const val NextItemMinScale = 0.94f

/** The scale factor that's between 1.0 and [NextItemMinScale], which is used in positioning. */
private const val NextItemPositioningScale = 0.97f // (1f + NextItemMinScale) / 2f
