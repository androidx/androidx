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

package androidx.ui.core.focus

import androidx.test.filters.SmallTest
import androidx.ui.core.InnerPlaceable
import androidx.ui.core.LayoutModifier
import androidx.ui.core.LayoutNode
import androidx.ui.core.ModifiedLayoutNode
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class FindParentFocusNodeTest {

    @Test
    fun noParentReturnsNull() {
        // Arrange.
        val layoutNode = LayoutNode()
        val focusNode = ModifiedFocusNode(InnerPlaceable(layoutNode))
        layoutNode.layoutNodeWrapper = focusNode

        // Act.
        val parentFousNode = focusNode.findParentFocusNode()

        // Assert.
        assertThat(parentFousNode).isNull()
    }

    @Test
    fun returnsImmediateParentFromModifierChain() {
        // Arrange.
        // focusNode1--focusNode2--focusNode3--focusNode4--focusNode5
        val focusNode5 = ModifiedFocusNode(InnerPlaceable(LayoutNode()))
        val focusNode4 = ModifiedFocusNode(focusNode5)
        val focusNode3 = ModifiedFocusNode(focusNode4)
        val focusNode2 = ModifiedFocusNode(focusNode3)
        val focusNode1 = ModifiedFocusNode(focusNode2)

        // Act.
        val parent = focusNode3.findParentFocusNode()

        // Assert.
        assertThat(parent).isEqualTo(focusNode2)
    }

    @Test
    fun returnsImmediateParentFromModifierChain_ignoresNonFocusModifiers() {
        // Arrange.
        // focusNode1--focusNode2--nonFocusNode--focusNode3
        val focusNode3 = ModifiedFocusNode(InnerPlaceable(LayoutNode()))
        val nonFocusNode = ModifiedLayoutNode(focusNode3, LayoutModifierTestImpl())
        val focusNode2 = ModifiedFocusNode(nonFocusNode)
        val focusNode1 = ModifiedFocusNode(focusNode2)

        // Act.
        val parent = focusNode3.findParentFocusNode()

        // Assert.
        assertThat(parent).isEqualTo(focusNode2)
    }

    @Test
    fun returnsLastFocusParentFromParentLayoutNode() {
        // Arrange.
        // parentLayoutNode--parentFocusNode1--parentFocusNode2
        //       |
        // layoutNode--focusNode
        val parentLayoutNode = LayoutNode()
        val parentFocusNode2 = ModifiedFocusNode(InnerPlaceable(parentLayoutNode))
        val parentFocusNode1 = ModifiedFocusNode(parentFocusNode2)
        parentLayoutNode.layoutNodeWrapper = parentFocusNode1

        val layoutNode = LayoutNode()
        val focusNode = ModifiedFocusNode(InnerPlaceable(layoutNode))
        layoutNode.layoutNodeWrapper = focusNode

        parentLayoutNode.insertAt(0, layoutNode)

        // Act.
        val parent = focusNode.findParentFocusNode()

        // Assert.
        assertThat(parent).isEqualTo(parentFocusNode2)
    }

    @Test
    fun returnsImmediateParent() {
        // Arrange.
        // grandparentLayoutNode--grandparentFocusNode
        //       |
        // parentLayoutNode--parentFocusNode
        //       |
        // layoutNode--focusNode
        val grandparentLayoutNode = LayoutNode()
        val grandparentFocusNode = ModifiedFocusNode(InnerPlaceable(grandparentLayoutNode))
        grandparentLayoutNode.layoutNodeWrapper = grandparentFocusNode

        val parentLayoutNode = LayoutNode()
        val parentFocusNode = ModifiedFocusNode(InnerPlaceable(parentLayoutNode))
        parentLayoutNode.layoutNodeWrapper = parentFocusNode

        val layoutNode = LayoutNode()
        val focusNode = ModifiedFocusNode(InnerPlaceable(layoutNode))
        layoutNode.layoutNodeWrapper = focusNode

        parentLayoutNode.insertAt(0, layoutNode)
        grandparentLayoutNode.insertAt(0, parentLayoutNode)

        // Act.
        val parent = focusNode.findParentFocusNode()

        // Assert.
        assertThat(parent).isEqualTo(parentFocusNode)
    }

    @Test
    fun ignoresIntermediateLayoutNodesThatDontHaveFocusNodes() {
        // Arrange.
        // grandparentLayoutNode--grandparentFocusNode
        //       |
        // parentLayoutNode
        //       |
        // layoutNode--focusNode
        val grandparentLayoutNode = LayoutNode()
        val grandparentFocusNode = ModifiedFocusNode(InnerPlaceable(grandparentLayoutNode))
        grandparentLayoutNode.layoutNodeWrapper = grandparentFocusNode

        val parentLayoutNode = LayoutNode()

        val layoutNode = LayoutNode()
        val focusNode = ModifiedFocusNode(InnerPlaceable(layoutNode))
        layoutNode.layoutNodeWrapper = focusNode

        parentLayoutNode.insertAt(0, layoutNode)
        grandparentLayoutNode.insertAt(0, parentLayoutNode)

        // Act.
        val parent = focusNode.findParentFocusNode()

        // Assert.
        assertThat(parent).isEqualTo(grandparentFocusNode)
    }

    @Suppress("Deprecation")
    private class LayoutModifierTestImpl : LayoutModifier
}
