/*
 * Copyright 2019 The Android Open Source Project
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
import androidx.annotation.RequiresApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.After
import org.junit.Assert
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SmallTest
@RunWith(AndroidJUnit4::class)
class ShellTest {
    @Before
    @After
    fun setup() {
        if (Build.VERSION.SDK_INT >= 23) {
            // ensure we don't leak background processes
            Shell.terminateProcessesAndWait(
                KILL_WAIT_POLL_PERIOD_MS,
                KILL_WAIT_POLL_MAX_COUNT,
                BACKGROUND_SPINNING_PROCESS_NAME
            )
        }
    }

    @Test
    fun optionalCommand_ls() {
        // command isn't important, it's just something that's not `echo`, and guaranteed to print
        val output = Shell.optionalCommand("ls /sys/devices/system/cpu")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            assertNotNull(output)
        } else {
            assertNull(output)
        }
    }

    @Test
    fun optionalCommand_echo() {
        val output = Shell.optionalCommand("echo foo")

        val expected = when {
            Build.VERSION.SDK_INT >= 23 -> "foo\n"
            // known bug in the shell on L (21,22). `echo` doesn't work with shell
            // programmatically, only works in interactive shell :|
            Build.VERSION.SDK_INT in 21..22 -> ""
            else -> null
        }

        assertEquals(expected, output)
    }

    private fun CpuInfo.CoreDir.scalingMinFreqPath() = "$path/cpufreq/scaling_min_freq"

    @Test
    fun catProcFileLong() {
        val onlineCores = CpuInfo.coreDirs.filter { it.online }

        // skip test on devices that can't read scaling_min_freq, like emulators
        assumeTrue(
            "cpufreq dirs don't have scaling_min_freq, bypassing test",
            onlineCores
                .all { File(it.scalingMinFreqPath()).exists() }
        )

        onlineCores.forEach {
            // While CpuInfo actually uses this function to read scaling_setspeed, reading
            // that value isn't enabled on all devices, so we use scaling_min_freq, which
            // is more likely to be readable
            val output = Shell.catProcFileLong(it.scalingMinFreqPath())

            // if the path exists, it should be readable by shell for every online core
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                assertNotNull(output)
            } else {
                assertNull(output)
            }
        }
    }

    @SdkSuppress(minSdkVersion = 21)
    @Test
    fun executeScript_trivial() {
        Assert.assertEquals("foo\n", Shell.executeScript("echo foo"))
    }

    @SdkSuppress(minSdkVersion = 21)
    @Test
    fun executeScriptWithStderr_trivial() {
        Assert.assertEquals(Shell.Output("foo\n", ""), Shell.executeScriptWithStderr("echo foo"))
    }

    @SdkSuppress(minSdkVersion = 21)
    @Test
    fun executeScriptWithStderr_invalidCommand() {
        val shellOutput = Shell.executeScriptWithStderr("invalidCommand")

        Assert.assertEquals("", shellOutput.stdout)

        val stderr = shellOutput.stderr

        // sample stderr observed in manual testing:
        // API 23: "invalidCommand: not found"
        // API 30: "invalidCommand: inaccessible or not found"
        Assert.assertTrue(
            "unexpected stderr \"$stderr\"",
            stderr.contains("invalidCommand") && stderr.contains("not found")
        )
    }

    @SdkSuppress(minSdkVersion = 26) // xargs only available 26+
    @Test
    fun executeScript_pipe_xargs() {
        // validate piping works with xargs
        Assert.assertEquals("foo\n", Shell.executeScript("echo foo | xargs echo $1"))
    }

    @SdkSuppress(minSdkVersion = 29) // `$(</dev/stdin)` doesn't work before 29
    @Test
    fun executeScript_pipe_echo() {
        // validate piping works
        Assert.assertEquals("foo\n", Shell.executeScript("echo foo | echo $(</dev/stdin)"))
    }

    @SdkSuppress(minSdkVersion = 26) // xargs only available 26+
    @Test
    fun executeScript_stdinArg_xargs() {
        // validate stdin to first command in script
        Assert.assertEquals("foo\n", Shell.executeScript("xargs echo $1", stdin = "foo"))
    }

    @SdkSuppress(minSdkVersion = 29) // `$(</dev/stdin)` doesn't work before 29
    @Test
    fun executeScript_stdinArg_echo() {
        // validate stdin to first command in script
        Assert.assertEquals("foo\n", Shell.executeScript("echo $(</dev/stdin)", stdin = "foo"))
    }

    @SdkSuppress(minSdkVersion = 21)
    @Test
    fun executeScript_multilineRedirect() {
        Assert.assertEquals(
            "foo\n",
            Shell.executeScript(
                """
                    echo foo > /data/local/tmp/foofile
                    cat /data/local/tmp/foofile
                """.trimIndent()
            )
        )
    }

    @SdkSuppress(minSdkVersion = 26) // xargs only available 26+
    @Test
    fun executeScript_multilineRedirectStdin_xargs() {
        Assert.assertEquals(
            "foo\n",
            Shell.executeScript(
                """
                    xargs echo $1 > /data/local/tmp/foofile
                    cat /data/local/tmp/foofile
                """.trimIndent(),
                stdin = "foo"
            )
        )
    }

    @SdkSuppress(minSdkVersion = 29) // `$(</dev/stdin)` doesn't work before 29
    @Test
    fun executeScript_multilineRedirectStdin_echo() {
        Assert.assertEquals(
            "foo\n",
            Shell.executeScript(
                """
                    echo $(</dev/stdin) > /data/local/tmp/foofile
                    cat /data/local/tmp/foofile
                """.trimIndent(),
                stdin = "foo"
            )
        )
    }

    @SdkSuppress(minSdkVersion = 21)
    @Test
    fun createRunnableExecutable_simpleScript() {
        val path = Shell.createRunnableExecutable(
            name = "myScript.sh",
            inputStream = "echo foo".byteInputStream()
        )
        try {
            Assert.assertEquals(
                "foo\n",
                Shell.executeCommand(path)
            )
        } finally {
            Shell.executeCommand("rm $path")
        }
    }

    @SdkSuppress(minSdkVersion = 21)
    @Test
    fun isPackageAlive() {
        // this package is certainly alive...
        assertNotNull(Shell.isPackageAlive(Packages.TEST))

        // this made up one shouldn't be
        assertNotNull(Shell.isPackageAlive(Packages.FAKE))
    }

    @SdkSuppress(minSdkVersion = 21)
    @Test
    fun pidof() {
        assertNotNull(pidof(Packages.TEST))
        assertNull(pidof(Packages.FAKE))
    }

    @SdkSuppress(minSdkVersion = 21)
    @Test
    fun isPidAlive() {
        val pid = pidof(Packages.TEST)!!
        assertTrue(Shell.isProcessAlive(pid, Packages.TEST))

        // if either package name OR pid doesn't match, return false
        assertFalse(Shell.isProcessAlive(pid, Packages.FAKE))
        assertFalse(Shell.isProcessAlive(pid + 1, Packages.TEST))
    }

    @SdkSuppress(minSdkVersion = 23)
    @Test
    fun killTermProcessesAndWait() {
        // validate that killTermProcessesAndWait kills bg process
        val backgroundProcess = getBackgroundSpinningProcess()
        assertTrue(backgroundProcess.isAlive())
        Shell.terminateProcessesAndWait(
            KILL_WAIT_POLL_PERIOD_MS,
            KILL_WAIT_POLL_MAX_COUNT,
            backgroundProcess
        )
        assertFalse(backgroundProcess.isAlive())
    }

    @SdkSuppress(minSdkVersion = 23)
    @Test
    fun killTermProcessesAndWait_allowBackground() {
        val backgroundProcess1 = getBackgroundSpinningProcess()
        val backgroundProcess2 = getBackgroundSpinningProcess()

        assertTrue(backgroundProcess1.isAlive())
        assertTrue(backgroundProcess2.isAlive())

        Shell.terminateProcessesAndWait(
            KILL_WAIT_POLL_PERIOD_MS,
            KILL_WAIT_POLL_MAX_COUNT,
            backgroundProcess1
        )

        // Only process 1 should be killed
        assertFalse(backgroundProcess1.isAlive())
        assertTrue(backgroundProcess2.isAlive())

        Shell.terminateProcessesAndWait(
            KILL_WAIT_POLL_PERIOD_MS,
            KILL_WAIT_POLL_MAX_COUNT,
            backgroundProcess2
        )

        // Now both are killed
        assertFalse(backgroundProcess1.isAlive())
        assertFalse(backgroundProcess2.isAlive())
    }

    @SdkSuppress(minSdkVersion = 23)
    @Test
    fun killTermProcessesAndWait_multi() {
        val backgroundProcess1 = getBackgroundSpinningProcess()
        val backgroundProcess2 = getBackgroundSpinningProcess()

        assertTrue(backgroundProcess1.isAlive())
        assertTrue(backgroundProcess2.isAlive())

        Shell.terminateProcessesAndWait(
            KILL_WAIT_POLL_PERIOD_MS,
            KILL_WAIT_POLL_MAX_COUNT,
            backgroundProcess1,
            backgroundProcess2
        )

        // both processes should be killed
        assertFalse(backgroundProcess1.isAlive())
        assertFalse(backgroundProcess2.isAlive())
    }

    @SdkSuppress(minSdkVersion = 23)
    @Test
    fun killTermAllAndWait() {
        val backgroundProcess1 = getBackgroundSpinningProcess()
        val backgroundProcess2 = getBackgroundSpinningProcess()

        assertTrue(backgroundProcess1.isAlive())
        assertTrue(backgroundProcess2.isAlive())

        Shell.terminateProcessesAndWait(
            KILL_WAIT_POLL_PERIOD_MS,
            KILL_WAIT_POLL_MAX_COUNT,
            BACKGROUND_SPINNING_PROCESS_NAME
        )

        assertFalse(backgroundProcess1.isAlive())
        assertFalse(backgroundProcess2.isAlive())
    }

    @SdkSuppress(minSdkVersion = 21)
    @Test
    fun getRunningSubPackages() {
        assertEquals(emptyList(), Shell.getRunningProcessesForPackage("not.a.real.packagename"))
        assertEquals(listOf(Packages.TEST), Shell.getRunningProcessesForPackage(Packages.TEST))
    }

    @SdkSuppress(minSdkVersion = 21)
    @Test
    fun checkRootStatus() {
        if (Shell.isSessionRooted()) {
            assertContains(Shell.executeCommand("id"), "uid=0(root)")
        } else {
            assertFalse(
                Shell.executeCommand("id").contains("uid=0(root)"),
                "Shell.isSessionRooted() is false so user should not be root"
            )
        }
    }

    @RequiresApi(21)
    private fun pidof(packageName: String): Int? {
        return Shell.getPidsForProcess(packageName).firstOrNull()
    }

    companion object {
        const val KILL_WAIT_POLL_PERIOD_MS = 50L
        const val KILL_WAIT_POLL_MAX_COUNT = 50

        /**
         * Run the shell command "yes" as a background process to enable testing process killing /
         * liveness checks.
         *
         * TODO: support this down to API 21, and lower RequiresApi for associated tests
         */
        @RequiresApi(23)
        fun getBackgroundSpinningProcess(): Shell.ProcessPid {
            val pid = Shell.executeScript(
                """
                    $BACKGROUND_SPINNING_PROCESS_NAME > /dev/null 2> /dev/null &
                    echo $!
                """.trimIndent()
            )
                .trim()
                .toIntOrNull()
            assertNotNull(pid)
            return Shell.ProcessPid(BACKGROUND_SPINNING_PROCESS_NAME, pid)
        }

        /**
         * Command and name of process for background task used in several tests.
         *
         * We use "yes" here, as it's available back to 23.
         *
         * Also tried "sleep 20", but this resulted in the Shell.executeCommand not completing
         * until after the sleep terminated, which made it not useful.
         */
        @RequiresApi(23)
        val BACKGROUND_SPINNING_PROCESS_NAME = "yes"
    }
}
