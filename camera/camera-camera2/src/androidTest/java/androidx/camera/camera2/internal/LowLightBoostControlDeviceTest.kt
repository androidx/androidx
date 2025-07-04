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
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.internal.util.TestUtil
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.CameraXUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val DEFAULT_CAMERA_SELECTOR = CameraSelector.DEFAULT_BACK_CAMERA

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35)
class LowLightBoostControlDeviceTest {

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            PreTestCameraIdList(Camera2Config.defaultConfig())
        )

    val context = ApplicationProvider.getApplicationContext() as Context

    private lateinit var camera: CameraUseCaseAdapter

    @Before
    fun setUp() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(DEFAULT_CAMERA_SELECTOR.lensFacing!!))
        // Init CameraX
        val config = Camera2Config.defaultConfig()
        CameraXUtil.initialize(context, config)

        // Prepare LowLightBoostControl
        // To get a functional Camera2CameraControlImpl, it needs to bind an active UseCase and the
        // UseCase must have repeating surface. Create and bind Preview as repeating surface.
        camera =
            CameraUtil.createCameraAndAttachUseCase(
                context,
                DEFAULT_CAMERA_SELECTOR,
                Preview.Builder().build().apply {
                    InstrumentationRegistry.getInstrumentation().runOnMainSync {
                        surfaceProvider = SurfaceTextureProvider.createSurfaceTextureProvider()
                    }
                },
            )
    }

    @After
    fun tearDown() {
        if (::camera.isInitialized) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                // TODO: The removeUseCases() call might be removed after clarifying the
                // abortCaptures() issue in b/162314023.
                camera.removeUseCases(camera.getUseCases())
            }
        }

        CameraXUtil.shutdown().get(10000, TimeUnit.MILLISECONDS)
    }

    @Test
    fun enableDisableLowLightBoost_futureWillCompleteSuccessfully_whenLlbIsSupported() {
        assumeTrue(camera.cameraInfo.isLowLightBoostSupported)
        TestUtil.getCamera2CameraControlImpl(camera.cameraControl).lowLightBoostControl.apply {
            // Future should return with no exception
            enableLowLightBoost(true).get(3, TimeUnit.SECONDS)

            // Future should return with no exception
            enableLowLightBoost(false).get(3, TimeUnit.SECONDS)
        }
    }
}
