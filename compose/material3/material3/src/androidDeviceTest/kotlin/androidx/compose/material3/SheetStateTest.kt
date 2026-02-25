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

package androidx.compose.material3

import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.SheetValue.PartiallyExpanded
import androidx.compose.runtime.MonotonicFrameClock
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(JUnit4::class)
class SheetStateTest {

    private fun createSheetState(
        skipPartiallyExpanded: Boolean,
        skipHiddenState: Boolean,
        initialValue: SheetValue,
    ): SheetState {
        return SheetState(
            skipPartiallyExpanded = skipPartiallyExpanded,
            positionalThreshold = { 56f },
            velocityThreshold = { 125f },
            initialValue = initialValue,
            skipHiddenState = skipHiddenState,
        )
    }

    private class PseudoFrameClock : MonotonicFrameClock {
        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            // We use the system time, but in a real test environment with StandardTestDispatcher,
            // this simply allows the coroutine to proceed.
            return onFrame(System.nanoTime())
        }
    }

    @Test
    fun sheetState_constructor_initialValueContracts() {
        // Conflicting anchor flags and initial values throw IllegalArgumentException
        assertThrows(IllegalArgumentException::class.java) {
            createSheetState(
                skipPartiallyExpanded = true,
                skipHiddenState = false,
                initialValue = PartiallyExpanded,
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            createSheetState(
                skipPartiallyExpanded = false,
                skipHiddenState = true,
                initialValue = Hidden,
            )
        }
    }

    @Test
    fun sheetState_currentValue_mapsToSettledValue() = runTest {
        val state =
            createSheetState(
                skipPartiallyExpanded = false,
                skipHiddenState = false,
                initialValue = Hidden,
            )

        val newAnchors = DraggableAnchors {
            Hidden at 1000f
            Expanded at 0f
        }
        state.anchoredDraggableState.updateAnchors(newAnchors, Hidden)

        assertThat(state.currentValue).isEqualTo(Hidden)

        state.snapTo(Expanded)
        assertThat(state.currentValue).isEqualTo(Expanded)
        assertThat(state.requireOffset()).isEqualTo(0f)
    }

    @Test
    fun sheetState_targetValue_mapsToCurrentValue_whenSettled() = runTest {
        val state =
            createSheetState(
                skipPartiallyExpanded = false,
                skipHiddenState = false,
                initialValue = Expanded,
            )
        val newAnchors = DraggableAnchors {
            Hidden at 1000f
            Expanded at 0f
        }

        state.anchoredDraggableState.updateAnchors(newAnchors, Expanded)
        assertThat(state.currentValue).isEqualTo(Expanded)
        assertThat(state.targetValue).isEqualTo(Expanded)
    }

    @Test
    fun sheetState_targetValue_fixLogic_handlesNonExistentAnchor() = runTest {
        val state =
            createSheetState(
                skipPartiallyExpanded = false,
                skipHiddenState = false,
                initialValue = PartiallyExpanded,
            )

        // Setup initial anchors that include PartiallyExpanded
        val initialAnchors = DraggableAnchors {
            PartiallyExpanded at 500f
            Expanded at 0f
        }
        state.anchoredDraggableState.updateAnchors(initialAnchors, PartiallyExpanded)
        assertThat(state.currentValue).isEqualTo(PartiallyExpanded)
        assertThat(state.targetValue).isEqualTo(PartiallyExpanded)

        // Now, update anchors to REMOVE PartiallyExpanded (e.g. layout change)
        val newAnchors = DraggableAnchors { Expanded at 0f }
        // Update the state's anchors without changing the offset/value immediately
        state.anchoredDraggableState.updateAnchors(
            newAnchors,
            state.anchoredDraggableState.targetValue,
        )

        // The custom logic in targetValue should see that currentValue (PartiallyExpanded)
        // is no longer in the anchor set (offset is NaN).
        // It should return currentValue (PartiallyExpanded) rather than jumping to Expanded.
        assertThat(state.targetValue).isEqualTo(PartiallyExpanded)
        assertThat(state.currentValue).isEqualTo(PartiallyExpanded)
    }

    @Test
    fun sheetState_targetValue_fixLogic_handlesExactOffsetMatch() = runTest {
        val state =
            createSheetState(
                skipPartiallyExpanded = false,
                skipHiddenState = false,
                initialValue = PartiallyExpanded,
            )
        val anchors = DraggableAnchors {
            PartiallyExpanded at 500f
            Expanded at 0f
        }
        state.anchoredDraggableState.updateAnchors(anchors, PartiallyExpanded)

        // Verify state
        assertThat(state.requireOffset()).isEqualTo(500f)
        assertThat(state.currentValue).isEqualTo(PartiallyExpanded)

        // targetValue should be stable at PartiallyExpanded
        assertThat(state.targetValue).isEqualTo(PartiallyExpanded)
    }

    @Test
    fun sheetState_zeroPeekHeight_partiallyExpandedMapsToHiddenOffset() =
        runTest(PseudoFrameClock()) {
            val state =
                createSheetState(
                    skipPartiallyExpanded = false,
                    skipHiddenState = false,
                    initialValue = PartiallyExpanded,
                )
            val screenHeight = 1000f

            val anchors = DraggableAnchors {
                Hidden at screenHeight
                PartiallyExpanded at screenHeight // Same offset as Hidden
                Expanded at 0f
            }
            state.anchoredDraggableState.updateAnchors(anchors, PartiallyExpanded)
            assertThat(state.requireOffset()).isEqualTo(screenHeight)
            assertThat(state.currentValue).isEqualTo(PartiallyExpanded)

            // Verify isVisible is true (because state != Hidden), even though it looks hidden.
            assertThat(state.isVisible).isTrue()

            // Even though Hidden and PartiallyExpanded share the same offset (1000f),
            // targetValue should prefer the currentValue if we are exactly at that offset.
            assertThat(state.targetValue).isEqualTo(PartiallyExpanded)

            // Component can still be hidden
            state.hide()
            assertThat(state.isVisible).isFalse()
            assertThat(state.targetValue).isEqualTo(Hidden)
            assertThat(state.currentValue).isEqualTo(Hidden)
        }

    @Test
    fun sheetState_zeroPeekHeight_partialExpandMethod() =
        runTest(PseudoFrameClock()) {
            val state =
                createSheetState(
                    skipPartiallyExpanded = false,
                    skipHiddenState = false,
                    initialValue = Expanded,
                )
            val screenHeight = 1000f

            val anchors = DraggableAnchors {
                Hidden at screenHeight
                PartiallyExpanded at screenHeight // Same offset as Hidden
                Expanded at 0f
            }
            state.anchoredDraggableState.updateAnchors(anchors, Expanded)
            assertThat(state.currentValue).isEqualTo(Expanded)
            state.partialExpand()

            // Should settle at PartiallyExpanded
            assertThat(state.currentValue).isEqualTo(PartiallyExpanded)
            assertThat(state.requireOffset()).isEqualTo(screenHeight)

            // And targetValue should be correct
            assertThat(state.targetValue).isEqualTo(PartiallyExpanded)
        }

    @Test
    fun sheetState_expand_hide_show_api() =
        runTest(PseudoFrameClock()) {
            val state =
                createSheetState(
                    skipPartiallyExpanded = false,
                    skipHiddenState = false,
                    initialValue = Hidden,
                )
            val anchors = DraggableAnchors {
                Hidden at 1000f
                PartiallyExpanded at 500f
                Expanded at 0f
            }
            state.anchoredDraggableState.updateAnchors(anchors, Hidden)

            // Test Show (defaults to PartiallyExpanded if available)
            state.show()
            assertThat(state.currentValue).isEqualTo(PartiallyExpanded)

            // Test Expand
            state.expand()
            assertThat(state.currentValue).isEqualTo(Expanded)

            // Test Hide
            state.hide()
            assertThat(state.currentValue).isEqualTo(Hidden)
            assertThat(state.isVisible).isFalse()
        }
}
