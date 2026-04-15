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

import androidx.ink.brush.behavior.DampingNode
import androidx.ink.brush.behavior.EasingFunction
import androidx.ink.brush.behavior.OutOfRange
import androidx.ink.brush.behavior.ProgressDomain
import androidx.ink.brush.behavior.ResponseNode
import androidx.ink.brush.behavior.SourceNode
import androidx.ink.brush.behavior.SourceNode.Source
import androidx.ink.brush.behavior.TargetNode
import androidx.ink.brush.behavior.TargetNode.Target
import androidx.ink.brush.behavior.ToolTypeFilterNode
import androidx.ink.nativeloader.UsedByNative
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalInkCustomBrushApi::class)
@RunWith(JUnit4::class)
class BrushBehaviorTest {

    @Test
    fun brushBehaviorToString_returnsReasonableString() {
        assertThat(
                BrushBehavior(
                        TargetNode(
                            target = Target.WIDTH_MULTIPLIER,
                            targetModifierRangeStart = 1.0f,
                            targetModifierRangeEnd = 1.75f,
                            input =
                                SourceNode(
                                    source = Source.NORMALIZED_PRESSURE,
                                    sourceValueRangeStart = 0.0f,
                                    sourceValueRangeEnd = 1.0f,
                                ),
                        ),
                        developerComment = "foobar",
                    )
                    .toString()
            )
            .isEqualTo(
                "BrushBehavior([TargetNode(WIDTH_MULTIPLIER, 1.0, 1.75, " +
                    "SourceNode(NORMALIZED_PRESSURE, 0.0, 1.0, CLAMP))], developerComment=foobar)"
            )
    }

    @Test
    fun brushBehaviorEquals_withIdenticalValues_returnsTrue() {
        val original =
            BrushBehavior(
                TargetNode(
                    target = Target.WIDTH_MULTIPLIER,
                    targetModifierRangeStart = 1.0f,
                    targetModifierRangeEnd = 1.75f,
                    input =
                        DampingNode(
                            dampingSource = ProgressDomain.TIME_IN_SECONDS,
                            dampingGap = 0.001f,
                            input =
                                ResponseNode(
                                    responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                                    input =
                                        ToolTypeFilterNode(
                                            enabledToolTypes = setOf(InputToolType.STYLUS),
                                            input =
                                                SourceNode(
                                                    source = Source.NORMALIZED_PRESSURE,
                                                    sourceValueRangeStart = 0.0f,
                                                    sourceValueRangeEnd = 1.0f,
                                                    sourceOutOfRangeBehavior = OutOfRange.CLAMP,
                                                ),
                                        ),
                                ),
                        ),
                )
            )

        val exact =
            BrushBehavior(
                TargetNode(
                    target = Target.WIDTH_MULTIPLIER,
                    targetModifierRangeStart = 1.0f,
                    targetModifierRangeEnd = 1.75f,
                    input =
                        DampingNode(
                            dampingSource = ProgressDomain.TIME_IN_SECONDS,
                            dampingGap = 0.001f,
                            input =
                                ResponseNode(
                                    responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                                    input =
                                        ToolTypeFilterNode(
                                            enabledToolTypes = setOf(InputToolType.STYLUS),
                                            input =
                                                SourceNode(
                                                    source = Source.NORMALIZED_PRESSURE,
                                                    sourceValueRangeStart = 0.0f,
                                                    sourceValueRangeEnd = 1.0f,
                                                    sourceOutOfRangeBehavior = OutOfRange.CLAMP,
                                                ),
                                        ),
                                ),
                        ),
                )
            )

        assertThat(original.equals(exact)).isTrue()
    }

    /**
     * Creates an expected C++ StepFunction BrushBehavior and returns true if every property of the
     * Kotlin BrushBehavior's JNI-created C++ counterpart is equivalent to the expected C++
     * BrushBehavior.
     */
    @UsedByNative
    private external fun matchesNativeStepBehavior(
        nativePointerToActualBrushBehavior: Long
    ): Boolean

    /**
     * Creates an expected C++ PredefinedFunction BrushBehavior and returns true if every property
     * of the Kotlin BrushBehavior's JNI-created C++ counterpart is equivalent to the expected C++
     * BrushBehavior.
     */
    @UsedByNative
    private external fun matchesNativePredefinedBehavior(
        nativePointerToActualBrushBehavior: Long
    ): Boolean

    /**
     * Creates an expected C++ CubicBezier BrushBehavior and returns true if every property of the
     * Kotlin BrushBehavior's JNI-created C++ counterpart is equivalent to the expected C++
     * BrushBehavior.
     */
    @UsedByNative
    private external fun matchesNativeCubicBezierBehavior(
        nativePointerToActualBrushBehavior: Long
    ): Boolean

    /**
     * Creates an expected C++ Linear BrushBehavior and returns true if every property of the Kotlin
     * BrushBehavior's JNI-created C++ counterpart is equivalent to the expected C++ BrushBehavior.
     */
    @UsedByNative
    private external fun matchesNativeLinearBehavior(
        nativePointerToActualBrushBehavior: Long
    ): Boolean
}
