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
import androidx.ui.core.FocusNode
import androidx.ui.core.LayoutNode
import androidx.ui.core.PointerInputNode
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
        val focusNode = FocusNode()

        // Act.
        val parentFousNode = focusNode.findParentFocusNode()

        // Assert.
        assertThat(parentFousNode).isNull()
    }

    @Test
    fun returnsParent() {
        // Arrange.
        val focusNode = FocusNode()
        val parentFocusNode = FocusNode()
        parentFocusNode.insertAt(0, focusNode)

        // Act.
        val parent = focusNode.findParentFocusNode()

        // Assert.
        assertThat(parent).isEqualTo(parentFocusNode)
    }

    @Test
    fun returnsImmediateParent() {
        // Arrange.
        val focusNode = FocusNode()
        val parentFocusNode = FocusNode()
        val grandparentFocusNode = FocusNode()
        grandparentFocusNode.insertAt(0, parentFocusNode)
        parentFocusNode.insertAt(0, focusNode)

        // Act.
        val parent = focusNode.findParentFocusNode()

        // Assert.
        assertThat(parent).isEqualTo(parentFocusNode)
    }

    @Test
    fun ignoresIntermediateComponentNodes() {
        // Arrange.
        val focusNode = FocusNode()
        val intermediatePointerInputNode = PointerInputNode()
        val intermediateLayoutNode = LayoutNode()
        val parentFocusNode = FocusNode()
        parentFocusNode.insertAt(0, intermediatePointerInputNode)
        intermediatePointerInputNode.insertAt(0, intermediateLayoutNode)
        intermediateLayoutNode.insertAt(0, focusNode)

        // Act.
        val parent = focusNode.findParentFocusNode()

        // Assert.
        assertThat(parent).isEqualTo(parentFocusNode)
    }
}