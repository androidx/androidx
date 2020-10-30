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
import androidx.camera.core.ImageCapture
import androidx.camera.testing.CameraUtil
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * Instrumentation tests for [CameraController].
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
public class CameraControllerDeviceTest {

    @get:Rule
    public val useCamera: TestRule = CameraUtil.grantCameraPermissionAndPreTest()

    private val controller = LifecycleCameraController(ApplicationProvider.getApplicationContext())
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Before
    public fun setUp() {
        controller.initializationFuture.get()
    }

    @After
    public fun tearDown() {
        instrumentation.runOnMainSync {
            controller.shutDownForTests()
        }
    }

    @UiThreadTest
    @Test
    public fun previewViewNotAttached_useCaseGroupIsNotBuilt() {
        assertThat(controller.createUseCaseGroup()).isNull()
    }

    @UiThreadTest
    @Test
    public fun frontCameraFlipNotSet_imageIsMirrored() {
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
    public fun frontCameraFlipSetToFalse_imageIsNotMirrored() {
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
    public fun frontCameraFlipSetToTrue_imageIsMirrored() {
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
    public fun analysisIsEnabledByDefault() {
        assertThat(controller.isImageAnalysisEnabled).isTrue()
    }

    @UiThreadTest
    @Test
    public fun captureIsEnabledByDefault() {
        assertThat(controller.isImageCaptureEnabled).isTrue()
    }

    @UiThreadTest
    @Test
    public fun disableAnalysisCaptureEnableVideo() {
        controller.setEnabledUseCases(CameraController.VIDEO_CAPTURE)
        assertThat(controller.isImageCaptureEnabled).isFalse()
        assertThat(controller.isImageAnalysisEnabled).isFalse()
        assertThat(controller.isVideoCaptureEnabled).isTrue()
    }
}