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
import androidx.glance.action.ActionModifier
import androidx.glance.action.LambdaAction
import androidx.glance.layout.EmittableColumn
import androidx.glance.layout.EmittableSpacer
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
import androidx.glance.testing.unit.assertHasText
import androidx.glance.testing.unit.getGlanceNodeAssertionCollectionFor
import androidx.glance.testing.unit.getGlanceNodeAssertionFor
import androidx.glance.testing.unit.hasClickAction
import androidx.glance.testing.unit.hasTestTag
import androidx.glance.testing.unit.hasText
import androidx.glance.testing.unit.hasTextEqualTo
import androidx.glance.text.EmittableText
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class GlanceNodeAssertionCollectionTest {
    @Test
    fun assertAll_noNodesToAssertOn() {
        // This is the object that in real usage a onAllNodes(matcher) would return.
        val assertion = getGlanceNodeAssertionCollectionFor(
            onAllNodesMatcher = hasClickAction(),
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply { text = "some text" })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply { text = "another text" })
            }
        )

        assertion.assertAll(hasText("substring"))
        // no error even if no nodes with click action were available to perform assertAll; on the
        // other hand calling assertCountEquals(x) before assertAll would have thrown an error
    }

    @Test
    fun assertAll_allMatchingNodes() {
        val assertion = getGlanceNodeAssertionCollectionFor(
            onAllNodesMatcher = hasClickAction(),
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply {
                    text = "some substring text"
                    modifier = ActionModifier(LambdaAction("1") {})
                })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "another substring text"
                    modifier = ActionModifier(LambdaAction("2") {})
                })
            }
        )

        assertion.assertAll(hasText("substring"))
        // no error
    }

    @Test
    fun assertAll_noneMatch_throwsError() {
        val assertion = getGlanceNodeAssertionCollectionFor(
            onAllNodesMatcher = hasClickAction(),
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply {
                    text = "some text"
                    modifier = ActionModifier(LambdaAction("1") {})
                })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "another text"
                    modifier = ActionModifier(LambdaAction("2") {})
                })
            }
        )

        val assertionError = assertThrows(AssertionError::class.java) {
            assertion.assertAll(hasText("substring"))
        }

        assertThat(assertionError).hasMessageThat().contains(
            "Failed to assertAll(contains text 'substring' (ignoreCase: 'false') as substring)" +
                "\nFound 2 node(s) that don't match."
        )
    }

    @Test
    fun assertAll_someNotMatch_throwsError() {
        val assertion = getGlanceNodeAssertionCollectionFor(
            onAllNodesMatcher = hasClickAction(),
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply {
                    text = "some substring text"
                    modifier = ActionModifier(LambdaAction("1") {})
                })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "another text"
                    modifier = ActionModifier(LambdaAction("2") {})
                })
            }
        )

        val assertionError = assertThrows(AssertionError::class.java) {
            assertion.assertAll(hasText("substring"))
        }

        assertThat(assertionError).hasMessageThat().contains(
            "Failed to assertAll(contains text 'substring' (ignoreCase: 'false') as substring)" +
                "\nFound 1 node(s) that don't match."
        )
    }

    @Test
    fun assertAny_noNodesToAssertOn_assertionError() {
        val assertion = getGlanceNodeAssertionCollectionFor(
            onAllNodesMatcher = hasClickAction(),
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply { text = "some text" })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply { text = "another text" })
            }
        )

        val assertionError = assertThrows(AssertionError::class.java) {
            assertion.assertAny(hasText("substring"))
        }

        assertThat(assertionError).hasMessageThat().contains(
            "Failed to assertAny(contains text 'substring' (ignoreCase: 'false') as substring)" +
                "\nReason: Expected to receive at least 1 node " +
                "but 0 nodes were found for condition: (has click action)"
        )
    }

    @Test
    fun assertAny_noneMatch_assertionError() {
        val assertion = getGlanceNodeAssertionCollectionFor(
            onAllNodesMatcher = hasClickAction(),
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply {
                    text = "some text"
                    modifier = ActionModifier(LambdaAction("1") {})
                })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "another text"
                    modifier = ActionModifier(LambdaAction("2") {})
                })
            }
        )

        val assertionError = assertThrows(AssertionError::class.java) {
            assertion.assertAny(hasText("expected-substring"))
        }

        assertThat(assertionError).hasMessageThat().contains(
            "Failed to assertAny(contains text 'expected-substring' " +
                "(ignoreCase: 'false') as substring)" +
                "\nFound 2 node(s) that don't match."
        )
    }

    @Test
    fun assertAny_oneMatch() {
        val assertion = getGlanceNodeAssertionCollectionFor(
            onAllNodesMatcher = hasClickAction(),
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply {
                    text = "some text"
                    modifier = ActionModifier(LambdaAction("1") {})
                })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "another substring text"
                    modifier = ActionModifier(LambdaAction("2") {})
                })
            }
        )

        assertion.assertAny(hasText("substring"))
    }

    @Test
    fun assertAny_multipleMatch() {
        val assertion = getGlanceNodeAssertionCollectionFor(
            onAllNodesMatcher = hasClickAction(),
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply {
                    text = "some text"
                    modifier = ActionModifier(LambdaAction("1") {})
                })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "another substring text"
                    modifier = ActionModifier(LambdaAction("2") {})
                })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "yet another substring text"
                    modifier = ActionModifier(LambdaAction("3") {})
                })
            }
        )

        assertion.assertAny(hasText("substring"))
    }

    @Test
    fun assertAllAfterFilter_matchingNodes() {
        val assertion = getGlanceNodeAssertionCollectionFor(
            onAllNodesMatcher = hasClickAction(),
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply {
                    text = "some substring text"
                    modifier = ActionModifier(LambdaAction("1") {})
                })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "another text"
                    modifier = ActionModifier(LambdaAction("2") {})
                })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "yet another substring text"
                    modifier = ActionModifier(LambdaAction("3") {})
                })
            }
        )

        assertion
            .filter(hasText("substring"))
            .assertAll(hasText("text"))
    }

    @Test
    fun assertAllAfterFilter_noFilteredNodes_noError() {
        val assertion = getGlanceNodeAssertionCollectionFor(
            onAllNodesMatcher = hasClickAction(),
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply {
                    text = "some substring text"
                    modifier = ActionModifier(LambdaAction("1") {})
                })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "another text"
                    modifier = ActionModifier(LambdaAction("2") {})
                })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "yet another substring text"
                    modifier = ActionModifier(LambdaAction("3") {})
                })
            }
        )

        assertion
            .filter(hasText("word"))
            .assertAll(hasText("text"))
    }

    @Test
    fun assertAnyAfterFilter_matchingNodes() {
        val assertion = getGlanceNodeAssertionCollectionFor(
            onAllNodesMatcher = hasClickAction(),
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply {
                    text = "some substring"
                    modifier = ActionModifier(LambdaAction("1") {})
                })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "another text"
                    modifier = ActionModifier(LambdaAction("2") {})
                })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "yet another substring text"
                    modifier = ActionModifier(LambdaAction("3") {})
                })
            }
        )

        assertion
            .filter(hasText("substring"))
            .assertAny(hasText("text"))
    }

    @Test
    fun assertAnyAfterFilter_noFilteredNodes_assertionError() {
        val assertion = getGlanceNodeAssertionCollectionFor(
            onAllNodesMatcher = hasClickAction(),
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply {
                    text = "some substring text"
                    modifier = ActionModifier(LambdaAction("1") {})
                })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "another text"
                    modifier = ActionModifier(LambdaAction("2") {})
                })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "yet another substring text"
                    modifier = ActionModifier(LambdaAction("3") {})
                })
            }
        )

        val assertionError = assertThrows(AssertionError::class.java) {
            assertion
                .filter(hasText("word"))
                .assertAny(hasText("text"))
        }

        assertThat(assertionError).hasMessageThat().contains(
            "Failed to assertAny(contains text 'text' (ignoreCase: 'false') as substring)" +
                "\nReason: Expected to receive at least 1 node but 0 nodes were found for " +
                "condition: " +
                "((has click action).filter(contains text 'word' (ignoreCase: 'false') " +
                "as substring))"
        )
    }

    @Test
    fun assertCountEqualsAfterFilter_matchingNodes() {
        val assertion = getGlanceNodeAssertionCollectionFor(
            onAllNodesMatcher = hasClickAction(),
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply {
                    text = "some substring text"
                    modifier = ActionModifier(LambdaAction("1") {})
                })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "another text"
                    modifier = ActionModifier(LambdaAction("2") {})
                })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "yet another substring text"
                    modifier = ActionModifier(LambdaAction("3") {})
                })
            }
        )

        assertion
            .filter(hasText("text"))
            .assertCountEquals(3)
    }

    @Test
    fun assertCountEqualsAfterFilter_noFilteredNodes_assertionError() {
        val assertion = getGlanceNodeAssertionCollectionFor(
            onAllNodesMatcher = hasClickAction(),
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply {
                    text = "some substring text"
                    modifier = ActionModifier(LambdaAction("1") {})
                })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "another text"
                    modifier = ActionModifier(LambdaAction("2") {})
                })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "yet another substring text"
                    modifier = ActionModifier(LambdaAction("3") {})
                })
            }
        )

        val assertionError = assertThrows(AssertionError::class.java) {
            assertion
                .filter(hasText("word"))
                .assertCountEquals(1)
        }

        assertThat(assertionError).hasMessageThat().contains(
            "Failed to assert count of nodes" +
                "\nReason: Expected '1' node(s) matching condition: " +
                "(has click action).filter(contains text 'word' " +
                "(ignoreCase: 'false') as substring), " +
                "but found '0'"
        )
    }

    @Test
    fun multipleFilters() {
        val assertion = getGlanceNodeAssertionCollectionFor(
            onAllNodesMatcher = hasClickAction(),
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply {
                    text = "some text"
                    modifier = ActionModifier(LambdaAction("1") {})
                })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "another substring"
                    modifier = ActionModifier(LambdaAction("2") {})
                })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "yet another substring text"
                    modifier = ActionModifier(LambdaAction("3") {})
                })
            }
        )

        assertion
            .filter(hasText("text"))
            .assertCountEquals(2)
            .filter(hasText("substring"))
            .assertCountEquals(1)
    }

    @Test
    fun getIndexOnFilter() {
        val assertion = getGlanceNodeAssertionCollectionFor(
            onAllNodesMatcher = hasClickAction(),
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply {
                    text = "some text"
                    modifier = ActionModifier(LambdaAction("1") {})
                })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "another substring"
                    modifier = ActionModifier(LambdaAction("2") {})
                })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "yet another substring text"
                    modifier = ActionModifier(LambdaAction("3") {})
                })
            }
        )

        assertion
            .filter(hasText("text"))
            .assertCountEquals(2)
            .get(index = 1)
            .assert(hasTextEqualTo("yet another substring text"))
    }

    @Test
    fun collectionGetIndex_notEnoughNodes_assertionError() {
        val assertion = getGlanceNodeAssertionCollectionFor(
            onAllNodesMatcher = hasClickAction(),
            emittable = EmittableColumn().apply {
                children.add(EmittableText().apply {
                    text = "some text"
                    modifier = ActionModifier(LambdaAction("1") {})
                })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "another substring"
                    modifier = ActionModifier(LambdaAction("2") {})
                })
                children.add(EmittableSpacer())
                children.add(EmittableText().apply {
                    text = "yet another substring text"
                    modifier = ActionModifier(LambdaAction("3") {})
                })
            }
        )

        val assertionError = assertThrows(AssertionError::class.java) {
            assertion
                .filter(hasText("text"))
                .assertCountEquals(2)
                .get(index = 3) // index out of bounds.
                .assert(hasTextEqualTo("yet another substring text"))
        }

        assertThat(assertionError).hasMessageThat().contains(
            "Failed to assert condition: " +
                "(text == 'yet another substring text' (ignoreCase: 'false'))" +
                "\nReason: Not enough node(s) matching condition: " +
                "((has click action).filter(contains text 'text' " +
                "(ignoreCase: 'false') as substring)) " +
                "to get node at index '4'. Found '2' matching node(s)"
        )
    }

    @Test
    fun assertOnChildren_multipleChildren() {
        val assertion = getGlanceNodeAssertionFor(
            onNodeMatcher = hasTestTag("test-list"),
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "test-list" }
                children.add(EmittableText().apply {
                    text = "some text"
                    modifier = ActionModifier(LambdaAction("1") {})
                })
                children.add(EmittableText().apply {
                    text = "another substring"
                    modifier = ActionModifier(LambdaAction("2") {})
                })
            }
        )

        assertion
            .onChildren()
            .assertCountEquals(2)
            .assertAll(hasClickAction())
    }

    @Test
    fun assertOnChildren_noChildren() {
        val assertion = getGlanceNodeAssertionFor(
            onNodeMatcher = hasTestTag("test-list"),
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "test-list" }
            }
        )

        assertion.onChildren().assertCountEquals(0)
    }

    @Test
    fun assertAnyOnChildren_noChildren_assertionError() {
        val assertion = getGlanceNodeAssertionFor(
            onNodeMatcher = hasTestTag("test-list"),
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "test-list" }
            }
        )

        val assertionError = assertThrows(AssertionError::class.java) {
            assertion.onChildren().assertCountEquals(1)
        }

        assertThat(assertionError).hasMessageThat().contains(
            "Failed to assert count of nodes" +
                "\nReason: Expected '1' node(s) matching condition: " +
                "(TestTag = 'test-list').children(), but found '0'"
        )
    }

    @Test
    fun filterOnChildren() {
        val assertion = getGlanceNodeAssertionFor(
            onNodeMatcher = hasTestTag("test-list"),
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "test-list" }
                children.add(EmittableText().apply {
                    text = "some text"
                    modifier = ActionModifier(LambdaAction("1") {})
                })
                children.add(EmittableText().apply {
                    text = "another substring"
                    modifier = ActionModifier(LambdaAction("2") {})
                })
            }
        )

        assertion
            .onChildren()
            .filter(hasText("substring"))
            .assertCountEquals(1)
    }

    @Test
    fun getIndexOnChildren() {
        val assertion = getGlanceNodeAssertionFor(
            onNodeMatcher = hasTestTag("test-list"),
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "test-list" }
                children.add(EmittableText().apply { text = "text-1" })
                children.add(EmittableText().apply { text = "text-2" })
            }
        )

        assertion
            .onChildren()
            .get(index = 0)
            .assertHasText("text-1")
    }
}
