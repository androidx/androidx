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

package androidx.animation

import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TransitionAnimationTest {
    @Test
    fun testDefaultTransition() {
        val clock = ManualAnimationClock(0)
        val anim = TransitionAnimation(def1, clock)
        anim.toState(AnimState.B)
        val physicsAnim = Physics<Float>()
        var playTime = 0L
        do {
            // Increment the time stamp until the animation finishes
            clock.clockTimeMillis = playTime
            assertEquals(anim[prop1],
                physicsAnim.getValue(playTime, 0f, 1f, 0f, ::lerp),
                epsilon)

            assertEquals(anim[prop2],
                physicsAnim.getValue(playTime, 100f, -100f, 0f, ::lerp),
                epsilon)
            playTime += 20L
        } while (anim.isRunning)
    }
}

private enum class AnimState {
    A, B, C
}

private val prop1 = FloatPropKey()
private val prop2 = FloatPropKey()

private val def1 = transitionDefinition {
    state(AnimState.A) {
        this[prop1] = 0f
        this[prop2] = 100f
    }

    state(AnimState.B) {
        this[prop1] = 1f
        this[prop2] = -100f
    }
}