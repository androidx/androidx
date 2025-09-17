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

package androidx.compose.ui.semantics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.Stable
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentDataType
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.autofill.FillableData
import androidx.compose.ui.autofill.createFrom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertValueEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.zIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.max
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SemanticsTests {
    private val TestTag = "semantics-test-tag"

    @get:Rule val rule = createComposeRule()

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun unchangedSemanticsDoesNotCauseRelayout() {
        val layoutCounter = Counter(0)
        val recomposeForcer = mutableStateOf(0)
        rule.setContent {
            recomposeForcer.value
            CountingLayout(Modifier.semantics { contentDescription = "label" }, layoutCounter)
        }

        rule.runOnIdle { assertEquals(1, layoutCounter.count) }

        rule.runOnIdle { recomposeForcer.value++ }

        rule.runOnIdle { assertEquals(1, layoutCounter.count) }
    }

    @Test
    fun paneTitleProperty_unmergedConfig() {
        val paneTitleString = "test PaneTitle string"

        rule.setContent {
            Surface { Box(Modifier.testTag(TestTag).semantics { paneTitle = paneTitleString }) {} }
        }

        rule
            .onNodeWithTag(TestTag)
            .assert(
                SemanticsMatcher("unmerged paneTitle property") {
                    it.unmergedConfig.getOrNull(SemanticsProperties.PaneTitle) == paneTitleString
                }
            )

        rule
            .onNodeWithTag(TestTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, paneTitleString))
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun testSemanticsCalculatedOncePerComposition() {
        var recomposeScope: RecomposeScope? = null
        var count = 0
        fun Modifier.count() = semantics { count++ }
        rule.setContent {
            recomposeScope = currentRecomposeScope
            Box(modifier = Modifier.count().count().count())
        }
        rule.runOnIdle {
            if (ComposeUiFlags.isSemanticAutofillEnabled) {
                // with autofill on, semantics is eagerly evaluated
                assertThat(count).isEqualTo(3)
            } else {
                // before autofill, semantics was lazily evaluated
                assertThat(count).isEqualTo(0)
            }
            count = 0
            recomposeScope!!.invalidate()
        }
        rule.runOnIdle {
            if (ComposeUiFlags.isSemanticAutofillEnabled) {
                // with autofill on, semantics is eagerly evaluated
                assertThat(count).isEqualTo(3)
            } else {
                // before autofill, semantics was lazily evaluated
                assertThat(count).isEqualTo(0)
            }
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun isContainerProperty_unmergedConfig() {
        rule.setContent {
            // Non-clickable Material surfaces use `isContainer` to maintain desired default
            // behaviour in a non-clickable Surface for now. See aosp/1660323 for more details.
            // TODO(mnuzen): This behavior should be reverted after b/347038246 is resolved.
            Surface(Modifier.testTag(TestTag)) {
                Text("Hello World", modifier = Modifier.padding(8.dp))
            }
        }

        rule
            .onNodeWithTag(TestTag)
            .assert(
                SemanticsMatcher("unmerged container property") {
                    it.unmergedConfig.getOrNull(SemanticsProperties.IsContainer) == true
                }
            )

        rule
            .onNodeWithTag(TestTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.IsContainer, true))
    }

    @Test
    fun isTraversalGroupProperty_unmergedConfig() {
        rule.setContent {
            Surface(Modifier.testTag(TestTag).semantics { isTraversalGroup = true }) {
                Text("Hello World", modifier = Modifier.padding(8.dp))
            }
        }

        rule
            .onNodeWithTag(TestTag)
            .assert(
                SemanticsMatcher("unmerged traversalGroup property") {
                    it.unmergedConfig.getOrNull(SemanticsProperties.IsTraversalGroup) == true
                }
            )

        rule
            .onNodeWithTag(TestTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.IsTraversalGroup, true))
    }

    @Test
    fun traversalIndexProperty_unmergedConfig() {
        rule.setContent {
            Box(Modifier.semantics { traversalIndex = 0f }.testTag(TestTag)) {
                Text("Hello World", modifier = Modifier.padding(8.dp))
            }
        }

        rule
            .onNodeWithTag(TestTag)
            .assert(
                SemanticsMatcher("unmerged traversalIndex property") {
                    // Using unmerged config here since `traversalIndex` doesn't depend on `config`
                    it.unmergedConfig.getOrNull(SemanticsProperties.TraversalIndex) == 0f
                }
            )
        rule
            .onNodeWithTag(TestTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.TraversalIndex, 0f))
    }

    @Test
    fun traversalIndexPropertyNull() {
        rule.setContent {
            Box(Modifier.testTag(TestTag)) {
                Text("Hello World", modifier = Modifier.padding(8.dp))
            }
        }

        // If traversalIndex is not explicitly set, the default value is zero, but
        // only considered so when sorting in the DelegateCompat file
        rule.onNodeWithTag(TestTag).assertDoesNotHaveProperty(SemanticsProperties.TraversalIndex)
    }

    @Test
    @Suppress("DEPRECATION")
    fun isContainerPropertyDeprecated() {
        rule.setContent {
            Box(Modifier.testTag(TestTag).semantics { isContainer = true }) {
                Text("Hello World", modifier = Modifier.padding(8.dp))
            }
        }

        rule
            .onNodeWithTag(TestTag)
            .assert(
                SemanticsMatcher("container property") {
                    it.unmergedConfig.getOrNull(SemanticsProperties.IsContainer) == true
                }
            )
        rule
            .onNodeWithTag(TestTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.IsContainer, true))
    }

    @Test
    fun contentTypeProperty() {
        rule.setContent {
            SimpleTestLayout(
                Modifier.testTag(TestTag).semantics { contentType = ContentType.Username }
            ) {}
        }

        rule
            .onNodeWithTag(TestTag, useUnmergedTree = true)
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.ContentType, ContentType.Username)
            )

        rule
            .onNodeWithTag(TestTag)
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.ContentType, ContentType.Username)
            )
    }

    @Test
    fun contentDataTypeProperty() {
        rule.setContent {
            SimpleTestLayout(
                Modifier.testTag(TestTag).semantics { contentDataType = ContentDataType.Text }
            ) {}
        }

        rule
            .onNodeWithTag(TestTag, useUnmergedTree = true)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDataType,
                    ContentDataType.Text,
                )
            )

        rule
            .onNodeWithTag(TestTag)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDataType,
                    ContentDataType.Text,
                )
            )
    }

    @Test
    fun fillableDataProperty() {
        rule.setContent {
            SimpleTestLayout(
                Modifier.testTag(TestTag).semantics {
                    FillableData.createFrom("foo")?.let { fillableData = it }
                }
            ) {}
        }

        rule
            .onNodeWithTag(TestTag, useUnmergedTree = true)
            .assert(
                SemanticsMatcher("fillableData") {
                    it.config.getOrNull(SemanticsProperties.FillableData)?.textValue == "foo"
                }
            )

        rule
            .onNodeWithTag(TestTag)
            .assert(
                SemanticsMatcher("fillableData") {
                    it.config.getOrNull(SemanticsProperties.FillableData)?.textValue == "foo"
                }
            )
    }

    @Test
    fun onFillDataAction() {
        val actionLabel = "fill"
        var receivedData: FillableData? = null
        rule.setContent {
            SimpleTestLayout(
                Modifier.testTag(TestTag).semantics {
                    onFillData(
                        label = actionLabel,
                        action = {
                            receivedData = it
                            true
                        },
                    )
                }
            ) {}
        }

        val fillableData = FillableData.createFrom("foo")
        rule.onNodeWithTag(TestTag).performSemanticsAction(SemanticsActions.OnFillData) {
            fillableData?.let { data -> it(data) }
        }

        assertThat(receivedData?.textValue).isEqualTo("foo")
        fillableData?.let { data ->
            val fillMatcher =
                SemanticsMatcher("fill") { node ->
                    // Assert that the SemanticsAction.OnFillData exists,
                    node.config.getOrNull(SemanticsActions.OnFillData)?.let { action ->
                        // has the correct label and,
                        action.label == actionLabel
                        // when invoked with the provided data, the action returns true.
                        && action.action?.invoke(data) == true
                    } ?: false
                }

            rule.onNodeWithTag(TestTag, useUnmergedTree = true).assert(fillMatcher)
            rule.onNodeWithTag(TestTag).assert(fillMatcher)
        }
    }

    @Test
    fun depthFirstPropertyConcat() {
        val root = "root"
        val child1 = "child1"
        val grandchild1 = "grandchild1"
        val grandchild2 = "grandchild2"
        val child2 = "grandchild2"
        rule.setContent {
            SimpleTestLayout(
                Modifier.testTag(TestTag).semantics(mergeDescendants = true) { testProperty = root }
            ) {
                SimpleTestLayout(Modifier.semantics { testProperty = child1 }) {
                    SimpleTestLayout(Modifier.semantics { testProperty = grandchild1 }) {}
                    SimpleTestLayout(Modifier.semantics { testProperty = grandchild2 }) {}
                }
                SimpleTestLayout(Modifier.semantics { testProperty = child2 }) {}
            }
        }

        rule
            .onNodeWithTag(TestTag)
            .assertTestPropertyEquals("$root, $child1, $grandchild1, $grandchild2, $child2")
    }

    @Test
    fun nestedMergedSubtree() {
        val tag1 = "tag1"
        val tag2 = "tag2"
        val label1 = "foo"
        val label2 = "bar"
        rule.setContent {
            SimpleTestLayout(Modifier.semantics(mergeDescendants = true) {}.testTag(tag1)) {
                SimpleTestLayout(Modifier.semantics { testProperty = label1 }) {}
                SimpleTestLayout(Modifier.semantics(mergeDescendants = true) {}.testTag(tag2)) {
                    SimpleTestLayout(Modifier.semantics { testProperty = label2 }) {}
                }
            }
        }

        rule.onNodeWithTag(tag1).assertTestPropertyEquals(label1)
        rule.onNodeWithTag(tag2).assertTestPropertyEquals(label2)
    }

    @Test
    fun nestedMergedSubtree_includeAllMergeableChildren() {
        val tag1 = "tag1"
        val tag2 = "tag2"
        val label1 = "foo"
        val label2 = "bar"
        val label3 = "hi"
        rule.setContent {
            SimpleTestLayout(Modifier.semantics(mergeDescendants = true) {}.testTag(tag1)) {
                SimpleTestLayout(Modifier.semantics { testProperty = label1 }) {}
                SimpleTestLayout(Modifier.semantics(mergeDescendants = true) {}.testTag(tag2)) {
                    SimpleTestLayout(Modifier.semantics { testProperty = label2 }) {}
                }
                SimpleTestLayout(Modifier.semantics { testProperty = label3 }) {}
            }
        }

        rule.onNodeWithTag(tag1).assertTestPropertyEquals("$label1, $label3")
        rule.onNodeWithTag(tag2).assertTestPropertyEquals(label2)
    }

    @Test
    fun clearAndSetSemantics() {
        val tag1 = "tag1"
        val tag2 = "tag2"
        val label1 = "foo"
        val label2 = "hidden"
        val label3 = "baz"
        rule.setContent {
            SimpleTestLayout(Modifier.semantics(mergeDescendants = true) {}.testTag(tag1)) {
                SimpleTestLayout(Modifier.semantics { testProperty = label1 }) {}
                SimpleTestLayout(Modifier.clearAndSetSemantics {}) {
                    SimpleTestLayout(Modifier.semantics { testProperty = label2 }) {}
                }
                SimpleTestLayout(Modifier.clearAndSetSemantics { testProperty = label3 }) {
                    SimpleTestLayout(Modifier.semantics { testProperty = label2 }) {}
                }
                SimpleTestLayout(
                    Modifier.semantics(mergeDescendants = true) {}
                        .testTag(tag2)
                        .clearAndSetSemantics { text = AnnotatedString(label1) }
                ) {
                    SimpleTestLayout(Modifier.semantics { text = AnnotatedString(label2) }) {}
                }
            }
        }

        rule.onNodeWithTag(tag1).assertTestPropertyEquals("$label1, $label3")
        rule.onNodeWithTag(tag2).assertTextEquals(label1)
    }

    @Test
    fun clearAndSetSemantics_unmergedTree() {
        val tag1 = "tag1"
        val label1 = "foo"
        val label2 = "hidden"
        val label4 = "bar"
        rule.setContent {
            SimpleTestLayout(
                Modifier.testTag(tag1).clearAndSetSemantics { testProperty = label4 }
            ) {
                SimpleTestLayout(Modifier.semantics(true) { testProperty = label1 }) {
                    SimpleTestLayout(Modifier.semantics { testProperty = label2 }) {}
                }
            }
        }

        rule.onNodeWithTag(tag1).assertTestPropertyEquals("$label4")
    }

    @Test
    fun clearAndSetSemanticsSameLayoutNode() {
        val tag1 = "tag1"
        val tag2 = "tag2"
        val label1 = "foo"
        val label2 = "hidden"
        val label3 = "baz"
        rule.setContent {
            SimpleTestLayout(Modifier.semantics(mergeDescendants = true) {}.testTag(tag1)) {
                SimpleTestLayout(
                    Modifier.clearAndSetSemantics { testProperty = label1 }
                        .semantics { text = AnnotatedString(label2) }
                ) {}
                SimpleTestLayout(
                    Modifier.semantics { testProperty = label3 }
                        .clearAndSetSemantics { text = AnnotatedString(label3) }
                ) {}
            }
            SimpleTestLayout(
                Modifier.testTag(tag2)
                    .semantics { testProperty = label1 }
                    .clearAndSetSemantics {}
                    .semantics { text = AnnotatedString(label1) }
            ) {}
        }

        rule.onNodeWithTag(tag1).assertTestPropertyEquals("$label1, $label3")
        rule.onNodeWithTag(tag1).assertTextEquals(label3)
        rule.onNodeWithTag(tag2).assertTestPropertyEquals("$label1")
        rule.onNodeWithTag(tag2).assertDoesNotHaveProperty(SemanticsProperties.Text)
    }

    @Test
    fun clearAndSetSemantics_children() {
        rule.setContent {
            Column(Modifier.testTag("tag").semantics(true) {}.clearAndSetSemantics {}) {
                val size = Modifier.size(100.dp)
                Box(size.semantics { contentDescription = "box 1" })
                Box(size.clearAndSetSemantics { contentDescription = "box 2" }) {}
                Box(size.semantics(true) { contentDescription = "box 3" })
            }
        }

        val allChildren = rule.onNodeWithTag("tag", true).fetchSemanticsNode().children
        val children = rule.onNodeWithTag("tag", true).fetchSemanticsNode().replacedChildren

        assertTrue(children.isEmpty())
        assertTrue(allChildren.size == 3)

        val allChildrenMerged = rule.onNodeWithTag("tag").fetchSemanticsNode().children
        val childrenMerged = rule.onNodeWithTag("tag").fetchSemanticsNode().replacedChildren

        assertTrue(childrenMerged.isEmpty())
        assertTrue(allChildrenMerged.isEmpty())
    }

    @Test
    fun higherUpSemanticsOverridePropertiesOfLowerSemanticsOnSameNode() {
        rule.setContent {
            Box(
                Modifier.testTag("tag")
                    .semantics { contentDescription = "high" }
                    .semantics { contentDescription = "low" }
            )
        }

        rule
            .onNodeWithTag("tag")
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("high"))
            )
    }

    @Test
    fun replacedChildren_includeFakeNodes() {
        val tag = "tag1"
        rule.setContent {
            SimpleTestLayout(Modifier.clickable(role = Role.Button, onClick = {}).testTag(tag)) {
                BasicText("text")
            }
        }

        val node = rule.onNodeWithTag(tag, true).fetchSemanticsNode()
        val children = node.replacedChildren
        assertThat(children.count()).isEqualTo(2)
        assertThat(children.last().isFake).isTrue()
    }

    @Test
    fun children_doNotIncludeFakeNodes() {
        val tag = "tag1"
        rule.setContent {
            SimpleTestLayout(Modifier.clickable(role = Role.Button, onClick = {}).testTag(tag)) {
                BasicText("text")
            }
        }

        val node = rule.onNodeWithTag(tag, true).fetchSemanticsNode()
        val children = node.children
        assertThat(children.count()).isEqualTo(1)
        assertThat(children.last().isFake).isFalse()
    }

    @Test
    fun fakeSemanticsNode_usesValuesFromParent() {
        val tag = "tag1"
        rule.setContent {
            SimpleTestLayout(
                Modifier.offset(10.dp, 10.dp)
                    .clickable(role = Role.Button, onClick = {})
                    .testTag(tag)
            ) {
                BasicText("text")
            }
        }

        val node = rule.onNodeWithTag(tag, true).fetchSemanticsNode()
        val fakeNode = node.replacedChildren.first { it.isFake }

        // Ensure that the fake node uses the properties of the parent.
        assertThat(fakeNode.size).isNotEqualTo(IntSize.Zero)
        assertThat(fakeNode.boundsInRoot).isNotEqualTo(Rect.Zero)
        assertThat(fakeNode.positionInRoot).isNotEqualTo(Offset.Zero)
        assertThat(fakeNode.boundsInWindow).isNotEqualTo(Rect.Zero)
        assertThat(fakeNode.positionInWindow).isNotEqualTo(Offset.Zero)
        assertThat(fakeNode.isTransparent).isFalse()
    }

    @Test
    fun removingMergedSubtree_updatesSemantics() {
        val label = "foo"
        val showSubtree = mutableStateOf(true)
        rule.setContent {
            SimpleTestLayout(Modifier.semantics(mergeDescendants = true) {}.testTag(TestTag)) {
                if (showSubtree.value) {
                    SimpleTestLayout(Modifier.semantics { testProperty = label }) {}
                }
            }
        }

        rule.onNodeWithTag(TestTag).assertTestPropertyEquals(label)

        rule.runOnIdle { showSubtree.value = false }

        rule.onNodeWithTag(TestTag).assert(SemanticsMatcher.keyNotDefined(TestProperty))

        rule.onAllNodesWithText(label).assertCountEquals(0)
    }

    @Test
    fun addingNewMergedNode_updatesSemantics() {
        val label = "foo"
        val value = "bar"
        val showNewNode = mutableStateOf(false)
        rule.setContent {
            SimpleTestLayout(Modifier.semantics(mergeDescendants = true) {}.testTag(TestTag)) {
                SimpleTestLayout(Modifier.semantics { testProperty = label }) {}
                if (showNewNode.value) {
                    SimpleTestLayout(Modifier.semantics { stateDescription = value }) {}
                }
            }
        }

        rule
            .onNodeWithTag(TestTag)
            .assertTestPropertyEquals(label)
            .assertDoesNotHaveProperty(SemanticsProperties.StateDescription)

        rule.runOnIdle { showNewNode.value = true }

        rule.onNodeWithTag(TestTag).assertTestPropertyEquals(label).assertValueEquals(value)
    }

    @Test
    fun removingSubtreeWithoutSemanticsAsTopNode_updatesSemantics() {
        val label = "foo"
        val showSubtree = mutableStateOf(true)
        rule.setContent {
            SimpleTestLayout(Modifier.testTag(TestTag)) {
                if (showSubtree.value) {
                    SimpleTestLayout(Modifier.semantics { contentDescription = label }) {}
                }
            }
        }

        rule.onAllNodesWithContentDescription(label).assertCountEquals(1)

        rule.runOnIdle { showSubtree.value = false }

        rule.onAllNodesWithContentDescription(label).assertCountEquals(0)
    }

    @Test
    fun changingStackedSemanticsComponent_updatesSemantics() {
        val beforeLabel = "before"
        val afterLabel = "after"
        val isAfter = mutableStateOf(false)
        rule.setContent {
            SimpleTestLayout(
                Modifier.testTag(TestTag).semantics {
                    contentDescription = if (isAfter.value) afterLabel else beforeLabel
                }
            ) {}
        }

        rule.onNodeWithTag(TestTag).assertContentDescriptionEquals(beforeLabel)

        rule.runOnIdle { isAfter.value = true }

        rule.onNodeWithTag(TestTag).assertContentDescriptionEquals(afterLabel)
    }

    @Test
    fun changingStackedSemanticsComponent_notTopMost_updatesSemantics() {
        val beforeLabel = "before"
        val afterLabel = "after"
        val isAfter = mutableStateOf(false)

        rule.setContent {
            SimpleTestLayout(Modifier.testTag("don't care")) {
                SimpleTestLayout(
                    Modifier.testTag(TestTag).semantics {
                        contentDescription = if (isAfter.value) afterLabel else beforeLabel
                    }
                ) {}
            }
        }

        rule.onNodeWithTag(TestTag).assertContentDescriptionEquals(beforeLabel)

        rule.runOnIdle { isAfter.value = true }

        rule.onNodeWithTag(TestTag).assertContentDescriptionEquals(afterLabel)
    }

    @Test
    fun changingSemantics_belowStackedLayoutNodes_updatesCorrectly() {
        val beforeLabel = "before"
        val afterLabel = "after"
        val isAfter = mutableStateOf(false)

        rule.setContent {
            SimpleTestLayout {
                SimpleTestLayout {
                    SimpleTestLayout(
                        Modifier.testTag(TestTag).semantics {
                            contentDescription = if (isAfter.value) afterLabel else beforeLabel
                        }
                    ) {}
                }
            }
        }

        rule.onNodeWithTag(TestTag).assertContentDescriptionEquals(beforeLabel)

        rule.runOnIdle { isAfter.value = true }

        rule.onNodeWithTag(TestTag).assertContentDescriptionEquals(afterLabel)
    }

    @Test
    fun changingSemantics_belowNodeMergedThroughBoundary_updatesCorrectly() {
        val beforeLabel = "before"
        val afterLabel = "after"
        val isAfter = mutableStateOf(false)

        rule.setContent {
            SimpleTestLayout(Modifier.testTag(TestTag).semantics(mergeDescendants = true) {}) {
                SimpleTestLayout(
                    Modifier.semantics {
                        testProperty = if (isAfter.value) afterLabel else beforeLabel
                    }
                ) {}
            }
        }

        rule.onNodeWithTag(TestTag).assertTestPropertyEquals(beforeLabel)

        rule.runOnIdle { isAfter.value = true }

        rule.onNodeWithTag(TestTag).assertTestPropertyEquals(afterLabel)
    }

    @Test
    fun mergeDescendants_doesNotCrossLayoutNodesUpward() {
        val label = "label"
        rule.setContent {
            SimpleTestLayout(Modifier.testTag(TestTag)) {
                SimpleTestLayout(Modifier.semantics(mergeDescendants = true) {}) {
                    SimpleTestLayout(Modifier.semantics { contentDescription = label }) {}
                }
            }
        }

        rule
            .onNodeWithTag(TestTag)
            .assertDoesNotHaveProperty(SemanticsProperties.ContentDescription)
        rule.onNodeWithContentDescription(label).assertExists()
    }

    @Test
    fun updateToNodeWithMultipleBoundaryChildren_updatesCorrectly() {
        // This test reproduced a bug that caused a ConcurrentModificationException when
        // detaching SemanticsNodes

        val beforeLabel = "before"
        val afterLabel = "after"
        val isAfter = mutableStateOf(false)

        rule.setContent {
            SimpleTestLayout(
                Modifier.testTag(TestTag).semantics {
                    contentDescription = if (isAfter.value) afterLabel else beforeLabel
                }
            ) {
                SimpleTestLayout(Modifier.semantics {}) {}
                SimpleTestLayout(Modifier.semantics {}) {}
            }
        }

        rule.onNodeWithTag(TestTag).assertContentDescriptionEquals(beforeLabel)

        rule.runOnIdle { isAfter.value = true }

        rule.onNodeWithTag(TestTag).assertContentDescriptionEquals(afterLabel)
    }

    @Test
    fun changingSemantics_doesNotReplaceNodesBelow() {
        // Regression test for b/148606417
        var nodeCount = 0
        val beforeLabel = "before"
        val afterLabel = "after"

        // Do different things in an attempt to defeat a sufficiently clever compiler
        val beforeAction = { println("this never gets called") }
        val afterAction = { println("neither does this") }

        val isAfter = mutableStateOf(false)

        val content: @Composable () -> Unit = { SimpleTestLayout { nodeCount++ } }

        rule.setContent {
            SimpleTestLayout(
                Modifier.testTag(TestTag).semantics {
                    contentDescription = if (isAfter.value) afterLabel else beforeLabel
                    onClick(
                        action = {
                            if (isAfter.value) afterAction() else beforeAction()
                            return@onClick true
                        }
                    )
                },
                content = content,
            )
        }

        // This isn't the important part, just makes sure everything is behaving as expected
        rule.onNodeWithTag(TestTag).assertContentDescriptionEquals(beforeLabel)
        assertThat(nodeCount).isEqualTo(1)

        rule.runOnIdle { isAfter.value = true }

        // Make sure everything is still behaving as expected
        rule.onNodeWithTag(TestTag).assertContentDescriptionEquals(afterLabel)
        // This is the important part: make sure we didn't replace the identity due to unwanted
        // pivotal properties
        assertThat(nodeCount).isEqualTo(1)
    }

    @Test
    fun collapseSemanticsActions_prioritizeNonNullAction() {
        val actionLabel = "copy"
        rule.setContent {
            SimpleTestLayout(
                Modifier.testTag(TestTag)
                    .semantics { copyText(label = actionLabel, action = null) }
                    .semantics { copyText { true } }
            ) {}
        }
        rule
            .onNodeWithTag(TestTag)
            .assert(
                SemanticsMatcher("collapse copyText") {
                    it.config.getOrNull(SemanticsActions.CopyText)?.label == actionLabel &&
                        it.config.getOrNull(SemanticsActions.CopyText)?.action?.invoke() == true
                }
            )
    }

    @Test
    fun collapseSemanticsActions_prioritizeNonNullLabel() {
        val actionLabel = "copy"
        rule.setContent {
            SimpleTestLayout(
                Modifier.testTag(TestTag)
                    .semantics { copyText { false } }
                    .semantics { copyText(label = actionLabel, action = { true }) }
            ) {}
        }
        rule
            .onNodeWithTag(TestTag)
            .assert(
                SemanticsMatcher("collapse copyText") {
                    it.config.getOrNull(SemanticsActions.CopyText)?.label == actionLabel &&
                        it.config.getOrNull(SemanticsActions.CopyText)?.action?.invoke() == false
                }
            )
    }

    @Test
    fun collapseSemanticsActions_changeActionLabel_notMergeDescendants() {
        val actionLabel = "send"
        rule.setContent {
            SimpleTestLayout(
                Modifier.testTag(TestTag)
                    .semantics { onClick(label = actionLabel, action = null) }
                    .clickable {}
            ) {}
        }
        rule
            .onNodeWithTag(TestTag)
            .assert(
                SemanticsMatcher("collapse onClick") {
                    it.config.getOrNull(SemanticsActions.OnClick)?.label == actionLabel &&
                        it.config.getOrNull(SemanticsActions.OnClick)?.action?.invoke() == true
                }
            )
    }

    @Test
    fun collapseSemanticsActions_changeActionLabel_mergeDescendants() {
        val actionLabel = "send"
        rule.setContent {
            SimpleTestLayout(
                Modifier.testTag(TestTag)
                    .semantics(mergeDescendants = true) {
                        onClick(label = actionLabel, action = null)
                    }
                    .clickable {}
            ) {}
        }
        rule
            .onNodeWithTag(TestTag)
            .assert(
                SemanticsMatcher("collapse onClick") {
                    it.config.getOrNull(SemanticsActions.OnClick)?.label == actionLabel &&
                        it.config.getOrNull(SemanticsActions.OnClick)?.action?.invoke() == true
                }
            )
    }

    @Test
    fun mergeSemanticsActions_prioritizeNonNullAction_mergeDescendants_descendantMergeable() {
        val actionLabel = "show more"
        rule.setContent {
            SimpleTestLayout(
                Modifier.testTag(TestTag).semantics(mergeDescendants = true) {
                    expand(label = actionLabel, action = null)
                }
            ) {
                SimpleTestLayout(Modifier.semantics { expand { true } }) {}
            }
        }
        rule
            .onNodeWithTag(TestTag)
            .assert(
                SemanticsMatcher("merge expand action") {
                    it.config.getOrNull(SemanticsActions.Expand)?.label == actionLabel &&
                        it.config.getOrNull(SemanticsActions.Expand)?.action?.invoke() == true
                }
            )
    }

    @Test
    fun mergeSemanticsActions_prioritizeNonNullLabel_mergeDescendants_descendantMergeable() {
        val actionLabel = "show more"
        rule.setContent {
            SimpleTestLayout(
                Modifier.testTag(TestTag).semantics(mergeDescendants = true) { expand { false } }
            ) {
                SimpleTestLayout(
                    Modifier.semantics { expand(label = actionLabel, action = { true }) }
                ) {}
            }
        }
        rule
            .onNodeWithTag(TestTag)
            .assert(
                SemanticsMatcher("merge expand action") {
                    it.config.getOrNull(SemanticsActions.Expand)?.label == actionLabel &&
                        it.config.getOrNull(SemanticsActions.Expand)?.action?.invoke() == false
                }
            )
    }

    @Test
    fun mergeSemanticsActions_changeActionLabelNotWork_notMergeDescendants_descendantMergeable() {
        val actionLabel = "show less"
        rule.setContent {
            SimpleTestLayout(
                Modifier.testTag(TestTag).semantics { collapse(label = actionLabel, action = null) }
            ) {
                SimpleTestLayout(Modifier.semantics { collapse { true } }) {}
            }
        }
        rule
            .onNodeWithTag(TestTag)
            .assert(
                SemanticsMatcher("merge collapse action") {
                    it.config.getOrNull(SemanticsActions.Collapse)?.label == actionLabel &&
                        it.config.getOrNull(SemanticsActions.OnClick)?.action == null
                }
            )
    }

    @Test
    fun mergeSemanticsActions_changeActionLabelNotWork_notMergeDescendants_descendantUnmergeable() {
        val actionLabel = "send"
        rule.setContent {
            SimpleTestLayout(
                Modifier.testTag(TestTag).semantics { onClick(label = actionLabel, action = null) }
            ) {
                SimpleTestLayout(Modifier.clickable {}) {}
            }
        }
        rule
            .onNodeWithTag(TestTag)
            .assert(
                SemanticsMatcher("merge onClick") {
                    it.config.getOrNull(SemanticsActions.OnClick)?.label == actionLabel &&
                        it.config.getOrNull(SemanticsActions.OnClick)?.action == null
                }
            )
    }

    @Test
    fun mergeSemanticsActions_changeActionLabelNotWork_mergeDescendants_descendantUnmergeable() {
        val actionLabel = "send"
        rule.setContent {
            SimpleTestLayout(
                Modifier.testTag(TestTag).semantics(mergeDescendants = true) {
                    onClick(label = actionLabel, action = null)
                }
            ) {
                SimpleTestLayout(Modifier.clickable {}) {}
            }
        }
        rule
            .onNodeWithTag(TestTag)
            .assert(
                SemanticsMatcher("merge onClick") {
                    it.config.getOrNull(SemanticsActions.OnClick)?.label == actionLabel &&
                        it.config.getOrNull(SemanticsActions.OnClick)?.action == null
                }
            )
    }

    @Test
    fun testInspectorValue() {
        val properties: SemanticsPropertyReceiver.() -> Unit = {
            paneTitle = "testTitle"
            focused = false
            role = Role.Image
        }
        rule.setContent {
            val modifier = Modifier.semantics(true, properties) as InspectableValue

            assertThat(modifier.nameFallback).isEqualTo("semantics")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.asIterable())
                .containsExactly(
                    ValueElement("mergeDescendants", true),
                    ValueElement(
                        "properties",
                        mapOf("PaneTitle" to "testTitle", "Focused" to false, "Role" to Role.Image),
                    ),
                )
        }
    }

    @Test
    fun testChildrenAreZSorted() {
        val child1 = "child1"
        val child2 = "child2"
        rule.setContent {
            SimpleTestLayout(Modifier.testTag(TestTag)) {
                SimpleTestLayout(Modifier.zIndex(1f).semantics { testTag = child1 }) {}
                SimpleTestLayout(Modifier.semantics { testTag = child2 }) {}
            }
        }

        val root = rule.onNodeWithTag(TestTag).fetchSemanticsNode("can't find node $TestTag")
        assertEquals(2, root.children.size)
        assertEquals(child2, root.children[0].config.getOrNull(SemanticsProperties.TestTag))
        assertEquals(child1, root.children[1].config.getOrNull(SemanticsProperties.TestTag))
    }

    @Test
    fun delegatedSemanticsPropertiesGetRead() {
        val node =
            object : DelegatingNode() {
                val inner = delegate(SemanticsMod { contentDescription = "hello world" })
            }

        rule.setContent { Box(Modifier.testTag(TestTag).elementFor(node)) }

        rule.onNodeWithTag(TestTag).assertContentDescriptionEquals("hello world")
    }

    @Test
    fun multipleDelegatesGetCombined() {
        val node =
            object : DelegatingNode() {
                val a = delegate(SemanticsMod { contentDescription = "hello world" })
                val b = delegate(SemanticsMod { testProperty = "bar" })
            }

        rule.setContent { Box(Modifier.testTag(TestTag).elementFor(node)) }

        rule
            .onNodeWithTag(TestTag)
            .assertContentDescriptionEquals("hello world")
            .assertTestPropertyEquals("bar")
    }

    @Test
    fun testBoundsInParent() {
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp()).padding(10.toDp(), 20.toDp()).semantics {}) {
                    Box(Modifier.size(10.toDp()).offset(20.toDp(), 30.toDp())) {
                        Box(Modifier.size(1.toDp()).testTag(TestTag)) {}
                    }
                }
            }
        }

        rule.waitForIdle()

        val bounds = rule.onNodeWithTag(TestTag, true).fetchSemanticsNode().boundsInParent
        assertEquals(Rect(20.0f, 30.0f, 21.0f, 31.0f), bounds)
    }

    @Test
    fun testBoundsInParent_boundsInRootWhenNoParent() {
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp()).padding(10.toDp(), 20.toDp())) {
                    Box(Modifier.size(10.toDp()).offset(20.toDp(), 30.toDp())) {
                        Box(Modifier.size(1.toDp()).testTag(TestTag)) {}
                    }
                }
            }
        }

        rule.waitForIdle()

        val bounds = rule.onNodeWithTag(TestTag, true).fetchSemanticsNode().boundsInParent
        assertEquals(Rect(30.0f, 50.0f, 31.0f, 51.0f), bounds)
    }

    @Test
    fun testBoundsInParent_boundsInRootWhenParentIsNotImportantForBounds() {
        val notImportantForBoundsSemanticsModifier =
            object : SemanticsModifierNode, Modifier.Node() {
                override val isImportantForBounds = false

                override fun SemanticsPropertyReceiver.applySemantics() {}
            }
        rule.setContent {
            with(LocalDensity.current) {
                Box(
                    Modifier.size(100.toDp())
                        .padding(10.toDp(), 20.toDp())
                        .elementFor(notImportantForBoundsSemanticsModifier)
                ) {
                    Box(Modifier.size(10.toDp()).offset(20.toDp(), 30.toDp())) {
                        Box(Modifier.size(1.toDp()).testTag(TestTag)) {}
                    }
                }
            }
        }

        rule.waitForIdle()

        val bounds = rule.onNodeWithTag(TestTag, true).fetchSemanticsNode().boundsInParent
        assertEquals(Rect(30.0f, 50.0f, 31.0f, 51.0f), bounds)
    }

    @Test
    fun testRegenerateSemanticsId() {
        var reuseKey by mutableStateOf(0)
        rule.setContent { ReusableContent(reuseKey) { Box(Modifier.testTag(TestTag)) } }
        val oldId = rule.onNodeWithTag(TestTag).fetchSemanticsNode().id
        rule.runOnIdle { reuseKey = 1 }
        val newId = rule.onNodeWithTag(TestTag).fetchSemanticsNode().id

        assertNotEquals(oldId, newId)
    }

    @Test
    fun testSetTextSubstitution_annotatedString() {
        rule.setContent { Surface { Text(AnnotatedString("hello"), Modifier.testTag(TestTag)) } }

        val config = rule.onNodeWithTag(TestTag, true).fetchSemanticsNode().config
        assertEquals(null, config.getOrNull(SemanticsProperties.IsShowingTextSubstitution))

        rule.runOnUiThread {
            config
                .getOrNull(SemanticsActions.SetTextSubstitution)
                ?.action
                ?.invoke(AnnotatedString("bonjour"))
        }

        rule.waitForIdle()

        var newConfig = rule.onNodeWithTag(TestTag, true).fetchSemanticsNode().config
        // SetTextSubstitution doesn't trigger text update
        assertThat(newConfig.getOrNull(SemanticsProperties.Text))
            .containsExactly(AnnotatedString("hello"))
        assertEquals(
            AnnotatedString("bonjour"),
            newConfig.getOrNull(SemanticsProperties.TextSubstitution),
        )
        assertEquals(false, newConfig.getOrNull(SemanticsProperties.IsShowingTextSubstitution))

        rule.runOnUiThread {
            config.getOrNull(SemanticsActions.ShowTextSubstitution)?.action?.invoke(true)
        }

        rule.waitForIdle()

        newConfig = rule.onNodeWithTag(TestTag, true).fetchSemanticsNode().config
        // ShowTextSubstitution triggers text update
        assertThat(newConfig.getOrNull(SemanticsProperties.Text))
            .containsExactly(AnnotatedString("hello"))
        assertEquals(
            AnnotatedString("bonjour"),
            newConfig.getOrNull(SemanticsProperties.TextSubstitution),
        )
        assertEquals(true, newConfig.getOrNull(SemanticsProperties.IsShowingTextSubstitution))
    }

    @Test
    fun testSetTextSubstitution_simpleString() {
        rule.setContent { Surface { Text("hello", Modifier.testTag(TestTag)) } }

        val config = rule.onNodeWithTag(TestTag, true).fetchSemanticsNode().config
        assertEquals(null, config.getOrNull(SemanticsProperties.IsShowingTextSubstitution))

        rule.runOnUiThread {
            config
                .getOrNull(SemanticsActions.SetTextSubstitution)
                ?.action
                ?.invoke(AnnotatedString("bonjour"))
        }

        rule.waitForIdle()

        var newConfig = rule.onNodeWithTag(TestTag, true).fetchSemanticsNode().config
        // SetTextSubstitution doesn't trigger text update
        assertThat(newConfig.getOrNull(SemanticsProperties.Text))
            .containsExactly(AnnotatedString("hello"))
        assertEquals(
            AnnotatedString("bonjour"),
            newConfig.getOrNull(SemanticsProperties.TextSubstitution),
        )
        assertEquals(false, newConfig.getOrNull(SemanticsProperties.IsShowingTextSubstitution))

        rule.runOnUiThread {
            config.getOrNull(SemanticsActions.ShowTextSubstitution)?.action?.invoke(true)
        }

        rule.waitForIdle()

        newConfig = rule.onNodeWithTag(TestTag, true).fetchSemanticsNode().config
        // ShowTextSubstitution triggers text update
        assertThat(newConfig.getOrNull(SemanticsProperties.Text))
            .containsExactly(AnnotatedString("hello"))
        assertEquals(
            AnnotatedString("bonjour"),
            newConfig.getOrNull(SemanticsProperties.TextSubstitution),
        )
        assertEquals(true, newConfig.getOrNull(SemanticsProperties.IsShowingTextSubstitution))
    }

    @Test
    fun testGetTextSizeFromTextLayoutResult() {
        var density = Float.NaN
        rule.setContent {
            with(LocalDensity.current) { density = 1.sp.toPx() }
            Surface { Text(AnnotatedString("hello"), Modifier.testTag(TestTag), fontSize = 14.sp) }
        }

        val config = rule.onNodeWithTag(TestTag, true).fetchSemanticsNode().config

        val textLayoutResult: TextLayoutResult
        val textLayoutResults = mutableListOf<TextLayoutResult>()
        val getLayoutResult =
            config[SemanticsActions.GetTextLayoutResult].action?.invoke(textLayoutResults)

        assertEquals(true, getLayoutResult)

        textLayoutResult = textLayoutResults[0]
        val result = textLayoutResult.layoutInput

        assertEquals(density, result.density.density)
        assertEquals(14.0f, result.style.fontSize.value)
    }
}

private fun SemanticsNodeInteraction.assertDoesNotHaveProperty(property: SemanticsPropertyKey<*>) {
    assert(SemanticsMatcher.keyNotDefined(property))
}

fun SemanticsNodeInteraction.assertContentDataTypeEquals(
    expected: ContentDataType
): SemanticsNodeInteraction {
    val actual = fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDataType)
    assert(actual == expected) {
        "ContentDataType assertion failed.\nExpected: '$expected'\nActual: '$actual'"
    }
    return this
}

private val TestProperty =
    SemanticsPropertyKey<String>("TestProperty") { parent, child ->
        if (parent == null) child else "$parent, $child"
    }

internal var SemanticsPropertyReceiver.testProperty by TestProperty

private fun SemanticsNodeInteraction.assertUnmergedTestPropertyEquals(value: String) =
    assert(SemanticsMatcher(value) { it.unmergedConfig.getOrNull(TestProperty) == value })

internal fun SemanticsNodeInteraction.assertTestPropertyEquals(value: String) =
    assert(SemanticsMatcher.expectValue(TestProperty, value))

// Falsely mark the layout counter stable to avoid influencing recomposition behavior
@Stable private class Counter(var count: Int)

@Composable
private fun CountingLayout(modifier: Modifier, counter: Counter) {
    Layout(
        modifier = modifier,
        content = {},
        measurePolicy =
            remember {
                MeasurePolicy { _, constraints ->
                    counter.count++
                    layout(constraints.minWidth, constraints.minHeight) {}
                }
            },
    )
}

/**
 * A simple test layout that does the bare minimum required to lay out an arbitrary number of
 * children reasonably. Useful for Semantics hierarchy testing
 */
@Composable
private fun SimpleTestLayout(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        if (measurables.isEmpty()) {
            layout(constraints.minWidth, constraints.minHeight) {}
        } else {
            val placeables = measurables.map { it.measure(constraints) }
            val (width, height) =
                with(placeables.filterNotNull()) {
                    Pair(
                        max(maxByOrNull { it.width }?.width ?: 0, constraints.minWidth),
                        max(maxByOrNull { it.height }?.height ?: 0, constraints.minHeight),
                    )
                }
            layout(width, height) {
                for (placeable in placeables) {
                    placeable.placeRelative(0, 0)
                }
            }
        }
    }
}

/**
 * A simple SubComposeLayout which lays [contentOne] at [positionOne] and lays [contentTwo] at
 * [positionTwo]. [contentOne] is placed first and [contentTwo] is placed second. Therefore, the
 * semantics node for [contentOne] is before semantics node for [contentTwo] in
 * [SemanticsNode.children].
 */
@Composable
private fun SimpleSubcomposeLayout(
    modifier: Modifier = Modifier,
    contentOne: @Composable () -> Unit,
    positionOne: Offset,
    contentTwo: @Composable () -> Unit,
    positionTwo: Offset,
) {
    SubcomposeLayout(modifier) { constraints ->
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight

        val looseConstraints = constraints.copyMaxDimensions()

        layout(layoutWidth, layoutHeight) {
            val placeablesOne =
                subcompose(TestSlot.First, contentOne).fastMap { it.measure(looseConstraints) }

            val placeablesTwo =
                subcompose(TestSlot.Second, contentTwo).fastMap { it.measure(looseConstraints) }

            // Placing to control drawing order to match default elevation of each placeable
            placeablesOne.fastForEach { it.place(positionOne.x.toInt(), positionOne.y.toInt()) }
            placeablesTwo.fastForEach { it.place(positionTwo.x.toInt(), positionTwo.y.toInt()) }
        }
    }
}

private enum class TestSlot {
    First,
    Second,
}

internal fun SemanticsMod(
    mergeDescendants: Boolean = false,
    properties: SemanticsPropertyReceiver.() -> Unit,
): CoreSemanticsModifierNode {
    return CoreSemanticsModifierNode(
        mergeDescendants = mergeDescendants,
        isClearingSemantics = false,
        properties = properties,
    )
}

internal fun Modifier.elementFor(node: Modifier.Node): Modifier {
    return this then NodeElement(node)
}

internal data class NodeElement(val node: Modifier.Node) : ModifierNodeElement<Modifier.Node>() {
    override fun create(): Modifier.Node = node

    override fun update(node: Modifier.Node) {}
}
