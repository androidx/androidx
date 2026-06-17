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

import androidx.collection.MutableObjectIntMap
import androidx.collection.ObjectIntMap
import androidx.collection.emptyObjectIntMap
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutIntervalContent
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.getDefaultLazyLayoutKey
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.LocalReduceMotion
import androidx.wear.compose.foundation.WearComposeFoundationFlags
import androidx.wear.compose.foundation.lazy.layout.LazyLayoutKeyIndexMap
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import androidx.wear.compose.foundation.rotary.RotaryScrollableBehavior
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable

/**
 * The vertically scrolling list that only composes and lays out the currently visible items. This
 * is a wear specific version of LazyColumn that adds support for scaling and morphing animations.
 *
 * Example of a [TransformingLazyColumn] with default parameters:
 *
 * @sample androidx.wear.compose.foundation.samples.SimpleTransformingLazyColumnSample
 *
 * Example of a [TransformingLazyColumn] that snaps items to the center of the viewport:
 *
 * @sample androidx.wear.compose.foundation.samples.TransformingLazyColumnWithSnapSample
 * @param modifier The modifier to be applied to the layout.
 * @param state The state object to be used to control the list and the applied layout.
 * @param contentPadding The padding around the whole content. This will add padding for the content
 *   after it has been clipped, which is not possible via [modifier] param. You can use it to add
 *   padding before the first item or after the last one. Note that if the first or last item uses
 *   [androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope.minimumVerticalContentPadding],
 *   the effective vertical padding at that edge will be the maximum of the value provided here and
 *   the value calculated by
 *   [androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope.minimumVerticalContentPadding].
 *   This allows enforcing a minimum padding (e.g. for global screen insets) while still allowing
 *   specific items to request larger padding at the screen edge for specific items.
 * @param reverseLayout reverse the direction of scrolling and layout, when `true` items will be
 *   composed from the bottom to the top
 * @param verticalArrangement The vertical arrangement of the items, to be used when there is enough
 *   space to show all the items. Note that only [Arrangement.Top], [Arrangement.Center] and
 *   [Arrangement.Bottom] arrangements (including their spacedBy variants, i.e., using spacedBy with
 *   [Alignment.Top], [Alignment.CenterVertically] and [Alignment.Bottom]) are supported, The
 *   default is [Arrangement.Top] when [reverseLayout] is false and [Arrangement.Bottom] when
 *   [reverseLayout] is true.
 * @param horizontalAlignment The horizontal alignment of the items.
 * @param flingBehavior Logic describing fling behavior for touch scroll. If snapping is required
 *   use [TransformingLazyColumnDefaults.snapFlingBehavior]. Note that when configuring fling or
 *   snap behavior, this flingBehavior parameter and the [rotaryScrollableBehavior] parameter that
 *   controls rotary scroll are expected to produce similar list scrolling. For example, if
 *   [rotaryScrollableBehavior] is set for snap (using [RotaryScrollableDefaults.snapBehavior]),
 *   [flingBehavior] should be set for snap as well (using
 *   [TransformingLazyColumnDefaults.snapFlingBehavior])
 * @param userScrollEnabled Whether the user should be able to scroll the list. This also affects
 *   scrolling with rotary.
 * @param rotaryScrollableBehavior Parameter for changing rotary scrollable behavior. Supports
 *   scroll [RotaryScrollableDefaults.behavior] and snap [RotaryScrollableDefaults.snapBehavior].
 *   Note that when configuring fling or snap behavior, this rotaryBehavior parameter and the
 *   [flingBehavior] parameter that controls touch scroll are expected to produce similar list
 *   scrolling. For example, if [rotaryScrollableBehavior] is set for snap (using
 *   [RotaryScrollableDefaults.snapBehavior]), [flingBehavior] should be set for snap as well (using
 *   [TransformingLazyColumnDefaults.snapFlingBehavior]). Can be null if rotary support is not
 *   required or when it should be handled externally - with a separate [Modifier.rotaryScrollable]
 *   modifier.
 * @param overscrollEffect the [OverscrollEffect] that will be used to render overscroll for this
 *   layout. Note that the [OverscrollEffect.node] will be applied internally as well - you do not
 *   need to use Modifier.overscroll separately.
 * @param content The content of the list.
 */
@OptIn(ExperimentalWearFoundationApi::class)
@Composable
public fun TransformingLazyColumn(
    modifier: Modifier = Modifier,
    state: TransformingLazyColumnState = rememberTransformingLazyColumnState(),
    contentPadding: PaddingValues = PaddingValues(),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        Arrangement.spacedBy(
            space = 4.dp,
            alignment = if (!reverseLayout) Alignment.Top else Alignment.Bottom,
        ),
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    rotaryScrollableBehavior: RotaryScrollableBehavior? = RotaryScrollableDefaults.behavior(state),
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    content: TransformingLazyColumnScope.() -> Unit,
) {
    val graphicsContext = LocalGraphicsContext.current
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val reduceMotionEnabled = LocalReduceMotion.current

    // Use derivedStateOf to ensure remeasure is only triggered when the scroll state *changes*,
    // preventing unnecessary work during an active scroll.
    val isScrollingState = remember { derivedStateOf { state.isScrollInProgress } }
    val measurementStrategy =
        remember(contentPadding, reverseLayout, density) {
            TransformingLazyColumnContentPaddingMeasurementStrategy(
                contentPadding = contentPadding,
                layoutDirection = layoutDirection,
                density = density,
                graphicsContext = graphicsContext,
                itemAnimator = state.animator,
                isScrollInProgress = { isScrollingState.value },
                reverseLayout = reverseLayout,
            )
        }

    val latestContent = rememberUpdatedState(newValue = content)
    val coroutineScope = rememberCoroutineScope()
    val itemProviderLambda by
        remember(state, reduceMotionEnabled) {
            val scope =
                derivedStateOf(referentialEqualityPolicy()) {
                    TransformingLazyColumnScopeImpl(latestContent.value)
                }
            derivedStateOf(referentialEqualityPolicy()) {
                {
                    val intervalContent = scope.value
                    val map = NearestRangeKeyIndexMap(state.nearestRange, intervalContent)
                    TransformingLazyColumnItemProvider(
                        intervalContent = intervalContent,
                        state = state,
                        keyIndexMap = map,
                        reduceMotionEnabled,
                    )
                }
            }
        }

    val measurePolicy =
        rememberTransformingLazyColumnMeasurePolicy(
            itemProviderLambda = itemProviderLambda,
            state = state,
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = verticalArrangement,
            measurementStrategy = measurementStrategy,
            coroutineScope = coroutineScope,
            reverseLayout = reverseLayout,
        )
    val reverseDirection =
        ScrollableDefaults.reverseDirection(
            LocalLayoutDirection.current,
            Orientation.Vertical,
            reverseScrolling = reverseLayout,
        )

    val semanticState = remember(state) { TransformingLazyColumnSemanticState(state) }
    val focusRequester = remember { FocusRequester() }
    val minimumHeightPx = remember { with(density) { VISIBLE_THRESHOLD_EDGE_ITEM.toPx() } }
    LazyLayout(
        itemProvider = itemProviderLambda,
        modifier =
            modifier
                .then(state.awaitLayoutModifier)
                .then(state.remeasurementModifier)
                .then(state.animator.modifier)
                .then(
                    if (rotaryScrollableBehavior != null && userScrollEnabled)
                        Modifier.requestFocusOnHierarchyActive()
                            .rotaryScrollable(
                                behavior = rotaryScrollableBehavior,
                                focusRequester = focusRequester,
                                overscrollEffect = overscrollEffect,
                                reverseDirection = reverseLayout,
                            )
                    else Modifier
                )
                .lazyLayoutSemantics(
                    itemProviderLambda = itemProviderLambda,
                    state = semanticState,
                    orientation = Orientation.Vertical,
                    userScrollEnabled = userScrollEnabled,
                    reverseScrolling = reverseLayout,
                )
                .overscroll(overscrollEffect)
                .scrollable(
                    state = state,
                    reverseDirection = reverseDirection,
                    enabled = userScrollEnabled,
                    orientation = Orientation.Vertical,
                    flingBehavior = flingBehavior,
                    overscrollEffect = overscrollEffect,
                )
                .then(
                    if (
                        WearComposeFoundationFlags.isTransformingLazyColumnClickableThresholdEnabled
                    ) {
                        Modifier.pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val startPosition = event.changes.first().position

                                    // Wait for up event
                                    var upEvent: PointerInputChange? = null
                                    while (upEvent == null) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val change = event.changes.first()

                                        if (change.changedToUp()) {
                                            upEvent = change
                                        }
                                    }

                                    val pointerDistance =
                                        (upEvent.position - startPosition).getDistance()
                                    // Check if pointer's drag distance is smaller than touch slop
                                    // and there is any item in edge item that smaller than
                                    // threshold when pointer leaves screen then consume it.
                                    if (
                                        pointerDistance < viewConfiguration.touchSlop &&
                                            !state.isItemClickableAt(startPosition, minimumHeightPx)
                                    ) {
                                        upEvent.consume()
                                    }
                                }
                            }
                        }
                    } else {
                        Modifier
                    }
                ),
        measurePolicy = measurePolicy,
        prefetchState = state.prefetchState,
    )
}

/**
 * The vertically scrolling list that only composes and lays out the currently visible items. This
 * is a wear specific version of LazyColumn that adds support for scaling and morphing animations.
 *
 * Example of a [TransformingLazyColumn] with default parameters:
 *
 * @sample androidx.wear.compose.foundation.samples.SimpleTransformingLazyColumnSample
 *
 * Example of a [TransformingLazyColumn] that snaps items to the center of the viewport:
 *
 * @sample androidx.wear.compose.foundation.samples.TransformingLazyColumnWithSnapSample
 * @param modifier The modifier to be applied to the layout.
 * @param state The state object to be used to control the list and the applied layout.
 * @param contentPadding The padding around the whole content. This will add padding for the content
 *   after it has been clipped, which is not possible via [modifier] param. You can use it to add
 *   padding before the first item or after the last one. Note that if the first or last item uses
 *   [androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope.minimumVerticalContentPadding],
 *   the effective vertical padding at that edge will be the maximum of the value provided here and
 *   the value calculated by
 *   [androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope.minimumVerticalContentPadding].
 *   This allows enforcing a minimum padding (e.g. for global screen insets) while still allowing
 *   specific items to request larger padding at the screen edge for specific items.
 * @param verticalArrangement The vertical arrangement of the items.
 * @param horizontalAlignment The horizontal alignment of the items.
 * @param flingBehavior Logic describing fling behavior for touch scroll. If snapping is required
 *   use [TransformingLazyColumnDefaults.snapFlingBehavior]. Note that when configuring fling or
 *   snap behavior, this flingBehavior parameter and the [rotaryScrollableBehavior] parameter that
 *   controls rotary scroll are expected to produce similar list scrolling. For example, if
 *   [rotaryScrollableBehavior] is set for snap (using [RotaryScrollableDefaults.snapBehavior]),
 *   [flingBehavior] should be set for snap as well (using
 *   [TransformingLazyColumnDefaults.snapFlingBehavior])
 * @param userScrollEnabled Whether the user should be able to scroll the list. This also affects
 *   scrolling with rotary.
 * @param rotaryScrollableBehavior Parameter for changing rotary scrollable behavior. Supports
 *   scroll [RotaryScrollableDefaults.behavior] and snap [RotaryScrollableDefaults.snapBehavior].
 *   Note that when configuring fling or snap behavior, this rotaryBehavior parameter and the
 *   [flingBehavior] parameter that controls touch scroll are expected to produce similar list
 *   scrolling. For example, if [rotaryScrollableBehavior] is set for snap (using
 *   [RotaryScrollableDefaults.snapBehavior]), [flingBehavior] should be set for snap as well (using
 *   [TransformingLazyColumnDefaults.snapFlingBehavior]). Can be null if rotary support is not
 *   required or when it should be handled externally - with a separate [Modifier.rotaryScrollable]
 *   modifier.
 * @param overscrollEffect the [OverscrollEffect] that will be used to render overscroll for this
 *   layout. Note that the [OverscrollEffect.node] will be applied internally as well - you do not
 *   need to use Modifier.overscroll separately.
 * @param content The content of the list.
 */
@Deprecated(
    "This overload is deprecated. Please use the new overload with the reverseLayout parameter.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
public fun TransformingLazyColumn(
    modifier: Modifier = Modifier,
    state: TransformingLazyColumnState = rememberTransformingLazyColumnState(),
    contentPadding: PaddingValues = PaddingValues(),
    verticalArrangement: Arrangement.Vertical =
        Arrangement.spacedBy(space = 4.dp, alignment = Alignment.Top),
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    rotaryScrollableBehavior: RotaryScrollableBehavior? = RotaryScrollableDefaults.behavior(state),
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    content: TransformingLazyColumnScope.() -> Unit,
) {
    TransformingLazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        // Forward to the new overload with the default value for reverseLayout.
        reverseLayout = false,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        rotaryScrollableBehavior = rotaryScrollableBehavior,
        overscrollEffect = overscrollEffect,
        content = content,
    )
}

/** Contains the default values used by [TransformingLazyColumn] */
public object TransformingLazyColumnDefaults {

    /**
     * Create and remember a [FlingBehavior] that will represent natural fling curve with snap to
     * central item as the fling decays.
     *
     * @param state the state of the [TransformingLazyColumn]
     * @param snapOffset an optional offset to be applied when snapping the item. Defines the
     *   distance from the center of the scrollable to the center of the snapped item.
     * @param decay the decay to use
     */
    @Composable
    public fun snapFlingBehavior(
        state: TransformingLazyColumnState,
        snapOffset: Dp = 0.dp,
        decay: DecayAnimationSpec<Float> = exponentialDecay(),
    ): FlingBehavior {
        val snapOffsetPx = with(LocalDensity.current) { snapOffset.roundToPx() }
        return remember(state, snapOffsetPx, decay) {
            TransformingLazyColumnSnapFlingBehavior(
                state = state,
                snapOffset = snapOffsetPx,
                decay = decay,
            )
        }
    }
}

internal class TransformingLazyColumnItemProvider(
    val intervalContent: LazyLayoutIntervalContent<TransformingLazyColumnInterval>,
    val state: TransformingLazyColumnState,
    val keyIndexMap: NearestRangeKeyIndexMap,
    val reduceMotionEnabled: Boolean,
) : LazyLayoutItemProvider {
    override val itemCount: Int
        get() = intervalContent.itemCount

    @Composable
    override fun Item(index: Int, key: Any) {
        val itemScope =
            remember(index, reduceMotionEnabled) {
                TransformingLazyColumnItemScopeImpl(
                    index,
                    state = state,
                    reduceMotionEnabled = reduceMotionEnabled,
                )
            }
        intervalContent.withInterval(index) { localIndex, content ->
            content.item(itemScope, localIndex)
        }
    }

    override fun getKey(index: Int): Any =
        keyIndexMap.getKey(index) ?: intervalContent.getKey(index)

    override fun getContentType(index: Int): Any? = intervalContent.getContentType(index)

    override fun getIndex(key: Any): Int = keyIndexMap.getIndex(key)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TransformingLazyColumnItemProvider) return false

        // the identity of this class is represented by intervalContent object.
        // having equals() allows us to skip items recomposition when intervalContent didn't change
        return intervalContent == other.intervalContent
    }

    override fun hashCode(): Int {
        return intervalContent.hashCode()
    }
}

internal class NearestRangeKeyIndexMap(
    nearestRange: IntRange,
    intervalContent: LazyLayoutIntervalContent<*>,
) : LazyLayoutKeyIndexMap {
    private val map: ObjectIntMap<Any>
    private val keys: Array<Any?>
    private val keysStartIndex: Int

    init {
        // Traverses the interval [list] in order to create a mapping from the key to the index for
        // all the indexes in the passed [range].
        val list = intervalContent.intervals
        val first = nearestRange.first
        val last = minOf(nearestRange.last, list.size - 1)
        if (last < first) {
            map = emptyObjectIntMap()
            keys = emptyArray()
            keysStartIndex = 0
        } else {
            val size = last - first + 1
            keys = arrayOfNulls<Any?>(size)
            keysStartIndex = first
            map =
                MutableObjectIntMap<Any>(size).also { map ->
                    list.forEach(fromIndex = first, toIndex = last) {
                        val keyFactory = it.value.key
                        val start = maxOf(first, it.startIndex)
                        val end = minOf(last, it.startIndex + it.size - 1)
                        for (i in start..end) {
                            val key =
                                // TODO: Use getDefaultLazyLayoutKey
                                keyFactory?.invoke(i - it.startIndex) ?: getDefaultLazyLayoutKey(i)
                            map[key] = i
                            keys[i - keysStartIndex] = key
                        }
                    }
                }
        }
    }

    override fun getIndex(key: Any): Int = map.getOrElse(key) { -1 }

    override fun getKey(index: Int) = keys.getOrElse(index - keysStartIndex) { null }
}

private fun TransformingLazyColumnState.isItemClickableAt(
    position: Offset,
    minimumHeightPx: Float,
): Boolean {
    // 1. Check if click event is on edge item
    val edgeItems =
        layoutInfo.visibleItems.let { items ->
            if (items.size > 1) {
                listOf(items.first(), items.last())
            } else {
                items
            }
        }
    val foundItem =
        edgeItems.fastFirstOrNull { info ->
            info.offset <= position.y && position.y <= info.offset + info.transformedHeight
        }
    // 2. Check if found item has visible area that is big enough. If click is not on edge items,
    // the function will return true since the visible check should be done only on edge items and
    // other items are considered clickable.
    return foundItem?.let {
        return if (it.offset > 0) {
            it.offset + minimumHeightPx <= layoutInfo.viewportSize.height &&
                it.transformedHeight >= minimumHeightPx
        } else {
            // Item was clipped at upper bound
            it.transformedHeight + it.offset >= minimumHeightPx
        }
    } ?: true
}

// Minimum visible height of edge item to be eligible for click.
private val VISIBLE_THRESHOLD_EDGE_ITEM = 20.dp
