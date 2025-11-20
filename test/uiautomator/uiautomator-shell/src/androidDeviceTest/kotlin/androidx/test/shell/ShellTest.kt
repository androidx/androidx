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

import android.annotation.SuppressLint
import androidx.kruth.assertThat
import androidx.test.filters.SmallTest
import androidx.test.shell.internal.instrumentationPackageMediaDir
import java.io.File
import org.junit.Test

@SmallTest
class ShellTest {

    companion object {
        private const val PKG_SETTINGS: String = "com.android.settings"
    }

    @Test
    fun wifi(): Unit =
        with(Shell.wifi()) {
            turnOff()
            assertThat(isEnabled()).isFalse()
            turnOn()
            assertThat(isEnabled()).isTrue()
            turnOff()
            assertThat(isEnabled()).isFalse()
        }

    @Test
    fun startStopApplication(): Unit =
        with(Shell.application(PKG_SETTINGS)) {
            startApp()
            assertThat(Shell.screen().resumedActivityName()).startsWith(PKG_SETTINGS)
            stopApp()
            assertThat(Shell.screen().resumedActivityName()).doesNotContain(PKG_SETTINGS)
        }

    @Test
    fun clearApplicationData(): Unit =
        with(Shell.application(PKG_SETTINGS)) {
            startApp()
            assertThat(Shell.screen().resumedActivityName()).startsWith(PKG_SETTINGS)
            clearAppData()
            assertThat(Shell.screen().resumedActivityName()).doesNotContain(PKG_SETTINGS)
        }

    @Test
    fun killPid(): Unit =
        with(Shell.process()) {
            val pid =
                with(Shell.command("echo pid:$$ ; exec sleep 10")) {
                    stdOutStream
                        .bufferedReader()
                        .lineSequence()
                        .first { it.startsWith("pid:") }
                        .split("pid:")[1]
                        .toInt()
                }

            assertThat(isProcessAlive(pid)).isTrue()
            killPid(pid = pid, signal = "SIGKILL")
            assertThat(isProcessAlive(pid)).isFalse()
        }

    @SuppressLint("BanThreadSleep")
    @Test
    fun recording(): Unit =
        with(Shell.recorder()) {
            val file = File(instrumentationPackageMediaDir, "recording.mp4")
            val recording = start(outputFile = file, timeLimitSeconds = 3, bitRateMb = 4)
            recording.await()
            assertThat(file.length()).isGreaterThan(0L)
        }

    @Test
    fun longOutputCommand() {
        val min = 100000
        val max = 999999
        val commandOutput =
            Shell.command(
                command = "i=$min; while [ ${"$"}i -le $max ]; do echo ${"$"}i; i=$((i+1)); done"
            )

        var i = min
        commandOutput.stdOutStream.bufferedReader().use {
            while (true) {
                val line = it.readLine()
                if (line == null) break
                assertThat(line.trim().toInt()).isEqualTo(i)
                i++
            }
        }
    }

    @Test
    fun shortOutputCommand() {
        val out = Shell.command("echo abc").stdOut.trim()
        assertThat(out).isEqualTo("abc")
    }

    @Test
    fun shortOutputCommandOnError() {
        val out = Shell.command("echo abc >&2").stdErr.trim()
        assertThat(out).isEqualTo("abc")
    }
}
