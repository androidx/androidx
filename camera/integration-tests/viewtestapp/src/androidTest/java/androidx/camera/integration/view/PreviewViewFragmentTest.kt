/*
 * Copyright 2019 The Android Open Source Project
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

import android.Manifest
import android.content.Context
import android.view.View
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.camera.view.PreviewView
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@LargeTest
class PreviewViewFragmentTest(
    private val implName: String,
    private val cameraConfig: CameraXConfig,
) {
    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName == CameraPipeConfig::class.simpleName)

    @get:Rule
    var useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(PreTestCameraIdList(cameraConfig))

    @get:Rule
    var storagePermissionRule =
        GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @get:Rule var audioPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var scenario: FragmentScenario<PreviewViewFragment>? = null
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    @Throws(CoreAppTestUtil.ForegroundOccupiedError::class)
    fun setup() {
        Assume.assumeTrue(CameraUtil.deviceHasCamera())
        CoreAppTestUtil.assumeCompatibleDevice()
        // Clear the device UI and check if there is no dialog or lock screen on the top of the
        // window before start the test.
        CoreAppTestUtil.prepareDeviceUI(instrumentation)
        ProcessCameraProvider.configureInstance(cameraConfig)
        scenario = createScenario()
    }

    @After
    fun tearDown() {
        if (scenario != null) {
            scenario!!.moveToState(Lifecycle.State.DESTROYED)
        }
        ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS].shutdownAsync()
    }

    @Test
    fun checkPreviewUpdatedAfterDestroyRecreate() {
        assertPreviewUpdating(scenario!!)

        // Recreate fragment
        scenario!!.recreate()
        assertPreviewUpdating(scenario!!)
    }

    @Test
    fun checkPreviewUpdatedAfterStopResume_3Times() {
        assertPreviewUpdating(scenario!!)

        // Stop the fragment
        scenario!!.moveToState(Lifecycle.State.CREATED)
        // Resume the fragment
        scenario!!.moveToState(Lifecycle.State.RESUMED)
        assertPreviewUpdating(scenario!!)

        // Stop the fragment
        scenario!!.moveToState(Lifecycle.State.CREATED)
        // Resume the fragment
        scenario!!.moveToState(Lifecycle.State.RESUMED)
        assertPreviewUpdating(scenario!!)

        // Stop the fragment
        scenario!!.moveToState(Lifecycle.State.CREATED)
        // Resume the fragment
        scenario!!.moveToState(Lifecycle.State.RESUMED)
        assertPreviewUpdating(scenario!!)
    }

    @Test
    fun checkPreviewUpdatedAfterToggleCameraAndStopResume() {
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))
        assertPreviewUpdating(scenario!!)

        // Toggle camera
        Espresso.onView(ViewMatchers.withId(R.id.toggle_camera)).perform(ViewActions.click())

        // Stop the fragment
        scenario!!.moveToState(Lifecycle.State.CREATED)
        // Resume the fragment
        scenario!!.moveToState(Lifecycle.State.RESUMED)
        assertPreviewUpdating(scenario!!)
    }

    @Test
    fun checkPreviewUpdatedAfterToggleCameraTwiceAndStopResume() {
        // This is to test that after toggling between the front and back camera the front camera
        // will still be working after stopping and resuming the lifecycle state.
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))

        // Toggle camera. This is to toggle the camera from back to front.
        Espresso.onView(ViewMatchers.withId(R.id.toggle_camera)).perform(ViewActions.click())

        // Toggle camera twice. This is to toggle the camera from front to back.
        Espresso.onView(ViewMatchers.withId(R.id.toggle_camera)).perform(ViewActions.click())

        // Stop the fragment
        scenario!!.moveToState(Lifecycle.State.CREATED)
        // Resume the fragment
        scenario!!.moveToState(Lifecycle.State.RESUMED)
        assertPreviewUpdating(scenario!!)
    }

    @Test
    fun checkPreviewUpdatesAfterToggleCameraMultipleTimes() {
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))
        assertPreviewUpdating(scenario!!)
        for (i in 0..10) {
            Espresso.onView(ViewMatchers.withId(R.id.toggle_camera)).perform(ViewActions.click())
        }
        assertPreviewUpdating(scenario!!)
    }

    @Test
    fun checkPreviewNotUpdatedAfterPreviewUnbound() {
        assertPreviewUpdating(scenario!!)

        // Toggle visibility to unbind preview
        Espresso.onView(ViewMatchers.withId(R.id.toggle_visibility)).perform(ViewActions.click())
        assertPreviewNotUpdating(scenario!!)
    }

    @Test
    fun checkPreviewUpdatedWhenSamePreviewViewAttachedToMultiplePreviewUseCases() {
        assertPreviewUpdating(scenario!!)

        // Toggle visibility to unbind preview
        Espresso.onView(ViewMatchers.withId(R.id.toggle_visibility)).perform(ViewActions.click())
        // Toggle visibility to bind new preview
        Espresso.onView(ViewMatchers.withId(R.id.toggle_visibility)).perform(ViewActions.click())
        assertPreviewUpdating(scenario!!)
    }

    @Test
    fun checkSameScaleTypeAfterStopResume() {
        assertPreviewUpdating(scenario!!)
        instrumentation.runOnMainSync {
            getPreviewView(scenario!!).scaleType = PreviewView.ScaleType.FIT_END
        }

        // Stop the fragment
        scenario!!.moveToState(Lifecycle.State.CREATED)
        // Resume the fragment
        scenario!!.moveToState(Lifecycle.State.RESUMED)
        instrumentation.runOnMainSync {
            Truth.assertThat(getPreviewView(scenario!!).scaleType)
                .isEqualTo(PreviewView.ScaleType.FIT_END)
        }
    }

    @Test
    fun checkSameImplementationModeAfterStopResume() {
        assertPreviewUpdating(scenario!!)
        instrumentation.runOnMainSync {
            getPreviewView(scenario!!).implementationMode =
                PreviewView.ImplementationMode.COMPATIBLE
        }

        // Stop the fragment
        scenario!!.moveToState(Lifecycle.State.CREATED)
        // Resume the fragment
        scenario!!.moveToState(Lifecycle.State.RESUMED)
        instrumentation.runOnMainSync {
            Truth.assertThat(getPreviewView(scenario!!).implementationMode)
                .isEqualTo(PreviewView.ImplementationMode.COMPATIBLE)
        }
    }

    private fun createScenario(): FragmentScenario<PreviewViewFragment> {
        return FragmentScenario.launchInContainer(
            PreviewViewFragment::class.java,
            null,
            R.style.AppTheme,
            FragmentFactory(),
        )
    }

    private fun assertPreviewUpdating(scenario: FragmentScenario<PreviewViewFragment>) {
        // Waits at most for the duration [.TIMEOUT_SECONDS] for the preview to update at least
        // [.PREVIEW_UPDATE_COUNT] times.
        val fragment = AtomicReference<PreviewViewFragment>()
        scenario.onFragment { newValue: PreviewViewFragment -> fragment.set(newValue) }
        val latch = CountDownLatch(PREVIEW_UPDATE_COUNT)
        fragment.get().setPreviewUpdatingLatch(latch)
        val isPreviewUpdating: Boolean
        isPreviewUpdating =
            try {
                latch.await(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                false
            }
        Truth.assertThat(isPreviewUpdating).isTrue()
    }

    private fun assertPreviewNotUpdating(scenario: FragmentScenario<PreviewViewFragment>) {
        val fragment = AtomicReference<PreviewViewFragment>()
        scenario.onFragment { newValue: PreviewViewFragment -> fragment.set(newValue) }
        val notUpdatingLatch = CountDownLatch(1)
        fragment.get().setPreviewNotUpdatingLatch(notUpdatingLatch)
        Truth.assertThat(notUpdatingLatch.await(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS))
            .isTrue()
    }

    private fun getPreviewView(scenario: FragmentScenario<PreviewViewFragment>): PreviewView {
        val previewView = AtomicReference<PreviewView>()
        scenario.onFragment { fragment: PreviewViewFragment ->
            previewView.set(
                fragment.requireActivity().findViewById<View>(R.id.preview_view) as PreviewView
            )
        }
        return previewView.get()
    }

    companion object {
        private const val TIMEOUT_SECONDS = 10
        private const val PREVIEW_UPDATE_COUNT = 30

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
                arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig()),
            )
    }
}
