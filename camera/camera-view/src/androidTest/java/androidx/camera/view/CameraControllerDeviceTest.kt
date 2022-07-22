/*
 * Copyright 2020 The Android Open Source Project
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
import android.provider.MediaStore
import android.view.View
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.LENS_FACING_BACK
import androidx.camera.core.CameraSelector.LENS_FACING_FRONT
import androidx.camera.core.EffectBundle
import androidx.camera.core.ImageCapture
import androidx.camera.core.SurfaceEffect
import androidx.camera.core.SurfaceEffect.PREVIEW
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.CoreAppTestUtil
import androidx.camera.testing.fakes.FakeActivity
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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

/**
 * Instrumentation tests for [CameraController].
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class CameraControllerDeviceTest {

    companion object {
        const val TIMEOUT_SECONDS = 10L
    }

    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    private lateinit var controller: LifecycleCameraController
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var activityScenario: ActivityScenario<FakeActivity>? = null

    @Before
    fun setUp() {
        CoreAppTestUtil.prepareDeviceUI(instrumentation)
        activityScenario = ActivityScenario.launch(FakeActivity::class.java)
        controller = LifecycleCameraController(instrumentation.context)
        controller.initializationFuture.get()
    }

    @After
    fun tearDown() {
        instrumentation.runOnMainSync {
            controller.shutDownForTests()
        }
    }

    @Test
    fun setEffectBundle_effectSetOnUseCase() {
        // Arrange: setup PreviewView and CameraController
        var previewView: PreviewView? = null
        activityScenario!!.onActivity {
            // Arrange.
            previewView = PreviewView(instrumentation.context)
            it.setContentView(previewView)
            previewView!!.controller = controller
            controller.bindToLifecycle(FakeLifecycleOwner())
            controller.initializationFuture.get()
        }
        waitUtilPreviewViewIsReady(previewView!!)

        // Act: set an EffectBundle
        instrumentation.runOnMainSync {
            val surfaceEffect = object : SurfaceEffect {
                override fun onInputSurface(request: SurfaceRequest) {}

                override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
                    surfaceOutput.close()
                }
            }
            controller.setEffectBundle(
                EffectBundle.Builder(mainThreadExecutor()).addEffect(PREVIEW, surfaceEffect).build()
            )
        }

        // Assert: preview has effect
        assertThat(controller.mPreview.effect).isNotNull()

        // Act: clear the EffectBundle
        instrumentation.runOnMainSync {
            controller.setEffectBundle(null)
        }

        // Assert: preview no longer has the effect.
        assertThat(controller.mPreview.effect).isNull()
    }

    @UiThreadTest
    @Test
    fun setSelectorAfterBound_selectorSet() {
        // Act
        assertThat(controller.cameraSelector.lensFacing).isEqualTo(CameraSelector.LENS_FACING_BACK)
        controller.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        // Assert.
        assertThat(controller.cameraSelector.lensFacing).isEqualTo(CameraSelector.LENS_FACING_FRONT)
    }

    @UiThreadTest
    @Test
    fun previewViewNotAttached_useCaseGroupIsNotBuilt() {
        assertThat(controller.createUseCaseGroup()).isNull()
    }

    @UiThreadTest
    @Test
    fun frontCameraFlipNotSet_imageIsMirrored() {
        // Arrange.
        controller.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        val options = getOutputFileOptionsBuilder().build()

        // Act.
        controller.updateMirroringFlagInOutputFileOptions(options)

        // Assert.
        assertThat(options.metadata.isReversedHorizontal).isTrue()
    }

    @UiThreadTest
    @Test
    fun frontCameraFlipSetToFalse_imageIsNotMirrored() {
        // Arrange.
        controller.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        val metadata = ImageCapture.Metadata()
        metadata.isReversedHorizontal = false
        val options = getOutputFileOptionsBuilder().setMetadata(metadata).build()

        // Act.
        controller.updateMirroringFlagInOutputFileOptions(options)

        // Assert.
        assertThat(options.metadata.isReversedHorizontal).isFalse()
    }

    @UiThreadTest
    @Test
    fun frontCameraFlipSetToTrue_imageIsMirrored() {
        // Arrange.
        controller.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        val metadata = ImageCapture.Metadata()
        metadata.isReversedHorizontal = true
        val options = getOutputFileOptionsBuilder().setMetadata(metadata).build()

        // Act.
        controller.updateMirroringFlagInOutputFileOptions(options)

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

    @UiThreadTest
    @Test
    fun analysisIsEnabledByDefault() {
        assertThat(controller.isImageAnalysisEnabled).isTrue()
    }

    @UiThreadTest
    @Test
    fun captureIsEnabledByDefault() {
        assertThat(controller.isImageCaptureEnabled).isTrue()
    }

    @UiThreadTest
    @Test
    fun disableAnalysisCaptureEnableVideo() {
        controller.setEnabledUseCases(CameraController.VIDEO_CAPTURE)
        assertThat(controller.isImageCaptureEnabled).isFalse()
        assertThat(controller.isImageAnalysisEnabled).isFalse()
        assertThat(controller.isVideoCaptureEnabled).isTrue()
    }

    @UiThreadTest
    @Test
    fun clearPreviewSurface_wontUnbindOthersUseCases() {
        // Arrange.
        assumeTrue(CameraUtil.hasCameraWithLensFacing(LENS_FACING_BACK))
        var cameraProvider = ProcessCameraProvider.getInstance(
            ApplicationProvider
                .getApplicationContext()
        )[10000, TimeUnit.MILLISECONDS]

        var imageCapture = ImageCapture.Builder().build()
        cameraProvider.bindToLifecycle(
            FakeLifecycleOwner(), CameraSelector.DEFAULT_BACK_CAMERA,
            imageCapture
        )

        assertThat(cameraProvider.isBound(imageCapture)).isTrue()

        controller.initializationFuture[10000, TimeUnit.MILLISECONDS]

        // Act.
        controller.clearPreviewSurface()

        // Assert.
        assertThat(cameraProvider.isBound(imageCapture)).isTrue()
    }

    @UiThreadTest
    @Test
    fun setCameraSelector_wontUnbindOthersUseCases() {
        // Arrange.
        assumeTrue(CameraUtil.hasCameraWithLensFacing(LENS_FACING_BACK))
        assumeTrue(CameraUtil.hasCameraWithLensFacing(LENS_FACING_FRONT))
        var cameraProvider = ProcessCameraProvider.getInstance(
            ApplicationProvider
                .getApplicationContext()
        )[10000, TimeUnit.MILLISECONDS]

        var imageCapture = ImageCapture.Builder().build()
        cameraProvider.bindToLifecycle(
            FakeLifecycleOwner(), CameraSelector.DEFAULT_BACK_CAMERA,
            imageCapture
        )

        assertThat(cameraProvider.isBound(imageCapture)).isTrue()

        controller.initializationFuture[10000, TimeUnit.MILLISECONDS]
        controller.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        // Act.
        controller.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

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