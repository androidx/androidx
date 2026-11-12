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

package androidx.xr.scenecore.integration.tests

import androidx.compose.animation.core.LinearEasing
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Scene
import androidx.xr.scenecore.SpatialVisibility
import androidx.xr.scenecore.scene
import androidx.xr.testutils.FakeHeadTrackingRule
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.createXrSessionRule
import com.google.common.truth.Truth.assertWithMessage
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * End-to-end integration tests for [Scene.addSpatialVisibilityChangedListener] driven by simulated
 * 6DoF head rotation via [FakeHeadTrackingRule]:
 * - Full Space Mode head rotation away from and back toward a stationary panel
 *   (`WITHIN_FIELD_OF_VIEW` -> `PARTIALLY_WITHIN_FIELD_OF_VIEW` -> `OUTSIDE_FIELD_OF_VIEW` ->
 *   `WITHIN_FIELD_OF_VIEW`).
 * - Home Space Mode head rotation away from and back toward the main 2D window.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class FieldOfViewVisibilityHeadTrackingTest {

    @get:Rule(order = 0) val fakeHeadTrackingRule = FakeHeadTrackingRule()

    @get:Rule(order = 1) val sessionRule = createXrSessionRule()

    @get:Rule(order = 2) val timeout: Timeout = Timeout.seconds(120)

    private val recorder = SpatialVisibilityRecorder()
    private lateinit var anchors: FovAnchors

    @Before
    fun setUp() {
        val scene = sessionRule.session.scene
        scene.addSpatialVisibilityChangedListener(recorder)
        sessionRule.requestFullSpace()
        anchors = sessionRule.computeFovAnchors()
        // Hide the main panel so only each test's own content contributes to visibility bounds,
        // and start from OUTSIDE so subsequent transitions always trigger callbacks.
        scene.mainPanelEntity.setEnabled(false)
        recorder.awaitState(SpatialVisibility.OUTSIDE_FIELD_OF_VIEW)
    }

    @After
    fun tearDown() {
        sessionRule.session.scene.removeSpatialVisibilityChangedListener(recorder)
    }

    @Test
    fun fieldOfView_rotateHeadAwayAndBackInFullSpace_reportsWithinPartiallyAndOutside() {
        val panel = sessionRule.onMainThread {
            createFovTestPanel(
                name = "fovHeadRotationPanel",
                pose = anchors.behindUser,
                dimensions = FloatSize2d(HEAD_ROTATION_PANEL_WIDTH_M, PANEL_SIZE_M),
            )
        }
        panel.setPose(anchors.inFront)
        recorder.awaitState(SpatialVisibility.WITHIN_FIELD_OF_VIEW)

        // Smoothly sweep the simulated head yaw from 0° to 120° so the stationary panel crosses
        // the peripheral frustum boundary (PARTIALLY_WITHIN_FIELD_OF_VIEW) and then leaves the
        // frustum completely (OUTSIDE_FIELD_OF_VIEW).
        sweepHeadYawAwayAndAwaitOutside()

        // Resetting the head pose to look straight ahead returns the panel to WITHIN_FIELD_OF_VIEW.
        fakeHeadTrackingRule.resetHeadMovement()
        recorder.awaitState(SpatialVisibility.WITHIN_FIELD_OF_VIEW)
    }

    @Test
    fun fieldOfView_rotateHeadAwayAndBackInHomeSpace_reportsWithinPartiallyAndOutside() {
        // Transition to Home Space Mode; the 2D main window is placed in front of the user
        // (WITHIN_FIELD_OF_VIEW or PARTIALLY_WITHIN_FIELD_OF_VIEW depending on device FOV).
        sessionRule.requestHomeSpace()
        recorder.awaitAnyState(
            SpatialVisibility.WITHIN_FIELD_OF_VIEW,
            SpatialVisibility.PARTIALLY_WITHIN_FIELD_OF_VIEW,
        )

        // Sweep the head yaw from 0° to 120° so the main window crosses the frustum edge and
        // leaves the field of view completely.
        sweepHeadYawAwayAndAwaitOutside()

        // Looking straight ahead again brings the HSM main window back into view.
        fakeHeadTrackingRule.resetHeadMovement()
        recorder.awaitAnyState(
            SpatialVisibility.WITHIN_FIELD_OF_VIEW,
            SpatialVisibility.PARTIALLY_WITHIN_FIELD_OF_VIEW,
        )
    }

    private fun sweepHeadYawAwayAndAwaitOutside() {
        val markBeforeSweep = recorder.mark()
        fakeHeadTrackingRule.performHeadMovement {
            animate(durationMs = HEAD_SWEEP_DURATION_MS, easing = LinearEasing) { fraction ->
                rotateHeadTo(Quaternion.fromAxisAngle(Vector3.Up, LOOK_AWAY_YAW_DEGREES * fraction))
            }
        }
        recorder.awaitState(SpatialVisibility.OUTSIDE_FIELD_OF_VIEW)
        assertWithMessage("Events since mark: ${recorder.eventsSince(markBeforeSweep)}")
            .that(recorder.stateBeforeLatest(markBeforeSweep))
            .isEqualTo(SpatialVisibility.PARTIALLY_WITHIN_FIELD_OF_VIEW)
    }

    private companion object {
        /**
         * Narrow enough to fit within the field of view before the sweep, and wide enough to linger
         * across the frustum edge as the head rotates past it.
         */
        const val HEAD_ROTATION_PANEL_WIDTH_M = 0.5f

        /**
         * Constant 20°/s sweep duration so the edge crossing is slow enough to register cleanly.
         */
        const val HEAD_SWEEP_DURATION_MS = 6000L
        const val LOOK_AWAY_YAW_DEGREES = 120f
    }
}
