/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.contextmenu.test.assertNotNull
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNull
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ScrollIndicatorTest {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    private val testState =
        object : ScrollIndicatorState {
            override val scrollOffset: Int = 0
            override val contentSize: Int = 100
            override val viewportSize: Int = 10
        }

    @Test
    fun scrollIndicatorModifier_inspectorInfo() {
        rule.setContent {
            val modifier =
                Modifier.scrollIndicator(
                    TestScrollIndicatorFactory(),
                    testState,
                    Orientation.Vertical,
                ) as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("scrollIndicator")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable())
                .containsExactly("factory", "state", "orientation")
        }
    }

    @Test
    fun scrollIndicatorModifier_producesEqualsModifiersForTheSameInput() {
        val factory = TestScrollIndicatorFactory()
        val first = Modifier.scrollIndicator(factory, testState, Orientation.Vertical)
        val second = Modifier.scrollIndicator(factory, testState, Orientation.Vertical)
        assertThat(first).isEqualTo(second)
    }

    @Test
    fun scrollIndicatorModifier_producesNotEqualsModifiersForDifferentInput() {
        val factory1 = TestScrollIndicatorFactory()
        val factory2 = TestScrollIndicatorFactory()

        val state2 =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 50
                override val contentSize: Int = 200
                override val viewportSize: Int = 20
            }

        val base = Modifier.scrollIndicator(factory1, testState, Orientation.Vertical)

        val diffFactory = Modifier.scrollIndicator(factory2, testState, Orientation.Vertical)
        assertThat(base).isNotEqualTo(diffFactory)

        val diffState = Modifier.scrollIndicator(factory1, state2, Orientation.Vertical)
        assertThat(base).isNotEqualTo(diffState)

        val diffOrientation = Modifier.scrollIndicator(factory1, testState, Orientation.Horizontal)
        assertThat(base).isNotEqualTo(diffOrientation)
    }

    @Test
    fun scrollIndicatorModifier_attachesNode() {
        val factory = TestScrollIndicatorFactory()

        rule.setContent { Box(Modifier.scrollIndicator(factory, testState, Orientation.Vertical)) }

        rule.runOnIdle {
            assertNotNull(factory.lastCreatedNode)
            assertThat(factory.lastCreatedNode?.isAttached).isTrue()
        }
    }

    @Test
    fun scrollIndicatorModifier_undelegatesNodeOnDetach() {
        val factory = TestScrollIndicatorFactory()

        var addModifier by mutableStateOf(true)

        rule.setContent {
            Box(
                if (addModifier) {
                    Modifier.scrollIndicator(factory, testState, Orientation.Vertical)
                } else {
                    Modifier
                }
            )
        }

        val firstNode =
            rule.runOnIdle {
                val node = factory.lastCreatedNode
                assertNotNull(node)
                assertThat(node?.node).isNotEqualTo(node)
                assertThat(node?.isAttached).isTrue()
                addModifier = false
                node
            }

        rule.runOnIdle {
            assertThat(firstNode?.node).isEqualTo(firstNode)
            assertThat(firstNode?.isAttached).isFalse()
            addModifier = true
        }

        rule.runOnIdle {
            assertNotNull(factory.lastCreatedNode)
            assertThat(factory.lastCreatedNode).isNotSameInstanceAs(firstNode)
            assertThat(factory.lastCreatedNode?.isAttached).isTrue()
        }
    }

    @Test
    fun scrollIndicatorModifier_updatesToNewNode() {
        val factory1 = TestScrollIndicatorFactory()
        val factory2 = TestScrollIndicatorFactory()
        var factory by mutableStateOf(factory1)

        rule.setContent { Box(Modifier.scrollIndicator(factory, testState, Orientation.Vertical)) }

        val firstNode =
            rule.runOnIdle {
                val node = factory1.lastCreatedNode
                assertThat(node?.isAttached).isTrue()
                assertNull(factory2.lastCreatedNode)
                factory = factory2
                node
            }

        rule.runOnIdle {
            assertThat(firstNode?.isAttached).isFalse()
            assertNotNull(factory2.lastCreatedNode)
            assertThat(factory2.lastCreatedNode?.isAttached).isTrue()
        }
    }

    @Test
    fun scrollIndicatorModifier_nodeIsReusedForSameInput() {
        val factory = TestScrollIndicatorFactory()
        // State used to force the composable to recompose
        var recomposeTrigger by mutableStateOf(0)

        rule.setContent {
            // Reading the state here ensures this block re-runs when trigger changes
            recomposeTrigger
            Box(Modifier.scrollIndicator(factory, testState, Orientation.Vertical))
        }

        rule.runOnIdle {
            assertThat(factory.createNodeCount).isEqualTo(1)
            assertThat(factory.lastCreatedNode?.isAttached).isTrue()
        }

        // Trigger a recomposition without changing scrollIndicator parameters
        recomposeTrigger++

        rule.runOnIdle {
            assertThat(factory.createNodeCount).isEqualTo(1)
            assertThat(factory.lastCreatedNode?.isAttached).isTrue()
        }
    }

    @Test
    fun scrollIndicatorModifier_nodeRecreatedWhenStateAndOrientationChanges() {
        val factory = TestScrollIndicatorFactory()

        val updatedState =
            object : ScrollIndicatorState {
                override val scrollOffset: Int = 50
                override val contentSize: Int = 200
                override val viewportSize: Int = 20
            }

        var state by mutableStateOf<ScrollIndicatorState>(testState)
        var orientation by mutableStateOf(Orientation.Vertical)

        rule.setContent { Box(Modifier.scrollIndicator(factory, state, orientation)) }

        rule.runOnIdle {
            assertNotNull(factory.lastCreatedNode)
            assertThat(factory.lastCreatedNode?.state).isEqualTo(testState)
            assertThat(factory.lastCreatedNode?.orientation).isEqualTo(Orientation.Vertical)
            assertThat(factory.createNodeCount).isEqualTo(1)

            state = updatedState
        }

        rule.runOnIdle {
            assertNotNull(factory.lastCreatedNode)
            assertThat(factory.lastCreatedNode?.state).isEqualTo(updatedState)
            assertThat(factory.lastCreatedNode?.orientation).isEqualTo(Orientation.Vertical)
            assertThat(factory.createNodeCount).isEqualTo(2)

            orientation = Orientation.Horizontal
        }

        rule.runOnIdle {
            assertNotNull(factory.lastCreatedNode)
            assertThat(factory.lastCreatedNode?.state).isEqualTo(updatedState)
            assertThat(factory.lastCreatedNode?.orientation).isEqualTo(Orientation.Horizontal)
            assertThat(factory.createNodeCount).isEqualTo(3)
        }
    }

    private class TestScrollIndicatorFactory : ScrollIndicatorFactory {
        var createNodeCount = 0
        var lastCreatedNode: TestScrollIndicatorNode? = null

        override fun createNode(
            state: ScrollIndicatorState,
            orientation: Orientation,
        ): DelegatableNode {
            createNodeCount++
            val node = TestScrollIndicatorNode(state, orientation)
            lastCreatedNode = node
            return node
        }

        override fun equals(other: Any?): Boolean = this === other

        override fun hashCode(): Int = -1
    }

    private class TestScrollIndicatorNode(
        val state: ScrollIndicatorState,
        val orientation: Orientation,
    ) : Modifier.Node()
}
