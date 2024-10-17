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

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.internal.BasicTooltipBox
import androidx.compose.material3.internal.rememberBasicTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.flow.collectLatest

/**
 * Label component that will append a [label] to [content]. The positioning logic uses
 * [TooltipDefaults.rememberTooltipPositionProvider].
 *
 * Label appended to thumbs of Slider:
 *
 * @sample androidx.compose.material3.samples.SliderWithCustomThumbSample
 *
 * Label appended to thumbs of RangeSlider:
 *
 * @sample androidx.compose.material3.samples.RangeSliderWithCustomComponents
 * @param label composable that will be appended to [content]
 * @param modifier [Modifier] that will be applied to [content]
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 *   for the [content].
 * @param isPersistent boolean to determine if the label should be persistent. If true, then the
 *   label will always show and be anchored to [content]. if false, then the label will only show
 *   when pressing down or hovering over the [content].
 * @param content the composable that [label] will anchor to.
 */
@ExperimentalMaterial3Api
@Composable
fun Label(
    label: @Composable TooltipScope.() -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
    isPersistent: Boolean = false,
    content: @Composable () -> Unit
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    // Has the same positioning logic as PlainTooltips
    val positionProvider = TooltipDefaults.rememberTooltipPositionProvider()
    val state =
        if (isPersistent) remember { LabelStateImpl() }
        else rememberBasicTooltipState(mutatorMutex = MutatorMutex())

    var anchorBounds: MutableState<LayoutCoordinates?> = remember { mutableStateOf(null) }
    val scope = remember { TooltipScopeImpl { anchorBounds.value } }

    val wrappedContent: @Composable () -> Unit = {
        Box(modifier = Modifier.onGloballyPositioned { anchorBounds.value = it }) { content() }
    }

    BasicTooltipBox(
        positionProvider = positionProvider,
        tooltip = { scope.label() },
        state = state,
        modifier = modifier,
        focusable = false,
        enableUserInput = false,
        content = wrappedContent
    )
    HandleInteractions(
        enabled = !isPersistent,
        state = state,
        interactionSource = interactionSource
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HandleInteractions(
    enabled: Boolean,
    state: TooltipState,
    interactionSource: MutableInteractionSource
) {
    if (enabled) {
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collectLatest { interaction ->
                when (interaction) {
                    is PressInteraction.Press,
                    is DragInteraction.Start,
                    is HoverInteraction.Enter -> {
                        state.show(MutatePriority.UserInput)
                    }
                    is PressInteraction.Release,
                    is DragInteraction.Stop,
                    is HoverInteraction.Exit -> {
                        state.dismiss()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private class LabelStateImpl(
    override val isVisible: Boolean = true,
    override val isPersistent: Boolean = true,
) : TooltipState {
    override val transition: MutableTransitionState<Boolean> = MutableTransitionState(false)

    override suspend fun show(mutatePriority: MutatePriority) {}

    override fun dismiss() {}

    override fun onDispose() {}
}
