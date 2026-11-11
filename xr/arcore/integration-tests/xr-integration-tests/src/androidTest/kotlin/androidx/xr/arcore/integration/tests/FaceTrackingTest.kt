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

package androidx.xr.arcore.integration.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import androidx.xr.arcore.Face
import androidx.xr.arcore.FaceConfidenceRegion
import androidx.xr.arcore.PerceptionState
import androidx.xr.arcore.TrackingState
import androidx.xr.arcore.perceptionState
import androidx.xr.runtime.Config
import androidx.xr.runtime.FaceTrackingMode
import androidx.xr.runtime.RequiredCalibrationType
import androidx.xr.runtime.SessionConfigureCalibrationRequired
import androidx.xr.runtime.SessionConfigureResult
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.filterSupportedPermissions
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assume.assumeTrue
import org.junit.AssumptionViolatedException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated integration tests for JXR ARCore [Face] tracking, blend-shape weights, and regional
 * confidence values.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class FaceTrackingTest {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            *filterSupportedPermissions(
                "android.permission.FACE_TRACKING",
                "android.permission.HEAD_TRACKING",
            )
        )

    @get:Rule(order = 1) val sessionRule = createXrSessionRule()

    private fun configureBlendShapesOrSkipIfUnsupported(): SessionConfigureResult {
        // TODO: b/581124636 - Include non-face modes in multi-feature config tests once native
        // AbortConfigureSession regression is resolved.
        val config = Config.Builder().setFaceTracking(FaceTrackingMode.BLEND_SHAPES).build()
        return try {
            sessionRule.session.configure(config)
        } catch (e: UnsupportedOperationException) {
            throw AssumptionViolatedException(
                "Skipping test: FaceTrackingMode.BLEND_SHAPES is not supported: ${e.message}",
                e,
            )
        }
    }

    private suspend fun configureCalibratedBlendShapesSession(): PerceptionState {
        val session = sessionRule.session
        val configResult = configureBlendShapesOrSkipIfUnsupported()
        if (configResult is SessionConfigureCalibrationRequired) {
            throw AssumptionViolatedException(
                "Skipping test: Face tracking calibration is required (${configResult.calibrationType})"
            )
        }
        check(configResult is SessionConfigureSuccess) {
            "Failed to configure FaceTrackingMode.BLEND_SHAPES: $configResult"
        }
        val stateUpdate =
            checkNotNull(
                withTimeoutOrNull(FRAME_TIMEOUT_MS) {
                    session.state.drop(1).first { it.perceptionState != null }
                }
            ) {
                "Timed out waiting for perception state"
            }
        return checkNotNull(stateUpdate.perceptionState)
    }

    private fun assertAllBlendShapesNormalized(faceState: Face.State) {
        // FaceBlendShapeType uses @JvmField constants rather than an enum, and
        // Face.State.blendShapes zips the library's 68-key list with runtime values, so size 68
        // guarantees all keys exist.
        assertThat(faceState.blendShapes).hasSize(EXPECTED_BLEND_SHAPE_COUNT)
        for (weight in faceState.blendShapes.values) {
            assertThat(weight).isAtLeast(0.0f)
            assertThat(weight).isAtMost(1.0f)
        }
    }

    @Test
    @XrDeviceTest
    fun configure_whenBlendShapesCalibrated_enablesConfigAndReturnsUserFace() {
        runBlocking {
            val session = sessionRule.session

            val perceptionState = configureCalibratedBlendShapesSession()
            val userFace = Face.getUserFace(session)

            assertThat(session.config.faceTracking).isEqualTo(FaceTrackingMode.BLEND_SHAPES)
            assertThat(perceptionState.userFaceState?.owner).isEqualTo(userFace)
        }
    }

    @Test
    @XrDeviceTest
    fun configure_whenFaceNotCalibrated_returnsCalibrationRequiredAndKeepsPreviousConfig() {
        val session = sessionRule.session

        val configResult = configureBlendShapesOrSkipIfUnsupported()
        assumeTrue(
            "Skipping uncalibrated branch test: Device is already face-calibrated",
            configResult !is SessionConfigureSuccess,
        )

        val calibrationRequired = assertIs<SessionConfigureCalibrationRequired>(configResult)
        assertThat(calibrationRequired.calibrationType)
            .isEqualTo(RequiredCalibrationType.REQUIRED_CALIBRATION_TYPE_FACE_TRACKING)
        assertThat(session.config.faceTracking).isEqualTo(FaceTrackingMode.DISABLED)
        assertFailsWith<IllegalStateException> { Face.getUserFace(session) }
    }

    @Test
    @XrDeviceTest
    fun getConfidence_whenActivelyTracking_returnsUnitRangeAndNormalizedBlendShapes() {
        runBlocking {
            val session = sessionRule.session
            val perceptionState = configureCalibratedBlendShapesSession()
            val userFace = Face.getUserFace(session)

            // TODO: b/575891923 - Migrate to synthetic/mocked face tracking input once Pillar 5
            // perception simulation supports face tracking so active blend-shape and confidence
            // assertions run deterministically.
            val trackedFaceState =
                withTimeoutOrNull(FRAME_TIMEOUT_MS) {
                    userFace.state.first { it.trackingState == TrackingState.TRACKING }
                }
            assumeTrue(
                "Skipping active confidence assertions: User face did not enter TRACKING state",
                trackedFaceState != null,
            )
            val activeFace = checkNotNull(trackedFaceState)
            val lowerConfidence = activeFace.getConfidence(FaceConfidenceRegion.LOWER)
            val leftUpperConfidence = activeFace.getConfidence(FaceConfidenceRegion.LEFT_UPPER)
            val rightUpperConfidence = activeFace.getConfidence(FaceConfidenceRegion.RIGHT_UPPER)

            assertThat(perceptionState.userFaceState?.owner).isEqualTo(userFace)
            assertThat(activeFace.owner).isEqualTo(userFace)
            assertAllBlendShapesNormalized(activeFace)
            assertThat(lowerConfidence).isAtLeast(0.0f)
            assertThat(lowerConfidence).isAtMost(1.0f)
            assertThat(leftUpperConfidence).isAtLeast(0.0f)
            assertThat(leftUpperConfidence).isAtMost(1.0f)
            assertThat(rightUpperConfidence).isAtLeast(0.0f)
            assertThat(rightUpperConfidence).isAtMost(1.0f)
        }
    }

    @Test
    @XrDeviceTest
    fun userFace_whenFaceTrackingDisabled_throwsIllegalStateException() {
        val session = sessionRule.session
        val config = Config.Builder().setFaceTracking(FaceTrackingMode.DISABLED).build()

        val configResult = session.configure(config)

        assertThat(configResult).isInstanceOf(SessionConfigureSuccess::class.java)
        assertFailsWith<IllegalStateException> { Face.getUserFace(session) }
    }

    companion object {
        private const val FRAME_TIMEOUT_MS = 5_000L
        private const val EXPECTED_BLEND_SHAPE_COUNT = 68
    }
}
