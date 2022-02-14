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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A scrollable list of items to pick from. By default, items will be repeated
 * "infinitely" in both directions, unless [PickerState#repeatItems] is specified as false.
 *
 * Example of a simple picker to select one of five options:
 * @sample androidx.wear.compose.material.samples.SimplePicker
 *
 * @param state The state of the component
 * @param modifier Modifier to be applied to the Picker
 * @param scalingParams The parameters to configure the scaling and transparency effects for the
 * component. See [ScalingParams]
 * @param separation The amount of separation in [Dp] between items. Can be negative, which can be
 * useful for Text if it has plenty of whitespace.
 * @param gradientRatio The size relative to the Picker height that the top and bottom gradients
 * take. These gradients blur the picker content on the top and bottom. The default is 0.33,
 * so the top 1/3 and the bottom 1/3 of the picker are taken by gradients. Should be between 0.0 and
 * 0.5. Use 0.0 to disable the gradient.
 * @param gradientColor Should be the color outside of the Picker, so there is continuity.
 * @param option A block which describes the content. Inside this block you can reference
 * [PickerScope.selectedOption] and other properties in [PickerScope]
 */
@Composable
public fun Picker(
    state: PickerState,
    modifier: Modifier = Modifier,
    scalingParams: ScalingParams = PickerDefaults.scalingParams(),
    separation: Dp = 0.dp,
    /* @FloatRange(from = 0.0, to = 0.5) */
    gradientRatio: Float = PickerDefaults.DefaultGradientRatio,
    gradientColor: Color = MaterialTheme.colors.background,
    option: @Composable PickerScope.(optionIndex: Int) -> Unit
) {
    require(gradientRatio in 0f..0.5f) { "gradientRatio should be between 0.0 and 0.5" }
    val pickerScope = remember(state) { PickerScopeImpl(state) }
    ScalingLazyColumn(
        modifier = modifier.drawWithContent {
            drawContent()
            if (gradientRatio > 0.0f) {
                // Apply a fade-out gradient on the top and bottom.
                drawRect(Brush.linearGradient(
                    colors = listOf(gradientColor, Color.Transparent),
                    start = Offset(size.width / 2, 0f),
                    end = Offset(size.width / 2, size.height * gradientRatio)
                ))
                drawRect(Brush.linearGradient(
                    colors = listOf(Color.Transparent, gradientColor),
                    start = Offset(size.width / 2, size.height * (1 - gradientRatio)),
                    end = Offset(size.width / 2, size.height)
                ))
            }
        },
        state = state.scalingLazyListState,
        content = {
            items(state.numberOfOptions * state.repeatTarget) { ix ->
                with(pickerScope) {
                    option(ix % state.numberOfOptions)
                }
            }
        },
        contentPadding = PaddingValues(0.dp),
        scalingParams = scalingParams,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = separation
        ),
        flingBehavior = ScalingLazyColumnDefaults.snapFlingBehavior(state.scalingLazyListState)
    )
}

/**
 * Creates a [PickerState] that is remembered across compositions.
 *
 * @param numberOfOptions the number of options
 * @param initiallySelectedOption the option to show in the center at the start
 * @param repeatItems if true (the default), the contents of the component will be repeated
 */
@Composable
public fun rememberPickerState(
    numberOfOptions: Int,
    initiallySelectedOption: Int = 0,
    repeatItems: Boolean = true
): PickerState = rememberSaveable(saver = PickerState.Saver) {
       PickerState(numberOfOptions, initiallySelectedOption, repeatItems)
    }

/**
 * A state object that can be hoisted to observe item selection.
 *
 * In most cases, this will be created via [rememberPickerState].
 *
 * @param numberOfOptions the number of options
 * @param initiallySelectedOption the option to show in the center at the start
 * @param repeatItems if true (the default), the contents of the component will be repeated
 */
@Stable
public class PickerState constructor(
    val numberOfOptions: Int,
    initiallySelectedOption: Int = 0,
    val repeatItems: Boolean = true
) {
    init {
        require(numberOfOptions > 0) { "The picker should have at least one item." }
    }

    internal val repeatTarget = if (repeatItems) 100_000_000 / numberOfOptions else 1
    private val centerOffset = numberOfOptions * (repeatTarget / 2)
    internal val scalingLazyListState = ScalingLazyListState(
        centerOffset + initiallySelectedOption,
        0
    )

    /**
     * Index of the item selected (i.e., at the center)
     */
    val selectedOption: Int
        get() = scalingLazyListState.centerItemIndex % numberOfOptions

    /**
     * Instantly scroll to an item.
     * Note that for this to work properly, all options need to have the same height, and this can
     * only be called after the Picker has been laid out.
     *
     * @sample androidx.wear.compose.material.samples.OptionChangePicker
     *
     * @param index The index of the option to scroll to.
     */
    suspend fun scrollToOption(index: Int) {
        scalingLazyListState.scrollToItem(index + centerOffset, 0)
    }

    companion object {
        /**
         * The default [Saver] implementation for [PickerState].
         */
        val Saver = listSaver<PickerState, Any?>(
            save = {
                listOf(
                    it.numberOfOptions,
                    it.selectedOption,
                    it.repeatItems
                )
            },
            restore = { saved ->
                @Suppress("UNCHECKED_CAST")
                PickerState(
                    numberOfOptions = saved[0] as Int,
                    initiallySelectedOption = saved[1] as Int,
                    repeatItems = saved[2] as Boolean
                )
            }
        )
    }
}

/**
 * Contains the default values used by [Picker]
 */
public object PickerDefaults {
    /**
     * Scaling params are used to determine when items start to be scaled down and alpha applied,
     * and how much. For details, see [ScalingParams]
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

    val DefaultGradientRatio = 0.33f
}

/**
 * Receiver scope which is used by [Picker].
 */
public interface PickerScope {
    /**
     * Index of the item selected (i.e., at the center)
     */
    val selectedOption: Int
}

@Stable
private class PickerScopeImpl(
    private val pickerState: PickerState
) : PickerScope {
    override val selectedOption: Int
        get() = pickerState.selectedOption
}
