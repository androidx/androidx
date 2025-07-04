/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.ink.geometry.compose

import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.ink.geometry.BoxAccumulator
import androidx.ink.geometry.ImmutableAffineTransform
import androidx.ink.geometry.ImmutableBox
import androidx.ink.geometry.ImmutableVec
import androidx.ink.geometry.MutableAffineTransform
import androidx.ink.geometry.MutableBox
import androidx.ink.geometry.MutableVec
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ComposeConversionExtensionsTest {

    @Test
    fun toOffset_resultingOffsetIsEquivalent() {
        assertThat(ImmutableVec(x = 1f, y = 2f).toOffset()).isEqualTo(Offset(x = 1f, y = 2f))
    }

    @Test
    fun from_resultingVecIsEquivalentToOffset() {
        assertThat(ImmutableVec.from(Offset(x = 1f, y = 2f)))
            .isEqualTo(ImmutableVec(x = 1f, y = 2f))
    }

    @Test
    fun populateFrom_resultingVecIsEquivalentToOffset() {
        val offset = Offset(1f, 2f)
        val point = MutableVec()
        assertThat(point.populateFrom(offset)).isSameInstanceAs(point)

        assertThat(point).isEqualTo(ImmutableVec(x = 1f, y = 2f))
    }

    @Test
    fun toRect_resultingRectIsEquivalent() {
        val box = ImmutableBox.fromTwoPoints(ImmutableVec(1f, 2f), ImmutableVec(3f, 4f))
        val rect = box.toRect()

        assertThat(rect.left).isEqualTo(box.xMin)
        assertThat(rect.top).isEqualTo(box.yMin)
        assertThat(rect.right).isEqualTo(box.xMax)
        assertThat(rect.bottom).isEqualTo(box.yMax)
    }

    @Test
    fun populateMutableRect_resultingRectIsEquivalent() {
        val box = ImmutableBox.fromTwoPoints(ImmutableVec(1f, 2f), ImmutableVec(3f, 4f))
        val rect = MutableRect(0f, 0f, 0f, 0f)
        assertThat(box.populateMutableRect(rect)).isSameInstanceAs(rect)

        assertThat(rect.left).isEqualTo(box.xMin)
        assertThat(rect.top).isEqualTo(box.yMin)
        assertThat(rect.right).isEqualTo(box.xMax)
        assertThat(rect.bottom).isEqualTo(box.yMax)
    }

    @Test
    fun from_Rect_resultingBoxIsEquivalentToRect() {
        val rect = Rect(1f, 2f, 3f, 4f)
        val box = ImmutableBox.from(rect)

        assertThat(box?.xMin).isEqualTo(rect.left)
        assertThat(box?.yMin).isEqualTo(rect.top)
        assertThat(box?.xMax).isEqualTo(rect.right)
        assertThat(box?.yMax).isEqualTo(rect.bottom)
    }

    @Test
    fun from_resultIsNullForEmptyRect() {
        val rect = Rect(0f, 0f, 0f, 0f)
        val box = ImmutableBox.from(rect)

        assertThat(box).isNull()
    }

    @Test
    fun from_MutableRect_resultingBoxIsEquivalentToRect() {
        val rect = MutableRect(1f, 2f, 3f, 4f)
        val box = ImmutableBox.from(rect)

        assertThat(box?.xMin).isEqualTo(rect.left)
        assertThat(box?.yMin).isEqualTo(rect.top)
        assertThat(box?.xMax).isEqualTo(rect.right)
        assertThat(box?.yMax).isEqualTo(rect.bottom)
    }

    @Test
    fun from_resultIsNullForEmptyMutableRect() {
        val rect = MutableRect(0f, 0f, 0f, 0f)
        val box = ImmutableBox.from(rect)

        assertThat(box).isNull()
    }

    @Test
    fun boxAccumulatorAdd_resultingBoxIsEquivalentToRect() {
        val rect = Rect(1f, 2f, 3f, 4f)
        val boxAccumulator = BoxAccumulator()
        boxAccumulator.add(rect)
        val box = boxAccumulator.box
        assertThat(box).isNotNull()

        assertThat(box?.xMin).isEqualTo(rect.left)
        assertThat(box?.yMin).isEqualTo(rect.top)
        assertThat(box?.xMax).isEqualTo(rect.right)
        assertThat(box?.yMax).isEqualTo(rect.bottom)
    }

    @Test
    fun boxAccumulatorAdd_existingBoxIsExtendedByRect() {
        val boxAccumulator = BoxAccumulator()
        // Add with a non-empty BoxAccumulator
        boxAccumulator.add(ImmutableBox.fromTwoPoints(ImmutableVec(2f, 3f), ImmutableVec(4f, 5f)))
        val box = boxAccumulator.box
        assertThat(box).isNotNull()
        assertThat(box?.xMin).isEqualTo(2f)
        assertThat(box?.yMin).isEqualTo(3f)
        assertThat(box?.xMax).isEqualTo(4f)
        assertThat(box?.yMax).isEqualTo(5f)

        val rect = Rect(1f, 2f, 3f, 4f)
        boxAccumulator.add(rect)
        assertThat(box).isNotNull()

        assertThat(box?.xMin).isEqualTo(rect.left)
        assertThat(box?.yMin).isEqualTo(rect.top)
        assertThat(box?.xMax).isEqualTo(4f)
        assertThat(box?.yMax).isEqualTo(5f)
    }

    @Test
    fun boxAccumulatorAdd_resultingBoxIsUnchangedForEmptyRect() {
        // Empty Rect
        val rect = Rect(5f, 8f, 2f, 1f)
        val startingBox = MutableBox().setXBounds(1F, 2F).setYBounds(3F, 4F)
        val boxAccumulator = BoxAccumulator(startingBox)
        boxAccumulator.add(rect)
        val box = boxAccumulator.box
        assertThat(box).isEqualTo(startingBox)
    }

    @Test
    fun boxAccumulatorAdd_resultingBoxIsEquivalentToMutableRect() {
        val rect = MutableRect(1f, 2f, 3f, 4f)
        val boxAccumulator = BoxAccumulator()
        boxAccumulator.add(rect)
        val box = boxAccumulator.box
        assertThat(box).isNotNull()

        assertThat(box?.xMin).isEqualTo(rect.left)
        assertThat(box?.yMin).isEqualTo(rect.top)
        assertThat(box?.xMax).isEqualTo(rect.right)
        assertThat(box?.yMax).isEqualTo(rect.bottom)
    }

    @Test
    fun boxAccumulatorAdd_existingBoxIsExtendedByMutableRect() {
        val boxAccumulator = BoxAccumulator()
        // Add with a non-empty BoxAccumulator
        boxAccumulator.add(ImmutableBox.fromTwoPoints(ImmutableVec(2f, 3f), ImmutableVec(4f, 5f)))
        val box = boxAccumulator.box
        assertThat(box).isNotNull()
        assertThat(box?.xMin).isEqualTo(2f)
        assertThat(box?.yMin).isEqualTo(3f)
        assertThat(box?.xMax).isEqualTo(4f)
        assertThat(box?.yMax).isEqualTo(5f)

        val rect = MutableRect(1f, 2f, 3f, 4f)
        boxAccumulator.add(rect)
        assertThat(box).isNotNull()

        assertThat(box?.xMin).isEqualTo(rect.left)
        assertThat(box?.yMin).isEqualTo(rect.top)
        assertThat(box?.xMax).isEqualTo(4f)
        assertThat(box?.yMax).isEqualTo(5f)
    }

    @Test
    fun boxAccumulatorAdd_resultingBoxIsUnchangedForEmptyMutableRect() {
        // Empty MutableRect
        val rect = MutableRect(5f, 8f, 2f, 1f)
        val startingBox = MutableBox().setXBounds(1F, 2F).setYBounds(3F, 4F)
        val boxAccumulator = BoxAccumulator(startingBox)
        boxAccumulator.add(rect)
        val box = boxAccumulator.box
        assertThat(box).isEqualTo(startingBox)
    }

    @Test
    fun populateMatrix_resultsInEquivalentVecTransformations() {
        val affineTransform = ImmutableAffineTransform(A, B, C, D, E, F)

        // First, apply the affineTransform to an ink Vec for later comparison.
        val inputVec = ImmutableVec(1f, 2f)
        val outputVec = MutableVec()
        affineTransform.applyTransform(inputVec, outputVec)

        // Then, populate an android.graphics.Matrix from the affineTransform and perform the
        // equivalent
        // operation.
        val matrix = Matrix()

        // There's a bit of complication here: The mutated Matrix instance is in fact returned, but
        // this
        // Matrix is an inline value class. If you pass it to a method that takes Any like
        // isSameInstanceAs, it winds up wrapped, and thus is no longer the same instance as far as
        // the
        // JVM can tell. The underlying values field is a mutable object, though, so we can check
        // that
        // instead.
        assertThat(matrix.values).isInstanceOf(FloatArray::class.java)
        assertThat(affineTransform.populateMatrix(matrix).values).isSameInstanceAs(matrix.values)
        val outputOffset = matrix.map(Offset(inputVec.x, inputVec.y))

        assertThat(ImmutableVec(outputOffset.x, outputOffset.y)).isEqualTo(outputVec)
    }

    @Test
    fun toMatrix_resultsInEquivalentVecTransformations() {
        val affineTransform = ImmutableAffineTransform(A, B, C, D, E, F)

        // First, apply the affineTransform to an ink Vec for later comparison.
        val inputVec = ImmutableVec(1f, 2f)
        val outputVec = MutableVec()
        affineTransform.applyTransform(inputVec, outputVec)

        // Then, populate a androidx.compose.ui.graphics.from the affineTransform and perform the
        // equivalent operation.
        val matrix = affineTransform.toMatrix()
        affineTransform.populateMatrix(matrix)
        val outputOffset = matrix.map(Offset(inputVec.x, inputVec.y))

        assertThat(ImmutableVec(outputOffset.x, outputOffset.y)).isEqualTo(outputVec)
    }

    @Test
    fun ImmutableAffineTransform_from_resultsInEquivalentVecTransformations() {
        val matrix = Matrix(floatArrayOf(A, D, 0f, 0f, B, E, 0f, 0f, 0f, 0f, 1f, 0f, C, F, 0f, 1f))
        // First, apply the matrix to the values of an ink Vec for later comparison.
        val inputVec = ImmutableVec(1f, 2f)
        val outputOffset = matrix.map(Offset(inputVec.x, inputVec.y))

        // Then, populate an ImmutableAffineTransform from the matrix and perform the equivalent
        // operation.
        val affineTransform = ImmutableAffineTransform.from(matrix)
        val outputVec = MutableVec()
        affineTransform?.applyTransform(inputVec, outputVec)

        assertThat(outputVec).isEqualTo(ImmutableVec(outputOffset.x, outputOffset.y))
    }

    @Test
    fun MutableAffineTransform_populateFrom_resultsInEquivalentVecTransformations() {
        val matrix = Matrix(floatArrayOf(A, D, 0f, 0f, B, E, 0f, 0f, 0f, 0f, 1f, 0f, C, F, 0f, 1f))
        // First, apply the matrix to the values of an ink Vec for later comparison.
        val inputVec = ImmutableVec(1f, 2f)
        val outputOffset = matrix.map(Offset(inputVec.x, inputVec.y))

        // Then, populate a MutableAffineTransform from the matrix and perform the equivalent
        // operation.
        val affineTransform = MutableAffineTransform()
        affineTransform.populateFrom(matrix)
        val outputVec = MutableVec()
        affineTransform.applyTransform(inputVec, outputVec)

        assertThat(outputVec).isEqualTo(ImmutableVec(outputOffset.x, outputOffset.y))
    }

    @Test
    fun MutableAffineTransform_populateFrom_throwsForNonAffineMatrix() {
        val matrix = Matrix(floatArrayOf(A, D, 0f, 6f, B, E, 0f, 0f, 0f, 0f, 1f, 0f, C, F, 0f, 1f))
        val affineTransform = MutableAffineTransform()
        assertFailsWith<IllegalArgumentException> { affineTransform.populateFrom(matrix) }
    }

    companion object {
        private const val A = 1.2f
        private const val B = 2f
        private const val C = -3f
        private const val D = -4f
        private const val E = 5f
        private const val F = 6f
    }
}
