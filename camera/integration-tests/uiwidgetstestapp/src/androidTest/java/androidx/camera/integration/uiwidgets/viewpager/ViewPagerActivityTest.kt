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

package androidx.camera.integration.uiwidgets.viewpager

import android.content.Context
import android.content.Intent
import android.graphics.SurfaceTexture
import android.view.TextureView
import android.view.View
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraSelector
import androidx.camera.integration.uiwidgets.R
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.camera.testing.impl.InternalTestConvenience.useInCameraTest
import androidx.camera.view.PreviewView
import androidx.lifecycle.Lifecycle.State
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@LargeTest
class ViewPagerActivityTest(private val lensFacing: Int, private val cameraXConfig: String) {

    companion object {
        private const val ACTION_IDLE_TIMEOUT: Long = 5000

        @JvmStatic
        private val lensFacingList =
            arrayOf(CameraSelector.LENS_FACING_BACK, CameraSelector.LENS_FACING_FRONT)

        @JvmStatic
        private val cameraXConfigList =
            arrayOf(
                CameraFragment.CAMERA2_IMPLEMENTATION_OPTION,
                CameraFragment.CAMERA_PIPE_IMPLEMENTATION_OPTION
            )

        @JvmStatic
        @Parameterized.Parameters(name = "lensFacing={0}, cameraXConfig={1}")
        fun data() =
            mutableListOf<Array<Any?>>().apply {
                lensFacingList.forEach { lens ->
                    cameraXConfigList.forEach { cameraXConfig -> add(arrayOf(lens, cameraXConfig)) }
                }
            }

        @JvmField val testCameraRule = CameraUtil.PreTestCamera()
    }

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(
            active = cameraXConfig == CameraFragment.CAMERA_PIPE_IMPLEMENTATION_OPTION,
        )

    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            testCameraRule,
            CameraUtil.PreTestCameraIdList(
                if (cameraXConfig == CameraFragment.CAMERA2_IMPLEMENTATION_OPTION) {
                    Camera2Config.defaultConfig()
                } else {
                    CameraPipeConfig.defaultConfig()
                }
            )
        )

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(lensFacing))

        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        // Ensure it's in a natural orientation. This change could delay around 1 sec, please
        // call this earlier before launching the test activity.
        device.setOrientationNatural()

        // Clear the device UI and check if there is no dialog or lock screen on the top of the
        // window.
        CoreAppTestUtil.prepareDeviceUI(InstrumentationRegistry.getInstrumentation())
    }

    @After
    fun tearDown() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
        cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS]
        if (::device.isInitialized) {
            device.unfreezeRotation()
        }
    }

    // The test makes sure the camera PreviewView is in the streaming state.
    @Test
    fun testPreviewViewUpdateAfterStopResume() {
        launchActivity(lensFacing, cameraXConfig).useInCameraTest { scenario ->
            // At first, check Preview in stream state
            assertStreamState(scenario, PreviewView.StreamState.STREAMING)

            // Go through Stop/Resume and then check Preview in stream state still.
            scenario.moveToState(State.CREATED)
            scenario.moveToState(State.RESUMED)

            assertStreamState(scenario, PreviewView.StreamState.STREAMING)
            // Make sure the surface texture of TextureView continues getting updates.
            assertSurfaceTextureFramesUpdate(scenario)
        }
    }

    // The test makes sure the TextureView surface texture keeps the same after switch.
    @Test
    fun testPreviewViewUpdateAfterSwitch() {
        launchActivity(lensFacing, cameraXConfig).useInCameraTest { scenario ->
            // At first, check Preview in stream state
            assertStreamState(scenario, PreviewView.StreamState.STREAMING)

            // Switch from CameraFragment to BlankFragment, and then switch back to check Preview
            // update
            onView(withText(ViewPagerActivity.BLANK_FRAGMENT_TAB_TITLE)).perform(click())
            onView(withId(R.id.blank_textview)).check(matches(isDisplayed()))

            onView(withText(ViewPagerActivity.CAMERA_FRAGMENT_TAB_TITLE)).perform(click())
            onView(withId(R.id.preview_textureview)).check(matches(isDisplayed()))
            // Check if the surface texture of TextureView continues getting updates after
            // detaching from window and then attaching to window.
            assertSurfaceTextureFramesUpdate(scenario)
        }
    }

    @Ignore(
        "b/344664219, b/346947822 - Enable after revamp since test does not work as intended" +
            " and the test passing does not indicate actual passing"
    )
    @Test
    fun testPreviewViewUpdateAfterSwitchOutAndStop_ResumeAndSwitchBack() {
        launchActivity(lensFacing, cameraXConfig).useInCameraTest { scenario ->
            // At first, check Preview in stream state
            assertStreamState(scenario, PreviewView.StreamState.STREAMING)

            // Switch from CameraFragment to BlankFragment, and then Stop and Resume
            // ViewPagerActivity
            onView(withText(ViewPagerActivity.BLANK_FRAGMENT_TAB_TITLE)).perform(click())
            onView(withId(R.id.blank_textview)).check(matches(isDisplayed()))

            scenario.moveToState(State.CREATED)
            scenario.moveToState(State.RESUMED)
            device.waitForIdle(ACTION_IDLE_TIMEOUT)

            // After resume, switch back to CameraFragment, to check Preview in stream state
            onView(withText(ViewPagerActivity.CAMERA_FRAGMENT_TAB_TITLE)).perform(click())
            onView(withId(R.id.preview_textureview)).check(matches(isDisplayed()))
            assertStreamState(scenario, PreviewView.StreamState.STREAMING)

            // The test covers pause/resume and ViewPager switch behaviors. Hence, need to
            // check if the surface texture of TextureView continues getting updates, or not.
            assertSurfaceTextureFramesUpdate(scenario)
        }
    }

    // TODO(b/162810853): Add tests for PreviewView with PreviewView.ImplementationMode
    //  .SURFACE_VIEW in ViewPagerActivity.

    private fun launchActivity(
        lensFacing: Int,
        cameraXConfig: String = CameraFragment.CAMERA2_IMPLEMENTATION_OPTION,
    ): ActivityScenario<ViewPagerActivity> {
        val intent =
            Intent(
                    ApplicationProvider.getApplicationContext<Context>(),
                    ViewPagerActivity::class.java
                )
                .apply {
                    putExtra(BaseActivity.INTENT_LENS_FACING, lensFacing)
                    putExtra(CameraFragment.KEY_CAMERA_IMPLEMENTATION, cameraXConfig)
                    putExtra(CameraFragment.KEY_CAMERA_IMPLEMENTATION_NO_HISTORY, true)
                }
        return ActivityScenario.launch<ViewPagerActivity>(intent)
    }

    private fun getTextureView(previewView: PreviewView): TextureView? {
        var index: Int = 0
        var textureView: TextureView? = null
        lateinit var childView: View

        while (index < previewView.childCount) {
            childView = previewView.getChildAt(index)
            if (childView is TextureView) {
                textureView = childView
                break
            }
            index++
        }
        return textureView
    }

    private fun assertStreamState(
        scenario: ActivityScenario<ViewPagerActivity>,
        expectStreamState: PreviewView.StreamState
    ) =
        runBlocking<Unit> {
            lateinit var result: Deferred<Boolean>

            scenario.onActivity { activity ->
                // Make async Coroutine to wait the result, not block the test thread.
                result = async { activity.waitForStreamState(expectStreamState) }
            }

            assertThat(result.await()).isTrue()
        }

    private fun assertSurfaceTextureFramesUpdate(scenario: ActivityScenario<ViewPagerActivity>) {
        var newSurfaceTexture: SurfaceTexture? = null
        lateinit var previewView: PreviewView

        scenario.onActivity { activity ->
            previewView = activity.findViewById(R.id.preview_textureview)
            newSurfaceTexture = getTextureView(previewView)!!.surfaceTexture
        }

        val latchForFrameUpdate = CountDownLatch(1)
        newSurfaceTexture!!.setOnFrameAvailableListener { _ -> latchForFrameUpdate.countDown() }
        assertThat(latchForFrameUpdate.await(ACTION_IDLE_TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
    }
}
