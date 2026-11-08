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

package androidx.xr.scenecore.releasetests

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.xr.testutils.XrDeviceTest
import java.util.regex.Pattern
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end tests verifying glTF model loading, animation playback, and material/texture overrides
 * in the R8-optimized release build of `:xr:scenecore:integration-tests:xr-integration-tests`.
 *
 * Each test launches a scenario activity in the target app, which runs the scenario on launch and
 * reports the outcome as `SUCCESS` or `FAILURE: <error>` in its UI. Running against the minified
 * release APK ensures library keep rules (such as for the AssetLoader and AssetAnimator JNI
 * bindings) are properly preserved.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@XrDeviceTest
class GltfModelE2ETest {

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.pressHome()
        device.waitForIdle()
        // Clear the crash buffer so a failure only reports crashes from this test.
        device.executeShellCommand("logcat -b crash -c")
    }

    @Test
    @XrDeviceTest
    fun gltfAnimation_loadModelCreateEntityAndPlay_succeeds() {
        runScenario("GltfAnimationScenarioActivity")
    }

    @Test
    @XrDeviceTest
    fun gltfMaterialTexture_applyTexturedMaterialOverride_succeeds() {
        runScenario("GltfMaterialTextureScenarioActivity")
    }

    /**
     * Launches the scenario [activity] in the target app and asserts it reports success.
     *
     * Fails as soon as the scenario reports a failure or the target app process dies (e.g. due to a
     * native crash from a missing keep rule), rather than waiting for [TIMEOUT_MS].
     */
    private fun runScenario(activity: String) {
        // -S force-stops any previous instance; -W waits for the launch so the process is running
        // (or has already crashed) once this returns.
        device.executeShellCommand("am start -S -W -n $TARGET_PACKAGE/$SCENARIO_PACKAGE.$activity")

        val deadline = SystemClock.uptimeMillis() + TIMEOUT_MS
        while (SystemClock.uptimeMillis() < deadline) {
            val status = device.findObject(By.pkg(TARGET_PACKAGE).text(RESULT_PATTERN))
            if (status != null) {
                check(status.text == STATUS_SUCCESS) { "$activity failed: ${status.text}" }
                return
            }
            check(isTargetRunning()) { "Target app crashed while running $activity\n${crashLog()}" }
            SystemClock.sleep(POLL_INTERVAL_MS)
        }
        throw IllegalStateException("$activity did not report a result within ${TIMEOUT_MS}ms")
    }

    private fun isTargetRunning(): Boolean =
        device.executeShellCommand("pidof $TARGET_PACKAGE").isNotBlank()

    /** Returns the most relevant lines of the crash log buffer, e.g. the abort message. */
    private fun crashLog(): String =
        device
            .executeShellCommand("logcat -d -b crash")
            .lineSequence()
            .filter { line -> CRASH_LOG_MARKERS.any { line.contains(it) } }
            .take(MAX_CRASH_LOG_LINES)
            .joinToString("\n")

    companion object {
        private const val TARGET_PACKAGE = "androidx.xr.scenecore.integration.tests"
        private const val SCENARIO_PACKAGE = "$TARGET_PACKAGE.e2e"
        private const val STATUS_SUCCESS = "SUCCESS"
        private val RESULT_PATTERN = Pattern.compile("^(SUCCESS|FAILURE).*", Pattern.DOTALL)
        // Generous to accommodate slow model loading on loaded CI devices; crashes and results
        // are detected by polling, so this only affects hanging runs.
        private const val TIMEOUT_MS = 30_000L
        private const val POLL_INTERVAL_MS = 250L
        private const val MAX_CRASH_LOG_LINES = 20
        private val CRASH_LOG_MARKERS =
            listOf("Abort message", "FATAL EXCEPTION", "AndroidRuntime", "Process: $TARGET_PACKAGE")
    }
}
