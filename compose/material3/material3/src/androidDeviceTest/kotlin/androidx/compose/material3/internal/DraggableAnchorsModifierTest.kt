/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.material3.internal

import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ComposeMaterial3Flags
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
class DraggableAnchorsModifierTest {

    @get:Rule val rule = createComposeRule()

    private val originalFlagValue =
        ComposeMaterial3Flags.isAnchoredDraggableComponentsInvalidationFixEnabled

    @Before
    fun setUp() {
        // Ensure the flag is enabled for the test
        ComposeMaterial3Flags.isAnchoredDraggableComponentsInvalidationFixEnabled = true
    }

    @After
    fun tearDown() {
        ComposeMaterial3Flags.isAnchoredDraggableComponentsInvalidationFixEnabled =
            originalFlagValue
    }

    @Test
    fun draggableAnchors_stateInstanceChanged_recalculatesAnchors() {
        // 1. Create two distinct state instances
        val state1 = AnchoredDraggableState(initialValue = TestValue.A)
        val state2 = AnchoredDraggableState(initialValue = TestValue.A)

        var currentState by mutableStateOf(state1)

        rule.setContent {
            Box(
                Modifier.size(100.dp).draggableAnchors(
                    state = currentState,
                    orientation = Orientation.Vertical,
                ) { size, _ ->
                    val newAnchors = DraggableAnchors {
                        TestValue.A at 0f
                        TestValue.B at size.height.toFloat()
                    }
                    newAnchors to TestValue.A
                }
            )
        }

        rule.waitForIdle()
        // 2. Verify State 1 has initialized anchors (measure pass happened)
        assertThat(state1.anchors.size).isGreaterThan(0)
        assertThat(state1.anchors.hasPositionFor(TestValue.B)).isTrue()

        // 3. Swap the state instance
        // Without the fix, the modifier updates the reference, but does NOT invalidate measurement.
        // Therefore, the measure block is skipped, and state2 never receives anchors.
        currentState = state2

        // 4. Verify State 2 has initialized anchors
        rule.waitForIdle()
        // This assertion should fail with the flag off
        assertThat(state2.anchors.size).isGreaterThan(0)
        assertThat(state2.anchors.hasPositionFor(TestValue.B)).isTrue()
    }

    @Test
    fun draggableAnchors_orphanTarget_recoversAndPreventsException() {
        val state = AnchoredDraggableState(initialValue = TestValue.C)

        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.size(100.dp).draggableAnchors(state, Orientation.Vertical) { _, _ ->
                        val anchors = DraggableAnchors {
                            TestValue.A at 0f
                            TestValue.B at 100f
                        }
                        anchors to TestValue.C
                    }
                )
            }
        }

        rule.waitForIdle()
        assertThat(state.offset).isNotNaN()
        assertThat(state.anchors.hasPositionFor(state.currentValue)).isTrue()
    }

    @Test
    fun draggableAnchors_safeTargeting_withLayoutChange_reconcilesCorrectly() {
        val state = AnchoredDraggableState(initialValue = TestValue.C)
        var supportsStateC by mutableStateOf(false)

        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.size(100.dp).draggableAnchors(state, Orientation.Vertical) { _, _ ->
                        val newAnchors = DraggableAnchors {
                            TestValue.A at 0f
                            TestValue.B at 100f
                            if (supportsStateC) TestValue.C at 200f
                        }

                        // Simple target selection
                        val newTarget =
                            when (state.targetValue) {
                                TestValue.C -> if (supportsStateC) TestValue.C else TestValue.B
                                else -> state.targetValue
                            }

                        newAnchors to newTarget
                    }
                )
            }
        }

        // Initial pass: C is unsupported, should settle at B
        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(TestValue.B)
        assertThat(state.offset).isEqualTo(100f)

        // Trigger layout change
        supportsStateC = true
        rule.waitForIdle()

        // Verify we can now reach C
        rule.runOnIdle { kotlinx.coroutines.runBlocking { state.snapTo(TestValue.C) } }
        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(TestValue.C)
        assertThat(state.offset).isEqualTo(200f)
    }

    enum class TestValue {
        A,
        B,
        C,
    }
}
