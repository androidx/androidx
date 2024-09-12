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

package androidx.benchmark.macro.junit4

import android.content.Intent
import android.os.Build
import androidx.benchmark.Shell
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMacrobenchmarkApi
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

@LargeTest
@SdkSuppress(minSdkVersion = 29)
@OptIn(ExperimentalMacrobenchmarkApi::class)
class CompilationModeTest {

    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun compilationModeFull_shouldSetProfileSpeed() {
        // Perform a compilation profile reset and then compile to `speed`.
        runWithCompilationMode(CompilationMode.Full())

        // Expected output should be speed
        assertThat(getCompilationMode()).isEqualTo("speed")
    }

    /**
     * Validate that compilation can be reset.
     *
     * Note, this test is skipped on rooted API 29/30, since "cmd package dump" result fails to
     * update compilation state correctly. Repro:
     * ```
     * adb shell cmd package compile -m speed androidx.benchmark.integration.macrobenchmark.target
     * adb shell cmd package compile --reset androidx.benchmark.integration.macrobenchmark.target
     * adb shell cmd package dump androidx.benchmark.integration.macrobenchmark.target | grep -A3 Dexopt
     * ```
     *
     * Which prints speed, though it should be 'verify':
     * ```
     * Dexopt state:
     *   [androidx.benchmark.integration.macrobenchmark.target]
     *     path: /data/app/~~Jn8HSPcIEU6RRXJknWt2ZA==/androidx.benchmark.integration.macrobenchmark.target-CM7MqDZo6wGLb-7Auxqv8g==/base.apk
     *       arm64: [status=speed] [reason=unknown]
     * ```
     */
    @Test
    fun compilationModeNone_shouldSetProfileVerified() {
        assumeTrue(Build.VERSION.SDK_INT >= 31 || !Shell.isSessionRooted())

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
            metrics = listOf(StartupTimingMetric()),
            startupMode = StartupMode.COLD,
            iterations = 1,
        ) {
            startActivityAndWait(Intent(TARGET_ACTION))
            device.waitForIdle()
            check(device.wait(Until.hasObject(By.text(EXPECTED_TEXT)), 3000))
        }

    private fun getCompilationMode() = Shell.getCompilationMode(TARGET_PACKAGE_NAME)

    @SmallTest
    @Test
    fun compileResetErrorString() {
        assertEquals(
            expected = "Unable to reset compilation of pkg (out=out).",
            actual =
                CompilationMode.compileResetErrorString(
                    packageName = "pkg",
                    output = "out",
                    isEmulator = true
                )
        )
        assertEquals(
            expected = "Unable to reset compilation of pkg (out=pkg could not be compiled).",
            actual =
                CompilationMode.compileResetErrorString(
                    packageName = "pkg",
                    output = "pkg could not be compiled",
                    isEmulator = false
                )
        )
        // verbose message requires emulator + specific "could not be compiled" output from --reset
        assertEquals(
            expected =
                "Unable to reset compilation of pkg (out=pkg could not be compiled)." +
                    " Try updating your emulator - see" +
                    " https://issuetracker.google.com/issue?id=251540646",
            actual =
                CompilationMode.compileResetErrorString(
                    packageName = "pkg",
                    output = "pkg could not be compiled",
                    isEmulator = true
                )
        )
    }

    companion object {

        // Intent
        private const val TARGET_PACKAGE_NAME =
            "androidx.benchmark.integration.macrobenchmark.target"
        private const val TARGET_ACTION =
            "$TARGET_PACKAGE_NAME.TRIVIAL_STARTUP_FULLY_DRAWN_ACTIVITY"

        // Screen assert
        private const val EXPECTED_TEXT = "FULL DISPLAY"
    }
}
