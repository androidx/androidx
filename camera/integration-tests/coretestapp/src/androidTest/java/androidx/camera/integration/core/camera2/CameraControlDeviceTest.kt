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
package androidx.camera.integration.core.camera2

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.LensFacing
import androidx.camera.core.CameraXConfig
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPoint
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.UseCase
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.core.internal.CameraUseCaseAdapter.CameraException
import androidx.camera.testing.CameraAvailabilityUtil
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.CameraXUtil
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Test if camera control functionality can run well in real devices. Autofocus may not work well in
 * devices because the camera might be faced down to the desktop and the auto-focus will never
 * finish on some devices. Thus we don't test AF related functions.
 */
@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class CameraControlDeviceTest(
    private val selectorName: String,
    private val cameraSelector: CameraSelector,
    private val implName: String,
    private val cameraConfig: CameraXConfig
) {
    @get:Rule
    val cameraRule = CameraUtil.grantCameraPermissionAndPreTest(
        PreTestCameraIdList(cameraConfig)
    )

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var camera: CameraUseCaseAdapter? = null
    private var boundUseCase: UseCase? = null
    private var meteringPoint1: MeteringPoint? = null
    private val analyzer = ImageAnalysis.Analyzer { obj: ImageProxy -> obj.close() }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cameraXConfig = Camera2Config.defaultConfig()
        CameraXUtil.initialize(context, cameraXConfig).get()
        val cameraX = CameraXUtil.getOrCreateInstance(context, null).get()
        Assume.assumeTrue(
            CameraAvailabilityUtil.hasCamera(
                cameraX.cameraRepository,
                cameraSelector
            )
        )
        val factory = SurfaceOrientedMeteringPointFactory(1f, 1f)
        meteringPoint1 = factory.createPoint(0f, 0f)
        val useCase = ImageAnalysis.Builder().build()
        camera = CameraUtil.createCameraAndAttachUseCase(context, cameraSelector,
            useCase.also {
                boundUseCase = it
            })
        useCase.setAnalyzer(CameraXExecutors.ioExecutor(), analyzer)
    }

    @After
    fun tearDown() {
        instrumentation.runOnMainSync {
            // TODO: The removeUseCases() call might be removed after clarifying the
            // abortCaptures() issue in b/162314023.
            camera?.removeUseCases(camera!!.useCases)
        }
        CameraXUtil.shutdown()[10000, TimeUnit.MILLISECONDS]
    }

    @Test
    fun startFocusMeteringAe_futureCompletes() {
        Assume.assumeTrue(isSupportAeRegion(cameraSelector))
        val action = FocusMeteringAction.Builder(
            meteringPoint1!!,
            FocusMeteringAction.FLAG_AE
        ).build()
        val future = camera!!.cameraControl.startFocusAndMetering(action)
        assertFutureCompletes(future)
    }

    @Test
    fun startFocusMeteringAwb_futureCompletes() {
        Assume.assumeTrue(isSupportAwbRegion(cameraSelector))
        val action = FocusMeteringAction.Builder(
            meteringPoint1!!,
            FocusMeteringAction.FLAG_AWB
        ).build()
        val future = camera!!.cameraControl.startFocusAndMetering(action)
        assertFutureCompletes(future)
    }

    @Test
    fun startFocusMeteringAeAwb_futureCompletes() {
        Assume.assumeTrue(isSupportAeRegion(cameraSelector) || isSupportAwbRegion(cameraSelector))
        val action = FocusMeteringAction.Builder(
            meteringPoint1!!,
            FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
        ).build()
        val future = camera!!.cameraControl.startFocusAndMetering(action)
        assertFutureCompletes(future)
    }

    @Test
    fun startFocusMeteringMorePointThanSupported_futureCompletes() {
        Assume.assumeTrue(isSupportAeRegion(cameraSelector) || isSupportAwbRegion(cameraSelector))
        val factory = SurfaceOrientedMeteringPointFactory(1f, 1f)
        // Most devices don't support 4 AF/AE/AWB regions. but it should still complete.
        val point1 = factory.createPoint(0f, 0f)
        val point2 = factory.createPoint(1f, 0f)
        val point3 = factory.createPoint(0.2f, 0.2f)
        val point4 = factory.createPoint(0.3f, 0.4f)
        val action = FocusMeteringAction.Builder(
            point1,
            FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
        )
            .addPoint(
                point2, FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
            )
            .addPoint(
                point3,
                FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
            )
            .addPoint(
                point4,
                FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
            )
            .build()
        val future = camera!!.cameraControl.startFocusAndMetering(action)
        assertFutureCompletes(future)
    }

    @Test
    fun cancelFocusMetering_futureCompletes() {
        val action = FocusMeteringAction.Builder(meteringPoint1!!).build()
        camera!!.cameraControl.startFocusAndMetering(action)
        val result = camera!!.cameraControl.cancelFocusAndMetering()
        assertFutureCompletes(result)
    }

    @Test
    fun rebindAndEnableTorch_futureCompletes() {
        Assume.assumeTrue(CameraUtil.hasFlashUnitWithLensFacing(cameraSelector.lensFacing!!))
        instrumentation.runOnMainSync {
            try {
                camera!!.removeUseCases(setOf(boundUseCase))
                val useCase = ImageAnalysis.Builder().build()
                camera!!.addUseCases(setOf<UseCase>(useCase.also { boundUseCase = it }))
                useCase.setAnalyzer(CameraXExecutors.ioExecutor(), analyzer)
            } catch (e: CameraException) {
                throw IllegalArgumentException(e)
            }
        }
        val result = camera!!.cameraControl.enableTorch(true)
        assertFutureCompletes(result)
    }

    @Test
    fun setZoomRatio_futuresCompletes() {
        Assume.assumeTrue(camera!!.cameraInfo.zoomState.value!!.maxZoomRatio >= 2.0f)

        // use ratio with fraction because it often causes unable-to-complete issue.
        val result = camera!!.cameraControl.setZoomRatio(1.3640054f)
        assertFutureCompletes(result)
    }

    @Test
    fun rebindAndSetZoomRatio_futureCompletes() {
        instrumentation.runOnMainSync {
            try {
                camera!!.removeUseCases(setOf(boundUseCase))
                val useCase = ImageAnalysis.Builder().build()
                camera!!.addUseCases(setOf<UseCase>(useCase.also { boundUseCase = it }))
                useCase.setAnalyzer(CameraXExecutors.ioExecutor(), analyzer)
            } catch (e: CameraException) {
                throw IllegalArgumentException(e)
            }
        }
        val result = camera!!.cameraControl.setZoomRatio(1.0f)
        assertFutureCompletes(result)
    }

    private fun <T> assertFutureCompletes(future: ListenableFuture<T>) {
        try {
            future[5, TimeUnit.SECONDS]
        } catch (e: Exception) {
            Assert.fail("future fail:$e")
        }
    }

    private fun getCameraCharacteristicWithLensFacing(
        @LensFacing lensFacing: Int
    ): CameraCharacteristics? {
        return CameraUtil.getCameraCharacteristics(lensFacing)
    }

    private fun isSupportAeRegion(cameraSelector: CameraSelector?): Boolean {
        return try {
            val characteristics = getCameraCharacteristicWithLensFacing(
                cameraSelector!!.lensFacing!!
            )
            characteristics!!.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE)!! > 0
        } catch (e: Exception) {
            false
        }
    }

    private fun isSupportAwbRegion(cameraSelector: CameraSelector?): Boolean {
        return try {
            val characteristics = getCameraCharacteristicWithLensFacing(
                cameraSelector!!.lensFacing!!
            )
            characteristics!!.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB)!! > 0
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "selector={0},config={2}")
        fun data() = listOf(
            arrayOf(
                "back",
                CameraSelector.DEFAULT_BACK_CAMERA,
                Camera2Config::class.simpleName,
                Camera2Config.defaultConfig()
            ),
            arrayOf(
                "back",
                CameraSelector.DEFAULT_BACK_CAMERA,
                CameraPipeConfig::class.simpleName,
                CameraPipeConfig.defaultConfig()
            ),
            arrayOf(
                "front",
                CameraSelector.DEFAULT_FRONT_CAMERA,
                Camera2Config::class.simpleName,
                Camera2Config.defaultConfig()
            ),
            arrayOf(
                "front",
                CameraSelector.DEFAULT_FRONT_CAMERA,
                CameraPipeConfig::class.simpleName,
                CameraPipeConfig.defaultConfig()
            )
        )
    }
}