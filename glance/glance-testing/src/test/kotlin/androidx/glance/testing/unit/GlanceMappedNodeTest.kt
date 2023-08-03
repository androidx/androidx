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

package androidx.glance.testing.unit

import androidx.glance.layout.EmittableColumn
import androidx.glance.layout.EmittableSpacer
import androidx.glance.text.EmittableText
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GlanceMappedNodeTest {
    @Test
    fun nodeChildren_returnsEmittableChildrenAsGlanceNodes() {
        val childNode1 = EmittableText().apply { text = "some text" }
        val childNode2 = EmittableSpacer()
        val childNode3 = EmittableText().apply { text = "another text" }

        val glanceNode = GlanceMappedNode(
            EmittableColumn().apply {
                children.add(childNode1)
                children.add(childNode2)
                children.add(childNode3)
            }
        )

        assertThat(glanceNode.children()).containsExactly(
            GlanceMappedNode(childNode1),
            GlanceMappedNode(childNode2),
            GlanceMappedNode(childNode3)
        ).inOrder()
    }

    @Test
    fun nodeChildren_noChildren_returnsEmptyList() {
        val glanceNode = GlanceMappedNode(
            EmittableColumn()
        )

        assertThat(glanceNode.children()).isEmpty()
    }

    @Test
    fun nodeChildren_terminalEmittable_returnsEmptyList() {
        val glanceNode = GlanceMappedNode(
            EmittableText().apply { text = "test" }
        )

        assertThat(glanceNode.children()).isEmpty()
    }
}
