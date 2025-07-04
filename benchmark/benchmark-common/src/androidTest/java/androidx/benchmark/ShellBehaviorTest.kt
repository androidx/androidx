/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.benchmark

import android.os.Build
import android.os.Process
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This class collects tests of strange shell behavior, for the purpose of documenting and
 * validating how general the problems are. For this reason, these tests call directly shell
 * commands without referring to the actual implementations in [Shell]. To test the [Shell]
 * implementations, please add to [ShellTest].
 */
@MediumTest
@SdkSuppress(minSdkVersion = 21)
@RunWith(AndroidJUnit4::class)
class ShellBehaviorTest {

    /**
     * Test validates consistent behavior of pgrep, for usage in discovering processes without
     * needing to check stderr
     */
    @Test
    fun pgrepLFExecuteScript() {

        val apiSpecificArgs =
            setOfNotNull(
                    // aosp/3507001 -> needed to print full command line (so full package name)
                    if (Build.VERSION.SDK_INT >= 36) "-a" else null
                )
                .joinToString(" ")

        // Should only be one process - this one!
        val pgrepOutput =
            Shell.executeScriptCaptureStdoutStderr("pgrep -l -f $apiSpecificArgs ${Packages.TEST}")

        if (Build.VERSION.SDK_INT >= 23) {
            // API 23 has trailing whitespace after the package name for some reason :shrug:
            val regex = "^\\d+ ${Packages.TEST.replace(".", "\\.")}\\s*$".toRegex()
            assertTrue(
                // For some reason, `stdout.contains(regex)` doesn't work :shrug:
                pgrepOutput.stdout.lines().any { it.matches(regex) },
                "expected $regex to be contained in output:\n${pgrepOutput.stdout}",
            )
        } else {
            // command doesn't exist
            assertEquals("", pgrepOutput.stdout)
            assertTrue(pgrepOutput.stderr.isNotBlank())
        }
    }

    @Test
    fun pidof() {
        // Should only be one process - this one!
        val output = Shell.executeScriptCaptureStdoutStderr("pidof ${Packages.TEST}")
        val pidofString = output.stdout.trim()

        when {
            Build.VERSION.SDK_INT < 23 -> {
                // command doesn't exist
                assertTrue(
                    output.stdout.isBlank() && output.stderr.isNotBlank(),
                    "saw output $output",
                )
            }
            Build.VERSION.SDK_INT == 23 -> {
                // on API 23 specifically, pidof prints... all processes, ignoring the arg...
                assertTrue(pidofString.contains(" "))
            }
            else -> {
                assertNotNull(pidofString.toLongOrNull(), "Error, can't parse $pidofString")
            }
        }
    }

    @Test
    fun psDashA() {
        val output = Shell.executeScriptCaptureStdout("ps -A").trim()
        when {
            Build.VERSION.SDK_INT <= 23 -> {
                // doesn't correctly handle -A, sometimes sees nothing, sometimes only this process
                val processes = output.lines()
                assertTrue(processes.size <= 2)
                assertTrue(processes.first().matches(psLabelRowRegex))
                if (processes.size > 1) {
                    assertTrue(processes.last().endsWith(Packages.TEST))
                }
            }
            Build.VERSION.SDK_INT in 24..25 -> {
                // still doesn't support, but useful error at least
                assertEquals("bad pid '-A'", output)
            }
            else -> {
                // ps -A should work - expect several processes including this one
                val processes = output.lines()
                assertTrue(processes.size > 5)
                assertTrue(processes.first().matches(psLabelRowRegex))
                assertTrue(processes.any { it.endsWith(Packages.TEST) })
            }
        }
    }

    /**
     * Test validates consistent behavior of ps, for usage in checking process is alive without
     * needing to check stderr
     */
    @Test
    fun ps() {
        val output = Shell.executeScriptCaptureStdout("ps ${Process.myPid()}").trim()
        // ps should work - expect several processes including this one
        val lines = output.lines()
        assertEquals(2, lines.size)
        assertTrue(lines.first().matches(psLabelRowRegex))
        assertTrue(lines.last().endsWith(Packages.TEST))
    }

    companion object {
        /**
         * Regex for matching ps output label row
         *
         * Note that `ps` output changes over time, e.g.:
         * * API 23 - `USER\s+PID\s+PPID\s+VSIZE\s+RSS\s+WCHAN\s+PC\s+NAME`
         * * API 33 - `USER\s+PID\s+PPID\s+VSZ\s+RSS\s+WCHAN\s+ADDR\s+S\s+NAME\s`
         */
        val psLabelRowRegex = Regex("USER\\s+PID.+NAME\\s*")
    }
}
