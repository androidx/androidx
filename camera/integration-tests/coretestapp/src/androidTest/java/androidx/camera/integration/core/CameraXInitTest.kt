/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.integration.core

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.core.CameraXConfig
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(Parameterized::class)
class CameraXInitTest(private val implName: String, private val cameraXConfig: CameraXConfig) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
            arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig())
        )
    }

    // Don't use CameraUtil.grantCameraPermissionAndPreTest. This test verifies the CameraX
    // initialization can be successfully done on a real device.
    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val packageManager = context.packageManager

    @Before
    fun setup() {
        // Only test the device when it has at least 1 camera. Don't use CameraUtil
        // .deviceHasCamera() to check the camera, it might ignore the test if the camera device
        // is in a bad state.
        assumeTrue(
            packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA) ||
                packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
        )
    }

    @After
    fun tearDown() {
        CameraX.shutdown().get(10, TimeUnit.SECONDS)
    }

    @Test
    fun initOnDevice() {
        CameraX.getOrCreateInstance(context, { cameraXConfig }).get(10, TimeUnit.SECONDS)
        assertThat(CameraX.isInitialized()).isTrue()
    }

    @Test
    fun initOnDevice_hasCamera() {
        val cameraX =
            CameraX.getOrCreateInstance(context, { cameraXConfig }).get(10, TimeUnit.SECONDS)
        try {
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                CameraSelector.DEFAULT_BACK_CAMERA.select(cameraX.cameraRepository.cameras)
            }
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
                CameraSelector.DEFAULT_FRONT_CAMERA.select(cameraX.cameraRepository.cameras)
            }
        } catch (e: IllegalArgumentException) {
            // Wrap the exception with specific error message for dashboard bug collection.
            throw IllegalArgumentException("CameraIdList_incorrect:" + Build.MODEL, e)
        }
    }
}
