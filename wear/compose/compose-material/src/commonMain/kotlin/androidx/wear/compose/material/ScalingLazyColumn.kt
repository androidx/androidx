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

package androidx.wear.compose.material

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset

/**
 * Receiver scope which is used by [ScalingLazyColumn].
 */
@ScalingLazyScopeMarker
public interface ScalingLazyListScope {
    /**
     * Adds a single item.
     *
     * @param key a stable and unique key representing the item. Using the same key
     * for multiple items in the list is not allowed. Type of the key should be saveable
     * via Bundle on Android. If null is passed the position in the list will represent the key.
     * When you specify the key the scroll position will be maintained based on the key, which
     * means if you add/remove items before the current visible item the item with the given key
     * will be kept as the first visible one.
     * @param content the content of the item
     */
    fun item(key: Any? = null, content: @Composable ScalingLazyListItemScope.() -> Unit)

    /**
     * Adds a [count] of items.
     *
     * @param count the items count
     * @param key a factory of stable and unique keys representing the item. Using the same key
     * for multiple items in the list is not allowed. Type of the key should be saveable
     * via Bundle on Android. If null is passed the position in the list will represent the key.
     * When you specify the key the scroll position will be maintained based on the key, which
     * means if you add/remove items before the current visible item the item with the given key
     * will be kept as the first visible one.
     * @param itemContent the content displayed by a single item
     */
    fun items(
        count: Int,
        key: ((index: Int) -> Any)? = null,
        itemContent: @Composable ScalingLazyListItemScope.(index: Int) -> Unit
    )
}

/**
 * Adds a list of items.
 *
 * @param items the data list
 * @param key a factory of stable and unique keys representing the item. Using the same key
 * for multiple items in the list is not allowed. Type of the key should be saveable
 * via Bundle on Android. If null is passed the position in the list will represent the key.
 * When you specify the key the scroll position will be maintained based on the key, which
 * means if you add/remove items before the current visible item the item with the given key
 * will be kept as the first visible one.
 * @param itemContent the content displayed by a single item
 */
inline fun <T> ScalingLazyListScope.items(
    items: List<T>,
    noinline key: ((item: T) -> Any)? = null,
    crossinline itemContent: @Composable ScalingLazyListItemScope.(item: T) -> Unit
) = items(items.size, if (key != null) { index: Int -> key(items[index]) } else null) {
    itemContent(items[it])
}

/**
 * Adds a list of items where the content of an item is aware of its index.
 *
 * @param items the data list
 * @param key a factory of stable and unique keys representing the item. Using the same key
 * for multiple items in the list is not allowed. Type of the key should be saveable
 * via Bundle on Android. If null is passed the position in the list will represent the key.
 * When you specify the key the scroll position will be maintained based on the key, which
 * means if you add/remove items before the current visible item the item with the given key
 * will be kept as the first visible one.
 * @param itemContent the content displayed by a single item
 */
inline fun <T> ScalingLazyListScope.itemsIndexed(
    items: List<T>,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    crossinline itemContent: @Composable ScalingLazyListItemScope.(index: Int, item: T) -> Unit
) = items(items.size, if (key != null) { index: Int -> key(index, items[index]) } else null) {
    itemContent(it, items[it])
}

/**
 * Adds an array of items.
 *
 * @param items the data array
 * @param key a factory of stable and unique keys representing the item. Using the same key
 * for multiple items in the list is not allowed. Type of the key should be saveable
 * via Bundle on Android. If null is passed the position in the list will represent the key.
 * When you specify the key the scroll position will be maintained based on the key, which
 * means if you add/remove items before the current visible item the item with the given key
 * will be kept as the first visible one.
 * @param itemContent the content displayed by a single item
 */
inline fun <T> ScalingLazyListScope.items(
    items: Array<T>,
    noinline key: ((item: T) -> Any)? = null,
    crossinline itemContent: @Composable ScalingLazyListItemScope.(item: T) -> Unit
) = items(items.size, if (key != null) { index: Int -> key(items[index]) } else null) {
    itemContent(items[it])
}

/**
 * Adds an array of items where the content of an item is aware of its index.
 *
 * @param items the data array
 * @param key a factory of stable and unique keys representing the item. Using the same key
 * for multiple items in the list is not allowed. Type of the key should be saveable
 * via Bundle on Android. If null is passed the position in the list will represent the key.
 * When you specify the key the scroll position will be maintained based on the key, which
 * means if you add/remove items before the current visible item the item with the given key
 * will be kept as the first visible one.
 * @param itemContent the content displayed by a single item
 */
public inline fun <T> ScalingLazyListScope.itemsIndexed(
    items: Array<T>,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    crossinline itemContent: @Composable ScalingLazyListItemScope.(index: Int, item: T) -> Unit
) = items(items.size, if (key != null) { index: Int -> key(index, items[index]) } else null) {
    itemContent(it, items[it])
}

@Suppress("INLINE_CLASS_DEPRECATED")
@Immutable
public inline class ScalingLazyListAnchorType internal constructor(internal val type: Int) {

    companion object {
        /**
         * Place the center of the item on (or as close to) the center line of the viewport
         */
        val ItemCenter = ScalingLazyListAnchorType(0)

        /**
         * Place the start (edge) of the item on, or as close to as possible, the center line of the
         * viewport. For normal layout this will be the top edge of the item, for reverseLayout it
         * will be the bottom edge.
         */
        val ItemStart = ScalingLazyListAnchorType(1)
    }

    override fun toString(): String {
        return when (this) {
            ItemStart -> "ScalingLazyListAnchorType.ItemStart"
            else -> "ScalingLazyListAnchorType.ItemCenter"
        }
    }
}

internal fun convertToCenterOffset(
    anchorType: ScalingLazyListAnchorType,
    itemScrollOffset: Int,
    viewPortSizeInPx: Int,
    beforeContentPaddingInPx: Int,
    itemSizeInPx: Int
): Int {
    if (anchorType == ScalingLazyListAnchorType.ItemStart) {
        return itemScrollOffset - (viewPortSizeInPx / 2) + beforeContentPaddingInPx
    } else {
        return itemScrollOffset + (itemSizeInPx / 2) -
            (viewPortSizeInPx / 2) + beforeContentPaddingInPx
    }
}

/**
 * A scrolling scaling/fisheye list component that forms a key part of the Wear Material Design
 * language. Provides scaling and transparency effects to the content items.
 *
 * [ScalingLazyColumn] is designed to be able to handle potentially large numbers of content
 * items. Content items are only materialized and composed when needed.
 *
 * If scaling/fisheye functionality is not required then a [LazyColumn] should be considered
 * instead to avoid any overhead of measuring and calculating scaling and transparency effects for
 * the content items.
 *
 * Example usage:
 * @sample androidx.wear.compose.material.samples.SimpleScalingLazyColumn
 *
 * For more information, see the
 * [Lists](https://developer.android.com/training/wearables/components/lists)
 * guide.
 *
 * @param modifier The modifier to be applied to the component
 * @param scalingParams The parameters to configure the scaling and transparency effects for the
 * component
 * @param reverseLayout reverse the direction of scrolling and layout, when `true` items will be
 * composed from the bottom to the top
 * @param verticalArrangement The vertical arrangement of the layout's children. This allows us
 * to add spacing between items and specify the arrangement of the items when we have not enough
 * of them to fill the whole minimum size
 * @param horizontalAlignment the horizontal alignment applied to the items
 * @param contentPadding The padding to apply around the contents
 * @param anchorType How to anchor list items to the center-line of the viewport
 * @param flingBehavior Logic describing fling behavior
 * @param autoCentering Flag to determine whether all items should be centerable in the viewport.
 * If true then sufficient space will be made available before the first and after the last
 * list item to ensure that they can be scrolled to the center of the viewport.
 * @param state The state of the component
 */
@Composable
public fun ScalingLazyColumn(
    modifier: Modifier = Modifier,
    scalingParams: ScalingParams = ScalingLazyColumnDefaults.scalingParams(),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        Arrangement.spacedBy(
            space = 4.dp,
            alignment = if (!reverseLayout) Alignment.Top else Alignment.Bottom
        ),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp),
    state: ScalingLazyListState = rememberScalingLazyListState(),
    anchorType: ScalingLazyListAnchorType = ScalingLazyListAnchorType.ItemCenter,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    autoCentering: Boolean = true,
    content: ScalingLazyListScope.() -> Unit
) {
    var initialized by remember { mutableStateOf(false) }
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val layoutDirection = LocalLayoutDirection.current
        val extraPaddingInPixels = scalingParams.resolveViewportVerticalOffset(constraints)

        with(density) {
            val extraPadding = extraPaddingInPixels.toDp()
            val combinedPaddingValues = CombinedPaddingValues(
                contentPadding = contentPadding,
                extraPadding = extraPadding
            )

            val beforeContentPaddingInPx =
                if (reverseLayout) contentPadding.calculateBottomPadding().roundToPx()
                else contentPadding.calculateTopPadding().roundToPx()

            val itemScope =
                ScalingLazyListItemScopeImpl(
                    density = density,
                    constraints = constraints.offset(
                        horizontal = -(
                            contentPadding.calculateStartPadding(layoutDirection) +
                                contentPadding.calculateEndPadding(layoutDirection)
                            ).toPx().toInt(),
                        vertical = -(
                            contentPadding.calculateTopPadding() +
                                contentPadding.calculateBottomPadding()
                            ).roundToPx()
                    )
                )

            // Set up transient state
            state.scalingParams.value = scalingParams
            state.extraPaddingPx.value = extraPaddingInPixels
            state.beforeContentPaddingPx.value = beforeContentPaddingInPx
            state.viewportHeightPx.value = constraints.maxHeight
            state.gapBetweenItemsPx.value =
                verticalArrangement.spacing.roundToPx()
            state.anchorType.value = anchorType
            state.autoCentering.value = autoCentering
            state.reverseLayout.value = reverseLayout

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .verticalNegativePadding(extraPadding)
                    .onGloballyPositioned {
                        initialized = true
                    },
                horizontalAlignment = horizontalAlignment,
                contentPadding = combinedPaddingValues,
                reverseLayout = reverseLayout,
                verticalArrangement = verticalArrangement,
                state = state.lazyListState,
                flingBehavior = flingBehavior,
            ) {
                val scope = ScalingLazyListScopeImpl(
                    state = state,
                    scope = this,
                    itemScope = itemScope
                )
                // Only add spacers if autoCentering == true as we have to consider the impact of
                // vertical spacing between items.
                if (autoCentering) {
                    item {
                        Spacer(
                            modifier = Modifier.height(state.topAutoCenteringPaddingPx.toDp())
                        )
                    }
                }
                scope.content()
                if (autoCentering) {
                    item {
                        Spacer(
                            modifier = Modifier.height(state.bottomAutoCenteringPaddingPx.toDp())
                        )
                    }
                }
            }
            if (initialized) {
                LaunchedEffect(state) {
                    state.scrollToInitialItem()
                }
            }
        }
    }
}

/**
 * Contains the default values used by [ScalingLazyColumn]
 */
public object ScalingLazyColumnDefaults {
    /**
     * Creates a [ScalingParams] that represents the scaling and alpha properties for a
     * [ScalingLazyColumn].
     *
     * Items in the ScalingLazyColumn have scaling and alpha effects applied to them depending on
     * their position in the viewport. The closer to the edge (top or bottom) of the viewport that
     * they are the greater the down scaling and transparency that is applied. Note that scaling and
     * transparency effects are applied from the center of the viewport (full size and normal
     * transparency) towards the edge (items can be smaller and more transparent).
     *
     * Deciding how much scaling and alpha to apply is based on the position and size of the item
     * and on a series of properties that are used to determine the transition area for each item.
     *
     * The transition area is defined by the edge of the screen and a transition line which is
     * calculated for each item in the list. The items transition line is based upon its size with
     * the potential for larger list items to start their transition earlier (closer to the center)
     * than smaller items.
     *
     * [minTransitionArea] and [maxTransitionArea] are both in the range [0f..1f] and are
     * the fraction of the distance between the edge of the viewport and the center of
     * the viewport. E.g. a value of 0.2f for minTransitionArea and 0.75f for maxTransitionArea
     * determines that all transition lines will fall between 1/5th (20%) and 3/4s (75%) of the
     * distance between the viewport edge and center.
     *
     * The size of the each item is used to determine where within the transition area range
     * minTransitionArea..maxTransitionArea the actual transition line will be. [minElementHeight]
     * and [maxElementHeight] are used along with the item height (as a fraction of the viewport
     * height in the range [0f..1f]) to find the transition line. So if the items size is 0.25f
     * (25%) of way between minElementSize..maxElementSize then the transition line will be 0.25f
     * (25%) of the way between minTransitionArea..maxTransitionArea.
     *
     * A list item smaller than minElementHeight is rounded up to minElementHeight and larger than
     * maxElementHeight is rounded down to maxElementHeight. Whereabouts the items height sits
     * between minElementHeight..maxElementHeight is then used to determine where the transition
     * line sits between minTransitionArea..maxTransition area.
     *
     * If an item is smaller than or equal to minElementSize its transition line with be at
     * minTransitionArea and if it is larger than or equal to maxElementSize its transition line
     * will be  at maxTransitionArea.
     *
     * For example, if we take the default values for minTransitionArea = 0.2f and
     * maxTransitionArea = 0.6f and minElementSize = 0.2f and maxElementSize= 0.8f then an item
     * with a height of 0.4f (40%) of the viewport height is one third of way between
     * minElementSize and maxElementSize, (0.4f - 0.2f) / (0.8f - 0.2f) = 0.33f. So its transition
     * line would be one third of way between 0.2f and 0.6f, transition line = 0.2f + (0.6f -
     * 0.2f) * 0.33f = 0.33f.
     *
     * Once the position of the transition line is established we now have a transition area
     * for the item, e.g. in the example above the item will start/finish its transitions when it
     * is 0.33f (33%) of the distance from the edge of the viewport and will start/finish its
     * transitions at the viewport edge.
     *
     * The scaleInterpolator is used to determine how much of the scaling and alpha to apply
     * as the item transits through the transition area.
     *
     * The edge of the item furthest from the edge of the screen is used as a scaling trigger
     * point for each item.
     *
     * @param edgeScale What fraction of the full size of the item to scale it by when most
     * scaled, e.g. at the edge of the viewport. A value between [0f,1f], so a value of 0.2f
     * means to scale an item to 20% of its normal size.
     *
     * @param edgeAlpha What fraction of the full transparency of the item to draw it with
     * when closest to the edge of the screen. A value between [0f,1f], so a value of
     * 0.2f means to set the alpha of an item to 20% of its normal value.
     *
     * @param minElementHeight The minimum element height as a ratio of the viewport size to use
     * for determining the transition point within ([minTransitionArea], [maxTransitionArea])
     * that a given content item will start to be transitioned. Items smaller than
     * [minElementHeight] will be treated as if [minElementHeight]. Must be less than or equal to
     * [maxElementHeight].
     *
     * @param maxElementHeight The maximum element height as a ratio of the viewport size to use
     * for determining the transition point within ([minTransitionArea], [maxTransitionArea])
     * that a given content item will start to be transitioned. Items larger than [maxElementHeight]
     * will be treated as if [maxElementHeight]. Must be greater than or equal to
     * [minElementHeight].
     *
     * @param minTransitionArea The lower bound of the transition line area, closest to the
     * edge of the viewport. Defined as a fraction (value between 0f..1f) of the distance between
     * the viewport edge and viewport center line. Must be less than or equal to
     * [maxTransitionArea].
     *
     * @param maxTransitionArea The upper bound of the transition line area, closest to the
     * center of the viewport. The fraction (value between 0f..1f) of the distance
     * between the viewport edge and viewport center line. Must be greater
     * than or equal to [minTransitionArea].
     *
     * @param scaleInterpolator An interpolator to use to determine how to apply scaling as a
     * item transitions across the scaling transition area.
     *
     * @param viewportVerticalOffsetResolver The additional padding to consider above and below the
     * viewport of a [ScalingLazyColumn] when considering which items to draw in the viewport. If
     * set to 0 then no additional padding will be provided and only the items which would appear
     * in the viewport before any scaling is applied will be considered for drawing, this may
     * leave blank space at the top and bottom of the viewport where the next available item
     * could have been drawn once other items have been scaled down in size. The larger this
     * value is set to will allow for more content items to be considered for drawing in the
     * viewport, however there is a performance cost associated with materializing items that are
     * subsequently not drawn. The higher/more extreme the scaling parameters that are applied to
     * the [ScalingLazyColumn] the more padding may be needed to ensure there are always enough
     * content items available to be rendered. By default will be 10% of the maxHeight of the
     * viewport above and below the content.
     */
    fun scalingParams(
        edgeScale: Float = 0.5f,
        edgeAlpha: Float = 0.5f,
        minElementHeight: Float = 0.2f,
        maxElementHeight: Float = 0.8f,
        minTransitionArea: Float = 0.2f,
        maxTransitionArea: Float = 0.6f,
        scaleInterpolator: Easing = CubicBezierEasing(0.25f, 0.00f, 0.75f, 1.00f),
        viewportVerticalOffsetResolver: (Constraints) -> Int = { (it.maxHeight / 10f).toInt() }
    ): ScalingParams = DefaultScalingParams(
        edgeScale = edgeScale,
        edgeAlpha = edgeAlpha,
        minElementHeight = minElementHeight,
        maxElementHeight = maxElementHeight,
        minTransitionArea = minTransitionArea,
        maxTransitionArea = maxTransitionArea,
        scaleInterpolator = scaleInterpolator,
        viewportVerticalOffsetResolver = viewportVerticalOffsetResolver
    )
}

private class ScalingLazyListScopeImpl(
    private val state: ScalingLazyListState,
    private val scope: LazyListScope,
    private val itemScope: ScalingLazyListItemScope
) : ScalingLazyListScope {

    private var currentStartIndex = 0

    override fun item(key: Any?, content: @Composable (ScalingLazyListItemScope.() -> Unit)) {
        val startIndex = currentStartIndex
        scope.item(key = key) {
            ScalingLazyColumnItemWrapper(
                startIndex,
                state,
                itemScope,
                content
            )
        }
        currentStartIndex++
    }

    override fun items(
        count: Int,
        key: ((index: Int) -> Any)?,
        itemContent: @Composable (ScalingLazyListItemScope.(index: Int) -> Unit)
    ) {
        val startIndex = currentStartIndex
        scope.items(count = count, key = key) {
            ScalingLazyColumnItemWrapper(
                startIndex + it,
                state = state,
                itemScope = itemScope
            ) {
                itemContent(it)
            }
        }
        currentStartIndex += count
    }
}

@Composable
private fun ScalingLazyColumnItemWrapper(
    index: Int,
    state: ScalingLazyListState,
    itemScope: ScalingLazyListItemScope,
    content: @Composable (ScalingLazyListItemScope.() -> Unit)
) {
    Box(
        modifier = Modifier.graphicsLayer {
            val reverseLayout = state.reverseLayout.value!!
            val anchorType = state.anchorType.value!!
            val items = state.layoutInfo.visibleItemsInfo
            val currentItem = items.find { it.index == index }
            if (currentItem != null) {
                alpha = currentItem.alpha
                scaleX = currentItem.scale
                scaleY = currentItem.scale
                // Calculate how much to adjust/translate the position of the list item by
                // determining the different between the unadjusted start position based on the
                // underlying LazyList layout and the start position adjusted to take into account
                // scaling of the list items. Items further from the middle of the visible viewport
                // will be subject to more adjustment.
                if (currentItem.scale > 0f) {
                    val offsetAdjust = currentItem.startOffset(anchorType) -
                        currentItem.unadjustedStartOffset(anchorType)
                    translationY = if (reverseLayout) -offsetAdjust else offsetAdjust
                    transformOrigin = TransformOrigin(
                        pivotFractionX = 0.5f,
                        pivotFractionY = if (reverseLayout) 1.0f else 0.0f
                    )
                }
            }
        }
    ) {
        itemScope.content()
    }
}

@Immutable
private class CombinedPaddingValues(
    @Stable
    val contentPadding: PaddingValues,
    @Stable
    val extraPadding: Dp
) : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
        contentPadding.calculateLeftPadding(layoutDirection)

    override fun calculateTopPadding(): Dp =
        contentPadding.calculateTopPadding() + extraPadding

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
        contentPadding.calculateRightPadding(layoutDirection)

    override fun calculateBottomPadding(): Dp =
        contentPadding.calculateBottomPadding() + extraPadding
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as CombinedPaddingValues

        if (contentPadding != other.contentPadding) return false
        if (extraPadding != other.extraPadding) return false

        return true
    }

    override fun hashCode(): Int {
        var result = contentPadding.hashCode()
        result = 31 * result + extraPadding.hashCode()
        return result
    }

    override fun toString(): String {
        return "CombinedPaddingValuesImpl(contentPadding=$contentPadding, " +
            "extraPadding=$extraPadding)"
    }
}

private fun Modifier.verticalNegativePadding(
    extraPadding: Dp
) = layout { measurable, constraints ->
    require(constraints.hasBoundedWidth)
    require(constraints.hasBoundedHeight)
    val placeable = measurable.measure(
        Constraints.fixed(
            width = constraints.maxWidth,
            height = constraints.maxHeight +
                (extraPadding * 2).roundToPx()
        )
    )
    layout(constraints.maxWidth, constraints.maxHeight) {
        placeable.place(0, -extraPadding.roundToPx())
    }
}
