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
import androidx.ui.test.SemanticsNodeInteraction
import androidx.ui.test.SemanticsTreeInteraction
import androidx.ui.test.SemanticsTreeNodeStub

internal class FakeSemanticsTreeInteraction internal constructor(
    private val selector: SemanticsConfiguration.() -> Boolean
) : SemanticsTreeInteraction() {

    private lateinit var semanticsToUse: List<SemanticsNodeInteraction>

    fun withProperties(
        vararg properties: SemanticsConfiguration
    ): FakeSemanticsTreeInteraction {
        semanticsToUse = properties.map {
            SemanticsNodeInteraction(SemanticsTreeNodeStub(/* data= */ it), this)
        }.toList()
        return this
    }

    fun withSemantics(vararg nodes: SemanticsTreeNode): FakeSemanticsTreeInteraction {
        semanticsToUse = nodes.toList().map {
            SemanticsNodeInteraction(it, this)
        }.toList()
        return this
    }

    override fun findAllMatching(): List<SemanticsNodeInteraction> {
        // TODO(pavlis): This is too simplified, use more of the real code so we test more than
        // just a lambda correctness.

        return semanticsToUse
            .filter { node -> node.semanticsTreeNode.data.selector() }
    }

    override fun findOne(): SemanticsNodeInteraction {
        val foundNodes = findAllMatching()

        if (foundNodes.size != 1) {
            throw AssertionError("Found '${foundNodes.size}' nodes but exactly '1' was expected!")
        }

        return foundNodes.first()
    }

    override fun sendClick(x: Float, y: Float) {
        TODO("replace with host side interaction")
    }

    override fun contains(semanticsConfiguration: SemanticsConfiguration): Boolean {
        return semanticsToUse
            .map { it.semanticsTreeNode.data }
            .contains(semanticsConfiguration)
    }
}