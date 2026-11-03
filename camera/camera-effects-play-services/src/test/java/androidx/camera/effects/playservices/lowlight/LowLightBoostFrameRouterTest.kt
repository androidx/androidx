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
package androidx.camera.effects.playservices.lowlight

import android.graphics.SurfaceTexture
import android.opengl.Matrix
import android.os.Build
import android.os.Handler
import android.os.Looper.getMainLooper
import android.util.Size
import android.view.Surface
import androidx.test.filters.SdkSuppress
import com.google.android.gms.cameralowlight.LowLightBoostSession
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock

/** Verifies [LowLightBoostFrameRouter] with a fake session and fake OpenGL renderers. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
// Tells lint that the test needs API level 30 for LowLightBoostSession.
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
class LowLightBoostFrameRouterTest {

    private val renderPipeline =
        LowLightBoostRenderPipeline(
            glHandler = Handler(getMainLooper()),
            rendererFactory = { FakeGlRenderer() },
            onInputFrameAvailable = { _, _ -> },
            onOutputFrameAvailable = { _, _ -> },
        )
    private val blockingCalls = BlockingCallTracker()
    private val host = FakeHost()
    private val router = LowLightBoostFrameRouter(renderPipeline, blockingCalls, host)
    private val session = FakeLowLightBoostSession()
    private val outputTexture = SurfaceTexture(OUTPUT_TEXTURE_NAME)
    private val outputSurface = Surface(outputTexture)
    private var lastTimestampNs = 0L

    @Before
    fun setUp() {
        renderPipeline.ensureInputGlInitialized(Size(1920, 1080))
        renderPipeline.ensureBoostedGlInitialized(Size(1920, 1080))
        renderPipeline.registerOutputSurface(FakeSurfaceOutput(outputSurface), outputSurface)
    }

    @After
    fun tearDown() {
        renderPipeline.releaseBridges()
        session.cameraSurfaceTexture.release()
        outputSurface.release()
        outputTexture.release()
    }

    @Test
    fun boostDisabled_rendersPassthroughFrames() {
        // Act.
        feedCameraFrames(count = 3)

        // Assert.
        assertThat(inputRenderer().renderedSurfaces)
            .containsExactly(outputSurface, outputSurface, outputSurface)
        assertThat(host.sessionNeededCount).isEqualTo(0)
    }

    @Test
    fun boostEnabledWithoutSession_reportsSessionNeeded() {
        // Arrange.
        host.isEnabled = true

        // Act.
        feedCameraFrames(count = 3)

        // Assert: frames are still passed through.
        assertThat(host.sessionNeededCount).isEqualTo(3)
        assertThat(inputRenderer().renderedSurfaces).contains(outputSurface)
    }

    @Test
    fun outputShowsPassthroughFramesUntilFirstBoostedFrame() {
        // Arrange.
        startBoosting()

        // Act: a camera frame arrives before any boosted frame.
        router.onInputFrameAvailable(1_000L, IDENTITY)

        // Assert: the frame is sent to the session and also shown as a passthrough frame.
        assertThat(inputRenderer().renderedSurfaces)
            .containsExactly(session.cameraSurfaceForTest, outputSurface)
            .inOrder()

        // Act: the first boosted frame arrives, then another camera frame.
        router.onOutputFrameAvailable(1_000L + 1, IDENTITY)
        inputRenderer().renderedSurfaces.clear()
        router.onInputFrameAvailable(2_000L, IDENTITY)

        // Assert: the output shows boosted frames, and camera frames only go to the session.
        assertThat(outputRenderer().renderedSurfaces).containsExactly(outputSurface)
        assertThat(inputRenderer().renderedSurfaces).containsExactly(session.cameraSurfaceForTest)
        assertThat(inputRenderer().registeredSurfaces).doesNotContain(outputSurface)
    }

    @Test
    fun boostedFrameOfShownPassthroughFrame_isDropped() {
        // Arrange.
        startBoosting()
        router.onInputFrameAvailable(2_000L, IDENTITY)

        // Act: the boosted frame of the camera frame already shown arrives, then a new one.
        router.onOutputFrameAvailable(2_000L, IDENTITY)
        val renderedAfterShownFrame = outputRenderer().renderedSurfaces.toList()
        inputRenderer().renderedSurfaces.clear()
        router.onInputFrameAvailable(3_000L, IDENTITY)
        router.onOutputFrameAvailable(3_000L, IDENTITY)

        // Assert: the frame already shown is dropped, so the output timestamps always increase,
        // and no more passthrough frames are shown.
        assertThat(renderedAfterShownFrame).isEmpty()
        assertThat(inputRenderer().renderedSurfaces).containsExactly(session.cameraSurfaceForTest)
        assertThat(outputRenderer().renderedSurfaces).containsExactly(outputSurface)
    }

    @Test
    fun sessionWithoutBoostedFrames_isReportedUnusableAfterFirstFrameTimeout() {
        // Arrange.
        startBoosting()

        // Act: camera frames keep arriving for 6 seconds (3 seconds plus 3 extra seconds for
        // the first boosted frame), then for a little longer.
        feedCameraFrames(count = 60)
        val unusableAfterSixSeconds = host.unusableSessions.toList()
        feedCameraFrames(count = 2)

        // Assert.
        assertThat(unusableAfterSixSeconds).isEmpty()
        assertThat(host.unusableSessions).contains(session)
    }

    @Test
    fun sessionThatStopsProducingFrames_isReportedUnusableAfterThreeSeconds() {
        // Arrange: the session produces a boosted frame.
        startBoosting()
        feedCameraFrames(count = 1)
        router.onOutputFrameAvailable(lastTimestampNs, IDENTITY)

        // Act: camera frames keep arriving for 3 seconds without boosted frames, then for a
        // little longer.
        feedCameraFrames(count = 30)
        val unusableAfterThreeSeconds = host.unusableSessions.toList()
        feedCameraFrames(count = 2)

        // Assert.
        assertThat(unusableAfterThreeSeconds).isEmpty()
        assertThat(host.unusableSessions).contains(session)
    }

    @Test
    fun cameraFramesResumeAfterPause_restartBoostedFrameTracking() {
        // Arrange: the session produces boosted frames.
        startBoosting()
        feedCameraFrames(count = 1)
        router.onOutputFrameAvailable(lastTimestampNs, IDENTITY)

        // Act: the camera stream pauses, e.g. while the app is in the background, then resumes.
        shadowOf(getMainLooper()).idleFor(10, TimeUnit.SECONDS)
        lastTimestampNs += TimeUnit.SECONDS.toNanos(10)
        inputRenderer().renderedSurfaces.clear()
        feedCameraFrames(count = 1)

        // Assert: the session isn't reported unusable, and passthrough frames are shown until a
        // boosted frame arrives.
        assertThat(host.resumedCount).isEqualTo(1)
        assertThat(host.unusableSessions).isEmpty()
        assertThat(inputRenderer().renderedSurfaces).contains(outputSurface)
    }

    @Test
    fun boostDisabled_sessionIsNotReportedUnusableWithoutBoostedFrames() {
        // Arrange.
        host.activeSession = session

        // Act.
        feedCameraFrames(count = 70)

        // Assert.
        assertThat(host.unusableSessions).isEmpty()
        assertThat(inputRenderer().renderedSurfaces).doesNotContain(session.cameraSurfaceForTest)
    }

    @Test
    fun cameraStreamPause_isReportedAsResumed() {
        // Arrange.
        feedCameraFrames(count = 3)

        // Act: the camera stream pauses, then resumes.
        shadowOf(getMainLooper()).idleFor(5, TimeUnit.SECONDS)
        lastTimestampNs += TimeUnit.SECONDS.toNanos(5)
        feedCameraFrames(count = 1)

        // Assert.
        assertThat(host.resumedCount).isEqualTo(1)
    }

    @Test
    fun gapCausedByBlockingCall_isNotReportedAsResumed() {
        // Arrange.
        feedCameraFrames(count = 3)

        // Act: a synchronous Google Play Services call blocks the GL thread, so the camera
        // timestamps jump.
        blockingCalls.call { ShadowSystemClock.advanceBy(5, TimeUnit.SECONDS) }
        lastTimestampNs += TimeUnit.SECONDS.toNanos(5)
        feedCameraFrames(count = 1)

        // Assert.
        assertThat(host.resumedCount).isEqualTo(0)
    }

    @Test
    fun framesDrainedAfterBlockingCall_areNotReportedAsResumed() {
        // Arrange: a synchronous Google Play Services call blocks the GL thread for 5 seconds.
        feedCameraFrames(count = 3)
        blockingCalls.call { ShadowSystemClock.advanceBy(5, TimeUnit.SECONDS) }

        // Act: the frames queued during the call are handled right away, followed by a frame
        // after the ones the camera dropped meanwhile.
        repeat(3) { router.onInputFrameAvailable(nextTimestampNs(), IDENTITY) }
        lastTimestampNs += TimeUnit.SECONDS.toNanos(5)
        router.onInputFrameAvailable(nextTimestampNs(), IDENTITY)
        feedCameraFrames(count = 3)

        // Assert.
        assertThat(host.resumedCount).isEqualTo(0)
    }

    @Test
    fun newCameraStream_isNotReportedAsResumed() {
        // Arrange.
        feedCameraFrames(count = 3)
        shadowOf(getMainLooper()).idleFor(5, TimeUnit.SECONDS)

        // Act: a new camera stream starts, whose timestamps don't follow the previous ones.
        router.resetCameraStreamTimestamps()
        lastTimestampNs += TimeUnit.SECONDS.toNanos(5)
        feedCameraFrames(count = 1)

        // Assert.
        assertThat(host.resumedCount).isEqualTo(0)
    }

    /** Makes [session] the active session while Low Light Boost is enabled. */
    private fun startBoosting() {
        host.isEnabled = true
        host.activeSession = session
    }

    private fun nextTimestampNs(): Long {
        lastTimestampNs += TimeUnit.MILLISECONDS.toNanos(FRAME_INTERVAL_MS)
        return lastTimestampNs
    }

    /** Sends a camera frame every [FRAME_INTERVAL_MS], [count] times. */
    private fun feedCameraFrames(count: Int) {
        repeat(count) {
            shadowOf(getMainLooper()).idleFor(FRAME_INTERVAL_MS, TimeUnit.MILLISECONDS)
            router.onInputFrameAvailable(nextTimestampNs(), IDENTITY)
        }
    }

    private fun inputRenderer(): FakeGlRenderer =
        renderPipeline.inputBridge.glRenderer as FakeGlRenderer

    private fun outputRenderer(): FakeGlRenderer =
        renderPipeline.outputBridge.glRenderer as FakeGlRenderer

    private class FakeHost : LowLightBoostFrameRouter.Host {
        var isEnabled = false
        var resumedCount = 0
        var sessionNeededCount = 0
        val unusableSessions = mutableListOf<LowLightBoostSession>()

        override var activeSession: LowLightBoostSession? = null

        override fun isBoostEnabled(): Boolean = isEnabled

        override fun onCameraStreamResumed() {
            resumedCount++
        }

        override fun onSessionUnusable(session: LowLightBoostSession, errorMessage: String) {
            unusableSessions.add(session)
        }

        override fun onSessionNeeded() {
            sessionNeededCount++
        }
    }

    private companion object {
        private const val OUTPUT_TEXTURE_NAME = 3
        private const val FRAME_INTERVAL_MS = 100L
        private val IDENTITY = FloatArray(16).apply { Matrix.setIdentityM(this, 0) }
    }
}
