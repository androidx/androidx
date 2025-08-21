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

package androidx.compose.runtime

import androidx.compose.runtime.mock.compositionTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

@OptIn(InternalComposeApi::class)
class RecomposerFrameEndSchedulingTests {

    @Test
    fun scheduleFrameEnd_Callback_invokesAfterMovableContentSettles() = compositionTest {
        var includeMovableContent by mutableStateOf(true)
        var didSettleMovableContentRemoval = false
        var didAwaitNextFrameEndResume = false

        val movableContent = movableContentOf {
            DisposableEffect(Unit) { onDispose { didSettleMovableContentRemoval = true } }
        }

        compose {
            if (includeMovableContent) {
                movableContent()
            } else {
                val recomposer = currentCompositionContext
                SideEffect {
                    recomposer.scheduleFrameEndCallback {
                        if (!didSettleMovableContentRemoval) {
                            fail("awaitNextFrameEnd() resumed before MovableContent settled.")
                        }
                        didAwaitNextFrameEndResume = true
                    }
                }
            }
        }

        includeMovableContent = false
        assertEquals(1, advanceCount(), "Expected composition to advance exactly once")
        assertFalse(hasPendingWork(), "Recomposer should be idle")
        if (!didAwaitNextFrameEndResume) {
            fail("awaitNextFrameEnd() didn't resume")
        }
    }

    @Test
    fun scheduleFrameCallback() = compositionTest {
        var includeMovableContent by mutableStateOf(true)
        var didSettleMovableContentRemoval = false

        val movableContent = movableContentOf {
            DisposableEffect(Unit) { onDispose { didSettleMovableContentRemoval = true } }
        }

        compose {
            if (includeMovableContent) {
                movableContent()
            }
        }

        includeMovableContent = false
        var didResume = false
        val compositionContext = (composition as CompositionImpl).parent
        compositionContext.scheduleFrameEndCallback {
            if (!didSettleMovableContentRemoval) {
                fail("awaitNextFrameEnd() resumed before MovableContent settled.")
            }
            didResume = true
        }

        assertFalse(
            didResume,
            "awaitNextFrameEnd() should not resume before finishing the next frame",
        )
        assertEquals(1, advanceCount(), "Expected composition to advance exactly once")
        assertFalse(hasPendingWork(), "Recomposer should be idle")
        assertTrue(didResume, "awaitNextFrameEnd() should have resumed after advancing")
    }

    @Test
    fun scheduleFrameEnd_whileIdle_requestsNewFrameCallback() = compositionTest {
        compose {}
        assertFalse(hasPendingWork(), "Recomposer should be idle")

        var didResume = false
        val compositionContext = (composition as CompositionImpl).parent
        compositionContext.scheduleFrameEndCallback { didResume = true }

        assertTrue(
            hasPendingWork(),
            "Recomposer should have pending work after awaitNextFrameEnd() suspends",
        )
        assertEquals(
            0,
            advanceCount(),
            "awaitNextFrameEnd() with no invalidations should do no work",
        )
        assertTrue(didResume, "awaitNextFrameEnd() should have resumed after advancing")
    }

    @Test
    fun scheduleFrameEnd_whileIdle_cancelFirst_requestsNewFrameCallback() = compositionTest {
        compose {}

        assertFalse(hasPendingWork(), "Recomposer should be idle")

        val resumed = mutableListOf<String>()
        val compositionContext = (composition as CompositionImpl).parent

        val job1 = compositionContext.scheduleFrameEndCallback { resumed += "Job 1" }

        assertTrue(
            hasPendingWork(),
            "Recomposer should have pending work after awaitNextFrameEnd() suspends",
        )

        val job2 = compositionContext.scheduleFrameEndCallback { resumed += "Job 2" }

        job1.cancel()

        assertTrue(
            hasPendingWork(),
            "Recomposer should have pending work after awaitNextFrameEnd() suspends",
        )
        assertEquals(
            0,
            advanceCount(),
            "awaitNextFrameEnd() with no invalidations should do no work",
        )
        assertEquals(listOf("Job 2"), resumed, "Unexpected jobs resumed")
    }
}
