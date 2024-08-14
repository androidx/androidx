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
import androidx.annotation.IntRange
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.scrollToIndex
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.ScalingParams
import androidx.wear.compose.foundation.rotary.RotaryScrollableBehavior
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.RotarySnapLayoutInfoProvider
import kotlinx.coroutines.launch

/**
 * A scrollable list of items to pick from. By default, items will be repeated "infinitely" in both
 * directions, unless [PickerState#repeatItems] is specified as false.
 *
 * This overload supports rotary input. Rotary input allows users to scroll the content of the
 * [Picker] - by using a crown or a rotating bezel on their Wear OS device. It can be modified with
 * [rotaryScrollableBehavior] param. Note that rotary scroll and touch scroll should be aligned. If
 * [rotaryScrollableBehavior] is set for snap (using [RotaryScrollableDefaults.snapBehavior]),
 * [flingBehavior] should be set for snap as well (using [PickerDefaults.flingBehavior]).
 *
 * Example of a simple picker to select one of five options:
 *
 * @sample androidx.wear.compose.material3.samples.SimplePicker
 *
 * Example of a sample picker group with an hour and minute picker (24 hour format):
 *
 * @sample androidx.wear.compose.material3.samples.PickerGroupSample
 * @param state The state of the component
 * @param contentDescription Text used by accessibility services to describe what the selected
 *   option represents. This text should be localized, such as by using
 *   [androidx.compose.ui.res.stringResource] or similar. Typically, the content description is
 *   inferred via derivedStateOf to avoid unnecessary recompositions, like this: val description by
 *   remember { derivedStateOf { /* expression using state.selectedOption */ } }
 * @param modifier [Modifier] to be applied to the Picker.
 * @param readOnly Determines whether the [Picker] should display other available options for this
 *   field, inviting the user to scroll to change the value. When readOnly = true, only displays the
 *   currently selected option (and optionally a label). This is intended to be used for screens
 *   that display multiple Pickers, only one of which has the focus at a time.
 * @param readOnlyLabel A slot for providing a label, displayed above the selected option when the
 *   [Picker] is read-only. The label is overlaid with the currently selected option within a Box,
 *   so it is recommended that the label is given [Alignment.TopCenter].
 * @param onSelected Action triggered when the Picker is selected by clicking. Used by accessibility
 *   semantics, which facilitates implementation of multi-picker screens.
 * @param scalingParams The parameters to configure the scaling and transparency effects for the
 *   component. See [ScalingParams].
 * @param spacing The amount of spacing in [Dp] between items. Can be negative, which can be useful
 *   for Text if it has plenty of whitespace.
 * @param gradientRatio The size relative to the Picker height that the top and bottom gradients
 *   take. These gradients blur the picker content on the top and bottom. The default is 0.33, so
 *   the top 1/3 and the bottom 1/3 of the picker are taken by gradients. Should be between 0.0 and
 *   0.5. Use 0.0 to disable the gradient.
 * @param gradientColor Should be the color outside of the Picker, so there is continuity.
 * @param flingBehavior logic describing fling behavior. Note that when configuring fling or snap
 *   behavior, this flingBehavior parameter and the [rotaryScrollableBehavior] parameter that
 *   controls rotary scroll are expected to be consistent.
 * @param userScrollEnabled Determines whether the picker should be scrollable or not. When
 *   userScrollEnabled = true, picker is scrollable. This is different from [readOnly] as it changes
 *   the scrolling behaviour.
 * @param rotaryScrollableBehavior Parameter for changing rotary behavior. Supports scroll
 *   [RotaryScrollableDefaults.behavior] and snap [RotaryScrollableDefaults.snapBehavior]. We do
 *   recommend to use [RotaryScrollableDefaults.snapBehavior] as this is a recommended behavior for
 *   Pickers. Note that when configuring fling or snap behavior, this rotaryBehavior parameter and
 *   the [flingBehavior] parameter that controls touch scroll are expected to be consistent. Can be
 *   null if rotary support is not required.
 * @param option A block which describes the content. Inside this block you can reference
 *   [PickerScope.selectedOption] and other properties in [PickerScope]. When read-only mode is in
 *   use on a screen, it is recommended that this content is given [Alignment.Center] in order to
 *   align with the centrally selected Picker value.
 */
@Composable
fun Picker(
    state: PickerState,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    readOnlyLabel: @Composable (BoxScope.() -> Unit)? = null,
    onSelected: () -> Unit = {},
    scalingParams: ScalingParams = PickerDefaults.scalingParams(),
    spacing: Dp = 0.dp,
    @FloatRange(from = 0.0, to = 0.5) gradientRatio: Float = PickerDefaults.GradientRatio,
    gradientColor: Color = MaterialTheme.colorScheme.background,
    flingBehavior: FlingBehavior = PickerDefaults.flingBehavior(state),
    userScrollEnabled: Boolean = true,
    rotaryScrollableBehavior: RotaryScrollableBehavior? =
        RotaryScrollableDefaults.snapBehavior(state, state.toRotarySnapLayoutInfoProvider()),
    option: @Composable PickerScope.(optionIndex: Int) -> Unit
) {
    require(gradientRatio in 0f..0.5f) { "gradientRatio should be between 0.0 and 0.5" }
    val pickerScope = remember(state) { PickerScopeImpl(state) }
    var forceScrollWhenReadOnly by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    Box(modifier = modifier) {
        ScalingLazyColumn(
            modifier =
                Modifier.clearAndSetSemantics {
                        onClick {
                            coroutineScope.launch { onSelected() }
                            true
                        }
                        scrollToIndex {
                            coroutineScope.launch {
                                state.scrollToOption(it)
                                onSelected()
                            }
                            true
                        }
                        if (!state.isScrollInProgress && contentDescription != null) {
                            this.contentDescription = contentDescription
                        }
                        focused = !readOnly
                    }
                    .then(
                        if (!readOnly && gradientRatio > 0.0f) {
                            Modifier.drawWithContent {
                                    drawContent()
                                    drawGradient(gradientColor, gradientRatio)
                                }
                                // b/223386180 - add padding when drawing rectangles to
                                // prevent jitter on screen.
                                .padding(vertical = 1.dp)
                                .align(Alignment.Center)
                        } else if (readOnly) {
                            Modifier.drawWithContent {
                                    drawContent()
                                    val visibleItems =
                                        state.scalingLazyListState.layoutInfo.visibleItemsInfo
                                    if (visibleItems.isNotEmpty()) {
                                        val centerItem =
                                            visibleItems.fastFirstOrNull { info ->
                                                info.index ==
                                                    state.scalingLazyListState.centerItemIndex
                                            } ?: visibleItems[visibleItems.size / 2]
                                        val shimHeight =
                                            (size.height -
                                                centerItem.unadjustedSize.toFloat() -
                                                spacing.toPx()) / 2.0f
                                        drawShim(gradientColor, shimHeight)
                                    }
                                }
                                // b/223386180 - add padding when drawing rectangles to
                                // prevent jitter on screen.
                                .padding(vertical = 1.dp)
                                .align(Alignment.Center)
                        } else {
                            Modifier.align(Alignment.Center)
                        }
                    ),
            state = state.scalingLazyListState,
            content = {
                items(state.numberOfItems()) { ix ->
                    with(pickerScope) {
                        Box(
                            Modifier.graphicsLayer {
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                        ) {
                            option((ix + state.optionsOffset) % state.numberOfOptions)
                        }
                    }
                }
            },
            rotaryScrollableBehavior = rotaryScrollableBehavior,
            contentPadding = PaddingValues(0.dp),
            scalingParams = scalingParams,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = spacing),
            flingBehavior = flingBehavior,
            autoCentering = AutoCenteringParams(itemIndex = 0),
            userScrollEnabled = userScrollEnabled
        )
        if (readOnly && readOnlyLabel != null) {
            readOnlyLabel()
        }
    }
    SideEffect {
        if (!readOnly) {
            forceScrollWhenReadOnly = true
        }
    }
    // If a Picker switches to read-only during animation, the ScalingLazyColumn can be
    // out of position, so we force an instant scroll to the selected option so that it is
    // correctly lined up when the Picker is next displayed.
    LaunchedEffect(readOnly, forceScrollWhenReadOnly) {
        if (readOnly && forceScrollWhenReadOnly) {
            state.scrollToOption(state.selectedOption)
            forceScrollWhenReadOnly = false
        }
    }
}

/**
 * Creates a [PickerState] that is remembered across compositions.
 *
 * @param initialNumberOfOptions the number of options.
 * @param initiallySelectedOption the option to show in the center at the start.
 * @param repeatItems if true (the default), the contents of the component will be repeated.
 */
@Composable
fun rememberPickerState(
    initialNumberOfOptions: Int,
    initiallySelectedOption: Int = 0,
    repeatItems: Boolean = true
): PickerState =
    rememberSaveable(
        initialNumberOfOptions,
        initiallySelectedOption,
        repeatItems,
        saver = PickerState.Saver
    ) {
        PickerState(initialNumberOfOptions, initiallySelectedOption, repeatItems)
    }

/**
 * A state object that can be hoisted to observe item selection.
 *
 * In most cases, this will be created via [rememberPickerState].
 *
 * @param initialNumberOfOptions the number of options.
 * @param initiallySelectedOption the option to show in the center at the start.
 * @param repeatItems if true (the default), the contents of the component will be repeated.
 */
@Stable
class PickerState(
    @IntRange(from = 1) initialNumberOfOptions: Int,
    initiallySelectedOption: Int = 0,
    val repeatItems: Boolean = true
) : ScrollableState {
    init {
        verifyNumberOfOptions(initialNumberOfOptions)
    }

    private var _numberOfOptions by mutableIntStateOf(initialNumberOfOptions)

    var numberOfOptions
        get() = _numberOfOptions
        set(newNumberOfOptions) {
            verifyNumberOfOptions(newNumberOfOptions)
            // We need to maintain the mapping between the currently selected item and the
            // currently selected option.
            optionsOffset =
                positiveModulo(
                    selectedOption.coerceAtMost(newNumberOfOptions - 1) -
                        scalingLazyListState.centerItemIndex,
                    newNumberOfOptions
                )
            _numberOfOptions = newNumberOfOptions
        }

    internal fun numberOfItems() = if (!repeatItems) numberOfOptions else LARGE_NUMBER_OF_ITEMS

    // The difference between the option we want to select for the current numberOfOptions
    // and the selection with the initial numberOfOptions.
    // Note that if repeatItems is true (the default), we have a large number of items, and a
    // smaller number of options, so many items map to the same options. This variable is part of
    // that mapping since we need to adjust it when the number of options change.
    // The mapping is that given an item index, subtracting optionsOffset and doing modulo the
    // current number of options gives the option index:
    // itemIndex - optionsOffset =(mod numberOfOptions) optionIndex
    internal var optionsOffset = 0

    internal val scalingLazyListState = run {
        val repeats = if (repeatItems) LARGE_NUMBER_OF_ITEMS / numberOfOptions else 1
        val centerOffset = numberOfOptions * (repeats / 2)
        ScalingLazyListState(centerOffset + initiallySelectedOption, 0)
    }

    /** Index of the option selected (i.e., at the center). */
    val selectedOption: Int
        get() = (scalingLazyListState.centerItemIndex + optionsOffset) % numberOfOptions

    /**
     * Instantly scroll to an item.
     *
     * @sample androidx.wear.compose.material3.samples.PickerScrollToOption
     * @param index The index of the option to scroll to.
     */
    suspend fun scrollToOption(index: Int) {
        scalingLazyListState.scrollToItem(getClosestTargetItemIndex(index), 0)
    }

    /**
     * Animate (smooth scroll) to the given item at [index].
     *
     * A smooth scroll always happens to the closest item if PickerState has repeatItems=true. For
     * example, picker values are : 0 1 2 3 0 1 2 [3] 0 1 2 3 Target value is [0]. 0 1 2 3 >0< 1 2
     * [3] >0< 1 2 3 Picker can be scrolled forwards or backwards. To get to the target 0 it
     * requires 1 step to scroll forwards and 3 steps to scroll backwards. Picker will be scrolled
     * forwards as this is the closest destination.
     *
     * If the distance between possible targets is the same, picker will be scrolled backwards.
     *
     * @sample androidx.wear.compose.material3.samples.PickerAnimateScrollToOption
     * @param index The index of the option to scroll to.
     */
    suspend fun animateScrollToOption(index: Int) {
        scalingLazyListState.animateScrollToItem(getClosestTargetItemIndex(index), 0)
    }

    companion object {
        /** The default [Saver] implementation for [PickerState]. */
        val Saver =
            listSaver<PickerState, Any?>(
                save = { listOf(it.numberOfOptions, it.selectedOption, it.repeatItems) },
                restore = { saved ->
                    PickerState(
                        initialNumberOfOptions = saved[0] as Int,
                        initiallySelectedOption = saved[1] as Int,
                        repeatItems = saved[2] as Boolean
                    )
                }
            )
    }

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit
    ) {
        scalingLazyListState.scroll(scrollPriority, block)
    }

    override fun dispatchRawDelta(delta: Float): Float {
        return scalingLazyListState.dispatchRawDelta(delta)
    }

    override val isScrollInProgress: Boolean
        get() = scalingLazyListState.isScrollInProgress

    override val canScrollForward: Boolean
        get() = scalingLazyListState.canScrollForward

    override val canScrollBackward: Boolean
        get() = scalingLazyListState.canScrollBackward

    /** Function which calculates the real position of an option. */
    private fun getClosestTargetItemIndex(option: Int): Int =
        if (!repeatItems) {
            option
        } else {
            // Calculating the distance to the target option in front or back.
            // The minimum distance is then selected and picker is scrolled in that direction.
            val stepsPrev = positiveModulo(selectedOption - option, numberOfOptions)
            val stepsNext = positiveModulo(option - selectedOption, numberOfOptions)
            scalingLazyListState.centerItemIndex +
                if (stepsPrev <= stepsNext) -stepsPrev else stepsNext
        }

    private fun verifyNumberOfOptions(numberOfOptions: Int) {
        require(numberOfOptions > 0) { "The picker should have at least one item." }
        require(numberOfOptions < LARGE_NUMBER_OF_ITEMS / 3) {
            // Set an upper limit to ensure there are at least 3 repeats of all the options
            "The picker should have less than ${LARGE_NUMBER_OF_ITEMS / 3} items"
        }
    }
}

/** Contains the default values used by [Picker]. */
object PickerDefaults {

    /**
     * Scaling params are used to determine when items start to be scaled down and alpha applied,
     * and how much. For details, see [ScalingParams].
     */
    fun scalingParams(
        edgeScale: Float = 0.45f,
        edgeAlpha: Float = 1.0f,
        minElementHeight: Float = 0.0f,
        maxElementHeight: Float = 0.0f,
        minTransitionArea: Float = 0.45f,
        maxTransitionArea: Float = 0.45f,
        scaleInterpolator: Easing = CubicBezierEasing(0.25f, 0.00f, 0.75f, 1.00f),
        viewportVerticalOffsetResolver: (Constraints) -> Int = { (it.maxHeight / 5f).toInt() }
    ): ScalingParams =
        ScalingLazyColumnDefaults.scalingParams(
            edgeScale = edgeScale,
            edgeAlpha = edgeAlpha,
            minElementHeight = minElementHeight,
            maxElementHeight = maxElementHeight,
            minTransitionArea = minTransitionArea,
            maxTransitionArea = maxTransitionArea,
            scaleInterpolator = scaleInterpolator,
            viewportVerticalOffsetResolver = viewportVerticalOffsetResolver
        )

    /**
     * Create and remember a [FlingBehavior] that will represent natural fling curve with snap to
     * central item as the fling decays.
     *
     * @param state the state of the [Picker].
     * @param decay the decay to use.
     */
    @Composable
    fun flingBehavior(
        state: PickerState,
        decay: DecayAnimationSpec<Float> = exponentialDecay()
    ): FlingBehavior {
        return ScalingLazyColumnDefaults.snapFlingBehavior(
            state = state.scalingLazyListState,
            snapOffset = 0.dp,
            decay = decay
        )
    }

    /**
     * Default Picker gradient ratio - the proportion of the Picker height allocated to each of the
     * of the top and bottom gradients.
     */
    val GradientRatio = 0.33f
}

/** Receiver scope which is used by [Picker]. */
interface PickerScope {
    /** Index of the item selected (i.e., at the center). */
    val selectedOption: Int
}

private fun positiveModulo(n: Int, mod: Int) = ((n % mod) + mod) % mod

/** An extension function for creating [RotarySnapLayoutInfoProvider] from [Picker]. */
@OptIn(ExperimentalWearFoundationApi::class)
@Composable
internal fun PickerState.toRotarySnapLayoutInfoProvider(): RotarySnapLayoutInfoProvider =
    remember(this) { PickerRotarySnapLayoutInfoProvider(this) }

/** An implementation of RotaryScrollableAdapter for [Picker]. */
@ExperimentalWearFoundationApi
internal class PickerRotarySnapLayoutInfoProvider(private val scrollableState: PickerState) :
    RotarySnapLayoutInfoProvider {

    /** Returns a height of a first item, as all items in picker have the same height. */
    override val averageItemSize: Float
        get() =
            scrollableState.scalingLazyListState.layoutInfo.visibleItemsInfo
                .firstOrNull()
                ?.unadjustedSize
                ?.toFloat() ?: 0f

    /** Current (centred) item index. */
    override val currentItemIndex: Int
        get() = scrollableState.scalingLazyListState.centerItemIndex

    /** An offset from the item centre. */
    override val currentItemOffset: Float
        get() = scrollableState.scalingLazyListState.centerItemScrollOffset.toFloat()

    override val totalItemCount: Int
        get() = scrollableState.scalingLazyListState.layoutInfo.totalItemsCount
}

// Apply a shim on the top and bottom of the Picker to hide all but the selected option.
private fun ContentDrawScope.drawShim(gradientColor: Color, height: Float) {
    drawRect(color = gradientColor, size = Size(size.width, height))
    drawRect(
        color = gradientColor,
        topLeft = Offset(0f, size.height - height),
        size = Size(size.width, height)
    )
}

// Apply a fade-out gradient on the top and bottom of the Picker.
private fun ContentDrawScope.drawGradient(gradientColor: Color, gradientRatio: Float) {
    drawRect(
        Brush.linearGradient(
            colors = listOf(gradientColor, Color.Transparent),
            start = Offset(size.width / 2, 0f),
            end = Offset(size.width / 2, size.height * gradientRatio)
        )
    )
    drawRect(
        Brush.linearGradient(
            colors = listOf(Color.Transparent, gradientColor),
            start = Offset(size.width / 2, size.height * (1 - gradientRatio)),
            end = Offset(size.width / 2, size.height)
        )
    )
}

@Stable
private class PickerScopeImpl(private val pickerState: PickerState) : PickerScope {
    override val selectedOption: Int
        get() = pickerState.selectedOption
}

private const val LARGE_NUMBER_OF_ITEMS = 100_000_000
