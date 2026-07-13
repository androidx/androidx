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

package androidx.compose.animation

import androidx.compose.animation.core.ExperimentalDeferredTransitionApi
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalDeferredTransitionApi::class)
@RunWith(JUnit4::class)
class SharedMutableTransformStateTest {

    @Test
    fun updateMutationState_fromHandoffToMutating_resetsTransformScope() {
        val state = SharedMutableTransformState()

        // 1) Simulate an initial gesture mutating the transformScope offset
        state.updateMutationState(isMutating = true, isSettled = false)
        state.transformScope.offset = IntOffset(-150, 0)
        assertTrue(
            "Precondition: offset should be marked as mutated",
            state.transformScope.isOffsetMutated,
        )

        // 2) Simulate releasing the gesture into the Handoff phase
        state.updateMutationState(isMutating = false, isSettled = false)
        assertEquals(MutationPhase.Handoff, state.mutationPhase)
        assertTrue(
            "Precondition: offset remains mutated during handoff",
            state.transformScope.isOffsetMutated,
        )

        // 3) Simulate initiating a second gesture while the first is still in the Handoff phase.
        // This should reset the transformScope so leftover mutations are not applied to the new
        // gesture.
        state.updateMutationState(isMutating = true, isSettled = false)

        // Assert: transformScope is cleanly reset for the incoming gesture
        assertFalse(
            "transformScope.isOffsetMutated must be reset to false when starting new gesture",
            state.transformScope.isOffsetMutated,
        )
        assertEquals(
            "transformScope.offset must be reset to IntOffset.Zero when starting new gesture",
            IntOffset.Zero,
            state.transformScope.offset,
        )
    }

    @Test
    fun updateMutationState_fromIdleToMutating_resetsTransformScope() {
        val state = SharedMutableTransformState()

        // 1) Simulate a previous gesture that mutated offset before settling to Idle
        state.transformScope.offset = IntOffset(100, 200)
        assertTrue(state.transformScope.isOffsetMutated)

        // 2) Start a new gesture from Idle
        assertEquals(MutationPhase.Idle, state.mutationPhase)
        state.updateMutationState(isMutating = true, isSettled = false)

        assertFalse(state.transformScope.isOffsetMutated)
        assertEquals(IntOffset.Zero, state.transformScope.offset)
    }

    @Test
    fun startCatchUp_transitionsPhaseToMutatingWhenComplete() {
        val state = SharedMutableTransformState()

        // 1) Simulate entering a catch-up phase when interrupting an active transition
        state.transformScope.offset = IntOffset(100, 0)
        state.updateMutationState(isMutating = true, isSettled = false)
        assertEquals(MutationPhase.MutatingPendingCatchUp, state.mutationPhase)

        // 2) Run startCatchUp to completion
        runBlocking { state.startCatchUp() }

        // Assert: once startCatchUp coroutines complete, mutationPhase must be Mutating
        assertEquals(
            "mutationPhase must transition from MutatingCatchingUp to Mutating upon catch-up completion",
            MutationPhase.Mutating,
            state.mutationPhase,
        )
    }
}
