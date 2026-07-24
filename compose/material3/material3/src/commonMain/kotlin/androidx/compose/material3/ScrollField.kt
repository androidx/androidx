/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.compose.material3

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private const val InfinitePageCount = 100_000

/**
 * A state object that can be hoisted to observe and control the scrolling behavior of a
 * [ScrollField].
 *
 * In most cases, this state should be created via [rememberScrollFieldState].
 *
 * @param pagerState the underlying [PagerState] used to handle the scroll logic.
 * @param itemCount the total number of unique items available in the scroll field.
 */
@Stable
public class ScrollFieldState(internal val pagerState: PagerState, public val itemCount: Int) {
    init {
        require(itemCount > 0) { "itemCount must be greater than 0" }
    }

    /**
     * The index of the currently selected option.
     *
     * This value is always clamped between 0 and [itemCount] - 1. When the internal pager is
     * scrolled, this value updates to reflect the item closest to the snap position.
     *
     * Setting this value will instantly scroll to the specified option.
     */
    public var selectedOption: Int
        get() = if (itemCount > 0) pagerState.currentPage % itemCount else 0
        set(value) {
            if (itemCount > 0) {
                pagerState.requestScrollToPage(
                    calculateTargetPage(value.coerceIn(0, itemCount - 1))
                )
            }
        }

    /**
     * Instantly scrolls to the specified [option].
     *
     * @param option the index of the item to scroll to.
     * @see animateScrollToOption for a smooth transition.
     */
    public suspend fun scrollToOption(option: Int) {
        val targetPage = calculateTargetPage(option.coerceIn(0, itemCount - 1))
        pagerState.scrollToPage(targetPage)
    }

    /**
     * Animates the scroll to the specified [option].
     *
     * @param option the index of the item to animate to.
     * @see scrollToOption for an instant scroll.
     */
    public suspend fun animateScrollToOption(option: Int) {
        val targetPage = calculateTargetPage(option.coerceIn(0, itemCount - 1))
        pagerState.animateScrollToPage(targetPage)
    }

    private fun calculateTargetPage(option: Int): Int {
        val currentContextPage = pagerState.currentPage
        val currentOption = currentContextPage % itemCount
        val diff = option - currentOption
        return currentContextPage + diff
    }

    /**
     * The option the pager will settle on once any in-progress scroll finishes. Equal to
     * [selectedOption] when the pager is at rest.
     */
    public val targetOption: Int
        get() = if (itemCount > 0) pagerState.targetPage % itemCount else 0

    /**
     * Whether this [ScrollField] is currently scrolling, either by user gesture or by animation.
     */
    public val isScrollInProgress: Boolean
        get() = pagerState.isScrollInProgress
}

/**
 * Creates and remembers a [ScrollFieldState] to be used with a [ScrollField].
 *
 * @param itemCount the total number of unique items to be displayed in the scrollable wheel.
 * @param index the initial selected index of the scroll field.
 * @return a [ScrollFieldState] that can be used to control or observe the scroll field.
 */
@Composable
public fun rememberScrollFieldState(itemCount: Int, index: Int = 0): ScrollFieldState {
    val initialPage =
        remember(itemCount, index) {
            val coercedIndex = if (itemCount > 0) index.coerceIn(0, itemCount - 1) else 0
            (InfinitePageCount / 2) - (InfinitePageCount / 2 % itemCount) + coercedIndex
        }
    val pagerState = rememberPagerState(initialPage = initialPage) { InfinitePageCount }
    return remember(pagerState, itemCount) { ScrollFieldState(pagerState, itemCount) }
}

/**
 * ScrollField's can be used to provide a more interactive way to select a time or other numerical
 * value.
 *
 * Generic ScrollField for scrollable numerical selection:
 *
 * @sample androidx.compose.material3.samples.ScrollFieldSample
 *
 * ScrollField for time selection:
 *
 * @sample androidx.compose.material3.samples.TimeScrollFieldSample
 *
 * ScrollField for unit selection:
 *
 * @sample androidx.compose.material3.samples.UnitScrollFieldSample
 * @param state the state object to be used to control or observe the pager's state.
 * @param contentDescription text used by accessibility services to describe what this field
 *   selects. This should include the available range when it is not obvious from context (e.g.,
 *   "Select year between 2000 and 2025"). Because the wheel wraps around endlessly, this
 *   description is the only way for an accessibility user to learn the field's bounds.
 * @param modifier the [Modifier] to be applied to the ScrollField container.
 * @param colors [ScrollFieldColors] that will be used to resolve the colors used for this
 *   ScrollField in different states.
 * @param fieldAccessibilityDescription returns the text accessibility services (e.g. TalkBack)
 *   announced for the option at the given index. It should match the text rendered by [field].
 * @param interactionSource [MutableInteractionSource] for observing and controlling the
 *   interactions with the scroll field.
 * @param field the composable used to render each item in the wheel.
 */
@Composable
public fun ScrollField(
    state: ScrollFieldState,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    colors: ScrollFieldColors = ScrollFieldDefaults.colors(),
    fieldAccessibilityDescription: (index: Int) -> String = { index -> index.toLocalString() },
    interactionSource: MutableInteractionSource? = null,
    field: @Composable (index: Int, selected: Boolean) -> Unit = { index, selected ->
        ScrollFieldDefaults.Item(index = index, selected = selected, colors = colors)
    },
) {
    val scope = rememberCoroutineScope()

    VerticalPager(
        state = state.pagerState,
        modifier =
            modifier
                .background(colors.containerColor, shape = ScrollFieldDefaults.shape)
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    val direction =
                        when (event.key) {
                            Key.DirectionDown -> 1
                            Key.DirectionUp -> -1
                            else -> return@onKeyEvent false
                        }
                    val nextPage = state.pagerState.targetPage + direction
                    scope.launch { state.pagerState.animateScrollToPage(nextPage) }
                    true
                }
                .focusable(interactionSource = interactionSource)
                .clearAndSetSemantics {
                    if (contentDescription != null) {
                        this.contentDescription = contentDescription
                    }
                    stateDescription = fieldAccessibilityDescription(state.targetOption)
                    progressBarRangeInfo =
                        ProgressBarRangeInfo(
                            current = state.pagerState.targetPage.toFloat(),
                            range = 0f..(InfinitePageCount - 1).toFloat(),
                            steps = InfinitePageCount - 2,
                        )

                    setProgress { targetValue ->
                        val targetPage = targetValue.roundToInt()
                        if (targetPage != state.pagerState.targetPage) {
                            scope.launch { state.pagerState.animateScrollToPage(targetPage) }
                            true
                        } else {
                            false
                        }
                    }
                },
        pageSize = PageSize.Fixed(ScrollFieldDefaults.ScrollFieldHeight / 3),
        horizontalAlignment = Alignment.CenterHorizontally,
        snapPosition = SnapPosition.Center,
    ) { page ->
        val index = page % state.itemCount
        val isSelected = state.pagerState.currentPage == page
        Box(
            modifier =
                Modifier.fillMaxHeight()
                    .focusProperties { canFocus = false }
                    .clickable { scope.launch { state.animateScrollToOption(index) } }
                    .semantics { selected = isSelected },
            contentAlignment = Alignment.Center,
        ) {
            field(index, isSelected)
        }
    }
}

/** Represents the colors used by a [ScrollField] in different states. */
@Immutable
public class ScrollFieldColors(
    public val containerColor: Color,
    public val unselectedContentColor: Color,
    public val selectedContentColor: Color,
) {
    /**
     * Returns a copy of this ScrollFieldColors, optionally overriding some of the values. This uses
     * the Color.Unspecified to mean “use the value from the source".
     */
    public fun copy(
        containerColor: Color = this.containerColor,
        unselectedContentColor: Color = this.unselectedContentColor,
        selectedContentColor: Color = this.selectedContentColor,
    ): ScrollFieldColors =
        ScrollFieldColors(
            containerColor.takeOrElse { this.containerColor },
            unselectedContentColor.takeOrElse { this.unselectedContentColor },
            selectedContentColor.takeOrElse { this.selectedContentColor },
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ScrollFieldColors) return false
        if (containerColor != other.containerColor) return false
        if (unselectedContentColor != other.unselectedContentColor) return false
        if (selectedContentColor != other.selectedContentColor) return false
        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + unselectedContentColor.hashCode()
        result = 31 * result + selectedContentColor.hashCode()
        return result
    }
}

/** Object to hold defaults used by [ScrollField]. */
@Stable
public object ScrollFieldDefaults {
    /**
     * The default height for a [ScrollField]. This can be used as a reference when providing a
     * Modifier.height to the ScrollField to ensure enough vertical space is available to display
     * the typical three-item layout.
     */
    public val ScrollFieldHeight: Dp = 200.dp
    /** The default shape for the [ScrollField] container background. */
    public val shape: Shape
        @Composable get() = ShapeDefaults.Large

    /** Default colors used by a [ScrollField]. */
    @Composable
    public fun colors(): ScrollFieldColors = MaterialTheme.colorScheme.defaultScrollFieldColors

    /**
     * Creates a [ScrollFieldColors] that represents the default container, unselected, and selected
     * colors used in a [ScrollField].
     *
     * @param containerColor The color of the [ScrollField] container.
     * @param unselectedContentColor The color of the numerical value(s) visible on the screen that
     *   are not chosen.
     * @param selectedContentColor The color of the numerical value that is centered and snapped
     *   into place.
     */
    @Composable
    public fun colors(
        containerColor: Color = Color.Unspecified,
        unselectedContentColor: Color = Color.Unspecified,
        selectedContentColor: Color = Color.Unspecified,
    ): ScrollFieldColors =
        MaterialTheme.colorScheme.defaultScrollFieldColors.copy(
            containerColor = containerColor,
            unselectedContentColor = unselectedContentColor,
            selectedContentColor = selectedContentColor,
        )

    internal val ColorScheme.defaultScrollFieldColors: ScrollFieldColors
        @Composable
        get() {
            return defaultScrollFieldColorsCached
                ?: ScrollFieldColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        unselectedContentColor = MaterialTheme.colorScheme.outline,
                        selectedContentColor = MaterialTheme.colorScheme.onSurface,
                    )
                    .also { defaultScrollFieldColorsCached = it }
        }

    /**
     * The default item implementation for [ScrollField].
     *
     * @param index the current item index.
     * @param selected whether this item is currently selected (centered).
     * @param colors the colors to use for the text content.
     */
    @Composable
    public fun Item(index: Int, selected: Boolean, colors: ScrollFieldColors = colors()) {
        val targetColor =
            if (selected) colors.selectedContentColor else colors.unselectedContentColor

        val selectionFraction by
            animateFloatAsState(
                targetValue = if (selected) 1f else 0f,
                animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
            )
        val color by
            animateColorAsState(
                targetValue = targetColor,
                animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
            )

        Text(
            text = index.toLocalString(minDigits = 2),
            style =
                lerp(
                    MaterialTheme.typography.displayMedium,
                    MaterialTheme.typography.displayLargeEmphasized,
                    selectionFraction.coerceIn(0f, 1f),
                ),
            color = color,
        )
    }
}
