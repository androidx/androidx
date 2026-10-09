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

import java.io.File
import java.util.logging.Logger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Backs up and restores [applicationId] through the device's local backup transport with `bmgr`.
 *
 * The backup data stays on the device, in the local transport's storage. The host only keeps a
 * placeholder archive, so that a backup produces a file like the other transport modes do.
 */
internal class BackupLocalTransport(
    private val shell: BackupDeviceShell,
    private val applicationId: String,
    private val reselectTransportTimeout: Duration = DEFAULT_CLEANUP_TIMEOUT,
) {
    private val logger = Logger.getLogger(BackupLocalTransport::class.java.name)

    /** Backs up the package to the local transport and writes a placeholder to [archive]. */
    suspend fun backup(archive: File) {
        logger.info("Executing robust local transport backup simulation...")
        shell.unstopPackage(applicationId)
        bmgr(
            "enable true",
            BackupErrorCode.BMGR_INIT_FAILED,
            "Failed to enable the backup manager with bmgr",
            "Backup Manager now enabled",
        )
        withLocalTransport {
            backUpNow(PACKAGE_MANAGER_SENTINEL)
            backUpNow(applicationId)
        }
        ZipOutputStream(archive.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("token.txt"))
            zip.write("1".toByteArray())
            zip.closeEntry()
        }
    }

    /**
     * Restores the package from the local transport and waits for the restore pass to end.
     *
     * [timeout] is the caller's budget for the whole restore. It limits how long to look for the
     * restore pass, so that a restore finishing before the first check still completes within it.
     * The wait for a pass that was seen has no bound of its own; the caller enforces [timeout].
     */
    suspend fun restore(timeout: Duration) {
        logger.info("Executing robust local transport restore simulation...")
        withLocalTransport {
            bmgr(
                "restore 1 ${BackupDeviceShell.quoteIfNeeded(applicationId)}",
                BackupErrorCode.RESTORE_FAILED,
                "Local restore of $applicationId failed",
                "restoreFinished: 0",
            )
            shell.exec("bmgr run")
            waitForRestorePassCompletion(minOf(RESTORE_DISPATCH_TIMEOUT, timeout / 2))
        }
    }

    /** Backs up [packageName] now, and throws unless both the package and the pass succeed. */
    private suspend fun backUpNow(packageName: String) {
        bmgr(
            "backupnow ${BackupDeviceShell.quoteIfNeeded(packageName)}",
            BackupErrorCode.BACKUP_FAILED,
            "Local backup of $packageName failed",
            // The pass reports success even when it skips the package, e.g. if it doesn't allow
            // backups, so the result of the package is checked as well.
            "Package $packageName with result: Success",
            "Backup finished with result: Success",
        )
    }

    /** Selects [transport] as the backup transport of the device. */
    private suspend fun selectTransport(transport: String) {
        bmgr(
            "transport ${BackupDeviceShell.quote(transport)}",
            BackupErrorCode.BMGR_INIT_FAILED,
            "Failed to select backup transport $transport",
            "Selected transport $transport",
        )
    }

    /**
     * Runs `bmgr` with [arguments], and throws a [BackupRestoreException] with [errorCode] and
     * [failureMessage] unless its output contains every one of [expected].
     *
     * bmgr reports failures only in its output: its exit code is 0 either way.
     */
    private suspend fun bmgr(
        arguments: String,
        errorCode: BackupErrorCode,
        failureMessage: String,
        vararg expected: String,
    ) {
        val output = shell.exec("bmgr $arguments")
        if (!expected.all { output.stdout.contains(it) }) {
            throw BackupRestoreException(
                errorCode,
                commandFailureMessage(failureMessage, output.stderr, output.stdout),
            )
        }
    }

    /**
     * Runs [block] with the local transport selected, then selects the original one again, even
     * when [block] fails or is cancelled. Selecting it again is bounded by
     * [reselectTransportTimeout], so an unresponsive device cannot block the caller.
     */
    private suspend fun withLocalTransport(block: suspend () -> Unit) {
        val originalTransport =
            shell
                .exec("bmgr list transports")
                .stdout
                .lineSequence()
                .firstOrNull { it.trim().startsWith("*") }
                ?.replace("*", "")
                ?.trim() ?: DEFAULT_TRANSPORT
        withCleanup(
            cleanup = {
                if (originalTransport != LOCAL_TRANSPORT) selectTransport(originalTransport)
            },
            cleanupTimeout = reselectTransportTimeout,
        ) {
            selectTransport(LOCAL_TRANSPORT)
            block()
        }
    }

    /**
     * Waits for BackupManagerService to dispatch the restore pass, and then for it to complete.
     *
     * A pass that is not seen within [dispatchTimeout] is assumed to have completed before the
     * first check.
     */
    private suspend fun waitForRestorePassCompletion(dispatchTimeout: Duration) {
        val started =
            withTimeoutOrNull(dispatchTimeout) {
                while (!isRestoreInProgress()) {
                    delay(RESTORE_DISPATCH_POLL_INTERVAL_MS)
                }
                true
            }
        if (started == null) {
            logger.warning(
                "Restore pass registration was not detected within $dispatchTimeout; proceeding."
            )
            return
        }
        while (isRestoreInProgress()) {
            delay(RESTORE_COMPLETION_POLL_INTERVAL_MS)
        }
    }

    private suspend fun isRestoreInProgress(): Boolean {
        val dumpsys = shell.exec("dumpsys backup").stdout
        return RESTORE_SESSION_REGEX.containsMatchIn(dumpsys) ||
            RESTORE_IN_PROGRESS_REGEX.containsMatchIn(dumpsys)
    }

    private companion object {
        const val LOCAL_TRANSPORT = "com.android.localtransport/.LocalTransport"

        /** Pseudo-package for the package metadata that a restore checks the app against. */
        const val PACKAGE_MANAGER_SENTINEL = "@pm@"

        /** Assumed to be the selected transport when `bmgr` does not mark one. */
        const val DEFAULT_TRANSPORT = "com.google.android.gms/.backup.BackupTransportService"

        /** Upper bound for detecting the restore pass, whatever the caller's timeout. */
        val RESTORE_DISPATCH_TIMEOUT = 7_500.milliseconds
        const val RESTORE_DISPATCH_POLL_INTERVAL_MS = 250L
        const val RESTORE_COMPLETION_POLL_INTERVAL_MS = 500L

        val RESTORE_SESSION_REGEX =
            Regex("""(?i)(?:Restore session|Active restore):\s*(?!null\b|none\b)\S+""")
        val RESTORE_IN_PROGRESS_REGEX = Regex("""(?i)Restore (?:pass )?in progress:\s*true\b""")
    }
}
