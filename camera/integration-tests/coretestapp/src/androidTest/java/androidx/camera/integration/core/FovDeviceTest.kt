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
import android.os.Build
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraXUtil
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import java.util.Collections
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private val DEFAULT_CAMERA_ID_GROUP = Collections.unmodifiableSet(setOf("0", "1"))

@LargeTest
@RunWith(Parameterized::class)
class FovDeviceTest(
    private val cameraId: String,
    private val implName: String,
    private val cameraXConfig: CameraXConfig,
) {
    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName == CameraPipeConfig::class.simpleName)

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(cameraXConfig)
        )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "cameraId: {0}, implName: {1}")
        fun data(): List<Array<Any?>> {
            val paramList = mutableListOf<Array<Any?>>()
            CameraUtil.getBackwardCompatibleCameraIdListOrThrow().forEach { cameraId ->
                paramList.add(
                    arrayOf(
                        cameraId,
                        Camera2Config::class.simpleName,
                        Camera2Config.defaultConfig(),
                    )
                )
                paramList.add(
                    arrayOf(
                        cameraId,
                        CameraPipeConfig::class.simpleName,
                        CameraPipeConfig.defaultConfig(),
                    )
                )
            }
            return paramList
        }
    }

    private lateinit var cameraUseCaseAdapter: CameraUseCaseAdapter
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        CameraXUtil.initialize(context, cameraXConfig).get()

        val cameraSelector =
            CameraSelector.Builder()
                .addCameraFilter { cameraInfoList ->
                    val filteredList = ArrayList<CameraInfo>()
                    cameraInfoList.forEach { cameraInfo ->
                        if ((cameraInfo as CameraInfoInternal).cameraId == cameraId) {
                            filteredList.add(cameraInfo)
                        }
                    }
                    filteredList
                }
                .build()
        cameraUseCaseAdapter = CameraUtil.createCameraUseCaseAdapter(context, cameraSelector)
    }

    @After
    fun tearDown() {
        CameraXUtil.shutdown()[10000, TimeUnit.MILLISECONDS]
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun intrinsicZoomRatio_greaterThanZero() {
        Truth.assertThat(cameraUseCaseAdapter.cameraInfo.intrinsicZoomRatio).isGreaterThan(0)
    }

    @Test
    fun intrinsicZoomRatio_defaultToOne() {
        Assume.assumeTrue(DEFAULT_CAMERA_ID_GROUP.contains(cameraId))
        Truth.assertThat(cameraUseCaseAdapter.cameraInfo.intrinsicZoomRatio).isEqualTo(1.0F)
    }
}
