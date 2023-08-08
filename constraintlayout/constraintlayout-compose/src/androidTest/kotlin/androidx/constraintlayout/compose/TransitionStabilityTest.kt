/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.constraintlayout.compose

import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.Test

@OptIn(ExperimentalMotionApi::class)
class TransitionStabilityTest {
    @Test
    fun testTransitionDslEquality() {
        val transition = Transition("start", "end") {
            val a = createRefFor("a")
            keyAttributes(a) {
                frame(20) {
                    alpha = 0.2f
                }
                frame(80) {
                    alpha = 0.8f
                }
            }
        }

        // Transitions should be equivalent
        assertEquals(
            expected = transition,
            actual = Transition("start", "end") {
                val a = createRefFor("a")
                keyAttributes(a) {
                    frame(20) {
                        alpha = 0.2f
                    }
                    frame(80) {
                        alpha = 0.8f
                    }
                }
            }
        )

        // Given transition is different
        assertNotEquals(
            illegal = transition,
            actual = Transition("start", "end") {
                val a = createRefFor("a")
                keyAttributes(a) {
                    frame(20) {
                        alpha = 0.2f
                    }
                    frame(80) {
                        alpha = 0.85f
                    }
                }
            }
        )
    }

    @Test
    fun testTransitionJsonEquality() {
        val transition = Transition(
            """
            {
              from: 'start',
              to: 'end',
              KeyFrames: {
                KeyAttributes: [
                  {
                    target: ['a'],
                    frames: [20, 80],
                    alpha: [0.2, 0.8],
                  }
                ],
              }
            }
        """.trimIndent()
        )

        assertEquals(
            transition,
            Transition(
                """
            {
              from: 'start',
              to: 'end',
              KeyFrames: {
                KeyAttributes: [
                  {
                    target: ['a'],
                    frames: [20, 80],
                    alpha: [0.2, 0.8],
                  }
                ],
              }
            }
            """.trimIndent()
            )
        )
    }
}
