/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.camera2.pipe.graph

import android.hardware.camera2.CaptureResult
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.LatestFrameMetadata
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
class LatestFrameMetadataImplTest {

    private val keyX = CaptureResult.SENSOR_EXPOSURE_TIME
    private val metaKeyY = Metadata.Key.create<Int>("test.key.y")

    @Test
    fun testGetAndGetOrDefault() {
        val map =
            LatestFrameMetadataImpl(
                captureResultKeys = arrayOf(keyX),
                captureResultValues = arrayOf(100L),
                captureResultFrameNumbers = arrayOf(FrameNumber(12)),
                rawMetadataKeys = arrayOf(metaKeyY),
                metadataValues = arrayOf(5),
                metadataFrameNumbers = arrayOf(FrameNumber(15)),
            )

        assertThat(map[keyX]).isEqualTo(100L)
        assertThat(map.getOrDefault(keyX, 200L)).isEqualTo(100L)

        assertThat(map[metaKeyY]).isEqualTo(5)
        assertThat(map.getOrDefault(metaKeyY, 10)).isEqualTo(5)

        val unmappedKey = CaptureResult.SENSOR_SENSITIVITY
        assertThat(map[unmappedKey]).isNull()
        assertThat(map.getOrDefault(unmappedKey, 400)).isEqualTo(400)
    }

    @Test
    fun testGetFrameNumber() {
        val map =
            LatestFrameMetadataImpl(
                captureResultKeys = arrayOf(keyX),
                captureResultValues = arrayOf(100L),
                captureResultFrameNumbers = arrayOf(FrameNumber(12)),
                rawMetadataKeys = arrayOf(metaKeyY),
                metadataValues = arrayOf(5),
                metadataFrameNumbers = arrayOf(FrameNumber(15)),
            )

        assertThat(map.getFrameNumber(keyX)).isEqualTo(FrameNumber(12))
        assertThat(map.getFrameNumber(metaKeyY)).isEqualTo(FrameNumber(15))

        val unmappedKey = CaptureResult.SENSOR_SENSITIVITY
        assertThat(map.getFrameNumber(unmappedKey)).isNull()
    }

    @Test
    fun testKeysList() {
        val map =
            LatestFrameMetadataImpl(
                captureResultKeys = arrayOf(keyX),
                captureResultValues = arrayOf(100L),
                captureResultFrameNumbers = arrayOf(FrameNumber(12)),
                rawMetadataKeys = arrayOf(metaKeyY),
                metadataValues = arrayOf(5),
                metadataFrameNumbers = arrayOf(FrameNumber(15)),
            )

        assertThat(map.keys).containsExactly(keyX)
        assertThat(map.metadataKeys).containsExactly(metaKeyY)
    }

    @Test
    fun testCompanionInvokePropagatesFrameNumbers() {
        val metadata =
            LatestFrameMetadata(
                captureResultParameters = mapOf(keyX to 100L),
                metadataParameters = mapOf(metaKeyY to 5),
                captureResultFrameNumbers = mapOf(keyX to FrameNumber(12)),
                metadataFrameNumbers = mapOf(metaKeyY to FrameNumber(15)),
            )

        assertThat(metadata[keyX]).isEqualTo(100L)
        assertThat(metadata.getFrameNumber(keyX)).isEqualTo(FrameNumber(12))

        assertThat(metadata[metaKeyY]).isEqualTo(5)
        assertThat(metadata.getFrameNumber(metaKeyY)).isEqualTo(FrameNumber(15))
    }
}
