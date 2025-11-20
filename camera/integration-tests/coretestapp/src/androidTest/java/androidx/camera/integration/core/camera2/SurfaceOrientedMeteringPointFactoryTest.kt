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
import android.util.Rational
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraXUtil
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class SurfaceOrientedMeteringPointFactoryTest(
    private val implName: String,
    private val cameraConfig: CameraXConfig,
) {
    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(cameraConfig)
        )

    private var pointFactory: SurfaceOrientedMeteringPointFactory? = null
    private var context: Context? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        CameraXUtil.initialize(context!!, cameraConfig)
        pointFactory = SurfaceOrientedMeteringPointFactory(WIDTH, HEIGHT)
    }

    @After
    fun tearDown() {
        CameraXUtil.shutdown()[10000, TimeUnit.MILLISECONDS]
    }

    @Test
    fun defaultAreaSize() {
        val point = pointFactory!!.createPoint(0f, 0f)
        Truth.assertThat(point.size).isEqualTo(MeteringPointFactory.getDefaultPointSize())
        Truth.assertThat(point.surfaceAspectRatio).isNull()
    }

    @Test
    fun createPointWithValidAreaSize() {
        val areaSize = 0.2f
        val point = pointFactory!!.createPoint(0f, 0f, areaSize)
        Truth.assertThat(point.size).isEqualTo(areaSize)
        Truth.assertThat(point.surfaceAspectRatio).isNull()
    }

    @Test
    fun createPointLeftTop_correctValueSet() {
        val meteringPoint = pointFactory!!.createPoint(0f, 0f)
        Truth.assertThat(meteringPoint.x).isEqualTo(0f)
        Truth.assertThat(meteringPoint.y).isEqualTo(0f)
    }

    @Test
    fun createPointLeftBottom_correctValueSet() {
        val meteringPoint2 = pointFactory!!.createPoint(0f, HEIGHT)
        Truth.assertThat(meteringPoint2.x).isEqualTo(0f)
        Truth.assertThat(meteringPoint2.y).isEqualTo(1f)
    }

    @Test
    fun createPointRightTop_correctValueSet() {
        val meteringPoint3 = pointFactory!!.createPoint(WIDTH, 0f)
        Truth.assertThat(meteringPoint3.x).isEqualTo(1f)
        Truth.assertThat(meteringPoint3.y).isEqualTo(0f)
    }

    @Test
    fun createPointRightBottom_correctValueSet() {
        val meteringPoint4 = pointFactory!!.createPoint(WIDTH, HEIGHT)
        Truth.assertThat(meteringPoint4.x).isEqualTo(1f)
        Truth.assertThat(meteringPoint4.y).isEqualTo(1f)
    }

    @Test
    fun createPointWithFoVUseCase_success() {
        val cameraSelector = CameraUtil.assumeFirstAvailableCameraSelector()
        val imageAnalysis = ImageAnalysis.Builder().setTargetName("ImageAnalysis").build()
        val camera =
            CameraUtil.createCameraAndAttachUseCase(context!!, cameraSelector, imageAnalysis)
        val surfaceResolution = imageAnalysis.attachedSurfaceResolution
        val factory = SurfaceOrientedMeteringPointFactory(WIDTH, HEIGHT, imageAnalysis)
        val point = factory.createPoint(0f, 0f)
        Truth.assertThat(point.surfaceAspectRatio)
            .isEqualTo(Rational(surfaceResolution!!.width, surfaceResolution.height))
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            // TODO: The removeUseCases() call might be removed after clarifying the
            //  abortCaptures() issue in b/162314023.
            camera.removeUseCases(camera.useCases)
        }
    }

    @Suppress("DEPRECATION") // test for legacy resolution API
    @Test(expected = IllegalStateException::class)
    fun createPointWithFoVUseCase_FailedNotBound() {
        val imageAnalysis =
            ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetName("ImageAnalysis")
                .build()

        // This will throw IllegalStateException.
        SurfaceOrientedMeteringPointFactory(WIDTH, HEIGHT, imageAnalysis)
    }

    companion object {
        private const val WIDTH = 480f
        private const val HEIGHT = 640f

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
                arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig()),
            )
    }
}
