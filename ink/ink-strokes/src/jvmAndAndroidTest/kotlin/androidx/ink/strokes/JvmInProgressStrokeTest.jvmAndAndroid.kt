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
import androidx.ink.brush.StockBrushes
import androidx.ink.strokes.testing.buildStrokeInputBatchFromPoints
import androidx.kruth.assertThat
import java.nio.ByteOrder
import java.nio.ReadOnlyBufferException
import java.util.WeakHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertFailsWith

/** Unit tests for JVM-specific extensions to [InProgressStroke]. */
class JvmInProgressStrokeTest {

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
    fun rawVertexData_retainsWeakReferenceToInProgressStrokeFromOriginalDirectBuffer() {
        val stroke = InProgressStroke()
        stroke.start(makeBrush())
        val unused = stroke.getRawVertexBuffer(0, 0)
        assertThat(inProgressStrokesReferencedByBuffers).isInstanceOf<WeakHashMap<*, *>>()
        // Unfortunately, we need to map from the _original_ direct buffer to the InProgressStroke,
        // not
        // the wrapped buffer that's ultimately returned by this getter. The internals of
        // DirectByteBuffer ensure that methods which slice or duplicate the buffer retain a
        // reference
        // to the original buffer, but not any intermediate copies. So retaining a weak reference to
        // the
        // original ensures that any further copies/slices also do the right thing. But this
        // reference
        // back to the original buffer isn't in the public API, so we can't assert about it.
        assertThat(inProgressStrokesReferencedByBuffers.values).contains(stroke)
        val reversedMap =
            inProgressStrokesReferencedByBuffers.entries.associate { (k, v) -> v to k }
        val originalDirectBuffer = reversedMap[stroke]!!
        assertThat(originalDirectBuffer.isDirect()).isTrue()
    }

    @Test
    fun rawIndexData_retainsWeakReferenceToInProgressStrokeFromOriginalDirectBuffer() {
        val stroke = InProgressStroke()
        stroke.start(makeBrush())
        val unused = stroke.getRawTriangleIndexBuffer(0, 0)
        assertThat(inProgressStrokesReferencedByBuffers).isInstanceOf<WeakHashMap<*, *>>()
        // See comment above about why this entry maps to the original direct buffer, not the
        // wrapped
        // one.
        assertThat(inProgressStrokesReferencedByBuffers.values).contains(stroke)
        val reversedMap =
            inProgressStrokesReferencedByBuffers.entries.associate { (k, v) -> v to k }
        val originalDirectBuffer = reversedMap[stroke]!!
        assertThat(originalDirectBuffer.isDirect()).isTrue()
    }

    private fun makeBrush() = Brush(family = StockBrushes.marker(), size = 10f, epsilon = 0.1f)
}
