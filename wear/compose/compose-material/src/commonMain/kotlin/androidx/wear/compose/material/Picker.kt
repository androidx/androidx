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

import androidx.compose.animation.core.AnimationScope
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
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
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A scrollable list of items to pick from. By default, items will be repeated
 * "infinitely" in both directions, unless [PickerState#repeatItems] is specified as false.
 *
 * @sample androidx.wear.compose.material.samples.SimplePicker
 *
 * @param state The state of the component
 * @param modifier Modifier to be applied to the Picker
 * @param scalingParams the parameters to configure the scaling and transparency effects for the
 * component. See [ScalingParams]
 * "infinitely" in both directions. If false, the elements will appear only once.
 * @param separation the amount of separation in [Dp] between items. Can be negative, which can be
 * useful for Text if it has plenty of whitespace.
 * @param flingBehavior Logic describing fling behavior, by default, it'll snap to the closest
 * option to the fling target.
 * @param option a block which describes the content. Inside this block you can reference
 * [PickerScope.selectedOption] and other properties in [PickerScope]
 */
@Composable
fun Picker(
    state: PickerState,
    modifier: Modifier = Modifier,
    scalingParams: ScalingParams = PickerDefaults.scalingParams(),
    separation: Dp = 0.dp,
    flingBehavior: FlingBehavior = scalingLazyListSnapping(state.scalingLazyListState),
    option: @Composable PickerScope.(optionIndex: Int) -> Unit
) {
    val pickerScope = remember(state) { PickerScopeImpl(state) }
    ScalingLazyColumn(
        modifier = modifier,
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
        flingBehavior = flingBehavior
    )
}

// TODO(b/216089745): Find the right place to put this and make available so it can be used outside
// of Pickers.
internal fun scalingLazyListSnapping(
    state: ScalingLazyListState,
    decay: DecayAnimationSpec<Float> = exponentialDecay()
) = object : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        val animationState = AnimationState(
            initialValue = 0f,
            initialVelocity = initialVelocity,
        )

        // Is it actually a fling?
        val visibleItemsInfo = state.layoutInfo.visibleItemsInfo
        if (abs(initialVelocity) > 1f && visibleItemsInfo.size > 1) {
            // Target we will land on given initialVelocity & decay
            val unmodifiedTarget = decay.calculateTargetValue(0f, initialVelocity)
            val viewPortHeight = state.viewportHeightPx.value!!

            // Estimate the item closest to the target, and adjust our aim.
            val totalSize = visibleItemsInfo.last().unadjustedOffset -
                visibleItemsInfo.first().unadjustedOffset
            val estimatedItemDistance = totalSize.toFloat() / (visibleItemsInfo.size - 1)
            val centerOffset = state.centerItemScrollOffset
            val itemsToTarget = (unmodifiedTarget + centerOffset) / estimatedItemDistance

            val estimatedTarget = itemsToTarget.roundToInt() * estimatedItemDistance -
                centerOffset

            animationState.animateDecayTo(estimatedTarget, decay) { delta ->
                val consumed = scrollBy(delta)

                // Check if the target entered the screen
                if (abs(value - estimatedTarget) < viewPortHeight / 2) {
                    this.cancelAnimation()
                }

                consumed
            }

            // Now that the target position is visible, adjust the animation to land on the
            // closest item.
            val finalTarget = state.layoutInfo.visibleItemsInfo
                .map { animationState.value + it.unadjustedOffset }
                .minByOrNull { abs(it - estimatedTarget) } ?: estimatedTarget

            animationState.animateDecayTo(
                finalTarget,
                decay,
                sequentialAnimation = true
            ) { delta -> scrollBy(delta) }
        } else {
            // The fling was too slow (or not even a fling), just animate a snap to the item already
            // in the center.
            var lastValue = 0f
            animationState.animateTo(
                targetValue = -state.centerItemScrollOffset.toFloat(),
                sequentialAnimation = true
            ) {
                scrollBy(value - lastValue)
                lastValue = value
            }
        }

        return animationState.velocity
    }
}

internal suspend fun <V : AnimationVector> AnimationState<Float, V>.animateDecayTo(
    targetValue: Float,
    decay: DecayAnimationSpec<Float>,
    // Indicates whether the animation should start from last frame
    sequentialAnimation: Boolean = false,
    block: AnimationScope<Float, V>.(delta: Float) -> Float
) {
    var lastValue = value
    val target = decay.calculateTargetValue(initialValue = value, initialVelocity = velocity)
    val velocityAdjustment = (targetValue - value) / (target - value)
    animateDecay(decay, sequentialAnimation = sequentialAnimation) {
        val delta = (value - lastValue) * velocityAdjustment
        val consumed = block(delta)
        lastValue = value

        // avoid rounding errors and stop if anything is unconsumed
        if (abs(delta - consumed) > 0.5f) this.cancelAnimation()
    }
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
class PickerState constructor(
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
