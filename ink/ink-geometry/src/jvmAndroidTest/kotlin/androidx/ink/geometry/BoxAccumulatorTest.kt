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

package androidx.ink.geometry

import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.testing.buildStrokeInputBatchFromPoints
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BoxAccumulatorTest {

    @Test
    fun isEmpty_whenNotPresent_returnsTrue() {
        val envelope = BoxAccumulator()
        assertThat(envelope.isEmpty()).isTrue()
    }

    @Test
    fun isEmpty_whenPresent_returnsFalse() {
        val envelope = BoxAccumulator().add(rect1234)
        assertThat(envelope.isEmpty()).isFalse()
    }

    @Test
    fun reset_hasNoBounds() {
        val envelope = BoxAccumulator().add(rect1234)

        envelope.reset()

        assertThat(envelope.isEmpty()).isTrue()
    }

    @Test
    fun add_whenHasNoBounds_updatesToAddedPoint() {
        val envelope = BoxAccumulator()

        envelope.add(MutableVec(3F, 4F))

        assertThat(envelope.box!!.xMin).isEqualTo(3F)
        assertThat(envelope.box!!.xMax).isEqualTo(3F)
        assertThat(envelope.box!!.yMin).isEqualTo(4F)
        assertThat(envelope.box!!.yMax).isEqualTo(4F)
    }

    @Test
    fun add_whenHasBoundsThatIncludeAddedPoint_doesNotChange() {
        val envelope = BoxAccumulator().add(rect1234)

        envelope.add(ImmutableVec(2.1F, 3.2F))

        assertThat(envelope).isEqualTo(BoxAccumulator().add(rect1234))
    }

    @Test
    fun add_whenNewPointIsBelowXMin_updates() {
        val envelope = BoxAccumulator().add(rect1234)

        envelope.add(MutableVec(0.1F, 3.2F))

        assertThat(envelope)
            .isEqualTo(
                BoxAccumulator()
                    .add(
                        MutableBox()
                            .populateFromTwoPoints(ImmutableVec(0.1F, 2F), ImmutableVec(3F, 4F))
                    )
            )
    }

    @Test
    fun add_whenNewPointIsBelowYMin_updates() {
        val envelope = BoxAccumulator().add(rect1234)

        envelope.add(ImmutableVec(2.1F, 1.2F))

        assertThat(envelope)
            .isEqualTo(
                BoxAccumulator()
                    .add(
                        MutableBox()
                            .populateFromTwoPoints(ImmutableVec(1F, 1.2F), ImmutableVec(3F, 4F))
                    )
            )
    }

    @Test
    fun add_whenNewPointIsAboveXMax_updates() {
        val envelope = BoxAccumulator().add(rect1234)

        envelope.add(MutableVec(3.1F, 3.2F))

        assertThat(envelope)
            .isEqualTo(
                BoxAccumulator()
                    .add(
                        MutableBox()
                            .populateFromTwoPoints(ImmutableVec(1F, 2F), ImmutableVec(3.1F, 4F))
                    )
            )
    }

    @Test
    fun add_whenNewPointIsAboveYMax_updates() {
        val envelope = BoxAccumulator().add(rect1234)

        envelope.add(ImmutableVec(2.1F, 4.2F))

        assertThat(envelope)
            .isEqualTo(
                BoxAccumulator()
                    .add(
                        MutableBox()
                            .populateFromTwoPoints(ImmutableVec(1F, 2F), ImmutableVec(3F, 4.2F))
                    )
            )
    }

    @Test
    fun add_whenMultiplePointsAdded_updatesToOverallBounds() {
        val envelope = BoxAccumulator()
        envelope.add(ImmutableVec(1F, 1F))
        envelope.add(MutableVec(3F, 3F))
        envelope.add(ImmutableVec(4F, 4F))
        envelope.add(MutableVec(2F, 2F))

        assertThat(envelope)
            .isEqualTo(
                BoxAccumulator()
                    .add(
                        MutableBox()
                            .populateFromTwoPoints(ImmutableVec(1F, 1F), ImmutableVec(4F, 4F))
                    )
            )
    }

    @Test
    fun addEnvelope_whenNewIsEmpty_shouldNotChangeCurrent() {
        val envelope = BoxAccumulator().add(rect1234)

        envelope.add(BoxAccumulator())

        assertThat(envelope).isEqualTo(BoxAccumulator().add(rect1234))
    }

    @Test
    fun addEnvelope_whenCurrentIsEmpty_shouldReplaceCurrent() {
        val envelope = BoxAccumulator()

        envelope.add(BoxAccumulator(rect1234))

        assertThat(envelope).isEqualTo(BoxAccumulator().add(rect1234))
    }

    @Test
    fun addEnvelope_whenNewIsInsideCurrent_shouldNotChangeCurrent() {
        val envelope = BoxAccumulator().add(rect1234)

        envelope.add(
            BoxAccumulator(
                ImmutableBox.fromTwoPoints(ImmutableVec(1.1F, 2.1F), ImmutableVec(2.9F, 3.9F))
            )
        )

        assertThat(envelope).isEqualTo(BoxAccumulator().add(rect1234))
    }

    @Test
    fun addEnvelope_whenNewSurroundsCurrent_shouldReplaceCurrent() {
        val envelope = BoxAccumulator().add(rect1234)

        envelope.add(
            BoxAccumulator(
                ImmutableBox.fromTwoPoints(ImmutableVec(0.9F, 1.9F), ImmutableVec(3.1F, 4.1F))
            )
        )

        assertThat(envelope)
            .isEqualTo(
                BoxAccumulator()
                    .add(
                        MutableBox()
                            .populateFromTwoPoints(
                                ImmutableVec(0.9F, 1.9F),
                                ImmutableVec(3.1F, 4.1F)
                            )
                    )
            )
    }

    @Test
    fun addEnvelope_whenNewAndCurrentOverlap_shouldUpdateToUnion() {
        val envelope =
            BoxAccumulator()
                .add(MutableBox().populateFromTwoPoints(ImmutableVec(1F, 8F), ImmutableVec(4F, 9F)))

        envelope.add(
            BoxAccumulator(ImmutableBox.fromTwoPoints(ImmutableVec(2F, 7F), ImmutableVec(3F, 10F)))
        )

        assertThat(envelope)
            .isEqualTo(
                BoxAccumulator()
                    .add(
                        MutableBox()
                            .populateFromTwoPoints(ImmutableVec(1F, 7F), ImmutableVec(4F, 10F))
                    )
            )
    }

    @Test
    fun addEnvelope_whenNewAndCurrentAreDisjoint_shouldUpdateToUnion() {
        val envelope = BoxAccumulator().add(rect1234)

        envelope.add(
            BoxAccumulator(ImmutableBox.fromTwoPoints(ImmutableVec(2F, 0F), ImmutableVec(5F, 1F)))
        )

        assertThat(envelope)
            .isEqualTo(
                BoxAccumulator()
                    .add(
                        MutableBox()
                            .populateFromTwoPoints(ImmutableVec(1F, 0F), ImmutableVec(5F, 4F))
                    )
            )
    }

    @Test
    fun rect_withNoBounds_returnsNull() {
        assertThat(BoxAccumulator().box).isNull()
    }

    @Test
    fun rect_withBounds_returnsBox() {
        val addition = MutableBox().populateFrom(rect1234)
        val envelope = BoxAccumulator().add(addition)

        val rect = envelope.box

        assertThat(rect).isNotNull()
        assertThat(rect).isEqualTo(addition)
    }

    @Test
    fun populateFrom_whenNewIsEmpty_shouldReplaceCurrent() {
        val oldEnvelope = BoxAccumulator().add(rect1234)
        val newEnvelope = BoxAccumulator()

        newEnvelope.populateFrom(oldEnvelope)
        val rect = newEnvelope.box

        assertThat(rect).isNotNull()
        assertThat(rect).isEqualTo(rect1234)
    }

    @Test
    fun add_segmentToEmptyEnvelope_updatesEnvelope() {
        val envelope = BoxAccumulator()
        val segment = ImmutableSegment(start = ImmutableVec(1f, 10f), end = ImmutableVec(3f, 15f))

        envelope.add(segment)

        assertThat(envelope.isEmpty()).isFalse()
        assertThat(envelope)
            .isEqualTo(
                BoxAccumulator()
                    .add(
                        MutableBox()
                            .populateFromTwoPoints(ImmutableVec(1F, 10F), ImmutableVec(3F, 15F))
                    )
            )
    }

    @Test
    fun add_segmentToNonEmptyEnvelope_updatesEnvelope() {
        val envelope =
            BoxAccumulator()
                .add(
                    MutableBox()
                        .populateFromTwoPoints(ImmutableVec(10F, 10F), ImmutableVec(20F, 25F))
                )
        val segment = ImmutableSegment(start = ImmutableVec(1f, 10f), end = ImmutableVec(30f, 150f))

        envelope.add(segment)

        assertThat(envelope.isEmpty()).isFalse()
        assertThat(envelope)
            .isEqualTo(
                BoxAccumulator()
                    .add(
                        MutableBox()
                            .populateFromTwoPoints(ImmutableVec(1F, 10F), ImmutableVec(30F, 150F))
                    )
            )
    }

    @Test
    fun add_triangleToEmptyEnvelope_updatesEnvelope() {
        val envelope = BoxAccumulator()
        val triangle =
            ImmutableTriangle(
                p0 = ImmutableVec(1f, 5f),
                p1 = ImmutableVec(10f, 15f),
                p2 = ImmutableVec(6f, 20f),
            )

        envelope.add(triangle)

        assertThat(envelope.isEmpty()).isFalse()
        assertThat(envelope)
            .isEqualTo(
                BoxAccumulator()
                    .add(
                        MutableBox()
                            .populateFromTwoPoints(ImmutableVec(1F, 5F), ImmutableVec(10F, 20F))
                    )
            )
    }

    @Test
    fun add_triangleToNonEmptyEnvelope_updatesEnvelope() {
        val envelope =
            BoxAccumulator()
                .add(
                    MutableBox()
                        .populateFromTwoPoints(ImmutableVec(10F, 10F), ImmutableVec(20F, 25F))
                )
        val triangle =
            ImmutableTriangle(
                p0 = ImmutableVec(1f, 5f),
                p1 = ImmutableVec(10f, 15f),
                p2 = ImmutableVec(6f, 20f),
            )

        envelope.add(triangle)

        assertThat(envelope.isEmpty()).isFalse()
        assertThat(envelope)
            .isEqualTo(
                BoxAccumulator()
                    .add(
                        MutableBox()
                            .populateFromTwoPoints(ImmutableVec(1F, 5F), ImmutableVec(20F, 25F))
                    )
            )
    }

    @Test
    fun add_rectToEmptyEnvelope_updatesEnvelope() {
        val envelope = BoxAccumulator()
        val rect = ImmutableBox.fromTwoPoints(ImmutableVec(1f, 10f), ImmutableVec(-3f, -20f))

        envelope.add(rect)

        assertThat(envelope.isEmpty()).isFalse()
        assertThat(envelope)
            .isEqualTo(
                BoxAccumulator()
                    .add(
                        MutableBox()
                            .populateFromTwoPoints(ImmutableVec(-3F, -20F), ImmutableVec(1F, 10F))
                    )
            )
    }

    @Test
    fun add_rectToNonEmptyEnvelope_updatesEnvelope() {
        val envelope =
            BoxAccumulator()
                .add(
                    MutableBox()
                        .populateFromTwoPoints(ImmutableVec(10F, 10F), ImmutableVec(20F, 25F))
                )
        val rect = ImmutableBox.fromTwoPoints(ImmutableVec(100f, 200f), ImmutableVec(300f, 400f))

        envelope.add(rect)

        assertThat(envelope.isEmpty()).isFalse()
        assertThat(envelope)
            .isEqualTo(
                BoxAccumulator()
                    .add(
                        MutableBox()
                            .populateFromTwoPoints(ImmutableVec(10F, 10F), ImmutableVec(300F, 400F))
                    )
            )
    }

    @Test
    fun add_parallelogramToEmptyEnvelope_updatesEnvelope() {
        val envelope = BoxAccumulator()
        val parallelogram =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                center = ImmutableVec(10f, 20f),
                width = 4f,
                height = 6f,
                rotation = Angle.ZERO,
                shearFactor = 0f,
            )

        envelope.add(parallelogram)

        assertThat(envelope.isEmpty()).isFalse()
        assertThat(envelope)
            .isEqualTo(
                BoxAccumulator()
                    .add(
                        MutableBox()
                            .populateFromTwoPoints(ImmutableVec(8F, 17F), ImmutableVec(12F, 23F))
                    )
            )
    }

    @Test
    fun add_parallelogramToNonEmptyEnvelope_updatesEnvelope() {
        val envelope =
            BoxAccumulator()
                .add(
                    MutableBox()
                        .populateFromTwoPoints(ImmutableVec(10F, 10F), ImmutableVec(20F, 25F))
                )
        val parallelogram =
            ImmutableParallelogram.fromCenterAndDimensions(
                center = ImmutableVec(100f, 200f),
                width = 500f,
                height = 1000f,
            )

        envelope.add(parallelogram)

        assertThat(envelope.isEmpty()).isFalse()
        assertThat(envelope)
            .isEqualTo(
                BoxAccumulator()
                    .add(
                        MutableBox()
                            .populateFromTwoPoints(
                                ImmutableVec(-150F, -300F),
                                ImmutableVec(350F, 700F)
                            )
                    )
            )
    }

    @Test
    fun add_envelopeToEmptyEnvelope_updatesEnvelope() {
        val envelope = BoxAccumulator()
        val secondEnvelope = BoxAccumulator().add(rect1234)

        envelope.add(secondEnvelope)

        assertThat(envelope.isEmpty()).isFalse()
        assertThat(envelope.box!!).isEqualTo(secondEnvelope.box!!)
    }

    @Test
    fun add_envelopeToNonEmptyEnvelope_updatesEnvelope() {
        val envelope =
            BoxAccumulator()
                .add(
                    MutableBox()
                        .populateFromTwoPoints(ImmutableVec(10F, 10F), ImmutableVec(20F, 25F))
                )
        val secondEnvelope =
            BoxAccumulator()
                .add(
                    MutableBox()
                        .populateFromTwoPoints(ImmutableVec(-150F, -300F), ImmutableVec(350F, 700F))
                )

        envelope.add(secondEnvelope)

        assertThat(envelope.isEmpty()).isFalse()
        assertThat(envelope)
            .isEqualTo(
                BoxAccumulator()
                    .add(
                        MutableBox()
                            .populateFromTwoPoints(
                                ImmutableVec(-150F, -300F),
                                ImmutableVec(350F, 700F)
                            )
                    )
            )
    }

    @Test
    fun add_pointToEmptyEnvelope_updatesEnvelope() {
        val envelope = BoxAccumulator()
        val point = MutableVec(1f, 10f)

        envelope.add(point)

        assertThat(envelope.isEmpty()).isFalse()
        assertThat(envelope)
            .isEqualTo(
                BoxAccumulator()
                    .add(
                        MutableBox()
                            .populateFromTwoPoints(ImmutableVec(1F, 10F), ImmutableVec(1F, 10F))
                    )
            )
    }

    @Test
    fun add_pointToNonEmptyEnvelope_updatesEnvelope() {
        val envelope =
            BoxAccumulator()
                .add(
                    MutableBox()
                        .populateFromTwoPoints(ImmutableVec(10F, 10F), ImmutableVec(20F, 25F))
                )
        val point = MutableVec(1f, 5f)

        envelope.add(point)

        assertThat(envelope.isEmpty()).isFalse()
        assertThat(envelope)
            .isEqualTo(
                BoxAccumulator()
                    .add(
                        MutableBox()
                            .populateFromTwoPoints(ImmutableVec(1F, 5F), ImmutableVec(20F, 25F))
                    )
            )
    }

    @Test
    fun add_emptyMeshToEmptyEnvelope_doesNotUpdateEnvelope() {
        val envelope = BoxAccumulator()
        val mesh = PartitionedMesh()

        envelope.add(mesh)

        assertThat(envelope.isEmpty()).isTrue()
    }

    @Test
    fun isAlmostEqual_forTwoEmptyEnvelopes_returnsTrue() {
        val envelope1 = BoxAccumulator()
        val envelope2 = BoxAccumulator()

        assertThat(envelope1.isAlmostEqual(envelope2, tolerance = 0.1f)).isTrue()
    }

    @Test
    fun isAlmostEqual_forAnEmptyAndNonEmptyEnvelope_returnsFalse() {
        val envelope1 = BoxAccumulator()
        val envelope2 = BoxAccumulator().add(rect1234)

        assertThat(envelope1.isAlmostEqual(envelope2, tolerance = 0.00001f)).isFalse()
        assertThat(envelope2.isAlmostEqual(envelope1, tolerance = 0.00001f)).isFalse()
    }

    @Test
    fun isAlmostEqual_forNonEmptyEnvelopes_returnsCorrectValue() {
        val envelope1 = BoxAccumulator().add(rect1234)
        val envelope2 = BoxAccumulator().add(rect1234)
        val envelope3 =
            BoxAccumulator()
                .add(
                    ImmutableBox.fromTwoPoints(
                        ImmutableVec(1.00001F, 2.00001F),
                        ImmutableVec(2.99999F, 3.99999F),
                    )
                )

        assertThat(envelope1.isAlmostEqual(envelope2, tolerance = 0.00001f)).isTrue()
        assertThat(envelope1.isAlmostEqual(envelope3, tolerance = 0.001f)).isTrue()
        assertThat(envelope1.isAlmostEqual(envelope3, tolerance = 0.00000001f)).isFalse()
    }

    @Test
    fun equals_whenSameInstance_returnsTrueAndSameHashCode() {
        val envelope = BoxAccumulator().add(rect1234)

        assertThat(envelope).isEqualTo(envelope)
        assertThat(envelope.hashCode()).isEqualTo(envelope.hashCode())
    }

    @Test
    fun equals_whenDifferentType_returnsFalse() {
        val envelope = BoxAccumulator().add(rect1234)

        assertThat(envelope).isNotEqualTo(rect1234)
    }

    @Test
    fun equals_whenBothNoBounds_returnsTrueAndSameHashCode() {
        // Start with different values and reset them to make sure that the underlying bounds
        // storage
        // does not affect the result when hasBounds=false.
        val envelope = BoxAccumulator().add(rect1234).reset()
        val other = BoxAccumulator().add(rect5678).reset()

        assertThat(envelope).isEqualTo(other)
        assertThat(envelope.hashCode()).isEqualTo(other.hashCode())
    }

    @Test
    fun equals_whenSameBounds_returnsTrueAndSameHashCode() {
        val envelope = BoxAccumulator().add(rect1234)

        val other = BoxAccumulator().add(rect1234)
        assertThat(envelope).isEqualTo(other)
        assertThat(envelope.hashCode()).isEqualTo(other.hashCode())
    }

    @Test
    fun equals_whenDifferentBounds_returnsFalse() {
        val envelope = BoxAccumulator().add(rect1234)

        assertThat(envelope)
            .isNotEqualTo(
                BoxAccumulator()
                    .add(
                        MutableBox()
                            .populateFromTwoPoints(ImmutableVec(2F, 2F), ImmutableVec(3F, 4F))
                    )
            )
    }

    @Test
    fun equals_whenOnlyOneHasBounds_returnsFalse() {
        // Start with the same values and reset one to make sure that the underlying bounds storage
        // is not considered when hasBounds=false.
        val envelope = BoxAccumulator().add(rect1234).reset()
        val other = BoxAccumulator().add(rect1234)

        assertThat(other).isNotEqualTo(envelope)
    }

    @Test
    fun toString_whenEmpty_returnsAString() {
        val string = BoxAccumulator().toString()

        // Not elaborate checks - this test mainly exists to ensure that toString doesn't crash.
        assertThat(string).contains("BoxAccumulator")
        assertThat(string).contains("box")
    }

    @Test
    fun toString_whenNotEmpty_returnsAString() {
        val string = BoxAccumulator().add(rect1234).toString()

        // Not elaborate checks - this test mainly exists to ensure that toString doesn't crash.
        assertThat(string).contains("BoxAccumulator")
        assertThat(string).contains("box")
        assertThat(string).contains("MutableBox")
    }

    private val rect1234 = ImmutableBox.fromTwoPoints(ImmutableVec(1F, 2F), ImmutableVec(3F, 4F))
    private val rect5678 = ImmutableBox.fromTwoPoints(ImmutableVec(5F, 6F), ImmutableVec(7F, 8F))

    @Test
    fun add_meshToEmptyEnvelope_updatesEnvelope() {
        val envelope = BoxAccumulator()
        val mesh = buildTestStrokeShape()

        envelope.add(mesh)

        assertThat(envelope.isEmpty()).isFalse()
    }

    @Test
    fun add_meshToNonEmptyEnvelope_updatesEnvelope() {
        val envelope =
            BoxAccumulator()
                .add(
                    MutableBox()
                        .populateFromTwoPoints(ImmutableVec(10F, 10F), ImmutableVec(20F, 25F))
                )
        val mesh = buildTestStrokeShape()

        envelope.add(mesh)

        // Verify that the original lower-bounds for envelope (10F, 10F) are updated after adding
        // the
        // mesh.
        assertThat(envelope.isEmpty()).isFalse()
        val box = envelope.box!!
        assertThat(box.xMin).isLessThan(10f)
        assertThat(box.yMin).isLessThan(10f)
    }

    private fun buildTestStrokeShape(): PartitionedMesh {
        return Stroke(
                Brush(family = StockBrushes.markerLatest, size = 10f, epsilon = 0.1f),
                buildStrokeInputBatchFromPoints(floatArrayOf(10f, 3f, 20f, 5f)).asImmutable(),
            )
            .shape
    }
}
