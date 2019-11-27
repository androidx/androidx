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

package androidx.ui.test

import androidx.ui.core.SemanticsTreeNode
import androidx.ui.core.semantics.SemanticsConfiguration
import androidx.ui.core.semantics.getOrNull
import androidx.ui.semantics.SemanticsProperties
import androidx.ui.semantics.accessibilityLabel
import androidx.ui.semantics.testTag
import androidx.ui.test.helpers.FakeSemanticsTreeInteraction
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.lang.AssertionError

class FindersTests {
    @Test
    fun findAll_zeroOutOfOne_findsNone() {
        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withSemantics(newNode(
                    SemanticsConfiguration().apply {
                        testTag = "not_myTestTag"
                    }
                ))
        }

        val foundNodes = findAll { getOrNull(SemanticsProperties.TestTag) == "myTestTag" }
        assertThat(foundNodes).isEmpty()
    }

    @Test
    fun findAll_oneOutOfTwo_findsOne() {
        val node1 = newNode(SemanticsConfiguration().apply {
            testTag = "myTestTag"
        })
        val node2 = newNode(SemanticsConfiguration().apply {
            testTag = "myTestTag2"
        })

        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withSemantics(node1, node2)
        }

        val foundNodes = findAll { getOrNull(SemanticsProperties.TestTag) == "myTestTag" }
        assertThat(foundNodes.map { it.semanticsTreeNode }).containsExactly(node1)
    }

    @Test
    fun findAll_twoOutOfTwo_findsTwo() {
        val node1 = newNode(
            SemanticsConfiguration().apply {
                testTag = "myTestTag"
            }
        )
        val node2 = newNode(
            SemanticsConfiguration().apply {
                testTag = "myTestTag"
            }
        )

        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withSemantics(node1, node2)
        }

        val foundNodes = findAll { getOrNull(SemanticsProperties.TestTag) == "myTestTag" }
        assertThat(foundNodes.map { it.semanticsTreeNode }).containsExactly(node1, node2)
    }

    @Test
    fun findByText_matches() {
        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withProperties(SemanticsConfiguration().also {
                    it.accessibilityLabel = "Hello World"
                })
        }

        findByText("Hello World")
    }

    @Test(expected = AssertionError::class)
    fun findByText_fails() {
        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withProperties(SemanticsConfiguration().also {
                    it.accessibilityLabel = "Hello World"
                })
        }

        findByText("World")
    }

    @Test
    fun findBySubstring_matches() {
        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withProperties(SemanticsConfiguration().also {
                    it.accessibilityLabel = "Hello World"
                })
        }

        findBySubstring("World")
    }

    @Test
    fun findBySubstring_ignoreCase_matches() {
        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withProperties(SemanticsConfiguration().also {
                    it.accessibilityLabel = "Hello World"
                })
        }

        findBySubstring("world", ignoreCase = true)
    }

    @Test(expected = AssertionError::class)
    fun findBySubstring_wrongCase_fails() {
        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withProperties(SemanticsConfiguration().also {
                    it.accessibilityLabel = "Hello World"
                })
        }

        findBySubstring("world")
    }

    private fun newNode(properties: SemanticsConfiguration): SemanticsTreeNode {
        return SemanticsTreeNodeStub(data = properties)
    }
}