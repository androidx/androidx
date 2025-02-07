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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.elementOf
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.node.requireLayoutNode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@SmallTest
@RunWith(Parameterized::class)
class SemanticsModifierNodeTest(private val precomputedSemantics: Boolean) {
    @get:Rule val rule = createComposeRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "pre-computed semantics = {0}")
        fun initParameters() = listOf(false, true)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private val previousFlagValue = ComposeUiFlags.isSemanticAutofillEnabled

    @Before
    fun enableAutofill() {
        @OptIn(ExperimentalComposeUiApi::class)
        ComposeUiFlags.isSemanticAutofillEnabled = precomputedSemantics
    }

    @After
    fun disableAutofill() {
        @OptIn(ExperimentalComposeUiApi::class)
        ComposeUiFlags.isSemanticAutofillEnabled = previousFlagValue
    }

    @Test
    fun applySemantics_firstComposition() {
        // Arrange.
        val semanticsModifier = TestSemanticsModifier { testTag = "TestTag" }
        rule.setContent { Box(Modifier.elementOf(semanticsModifier)) }

        // Assert.
        rule.runOnIdle {
            if (precomputedSemantics) {
                assertThat(semanticsModifier.applySemanticsInvocations).isEqualTo(1)
            } else {
                assertThat(semanticsModifier.applySemanticsInvocations).isEqualTo(0)
            }
        }
    }

    @Test
    fun applySemantics_calledWhenSemanticsIsRead() {
        // Arrange.
        val semanticsModifier = TestSemanticsModifier { testTag = "TestTag" }
        rule.setContent { Box(Modifier.elementOf(semanticsModifier)) }
        rule.runOnIdle { semanticsModifier.resetCounters() }

        // Act.
        rule.onNodeWithTag("TestTag").assertExists()

        // Assert.
        rule.runOnIdle {
            assertThat(semanticsModifier.applySemanticsInvocations)
                .isEqualTo(if (precomputedSemantics) 0 else 1)
        }
    }

    // This fixes b/392442163 and fixes a bug in NodeChain.isUpdating().
    @Test
    fun applySemantics_alongWithModifierRemoval() {
        // Helper functions.
        class EnabledNode(var isEnabled: Boolean) : SemanticsModifierNode, DelegatingNode() {
            override val shouldAutoInvalidate: Boolean = false

            override fun SemanticsPropertyReceiver.applySemantics() {
                if (!isEnabled) disabled()
            }
        }

        data class EnabledElement(val isEnabled: Boolean) : ModifierNodeElement<EnabledNode>() {
            override fun create() = EnabledNode(isEnabled)

            override fun update(node: EnabledNode) {
                // Invalidating before setting the new value should not matter since semantics is
                // calculated after the modifier update completes.
                node.invalidateSemantics()
                node.isEnabled = isEnabled
            }
        }

        fun Modifier.enabled(enabled: Boolean) = this.then(EnabledElement(enabled))

        // Arrange.
        var enabled by mutableStateOf(true)
        rule.setContent {
            Box(
                modifier =
                    Modifier.then(if (enabled) Modifier.size(10.dp) else Modifier)
                        .testTag("tag")
                        .enabled(enabled)
            )
        }

        // Act.
        rule.runOnIdle { enabled = false }

        // Assert.
        rule.onNodeWithTag("tag").assertIsNotEnabled()
    }

    @Test
    fun applySemantics_calledWhenFetchSemanticsNodeIsCalled() {
        // Arrange.
        val semanticsModifier = TestSemanticsModifier { testTag = "TestTag" }
        rule.setContent { Box(Modifier.elementOf(semanticsModifier)) }
        rule.runOnIdle { semanticsModifier.resetCounters() }

        // Act.
        rule.onNodeWithTag("TestTag").fetchSemanticsNode()

        // Assert.
        rule.runOnIdle {
            assertThat(semanticsModifier.applySemanticsInvocations)
                .isEqualTo(if (precomputedSemantics) 0 else 1)
        }
    }

    @Test
    fun applySemantics_multipleCallsToFetchSemanticsNodeDoesNotTriggerMultipleCalls() {
        // Arrange.
        val semanticsModifier = TestSemanticsModifier { testTag = "TestTag" }
        rule.setContent { Box(Modifier.elementOf(semanticsModifier)) }
        rule.runOnIdle { semanticsModifier.resetCounters() }

        // Act.
        rule.onNodeWithTag("TestTag").fetchSemanticsNode()
        rule.onNodeWithTag("TestTag").fetchSemanticsNode()
        rule.onNodeWithTag("TestTag").fetchSemanticsNode()

        // Assert.
        rule.runOnIdle {
            assertThat(semanticsModifier.applySemanticsInvocations)
                .isEqualTo(if (precomputedSemantics) 0 else 1)
        }
    }

    @Test
    fun applySemantics_InResponseToInvalidateSemantics() {
        // Arrange.
        val semanticsModifier = TestSemanticsModifier { testTag = "TestTag" }
        rule.setContent { Box(Modifier.elementOf(semanticsModifier)) }
        rule.runOnIdle { semanticsModifier.resetCounters() }

        // Act.
        rule.runOnIdle { semanticsModifier.invalidateSemantics() }

        // Assert.
        rule.runOnIdle {
            assertThat(semanticsModifier.applySemanticsInvocations)
                .isEqualTo(if (precomputedSemantics) 1 else 0)
        }
    }

    @Test
    fun applySemantics_calledAgainInResponseToInvalidateSemantics() {
        // Arrange.
        val semanticsModifier = TestSemanticsModifier { testTag = "TestTag" }
        rule.setContent { Box(Modifier.elementOf(semanticsModifier)) }
        rule.runOnIdle { semanticsModifier.resetCounters() }

        // Act.
        rule.onNodeWithTag("TestTag").assertExists()
        rule.runOnIdle { semanticsModifier.invalidateSemantics() }
        rule.onNodeWithTag("TestTag").assertExists()

        // Assert.
        rule.runOnIdle {
            assertThat(semanticsModifier.applySemanticsInvocations)
                .isEqualTo(if (precomputedSemantics) 1 else 2)
        }
    }

    @Test
    fun applySemantics_calledInResponseToMultipleInvalidateSemantics() {
        // Arrange.
        val semanticsModifier = TestSemanticsModifier { testTag = "TestTag" }
        rule.setContent { Box(Modifier.elementOf(semanticsModifier)) }

        // Act.
        rule.onNodeWithTag("TestTag").assertExists()
        rule.runOnIdle { semanticsModifier.invalidateSemantics() }
        rule.runOnIdle { semanticsModifier.invalidateSemantics() }
        rule.runOnIdle { semanticsModifier.invalidateSemantics() }
        rule.runOnIdle { semanticsModifier.invalidateSemantics() }
        rule.runOnIdle { semanticsModifier.invalidateSemantics() }
        rule.runOnIdle { semanticsModifier.invalidateSemantics() }
        rule.onNodeWithTag("TestTag").assertExists()

        // Assert.
        rule.runOnIdle {
            assertThat(semanticsModifier.applySemanticsInvocations)
                .isEqualTo(if (precomputedSemantics) 7 else 2)
        }
    }

    @Test
    fun applySemantics_calledAutomaticallyInResponseToChangesToObservedReads() {
        // Arrange.
        var tag by mutableStateOf("tag1")
        val semanticsModifier = TestSemanticsModifier { testTag = tag }
        rule.setContent { Box(Modifier.elementOf(semanticsModifier)) }
        rule.onNodeWithTag("tag1").assertExists()
        semanticsModifier.resetCounters()

        // Act,
        rule.runOnIdle { tag = "tag2" }
        rule.onNodeWithTag("tag2").assertExists()

        // Assert - additional invocation due to the change in mutable state.
        rule.runOnIdle { assertThat(semanticsModifier.applySemanticsInvocations).isEqualTo(1) }
    }

    @Test
    fun applySemantics_notCalledAutomaticallyInResponseToChangesToNonObservedReads() {
        // Arrange.
        var tag = "tag1"
        val semanticsModifier = TestSemanticsModifier { testTag = tag }
        rule.setContent { Box(Modifier.elementOf(semanticsModifier)) }
        rule.onNodeWithTag("tag1").assertExists()
        semanticsModifier.resetCounters()

        // Act,
        rule.runOnIdle { tag = "tag2" }

        // Assert - Can't find the new item without invalidation.
        rule.onNodeWithTag("tag2").assertDoesNotExist()
        rule.runOnIdle { assertThat(semanticsModifier.applySemanticsInvocations).isEqualTo(0) }
    }

    @Test
    fun applySemantics_calledInResponseToChangesToNonObservedReads_whenInvalidated() {
        // Arrange.
        var tag = "tag1"
        val semanticsModifier = TestSemanticsModifier { testTag = tag }
        rule.setContent { Box(Modifier.elementOf(semanticsModifier)) }
        rule.onNodeWithTag("tag1").assertExists()
        semanticsModifier.resetCounters()

        // Act,
        rule.runOnIdle {
            tag = "tag2"
            semanticsModifier.invalidateSemantics()
        }

        // Assert - finds the new item after invalidation.
        rule.onNodeWithTag("tag2").assertExists()
        rule.runOnIdle { assertThat(semanticsModifier.applySemanticsInvocations).isEqualTo(1) }
    }

    @Test
    fun applySemantics_notCalledForDeactivatedNode() {
        // Arrange.
        lateinit var lazyListState: LazyListState
        val semanticsModifiers = List(2) { TestSemanticsModifier { testTag = "$it" } }
        rule.setContent {
            lazyListState = rememberLazyListState()
            LazyRow(state = lazyListState, modifier = Modifier.size(10.dp)) {
                items(2) { index -> Box(Modifier.size(10.dp).elementOf(semanticsModifiers[index])) }
            }
        }
        rule.runOnIdle { semanticsModifiers[0].resetCounters() }

        // Act.
        rule.runOnIdle { lazyListState.requestScrollToItem(1) }

        // Assert.
        rule.onNodeWithTag("0").assertDoesNotExist()
        assertThat(semanticsModifiers[0].applySemanticsInvocations).isEqualTo(0)
        assertThat(semanticsModifiers[0].requireLayoutNode().semanticsConfiguration).isNull()
        assertThat(semanticsModifiers[0].applySemanticsInvocations).isEqualTo(0)
    }

    @Test
    fun invalidateSemantics_onDeactivatedNodeDoesNotCrash() {
        // Arrange.
        lateinit var lazyListState: LazyListState
        val semanticsModifiers = List(2) { TestSemanticsModifier { testTag = "$it" } }
        rule.setContent {
            lazyListState = rememberLazyListState()
            LazyRow(state = lazyListState, modifier = Modifier.size(10.dp)) {
                items(2) { index -> Box(Modifier.size(10.dp).elementOf(semanticsModifiers[index])) }
            }
        }
        rule.runOnIdle { lazyListState.requestScrollToItem(1) }

        // Act.
        rule.runOnIdle { semanticsModifiers[0].invalidateSemantics() }

        // Assert.
        rule.onNodeWithTag("0").assertDoesNotExist()
        assertThat(semanticsModifiers[0].requireLayoutNode().semanticsConfiguration).isNull()
    }

    @Test
    fun invalidateSemantics_calledWithinApplySemantics_doesNotTriggerAnotherCallToApplySemantics() {
        // Arrange.
        lateinit var semanticsModifier: TestSemanticsModifier
        semanticsModifier = TestSemanticsModifier {
            testTag = "tag"
            semanticsModifier.invalidateSemantics()
        }
        rule.setContent { Box(Modifier.elementOf(semanticsModifier)) }
        rule.runOnIdle { semanticsModifier.resetCounters() }

        // Act.
        rule.onNodeWithTag("tag").assertExists()

        // Assert.
        if (precomputedSemantics) {
            assertThat(semanticsModifier.applySemanticsInvocations).isEqualTo(0)
        } else {
            assertThat(semanticsModifier.applySemanticsInvocations).isEqualTo(1)
        }
    }

    private class TestSemanticsModifier(
        private val onApplySemantics: SemanticsPropertyReceiver.() -> Unit
    ) : SemanticsModifierNode, Modifier.Node() {
        var applySemanticsInvocations = 0

        fun resetCounters() {
            applySemanticsInvocations = 0
        }

        override fun SemanticsPropertyReceiver.applySemantics() {
            applySemanticsInvocations++
            onApplySemantics.invoke(this)
        }
    }
}
