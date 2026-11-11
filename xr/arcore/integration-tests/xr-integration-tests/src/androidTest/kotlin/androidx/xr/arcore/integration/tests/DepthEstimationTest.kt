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
import androidx.xr.arcore.Depth
import androidx.xr.arcore.perceptionState
import androidx.xr.runtime.Config
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.RenderingMode
import androidx.xr.runtime.SessionConfigureResult
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.XrDevice
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.filterSupportedPermissions
import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated integration tests for JXR ARCore [Depth] estimation state, dimensions, and depth buffer
 * invariants across [DepthEstimationMode] configurations.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class DepthEstimationTest {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            *filterSupportedPermissions(
                "android.permission.SCENE_UNDERSTANDING_FINE",
                "android.permission.SCENE_UNDERSTANDING_COARSE",
                "android.permission.HEAD_TRACKING",
            )
        )

    @get:Rule(order = 1) val sessionRule = createXrSessionRule()

    private data class DepthBufferSample(
        val capacity: Int,
        val limit: Int,
        val isReadOnly: Boolean,
        val firstPixel: Float?,
        val centerPixel: Float?,
    )

    private data class ConfidenceBufferSample(
        val capacity: Int,
        val limit: Int,
        val isReadOnly: Boolean,
    )

    private data class EyeDepthSample(
        val owner: Depth,
        val width: Int,
        val height: Int,
        val rawDepth: DepthBufferSample?,
        val rawConfidence: ConfidenceBufferSample?,
        val rawValidConfidentPixelCount: Int,
        val smoothDepth: DepthBufferSample?,
        val smoothConfidence: ConfidenceBufferSample?,
        val smoothValidConfidentPixelCount: Int,
    )

    private data class StereoDepthSample(
        val configuredMode: DepthEstimationMode,
        val leftDepth: Depth,
        val rightDepth: Depth,
        val leftSample: EyeDepthSample,
        val rightSample: EyeDepthSample,
        val perceptionLeftOwner: Depth?,
        val perceptionRightOwner: Depth?,
    )

    private data class DepthModeToggleTrace(
        val rawInitial: StereoDepthSample,
        val smoothToggled: StereoDepthSample,
        val rawRestored: StereoDepthSample,
        val disableResult: SessionConfigureResult,
        val disabledMode: DepthEstimationMode,
        val clearedLeftDepthState: Depth.State,
        val clearedRightDepthState: Depth.State,
        val clearedLeftPerceptionState: Depth.State?,
        val clearedRightPerceptionState: Depth.State?,
    )

    private fun assumeStereoDepthSupported(vararg modes: DepthEstimationMode) {
        val xrDevice = XrDevice.getCurrentDevice(sessionRule.session.context)
        for (mode in modes) {
            assumeTrue(
                "Skipping test: Device does not support $mode",
                xrDevice.isDepthEstimationModeSupported(mode),
            )
        }
        assumeTrue(
            "Skipping stereo depth test: Device does not support STEREO rendering",
            xrDevice.isRenderingModeSupported(RenderingMode.STEREO),
        )
    }

    private fun countValidConfidentDepthPixels(
        depthBuffer: FloatBuffer?,
        confidenceBuffer: ByteBuffer?,
    ): Int {
        if (depthBuffer == null || confidenceBuffer == null) return 0
        val pixelCount = minOf(depthBuffer.limit(), confidenceBuffer.limit())
        var validCount = 0
        for (i in 0 until pixelCount) {
            val depth = depthBuffer.get(i)
            // Depth confidence values are unsigned uint8 (0..255); ByteBuffer.get returns a
            // signed Byte where values >= 128 appear negative without an 0xFF bitmask.
            val confidence = confidenceBuffer.get(i).toInt() and 0xFF
            if (depth.isFinite() && depth > 0f && confidence > 0) {
                validCount++
            }
        }
        return validCount
    }

    private fun hasActiveConfidentDepthFrame(
        state: Depth.State,
        mode: DepthEstimationMode,
    ): Boolean {
        if (state.width <= 0 || state.height <= 0) return false
        return when (mode) {
            DepthEstimationMode.RAW_ONLY ->
                countValidConfidentDepthPixels(state.rawDepthMap, state.rawConfidenceMap) > 0
            DepthEstimationMode.SMOOTH_ONLY ->
                countValidConfidentDepthPixels(state.smoothDepthMap, state.smoothConfidenceMap) > 0
            else -> false
        }
    }

    private fun sampleDepthBuffer(
        buffer: FloatBuffer?,
        width: Int,
        height: Int,
    ): DepthBufferSample? {
        if (buffer == null) return null
        val centerIndex = (height / 2) * width + (width / 2)
        return DepthBufferSample(
            capacity = buffer.capacity(),
            limit = buffer.limit(),
            isReadOnly = buffer.isReadOnly,
            firstPixel = if (buffer.limit() > 0) buffer.get(0) else null,
            centerPixel =
                if (centerIndex in 0 until buffer.limit()) buffer.get(centerIndex) else null,
        )
    }

    private fun sampleConfidenceBuffer(buffer: ByteBuffer?): ConfidenceBufferSample? {
        if (buffer == null) return null
        return ConfidenceBufferSample(
            capacity = buffer.capacity(),
            limit = buffer.limit(),
            isReadOnly = buffer.isReadOnly,
        )
    }

    private fun sampleEyeDepthState(state: Depth.State): EyeDepthSample {
        return EyeDepthSample(
            owner = state.owner,
            width = state.width,
            height = state.height,
            rawDepth = sampleDepthBuffer(state.rawDepthMap, state.width, state.height),
            rawConfidence = sampleConfidenceBuffer(state.rawConfidenceMap),
            rawValidConfidentPixelCount =
                countValidConfidentDepthPixels(state.rawDepthMap, state.rawConfidenceMap),
            smoothDepth = sampleDepthBuffer(state.smoothDepthMap, state.width, state.height),
            smoothConfidence = sampleConfidenceBuffer(state.smoothConfidenceMap),
            smoothValidConfidentPixelCount =
                countValidConfidentDepthPixels(state.smoothDepthMap, state.smoothConfidenceMap),
        )
    }

    private suspend fun awaitStereoDepthSample(
        mode: DepthEstimationMode,
        requirePopulatedFrames: Boolean = false,
    ): StereoDepthSample {
        val session = sessionRule.session
        val leftDepth = Depth.left(session)
        val rightDepth = Depth.right(session)
        // OpenXrDepth initializes width and height at configure() time before native frames arrive,
        // so require at least one valid positive depth pixel with nonzero unsigned confidence.
        val coreStateUpdate =
            withTimeoutOrNull(FRAME_TIMEOUT_MS) {
                session.state.drop(1).first {
                    val leftState = it.perceptionState?.leftDepthState
                    val rightState = it.perceptionState?.rightDepthState
                    leftState != null &&
                        rightState != null &&
                        hasActiveConfidentDepthFrame(leftState, mode) &&
                        hasActiveConfidentDepthFrame(rightState, mode)
                }
            }
        if (!requirePopulatedFrames) {
            assumeTrue(
                "Skipping buffer assertions: Runtime did not emit confident $mode depth frames",
                coreStateUpdate != null,
            )
        }
        val nonNullUpdate =
            checkNotNull(coreStateUpdate) {
                "Timed out waiting for confident $mode depth frames after mode transition"
            }
        val perceptionState = checkNotNull(nonNullUpdate.perceptionState)
        val leftState = checkNotNull(perceptionState.leftDepthState)
        val rightState = checkNotNull(perceptionState.rightDepthState)
        // Copy buffer metadata and pixel counts while the current swapchain is still active
        // before subsequent reconfigure or disable calls destroy native buffers.
        return StereoDepthSample(
            configuredMode = session.config.depthEstimation,
            leftDepth = leftDepth,
            rightDepth = rightDepth,
            leftSample = sampleEyeDepthState(leftState),
            rightSample = sampleEyeDepthState(rightState),
            perceptionLeftOwner = leftState.owner,
            perceptionRightOwner = rightState.owner,
        )
    }

    private suspend fun configureAndSampleStereoDepth(
        mode: DepthEstimationMode,
        requirePopulatedFrames: Boolean = false,
    ): StereoDepthSample {
        // TODO: b/581124636 - Keep depth configurations isolated until native AbortConfigureSession
        // rollback regression is resolved.
        val config = Config.Builder().setDepthEstimation(mode).build()
        val configResult = sessionRule.session.configure(config)
        check(configResult is SessionConfigureSuccess) {
            "Failed to configure $mode: $configResult"
        }
        return awaitStereoDepthSample(mode, requirePopulatedFrames)
    }

    private fun assertDepthBufferSample(sample: DepthBufferSample?, expectedCapacity: Int) {
        val nonNullSample = assertNotNull(sample, "Expected non-null active depth map")
        assertThat(nonNullSample.capacity).isEqualTo(expectedCapacity)
        assertThat(nonNullSample.limit).isEqualTo(expectedCapacity)
        assertThat(nonNullSample.isReadOnly).isTrue()
        val firstPixel = assertNotNull(nonNullSample.firstPixel, "Expected non-null first pixel")
        assertThat(firstPixel).isNotNaN()
        assertThat(firstPixel).isAtLeast(0f)
        val centerPixel = assertNotNull(nonNullSample.centerPixel, "Expected non-null center pixel")
        assertThat(centerPixel).isNotNaN()
        assertThat(centerPixel).isAtLeast(0f)
    }

    private fun assertConfidenceBufferSample(
        sample: ConfidenceBufferSample?,
        expectedCapacity: Int,
    ) {
        val nonNullSample = assertNotNull(sample, "Expected non-null active confidence map")
        assertThat(nonNullSample.capacity).isEqualTo(expectedCapacity)
        assertThat(nonNullSample.limit).isEqualTo(expectedCapacity)
        assertThat(nonNullSample.isReadOnly).isTrue()
    }

    private fun assertClearedDepthState(state: Depth.State) {
        assertThat(state.width).isEqualTo(0)
        assertThat(state.height).isEqualTo(0)
        assertThat(state.rawDepthMap).isNull()
        assertThat(state.rawConfidenceMap).isNull()
        assertThat(state.smoothDepthMap).isNull()
        assertThat(state.smoothConfidenceMap).isNull()
    }

    private fun assertStereoRawDepthSample(sample: StereoDepthSample) {
        assertThat(sample.configuredMode).isEqualTo(DepthEstimationMode.RAW_ONLY)
        assertThat(sample.perceptionLeftOwner).isEqualTo(sample.leftDepth)
        assertThat(sample.perceptionRightOwner).isEqualTo(sample.rightDepth)
        assertRawEyeDepthSample(sample.leftDepth, sample.leftSample)
        assertRawEyeDepthSample(sample.rightDepth, sample.rightSample)
    }

    private fun assertStereoSmoothDepthSample(sample: StereoDepthSample) {
        assertThat(sample.configuredMode).isEqualTo(DepthEstimationMode.SMOOTH_ONLY)
        assertThat(sample.perceptionLeftOwner).isEqualTo(sample.leftDepth)
        assertThat(sample.perceptionRightOwner).isEqualTo(sample.rightDepth)
        assertSmoothEyeDepthSample(sample.leftDepth, sample.leftSample)
        assertSmoothEyeDepthSample(sample.rightDepth, sample.rightSample)
    }

    private fun assertRawEyeDepthSample(depth: Depth, eyeSample: EyeDepthSample) {
        assertThat(eyeSample.owner).isEqualTo(depth)
        assertThat(eyeSample.width).isGreaterThan(0)
        assertThat(eyeSample.height).isGreaterThan(0)
        val expectedCapacity = eyeSample.width * eyeSample.height
        assertDepthBufferSample(eyeSample.rawDepth, expectedCapacity)
        assertConfidenceBufferSample(eyeSample.rawConfidence, expectedCapacity)
        assertThat(eyeSample.rawValidConfidentPixelCount).isGreaterThan(0)
        assertThat(eyeSample.smoothDepth).isNull()
        assertThat(eyeSample.smoothConfidence).isNull()
    }

    private fun assertSmoothEyeDepthSample(depth: Depth, eyeSample: EyeDepthSample) {
        assertThat(eyeSample.owner).isEqualTo(depth)
        assertThat(eyeSample.width).isGreaterThan(0)
        assertThat(eyeSample.height).isGreaterThan(0)
        val expectedCapacity = eyeSample.width * eyeSample.height
        assertDepthBufferSample(eyeSample.smoothDepth, expectedCapacity)
        assertConfidenceBufferSample(eyeSample.smoothConfidence, expectedCapacity)
        assertThat(eyeSample.smoothValidConfidentPixelCount).isGreaterThan(0)
        assertThat(eyeSample.rawDepth).isNull()
        assertThat(eyeSample.rawConfidence).isNull()
    }

    @Test
    @XrDeviceTest
    fun rawDepth_whenRawOnlyAndStereoSupported_emitsRawDepthBuffersWithMatchingCapacity() {
        runBlocking {
            assumeStereoDepthSupported(DepthEstimationMode.RAW_ONLY)

            val sample = configureAndSampleStereoDepth(DepthEstimationMode.RAW_ONLY)

            assertStereoRawDepthSample(sample)
        }
    }

    @Test
    @XrDeviceTest
    fun smoothDepth_whenSmoothOnlyAndStereoSupported_emitsSmoothDepthBuffersWithMatchingCapacity() {
        runBlocking {
            assumeStereoDepthSupported(DepthEstimationMode.SMOOTH_ONLY)

            val sample = configureAndSampleStereoDepth(DepthEstimationMode.SMOOTH_ONLY)

            assertStereoSmoothDepthSample(sample)
        }
    }

    @Test
    @XrDeviceTest
    fun depthEstimation_whenTogglingModes_updatesBuffersAndThrowsWhenDisabled() {
        runBlocking {
            val session = sessionRule.session
            assumeStereoDepthSupported(
                DepthEstimationMode.RAW_ONLY,
                DepthEstimationMode.SMOOTH_ONLY,
            )

            val rawInitial = configureAndSampleStereoDepth(DepthEstimationMode.RAW_ONLY)
            val smoothToggled =
                configureAndSampleStereoDepth(
                    DepthEstimationMode.SMOOTH_ONLY,
                    requirePopulatedFrames = true,
                )
            val rawRestored =
                configureAndSampleStereoDepth(
                    DepthEstimationMode.RAW_ONLY,
                    requirePopulatedFrames = true,
                )
            val disableResult =
                session.configure(
                    Config.Builder().setDepthEstimation(DepthEstimationMode.DISABLED).build()
                )
            val disabledMode = session.config.depthEstimation
            val disabledFrame =
                checkNotNull(
                    withTimeoutOrNull(FRAME_TIMEOUT_MS) {
                        session.state.drop(1).first { it.perceptionState != null }
                    }
                ) {
                    "Timed out waiting for perception state after disabling depth estimation"
                }
            val disabledPerception = checkNotNull(disabledFrame.perceptionState)
            val trace =
                DepthModeToggleTrace(
                    rawInitial = rawInitial,
                    smoothToggled = smoothToggled,
                    rawRestored = rawRestored,
                    disableResult = disableResult,
                    disabledMode = disabledMode,
                    clearedLeftDepthState = rawRestored.leftDepth.state.value,
                    clearedRightDepthState = rawRestored.rightDepth.state.value,
                    clearedLeftPerceptionState = disabledPerception.leftDepthState,
                    clearedRightPerceptionState = disabledPerception.rightDepthState,
                )

            assertStereoRawDepthSample(trace.rawInitial)
            assertStereoSmoothDepthSample(trace.smoothToggled)
            assertStereoRawDepthSample(trace.rawRestored)
            assertThat(trace.disableResult).isInstanceOf(SessionConfigureSuccess::class.java)
            assertThat(trace.disabledMode).isEqualTo(DepthEstimationMode.DISABLED)
            assertClearedDepthState(trace.clearedLeftDepthState)
            assertClearedDepthState(trace.clearedRightDepthState)
            assertClearedDepthState(
                assertNotNull(
                    trace.clearedLeftPerceptionState,
                    "Expected non-null cleared left perception depth state",
                )
            )
            assertClearedDepthState(
                assertNotNull(
                    trace.clearedRightPerceptionState,
                    "Expected non-null cleared right perception depth state",
                )
            )
            assertFailsWith<IllegalStateException> { Depth.left(session) }
            assertFailsWith<IllegalStateException> { Depth.right(session) }
        }
    }

    @Test
    @XrDeviceTest
    fun depthEstimation_configureSmoothAndRawWhenUnsupported_throwsUnsupportedOperationException() {
        val session = sessionRule.session
        val xrDevice = XrDevice.getCurrentDevice(session.context)
        assumeFalse(
            "Skipping test: Device supports DepthEstimationMode.SMOOTH_AND_RAW",
            xrDevice.isDepthEstimationModeSupported(DepthEstimationMode.SMOOTH_AND_RAW),
        )
        val config = Config.Builder().setDepthEstimation(DepthEstimationMode.SMOOTH_AND_RAW).build()

        assertFailsWith<UnsupportedOperationException> { session.configure(config) }
        assertThat(session.config.depthEstimation).isEqualTo(DepthEstimationMode.DISABLED)
    }

    @Test
    @XrDeviceTest
    fun depthEstimation_configureSmoothAndRawWhenRawOnlyActive_keepsRawOnlyConfigAndBuffers() {
        runBlocking {
            val session = sessionRule.session
            assumeStereoDepthSupported(DepthEstimationMode.RAW_ONLY)
            val xrDevice = XrDevice.getCurrentDevice(session.context)
            assumeFalse(
                "Skipping test: Device supports DepthEstimationMode.SMOOTH_AND_RAW",
                xrDevice.isDepthEstimationModeSupported(DepthEstimationMode.SMOOTH_AND_RAW),
            )
            val rawBefore = configureAndSampleStereoDepth(DepthEstimationMode.RAW_ONLY)
            val rejectedConfig =
                Config.Builder().setDepthEstimation(DepthEstimationMode.SMOOTH_AND_RAW).build()

            val rejectedFailure = runCatching {
                session.configure(rejectedConfig)
            }
                .exceptionOrNull()
            val modeAfterRejection = session.config.depthEstimation
            val rawAfter =
                awaitStereoDepthSample(DepthEstimationMode.RAW_ONLY, requirePopulatedFrames = true)

            assertThat(rejectedFailure).isInstanceOf(UnsupportedOperationException::class.java)
            assertThat(modeAfterRejection).isEqualTo(DepthEstimationMode.RAW_ONLY)
            assertStereoRawDepthSample(rawBefore)
            assertStereoRawDepthSample(rawAfter)
            assertThat(rawAfter.leftDepth).isSameInstanceAs(rawBefore.leftDepth)
            assertThat(rawAfter.rightDepth).isSameInstanceAs(rawBefore.rightDepth)
        }
    }

    @Test
    @XrDeviceTest
    fun depthMap_whenDepthEstimationDisabled_throwsIllegalStateException() {
        val session = sessionRule.session
        val config = Config.Builder().setDepthEstimation(DepthEstimationMode.DISABLED).build()

        val configResult = session.configure(config)

        assertThat(configResult).isInstanceOf(SessionConfigureSuccess::class.java)
        assertFailsWith<IllegalStateException> { Depth.left(session) }
        assertFailsWith<IllegalStateException> { Depth.right(session) }
    }

    companion object {
        private const val FRAME_TIMEOUT_MS = 5_000L
    }
}
