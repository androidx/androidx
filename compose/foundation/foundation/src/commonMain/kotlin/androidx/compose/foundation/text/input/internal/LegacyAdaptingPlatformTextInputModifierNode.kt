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

package androidx.compose.foundation.text.input.internal

import androidx.compose.foundation.text.LegacyTextFieldState
import androidx.compose.foundation.text.selection.TextFieldSelectionManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.PlatformTextInputModifierNode
import androidx.compose.ui.platform.PlatformTextInputSession
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.establishTextInputSession
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Connects a [LegacyPlatformTextInputServiceAdapter] to the PlatformTextInput system. This modifier
 * must be applied to the text field in order for the [LegacyPlatformTextInputServiceAdapter] to
 * function.
 */
internal fun Modifier.legacyTextInputAdapter(
    serviceAdapter: LegacyPlatformTextInputServiceAdapter,
    legacyTextFieldState: LegacyTextFieldState,
    textFieldSelectionManager: TextFieldSelectionManager
): Modifier =
    this then
        LegacyAdaptingPlatformTextInputModifier(
            serviceAdapter,
            legacyTextFieldState,
            textFieldSelectionManager
        )

private data class LegacyAdaptingPlatformTextInputModifier(
    val serviceAdapter: LegacyPlatformTextInputServiceAdapter,
    val legacyTextFieldState: LegacyTextFieldState,
    val textFieldSelectionManager: TextFieldSelectionManager
) : ModifierNodeElement<LegacyAdaptingPlatformTextInputModifierNode>() {

    override fun create(): LegacyAdaptingPlatformTextInputModifierNode {
        return LegacyAdaptingPlatformTextInputModifierNode(
            serviceAdapter,
            legacyTextFieldState,
            textFieldSelectionManager
        )
    }

    override fun update(node: LegacyAdaptingPlatformTextInputModifierNode) {
        node.setServiceAdapter(serviceAdapter)
        node.legacyTextFieldState = legacyTextFieldState
        node.textFieldSelectionManager = textFieldSelectionManager
    }

    override fun InspectorInfo.inspectableProperties() {
        // Not a public-facing modifier.
    }
}

/**
 * Exposes [PlatformTextInputModifierNode] capabilities and other information from the modifier
 * system such as position and some composition locals to implementations of
 * [LegacyPlatformTextInputServiceAdapter].
 */
internal class LegacyAdaptingPlatformTextInputModifierNode(
    private var serviceAdapter: LegacyPlatformTextInputServiceAdapter,
    override var legacyTextFieldState: LegacyTextFieldState,
    override var textFieldSelectionManager: TextFieldSelectionManager
) :
    Modifier.Node(),
    PlatformTextInputModifierNode,
    CompositionLocalConsumerModifierNode,
    GlobalPositionAwareModifierNode,
    LegacyPlatformTextInputServiceAdapter.LegacyPlatformTextInputNode {

    override var layoutCoordinates: LayoutCoordinates? by mutableStateOf(null)
        private set

    override val softwareKeyboardController: SoftwareKeyboardController?
        get() = currentValueOf(LocalSoftwareKeyboardController)

    fun setServiceAdapter(serviceAdapter: LegacyPlatformTextInputServiceAdapter) {
        if (isAttached) {
            this.serviceAdapter.stopInput()
            this.serviceAdapter.unregisterModifier(this)
        }
        this.serviceAdapter = serviceAdapter
        if (isAttached) {
            this.serviceAdapter.registerModifier(this)
        }
    }

    override val viewConfiguration: ViewConfiguration
        get() = currentValueOf(LocalViewConfiguration)

    override fun onAttach() {
        serviceAdapter.registerModifier(this)
    }

    override fun onDetach() {
        serviceAdapter.unregisterModifier(this)
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        this.layoutCoordinates = coordinates
    }

    override fun launchTextInputSession(
        block: suspend PlatformTextInputSession.() -> Nothing
    ): Job? {
        if (!isAttached) return null
        return coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            establishTextInputSession(block)
        }
    }
}
