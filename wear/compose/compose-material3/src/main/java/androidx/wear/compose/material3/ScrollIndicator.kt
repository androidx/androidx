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

package androidx.wear.compose.material3

import androidx.annotation.FloatRange
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.ScalingLazyListItemInfo
import androidx.wear.compose.foundation.lazy.ScalingLazyListLayoutInfo
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnLayoutInfo
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.inverseLerp
import androidx.wear.compose.material3.ScrollIndicatorDefaults.maxSizeFraction
import androidx.wear.compose.material3.ScrollIndicatorDefaults.minSizeFraction
import androidx.wear.compose.materialcore.isRoundDevice
import kotlin.math.asin
import kotlin.math.max
import kotlin.math.sqrt
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * A composable that displays a visual indicator of scrolling progress within a scrollable
 * container.
 *
 * Creates a [ScrollIndicator] based on the values in a [ScrollState] object. e.g. a [Column]
 * implementing [Modifier.verticalScroll] provides a [ScrollState].
 *
 * To comply with Wear Material Design guidelines, this composable should be aligned to the center
 * end of the screen using `Alignment.CenterEnd`, such as by setting `modifier =
 * Modifier.align(Alignment.CenterEnd)`. This way, the [ScrollIndicator] will appear on the right in
 * Ltr orientation and on the left in Rtl orientation.
 *
 * It detects if the screen is round or square and draws itself as a curve or line.
 *
 * For more information, see the
 * [Scroll indicators](https://developer.android.com/training/wearables/components/scroll) guide.
 *
 * Example of a sample ScrollIndicator with Column:
 *
 * @sample androidx.wear.compose.material3.samples.ScrollIndicatorWithColumnSample
 * @param state The scrollState to use as the basis for the ScrollIndicatorState.
 * @param modifier The modifier to be applied to the component - usually set to
 *   `Modifier.align(Alignment.CenterEnd)`.
 * @param reverseDirection Reverses direction of ScrollIndicator if true
 * @param positionAnimationSpec [AnimationSpec] for position animation. The Position animation is
 *   used for animating changes to the scroll size and position. To disable this animation [snap]
 *   AnimationSpec should be passed instead.
 */
@Composable
fun ScrollIndicator(
    state: ScrollState,
    modifier: Modifier = Modifier,
    reverseDirection: Boolean = false,
    positionAnimationSpec: AnimationSpec<Float> = ScrollIndicatorDefaults.PositionAnimationSpec
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    IndicatorImpl(
        remember { ScrollStateAdapter(state) { containerSize } },
        indicatorHeight = ScrollIndicatorDefaults.indicatorHeight,
        indicatorWidth = ScrollIndicatorDefaults.indicatorWidth,
        paddingHorizontal = ScrollIndicatorDefaults.edgePadding,
        modifier = modifier.onSizeChanged { containerSize = it },
        reverseDirection = reverseDirection,
        positionAnimationSpec = positionAnimationSpec
    )
}

/**
 * A composable that displays a visual indicator of scrolling progress within a scrollable
 * container.
 *
 * Creates an [ScrollIndicator] based on the values in a [ScalingLazyListState] object that a
 * [ScalingLazyColumn] uses.
 *
 * Typically used with the [ScreenScaffold] but can be used to decorate any full screen situation.
 *
 * To comply with Wear Material Design guidelines, this composable should be aligned to the center
 * end of the screen using `Alignment.CenterEnd`. It will appear on the right in Ltr orientation and
 * on the left in Rtl orientation.
 *
 * It detects if the screen is round or square and draws itself as a curve or line.
 *
 * For more information, see the
 * [Scroll indicators](https://developer.android.com/training/wearables/components/scroll) guide.
 *
 * Example of a sample ScrollIndicator with LazyColumn:
 *
 * @sample androidx.wear.compose.material3.samples.ScrollIndicatorWithSLCSample
 * @param state the [ScalingLazyListState] to use as the basis for the ScrollIndicatorState.
 * @param modifier The modifier to be applied to the component
 * @param reverseDirection Reverses direction of ScrollIndicator if true
 * @param positionAnimationSpec [AnimationSpec] for position animation. The Position animation is
 *   used for animating changes to the scroll size and position. To disable this animation [snap]
 *   AnimationSpec should be passed instead.
 */
@Composable
fun ScrollIndicator(
    state: ScalingLazyListState,
    modifier: Modifier = Modifier,
    reverseDirection: Boolean = false,
    positionAnimationSpec: AnimationSpec<Float> = ScrollIndicatorDefaults.PositionAnimationSpec
) =
    IndicatorImpl(
        state = ScalingLazyColumnStateAdapter(state = state),
        indicatorHeight = ScrollIndicatorDefaults.indicatorHeight,
        indicatorWidth = ScrollIndicatorDefaults.indicatorWidth,
        paddingHorizontal = ScrollIndicatorDefaults.edgePadding,
        modifier = modifier,
        reverseDirection = reverseDirection,
        positionAnimationSpec = positionAnimationSpec
    )

@Composable
fun ScrollIndicator(
    state: TransformingLazyColumnState,
    modifier: Modifier = Modifier,
    reverseDirection: Boolean = false,
    positionAnimationSpec: AnimationSpec<Float> = ScrollIndicatorDefaults.PositionAnimationSpec
) =
    IndicatorImpl(
        state = TransformingLazyColumnStateAdapter(state = state),
        indicatorHeight = ScrollIndicatorDefaults.indicatorHeight,
        indicatorWidth = ScrollIndicatorDefaults.indicatorWidth,
        paddingHorizontal = ScrollIndicatorDefaults.edgePadding,
        modifier = modifier,
        reverseDirection = reverseDirection,
        positionAnimationSpec = positionAnimationSpec
    )

/**
 * A composable that displays a visual indicator of scrolling progress within a scrollable
 * container.
 *
 * Creates an [ScrollIndicator] based on the values in a [LazyListState] object that a [LazyColumn]
 * uses.
 *
 * To comply with Wear Material Design guidelines, this composable should be aligned to the center
 * end of the screen using `Alignment.CenterEnd`. It will appear on the right in Ltr orientation and
 * on the left in Rtl orientation.
 *
 * It detects if the screen is round or square and draws itself as a curve or line.
 *
 * For more information, see the
 * [Scroll indicators](https://developer.android.com/training/wearables/components/scroll) guide.
 *
 * Example of a sample ScrollIndicator with LazyColumn:
 *
 * @sample androidx.wear.compose.material3.samples.ScrollIndicatorWithLCSample
 * @param state the [LazyListState] to use as the basis for the ScrollIndicatorState.
 * @param modifier The modifier to be applied to the component
 * @param reverseDirection Reverses direction of ScrollIndicator if true
 * @param positionAnimationSpec [AnimationSpec] for position animation. The Position animation is
 *   used for animating changes to the scroll size and position. To disable this animation [snap]
 *   AnimationSpec should be passed instead.
 */
@Composable
fun ScrollIndicator(
    state: LazyListState,
    modifier: Modifier = Modifier,
    reverseDirection: Boolean = false,
    positionAnimationSpec: AnimationSpec<Float> = ScrollIndicatorDefaults.PositionAnimationSpec
) =
    IndicatorImpl(
        state = LazyColumnStateAdapter(state = state),
        indicatorHeight = ScrollIndicatorDefaults.indicatorHeight,
        indicatorWidth = ScrollIndicatorDefaults.indicatorWidth,
        paddingHorizontal = ScrollIndicatorDefaults.edgePadding,
        modifier = modifier,
        reverseDirection = reverseDirection,
        positionAnimationSpec = positionAnimationSpec
    )

/** Contains the default values used for [ScrollIndicator]. */
object ScrollIndicatorDefaults {
    /**
     * [AnimationSpec] used for position animation. To disable this animation, pass [snap]
     * AnimationSpec instead
     */
    val PositionAnimationSpec: AnimationSpec<Float> =
        tween(durationMillis = 500, easing = CubicBezierEasing(0f, 0f, 0f, 1f))

    internal const val minSizeFraction = 0.2f
    internal const val maxSizeFraction = 0.8f

    internal val indicatorHeight = 50.dp
    internal val indicatorWidth = 4.dp
    internal val edgePadding = PaddingDefaults.edgePadding
}

/**
 * An object representing the relative position of a scrollbar. This interface is implemented by
 * classes that adapt other state information such as [ScalingLazyListState] or [ScrollState] of
 * scrollable containers.
 *
 * Implementing classes provide [positionFraction] to determine where in the range [0..1] that the
 * indicator should be displayed and [sizeFraction] to determine the size of the indicator in the
 * range [0..1]. E.g. If a [ScalingLazyListState] had 50 items and the last 5 were visible it would
 * have a position of 1.0f to show that the scroll is positioned at the end of the list and a size
 * of 5 / 50 = 0.1f to indicate that 10% of the visible items are currently visible.
 */
@Stable
internal interface IndicatorState {
    /**
     * Position of the indicator in the range [0f,1f]. 0f means it is at the top|start, 1f means it
     * is positioned at the bottom|end.
     */
    @get:FloatRange(from = 0.0, to = 1.0) val positionFraction: Float

    /** Size of the indicator in the range [0f,1f]. 1f means it takes the whole space. */
    @get:FloatRange(from = 0.0, to = 1.0) val sizeFraction: Float
}

/**
 * An indicator on one side of the screen to show the current [IndicatorState].
 *
 * Typically used with the [ScreenScaffold] but can be used to decorate any full screen situation.
 *
 * This composable should only be used to fill the whole screen as Wear Material Design language
 * requires the placement of the position indicator to be right center of the screen as the
 * indicator is curved on circular devices.
 *
 * It detects if the screen is round or square and draws itself as a curve or line.
 *
 * Note that the composable will take the whole screen, but it will be drawn with the given
 * dimensions [indicatorHeight] and [indicatorWidth], and position with respect to the edge of the
 * screen according to [paddingHorizontal]
 *
 * For more information, see the
 * [Scroll indicators](https://developer.android.com/training/wearables/components/scroll) guide.
 *
 * @param state the [IndicatorState] of the state we are displaying.
 * @param indicatorHeight the height of the position indicator in Dp.
 * @param indicatorWidth the width of the position indicator in Dp.
 * @param paddingHorizontal the padding to apply between the indicator and the border of the screen.
 * @param modifier The modifier to be applied to the component.
 * @param background the color to draw the non-active part of the position indicator.
 * @param color the color to draw the active part of the indicator in.
 * @param reverseDirection Reverses direction of ScrollIndicator if true.
 * @param positionAnimationSpec [AnimationSpec] for position animation. The Position animation is
 *   used for animating changes to the scroll size and position. To disable this animation [snap]
 *   AnimationSpec should be passed instead.
 */
@Composable
internal fun IndicatorImpl(
    state: IndicatorState,
    indicatorHeight: Dp,
    indicatorWidth: Dp,
    paddingHorizontal: Dp,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
    color: Color = MaterialTheme.colorScheme.onBackground,
    reverseDirection: Boolean = false,
    rsbSide: Boolean = true,
    positionAnimationSpec: AnimationSpec<Float> = ScrollIndicatorDefaults.PositionAnimationSpec
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp

    val isScreenRound = isRoundDevice()
    val layoutDirection = LocalLayoutDirection.current

    val positionFractionAnimatable = remember { Animatable(0f) }
    val sizeFractionAnimatable = remember { Animatable(0f) }

    // TODO(b/360358568) - consider taking lefty-mode into account for orientation.
    val indicatorOnTheRight =
        if (rsbSide) layoutDirection == LayoutDirection.Ltr
        else layoutDirection == LayoutDirection.Rtl

    val size: () -> DpSize = {
        // radius is the distance from the center of the container to the arc we draw the
        // position indicator on (the center of the arc, which is indicatorWidth wide).
        val radius = screenWidthDp.value / 2 - paddingHorizontal.value - indicatorWidth.value / 2
        val width =
            (if (isScreenRound) {
                // The sqrt is the size of the projection on the x axis of line between center of
                // the container and the point where we start the arc.
                // The coerceAtLeast is needed while initializing since containerSize.width is 0
                radius - sqrt((sqr(radius) - sqr(indicatorHeight.value / 2)).coerceAtLeast(0f))
            } else 0f) + paddingHorizontal.value + indicatorWidth.value

        val height = indicatorHeight.value + indicatorWidth.value

        DpSize(width.dp, height.dp)
    }

    val updatedPositionAnimationSpec by rememberUpdatedState(positionAnimationSpec)

    LaunchedEffect(state) {
        // We don't want to trigger first animation when we receive position or size
        // for the first time, because initial position and size are equal to 0.
        var skipFirstPositionAnimation = true
        var skipUninitialisedData = true

        launch {
            // This snapshotFlow listens to changes in position, size and visibility
            // of ScrollIndicatorState and starts necessary animations if needed
            snapshotFlow {
                    DisplayState(
                        state.positionFraction,
                        state.sizeFraction,
                    )
                }
                .collectLatest {
                    // Workaround for b/315149417. When position and height are equal to 0,
                    // we consider that as non-initialized state.
                    // It means that we skip first alpha animation, and also ignore these values.
                    if (skipUninitialisedData && it.size == 0.0f && it.position == 0.0f) {
                        skipUninitialisedData = false
                        return@collectLatest
                    }
                    if (skipFirstPositionAnimation || updatedPositionAnimationSpec is SnapSpec) {
                        sizeFractionAnimatable.snapTo(it.size)
                        positionFractionAnimatable.snapTo(it.position)
                        skipFirstPositionAnimation = false
                    } else {
                        launch {
                            sizeFractionAnimatable.animateTo(
                                it.size,
                                animationSpec = updatedPositionAnimationSpec
                            )
                        }
                        launch {
                            positionFractionAnimatable.animateTo(
                                it.position,
                                animationSpec = updatedPositionAnimationSpec
                            )
                        }
                    }
                }
        }
    }
    Box(
        modifier =
            modifier.size(size()).drawWithCache {
                // We need to invert reverseDirection when the screen is round and we are on
                // the left.
                val actualReverseDirection =
                    if (isScreenRound && !indicatorOnTheRight) {
                        !reverseDirection
                    } else {
                        reverseDirection
                    }

                val indicatorPosition =
                    if (actualReverseDirection) {
                        1 - positionFractionAnimatable.value
                    } else {
                        positionFractionAnimatable.value
                    }

                val indicatorWidthPx = indicatorWidth.toPx()

                // We want position = 0 be the indicator aligned at the top of its area and
                // position = 1 be aligned at the bottom of the area.
                val indicatorStart = indicatorPosition * (1 - sizeFractionAnimatable.value)

                val paddingHorizontalPx = paddingHorizontal.toPx()
                onDrawWithContent {
                    if (isScreenRound) {
                        val gapHeight = 2.dp

                        drawCurvedIndicator(
                            screenWidthDp.toPx(),
                            color,
                            background,
                            paddingHorizontalPx,
                            indicatorOnTheRight,
                            indicatorHeight,
                            gapHeight,
                            indicatorWidthPx,
                            indicatorStart,
                            sizeFractionAnimatable.value,
                        )
                    } else {
                        drawStraightIndicator(
                            color,
                            background,
                            paddingHorizontalPx,
                            indicatorOnTheRight,
                            indicatorWidthPx,
                            indicatorHeight.toPx(),
                            indicatorStart,
                            sizeFractionAnimatable.value,
                        )
                    }
                }
            }
    )
}

@Immutable
internal class DisplayState(
    val position: Float,
    val size: Float,
) {
    override fun hashCode(): Int {
        var result = position.hashCode()
        result = 31 * result + size.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as DisplayState

        if (position != other.position) return false
        if (size != other.size) return false

        return true
    }
}

/**
 * An implementation of [IndicatorState] to display the amount and position of a component
 * implementing the [ScrollState] class such as a [Column] implementing [Modifier.verticalScroll].
 *
 * @param scrollState the [ScrollState] to adapt
 * @VisibleForTesting
 */
internal class ScrollStateAdapter(
    private val scrollState: ScrollState,
    private val scrollableContainerSize: () -> IntSize
) : IndicatorState {

    override val positionFraction: Float
        get() {
            return if (scrollState.maxValue == 0) {
                0f
            } else {
                scrollState.value.toFloat() / scrollState.maxValue
            }
        }

    override val sizeFraction: Float
        get() {
            val scrollableContainerSizePx = scrollableContainerSize().height.toFloat()
            return if (scrollableContainerSizePx + scrollState.maxValue == 0.0f) {
                maxSizeFraction
            } else {
                (scrollableContainerSizePx / (scrollableContainerSizePx + scrollState.maxValue))
                    .coerceIn(minSizeFraction, maxSizeFraction)
            }
        }

    override fun equals(other: Any?): Boolean {
        return (other as? ScrollStateAdapter)?.scrollState == scrollState
    }

    override fun hashCode(): Int {
        return scrollState.hashCode()
    }
}

/**
 * An implementation of [IndicatorState] to display the amount and position of a [ScalingLazyColumn]
 * component via its [ScalingLazyListState].
 *
 * Note that size and position calculations ignore spacing between list items both for determining
 * the number and the number of visible items.
 *
 * @param state the [ScalingLazyListState] to adapt.
 * @VisibleForTesting
 */
internal class ScalingLazyColumnStateAdapter(private val state: ScalingLazyListState) :
    IndicatorState {
    private var currentSizeFraction: Float = 0f
    private var previousItemsCount: Int = 0

    // TODO: b/368270238 - Fix calculation on a small content size.
    override val positionFraction: Float
        get() {
            val layoutInfo: ScalingLazyListLayoutInfo = state.layoutInfo
            return if (layoutInfo.visibleItemsInfo.isEmpty()) {
                0.0f
            } else {
                val decimalFirstItemIndex = decimalFirstItemIndex(layoutInfo)
                val decimalLastItemIndex = decimalLastItemIndex(layoutInfo)
                val decimalLastItemIndexDistanceFromEnd =
                    layoutInfo.totalItemsCount - decimalLastItemIndex

                if (decimalFirstItemIndex + decimalLastItemIndexDistanceFromEnd == 0.0f) {
                    0.0f
                } else {
                    decimalFirstItemIndex /
                        (decimalFirstItemIndex + decimalLastItemIndexDistanceFromEnd)
                }
            }
        }

    // Represents the fraction of total items currently visible within the ScalingLazyColumn.
    // Initially calculated based on the visible items and updated only when the total number
    // of items in the ScalingLazyColumn changes.
    override val sizeFraction: Float
        get() {
            val layoutInfo: ScalingLazyListLayoutInfo = state.layoutInfo

            // Workaround for b/315149417 with first iteration of SLC when visibleItemsInfo is empty
            if (layoutInfo.visibleItemsInfo.isEmpty()) return 0.0f

            if (previousItemsCount != layoutInfo.totalItemsCount) {
                previousItemsCount = layoutInfo.totalItemsCount
                val decimalFirstItemIndex = decimalFirstItemIndex(layoutInfo)
                val decimalLastItemIndex = decimalLastItemIndex(layoutInfo)

                currentSizeFraction =
                    ((decimalLastItemIndex - decimalFirstItemIndex) /
                            layoutInfo.totalItemsCount.toFloat())
                        .coerceIn(minSizeFraction, maxSizeFraction)
            }
            return currentSizeFraction
        }

    override fun hashCode(): Int {
        return state.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return (other as? ScalingLazyColumnStateAdapter)?.state == state
    }

    /**
     * Provide a float value that represents the index of the last visible list item in a scaling
     * lazy column. The value should be in the range from [n,n+1] for a given index n, where n is
     * the index of the last visible item and a value of n represents that only the very start|top
     * of the item is visible, and n+1 means that whole of the item is visible in the viewport.
     *
     * Note that decimal index calculations ignore spacing between list items both for determining
     * the number and the number of visible items.
     */
    private fun decimalLastItemIndex(layoutInfo: ScalingLazyListLayoutInfo): Float {
        if (layoutInfo.visibleItemsInfo.isEmpty()) return 0f
        val lastItem = layoutInfo.visibleItemsInfo.last()
        // This is the offset of the last item w.r.t. the ScalingLazyColumn coordinate system where
        // 0 in the center of the visible viewport and +/-(state.viewportHeightPx / 2f) are the
        // start and end of the viewport.
        //
        // Note that [ScalingLazyListAnchorType] determines how the list items are anchored to the
        // center of the viewport, it does not change viewport coordinates. As a result this
        // calculation needs to take the anchorType into account to calculate the correct end
        // of list item offset.
        val lastItemEndOffset = lastItem.startOffset(layoutInfo.anchorType) + lastItem.size
        val viewportEndOffset = layoutInfo.viewportSize.height / 2f
        // Coerce item size to at least 1 to avoid divide by zero for zero height items
        val lastItemVisibleFraction =
            (1f - ((lastItemEndOffset - viewportEndOffset) / lastItem.size.coerceAtLeast(1)))
                .coerceAtMost(1f)

        return lastItem.index.toFloat() + lastItemVisibleFraction
    }

    /**
     * Provide a float value that represents the index of first visible list item in a scaling lazy
     * column. The value should be in the range from [n,n+1] for a given index n, where n is the
     * index of the first visible item and a value of n represents that all of the item is visible
     * in the viewport and a value of n+1 means that only the very end|bottom of the list item is
     * visible at the start|top of the viewport.
     *
     * Note that decimal index calculations ignore spacing between list items both for determining
     * the number and the number of visible items.
     */
    private fun decimalFirstItemIndex(layoutInfo: ScalingLazyListLayoutInfo): Float {
        if (layoutInfo.visibleItemsInfo.isEmpty()) return 0f
        val firstItem = layoutInfo.visibleItemsInfo.first()
        val firstItemStartOffset = firstItem.startOffset(layoutInfo.anchorType)
        val viewportStartOffset = -(layoutInfo.viewportSize.height / 2f)
        // Coerce item size to at least 1 to avoid divide by zero for zero height items
        val firstItemInvisibleFraction =
            ((viewportStartOffset - firstItemStartOffset) / firstItem.size.coerceAtLeast(1))
                .coerceAtLeast(0f)

        return firstItem.index.toFloat() + firstItemInvisibleFraction
    }
}

/**
 * An implementation of [IndicatorState] to display the amount and position of a
 * [TransformingLazyColumn] component via its [TransformingLazyColumnState].
 *
 * @param state the [TransformingLazyColumnState] to adapt.
 * @VisibleForTesting
 */
internal class TransformingLazyColumnStateAdapter(private val state: TransformingLazyColumnState) :
    IndicatorState {
    private var latestSizeFraction: Float = 0f
    private var previousItemsCount: Int = 0

    // TODO: b/368270238 - Fix calculation on a small content size.
    override val positionFraction: Float
        get() =
            with(state.layoutInfo) {
                if (visibleItems.isEmpty()) {
                    0f
                } else {
                    val decimalFirstItemIndex = decimalFirstItemIndex()
                    val decimalLastItemIndex = decimalLastItemIndex()

                    val decimalLastItemIndexDistanceFromEnd = totalItemsCount - decimalLastItemIndex

                    if (decimalFirstItemIndex + decimalLastItemIndexDistanceFromEnd == 0.0f) {
                        0.0f
                    } else {
                        decimalFirstItemIndex /
                            (decimalFirstItemIndex + decimalLastItemIndexDistanceFromEnd)
                    }
                }
            }

    // Represents the fraction of total items currently visible within the LazyColumn.
    // Initially calculated based on the visible items and updated only when the total number
    // of items in the LazyColumn changes.
    override val sizeFraction: Float
        get() =
            with(state.layoutInfo) {
                if (totalItemsCount == 0) return@with 0.0f

                if (previousItemsCount != totalItemsCount) {
                    previousItemsCount = totalItemsCount
                    val decimalFirstItemIndex = decimalFirstItemIndex()
                    val decimalLastItemIndex = decimalLastItemIndex()

                    latestSizeFraction =
                        ((decimalLastItemIndex - decimalFirstItemIndex) / totalItemsCount.toFloat())
                            .coerceIn(minSizeFraction, maxSizeFraction)
                }
                return@with latestSizeFraction
            }

    override fun hashCode(): Int {
        return state.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return (other as? TransformingLazyColumnStateAdapter)?.state == state
    }

    private fun TransformingLazyColumnLayoutInfo.decimalLastItemIndex(): Float =
        visibleItems.lastOrNull()?.let { lastItem ->
            // Coerce item sizes to at least 1 to avoid divide by zero for zero height items.
            val lastItemVisibleSize =
                (viewportSize.height - lastItem.offset)
                    .coerceAtMost(lastItem.transformedHeight)
                    .coerceAtLeast(0)
            return lastItem.index.toFloat() +
                lastItemVisibleSize.toFloat() /
                    lastItem.transformedHeight.coerceAtLeast(1).toFloat()
        } ?: 0f

    private fun TransformingLazyColumnLayoutInfo.decimalFirstItemIndex(): Float =
        visibleItems.firstOrNull()?.let { firstItem ->
            // Coerce item size to at least 1 to avoid divide by zero for zero height items.
            return firstItem.index.toFloat() -
                firstItem.offset.coerceAtMost(0).toFloat() /
                    firstItem.transformedHeight.coerceAtLeast(1).toFloat()
        } ?: 0f
}

/**
 * An implementation of [IndicatorState] to display the amount and position of a [LazyColumn]
 * component via its [LazyListState].
 *
 * @param state the [LazyListState] to adapt.
 * @VisibleForTesting
 */
internal class LazyColumnStateAdapter(private val state: LazyListState) : IndicatorState {
    private var latestSizeFraction: Float = 0f
    private var previousItemsCount: Int = 0

    // TODO: b/368270238 - Fix calculation on a small content size.
    override val positionFraction: Float
        get() {
            return if (state.layoutInfo.visibleItemsInfo.isEmpty()) {
                0.0f
            } else {
                val decimalFirstItemIndex = decimalFirstItemIndex()
                val decimalLastItemIndex = decimalLastItemIndex()

                val decimalLastItemIndexDistanceFromEnd =
                    state.layoutInfo.totalItemsCount - decimalLastItemIndex

                if (decimalFirstItemIndex + decimalLastItemIndexDistanceFromEnd == 0.0f) {
                    0.0f
                } else {
                    decimalFirstItemIndex /
                        (decimalFirstItemIndex + decimalLastItemIndexDistanceFromEnd)
                }
            }
        }

    // Represents the fraction of total items currently visible within the LazyColumn.
    // Initially calculated based on the visible items and updated only when the total number
    // of items in the LazyColumn changes.
    override val sizeFraction: Float
        get() {
            val layoutInfo: LazyListLayoutInfo = state.layoutInfo

            if (layoutInfo.totalItemsCount == 0) return 0.0f

            if (previousItemsCount != layoutInfo.totalItemsCount) {
                previousItemsCount = layoutInfo.totalItemsCount
                val decimalFirstItemIndex = decimalFirstItemIndex()
                val decimalLastItemIndex = decimalLastItemIndex()

                latestSizeFraction =
                    ((decimalLastItemIndex - decimalFirstItemIndex) /
                            layoutInfo.totalItemsCount.toFloat())
                        .coerceIn(minSizeFraction, maxSizeFraction)
            }

            return latestSizeFraction
        }

    override fun hashCode(): Int {
        return state.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return (other as? LazyColumnStateAdapter)?.state == state
    }

    private fun decimalLastItemIndex(): Float {
        if (state.layoutInfo.visibleItemsInfo.isEmpty()) return 0f
        val lastItem = state.layoutInfo.visibleItemsInfo.last()
        // Coerce item sizes to at least 1 to avoid divide by zero for zero height items
        val lastItemVisibleSize =
            (state.layoutInfo.viewportEndOffset - lastItem.offset)
                .coerceAtMost(lastItem.size)
                .coerceAtLeast(1)
        return lastItem.index.toFloat() +
            lastItemVisibleSize.toFloat() / lastItem.size.coerceAtLeast(1).toFloat()
    }

    private fun decimalFirstItemIndex(): Float {
        if (state.layoutInfo.visibleItemsInfo.isEmpty()) return 0f
        val firstItem = state.layoutInfo.visibleItemsInfo.first()
        val firstItemOffset = firstItem.offset - state.layoutInfo.viewportStartOffset
        // Coerce item size to at least 1 to avoid divide by zero for zero height items
        return firstItem.index.toFloat() -
            firstItemOffset.coerceAtMost(0).toFloat() / firstItem.size.coerceAtLeast(1).toFloat()
    }
}

private fun ContentDrawScope.drawCurvedIndicator(
    diameter: Float,
    color: Color,
    background: Color,
    paddingHorizontalPx: Float,
    indicatorOnTheRight: Boolean,
    indicatorHeight: Dp,
    gapHeight: Dp,
    indicatorWidthPx: Float,
    indicatorStart: Float,
    indicatorSize: Float,
) {
    // Calculate usable radius for drawing arcs (subtract padding from half diameter)
    val usableRadius = diameter / 2f - paddingHorizontalPx

    // Convert heights to angles (sweep for indicator, gap padding for spacing)
    val sweepDegrees = pixelsHeightToDegrees(indicatorHeight.toPx(), usableRadius)
    val gapHeightPadding = pixelsHeightToDegrees(gapHeight.toPx(), usableRadius)
    val gapPadding = pixelsHeightToDegrees(indicatorWidthPx + gapHeight.toPx(), usableRadius)

    // Define size for the arcs and calculate arc's top-left position.
    val arcSize =
        Size(
            diameter - 2 * paddingHorizontalPx - indicatorWidthPx,
            diameter - 2 * paddingHorizontalPx - indicatorWidthPx
        )
    val arcTopLeft =
        Offset(
            indicatorWidthPx / 2f +
                if (indicatorOnTheRight) {
                    size.width - diameter + paddingHorizontalPx
                } else {
                    paddingHorizontalPx
                },
            (size.height - diameter) / 2f + paddingHorizontalPx + indicatorWidthPx / 2f,
        )
    val startAngleOffset = if (indicatorOnTheRight) 0f else 180f

    // Calculate sweep angles for top, medium and bottom arcs
    val sweepTopArc = sweepDegrees * indicatorStart - gapPadding
    val startMidArc = startAngleOffset + sweepDegrees * (indicatorStart - 0.5f)
    val sweepMidArc = sweepDegrees * indicatorSize
    val endMidArc = startMidArc + sweepMidArc
    val sweepBottomArc = sweepDegrees * (1 - indicatorSize - indicatorStart) - gapPadding

    // Calculate scale fraction for top arc
    val topRadiusFraction =
        inverseLerp(
            -sweepDegrees / 2 + gapHeightPadding,
            -sweepDegrees / 2 + gapPadding,
            startMidArc - startAngleOffset
        )
    val topArcIndicatorWidth = lerp(0f, indicatorWidthPx, topRadiusFraction)
    // Calculate start angle for top segment.
    val startTopArc = startAngleOffset - sweepDegrees / 2
    // Represents an offset for top arc which moves when topRadiusFraction changes.
    val startTopArcOffset =
        pixelsHeightToDegrees(indicatorWidthPx * (1 - topRadiusFraction) / 2, usableRadius)
    // Calculate scale fraction for bottom arc
    val bottomRadiusFraction =
        inverseLerp(
            sweepDegrees / 2 - gapHeightPadding,
            sweepDegrees / 2 - gapPadding,
            endMidArc - startAngleOffset
        )
    val bottomArcIndicatorWidth = lerp(0f, indicatorWidthPx, bottomRadiusFraction)
    // Calculate start angle for bottom segment.
    val startBottomArc =
        startAngleOffset + sweepDegrees * (indicatorStart + indicatorSize - 0.5f) + gapPadding
    // Represents an offset for bottom arc which moves when bottomRadiusFraction changes.
    val startBottomArcOffset =
        pixelsHeightToDegrees(indicatorWidthPx * (1 - bottomRadiusFraction) / 2, usableRadius)
    // Draw top arc (unselected/background)
    drawArc(
        color = background,
        startAngle = startTopArc - startTopArcOffset,
        sweepAngle = max(sweepTopArc, 0.01f),
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = topArcIndicatorWidth, cap = StrokeCap.Round)
    )
    // Draw mid arc (selected/thumb)
    drawArc(
        color = color,
        startAngle = startMidArc,
        sweepAngle = sweepMidArc,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = indicatorWidthPx, cap = StrokeCap.Round)
    )
    // Draw bottom arc (unselected/background)
    drawArc(
        color = background,
        startAngle = startBottomArc - startBottomArcOffset,
        sweepAngle = max(sweepBottomArc, 0.01f),
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = bottomArcIndicatorWidth, cap = StrokeCap.Round)
    )
}

private fun pixelsHeightToDegrees(heightInPixels: Float, radius: Float): Float =
    2 * asin(heightInPixels / 2 / radius).toDegrees()

private fun ContentDrawScope.drawStraightIndicator(
    color: Color,
    background: Color,
    paddingHorizontalPx: Float,
    indicatorOnTheRight: Boolean,
    indicatorWidthPx: Float,
    indicatorHeightPx: Float,
    indicatorStart: Float,
    indicatorSize: Float,
) {
    val x =
        if (indicatorOnTheRight) {
            size.width - paddingHorizontalPx - indicatorWidthPx / 2
        } else {
            paddingHorizontalPx + indicatorWidthPx / 2
        }
    val lineTop = Offset(x, (size.height - indicatorHeightPx) / 2f)
    val lineBottom = lineTop + Offset(0f, indicatorHeightPx)
    drawLine(
        color = background,
        lineTop,
        lineBottom,
        strokeWidth = indicatorWidthPx,
        cap = StrokeCap.Round
    )
    drawLine(
        color,
        lerp(lineTop, lineBottom, indicatorStart),
        lerp(lineTop, lineBottom, indicatorStart + indicatorSize),
        strokeWidth = indicatorWidthPx,
        cap = StrokeCap.Round
    )
}

private fun sqr(x: Float) = x * x

private fun ScalingLazyListItemInfo.startOffset(anchorType: ScalingLazyListAnchorType) =
    offset -
        if (anchorType == ScalingLazyListAnchorType.ItemCenter) {
            (size / 2f)
        } else {
            0f
        }
