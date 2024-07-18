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

package androidx.camera.camera2.internal.compat.workaround

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.os.Handler
import android.os.Looper
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.internal.Camera2CameraFactory
import androidx.camera.camera2.internal.compat.quirk.DeviceQuirks
import androidx.camera.camera2.internal.compat.quirk.ExtraSupportedSurfaceCombinationsQuirk
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.impl.CameraConfig
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.CameraThreadConfig
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.Identifier
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.SessionProcessor
import androidx.camera.core.impl.SurfaceCombination
import androidx.camera.core.impl.SurfaceConfig
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.CameraXUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeSessionProcessor
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.Arrays
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeNotNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private const val CAPTURE_TIMEOUT = 10_000.toLong() //  10 seconds

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class ExtraSupportedSurfaceCombinationsContainerDeviceTest(val cameraId: String) {

    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    private val context = ApplicationProvider.getApplicationContext<Context>()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "cameraId = {0}")
        fun initParameters(): MutableSet<String> = getCameraIds()

        private fun getCameraIds(): MutableSet<String> {
            val camera2CameraFactory = Camera2CameraFactory(
                ApplicationProvider.getApplicationContext(),
                CameraThreadConfig.create(
                    CameraXExecutors.mainThreadExecutor(),
                    Handler(Looper.getMainLooper())
                ),
                null,
                -1L)
            return camera2CameraFactory.availableCameraIds
        }
    }

    private lateinit var cameraUseCaseAdapter: CameraUseCaseAdapter

    private val extraConfigurationQuirk = ExtraSupportedSurfaceCombinationsContainer()

    @Before
    fun setUp() {
        assumeTrue(CameraUtil.deviceHasCamera())
        CameraXUtil.initialize(
            context,
            Camera2Config.defaultConfig()
        ).get()

        // Only runs the test when the ExtraSupportedSurfaceCombinationsQuirk is applied for the
        // device.
        assumeNotNull(DeviceQuirks.get(ExtraSupportedSurfaceCombinationsQuirk::class.java))
    }

    @After
    fun tearDown() {
        CameraXUtil.shutdown().get(10000, TimeUnit.MILLISECONDS)
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun successCaptureImage_whenExtraYuvPrivYuvConfigurationSupported() = runBlocking {
        var cameraSelector = createCameraSelectorById(cameraId)
        cameraUseCaseAdapter = CameraUtil.createCameraUseCaseAdapter(context, cameraSelector)
        var camera2CameraInfo = Camera2CameraInfo.from(cameraUseCaseAdapter.cameraInfo)

        var hardwareLevel: Int? = camera2CameraInfo.getCameraCharacteristic(
            CameraCharacteristics
                .INFO_SUPPORTED_HARDWARE_LEVEL
        )

        val capabilities = camera2CameraInfo
            .getCameraCharacteristic(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

        assumeTrue(
            capabilities != null &&
                capabilities.contains(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE
                )
        )

        // Only runs the test when the YUV + PRIV + YUV configuration is included in the extra
        // supported configurations list.
        assumeTrue(
            supportExtraFullYuvPrivYuvConfiguration(
                camera2CameraInfo.cameraId,
                hardwareLevel!!
            )
        )

        // Image analysis use a YUV stream by default
        var imageAnalysis = ImageAnalysis.Builder().build()
        // Preview use a PRIV stream by default
        var preview = Preview.Builder().build()
        var imageCapture = ImageCapture.Builder().build()
        // This will force ImageCapture to use YUV_420_888 to configure capture session.
        val fakeSessionProcessor = FakeSessionProcessor(
            inputFormatPreview = null,
            inputFormatCapture = ImageFormat.YUV_420_888
        )
        enableSessionProcessor(cameraUseCaseAdapter, fakeSessionProcessor)
        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(getSurfaceProvider())
            cameraUseCaseAdapter.addUseCases(Arrays.asList(imageAnalysis, preview, imageCapture))
        }

        // Checks whether a picture can be captured successfully in the YUV + PRIV + YUV
        // configuration. This means that a capture session can be created successfully in the
        // configuration.
        val callback = FakeImageCaptureCallback()
        imageCapture.takePicture(CameraXExecutors.directExecutor(), callback)
        callback.awaitCapturesAndAssert()

        withContext(Dispatchers.Main) {
            cameraUseCaseAdapter.removeUseCases(cameraUseCaseAdapter.useCases)
        }
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun successCaptureImage_whenExtraYuvYuvYuvConfigurationSupported() = runBlocking {
        var cameraSelector = createCameraSelectorById(cameraId)
        cameraUseCaseAdapter = CameraUtil.createCameraUseCaseAdapter(context, cameraSelector)
        var camera2CameraInfo = Camera2CameraInfo.from(cameraUseCaseAdapter.cameraInfo)

        var hardwareLevel: Int? = camera2CameraInfo.getCameraCharacteristic(
            CameraCharacteristics
                .INFO_SUPPORTED_HARDWARE_LEVEL
        )

        val capabilities = camera2CameraInfo
            .getCameraCharacteristic(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

        assumeTrue(
            capabilities != null &&
                capabilities.contains(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE
                )
        )

        // Only runs the test when the YUV + YUV + YUV configuration is included in the extra
        // supported configurations list.
        assumeTrue(
            supportExtraFullYuvYuvYuvConfiguration(
                camera2CameraInfo.cameraId,
                hardwareLevel!!
            )
        )

        // Image analysis use a YUV stream by default
        var imageAnalysis = ImageAnalysis.Builder().build()
        var preview = Preview.Builder().build()
        var imageCapture = ImageCapture.Builder().build()

        // This will force ImageCapture / Preview to use YUV_420_888 to configure capture session.
        val fakeSessionProcessor = FakeSessionProcessor(
            inputFormatPreview = ImageFormat.YUV_420_888,
            inputFormatCapture = ImageFormat.YUV_420_888
        )
        enableSessionProcessor(cameraUseCaseAdapter, fakeSessionProcessor)
        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(getSurfaceProvider())
            cameraUseCaseAdapter.addUseCases(Arrays.asList(imageAnalysis, preview, imageCapture))
        }

        // Checks whether a picture can be captured successfully in the YUV + YUV + YUV
        // configuration. This means that a capture session can be created successfully in the
        // configuration.
        val callback = FakeImageCaptureCallback()
        imageCapture.takePicture(CameraXExecutors.directExecutor(), callback)
        callback.awaitCapturesAndAssert()

        withContext(Dispatchers.Main) {
            cameraUseCaseAdapter.removeUseCases(cameraUseCaseAdapter.useCases)
        }
    }

    private fun enableSessionProcessor(
        cameraUseCaseAdapter: CameraUseCaseAdapter,
        sessionProcessor: SessionProcessor
    ) {
        cameraUseCaseAdapter.setExtendedConfig(object : CameraConfig {
            override fun getConfig(): Config {
                return MutableOptionsBundle.create()
            }

            override fun getCompatibilityId(): Identifier {
                return Identifier.create(0)
            }

            override fun getSessionProcessor(
                valueIfMissing: SessionProcessor?
            ): SessionProcessor? {
                return sessionProcessor
            }

            override fun getSessionProcessor(): SessionProcessor {
                return sessionProcessor
            }
        })
    }

    private fun createCameraSelectorById(id: String): CameraSelector {
        var builder = CameraSelector.Builder()

        builder.addCameraFilter { cameraInfos: List<CameraInfo> ->
            val output: MutableList<CameraInfo> = ArrayList()

            cameraInfos.forEach {
                if ((it as CameraInfoInternal).cameraId.equals(id)) {
                    output.add(it)
                    return@addCameraFilter output
                }
            }

            throw IllegalArgumentException("No camera can be find for id: " + id)
        }

        return builder.build()
    }

    private fun getSurfaceProvider(): Preview.SurfaceProvider {
        // Must use auto draining SurfaceTexture which will close the Image. Otherwise it could
        // block the imageWriter to cause problems.
        return SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
    }

    /**
     * Checks whether the device supports the extra (YUV, ANALYSIS) + (PRIV, PREVIEW) + (YUV,
     * MAXIMUM) configuration.
     */
    fun supportExtraFullYuvPrivYuvConfiguration(cameraId: String, hardwareLevel: Int): Boolean {
        // (YUV, ANALYSIS) + (PRIV, PREVIEW) + (YUV, MAXIMUM)
        val surfaceCombinationYuvPrivYuv = SurfaceCombination()
        surfaceCombinationYuvPrivYuv.addSurfaceConfig(
            SurfaceConfig.create(
                SurfaceConfig.ConfigType.YUV,
                SurfaceConfig.ConfigSize.VGA
            )
        )
        surfaceCombinationYuvPrivYuv.addSurfaceConfig(
            SurfaceConfig.create(
                SurfaceConfig.ConfigType.PRIV,
                SurfaceConfig.ConfigSize.PREVIEW
            )
        )
        surfaceCombinationYuvPrivYuv.addSurfaceConfig(
            SurfaceConfig.create(
                SurfaceConfig.ConfigType.YUV,
                SurfaceConfig.ConfigSize.MAXIMUM
            )
        )

        extraConfigurationQuirk.get(cameraId, hardwareLevel).forEach { surfaceCombination ->
            if (surfaceCombination.getOrderedSupportedSurfaceConfigList(
                    surfaceCombinationYuvPrivYuv.surfaceConfigList
                )
                != null
            ) {
                return true
            }
        }

        return false
    }

    /**
     * Checks whether the device supports the extra (YUV, ANALYSIS) + (YUV, PREVIEW) + (YUV,
     * MAXIMUM) configuration.
     */
    fun supportExtraFullYuvYuvYuvConfiguration(cameraId: String, hardwareLevel: Int): Boolean {
        // (YUV, ANALYSIS) + (YUV, PREVIEW) + (YUV, MAXIMUM)
        val surfaceCombinationYuvYuvYuv = SurfaceCombination()
        surfaceCombinationYuvYuvYuv.addSurfaceConfig(
            SurfaceConfig.create(
                SurfaceConfig.ConfigType.YUV,
                SurfaceConfig.ConfigSize.VGA
            )
        )
        surfaceCombinationYuvYuvYuv.addSurfaceConfig(
            SurfaceConfig.create(
                SurfaceConfig.ConfigType.YUV,
                SurfaceConfig.ConfigSize.PREVIEW
            )
        )
        surfaceCombinationYuvYuvYuv.addSurfaceConfig(
            SurfaceConfig.create(
                SurfaceConfig.ConfigType.YUV,
                SurfaceConfig.ConfigSize.MAXIMUM
            )
        )

        extraConfigurationQuirk.get(cameraId, hardwareLevel).forEach { surfaceCombination ->
            if (surfaceCombination.getOrderedSupportedSurfaceConfigList(
                    surfaceCombinationYuvYuvYuv.surfaceConfigList
                ) != null
            ) {
                return true
            }
        }

        return false
    }

    private class FakeImageCaptureCallback() : ImageCapture.OnImageCapturedCallback() {

        private val latch = CountDownLatch(1)

        override fun onCaptureSuccess(image: ImageProxy) {
            image.close()
            latch.countDown()
        }

        override fun onError(exception: ImageCaptureException) {
            throw exception
        }

        fun awaitCapturesAndAssert(timeout: Long = CAPTURE_TIMEOUT) {
            assertThat(latch.await(timeout, TimeUnit.MILLISECONDS)).isTrue()
        }
    }
}
