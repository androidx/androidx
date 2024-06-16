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

package androidx.camera.integration.core

import android.content.Context
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class CameraInfoDeviceTest(private val implName: String, private val cameraXConfig: CameraXConfig) {
    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(
                if (implName == Camera2Config::class.simpleName) {
                    Camera2Config.defaultConfig()
                } else {
                    CameraPipeConfig.defaultConfig()
                }
            )
        )

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(
            active = implName == CameraPipeConfig::class.simpleName,
        )

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var cameraProvider: ProcessCameraProvider

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
                arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig())
            )
    }

    @Before
    fun setUp() = runBlocking {
        assumeTrue(CameraUtil.deviceHasCamera())
        CoreAppTestUtil.assumeCompatibleDevice()

        withTimeout(10000) {
            ProcessCameraProvider.configureInstance(cameraXConfig)
            cameraProvider = ProcessCameraProvider.getInstance(context).await()
        }
    }

    @After
    fun tearDown() {
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS]
        }
    }

    @Test
    fun allCamerasAdvertiseSdr() {
        cameraProvider.availableCameraInfos.forEach { cameraInfo ->
            assertThat(cameraInfo.querySupportedDynamicRanges(setOf(DynamicRange.UNSPECIFIED)))
                .contains(DynamicRange.SDR)
        }
    }

    @Test
    fun underSpecifiedDynamicRange_neverReturnedFromQuery() {
        cameraProvider.availableCameraInfos.forEach { cameraInfo ->
            cameraInfo.querySupportedDynamicRanges(setOf(DynamicRange.UNSPECIFIED)).forEach {
                assertWithMessage("$cameraInfo advertises under-specified dynamic range: $it")
                    .that(it.isFullySpecified)
                    .isTrue()
            }
        }
    }

    @Test
    fun emptyCandidateDynamicRangeSet_throwsIllegalArgumentException() {
        cameraProvider.availableCameraInfos.forEach { cameraInfo ->
            assertThrows(IllegalArgumentException::class.java) {
                cameraInfo.querySupportedDynamicRanges(emptySet())
            }
        }
    }
}
