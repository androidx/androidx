/*
 * Copyright 2026 The Android Open Source Project
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
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.Surface
import android.view.View
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.integration.view.LowLightBoostTestUtil.assertLlbStateAfterSwitch
import androidx.camera.integration.view.LowLightBoostTestUtil.assertNoUnexpectedLlbError
import androidx.camera.integration.view.LowLightBoostTestUtil.assertPreviewOrientationAndGeometry
import androidx.camera.integration.view.LowLightBoostTestUtil.assertViewfinderUpdating
import androidx.camera.integration.view.LowLightBoostTestUtil.clickRadioButton
import androidx.camera.integration.view.LowLightBoostTestUtil.ensureNaturalRotationAfterLaunch
import androidx.camera.integration.view.LowLightBoostTestUtil.findActiveFragment
import androidx.camera.integration.view.LowLightBoostTestUtil.getStatusText
import androidx.camera.integration.view.LowLightBoostTestUtil.waitForActivityStage
import androidx.camera.integration.view.LowLightBoostTestUtil.waitForCondition
import androidx.camera.integration.view.LowLightBoostTestUtil.waitForFrames
import androidx.camera.integration.view.LowLightBoostTestUtil.waitForLlbEnabledState
import androidx.camera.integration.view.LowLightBoostTestUtil.waitForStatusContains
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.AndroidUtil.isEmulator
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.RequireForegroundRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Instrumentation tests verifying that the viewfinder keeps showing new frames across lifecycle
 * transitions (including a longer screen-off pause) and display rotations, with REBIND and
 * BIND_ONCE strategies for both [LowLightBoostEffectActivity] and [LowLightBoostEffectFragment].
 * The tests don't check whether the idle session release happened.
 */
@LargeTest
@RunWith(JUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
class LowLightBoostEffectLifecycleTest {

    @get:Rule(order = 0) val requireForegroundRule = RequireForegroundRule()

    @get:Rule(order = 1)
    val useCameraRule: TestRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
        )

    @get:Rule(order = 2)
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )

    private lateinit var uiDevice: UiDevice
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))
        requireForegroundRule.deferCleanup {
            ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS].shutdownAsync()[
                    10, TimeUnit.SECONDS]
        }
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        LowLightBoostTestUtil.resetViewfinderBaseline()
    }

    @After
    fun tearDown() {
        if (::uiDevice.isInitialized) {
            LowLightBoostTestUtil.tearDownDeviceAndActivities(uiDevice, finishActivities = true)
        }
    }

    private fun prepareDeviceForActivityLaunch() {
        LowLightBoostTestUtil.prepareDeviceForActivityLaunch(uiDevice)
    }

    /**
     * Verifies that whether the device/camera supports Low Light Boost or falls back to passthrough
     * mode, the camera stream delivers capture results, survives background/foreground lifecycle
     * transitions (HOME pause -> Activity resume), and the viewfinder shows new frames again after
     * the resume.
     */
    @Test
    fun testPassthroughLifecycle_SupportedOrUnsupportedDevice() {
        prepareDeviceForActivityLaunch()
        val intent =
            Intent(context, LowLightBoostEffectActivity::class.java).apply {
                putExtra(LowLightBoostEffectActivity.EXTRA_BINDING_BEHAVIOR, "bind_once")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }

        context.startActivity(intent)
        uiDevice.wait(Until.hasObject(By.pkg(context.packageName).depth(0)), 10000)
        ensureNaturalRotationAfterLaunch(uiDevice)

        val frameCountProvider = { LowLightBoostEffectActivity.captureResultCount }

        val initialCount = frameCountProvider()
        assertWithMessage("Initial preview failed to start streaming")
            .that(waitForFrames(frameCountProvider, initialCount, delta = 15L, timeoutMs = 15000L))
            .isTrue()
        assertViewfinderUpdating(isActivity = true, "Initial preview")

        // Background the Activity (ON_PAUSE / ON_STOP)
        assertWithMessage("Activity did not stop after navigating Home")
            .that(
                LowLightBoostTestUtil.navigateHomeAndWaitStopped(
                    uiDevice,
                    isActivity = true,
                    timeoutMs = 10000L,
                )
            )
            .isTrue()

        // Foreground the existing Activity instance (ON_START / ON_RESUME) without clearing the
        // task
        context.startActivity(
            Intent(intent).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        )
        uiDevice.wait(Until.hasObject(By.pkg(context.packageName).depth(0)), 10000)
        assertWithMessage("Activity did not resume after returning from Home")
            .that(waitForActivityStage(isActivity = true, Stage.RESUMED, timeoutMs = 10000L))
            .isTrue()

        val countAfterResume = frameCountProvider()
        assertWithMessage("Preview failed to recover streaming after lifecycle resume")
            .that(
                waitForFrames(
                    frameCountProvider = frameCountProvider,
                    initialCount = countAfterResume,
                    delta = 15L,
                    timeoutMs = 15000L,
                )
            )
            .isTrue()

        assertWithMessage("Stream state should be STREAMING after resume: ${getStatusText(true)}")
            .that(waitForStatusContains(isActivity = true, "Stream: STREAMING"))
            .isTrue()
        assertViewfinderUpdating(isActivity = true, "After resume")
    }

    // =========================================================================
    // LowLightBoostEffectActivity Lifecycle Permutations
    // =========================================================================

    @Test
    fun testRebind_LongInactivity() =
        runActivityTest(
            binding = "rebind",
            scenario = Scenario.LONG_INACTIVITY,
        )

    @Test
    fun testRebind_ScreenOnOff() =
        runActivityTest(
            binding = "rebind",
            scenario = Scenario.SCREEN_ON_OFF,
        )

    @Test
    fun testRebind_HomeResume() =
        runActivityTest(
            binding = "rebind",
            scenario = Scenario.HOME_RESUME,
        )

    @Test
    fun testBindOnce_LongInactivity() =
        runActivityTest(
            binding = "bind_once",
            scenario = Scenario.LONG_INACTIVITY,
        )

    @Test
    fun testBindOnce_ScreenOnOff() =
        runActivityTest(
            binding = "bind_once",
            scenario = Scenario.SCREEN_ON_OFF,
        )

    @Test
    fun testBindOnce_HomeResume() =
        runActivityTest(
            binding = "bind_once",
            scenario = Scenario.HOME_RESUME,
        )

    // =========================================================================
    // LowLightBoostEffectFragment Lifecycle Permutations
    // =========================================================================

    @Test
    fun testFragment_BindOnce_LongInactivity() =
        runFragmentTest(
            binding = "bind_once",
            scenario = Scenario.LONG_INACTIVITY,
        )

    @Test
    fun testFragment_Rebind_HomeResume() =
        runFragmentTest(
            binding = "rebind",
            scenario = Scenario.HOME_RESUME,
        )

    @Test
    fun testFragment_BindOnce_ScreenOnOff() =
        runFragmentTest(
            binding = "bind_once",
            scenario = Scenario.SCREEN_ON_OFF,
        )

    // =========================================================================
    // Activity / Fragment Navigation & Rotation Stress Tests
    // =========================================================================

    @Test
    fun testFragment_SwitchToActivityAndBack_ResumesStreaming() {
        prepareDeviceForActivityLaunch()
        val intent =
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra("fragment_type", "LowLightBoostEffect")
            }

        context.startActivity(intent)
        uiDevice.wait(Until.hasObject(By.pkg(context.packageName).depth(0)), 10000)
        ensureNaturalRotationAfterLaunch(uiDevice)

        val fragmentFrameCountProvider = { LowLightBoostEffectFragment.captureResultCount }
        val activityFrameCountProvider = { LowLightBoostEffectActivity.captureResultCount }

        // 1. Verify initial Fragment streaming
        var initialCount = fragmentFrameCountProvider()
        assertWithMessage("Fragment failed to stream initial frames")
            .that(
                waitForFrames(
                    fragmentFrameCountProvider,
                    initialCount,
                    delta = 10L,
                    timeoutMs = 15000L,
                )
            )
            .isTrue()
        // PreviewView reports STREAMING after its first frame, which can follow the capture
        // results.
        val initialStreaming = waitForStatusContains(isActivity = false, "Stream: STREAMING")
        assertWithMessage("Fragment status should be STREAMING: ${getStatusText(false)}")
            .that(initialStreaming)
            .isTrue()
        assertViewfinderUpdating(isActivity = false, "Initial preview")

        // Perform 2 round-trips: Fragment -> Activity -> BACK -> Fragment
        for (cycle in 1..2) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val launchBtn =
                    findActiveFragment()?.view?.findViewById<View>(R.id.launch_activity_button)
                requireNotNull(launchBtn) { "launch_activity_button not found" }
                launchBtn.performClick()
            }

            // Wait for Activity to launch and stream frames
            initialCount = activityFrameCountProvider()
            assertWithMessage("Cycle $cycle: Activity failed to stream frames after launch")
                .that(
                    waitForFrames(
                        activityFrameCountProvider,
                        initialCount,
                        delta = 10L,
                        timeoutMs = 15000L,
                    )
                )
                .isTrue()
            val activityStreaming = waitForStatusContains(isActivity = true, "Stream: STREAMING")
            assertWithMessage(
                    "Cycle $cycle: Activity status (${getStatusText(isActivity = true)}) must be " +
                        "STREAMING"
                )
                .that(activityStreaming)
                .isTrue()
            assertLlbStateAfterSwitch(isActivity = true, "Cycle $cycle: Activity")
            assertViewfinderUpdating(isActivity = true, "Cycle $cycle: Activity")

            // Press BACK key to return to Fragment
            uiDevice.pressBack()

            // Verify the Fragment's camera stream resumes
            initialCount = fragmentFrameCountProvider()
            assertWithMessage(
                    "Cycle $cycle: Fragment failed to resume streaming after pressing BACK from " +
                        "Activity"
                )
                .that(
                    waitForFrames(
                        fragmentFrameCountProvider,
                        initialCount,
                        delta = 10L,
                        timeoutMs = 15000L,
                    )
                )
                .isTrue()

            val streaming = waitForStatusContains(isActivity = false, "Stream: STREAMING")
            assertWithMessage(
                    "Cycle $cycle: Fragment status after BACK press " +
                        "(${getStatusText(isActivity = false)}) must be STREAMING"
                )
                .that(streaming)
                .isTrue()
            assertLlbStateAfterSwitch(
                isActivity = false,
                "Cycle $cycle: Fragment after BACK press",
            )
            assertViewfinderUpdating(isActivity = false, "Cycle $cycle: Fragment after BACK press")
        }
    }

    @Test fun testFragment_RapidRotationStress() = runFragmentRotationTest()

    /**
     * Verifies that in [LowLightBoostEffectActivity] (which declares `android:configChanges`),
     * rotating across 0°, 90°, 180°, 270°, and back to 0° dynamically updates `targetRotation`
     * while the camera stream stays upright and running in both `PERFORMANCE` (`SurfaceView`) and
     * `COMPATIBLE` (`TextureView`) modes.
     */
    @Test
    fun testActivity_RotationSweeps_KeepsStreaming() {
        assumeFalse(
            "Google Play Services Low Light Boost active session requires physical camera hardware",
            isEmulator(),
        )
        prepareDeviceForActivityLaunch()
        val intent =
            Intent(context, LowLightBoostEffectActivity::class.java).apply {
                putExtra(LowLightBoostEffectActivity.EXTRA_BINDING_BEHAVIOR, "bind_once")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }

        context.startActivity(intent)
        uiDevice.wait(Until.hasObject(By.pkg(context.packageName).depth(0)), 10000)
        ensureNaturalRotationAfterLaunch(uiDevice)

        val frameCountProvider = { LowLightBoostEffectActivity.captureResultCount }

        // 1. Verify initial streaming in PERFORMANCE mode
        val initialCount = frameCountProvider()
        assertWithMessage("Activity initial preview failed to start")
            .that(waitForFrames(frameCountProvider, initialCount, delta = 10L, timeoutMs = 15000L))
            .isTrue()
        assertPreviewOrientationAndGeometry(
            isActivity = true,
            expectedRotation = Surface.ROTATION_0,
            message = "Activity initial preview",
        )
        assertViewfinderUpdating(isActivity = true, "Initial preview")
        assumeLowLightBoostSupported(isActivity = true)

        var appliedRotations =
            sweepRotationsAndAssertStreaming(
                modeLabel = "PERFORMANCE",
                frameCountProvider = frameCountProvider,
            )

        // 2. Switch to COMPATIBLE mode and verify rotation sweep
        clickRadioButton(isActivity = true, R.id.radio_mode_compatible)
        assertWithMessage("Activity did not switch to COMPATIBLE mode")
            .that(waitForStatusContains(isActivity = true, "Mode: COMPATIBLE"))
            .isTrue()

        val countBeforeCompat = frameCountProvider()
        assertWithMessage("Preview failed to stream after switching to COMPATIBLE mode")
            .that(
                waitForFrames(
                    frameCountProvider,
                    countBeforeCompat,
                    delta = 10L,
                    timeoutMs = 15000L,
                )
            )
            .isTrue()
        assertPreviewOrientationAndGeometry(
            isActivity = true,
            expectedRotation = Surface.ROTATION_0,
            message = "Activity switched to COMPATIBLE mode",
        )

        appliedRotations +=
            sweepRotationsAndAssertStreaming(
                modeLabel = "COMPATIBLE",
                frameCountProvider = frameCountProvider,
            )
        assumeTrue("The display didn't apply enough requested rotations", appliedRotations >= 2)
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private fun assumeLowLightBoostSupported(isActivity: Boolean) {
        LowLightBoostTestUtil.assumeLowLightBoostSupported(isActivity)
    }

    /** Returns the number of rotations other than [Surface.ROTATION_0] the display applied. */
    private fun sweepRotationsAndAssertStreaming(
        modeLabel: String,
        frameCountProvider: () -> Long,
    ): Int {
        var appliedRotations = 0
        val rotationSteps =
            listOf(
                Pair(Surface.ROTATION_90, "Rot: 90°"),
                Pair(Surface.ROTATION_180, "Rot: 180°"),
                Pair(Surface.ROTATION_270, "Rot: 270°"),
                Pair(Surface.ROTATION_0, "Rot: 0°"),
            )

        for ((rotation, expectedRotLabel) in rotationSteps) {
            setDeviceRotation(rotation)
            // Some devices don't allow every display rotation. Skip the rotations the display
            // didn't change to.
            if (!waitForCondition(timeoutMs = 5000L) { uiDevice.displayRotation == rotation }) {
                continue
            }
            if (rotation != Surface.ROTATION_0) {
                appliedRotations++
            }
            assertWithMessage(
                    "Activity ($modeLabel) status did not update to $expectedRotLabel after " +
                        "rotation"
                )
                .that(waitForStatusContains(isActivity = true, expectedRotLabel))
                .isTrue()

            val beforeFrames = frameCountProvider()
            assertWithMessage("Activity ($modeLabel) failed to stream frames at $expectedRotLabel")
                .that(
                    waitForFrames(
                        frameCountProvider = frameCountProvider,
                        initialCount = beforeFrames,
                        delta = 10L,
                        timeoutMs = 15000L,
                    )
                )
                .isTrue()

            val status = getStatusText(isActivity = true)
            assertNoUnexpectedLlbError(status, "Activity ($modeLabel) at $expectedRotLabel")
            assertPreviewOrientationAndGeometry(
                isActivity = true,
                expectedRotation = rotation,
                message = "Activity ($modeLabel) at $expectedRotLabel",
            )
            assertViewfinderUpdating(
                isActivity = true,
                "Activity ($modeLabel) at $expectedRotLabel",
            )
        }
        return appliedRotations
    }

    private fun setDeviceRotation(rotation: Int) {
        LowLightBoostTestUtil.setDeviceRotation(uiDevice, rotation)
    }

    private fun runFragmentRotationTest() {
        assumeFalse(
            "Google Play Services Low Light Boost active session requires physical camera hardware",
            isEmulator(),
        )
        prepareDeviceForActivityLaunch()
        val intent =
            Intent(context, MainActivity::class.java).apply {
                putExtra("fragment_type", "LowLightBoostEffect")
                putExtra(LowLightBoostEffectFragment.EXTRA_BINDING_BEHAVIOR, "bind_once")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }

        context.startActivity(intent)
        uiDevice.wait(Until.hasObject(By.pkg(context.packageName).depth(0)), 10000)
        ensureNaturalRotationAfterLaunch(uiDevice)

        val initialCount = LowLightBoostEffectFragment.captureResultCount
        assertWithMessage("Fragment initial preview failed to start")
            .that(
                waitForFrames(
                    frameCountProvider = { LowLightBoostEffectFragment.captureResultCount },
                    initialCount = initialCount,
                    delta = 5L,
                    timeoutMs = 15000L,
                )
            )
            .isTrue()
        assertPreviewOrientationAndGeometry(
            isActivity = false,
            expectedRotation = Surface.ROTATION_0,
            message = "Fragment initial preview",
        )
        assertViewfinderUpdating(isActivity = false, "Initial preview")
        assumeLowLightBoostSupported(isActivity = false)

        val rotations =
            listOf(
                Pair(Surface.ROTATION_90, "Rot: 90°"),
                Pair(Surface.ROTATION_270, "Rot: 270°"),
                Pair(Surface.ROTATION_180, "Rot: 180°"),
                Pair(Surface.ROTATION_0, "Rot: 0°"),
            )

        var appliedRotations = 0
        rotations.forEachIndexed { index, (rotation, expectedRotLabel) ->
            setDeviceRotation(rotation)
            // Some devices don't allow every display rotation. Skip the rotations the display
            // didn't change to.
            if (!waitForCondition(timeoutMs = 5000L) { uiDevice.displayRotation == rotation }) {
                return@forEachIndexed
            }
            if (rotation != Surface.ROTATION_0) {
                appliedRotations++
            }
            assertWithMessage("Fragment status did not update to $expectedRotLabel (#$index)")
                .that(
                    waitForStatusContains(isActivity = false, expectedRotLabel, timeoutMs = 5000L)
                )
                .isTrue()

            val beforeCount = LowLightBoostEffectFragment.captureResultCount
            assertWithMessage("Fragment preview failed to stream after rotation #$index")
                .that(
                    waitForFrames(
                        frameCountProvider = { LowLightBoostEffectFragment.captureResultCount },
                        initialCount = beforeCount,
                        delta = 10L,
                        timeoutMs = 15000L,
                    )
                )
                .isTrue()

            val status = getStatusText(isActivity = false)
            assertNoUnexpectedLlbError(status, "Fragment after rotation #$index")
            assertPreviewOrientationAndGeometry(
                isActivity = false,
                expectedRotation = rotation,
                message = "Fragment after rotation #$index ($expectedRotLabel)",
            )
            assertViewfinderUpdating(isActivity = false, "Fragment after rotation #$index")
        }
        assumeTrue("The display didn't apply enough requested rotations", appliedRotations >= 2)
    }

    private enum class Scenario {
        LONG_INACTIVITY,
        SCREEN_ON_OFF,
        HOME_RESUME,
    }

    private fun runActivityTest(binding: String, scenario: Scenario) {
        assumeFalse(
            "Google Play Services Low Light Boost active session requires physical camera hardware",
            isEmulator(),
        )
        prepareDeviceForActivityLaunch()
        val intent =
            Intent(context, LowLightBoostEffectActivity::class.java).apply {
                putExtra(LowLightBoostEffectActivity.EXTRA_BINDING_BEHAVIOR, binding)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }

        context.startActivity(intent)
        uiDevice.wait(Until.hasObject(By.pkg(context.packageName).depth(0)), 10000)
        ensureNaturalRotationAfterLaunch(uiDevice)

        val initialCount = LowLightBoostEffectActivity.captureResultCount
        val initialStreaming =
            waitForFrames(
                frameCountProvider = { LowLightBoostEffectActivity.captureResultCount },
                initialCount = initialCount,
                delta = 5L,
                timeoutMs = 15000L,
            )
        assertWithMessage("Initial preview failed to start ($binding)")
            .that(initialStreaming)
            .isTrue()
        assertViewfinderUpdating(isActivity = true, "Initial preview ($binding)")
        assumeLowLightBoostSupported(isActivity = true)

        when (scenario) {
            Scenario.LONG_INACTIVITY ->
                simulateLongInactivity(
                    binding = binding,
                    isActivity = true,
                )
            Scenario.SCREEN_ON_OFF ->
                simulateScreenOnOffStress(
                    binding = binding,
                    isActivity = true,
                )
            Scenario.HOME_RESUME ->
                simulateHomeAndResumeStress(
                    binding = binding,
                    isActivity = true,
                    resumeIntent = intent,
                )
        }
    }

    private fun runFragmentTest(binding: String, scenario: Scenario) {
        assumeFalse(
            "Google Play Services Low Light Boost active session requires physical camera hardware",
            isEmulator(),
        )
        prepareDeviceForActivityLaunch()
        val intent =
            Intent(context, MainActivity::class.java).apply {
                putExtra("fragment_type", "LowLightBoostEffect")
                putExtra(LowLightBoostEffectFragment.EXTRA_BINDING_BEHAVIOR, binding)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }

        context.startActivity(intent)
        uiDevice.wait(Until.hasObject(By.pkg(context.packageName).depth(0)), 10000)
        ensureNaturalRotationAfterLaunch(uiDevice)

        val initialCount = LowLightBoostEffectFragment.captureResultCount
        val initialStreaming =
            waitForFrames(
                frameCountProvider = { LowLightBoostEffectFragment.captureResultCount },
                initialCount = initialCount,
                delta = 5L,
                timeoutMs = 15000L,
            )
        assertWithMessage("Fragment initial preview failed to start ($binding)")
            .that(initialStreaming)
            .isTrue()
        assertViewfinderUpdating(isActivity = false, "Initial preview ($binding)")
        assumeLowLightBoostSupported(isActivity = false)

        when (scenario) {
            Scenario.LONG_INACTIVITY ->
                simulateLongInactivity(
                    binding = binding,
                    isActivity = false,
                )
            Scenario.SCREEN_ON_OFF ->
                simulateScreenOnOffStress(
                    binding = binding,
                    isActivity = false,
                )
            Scenario.HOME_RESUME ->
                simulateHomeAndResumeStress(
                    binding = binding,
                    isActivity = false,
                    resumeIntent = intent,
                )
        }
    }

    @SuppressLint("BanThreadSleep")
    private fun simulateLongInactivity(
        binding: String,
        isActivity: Boolean,
    ) {
        uiDevice.executeShellCommand("input keyevent KEYCODE_SLEEP")
        assertWithMessage("Not stopped after the screen turned off ($binding)")
            .that(waitForActivityStage(isActivity, Stage.STOPPED, timeoutMs = 10000L))
            .isTrue()
        // Intentionally hold the screen off well past LowLightBoostSessionManager's 2000ms
        // SESSION_RELEASE_DELAY_MS to trigger idle session release.
        Thread.sleep(5000)

        assertWithMessage("Not resumed after the screen turned on ($binding)")
            .that(LowLightBoostTestUtil.wakeUpAndWaitForResumed(uiDevice, isActivity))
            .isTrue()

        val countAfterWake =
            if (isActivity) {
                LowLightBoostEffectActivity.captureResultCount
            } else {
                LowLightBoostEffectFragment.captureResultCount
            }
        val resumed =
            waitForFrames(
                frameCountProvider = {
                    if (isActivity) {
                        LowLightBoostEffectActivity.captureResultCount
                    } else {
                        LowLightBoostEffectFragment.captureResultCount
                    }
                },
                initialCount = countAfterWake,
                delta = 15L,
                timeoutMs = 15000L,
            )
        assertWithMessage("Camera stream didn't resume after Long Inactivity in $binding")
            .that(resumed)
            .isTrue()
        assertWithMessage(
                "LLB didn't become INACTIVE/ACTIVE after Long Inactivity in $binding: " +
                    getStatusText(isActivity)
            )
            .that(waitForLlbEnabledState(isActivity))
            .isTrue()
        assertViewfinderUpdating(isActivity, "After Long Inactivity in $binding")
    }

    private fun simulateScreenOnOffStress(
        binding: String,
        isActivity: Boolean,
    ) {
        repeat(3) { cycle ->
            uiDevice.executeShellCommand("input keyevent KEYCODE_SLEEP")
            assertWithMessage("Not stopped after the screen turned off, cycle $cycle ($binding)")
                .that(waitForActivityStage(isActivity, Stage.STOPPED, timeoutMs = 10000L))
                .isTrue()
            assertWithMessage("Not resumed after the screen turned on, cycle $cycle ($binding)")
                .that(LowLightBoostTestUtil.wakeUpAndWaitForResumed(uiDevice, isActivity))
                .isTrue()

            val countBefore =
                if (isActivity) {
                    LowLightBoostEffectActivity.captureResultCount
                } else {
                    LowLightBoostEffectFragment.captureResultCount
                }
            val resumed =
                waitForFrames(
                    frameCountProvider = {
                        if (isActivity) {
                            LowLightBoostEffectActivity.captureResultCount
                        } else {
                            LowLightBoostEffectFragment.captureResultCount
                        }
                    },
                    initialCount = countBefore,
                    delta = 10L,
                    timeoutMs = 15000L,
                )
            assertWithMessage("Camera stream stuck on Screen On/Off cycle $cycle ($binding)")
                .that(resumed)
                .isTrue()
            assertWithMessage(
                    "LLB didn't become INACTIVE/ACTIVE on Screen On/Off cycle $cycle ($binding): " +
                        getStatusText(isActivity)
                )
                .that(waitForLlbEnabledState(isActivity))
                .isTrue()
            assertViewfinderUpdating(isActivity, "Screen On/Off cycle $cycle ($binding)")
        }
    }

    private fun simulateHomeAndResumeStress(
        binding: String,
        isActivity: Boolean,
        resumeIntent: Intent,
    ) {
        repeat(3) { cycle ->
            assertWithMessage("Not stopped after navigating HOME, cycle $cycle ($binding)")
                .that(
                    LowLightBoostTestUtil.navigateHomeAndWaitStopped(
                        uiDevice,
                        isActivity,
                        timeoutMs = 10000L,
                    )
                )
                .isTrue()

            context.startActivity(
                Intent(resumeIntent).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
            uiDevice.wait(Until.hasObject(By.pkg(context.packageName).depth(0)), 5000)
            assertWithMessage("Not resumed after returning from HOME, cycle $cycle ($binding)")
                .that(waitForActivityStage(isActivity, Stage.RESUMED, timeoutMs = 10000L))
                .isTrue()

            val countBefore =
                if (isActivity) {
                    LowLightBoostEffectActivity.captureResultCount
                } else {
                    LowLightBoostEffectFragment.captureResultCount
                }
            val resumed =
                waitForFrames(
                    frameCountProvider = {
                        if (isActivity) {
                            LowLightBoostEffectActivity.captureResultCount
                        } else {
                            LowLightBoostEffectFragment.captureResultCount
                        }
                    },
                    initialCount = countBefore,
                    delta = 10L,
                    timeoutMs = 15000L,
                )
            assertWithMessage("Camera stream stuck on Home/Resume cycle $cycle ($binding)")
                .that(resumed)
                .isTrue()
            assertWithMessage(
                    "LLB didn't become INACTIVE/ACTIVE on Home/Resume cycle $cycle ($binding): " +
                        getStatusText(isActivity)
                )
                .that(waitForLlbEnabledState(isActivity))
                .isTrue()
            assertViewfinderUpdating(isActivity, "Home/Resume cycle $cycle ($binding)")
        }
    }
}
