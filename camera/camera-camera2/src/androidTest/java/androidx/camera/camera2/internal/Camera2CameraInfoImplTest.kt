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

package androidx.camera.camera2.internal

import android.content.Context
import android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.internal.compat.CameraManagerCompat
import androidx.camera.core.CameraSelector
import androidx.camera.testing.impl.CameraUtil
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Contains tests for [Camera2CameraInfoImpl] internal implementation. */
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class Camera2CameraInfoImplTest {

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
        )

    private val lensFacing = CameraSelector.LENS_FACING_BACK
    private lateinit var camera2CameraInfo: Camera2CameraInfoImpl

    @Before
    fun setUp() {
        val cameraId = CameraUtil.getCameraIdWithLensFacing(lensFacing)!!
        val cameraManagerCompat = CameraManagerCompat.from((getApplicationContext() as Context))
        camera2CameraInfo = Camera2CameraInfoImpl(cameraId, cameraManagerCompat)
    }

    @Test
    fun canReturnSupportedOutputFormats() {
        val formats = camera2CameraInfo.supportedOutputFormats
        val cameraCharacteristics = CameraUtil.getCameraCharacteristics(lensFacing)!!
        val streamConfigurationMap = cameraCharacteristics.get(SCALER_STREAM_CONFIGURATION_MAP)!!

        assertThat(formats).containsExactlyElementsIn(streamConfigurationMap.outputFormats.toSet())
    }
}
