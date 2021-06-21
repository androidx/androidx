/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.benchmark.macro.perfetto

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
public class ShellUtilsTest {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @SdkSuppress(minSdkVersion = 23) // broken below 23
    @Test
    public fun trivial() {
        assertEquals("foo\n", device.executeShellScript("echo foo"))
    }

    @SdkSuppress(minSdkVersion = 26) // xargs only available before 26
    @Test
    public fun pipe_xargs() {
        // validate piping works with xargs
        assertEquals("foo\n", device.executeShellScript("echo foo | xargs echo $1"))
    }

    @SdkSuppress(minSdkVersion = 29) // `$(</dev/stdin)` doesn't work before 29
    @Test
    public fun pipe_echo() {
        // validate piping works
        assertEquals("foo\n", device.executeShellScript("echo foo | echo $(</dev/stdin)"))
    }

    @SdkSuppress(minSdkVersion = 26) // xargs only available before 26
    @Test
    public fun stdinArg_xargs() {
        // validate stdin to first command in script
        assertEquals("foo\n", device.executeShellScript("xargs echo $1", stdin = "foo"))
    }

    @SdkSuppress(minSdkVersion = 29) // `$(</dev/stdin)` doesn't work before 29
    @Test
    public fun stdinArg_echo() {
        // validate stdin to first command in script
        assertEquals("foo\n", device.executeShellScript("echo $(</dev/stdin)", stdin = "foo"))
    }

    @SdkSuppress(minSdkVersion = 23) // broken below 23
    @Test
    public fun multilineRedirect() {
        assertEquals(
            "foo\n",
            device.executeShellScript(
                """
                    echo foo > /data/local/tmp/foofile
                    cat /data/local/tmp/foofile
                """.trimIndent()
            )
        )
    }

    @SdkSuppress(minSdkVersion = 26) // xargs only available before 26
    @Test
    public fun multilineRedirectStdin_xargs() {
        assertEquals(
            "foo\n",
            device.executeShellScript(
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
    public fun multilineRedirectStdin_echo() {
        assertEquals(
            "foo\n",
            device.executeShellScript(
                """
                    echo $(</dev/stdin) > /data/local/tmp/foofile
                    cat /data/local/tmp/foofile
                """.trimIndent(),
                stdin = "foo"
            )
        )
    }

    @SdkSuppress(minSdkVersion = 23) // broken below 23
    @Test
    public fun createRunnableExecutable_simpleScript() {
        val path = device.createRunnableExecutable(
            name = "myScript.sh",
            inputStream = "echo foo".byteInputStream()
        )
        try {
            assertEquals(
                "foo\n",
                device.executeShellCommand(path)
            )
        } finally {
            device.executeShellCommand("rm $path")
        }
    }
}