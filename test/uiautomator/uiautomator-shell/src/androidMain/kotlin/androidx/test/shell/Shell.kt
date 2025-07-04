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

import android.util.Log
import androidx.test.shell.command.ApplicationCommands
import androidx.test.shell.command.PermissionCommands
import androidx.test.shell.command.ProcessCommands
import androidx.test.shell.command.RecorderCommands
import androidx.test.shell.command.ScreenCommands
import androidx.test.shell.command.WifiCommands
import androidx.test.shell.internal.TAG
import java.io.InputStream

/**
 * Allows to execute commands. This class builds on top of [ShellProcess] and abstracts the shell
 * streams to focus on the output of a single command execution. A [ShellProcess] is created for
 * each executed command, using a [ShellServer] that can be reset by [Shell.setShellServer].
 */
public object Shell {

    private val DEFAULT_SHELL_SERVER by lazy { ShellServer.start() }
    private var shellServer: ShellServer? = null

    /**
     * Allows configuring the underlying [ShellProcess] utilized to execute the commands. The
     * current [ShellServer] backing [Shell] is always closed when setting a new one.
     *
     * @param shellServer The new shell server to produce [ShellProcess] to launch commands.
     */
    public fun setShellServer(shellServer: ShellServer) {
        this.shellServer?.close()
        this.shellServer = shellServer
    }

    /**
     * Commands for wifi.
     *
     * @return an instance of [WifiCommands].
     */
    public fun wifi(): WifiCommands = WifiCommands(shell = this)

    /** Commands for screen. */
    public fun screen(): ScreenCommands = ScreenCommands(shell = this)

    /**
     * Commands for application.
     *
     * @param packageName the application package name
     * @return an instance of [ApplicationCommands].
     */
    public fun application(packageName: String): ApplicationCommands =
        ApplicationCommands(shell = this, packageName = packageName)

    /**
     * Commands for screen recorder.
     *
     * @return an instance of [RecorderCommands].
     */
    public fun recorder(): RecorderCommands = RecorderCommands(shell = this)

    /**
     * Commands for processes.
     *
     * @return an instance of [ProcessCommands].
     */
    public fun process(): ProcessCommands = ProcessCommands(shell = this)

    /**
     * Commands for permissions.
     *
     * @param packageName the application package name
     * @return an instance of [PermissionCommands].
     */
    public fun permission(packageName: String): PermissionCommands =
        PermissionCommands(shell = this, packageName = packageName)

    /**
     * Executes a given command and returns the ongoing [CommandOutput].
     *
     * @param command a string containing the command to launch.
     * @return a [CommandOutput] that allows to access the stream of the command output.
     */
    public fun command(command: String): CommandOutput {
        val server = shellServer ?: DEFAULT_SHELL_SERVER
        val shellProcess = server.newProcess()
        shellProcess.writeLine(command)
        shellProcess.close()
        return CommandOutput(command = command, shellProcess = shellProcess)
    }

    /**
     * The output of a shell command executed via [Shell.command]. This class offers access to the
     * [stdOut] and [stdErr] of the launched command, with utility methods to wait for the full
     * output or parse the stream incrementally.
     */
    public class CommandOutput(
        private val command: String,
        private val shellProcess: ShellProcess,
    ) {

        /** Awaits the process termination and returns the full stdout of the launched command. */
        public val stdOut: String by lazy {
            shellProcess.stdOut.use { it.bufferedReader().readText() }
        }

        /** Awaits the process termination and returns the full stderr of the launched command. */
        public val stdErr: String by lazy {
            shellProcess.stdErr.use { it.bufferedReader().readText() }
        }

        /** Allows incrementally consuming the stdout of a single command as an [InputStream]. */
        public val stdOutStream: InputStream = shellProcess.stdOut

        /** Allows incrementally consuming the stderr of a single command as an [InputStream]. */
        public val stdErrStream: InputStream = shellProcess.stdErr

        /** Returns whether the launched shell command is still running. */
        public fun isRunning(): Boolean = !shellProcess.isClosed()

        internal fun String.assertEmpty() {
            if (isNotBlank()) throwWithCommandOutput()
        }

        internal fun String.assertNoFailure(
            vararg failureStrings: String = arrayOf("Error", "Failed", "Failure")
        ) {
            if (failureStrings.any { w -> contains(w, ignoreCase = true) }) throwWithCommandOutput()
        }

        internal fun throwWithCommandOutput() {
            val message = stdOutStdErrCommandOutput()
            message.lines().forEach { Log.e(TAG, it) }
            throw IllegalStateException(message)
        }

        internal fun stdOutStdErrCommandOutput(): String {
            val errorMsgLines =
                listOfNotNull(
                    "Fatal exception, command failed: `$command`",
                    *(if (stdOut.isNotBlank()) arrayOf("Stdout:", stdOut.trim()) else emptyArray()),
                    *(if (stdErr.isNotBlank()) arrayOf("StdErr:", stdErr.trim()) else emptyArray()),
                )
            return errorMsgLines.joinToString(separator = System.lineSeparator())
        }
    }
}
