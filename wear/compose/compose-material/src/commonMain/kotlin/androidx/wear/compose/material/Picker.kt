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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A scrollable list of items to pick from. By default, items will be repeated
 * "infinitely" in both directions, unless [repeatItems] is specified as false.
 *
 * @sample androidx.wear.compose.material.samples.SimplePicker
 *
 * @param numberOfOptions the number of options
 * @param state The state of the component
 * @param modifier Modifier to be applied to the Picker
 * @param scalingParams the parameters to configure the scaling and transparency effects for the
 * component. See [ScalingParams]
 * @param repeatItems if true (the default), the contents of the component will be repeated
 * "infinitely" in both directions. If false, the elements will appear only once.
 * @param separation the amount of separation in [Dp] between items. Can be negative, which can be
 * useful for Text if it has plenty of whitespace.
 * @param option a block which describes the content. Inside this block you can reference
 * [PickerScope.selectedOption] and other properties in [PickerScope]
 */
@Composable
fun Picker(
    numberOfOptions: Int,
    state: PickerState,
    modifier: Modifier = Modifier,
    scalingParams: ScalingParams = PickerDefaults.scalingParams(),
    repeatItems: Boolean = true,
    separation: Dp = 0.dp,
    option: @Composable PickerScope.(optionIndex: Int) -> Unit
) {
    require(numberOfOptions > 0) { "The picker should have at least one item." }

    SideEffect {
        state.itemCount = numberOfOptions
    }

    val repeatTarget = if (repeatItems) 100_000_000 / numberOfOptions else 1
    if (repeatItems) {
        LaunchedEffect(state, numberOfOptions) {
            // Scroll to the middle block.
            state.scalingLazyListState.lazyListState.scrollToItem(
                numberOfOptions * (repeatTarget / 2),
                0
            )
        }
    }
    val pickerScope = remember(state) { PickerScopeImpl(state) }
    ScalingLazyColumn(
        modifier = modifier,
        state = state.scalingLazyListState,
        content = {
            items(numberOfOptions * repeatTarget) { ix ->
                with(pickerScope) {
                    option(ix % numberOfOptions)
                }
            }
        },
        contentPadding = PaddingValues(0.dp),
        scalingParams = scalingParams,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = separation
        )
    )
}

/**
 * Creates a [PickerState] that is remembered across compositions.
 */
@Composable
public fun rememberPickerState() = rememberSaveable(saver = PickerState.Saver) {
    PickerState(ScalingLazyListState())
}

/**
 * A state object that can be hoisted to observe item selection.
 *
 * In most cases, this will be created via [rememberPickerState].
 */
@Stable
class PickerState internal constructor(internal val scalingLazyListState: ScalingLazyListState) {
    /**
     * Index of the item selected (i.e., at the center)
     */
    val selectedOption: Int
        get() = if (itemCount == 0) {
            0
        } else {
            scalingLazyListState.layoutInfo.centralItemIndex % itemCount
        }

    /**
     * Amount of items in the picker
     */
    internal var itemCount by mutableStateOf(0)
        internal set

    companion object {
        /**
         * The default [Saver] implementation for [PickerState].
         */
        val Saver = listSaver<PickerState, Any>(
            save = {
                val scalingLazyListStateSaveable = with(ScalingLazyListState.Saver) {
                    save(it.scalingLazyListState)
                }
                listOf(
                    scalingLazyListStateSaveable!!,
                    it.itemCount
                )
            },
            restore = { saved ->
                @Suppress("UNCHECKED_CAST")
                (saved[0] as? List<Any>)
                    ?.let { ScalingLazyListState.Saver.restore(it) }
                    ?.let {
                        PickerState(it).apply {
                            itemCount = saved[1] as Int
                        }
                    }
            }
        )
    }

    // TODO(): provide a way to make an item selected, once we get support from the ScalingLazyColumn
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
        minTransitionArea: Float = 0.9f,
        maxTransitionArea: Float = 0.9f,
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
