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

package androidx.camera.camera2.pipe.integration.internal

import android.content.Context
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.camera.camera2.pipe.integration.adapter.CameraFactoryProvider
import androidx.camera.camera2.pipe.integration.interop.Camera2CameraInfo
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraFilter
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.CameraThreadConfig
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowCameraManager
import org.robolectric.shadows.StreamConfigurationMapBuilder

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(
    minSdk = Build.VERSION_CODES.LOLLIPOP,
    instrumentedPackages = ["androidx.camera.camera2.pipe.integration.adapter"]
)
class CameraSelectionOptimizerTest {
    private lateinit var cameraFactory: CameraFactory

    private fun setupNormalCameras() {
        initCharacteristics("0", CameraCharacteristics.LENS_FACING_BACK, 3.52f)
        initCharacteristics("1", CameraCharacteristics.LENS_FACING_FRONT, 3.52f)
        initCharacteristics("2", CameraCharacteristics.LENS_FACING_BACK, 2.7f)
        initCharacteristics("3", CameraCharacteristics.LENS_FACING_BACK, 10.0f)
    }

    private fun setupAbnormalCameras() {
        // "0" is front
        initCharacteristics("0", CameraCharacteristics.LENS_FACING_FRONT, 3.52f)
        // "1" is back
        initCharacteristics("1", CameraCharacteristics.LENS_FACING_BACK, 3.52f)
        initCharacteristics("2", CameraCharacteristics.LENS_FACING_BACK, 2.7f)
        initCharacteristics("3", CameraCharacteristics.LENS_FACING_BACK, 10.0f)
    }

    @Test
    fun availableCamerasSelectorNull_returnAllCameras() {
        setupNormalCameras()

        val cameraIds: List<String> = getCameraIdsBasedOnCameraSelector(null)

        Truth.assertThat(cameraIds).containsExactly("0", "1", "2", "3")
    }

    @Test
    fun requireLensFacingBack() {
        setupNormalCameras()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val cameraIds: List<String> = getCameraIdsBasedOnCameraSelector(cameraSelector)

        Truth.assertThat(cameraIds).containsExactly("0", "2", "3")
        Mockito.verify(cameraFactory, Mockito.never())
            .getCamera("1")
    }

    @Test
    fun requireLensFacingFront() {

        setupNormalCameras()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()

        val cameraIds: List<String> = getCameraIdsBasedOnCameraSelector(cameraSelector)
        Truth.assertThat(cameraIds).containsExactly("1")
        Mockito.verify(cameraFactory, Mockito.never())
            .getCamera("0")
    }

    @OptIn(ExperimentalCamera2Interop::class)
    @Test
    fun requireLensFacingBack_andSelectWidestAngle() {
        setupNormalCameras()

        val widestAngleFilter = CameraFilter { cameraInfoList: List<CameraInfo> ->
            var minFocalLength = 10000f
            var minFocalCameraInfo: CameraInfo? = null
            for (cameraInfo in cameraInfoList) {
                val focalLength: Float =
                    Camera2CameraInfo.from(cameraInfo)
                        .getCameraCharacteristic<FloatArray>(
                            CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
                        )!![0]
                if (focalLength < minFocalLength) {
                    minFocalLength = focalLength
                    minFocalCameraInfo = cameraInfo
                }
            }
            listOf(minFocalCameraInfo)
        }
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .addCameraFilter(widestAngleFilter)
            .build()
        val cameraIds: List<String> = getCameraIdsBasedOnCameraSelector(cameraSelector)
        Truth.assertThat(cameraIds).containsExactly("2")
        // only camera "1" 's getCameraCharacteristics can be avoided.
        Mockito.verify(cameraFactory, Mockito.never())
            .getCamera("1")
    }

    @Test
    fun abnormalCameraSetup_requireLensFacingBack() {
        setupAbnormalCameras()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        val cameraIds: List<String> = getCameraIdsBasedOnCameraSelector(cameraSelector)

        // even though heuristic failed, it still works as expected.
        Truth.assertThat(cameraIds).containsExactly("1", "2", "3")
    }

    @Test
    fun abnormalCameraSetup_requireLensFacingFront() {
        setupAbnormalCameras()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()

        val cameraIds: List<String> = getCameraIdsBasedOnCameraSelector(cameraSelector)

        // even though heuristic failed, it still works as expected.
        Truth.assertThat(cameraIds).containsExactly("0")
    }

    @Test
    fun emptyCameraIdList_returnEmptyAvailableIds() {
        // Do not set up any cameras.
        val cameraIds: List<String> =
            getCameraIdsBasedOnCameraSelector(CameraSelector.DEFAULT_BACK_CAMERA)
        Truth.assertThat(cameraIds).isEmpty()
    }

    @Test
    fun onlyCamera0_requireFront_returnEmptyAvailableIds() {
        initCharacteristics("0", CameraCharacteristics.LENS_FACING_BACK, 3.52f)

        val cameraIds: List<String> =
            getCameraIdsBasedOnCameraSelector(CameraSelector.DEFAULT_FRONT_CAMERA)

        Truth.assertThat(cameraIds).isEmpty()
    }

    @Test
    fun onlyCamera1_requireBack_returnEmptyAvailableIds() {
        initCharacteristics("1", CameraCharacteristics.LENS_FACING_FRONT, 3.52f)

        val cameraIds: List<String> =
            getCameraIdsBasedOnCameraSelector(CameraSelector.DEFAULT_BACK_CAMERA)

        Truth.assertThat(cameraIds).isEmpty()
    }

    private fun getCameraIdsBasedOnCameraSelector(cameraSelector: CameraSelector?): List<String> {
        val actualCameraFactory = CameraFactoryProvider().newInstance(
            ApplicationProvider.getApplicationContext(), CameraThreadConfig.create(
                CameraXExecutors.mainThreadExecutor(), Handler(Looper.getMainLooper())
            ),
            cameraSelector,
            -1L
        )

        cameraFactory = Mockito.spy(actualCameraFactory)

        return CameraSelectionOptimizer.getSelectedAvailableCameraIds(
            cameraFactory,
            cameraSelector
        )
    }

    private fun initCharacteristics(cameraId: String, lensFacing: Int, focalLength: Float) {
        val sensorWidth = 640
        val sensorHeight = 480

        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        Shadow.extract<ShadowCameraCharacteristics>(characteristics).apply {

            set(CameraCharacteristics.LENS_FACING, lensFacing)
            set(
                CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS,
                floatArrayOf(focalLength)
            )
            set(
                CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE,
                Rect(0, 0, sensorWidth, sensorHeight)
            )
            set(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
            )
            set(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP,
                StreamConfigurationMapBuilder.newBuilder().build()
            )
        }

        // Add the camera to the camera service
        (Shadow.extract<Any>(
            ApplicationProvider.getApplicationContext<Context>()
                .getSystemService(Context.CAMERA_SERVICE)
        ) as ShadowCameraManager)
            .addCamera(cameraId, characteristics)
    }
}
