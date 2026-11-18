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

package androidx.xr.testutils

import android.os.Looper
import androidx.annotation.VisibleForTesting
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Enforces bounded CPM attached node, buffer pixel, and living node deltas after test teardown.
 *
 * Intended specifically for dedicated resource-reclamation tests rather than blanket attachment to
 * every functional integration test. Triggers GC after the wrapped statement (and any inner
 * session/activity teardown rules) finishes and polls [SnapshotXrScene.stats] until
 * [SpatialSceneStats.attachedNodes], [SpatialSceneStats.bufferPixels], and (when present in the
 * baseline) [SpatialSceneStats.livingNodes] settle within budget.
 *
 * `attachedNodes` and `bufferPixels` are derived from the parsed node tree and work identically
 * across text and binary `--proto` scene captures.
 *
 * Note: Declare [XrResourceLeakRule] *inside* `XrSessionRule` (`@get:Rule(order = 0)` on
 * `XrSessionRule`, `@get:Rule(order = 1)` on [XrResourceLeakRule]) or scope entity create/dispose
 * cycles with [assertWithinBudget], because `ActivityScenario.close()` leaves hidden
 * `space-root-task-<id>` entries in WindowManager Recents until instrumentation process exit. Also
 * note that `PanelEntity` holds an internal `SurfaceControl.Transaction.setMirrorCPMNode` reference
 * released via `Node.finalize()`, which retains 1 empty CPM node per `PanelEntity` (with
 * `bufferPixels == 0`) until process exit on some platform builds; allow headroom via
 * [maxLivingNodesDelta] when wrapping scoped `PanelEntity` lifecycles. Failures thrown by the test
 * body itself are never masked; leak verification only runs when the test body completes normally.
 *
 * @param maxAttachedNodesDelta maximum permitted increase in [SpatialSceneStats.attachedNodes]
 * @param maxBufferPixelsDelta maximum permitted increase in [SpatialSceneStats.bufferPixels]
 * @param maxLivingNodesDelta maximum permitted increase in `Number of living nodes` when
 *   [SpatialSceneStats.livingNodes] is present in the baseline snapshot
 * @param settleTimeoutMs maximum time in milliseconds to wait for GC and compositor teardown
 * @param pollIntervalMs poll interval in milliseconds while awaiting reclamation
 */
public class XrResourceLeakRule
@VisibleForTesting
internal constructor(
    private val maxAttachedNodesDelta: Int,
    private val maxBufferPixelsDelta: Long,
    private val maxLivingNodesDelta: Int,
    private val settleTimeoutMs: Long,
    private val pollIntervalMs: Long,
    private val sceneProvider: () -> SnapshotXrScene,
    private val gcTrigger: () -> Unit,
    private val sleeper: (Long) -> Unit,
    private val clockMs: () -> Long,
) : TestRule {

    @JvmOverloads
    public constructor(
        maxAttachedNodesDelta: Int = 0,
        maxBufferPixelsDelta: Long = 0L,
        maxLivingNodesDelta: Int = 0,
        settleTimeoutMs: Long = 2000L,
        pollIntervalMs: Long = 100L,
    ) : this(
        maxAttachedNodesDelta = maxAttachedNodesDelta,
        maxBufferPixelsDelta = maxBufferPixelsDelta,
        maxLivingNodesDelta = maxLivingNodesDelta,
        settleTimeoutMs = settleTimeoutMs,
        pollIntervalMs = pollIntervalMs,
        sceneProvider = { captureSpatialScene() },
        gcTrigger = ::defaultLeakGcTrigger,
        sleeper = ::sleep,
        clockMs = ::monotonicClockMs,
    )

    init {
        require(maxAttachedNodesDelta >= 0) {
            "maxAttachedNodesDelta must be non-negative, was $maxAttachedNodesDelta."
        }
        require(maxBufferPixelsDelta >= 0L) {
            "maxBufferPixelsDelta must be non-negative, was $maxBufferPixelsDelta."
        }
        require(maxLivingNodesDelta >= 0) {
            "maxLivingNodesDelta must be non-negative, was $maxLivingNodesDelta."
        }
        require(settleTimeoutMs >= 0L) {
            "settleTimeoutMs must be non-negative, was $settleTimeoutMs."
        }
        require(pollIntervalMs >= 0L) {
            "pollIntervalMs must be non-negative, was $pollIntervalMs."
        }
    }

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                val baseline = sceneProvider().stats
                base.evaluate()
                verifyWithinBudget(baseline, description.displayName)
            }
        }

    /** Executes [block] and asserts that CPM node and buffer counts stay within budget. */
    public fun <T> assertWithinBudget(block: () -> T): T {
        val baseline = sceneProvider().stats
        val result = block()
        verifyWithinBudget(baseline, "assertWithinBudget")
        return result
    }

    private fun verifyWithinBudget(baseline: SpatialSceneStats?, testName: String) {
        if (baseline == null) {
            throw AssertionError("Missing baseline CPM scene stats for $testName.")
        }

        gcTrigger()
        val startMs = clockMs()
        var secondGcTriggered = false

        while (true) {
            val current = sceneProvider().stats
            val statsComparable =
                current != null && (baseline.livingNodes == null || current.livingNodes != null)
            val attachedDelta =
                if (current != null) current.attachedNodes - baseline.attachedNodes else 0
            val pixelsDelta =
                if (current != null) current.bufferPixels - baseline.bufferPixels else 0L
            val livingDelta =
                if (baseline.livingNodes != null && current?.livingNodes != null) {
                    current.livingNodes - baseline.livingNodes
                } else {
                    0
                }

            if (
                statsComparable &&
                    attachedDelta <= maxAttachedNodesDelta &&
                    pixelsDelta <= maxBufferPixelsDelta &&
                    livingDelta <= maxLivingNodesDelta
            ) {
                return
            }

            val elapsedMs = clockMs() - startMs
            if (elapsedMs >= settleTimeoutMs) {
                if (!statsComparable) {
                    throw AssertionError(
                        "CPM resource leak check timed out with missing telemetry after " +
                            "$testName: attachedNodes ${baseline.attachedNodes} -> " +
                            "${current?.attachedNodes}, bufferPixels ${baseline.bufferPixels} -> " +
                            "${current?.bufferPixels}, livingNodes ${baseline.livingNodes} -> " +
                            "${current?.livingNodes}."
                    )
                }
                throw AssertionError(
                    "CPM resource leak detected after $testName: " +
                        "attachedNodes ${baseline.attachedNodes} -> ${current?.attachedNodes} " +
                        "(delta=$attachedDelta, max=$maxAttachedNodesDelta), " +
                        "bufferPixels ${baseline.bufferPixels} -> ${current?.bufferPixels} " +
                        "(delta=$pixelsDelta, max=$maxBufferPixelsDelta), " +
                        "livingNodes ${baseline.livingNodes} -> ${current?.livingNodes} " +
                        "(delta=$livingDelta, max=$maxLivingNodesDelta)."
                )
            }

            if (!secondGcTriggered && elapsedMs >= settleTimeoutMs / 2L) {
                secondGcTriggered = true
                gcTrigger()
            }
            if (pollIntervalMs > 0L) {
                sleeper(pollIntervalMs)
            }
        }
    }

    private companion object {
        private fun drainMainLooperIfPossible() {
            try {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                }
            } catch (_: IllegalStateException) {
                // Not running under Android Instrumentation (for example host JVM unit tests).
            }
        }

        private fun defaultLeakGcTrigger() {
            drainMainLooperIfPossible()
            repeat(4) {
                @Suppress("ExplicitGarbageCollectionCall") Runtime.getRuntime().gc()
                System.runFinalization()
                sleep(25L)
                drainMainLooperIfPossible()
            }
            @Suppress("ExplicitGarbageCollectionCall") Runtime.getRuntime().gc()
            System.runFinalization()
        }
    }
}
