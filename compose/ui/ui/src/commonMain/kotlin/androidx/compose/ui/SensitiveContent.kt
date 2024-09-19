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

package androidx.compose.ui

import androidx.compose.ui.internal.checkPrecondition
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireOwner
import androidx.compose.ui.platform.InspectorInfo

/**
 * This modifier hints that the composable renders sensitive content (i.e. username, password,
 * credit card etc) on the screen, and the content should be protected during screen share in
 * supported environments.
 *
 * @param isContentSensitive whether the content is sensitive or not. Defaults to true.
 */
fun Modifier.sensitiveContent(isContentSensitive: Boolean = true): Modifier =
    this then SensitiveNodeElement(isContentSensitive)

private data class SensitiveNodeElement(val isContentSensitive: Boolean) :
    ModifierNodeElement<SensitiveContentNode>() {
    override fun create(): SensitiveContentNode = SensitiveContentNode(isContentSensitive)

    override fun update(node: SensitiveContentNode) {
        node.isContentSensitive = isContentSensitive
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "sensitiveContent"
        properties["isContentSensitive"] = isContentSensitive
    }
}

private data class SensitiveContentNode(private var _isContentSensitive: Boolean) :
    Modifier.Node() {
    // Tracks if this node has been counted as sensitive or not.
    private var isCountedSensitive: Boolean = false

    var isContentSensitive: Boolean = _isContentSensitive
        set(value) {
            field = value
            if (isContentSensitive && !isCountedSensitive) {
                requireOwner().incrementSensitiveComponentCount()
                isCountedSensitive = true
            } else if (!isContentSensitive && isCountedSensitive) {
                requireOwner().decrementSensitiveComponentCount()
                isCountedSensitive = false
            }
        }

    override fun onAttach() {
        super.onAttach()
        if (isContentSensitive) {
            checkPrecondition(!isCountedSensitive) { "invalid sensitive content state" }
            requireOwner().incrementSensitiveComponentCount()
            isCountedSensitive = true
        }
    }

    override fun onDetach() {
        if (isCountedSensitive) {
            requireOwner().decrementSensitiveComponentCount()
            isCountedSensitive = false
        }
        super.onDetach()
    }
}
