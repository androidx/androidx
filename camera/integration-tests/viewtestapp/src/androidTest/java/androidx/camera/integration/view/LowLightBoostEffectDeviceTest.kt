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
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.Surface
import androidx.camera.camera2.Camera2Config
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.effects.playservices.lowlight.LowLightBoostEffect
import androidx.camera.integration.view.LowLightBoostTestUtil.assertLlbStateAfterSwitch
import androidx.camera.integration.view.LowLightBoostTestUtil.assertNoUnexpectedLlbError
import androidx.camera.integration.view.LowLightBoostTestUtil.assertPreviewOrientationAndGeometry
import androidx.camera.integration.view.LowLightBoostTestUtil.assertViewfinderUpdating
import androidx.camera.integration.view.LowLightBoostTestUtil.clickRadioButton
import androidx.camera.integration.view.LowLightBoostTestUtil.clickView
import androidx.camera.integration.view.LowLightBoostTestUtil.ensureNaturalRotationAfterLaunch
import androidx.camera.integration.view.LowLightBoostTestUtil.findActiveActivity
import androidx.camera.integration.view.LowLightBoostTestUtil.getStatusText
import androidx.camera.integration.view.LowLightBoostTestUtil.navigateHomeAndWaitStopped
import androidx.camera.integration.view.LowLightBoostTestUtil.waitForActivityStage
import androidx.camera.integration.view.LowLightBoostTestUtil.waitForCondition
import androidx.camera.integration.view.LowLightBoostTestUtil.waitForFrames
import androidx.camera.integration.view.LowLightBoostTestUtil.waitForLlbEnabledState
import androidx.camera.integration.view.LowLightBoostTestUtil.waitForStatusContains
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.AndroidUtil.isEmulator
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.RequireForegroundRule
import androidx.camera.view.PreviewView
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
 * Instrumentation tests verifying functional, UI, topology, camera lens switching, dynamic LLB
 * toggling, StreamSharing, orientation matrix, and passthrough scenarios for
 * [LowLightBoostEffectActivity] (Compose [CameraXViewfinder]) and [LowLightBoostEffectFragment]
 * ([PreviewView]).
 */
@LargeTest
@RunWith(JUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
class LowLightBoostEffectDeviceTest {

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
     * Verifies that whether the device/camera supports Low Light Boost (`Supported: true`) or does
     * not support Low Light Boost (`Supported: false`), camera preview always reaches `Stream:
     * STREAMING` and the viewfinder keeps showing new frames. On unsupported devices, verifies the
     * passthrough fallback state (`LLB: ERROR` while the camera stream keeps running), which
     * disabling Low Light Boost clears (`LLB: OFF`) and enabling it reports again. If the support
     * check reported `false` but Low Light Boost became enabled, the check must have timed out, so
     * the support is queried again and must be `true`.
     */
    @Test
    fun testPassthroughStreaming_SupportedOrUnsupportedDevice() {
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
        assertWithMessage("Preview failed to start streaming")
            .that(waitForFrames(frameCountProvider, initialCount, delta = 15L, timeoutMs = 15000L))
            .isTrue()

        assertWithMessage("Stream state should be STREAMING: ${getStatusText(isActivity = true)}")
            .that(waitForStatusContains(isActivity = true, "Stream: STREAMING"))
            .isTrue()
        val supportChecked =
            waitForCondition(timeoutMs = 10000L) {
                !getStatusText(isActivity = true).contains("checking...")
            }
        assertWithMessage("Support check didn't complete: ${getStatusText(isActivity = true)}")
            .that(supportChecked)
            .isTrue()
        val status = getStatusText(isActivity = true)
        assertPreviewOrientationAndGeometry(
            isActivity = true,
            expectedRotation = Surface.ROTATION_0,
            message = "Initial preview",
        )
        assertViewfinderUpdating(isActivity = true, "Initial preview")

        if (status.contains("Supported: false")) {
            // ERROR can be reported after session creation retries, so wait for it. The support
            // check also reports false if Google Play Services doesn't answer in time, while the
            // effect may still install its module or work, so PENDING_INSTALLATION, INACTIVE, or
            // ACTIVE is handled too.
            var finalStatus = ""
            val reachedState =
                waitForCondition(timeoutMs = 10000L) {
                    finalStatus = getStatusText(isActivity = true)
                    finalStatus.contains("LLB: ERROR") ||
                        finalStatus.contains("LLB: PENDING_INSTALLATION") ||
                        finalStatus.contains("LLB: INACTIVE") ||
                        finalStatus.contains("LLB: ACTIVE")
                }
            if (finalStatus.contains("LLB: PENDING_INSTALLATION")) {
                assertWithMessage(
                        "Module installation finished without reaching INACTIVE/ACTIVE " +
                            "(${getStatusText(isActivity = true)})"
                    )
                    .that(waitForLlbEnabledState(isActivity = true))
                    .isTrue()
                finalStatus = getStatusText(isActivity = true)
            } else {
                LowLightBoostTestUtil.assumeNoModuleInstallationFailure(finalStatus)
                assertWithMessage(
                        "Unsupported device/camera should report LLB: ERROR, or INACTIVE/ACTIVE " +
                            "only if the support check timed out ($finalStatus)"
                    )
                    .that(reachedState)
                    .isTrue()
            }
            if (!finalStatus.contains("LLB: ERROR")) {
                // The app's support check may have timed out. Its late answer is cached, so query
                // again: Low Light Boost may only be enabled on a supported camera.
                val provider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
                val cameraInfo = provider.getCameraInfo(CameraSelector.DEFAULT_BACK_CAMERA)
                // A query that times out reports false without caching it, so retry it a few
                // times before concluding that the camera is unsupported.
                var isSupported = false
                for (attempt in 1..SUPPORT_QUERY_ATTEMPTS) {
                    isSupported =
                        LowLightBoostEffect.isSupportedAsync(context, cameraInfo)[
                                10, TimeUnit.SECONDS]
                    if (isSupported) {
                        break
                    }
                }
                assertWithMessage(
                        "LLB is enabled although the camera is reported as unsupported " +
                            "($finalStatus)"
                    )
                    .that(isSupported)
                    .isTrue()
            } else {
                assertNoUnexpectedLlbError(finalStatus, "Unsupported device initial LLB state")
                // Disabling Low Light Boost clears the error, and enabling it reports it again.
                clickView(isActivity = true, R.id.toggle_llb_button)
                assertWithMessage("Disabling LLB didn't clear the error: ${getStatusText(true)}")
                    .that(waitForStatusContains(isActivity = true, "LLB: OFF"))
                    .isTrue()
                clickView(isActivity = true, R.id.toggle_llb_button)
                assertWithMessage("Enabling LLB didn't report the error: ${getStatusText(true)}")
                    .that(waitForStatusContains(isActivity = true, "LLB: ERROR"))
                    .isTrue()
                assertNoUnexpectedLlbError(
                    getStatusText(isActivity = true),
                    "After re-enabling LLB",
                )
                assertViewfinderUpdating(isActivity = true, "After toggling LLB")
            }
        } else {
            val reachedEnabledState = waitForLlbEnabledState(isActivity = true)
            val finalStatus = getStatusText(isActivity = true)
            assertWithMessage(
                    "Supported device/camera should transition to LLB: INACTIVE or ACTIVE " +
                        "($finalStatus)"
                )
                .that(reachedEnabledState)
                .isTrue()
        }
    }

    /**
     * Verifies dynamic toggling of Low Light Boost (`isLowLightBoostEnabled = false` and
     * `isLowLightBoostEnabled = true`) via the UI button during an active camera session. Asserts
     * that toggling updates the effect state (`LLB: OFF` <-> `INACTIVE`/`ACTIVE`) while the
     * viewfinder keeps showing new frames.
     */
    @Test
    fun testDynamicLlbToggle_KeepsStreaming() {
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

        val initialCount = frameCountProvider()
        assertWithMessage("Activity initial preview failed to start")
            .that(waitForFrames(frameCountProvider, initialCount, delta = 10L, timeoutMs = 15000L))
            .isTrue()
        assertViewfinderUpdating(isActivity = true, "Initial preview")
        assumeLowLightBoostSupported(isActivity = true)

        val initialEnabled = waitForLlbEnabledState(isActivity = true)
        assertWithMessage(
                "Initial LLB state did not reach INACTIVE/ACTIVE before toggling: " +
                    getStatusText(true)
            )
            .that(initialEnabled)
            .isTrue()

        // 1. Click Toggle LLB button to turn LLB OFF and verify rotation passthrough keeps
        // preview upright and streaming while disabled.
        clickView(isActivity = true, R.id.toggle_llb_button)
        assertWithMessage("Status did not update to LLB: OFF after disabling")
            .that(waitForStatusContains(isActivity = true, "LLB: OFF"))
            .isTrue()

        val framesBeforeOffStream = frameCountProvider()
        assertWithMessage("Preview failed to stream while LLB is OFF")
            .that(
                waitForFrames(
                    frameCountProvider,
                    framesBeforeOffStream,
                    delta = 10L,
                    timeoutMs = 15000L,
                )
            )
            .isTrue()
        assertPreviewOrientationAndGeometry(
            isActivity = true,
            expectedRotation = Surface.ROTATION_0,
            message = "LLB OFF at ROTATION_0",
        )
        assertViewfinderUpdating(isActivity = true, "LLB OFF")

        setDeviceRotation(Surface.ROTATION_90)
        if (
            waitForCondition(timeoutMs = 5000L) { uiDevice.displayRotation == Surface.ROTATION_90 }
        ) {
            assertWithMessage("Activity rotation did not update to 90° while LLB is OFF")
                .that(waitForStatusContains(isActivity = true, "Rot: 90° (ROTATION_90)"))
                .isTrue()
            val framesOffRotated = frameCountProvider()
            assertWithMessage("Preview failed to stream at 90° while LLB is OFF")
                .that(
                    waitForFrames(
                        frameCountProvider,
                        framesOffRotated,
                        delta = 8L,
                        timeoutMs = 15000L,
                    )
                )
                .isTrue()
            assertWithMessage("LLB should remain OFF while rotated: ${getStatusText(true)}")
                .that(getStatusText(isActivity = true))
                .contains("LLB: OFF")
            assertPreviewOrientationAndGeometry(
                isActivity = true,
                expectedRotation = Surface.ROTATION_90,
                message = "LLB OFF at ROTATION_90",
            )
            assertViewfinderUpdating(isActivity = true, "LLB OFF at ROTATION_90")
        }
        setDeviceRotation(Surface.ROTATION_0)
        assertWithMessage("Activity rotation did not return to 0° while LLB is OFF")
            .that(waitForStatusContains(isActivity = true, "Rot: 0° (ROTATION_0)"))
            .isTrue()
        assertPreviewOrientationAndGeometry(
            isActivity = true,
            expectedRotation = Surface.ROTATION_0,
            message = "LLB OFF back at ROTATION_0",
        )

        // 2. Click Toggle LLB button again to turn LLB back ON
        clickView(isActivity = true, R.id.toggle_llb_button)
        val reEnabled = waitForLlbEnabledState(isActivity = true)
        assertWithMessage(
                "Status did not transition back to INACTIVE/ACTIVE after re-enabling: " +
                    getStatusText(true)
            )
            .that(reEnabled)
            .isTrue()

        val framesBeforeOnStream = frameCountProvider()
        assertWithMessage("Preview failed to stream after re-enabling LLB")
            .that(
                waitForFrames(
                    frameCountProvider,
                    framesBeforeOnStream,
                    delta = 10L,
                    timeoutMs = 15000L,
                )
            )
            .isTrue()
        assertPreviewOrientationAndGeometry(
            isActivity = true,
            expectedRotation = Surface.ROTATION_0,
            message = "LLB re-enabled",
        )
        assertViewfinderUpdating(isActivity = true, "LLB re-enabled")
    }

    /**
     * Verifies switching camera lens facing between Rear (`Facing: Back`) and Front (`Facing:
     * Front`) cameras. Asserts that `isSupported` capability checks complete and that the
     * viewfinder keeps showing new frames of both cameras (using passthrough fallback if a lens
     * does not support LLB).
     */
    @Test
    fun testLensFacingSwitch_RearAndFrontCamera() {
        assumeTrue(
            "Device must have both back and front cameras for lens switch test",
            CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK) &&
                CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT),
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

        // 1. Verify Rear Camera streaming & capability check completion
        val initialCount = frameCountProvider()
        assertWithMessage("Rear camera initial preview failed to start")
            .that(waitForFrames(frameCountProvider, initialCount, delta = 10L, timeoutMs = 15000L))
            .isTrue()
        assertViewfinderUpdating(isActivity = true, "Initial preview")
        assertWithMessage("Rear camera status did not show Facing: Back")
            .that(waitForStatusContains(isActivity = true, "Facing: Back"))
            .isTrue()
        assertWithMessage("Rear camera support check did not finish")
            .that(
                waitForCondition(timeoutMs = 10000L) {
                    !getStatusText(isActivity = true).contains("checking...")
                }
            )
            .isTrue()

        // 2. Flip to Front Camera
        clickView(isActivity = true, R.id.flip_camera_button)
        assertWithMessage("Status did not update to Facing: Front after flipping camera")
            .that(waitForStatusContains(isActivity = true, "Facing: Front"))
            .isTrue()

        val framesBeforeFront = frameCountProvider()
        assertWithMessage("Front camera failed to stream frames")
            .that(
                waitForFrames(
                    frameCountProvider,
                    framesBeforeFront,
                    delta = 10L,
                    timeoutMs = 15000L,
                )
            )
            .isTrue()
        assertWithMessage("Front camera stream state should be STREAMING")
            .that(waitForStatusContains(isActivity = true, "Stream: STREAMING"))
            .isTrue()
        assertWithMessage("Front camera support check did not finish")
            .that(
                waitForCondition(timeoutMs = 10000L) {
                    !getStatusText(isActivity = true).contains("checking...")
                }
            )
            .isTrue()
        assertLlbStateAfterSwitch(isActivity = true, "Front camera")
        assertPreviewOrientationAndGeometry(
            isActivity = true,
            expectedRotation = Surface.ROTATION_0,
            message = "Front camera",
        )
        assertViewfinderUpdating(isActivity = true, "Front camera")

        // 3. Flip back to Rear Camera
        clickView(isActivity = true, R.id.flip_camera_button)
        assertWithMessage("Status did not update back to Facing: Back")
            .that(waitForStatusContains(isActivity = true, "Facing: Back"))
            .isTrue()

        val framesBeforeBackAgain = frameCountProvider()
        assertWithMessage("Rear camera failed to resume streaming after flip back")
            .that(
                waitForFrames(
                    frameCountProvider,
                    framesBeforeBackAgain,
                    delta = 10L,
                    timeoutMs = 15000L,
                )
            )
            .isTrue()
        assertWithMessage("Rear camera stream state should be STREAMING after flip back")
            .that(waitForStatusContains(isActivity = true, "Stream: STREAMING"))
            .isTrue()
        assertLlbStateAfterSwitch(isActivity = true, "Rear camera after flip back")
        assertPreviewOrientationAndGeometry(
            isActivity = true,
            expectedRotation = Surface.ROTATION_0,
            message = "Rear camera after flip back",
        )
        assertViewfinderUpdating(isActivity = true, "Rear camera after flip back")
    }

    // =========================================================================
    // Dynamic UI Configuration Changes (Implementation Mode & Target Settings)
    // =========================================================================

    @Test
    fun testActivity_DynamicUiConfigurationChanges() {
        val frameCountProvider = { LowLightBoostEffectActivity.captureResultCount }
        launchLlbHost(isActivity = true, frameCountProvider = frameCountProvider)
        verifyDynamicUiSwitches(isActivity = true, frameCountProvider = frameCountProvider)
    }

    @Test
    fun testFragment_DynamicUiConfigurationChanges() {
        val frameCountProvider = { LowLightBoostEffectFragment.captureResultCount }
        launchLlbHost(isActivity = false, frameCountProvider = frameCountProvider)
        verifyDynamicUiSwitches(isActivity = false, frameCountProvider = frameCountProvider)
    }

    /**
     * Verifies that [LowLightBoostEffectActivity] (using Jetpack Compose [CameraXViewfinder]) keeps
     * the camera stream running, stays upright and isotropic, and updates rotation across the Back
     * camera in EXTERNAL (`PERFORMANCE`) & EMBEDDED (`COMPATIBLE`) implementation modes and all 4
     * display rotations (`0°`, `90°`, `180°`, `270°`), followed by a Front camera check at `0°`.
     */
    @Test
    fun testOrientationAndStreamingMatrix_Activity_ComposeViewfinder() {
        val frameCountProvider = { LowLightBoostEffectActivity.captureResultCount }
        launchLlbHost(isActivity = true, frameCountProvider = frameCountProvider)
        verifyRotationMatrixForConfig(
            isActivity = true,
            frameCountProvider = frameCountProvider,
            contextDesc = "Back PREVIEW",
        )
        verifyFrontCameraCheckAtZero(isActivity = true, frameCountProvider = frameCountProvider)
    }

    /**
     * Verifies that [LowLightBoostEffectFragment] (using [PreviewView]) keeps the camera stream
     * running, stays upright and isotropic, and updates rotation across the Back camera in
     * `PERFORMANCE` & `COMPATIBLE` implementation modes and all 4 display rotations (`0°`, `90°`,
     * `180°`, `270°`), including direct `90° -> 270°` half-turns without recreating `MainActivity`,
     * followed by a Front camera check at `0°`.
     */
    @Test
    fun testOrientationAndStreamingMatrix_Fragment_PreviewView() {
        val frameCountProvider = { LowLightBoostEffectFragment.captureResultCount }
        launchLlbHost(isActivity = false, frameCountProvider = frameCountProvider)
        verifyRotationMatrixForConfig(
            isActivity = false,
            frameCountProvider = frameCountProvider,
            contextDesc = "Back PREVIEW",
        )
        verifyFragmentHalfTurnWithoutRecreation(frameCountProvider = frameCountProvider)
        verifyFrontCameraCheckAtZero(isActivity = false, frameCountProvider = frameCountProvider)
    }

    /**
     * Verifies Front camera orientation, horizontal mirroring, isotropic geometry, and streaming in
     * [LowLightBoostEffectActivity] across `PERFORMANCE` & `COMPATIBLE` modes and all 4 display
     * rotations (`0°`, `90°`, `180°`, `270°`).
     */
    @Test
    fun testFrontCamera_AllRotations_Activity_ComposeViewfinder() {
        assumeTrue(
            "Device must have a front camera",
            CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT),
        )
        val frameCountProvider = { LowLightBoostEffectActivity.captureResultCount }
        launchLlbHost(
            isActivity = true,
            lens = "FRONT",
            frameCountProvider = frameCountProvider,
        )
        assertWithMessage("Activity status did not show Facing: Front")
            .that(waitForStatusContains(isActivity = true, "Facing: Front"))
            .isTrue()
        verifyRotationMatrixForConfig(
            isActivity = true,
            frameCountProvider = frameCountProvider,
            contextDesc = "Front PREVIEW",
        )
    }

    /**
     * Verifies Front camera orientation, horizontal mirroring, isotropic geometry, and streaming in
     * [LowLightBoostEffectFragment] across `PERFORMANCE` & `COMPATIBLE` modes and all 4 display
     * rotations (`0°`, `90°`, `180°`, `270°`).
     */
    @Test
    fun testFrontCamera_AllRotations_Fragment_PreviewView() {
        assumeTrue(
            "Device must have a front camera",
            CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT),
        )
        val frameCountProvider = { LowLightBoostEffectFragment.captureResultCount }
        launchLlbHost(
            isActivity = false,
            lens = "FRONT",
            frameCountProvider = frameCountProvider,
        )
        assertWithMessage("Fragment status did not show Facing: Front")
            .that(waitForStatusContains(isActivity = false, "Facing: Front"))
            .isTrue()
        verifyRotationMatrixForConfig(
            isActivity = false,
            frameCountProvider = frameCountProvider,
            contextDesc = "Front PREVIEW",
        )
    }

    /**
     * Verifies `Target: PREV|VIDEO` (`StreamSharing`: `Preview + VideoCapture`) in
     * [LowLightBoostEffectActivity] across `PERFORMANCE` & `COMPATIBLE` modes and all 4 display
     * rotations (`0°`, `90°`, `180°`, `270°`).
     */
    @Test
    fun testTargetPreviewVideo_AllRotations_Activity_ComposeViewfinder() {
        val frameCountProvider = { LowLightBoostEffectActivity.captureResultCount }
        launchLlbHost(
            isActivity = true,
            target = "PREV_VIDEO",
            frameCountProvider = frameCountProvider,
        )
        assertWithMessage("Activity status should report Target: PREV|VIDEO")
            .that(waitForStatusContains(isActivity = true, "Target: PREV|VIDEO"))
            .isTrue()
        verifyRotationMatrixForConfig(
            isActivity = true,
            frameCountProvider = frameCountProvider,
            contextDesc = "Target: PREV|VIDEO",
        )
    }

    /**
     * Verifies `Target: PREV|VIDEO` (`StreamSharing`: `Preview + VideoCapture`) in
     * [LowLightBoostEffectFragment] across `PERFORMANCE` & `COMPATIBLE` modes and all 4 display
     * rotations (`0°`, `90°`, `180°`, `270°`).
     */
    @Test
    fun testTargetPreviewVideo_AllRotations_Fragment_PreviewView() {
        val frameCountProvider = { LowLightBoostEffectFragment.captureResultCount }
        launchLlbHost(
            isActivity = false,
            target = "PREV_VIDEO",
            frameCountProvider = frameCountProvider,
        )
        assertWithMessage("Fragment status should report Target: PREV|VIDEO")
            .that(waitForStatusContains(isActivity = false, "Target: PREV|VIDEO"))
            .isTrue()
        verifyRotationMatrixForConfig(
            isActivity = false,
            frameCountProvider = frameCountProvider,
            contextDesc = "Target: PREV|VIDEO",
        )
    }

    /**
     * Verifies that [LowLightBoostEffectActivity] with `Target: ALL` (`Preview + VideoCapture +
     * ImageCapture` via `StreamSharing`) dynamically updates rotation across all 4 display
     * rotations (`0°`, `90°`, `180°`, `270°`) in both `PERFORMANCE` and `COMPATIBLE` modes on the
     * Back camera, and verifies Front camera orientation at `0°` and `90°`.
     */
    @Test
    fun testTargetAll_DynamicRotation_Activity_ComposeViewfinder() {
        verifyTargetAllRotations(
            isActivity = true,
            frameCountProvider = { LowLightBoostEffectActivity.captureResultCount },
        )
    }

    /**
     * Verifies that [LowLightBoostEffectFragment] with `Target: ALL` (`Preview + VideoCapture +
     * ImageCapture` via `StreamSharing`) updates rotation across all 4 display rotations (`0°`,
     * `90°`, `180°`, `270°`) in both `PERFORMANCE` and `COMPATIBLE` modes on the Back camera, and
     * verifies Front camera orientation at `0°` and `90°`.
     */
    @Test
    fun testTargetAll_AllRotations_Fragment_PreviewView() {
        verifyTargetAllRotations(
            isActivity = false,
            frameCountProvider = { LowLightBoostEffectFragment.captureResultCount },
        )
    }

    /**
     * Verifies `Rebind on Resume` in [LowLightBoostEffectActivity] across all 4 display rotations
     * (`0°`, `90°`, `180°`, `270°`), including an `onPause` -> `onResume` lifecycle cycle while
     * rotated at `90°`.
     */
    @Test
    fun testRebindOnResume_AllRotations_Activity_ComposeViewfinder() {
        val frameCountProvider = { LowLightBoostEffectActivity.captureResultCount }
        val launchIntent =
            launchLlbHost(
                isActivity = true,
                binding = "rebind",
                frameCountProvider = frameCountProvider,
            )
        verifyRotationMatrixForConfig(
            isActivity = true,
            frameCountProvider = frameCountProvider,
            modes = listOf(R.id.radio_mode_performance to "Mode: PERFORMANCE"),
            contextDesc = "Rebind on Resume",
        )

        setDeviceRotation(Surface.ROTATION_90)
        if (
            waitForCondition(timeoutMs = 5000L) { uiDevice.displayRotation == Surface.ROTATION_90 }
        ) {
            assertWithMessage("Activity did not pause/stop on Home while rotated at 90°")
                .that(navigateHomeAndWaitStopped(uiDevice, isActivity = true, timeoutMs = 10000L))
                .isTrue()
            context.startActivity(
                Intent(launchIntent).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
            uiDevice.wait(Until.hasObject(By.pkg(context.packageName).depth(0)), 10000)
            assertWithMessage("Activity did not resume while rotated at 90°")
                .that(waitForActivityStage(isActivity = true, Stage.RESUMED, timeoutMs = 10000L))
                .isTrue()
            if (
                !waitForCondition(timeoutMs = 3000L) {
                    uiDevice.displayRotation == Surface.ROTATION_90
                }
            ) {
                // Portrait-locked Home launchers can reset display rotation while stopped;
                // re-apply 90°.
                setDeviceRotation(Surface.ROTATION_90)
            }
            assertWithMessage("Display did not return to ROTATION_90 after Home/Resume")
                .that(
                    waitForCondition(timeoutMs = 5000L) {
                        uiDevice.displayRotation == Surface.ROTATION_90
                    }
                )
                .isTrue()
            val afterResume = frameCountProvider()
            assertWithMessage("Rebind preview failed to stream after Home/Resume at 90°")
                .that(
                    waitForFrames(
                        frameCountProvider,
                        afterResume,
                        delta = 10L,
                        timeoutMs = 15000L,
                    )
                )
                .isTrue()
            assertWithMessage("Rebind status missing Rot: 90° after resume")
                .that(waitForStatusContains(isActivity = true, "Rot: 90° (ROTATION_90)"))
                .isTrue()
            assertLlbStateAfterSwitch(
                isActivity = true,
                "Activity Rebind after Home/Resume at 90°",
            )
            assertPreviewOrientationAndGeometry(
                isActivity = true,
                expectedRotation = Surface.ROTATION_90,
                message = "Activity Rebind after Home/Resume at 90°",
            )
            assertViewfinderUpdating(
                isActivity = true,
                "Activity Rebind after Home/Resume at 90°",
            )
        }
        setDeviceRotation(Surface.ROTATION_0)
        assertWithMessage("Rebind rotation didn't return to ROTATION_0")
            .that(waitForStatusContains(isActivity = true, "Rot: 0° (ROTATION_0)"))
            .isTrue()
    }

    /**
     * Verifies `Rebind on Resume` in [LowLightBoostEffectFragment] across all 4 display rotations
     * (`0°`, `90°`, `180°`, `270°`) and direct `90° -> 270°` half-turn, exercising `onPause`
     * unbinding and `onResume` rebinding across Activity recreations.
     */
    @Test
    fun testRebindOnResume_AllRotations_Fragment_PreviewView() {
        val frameCountProvider = { LowLightBoostEffectFragment.captureResultCount }
        launchLlbHost(
            isActivity = false,
            binding = "rebind",
            frameCountProvider = frameCountProvider,
        )
        verifyRotationMatrixForConfig(
            isActivity = false,
            frameCountProvider = frameCountProvider,
            modes = listOf(R.id.radio_mode_performance to "Mode: PERFORMANCE"),
            contextDesc = "Fragment Rebind on Resume",
        )
        verifyFragmentHalfTurnWithoutRecreation(frameCountProvider = frameCountProvider)
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private fun setDeviceRotation(rotation: Int) {
        LowLightBoostTestUtil.setDeviceRotation(uiDevice, rotation)
    }

    private fun launchLlbHost(
        isActivity: Boolean,
        binding: String = "bind_once",
        mode: String? = null,
        lens: String? = null,
        target: String? = null,
        frameCountProvider: () -> Long,
    ): Intent {
        prepareDeviceForActivityLaunch()
        val intent =
            if (isActivity) {
                Intent(context, LowLightBoostEffectActivity::class.java).apply {
                    putExtra(LowLightBoostEffectActivity.EXTRA_BINDING_BEHAVIOR, binding)
                    mode?.let { putExtra(LowLightBoostEffectActivity.EXTRA_MODE, it) }
                    lens?.let { putExtra(LowLightBoostEffectActivity.EXTRA_LENS, it) }
                    target?.let { putExtra(LowLightBoostEffectActivity.EXTRA_TARGET, it) }
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            } else {
                Intent(context, MainActivity::class.java).apply {
                    putExtra("fragment_type", "LowLightBoostEffect")
                    putExtra(LowLightBoostEffectFragment.EXTRA_BINDING_BEHAVIOR, binding)
                    mode?.let { putExtra(LowLightBoostEffectActivity.EXTRA_MODE, it) }
                    lens?.let { putExtra(LowLightBoostEffectActivity.EXTRA_LENS, it) }
                    target?.let { putExtra(LowLightBoostEffectActivity.EXTRA_TARGET, it) }
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            }

        context.startActivity(intent)
        uiDevice.wait(Until.hasObject(By.pkg(context.packageName).depth(0)), 10000)
        ensureNaturalRotationAfterLaunch(uiDevice)

        val label = if (isActivity) "Activity" else "Fragment"
        val initialCount = frameCountProvider()
        assertWithMessage("$label initial preview failed to start")
            .that(waitForFrames(frameCountProvider, initialCount, delta = 10L, timeoutMs = 15000L))
            .isTrue()
        assertPreviewOrientationAndGeometry(
            isActivity = isActivity,
            expectedRotation = Surface.ROTATION_0,
            message = "$label initial preview",
        )
        assertViewfinderUpdating(isActivity, "$label initial preview")
        return intent
    }

    private fun verifyTargetAllRotations(isActivity: Boolean, frameCountProvider: () -> Long) {
        val label = if (isActivity) "Activity" else "Fragment"
        launchLlbHost(
            isActivity = isActivity,
            target = "ALL",
            frameCountProvider = frameCountProvider,
        )
        assertWithMessage("$label status should report Target: ALL")
            .that(waitForStatusContains(isActivity, "Target: ALL"))
            .isTrue()

        verifyRotationMatrixForConfig(
            isActivity = isActivity,
            frameCountProvider = frameCountProvider,
            contextDesc = "Target: ALL",
        )

        if (CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT)) {
            clickView(isActivity, R.id.flip_camera_button)
            assertWithMessage("$label did not switch to Front camera with Target: ALL")
                .that(waitForStatusContains(isActivity, "Facing: Front"))
                .isTrue()
            val frontRotations =
                listOf(
                    Surface.ROTATION_0 to "0° (ROTATION_0)",
                    Surface.ROTATION_90 to "90° (ROTATION_90)",
                )
            verifyRotationMatrixForConfig(
                isActivity = isActivity,
                frameCountProvider = frameCountProvider,
                modes = listOf(R.id.radio_mode_compatible to "Mode: COMPATIBLE"),
                rotations = frontRotations,
                minDistinctNonZeroRotations = 1,
                contextDesc = "Target: ALL Front camera",
            )
        }
    }

    private fun verifyRotationMatrixForConfig(
        isActivity: Boolean,
        frameCountProvider: () -> Long,
        modes: List<Pair<Int, String>> = DEFAULT_MODES,
        rotations: List<Pair<Int, String>> = ALL_ROTATIONS,
        minDistinctNonZeroRotations: Int = 2,
        contextDesc: String,
    ) {
        val hostLabel = if (isActivity) "Activity (ComposeViewfinder)" else "Fragment (PreviewView)"
        val appliedNonZeroSet = mutableSetOf<Int>()

        for ((modeRadioId, expectedModeStr) in modes) {
            clickRadioButton(isActivity, modeRadioId)
            assertWithMessage("$hostLabel [$contextDesc] status did not update to $expectedModeStr")
                .that(waitForStatusContains(isActivity, expectedModeStr))
                .isTrue()
            val modeSwitchCount = frameCountProvider()
            assertWithMessage("$hostLabel [$contextDesc] failed to stream in $expectedModeStr")
                .that(
                    waitForFrames(
                        frameCountProvider,
                        modeSwitchCount,
                        delta = 5L,
                        timeoutMs = 15000L,
                    )
                )
                .isTrue()
            assertLlbStateAfterSwitch(isActivity, "$hostLabel [$contextDesc] in $expectedModeStr")

            for ((rotConstant, expectedRotStr) in rotations) {
                setDeviceRotation(rotConstant)
                // Some devices reject specific rotations under window policy (e.g. 180°). Skip any
                // rotation the display did not change to.
                if (
                    !waitForCondition(timeoutMs = 5000L) { uiDevice.displayRotation == rotConstant }
                ) {
                    Log.w(
                        TAG,
                        "$hostLabel [$contextDesc]: display rejected rotation $expectedRotStr in " +
                            expectedModeStr,
                    )
                    continue
                }
                if (rotConstant != Surface.ROTATION_0) {
                    appliedNonZeroSet.add(rotConstant)
                }
                val rotationUpdated =
                    waitForStatusContains(isActivity, "Rot: $expectedRotStr", timeoutMs = 10000L)
                assertWithMessage(
                        "$hostLabel [$contextDesc] rotation did not update to $expectedRotStr in " +
                            "$expectedModeStr: ${getStatusText(isActivity)}"
                    )
                    .that(rotationUpdated)
                    .isTrue()

                val beforeCount = frameCountProvider()
                assertWithMessage(
                        "$hostLabel [$contextDesc] preview failed to stream frames at " +
                            "$expectedRotStr in $expectedModeStr"
                    )
                    .that(
                        waitForFrames(
                            frameCountProvider,
                            beforeCount,
                            delta = 8L,
                            timeoutMs = 15000L,
                        )
                    )
                    .isTrue()
                assertWithMessage(
                        "$hostLabel [$contextDesc] status should report Stream: STREAMING at " +
                            "$expectedRotStr in $expectedModeStr"
                    )
                    .that(waitForStatusContains(isActivity, "Stream: STREAMING"))
                    .isTrue()
                assertNoUnexpectedLlbError(
                    getStatusText(isActivity),
                    "$hostLabel [$contextDesc] at $expectedRotStr in $expectedModeStr",
                )
                assertPreviewOrientationAndGeometry(
                    isActivity = isActivity,
                    expectedRotation = rotConstant,
                    message = "$hostLabel [$contextDesc] at $expectedRotStr in $expectedModeStr",
                )
                assertViewfinderUpdating(
                    isActivity,
                    "$hostLabel [$contextDesc] at $expectedRotStr in $expectedModeStr",
                )
            }
        }

        // Reset rotation to natural 0° and verify geometry before returning.
        setDeviceRotation(Surface.ROTATION_0)
        val returnedToRotation0 = waitForStatusContains(isActivity, "Rot: 0° (ROTATION_0)")
        assertWithMessage(
                "$hostLabel [$contextDesc] rotation didn't return to ROTATION_0: " +
                    getStatusText(isActivity)
            )
            .that(returnedToRotation0)
            .isTrue()
        assertPreviewOrientationAndGeometry(
            isActivity = isActivity,
            expectedRotation = Surface.ROTATION_0,
            message = "$hostLabel [$contextDesc] returned to 0°",
        )
        Log.i(
            TAG,
            "$hostLabel [$contextDesc] applied distinct non-zero rotations: $appliedNonZeroSet",
        )
        assumeTrue(
            "The display didn't apply enough distinct non-zero rotations " +
                "(${appliedNonZeroSet.size} < $minDistinctNonZeroRotations)",
            appliedNonZeroSet.size >= minDistinctNonZeroRotations,
        )
    }

    private fun verifyFragmentHalfTurnWithoutRecreation(frameCountProvider: () -> Long) {
        for ((modeRadioId, expectedModeStr) in DEFAULT_MODES) {
            clickRadioButton(isActivity = false, modeRadioId)
            assertWithMessage("Fragment half-turn status did not update to $expectedModeStr")
                .that(waitForStatusContains(isActivity = false, expectedModeStr))
                .isTrue()

            setDeviceRotation(Surface.ROTATION_90)
            if (
                !waitForCondition(timeoutMs = 5000L) {
                    uiDevice.displayRotation == Surface.ROTATION_90
                }
            ) {
                continue
            }
            assertWithMessage("Fragment half-turn did not reach 90° in $expectedModeStr")
                .that(waitForStatusContains(isActivity = false, "Rot: 90° (ROTATION_90)"))
                .isTrue()
            assertPreviewOrientationAndGeometry(
                isActivity = false,
                expectedRotation = Surface.ROTATION_90,
                message = "Fragment half-turn before flip in $expectedModeStr at 90°",
            )
            val activityAt90 = findActiveActivity(MainActivity::class.java)

            // Rotate 180° directly from landscape 90° -> reverse landscape 270°.
            setDeviceRotation(Surface.ROTATION_270)
            if (
                !waitForCondition(timeoutMs = 5000L) {
                    uiDevice.displayRotation == Surface.ROTATION_270
                }
            ) {
                continue
            }
            assertWithMessage("Fragment half-turn did not update to 270° in $expectedModeStr")
                .that(waitForStatusContains(isActivity = false, "Rot: 270° (ROTATION_270)"))
                .isTrue()
            val activityAt270 = findActiveActivity(MainActivity::class.java)
            assertWithMessage("MainActivity should remain non-null after 90°->270° half-turn")
                .that(activityAt270)
                .isNotNull()
            assertWithMessage(
                    "90°->270° half-turn ($expectedModeStr) should keep the same MainActivity " +
                        "instance"
                )
                .that(activityAt270)
                .isSameInstanceAs(activityAt90)
            Log.i(
                TAG,
                "Fragment 90°->270° half-turn ($expectedModeStr): sameInstance=" +
                    "${activityAt90 === activityAt270}",
            )
            val afterFlip = frameCountProvider()
            assertWithMessage("Fragment half-turn failed to stream at 270° in $expectedModeStr")
                .that(waitForFrames(frameCountProvider, afterFlip, delta = 8L, timeoutMs = 15000L))
                .isTrue()
            assertPreviewOrientationAndGeometry(
                isActivity = false,
                expectedRotation = Surface.ROTATION_270,
                message = "Fragment half-turn after flip in $expectedModeStr at 270°",
            )
            assertViewfinderUpdating(
                isActivity = false,
                "Fragment half-turn in $expectedModeStr at 270°",
            )
        }
        setDeviceRotation(Surface.ROTATION_0)
        assertWithMessage("Fragment half-turn didn't return to ROTATION_0")
            .that(waitForStatusContains(isActivity = false, "Rot: 0° (ROTATION_0)"))
            .isTrue()
        assertPreviewOrientationAndGeometry(
            isActivity = false,
            expectedRotation = Surface.ROTATION_0,
            message = "Fragment half-turn returned to 0°",
        )
    }

    private fun verifyFrontCameraCheckAtZero(
        isActivity: Boolean,
        frameCountProvider: () -> Long,
    ) {
        if (!CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT)) {
            return
        }
        val label = if (isActivity) "Activity (ComposeViewfinder)" else "Fragment (PreviewView)"
        clickView(isActivity, R.id.flip_camera_button)
        assertWithMessage("$label did not switch to Front camera")
            .that(waitForStatusContains(isActivity, "Facing: Front"))
            .isTrue()
        val frontBeforeCount = frameCountProvider()
        assertWithMessage("$label preview failed to stream on Front camera")
            .that(
                waitForFrames(
                    frameCountProvider,
                    frontBeforeCount,
                    delta = 8L,
                    timeoutMs = 15000L,
                )
            )
            .isTrue()
        assertLlbStateAfterSwitch(isActivity, "$label on Front camera")
        assertPreviewOrientationAndGeometry(
            isActivity = isActivity,
            expectedRotation = Surface.ROTATION_0,
            message = "$label on Front camera",
        )
        assertViewfinderUpdating(isActivity, "$label on Front camera")
    }

    private fun assumeLowLightBoostSupported(isActivity: Boolean) {
        LowLightBoostTestUtil.assumeLowLightBoostSupported(isActivity)
    }

    private fun verifyDynamicUiSwitches(
        isActivity: Boolean,
        frameCountProvider: () -> Long,
    ) {
        val label = if (isActivity) "Activity" else "Fragment"

        // 1. Switch implementation mode to COMPATIBLE
        clickRadioButton(isActivity, R.id.radio_mode_compatible)
        assertWithMessage("$label status did not update to COMPATIBLE mode")
            .that(waitForStatusContains(isActivity, "Mode: COMPATIBLE"))
            .isTrue()
        var beforeCount = frameCountProvider()
        assertWithMessage("$label preview failed to stream after switching to COMPATIBLE mode")
            .that(waitForFrames(frameCountProvider, beforeCount, delta = 10L, timeoutMs = 15000L))
            .isTrue()
        assertLlbStateAfterSwitch(isActivity, "$label after switching to COMPATIBLE mode")
        assertPreviewOrientationAndGeometry(
            isActivity = isActivity,
            expectedRotation = Surface.ROTATION_0,
            message = "$label after switching to COMPATIBLE mode",
        )
        assertViewfinderUpdating(isActivity, "$label after switching to COMPATIBLE mode")

        // 2. Switch implementation mode back to PERFORMANCE
        clickRadioButton(isActivity, R.id.radio_mode_performance)
        assertWithMessage("$label status did not update to PERFORMANCE mode")
            .that(waitForStatusContains(isActivity, "Mode: PERFORMANCE"))
            .isTrue()
        beforeCount = frameCountProvider()
        assertWithMessage("$label preview failed to stream after switching to PERFORMANCE mode")
            .that(waitForFrames(frameCountProvider, beforeCount, delta = 10L, timeoutMs = 15000L))
            .isTrue()
        assertLlbStateAfterSwitch(isActivity, "$label after switching to PERFORMANCE mode")
        assertPreviewOrientationAndGeometry(
            isActivity = isActivity,
            expectedRotation = Surface.ROTATION_0,
            message = "$label after switching to PERFORMANCE mode",
        )
        assertViewfinderUpdating(isActivity, "$label after switching to PERFORMANCE mode")

        // 3. Switch target setting to Preview + Video
        clickRadioButton(isActivity, R.id.radio_target_preview_video)
        assertWithMessage("$label status did not update to PREV|VIDEO target")
            .that(waitForStatusContains(isActivity, "Target: PREV|VIDEO"))
            .isTrue()
        beforeCount = frameCountProvider()
        assertWithMessage("$label preview failed to stream after switching to PREV|VIDEO target")
            .that(waitForFrames(frameCountProvider, beforeCount, delta = 10L, timeoutMs = 15000L))
            .isTrue()
        assertLlbStateAfterSwitch(isActivity, "$label after switching to PREV|VIDEO target")
        assertPreviewOrientationAndGeometry(
            isActivity = isActivity,
            expectedRotation = Surface.ROTATION_0,
            message = "$label after switching to PREV|VIDEO target",
        )
        assertViewfinderUpdating(isActivity, "$label after switching to PREV|VIDEO target")

        // 4. Switch target setting to All (Preview + Video + ImageCapture)
        clickRadioButton(isActivity, R.id.radio_target_all)
        assertWithMessage("$label status did not update to ALL target")
            .that(waitForStatusContains(isActivity, "Target: ALL"))
            .isTrue()
        beforeCount = frameCountProvider()
        assertWithMessage("$label preview failed to stream after switching to ALL target")
            .that(waitForFrames(frameCountProvider, beforeCount, delta = 10L, timeoutMs = 15000L))
            .isTrue()
        assertLlbStateAfterSwitch(isActivity, "$label after switching to ALL target")
        assertPreviewOrientationAndGeometry(
            isActivity = isActivity,
            expectedRotation = Surface.ROTATION_0,
            message = "$label after switching to ALL target",
        )
        assertViewfinderUpdating(isActivity, "$label after switching to ALL target")
    }

    private companion object {
        private const val TAG = "LowLightBoostEffectDeviceTest"

        // Retries a support query that timed out, since a timed-out result isn't cached.
        private const val SUPPORT_QUERY_ATTEMPTS = 3

        private val DEFAULT_MODES =
            listOf(
                R.id.radio_mode_performance to "Mode: PERFORMANCE",
                R.id.radio_mode_compatible to "Mode: COMPATIBLE",
            )

        private val ALL_ROTATIONS =
            listOf(
                Surface.ROTATION_0 to "0° (ROTATION_0)",
                Surface.ROTATION_90 to "90° (ROTATION_90)",
                Surface.ROTATION_180 to "180° (ROTATION_180)",
                Surface.ROTATION_270 to "270° (ROTATION_270)",
            )
    }
}
