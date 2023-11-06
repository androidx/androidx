/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.integration.avsync.model

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

private const val FLOAT_ERROR = 1E-4

@RunWith(JUnit4::class)
class AudioGeneratorTest {

    private lateinit var audioGenerator: AudioGenerator

    @Before
    fun setUp() {
        audioGenerator = AudioGenerator()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun generateSineSamples_canGenerateCorrectResult() = runTest {
        val res = audioGenerator.generateSineSamples(1, 1.0, 2, 8)
        val expected = listOf(
            0x00.toByte(), 0x00.toByte(),
            0x40.toByte(), 0x2D.toByte(),
            0xFF.toByte(), 0x3F.toByte(),
            0x40.toByte(), 0x2D.toByte(),
            0x00.toByte(), 0x00.toByte(),
            0xC0.toByte(), 0xD2.toByte(),
            0x01.toByte(), 0xC0.toByte(),
            0xC0.toByte(), 0xD2.toByte(),
        ).toByteArray()

        assertThat(res).isEqualTo(expected)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun generateSineData_canGenerateCorrectResult() = runTest {
        val res = audioGenerator.generateSineData(1, 1.0, 8, 0.5)
        val expected = listOf(0.0, 0.3535, 0.5, 0.3535, 0.0, -0.3535, -0.5, -0.3535)

        assertThat(res.size).isEqualTo(expected.size)
        for (index in res.indices) {
            assertThat(res[index]).isWithin(FLOAT_ERROR).of(expected[index])
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun generateSineData_allZeroWhenFrequencyZero() = runTest {
        val res = audioGenerator.generateSineData(0, 11.0, 10, 0.5)

        assertThat(res.size).isEqualTo(110)
        res.forEach { value ->
            assertThat(value).isZero()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun generateSineData_emptyWhenLengthZero() = runTest {
        val res = audioGenerator.generateSineData(53, 0.0, 10, 0.5)
        val expected = listOf<Double>()

        assertThat(res).isEqualTo(expected)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun generateSineData_emptyWhenSampleRateZero() = runTest {
        val res = audioGenerator.generateSineData(53, 11.0, 0, 0.5)
        val expected = listOf<Double>()

        assertThat(res).isEqualTo(expected)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun generateSineData_allZeroWhenMagnitudeZero() = runTest {
        val res = audioGenerator.generateSineData(53, 11.0, 10, 0.0)

        assertThat(res.size).isEqualTo(110)
        res.forEach { value ->
            assertThat(value).isZero()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun toSamples_canGenerateCorrectResult() = runTest {
        val input = listOf(-0.1, 0.2, -0.3, 0.5, -0.75, 0.9, -1.0)
        val expected = listOf(
            0x34.toByte(), 0xF3.toByte(),
            0x99.toByte(), 0x19.toByte(),
            0x9A.toByte(), 0xD9.toByte(),
            0xFF.toByte(), 0x3F.toByte(),
            0x01.toByte(), 0xA0.toByte(),
            0x32.toByte(), 0x73.toByte(),
            0x01.toByte(), 0x80.toByte(),
        )

        assertThat(audioGenerator.toSamples(input, 2)).isEqualTo(expected)
    }

    @Test
    fun toBytes_canGenerateCorrectResult() {
        val expected = listOf(0xBF.toByte(), 0x14.toByte())

        audioGenerator.run {
            assertThat(5311.toBytes(2)).isEqualTo(expected)
        }
    }
}
