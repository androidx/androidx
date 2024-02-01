/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.platform

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag

/**
 * Applies a tag to allow modified element to be found in tests.
 *
 * This is a convenience method for a [semantics] that sets [SemanticsPropertyReceiver.testTag].
 */
@Stable
fun Modifier.testTag(tag: String) = this then TestTagElement(tag)

private class TestTagElement(private val tag: String) :
    ModifierNodeElement<TestTagNode>() {

    override fun create(): TestTagNode {
        return TestTagNode(tag)
    }

    override fun update(node: TestTagNode) {
        node.tag = tag
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "testTag"
        properties["tag"] = tag
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TestTagElement) return false

        return tag == other.tag
    }

    override fun hashCode(): Int {
        return tag.hashCode()
    }
}

private class TestTagNode(var tag: String) : Modifier.Node(), SemanticsModifierNode {
    override fun SemanticsPropertyReceiver.applySemantics() {
        testTag = tag
    }
}
