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

package androidx.test.shell.command

import android.os.Build
import androidx.test.shell.Shell

/** Commands about processes. */
public class ProcessCommands internal constructor(private val shell: Shell) {

    private companion object {
        private val REGEX_PGREP = "^(\\d+) (.*)$".toRegex()
    }

    /**
     * Kills a process with the given pid using the given signal.
     *
     * @param pid the pid of the process to kill.
     * @param signal the name of the signal to send to the process.
     */
    @JvmOverloads
    public fun killPid(pid: Int, signal: String = "TERM"): Unit =
        with(shell.command("kill -$signal $pid")) {
            stdOut.assertEmpty()
            stdErr.assertEmpty()
        }

    /**
     * Runs process grep (pgrep) with the given process name.
     *
     * @param processName the name of the process to grep for.
     * @return a list of [ProcessPid] containing the pid and process name for each matching grep.
     */
    public fun processGrep(processName: String): List<ProcessPid> {
        val args =
            listOfNotNull(
                    "-l",
                    "-f",
                    // aosp/3507001 -> needed to print full command line (so full package name)
                    if (Build.VERSION.SDK_INT >= 36) "-a" else null,
                )
                .joinToString(separator = " ")
        val cmd = "pgrep $args $processName"
        return with(shell.command(cmd)) {
            stdOut
                .lines()
                .filter { it.isNotBlank() }
                .mapNotNull {
                    val (pid, processName) =
                        REGEX_PGREP.find(it)?.destructured ?: return@mapNotNull null
                    ProcessPid(pid = pid.toInt(), processName = processName)
                }
        }
    }

    /**
     * Gets the pid of a given process name. Note that underlying this api utilizes pgrep and
     * returns the pid associated to the first process with the name matching with the given
     * [processName], or -1 if not found.
     *
     * @param processName the name of the process to get the pid of.
     * @return the pid of the process if found or -1.
     */
    public fun getPid(processName: String): Int =
        processGrep(processName).firstOrNull { it.processName == processName }.let { it?.pid ?: -1 }

    /** Returns whether the given process id is associated with a process that is alive. */
    public fun isProcessAlive(pid: Int): Boolean =
        with(shell.command("kill -0 $pid")) { stdOut.isBlank() && stdErr.isBlank() }
}

/** Contains a process name and the process id associated with that name. */
public class ProcessPid(

    /** The name of the process. */
    public val processName: String,

    /** The id of the process. */
    public val pid: Int,
)
