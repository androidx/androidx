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

package androidx.compose.foundation.text2

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.editableText
import androidx.compose.ui.semantics.textSelectionRange
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange

@OptIn(ExperimentalFoundationApi::class)
internal class TextFieldContentSemanticsElement(
    private val state: TextFieldState
) : ModifierNodeElement<TextFieldContentSemanticsNode>() {
    override fun create(): TextFieldContentSemanticsNode = TextFieldContentSemanticsNode(state)

    override fun update(node: TextFieldContentSemanticsNode): TextFieldContentSemanticsNode {
        node.state = state
        return node
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextFieldContentSemanticsElement) return false

        if (state != other.state) return false

        return true
    }

    override fun hashCode(): Int {
        return state.hashCode()
    }

    override fun InspectorInfo.inspectableProperties() {
        // Show nothing in the inspector.
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal data class TextFieldContentSemanticsNode(
    var state: TextFieldState
) : Modifier.Node(), SemanticsModifierNode {

    private var lastText: AnnotatedString? = null
    private var lastSelection: TextRange? = null

    private var _semanticsConfiguration: SemanticsConfiguration? = null

    private fun generateSemantics(
        text: AnnotatedString,
        selection: TextRange
    ): SemanticsConfiguration {
        lastText = text
        lastSelection = selection
        return SemanticsConfiguration().also {
            it.editableText = text
            it.textSelectionRange = selection
            _semanticsConfiguration = it
        }
    }

    override val semanticsConfiguration: SemanticsConfiguration
        get() {
            var localSemantics = _semanticsConfiguration
            val value = state.value
            if (localSemantics == null ||
                lastText != value.annotatedString ||
                lastSelection != value.selection
            ) {
                localSemantics = generateSemantics(value.annotatedString, value.selection)
            }
            return localSemantics
        }
}