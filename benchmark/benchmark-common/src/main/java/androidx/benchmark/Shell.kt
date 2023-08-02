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

    /**
     * Equivalent of [psLineContainsProcess], but to be used with full process name string
     * (e.g. from pgrep)
     */
    private fun fullProcessNameMatchesProcess(
        fullProcessName: String,
        processName: String
    ): Boolean {
        return fullProcessName == processName || fullProcessName.endsWith("/$processName")
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
     * Returns true if the shell session is rooted or su is usable, and thus root commands can be
     * run (e.g. atrace commands with root-only tags)
     */
    @RequiresApi(21)
    fun isSessionRooted(): Boolean {
        return ShellImpl.isSessionRooted || ShellImpl.isSuAvailable
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
        if (Build.VERSION.SDK_INT >= 23) {
            return pgrepLF(pattern = processName)
                .mapNotNull { (pid, fullProcessName) ->
                    // aggressive safety - ensure target isn't subset of another running package
                    if (fullProcessNameMatchesProcess(fullProcessName, processName)) {
                        pid
                    } else {
                        null
                    }
                }
        }

        // NOTE: `pidof $processName` would work too, but filtering by process
        // (the whole point of the command) doesn't work pre API 24

        // Can't use ps -A pre API 26, arg isn't supported.
        // Grep device side, since ps output by itself gets truncated
        // NOTE: `ps | grep` is slow (multiple seconds), so avoid whenever possible!
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
     * pgrep -l -f <pattern>
     *
     * pgrep is *fast*, way faster than ps | grep, but requires API 23
     *
     * -l, --list-name           list PID and process name
     * -f, --full                use full process name to match
     *
     * @return List of processes - pid & full process name
     */
    @RequiresApi(23)
    private fun pgrepLF(pattern: String): List<Pair<Int, String>> {
        return executeCommand("pgrep -l -f $pattern")
            .split(Regex("\r?\n"))
            .filter { it.isNotEmpty() }
            .map {
                val (pidString, process) = it.trim().split(" ")
                Pair(pidString.toInt(), process)
            }
    }

    @RequiresApi(21)
    fun getRunningProcessesForPackage(packageName: String): List<String> {
        require(!packageName.contains(":")) { "Package $packageName must not contain ':'" }

        // pgrep is nice and fast, but requires API 23
        if (Build.VERSION.SDK_INT >= 23) {
            return pgrepLF(pattern = packageName)
                .mapNotNull { (_, process) ->
                    // aggressive safety - ensure target isn't subset of another running package
                    if (process == packageName || process.startsWith("$packageName:")) {
                        process
                    } else {
                        null
                    }
                }
        }

        // Grep device side, since ps output by itself gets truncated
        // NOTE: Can't use ps -A pre API 26, arg isn't supported, but would need
        // to pass it on 26 to see all processes.
        // NOTE: `ps | grep` is slow (multiple seconds), so avoid whenever possible!
        return executeScript("ps | grep $packageName")
            .split(Regex("\r?\n"))
            .map {
                // get process name from end
                it.substringAfterLast(" ")
            }
            .filter {
                // allow primary or sub process
                it == packageName || it.startsWith("$packageName:")
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

    @RequiresApi(21)
    fun pathExists(absoluteFilePath: String): Boolean {
        return executeCommand("ls $absoluteFilePath").trim() == absoluteFilePath
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
     * When true, the session is already rooted and all commands run as root by default.
     */
    var isSessionRooted = false

    /**
     * When true, su is available for running commands and scripts as root.
     */
    var isSuAvailable = false

    init {
        // These variables are used in executeCommand and executeScript, so we keep them as var
        // instead of val and use a separate initializer
        isSessionRooted = executeCommand("id").contains("uid=0(root)")
        isSuAvailable = executeScript(
            "su root id",
            null,
            false
        ).first.contains("uid=0(root)")
    }

    /**
     * Reimplementation of UiAutomator's Device.executeShellCommand,
     * to avoid the UiAutomator dependency, and add tracing
     */
    fun executeCommand(cmd: String): String = trace("executeCommand $cmd".take(127)) {
        val parcelFileDescriptor = uiAutomation.executeShellCommand(
            if (!isSessionRooted && isSuAvailable) {
                "su root $cmd"
            } else {
                cmd
            }
        )
        AutoCloseInputStream(parcelFileDescriptor).use { inputStream ->
            return@trace inputStream.readBytes().toString(Charset.defaultCharset())
        }
    }

    fun executeScript(
        script: String,
        stdin: String?,
        includeStderr: Boolean
    ): Pair<String, String?> = trace("executeScript $script".take(127)) {
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

            val stdout = executeCommand(runnableScriptPath)
            val stderr = stderrPath?.run { executeCommand("cat $stderrPath") }

            return@trace Pair(stdout, stderr)
        } finally {
            stdinFile?.delete()
            writableScriptFile.delete()

            if (stderrPath != null) {
                executeCommand("rm $stderrPath $runnableScriptPath")
            } else {
                executeCommand("rm $runnableScriptPath")
            }
        }
    }
}