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

class GlanceMappedNodeFiltersAndMatcherTest {
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
                EmittableText().apply {
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
    fun hasText_match_returnsTrue() {
        val testSingleNode = GlanceMappedNode(
            EmittableText().apply {
                text = "existing text"
            }
        )

        val result = hasText("existing text").matches(testSingleNode)

        assertThat(result).isTrue()
    }

    @Test
    fun hasText_noMatch_returnsFalse() {
        val testSingleNode = GlanceMappedNode(
            EmittableText().apply {
                text = "existing text"
            }
        )

        val result = hasText("non-existing text").matches(testSingleNode)

        assertThat(result).isFalse()
    }

    @Test
    fun hasText_subStringMatch_returnsTrue() {
        val testSingleNode = GlanceMappedNode(
            EmittableText().apply {
                text = "some existing text"
            }
        )

        val result = hasText(text = "existing", substring = true).matches(testSingleNode)

        assertThat(result).isTrue()
    }

    @Test
    fun hasText_subStringNoMatch_returnsFalse() {
        val testSingleNode = GlanceMappedNode(
            EmittableText().apply {
                text = "some existing text"
            }
        )

        val result = hasText(text = "non-existing", substring = true).matches(testSingleNode)

        assertThat(result).isFalse()
    }

    @Test
    fun hasText_subStringCaseInsensitiveMatch_returnsTrue() {
        val testSingleNode = GlanceMappedNode(
            EmittableText().apply {
                text = "some EXISTING text"
            }
        )

        val result =
            hasText(text = "existing", substring = true, ignoreCase = true).matches(testSingleNode)

        assertThat(result).isTrue()
    }

    @Test
    fun hasText_subStringCaseInsensitiveNoMatch_returnsFalse() {
        val testSingleNode = GlanceMappedNode(
            EmittableText().apply {
                text = "some EXISTING text"
            }
        )

        val result =
            hasText(text = "non-EXISTING", substring = true, ignoreCase = true)
                .matches(testSingleNode)

        assertThat(result).isFalse()
    }

    @Test
    fun hasText_caseInsensitiveMatch_returnsTrue() {
        val testSingleNode = GlanceMappedNode(
            EmittableText().apply {
                text = "some EXISTING text"
            }
        )

        val result =
            hasText(text = "SOME existing TEXT", ignoreCase = true).matches(testSingleNode)

        assertThat(result).isTrue()
    }

    @Test
    fun hasText_caseInsensitiveNoMatch_returnsFalse() {
        val testSingleNode = GlanceMappedNode(
            EmittableText().apply {
                text = "some EXISTING text"
            }
        )

        val result =
            hasText(text = "SOME non-existing TEXT", ignoreCase = true).matches(testSingleNode)

        assertThat(result).isFalse()
    }
}
