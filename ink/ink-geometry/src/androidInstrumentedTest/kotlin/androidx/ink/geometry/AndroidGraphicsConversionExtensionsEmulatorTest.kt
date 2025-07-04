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

import android.graphics.Path
import android.os.Build
import androidx.graphics.path.PathIterator
import androidx.graphics.path.PathSegment
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.testing.buildStrokeInputBatchFromPoints
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
class AndroidGraphicsConversionExtensionsEmulatorTest {
    @Test
    fun outlinesToPath_returnsCorrectListOfpath() {
        val partitionedMesh = buildTestStrokeShape()
        val path = partitionedMesh.outlinesToPath(0)
        assertThat(path.isEmpty).isFalse()

        val outlineVertexCount = partitionedMesh.getOutlineVertexCount(0, 0)
        val pathIterator = PathIterator(path)
        for (outlineIndex in 0 until outlineVertexCount) {
            val point = MutableVec()
            // PathIterator can return up to 8 values for arbitrary Paths, but we only expect the
            // contents of the first 4 values.
            val points = FloatArray(8)

            partitionedMesh.populateOutlinePosition(0, 0, outlineIndex, point)

            val type = pathIterator.next(points, 0)
            assertThat(type)
                .isEqualTo(if (outlineIndex == 0) PathSegment.Type.Move else PathSegment.Type.Line)
            val xIndex = if (outlineIndex == 0) 0 else 2
            val yIndex = if (outlineIndex == 0) 1 else 3
            assertThat(point.x).isEqualTo(points[xIndex])
            assertThat(point.y).isEqualTo(points[yIndex])
        }
    }

    @Test
    fun populatePathFromOutlines_returnsCorrectPath() {
        val partitionedMesh = buildTestStrokeShape()
        val path = Path()
        partitionedMesh.populateOutlines(0, path)
        assertThat(path.isEmpty).isFalse()

        val outlineVertexCount = partitionedMesh.getOutlineVertexCount(0, 0)
        val pathIterator = PathIterator(path)
        for (outlineIndex in 0 until outlineVertexCount) {
            val point = MutableVec()
            // PathIterator can return up to 8 values for arbitrary Paths, but we only expect the
            // contents of the first 4 values.
            val points = FloatArray(8)

            partitionedMesh.populateOutlinePosition(0, 0, outlineIndex, point)

            val type = pathIterator.next(points, 0)
            assertThat(type)
                .isEqualTo(if (outlineIndex == 0) PathSegment.Type.Move else PathSegment.Type.Line)
            val xIndex = if (outlineIndex == 0) 0 else 2
            val yIndex = if (outlineIndex == 0) 1 else 3
            assertThat(point.x).isEqualTo(points[xIndex])
            assertThat(point.y).isEqualTo(points[yIndex])
        }
    }

    private fun buildTestStrokeShape(): PartitionedMesh {
        return Stroke(
                TEST_BRUSH,
                buildStrokeInputBatchFromPoints(floatArrayOf(10f, 3f, 20f, 5f)).asImmutable(),
            )
            .shape
    }

    companion object {
        private const val A = 1f
        private const val B = 2f
        private const val C = -3f
        private const val D = -4f
        private const val E = 5f
        private const val F = 6f

        private val TEST_BRUSH =
            Brush(family = StockBrushes.markerLatest, size = 10f, epsilon = 0.1f)
    }
}
