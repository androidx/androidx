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

import androidx.glance.GlanceModifier
import androidx.glance.layout.EmittableColumn
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
import androidx.glance.text.EmittableText
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GlanceMappedNodeMatcherTest {
    @Test
    fun matchAny_match_returnsTrue() {
        val node1 = GlanceMappedNode(
            EmittableText().apply {
                text = "node1"
            }
        )
        val node2 = GlanceMappedNode(
            EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
                EmittableText().apply { text = "node2" }
            }
        )

        val result = hasTestTag("existing-test-tag").matchesAny(listOf(node1, node2))

        assertThat(result).isTrue()
    }

    @Test
    fun matchAny_noMatch_returnsFalse() {
        val node1 = GlanceMappedNode(
            EmittableText().apply {
                text = "node1"
            }
        )
        val node2 = GlanceMappedNode(
            EmittableColumn().apply {
                children += EmittableText().apply {
                    text = "node2"
                    // this won't be inspected, as EmittableColumn node is being run against
                    // matcher, not its children
                    modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
                }
            }
        )

        val result = hasTestTag("existing-test-tag").matchesAny(listOf(node1, node2))

        assertThat(result).isFalse()
    }

    @Test
    fun andBetweenMatchers_match_returnsTrue() {
        val node = GlanceMappedNode(
            EmittableText().apply {
                text = "node"
                modifier = GlanceModifier.semantics { testTag = "test-tag" }
            }
        )

        val result = (hasTestTag("test-tag") and hasText("node")).matches(node)

        assertThat(result).isTrue()
    }

    @Test
    fun andBetweenMatchers_partialMatch_returnsFalse() {
        val node = GlanceMappedNode(
            EmittableText().apply {
                text = "node"
                modifier = GlanceModifier.semantics { testTag = "test-tag" }
            }
        )

        val result = (hasTestTag("test-tag") and hasText("non-existing-node"))
            .matches(node)

        assertThat(result).isFalse()
    }

    @Test
    fun andBetweenMatchers_noMatch_returnsFalse() {
        val node = GlanceMappedNode(
            EmittableText().apply {
                text = "node"
                modifier = GlanceModifier.semantics { testTag = "test-tag" }
            }
        )

        val result = (hasTestTag("non-existing") and hasText("non-existing-node"))
            .matches(node)

        assertThat(result).isFalse()
    }

    @Test
    fun orBetweenMatchers_bothMatch_returnsTrue() {
        val node = GlanceMappedNode(
            EmittableText().apply {
                text = "node"
                modifier = GlanceModifier.semantics { testTag = "test-tag" }
            }
        )

        val result = (hasTestTag("test-tag") or hasText("node"))
            .matches(node)

        assertThat(result).isTrue()
    }

    @Test
    fun orBetweenMatchers_secondMatch_returnsTrue() {
        val node = GlanceMappedNode(
            EmittableText().apply {
                text = "node"
                modifier = GlanceModifier.semantics { testTag = "test-tag" }
            }
        )

        val result = (hasTestTag("non-existing-tag") or hasText("node"))
            .matches(node)

        assertThat(result).isTrue()
    }

    @Test
    fun orBetweenMatchers_noneMatch_returnsFalse() {
        val node = GlanceMappedNode(
            EmittableText().apply {
                text = "node"
                modifier = GlanceModifier.semantics { testTag = "test-tag" }
            }
        )

        val result = (hasTestTag("non-existing-tag") or hasText("non-existing-node"))
            .matches(node)

        assertThat(result).isFalse()
    }

    @Test
    fun not_match_returnsTrue() {
        val node = GlanceMappedNode(
            EmittableText().apply {
                text = "node"
                modifier = GlanceModifier.semantics { testTag = "test-tag" }
            }
        )

        val result = (!hasTestTag("non-existing-test-tag")).matches(node)

        assertThat(result).isTrue()
    }

    @Test
    fun not_noMatch_returnsFalse() {
        val node = GlanceMappedNode(
            EmittableText().apply {
                text = "node"
                modifier = GlanceModifier.semantics { testTag = "test-tag" }
            }
        )

        val result = (!hasTestTag("test-tag")).matches(node)

        assertThat(result).isFalse()
    }

    @Test
    fun hasTestTag_match_returnsTrue() {
        // a single node that will be matched against matcher returned by the filter under test
        val testSingleNode = GlanceMappedNode(
            EmittableText().apply {
                text = "some text"
                modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
            }
        )

        val result = hasTestTag("existing-test-tag").matches(testSingleNode)

        assertThat(result).isTrue()
    }

    @Test
    fun hasTestTag_noMatch_returnsFalse() {
        val testSingleNode = GlanceMappedNode(
            EmittableText().apply {
                text = "some text"
                modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
            }
        )

        val result = hasTestTag("non-existing-test-tag").matches(testSingleNode)

        assertThat(result).isFalse()
    }

    @Test
    fun hasTextEqualTo_match_returnsTrue() {
        val testSingleNode = GlanceMappedNode(
            EmittableText().apply {
                text = "existing text"
            }
        )

        val result = hasTextEqualTo("existing text").matches(testSingleNode)

        assertThat(result).isTrue()
    }

    @Test
    fun hasTextEqualTo_noMatch_returnsFalse() {
        val testSingleNode = GlanceMappedNode(
            EmittableText().apply {
                text = "existing text"
            }
        )

        val result = hasTextEqualTo("non-existing text").matches(testSingleNode)

        assertThat(result).isFalse()
    }

    @Test
    fun hasTextEqualTo_caseInsensitiveMatch_returnsTrue() {
        val testSingleNode = GlanceMappedNode(
            EmittableText().apply {
                text = "some EXISTING text"
            }
        )

        val result =
            hasTextEqualTo(
                text = "SOME existing TEXT",
                ignoreCase = true
            ).matches(testSingleNode)

        assertThat(result).isTrue()
    }

    @Test
    fun hasTextEqualTo_caseInsensitiveButNoMatch_returnsFalse() {
        val testSingleNode = GlanceMappedNode(
            EmittableText().apply {
                text = "some EXISTING text"
            }
        )

        val result =
            hasTextEqualTo(
                text = "SOME non-existing TEXT",
                ignoreCase = true
            ).matches(testSingleNode)

        assertThat(result).isFalse()
    }

    @Test
    fun hasText_match_returnsTrue() {
        val testSingleNode = GlanceMappedNode(
            EmittableText().apply {
                text = "some existing text"
            }
        )

        val result = hasText("existing").matches(testSingleNode)

        assertThat(result).isTrue()
    }

    @Test
    fun hasText_noMatch_returnsFalse() {
        val testSingleNode = GlanceMappedNode(
            EmittableText().apply {
                text = "some existing text"
            }
        )

        val result = hasText("non-existing").matches(testSingleNode)

        assertThat(result).isFalse()
    }

    @Test
    fun hasText_insensitiveMatch_returnsTrue() {
        val testSingleNode = GlanceMappedNode(
            EmittableText().apply {
                text = "some EXISTING text"
            }
        )

        val result = hasText(
            text = "existing",
            ignoreCase = true
        ).matches(testSingleNode)

        assertThat(result).isTrue()
    }

    @Test
    fun hasText_caseInsensitiveButNoMatch_returnsFalse() {
        val testSingleNode = GlanceMappedNode(
            EmittableText().apply {
                text = "some EXISTING text"
            }
        )

        val result = hasText(
            text = "non-EXISTING",
            ignoreCase = true
        ).matches(testSingleNode)

        assertThat(result).isFalse()
    }
}
