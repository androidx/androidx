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

package androidx.glance.testing

import androidx.glance.GlanceModifier
import androidx.glance.layout.EmittableColumn
import androidx.glance.layout.EmittableSpacer
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
import androidx.glance.testing.unit.getGlanceNodeAssertionFor
import androidx.glance.testing.unit.hasTestTag
import androidx.glance.testing.unit.hasText
import androidx.glance.text.EmittableText
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

// Uses emittable based GlanceNode, matcher and filters to validate the logic in
// GlanceNodeAssertion.
class GlanceNodeAssertionTest {
    @Test
    fun assertExists_success() {
        // This is the object that in real usage a rule.onNode(matcher) would return.
        val assertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply { text = "some text" })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "another text"
                    modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
                })
            },
            onNodeMatcher = hasTestTag(testTag = "existing-test-tag"),
        )

        assertion.assertExists()
        // no error
    }

    @Test
    fun assertExists_error() {
        val assertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply { text = "some text" })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "another text"
                    modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
                })
            },
            onNodeMatcher = hasTestTag(testTag = "non-existing-test-tag")
        )

        val assertionError = assertThrows(AssertionError::class.java) {
            assertion.assertExists()
        }

        assertThat(assertionError).hasMessageThat().isEqualTo(
            "Failed assertExists" +
                "\nReason: Expected '1' node(s) matching condition: " +
                "TestTag = 'non-existing-test-tag', but found '0'"
        )
    }

    @Test
    fun assertDoesNotExist_success() {
        val assertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply { text = "some text" })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "another text"
                    modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
                })
            },
            onNodeMatcher = hasTestTag(testTag = "non-existing-test-tag")
        )

        assertion.assertDoesNotExist()
        // no error
    }

    @Test
    fun assertDoesNotExist_error() {
        val assertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply { text = "some text" })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "another text"
                    modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
                })
            },
            onNodeMatcher = hasTestTag(testTag = "existing-test-tag")
        )

        val assertionError = assertThrows(AssertionError::class.java) {
            assertion.assertDoesNotExist()
        }

        assertThat(assertionError).hasMessageThat().isEqualTo(
            "Failed assertDoesNotExist" +
                "\nReason: Did not expect any node matching condition: " +
                "TestTag = 'existing-test-tag', but found '1'"
        )
    }

    @Test
    fun assert_withMatcher_success() {
        val assertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply { text = "some text" })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "another text"
                    modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
                })
            },
            onNodeMatcher = hasTestTag(testTag = "existing-test-tag")
        )

        assertion.assert(hasText(text = "another text"))
        // no error
    }

    @Test
    fun chainAssertions() {
        val assertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply { text = "some text" })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "another text"
                    modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
                })
            },
            onNodeMatcher = hasTestTag(testTag = "existing-test-tag")
        )

        assertion
            .assertExists()
            .assert(hasText(text = "another text"))
        // no error
    }

    @Test
    fun chainAssertion_failureInFirst() {
        val assertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply { text = "some text" })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "another text"
                    modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
                })
            },
            onNodeMatcher = hasTestTag(testTag = "existing-test-tag")
        )

        val assertionError = assertThrows(AssertionError::class.java) {
            assertion
                .assertDoesNotExist()
                .assert(hasText(text = "another text"))
        }

        assertThat(assertionError)
            .hasMessageThat()
            .isEqualTo(
                "Failed assertDoesNotExist" +
                    "\nReason: Did not expect any node matching condition: " +
                    "TestTag = 'existing-test-tag', but found '1'"
            )
    }

    @Test
    fun chainAssertion_failureInSecond() {
        val assertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply { text = "some text" })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "another text"
                    modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
                })
            },
            onNodeMatcher = hasTestTag(testTag = "existing-test-tag")
        )

        val assertionError = assertThrows(AssertionError::class.java) {
            assertion
                .assertExists()
                .assert(hasText(text = "non-existing text"))
        }

        assertThat(assertionError)
            .hasMessageThat()
            .startsWith(
                "Failed to assert condition: " +
                    "(contains text 'non-existing text' (ignoreCase: 'false') as substring)" +
                    "\nGlance Node:"
            )
    }
}
