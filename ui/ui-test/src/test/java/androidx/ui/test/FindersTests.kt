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

import androidx.ui.core.semantics.SemanticsConfiguration
import androidx.ui.core.semantics.SemanticsNode
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
                .withSemantics(newNode {
                    testTag = "not_myTestTag"
                })
        }

        val foundNodes = findAll(hasTestTag("myTestTag"))
        assertThat(foundNodes).isEmpty()
    }

    @Test
    fun findAll_oneOutOfTwo_findsOne() {
        val node1 = newNode {
            testTag = "myTestTag"
        }
        val node2 = newNode {
            testTag = "myTestTag2"
        }

        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withSemantics(node1, node2)
        }

        val foundNodes = findAll(hasTestTag("myTestTag"))
        assertThat(foundNodes.map { it.semanticsNode }).containsExactly(node1)
    }

    @Test
    fun findAll_twoOutOfTwo_findsTwo() {
        val node1 = newNode {
            testTag = "myTestTag"
        }
        val node2 = newNode {
            testTag = "myTestTag"
        }

        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withSemantics(node1, node2)
        }

        val foundNodes = findAll(hasTestTag("myTestTag"))
        assertThat(foundNodes.map { it.semanticsNode }).containsExactly(node1, node2)
    }

    @Test
    fun findByText_matches() {
        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withProperties({
                    accessibilityLabel = "Hello World"
                })
        }

        findByText("Hello World")
    }

    @Test(expected = AssertionError::class)
    fun findByText_fails() {
        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withProperties({
                    accessibilityLabel = "Hello World"
                })
        }

        // Need to assert exists or it won't fail
        findByText("World").assertExists()
    }

    @Test
    fun findBySubstring_matches() {
        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withProperties({
                    accessibilityLabel = "Hello World"
                })
        }

        findBySubstring("World")
    }

    @Test
    fun findBySubstring_ignoreCase_matches() {
        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withProperties({
                    accessibilityLabel = "Hello World"
                })
        }

        findBySubstring("world", ignoreCase = true)
    }

    @Test(expected = AssertionError::class)
    fun findBySubstring_wrongCase_fails() {
        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withProperties({
                    accessibilityLabel = "Hello World"
                })
        }

        // Need to assert exists or it won't fail
        findBySubstring("world").assertExists()
    }

    private fun newNode(propertiesBlock: SemanticsConfiguration.() -> Unit): SemanticsNode {
        val config = SemanticsConfiguration()
        config.isSemanticBoundary = true
        config.propertiesBlock()
        return SemanticsTreeNodeStub(data = config)
    }
}