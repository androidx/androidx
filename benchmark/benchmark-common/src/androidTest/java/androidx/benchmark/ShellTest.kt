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
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.After
import org.junit.Assert
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ShellTest {

    @Before
    @After
    fun setup() {
        if (Build.VERSION.SDK_INT >= 23) {
            // ensure we don't leak background processes
            Shell.killProcessesAndWait(BACKGROUND_SPINNING_PROCESS_NAME)
        }
    }

    private fun CpuInfo.CoreDir.scalingMinFreqPath() = "$path/cpufreq/scaling_min_freq"

    @Test
    fun catProcFileLong() {
        val onlineCores = CpuInfo.coreDirs.filter { it.online }

        // skip test on devices that can't read scaling_min_freq, like emulators
        assumeTrue(
            "cpufreq dirs don't have scaling_min_freq, bypassing test",
            onlineCores.all { File(it.scalingMinFreqPath()).exists() },
        )

        onlineCores.forEach {
            // While CpuInfo actually uses this function to read scaling_setspeed, reading
            // that value isn't enabled on all devices, so we use scaling_min_freq, which
            // is more likely to be readable
            val output = Shell.catProcFileLong(it.scalingMinFreqPath())

            // if the path exists, it should be readable by shell for every online core
            assertNotNull(output)
        }
    }

    @Test
    fun executeScriptCaptureStdout_trivial() {
        Assert.assertEquals("foo\n", Shell.executeScriptCaptureStdout("echo foo"))
    }

    @Test
    fun executeScriptCaptureStdoutStderr_trivial() {
        Assert.assertEquals(
            Shell.Output("foo\n", ""),
            Shell.executeScriptCaptureStdoutStderr("echo foo"),
        )
    }

    @Test
    fun executeScriptCaptureStdoutStderr_stderrFirstLine() {
        Assert.assertEquals(
            Shell.Output("bar\n", "foo\n"),
            Shell.executeScriptCaptureStdoutStderr(
                """
                echo foo 1>&2 # stderr on 1st line, previously unsupported
                echo bar
                """
                    .trimIndent()
            ),
        )
    }

    @Test
    fun executeScriptCaptureStdoutStderr_invalidCommand() {
        val shellOutput = Shell.executeScriptCaptureStdoutStderr("invalidCommand")

        Assert.assertEquals("", shellOutput.stdout)

        val stderr = shellOutput.stderr

        // sample stderr observed in manual testing:
        // API 23: "invalidCommand: not found"
        // API 30: "invalidCommand: inaccessible or not found"
        Assert.assertTrue(
            "unexpected stderr \"$stderr\"",
            stderr.contains("invalidCommand") && stderr.contains("not found"),
        )
    }

    @Test
    fun executeScriptCaptureStdout_pipe_xargs() {
        // validate piping works with xargs
        Assert.assertEquals("foo\n", Shell.executeScriptCaptureStdout("echo foo | xargs echo $1"))
    }

    @SdkSuppress(minSdkVersion = 29) // `$(</dev/stdin)` doesn't work before 29
    @Test
    fun executeScriptCaptureStdout_pipe_echo() {
        // validate piping works
        Assert.assertEquals(
            "foo\n",
            Shell.executeScriptCaptureStdout("echo foo | echo $(</dev/stdin)"),
        )
    }

    @Test
    fun executeScriptCaptureStdout_stdinArg_xargs() {
        // validate stdin to first command in script
        Assert.assertEquals(
            "foo\n",
            Shell.executeScriptCaptureStdout("xargs echo $1", stdin = "foo"),
        )
    }

    @SdkSuppress(minSdkVersion = 29) // `$(</dev/stdin)` doesn't work before 29
    @Test
    fun executeScriptCaptureStdout_stdinArg_echo() {
        // validate stdin to first command in script
        Assert.assertEquals(
            "foo\n",
            Shell.executeScriptCaptureStdout("echo $(</dev/stdin)", stdin = "foo"),
        )
    }

    @Test
    fun executeScriptCaptureStdout_multilineRedirect() {
        Assert.assertEquals(
            "foo\n",
            Shell.executeScriptCaptureStdout(
                """
                    echo foo > /data/local/tmp/foofile
                    cat /data/local/tmp/foofile
                """
                    .trimIndent()
            ),
        )
    }

    @Test
    fun executeScriptCaptureStdout_multilineRedirectStdin_xargs() {
        Assert.assertEquals(
            "foo\n",
            Shell.executeScriptCaptureStdout(
                """
                    xargs echo $1 > /data/local/tmp/foofile
                    cat /data/local/tmp/foofile
                """
                    .trimIndent(),
                stdin = "foo",
            ),
        )
    }

    @SdkSuppress(minSdkVersion = 29) // `$(</dev/stdin)` doesn't work before 29
    @Test
    fun executeScriptCaptureStdout_multilineRedirectStdin_echo() {
        Assert.assertEquals(
            "foo\n",
            Shell.executeScriptCaptureStdout(
                """
                    echo $(</dev/stdin) > /data/local/tmp/foofile
                    cat /data/local/tmp/foofile
                """
                    .trimIndent(),
                stdin = "foo",
            ),
        )
    }

    @Test
    fun createRunnableExecutable_simpleScript() {
        val path =
            Shell.createRunnableExecutable(
                name = "myScript.sh",
                inputStream = "echo foo".byteInputStream(),
            )
        try {
            Assert.assertEquals("foo\n", Shell.executeScriptCaptureStdout(path))
        } finally {
            Shell.executeScriptCaptureStdout("rm $path")
        }
    }

    @Test
    fun isPackageAlive() {
        // this package is certainly alive...
        assertNotNull(Shell.isPackageAlive(Packages.TEST))

        // this made up one shouldn't be
        assertNotNull(Shell.isPackageAlive(Packages.FAKE))
    }

    @Test
    fun pidof() {
        assertNotNull(pidof(Packages.TEST))
        assertNull(pidof(Packages.FAKE))
    }

    @Test
    fun isPidAlive() {
        val pid = pidof(Packages.TEST)!!
        assertTrue(Shell.isProcessAlive(pid, Packages.TEST))

        // if either package name OR pid doesn't match, return false
        assertFalse(Shell.isProcessAlive(pid, Packages.FAKE))
        assertFalse(Shell.isProcessAlive(pid + 1, Packages.TEST))
    }

    @Test
    fun killProcessesAndWait() {
        // validate that killTermProcessesAndWait kills bg process
        val backgroundProcess = getBackgroundSpinningProcess()
        assertTrue(backgroundProcess.isAlive())
        Shell.killProcessesAndWait(listOf(backgroundProcess))
        assertFalse(backgroundProcess.isAlive())
    }

    @Test
    fun killProcessesAndWait_nonExistentProcess() {
        Shell.killProcessesAndWait(
            processName = Packages.FAKE,
            processKiller = { fail("shouldn't execute process killer, no process of this name") },
        )
    }

    @Test
    fun killProcessesAndWait_failure() {
        // validate that killTermProcessesAndWait kills bg process
        val backgroundProcess = getBackgroundSpinningProcess()
        assertTrue(backgroundProcess.isAlive())
        assertFailsWith<IllegalStateException> {
            Shell.killProcessesAndWait(listOf(backgroundProcess), waitPollMaxCount = 5) {
                // noop, process not killed!
            }
        }

        var failureMessage = ""
        Shell.killProcessesAndWait(
            listOf(backgroundProcess),
            waitPollMaxCount = 5,
            onFailure = { error -> failureMessage = error },
        ) {
            // noop, process not killed!
        }
        assertTrue(failureMessage.startsWith("Failed to stop "))
        assertTrue(backgroundProcess.isAlive())
    }

    @Test
    fun killProcessesAndWait_allowBackground() {
        val backgroundProcess1 = getBackgroundSpinningProcess()
        val backgroundProcess2 = getBackgroundSpinningProcess()

        assertTrue(backgroundProcess1.isAlive())
        assertTrue(backgroundProcess2.isAlive())

        Shell.killProcessesAndWait(listOf(backgroundProcess1))

        // Only process 1 should be killed
        assertFalse(backgroundProcess1.isAlive())
        assertTrue(backgroundProcess2.isAlive())

        Shell.killProcessesAndWait(listOf(backgroundProcess2))

        // Now both are killed
        assertFalse(backgroundProcess1.isAlive())
        assertFalse(backgroundProcess2.isAlive())
    }

    @Test
    fun killProcessesAndWait_multi() {
        val backgroundProcess1 = getBackgroundSpinningProcess()
        val backgroundProcess2 = getBackgroundSpinningProcess()

        assertTrue(backgroundProcess1.isAlive())
        assertTrue(backgroundProcess2.isAlive())

        Shell.killProcessesAndWait(listOf(backgroundProcess1, backgroundProcess2))

        // both processes should be killed
        assertFalse(backgroundProcess1.isAlive())
        assertFalse(backgroundProcess2.isAlive())
    }

    @Test
    fun killProcessesAndWait_processName() {
        val backgroundProcess1 = getBackgroundSpinningProcess()
        val backgroundProcess2 = getBackgroundSpinningProcess()

        assertTrue(backgroundProcess1.isAlive())
        assertTrue(backgroundProcess2.isAlive())

        Shell.killProcessesAndWait(BACKGROUND_SPINNING_PROCESS_NAME)

        assertFalse(backgroundProcess1.isAlive())
        assertFalse(backgroundProcess2.isAlive())
    }

    @Test
    fun getRunningSubPackages() {
        assertEquals(emptyList(), Shell.getRunningProcessesForPackage("not.a.real.packagename"))
        assertEquals(listOf(Packages.TEST), Shell.getRunningProcessesForPackage(Packages.TEST))
    }

    @Test
    fun checkRootStatus() {
        if (Shell.isSessionRooted()) {
            assertContains(Shell.executeScriptCaptureStdout("id"), "uid=0(root)")
        } else {
            assertFalse(
                Shell.executeScriptCaptureStdout("id").contains("uid=0(root)"),
                "Shell.isSessionRooted() is false so user should not be root",
            )
        }
    }

    @Test
    fun shellReuse() {
        val script = Shell.createShellScript("xargs echo $1", stdin = "foo")

        repeat(2) {
            // validates that the stdin can be reused across multiple invocations
            with(script.start()) { assertEquals(listOf("foo"), stdOutLineSequence().toList()) }
        }
        script.cleanUp()
    }

    @Test
    fun getChecksum() {
        val emptyPaths = listOf("/data/local/tmp/emptyfile1", "/data/local/tmp/emptyfile2")
        try {
            val checksums =
                emptyPaths.map {
                    Shell.executeScriptSilent("rm -f $it")
                    Shell.executeScriptSilent("touch $it")
                    Shell.getChecksum(it)
                }

            assertEquals(checksums.first(), checksums.last())
            if (Build.VERSION.SDK_INT < 23) {
                checksums.forEach { checksum ->
                    // getChecksum uses ls -l to check size pre API 23,
                    // this validates that behavior + result parsing
                    assertEquals("0", checksum)
                }
            }
        } finally {
            emptyPaths.forEach { Shell.executeScriptSilent("rm -f $it") }
        }
    }

    @Test
    fun getCompilationMode() {
        val status = Shell.getCompilationMode(Packages.TEST)
        assertTrue(
            actual =
                status in
                    listOf(
                        // Api 21-23
                        "speed",
                        // Api 24-25
                        "interpret-only",
                        // Api 26-27
                        "quicken",
                        // Api 28 and above
                        "run-from-apk",
                    ),
            message = "Unexpected status value: $status",
        )
    }

    @Test
    fun parseCompilationMode() {
        // Captured on API 26 emulator
        assertEquals(
            expected = "quicken",
            actual =
                Shell.parseCompilationMode(
                    26,
                    """
                      Dexopt state:
                          [androidx.benchmark.test]
                            Instruction Set: x86
                              path: /data/app/androidx.benchmark.test-C3VDUG1iLystEGyQTxcspA==/base.apk
                              status: /data/app/androidx.benchmark.test-C3VDUG1iLystEGyQTxcspA==/oat/x86/base.odex[status=kOatUpToDate, compilat
                              ion_filter=quicken]
        """
                        .trimIndent(),
                ),
        )

        // Captured on API 26 sailfish
        assertEquals(
            expected = "speed",
            actual =
                Shell.parseCompilationMode(
                    26,
                    """
                    Dexopt state:
                      [androidx.compose.foundation.layout.benchmark.test]
                        path: /data/app/androidx.compose.foundation.layout.benchmark.test-pBhSh_spHfjDL-5jgzu_Jg==/base.apk
                          arm64: /data/app/androidx.compose.foundation.layout.benchmark.test-pBhSh_spHfjDL-5jgzu_Jg==/oat/arm64/base.odex[status=kOatUpToDate, compilation_filter=speed]
        """
                        .trimIndent(),
                ),
        )

        // Captured on API 28 or higher device, duplicated from comment, specifics not known
        assertEquals(
            expected = "verify",
            actual =
                Shell.parseCompilationMode(
                    29,
                    """
                Dexopt state:
                 [com.android.settings]
                   path: .../SettingsGoogle.apk
                     arm64: [status=verify] [reason=vdex] [primary-abi]
                       [location is .../SettingsGoogle.vdex]

                ## These lines added for test purposes
                ## status=0 []
        """
                        .trimIndent(),
                ),
        )

        // Captured on API 32 emulator
        assertEquals(
            expected = "run-from-apk",
            actual =
                Shell.parseCompilationMode(
                    32,
                    """
                Dexopt state:
                  [androidx.benchmark.test]
                    path: /data/app/~~coMYW_NCkevOuZyH32n5Ag==/androidx.benchmark.test-kcNBMDGJ58lezaNWmNyTzQ==/base.apk
                      x86_64: [status=run-from-apk] [reason=unknown]
                """
                        .trimIndent(),
                ),
        )
    }

    @Test
    fun psLineContainsProcess() {
        // shell executables
        "root      10065 10061 14848  3932  poll_sched 7bcaf1fc8c S /data/local/tmp/tracebox"
            .apply {
                assertTrue(Shell.psLineContainsProcess(this, "tracebox"))
                assertFalse(Shell.psLineContainsProcess(this, "tracebo"))
            }

        "root      10109 1     11552  1140  poll_sched 78c86eac8c S ./tracebox"
            .apply {
                assertTrue(Shell.psLineContainsProcess(this, "tracebox"))
                assertFalse(Shell.psLineContainsProcess(this, "tracebo"))
            }

        // app
        "u0_a140       9253  9778   15120128 139856 do_epoll_wait       0 S example.app"
            .apply {
                assertTrue(Shell.psLineContainsProcess(this, "example.app"))
                assertFalse(Shell.psLineContainsProcess(this, "example.ap"))
            }
        // app subprocess
        "u0_a140       9253  9778   15120128 139856 do_epoll_wait       0 S example.app:ui"
            .apply {
                assertTrue(Shell.psLineContainsProcess(this, "example.app:ui"))
                assertTrue(Shell.psLineContainsProcess(this, "example.app"))
                assertFalse(Shell.psLineContainsProcess(this, "example.ap"))
            }
    }

    @Test
    fun fullProcessNameMatchesProcess() {
        // shell executables
        assertTrue(Shell.fullProcessNameMatchesProcess("/data/local/tmp/tracebox", "tracebox"))
        assertFalse(Shell.fullProcessNameMatchesProcess("/data/local/tmp/tracebox", "tracebo"))

        assertTrue(Shell.fullProcessNameMatchesProcess("./tracebox", "tracebox"))
        assertFalse(Shell.fullProcessNameMatchesProcess("./tracebox", "tracebo"))

        // app
        assertTrue(Shell.fullProcessNameMatchesProcess("example.app", "example.app"))
        assertFalse(Shell.fullProcessNameMatchesProcess("example.app", "example.ap"))

        // app subprocess
        assertTrue(Shell.fullProcessNameMatchesProcess("example.app:ui", "example.app:ui"))
        assertTrue(Shell.fullProcessNameMatchesProcess("example.app:ui", "example.app"))
        assertFalse(Shell.fullProcessNameMatchesProcess("example.app:ui", "example.ap"))
    }

    @Test
    @SdkSuppress(minSdkVersion = 36)
    fun pgrepLF() {
        val processPids = Shell.pgrepLF(Packages.TEST)
        assertTrue(
            processPids.any { it.processName == Packages.TEST },
            "expected package name to be contained in output:\n${processPids.joinToString("\n")}",
        )
    }

    @Test
    @SdkSuppress(maxSdkVersion = 35)
    fun pgrepLFBelowApi36() {
        val processPids = Shell.pgrepLF(Packages.TEST)
        assertTrue(
            processPids.any { it.processName == Packages.TEST },
            "expected package name to be contained in output:\n${processPids.joinToString("\n")}",
        )
    }

    @Test
    fun enablePackages() {
        Shell.enablePackages(listOf(Packages.FAKE, Packages.FAKE + "1"))
    }

    @Test
    fun disablePackages() {
        Shell.disablePackages(listOf(Packages.FAKE, Packages.FAKE + "1"))
    }

    private fun pidof(packageName: String): Int? {
        return Shell.getPidsForProcess(packageName).firstOrNull()
    }

    companion object {

        /**
         * Run the shell command "yes" as a background process to enable testing process killing /
         * liveness checks.
         *
         * TODO: support this down to API 21, and lower RequiresApi for associated tests
         */
        @RequiresApi(23)
        fun getBackgroundSpinningProcess(): Shell.ProcessPid {
            val pid =
                Shell.executeScriptCaptureStdout(
                        """
                    $BACKGROUND_SPINNING_PROCESS_NAME > /dev/null 2> /dev/null &
                    echo $!
                """
                            .trimIndent()
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
         * Also tried "sleep 20", but this resulted in the Shell.executeCommand not completing until
         * after the sleep terminated, which made it not useful.
         */
        @RequiresApi(23) val BACKGROUND_SPINNING_PROCESS_NAME = "yes"
    }
}
