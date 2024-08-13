/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.brush

import androidx.ink.brush.EasingFunction.Predefined
import androidx.ink.geometry.ImmutableVec
import com.google.common.truth.Truth.assertThat
import kotlin.IllegalArgumentException
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalInkCustomBrushApi::class)
@RunWith(JUnit4::class)
class EasingFunctionTest {

    @Test
    fun predefinedConstants_areDistinct() {
        val set =
            setOf<EasingFunction.Predefined>(
                EasingFunction.Predefined.LINEAR,
                EasingFunction.Predefined.EASE,
                EasingFunction.Predefined.EASE_IN,
                EasingFunction.Predefined.EASE_OUT,
                EasingFunction.Predefined.EASE_IN_OUT,
                EasingFunction.Predefined.STEP_START,
                EasingFunction.Predefined.STEP_END,
            )
        assertThat(set.size).isEqualTo(7)
    }

    @Test
    fun predefinedToString_returnsCorrectString() {
        assertThat(Predefined.LINEAR.toString()).isEqualTo("EasingFunction.Predefined.LINEAR")
        assertThat(Predefined.EASE.toString()).isEqualTo("EasingFunction.Predefined.EASE")
        assertThat(EasingFunction.Predefined.EASE_IN.toString())
            .isEqualTo("EasingFunction.Predefined.EASE_IN")
        assertThat(EasingFunction.Predefined.EASE_OUT.toString())
            .isEqualTo("EasingFunction.Predefined.EASE_OUT")
        assertThat(EasingFunction.Predefined.EASE_IN_OUT.toString())
            .isEqualTo("EasingFunction.Predefined.EASE_IN_OUT")
        assertThat(EasingFunction.Predefined.STEP_START.toString())
            .isEqualTo("EasingFunction.Predefined.STEP_START")
        assertThat(EasingFunction.Predefined.STEP_END.toString())
            .isEqualTo("EasingFunction.Predefined.STEP_END")
    }

    @Test
    fun predefinedHashCode_withIdenticalValues_matches() {
        assertThat(EasingFunction.Predefined.LINEAR.hashCode())
            .isEqualTo(EasingFunction.Predefined.LINEAR.hashCode())

        assertThat(EasingFunction.Predefined.LINEAR.hashCode())
            .isNotEqualTo(EasingFunction.Predefined.STEP_END.hashCode())
    }

    @Test
    fun predefinedEquals_checksEqualityOfValues() {
        assertThat(EasingFunction.Predefined.LINEAR).isEqualTo(EasingFunction.Predefined.LINEAR)
        assertThat(EasingFunction.Predefined.LINEAR).isNotEqualTo(EasingFunction.Predefined.EASE)
        assertThat(EasingFunction.Predefined.LINEAR).isNotEqualTo(null)
    }

    @Test
    fun cubicBezierConstructor_requiresValuesInRange() {
        // arg x1 outside range [0,1]
        assertFailsWith<IllegalArgumentException> {
            EasingFunction.CubicBezier(x1 = 1.1F, 1F, 3F, 4F)
        }
        assertFailsWith<IllegalArgumentException> {
            EasingFunction.CubicBezier(x1 = -0.2F, 3F, 1F, 4F)
        }
        // arg x2 outside range [0,1]
        assertFailsWith<IllegalArgumentException> {
            EasingFunction.CubicBezier(1F, 3F, x2 = 2F, 4F)
        }
        assertFailsWith<IllegalArgumentException> {
            EasingFunction.CubicBezier(1F, 3F, x2 = -0.5F, 4F)
        }
    }

    @Test
    fun cubicBezierConstructor_requiresFiniteValues() {
        assertFailsWith<IllegalArgumentException> {
            EasingFunction.CubicBezier(x1 = Float.POSITIVE_INFINITY, 1F, 1F, 1F)
        }
        assertFailsWith<IllegalArgumentException> {
            EasingFunction.CubicBezier(x1 = Float.NaN, 1F, 1F, 1F)
        }
        assertFailsWith<IllegalArgumentException> {
            EasingFunction.CubicBezier(1F, 1F, x2 = Float.POSITIVE_INFINITY, 1F)
        }
        assertFailsWith<IllegalArgumentException> {
            EasingFunction.CubicBezier(1F, 1F, x2 = Float.NaN, 1F)
        }
        assertFailsWith<IllegalArgumentException> {
            EasingFunction.CubicBezier(1F, y1 = Float.POSITIVE_INFINITY, 1F, 1F)
        }
        assertFailsWith<IllegalArgumentException> {
            EasingFunction.CubicBezier(1F, y1 = Float.NaN, 1F, 1F)
        }
        assertFailsWith<IllegalArgumentException> {
            EasingFunction.CubicBezier(1F, 1F, 1F, y2 = Float.POSITIVE_INFINITY)
        }
        assertFailsWith<IllegalArgumentException> {
            EasingFunction.CubicBezier(1F, 1F, 1F, y2 = Float.NaN)
        }
    }

    @Test
    fun cubicBezierHashCode_withIdenticalValues_matches() {
        assertThat(EasingFunction.CubicBezier(1f, 2f, 0.3f, 4f).hashCode())
            .isEqualTo(EasingFunction.CubicBezier(1f, 2f, 0.3f, 4f).hashCode())
    }

    @Test
    fun cubicBezierEquals_checksEqualityOfValues() {
        val original = EasingFunction.CubicBezier(1f, 2f, 0.3f, 4f)

        // Equal
        assertThat(original).isEqualTo(original) // Same instance.
        assertThat(original).isEqualTo(EasingFunction.CubicBezier(1f, 2f, 0.3f, 4f)) // Same values.

        // Not equal
        assertThat(original).isNotEqualTo(null)
        assertThat(original).isNotEqualTo(EasingFunction.Predefined.LINEAR) // Different type.
        assertThat(original)
            .isNotEqualTo(EasingFunction.CubicBezier(0.9f, 0.8f, 0.7f, 0.6f)) // Values.
    }

    @Test
    fun cubicBezierToString_returnsReasonableString() {
        assertThat(EasingFunction.CubicBezier(1f, 2f, 0.3f, 4f).toString())
            .isEqualTo("EasingFunction.CubicBezier(x1=1.0, y1=2.0, x2=0.3, y2=4.0)")
    }

    @Test
    fun linearConstructor_requiresXValuesInRange() {
        assertFailsWith<IllegalArgumentException> {
            EasingFunction.Linear(listOf(ImmutableVec(-0.1F, 0.5F)))
        }
        assertFailsWith<IllegalArgumentException> {
            EasingFunction.Linear(listOf(ImmutableVec(1.1F, 0.5F)))
        }
    }

    @Test
    fun linearConstructor_requiresFiniteValues() {
        assertFailsWith<IllegalArgumentException> {
            EasingFunction.Linear(listOf(ImmutableVec(Float.POSITIVE_INFINITY, 0.5F)))
        }
        assertFailsWith<IllegalArgumentException> {
            EasingFunction.Linear(listOf(ImmutableVec(Float.NaN, 0.5F)))
        }
        assertFailsWith<IllegalArgumentException> {
            EasingFunction.Linear(listOf(ImmutableVec(0.5F, Float.POSITIVE_INFINITY)))
        }
        assertFailsWith<IllegalArgumentException> {
            EasingFunction.Linear(listOf(ImmutableVec(0.5F, Float.NaN)))
        }
    }

    @Test
    fun linearConstructor_requiresSortedXValues() {
        assertFailsWith<IllegalArgumentException> {
            EasingFunction.Linear(listOf(ImmutableVec(0.75F, 0.5F), ImmutableVec(0.25F, 0.5F)))
        }
    }

    @Test
    fun linearHashCode_withIdenticalValues_matches() {
        assertThat(EasingFunction.Linear(listOf(ImmutableVec(0.25f, 0.1f))).hashCode())
            .isEqualTo(EasingFunction.Linear(listOf(ImmutableVec(0.25f, 0.1f))).hashCode())
    }

    @Test
    fun linearEquals_checksEqualityOfValues() {
        val original =
            EasingFunction.Linear(listOf(ImmutableVec(0.25f, 0.1f), ImmutableVec(0.75f, 0.9f)))

        // Equal
        assertThat(original).isEqualTo(original) // Same instance.
        assertThat(original)
            .isEqualTo(
                EasingFunction.Linear(listOf(ImmutableVec(0.25f, 0.1f), ImmutableVec(0.75f, 0.9f)))
            ) // Same values.

        // Not equal
        assertThat(original).isNotEqualTo(null)
        assertThat(original).isNotEqualTo(EasingFunction.Predefined.LINEAR) // Different type.
        assertThat(original)
            .isNotEqualTo(
                EasingFunction.Linear(listOf(ImmutableVec(0.25f, 0.1f)))
            ) // Shorter list of points.
        assertThat(original)
            .isNotEqualTo(
                EasingFunction.Linear(listOf(ImmutableVec(0.15f, 0.1f), ImmutableVec(0.75f, 0.9f)))
            ) // Different point values.
        assertThat(original)
            .isNotEqualTo(
                EasingFunction.Linear(
                    listOf(
                        ImmutableVec(0.25f, 0.1f),
                        ImmutableVec(0.75f, 0.9f),
                        ImmutableVec(0.9f, 0.5f)
                    )
                )
            ) // Longer list of points.
    }

    @Test
    fun linearToString_returnsReasonableString() {
        val string =
            EasingFunction.Linear(listOf(ImmutableVec(0.25f, 0.1f), ImmutableVec(0.75f, 0.9f)))
                .toString()
        assertThat(string).contains("EasingFunction.Linear")
        assertThat(string).contains("Vec")
        assertThat(string).contains("0.25")
        assertThat(string).contains("0.1")
        assertThat(string).contains("0.75")
        assertThat(string).contains("0.9")
    }

    @Test
    fun stepPositionConstants_areDistinct() {
        val set =
            setOf<EasingFunction.StepPosition>(
                EasingFunction.StepPosition.JUMP_START,
                EasingFunction.StepPosition.JUMP_END,
                EasingFunction.StepPosition.JUMP_NONE,
                EasingFunction.StepPosition.JUMP_BOTH,
            )
        assertThat(set.size).isEqualTo(4)
    }

    @Test
    fun stepPositionToString_returnsReasonableString() {
        assertThat(EasingFunction.StepPosition.JUMP_START.toString())
            .isEqualTo("EasingFunction.StepPosition.JUMP_START")
        assertThat(EasingFunction.StepPosition.JUMP_END.toString())
            .isEqualTo("EasingFunction.StepPosition.JUMP_END")
        assertThat(EasingFunction.StepPosition.JUMP_BOTH.toString())
            .isEqualTo("EasingFunction.StepPosition.JUMP_BOTH")
        assertThat(EasingFunction.StepPosition.JUMP_NONE.toString())
            .isEqualTo("EasingFunction.StepPosition.JUMP_NONE")
    }

    @Test
    fun stepPositionHashCode_withIdenticalValues_matches() {
        assertThat(EasingFunction.StepPosition.JUMP_START.hashCode())
            .isEqualTo(EasingFunction.StepPosition.JUMP_START.hashCode())

        assertThat(EasingFunction.StepPosition.JUMP_START.hashCode())
            .isNotEqualTo(EasingFunction.StepPosition.JUMP_END.hashCode())
    }

    @Test
    fun steps_withInvalidStepCount_throws() {
        // Step count less than zero throws.
        assertFailsWith<IllegalArgumentException> {
            EasingFunction.Steps(0, EasingFunction.StepPosition.JUMP_START)
        }
        assertFailsWith<IllegalArgumentException> {
            EasingFunction.Steps(-1, EasingFunction.StepPosition.JUMP_START)
        }

        // Step count not greater than 1 for JUMP_NONE throws.
        assertFailsWith<IllegalArgumentException> {
            EasingFunction.Steps(0, EasingFunction.StepPosition.JUMP_NONE)
        }
        assertFailsWith<IllegalArgumentException> {
            EasingFunction.Steps(1, EasingFunction.StepPosition.JUMP_NONE)
        }

        assertThat(EasingFunction.Steps(2, EasingFunction.StepPosition.JUMP_NONE)).isNotNull()
        assertThat(EasingFunction.Steps(1, EasingFunction.StepPosition.JUMP_START)).isNotNull()
    }

    @Test
    fun stepsHashCode_withSameValues_match() {
        assertThat(EasingFunction.Steps(1, EasingFunction.StepPosition.JUMP_START).hashCode())
            .isEqualTo(EasingFunction.Steps(1, EasingFunction.StepPosition.JUMP_START).hashCode())

        // Different step count.
        assertThat(EasingFunction.Steps(2, EasingFunction.StepPosition.JUMP_START).hashCode())
            .isNotEqualTo(
                EasingFunction.Steps(1, EasingFunction.StepPosition.JUMP_START).hashCode()
            )

        // Different stepPosition.
        assertThat(EasingFunction.Steps(1, EasingFunction.StepPosition.JUMP_START).hashCode())
            .isNotEqualTo(EasingFunction.Steps(1, EasingFunction.StepPosition.JUMP_END).hashCode())
    }

    @Test
    fun stepsEquals_checksEqualityOfValues() {
        val original = EasingFunction.Steps(2, EasingFunction.StepPosition.JUMP_START)

        // Equal
        assertThat(original).isEqualTo(original) // Same instance.
        assertThat(original)
            .isEqualTo(
                EasingFunction.Steps(2, EasingFunction.StepPosition.JUMP_START)
            ) // Same values.

        // Not equal
        assertThat(original).isNotEqualTo(null)
        // Different type.
        assertThat(original).isNotEqualTo(EasingFunction.Predefined.LINEAR)
        // Different count.
        assertThat(original)
            .isNotEqualTo(EasingFunction.Steps(3, EasingFunction.StepPosition.JUMP_START))
        // Different position.
        assertThat(original)
            .isNotEqualTo(EasingFunction.Steps(2, EasingFunction.StepPosition.JUMP_END))
    }

    @Test
    fun stepsToString_returnsReasonableString() {
        assertThat(EasingFunction.Steps(2, EasingFunction.StepPosition.JUMP_START).toString())
            .isEqualTo(
                "EasingFunction.Steps(stepCount=2, stepPosition=EasingFunction.StepPosition.JUMP_START)"
            )
    }
}
