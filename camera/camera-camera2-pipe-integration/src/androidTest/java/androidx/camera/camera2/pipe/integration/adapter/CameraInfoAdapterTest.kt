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

package androidx.camera.camera2.pipe.integration.adapter

import android.content.Context
import android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.camera2.pipe.testing.toCameraInfoAdapter
import androidx.camera.core.CameraSelector
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraXUtil
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Contains tests for [CameraInfoAdapter]. */
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class CameraInfoAdapterTest {

    @get:Rule val useCamera = CameraUtil.grantCameraPermissionAndPreTestAndPostTest()

    private val lensFacing = CameraSelector.LENS_FACING_BACK
    private lateinit var cameraInfoAdapter: CameraInfoAdapter

    @Before
    fun setUp() {
        val context: Context = getApplicationContext()
        CameraXUtil.initialize(context, CameraPipeConfig.defaultConfig())
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val camera = CameraUtil.createCameraUseCaseAdapter(context, cameraSelector)
        cameraInfoAdapter = camera.cameraInfo.toCameraInfoAdapter()
    }

    @After
    fun tearDown() {
        CameraXUtil.shutdown()[10000, TimeUnit.MILLISECONDS]
    }

    @Test
    fun canReturnSupportedOutputFormats() {
        val formats = cameraInfoAdapter.supportedOutputFormats
        val cameraCharacteristics = CameraUtil.getCameraCharacteristics(lensFacing)!!
        val streamConfigurationMap = cameraCharacteristics.get(SCALER_STREAM_CONFIGURATION_MAP)!!

        assertThat(formats).containsExactlyElementsIn(streamConfigurationMap.outputFormats.toSet())
    }
}
