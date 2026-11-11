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
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Checkable
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.camera.testing.impl.RequireForegroundRule
import androidx.camera.view.PreviewView
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
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
    val requireForegroundRule = RequireForegroundRule {
        Assume.assumeTrue(CameraUtil.deviceHasCamera())
        CoreAppTestUtil.assumeCompatibleDevice()
    }

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
    fun setup() {
        ProcessCameraProvider.configureInstance(cameraConfig)
        requireForegroundRule.deferCleanup {
            val provider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
            provider.shutdownAsync()[10, TimeUnit.SECONDS]
        }
        scenario = createScenario()
    }

    @After
    fun tearDown() {
        if (scenario != null) {
            scenario!!.moveToState(Lifecycle.State.DESTROYED)
        }
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
        clickView(R.id.toggle_camera)

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
        assertPreviewUpdating(scenario!!)

        // Toggle camera. This is to toggle the camera from back to front.
        clickView(R.id.toggle_camera)

        // Toggle camera twice. This is to toggle the camera from front to back.
        clickView(R.id.toggle_camera)

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
            clickView(R.id.toggle_camera)
        }
        assertPreviewUpdating(scenario!!)
    }

    @Test
    fun checkPreviewNotUpdatedAfterPreviewUnbound() {
        assertPreviewUpdating(scenario!!)

        // Toggle visibility to unbind preview
        clickView(R.id.toggle_visibility)
        assertPreviewNotUpdating(scenario!!)
    }

    @Test
    fun checkPreviewUpdatedWhenSamePreviewViewAttachedToMultiplePreviewUseCases() {
        assertPreviewUpdating(scenario!!)

        // Toggle visibility to unbind preview
        clickView(R.id.toggle_visibility)
        // Toggle visibility to bind new preview
        clickView(R.id.toggle_visibility)
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

    private fun clickView(viewId: Int) {
        waitUntilViewReady(viewId)
        checkNotNull(scenario).onFragment { fragment ->
            val view = fragment.requireActivity().findViewById<View>(viewId)
            Truth.assertWithMessage("View $viewId not found").that(view).isNotNull()
            if (!view!!.performClick() && view !is Checkable) {
                throw RuntimeException("Failed to click view $viewId")
            }
        }
        instrumentation.waitForIdleSync()
    }

    /**
     * Waits until the view with [viewId] is able to handle a click.
     *
     * [PreviewViewFragment] enables its UI controls and sets their click listeners only after
     * [ProcessCameraProvider] is initialized and the fragment view is attached to a window, both of
     * which may complete asynchronously after the fragment is resumed.
     */
    private fun waitUntilViewReady(viewId: Int) {
        val fragment = AtomicReference<PreviewViewFragment>()
        checkNotNull(scenario).onFragment { fragment.set(it) }
        val readyLatch = CountDownLatch(1)
        val mainHandler = Handler(Looper.getMainLooper())
        // The view state is only mutated on the main thread, so check it there and re-check
        // periodically until it is ready.
        val readinessCheck =
            object : Runnable {
                override fun run() {
                    if (isReadyToClick(fragment.get().activity?.findViewById(viewId))) {
                        readyLatch.countDown()
                    } else {
                        mainHandler.postDelayed(this, VIEW_READY_CHECK_INTERVAL_MS)
                    }
                }
            }
        mainHandler.post(readinessCheck)
        val isReady =
            try {
                readyLatch.await(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
            } finally {
                // Removes the pending check on the main thread so that it can't re-post itself.
                instrumentation.runOnMainSync { mainHandler.removeCallbacks(readinessCheck) }
            }
        if (!isReady) {
            val viewState = AtomicReference<String>()
            instrumentation.runOnMainSync {
                viewState.set(describeClickReadiness(fragment.get().activity?.findViewById(viewId)))
            }
            Truth.assertWithMessage(
                    "View $viewId is not ready to be clicked within $TIMEOUT_SECONDS seconds: " +
                        viewState.get()
                )
                .fail()
        }
    }

    private fun isReadyToClick(view: View?): Boolean =
        view != null &&
            view.isAttachedToWindow &&
            view.visibility == View.VISIBLE &&
            view.isEnabled &&
            (view.hasOnClickListeners() || view is Checkable)

    private fun describeClickReadiness(view: View?): String =
        if (view == null) {
            "view not found"
        } else {
            "isAttachedToWindow=${view.isAttachedToWindow}, visibility=${view.visibility}, " +
                "isEnabled=${view.isEnabled}, hasOnClickListeners=${view.hasOnClickListeners()}, " +
                "isCheckable=${view is Checkable}"
        }

    companion object {
        private const val TIMEOUT_SECONDS = 10
        private const val PREVIEW_UPDATE_COUNT = 30
        private const val VIEW_READY_CHECK_INTERVAL_MS = 50L

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()))
    }
}
