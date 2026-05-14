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

import android.annotation.SuppressLint
import android.os.Build
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.ParcelFileDescriptor.AutoCloseInputStream
import android.os.SystemClock
import android.util.Log
import androidx.annotation.CheckResult
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tracing.trace
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import kotlin.random.Random
import kotlin.random.nextUInt

/**
 * Wrappers for UiAutomation.executeShellCommand to handle compat behavior, and add additional
 * features like script execution (with piping), stdin/stderr.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object Shell {

    private const val COMPILATION_PROFILE_UNKNOWN = "unknown"

    /**
     * Returns true if the line from ps output contains the given process/package name.
     *
     * NOTE: On API 25 and earlier, the processName of unbundled executables will include the
     * relative path they were invoked from:
     * ```
     * root      10065 10061 14848  3932  poll_sched 7bcaf1fc8c S /data/local/tmp/tracebox
     * root      10109 1     11552  1140  poll_sched 78c86eac8c S ./tracebox
     * ```
     *
     * On higher API levels, the process name will simply be e.g. "tracebox".
     *
     * As this function is also used for package names (which never have a leading `/`), we simply
     * check for either.
     */
    internal fun psLineContainsProcess(psOutputLine: String, processName: String): Boolean {
        val processLabel = psOutputLine.substringAfterLast(" ")
        return processLabel == processName || // exact match
            processLabel.startsWith("$processName:") || // app subprocess
            processLabel.endsWith("/$processName") // executable with relative path
    }

    /**
     * Equivalent of [Shell.psLineContainsProcess], but to be used with full process name (e.g. from
     * pgrep)
     */
    internal fun fullProcessNameMatchesProcess(
        fullProcessName: String,
        processName: String,
    ): Boolean {
        return fullProcessName == processName || // exact match
            fullProcessName.startsWith("$processName:") || // app subprocess
            fullProcessName.endsWith("/$processName") // executable with relative path
    }

    fun connectUiAutomation() {
        @Suppress("UNUSED_EXPRESSION") ShellImpl // force initialization
    }

    /**
     * Function for reading shell-accessible proc files, like scaling_max_freq, which can't be read
     * directly by the app process.
     */
    fun catProcFileLong(path: String): Long? {
        return executeScriptCaptureStdoutStderr("cat $path").stdout.trim().run {
            try {
                toLong()
            } catch (_: NumberFormatException) {
                // silently catch exception, as it may be not readable (e.g. due to offline)
                null
            }
        }
    }

    /**
     * Get a checksum for a given path
     *
     * Note: Does not check for stderr, as this method is used during ShellImpl init, so stderr not
     * yet available
     */
    internal fun getChecksum(path: String): String {
        val sum = md5sum(path)
        if (sum.isBlank()) {
            if (!ShellImpl.isSessionRooted) {
                val lsOutput = ShellImpl.executeCommandUnsafe("ls -l $path")
                throw IllegalStateException(
                    "Checksum for $path was blank. Adb session is not rooted, if root owns file, " +
                        "you may need to \"adb root\" and delete the file: $lsOutput"
                )
            } else {
                throw IllegalStateException("Checksum for $path was blank.")
            }
        }
        return sum
    }

    /** Waits for the file size of the [path] to be table for at least [stableIterations]. */
    @SuppressLint("BanThreadSleep") // Need polling to wait for file content to be flushed
    fun waitForFileFlush(
        path: String,
        stableIterations: Int,
        maxInitialFlushWaitIterations: Int,
        maxStableFlushWaitIterations: Int,
        pollDurationMs: Long,
        triggerFileFlush: () -> Unit,
    ) {
        var lastKnownSize = getFileSizeUnsafe(path)

        triggerFileFlush()

        // first, wait for initial dump from flush, which can be a long amount of time
        var currentSize = getFileSizeUnsafe(path)
        var iteration = 0
        while (iteration < maxInitialFlushWaitIterations && currentSize == lastKnownSize) {
            Thread.sleep(pollDurationMs)
            currentSize = getFileSizeUnsafe(path)
            iteration++
        }

        // wait for stabilization, which should take much less time and happen quickly
        iteration = 0
        lastKnownSize = 0
        var stable = 0
        while (iteration < maxStableFlushWaitIterations) {
            currentSize = getFileSizeUnsafe(path)
            if (currentSize > 0) {
                if (currentSize == lastKnownSize) {
                    stable += 1
                    if (stable == stableIterations) {
                        break
                    }
                } else {
                    // reset
                    stable = 0
                    lastKnownSize = currentSize
                }
            }
            iteration += 1
            Thread.sleep(pollDurationMs)
        }
    }

    /** Gets the file size for a given path. */
    internal fun getFileSizeUnsafe(path: String): Long {
        // Using executeCommandUnsafe for perf reasons, but this API is still safe, given
        // we validate the outputs.
        val fileSize = ShellImpl.executeCommandUnsafe("stat -c %s $path").trim().toLongOrNull()
        require(fileSize != null) { "Unable to obtain file size for the file $path" }
        return fileSize
    }

    /**
     * Copy file and make executable
     *
     * Note: this operation does checksum validation of dst, since it's used during setup of the
     * shell script used to capture stderr, so stderr isn't available.
     */
    private fun moveToTmpAndMakeExecutable(src: String, dst: String) {
        if (UserInfo.isAdditionalUser) {
            val dstFile = ShellFile(dst).also { it.delete() }
            val srcFile = UserFile(src)
            srcFile.copyTo(dstFile)
        } else {
            ShellImpl.executeCommandUnsafe("cp $src $dst")
        }

        // Sets execution permissions on the script
        ShellImpl.executeCommandUnsafe("chmod +x $dst")

        // validate checksums instead of checking stderr, since it's not yet safe to
        // read from stderr. This detects the problem where root left a stale executable
        // that can't be modified by shell at the dst path
        val srcSum = getChecksum(path = src)
        val dstSum = getChecksum(path = dst)
        if (srcSum != dstSum) {
            throw IllegalStateException(
                "Failed to verify copied executable $dst, " +
                    "md5 sums $srcSum, $dstSum don't match. Check if root owns" +
                    " $dst and if so, delete it with `adb root`-ed shell session."
            )
        }
    }

    /**
     * Writes the inputStream to an executable file with the given name in `/data/local/tmp`
     *
     * Note: this operation does not validate command success, since it's used during setup of shell
     * scripting code used to parse stderr. This means callers should validate.
     */
    fun createRunnableExecutable(name: String, inputStream: InputStream): String {
        // dirUsableByAppAndShell is writable, but we can't execute there (as of Q),
        // so we copy to /data/local/tmp
        val writableExecutableFile =
            File.createTempFile(
                /* prefix */ "temporary_$name",
                /* suffix */ null,
                /* directory */ Outputs.dirUsableByAppAndShell,
            )
        val runnableExecutablePath = "/data/local/tmp/$name"

        try {
            writableExecutableFile.outputStream().use { inputStream.copyTo(it) }
            if (Outputs.forceFilesForShellAccessible) {
                // executable must be readable by shell to be moved, and for some reason
                // doesn't inherit shell readability from dirUsableByAppAndShell
                writableExecutableFile.setReadable(true, false)
            }
            moveToTmpAndMakeExecutable(
                src = writableExecutableFile.absolutePath,
                dst = runnableExecutablePath,
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
    fun isSessionRooted(): Boolean {
        return ShellImpl.isSessionRooted || ShellImpl.isSuAvailable
    }

    fun getprop(propertyName: String): String {
        return executeScriptCaptureStdout("getprop $propertyName").trim()
    }

    /**
     * Convenience wrapper around [android.app.UiAutomation.executeShellCommand] which adds
     * scripting functionality like piping and redirects, and which throws if stdout or stderr was
     * produced.
     *
     * Unlike `executeShellCommand()`, this method supports arbitrary multi-line shell expressions,
     * as it creates and executes a shell script in `/data/local/tmp/`.
     *
     * Note that shell scripting capabilities differ based on device version. To see which utilities
     * are available on which platform versions,see
     * [Android's shell and utilities](https://android.googlesource.com/platform/system/core/+/master/shell_and_utilities/README.md#)
     *
     * @param script Script content to run
     * @param stdin String to pass in as stdin to first command in script
     * @return Stdout string
     */
    fun executeScriptSilent(script: String, stdin: String? = null) {
        val output = executeScriptCaptureStdoutStderr(script, stdin)
        check(output.isBlank()) { "Expected no stdout/stderr from $script, saw $output" }
    }

    /**
     * Convenience wrapper around [android.app.UiAutomation.executeShellCommand] which adds
     * scripting functionality like piping and redirects, and which captures stdout and throws if
     * stderr was produced.
     *
     * Unlike `executeShellCommand()`, this method supports arbitrary multi-line shell expressions,
     * as it creates and executes a shell script in `/data/local/tmp/`.
     *
     * Note that shell scripting capabilities differ based on device version. To see which utilities
     * are available on which platform versions,see
     * [Android's shell and utilities](https://android.googlesource.com/platform/system/core/+/master/shell_and_utilities/README.md#)
     *
     * @param script Script content to run
     * @param stdin String to pass in as stdin to first command in script
     * @return Stdout string
     */
    @CheckResult
    fun executeScriptCaptureStdout(script: String, stdin: String? = null): String {
        val output = executeScriptCaptureStdoutStderr(script, stdin)
        check(output.stderr.isBlank()) { "Expected no stderr from $script, saw ${output.stderr}" }
        return output.stdout
    }

    internal fun parseCompilationMode(apiLevel: Int, dump: String): String {
        require(apiLevel >= 24)

        /**
         * Note that the actual string can take several forms, depending on API level and
         * potentially ABI as well.
         *
         * Emulators are known to have different structure than physical devices on the same API
         * level, this is potentially due to ABI.
         *
         * For this reason, we use a relatively lax matching system (only relying on prefix, equals,
         * and trailing bracket), and rely on tests to validate.
         */
        val modePrefix =
            when (apiLevel) {
                // lower API levels will sometimes have newlines within the compilation_filter=...
                // so we're happy to accept any whitespace within. whitespace in the capture is
                // filtered below
                in 24..27 -> ", compilation_filter=".toCharArray().joinToString("\\s*?")
                // haven't observed this on higher APIs :shrug:
                else -> "\\[status="
            }
        return "Dexopt state:.*?$modePrefix([^]]+?)]"
            .toRegex(RegexOption.DOT_MATCHES_ALL)
            .find(dump)
            ?.groups
            ?.get(1)
            ?.value
            ?.filter { !it.isWhitespace() } ?: COMPILATION_PROFILE_UNKNOWN
    }

    @CheckResult
    fun getCompilationMode(packageName: String): String {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) return "speed"
        val dump = executeScriptCaptureStdout("cmd package dump $packageName").trim()
        return parseCompilationMode(Build.VERSION.SDK_INT, dump)
    }

    /**
     * Returns one apk (or more, if multi-apk/bundle) path for the given package
     *
     * The result of `pm path <package>` is one or more lines like:
     * ```
     * package: </path/to/apk1>
     * package: </path/to/apk2>
     * ```
     *
     * Note - to test multi-apk behavior locally, you can build and install a module like
     * `benchmark:integration-tests:macrobenchmark-target` with the instructions below:
     * ```
     * ./gradlew benchmark:integ:macrobenchmark-target:bundleRelease
     * java -jar bundletool.jar build-apks --local-testing --bundle=../../out/androidx/benchmark/integration-tests/macrobenchmark-target/build/outputs/bundle/release/macrobenchmark-target-release.aab --output=out.apks --overwrite --ks=/path/to/androidx/frameworks/support/development/keystore/debug.keystore --connected-device --ks-key-alias=AndroidDebugKey --ks-pass=pass:android
     * java -jar bundletool.jar install-apks --apks=out.apks
     * ```
     */
    @CheckResult
    fun pmPath(packageName: String): List<String> {
        return executeScriptCaptureStdout("pm path $packageName").split("\n").mapNotNull {
            val delimiter = "package:"
            val index = it.indexOf(delimiter)
            if (index != -1) {
                it.substring(index + delimiter.length).trim()
            } else {
                null
            }
        }
    }

    data class Output(val stdout: String, val stderr: String) {
        /**
         * Returns true if both stdout and stderr are blank
         *
         * This can be used with silent-if-successful shell commands:
         * ```
         * check(Shell.executeScriptWithStderr("mv $src $dest").isBlank()) { "Oh no mv failed!" }
         * ```
         */
        fun isBlank(): Boolean = stdout.isBlank() && stderr.isBlank()
    }

    /**
     * Convenience wrapper around [android.app.UiAutomation.executeShellCommand] which adds
     * scripting functionality like piping and redirects, and which captures both stdout and stderr.
     *
     * Unlike `executeShellCommand()`, this method supports arbitrary multi-line shell expressions,
     * as it creates and executes a shell script in `/data/local/tmp/`.
     *
     * Note that shell scripting capabilities differ based on device version. To see which utilities
     * are available on which platform versions,see
     * [Android's shell and utilities](https://android.googlesource.com/platform/system/core/+/master/shell_and_utilities/README.md#)
     *
     * @param script Script content to run
     * @param stdin String to pass in as stdin to first command in script
     * @return Output object containing stdout and stderr of full script, and stderr of last command
     */
    @CheckResult
    fun executeScriptCaptureStdoutStderr(script: String, stdin: String? = null): Output {
        return trace("executeScript $script".take(127)) {
            ShellImpl.createShellScript(script = script, stdin = stdin).start().getOutputAndClose()
        }
    }

    /**
     * Direct execution of executeShellCommand which doesn't account for scripting functionality,
     * and doesn't capture stderr.
     *
     * Only use this function if you do not care about failure / errors.
     */
    @CheckResult
    fun executeCommandCaptureStdoutOnly(command: String): String {
        return ShellImpl.executeCommandUnsafe(command)
    }

    /**
     * Creates an executable shell script that can be started. Similar to
     * [Shell.executeScriptCaptureStdoutStderr] but allows deferring and caching script execution.
     *
     * @param script Script content to run
     * @param stdin String to pass in as stdin to first command in script
     * @return ShellScript that can be started.
     */
    fun createShellScript(script: String, stdin: String? = null): ShellScript {
        return ShellImpl.createShellScript(script = script, stdin = stdin)
    }

    fun isPackageAlive(packageName: String): Boolean {
        return getPidsForProcess(packageName).isNotEmpty()
    }

    fun getPidsForProcess(processName: String): List<Int> {
        return pgrepLF(pattern = processName).mapNotNull { runningProcess ->
            // aggressive safety - ensure target isn't subset of another running package
            if (fullProcessNameMatchesProcess(runningProcess.processName, processName)) {
                runningProcess.pid
            } else {
                null
            }
        }
    }

    /**
     * pgrep -l -f <pattern>
     *
     * pgrep is *fast*, way faster than ps | grep, but requires API 23
     *
     * -l, --list-name list PID and process name -f, --full use full process name to match
     *
     * @return List of processes - pid & full process name
     */
    fun pgrepLF(pattern: String): List<ProcessPid> {
        // Note: we use the unsafe variant for performance, since this is a
        // common operation, and pgrep is stable after API 23 see [ShellBehaviorTest#pgrep]
        val apiSpecificArgs =
            setOfNotNull(
                    // aosp/3507001 -> needed to print full command line (so full package name)
                    if (Build.VERSION.SDK_INT >= 36) "-a" else null
                )
                .joinToString(" ")

        return ShellImpl.executeCommandUnsafe("pgrep -l -f $apiSpecificArgs $pattern")
            .split(Regex("\r?\n"))
            .filter { it.isNotEmpty() }
            .map {
                val (pidString, process) = it.trim().split(" ")
                ProcessPid(process, pidString.toInt())
            }
    }

    fun getRunningPidsAndProcessesForPackage(packageName: String): List<ProcessPid> {
        require(!packageName.contains(":")) { "Package $packageName must not contain ':'" }
        return pgrepLF(pattern = packageName.replace(".", "\\.")).filter {
            it.processName == packageName || it.processName.startsWith("$packageName:")
        }
    }

    fun getRunningProcessesForPackage(packageName: String): List<String> {
        require(!packageName.contains(":")) { "Package $packageName must not contain ':'" }
        return getRunningPidsAndProcessesForPackage(packageName).map { it.processName }
    }

    /**
     * Checks if a process is alive, given a specified pid **and** process name.
     *
     * Both must match in order to return true.
     */
    fun isProcessAlive(pid: Int, processName: String): Boolean {
        // unsafe, since this behavior is well tested, and performance here is important
        // See [ShellBehaviorTest#ps]
        return ShellImpl.executeCommandUnsafe("ps $pid").split(Regex("\r?\n")).any {
            psLineContainsProcess(psOutputLine = it, processName = processName)
        }
    }

    data class ProcessPid(val processName: String, val pid: Int) {
        fun isAlive() = isProcessAlive(pid, processName)
    }

    fun killTerm(processes: List<ProcessPid>) {
        processes.forEach {
            // NOTE: we don't fail on stdout/stderr, since killing processes can be racy, and
            // killing one can kill others. Instead, validation of process death happens below.
            val stopOutput = executeScriptCaptureStdoutStderr("kill -TERM ${it.pid}")
            Log.d(BenchmarkState.TAG, "kill -TERM command output - $stopOutput")
        }
    }

    private const val DEFAULT_KILL_POLL_PERIOD_MS = 50L
    private const val DEFAULT_KILL_POLL_MAX_COUNT = 100

    fun killProcessesAndWait(
        processName: String,
        waitPollPeriodMs: Long = DEFAULT_KILL_POLL_PERIOD_MS,
        waitPollMaxCount: Int = DEFAULT_KILL_POLL_MAX_COUNT,
        onFailure: (String) -> Unit = { errorMessage -> throw IllegalStateException(errorMessage) },
        processKiller: (List<ProcessPid>) -> Unit = ::killTerm,
    ) {
        val processes =
            getPidsForProcess(processName).map { pid ->
                ProcessPid(pid = pid, processName = processName)
            }
        if (!processes.isEmpty()) {
            killProcessesAndWait(
                processes,
                waitPollPeriodMs = waitPollPeriodMs,
                waitPollMaxCount = waitPollMaxCount,
                onFailure,
                processKiller,
            )
        } else {
            Log.d(BenchmarkState.TAG, "No processes for name $processName, skipping kill")
        }
    }

    fun killProcessesAndWait(
        processes: List<ProcessPid>,
        waitPollPeriodMs: Long = DEFAULT_KILL_POLL_PERIOD_MS,
        waitPollMaxCount: Int = DEFAULT_KILL_POLL_MAX_COUNT,
        onFailure: (String) -> Unit = { errorMessage -> throw IllegalStateException(errorMessage) },
        processKiller: (List<ProcessPid>) -> Unit = ::killTerm,
    ) {
        var runningProcesses = processes.toList()
        processKiller(runningProcesses)
        repeat(waitPollMaxCount) {
            runningProcesses = runningProcesses.filter { isProcessAlive(it.pid, it.processName) }
            if (runningProcesses.isEmpty()) {
                return
            }
            inMemoryTrace("wait for $runningProcesses to die") {
                SystemClock.sleep(waitPollPeriodMs)
            }
            Log.d(BenchmarkState.TAG, "Waiting $waitPollPeriodMs ms for $runningProcesses to die")
        }
        onFailure.invoke("Failed to stop $runningProcesses")
    }

    fun pathExists(absoluteFilePath: String) =
        if (UserInfo.isAdditionalUser) {
            VirtualFile.fromPath(absoluteFilePath).ls().first() == absoluteFilePath
        } else {
            ShellImpl.executeCommandUnsafe("ls $absoluteFilePath").trim() == absoluteFilePath
        }

    // Broadcast results are parsed this way.
    private val broadcastRegex =
        Regex("Broadcast completed:\\s*result\\s*=\\s*(\\d+)\\s*(,\\s*data\\s*=\\s*\"(.*)\")?")

    /**
     * Invokes `am broadcast broadcastArguments` using the shell user.
     *
     * @return a [Pair] that optionally contains the result code and data. Note: A result code of
     *   `null` typically means that the broadcast was unsuccessful.
     */
    fun amBroadcast(broadcastArguments: String): Pair<Int?, String?> {
        // unsafe here for perf, since we validate the return value so we don't need to check stderr
        val response = ShellImpl.executeCommandUnsafe("am broadcast $broadcastArguments")
        Log.d(BenchmarkState.TAG, "Broadcast response: $response")
        val line =
            response.lines().firstOrNull { it.contains(broadcastRegex) } ?: return (null to null)
        val matcher = broadcastRegex.matchEntire(line) ?: return (null to null)
        // Group 1 is the result code
        val code = matcher.groupValues[1].toIntOrNull()
        // Group 3 is data
        val data = matcher.groupValues.getOrElse(3) { null }
        return code to data
    }

    fun disablePackages(appPackages: List<String>) {
        // Additionally use `am force-stop` to force JobScheduler to drop all jobs.
        val command =
            appPackages.joinToString(separator = "\n") { appPackage ->
                """
                am force-stop $appPackage
                pm disable-user $appPackage
            """
                    .trimIndent()
            }

        val output = executeScriptCaptureStdoutStderr(command)
        if (output.stderr.isNotBlank()) {
            Log.d(BenchmarkState.TAG, "disabling packages failed, stderr: ${output.stderr}")
        }
    }

    fun enablePackages(appPackages: List<String>) {
        val command =
            appPackages.joinToString(separator = "\n") { appPackage -> "pm enable $appPackage" }

        val output = executeScriptCaptureStdoutStderr(command)
        if (output.stderr.isNotBlank()) {
            Log.d(BenchmarkState.TAG, "enabling packages failed, stderr: ${output.stderr}")
        }
    }

    @RequiresApi(24)
    fun disableBackgroundDexOpt() {
        // Cancels the active job if any
        ShellImpl.executeCommandUnsafe("cmd package bg-dexopt-job --cancel")
        ShellImpl.executeCommandUnsafe("cmd package bg-dexopt-job --disable")
    }

    @RequiresApi(24)
    fun enableBackgroundDexOpt() {
        ShellImpl.executeCommandUnsafe("cmd package bg-dexopt-job --enable")
    }

    fun isSELinuxEnforced(): Boolean {
        return when (val value = executeScriptCaptureStdout("getenforce").trim()) {
            "Permissive" -> false
            "Disabled" -> false
            "Enforcing" -> true
            else -> throw IllegalStateException("unexpected result from getenforce: $value")
        }
    }

    fun cp(from: String, to: String) {
        if (UserInfo.isAdditionalUser) {
            val fromFile = VirtualFile.fromPath(from)
            val toFile = VirtualFile.fromPath(to)
            toFile.delete()
            fromFile.copyTo(toFile)
        } else {
            executeScriptSilent("cp $from $to")
        }
    }

    fun mv(from: String, to: String) {
        if (UserInfo.isAdditionalUser) {
            val fromFile = VirtualFile.fromPath(from)
            val toFile = VirtualFile.fromPath(to)
            toFile.delete()
            fromFile.moveTo(toFile)
        } else {
            executeScriptSilent("mv $from $to")
        }
    }

    fun rm(path: String) {
        if (UserInfo.isAdditionalUser) {
            VirtualFile.fromPath(path).delete()
        } else {
            executeScriptSilent("rm -f $path")
        }
    }

    fun chmod(path: String, args: String) {
        if (UserInfo.isAdditionalUser) {
            VirtualFile.fromPath(path).chmod(args)
        } else {
            executeScriptSilent("chmod $args $path")
        }
    }

    fun mkdir(path: String) {
        if (UserInfo.isAdditionalUser) {
            VirtualFile.fromPath(path).mkdir()
        } else {
            executeScriptSilent("mkdir -p $path")
        }
    }

    private fun md5sum(path: String): String {
        return if (UserInfo.isAdditionalUser) {
            VirtualFile.fromPath(path).md5sum()
        } else {
            ShellImpl.executeCommandUnsafe("md5sum $path").substringBefore(" ")
        }
    }
}

private object ShellImpl {
    init {
        require(Looper.getMainLooper().thread != Thread.currentThread()) {
            "ShellImpl must not be initialized on the UI thread - UiAutomation must not be " +
                "connected on the main thread!"
        }
    }

    private val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation

    /** When true, the session is already rooted and all commands run as root by default. */
    var isSessionRooted = false

    /** When true, su is available for running commands and scripts as root. */
    var isSuAvailable = false

    init {
        // b/268107648: UiAutomation always runs on user 0 so shell cannot access other user data.
        // This behavior was introduced with FUSE on api 30. Before then, shell could access any
        // user data.
        if (UserInfo.currentUserId > 0 && Build.VERSION.SDK_INT in 30 until 31) {
            throw IllegalStateException(
                "Benchmark and Baseline Profile generation are not currently " +
                    "supported on AAOS and multiuser environment when a secondary user is " +
                    "selected, on api 30"
            )
        }
        // These variables are used in executeCommand and executeScript, so we keep them as var
        // instead of val and use a separate initializer
        isSessionRooted = executeCommandUnsafe("id").contains("uid=0(root)")
        // use a script below, since direct `su` command failure brings down this process
        // on some API levels (and can fail even on userdebug builds)
        isSuAvailable =
            createShellScript(script = "su root id", stdin = null)
                .start()
                .getOutputAndClose()
                .stdout
                .contains("uid=0(root)")
    }

    /**
     * Reimplementation of UiAutomator's Device.executeShellCommand, to avoid the UiAutomator
     * dependency, and add tracing
     *
     * NOTE: this does not capture stderr, and is thus unsafe. Only use this when the more complex
     * Shell.executeScript APIs aren't appropriate (such as in their implementation)
     */
    fun executeCommandUnsafe(cmd: String): String =
        trace("executeCommand $cmd".take(127)) {
            return@trace executeCommandNonBlockingUnsafe(cmd).fullyReadInputStream()
        }

    fun executeCommandNonBlockingUnsafe(cmd: String): ParcelFileDescriptor =
        trace("executeCommandNonBlocking $cmd".take(127)) {
            return@trace uiAutomation.executeShellCommand(
                if (!isSessionRooted && isSuAvailable) {
                    "su root $cmd"
                } else {
                    cmd
                }
            )
        }

    fun createShellScript(script: String, stdin: String?): ShellScript =
        trace("createShellScript") {

            // dirUsableByAppAndShell is writable, but we can't execute there (as of Q),
            // so we copy to /data/local/tmp
            val scriptName = "temporaryScript_${Random.nextUInt()}.sh"

            val (scriptContentFile, stdInFile) =
                if (UserInfo.isAdditionalUser) {
                    Pair(
                        ShellFile.inTempDir(scriptName).apply { writeText(script) },
                        stdin?.let {
                            ShellFile.inTempDir("${scriptName}_stdin").apply { writeText(it) }
                        },
                    )
                } else {
                    Pair(
                        UserFile.inOutputsDir(scriptName).apply { writeText(script) },
                        stdin?.let { input ->
                            UserFile.inOutputsDir("${scriptName}_stdin").apply { writeText(input) }
                        },
                    )
                }

            // we use a path on /data/local/tmp (as opposed to externalDir) because some shell
            // commands fail to redirect stderr to externalDir (notably, `am start`).
            // This also means we need to `cat` the file to read it, and `rm` to remove it.
            val stderrPath = "/data/local/tmp/${scriptName}_stderr"

            try {
                return@trace ShellScript(
                    stdinFile = stdInFile,
                    scriptContentFile = scriptContentFile,
                    stderrPath = stderrPath,
                )
            } catch (e: Exception) {
                throw Exception("Can't create shell script", e)
            }
        }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ShellScript
internal constructor(
    private val stdinFile: VirtualFile?,
    private val scriptContentFile: VirtualFile,
    private val stderrPath: String,
) {
    private var cleanedUp: Boolean = false

    /**
     * Starts the shell script previously created.
     *
     * @return a [StartedShellScript] that contains streams to read output streams.
     */
    fun start(): StartedShellScript =
        trace("ShellScript#start") {
            val stdoutDescriptor =
                ShellImpl.executeCommandNonBlockingUnsafe(
                    scriptWrapperCommand(
                        scriptContentPath = scriptContentFile.absolutePath,
                        stderrPath = stderrPath,
                        stdinPath = stdinFile?.absolutePath,
                    )
                )
            val stderrDescriptorFn =
                stderrPath.run { { ShellImpl.executeCommandUnsafe("cat $stderrPath") } }

            return@trace StartedShellScript(
                stdoutDescriptor = stdoutDescriptor,
                stderrDescriptorFn = stderrDescriptorFn,
                cleanUpBlock = ::cleanUp,
            )
        }

    /** Manually clean up the shell script temporary files from the temp folder. */
    fun cleanUp() =
        trace("ShellScript#cleanUp") {
            if (cleanedUp) {
                return@trace
            }

            // NOTE: while we could theoretically remove some of these files from the script, this
            // isn't
            // safe when the script is called multiple times, expecting the intermediates to remain.
            // We need a rm to clean up the stderr file anyway (b/c it's not ready until stdout is
            // complete), so we just delete everything here, all at once.
            ShellImpl.executeCommandUnsafe(
                "rm -f " +
                    listOfNotNull(
                            stderrPath,
                            scriptContentFile.absolutePath,
                            stdinFile?.absolutePath,
                        )
                        .joinToString(" ")
            )
            cleanedUp = true
        }

    companion object {
        /** Usage args: ```path/to/shellWrapper.sh <scriptFile> <stderrFile> [inputFile]``` */
        private val scriptWrapperPath =
            Shell.createRunnableExecutable(
                // use separate paths to prevent access errors after `adb unroot`
                if (ShellImpl.isSessionRooted) "shellWrapper_root.sh" else "shellWrapper.sh",
                """
                ### shell script which passes in stdin as needed, and captures stderr in a file
                # $1 == script content (not executable)
                # $2 == stderr
                # $3 == stdin (optional)
                if [[ $3 -eq "0" ]]; then
                    /system/bin/sh $1 2> $2
                else
                    cat $3 | /system/bin/sh $1 2> $2
                fi
                """
                    .trimIndent()
                    .byteInputStream(),
            )

        fun scriptWrapperCommand(
            scriptContentPath: String,
            stderrPath: String,
            stdinPath: String?,
        ): String =
            listOfNotNull(scriptWrapperPath, scriptContentPath, stderrPath, stdinPath)
                .joinToString(" ")
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class StartedShellScript
internal constructor(
    private val stdoutDescriptor: ParcelFileDescriptor,
    private val stderrDescriptorFn: (() -> (String)),
    private val cleanUpBlock: () -> Unit,
) : Closeable {

    /** Returns a [Sequence] of [String] containing the lines written by the process to stdOut. */
    fun stdOutLineSequence(): Sequence<String> =
        AutoCloseInputStream(stdoutDescriptor).bufferedReader().lineSequence()

    /** Cleans up this shell script. */
    override fun close() = cleanUpBlock()

    /** Reads the full process output and cleans up the generated script */
    fun getOutputAndClose(): Shell.Output {
        val output =
            Shell.Output(
                stdout = stdoutDescriptor.fullyReadInputStream(),
                stderr = stderrDescriptorFn.invoke(),
            )
        close()
        return output
    }
}

internal fun ParcelFileDescriptor.fullyReadInputStream(): String {
    AutoCloseInputStream(this).use { inputStream ->
        return inputStream.readBytes().toString(Charset.defaultCharset())
    }
}
