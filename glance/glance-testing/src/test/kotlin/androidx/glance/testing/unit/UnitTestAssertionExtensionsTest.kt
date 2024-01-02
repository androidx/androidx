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
    fun assertHasTextEqualTo_matching() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply {
                    text = "test text"
                    modifier = GlanceModifier.semantics { testTag = "test-tag" }
                })
            },
            onNodeMatcher = hasTestTag("test-tag")
        )

        nodeAssertion.assertHasTextEqualTo("test text")
    }

    @Test
    fun assertHasTextEqualTo_ignoreCase_matching() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply {
                    text = "test text"
                    modifier = GlanceModifier.semantics { testTag = "test-tag" }
                })
            },
            onNodeMatcher = hasTestTag("test-tag")
        )

        nodeAssertion.assertHasTextEqualTo(text = "TEST TEXT", ignoreCase = true)
    }

    @Test
    fun assertHasTextEqualTo_notMatching_assertionError() {
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
            nodeAssertion.assertHasTextEqualTo("non-existing text")
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains(
                "Failed to assert condition: " +
                    "(text == 'non-existing text' (ignoreCase: 'false'))"
            )
    }

    @Test
    fun assertHasTextEqualTo_ignoreCaseAndNotMatching_assertionError() {
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
            nodeAssertion.assertHasTextEqualTo("NON-EXISTING TEXT", ignoreCase = true)
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains(
                "Failed to assert condition: " +
                    "(text == 'NON-EXISTING TEXT' (ignoreCase: 'true'))"
            )
    }

    @Test
    fun assertHasText_matching() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply {
                    text = "test text"
                    modifier = GlanceModifier.semantics { testTag = "test-tag" }
                })
            },
            onNodeMatcher = hasTestTag("test-tag")
        )

        nodeAssertion.assertHasText(text = "text")
    }

    @Test
    fun assertHasText_ignoreCase_matching() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply {
                    text = "test text"
                    modifier = GlanceModifier.semantics { testTag = "test-tag" }
                })
            },
            onNodeMatcher = hasTestTag("test-tag")
        )

        nodeAssertion.assertHasText(text = "TEXT", ignoreCase = true)
    }

    @Test
    fun assertHasText_notMatching_assertionError() {
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
            nodeAssertion.assertHasText("non-existing")
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains(
                "Failed to assert condition: " +
                    "(contains text 'non-existing' (ignoreCase: 'false') as substring)"
            )
    }

    @Test
    fun assertHasText_ignoreCaseAndNotMatching_assertionError() {
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
            nodeAssertion.assertHasText(text = "NON-EXISTING", ignoreCase = true)
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains(
                "Failed to assert condition: " +
                    "(contains text 'NON-EXISTING' (ignoreCase: 'true') as substring)"
            )
    }

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
    fun assertHasContentDescriptionEqualTo_matching() {
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

        nodeAssertion.assertHasContentDescriptionEqualTo("test text description")
    }

    @Test
    fun assertHasContentDescriptionEqualTo_ignoreCaseMatching() {
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

        nodeAssertion.assertHasContentDescriptionEqualTo(
            value = "TEST TEXT DESCRIPTION",
            ignoreCase = true
        )
    }

    @Test
    fun assertHasContentDescriptionEqualTo_notMatching_assertionError() {
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
            nodeAssertion.assertHasContentDescriptionEqualTo("text description")
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains(
                "Failed to assert condition: " +
                    "(ContentDescription == 'text description' (ignoreCase: 'false'))"
            )
    }

    @Test
    fun assertHasContentDescriptionEqualTo_ignoreCaseAndNotMatching_assertionError() {
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

        val assertionError = assertThrows(AssertionError::class.java) {
            nodeAssertion.assertHasContentDescriptionEqualTo(
                value = "TEST DESCRIPTION",
                ignoreCase = true
            )
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains(
                "Failed to assert condition: " +
                    "(ContentDescription == 'TEST DESCRIPTION' (ignoreCase: 'true'))"
            )
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

        nodeAssertion.assertHasContentDescription("text")
    }

    @Test
    fun assertHasContentDescription_ignoreCaseMatching() {
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

        nodeAssertion.assertHasContentDescription(
            value = "TEXT",
            ignoreCase = true
        )
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
                    "(ContentDescription contains 'test text description' " +
                    "(ignoreCase: 'false') as substring)"
            )
    }

    @Test
    fun assertHasContentDescription_ignoreCaseAndNotMatching_assertionError() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply {
                    text = "test text"
                    modifier = GlanceModifier.semantics {
                        testTag = "test-tag"
                        contentDescription = "text"
                    }
                })
            },
            onNodeMatcher = hasTestTag("test-tag")
        )

        val assertionError = assertThrows(AssertionError::class.java) {
            nodeAssertion.assertHasContentDescription(
                value = "TEXT DESCRIPTION",
                ignoreCase = true
            )
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains(
                "Failed to assert condition: " +
                    "(ContentDescription contains 'TEXT DESCRIPTION' (ignoreCase: 'true') " +
                    "as substring)"
            )
    }
}
