/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.foundation.BasicTooltipBox
import androidx.compose.foundation.BasicTooltipState
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.rememberBasicTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.collectLatest

/**
 * Label component that will append a [label] to [content].
 * The positioning logic uses [TooltipDefaults.rememberPlainTooltipPositionProvider].
 *
 * Label appended to thumbs of Slider:
 *
 * @sample androidx.compose.material3.samples.SliderWithCustomThumbSample
 *
 * Label appended to thumbs of RangeSlider:
 *
 * @sample androidx.compose.material3.samples.RangeSliderWithCustomComponents
 *
 * @param label composable that will be appended to [content]
 * @param modifier [Modifier] that will be applied to [content]
 * @param interactionSource the [MutableInteractionSource] representing the
 * stream of [Interaction]s for the [content].
 * @param isPersistent boolean to determine if the label should be persistent.
 * If true, then the label will always show and be anchored to [content].
 * if false, then the label will only show when pressing down or hovering over the [content].
 * @param content the composable that [label] will anchor to.
 */
@ExperimentalMaterial3Api
@Composable
fun Label(
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    isPersistent: Boolean = false,
    content: @Composable () -> Unit
) {
    // Has the same positioning logic as PlainTooltips
    val positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider()
    val state = if (isPersistent)
        remember { LabelStateImpl() }
    else
        rememberBasicTooltipState(mutatorMutex = MutatorMutex())
    BasicTooltipBox(
        positionProvider = positionProvider,
        tooltip = label,
        state = state,
        modifier = modifier,
        focusable = false,
        enableUserInput = false,
        content = content
    )
    HandleInteractions(
        enabled = !isPersistent,
        state = state,
        interactionSource = interactionSource
    )
}

@Composable
private fun HandleInteractions(
    enabled: Boolean,
    state: BasicTooltipState,
    interactionSource: MutableInteractionSource
) {
    if (enabled) {
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collectLatest { interaction ->
                when (interaction) {
                    is PressInteraction.Press,
                    is DragInteraction.Start,
                    is HoverInteraction.Enter -> { state.show(MutatePriority.UserInput) }
                    is PressInteraction.Release,
                    is DragInteraction.Stop,
                    is HoverInteraction.Exit -> { state.dismiss() }
                }
            }
        }
    }
}

private class LabelStateImpl(
    override val isVisible: Boolean = true,
    override val isPersistent: Boolean = true
) : BasicTooltipState {
    override suspend fun show(mutatePriority: MutatePriority) {}

    override fun dismiss() {}

    override fun onDispose() {}
}
