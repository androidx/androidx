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

import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.camera.core.Logger
import com.google.android.gms.cameralowlight.LowLightBoostSession
import java.util.concurrent.TimeUnit

/**
 * Routes the frames of [renderPipeline]: sends camera frames to the active Low Light Boost
 * session's camera surface while Low Light Boost is enabled, and renders passthrough frames to the
 * output until the first boosted frame arrives, then boosted frames.
 *
 * It keeps the frame timing state. It reports to [host] when the camera stream resumes after a
 * pause, when the session can't be used anymore, e.g. because it stopped producing boosted frames,
 * and when a session is needed but none is active. It doesn't change the session itself.
 *
 * **Threading:** used on the GL thread only, where the frame callbacks are delivered.
 */
@WorkerThread
internal class LowLightBoostFrameRouter(
    private val renderPipeline: LowLightBoostRenderPipeline,
    private val blockingCalls: BlockingCallTracker,
    private val host: Host,
) {
    /**
     * The session state that frame routing reads, and the events it reports. Events are only
     * reported on API level 30 and higher, where a session can exist.
     */
    internal interface Host {
        /** The active session, or null if none is active. */
        val activeSession: LowLightBoostSession?

        /**
         * Whether the owner of the session, i.e. the effect's surface processor, has Low Light
         * Boost enabled.
         */
        fun isBoostEnabled(): Boolean

        /** Called when the camera stream resumed after a pause. */
        @RequiresApi(Build.VERSION_CODES.R) fun onCameraStreamResumed()

        /** Called when [session] can't be used anymore. */
        @RequiresApi(Build.VERSION_CODES.R)
        fun onSessionUnusable(session: LowLightBoostSession, errorMessage: String)

        /** Called for a camera frame while Low Light Boost is enabled but no session is active. */
        @RequiresApi(Build.VERSION_CODES.R) fun onSessionNeeded()
    }

    // The session that camera frames are being sent to, or null in passthrough mode.
    private var boostFeedSession: LowLightBoostSession? = null

    // When camera frames started to be sent to boostFeedSession, and the latest one was sent.
    private var boostFeedStartTimeMs = 0L

    private var lastFedFrameTimeMs = 0L

    // The camera timestamp of the latest input frame, or NO_TIMESTAMP if none.
    private var lastInputFrameTimestampNs = NO_TIMESTAMP

    // When the latest input frame was handled.
    private var lastInputFrameElapsedMs = 0L

    // When the first and the latest boosted frames of boostFeedSession arrived, or 0 if none.
    private var firstBoostedFrameTimeMs = 0L

    private var lastBoostedFrameTimeMs = 0L

    // Whether the output shows boosted frames. Until the first boosted frame arrives, the output
    // keeps showing passthrough frames.
    private var isShowingBoostedFrames = false

    private var lastPassthroughTimestampNs = Long.MIN_VALUE

    /** Forgets the latest camera timestamp, since the timestamps of a new stream may not follow. */
    internal fun resetCameraStreamTimestamps() {
        lastInputFrameTimestampNs = NO_TIMESTAMP
    }

    /** Called when the session is released, so camera frames are no longer sent to it. */
    internal fun onSessionReleased() {
        boostFeedSession = null
    }

    /**
     * Frame callback invoked when a camera frame arrives on
     * [LowLightBoostRenderPipeline.inputBridge]. Sends the frame to the Low Light Boost session's
     * camera surface when Low Light Boost is active. Otherwise, or until the first boosted frame
     * arrives, renders it directly to the downstream output, and reports that a session is needed
     * if Low Light Boost is enabled.
     *
     * If the session stops producing boosted frames while camera frames are sent to it (see
     * [BOOSTED_FRAME_TIMEOUT_MS]), it is reported as unusable, so the output falls back to
     * passthrough frames instead of freezing.
     */
    internal fun onInputFrameAvailable(timestampNs: Long, textureTransform: FloatArray) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // No LowLightBoostSession can exist below API level 30.
            renderPassthroughFrame(timestampNs, textureTransform)
            return
        }
        if (isCameraStreamResumed(timestampNs)) {
            // The camera stream resumed after a pause, e.g. when the app returns to the
            // foreground.
            host.onCameraStreamResumed()
        }
        handleInputFrame(timestampNs, textureTransform)
        lastInputFrameTimestampNs = timestampNs
        lastInputFrameElapsedMs = SystemClock.elapsedRealtime()
        blockingCalls.onInputFrameHandled()
    }

    /**
     * Returns true if the camera stream resumed after a pause before the frame at [timestampNs].
     *
     * Both the gap between the camera timestamps and the wall-clock time since the previous frame
     * was handled, minus the time the GL thread was blocked by synchronous Google Play Services
     * calls meanwhile, must exceed [MAX_FEED_GAP_MS]. Camera frames queue up, or are dropped, while
     * such a call blocks the GL thread, so a stall isn't treated as a pause of the camera stream,
     * whether the queued frames are drained afterwards or dropped.
     */
    private fun isCameraStreamResumed(timestampNs: Long): Boolean {
        if (
            lastInputFrameTimestampNs == NO_TIMESTAMP ||
                timestampNs - lastInputFrameTimestampNs <= MAX_FEED_GAP_NS
        ) {
            return false
        }
        val unblockedMs =
            SystemClock.elapsedRealtime() -
                lastInputFrameElapsedMs -
                blockingCalls.blockedMsSinceLastFrame
        return unblockedMs > MAX_FEED_GAP_MS
    }

    /** Handles a camera frame on API level 30 and higher. See [onInputFrameAvailable]. */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun handleInputFrame(timestampNs: Long, textureTransform: FloatArray) {
        val now = SystemClock.elapsedRealtime()
        val session = host.activeSession
        val camSurface = session?.let { blockingCalls.cameraSurfaceOf(it) }
        // False (passthrough mode) if there is no owner.
        val isEnabled = host.isBoostEnabled()

        if (isEnabled && session != null && camSurface != null && camSurface.isValid) {
            if (session !== boostFeedSession || now - lastFedFrameTimeMs > MAX_FEED_GAP_MS) {
                // A new session, or the camera stream resumed after a pause, e.g. while the app
                // was in the background. Give the session time to produce a boosted frame, and
                // show passthrough frames until then.
                boostFeedSession = session
                boostFeedStartTimeMs = now
                firstBoostedFrameTimeMs = 0L
                lastBoostedFrameTimeMs = 0L
                isShowingBoostedFrames = false
            }
            lastFedFrameTimeMs = now
            if (!renderPipeline.isSessionCameraSurfaceBound(camSurface)) {
                renderPipeline.bindSessionCameraSurfaceToInputBridge(camSurface)
            }
            renderPipeline.inputBridge.render(timestampNs, textureTransform, camSurface)
            if (!isShowingBoostedFrames) {
                renderPassthroughFrame(timestampNs, textureTransform)
            }
            if (isBoostedFrameOverdue(now)) {
                Logger.w(TAG, "No boosted frame received; destroying the session for recovery")
                host.onSessionUnusable(session, "LowLightBoostSession stopped producing frames")
            }
        } else {
            boostFeedSession = null
            renderPassthroughFrame(timestampNs, textureTransform)
            if (isEnabled) {
                if (session != null) {
                    Logger.w(
                        TAG,
                        "Active LowLightBoostSession camera surface is invalid; destroying " +
                            "session for recovery",
                    )
                    // This also triggers recovery.
                    host.onSessionUnusable(
                        session,
                        "LowLightBoostSession camera surface is invalid",
                    )
                } else {
                    host.onSessionNeeded()
                }
            }
        }
    }

    /**
     * Returns true if the session that has been receiving camera frames since
     * [boostFeedStartTimeMs] hasn't produced a boosted frame for [BOOSTED_FRAME_TIMEOUT_MS], so it
     * is treated as dead and the output doesn't freeze. The first boosted frame can take longer,
     * e.g. while the session warms up.
     */
    private fun isBoostedFrameOverdue(nowMs: Long): Boolean {
        val elapsedMs =
            if (lastBoostedFrameTimeMs == 0L) {
                nowMs - boostFeedStartTimeMs - FIRST_BOOSTED_FRAME_EXTRA_TIMEOUT_MS
            } else {
                nowMs - lastBoostedFrameTimeMs
            }
        return elapsedMs > BOOSTED_FRAME_TIMEOUT_MS
    }

    /** Renders a passthrough frame to the output, which stops showing boosted frames. */
    private fun renderPassthroughFrame(timestampNs: Long, textureTransform: FloatArray) {
        isShowingBoostedFrames = false
        lastPassthroughTimestampNs = timestampNs
        renderPipeline.renderPassthroughFrameToOutputs(timestampNs, textureTransform)
    }

    /**
     * Frame callback invoked when a boosted frame arrives on
     * [LowLightBoostRenderPipeline.outputBridge]. Renders it to the downstream output if Low Light
     * Boost is enabled and camera frames are being sent to the active session.
     *
     * The output keeps showing passthrough frames until the first boosted frame arrives. Boosted
     * frames of camera frames already shown in passthrough mode are dropped (for up to
     * [BOOSTED_FRAME_CATCH_UP_TIMEOUT_MS]), so frame timestamps don't go back.
     */
    internal fun onOutputFrameAvailable(timestampNs: Long, textureTransform: FloatArray) {
        val session = host.activeSession
        if (!host.isBoostEnabled() || session == null) {
            return
        }
        if (session !== boostFeedSession) {
            // A late frame from before camera frames were sent to this session.
            return
        }
        val now = SystemClock.elapsedRealtime()
        if (firstBoostedFrameTimeMs == 0L) {
            firstBoostedFrameTimeMs = now
        }
        lastBoostedFrameTimeMs = now
        if (!isShowingBoostedFrames) {
            // Stop showing passthrough frames from now on.
            Logger.d(
                TAG,
                "Switching the output to boosted frames after ${now - boostFeedStartTimeMs}ms " +
                    "(timestampNs=$timestampNs, lastPassthroughTimestampNs=" +
                    "$lastPassthroughTimestampNs)",
            )
            isShowingBoostedFrames = true
        }
        // Boosted frames lag behind camera frames, so drop the ones whose camera frames were
        // already shown in passthrough mode. Only do it for a short time, in case the session
        // doesn't keep the camera timestamps.
        if (
            timestampNs <= lastPassthroughTimestampNs &&
                now - firstBoostedFrameTimeMs < BOOSTED_FRAME_CATCH_UP_TIMEOUT_MS
        ) {
            return
        }
        renderPipeline.renderBoostedFrameToOutputs(timestampNs, textureTransform)
    }

    private companion object {
        private const val TAG = "LlbFrameRouter"

        // The timing values below are tuned for Google Play Services Low Light Boost behavior
        // (b/448524973) and can be revisited when the service changes.

        /**
         * How long the session can go without producing a boosted frame, while camera frames are
         * sent to it, before it is treated as dead.
         */
        private const val BOOSTED_FRAME_TIMEOUT_MS = 3000L

        /** Extra time allowed for the first boosted frame of a session, e.g. for its warm-up. */
        private const val FIRST_BOOSTED_FRAME_EXTRA_TIMEOUT_MS = 3000L

        /**
         * The longest gap between camera frames that isn't treated as a pause of the camera stream.
         * Used in two ways:
         * - A wall-clock gap between camera frames sent to the session restarts the boosted frame
         *   tracking, so a pause isn't treated as a session that stopped producing frames.
         * - A gap between camera frame timestamps, together with a wall-clock gap that isn't
         *   explained by blocking Google Play Services calls, is reported as a resumed camera
         *   stream, which allows a new creation round after a failed one and resets the recovery
         *   cap.
         */
        private const val MAX_FEED_GAP_MS = 1000L
        private val MAX_FEED_GAP_NS = TimeUnit.MILLISECONDS.toNanos(MAX_FEED_GAP_MS)

        /** The value of [lastInputFrameTimestampNs] before the first input frame. */
        private const val NO_TIMESTAMP = Long.MIN_VALUE

        /**
         * How long boosted frames of camera frames already shown in passthrough mode are dropped
         * after the first boosted frame arrives.
         */
        private const val BOOSTED_FRAME_CATCH_UP_TIMEOUT_MS = 1000L
    }
}
