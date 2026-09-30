/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.test.backup.host

import com.android.adblib.AdbSession
import com.android.adblib.DeviceSelector
import com.android.adblib.RemoteFileMode
import com.android.adblib.ShellCommandOutput
import com.android.adblib.shellAsText
import java.io.IOException
import java.nio.file.Path
import java.time.Duration
import java.util.logging.Logger
import kotlinx.coroutines.delay

/**
 * Runs shell commands and file transfers on one device over ADB.
 *
 * This is the only class that talks to `adblib`. Each method builds one device command, quotes the
 * values it interpolates, runs it, and interprets its output, so callers never assemble shell
 * strings themselves.
 */
internal class BackupDeviceShell(private val adbSession: AdbSession, serialNumber: String) {

    private val device = DeviceSelector.fromSerialNumber(serialNumber)

    private val logger = Logger.getLogger(BackupDeviceShell::class.java.name)

    /** Runs [command] through the device shell and returns its output. */
    suspend fun exec(command: String): ShellCommandOutput {
        @Suppress("AdbDeviceServicesCommand")
        return adbSession.deviceServices.shellAsText(device, command)
    }

    /** Copies [devicePath] from the device to [hostPath]. */
    suspend fun pullFile(devicePath: String, hostPath: Path) {
        adbSession.channelFactory.createFile(hostPath).use { output ->
            adbSession.deviceServices.sync(device).use { sync ->
                sync.recv(devicePath, output, null)
            }
        }
    }

    /** Copies [hostPath] to [devicePath] on the device. */
    suspend fun pushFile(hostPath: Path, devicePath: String) {
        adbSession.channelFactory.openFile(hostPath).use { input ->
            adbSession.deviceServices.sync(device).use { sync ->
                sync.send(input, devicePath, PUSHED_FILE_MODE, null, null)
            }
        }
    }

    /** Deletes [devicePath] on the device, if it exists. */
    suspend fun removeFile(devicePath: String) {
        exec("rm -f -- ${quote(devicePath)}")
    }

    /**
     * Runs the instrumentation [component] with string [args], waits for it to finish, and returns
     * what it printed.
     */
    suspend fun instrument(component: String, args: Map<String, String>): String {
        val command = buildString {
            append("am instrument -w")
            for ((key, value) in args) {
                append(" -e ").append(quoteIfNeeded(key)).append(' ').append(quote(value))
            }
            append(' ').append(quoteIfNeeded(component))
        }
        return exec(command).stdout
    }

    /**
     * Starts an activity and waits for it to launch.
     *
     * Targets [component] when it is not null, and otherwise lets the system resolve the intent
     * within [packageName].
     */
    suspend fun startActivity(
        action: String,
        category: String?,
        component: String?,
        packageName: String,
        stringExtras: Map<String, String>,
    ) {
        val command = buildString {
            append("am start -W -a ").append(quoteIfNeeded(action))
            if (category != null) append(" -c ").append(quoteIfNeeded(category))
            if (component != null) {
                append(" -n ").append(quote(component))
            } else {
                append(" -p ").append(quoteIfNeeded(packageName))
            }
            for ((key, value) in stringExtras) {
                append(" --es ").append(quoteIfNeeded(key)).append(' ').append(quote(value))
            }
        }
        logger.info("Launching app via am start: $command")
        exec(command)
    }

    /**
     * Returns the launcher activity of [packageName] as `package/class`, or null if it has none.
     */
    suspend fun resolveLauncherActivity(packageName: String): String? {
        val output =
            exec(
                    "cmd package resolve-activity --brief -c $CATEGORY_LAUNCHER " +
                        quoteIfNeeded(packageName)
                )
                .stdout
        // Matches `package/class`, skipping other fields such as `priority=`. Allows `$` for
        // nested activity classes.
        val component = Regex("""\b${Regex.escape(packageName)}/[a-zA-Z0-9._${'$'}]+\b""")
        return component.find(output)?.value
    }

    /**
     * Takes [packageName] out of the stopped state (`FLAG_STOPPED`).
     *
     * Freshly installed or cleared packages are stopped, and `BackupManagerService` skips stopped
     * packages. A broadcast with `--include-stopped-packages` wakes packages that have receivers or
     * services without touching the UI. Newer Android versions only unstop activity-based apps on
     * an explicit activity start, so the launcher activity, if any, is also started and then sent
     * to the background once it has had time to initialize.
     */
    suspend fun unstopPackage(packageName: String) {
        val pkg = quoteIfNeeded(packageName)
        exec("am broadcast -a $ACTION_MAIN -p $pkg --include-stopped-packages")

        val launcher = resolveLauncherActivity(packageName)
        if (launcher == null) {
            logger.warning(
                "No launcher activity found for package $packageName; relied on broadcast wake-up."
            )
            return
        }
        exec("am start -W -n ${quote(launcher)}")
        delay(ACTIVITY_SETTLE_DELAY_MS)
        pressHome()
    }

    /** Force-stops [packageName]. */
    suspend fun forceStop(packageName: String) {
        logger.info("Force-stopping $packageName...")
        exec("am force-stop ${quoteIfNeeded(packageName)}")
    }

    /**
     * Clears the data of [packageName].
     *
     * @throws IOException if the package manager does not report success
     */
    suspend fun clearPackageData(packageName: String) {
        val output = exec("pm clear ${quoteIfNeeded(packageName)}")
        // `pm clear` prints "Success" on stdout when the data was removed. Otherwise it prints
        // "Failed", or the exception that stopped it, on stderr with a non-zero exit code.
        // Continuing after a failed clear would restore onto the seeded data and let verification
        // pass without a restore having taken place.
        val succeeded =
            output.exitCode == 0 &&
                output.stdout.lineSequence().any { it.trim().equals("Success", ignoreCase = true) }
        if (!succeeded) {
            val details =
                listOf(output.stderr, output.stdout)
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .joinToString(" ")
            val detailsSuffix = if (details.isNotEmpty()) ": $details" else ""
            throw IOException(
                "Failed to clear app data for $packageName " +
                    "(exit code ${output.exitCode})$detailsSuffix"
            )
        }
    }

    /**
     * Installs [apkFile] with the package manager [options].
     *
     * @throws IllegalStateException if the package manager does not report success
     */
    suspend fun installPackage(apkFile: Path, options: List<String>) {
        logger.info("Pushing APK: ${apkFile.toAbsolutePath()} to device staging area...")
        pushFile(apkFile, STAGED_APK_PATH)

        logger.info("Installing staged APK via pm install...")
        val flags = options.joinToString(" ") { quoteIfNeeded(it) }
        val result = exec("pm install $flags ${quote(STAGED_APK_PATH)}")
        if (!result.stdout.contains("Success", ignoreCase = true)) {
            throw IllegalStateException("Failed to install APK: ${result.stdout.trim()}")
        }
        removeFile(STAGED_APK_PATH)
    }

    /** Turns the screen on and dismisses the keyguard. */
    suspend fun wakeAndDismissKeyguard() {
        exec("input keyevent KEYCODE_WAKEUP")
        exec("wm dismiss-keyguard")
    }

    /** Sends the home key event, moving any foreground activity to the background. */
    suspend fun pressHome() {
        exec("input keyevent KEYCODE_HOME")
    }

    /** Returns the logcat entries logged within the last [duration]. */
    suspend fun dumpLogcat(duration: Duration): String =
        exec("logcat -d -t ${duration.toSeconds()}s").stdout

    /** Clears the logcat buffer. */
    suspend fun clearLogcat() {
        exec("logcat -c")
    }

    companion object {
        const val ACTION_MAIN = "android.intent.action.MAIN"
        const val CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER"

        private const val STAGED_APK_PATH = "/data/local/tmp/backup_test_temp.apk"
        private const val ACTIVITY_SETTLE_DELAY_MS = 500L

        /** `rwxrwxrwx`, so the package manager can read the staged file. */
        private val PUSHED_FILE_MODE = RemoteFileMode.fromModeBits(511)

        /**
         * Quotes [arg] as a single POSIX shell word, so the shell passes it through unchanged:
         * without expanding `$`, backticks or backslashes, and without splitting it on spaces.
         */
        fun quote(arg: String): String = "'" + arg.replace("'", "'\\''") + "'"

        /**
         * Returns [token] unchanged when every character is shell-safe, and [quote]s it otherwise.
         *
         * Keeps commands readable for the common case of package names, keys and flags. An empty
         * token is always quoted, so it still counts as an argument.
         */
        fun quoteIfNeeded(token: String): String =
            if (token.isNotEmpty() && token.all { it.isLetterOrDigit() || it in SHELL_SAFE }) {
                token
            } else {
                quote(token)
            }

        /** Characters, besides letters and digits, that the shell never interprets in a word. */
        private const val SHELL_SAFE = "._/:=@%+,-"
    }
}
