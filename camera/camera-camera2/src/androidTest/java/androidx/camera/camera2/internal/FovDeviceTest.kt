/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraXUtil
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.Collections
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private val DEFAULT_CAMERA_ID_GROUP = Collections.unmodifiableSet(setOf("0", "1"))

@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class FovDeviceTest(private val cameraId: String) {
    private val cameraConfig = Camera2Config.defaultConfig()

    @get:Rule
    val cameraRule = CameraUtil.grantCameraPermissionAndPreTest(
        CameraUtil.PreTestCameraIdList(cameraConfig)
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): ArrayList<String> {
            return ArrayList(CameraUtil.getBackwardCompatibleCameraIdListOrThrow())
        }
    }

    private lateinit var cameraUseCaseAdapter: CameraUseCaseAdapter
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        CameraXUtil.initialize(
            context,
            cameraConfig
        ).get()

        val cameraSelector = CameraSelector.Builder().addCameraFilter { cameraInfoList ->
            val filteredList = ArrayList<CameraInfo>()
            cameraInfoList.forEach { cameraInfo ->
                if ((cameraInfo as CameraInfoInternal).cameraId == cameraId) {
                    filteredList.add(cameraInfo)
                }
            }
            filteredList
        }.build()
        cameraUseCaseAdapter =
            CameraUtil.createCameraUseCaseAdapter(context, cameraSelector)
    }

    @After
    fun tearDown() {
        CameraXUtil.shutdown()[10000, TimeUnit.MILLISECONDS]
    }

    @Test
    fun intrinsicZoomRatio_greaterThanZero() {
        assertThat(cameraUseCaseAdapter.cameraInfo.intrinsicZoomRatio).isGreaterThan(0)
    }

    @Test
    fun intrinsicZoomRatio_defaultToOne() {
        assumeTrue(DEFAULT_CAMERA_ID_GROUP.contains(cameraId))
        assertThat(cameraUseCaseAdapter.cameraInfo.intrinsicZoomRatio).isEqualTo(1.0F)
    }
}
