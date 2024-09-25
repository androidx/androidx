/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.core.streamsharing

import android.os.Build
import android.util.Pair
import androidx.camera.core.impl.CameraCaptureMetaData
import androidx.camera.core.impl.TagBundle
import androidx.camera.testing.fakes.FakeCameraCaptureResult
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/** Unit tests for [VirtualCameraCaptureResult]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class VirtualCameraCaptureResultTest {

    companion object {
        private const val KEY = "key"
        private const val VALUE = "value"
        private val TAG_BUNDLE = TagBundle.create(Pair(KEY, VALUE))
    }

    @Test
    fun metadataWithTimestamp_overrideTimestamp() {
        // Act.
        val result = VirtualCameraCaptureResult(TAG_BUNDLE, 1L)
        // Assert.
        assertThat(result.timestamp).isEqualTo(1L)
        assertThat(result.tagBundle).isEqualTo(TAG_BUNDLE)
        assertThat(result.aeState).isEqualTo(CameraCaptureMetaData.AeState.UNKNOWN)
        assertThat(result.afState).isEqualTo(CameraCaptureMetaData.AfState.UNKNOWN)
        assertThat(result.awbState).isEqualTo(CameraCaptureMetaData.AwbState.UNKNOWN)
        assertThat(result.flashState).isEqualTo(CameraCaptureMetaData.FlashState.UNKNOWN)
        assertThat(result.afMode).isEqualTo(CameraCaptureMetaData.AfMode.UNKNOWN)
    }

    @Test
    fun metadataWithBaseValue_returnBaseValue() {
        // Arrange.
        val baseCameraCaptureResult =
            FakeCameraCaptureResult().apply {
                timestamp = 2L
                aeState = CameraCaptureMetaData.AeState.CONVERGED
                afState = CameraCaptureMetaData.AfState.LOCKED_FOCUSED
                awbState = CameraCaptureMetaData.AwbState.CONVERGED
                flashState = CameraCaptureMetaData.FlashState.FIRED
                afMode = CameraCaptureMetaData.AfMode.ON_CONTINUOUS_AUTO
                aeMode = CameraCaptureMetaData.AeMode.ON
                awbMode = CameraCaptureMetaData.AwbMode.AUTO
            }
        // Act.
        val result = VirtualCameraCaptureResult(TAG_BUNDLE, baseCameraCaptureResult)
        // Assert.
        assertThat(result.timestamp).isEqualTo(baseCameraCaptureResult.timestamp)
        assertThat(result.tagBundle).isEqualTo(TAG_BUNDLE)
        assertThat(result.aeState).isEqualTo(baseCameraCaptureResult.aeState)
        assertThat(result.afState).isEqualTo(baseCameraCaptureResult.afState)
        assertThat(result.awbState).isEqualTo(baseCameraCaptureResult.awbState)
        assertThat(result.flashState).isEqualTo(baseCameraCaptureResult.flashState)
        assertThat(result.afMode).isEqualTo(baseCameraCaptureResult.afMode)
        assertThat(result.aeMode).isEqualTo(baseCameraCaptureResult.aeMode)
        assertThat(result.awbMode).isEqualTo(baseCameraCaptureResult.awbMode)
    }
}
