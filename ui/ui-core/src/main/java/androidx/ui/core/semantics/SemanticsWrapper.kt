/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.core.semantics

import androidx.ui.core.DelegatingLayoutNodeWrapper
import androidx.ui.core.LayoutNodeWrapper
import androidx.ui.util.fastForEach

internal class SemanticsWrapper(
    wrapped: LayoutNodeWrapper,
    val semanticsModifier: SemanticsModifier
) : DelegatingLayoutNodeWrapper<SemanticsModifier>(wrapped, semanticsModifier) {
    fun semanticsNode(): SemanticsNode {
        return SemanticsNode(modifier.id,
            collapsedSemanticsConfiguration(),
            layoutNode)
    }

    fun collapsedSemanticsConfiguration(): SemanticsConfiguration {
        var config = SemanticsConfiguration()
        collapseChainedSemanticsIntoTopConfig(config, this)
        return config
    }

    override fun toString(): String {
        return "${super.toString()} localConfig: ${modifier.semanticsConfiguration}"
    }

    /**
     * "Collapses" together the [SemanticsConfiguration]s that will apply to the child [LayoutNode].
     *
     * This ignores semantic boundaries (because they only apply once the node
     * is built), and currently does not validate that a [LayoutNode] actually
     * exists as a child (though this is not contractual)
     */
    private fun collapseChainedSemanticsIntoTopConfig(
        parentConfig: SemanticsConfiguration,
        topNodeOfConfig: SemanticsWrapper
    ) {
        parentConfig.absorb(modifier.semanticsConfiguration, ignoreAlreadySet = true)

        if (semanticsModifier.applyToChildLayoutNode) {
            // Recursively collapse the chain, if we have an immediate child
            findOneImmediateChild()?.collapseChainedSemanticsIntoTopConfig(
                parentConfig, topNodeOfConfig)
        } else {
            // Collapse locally within the modifiers directly applying to the current layout node
            val innerConfig = wrapped.nearestSemantics?.collapsedSemanticsConfiguration()
            if (innerConfig != null) {
                parentConfig.absorb(innerConfig, ignoreAlreadySet = true)
            }
        }
    }

    // This searches the children down only one level of the tree and returns one child found.
    // Note that this assumes each LayoutNode only has one semantics modifier for now.
    private fun findOneImmediateChild(): SemanticsWrapper? {
        var immediateChild: SemanticsWrapper? = null
        layoutNode.children.fastForEach { child ->
            if (child.outerSemantics != null) {
                immediateChild = child.outerSemantics
            }
        }
        return immediateChild
    }
}
