/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.semantics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.collections.listOf
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class MergedSemanticsConfigurationTest {

    @get:Rule val rule = createComposeRule()

    lateinit var semanticsOwner: SemanticsOwner

    @Test
    fun singleModifier() {
        // Arrange.
        rule.setTestContent {
            Box(
                Modifier.semantics {
                    testTag = "box"
                    text = AnnotatedString("1")
                }
            )
        }

        // Act.
        val id = rule.onNodeWithTag("box").semanticsId()
        val (unmerged, merged) =
            with(checkNotNull(semanticsOwner[id])) {
                Pair(semanticsConfiguration, mergedSemanticsConfiguration())
            }

        // Assert.
        assertThat(unmerged?.getOrNull(SemanticsProperties.Text))
            .isEqualTo(listOf(AnnotatedString("1")))
        assertThat(merged).isEqualTo(unmerged)
    }

    @Test
    fun multipleModifierOnSameNode() {
        // Arrange.
        rule.setTestContent {
            Box(
                Modifier.semantics {
                        testTag = "box"
                        text = AnnotatedString("1")
                    }
                    .semantics { text = AnnotatedString("2") }
            )
        }

        // Act.
        val id = rule.onNodeWithTag("box").semanticsId()
        val (unmerged, merged) =
            with(checkNotNull(semanticsOwner[id])) {
                Pair(semanticsConfiguration, mergedSemanticsConfiguration())
            }

        // Assert.
        assertThat(unmerged?.getOrNull(SemanticsProperties.Text))
            .isEqualTo(listOf(AnnotatedString("1")))
        assertThat(merged).isEqualTo(unmerged)
    }

    @Test
    fun multipleModifierOnSameNode_mergingDescendants() {
        // Arrange.
        rule.setTestContent {
            Box(
                Modifier.semantics(mergeDescendants = true) {
                        testTag = "box"
                        text = AnnotatedString("1")
                    }
                    .semantics { text = AnnotatedString("2") }
            )
        }

        // Act.
        val id = rule.onNodeWithTag("box").semanticsId()
        val (unmerged, merged) =
            with(checkNotNull(semanticsOwner[id])) {
                Pair(semanticsConfiguration, mergedSemanticsConfiguration())
            }

        // Assert.
        assertThat(unmerged?.getOrNull(SemanticsProperties.Text))
            .isEqualTo(listOf(AnnotatedString("1")))
        assertThat(merged).isEqualTo(unmerged)
    }

    @Test
    fun multipleLayoutNodes_default() {
        // Arrange.
        rule.setTestContent {
            Box(
                Modifier.semantics {
                    testTag = "box"
                    text = AnnotatedString("1")
                }
            ) {
                Box(Modifier.semantics { text = AnnotatedString("2") })
            }
        }

        // Act.
        val id = rule.onNodeWithTag("box").semanticsId()
        val (unmerged, merged) =
            with(checkNotNull(semanticsOwner[id])) {
                Pair(semanticsConfiguration, mergedSemanticsConfiguration())
            }

        // Assert.
        assertThat(unmerged?.getOrNull(SemanticsProperties.Text))
            .isEqualTo(listOf(AnnotatedString("1")))
        assertThat(merged).isEqualTo(unmerged)
    }

    @Test
    fun multipleLayoutNodes_isMergingDescendants_oneChild() {
        // Arrange.
        rule.setTestContent {
            Box(
                Modifier.semantics(mergeDescendants = true) {
                    testTag = "box"
                    text = AnnotatedString("1")
                }
            ) {
                Box(Modifier.semantics { text = AnnotatedString("2") })
            }
        }

        // Act.
        val id = rule.onNodeWithTag("box").semanticsId()
        val (unmerged, merged) =
            with(checkNotNull(semanticsOwner[id])) {
                Pair(semanticsConfiguration, mergedSemanticsConfiguration())
            }

        // Assert.
        assertThat(unmerged?.getOrNull(SemanticsProperties.Text))
            .isEqualTo(listOf(AnnotatedString("1")))
        assertThat(merged?.getOrNull(SemanticsProperties.Text))
            .isEqualTo(listOf(AnnotatedString("1"), AnnotatedString("2")))
    }

    @Test
    fun multipleLayoutNodes_isMergingDescendants_twoChildren() {
        // Arrange.
        rule.setTestContent {
            Row(
                Modifier.semantics(mergeDescendants = true) {
                    testTag = "row"
                    text = AnnotatedString("1")
                }
            ) {
                Box(Modifier.semantics { text = AnnotatedString("2.1") })
                Box(Modifier.semantics { text = AnnotatedString("2.2") })
            }
        }

        // Act.
        val id = rule.onNodeWithTag("row").semanticsId()
        val (unmerged, merged) =
            with(checkNotNull(semanticsOwner[id])) {
                Pair(semanticsConfiguration, mergedSemanticsConfiguration())
            }

        // Assert.
        assertThat(unmerged?.getOrNull(SemanticsProperties.Text))
            .isEqualTo(listOf(AnnotatedString("1")))
        assertThat(merged?.getOrNull(SemanticsProperties.Text))
            .isEqualTo(listOf(AnnotatedString("1"), AnnotatedString("2.2"), AnnotatedString("2.1")))
    }

    @Test
    fun multipleLayoutNodes_isMergingDescendants_clearAndSetSemantics_twoChildren() {
        // Arrange.
        rule.setTestContent {
            Row(
                Modifier.semantics(mergeDescendants = true) {
                        testTag = "row"
                        text = AnnotatedString("1")
                    }
                    .clearAndSetSemantics {}
            ) {
                Box(Modifier.semantics { text = AnnotatedString("2.1") })
                Box(Modifier.semantics { text = AnnotatedString("2.2") })
            }
        }

        // Act.
        val id = rule.onNodeWithTag("row").semanticsId()
        val (unmerged, merged) =
            with(checkNotNull(semanticsOwner[id])) {
                Pair(semanticsConfiguration, mergedSemanticsConfiguration())
            }

        // Assert.
        assertThat(unmerged?.getOrNull(SemanticsProperties.Text))
            .isEqualTo(listOf(AnnotatedString("1")))
        assertThat(merged?.getOrNull(SemanticsProperties.Text))
            .isEqualTo(listOf(AnnotatedString("1")))
    }

    @Test
    fun multipleLayoutNodes_isClearingSemantics_twoChildren() {
        // Arrange.
        rule.setTestContent {
            Row(
                Modifier.clearAndSetSemantics {
                    testTag = "row"
                    text = AnnotatedString("1")
                }
            ) {
                Box(Modifier.semantics { text = AnnotatedString("2.1") })
                Box(Modifier.semantics { text = AnnotatedString("2.2") })
            }
        }

        // Act.
        val id = rule.onNodeWithTag("row").semanticsId()
        val (unmerged, merged) =
            with(checkNotNull(semanticsOwner[id])) {
                Pair(semanticsConfiguration, mergedSemanticsConfiguration())
            }

        // Assert.
        assertThat(unmerged?.getOrNull(SemanticsProperties.Text))
            .isEqualTo(listOf(AnnotatedString("1")))
        assertThat(merged?.getOrNull(SemanticsProperties.Text))
            .isEqualTo(listOf(AnnotatedString("1")))
    }

    @Test
    fun deepHierarchy() {
        // Arrange.
        rule.setTestContent {
            Row(
                Modifier.semantics(mergeDescendants = true) {
                    testTag = "row"
                    text = AnnotatedString("1")
                }
            ) {
                Box(Modifier.semantics { text = AnnotatedString("2.1") })
                Column(Modifier.semantics { text = AnnotatedString("2.2") }) {
                    Box(Modifier.semantics { text = AnnotatedString("3.1") })
                    Box(Modifier.semantics { text = AnnotatedString("3.2") })
                }
                Box(Modifier.semantics { text = AnnotatedString("2.3") })
            }
        }

        // Act.
        val id = rule.onNodeWithTag("row").semanticsId()
        val (unmerged, merged) =
            with(checkNotNull(semanticsOwner[id])) {
                Pair(semanticsConfiguration, mergedSemanticsConfiguration())
            }

        // Assert.
        assertThat(unmerged?.getOrNull(SemanticsProperties.Text))
            .isEqualTo(listOf(AnnotatedString("1")))
        assertThat(merged?.getOrNull(SemanticsProperties.Text))
            .isEqualTo(
                listOf(
                    AnnotatedString("1"),
                    AnnotatedString("2.3"),
                    AnnotatedString("2.2"),
                    AnnotatedString("3.2"),
                    AnnotatedString("3.1"),
                    AnnotatedString("2.1"),
                )
            )
    }

    @Test
    fun doesNotMergeMergedItems() {
        // Arrange.
        rule.setTestContent {
            Row(
                Modifier.semantics(mergeDescendants = true) {
                    testTag = "row"
                    text = AnnotatedString("1")
                }
            ) {
                Box(Modifier.semantics { text = AnnotatedString("2.1") })
                Column(
                    Modifier.semantics(mergeDescendants = true) { text = AnnotatedString("2.2") }
                ) {
                    Box(Modifier.semantics { text = AnnotatedString("3.1") })
                    Box(Modifier.semantics { text = AnnotatedString("3.2") })
                }
                Box(Modifier.semantics { text = AnnotatedString("2.3") })
            }
        }

        // Act.
        val id = rule.onNodeWithTag("row").semanticsId()
        val (unmerged, merged) =
            with(checkNotNull(semanticsOwner[id])) {
                Pair(semanticsConfiguration, mergedSemanticsConfiguration())
            }

        // Assert.
        assertThat(unmerged?.getOrNull(SemanticsProperties.Text))
            .isEqualTo(listOf(AnnotatedString("1")))
        assertThat(merged?.getOrNull(SemanticsProperties.Text))
            .isEqualTo(listOf(AnnotatedString("1"), AnnotatedString("2.3"), AnnotatedString("2.1")))
    }

    @Test
    fun doesNotIncludeChildrenThatAreCleared() {
        // Arrange.
        rule.setTestContent {
            Row(
                Modifier.semantics(mergeDescendants = true) {
                    testTag = "row"
                    text = AnnotatedString("1")
                }
            ) {
                Box(Modifier.semantics { text = AnnotatedString("2.1") })
                Column(Modifier.clearAndSetSemantics { text = AnnotatedString("2.2") }) {
                    Box(Modifier.semantics { text = AnnotatedString("3.1") })
                    Box(Modifier.semantics { text = AnnotatedString("3.2") })
                }
                Box(Modifier.semantics { text = AnnotatedString("2.3") })
            }
        }

        // Act.
        val id = rule.onNodeWithTag("row").semanticsId()
        val (unmerged, merged) =
            with(checkNotNull(semanticsOwner[id])) {
                Pair(semanticsConfiguration, mergedSemanticsConfiguration())
            }

        // Assert.
        assertThat(unmerged?.getOrNull(SemanticsProperties.Text))
            .isEqualTo(listOf(AnnotatedString("1")))
        assertThat(merged?.getOrNull(SemanticsProperties.Text))
            .isEqualTo(
                listOf(
                    AnnotatedString("1"),
                    AnnotatedString("2.3"),
                    AnnotatedString("2.2"),
                    AnnotatedString("2.1"),
                )
            )
    }

    private fun ComposeContentTestRule.setTestContent(composable: @Composable () -> Unit) {
        setContent {
            semanticsOwner = (LocalView.current as RootForTest).semanticsOwner
            composable()
        }
    }
}
