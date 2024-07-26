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

package androidx.compose.foundation

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalInputModeManager
import kotlinx.coroutines.flow.flow

internal actual fun platformIndication(indication: Indication?) =
    inputModeFilterIndication(indication)

// TODO https://youtrack.jetbrains.com/issue/CMP-5814/Review-Request-focus-on-click-feature
/**
 * When in Touch mode, skip the Focus interaction - its indication should not be drawn
 */
internal fun inputModeFilterIndication(indication: Indication?): Indication? {
    return when (indication) {
        null -> null
        is IndicationNodeFactory -> InputModeFilterIndicationNodeFactory(indication)
        else -> InputModeFilterIndication(indication)
    }
}

internal class InputModeFilterIndicationNodeFactory(
    private val original: IndicationNodeFactory
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        val filteredSource = InputModeFilterInteractionSource(interactionSource)
        return object : DelegatingNode(), CompositionLocalConsumerModifierNode {
            init {
                delegate(original.create(filteredSource))
            }

            override fun onAttach() {
                filteredSource.inputModeManager = currentValueOf(LocalInputModeManager)
            }

            override fun onDetach() {
                filteredSource.inputModeManager = null
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as InputModeFilterIndicationNodeFactory

        return original == other.original
    }

    override fun hashCode(): Int {
        return original.hashCode()
    }
}

@Suppress("DEPRECATION_ERROR", "OVERRIDE_DEPRECATION")
internal class InputModeFilterIndication(
    private val original: Indication
) : Indication {
    @Composable
    override fun rememberUpdatedInstance(
        interactionSource: InteractionSource
    ): IndicationInstance {
        val inputModeManager = LocalInputModeManager.current
        return super.rememberUpdatedInstance(
            InputModeFilterInteractionSource(interactionSource, inputModeManager)
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as InputModeFilterIndication

        return original == other.original
    }

    override fun hashCode(): Int {
        return original.hashCode()
    }
}

private class InputModeFilterInteractionSource(
    original: InteractionSource,
    var inputModeManager: InputModeManager? = null
) : InteractionSource {
    private val isKeyboardMode get() = inputModeManager == null ||
        inputModeManager?.inputMode == InputMode.Keyboard

    override val interactions = flow {
        // keep tracking counts to always send symmetric Focus/Unfocus
        var actualFocusCount = 0
        var sentFocusCount = 0

        original.interactions.collect {
            if (it !is FocusInteraction) {
                emit(it)
            } else {
                // if it is already focused, we always show indication for simplicity
                // (otherwise we have to generate multiple synthetic Unfocus)
                if (actualFocusCount > 0 && sentFocusCount > 0 ||
                    actualFocusCount == 0 && isKeyboardMode) {
                    when (it) {
                        is FocusInteraction.Focus -> sentFocusCount++
                        is FocusInteraction.Unfocus -> sentFocusCount--
                    }

                    emit(it)
                }

                when (it) {
                    is FocusInteraction.Focus -> actualFocusCount++
                    is FocusInteraction.Unfocus -> actualFocusCount--
                }
            }
        }
    }
}
