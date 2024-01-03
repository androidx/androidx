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

package androidx.camera.view

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import android.view.View
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.LENS_FACING_BACK
import androidx.camera.core.CameraSelector.LENS_FACING_FRONT
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.camera.testing.impl.fakes.FakeActivity
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.fakes.FakeSurfaceEffect
import androidx.camera.testing.impl.fakes.FakeSurfaceProcessor
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Instrumentation tests for [CameraController].
 */
@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class CameraControllerDeviceTest(
    private val implName: String,
    private val cameraConfig: CameraXConfig
) {

    companion object {
        const val TIMEOUT_SECONDS = 10L

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
            arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig())
        )
    }

    @get:Rule
    val cameraPipeConfigTestRule = CameraPipeConfigTestRule(
        active = implName == CameraPipeConfig::class.simpleName,
    )

    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        PreTestCameraIdList(cameraConfig)
    )

    private var controller: LifecycleCameraController? = null
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var activityScenario: ActivityScenario<FakeActivity>? = null
    private lateinit var context: Context
    private var cameraProvider: ProcessCameraProvider? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        CoreAppTestUtil.prepareDeviceUI(instrumentation)
        ProcessCameraProvider.configureInstance(cameraConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context).get()
        activityScenario = ActivityScenario.launch(FakeActivity::class.java)
        controller = LifecycleCameraController(context)
        controller!!.initializationFuture.get()
    }

    @After
    fun tearDown() {
        instrumentation.runOnMainSync {
            controller?.shutDownForTests()
            cameraProvider?.shutdown()?.get(10000, TimeUnit.MILLISECONDS)
            cameraProvider = null
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun setInvalidEffectsCombination_throwsException() {
        // Arrange: setup PreviewView and CameraController
        var previewView: PreviewView? = null
        activityScenario!!.onActivity {
            // Arrange.
            previewView = PreviewView(context)
            it.setContentView(previewView)
            previewView!!.controller = controller
            controller!!.bindToLifecycle(FakeLifecycleOwner())
            controller!!.initializationFuture.get()
        }
        waitUtilPreviewViewIsReady(previewView!!)

        // Act: set the same effect twice, which is invalid.
        val previewEffect1 = FakeSurfaceEffect(
            mainThreadExecutor(),
            FakeSurfaceProcessor(mainThreadExecutor())
        )
        val previewEffect2 = FakeSurfaceEffect(
            mainThreadExecutor(),
            FakeSurfaceProcessor(mainThreadExecutor())
        )
        instrumentation.runOnMainSync {
            controller!!.setEffects(
                setOf(
                    previewEffect1,
                    previewEffect2
                )
            )
        }
    }

    @Test
    fun setEffect_effectSetOnUseCase() {
        // Arrange: setup PreviewView and CameraController
        var previewView: PreviewView? = null
        activityScenario!!.onActivity {
            // Arrange.
            previewView = PreviewView(context)
            it.setContentView(previewView)
            previewView!!.controller = controller
            controller!!.bindToLifecycle(FakeLifecycleOwner())
            controller!!.initializationFuture.get()
        }
        waitUtilPreviewViewIsReady(previewView!!)

        // Act: set an effect
        val effect = FakeSurfaceEffect(
            mainThreadExecutor(), FakeSurfaceProcessor(mainThreadExecutor())
        )
        instrumentation.runOnMainSync { controller!!.setEffects(setOf(effect)) }

        // Assert: preview has effect
        assertThat(controller!!.mPreview.effect).isNotNull()

        // Act: clear the effects
        instrumentation.runOnMainSync { controller!!.clearEffects() }

        // Assert: preview no longer has the effect.
        assertThat(controller!!.mPreview.effect).isNull()
    }

    @Test
    fun setSelectorAfterBound_selectorSet() {
        // Act
        instrumentation.runOnMainSync {
            assertThat(controller!!.cameraSelector.lensFacing)
                .isEqualTo(CameraSelector.LENS_FACING_BACK)
            controller!!.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            // Assert.
            assertThat(controller!!.cameraSelector.lensFacing)
                .isEqualTo(CameraSelector.LENS_FACING_FRONT)
        }
    }

    @Test
    fun previewViewNotAttached_useCaseGroupIsNotBuilt() {
        assertThat(controller!!.createUseCaseGroup()).isNull()
    }

    @Test
    fun frontCameraFlipNotSet_imageIsMirrored() {
        // Arrange.
        instrumentation.runOnMainSync {
            controller!!.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        }
        val options = getOutputFileOptionsBuilder().build()

        // Act.
        controller!!.updateMirroringFlagInOutputFileOptions(options)

        // Assert.
        assertThat(options.metadata.isReversedHorizontal).isTrue()
    }

    @Test
    fun frontCameraFlipSetToFalse_imageIsNotMirrored() {
        // Arrange.
        instrumentation.runOnMainSync {
            controller!!.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        }
        val metadata = ImageCapture.Metadata()
        metadata.isReversedHorizontal = false
        val options = getOutputFileOptionsBuilder().setMetadata(metadata).build()

        // Act.
        controller!!.updateMirroringFlagInOutputFileOptions(options)

        // Assert.
        assertThat(options.metadata.isReversedHorizontal).isFalse()
    }

    @Test
    fun frontCameraFlipSetToTrue_imageIsMirrored() {
        // Arrange.
        instrumentation.runOnMainSync {
            controller!!.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        }
        val metadata = ImageCapture.Metadata()
        metadata.isReversedHorizontal = true
        val options = getOutputFileOptionsBuilder().setMetadata(metadata).build()

        // Act.
        controller!!.updateMirroringFlagInOutputFileOptions(options)

        // Assert.
        assertThat(options.metadata.isReversedHorizontal).isTrue()
    }

    private fun getOutputFileOptionsBuilder(): ImageCapture.OutputFileOptions.Builder {
        return ImageCapture.OutputFileOptions.Builder(
            instrumentation.context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues()
        )
    }

    @Test
    fun analysisIsEnabledByDefault() {
        instrumentation.runOnMainSync {
            assertThat(controller!!.isImageAnalysisEnabled).isTrue()
        }
    }

    @Test
    fun captureIsEnabledByDefault() {
        instrumentation.runOnMainSync {
            assertThat(controller!!.isImageCaptureEnabled).isTrue()
        }
    }

    @Test
    fun disableAnalysisCaptureEnableVideo() {
        instrumentation.runOnMainSync {
            controller!!.setEnabledUseCases(CameraController.VIDEO_CAPTURE)
            assertThat(controller!!.isImageCaptureEnabled).isFalse()
            assertThat(controller!!.isImageAnalysisEnabled).isFalse()
            assertThat(controller!!.isVideoCaptureEnabled).isTrue()
        }
    }

    @Test
    fun clearPreviewSurface_wontUnbindOthersUseCases() {
        // Arrange.
        assumeTrue(CameraUtil.hasCameraWithLensFacing(LENS_FACING_BACK))
        var cameraProvider = ProcessCameraProvider.getInstance(
            ApplicationProvider
                .getApplicationContext()
        )[10000, TimeUnit.MILLISECONDS]

        var imageCapture = ImageCapture.Builder().build()
        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(
                FakeLifecycleOwner(), CameraSelector.DEFAULT_BACK_CAMERA,
                imageCapture
            )
        }

        assertThat(cameraProvider.isBound(imageCapture)).isTrue()

        controller!!.initializationFuture[10000, TimeUnit.MILLISECONDS]

        // Act.
        instrumentation.runOnMainSync {
            controller!!.clearPreviewSurface()
        }

        // Assert.
        assertThat(cameraProvider.isBound(imageCapture)).isTrue()
    }

    @Test
    fun setCameraSelector_wontUnbindOthersUseCases() {
        testCameraSelectorWontUnbindUseCases(
            CameraSelector.DEFAULT_BACK_CAMERA,
            CameraSelector.DEFAULT_BACK_CAMERA
        )
        testCameraSelectorWontUnbindUseCases(
            CameraSelector.DEFAULT_FRONT_CAMERA,
            CameraSelector.DEFAULT_FRONT_CAMERA
        )
        testCameraSelectorWontUnbindUseCases(
            CameraSelector.DEFAULT_BACK_CAMERA,
            CameraSelector.DEFAULT_FRONT_CAMERA
        )
        testCameraSelectorWontUnbindUseCases(
            CameraSelector.DEFAULT_FRONT_CAMERA,
            CameraSelector.DEFAULT_BACK_CAMERA
        )
    }

    private fun testCameraSelectorWontUnbindUseCases(
        firstCamera: CameraSelector,
        secondCamera: CameraSelector
    ) {
        // Arrange.
        assumeTrue(CameraUtil.hasCameraWithLensFacing(LENS_FACING_BACK))
        assumeTrue(CameraUtil.hasCameraWithLensFacing(LENS_FACING_FRONT))
        var cameraProvider = ProcessCameraProvider.getInstance(
            ApplicationProvider
                .getApplicationContext()
        )[10000, TimeUnit.MILLISECONDS]

        var imageCapture = ImageCapture.Builder().build()
        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(
                FakeLifecycleOwner(), firstCamera,
                imageCapture
            )
        }

        assertThat(cameraProvider.isBound(imageCapture)).isTrue()

        controller!!.initializationFuture[10000, TimeUnit.MILLISECONDS]

        // Act.
        instrumentation.runOnMainSync {
            controller!!.cameraSelector = secondCamera
        }

        // Assert.
        assertThat(cameraProvider.isBound(imageCapture)).isTrue()
    }

    private fun waitUtilPreviewViewIsReady(previewView: PreviewView) {
        val countDownLatch = CountDownLatch(1)
        previewView.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int
            ) {
                if (v.width > 0 && v.height > 0) {
                    countDownLatch.countDown()
                    previewView.removeOnLayoutChangeListener(this)
                }
            }
        })
        assertThat(countDownLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
    }
}
