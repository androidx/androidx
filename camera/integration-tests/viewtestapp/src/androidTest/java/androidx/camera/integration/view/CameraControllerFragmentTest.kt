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

package androidx.camera.integration.view

import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.testing.CameraUtil
import androidx.camera.view.PreviewView
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

const val TIMEOUT_SECONDS = 3L

/**
 * Instrument tests for {@link CameraControllerFragment} and {@link CameraController}.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class CameraControllerFragmentTest {

    @get:Rule
    val thrown: ExpectedException = ExpectedException.none()

    @get:Rule
    val useCamera: TestRule = CameraUtil.grantCameraPermissionAndPreTest()

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.RECORD_AUDIO
    )

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun fragmentLaunched_canTakePicture() {
        val fragment = createFragmentScenario().getFragment()
        fragment.assertPreviewIsStreaming()
        fragment.assertCanTakePicture()
    }

    @Test
    fun captureDisabled_cannotTakePicture() {
        // Arrange.
        thrown.expectMessage("ImageCapture disabled")
        val fragment = createFragmentScenario().getFragment()
        fragment.assertPreviewIsStreaming()
        instrumentation.runOnMainSync {
            fragment.cameraController.isImageCaptureEnabled = false
        }

        // Act & assert.
        fragment.assertCanTakePicture()
    }

    @Test
    fun captureDisabledAndEnabled_canTakePicture() {
        // Arrange.
        val fragment = createFragmentScenario().getFragment()
        fragment.assertPreviewIsStreaming()

        // Act.
        instrumentation.runOnMainSync {
            fragment.cameraController.isImageCaptureEnabled = false
            fragment.cameraController.isImageCaptureEnabled = true
        }
        fragment.assertPreviewIsStreaming()

        // Assert.
        fragment.assertCanTakePicture()
    }

    @Test
    fun previewViewRemoved_previewIsIdle() {
        val fragment = createFragmentScenario().getFragment()
        onView(withId(R.id.remove_or_add)).perform(click())
        fragment.assertPreviewIsIdle()
    }

    @Test
    fun previewViewRemovedAndAdded_previewIsStreaming() {
        val fragment = createFragmentScenario().getFragment()
        onView(withId(R.id.remove_or_add)).perform(click())
        onView(withId(R.id.remove_or_add)).perform(click())
        fragment.assertPreviewIsStreaming()
    }

    @Test
    fun cameraToggled_previewIsStreaming() {
        val fragment = createFragmentScenario().getFragment()
        onView(withId(R.id.camera_toggle)).perform(click())
        fragment.assertPreviewIsStreaming()
    }

    @Test
    fun cameraToggled_canTakePicture() {
        val fragment = createFragmentScenario().getFragment()
        onView(withId(R.id.camera_toggle)).perform(click())
        fragment.assertPreviewIsStreaming()
        fragment.assertCanTakePicture()
    }

    /**
     * Takes a picture and assert the URI exists.
     *
     * <p> Also cleans up the saved picture afterwards.
     */
    private fun CameraControllerFragment.assertCanTakePicture() {
        val imageCallbackSemaphore = Semaphore(0)
        var uri: Uri? = null
        instrumentation.runOnMainSync {
            this.takePicture(object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    uri = outputFileResults.savedUri
                    imageCallbackSemaphore.release()
                }

                override fun onError(exception: ImageCaptureException) {
                    imageCallbackSemaphore.release()
                }
            })
        }
        assertThat(imageCallbackSemaphore.tryAcquire(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
        assertThat(uri).isNotNull()
        // Delete the saved picture. Assert 1 row was deleted.
        assertThat(this.activity!!.contentResolver.delete(uri!!, null, null)).isEqualTo(1)
    }

    private fun createFragmentScenario(): FragmentScenario<CameraControllerFragment?> {
        return FragmentScenario.launchInContainer(
            CameraControllerFragment::class.java, null, R.style.AppTheme,
            null
        ).also {
            it.moveToState(Lifecycle.State.CREATED)
            it.moveToState(Lifecycle.State.RESUMED)
        }
    }

    private fun FragmentScenario<CameraControllerFragment?>.getFragment():
            CameraControllerFragment {
        var fragment: CameraControllerFragment? = null
        this.onFragment { newValue: CameraControllerFragment -> fragment = newValue }
        return fragment!!
    }

    private fun CameraControllerFragment.assertPreviewIsStreaming() {
        assertPreviewState(PreviewView.StreamState.STREAMING)
    }

    private fun CameraControllerFragment.assertPreviewIsIdle() {
        assertPreviewState(PreviewView.StreamState.IDLE)
    }

    private fun CameraControllerFragment.assertPreviewState(state: PreviewView.StreamState) {
        val previewStreaming = Semaphore(0)
        instrumentation.runOnMainSync {
            this.observePreviewStreamState(Observer {
                if (it == state) {
                    previewStreaming.release()
                }
            })
        }
        assertThat(previewStreaming.tryAcquire(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
    }
}