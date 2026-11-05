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

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class SharedMutableTransformStateTest {

    @After
    fun tearDown() {
        testTimeSource = null
    }

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

    @Test
    fun mutableContentTransform_fromTwoMutableTransforms_preservesInstances() {
        val initialTransform = MutableTransform(veilMatchParentSize = false) { alpha = 0.5f }
        val targetTransform = MutableTransform(veilMatchParentSize = true) { scale = 0.8f }

        val contentTransform = MutableContentTransform(initialTransform, targetTransform)

        assertEquals(initialTransform, contentTransform.initialTransform)
        assertEquals(targetTransform, contentTransform.targetTransform)
    }

    @Test
    fun combinedScale_constantValueWithDuplicateTimestamps_handoffVelocityIsZero() {
        var now = 0L
        testTimeSource = { now }
        val state = SharedMutableTransformState()
        state.updateMutationState(isMutating = true, isSettled = true)
        state.transformScope.scale = 0.5f

        // Record 3 samples across 2 timestamps ([0, 30, 30]), matching the duplicate-timestamp
        // layout/layer pass pattern from b/574451802.
        now = 0L
        state.combinedScale(transitionValue = 1f)
        now = 30L
        state.combinedScale(transitionValue = 1f)
        state.combinedScale(transitionValue = 1f)

        state.updateMutationState(isMutating = false, isSettled = true)

        assertEquals(0f, state.scaleHandoffVelocity?.value ?: Float.NaN, 0f)
    }

    @Test
    fun combinedSlide_constantValueWithDuplicateTimestamps_handoffVelocityIsZero() {
        var now = 0L
        testTimeSource = { now }
        val state = SharedMutableTransformState()
        state.updateMutationState(isMutating = true, isSettled = true)
        state.transformScope.offset = IntOffset(100, -50)

        now = 0L
        state.combinedSlide(IntOffset.Zero)
        state.combinedSlide(IntOffset.Zero)
        now = 7L
        state.combinedSlide(IntOffset.Zero)

        state.updateMutationState(isMutating = false, isSettled = true)

        assertEquals(0f, state.slideHandoffVelocity?.v1 ?: Float.NaN, 0f)
        assertEquals(0f, state.slideHandoffVelocity?.v2 ?: Float.NaN, 0f)
    }

    @Test
    fun resolveSharedElementProgress_constantValueWithDuplicateTimestamps_handoffVelocityIsZero() {
        var now = 0L
        testTimeSource = { now }
        val state = SharedMutableTransformState()
        state.mutableData = MutableTransform().apply { sharedElementProgress(progress = { 0.5f }) }
        state.updateMutationState(isMutating = true, isSettled = true)

        now = 0L
        state.resolveSharedElementProgress()
        now = 30L
        state.resolveSharedElementProgress()
        state.resolveSharedElementProgress()

        state.updateMutationState(isMutating = false, isSettled = true)

        assertEquals(0f, state.sharedElementProgressHandoffVelocity, 0f)
    }

    @Test
    fun combinedProperties_whenMutatingWithoutExplicitMutations_overrideToDefaultRestingValues() {
        val state = SharedMutableTransformState()
        state.updateMutationState(isMutating = true, isSettled = true)

        assertEquals(1f, state.combinedAlpha(transitionValue = 0f), 0f)
        assertEquals(1f, state.combinedScale(transitionValue = 0.5f), 0f)
        assertEquals(
            IntOffset.Zero,
            state.combinedSlide(transitionValue = IntOffset(200, 100)),
        )
        assertEquals(
            TransformOrigin.Center,
            state.combinedTransformOrigin(transitionValue = TransformOrigin(0f, 0f)),
        )
        assertEquals(Color.Red.copy(alpha = 0f), state.combinedVeil(transitionValue = Color.Red))

        state.updateMutationState(isMutating = false, isSettled = false)
        assertEquals(1f, state.alphaHandoffValue)
        assertEquals(1f, state.scaleHandoffValue)
        assertEquals(IntOffset.Zero, state.slideHandoffValue)
        assertEquals(TransformOrigin.Center, state.transformOriginHandoffValue)
        assertEquals(Color.Red.copy(alpha = 0f), state.veilHandoffValue)
    }

    @Test
    fun combinedProperties_whenNotMutating_preserveTransitionValues() {
        val state = SharedMutableTransformState()

        assertEquals(0f, state.combinedAlpha(transitionValue = 0f), 0f)
        assertEquals(0.5f, state.combinedScale(transitionValue = 0.5f), 0f)
        assertEquals(
            IntOffset(200, 100),
            state.combinedSlide(transitionValue = IntOffset(200, 100)),
        )
        assertEquals(
            TransformOrigin(0f, 0f),
            state.combinedTransformOrigin(transitionValue = TransformOrigin(0f, 0f)),
        )
        assertEquals(Color.Red, state.combinedVeil(transitionValue = Color.Red))
    }

    @Test
    fun combinedLayerProperties_doNotObserveTransformScopeMutations() {
        val state = SharedMutableTransformState()
        state.updateMutationState(isMutating = true, isSettled = true)
        state.transformScope.alpha = 0.5f
        state.transformScope.scale = 0.8f
        state.transformScope.transformOrigin = TransformOrigin(0f, 0f)
        Snapshot.sendApplyNotifications()

        val readObjects = mutableSetOf<Any>()
        Snapshot.observe(readObserver = { readObjects.add(it) }) {
            assertEquals(0.5f, state.combinedAlpha(transitionValue = 1f), 0f)
            assertEquals(0.8f, state.combinedScale(transitionValue = 1f), 0f)
            assertEquals(
                TransformOrigin(0f, 0f),
                state.combinedTransformOrigin(transitionValue = TransformOrigin.Center),
            )
        }

        val modifiedObjects = mutableSetOf<Any>()
        val handle = Snapshot.registerApplyObserver { changed, _ ->
            modifiedObjects.addAll(changed)
        }
        try {
            // reset() first, so the is*Mutated flags flip false -> true below and are part of the
            // modified set as well.
            state.transformScope.reset()
            Snapshot.sendApplyNotifications()
            state.transformScope.alpha = 0.3f
            state.transformScope.scale = 0.6f
            state.transformScope.transformOrigin = TransformOrigin(1f, 1f)
            Snapshot.sendApplyNotifications()
        } finally {
            handle.dispose()
        }

        // transformScope is evaluated during placement right before updateLayerBlock(layerBlock),
        // so layerBlock must not observe it; otherwise every gesture frame would schedule a
        // redundant second layer update on snapshot apply.
        assertTrue(modifiedObjects.isNotEmpty())
        assertEquals(emptySet<Any>(), readObjects intersect modifiedObjects)
    }
}
