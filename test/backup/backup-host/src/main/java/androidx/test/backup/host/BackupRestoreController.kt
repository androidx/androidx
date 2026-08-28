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
 * Orchestrates on-device backup, restore, and verification workflows.
 *
 * Provides a fluent Kotlin DSL for on-device setup, backup execution, restore triggering, and
 * validation, as well as explicit interface method overloads for Java test suites.
 *
 * Exposes core primitive operations ([runOnDevice], [performBackup], [performRestore],
 * [clearAppData], [pullFile], [installApk], [launchApp], [stopApp]) for fine-grained manual
 * control.
 *
 * This controller is completely framework-agnostic. While the optional `BackupRestoreExtension`
 * provides integration with JUnit 5 out of the box, `BackupRestoreController` itself has no
 * compile-time or run-time dependencies on any particular testing framework, and can be manually
 * instantiated and used within JUnit 4 tests, custom test runners, or main JVM orchestrations.
 */
public interface BackupRestoreController : AutoCloseable {
    public companion object {
        /** Default package installation options. */
        @JvmField public val DEFAULT_INSTALL_OPTIONS: List<String> = listOf("-r", "-t", "-g")

        /** Fully-qualified class name of the prebuilt populate storage action. */
        @JvmField
        public val ACTION_POPULATE_STORAGE: String =
            "androidx.test.backup.actions.PopulateStorageAction"

        /** Fully-qualified class name of the prebuilt assert storage action. */
        @JvmField
        public val ACTION_ASSERT_STORAGE: String =
            "androidx.test.backup.actions.AssertStorageAction"
    }

    /** Closes any open resources, such as ADB sessions. */
    @Throws(IOException::class) override fun close()

    /** The serial number of this device/emulator. */
    public val serialNumber: String

    /** The SDK API level of this device/emulator (e.g. 31, 34). */
    public val apiLevel: Int

    /** The application ID (package name) of the target application being tested. */
    public val applicationId: String

    /**
     * Executes a specific `BackupDeviceAction` inside the application's process on this device.
     *
     * @param actionClassName Fully-qualified class name of the `BackupDeviceAction` implementation.
     * @param args Key-value pair arguments to pass to the payload. Defaults to `emptyMap()`.
     * @param timeout Maximum time to wait for the action execution to complete on the device.
     *   Defaults to 1 minute.
     * @param waitForDebugger If true, the runner on the device waits for a debugger to attach
     *   before executing. Defaults to `false`.
     * @return A [BackupActionResult] enclosing the execution outcome and serialized return data.
     * @throws IOException if a communication error occurs with the device.
     */
    @Throws(IOException::class)
    public suspend fun runOnDevice(
        actionClassName: String,
        args: Map<String, String> = emptyMap(),
        timeout: Duration = Duration.ofMinutes(1),
        waitForDebugger: Boolean = false,
    ): BackupActionResult

    // TODO(b/551659754): Update to PopulateStorageAction to link to public API
    /**
     * Executes a declarative, zero-boilerplate backup and restore flow. Maps the provided [storage]
     * domain properties to arguments, executes the PopulateStorageAction prebuilt (TODO: Update to
     * a link once PopulateStorageAction is available as public API), performs the backup, clears
     * the app sandbox, performs the restore, and runs AssertStorageAction (TODO: Update to a link
     * once AssertStorageAction is available as public API) to assert restored values are identical
     * to the seeded values.
     *
     * @param storage The strongly-typed storage domain specifying key-values to seed and assert.
     * @param outputDir The local directory where the generated .backup ZIP should be written.
     * @param mode The transport mode to emulate.
     * @return This [BackupRestoreController] orchestration instance.
     * @throws IOException if any step in the orchestration pipeline fails.
     */
    @Throws(IOException::class)
    public suspend fun runStandardBackupRestoreFlow(
        storage: StorageDomain,
        outputDir: Path,
        mode: BackupTransportMode,
    ): BackupRestoreController

    /**
     * Executes a declarative, zero-boilerplate backup and restore flow across multiple [storages]
     * domains. For each domain, seeds the data, then stops the app, performs the backup, clears the
     * app sandbox, performs the restore, and verifies each storage domain individually.
     *
     * @param storages The list of strongly-typed storage domains to seed and assert.
     * @param outputDir The local directory where the generated .backup ZIP should be written.
     * @param mode The transport mode to emulate.
     * @return This [BackupRestoreController] orchestration instance.
     * @throws IOException if any step in the orchestration pipeline fails.
     */
    @Throws(IOException::class)
    public suspend fun runStandardBackupRestoreFlow(
        storages: List<StorageDomain>,
        outputDir: Path,
        mode: BackupTransportMode,
    ): BackupRestoreController

    /**
     * Triggers a host-driven backup operation for the application on this device.
     *
     * Captures the application data stream according to the specified transport [mode] and saves
     * the generated archive into [outputDir].
     *
     * @param mode The transport mode to emulate (e.g. [BackupTransportMode.DEVICE_TO_DEVICE],
     *   [BackupTransportMode.CLOUD_ENCRYPTED]).
     * @param outputDir The local directory where the generated .backup ZIP should be written.
     * @param timeout Maximum time to wait for the backup operation to complete. Defaults to 5
     *   minutes.
     * @return The local [Path] referencing the generated backup zip file created within
     *   [outputDir].
     * @throws IOException if the backup operation fails or the device is unreachable.
     */
    @Throws(IOException::class)
    public suspend fun performBackup(
        mode: BackupTransportMode,
        outputDir: Path,
        timeout: Duration = Duration.ofMinutes(5),
    ): Path

    /**
     * Triggers a host-driven restore operation for the application on this device.
     *
     * Ingests the provided [backupFile] and restores the application sandbox data on the device.
     *
     * @param backupFile The local backup ZIP file generated by [performBackup].
     * @param timeout Maximum time to wait for the restore operation to complete. Defaults to 5
     *   minutes.
     * @return This [BackupRestoreController] orchestration instance.
     * @throws IOException if the restore operation fails or the device is unreachable.
     */
    @Throws(IOException::class)
    public suspend fun performRestore(
        backupFile: Path,
        timeout: Duration = Duration.ofMinutes(5),
    ): BackupRestoreController

    /**
     * Extracts device logcat entries. Streams logs directly to a local file on the host.
     *
     * @param destinationPath The local file path on the host filesystem where the retrieved logs
     *   will be written.
     * @param duration Duration of historical logs to look back from the current timestamp. Defaults
     *   to 30 seconds.
     * @return This [BackupRestoreController] orchestration instance.
     * @throws IOException if the logcat stream cannot be read or written.
     */
    @Throws(IOException::class)
    public suspend fun fetchDeviceLogs(
        destinationPath: Path,
        duration: Duration = Duration.ofSeconds(30),
    ): BackupRestoreController

    /**
     * Clears the device's logcat buffer (via logcat -c).
     *
     * Useful at the beginning of a test or step to ensure subsequent calls to [fetchDeviceLogs]
     * capture only the relevant execution window without background noise or history.
     *
     * @return This [BackupRestoreController] orchestration instance.
     * @throws IOException if the clear command fails or the device is unreachable.
     */
    @Throws(IOException::class) public suspend fun clearDeviceLogs(): BackupRestoreController

    /**
     * Manually clears the target application's sandbox data (via pm clear).
     *
     * @return This [BackupRestoreController] orchestration instance.
     * @throws IOException if the shell command fails.
     */
    @Throws(IOException::class) public suspend fun clearAppData(): BackupRestoreController

    /**
     * Pulls an accessible file from the target device to the host filesystem.
     *
     * @param devicePath The absolute path to the file on the device.
     * @param hostDestination The local file path on the host filesystem where the pulled file will
     *   be written.
     * @return This [BackupRestoreController] orchestration instance.
     * @throws IOException if the file cannot be pulled.
     */
    @Throws(IOException::class)
    public suspend fun pullFile(devicePath: String, hostDestination: Path): BackupRestoreController

    /**
     * Installs a specific APK file from the host workstation onto the target device. Useful for
     * performing multi-version upgrade and migration testing (e.g. seeding v1, backing up,
     * installing v2, restoring).
     *
     * @param apkFile The local APK file on the host workstation.
     * @param options Additional pm install options/flags to pass (defaults to
     *   [DEFAULT_INSTALL_OPTIONS]).
     * @return This [BackupRestoreController] orchestration instance.
     * @throws IOException if the APK installation fails.
     */
    @Throws(IOException::class)
    public suspend fun installApk(
        apkFile: Path,
        options: List<String> = DEFAULT_INSTALL_OPTIONS,
    ): BackupRestoreController

    /**
     * Launches the target application on this device.
     *
     * Supports custom target activity class names, intent action strings, and key-value string
     * intent extras.
     *
     * @param activityClass The fully-qualified or relative (starting with dot) Activity class name
     *   to launch, or `null` to use the application's default launcher activity. Defaults to
     *   `null`.
     * @param intentExtras Key-value string pairs to pass as intent extras (use `emptyMap()` if
     *   passing no extras). Defaults to `emptyMap()`.
     * @param action The intent action string to launch with, or `null` to default to
     *   `android.intent.action.MAIN`. Defaults to `null`.
     * @return This [BackupRestoreController] orchestration instance.
     * @throws IOException if the launch command fails.
     */
    @Throws(IOException::class)
    public suspend fun launchApp(
        activityClass: String? = null,
        intentExtras: Map<String, String> = emptyMap(),
        action: String? = null,
    ): BackupRestoreController

    /**
     * Force-stops the target application on this device.
     *
     * @return This [BackupRestoreController] orchestration instance.
     * @throws IOException if the stop command fails.
     */
    @Throws(IOException::class) public suspend fun stopApp(): BackupRestoreController

    /**
     * Executes a specific `BackupDeviceAction` inside the application's process on this device.
     *
     * Java-compatible asynchronous alternative to `runOnDevice` returning a [ListenableFuture].
     *
     * @param actionClassName Fully-qualified class name of the `BackupDeviceAction` implementation.
     * @return A [ListenableFuture] wrapping the [BackupActionResult] enclosing the execution
     *   outcome and serialized return data.
     */
    @CheckResult
    public fun runOnDeviceAsync(actionClassName: String): ListenableFuture<BackupActionResult>

    /**
     * Executes a specific `BackupDeviceAction` inside the application's process on this device.
     *
     * Java-compatible asynchronous alternative to `runOnDevice` returning a [ListenableFuture].
     *
     * @param actionClassName Fully-qualified class name of the `BackupDeviceAction` implementation.
     * @param args Key-value pair arguments to pass to the payload.
     * @return A [ListenableFuture] wrapping the [BackupActionResult] enclosing the execution
     *   outcome and serialized return data.
     */
    @CheckResult
    public fun runOnDeviceAsync(
        actionClassName: String,
        args: Map<String, String>,
    ): ListenableFuture<BackupActionResult>

    /**
     * Executes a specific `BackupDeviceAction` inside the application's process on this device.
     *
     * Java-compatible asynchronous alternative to `runOnDevice` returning a [ListenableFuture].
     *
     * @param actionClassName Fully-qualified class name of the `BackupDeviceAction` implementation.
     * @param args Key-value pair arguments to pass to the payload.
     * @param timeout Maximum time to wait for the action execution to complete on the device.
     * @return A [ListenableFuture] wrapping the [BackupActionResult] enclosing the execution
     *   outcome and serialized return data.
     */
    @CheckResult
    public fun runOnDeviceAsync(
        actionClassName: String,
        args: Map<String, String>,
        timeout: Duration,
    ): ListenableFuture<BackupActionResult>

    /**
     * Executes a specific `BackupDeviceAction` inside the application's process on this device.
     *
     * Java-compatible asynchronous alternative to `runOnDevice` returning a [ListenableFuture].
     *
     * @param actionClassName Fully-qualified class name of the `BackupDeviceAction` implementation.
     * @param args Key-value pair arguments to pass to the payload.
     * @param timeout Maximum time to wait for the action execution to complete on the device.
     * @param waitForDebugger If true, the runner on the device waits for a debugger to attach.
     * @return A [ListenableFuture] wrapping the [BackupActionResult] enclosing the execution
     *   outcome and serialized return data.
     */
    @CheckResult
    public fun runOnDeviceAsync(
        actionClassName: String,
        args: Map<String, String>,
        timeout: Duration,
        waitForDebugger: Boolean,
    ): ListenableFuture<BackupActionResult>

    /**
     * Executes a declarative, zero-boilerplate backup and restore flow. Maps the provided `storage`
     * domain properties to arguments, executes the PopulateStorageAction prebuilt, performs the
     * backup, clears the app sandbox, performs the restore, and runs AssertStorageAction to assert
     * restored values are identical to the seeded values.
     *
     * Java-compatible asynchronous alternative to `runStandardBackupRestoreFlow` returning a
     * [ListenableFuture].
     *
     * @param storage The strongly-typed storage domain specifying key-values to seed and assert.
     * @param outputDir The local directory where the generated .backup ZIP should be written.
     * @param mode The transport mode to emulate.
     * @return A [ListenableFuture] wrapping the [BackupRestoreController] orchestration instance.
     */
    @CheckResult
    public fun runStandardBackupRestoreFlowAsync(
        storage: StorageDomain,
        outputDir: Path,
        mode: BackupTransportMode,
    ): ListenableFuture<BackupRestoreController>

    /**
     * Executes a declarative, zero-boilerplate backup and restore flow across multiple `storages`
     * domains.
     *
     * Java-compatible asynchronous alternative to `runStandardBackupRestoreFlow` returning a
     * [ListenableFuture].
     *
     * @param storages The list of strongly-typed storage domains to seed and assert.
     * @param outputDir The local directory where the generated .backup ZIP should be written.
     * @param mode The transport mode to emulate.
     * @return A [ListenableFuture] wrapping the [BackupRestoreController] orchestration instance.
     */
    @CheckResult
    public fun runStandardBackupRestoreFlowAsync(
        storages: List<StorageDomain>,
        outputDir: Path,
        mode: BackupTransportMode,
    ): ListenableFuture<BackupRestoreController>

    /**
     * Triggers a host-driven backup operation of the application on this device.
     *
     * Java-compatible asynchronous alternative to `performBackup` returning a [ListenableFuture].
     *
     * @param mode The transport mode to emulate.
     * @param outputDir The local directory where the generated .backup ZIP should be written.
     * @return A [ListenableFuture] wrapping the local [Path] referencing the generated backup zip.
     */
    @CheckResult
    public fun performBackupAsync(
        mode: BackupTransportMode,
        outputDir: Path,
    ): ListenableFuture<Path>

    /**
     * Triggers a host-driven backup operation of the application on this device.
     *
     * Java-compatible asynchronous alternative to `performBackup` returning a [ListenableFuture].
     *
     * @param mode The transport mode to emulate.
     * @param outputDir The local directory where the generated .backup ZIP should be written.
     * @param timeout Maximum time to wait for the backup operation to complete.
     * @return A [ListenableFuture] wrapping the local [Path] referencing the generated backup zip.
     */
    @CheckResult
    public fun performBackupAsync(
        mode: BackupTransportMode,
        outputDir: Path,
        timeout: Duration,
    ): ListenableFuture<Path>

    /**
     * Triggers a host-driven restore operation of the application on this device.
     *
     * Java-compatible asynchronous alternative to `performRestore` returning a [ListenableFuture].
     *
     * @param backupFile The local backup ZIP file generated by `performBackup`.
     * @return A [ListenableFuture] wrapping the [BackupRestoreController] orchestration instance.
     */
    @CheckResult
    public fun performRestoreAsync(backupFile: Path): ListenableFuture<BackupRestoreController>

    /**
     * Triggers a host-driven restore operation of the application on this device.
     *
     * Java-compatible asynchronous alternative to `performRestore` returning a [ListenableFuture].
     *
     * @param backupFile The local backup ZIP file generated by `performBackup`.
     * @param timeout Maximum time to wait for the restore operation to complete.
     * @return A [ListenableFuture] wrapping the [BackupRestoreController] orchestration instance.
     */
    @CheckResult
    public fun performRestoreAsync(
        backupFile: Path,
        timeout: Duration,
    ): ListenableFuture<BackupRestoreController>

    /**
     * Extracts device logcat entries. Streams logs directly to a local file on the host.
     *
     * Java-compatible asynchronous alternative to `fetchDeviceLogs` returning a [ListenableFuture].
     *
     * @param destinationPath The local file path on the host filesystem where the retrieved logs
     *   will be written.
     * @return A [ListenableFuture] wrapping the [BackupRestoreController] orchestration instance.
     */
    @CheckResult
    public fun fetchDeviceLogsAsync(
        destinationPath: Path
    ): ListenableFuture<BackupRestoreController>

    /**
     * Extracts device logcat entries. Streams logs directly to a local file on the host.
     *
     * Java-compatible asynchronous alternative to `fetchDeviceLogs` returning a [ListenableFuture].
     *
     * @param destinationPath The local file path on the host filesystem where the retrieved logs
     *   will be written.
     * @param duration Duration of historical logs to look back from the current timestamp.
     * @return A [ListenableFuture] wrapping the [BackupRestoreController] orchestration instance.
     */
    @CheckResult
    public fun fetchDeviceLogsAsync(
        destinationPath: Path,
        duration: Duration,
    ): ListenableFuture<BackupRestoreController>

    /**
     * Clears the device's logcat buffer (via logcat -c).
     *
     * Java-compatible asynchronous alternative to `clearDeviceLogs` returning a [ListenableFuture].
     *
     * @return A [ListenableFuture] wrapping the [BackupRestoreController] orchestration instance.
     */
    @CheckResult public fun clearDeviceLogsAsync(): ListenableFuture<BackupRestoreController>

    /**
     * Manually clears the target application's sandbox data (via pm clear).
     *
     * Java-compatible asynchronous alternative to `clearAppData` returning a [ListenableFuture].
     *
     * @return A [ListenableFuture] wrapping the [BackupRestoreController] orchestration instance.
     */
    @CheckResult public fun clearAppDataAsync(): ListenableFuture<BackupRestoreController>

    /**
     * Pulls an accessible file from the target device to the host filesystem.
     *
     * Java-compatible asynchronous alternative to `pullFile` returning a [ListenableFuture].
     *
     * @param devicePath The absolute path to the file on the device.
     * @param hostDestination The local file path on the host filesystem where the pulled file will
     *   be written.
     * @return A [ListenableFuture] wrapping the [BackupRestoreController] orchestration instance.
     */
    @CheckResult
    public fun pullFileAsync(
        devicePath: String,
        hostDestination: Path,
    ): ListenableFuture<BackupRestoreController>

    /**
     * Installs a specific APK file from the host workstation onto the target device.
     *
     * Java-compatible asynchronous alternative to `installApk` returning a [ListenableFuture].
     *
     * @param apkFile The local APK file on the host workstation.
     * @return A [ListenableFuture] wrapping the [BackupRestoreController] orchestration instance.
     */
    @CheckResult
    public fun installApkAsync(apkFile: Path): ListenableFuture<BackupRestoreController>

    /**
     * Installs a specific APK file from the host workstation onto the target device.
     *
     * Java-compatible asynchronous alternative to `installApk` returning a [ListenableFuture].
     *
     * @param apkFile The local APK file on the host workstation.
     * @param options Additional pm install options/flags to pass.
     * @return A [ListenableFuture] wrapping the [BackupRestoreController] orchestration instance.
     */
    @CheckResult
    public fun installApkAsync(
        apkFile: Path,
        options: List<String>,
    ): ListenableFuture<BackupRestoreController>

    /**
     * Launches the target application on this device.
     *
     * Java-compatible asynchronous alternative to `launchApp` returning a [ListenableFuture].
     *
     * @return A [ListenableFuture] wrapping the [BackupRestoreController] orchestration instance.
     */
    @CheckResult public fun launchAppAsync(): ListenableFuture<BackupRestoreController>

    /**
     * Launches the target application on this device.
     *
     * Java-compatible asynchronous alternative to `launchApp` returning a [ListenableFuture].
     *
     * @param activityClass The fully-qualified or relative Activity class name to launch.
     * @return A [ListenableFuture] wrapping the [BackupRestoreController] orchestration instance.
     */
    @CheckResult
    public fun launchAppAsync(activityClass: String?): ListenableFuture<BackupRestoreController>

    /**
     * Launches the target application on this device.
     *
     * Java-compatible asynchronous alternative to `launchApp` returning a [ListenableFuture].
     *
     * @param activityClass The fully-qualified or relative Activity class name to launch.
     * @param intentExtras Key-value string pairs to pass as intent extras.
     * @return A [ListenableFuture] wrapping the [BackupRestoreController] orchestration instance.
     */
    @CheckResult
    public fun launchAppAsync(
        activityClass: String?,
        intentExtras: Map<String, String>,
    ): ListenableFuture<BackupRestoreController>

    /**
     * Launches the target application on this device.
     *
     * Java-compatible asynchronous alternative to `launchApp` returning a [ListenableFuture].
     *
     * @param activityClass The fully-qualified or relative Activity class name to launch.
     * @param intentExtras Key-value string pairs to pass as intent extras.
     * @param action The intent action string to launch with.
     * @return A [ListenableFuture] wrapping the [BackupRestoreController] orchestration instance.
     */
    @CheckResult
    public fun launchAppAsync(
        activityClass: String?,
        intentExtras: Map<String, String>,
        action: String?,
    ): ListenableFuture<BackupRestoreController>

    /**
     * Force-stops the target application on this device.
     *
     * Java-compatible asynchronous alternative to `stopApp` returning a [ListenableFuture].
     *
     * @return A [ListenableFuture] wrapping the [BackupRestoreController] orchestration instance.
     */
    @CheckResult public fun stopAppAsync(): ListenableFuture<BackupRestoreController>
}
