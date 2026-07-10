/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.test.shell

import androidx.kruth.assertThat
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import java.io.DataInputStream
import org.junit.Test

@SmallTest
class ShellProcessTest {

    private val shellServer = ShellServer.start()

    @Test
    fun echo() {
        shellServer.newProcess().use {
            it.writeLine("echo cat")
            assertThat(it.stdOut.bufferedReader().readLine()).isEqualTo("cat")
        }
    }

    @Test
    fun closeShell() {
        val shellProcess = shellServer.newProcess()
        assertThat(shellProcess.isClosed()).isFalse()
        shellProcess.close()

        // Wait at most 5 seconds for shell to close.
        val start = System.currentTimeMillis()
        while (!shellProcess.isClosed() && System.currentTimeMillis() - start < 5000) {
            // Just wait
        }
        assertThat(shellProcess.isClosed()).isTrue()
    }

    @SdkSuppress(minSdkVersion = 24) // b/441558679
    @Test
    fun pipe() {
        shellServer.newProcess().use {
            it.writeLine("echo cat | sed 's/c/b/g'")
            assertThat(it.stdOut.bufferedReader().readLine()).isEqualTo("bat")
        }
    }

    @Test
    fun streams() {
        shellServer.newProcess().use {
            it.writeLine("echo foo > /data/local/tmp/test")
            it.writeLine("cat /data/local/tmp/test")
            assertThat(it.stdOut.bufferedReader().readLine()).isEqualTo("foo")
        }
    }

    @Test
    fun multiline() {
        val shellProcess = shellServer.newProcess()
        val reader = shellProcess.stdOut.bufferedReader()

        for (i in 0 until 100) {
            shellProcess.writeLine("echo $i")
            assertThat(reader.readLine()).isEqualTo("$i")

            shellProcess.writeLine("echo a; echo b; echo c;")
            assertThat(reader.readLine()).isEqualTo("a")
            assertThat(reader.readLine()).isEqualTo("b")
            assertThat(reader.readLine()).isEqualTo("c")
        }

        shellProcess.close()
    }

    // Deprecation suppressed because we want to specifically test DataInputStream#readline to
    // ensure compatibility.
    @Suppress("DEPRECATION")
    @Test
    fun multilineWithDataInputStream() {
        val shellProcess = shellServer.newProcess()
        val dis = DataInputStream(shellProcess.stdOut)

        for (i in 0 until 100) {
            shellProcess.writeLine("echo $i")
            assertThat(dis.readLine()).isEqualTo("$i")

            shellProcess.writeLine("echo a; echo b; echo c;")
            assertThat(dis.readLine()).isEqualTo("a")
            assertThat(dis.readLine()).isEqualTo("b")
            assertThat(dis.readLine()).isEqualTo("c")
        }

        shellProcess.close()
    }

    @Test
    fun bufferedReaderReadText() {
        val pid =
            shellServer.newProcess().use {
                it.writeLine("echo $$ ; exec sleep 10")
                it.stdOut.bufferedReader().readLine().trim().toInt()
            }

        shellServer.newProcess().use {
            it.writeLine("kill -TERM $pid")
            it.writeLine("exit")
            it.stdOut.bufferedReader().readText()
        }
    }
}
