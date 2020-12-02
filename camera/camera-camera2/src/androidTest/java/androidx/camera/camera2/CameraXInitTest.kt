/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth
import com.google.common.util.concurrent.ListenableFuture
import org.junit.AfterClass
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class CameraXInitTest {

    // Please don't use CameraUtil.grantCameraPermissionAndPreTest. This test verifies the CameraX
    // initialization can successfully done on real device.
    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    companion object {
        private lateinit var cameraXListenableFuture: ListenableFuture<CameraX>
        private val context = ApplicationProvider.getApplicationContext<Context>()
        private val pm = context.packageManager

        @BeforeClass
        @JvmStatic
        fun classSetup() {
            cameraXListenableFuture = CameraX.getOrCreateInstance(context)
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            CameraX.shutdown().get()
        }
    }

    @Before
    fun setup() {
        // Only test the device when it have least one camera. Please don't use the
        // CameraUtil.deviceHasCamera() to check the camera, it might ignore the test if the
        // camera device in bad status.
        assumeTrue(
            pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) || pm.hasSystemFeature(
                PackageManager.FEATURE_CAMERA_FRONT
            )
        )
    }

    @Test
    fun initOnDevice() {
        cameraXListenableFuture.get()

        Truth.assertThat(CameraX.isInitialized()).isTrue()
    }

    @Test
    fun initOnDevice_hasCamera() {
        val cameraX = cameraXListenableFuture.get()

        try {
            if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                CameraSelector.DEFAULT_BACK_CAMERA.select(cameraX.cameraRepository.cameras)
            }
            if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
                CameraSelector.DEFAULT_FRONT_CAMERA.select(cameraX.cameraRepository.cameras)
            }
        } catch (e: IllegalArgumentException) {
            // Wrap the exception with specific error message for dashboard bug collection.
            throw java.lang.IllegalArgumentException("CameraIdList_incorrect:" + Build.MODEL, e)
        }
    }
}