/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.ink.storage

import androidx.ink.brush.Brush
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.InputToolType
import androidx.ink.geometry.MutableVec
import androidx.ink.geometry.PartitionedMesh
import androidx.ink.strokes.ExperimentalInkEraserApi
import androidx.ink.strokes.ImmutableStrokeInputBatch
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import androidx.kruth.assertThat
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertFailsWith
import okio.Buffer
import okio.GzipSink
import okio.buffer
import okio.use

@OptIn(ExperimentalEncodingApi::class, ExperimentalInkEraserApi::class)
class PartitionedMeshExtensionsTest {
    private val testBrush = Brush(family = BrushFamily(), size = 10f, epsilon = 0.1f)

    private val testBatch =
        MutableStrokeInputBatch()
            .add(
                StrokeInput.create(
                    toolType = InputToolType.STYLUS,
                    x = 2.0f,
                    y = 3.0f,
                    elapsedTimeMillis = 2,
                    pressure = 0.1f,
                    tiltRadians = 0.2f,
                    orientationRadians = 0.3f,
                )
            )
            .add(
                StrokeInput.create(
                    toolType = InputToolType.STYLUS,
                    x = 9.0f,
                    y = 1.0f,
                    elapsedTimeMillis = 4,
                    pressure = 0.7f,
                    tiltRadians = 0.8f,
                    orientationRadians = 0.9f,
                )
            )
            .toImmutable()

    private val testShape = Stroke(testBrush, testBatch).shape
    private val emptyShape = Stroke(testBrush, ImmutableStrokeInputBatch.EMPTY).shape

    private val compressedInvalidCodedModeledShapeBytes =
        // Using hard-coded examples to avoid a proto dependency for these tests. This string is the
        // base64 encoding of a gzipped ink.proto.CodedModeledShape binary-proto:
        /*
        group_first_mesh_indices: 0
        */
        // Which has `group_first_mesh_indices` (size 1) not matching `group_formats` (size 0).
        Base64.Default.decode("H4sIAAAAAAAA/9NiZAAAZSuA0wMAAAA=")

    private val compressedNotAProtoBytes = run {
        val buffer = Buffer()
        GzipSink(buffer).buffer().use { it.write(byteArrayOf(1)) }
        buffer.readByteArray()
    }

    private val notGzipCompressedBytes = byteArrayOf(1)

    private fun assertPartitionedMeshesAreNearEqual(
        actual: PartitionedMesh,
        expected: PartitionedMesh,
        tolerance: Float = 0.01f,
    ) {
        assertThat(actual.getRenderGroupCount()).isEqualTo(expected.getRenderGroupCount())
        val pActual = MutableVec()
        val pExpected = MutableVec()
        for (groupIndex in 0 until expected.getRenderGroupCount()) {
            assertThat(actual.getOutlineCount(groupIndex))
                .isEqualTo(expected.getOutlineCount(groupIndex))
            for (outlineIndex in 0 until expected.getOutlineCount(groupIndex)) {
                val outlineVertexCount = expected.getOutlineVertexCount(groupIndex, outlineIndex)
                assertThat(actual.getOutlineVertexCount(groupIndex, outlineIndex))
                    .isEqualTo(outlineVertexCount)
                for (outlineVertexIndex in 0 until outlineVertexCount) {
                    actual.populateOutlinePosition(
                        groupIndex,
                        outlineIndex,
                        outlineVertexIndex,
                        pActual,
                    )
                    expected.populateOutlinePosition(
                        groupIndex,
                        outlineIndex,
                        outlineVertexIndex,
                        pExpected,
                    )
                    assertThat(abs(pActual.x - pExpected.x)).isLessThan(tolerance)
                    assertThat(abs(pActual.y - pExpected.y)).isLessThan(tolerance)
                }
            }
        }
    }

    @Test
    fun decode_invalidProto_throwsException() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                @Suppress("CheckReturnValue")
                PartitionedMesh.decode(compressedInvalidCodedModeledShapeBytes)
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("`CodedModeledShape.group_*` fields must all be the same size")
    }

    @Test
    fun decode_gzippedNotAProto_throwsException() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                @Suppress("CheckReturnValue") PartitionedMesh.decode(compressedNotAProtoBytes)
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("Failed to parse ink.proto.CodedModeledShape")
    }

    @Test
    fun decode_notGzipped_throwsException() {
        assertFailsWith<IOException> {
            @Suppress("CheckReturnValue") PartitionedMesh.decode(notGzipCompressedBytes)
        }
    }

    @Test
    fun encode_decode_roundTrip() {
        assertPartitionedMeshesAreNearEqual(PartitionedMesh.decode(testShape.encode()), testShape)
    }

    @Test
    fun encode_decode_emptyShape_roundTrip() {
        assertPartitionedMeshesAreNearEqual(PartitionedMesh.decode(emptyShape.encode()), emptyShape)
    }
}
