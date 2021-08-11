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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SmallTest
@RunWith(AndroidJUnit4::class)
class ShellTest {
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
        val packageName = InstrumentationRegistry.getInstrumentation().context.packageName
        assertNotNull(Shell.isPackageAlive(packageName))

        // this made up one shouldn't be
        assertNotNull(Shell.isPackageAlive("com.notalive.package.notarealapp"))
    }
}
