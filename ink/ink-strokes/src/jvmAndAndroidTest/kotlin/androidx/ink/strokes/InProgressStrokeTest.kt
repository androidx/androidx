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

package androidx.ink.strokes

import androidx.ink.brush.Brush
import androidx.ink.brush.BrushBehavior
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.BrushTip
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.InputToolType
import androidx.ink.brush.StockBrushes
import androidx.ink.geometry.BoxAccumulator
import androidx.ink.geometry.ImmutableVec
import androidx.ink.geometry.MutableVec
import androidx.ink.strokes.testing.buildStrokeInputBatchFromPoints
import com.google.common.truth.Truth.assertThat
import java.nio.ByteOrder
import java.nio.ReadOnlyBufferException
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Unit tests for [InProgressStroke]. */
@RunWith(JUnit4::class)
class InProgressStrokeTest {

    private fun makeStartAndExtendStroke() =
        InProgressStroke().apply {
            start(makeBrush())
            enqueueInputs(
                buildStrokeInputBatchFromPoints(floatArrayOf(10f, 3f, 20f, 5f, 30f, 7f)),
                ImmutableStrokeInputBatch.EMPTY,
            )
            updateShape(2L)
        }

    @Test
    fun unstartedStroke_hasNullBrush() {
        val inProgressStroke = InProgressStroke()

        assertThat(inProgressStroke.brush).isNull()
    }

    @Test
    fun unstartedStroke_doesNotNeedUpdate() {
        val inProgressStroke = InProgressStroke()

        assertThat(inProgressStroke.isUpdateNeeded()).isFalse()
    }

    @Test
    fun unstartedStroke_doesNotChangeWithTime() {
        val inProgressStroke = InProgressStroke()

        assertThat(inProgressStroke.changesWithTime()).isFalse()
    }

    @Test
    fun unstartedStroke_inputIsFinished() {
        val inProgressStroke = InProgressStroke()

        assertThat(inProgressStroke.isInputFinished()).isTrue()
    }

    @Test
    fun startStroke_setsBrush() {
        val brush = makeBrush()
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(brush)
        val brushOut = inProgressStroke.brush

        assertThat(brushOut).isNotNull()
        assertThat(brushOut!!).isEqualTo(brush)
    }

    @Test
    fun startStroke_inputIsNotFinished() {
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(makeBrush())

        assertThat(inProgressStroke.isInputFinished()).isFalse()
    }

    @Test
    fun startStroke_withSimpleBrush_doesNotChangeWithTime() {
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(makeBrush())

        assertThat(inProgressStroke.changesWithTime()).isFalse()
    }

    @OptIn(ExperimentalInkCustomBrushApi::class)
    @Test
    fun startStroke_withTimeSinceInputBrush_changesWithTime() {
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(makeTimeSinceInputBrush())

        assertThat(inProgressStroke.changesWithTime()).isTrue()
    }

    @OptIn(ExperimentalInkCustomBrushApi::class)
    @Test
    fun startStroke_withTimeSinceInputBrushAfterEndTime_noLongerChangesWithTime() {
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(makeTimeSinceInputBrush(timeSinceInputEndMillis = 1000F))
        assertThat(inProgressStroke.changesWithTime()).isTrue()

        inProgressStroke.updateShape(currentElapsedTimeMillis = 999)
        assertThat(inProgressStroke.changesWithTime()).isTrue()

        inProgressStroke.updateShape(currentElapsedTimeMillis = 1000)
        assertThat(inProgressStroke.changesWithTime()).isFalse()
    }

    @Test
    fun enqueueInputs_withRealAndPredictedInputs_isUpdateNeeded() {
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(makeBrush())
        val realInputs = buildStrokeInputBatchFromPoints(floatArrayOf(10f, 3f, 20f, 5f, 30f, 7f))
        val predictedInputs =
            buildStrokeInputBatchFromPoints(
                floatArrayOf(40f, 9f, 50f, 11f, 60f, 13f),
                InputToolType.STYLUS,
                startTime = 3L,
            )

        inProgressStroke.enqueueInputs(realInputs, predictedInputs)
        assertThat(inProgressStroke.isUpdateNeeded()).isTrue()
        assertThat(inProgressStroke.changesWithTime()).isFalse()
    }

    @Test
    fun enqueueInputs_onSuccess_incrementsVersion() {
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(makeBrush())
        val realInputs = buildStrokeInputBatchFromPoints(floatArrayOf(10f, 3f, 20f, 5f, 30f, 7f))
        val predictedInputs =
            buildStrokeInputBatchFromPoints(
                floatArrayOf(40f, 9f, 50f, 11f, 60f, 13f),
                InputToolType.STYLUS,
                startTime = 3L,
            )

        val previousVersion = inProgressStroke.version
        inProgressStroke.enqueueInputs(realInputs, predictedInputs)
        assertThat(inProgressStroke.version).isEqualTo(previousVersion + 1)
    }

    @Test
    fun enqueueInputs_beforeStart_fails() {
        val inProgressStroke = InProgressStroke()

        val realInputs = buildStrokeInputBatchFromPoints(floatArrayOf(10f, 3f, 20f, 5f, 30f, 7f))
        assertThat(
                assertFailsWith<IllegalStateException> {
                    inProgressStroke.enqueueInputs(realInputs, ImmutableStrokeInputBatch.EMPTY)
                }
            )
            .hasMessageThat()
            .contains("Start")
        assertThat(inProgressStroke.isUpdateNeeded()).isFalse()
    }

    @Test
    fun enqueueInputs_onFailure_doesNotIncrementVersion() {
        val inProgressStroke = InProgressStroke()

        val realInputs = buildStrokeInputBatchFromPoints(floatArrayOf(10f, 3f, 20f, 5f, 30f, 7f))
        val previousVersion = inProgressStroke.version
        assertFailsWith<IllegalStateException> {
            inProgressStroke.enqueueInputs(realInputs, ImmutableStrokeInputBatch.EMPTY)
        }
        assertThat(inProgressStroke.version).isEqualTo(previousVersion)
    }

    @Test
    fun updateShape_withPositiveElapsedTime_succeeds() {
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(makeBrush())
        inProgressStroke.updateShape(2)
    }

    @Test
    fun updateShape_onSuccess_updatesVersion() {
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(makeBrush())
        val previousVersion = inProgressStroke.version
        inProgressStroke.updateShape(2)
        assertThat(inProgressStroke.version).isEqualTo(previousVersion + 1)
    }

    @Test
    fun updateShape_withNegativeElapsedTime_throws() {
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(makeBrush())

        val realInputs = buildStrokeInputBatchFromPoints(floatArrayOf(10f, 3f, 20f, 5f, 30f, 7f))
        inProgressStroke.enqueueInputs(realInputs, ImmutableStrokeInputBatch.EMPTY)

        assertThat(inProgressStroke.isUpdateNeeded()).isTrue()
        val error = assertFailsWith<IllegalArgumentException> { inProgressStroke.updateShape(-1) }
        assertThat(error).hasMessageThat().contains("non-negative")
        assertThat(inProgressStroke.isUpdateNeeded()).isTrue()
    }

    @Test
    fun updateShape_onFailure_doesNotIncrementVersion() {
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(makeBrush())

        val realInputs = buildStrokeInputBatchFromPoints(floatArrayOf(10f, 3f, 20f, 5f, 30f, 7f))
        inProgressStroke.enqueueInputs(realInputs, ImmutableStrokeInputBatch.EMPTY)
        val previousVersion = inProgressStroke.version
        assertFailsWith<IllegalArgumentException> { inProgressStroke.updateShape(-1) }
        assertThat(inProgressStroke.version).isEqualTo(previousVersion)
    }

    @Test
    fun enqueueInputs_withEmptyRealInputs_succeeds() {
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(makeBrush())
        val predictedInputs =
            buildStrokeInputBatchFromPoints(floatArrayOf(10f, 3f, 20f, 5f, 30f, 7f))
        inProgressStroke.enqueueInputs(ImmutableStrokeInputBatch.EMPTY, predictedInputs)
    }

    @Test
    fun enqueueInputs_withEmptyPredictedInputs_succeeds() {
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(makeBrush())
        val realInputs = buildStrokeInputBatchFromPoints(floatArrayOf(10f, 3f, 20f, 5f, 30f, 7f))
        inProgressStroke.enqueueInputs(realInputs, ImmutableStrokeInputBatch.EMPTY)
    }

    @Test
    fun enqueueInputs_withRealAndPredictedInputs_succeeds() {
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(makeBrush())
        val realInputs = buildStrokeInputBatchFromPoints(floatArrayOf(10f, 3f, 20f, 5f, 30f, 7f))
        val predictedInputs =
            buildStrokeInputBatchFromPoints(
                floatArrayOf(40f, 9f, 50f, 11f, 60f, 13f),
                InputToolType.STYLUS,
                startTime = 3L,
            )

        inProgressStroke.enqueueInputs(realInputs, predictedInputs)
    }

    @Test
    fun enqueueInputs_withRealAndPredictedInputsImmutable_succeeds() {
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(makeBrush())
        val realInputs =
            buildStrokeInputBatchFromPoints(floatArrayOf(10f, 3f, 20f, 5f, 30f, 7f)).toImmutable()
        val predictedInputs =
            buildStrokeInputBatchFromPoints(
                    floatArrayOf(40f, 9f, 50f, 11f, 60f, 13f),
                    InputToolType.STYLUS,
                    startTime = 3L,
                )
                .toImmutable()

        inProgressStroke.enqueueInputs(realInputs, predictedInputs)
    }

    @Test
    fun enqueueInputs_withLowElapsedTime_succeeds() {
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(makeBrush())
        val realInputs = buildStrokeInputBatchFromPoints(floatArrayOf(10f, 3f, 20f, 5f))
        val predictedInputs = ImmutableStrokeInputBatch.EMPTY
        inProgressStroke.enqueueInputs(realInputs, predictedInputs) // adds 2 inputs points
        // Adding the same two points does not throw an error.
        inProgressStroke.enqueueInputs(realInputs, predictedInputs)

        inProgressStroke.updateShape(10)
        assertThat(inProgressStroke.getInputCount()).isEqualTo(2)
    }

    @Test
    fun enqueueInputs_withInvalidRealInputs_succeeds() {
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(makeBrush())
        val realInputs1 = buildStrokeInputBatchFromPoints(floatArrayOf(10f, 3f, 20f, 5f, 30f, 7f))
        val realInputs2 =
            buildStrokeInputBatchFromPoints(floatArrayOf(10f, 3f, 20f, 5f, 30f, 7f, 12f, 34f))
        inProgressStroke.enqueueInputs(realInputs1, ImmutableStrokeInputBatch.EMPTY)
        // Adding the invalid inputs does not throw an error, but discards the invalid points.
        inProgressStroke.enqueueInputs(realInputs2, ImmutableStrokeInputBatch.EMPTY)

        inProgressStroke.updateShape(10)
        assertThat(inProgressStroke.getInputCount()).isEqualTo(4)
    }

    @Test
    fun enqueueInputs_withInvalidPredictedInputs_succeeds() {
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(makeBrush())
        val realInputs =
            buildStrokeInputBatchFromPoints(
                floatArrayOf(10f, 3f, 20f, 5f, 30f, 7f)
            ) // elapsed time 0, 1, 2
        val predictedInputs =
            buildStrokeInputBatchFromPoints(floatArrayOf(30f, 7f, 40f, 9f)) // elapsed time 0, 1

        inProgressStroke.enqueueInputs(realInputs, predictedInputs)
        // No error is thrown, but none of the predicted inputs are queued.
        inProgressStroke.updateShape(10)
        assertThat(inProgressStroke.getPredictedInputCount()).isEqualTo(0)
    }

    @Test
    fun enqueueInputs_withMismatchedOptionalAttributes_fails() {
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(makeBrush())

        val batchWithPressure =
            MutableStrokeInputBatch().apply {
                add(
                    StrokeInput.create(1f, 2f, 0L, toolType = InputToolType.STYLUS, pressure = 0.5f)
                )
                add(
                    StrokeInput.create(3f, 4f, 1L, toolType = InputToolType.STYLUS, pressure = 0.6f)
                )
            }

        val batchWithoutPressure =
            MutableStrokeInputBatch().apply {
                add(StrokeInput.create(5f, 6f, 2L, toolType = InputToolType.STYLUS))
                add(StrokeInput.create(7f, 8f, 3L, toolType = InputToolType.STYLUS))
            }

        // First, enqueue inputs with pressure.
        inProgressStroke.enqueueInputs(batchWithPressure, ImmutableStrokeInputBatch.EMPTY)

        // Attempt to enqueue inputs without pressure, which should fail.
        val error =
            assertFailsWith<IllegalArgumentException> {
                inProgressStroke.enqueueInputs(
                    batchWithoutPressure,
                    ImmutableStrokeInputBatch.EMPTY,
                )
            }
        assertThat(error).hasMessageThat().contains("pressure")
    }

    @Test
    fun finishInput_inputIsFinished() {
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(makeBrush())
        inProgressStroke.finishInput()

        assertThat(inProgressStroke.isInputFinished()).isTrue()
    }

    @Test
    fun inputCount_isRealAndPredictedInputs() {
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(makeBrush())
        val realInputs = buildStrokeInputBatchFromPoints(floatArrayOf(10f, 3f, 20f, 5f, 30f, 7f))
        val predictedInputs =
            buildStrokeInputBatchFromPoints(
                floatArrayOf(40f, 9f, 50f, 11f),
                InputToolType.STYLUS,
                startTime = 3L,
            )
        inProgressStroke.enqueueInputs(realInputs, predictedInputs)
        inProgressStroke.updateShape(2)

        assertThat(inProgressStroke.getInputCount()).isEqualTo(5)
        assertThat(inProgressStroke.getRealInputCount()).isEqualTo(3)
        assertThat(inProgressStroke.getPredictedInputCount()).isEqualTo(2)
    }

    @Test
    fun populateInput_returnsSameInputsAsPopulateInputs() {
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(makeBrush())
        val realInputs = buildStrokeInputBatchFromPoints(floatArrayOf(10f, 3f, 20f, 5f, 30f, 7f))
        val predictedInputs =
            buildStrokeInputBatchFromPoints(
                floatArrayOf(40f, 9f, 50f, 11f, 60f, 13f),
                InputToolType.STYLUS,
                startTime = 3L,
            )
        inProgressStroke.enqueueInputs(realInputs, predictedInputs)
        inProgressStroke.updateShape(2)

        val inputCount = inProgressStroke.getInputCount()
        assertThat(inputCount).isEqualTo(6)
        val copiedInputs = MutableStrokeInputBatch().apply { inProgressStroke.populateInputs(this) }
        assertThat(copiedInputs.size).isEqualTo(inputCount)
        for (i in 0 until inputCount) {
            val input = StrokeInput()
            inProgressStroke.populateInput(input, i)
            assertThat(input).isEqualTo(copiedInputs.get(i))
        }
    }

    @Test
    fun populateInputs_withFromAndToBounds() {
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(makeBrush())
        val realInputs = buildStrokeInputBatchFromPoints(floatArrayOf(10f, 3f, 20f, 5f, 30f, 7f))
        val predictedInputs =
            buildStrokeInputBatchFromPoints(
                floatArrayOf(40f, 9f, 50f, 11f, 60f, 13f),
                InputToolType.STYLUS,
                startTime = 3L,
            )
        inProgressStroke.enqueueInputs(realInputs, predictedInputs)
        inProgressStroke.updateShape(2)

        val inputCount = inProgressStroke.getInputCount()
        assertThat(inputCount).isEqualTo(6)
        val copiedInputs =
            MutableStrokeInputBatch().apply { inProgressStroke.populateInputs(this, 2, 4) }
        assertThat(copiedInputs.size).isEqualTo(2)
        for (i in 2 until 4) {
            val input = StrokeInput()
            inProgressStroke.populateInput(input, i)
            assertThat(input).isEqualTo(copiedInputs.get(i - 2))
        }
    }

    @Test
    fun populateInputs_clearsExistingInputs() {
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(makeBrush())
        val realInputs = buildStrokeInputBatchFromPoints(floatArrayOf(10f, 3f, 20f, 5f, 30f, 7f))
        val predictedInputs = ImmutableStrokeInputBatch.EMPTY
        inProgressStroke.enqueueInputs(realInputs, predictedInputs)
        inProgressStroke.updateShape(2)

        val inputCount = inProgressStroke.getInputCount()
        assertThat(inputCount).isEqualTo(3)
        val existingInputs =
            MutableStrokeInputBatch().apply { inProgressStroke.populateInputs(this) }
        assertThat(existingInputs.size).isEqualTo(inputCount)
        val copiedInputs =
            MutableStrokeInputBatch().apply { inProgressStroke.populateInputs(this, 2, 3) }
        assertThat(copiedInputs.size).isEqualTo(1)
    }

    @Test
    @Suppress("Range")
    fun populateInputs_incorrectBoundsRaisesException() {
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(makeBrush())
        val realInputs = buildStrokeInputBatchFromPoints(floatArrayOf(10f, 3f, 20f, 5f, 30f, 7f))
        val predictedInputs =
            buildStrokeInputBatchFromPoints(
                floatArrayOf(40f, 9f, 50f, 11f, 60f, 13f),
                InputToolType.STYLUS,
                startTime = 3L,
            )
        inProgressStroke.enqueueInputs(realInputs, predictedInputs)
        inProgressStroke.updateShape(2)
        assertThat(inProgressStroke.getInputCount()).isEqualTo(6)
        assertFailsWith<IllegalArgumentException> {
            inProgressStroke.populateInputs(MutableStrokeInputBatch(), -1)
        }
        assertFailsWith<IllegalArgumentException> {
            inProgressStroke.populateInputs(MutableStrokeInputBatch(), 6, 7)
        }
        assertFailsWith<IllegalArgumentException> {
            inProgressStroke.populateInputs(MutableStrokeInputBatch(), 6, 5)
        }
    }

    @Test
    fun populateInputs_emptyRangeIsValid() {
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(makeBrush())
        val realInputs = buildStrokeInputBatchFromPoints(floatArrayOf(10f, 3f, 20f, 5f, 30f, 7f))
        val predictedInputs =
            buildStrokeInputBatchFromPoints(
                floatArrayOf(40f, 9f, 50f, 11f, 60f, 13f),
                InputToolType.STYLUS,
                startTime = 3L,
            )
        inProgressStroke.enqueueInputs(realInputs, predictedInputs)
        inProgressStroke.updateShape(2)
        assertThat(inProgressStroke.getInputCount()).isEqualTo(6)
        val output = MutableStrokeInputBatch().apply { inProgressStroke.populateInputs(this, 6) }
        assertThat(output.size).isEqualTo(0)
    }

    @Test
    fun getBrushCoatCount_withUnstartedStroke_isZero() {
        val inProgressStroke = InProgressStroke()
        assertThat(inProgressStroke.getBrushCoatCount()).isEqualTo(0)
    }

    @Test
    fun getMeshBounds_withStartedStroke_returnsBounds() {
        val inProgressStroke = makeStartAndExtendStroke()

        assertThat(inProgressStroke.getBrushCoatCount()).isEqualTo(1)
        val envelope = BoxAccumulator()
        inProgressStroke.populateMeshBounds(0, envelope)
        assertThat(envelope.isEmpty()).isFalse()
        val bounds = envelope.box!!
        assertThat(bounds.xMin).isNonZero()
        assertThat(bounds.yMin).isNonZero()
        assertThat(bounds.xMax).isGreaterThan(20f) // change in x of inputs
        assertThat(bounds.yMax).isGreaterThan(4f) // change in y of inputs
    }

    @Test
    fun populateUpdatedRegion_withEmptyStroke_returnsEmptyEnvelope() {
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(makeBrush())
        val envelope = BoxAccumulator()
        inProgressStroke.populateUpdatedRegion(envelope)
        assertThat(envelope.isEmpty()).isTrue()
    }

    @Test
    fun populateUpdatedRegion_withStartedStroke_returnsBounds() {
        val inProgressStroke = makeStartAndExtendStroke()
        val envelope = BoxAccumulator()

        inProgressStroke.populateUpdatedRegion(envelope)

        assertThat(envelope.isEmpty()).isFalse()
        val bounds = envelope.box!!
        assertThat(bounds.xMin).isNonZero()
        assertThat(bounds.yMin).isNonZero()
        assertThat(bounds.xMax).isGreaterThan(20f) // change in x of inputs
        assertThat(bounds.yMax).isGreaterThan(4f) // change in y of inputs
    }

    @Test
    fun populateUpdatedRegion_overwritesInput() {
        val inProgressStroke = makeStartAndExtendStroke()
        val previouslyEmpty = BoxAccumulator()
        val hadExistingData = BoxAccumulator().apply { add(ImmutableVec(10000F, 20000F)) }

        inProgressStroke.populateUpdatedRegion(previouslyEmpty)
        inProgressStroke.populateUpdatedRegion(hadExistingData)

        assertThat(hadExistingData).isEqualTo(previouslyEmpty)
    }

    @Test
    fun populateUpdatedRegion_afterResetRegion_returnsFalse() {
        val inProgressStroke = makeStartAndExtendStroke()
        inProgressStroke.resetUpdatedRegion()

        val envelope = BoxAccumulator().apply { add(ImmutableVec(10000F, 20000F)) }
        inProgressStroke.populateUpdatedRegion(envelope)

        assertThat(envelope.isEmpty()).isTrue()
    }

    @Test
    fun meshPartitionCount_isOne() {
        val stroke = makeStartAndExtendStroke()
        assertThat(stroke.getBrushCoatCount()).isEqualTo(1)
        assertThat(stroke.getMeshPartitionCount(0)).isEqualTo(1)
    }

    @Test
    fun getVertexCount_withEmptyStroke_returnsZero() {
        val stroke = InProgressStroke()
        stroke.start(makeBrush())
        assertThat(stroke.getBrushCoatCount()).isEqualTo(1)
        assertThat(stroke.getMeshPartitionCount(0)).isEqualTo(1)

        assertThat(stroke.getVertexCount(0, 0)).isEqualTo(0)
    }

    @Test
    fun getVertexCount_withStroke_returnsNonZero() {
        val stroke = makeStartAndExtendStroke()
        assertThat(stroke.getBrushCoatCount()).isEqualTo(1)
        assertThat(stroke.getMeshPartitionCount(0)).isEqualTo(1)

        assertThat(stroke.getVertexCount(0, 0)).isGreaterThan(0)
    }

    @Test
    fun getRawVertexBuffer_withEmptyStroke_returnsEmptyBuffer() {
        val stroke = InProgressStroke()
        stroke.start(makeBrush())
        assertThat(stroke.getBrushCoatCount()).isEqualTo(1)
        assertThat(stroke.getMeshPartitionCount(0)).isEqualTo(1)

        val vertexBuffer = stroke.getRawVertexBuffer(0, 0)

        assertThat(vertexBuffer.isReadOnly).isTrue()
        assertFailsWith<ReadOnlyBufferException> { vertexBuffer.put(5) }
        assertThat(vertexBuffer.limit()).isEqualTo(0)
        assertThat(vertexBuffer.capacity()).isEqualTo(0)
    }

    @Test
    fun getRawVertexBuffer_withStroke_returnsNonEmptyBuffer() {
        val stroke = makeStartAndExtendStroke()
        assertThat(stroke.getBrushCoatCount()).isEqualTo(1)
        assertThat(stroke.getMeshPartitionCount(0)).isEqualTo(1)

        val vertexBuffer = stroke.getRawVertexBuffer(0, 0)

        assertThat(vertexBuffer.isDirect).isTrue()
        assertThat(vertexBuffer.isReadOnly).isTrue()
        assertFailsWith<ReadOnlyBufferException> { vertexBuffer.put(5) }
        assertThat(vertexBuffer.limit()).isNotEqualTo(0)
        assertThat(vertexBuffer.capacity()).isNotEqualTo(0)
    }

    @Test
    fun getRawTriangleIndexBuffer_isNativeOrder() {
        val stroke = makeStartAndExtendStroke()
        val triangleIndexBuffer = stroke.getRawTriangleIndexBuffer(0, 0)
        assertThat(triangleIndexBuffer.order()).isEqualTo(java.nio.ByteOrder.nativeOrder())
    }

    @Test
    fun getRawTriangleIndexBuffer_withEmptyStroke_returnsEmptyBuffer() {
        val stroke = InProgressStroke()
        stroke.start(makeBrush())
        assertThat(stroke.getBrushCoatCount()).isEqualTo(1)
        assertThat(stroke.getMeshPartitionCount(0)).isEqualTo(1)

        val triangleIndexBuffer = stroke.getRawTriangleIndexBuffer(0, 0)
        assertThat(triangleIndexBuffer.isDirect).isTrue()
        assertThat(triangleIndexBuffer.isReadOnly).isTrue()
        // There aren't valid writes to make, so can't assert that this fails reads with
        // ReadOnlyBufferException. put() fails with BufferOverflowException first, clear doesn't
        // object to the no-op call.

        assertThat(triangleIndexBuffer.limit()).isEqualTo(0)
        assertThat(triangleIndexBuffer.capacity()).isEqualTo(0)
    }

    @Test
    fun getRawTriangleIndexBuffer_withStroke_returnsNonEmptyBuffer() {
        val stroke = makeStartAndExtendStroke()
        assertThat(stroke.getBrushCoatCount()).isEqualTo(1)
        assertThat(stroke.getMeshPartitionCount(0)).isEqualTo(1)

        val triangleIndexBuffer = stroke.getRawTriangleIndexBuffer(0, 0)
        assertThat(triangleIndexBuffer.isDirect).isTrue()
        assertThat(triangleIndexBuffer.isReadOnly).isTrue()
        assertFailsWith<ReadOnlyBufferException> { triangleIndexBuffer.put(5) }

        assertThat(triangleIndexBuffer.limit()).isNotEqualTo(0)
        assertThat(triangleIndexBuffer.capacity()).isNotEqualTo(0)
    }

    @Test
    fun getRawTriangleIndexBuffer_withIncreasingStrokeSize_eventuallyPartitionsBuffer() {
        val stroke = InProgressStroke()
        stroke.start(makeBrush())
        // The condition that this test is exercising is where a triangle index value would start
        // overflowing a ushort, which is related to the number of vertices in the stroke rather
        // than
        // the size of this buffer (triangle count * 3). In this test, the way we know that the
        // desired
        // condition has been met is when the triangle index buffer stops growing, and that is when
        // this
        // loop will end. The number of input points that will take is very dependent on the brush,
        // the
        // input points themselves, the extrusion/tessellation code, and possibly more factors, so a
        // fixed-length loop is not appropriate here. The test will fail if it crashes due to an
        // internal logic error or running out of memory to allocate more ShortBuffers.
        while (stroke.getMeshPartitionCount(0) <= 1) {
            // Draw the stroke as a spiral that gets bigger and bigger. Drawing a straight line
            // would take
            // longer to reach the goal because there would be fewer triangles.
            val inputsAdded = stroke.getInputCount()
            val spiralRadius = 100 * sqrt(inputsAdded.toFloat())
            val angle = inputsAdded.toFloat() % (2 * PI.toFloat())
            val x = spiralRadius * cos(angle)
            val y = spiralRadius * sin(angle)
            val time = inputsAdded.toLong()
            stroke.enqueueInputs(
                MutableStrokeInputBatch().add(StrokeInput.create(x, y, time)).toImmutable(),
                ImmutableStrokeInputBatch.EMPTY,
            )
            stroke.updateShape(time)
        }
        // Takes a while before the partition happens.
        assertThat(stroke.getInputCount()).isGreaterThan(1000)
        // At that point there's a long first partition and a shorter second one.
        assertThat(stroke.getMeshPartitionCount(0)).isEqualTo(2)
        assertThat(stroke.getRawTriangleIndexBuffer(0, 0).capacity())
            .isGreaterThan(stroke.getRawTriangleIndexBuffer(0, 1).capacity())
        assertThat(stroke.getRawVertexBuffer(0, 0).capacity())
            .isGreaterThan(stroke.getRawVertexBuffer(0, 1).capacity())
        // The dry stroke has all the inputs added.
        assertThat(stroke.toImmutable().inputs.size).isEqualTo(stroke.getInputCount())
    }

    @Test
    fun getMeshFormat_returnsFormat() {
        val stroke = makeStartAndExtendStroke()

        assertThat(stroke.getBrushCoatCount()).isEqualTo(1)
        assertThat(stroke.getMeshPartitionCount(0)).isEqualTo(1)
        assertThat(stroke.getMeshFormat(0)).isNotNull()
    }

    @Test
    fun getOutlineCount_whenEmptyStroke_shouldThrow() {
        val emptyStroke = InProgressStroke()

        assertThat(emptyStroke.getBrushCoatCount()).isEqualTo(0)
        assertFailsWith<IllegalArgumentException> { emptyStroke.getOutlineCount(0) }
    }

    @Test
    fun getOutlineVertexCount_whenEmptyStroke_shouldThrow() {
        val stroke = InProgressStroke()

        assertFailsWith<IllegalArgumentException> { stroke.getOutlineVertexCount(0, 0) }
    }

    @Test
    fun populateOutlinePosition_whenEmptyStroke_shouldThrow() {
        val stroke = InProgressStroke()
        stroke.start(makeBrush())

        assertThat(stroke.getBrushCoatCount()).isGreaterThan(0)
        assertFailsWith<IllegalArgumentException> {
            stroke.populateOutlinePosition(0, 0, 0, MutableVec())
        }
    }

    @Test
    fun populateOutlinePosition_withNonEmptyStroke_shouldBeWithinBounds() {
        val stroke = makeStartAndExtendStroke()

        assertThat(stroke.getBrushCoatCount()).isGreaterThan(0)
        assertThat(stroke.getOutlineCount(0)).isGreaterThan(0)
        assertThat(stroke.getOutlineVertexCount(0, 0)).isGreaterThan(0)

        val bounds = BoxAccumulator()
        stroke.populateMeshBounds(0, bounds)

        val p = MutableVec()
        for (outlineIndex in 0 until stroke.getOutlineCount(0)) {
            for (outlineVertexIndex in 0 until stroke.getOutlineVertexCount(0, outlineIndex)) {
                assertThat(stroke.populateOutlinePosition(0, outlineIndex, outlineVertexIndex, p))
                    .isSameInstanceAs(p)
                assertThat(p.x).isAtLeast(bounds.box!!.xMin)
                assertThat(p.y).isAtLeast(bounds.box!!.yMin)
                assertThat(p.x).isAtMost(bounds.box!!.xMax)
                assertThat(p.y).isAtMost(bounds.box!!.yMax)
            }
        }
    }

    @Test
    @Suppress("Range") // Testing behavior when index is out of range.
    fun populateOutlinePosition_whenBadIndex_shouldThrow() {
        val stroke = makeStartAndExtendStroke()

        val p = MutableVec()
        assertFailsWith<IllegalArgumentException> { (stroke.populateOutlinePosition(-1, 0, 0, p)) }
        assertFailsWith<IllegalArgumentException> {
            (stroke.populateOutlinePosition(stroke.getBrushCoatCount() + 1, 0, 0, p))
        }
        assertFailsWith<IllegalArgumentException> { (stroke.populateOutlinePosition(0, -1, 0, p)) }
        assertFailsWith<IllegalArgumentException> {
            (stroke.populateOutlinePosition(0, stroke.getOutlineCount(0) + 1, 0, p))
        }
        assertFailsWith<IllegalArgumentException> { (stroke.populateOutlinePosition(0, 0, -1, p)) }
        assertFailsWith<IllegalArgumentException> {
            (stroke.populateOutlinePosition(0, 0, stroke.getOutlineVertexCount(0, 0) + 1, p))
        }
    }

    @Test
    fun populatePosition_shouldBeWithinBounds() {
        val stroke = makeStartAndExtendStroke()

        assertThat(stroke.getBrushCoatCount()).isGreaterThan(0)
        val bounds = BoxAccumulator()
        stroke.populateMeshBounds(0, bounds)

        val p = MutableVec()
        for (coatIndex in 0 until stroke.getBrushCoatCount()) {
            for (partitionIndex in 0 until stroke.getMeshPartitionCount(coatIndex)) {
                for (vertexIndex in 0 until stroke.getVertexCount(coatIndex, partitionIndex)) {
                    assertThat(stroke.populatePosition(coatIndex, partitionIndex, vertexIndex, p))
                        .isSameInstanceAs(p)
                    assertThat(p.x).isAtLeast(bounds.box!!.xMin)
                    assertThat(p.y).isAtLeast(bounds.box!!.yMin)
                    assertThat(p.x).isAtMost(bounds.box!!.xMax)
                    assertThat(p.y).isAtMost(bounds.box!!.yMax)
                }
            }
        }
    }

    @Test
    @Suppress("Range")
    fun populatePosition_whenBadIndex_shouldThrow() {
        val stroke = makeStartAndExtendStroke()

        val p = MutableVec()
        assertFailsWith<IllegalArgumentException> { (stroke.populatePosition(-1, 0, 0, p)) }
        assertFailsWith<IllegalArgumentException> {
            (stroke.populatePosition(stroke.getBrushCoatCount() + 1, 0, 0, p))
        }
        assertFailsWith<IllegalArgumentException> { (stroke.populatePosition(0, -1, 0, p)) }
        assertFailsWith<IllegalArgumentException> {
            (stroke.populatePosition(0, stroke.getMeshPartitionCount(0) + 1, 0, p))
        }
        assertFailsWith<IllegalArgumentException> { (stroke.populatePosition(0, 0, -1, p)) }
        assertFailsWith<IllegalArgumentException> {
            (stroke.populatePosition(0, 0, stroke.getVertexCount(0, 0) + 1, p))
        }
    }

    private fun makeBrush() = Brush(family = StockBrushes.marker(), size = 10f, epsilon = 0.1f)

    @OptIn(ExperimentalInkCustomBrushApi::class)
    private fun makeTimeSinceInputBrush(
        timeSinceInputStartMillis: Float = 0F,
        timeSinceInputEndMillis: Float = 1000F,
    ) =
        Brush(
            BrushFamily(
                BrushTip(
                    behaviors =
                        listOf(
                            BrushBehavior(
                                BrushBehavior.TargetNode(
                                    target = BrushBehavior.Target.CORNER_ROUNDING_OFFSET,
                                    targetModifierRangeStart = 0F,
                                    targetModifierRangeEnd = 1F,
                                    input =
                                        BrushBehavior.SourceNode(
                                            source =
                                                BrushBehavior.Source.TIME_SINCE_INPUT_IN_SECONDS,
                                            sourceValueRangeStart =
                                                timeSinceInputStartMillis / 1000f,
                                            sourceValueRangeEnd = timeSinceInputEndMillis / 1000f,
                                        ),
                                )
                            )
                        )
                )
            ),
            size = 10F,
            epsilon = 0.1F,
        )
}
