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

package androidx.ink.storage

import androidx.ink.brush.InputToolType
import androidx.ink.strokes.ImmutableStrokeInputBatch
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.StrokeInput
import androidx.ink.strokes.StrokeInputBatch
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Base64
import kotlin.math.abs
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StrokeInputBatchExtensionsTest {
    private val testBatch =
        MutableStrokeInputBatch()
            .addOrThrow(
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
            .addOrThrow(
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
            .asImmutable()

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
    fun decodeOrThrow_invalid_throwsException() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                ByteArrayInputStream(compressedInvalidCodedStrokeInputBatchBytes).use {
                    @Suppress("CheckReturnValue") StrokeInputBatch.decodeOrThrow(it)
                }
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("invalid StrokeInputBatch: mismatched numeric run lengths")
    }

    @Test
    fun decodeOrNull_invalid_returnsNull() {
        assertThat(
                ByteArrayInputStream(compressedInvalidCodedStrokeInputBatchBytes).use {
                    val result: ImmutableStrokeInputBatch? = StrokeInputBatch.decodeOrNull(it)
                    result
                }
            )
            .isNull()
    }

    @Test
    fun staticDecodeOrThrow_invalid_throwsException() {
        // Not the preferred way to call from Kotlin, but it does work.
        val exception =
            assertFailsWith<IllegalArgumentException> {
                ByteArrayInputStream(compressedInvalidCodedStrokeInputBatchBytes).use {
                    @Suppress("CheckReturnValue") decodeOrThrow(it)
                }
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("invalid StrokeInputBatch: mismatched numeric run lengths")
    }

    @Test
    fun staticDecodeOrNull_invalid_returnsNull() {
        // Not the preferred way to call from Kotlin, but it does work.
        assertThat(
                ByteArrayInputStream(compressedInvalidCodedStrokeInputBatchBytes).use {
                    decodeOrNull(it)
                }
            )
            .isNull()
    }

    @Test
    fun decodeAllFormatsFromStrokeInputBatch_roundTrip() {
        val encodedInputs =
            ByteArrayOutputStream().use {
                testBatch.encode(it)
                it.toByteArray()
            }
        for (decode in DECODE_STROKE_INPUT_BATCH_FUNCTIONS) {
            val decodedInput = ByteArrayInputStream(encodedInputs).use { decode(it) }
            assertBatchesAreNearEqual(decodedInput, testBatch)
        }
    }

    companion object {
        private val DECODE_STROKE_INPUT_BATCH_FUNCTIONS:
            List<(InputStream) -> ImmutableStrokeInputBatch> =
            listOf(
                StrokeInputBatch::decodeOrThrow,
                { input -> StrokeInputBatch.decodeOrNull(input)!! },
                // Kotlin clients should prefer the extension methods, but the top-level functions
                // do
                // work.
                ::decodeOrThrow,
                { input -> decodeOrNull(input)!! },
            )
    }
}
