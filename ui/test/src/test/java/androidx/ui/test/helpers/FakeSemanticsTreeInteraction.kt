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

package androidx.ui.test.helpers

import androidx.ui.core.SemanticsTreeNode
import androidx.ui.core.semantics.SemanticsConfiguration
import androidx.ui.test.SemanticsTreeInteraction
import androidx.ui.test.SemanticsTreeNodeStub

class FakeSemanticsTreeInteraction : SemanticsTreeInteraction() {

    private lateinit var semanticsToUse: List<SemanticsTreeNode>

    private val selectors = mutableListOf<(SemanticsTreeNode) -> Boolean>()

    fun withProperties(
        vararg properties: SemanticsConfiguration
    ): FakeSemanticsTreeInteraction {
        semanticsToUse = properties.map {
            SemanticsTreeNodeStub(/* data= */ it)
        }.toList()
        return this
    }

    fun withSemantics(vararg nodes: SemanticsTreeNode): FakeSemanticsTreeInteraction {
        semanticsToUse = nodes.toList()
        return this
    }

    override fun addSelector(
        selector: (SemanticsTreeNode) -> Boolean
    ): SemanticsTreeInteraction {
        selectors.add(selector)
        return this
    }

    override fun findAllMatching(): List<SemanticsTreeNode> {
        // TODO(pavlis): This is too simplified, use more of the real code so we test more than
        // just a lambda correctness.
        return semanticsToUse
            .filter { node -> selectors.all { selector -> selector(node) } }
            .toList()
    }

    override fun sendClick(x: Float, y: Float) {
        TODO("replace with host side interaction")
    }
}