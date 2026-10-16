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
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import androidx.kruth.assertThat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.test.Test

/**
 * Tests for parts of [PartitionedMesh] serialization and deserialization logic with JVM-specific
 * interfaces. (Specifically, the Java-specific static API and the interfaces which use
 * InputStream/OutputStream instead of ByteArray.) These compose over a common implementation, so
 * this just covers basic plumbing, the tests for detailed error behavior are in the KMP-common
 * tests.
 */
@OptIn(ExperimentalInkEraserApi::class)
class JvmPartitionedMeshExtensionsTest {
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

    @Test
    fun encode_decode_iostreamApi_roundTrip() {
        val encodedShape =
            ByteArrayOutputStream().use {
                testShape.encode(it)
                it.toByteArray()
            }
        assertPartitionedMeshesAreNearEqual(
            ByteArrayInputStream(encodedShape).use(PartitionedMesh::decode),
            testShape,
        )
    }

    @Test
    fun encode_decode_iostreamStaticApi_roundTrip() {
        // Kotlin clients should prefer the extension methods, but the static wrappers do work.
        val encodedShape =
            ByteArrayOutputStream()
                .apply { use { PartitionedMeshSerialization.encode(testShape, it) } }
                .toByteArray()
        assertPartitionedMeshesAreNearEqual(
            ByteArrayInputStream(encodedShape).use(PartitionedMeshSerialization::decode),
            testShape,
        )
    }

    @Test
    fun encode_decode_byteArrayStaticApi_roundTrip() {
        // Kotlin clients should prefer the extension methods, but the static wrappers do work.
        val encodedShape = PartitionedMeshSerialization.encode(testShape)
        assertPartitionedMeshesAreNearEqual(
            PartitionedMeshSerialization.decode(encodedShape),
            testShape,
        )
    }
}
