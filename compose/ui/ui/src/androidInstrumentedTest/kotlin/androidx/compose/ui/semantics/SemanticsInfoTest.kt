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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties.TestTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Correspondence
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SemanticsInfoTest {

    @get:Rule val rule = createComposeRule()

    lateinit var semanticsOwner: SemanticsOwner

    @Test
    fun contentWithNoSemantics() {
        // Arrange.
        rule.setTestContent { Box {} }
        rule.waitForIdle()

        // Act.
        val rootSemantics = semanticsOwner.rootInfo

        // Assert.
        assertThat(rootSemantics).isNotNull()
        assertThat(rootSemantics.parentInfo).isNull()
        assertThat(rootSemantics.childrenInfo.map { it.semanticsConfiguration })
            .comparingElementsUsing(SemanticsConfigurationComparator)
            .containsExactly(null)

        // Assert extension Functions.
        assertThat(rootSemantics.nearestParentThatHasSemantics()).isNull()
        assertThat(rootSemantics.findMergingSemanticsParent()).isNull()
        assertThat(rootSemantics.boundsInParent).isEqualTo(Rect.Zero)
    }

    @Test
    fun singleSemanticsModifier() {
        // Arrange.
        rule.setTestContent { Box(Modifier.semantics { this.testTag = "testTag" }) }
        rule.waitForIdle()

        // Act.
        val rootSemantics = semanticsOwner.rootInfo
        val semantics = rule.getSemanticsInfoForTag("testTag")!!

        // Assert.
        assertThat(rootSemantics.parentInfo).isNull()
        assertThat(rootSemantics.childrenInfo).containsExactly(semantics)

        assertThat(semantics.parentInfo).isEqualTo(rootSemantics)
        assertThat(semantics.childrenInfo).isEmpty()

        // Assert extension Functions.
        assertThat(rootSemantics.nearestParentThatHasSemantics()).isNull()
        assertThat(rootSemantics.findMergingSemanticsParent()).isNull()
        assertThat(rootSemantics.childrenInfo.map { it.semanticsConfiguration })
            .comparingElementsUsing(SemanticsConfigurationComparator)
            .containsExactly(SemanticsConfiguration().apply { testTag = "testTag" })

        assertThat(semantics.nearestParentThatHasSemantics()).isEqualTo(rootSemantics)
        assertThat(semantics.findMergingSemanticsParent()).isNull()
        assertThat(semantics.childrenInfo).isEmpty()
    }

    @Test
    fun twoSemanticsModifiers() {
        // Arrange.
        rule.setTestContent {
            Box(Modifier.semantics { this.testTag = "item1" })
            Box(Modifier.semantics { this.testTag = "item2" })
        }
        rule.waitForIdle()

        // Act.
        val rootSemantics: SemanticsInfo = semanticsOwner.rootInfo
        val semantics1 = rule.getSemanticsInfoForTag("item1")
        val semantics2 = rule.getSemanticsInfoForTag("item2")

        // Assert.
        assertThat(rootSemantics.parentInfo).isNull()
        assertThat(rootSemantics.childrenInfo.map { it.semanticsConfiguration }.toList())
            .comparingElementsUsing(SemanticsConfigurationComparator)
            .containsExactly(
                SemanticsConfiguration().apply { testTag = "item1" },
                SemanticsConfiguration().apply { testTag = "item2" },
            )
            .inOrder()

        assertThat(rootSemantics.childrenInfo.map { it.semanticsConfiguration })
            .comparingElementsUsing(SemanticsConfigurationComparator)
            .containsExactly(
                SemanticsConfiguration().apply { testTag = "item1" },
                SemanticsConfiguration().apply { testTag = "item2" },
            )
            .inOrder()

        checkNotNull(semantics1)
        assertThat(semantics1.parentInfo).isEqualTo(rootSemantics)
        assertThat(semantics1.childrenInfo).isEmpty()

        checkNotNull(semantics2)
        assertThat(semantics2.parentInfo).isEqualTo(rootSemantics)
        assertThat(semantics2.childrenInfo).isEmpty()

        // Assert extension Functions.
        assertThat(rootSemantics.nearestParentThatHasSemantics()).isNull()
        assertThat(rootSemantics.findMergingSemanticsParent()).isNull()
        assertThat(rootSemantics.childrenInfo.map { it.semanticsConfiguration })
            .comparingElementsUsing(SemanticsConfigurationComparator)
            .containsExactly(
                SemanticsConfiguration().apply { testTag = "item1" },
                SemanticsConfiguration().apply { testTag = "item2" },
            )
            .inOrder()

        assertThat(semantics1.nearestParentThatHasSemantics()).isEqualTo(rootSemantics)
        assertThat(semantics1.findMergingSemanticsParent()).isNull()
        assertThat(semantics1.childrenInfo).isEmpty()

        assertThat(semantics2.nearestParentThatHasSemantics()).isEqualTo(rootSemantics)
        assertThat(semantics2.findMergingSemanticsParent()).isNull()
        assertThat(semantics2.childrenInfo).isEmpty()
    }

    // TODO(ralu): Split this into multiple tests.
    @Test
    fun nodeDeepInHierarchy() {
        // Arrange.
        rule.setTestContent {
            Column(Modifier.semantics(mergeDescendants = true) { testTag = "outerColumn" }) {
                Row(Modifier.semantics { testTag = "outerRow" }) {
                    Column(Modifier.semantics(mergeDescendants = true) { testTag = "column" }) {
                        Row(Modifier.semantics { testTag = "row" }) {
                            Column {
                                Box(Modifier.semantics { testTag = "box" })
                                Row(
                                    Modifier.semantics {}
                                        .semantics { testTag = "testTarget" }
                                        .semantics { testTag = "extra modifier2" }
                                ) {
                                    Box { Box(Modifier.semantics { testTag = "child1" }) }
                                    Box(Modifier.semantics { testTag = "child2" }) {
                                        Box(Modifier.semantics { testTag = "grandChild" })
                                    }
                                    Box {}
                                    Row {
                                        Box {}
                                        Box {}
                                    }
                                    Box { Box(Modifier.semantics { testTag = "child3" }) }
                                }
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        val row = rule.getSemanticsInfoForTag(tag = "row", useUnmergedTree = true)
        val column = rule.getSemanticsInfoForTag("column")

        // Act.
        val testTarget = rule.getSemanticsInfoForTag(tag = "testTarget", useUnmergedTree = true)

        // Assert.
        checkNotNull(testTarget)
        assertThat(testTarget.parentInfo).isNotEqualTo(row)
        assertThat(testTarget.nearestParentThatHasSemantics()).isEqualTo(row)
        assertThat(testTarget.findMergingSemanticsParent()).isEqualTo(column)
        assertThat(testTarget.childrenInfo.map { it.semanticsConfiguration })
            .comparingElementsUsing(SemanticsConfigurationComparator)
            .containsExactly(
                null,
                SemanticsConfiguration().apply { testTag = "child2" },
                null,
                null,
                null,
            )
            .inOrder()
        assertThat(testTarget.semanticsConfiguration?.getOrNull(TestTag)).isEqualTo("testTarget")
    }

    @Test
    fun readingSemanticsConfigurationOfDeactivatedNode() {
        // Arrange.
        lateinit var lazyListState: LazyListState
        lateinit var rootForTest: RootForTest
        rule.setContent {
            rootForTest = LocalView.current as RootForTest
            lazyListState = rememberLazyListState()
            LazyRow(state = lazyListState, modifier = Modifier.size(10.dp)) {
                items(2) { index -> Box(Modifier.size(10.dp).testTag("$index")) }
            }
        }
        val semanticsId = rule.onNodeWithTag("0").semanticsId()
        val semanticsInfo = checkNotNull(rootForTest.semanticsOwner[semanticsId])

        // Act.
        rule.runOnIdle { lazyListState.requestScrollToItem(1) }
        val semanticsConfiguration = rule.runOnIdle { semanticsInfo.semanticsConfiguration }

        // Assert.
        rule.runOnIdle {
            assertThat(semanticsInfo.isDeactivated).isTrue()
            assertThat(semanticsConfiguration).isNull()
        }
    }

    @Test
    fun transparent() {
        // Arrange.
        rule.setTestContent { Box(Modifier.alpha(0.0f)) { Box(Modifier.testTag("item")) } }
        rule.waitForIdle()

        // Act.
        val semantics = rule.getSemanticsInfoForTag("item")

        // Assert.
        assertThat(semantics?.isTransparent()).isTrue()
    }

    @Test
    fun semiTransparent() {
        // Arrange.
        rule.setTestContent { Box(Modifier.alpha(0.5f)) { Box(Modifier.testTag("item")) } }
        rule.waitForIdle()

        // Act.
        val semantics = rule.getSemanticsInfoForTag("item")

        // Assert.
        assertThat(semantics?.isTransparent()).isFalse()
    }

    @Test
    fun nonTransparent() {
        // Arrange.
        rule.setTestContent { Box(Modifier.alpha(1.0f)) { Box(Modifier.testTag("item")) } }
        rule.waitForIdle()

        // Act.
        val semantics = rule.getSemanticsInfoForTag("item")

        // Assert.
        assertThat(semantics?.isTransparent()).isFalse()
    }

    @Test
    fun transparencyOfStackedItems() {
        // Arrange.
        rule.setTestContent {
            Box(Modifier.alpha(1.0f)) { Box(Modifier.testTag("item1")) }
            Box(Modifier.alpha(1.0f)) { Box(Modifier.testTag("item2")) }
            Box(Modifier.alpha(0.0f)) { Box(Modifier.testTag("item3")) }
        }
        rule.waitForIdle()

        // Act.
        val semantics1 = rule.getSemanticsInfoForTag("item1")
        val semantics2 = rule.getSemanticsInfoForTag("item2")
        val semantics3 = rule.getSemanticsInfoForTag("item3")

        // Assert.
        assertThat(semantics1?.isTransparent()).isFalse()
        assertThat(semantics2?.isTransparent()).isFalse()
        assertThat(semantics3?.isTransparent()).isTrue()
    }

    private fun ComposeContentTestRule.setTestContent(composable: @Composable () -> Unit) {
        setContent {
            semanticsOwner = (LocalView.current as RootForTest).semanticsOwner
            composable()
        }
    }

    private fun ComposeContentTestRule.getSemanticsInfoForTag(
        tag: String,
        useUnmergedTree: Boolean = true,
    ): SemanticsInfo? {
        return semanticsOwner[onNodeWithTag(tag, useUnmergedTree).semanticsId()]
    }

    companion object {
        private val SemanticsConfigurationComparator =
            Correspondence.from<SemanticsConfiguration, SemanticsConfiguration>(
                { actual, expected ->
                    (actual == null && expected == null) ||
                        (actual != null &&
                            expected != null &&
                            actual.getOrNull(TestTag) == expected.getOrNull(TestTag))
                },
                "has same test tag as ",
            )
    }
}
