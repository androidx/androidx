/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.camera2.internal.compat

import android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
import android.hardware.camera2.params.StreamConfigurationMap
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.internal.compat.StreamConfigurationMapCompat.toStreamConfigurationMapCompat
import androidx.camera.camera2.internal.compat.workaround.OutputSizesCorrector
import androidx.camera.core.CameraSelector
import androidx.camera.testing.impl.CameraUtil
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Contains tests for [StreamConfigurationMapCompat].
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class StreamConfigurationMapCompatTest {

    @get:Rule
    val cameraRule = CameraUtil.grantCameraPermissionAndPreTest(
        CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    private val lensFacing = CameraSelector.LENS_FACING_BACK
    private lateinit var streamConfigurationMap: StreamConfigurationMap
    private lateinit var streamConfigurationMapCompat: StreamConfigurationMapCompat

    @Before
    fun setUp() {
        val cameraCharacteristics = CameraUtil.getCameraCharacteristics(lensFacing)!!
        streamConfigurationMap = cameraCharacteristics.get(SCALER_STREAM_CONFIGURATION_MAP)!!
        val cameraId = CameraUtil.getCameraIdWithLensFacing(lensFacing)!!
        streamConfigurationMapCompat = toStreamConfigurationMapCompat(
            streamConfigurationMap,
            OutputSizesCorrector(cameraId)
        )
    }

    @Test
    fun canGetOutputFormats() {
        val formats = streamConfigurationMapCompat.outputFormats!!.toList()
        assertThat(formats).containsExactlyElementsIn(streamConfigurationMap.outputFormats.toList())
    }
}
