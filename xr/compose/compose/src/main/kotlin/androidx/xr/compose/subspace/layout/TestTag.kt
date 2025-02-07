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

package androidx.xr.compose.subspace.layout

import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.testTag
import androidx.xr.compose.subspace.node.SubspaceModifierElement
import androidx.xr.compose.subspace.node.SubspaceSemanticsModifierNode

/**
 * Applies a tag to allow modified element to be found in tests.
 *
 * This is a convenience method for a [semantics] that sets [SemanticsPropertyReceiver.testTag].
 */
public fun SubspaceModifier.testTag(tag: String): SubspaceModifier = this then TestTagElement(tag)

private class TestTagElement(private val tag: String) : SubspaceModifierElement<TestTagNode>() {
    override fun create(): TestTagNode {
        return TestTagNode(tag)
    }

    override fun update(node: TestTagNode) {
        node.tag = tag
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

public class TestTagNode(public var tag: String) :
    SubspaceModifier.Node(), SubspaceSemanticsModifierNode {
    override fun SemanticsPropertyReceiver.applySemantics() {
        testTag = tag
    }
}
