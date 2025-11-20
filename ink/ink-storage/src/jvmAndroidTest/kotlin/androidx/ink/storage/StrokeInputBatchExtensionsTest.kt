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

package androidx.ink.storage

import androidx.ink.brush.InputToolType
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.StrokeInput
import androidx.ink.strokes.StrokeInputBatch
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Base64
import java.util.zip.GZIPOutputStream
import kotlin.math.abs
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StrokeInputBatchExtensionsTest {
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

    private val compressedInvalidCodedStrokeInputBatchBytes =
        // Using hard-coded examples to avoid a proto dependency for these tests. This string is the
        // base64 encoding of a gzipped ink.proto.CodedNumericRun binary-proto:
        // https://github.com/google/ink/blob/bf387a/ink/storage/proto/coded.proto#L35
        /*
        elapsed_time_seconds {
          deltas: 0
          deltas: 0
        }
        x_stroke_space {
          deltas: 0
        }
        y_stroke_space {
          deltas: 0
        }
        */
        // Which has more times encoded than positions.
        Base64.getDecoder().decode("H4sIAAAAAAAA/+Ni5mJkEAIRUixcTAwMACBnCXkQAAAA")

    private val compressedNotAProtoBytes =
        ByteArrayOutputStream().use { byteArrayStream ->
            GZIPOutputStream(byteArrayStream).use { gzipOutputStream ->
                gzipOutputStream.write(byteArrayOf(1))
            }
            byteArrayStream.toByteArray()
        }

    private val notGzipCompressedBytes = byteArrayOf(1)

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

    @Test
    fun decode_invalidProto_throwsException() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                ByteArrayInputStream(compressedInvalidCodedStrokeInputBatchBytes).use {
                    @Suppress("CheckReturnValue") StrokeInputBatch.decode(it)
                }
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("invalid StrokeInputBatch: mismatched numeric run lengths")
    }

    @Test
    fun decode_gzippedNotAProto_throwsException() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                ByteArrayInputStream(compressedNotAProtoBytes).use {
                    @Suppress("CheckReturnValue") StrokeInputBatch.decode(it)
                }
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("Failed to parse ink.proto.CodedStrokeInputBatch")
    }

    @Test
    fun decode_notGzipped_throwsException() {
        assertFailsWith<IOException> {
            ByteArrayInputStream(notGzipCompressedBytes).use {
                @Suppress("CheckReturnValue") StrokeInputBatch.decode(it)
            }
        }
    }

    @Test
    fun staticDecode_invalidProto_throwsException() {
        // Not the preferred way to call from Kotlin, but it does work.
        val exception =
            assertFailsWith<IllegalArgumentException> {
                ByteArrayInputStream(compressedInvalidCodedStrokeInputBatchBytes).use {
                    @Suppress("CheckReturnValue") StrokeInputBatchSerialization.decode(it)
                }
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("invalid StrokeInputBatch: mismatched numeric run lengths")
    }

    @Test
    fun staticDecode_invalidBytes_throwsException() {
        // Not the preferred way to call from Kotlin, but it does work.
        val exception =
            assertFailsWith<IllegalArgumentException> {
                ByteArrayInputStream(compressedNotAProtoBytes).use {
                    @Suppress("CheckReturnValue") StrokeInputBatchSerialization.decode(it)
                }
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("Failed to parse ink.proto.CodedStrokeInputBatch")
    }

    @Test
    fun decodeAllFormatsFromStrokeInputBatch_roundTrip() {
        val encodedInputs =
            ByteArrayOutputStream().use {
                testBatch.encode(it)
                it.toByteArray()
            }
        assertBatchesAreNearEqual(
            ByteArrayInputStream(encodedInputs).use(StrokeInputBatch::decode),
            testBatch,
        )
        // Kotlin clients should prefer the extension methods, but the static wrappers do work.
        assertBatchesAreNearEqual(
            ByteArrayInputStream(encodedInputs).use(StrokeInputBatchSerialization::decode),
            testBatch,
        )
    }
}
