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
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPoint
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.UseCase
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.core.internal.CameraUseCaseAdapter.CameraException
import androidx.camera.testing.impl.CameraAvailabilityUtil
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.CameraXUtil
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
    val cameraPipeConfigTestRule = CameraPipeConfigTestRule(
        active = implName == CameraPipeConfig::class.simpleName,
    )

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
        CameraXUtil.initialize(context, cameraConfig).get()
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

    private fun <T> assertFutureCompletes(future: ListenableFuture<T>) {
        try {
            future[10, TimeUnit.SECONDS]
        } catch (e: Exception) {
            Assert.fail("future fail:$e")
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
