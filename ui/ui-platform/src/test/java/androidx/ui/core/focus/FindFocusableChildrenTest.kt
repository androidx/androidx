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

package androidx.ui.core.focus

import androidx.test.filters.SmallTest
import androidx.ui.core.InnerPlaceable
import androidx.ui.core.LayoutModifier
import androidx.ui.core.LayoutNode
import androidx.ui.core.ModifiedLayoutNode
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class FindFocusableChildrenTest {
    @Test
    fun returnsFirstFocusNodeInModifierChain() {
        // Arrange.
        // layoutNode--focusNode1--focusNode2--focusNode3
        val layoutNode = LayoutNode()
        val focusNode3 = ModifiedFocusNode(InnerPlaceable(LayoutNode()))
        val focusNode2 = ModifiedFocusNode(focusNode3)
        val focusNode1 = ModifiedFocusNode(focusNode2)
        layoutNode.layoutNodeWrapper = focusNode1

        // Act.
        val focusableChildren = focusNode1.focusableChildren()

        // Assert.
        Truth.assertThat(focusableChildren).containsExactly(focusNode2)
    }

    @Test
    fun skipsNonFocusNodesAndReturnsFirstFocusNodeInModifierChain() {
        // Arrange.
        // layoutNode--focusNode1--nonFocusNode--focusNode2
        val layoutNode = LayoutNode()
        val focusNode2 = ModifiedFocusNode(InnerPlaceable(layoutNode))
        val nonFocusNode = ModifiedLayoutNode(focusNode2, LayoutModifierTestImpl())
        val focusNode1 = ModifiedFocusNode(nonFocusNode)
        layoutNode.layoutNodeWrapper = focusNode1

        // Act.
        val focusableChildren = focusNode1.focusableChildren()

        // Assert.
        Truth.assertThat(focusableChildren).containsExactly(focusNode2)
    }

    @Test
    fun returnsFirstFocusChildOfEachChildLayoutNode() {
        // Arrange.
        // parentLayoutNode--parentFocusNode
        //       |___________________________________
        //       |                                   |
        // childLayoutNode1--focusNode1          childLayoutNode2--focusNode2--focusNode3
        val parentLayoutNode = LayoutNode()
        val parentFocusNode = ModifiedFocusNode(InnerPlaceable(parentLayoutNode))
        parentLayoutNode.layoutNodeWrapper = parentFocusNode

        val childLayoutNode1 = LayoutNode()
        val focusNode1 = ModifiedFocusNode(InnerPlaceable(childLayoutNode1))
        childLayoutNode1.layoutNodeWrapper = focusNode1
        parentLayoutNode.insertAt(0, childLayoutNode1)

        val childLayoutNode2 = LayoutNode()
        val focusNode3 = ModifiedFocusNode(InnerPlaceable(childLayoutNode2))
        val focusNode2 = ModifiedFocusNode(focusNode3)
        childLayoutNode2.layoutNodeWrapper = focusNode2
        parentLayoutNode.insertAt(1, childLayoutNode2)

        Truth.assertThat(parentLayoutNode.children).hasSize(2)
        Truth.assertThat(parentLayoutNode.layoutChildren).hasSize(2)

        // Act.
        val focusableChildren = parentFocusNode.focusableChildren()

        // Assert.
        Truth.assertThat(focusableChildren).containsExactly(focusNode1, focusNode2)
    }

    private class LayoutModifierTestImpl : LayoutModifier
}
