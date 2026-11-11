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
import java.io.IOException
import java.nio.file.Path
import java.util.Properties
import java.util.logging.Logger
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.time.Duration

/**
 * Backs up and restores [applicationId] through the device's local backup transport with `bmgr`.
 *
 * The backup data stays on the device, in the local transport's storage. The host only keeps an
 * archive that marks the backup, so that a backup produces a file like the other transport modes
 * do. The marker names the app and the device, since only that device can restore the data.
 */
internal class BackupLocalTransport(
    private val shell: BackupDeviceShell,
    private val applicationId: String,
    private val reselectTransportTimeout: Duration = DEFAULT_CLEANUP_TIMEOUT,
) {
    private val logger = Logger.getLogger(BackupLocalTransport::class.java.name)

    /**
     * Backs up the package to the local transport and writes a marker of the backup to [archive].
     */
    suspend fun backup(archive: File) {
        logger.info("Executing robust local transport backup simulation...")
        shell.unstopPackage(applicationId)
        val state = shell.backupManagerState()
        if (state?.isEnabled != true) {
            bmgr(
                "enable true",
                Regex("Backup Manager now enabled"),
                BackupErrorCode.BMGR_INIT_FAILED,
                "Failed to enable the backup manager with bmgr",
                "Backup Manager now enabled",
            )
        }
        withLocalTransport(state) {
            // One pass backs up the package metadata and then the app.
            bmgr(
                "backupnow $PACKAGE_MANAGER_SENTINEL " +
                    BackupDeviceShell.quoteIfNeeded(applicationId),
                // A pass that does not succeed can print a hint after its result.
                Regex("Backup finished with result: Success"),
                BackupErrorCode.BACKUP_FAILED,
                "Local backup of $applicationId failed",
                // The pass reports success even when it skips a package, e.g. if it doesn't allow
                // backups, so the result of each package is checked as well.
                "Package $PACKAGE_MANAGER_SENTINEL with result: Success",
                "Package $applicationId with result: Success",
                "Backup finished with result: Success",
            )
        }
        ZipOutputStream(archive.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry(MARKER_ENTRY))
            Properties()
                .apply {
                    setProperty(MARKER_PACKAGE, applicationId)
                    setProperty(MARKER_SERIAL, shell.serialNumber)
                }
                .store(zip, "A LOCAL backup, whose data stays in the local transport of the device")
            zip.closeEntry()
        }
    }

    /**
     * Restores the package from the local transport, as [backup], marked by [archive], and returns
     * when the restore has finished.
     *
     * @throws IOException if [backup] is of another app, or was made on another device
     */
    suspend fun restore(archive: Path, backup: LocalBackup) {
        if (backup.packageName != null && backup.packageName != applicationId) {
            throw IOException(
                "${archive.fileName} is a LOCAL backup of ${backup.packageName}, not of " +
                    applicationId
            )
        }
        if (backup.serialNumber != null && backup.serialNumber != shell.serialNumber) {
            throw IOException(
                "${archive.fileName} is a LOCAL backup made on ${backup.serialNumber}. Its data " +
                    "stays in the local transport of that device, so ${shell.serialNumber} " +
                    "can't restore it."
            )
        }
        logger.info("Executing robust local transport restore simulation...")
        withLocalTransport(shell.backupManagerState()) {
            // bmgr reports the result only when the restore has finished.
            bmgr(
                "restore 1 ${BackupDeviceShell.quoteIfNeeded(applicationId)}",
                Regex("done"),
                BackupErrorCode.RESTORE_FAILED,
                "Local restore of $applicationId failed",
                "restoreFinished: 0",
            )
        }
    }

    /** Selects [transport] as the backup transport of the device. */
    private suspend fun selectTransport(transport: String) {
        bmgr(
            "transport ${BackupDeviceShell.quote(transport)}",
            Regex("""Selected transport .* \(formerly .*\)"""),
            BackupErrorCode.BMGR_INIT_FAILED,
            "Failed to select backup transport $transport",
            "Selected transport $transport",
        )
    }

    /**
     * Runs `bmgr` with [arguments], and throws a [BackupRestoreException] with [errorCode] and
     * [failureMessage] unless its output contains every one of [expected].
     *
     * bmgr reports failures only in its output: its exit code is 0 either way. It prints [lastLine]
     * when it has done what it was asked to, and its exit after that is not waited for.
     */
    private suspend fun bmgr(
        arguments: String,
        lastLine: Regex,
        errorCode: BackupErrorCode,
        failureMessage: String,
        vararg expected: String,
    ) {
        // A command that asks bmgr for monitor events could not end on its last line, since
        // another thread prints the events, which may follow that line.
        val output = shell.exec("bmgr $arguments", lastLine)
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
     *
     * The original transport is the selected one of [state], or the one that `bmgr` marks as
     * selected when [state] is null. When it is the local transport, none is selected.
     */
    private suspend fun withLocalTransport(
        state: BackupDeviceShell.BackupManagerState?,
        block: suspend () -> Unit,
    ) {
        val originalTransport =
            state?.selectedTransport
                ?: shell
                    .exec("bmgr list transports")
                    .stdout
                    .lineSequence()
                    .firstOrNull { it.trim().startsWith("*") }
                    ?.replace("*", "")
                    ?.trim()
                ?: DEFAULT_TRANSPORT
        if (originalTransport == LOCAL_TRANSPORT) {
            block()
            return
        }
        withCleanup(
            cleanup = { selectTransport(originalTransport) },
            cleanupTimeout = reselectTransportTimeout,
        ) {
            selectTransport(LOCAL_TRANSPORT)
            block()
        }
    }

    /**
     * A LOCAL backup of [packageName], made on the device [serialNumber]. Both are null for a
     * marker written by a version of this library that didn't record them.
     */
    data class LocalBackup(val packageName: String?, val serialNumber: String?)

    companion object {
        /**
         * Returns the LOCAL backup that [archive] marks, or null if [archive] isn't the marker of a
         * LOCAL backup, for example the archive of another transport mode.
         *
         * @throws IOException if [archive] has a LOCAL backup marker that can't be read, or that
         *   doesn't name both the app and the device
         */
        fun localBackupOf(archive: Path): LocalBackup? {
            val zipFile =
                try {
                    ZipFile(archive.toFile())
                } catch (e: IOException) {
                    // Not a readable zip file, so not a marker.
                    return null
                }
            return zipFile.use { zip ->
                val marker = zip.getEntry(MARKER_ENTRY)
                when {
                    marker != null -> {
                        val properties = Properties()
                        try {
                            zip.getInputStream(marker).use { properties.load(it) }
                        } catch (e: IllegalArgumentException) {
                            throw IOException("$archive has a malformed LOCAL backup marker", e)
                        }
                        val packageName = properties.getProperty(MARKER_PACKAGE)
                        val serialNumber = properties.getProperty(MARKER_SERIAL)
                        if (packageName.isNullOrBlank() || serialNumber.isNullOrBlank()) {
                            throw IOException("$archive has a malformed LOCAL backup marker")
                        }
                        LocalBackup(packageName, serialNumber)
                    }
                    zip.size() == 1 && zip.getEntry(LEGACY_MARKER_ENTRY) != null ->
                        LocalBackup(packageName = null, serialNumber = null)
                    else -> null
                }
            }
        }

        private const val LOCAL_TRANSPORT = "com.android.localtransport/.LocalTransport"

        /** Zip entry of a LOCAL backup marker, as properties naming the app and the device. */
        private const val MARKER_ENTRY = "androidx.test.backup.local.properties"
        private const val MARKER_PACKAGE = "package"
        private const val MARKER_SERIAL = "serial"

        /** The only zip entry of the markers that earlier versions of this library wrote. */
        private const val LEGACY_MARKER_ENTRY = "token.txt"

        /** Pseudo-package for the package metadata that a restore checks the app against. */
        private const val PACKAGE_MANAGER_SENTINEL = "@pm@"

        /** Assumed to be the selected transport when `bmgr` does not mark one. */
        private const val DEFAULT_TRANSPORT =
            "com.google.android.gms/.backup.BackupTransportService"
    }
}
