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
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
import androidx.glance.text.EmittableText
import com.google.common.truth.ExpectFailure.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

// Tests convenience assertions and the underlying filters / matchers that are common to surfaces
// and relevant to unit tests
class UnitTestAssertionExtensionsTest {
    @Test
    fun assertHasTestTag_matching() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply {
                    text = "test text"
                    modifier = GlanceModifier.semantics { testTag = "test-tag" }
                })
            },
            onNodeMatcher = hasText("test text")
        )

        nodeAssertion.assertHasTestTag("test-tag")
    }

    @Test
    fun assertHasTestTag_notMatching_assertionError() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply {
                    text = "test text"
                    modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
                })
            },
            onNodeMatcher = hasText("test text")
        )

        val assertionError = assertThrows(AssertionError::class.java) {
            nodeAssertion.assertHasTestTag("non-existing-tag")
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains("Failed to assert condition: (TestTag = 'non-existing-tag')")
    }

    @Test
    fun assertHasContentDescription_matching() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply {
                    text = "test text"
                    modifier = GlanceModifier.semantics {
                        testTag = "test-tag"
                        contentDescription = "test text description"
                    }
                })
            },
            onNodeMatcher = hasTestTag("test-tag")
        )

        nodeAssertion.assertHasContentDescription("test text description")
    }

    @Test
    fun assertHasContentDescription_notMatching_assertionError() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply {
                    text = "test text"
                    modifier = GlanceModifier.semantics { testTag = "test-tag" }
                })
            },
            onNodeMatcher = hasTestTag("test-tag")
        )

        val assertionError = assertThrows(AssertionError::class.java) {
            nodeAssertion.assertHasContentDescription("test text description")
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains(
                "Failed to assert condition: " +
                    "(ContentDescription = 'test text description' (ignoreCase: 'false'))"
            )
    }
}
