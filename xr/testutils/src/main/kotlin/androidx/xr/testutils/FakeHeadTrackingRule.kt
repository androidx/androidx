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

import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.runtime.math.Pose
import org.junit.Assume
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/** System property that enables CPM fake head tracking at SpaceFlinger startup. */
internal const val PROP_FAKE_HEAD_TRACKING: String = "cpm.fake_headtracking"

/** Persistent fallback system property for CPM fake head tracking. */
internal const val PROP_PERSIST_FAKE_HEAD_TRACKING: String = "persist.cpm.fake_headtracking"

/**
 * [TestRule] that simulates the user's 6DoF head pose through CPM fake head tracking.
 *
 * Install the rule in any test class that moves the simulated head, and drive the head with
 * [performHeadMovement]:
 * ```kotlin
 * @get:Rule val fakeHeadTrackingRule = FakeHeadTrackingRule()
 *
 * @Test
 * @XrDeviceTest
 * fun headMovement_updatesPose() {
 *     fakeHeadTrackingRule.performHeadMovement { moveHeadTo(Vector3(0.5f, 0f, 0f)) }
 * }
 * ```
 *
 * For every test it guards, the rule:
 * * Skips the test via [Assume.assumeTrue] when CPM fake head tracking is not enabled.
 * * Resets the simulated head pose to [Pose.Identity] before the test body and again afterwards, so
 *   pose overrides never leak across tests.
 *
 * Enabling fake head tracking requires `adbd` running as root and a restart of the XR framework, so
 * it cannot be done from within the test process. Test modules that use this rule should apply
 * `xr/testutils/xr-device-tests.gradle` in their `build.gradle`, which enables it automatically
 * during Gradle `connectedAndroidTest` runs and restores the device afterwards:
 * ```
 * apply from: "$rootDir/xr/testutils/xr-device-tests.gradle"
 * ```
 *
 * Otherwise, enable it manually before running the tests:
 * ```
 * adb root
 * adb shell "setprop cpm.fake_headtracking true && stop && start"
 * ```
 *
 * When pairing this rule with a rule that launches an activity (such as `ActivityScenarioRule` or
 * `createAndroidComposeRule`), give it a lower `order` (for example `@get:Rule(order = 0)`) so that
 * unconfigured devices skip before the activity launches and the activity starts at the identity
 * head pose.
 *
 * @param settleDelayMs milliseconds to wait after each standalone pose change, completed
 *   [HeadMovementScope.animate], and head pose reset (default: [DEFAULT_HEAD_SETTLE_DELAY_MS])
 * @throws IllegalArgumentException if [settleDelayMs] is negative
 */
public class FakeHeadTrackingRule
@VisibleForTesting
internal constructor(
    private val settleDelayMs: Long,
    private val shellExecutor: (String) -> String,
    private val sleeper: (Long) -> Unit = {},
    private val clockMs: () -> Long = ::monotonicClockMs,
) : TestRule {

    @Volatile private var isActive: Boolean = false

    init {
        require(settleDelayMs >= 0L) { "settleDelayMs must be non-negative, was $settleDelayMs." }
    }

    @JvmOverloads
    public constructor(
        settleDelayMs: Long = DEFAULT_HEAD_SETTLE_DELAY_MS
    ) : this(settleDelayMs, ::instrumentationShellExecutor, ::sleep)

    /**
     * Performs a series of simulated head movements within a scoped [HeadMovementScope].
     *
     * Blocks the calling thread while shell commands and animations run, so call it from the
     * instrumentation thread or a background dispatcher rather than the main thread.
     *
     * [HeadMovementScope.currentPose] starts from the device's active simulated head pose, so
     * consecutive calls within a test build on each other. The final pose is retained when [block]
     * completes unless [resetOnClose] is `true`; the rule resets it after the test regardless.
     *
     * @param resetOnClose when `true`, resets the simulated head pose to [Pose.Identity] when
     *   [block] completes or throws (default: `false`)
     * @param block head movement lambda run in [HeadMovementScope]
     * @throws IllegalStateException if called outside a test guarded by this rule
     */
    @WorkerThread
    @JvmOverloads
    public fun performHeadMovement(
        resetOnClose: Boolean = false,
        block: HeadMovementScope.() -> Unit,
    ) {
        checkActive("performHeadMovement")
        performHeadMovementInternal(
            resetOnClose = resetOnClose,
            settleDelayMs = settleDelayMs,
            shellExecutor = shellExecutor,
            sleeper = sleeper,
            clockMs = clockMs,
            block = block,
        )
    }

    /**
     * Resets the simulated head pose to [Pose.Identity] (`(0, 0, 0)` position and `(0, 0, 0, 1)`
     * orientation).
     *
     * @throws IllegalStateException if called outside a test guarded by this rule
     */
    @WorkerThread
    public fun resetHeadMovement() {
        checkActive("resetHeadMovement")
        resetHeadMovementInternal(settleDelayMs, shellExecutor, sleeper)
    }

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                val status = checkFakeHeadTrackingStatus(shellExecutor)
                Assume.assumeTrue(status.diagnosticMessage, status.isEnabled)

                try {
                    resetHeadMovementInternal(settleDelayMs, shellExecutor, sleeper)
                } catch (e: Exception) {
                    throw IllegalStateException(
                        "Fake head tracking is enabled, but '$PROP_FAKE_HET_POSITION' and " +
                            "'$PROP_FAKE_HET_ORIENTATION' cannot be written; adbd may no " +
                            "longer run as root (for example after a reboot). Apply " +
                            "'xr/testutils/xr-device-tests.gradle' and run via Gradle " +
                            "'connectedAndroidTest', or run 'adb root' first.",
                        e,
                    )
                }
                isActive = true
                try {
                    base.evaluate()
                } catch (t: Throwable) {
                    // Preserve the original test failure if the cleanup shell command also fails.
                    runCatching { resetHeadMovementInternal(settleDelayMs, shellExecutor, sleeper) }
                        .exceptionOrNull()
                        ?.let(t::addSuppressed)
                    throw t
                } finally {
                    isActive = false
                }
                resetHeadMovementInternal(settleDelayMs, shellExecutor, sleeper)
            }
        }

    private fun checkActive(methodName: String) {
        check(isActive) {
            "$methodName() must be called while a test guarded by FakeHeadTrackingRule is " +
                "running; declare the rule with @get:Rule in the test class."
        }
    }
}

/**
 * Runs [command] through the current instrumentation's [android.app.UiAutomation], resolved per
 * call so that constructing [FakeHeadTrackingRule] does not require an instrumentation.
 */
private fun instrumentationShellExecutor(command: String): String =
    runShellCommandOrThrow(command, InstrumentationRegistry.getInstrumentation().uiAutomation)

internal data class FakeHeadTrackingStatus(
    val isEnabled: Boolean,
    val diagnosticMessage: String,
)

/**
 * Checks whether CPM fake head tracking simulation is active on the connected device.
 *
 * Checks the runtime `cpm.fake_headtracking` system property, falling back to
 * `persist.cpm.fake_headtracking` when the runtime value is unset.
 */
internal fun checkFakeHeadTrackingStatus(
    shellExecutor: (String) -> String
): FakeHeadTrackingStatus {
    val fht = readSystemProperty(shellExecutor, PROP_FAKE_HEAD_TRACKING)
    val pfht =
        if (fht.isEmpty()) {
            readSystemProperty(shellExecutor, PROP_PERSIST_FAKE_HEAD_TRACKING)
        } else {
            ""
        }
    val effectiveFakeHt = fht.ifEmpty { pfht }

    val fakeHtActive = effectiveFakeHt.equals("true", ignoreCase = true) || effectiveFakeHt == "1"
    if (!fakeHtActive) {
        return FakeHeadTrackingStatus(
            isEnabled = false,
            diagnosticMessage =
                "Skipping test guarded by FakeHeadTrackingRule: '$PROP_FAKE_HEAD_TRACKING' is " +
                    "not enabled (cpm='$fht', persist='$pfht'). Apply " +
                    "'xr/testutils/xr-device-tests.gradle' in this test module's build.gradle " +
                    "so Gradle 'connectedAndroidTest' enables fake head tracking automatically, " +
                    "or run 'adb root' and " +
                    "'adb shell \"setprop $PROP_FAKE_HEAD_TRACKING true && stop && start\"' first.",
        )
    }

    return FakeHeadTrackingStatus(
        isEnabled = true,
        diagnosticMessage = "Fake head tracking is active.",
    )
}
