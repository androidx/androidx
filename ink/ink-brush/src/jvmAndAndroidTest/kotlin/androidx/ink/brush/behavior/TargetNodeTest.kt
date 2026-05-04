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

package androidx.ink.brush.behavior

import androidx.ink.brush.behavior.TargetNode.Target
import com.google.common.truth.Truth.assertThat
import kotlin.IllegalArgumentException
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TargetNodeTest {

    @Test
    fun targetToString_returnsCorrectString() {
        assertThat(Target.WIDTH_MULTIPLIER.toString()).isEqualTo("Target.WIDTH_MULTIPLIER")
        assertThat(Target.HEIGHT_MULTIPLIER.toString()).isEqualTo("Target.HEIGHT_MULTIPLIER")
        assertThat(Target.SIZE_MULTIPLIER.toString()).isEqualTo("Target.SIZE_MULTIPLIER")
        assertThat(Target.SLANT_OFFSET_IN_RADIANS.toString())
            .isEqualTo("Target.SLANT_OFFSET_IN_RADIANS")
        assertThat(Target.PINCH_OFFSET.toString()).isEqualTo("Target.PINCH_OFFSET")
        assertThat(Target.ROTATION_OFFSET_IN_RADIANS.toString())
            .isEqualTo("Target.ROTATION_OFFSET_IN_RADIANS")
        assertThat(Target.CORNER_ROUNDING_OFFSET.toString())
            .isEqualTo("Target.CORNER_ROUNDING_OFFSET")
        assertThat(Target.POSITION_OFFSET_X_IN_MULTIPLES_OF_BRUSH_SIZE.toString())
            .isEqualTo("Target.POSITION_OFFSET_X_IN_MULTIPLES_OF_BRUSH_SIZE")
        assertThat(Target.POSITION_OFFSET_Y_IN_MULTIPLES_OF_BRUSH_SIZE.toString())
            .isEqualTo("Target.POSITION_OFFSET_Y_IN_MULTIPLES_OF_BRUSH_SIZE")
        assertThat(Target.POSITION_OFFSET_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE.toString())
            .isEqualTo("Target.POSITION_OFFSET_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE")
        assertThat(Target.POSITION_OFFSET_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE.toString())
            .isEqualTo("Target.POSITION_OFFSET_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE")
        assertThat(Target.TEXTURE_ANIMATION_PROGRESS_OFFSET.toString())
            .isEqualTo("Target.TEXTURE_ANIMATION_PROGRESS_OFFSET")
        assertThat(Target.HUE_OFFSET_IN_RADIANS.toString())
            .isEqualTo("Target.HUE_OFFSET_IN_RADIANS")
        assertThat(Target.SATURATION_MULTIPLIER.toString())
            .isEqualTo("Target.SATURATION_MULTIPLIER")
        assertThat(Target.LUMINOSITY_OFFSET.toString()).isEqualTo("Target.LUMINOSITY_OFFSET")
        assertThat(Target.OPACITY_MULTIPLIER.toString()).isEqualTo("Target.OPACITY_MULTIPLIER")
    }

    @Test
    fun targetNodeConstructor_throwsForNonFiniteTargetModifierRange() {
        val input = ConstantNode(0f)
        assertFailsWith<IllegalArgumentException> {
            TargetNode(Target.SIZE_MULTIPLIER, Float.NaN, 1f, input)
        }
        assertFailsWith<IllegalArgumentException> {
            TargetNode(Target.SIZE_MULTIPLIER, 0f, Float.POSITIVE_INFINITY, input)
        }
    }

    @Test
    fun targetNodeConstructor_throwsForEmptyTargetModifierRange() {
        val input = ConstantNode(0f)
        assertFailsWith<IllegalArgumentException> {
            TargetNode(Target.SIZE_MULTIPLIER, 0.5f, 0.5f, input)
        }
    }

    @Test
    fun targetNodeInputs_containsInput() {
        val input = ConstantNode(0f)
        val node = TargetNode(Target.SIZE_MULTIPLIER, 0f, 1f, input)
        assertThat(node.inputs).containsExactly(input)
    }

    @Test
    fun targetNodeToString() {
        val input = ConstantNode(0f)
        val node = TargetNode(Target.SIZE_MULTIPLIER, 0f, 1f, input)
        assertThat(node.toString())
            .isEqualTo("TargetNode(SIZE_MULTIPLIER, 0.0, 1.0, ConstantNode(0.0))")
    }

    @Test
    fun targetNodeEquals_checksEqualityOfValues() {
        val node1 = TargetNode(Target.SIZE_MULTIPLIER, 0f, 1f, ConstantNode(1f))
        val node2 = TargetNode(Target.SIZE_MULTIPLIER, 0f, 1f, ConstantNode(1f))
        val node3 = TargetNode(Target.SIZE_MULTIPLIER, 0f, 1f, ConstantNode(2f))
        assertThat(node1).isEqualTo(node2)
        assertThat(node1).isNotEqualTo(node3)
    }

    @Test
    fun targetNodeHashCode_withIdenticalValues_match() {
        val node1 = TargetNode(Target.SIZE_MULTIPLIER, 0f, 1f, ConstantNode(1f))
        val node2 = TargetNode(Target.SIZE_MULTIPLIER, 0f, 1f, ConstantNode(1f))
        val node3 = TargetNode(Target.SIZE_MULTIPLIER, 0f, 1f, ConstantNode(2f))
        assertThat(node1.hashCode()).isEqualTo(node2.hashCode())
        assertThat(node1.hashCode()).isNotEqualTo(node3.hashCode())
    }

    @Test
    fun targetNodeConstructor_withInvalidArguments_throws() {
        // targetModifierRangeStart not finite
        val targetModifierRangeStartError =
            assertFailsWith<IllegalArgumentException> {
                TargetNode(
                    target = Target.WIDTH_MULTIPLIER,
                    targetModifierRangeStart = Float.NaN, // Not finite.
                    targetModifierRangeEnd = 1.75f,
                    input = ConstantNode(0f),
                )
            }
        assertThat(targetModifierRangeStartError.message).contains("target")
        assertThat(targetModifierRangeStartError.message).contains("finite")

        // targetModifierRangeEnd not finite
        val targetModifierRangeEndError =
            assertFailsWith<IllegalArgumentException> {
                TargetNode(
                    target = Target.WIDTH_MULTIPLIER,
                    targetModifierRangeStart = 1.0f,
                    targetModifierRangeEnd = Float.NaN, // Not finite.
                    input = ConstantNode(0f),
                )
            }
        assertThat(targetModifierRangeEndError.message).contains("target")
        assertThat(targetModifierRangeEndError.message).contains("finite")
    }

    @Test
    fun targetNodeEquals_withDifferentValues_returnsFalse() {
        val original =
            TargetNode(
                target = Target.WIDTH_MULTIPLIER,
                targetModifierRangeStart = 1.0f,
                targetModifierRangeEnd = 1.75f,
                input = ConstantNode(0f),
            )
        assertThat(
                original.equals(
                    TargetNode(
                        target = Target.HEIGHT_MULTIPLIER, // different
                        targetModifierRangeStart = 1.0f,
                        targetModifierRangeEnd = 1.75f,
                        input = ConstantNode(0f),
                    )
                )
            )
            .isFalse()
        assertThat(
                original.equals(
                    TargetNode(
                        target = Target.WIDTH_MULTIPLIER,
                        targetModifierRangeStart = 1.56f, // different
                        targetModifierRangeEnd = 1.75f,
                        input = ConstantNode(0f),
                    )
                )
            )
            .isFalse()
        assertThat(
                original.equals(
                    TargetNode(
                        target = Target.WIDTH_MULTIPLIER,
                        targetModifierRangeStart = 1.0f,
                        targetModifierRangeEnd = 1.99f, // different
                        input = ConstantNode(0f),
                    )
                )
            )
            .isFalse()
        assertThat(
                original.equals(
                    TargetNode(
                        target = Target.WIDTH_MULTIPLIER,
                        targetModifierRangeStart = 1.0f,
                        targetModifierRangeEnd = 1.75f,
                        input = ConstantNode(1f), // different
                    )
                )
            )
            .isFalse()
    }
}
