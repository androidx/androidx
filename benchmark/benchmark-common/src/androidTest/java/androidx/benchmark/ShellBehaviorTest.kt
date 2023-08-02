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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * This class collects tests of strange shell behavior, for the purpose of documenting
 * and validating how general the problems are.
 */
@MediumTest
@SdkSuppress(minSdkVersion = 21)
@RunWith(AndroidJUnit4::class)
class ShellBehaviorTest {
    @Test
    fun pgrepLF() {
        // Should only be one process - this one!
        val pgrepString = Shell.executeCommand("pgrep -l -f ${Packages.TEST}").trim()

        if (Build.VERSION.SDK_INT >= 23) {
            assertTrue(pgrepString.endsWith(" ${Packages.TEST}"))
        } else {
            // command doesn't exist (and we don't try and read stderr here)
            assertEquals("", pgrepString)
        }
    }

    @Test
    fun pidof() {
        // Should only be one process - this one!
        val pidofString = Shell.executeCommand("pidof ${Packages.TEST}").trim()

        when {
            Build.VERSION.SDK_INT < 23 -> {
                // command doesn't exist (and we don't try and read stderr here)
                assertEquals("", pidofString)
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
        val output = Shell.executeCommand("ps -A").trim()
        when {
            Build.VERSION.SDK_INT <= 23 -> {
                // doesn't correctly handle -A
                assertTrue(
                    output.matches(Regex("USER.+PID.+PPID.+VSIZE.+RSS.+WCHAN.+PC.+NAME")),
                    "expected no processes from 'ps -A', saw $output"
                )
            }
            Build.VERSION.SDK_INT in 24..25 -> {
                // still doesn't support, but useful error at least
                assertEquals("bad pid '-A'", output)
            }
            else -> {
                // ps -A should work - expect several processes including this one
                val processes = output.split("\n")
                assertTrue(processes.size > 5)
                assertTrue(processes.any { it.endsWith(Packages.TEST) })
            }
        }
    }
}