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
import android.os.Looper
import android.os.ParcelFileDescriptor.AutoCloseInputStream
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tracing.trace
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset

/**
 * Wrappers for UiAutomation.executeShellCommand to handle compat behavior, and add additional
 * features like script execution (with piping), stdin/stderr.
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object Shell {
    /**
     * Returns true if the line from ps output contains the given process/package name.
     *
     * NOTE: On API 25 and earlier, the processName of unbundled executables will include the
     * relative path they were invoked from:
     *
     * ```
     * root      10065 10061 14848  3932  poll_sched 7bcaf1fc8c S /data/local/tmp/tracebox
     * root      10109 1     11552  1140  poll_sched 78c86eac8c S ./tracebox
     * ```
     *
     * On higher API levels, the process name will simply be e.g. "tracebox".
     *
     * As this function is also used for package names (which never have a leading `/`), we
     * simply check for either.
     */
    private fun psLineContainsProcess(psOutputLine: String, processName: String): Boolean {
        return psOutputLine.endsWith(" $processName") || psOutputLine.endsWith("/$processName")
    }

    fun connectUiAutomation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ShellImpl // force initialization
        }
    }

    /**
     * Run a command, and capture stdout
     *
     * Below L, returns null
     */
    fun optionalCommand(command: String): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            executeCommand(command)
        } else {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun executeCommand(command: String): String {
        return ShellImpl.executeCommand(command)
    }

    /**
     * Function for reading shell-accessible proc files, like scaling_max_freq, which can't be
     * read directly by the app process.
     */
    fun catProcFileLong(path: String): Long? {
        return optionalCommand("cat $path")
            ?.trim()
            ?.run {
                try {
                    toLong()
                } catch (exception: NumberFormatException) {
                    // silently catch exception, as it may be not readable (e.g. due to offline)
                    null
                }
            }
    }

    @RequiresApi(21)
    fun chmodExecutable(absoluteFilePath: String) {
        if (Build.VERSION.SDK_INT >= 23) {
            ShellImpl.executeCommand("chmod +x $absoluteFilePath")
        } else {
            // chmod with support for +x only added in API 23
            // While 777 is technically more permissive, this is only used for scripts and temporary
            // files in tests, so we don't worry about permissions / access here
            ShellImpl.executeCommand("chmod 777 $absoluteFilePath")
        }
    }

    @RequiresApi(21)
    fun moveToTmpAndMakeExecutable(src: String, dst: String) {
        // Note: we don't check for return values from the below, since shell based file
        // permission errors generally crash our process.
        ShellImpl.executeCommand("cp $src $dst")
        chmodExecutable(dst)
    }

    /**
     * Writes the inputStream to an executable file with the given name in `/data/local/tmp`
     */
    @RequiresApi(21)
    fun createRunnableExecutable(name: String, inputStream: InputStream): String {
        // dirUsableByAppAndShell is writable, but we can't execute there (as of Q),
        // so we copy to /data/local/tmp
        val externalDir = Outputs.dirUsableByAppAndShell
        val writableExecutableFile = File.createTempFile(
            /* prefix */ "temporary_$name",
            /* suffix */ null,
            /* directory */ externalDir
        )
        val runnableExecutablePath = "/data/local/tmp/$name"

        try {
            writableExecutableFile.outputStream().use {
                inputStream.copyTo(it)
            }
            moveToTmpAndMakeExecutable(
                src = writableExecutableFile.absolutePath,
                dst = runnableExecutablePath
            )
        } finally {
            writableExecutableFile.delete()
        }

        return runnableExecutablePath
    }

    /**
     * Returns true if the shell session is rooted, and thus root commands can be run (e.g. atrace
     * commands with root-only tags)
     */
    @RequiresApi(21)
    fun isSessionRooted(): Boolean {
        return ShellImpl.executeCommand("getprop service.adb.root").trim() == "1"
    }

    /**
     * Convenience wrapper around [UiAutomation.executeShellCommand()] which enables redirects,
     * piping, and all other shell script functionality.
     *
     * Unlike [UiDevice.executeShellCommand()], this method supports arbitrary multi-line shell
     * expressions, as it creates and executes a shell script in `/data/local/tmp/`.
     *
     * Note that shell scripting capabilities differ based on device version. To see which utilities
     * are available on which platform versions,see
     * [Android's shell and utilities](https://android.googlesource.com/platform/system/core/+/master/shell_and_utilities/README.md#)
     *
     * @param script Script content to run
     * @param stdin String to pass in as stdin to first command in script
     *
     * @return Stdout string
     */
    @RequiresApi(21)
    fun executeScript(script: String, stdin: String? = null): String {
        return ShellImpl.executeScript(script, stdin, false).first
    }

    data class Output(val stdout: String, val stderr: String)

    /**
     * Convenience wrapper around [UiAutomation.executeShellCommand()] which enables redirects,
     * piping, and all other shell script functionality, and which captures stderr of last command.
     *
     * Unlike [UiDevice.executeShellCommand()], this method supports arbitrary multi-line shell
     * expressions, as it creates and executes a shell script in `/data/local/tmp/`.
     *
     * Note that shell scripting capabilities differ based on device version. To see which utilities
     * are available on which platform versions,see
     * [Android's shell and utilities](https://android.googlesource.com/platform/system/core/+/master/shell_and_utilities/README.md#)
     *
     * @param script Script content to run
     * @param stdin String to pass in as stdin to first command in script
     *
     * @return ShellOutput, including stdout of full script, and stderr of last command.
     */
    @RequiresApi(21)
    fun executeScriptWithStderr(
        script: String,
        stdin: String? = null
    ): Output {
        return ShellImpl.executeScript(
            script = script,
            stdin = stdin,
            includeStderr = true
        ).run {
            Output(first, second!!)
        }
    }

    @RequiresApi(21)
    fun isPackageAlive(packageName: String): Boolean {
        return getPidsForProcess(packageName).isNotEmpty()
    }

    @RequiresApi(21)
    fun getPidsForProcess(processName: String): List<Int> {
        if (Build.VERSION.SDK_INT >= 24) {
            // On API 23 (first version to offer it) we observe that 'pidof'
            // returns list of all processes :|
            return executeCommand("pidof $processName")
                .trim()
                .split(Regex("\\s+"))
                .filter { it.isNotEmpty() }
                .map {
                    it.toInt()
                }
        }

        // Can't use ps -A on older platforms, arg isn't supported.
        // Can't simply run ps, since it gets truncated
        return executeScript("ps | grep $processName")
            .split(Regex("\r?\n"))
            .map { it.trim() }
            .filter { psLineContainsProcess(psOutputLine = it, processName = processName) }
            .map {
                // map to int - split, and take 2nd column (PID)
                it.split(Regex("\\s+"))[1]
                    .toInt()
            }
    }

    /**
     * Checks if a process is alive, given a specified pid **and** process name.
     *
     * Both must match in order to return true.
     */
    @RequiresApi(21)
    fun isProcessAlive(pid: Int, processName: String): Boolean {
        return executeCommand("ps $pid")
            .split(Regex("\r?\n"))
            .any { psLineContainsProcess(psOutputLine = it, processName = processName) }
    }

    @RequiresApi(21)
    data class ProcessPid(val processName: String, val pid: Int) {
        fun isAlive() = isProcessAlive(pid, processName)
    }

    @RequiresApi(21)
    fun terminateProcessesAndWait(
        waitPollPeriodMs: Long,
        waitPollMaxCount: Int,
        processName: String
    ) {
        val processes = getPidsForProcess(processName).map { pid ->
            ProcessPid(pid = pid, processName = processName)
        }
        terminateProcessesAndWait(
            waitPollPeriodMs = waitPollPeriodMs,
            waitPollMaxCount = waitPollMaxCount,
            *processes.toTypedArray()
        )
    }

    @RequiresApi(21)
    fun terminateProcessesAndWait(
        waitPollPeriodMs: Long,
        waitPollMaxCount: Int,
        vararg processes: ProcessPid
    ) {
        processes.forEach {
            val stopOutput = executeCommand("kill -TERM ${it.pid}")
            Log.d(BenchmarkState.TAG, "kill -TERM command output - $stopOutput")
        }

        var runningProcesses = processes.toList()
        repeat(waitPollMaxCount) {
            runningProcesses = runningProcesses.filter { isProcessAlive(it.pid, it.processName) }
            if (runningProcesses.isEmpty()) {
                return
            }
            userspaceTrace("wait for $runningProcesses to die") {
                SystemClock.sleep(waitPollPeriodMs)
            }
            Log.d(BenchmarkState.TAG, "Waiting $waitPollPeriodMs ms for $runningProcesses to die")
        }
        throw IllegalStateException("Failed to stop $runningProcesses")
    }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private object ShellImpl {
    init {
        require(Looper.getMainLooper().thread != Thread.currentThread()) {
            "ShellImpl must not be initialized on the UI thread - UiAutomation must not be " +
                "connected on the main thread!"
        }
    }

    private val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation

    /**
     * Reimplementation of UiAutomator's Device.executeShellCommand,
     * to avoid the UiAutomator dependency
     */
    fun executeCommand(cmd: String): String {
        val parcelFileDescriptor = uiAutomation.executeShellCommand(cmd)
        AutoCloseInputStream(parcelFileDescriptor).use { inputStream ->
            return inputStream.readBytes().toString(Charset.defaultCharset())
        }
    }

    fun executeScript(
        script: String,
        stdin: String?,
        includeStderr: Boolean
    ): Pair<String, String?> {
        // dirUsableByAppAndShell is writable, but we can't execute there (as of Q),
        // so we copy to /data/local/tmp
        val externalDir = Outputs.dirUsableByAppAndShell
        val writableScriptFile = File.createTempFile("temporaryScript", ".sh", externalDir)
        val runnableScriptPath = "/data/local/tmp/" + writableScriptFile.name

        // only create/read/delete stdin/stderr files if they are needed
        val stdinFile = stdin?.run {
            File.createTempFile("temporaryStdin", null, externalDir)
        }
        val stderrPath = if (includeStderr) {
            // we use a modified runnableScriptPath (as opposed to externalDir) because some shell
            // commands fail to redirect stderr to externalDir (notably, `am start`).
            // This also means we need to `cat` the file to read it, and `rm` to remove it.
            runnableScriptPath + "_stderr"
        } else {
            null
        }

        try {
            var scriptText: String = script
            if (stdinFile != null) {
                stdinFile.writeText(stdin)
                scriptText = "cat ${stdinFile.absolutePath} | $scriptText"
            }
            if (stderrPath != null) {
                scriptText = "$scriptText 2> $stderrPath"
            }
            writableScriptFile.writeText(scriptText)

            // Note: we don't check for return values from the below, since shell based file
            // permission errors generally crash our process.
            executeCommand("cp ${writableScriptFile.absolutePath} $runnableScriptPath")
            Shell.chmodExecutable(runnableScriptPath)

            val stdout = trace("executeCommand") { executeCommand(runnableScriptPath) }
            val stderr = stderrPath?.run { executeCommand("cat $stderrPath") }

            return Pair(stdout, stderr)
        } finally {
            stdinFile?.delete()
            stderrPath?.run {
                executeCommand("rm $stderrPath")
            }
            writableScriptFile.delete()
            executeCommand("rm $runnableScriptPath")
        }
    }
}