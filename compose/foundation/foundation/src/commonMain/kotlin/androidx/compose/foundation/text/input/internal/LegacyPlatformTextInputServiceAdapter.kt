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

@file:Suppress("DEPRECATION")

package androidx.compose.foundation.text.input.internal

import androidx.compose.foundation.internal.checkPrecondition
import androidx.compose.foundation.text.LegacyTextFieldState
import androidx.compose.foundation.text.selection.TextFieldSelectionManager
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.PlatformTextInputSession
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.text.input.PlatformTextInputService
import kotlinx.coroutines.Job

internal expect fun createLegacyPlatformTextInputServiceAdapter():
    LegacyPlatformTextInputServiceAdapter

/**
 * An implementation of the legacy [PlatformTextInputService] interface that delegates to a
 * [LegacyAdaptingPlatformTextInputModifierNode].
 *
 * For this class to work, exactly one [LegacyAdaptingPlatformTextInputModifier] must be attached to
 * a layout node and passed an instance of this class. This class will only function when such a
 * modifier is attached to the modifier system, otherwise many of its operations will no-op.
 *
 * Note that, contrary to the original design intent of a [PlatformTextInputService], every text
 * field has its own instance of this class, so it does not need to worry about multiple consumers.
 */
internal abstract class LegacyPlatformTextInputServiceAdapter : PlatformTextInputService {

    protected var textInputModifierNode: LegacyPlatformTextInputNode? = null
        private set

    fun registerModifier(node: LegacyPlatformTextInputNode) {
        checkPrecondition(textInputModifierNode == null) {
            "Expected textInputModifierNode to be null"
        }
        textInputModifierNode = node
    }

    fun unregisterModifier(node: LegacyPlatformTextInputNode) {
        checkPrecondition(textInputModifierNode === node) {
            "Expected textInputModifierNode to be $node but was $textInputModifierNode"
        }
        textInputModifierNode = null
    }

    final override fun showSoftwareKeyboard() {
        textInputModifierNode?.softwareKeyboardController?.show()
    }

    final override fun hideSoftwareKeyboard() {
        textInputModifierNode?.softwareKeyboardController?.hide()
    }

    abstract fun startStylusHandwriting()

    interface LegacyPlatformTextInputNode {
        val softwareKeyboardController: SoftwareKeyboardController?
        val layoutCoordinates: LayoutCoordinates?
        val legacyTextFieldState: LegacyTextFieldState?
        val textFieldSelectionManager: TextFieldSelectionManager?
        val viewConfiguration: ViewConfiguration

        fun launchTextInputSession(block: suspend PlatformTextInputSession.() -> Nothing): Job?
    }
}
