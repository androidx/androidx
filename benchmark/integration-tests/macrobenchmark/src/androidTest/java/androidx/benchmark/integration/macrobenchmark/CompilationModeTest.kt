/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.benchmark.integration.macrobenchmark

import android.content.Intent
import androidx.benchmark.Shell
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMacrobenchmarkApi
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.testutils.getStartupMetrics
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

/**
 *
 */
@LargeTest
@SdkSuppress(minSdkVersion = 29)
@OptIn(ExperimentalMacrobenchmarkApi::class)
class CompilationModeTest {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun compilationModeFull_shouldSetProfileSpeed() {
        // Perform a compilation profile reset and then compile to `speed`.
        runWithCompilationMode(CompilationMode.Full())

        // Expected output should be speed
        assertThat(getCompilationMode()).isEqualTo("speed")
    }

    @Test
    fun compilationModeNone_shouldSetProfileVerified() {
        // Perform a compilation profile reset and then compile to `speed`.
        runWithCompilationMode(CompilationMode.Full())

        // Perform a compilation profile reset only.
        runWithCompilationMode(CompilationMode.None())

        // Expected output should be the default after reset
        assertThat(getCompilationMode()).isEqualTo("verify")
    }

    @Test
    fun compilationModeNoop_shouldNotPerformAnyReset() {
        // Perform a compilation profile reset and then compile to `speed`.
        runWithCompilationMode(CompilationMode.Full())

        // Don't perform any compilation profile reset
        runWithCompilationMode(CompilationMode.Ignore())

        // Expected output should still be speed
        assertThat(getCompilationMode()).isEqualTo("speed")
    }

    private fun runWithCompilationMode(compilationMode: CompilationMode) =
        benchmarkRule.measureRepeated(
            compilationMode = compilationMode,
            packageName = TARGET_PACKAGE_NAME,
            metrics = getStartupMetrics(),
            startupMode = StartupMode.COLD,
            iterations = 1,
        ) {
            startActivityAndWait(Intent(TARGET_ACTION))
            device.waitForIdle()
            check(device.wait(Until.hasObject(By.text(EXPECTED_TEXT)), 3000))
        }

    private fun getCompilationMode(): String {
        val dump =
            Shell.executeScriptWithStderr("cmd package dump $TARGET_PACKAGE_NAME").stdout.trim()

        // Find `Dexopt state:` line
        var firstMarkerFound = false
        for (line in dump.lines()) {

            // Looks for first marker
            if (!firstMarkerFound && line.trim() == FIRST_MARKER) {
                firstMarkerFound = true
                continue
            }

            // Looks for second marker
            if (firstMarkerFound && line.trim().contains(SECOND_MARKER)) {
                return line.substringAfter(SECOND_MARKER).substringBefore("]")
            }
        }

        return COMPILATION_PROFILE_UNKNOWN
    }

    companion object {

        // Intent
        private const val TARGET_PACKAGE_NAME =
            "androidx.benchmark.integration.macrobenchmark.target"
        private const val TARGET_ACTION =
            "$TARGET_PACKAGE_NAME.TRIVIAL_STARTUP_FULLY_DRAWN_ACTIVITY"

        // Screen assert
        private const val EXPECTED_TEXT = "FULL DISPLAY"

        // Compilation mode
        private const val FIRST_MARKER = "Dexopt state:"
        private const val SECOND_MARKER = "[status="
        private const val COMPILATION_PROFILE_UNKNOWN = "unknown"
    }
}
