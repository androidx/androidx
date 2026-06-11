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

import androidx.ink.brush.InputToolType
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.StrokeInput
import androidx.ink.strokes.StrokeInputBatch
import androidx.kruth.assertThat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.test.Test

/**
 * Tests for parts of [StrokeInputBatch] serialization and deserialization logic with JVM-specific
 * interfaces. (Specifically, the Java-specific static API and the interfaces which use
 * InputStream/OutputStream instead of ByteArray.) These compose over a common implementation, so
 * this just covers basic plumbing, the tests for detailed error behavior are in the KMP-common
 * tests.
 */
class JvmStrokeInputBatchExtensionsTest {
    private fun assertBatchesAreNearEqual(
        batch1: StrokeInputBatch,
        batch2: StrokeInputBatch,
        tolerance: Float = 0.001f,
    ) {
        assertThat(batch1.size).isEqualTo(batch2.size)
        val s1 = StrokeInput()
        val s2 = StrokeInput()
        for (i in 0 until batch2.size) {
            batch1.populate(i, s1)
            batch2.populate(i, s2)
            assertThat(s1.elapsedTimeMillis).isEqualTo(s2.elapsedTimeMillis)
            assertThat(abs(s1.x - s2.x)).isLessThan(tolerance)
            assertThat(abs(s1.y - s2.y)).isLessThan(tolerance)
            assertThat(s1.toolType).isEqualTo(s2.toolType)
            assertThat(abs(s1.pressure - s2.pressure)).isLessThan(tolerance)
            assertThat(abs(s1.tiltRadians - s2.tiltRadians)).isLessThan(tolerance)
            assertThat(abs(s1.orientationRadians - s2.orientationRadians)).isLessThan(tolerance)
        }
    }

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

    @Test
    fun decodeAllFormatsFromStrokeInputBatch_iostreamApi_roundTrip() {
        val encodedInputs =
            ByteArrayOutputStream().use {
                testBatch.encode(it)
                it.toByteArray()
            }
        assertBatchesAreNearEqual(
            ByteArrayInputStream(encodedInputs).use(StrokeInputBatch::decode),
            testBatch,
        )
    }

    @Test
    fun decodeAllFormatsFromStrokeInputBatch_iostreamStaticApi_roundTrip() {
        // Kotlin clients should prefer the extension methods, but the static wrappers do work.
        val encodedInputs =
            ByteArrayOutputStream()
                .apply { use { StrokeInputBatchSerialization.encode(testBatch, it) } }
                .toByteArray()
        assertBatchesAreNearEqual(
            ByteArrayInputStream(encodedInputs).use(StrokeInputBatchSerialization::decode),
            testBatch,
        )
    }

    @Test
    fun decodeAllFormatsFromStrokeInputBatch_byteArrayStaticApi_roundTrip() {
        // Kotlin clients should prefer the extension methods, but the static wrappers do work.
        val encodedInputs = StrokeInputBatchSerialization.encode(testBatch)
        assertBatchesAreNearEqual(StrokeInputBatchSerialization.decode(encodedInputs), testBatch)
    }
}
