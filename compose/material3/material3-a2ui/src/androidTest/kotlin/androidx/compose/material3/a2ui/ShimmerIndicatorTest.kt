/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.material3.a2ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ShimmerIndicatorTest {

    @Test
    fun shimmerIndicatorNode_standalone_initializesLocalStateAndAnimates() = runComposeUiTest {
        mainClock.autoAdvance = false
        var attachNode by mutableStateOf(true)
        var capturedNode: ShimmerIndicatorNode? = null
        val trackingElement =
            object : ModifierNodeElement<ShimmerIndicatorNode>() {
                override fun create(): ShimmerIndicatorNode =
                    ShimmerIndicatorNode(
                            isLight = true,
                            shape = RectangleShape,
                            index = 0,
                            groupState = null,
                        )
                        .also { capturedNode = it }

                override fun update(node: ShimmerIndicatorNode) {
                    node.update(
                        isLight = true,
                        shape = RectangleShape,
                        index = 0,
                        groupState = null,
                    )
                }

                override fun hashCode(): Int = 0

                override fun equals(other: Any?): Boolean = other === this
            }

        setContent {
            if (attachNode) {
                Box(modifier = Modifier.size(60.dp).then(trackingElement).testTag("shimmer"))
            }
        }

        onNodeWithTag("shimmer").assertIsDisplayed()
        var attachedState: ShimmerIndicatorGroupState? = null
        runOnIdle {
            val activeNode = checkNotNull(capturedNode)
            assertThat(activeNode.groupState).isNull()
            val nodeState = checkNotNull(activeNode.state)
            attachedState = nodeState
            assertThat(nodeState.activeIndicatorCount).isEqualTo(1)
        }

        mainClock.advanceTimeBy(500L)
        runOnIdle {
            val nodeState = checkNotNull(attachedState)
            assertThat(nodeState.sweepProgress).isGreaterThan(0f)
            assertThat(nodeState.highlightAlpha).isGreaterThan(0f)
            assertThat(nodeState.breathingPhase).isGreaterThan(0f)
        }

        attachNode = false
        mainClock.advanceTimeByFrame()
        runOnIdle {
            val activeNode = checkNotNull(capturedNode)
            assertThat(activeNode.state).isNull()
            val nodeState = checkNotNull(attachedState)
            assertThat(nodeState.activeIndicatorCount).isEqualTo(0)
            assertThat(nodeState.sweepProgress).isEqualTo(0f)
            assertThat(nodeState.highlightAlpha).isEqualTo(0f)
            assertThat(nodeState.breathingPhase).isEqualTo(0f)
        }
    }

    @Test
    fun shimmerIndicatorNode_update_switchesGroupStatesAndUpdatesProperties() = runComposeUiTest {
        mainClock.autoAdvance = false
        lateinit var groupState1: ShimmerIndicatorGroupState
        lateinit var groupState2: ShimmerIndicatorGroupState

        var isLight by mutableStateOf(true)
        var shape by mutableStateOf<Shape>(RectangleShape)
        var index by mutableIntStateOf(0)
        var activeGroupSelector by mutableIntStateOf(1)

        setContent {
            val scope = rememberCoroutineScope()
            groupState1 = remember(scope) { ShimmerIndicatorGroupState(scope) }
            groupState2 = remember(scope) { ShimmerIndicatorGroupState(scope) }
            val currentGroupState =
                when (activeGroupSelector) {
                    1 -> groupState1
                    2 -> groupState2
                    else -> null
                }

            Box(
                modifier =
                    Modifier.size(80.dp)
                        .shimmerIndicator(
                            isLight = isLight,
                            shape = shape,
                            index = index,
                            groupState = currentGroupState,
                        )
                        .testTag("shimmer")
            )
        }

        runOnIdle {
            assertThat(groupState1.activeIndicatorCount).isEqualTo(1)
            assertThat(groupState2.activeIndicatorCount).isEqualTo(0)
        }

        // Switch from groupState1 to groupState2 and update properties
        isLight = false
        shape = CircleShape
        index = 2
        activeGroupSelector = 2
        mainClock.advanceTimeBy(400L)

        runOnIdle {
            assertThat(groupState1.activeIndicatorCount).isEqualTo(0)
            assertThat(groupState2.activeIndicatorCount).isEqualTo(1)
            assertThat(groupState2.sweepProgress).isGreaterThan(0f)
        }

        // Switch from groupState2 to standalone (null)
        activeGroupSelector = 0
        mainClock.advanceTimeBy(400L)

        runOnIdle {
            assertThat(groupState1.activeIndicatorCount).isEqualTo(0)
            assertThat(groupState2.activeIndicatorCount).isEqualTo(0)
        }
        onNodeWithTag("shimmer").assertIsDisplayed()
    }

    @Test
    fun shimmerIndicatorNode_zeroSize_doesNotCrash() = runComposeUiTest {
        mainClock.autoAdvance = false

        setContent {
            Box(
                modifier =
                    Modifier.size(0.dp)
                        .shimmerIndicator(isLight = true, shape = RoundedCornerShape(8.dp))
            )
        }

        mainClock.advanceTimeBy(500L)
        waitForIdle()
    }

    @Test
    fun shimmerIndicatorElement_createAndUpdate_updatesNodeFields() {
        val scope = CoroutineScope(EmptyCoroutineContext)
        val groupState1 = ShimmerIndicatorGroupState(scope)
        val groupState2 = ShimmerIndicatorGroupState(scope)
        val initialElement =
            ShimmerIndicatorElement(
                isLight = true,
                shape = RectangleShape,
                index = 0,
                groupState = groupState1,
            )
        val node = initialElement.create()

        assertThat(node.isLight).isTrue()
        assertThat(node.shape).isEqualTo(RectangleShape)
        assertThat(node.index).isEqualTo(0)
        assertThat(node.groupState).isSameInstanceAs(groupState1)
        assertThat(node.state).isNull()

        val updatedElement =
            ShimmerIndicatorElement(
                isLight = false,
                shape = CircleShape,
                index = 4,
                groupState = groupState2,
            )
        updatedElement.update(node)

        assertThat(node.isLight).isFalse()
        assertThat(node.shape).isEqualTo(CircleShape)
        assertThat(node.index).isEqualTo(4)
        assertThat(node.groupState).isSameInstanceAs(groupState2)
        assertThat(node.state).isNull()
    }

    @Test
    fun shimmerIndicatorElement_inspectableProperties() {
        val groupState = ShimmerIndicatorGroupState(CoroutineScope(EmptyCoroutineContext))
        val modifier =
            Modifier.shimmerIndicator(
                isLight = false,
                shape = CircleShape,
                index = 3,
                groupState = groupState,
            ) as InspectableValue

        assertThat(modifier.nameFallback).isEqualTo("shimmerIndicator")
        assertThat(modifier.inspectableElements.asIterable())
            .containsExactly(
                ValueElement("isLight", false),
                ValueElement("shape", CircleShape),
                ValueElement("index", 3),
                ValueElement("groupState", groupState),
            )
    }

    @Test
    fun calculateBreathingAlpha_returnsExpectedValuesAndStaggersByIndex() {
        val phaseZeroAlpha = calculateBreathingAlpha(breathingPhase = 0f, index = 0)
        val phaseHalfAlpha = calculateBreathingAlpha(breathingPhase = 0.5f, index = 0)
        val staggeredIndexOneAlpha = calculateBreathingAlpha(breathingPhase = 0f, index = 1)
        val staggeredIndexTwoAlpha = calculateBreathingAlpha(breathingPhase = 0f, index = 2)

        assertThat(phaseZeroAlpha).isWithin(0.001f).of(ShimmerBreathingMaxAlpha)
        assertThat(phaseHalfAlpha).isWithin(0.001f).of(ShimmerBreathingMinAlpha)
        assertThat(staggeredIndexOneAlpha).isLessThan(phaseZeroAlpha)
        assertThat(staggeredIndexOneAlpha).isGreaterThan(ShimmerBreathingMinAlpha)
        assertThat(staggeredIndexTwoAlpha).isLessThan(staggeredIndexOneAlpha)
        assertThat(staggeredIndexTwoAlpha).isGreaterThan(ShimmerBreathingMinAlpha)
    }
}
