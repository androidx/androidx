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
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.LENS_FACING_BACK
import androidx.camera.core.CameraSelector.LENS_FACING_FRONT
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * Instrumentation tests for [CameraController].
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class CameraControllerDeviceTest {

    @get:Rule
    val useCamera: TestRule = CameraUtil.grantCameraPermissionAndPreTest()

    private val controller = LifecycleCameraController(ApplicationProvider.getApplicationContext())
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Before
    fun setUp() {
        controller.initializationFuture.get()
    }

    @After
    fun tearDown() {
        instrumentation.runOnMainSync {
            controller.shutDownForTests()
        }
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
}