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
import android.os.Build
import android.view.TextureView
import android.view.View
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraSelector
import androidx.camera.integration.uiwidgets.R
import androidx.camera.integration.uiwidgets.viewpager.BaseActivity.Companion.COMPATIBLE_MODE
import androidx.camera.integration.uiwidgets.viewpager.BaseActivity.Companion.PERFORMANCE_MODE
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.AndroidUtil.isEmulator
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
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
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
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@LargeTest
class ViewPager2ActivityTest(
    private val lensFacing: Int,
    private val implementationMode: Int,
    private val cameraXConfig: String
) {

    companion object {
        private const val ACTION_IDLE_TIMEOUT: Long = 5000
        private const val PREVIEW_UPDATE_COUNT = 10

        @JvmStatic
        private val lensFacingList =
            arrayOf(CameraSelector.LENS_FACING_BACK, CameraSelector.LENS_FACING_FRONT)

        @JvmStatic private val implementationModeList = arrayOf(COMPATIBLE_MODE, PERFORMANCE_MODE)

        @JvmStatic
        private val cameraXConfigList =
            arrayOf(
                CameraFragment.CAMERA2_IMPLEMENTATION_OPTION,
                CameraFragment.CAMERA_PIPE_IMPLEMENTATION_OPTION
            )

        @JvmStatic
        @Parameterized.Parameters(name = "lensFacing={0}, mode={1}, cameraXConfig={2}")
        fun data() =
            mutableListOf<Array<Any?>>().apply {
                lensFacingList.forEach { lens ->
                    implementationModeList.forEach { mode ->
                        cameraXConfigList.forEach { cameraXConfig ->
                            add(arrayOf(lens, mode, cameraXConfig))
                        }
                    }
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
    @SdkSuppress(maxSdkVersion = 33) // b/360867144: Module crashes on API34
    fun testPreviewViewUpdateAfterStopResume() {
        launchActivity(lensFacing, cameraXConfig).useInCameraTest { scenario ->
            // At first, check Preview in stream state
            assertStreamState(scenario, PreviewView.StreamState.STREAMING)

            // Go through Stop/Resume and then check Preview in stream state still
            scenario.moveToState(State.CREATED)
            scenario.moveToState(State.RESUMED)

            assertStreamState(scenario, PreviewView.StreamState.STREAMING)
            // Make sure the surface texture of TextureView continues getting updates.
            assertPreviewViewUpdate(scenario)
        }
    }

    // The test makes sure the TextureView surface texture keeps the same after switch.
    @Test
    @SdkSuppress(maxSdkVersion = 33) // b/360867144: Module crashes on API34
    fun testPreviewViewUpdateAfterSwitch() {
        assumeFalse(shouldSkipTest()) // b/331933633

        launchActivity(lensFacing, cameraXConfig).useInCameraTest { scenario ->
            // At first, check Preview in stream state
            assertStreamState(scenario, PreviewView.StreamState.STREAMING)

            // Switch from CameraFragment to BlankFragment, and then switch back to check Preview
            // update
            onView(withId(ViewPager2Activity.BLANK_VIEW_ID)).perform(click())
            onView(withId(R.id.blank_textview)).check(matches(isDisplayed()))

            onView(withId(ViewPager2Activity.CAMERA_VIEW_ID)).perform(click())
            onView(withId(R.id.preview_textureview)).check(matches(isDisplayed()))

            // For b/149877652, need to check if the surface texture of TextureView continues
            // getting updates after detaching from window and then attaching to window.
            assertPreviewViewUpdate(scenario)
        }
    }

    /**
     * The testPreviewViewUpdateAfterSwitch test will run failed in API 34 emulator's front camera
     * when using SurfaceView implementation. See b/331933633.
     */
    private fun shouldSkipTest() =
        isEmulator() &&
            Build.VERSION.SDK_INT == Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            lensFacing == CameraSelector.LENS_FACING_FRONT &&
            implementationMode == PERFORMANCE_MODE

    @Test
    @SdkSuppress(maxSdkVersion = 33) // b/360867144: Module crashes on API34
    fun testPreviewViewUpdateAfterSwitchAndStop_ResumeAndSwitchBack() {
        launchActivity(lensFacing, cameraXConfig).useInCameraTest { scenario ->
            // At first, check Preview in stream state
            assertStreamState(scenario, PreviewView.StreamState.STREAMING)

            // Switch from CameraFragment to BlankFragment, and then Stop and Resume
            // ViewPager2Activity
            onView(withId(ViewPager2Activity.BLANK_VIEW_ID)).perform(click())
            onView(withId(R.id.blank_textview)).check(matches(isDisplayed()))

            scenario.moveToState(State.CREATED)
            scenario.moveToState(State.RESUMED)
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            // After resume, switch back to CameraFragment, to check Preview in stream state
            onView(withId(ViewPager2Activity.CAMERA_VIEW_ID)).perform(click())
            onView(withId(R.id.preview_textureview)).check(matches(isDisplayed()))

            assertStreamState(scenario, PreviewView.StreamState.STREAMING)

            // The test covers pause/resume and ViewPager2 switch behaviors. Hence, need to
            // check the surface texture of TextureView continues getting updates for b/149877652.
            assertPreviewViewUpdate(scenario)
        }
    }

    private fun launchActivity(
        lensFacing: Int,
        cameraXConfig: String = CameraFragment.CAMERA2_IMPLEMENTATION_OPTION,
    ): ActivityScenario<ViewPager2Activity> {
        val intent =
            Intent(ApplicationProvider.getApplicationContext(), ViewPager2Activity::class.java)
                .apply {
                    putExtra(BaseActivity.INTENT_LENS_FACING, lensFacing)
                    putExtra(BaseActivity.INTENT_IMPLEMENTATION_MODE, implementationMode)
                    putExtra(CameraFragment.KEY_CAMERA_IMPLEMENTATION, cameraXConfig)
                    putExtra(CameraFragment.KEY_CAMERA_IMPLEMENTATION_NO_HISTORY, true)
                }
        return ActivityScenario.launch(intent)
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
        scenario: ActivityScenario<ViewPager2Activity>,
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

    private fun assertPreviewViewUpdate(scenario: ActivityScenario<ViewPager2Activity>) {
        when (implementationMode) {
            COMPATIBLE_MODE -> assertSurfaceTextureFramesUpdate(scenario)
            PERFORMANCE_MODE -> assertPreviewUpdate(scenario)
            else -> throw IllegalArgumentException()
        }
    }

    private fun assertSurfaceTextureFramesUpdate(scenario: ActivityScenario<ViewPager2Activity>) {
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

    /**
     * Checks update from Preview instead of SurfaceView, since the SurfaceView's content can not be
     * got.
     */
    private fun assertPreviewUpdate(scenario: ActivityScenario<ViewPager2Activity>) {
        val latch = CountDownLatch(PREVIEW_UPDATE_COUNT)
        getCameraFragment(scenario)?.setPreviewUpdatingLatch(latch)
        assertThat(latch.await(ACTION_IDLE_TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
    }

    private fun getCameraFragment(scenario: ActivityScenario<ViewPager2Activity>): CameraFragment? {
        var fragment: CameraFragment? = null
        scenario.onActivity { activity ->
            val fragments = activity.supportFragmentManager.fragments
            fragment = fragments.firstOrNull { it is CameraFragment } as? CameraFragment
        }

        return fragment
    }
}
