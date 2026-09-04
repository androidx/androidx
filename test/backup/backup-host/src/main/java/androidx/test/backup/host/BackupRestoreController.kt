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

import androidx.annotation.CheckResult
import com.google.common.util.concurrent.ListenableFuture
import java.io.IOException
import java.nio.file.Path
import java.time.Duration

/**
 * Controls backup, restore, and verification workflows for a test device.
 *
 * Provides operations to seed test data with [runOnDevice], run backups with [performBackup], wipe
 * app data with [clearAppData], and restore data with [performRestore].
 */
public interface BackupRestoreController : AutoCloseable {
    public companion object {
        /**
         * Default package installation flags: reinstall, allow test, grant all runtime permissions.
         */
        @JvmField public val DEFAULT_INSTALL_OPTIONS: List<String> = listOf("-r", "-t", "-g")

        /** Class name of the prebuilt action that seeds test data into app storage. */
        @JvmField
        public val ACTION_POPULATE_STORAGE: String =
            "androidx.test.backup.actions.PopulateStorageAction"

        /** Class name of the prebuilt action that verifies restored data in app storage. */
        @JvmField
        public val ACTION_ASSERT_STORAGE: String =
            "androidx.test.backup.actions.AssertStorageAction"
    }

    /** Closes open resources, including ADB connections. */
    @Throws(IOException::class) override fun close()

    /** Serial number of this device or emulator. */
    public val serialNumber: String

    /** SDK API level of this device or emulator. */
    public val apiLevel: Int

    /** Application ID of the target package under test. */
    public val applicationId: String

    /**
     * Runs an on-device action inside the target application process.
     *
     * @param actionClassName class name of the [androidx.test.backup.BackupDeviceAction] to run
     * @param args arguments to pass to the action
     * @param timeout maximum duration to wait for the action to complete
     * @param waitForDebugger whether the runner waits for a debugger to attach before running
     * @return result of the action execution
     * @throws IOException if communicating with the device fails
     */
    @Throws(IOException::class)
    public suspend fun runOnDevice(
        actionClassName: String,
        args: Map<String, String> = emptyMap(),
        timeout: Duration = Duration.ofMinutes(1),
        waitForDebugger: Boolean = false,
    ): BackupActionResult

    /**
     * Runs a full backup and restore flow for a single storage domain.
     *
     * Seeds test data into [storage] using [ACTION_POPULATE_STORAGE], runs a backup to [outputDir]
     * using [mode], clears app data, restores the backup archive, and verifies data integrity using
     * [ACTION_ASSERT_STORAGE].
     *
     * @param storage storage domain to seed and verify
     * @param outputDir directory where the generated backup file is saved
     * @param mode transport mode to test
     * @return this controller instance
     * @throws IOException if any step fails
     */
    @Throws(IOException::class)
    public suspend fun runBackupRestoreFlow(
        storage: StorageDomain,
        outputDir: Path,
        mode: BackupTransportMode,
    ): BackupRestoreController

    /**
     * Runs a full backup and restore flow for multiple storage domains.
     *
     * Seeds each domain in [storages] using [ACTION_POPULATE_STORAGE], runs a backup to [outputDir]
     * using [mode], clears app data, restores the backup archive, and verifies each domain using
     * [ACTION_ASSERT_STORAGE].
     *
     * @param storages storage domains to seed and verify
     * @param outputDir directory where the generated backup file is saved
     * @param mode transport mode to test
     * @return this controller instance
     * @throws IOException if any step fails
     */
    @Throws(IOException::class)
    public suspend fun runBackupRestoreFlow(
        storages: List<StorageDomain>,
        outputDir: Path,
        mode: BackupTransportMode,
    ): BackupRestoreController

    /**
     * Captures an application backup archive using the specified transport mode.
     *
     * @param mode transport mode to test
     * @param outputDir directory where the backup archive is saved
     * @param timeout maximum duration to wait for the backup to complete
     * @return path to the generated backup archive
     * @throws IOException if the backup operation fails
     */
    @Throws(IOException::class)
    public suspend fun performBackup(
        mode: BackupTransportMode,
        outputDir: Path,
        timeout: Duration = Duration.ofMinutes(5),
    ): Path

    /**
     * Restores application data from a backup archive.
     *
     * @param backupFile backup archive generated by [performBackup]
     * @param timeout maximum duration to wait for the restore to complete
     * @return this controller instance
     * @throws IOException if the restore operation fails
     */
    @Throws(IOException::class)
    public suspend fun performRestore(
        backupFile: Path,
        timeout: Duration = Duration.ofMinutes(5),
    ): BackupRestoreController

    /**
     * Saves recent device logcat entries to a local file.
     *
     * @param destinationPath local file path where logs will be written
     * @param duration time window of historical logs to capture
     * @return this controller instance
     * @throws IOException if capturing logs fails
     */
    @Throws(IOException::class)
    public suspend fun fetchDeviceLogs(
        destinationPath: Path,
        duration: Duration = Duration.ofSeconds(30),
    ): BackupRestoreController

    /**
     * Clears the device logcat buffer via `logcat -c`.
     *
     * @return this controller instance
     * @throws IOException if clearing the logcat buffer fails
     */
    @Throws(IOException::class) public suspend fun clearDeviceLogs(): BackupRestoreController

    /**
     * Clears application sandbox data on the device via `pm clear`.
     *
     * @return this controller instance
     * @throws IOException if clearing application data fails
     */
    @Throws(IOException::class) public suspend fun clearAppData(): BackupRestoreController

    /**
     * Copies a file from the device to the host machine via `adb pull`.
     *
     * @param devicePath path to the file on the device
     * @param hostDestination path to write the file on the host
     * @return this controller instance
     * @throws IOException if copying the file fails
     */
    @Throws(IOException::class)
    public suspend fun pullFile(devicePath: String, hostDestination: Path): BackupRestoreController

    /**
     * Installs an APK from the host onto the device via `pm install`.
     *
     * @param apkFile path to the APK on the host
     * @param options installation flags to pass to package manager
     * @return this controller instance
     * @throws IOException if package installation fails
     */
    @Throws(IOException::class)
    public suspend fun installApk(
        apkFile: Path,
        options: List<String> = DEFAULT_INSTALL_OPTIONS,
    ): BackupRestoreController

    /**
     * Starts the target application on the device via `am start`.
     *
     * @param activityClass activity class to launch, or null for default launcher activity
     * @param intentExtras key-value pairs to pass as intent extras
     * @param action intent action string to launch with, or null for MAIN
     * @return this controller instance
     * @throws IOException if starting the application fails
     */
    @Throws(IOException::class)
    public suspend fun launchApp(
        activityClass: String? = null,
        intentExtras: Map<String, String> = emptyMap(),
        action: String? = null,
    ): BackupRestoreController

    /**
     * Force-stops the target application on the device via `am force-stop`.
     *
     * @return this controller instance
     * @throws IOException if stopping the application fails
     */
    @Throws(IOException::class) public suspend fun stopApp(): BackupRestoreController

    /**
     * Runs an on-device action asynchronously inside the target application process.
     *
     * @param actionClassName class name of the [androidx.test.backup.BackupDeviceAction] to run
     * @return a [ListenableFuture] with the action result
     */
    @CheckResult
    public fun runOnDeviceAsync(actionClassName: String): ListenableFuture<BackupActionResult>

    /**
     * Runs an on-device action asynchronously inside the target application process.
     *
     * @param actionClassName class name of the [androidx.test.backup.BackupDeviceAction] to run
     * @param args arguments to pass to the action
     * @return a [ListenableFuture] with the action result
     */
    @CheckResult
    public fun runOnDeviceAsync(
        actionClassName: String,
        args: Map<String, String>,
    ): ListenableFuture<BackupActionResult>

    /**
     * Runs an on-device action asynchronously inside the target application process.
     *
     * @param actionClassName class name of the [androidx.test.backup.BackupDeviceAction] to run
     * @param args arguments to pass to the action
     * @param timeout maximum duration to wait for the action to complete
     * @return a [ListenableFuture] with the action result
     */
    @CheckResult
    public fun runOnDeviceAsync(
        actionClassName: String,
        args: Map<String, String>,
        timeout: Duration,
    ): ListenableFuture<BackupActionResult>

    /**
     * Runs an on-device action asynchronously inside the target application process.
     *
     * @param actionClassName class name of the [androidx.test.backup.BackupDeviceAction] to run
     * @param args arguments to pass to the action
     * @param timeout maximum duration to wait for the action to complete
     * @param waitForDebugger whether the runner waits for a debugger to attach before running
     * @return a [ListenableFuture] with the action result
     */
    @CheckResult
    public fun runOnDeviceAsync(
        actionClassName: String,
        args: Map<String, String>,
        timeout: Duration,
        waitForDebugger: Boolean,
    ): ListenableFuture<BackupActionResult>

    /**
     * Runs a full backup and restore flow asynchronously for a single storage domain.
     *
     * @param storage storage domain to seed and verify
     * @param outputDir directory where the generated backup file is saved
     * @param mode transport mode to test
     * @return a [ListenableFuture] with this controller instance
     */
    @CheckResult
    public fun runBackupRestoreFlowAsync(
        storage: StorageDomain,
        outputDir: Path,
        mode: BackupTransportMode,
    ): ListenableFuture<BackupRestoreController>

    /**
     * Runs a full backup and restore flow asynchronously for multiple storage domains.
     *
     * @param storages storage domains to seed and verify
     * @param outputDir directory where the generated backup file is saved
     * @param mode transport mode to test
     * @return a [ListenableFuture] with this controller instance
     */
    @CheckResult
    public fun runBackupRestoreFlowAsync(
        storages: List<StorageDomain>,
        outputDir: Path,
        mode: BackupTransportMode,
    ): ListenableFuture<BackupRestoreController>

    /**
     * Captures an application backup archive asynchronously using the specified transport mode.
     *
     * @param mode transport mode to test
     * @param outputDir directory where the backup archive is saved
     * @return a [ListenableFuture] with the generated backup path
     */
    @CheckResult
    public fun performBackupAsync(
        mode: BackupTransportMode,
        outputDir: Path,
    ): ListenableFuture<Path>

    /**
     * Captures an application backup archive asynchronously using the specified transport mode.
     *
     * @param mode transport mode to test
     * @param outputDir directory where the backup archive is saved
     * @param timeout maximum duration to wait for the backup to complete
     * @return a [ListenableFuture] with the generated backup path
     */
    @CheckResult
    public fun performBackupAsync(
        mode: BackupTransportMode,
        outputDir: Path,
        timeout: Duration,
    ): ListenableFuture<Path>

    /**
     * Restores application data asynchronously from a backup archive.
     *
     * @param backupFile backup archive generated by [performBackup]
     * @return a [ListenableFuture] with this controller instance
     */
    @CheckResult
    public fun performRestoreAsync(backupFile: Path): ListenableFuture<BackupRestoreController>

    /**
     * Restores application data asynchronously from a backup archive.
     *
     * @param backupFile backup archive generated by [performBackup]
     * @param timeout maximum duration to wait for the restore to complete
     * @return a [ListenableFuture] with this controller instance
     */
    @CheckResult
    public fun performRestoreAsync(
        backupFile: Path,
        timeout: Duration,
    ): ListenableFuture<BackupRestoreController>

    /**
     * Saves recent device logcat entries asynchronously to a local file.
     *
     * @param destinationPath local file path where logs will be written
     * @return a [ListenableFuture] with this controller instance
     */
    @CheckResult
    public fun fetchDeviceLogsAsync(
        destinationPath: Path
    ): ListenableFuture<BackupRestoreController>

    /**
     * Saves recent device logcat entries asynchronously to a local file.
     *
     * @param destinationPath local file path where logs will be written
     * @param duration time window of historical logs to capture
     * @return a [ListenableFuture] with this controller instance
     */
    @CheckResult
    public fun fetchDeviceLogsAsync(
        destinationPath: Path,
        duration: Duration,
    ): ListenableFuture<BackupRestoreController>

    /**
     * Clears the device logcat buffer asynchronously.
     *
     * @return a [ListenableFuture] with this controller instance
     */
    @CheckResult public fun clearDeviceLogsAsync(): ListenableFuture<BackupRestoreController>

    /**
     * Clears application sandbox data asynchronously on the device.
     *
     * @return a [ListenableFuture] with this controller instance
     */
    @CheckResult public fun clearAppDataAsync(): ListenableFuture<BackupRestoreController>

    /**
     * Copies a file asynchronously from the device to the host machine.
     *
     * @param devicePath path to the file on the device
     * @param hostDestination path to write the file on the host
     * @return a [ListenableFuture] with this controller instance
     */
    @CheckResult
    public fun pullFileAsync(
        devicePath: String,
        hostDestination: Path,
    ): ListenableFuture<BackupRestoreController>

    /**
     * Installs an APK asynchronously from the host onto the device.
     *
     * @param apkFile path to the APK on the host
     * @return a [ListenableFuture] with this controller instance
     */
    @CheckResult
    public fun installApkAsync(apkFile: Path): ListenableFuture<BackupRestoreController>

    /**
     * Installs an APK asynchronously from the host onto the device.
     *
     * @param apkFile path to the APK on the host
     * @param options installation flags to pass to package manager
     * @return a [ListenableFuture] with this controller instance
     */
    @CheckResult
    public fun installApkAsync(
        apkFile: Path,
        options: List<String>,
    ): ListenableFuture<BackupRestoreController>

    /**
     * Starts the target application asynchronously on the device.
     *
     * @return a [ListenableFuture] with this controller instance
     */
    @CheckResult public fun launchAppAsync(): ListenableFuture<BackupRestoreController>

    /**
     * Starts the target application asynchronously on the device.
     *
     * @param activityClass activity class to launch, or null for default launcher activity
     * @return a [ListenableFuture] with this controller instance
     */
    @CheckResult
    public fun launchAppAsync(activityClass: String?): ListenableFuture<BackupRestoreController>

    /**
     * Starts the target application asynchronously on the device.
     *
     * @param activityClass activity class to launch, or null for default launcher activity
     * @param intentExtras key-value pairs to pass as intent extras
     * @return a [ListenableFuture] with this controller instance
     */
    @CheckResult
    public fun launchAppAsync(
        activityClass: String?,
        intentExtras: Map<String, String>,
    ): ListenableFuture<BackupRestoreController>

    /**
     * Starts the target application asynchronously on the device.
     *
     * @param activityClass activity class to launch, or null for default launcher activity
     * @param intentExtras key-value pairs to pass as intent extras
     * @param action intent action string to launch with
     * @return a [ListenableFuture] with this controller instance
     */
    @CheckResult
    public fun launchAppAsync(
        activityClass: String?,
        intentExtras: Map<String, String>,
        action: String?,
    ): ListenableFuture<BackupRestoreController>

    /**
     * Force-stops the target application asynchronously on the device.
     *
     * @return a [ListenableFuture] with this controller instance
     */
    @CheckResult public fun stopAppAsync(): ListenableFuture<BackupRestoreController>
}
