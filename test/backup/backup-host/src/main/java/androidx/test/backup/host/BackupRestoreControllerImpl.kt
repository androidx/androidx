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

import androidx.test.backup.host.BackupActionWireProtocol.MAX_INLINE_COMMAND_BYTES
import com.android.adblib.AdbSession
import com.android.backup.BackupResult
import com.android.backup.BackupService as Service
import com.android.backup.BackupType as ServiceType
import com.android.backup.ErrorCode
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.Locale
import java.util.logging.Logger
import kotlin.time.Duration as KotlinDuration
import kotlin.time.toKotlinDuration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The [BackupRestoreController] of [applicationId] on the device [serialNumber].
 *
 * @param testApplicationId the package of the test APK whose runner runs device actions, or null to
 *   look the runner up on the device
 */
internal class BackupRestoreControllerImpl(
    private val adbSession: AdbSession,
    override val serialNumber: String,
    override val apiLevel: Int,
    override val applicationId: String,
    private val testApplicationId: String? = null,
    private val telemetryPublisher: ((key: String, value: String) -> Unit)? = null,
    private val cleanupTimeout: KotlinDuration = DEFAULT_CLEANUP_TIMEOUT,
    backupServiceProvider: () -> Service = { createBackupService(adbSession) },
) : BackupRestoreController {

    internal var lastExecutionSummary: BackupExecutionSummary? = null
        private set

    private val logger = Logger.getLogger(BackupRestoreControllerImpl::class.java.name)

    private val shell = BackupDeviceShell(adbSession, serialNumber)

    private val localTransport = BackupLocalTransport(shell, applicationId, cleanupTimeout)

    private val backupService: Service by lazy(backupServiceProvider)

    override suspend fun runBackupRestoreFlow(
        storage: StorageDomain,
        outputDir: Path,
        mode: BackupTransportMode,
        restoreOn: BackupRestoreController,
    ): BackupRestoreController = runBackupRestoreFlow(listOf(storage), outputDir, mode, restoreOn)

    override suspend fun runBackupRestoreFlow(
        storages: List<StorageDomain>,
        outputDir: Path,
        mode: BackupTransportMode,
        restoreOn: BackupRestoreController,
    ): BackupRestoreController {
        val tracker = BackupExecutionTracker(mode, storages.size)
        try {
            require(storages.isNotEmpty()) { "At least one StorageDomain must be provided." }
            require(storages.any { it !is StorageDomain.ExcludedData }) {
                "All storages are ExcludedData, so the flow can't verify that the restore " +
                    "happened. Add a domain that the restore brings back."
            }
            require(restoreOn.applicationId == applicationId) {
                "restoreOn applicationId (${restoreOn.applicationId}) must match this " +
                    "controller's applicationId ($applicationId)."
            }
            require(mode != BackupTransportMode.LOCAL || restoreOn.serialNumber == serialNumber) {
                "A LOCAL backup stays in the local transport of $serialNumber, so " +
                    "${restoreOn.serialNumber} can't restore it. To restore on another device, use " +
                    "a cloud or device-to-device mode."
            }
            logger.info(
                "Executing standard backup and restore flow for $applicationId across " +
                    "${storages.size} storage domains..."
            )
            val verifyCalls =
                tracker.stage(BackupExecutionStage.SEEDING) {
                    storages.map { domain ->
                        logger.info("Seeding data on device for $domain...")
                        val data =
                            runStorageAction(domain, BackupActionWireProtocol.populateCall(domain))
                        BackupActionWireProtocol.verifyCall(domain, data)
                    }
                }
            val backupFile =
                tracker.stage(BackupExecutionStage.BACKUP) {
                    // Stopping the app flushes its pending writes to disk before the backup.
                    stopApp()
                    logger.info("Performing backup via performBackup ($mode)...")
                    performBackup(mode, outputDir)
                }
            tracker.stage(BackupExecutionStage.CLEAR_DATA) {
                logger.info("Clearing app sandbox via clearAppData...")
                restoreOn.clearAppData()
            }
            tracker.stage(BackupExecutionStage.RESTORE) {
                logger.info("Restoring data via performRestore...")
                restoreOn.performRestore(backupFile)
            }
            tracker.stage(BackupExecutionStage.VERIFICATION) {
                storages.zip(verifyCalls).forEach { (domain, verifyCall) ->
                    logger.info("Verifying restored data on device for $domain...")
                    restoreOn.runStorageAction(domain, verifyCall)
                }
            }
        } catch (e: Exception) {
            // A cancelled flow has no outcome to report. A CancellationException thrown while the
            // flow is still active, such as the timeout of an inner withTimeout, is a failure.
            if (e !is CancellationException || currentCoroutineContext().isActive) {
                record(tracker.failed(e))
            }
            throw e
        }
        val summary = tracker.succeeded()
        record(summary)
        logger.info(
            "Standard backup and restore flow executed successfully across all storage " +
                "domains with 100% data integrity! (${summary.totalDuration.toMillis()}ms)"
        )
        return this
    }

    /**
     * Runs the action of [call] for [domain], throws if it fails, and returns the action's data.
     */
    private suspend fun BackupRestoreController.runStorageAction(
        domain: StorageDomain,
        call: Pair<String, Map<String, String>>,
    ): Map<String, String> {
        val (actionClassName, args) = call
        val result = runOnDevice(actionClassName = actionClassName, args = args)
        if (result is BackupActionResult.Failure) {
            throw IOException(
                "${actionClassName.substringAfterLast('.')} failed for $domain: " +
                    result.errorMessage
            )
        }
        return result.data
    }

    /** Stores [summary] as the latest one and publishes it; publishing never fails the flow. */
    private fun record(summary: BackupExecutionSummary) {
        lastExecutionSummary = summary
        val publisher = telemetryPublisher ?: return
        try {
            summary.publishTo(publisher)
        } catch (e: Throwable) {
            logger.warning("Failed to publish telemetry: ${e.message}")
        }
    }

    /**
     * Returns the result of [block], or null when it does not complete within [timeout].
     *
     * On timeout, force-stops the app, so that nothing [block] started keeps running on the device.
     * Cancellation of the caller is propagated, not reported as a timeout.
     */
    private suspend fun <T : Any> withinTimeout(timeout: Duration, block: suspend () -> T): T? {
        require(!timeout.isNegative && !timeout.isZero) { "Timeout must be positive: $timeout" }
        // The kotlin.time overload rounds a sub-millisecond remainder up instead of truncating it.
        val result = withTimeoutOrNull(timeout.toKotlinDuration()) { block() }
        if (result == null) {
            logger.warning(
                "Timed out after ${timeout.toKotlinDuration()}; force-stopping $applicationId."
            )
            try {
                withTimeoutOrNull(cleanupTimeout) { stopApp() }
                    ?: logger.warning("Force-stopping $applicationId did not complete.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.warning("Failed to force-stop $applicationId: ${e.message}")
            }
        }
        return result
    }

    override suspend fun runOnDevice(
        actionClassName: String,
        args: Map<String, String>,
        timeout: Duration,
        waitForDebugger: Boolean,
    ): BackupActionResult =
        withinTimeout(timeout) { runAction(actionClassName, args, waitForDebugger) }
            ?: BackupActionResult.Failure("Timed out after ${timeout.toKotlinDuration()}")

    private suspend fun runAction(
        actionClassName: String,
        args: Map<String, String>,
        waitForDebugger: Boolean,
    ): BackupActionResult {
        val instrumentationArgs =
            BackupActionWireProtocol.instrumentationArgs(actionClassName, args, waitForDebugger)
        val runners = installedRunners()
        val runner =
            findRunner(runners) ?: return BackupActionResult.Failure(noRunnerMessage(runners))
        val command = BackupDeviceShell.instrumentCommand(runner, instrumentationArgs)
        val stdout =
            if (command.toByteArray(Charsets.UTF_8).size <= MAX_INLINE_COMMAND_BYTES) {
                shell.instrument(runner, instrumentationArgs)
            } else {
                instrumentWithArgsFile(runner, actionClassName, args, waitForDebugger)
            }
        val report = BackupRunnerReport.parse(stdout)
        val payloadPath = report.payloadPath
        val payloadJson =
            when {
                payloadPath != null ->
                    try {
                        pullOverflowPayload(payloadPath)
                    } catch (e: Exception) {
                        return BackupActionResult.Failure(
                            "Failed to pull Binder overflow payload: " + e.message
                        )
                    }
                // A failure with no payload is a crash or a runner error; nothing more to read. A
                // failure that does carry a payload is an action reporting its own failure, and the
                // payload is still read: it names the specific error, and an overflow file must not
                // be left behind.
                report.inlinePayload == null && report.runnerFailure != null ->
                    return report.runnerFailure
                else ->
                    report.inlinePayload.orEmpty().also {
                        logger.info("Executed $actionClassName on device.")
                        if (it.isNotEmpty()) logger.fine("Payload returned: $it")
                    }
            }
        return report.resultFor(payloadJson, actionClassName)
    }

    /**
     * Runs [actionClassName] with the instrumentation [runner], passing it [args] in a file that is
     * pushed to the device and deleted afterwards, and returns what the instrumentation printed.
     */
    private suspend fun instrumentWithArgsFile(
        runner: String,
        actionClassName: String,
        args: Map<String, String>,
        waitForDebugger: Boolean,
    ): String {
        val argsFile = BackupActionWireProtocol.newArgsFilePath()
        val hostFile = Files.createTempFile("backup_args_", ".json")
        try {
            Files.write(hostFile, BackupActionWireProtocol.encodeArgsFile(args).toByteArray())
            return withCleanup(cleanup = { shell.removeFile(argsFile) }, cleanupTimeout) {
                logger.info("Pushing the arguments of $actionClassName to $argsFile...")
                shell.pushFile(hostFile, argsFile, BackupDeviceShell.SHELL_ONLY_FILE_MODE)
                shell.instrument(
                    runner,
                    BackupActionWireProtocol.argsFileInstrumentationArgs(
                        actionClassName,
                        argsFile,
                        waitForDebugger,
                    ),
                )
            }
        } finally {
            Files.deleteIfExists(hostFile)
        }
    }

    /**
     * Reads the payload that a runner of an older version wrote to [devicePath], then deletes it.
     */
    private suspend fun pullOverflowPayload(devicePath: String): String {
        val localFile = File.createTempFile("overflow_", ".json")
        try {
            shell.pullFile(devicePath, localFile.toPath())
            shell.removeFile(devicePath)
            return BackupRunnerReport.parseOverflowFile(localFile.readText())
        } finally {
            localFile.delete()
        }
    }

    /** Returns the instrumentation components of the runners installed for the app. */
    private suspend fun installedRunners(): List<String> =
        shell.listInstrumentations(applicationId).filter {
            it.substringAfter('/') == BackupActionWireProtocol.RUNNER_CLASS
        }

    /**
     * Returns the instrumentation component of the runner among [runners], the runners installed
     * for the app: the one of [testApplicationId] when it is set, or null if it isn't among them,
     * or else the only runner, or else the one AGP gives the test APK.
     *
     * Without any installed runner, `am instrument` then reports that the test APK is missing.
     */
    private fun findRunner(runners: List<String>): String? {
        testApplicationId?.let {
            return "$it/${BackupActionWireProtocol.RUNNER_CLASS}"
                .takeIf { runner -> runner in runners }
        }
        val defaultRunner = BackupActionWireProtocol.runnerComponent(applicationId)
        if (runners.size > 1 && defaultRunner !in runners) {
            logger.warning("Several test APKs have a runner for $applicationId: $runners")
        }
        return runners.singleOrNull() ?: defaultRunner
    }

    /**
     * Returns why [testApplicationId] can't run device actions, naming the test APKs of [runners].
     */
    private fun noRunnerMessage(runners: List<String>): String {
        val testApks = runners.map { it.substringBefore('/') }.ifEmpty { listOf("none") }
        return "Test APK $testApplicationId isn't installed or has no " +
            "${BackupActionWireProtocol.RUNNER_CLASS} for $applicationId. Test APKs with one: " +
            testApks.joinToString()
    }

    override suspend fun performBackup(
        mode: BackupTransportMode,
        outputDir: Path,
        timeout: Duration,
    ): Path =
        withinTimeout(timeout) { backup(mode, outputDir) }
            ?: throw IOException("Backup ($mode) timed out after ${timeout.toKotlinDuration()}")

    private suspend fun backup(mode: BackupTransportMode, outputDir: Path): Path {
        if (mode == BackupTransportMode.LOCAL) {
            val archive = prepareBackupFile(outputDir, mode)
            localTransport.backup(archive)
            return archive.toPath()
        }
        val serviceType =
            when (mode) {
                BackupTransportMode.DEVICE_TO_DEVICE -> ServiceType.DEVICE_TO_DEVICE
                BackupTransportMode.CLOUD_ENCRYPTED -> ServiceType.CLOUD
                BackupTransportMode.CLOUD_UNENCRYPTED -> ServiceType.CLOUD_UNENCRYPTED
                else -> throw IllegalArgumentException("Unsupported backup transport mode: $mode")
            }
        val backupFile = prepareBackupFile(outputDir, mode)
        shell.unstopPackage(applicationId)

        logger.info(
            "Attempting full-fidelity production backup via BackupService for type $mode..."
        )
        val result =
            backupService.backup(
                serialNumber = serialNumber,
                applicationId = applicationId,
                type = serviceType,
                backupFile = backupFile.toPath(),
                listener = null,
            )
        when (result) {
            is BackupResult.Success -> {
                logger.info(
                    "BackupService successfully created production backup archive: " +
                        backupFile.absolutePath
                )
                return backupFile.toPath()
            }
            // The archive holds only data that is kept even for apps that opt out of backups,
            // such as Block Store data, so restoring it cannot restore the app's own data.
            is BackupResult.WithoutAppData -> {
                backupFile.delete()
                throw IOException(
                    "Backup ($mode) of $applicationId contains no app data, because the app " +
                        "does not allow backups (android:allowBackup=\"false\")"
                )
            }
            is BackupResult.Error -> throw serviceFailure(result, BackupErrorCode.BACKUP_FAILED)
        }
    }

    override suspend fun performRestore(
        backupFile: Path,
        timeout: Duration,
    ): BackupRestoreController {
        withinTimeout(timeout) { restore(backupFile, timeout) }
            ?: throw BackupRestoreException(
                BackupErrorCode.RESTORE_POLL_TIMEOUT,
                "Restore timed out after ${timeout.toKotlinDuration()}",
            )
        return this
    }

    private suspend fun restore(backupFile: Path, timeout: Duration) {
        val localBackup = BackupLocalTransport.localBackupOf(backupFile)
        if (localBackup != null) {
            localTransport.restore(backupFile, localBackup, timeout.toKotlinDuration())
            return
        }
        requireGmsRestoreSupport()
        // Moves any foreground activity to the background so the restore agent can bind.
        shell.pressHome()
        val restoreStart = deviceTimeOrNull()

        logger.info(
            "Attempting full-fidelity production restore via BackupService for file: " +
                "${backupFile.toAbsolutePath()}..."
        )
        val result =
            backupService.restore(
                serialNumber = serialNumber,
                backupFile = backupFile,
                listener = null,
            )
        when (result) {
            is BackupResult.Success -> {
                restoreStart?.let { checkRestoreNotSkipped(backupFile, it) }
                logger.info("BackupService successfully executed production restore.")
            }
            is BackupResult.WithoutAppData ->
                throw IOException(
                    "Restore of $applicationId from ${backupFile.fileName} restored no app data"
                )
            is BackupResult.Error -> throw serviceFailure(result, BackupErrorCode.RESTORE_FAILED)
        }
    }

    /** Returns the time since the epoch on the device clock, or null if it cannot be read. */
    private suspend fun deviceTimeOrNull(): Duration? =
        try {
            shell.timeSinceEpoch()
        } catch (e: IOException) {
            logger.warning("Failed to read the device clock: ${e.message}")
            null
        }

    /**
     * Throws if Google Play services on the device is too old to restore cloud and device-to-device
     * backups: on Android 16 QPR2 (API level 36.1) and higher, versions lower than 26.33.30 take
     * them for cross-platform restores, so the restore fails. A device without Google Play services
     * is left to the backup service, which reports it.
     */
    private suspend fun requireGmsRestoreSupport() {
        // Reads the minor API level as Build.VERSION.SDK_INT_FULL does.
        val isApi36Point1OrHigher =
            apiLevel > 36 ||
                (apiLevel == 36 &&
                    (shell.getProperty("ro.build.version.sdk_minor")?.toIntOrNull() ?: 0) > 0)
        if (!isApi36Point1OrHigher) return
        val versionCode = shell.packageVersionCode("com.google.android.gms") ?: return
        if (versionCode >= MIN_GMS_VERSION_CODE_API_36_1) return
        // The version code is the version name, such as 26.33.30 for 263330038, and three digits.
        val version = versionCode / 1000
        val versionName =
            String.format(
                Locale.ROOT,
                "%d.%02d.%02d",
                version / 10000,
                version / 100 % 100,
                version % 100,
            )
        throw BackupRestoreException(
            BackupErrorCode.GMSCORE_OUTDATED_OR_MISSING,
            "Can't restore the backup on $serialNumber: Google Play services $versionName is too " +
                "old. On Android 16 QPR2 (API level 36.1) and higher, restoring cloud and " +
                "device-to-device backups requires Google Play services 26.33.30 or higher. " +
                "Update Google Play services through the Play Store on the device, or use a " +
                "system image that comes with a newer version",
        )
    }

    /**
     * Throws if Google Play services logged since [start] that it skipped the restore of the app.
     *
     * It reports a restore that skipped the app as successful, so only its log tells. When logcat
     * cannot be read, the restore is taken as successful.
     *
     * @throws IOException that tells why the app was skipped
     */
    private suspend fun checkRestoreNotSkipped(backupFile: Path, start: Duration) {
        val logcat =
            try {
                shell.dumpLogcatSince(start)
            } catch (e: IOException) {
                logger.warning("Failed to read logcat to check the restore: ${e.message}")
                return
            }
        val reason = restoreSkipReason(logcat, applicationId) ?: return
        throw IOException(
            "Restore of $applicationId from ${backupFile.fileName} restored no app data: $reason"
        )
    }

    override suspend fun fetchDeviceLogs(
        destinationPath: Path,
        duration: Duration,
    ): BackupRestoreController {
        Files.write(destinationPath, shell.dumpLogcat(duration).toByteArray(Charsets.UTF_8))
        return this
    }

    override suspend fun clearDeviceLogs(): BackupRestoreController {
        shell.clearLogcat()
        return this
    }

    override suspend fun clearAppData(): BackupRestoreController {
        shell.clearPackageData(applicationId)
        return this
    }

    override suspend fun pullFile(
        devicePath: String,
        hostDestination: Path,
    ): BackupRestoreController {
        shell.pullFile(devicePath, hostDestination)
        return this
    }

    override suspend fun installApk(apkFile: Path, options: List<String>): BackupRestoreController {
        shell.installPackage(apkFile, options)
        return this
    }

    override suspend fun uninstallApp(): BackupRestoreController {
        shell.uninstallPackage(applicationId)
        return this
    }

    /**
     * Returns the installed APK paths of the app, of the other packages whose name contains its
     * application ID, such as its test APK, and of the other packages with an instrumentation that
     * targets the app, or null if none of them is installed.
     *
     * The paths change whenever one of these packages is installed again.
     */
    suspend fun installedApkPaths(): List<String>? {
        val otherTestPackages =
            shell
                .listInstrumentations(applicationId)
                .map { it.substringBefore('/') }
                .filterNot { it.contains(applicationId) }
                .distinct()
        val paths =
            shell.listPackagePaths(applicationId) +
                otherTestPackages.flatMap { shell.listPackagePaths(it) }
        return paths.distinct().sorted().ifEmpty { null }
    }

    /** Turns the screen on and dismisses the keyguard. */
    suspend fun wakeAndDismissKeyguard() {
        shell.wakeAndDismissKeyguard()
    }

    override suspend fun launchApp(
        activityClass: String?,
        intentExtras: Map<String, String>,
        action: String?,
    ): BackupRestoreController {
        // Keeps the launched app visible on the device screen.
        shell.wakeAndDismissKeyguard()
        val component =
            when {
                activityClass != null -> qualifyActivity(activityClass)
                action == null -> shell.resolveLauncherActivity(applicationId)
                else -> null
            }
        shell.startActivity(
            action = action ?: BackupDeviceShell.ACTION_MAIN,
            category = if (action == null) BackupDeviceShell.CATEGORY_LAUNCHER else null,
            component = component,
            packageName = applicationId,
            stringExtras = intentExtras,
        )
        return this
    }

    /** Returns [activityClass] as a `package/class` component of the app under test. */
    private fun qualifyActivity(activityClass: String): String =
        when {
            activityClass.contains("/") -> activityClass
            activityClass.startsWith(".") -> "$applicationId/$activityClass"
            !activityClass.contains(".") -> "$applicationId/.$activityClass"
            else -> "$applicationId/$activityClass"
        }

    override suspend fun stopApp(): BackupRestoreController {
        shell.forceStop(applicationId)
        return this
    }

    // --- ListenableFuture / Java interoperability implementations ---

    private fun <T> asFuture(block: suspend () -> T): ListenableFuture<T> =
        adbSession.scope.future { block() }

    override fun runOnDeviceAsync(actionClassName: String): ListenableFuture<BackupActionResult> =
        asFuture {
            runOnDevice(actionClassName)
        }

    override fun runOnDeviceAsync(
        actionClassName: String,
        args: Map<String, String>,
    ): ListenableFuture<BackupActionResult> = asFuture { runOnDevice(actionClassName, args) }

    override fun runOnDeviceAsync(
        actionClassName: String,
        args: Map<String, String>,
        timeout: Duration,
    ): ListenableFuture<BackupActionResult> = asFuture {
        runOnDevice(actionClassName, args, timeout)
    }

    override fun runOnDeviceAsync(
        actionClassName: String,
        args: Map<String, String>,
        timeout: Duration,
        waitForDebugger: Boolean,
    ): ListenableFuture<BackupActionResult> = asFuture {
        runOnDevice(actionClassName, args, timeout, waitForDebugger)
    }

    override fun runBackupRestoreFlowAsync(
        storage: StorageDomain,
        outputDir: Path,
        mode: BackupTransportMode,
    ): ListenableFuture<BackupRestoreController> = asFuture {
        runBackupRestoreFlow(storage, outputDir, mode)
    }

    override fun runBackupRestoreFlowAsync(
        storage: StorageDomain,
        outputDir: Path,
        mode: BackupTransportMode,
        restoreOn: BackupRestoreController,
    ): ListenableFuture<BackupRestoreController> = asFuture {
        runBackupRestoreFlow(storage, outputDir, mode, restoreOn)
    }

    override fun runBackupRestoreFlowAsync(
        storages: List<StorageDomain>,
        outputDir: Path,
        mode: BackupTransportMode,
    ): ListenableFuture<BackupRestoreController> = asFuture {
        runBackupRestoreFlow(storages, outputDir, mode)
    }

    override fun runBackupRestoreFlowAsync(
        storages: List<StorageDomain>,
        outputDir: Path,
        mode: BackupTransportMode,
        restoreOn: BackupRestoreController,
    ): ListenableFuture<BackupRestoreController> = asFuture {
        runBackupRestoreFlow(storages, outputDir, mode, restoreOn)
    }

    override fun performBackupAsync(
        mode: BackupTransportMode,
        outputDir: Path,
    ): ListenableFuture<Path> = asFuture { performBackup(mode, outputDir) }

    override fun performBackupAsync(
        mode: BackupTransportMode,
        outputDir: Path,
        timeout: Duration,
    ): ListenableFuture<Path> = asFuture { performBackup(mode, outputDir, timeout) }

    override fun performRestoreAsync(backupFile: Path): ListenableFuture<BackupRestoreController> =
        asFuture {
            performRestore(backupFile)
        }

    override fun performRestoreAsync(
        backupFile: Path,
        timeout: Duration,
    ): ListenableFuture<BackupRestoreController> = asFuture { performRestore(backupFile, timeout) }

    override fun fetchDeviceLogsAsync(
        destinationPath: Path
    ): ListenableFuture<BackupRestoreController> = asFuture { fetchDeviceLogs(destinationPath) }

    override fun fetchDeviceLogsAsync(
        destinationPath: Path,
        duration: Duration,
    ): ListenableFuture<BackupRestoreController> = asFuture {
        fetchDeviceLogs(destinationPath, duration)
    }

    override fun clearDeviceLogsAsync(): ListenableFuture<BackupRestoreController> = asFuture {
        clearDeviceLogs()
    }

    override fun clearAppDataAsync(): ListenableFuture<BackupRestoreController> = asFuture {
        clearAppData()
    }

    override fun pullFileAsync(
        devicePath: String,
        hostDestination: Path,
    ): ListenableFuture<BackupRestoreController> = asFuture {
        pullFile(devicePath, hostDestination)
    }

    override fun installApkAsync(apkFile: Path): ListenableFuture<BackupRestoreController> =
        asFuture {
            installApk(apkFile)
        }

    override fun installApkAsync(
        apkFile: Path,
        options: List<String>,
    ): ListenableFuture<BackupRestoreController> = asFuture { installApk(apkFile, options) }

    override fun uninstallAppAsync(): ListenableFuture<BackupRestoreController> = asFuture {
        uninstallApp()
    }

    override fun launchAppAsync(): ListenableFuture<BackupRestoreController> = asFuture {
        launchApp()
    }

    override fun launchAppAsync(activityClass: String?): ListenableFuture<BackupRestoreController> =
        asFuture {
            launchApp(activityClass)
        }

    override fun launchAppAsync(
        activityClass: String?,
        intentExtras: Map<String, String>,
    ): ListenableFuture<BackupRestoreController> = asFuture {
        launchApp(activityClass, intentExtras)
    }

    override fun launchAppAsync(
        activityClass: String?,
        intentExtras: Map<String, String>,
        action: String?,
    ): ListenableFuture<BackupRestoreController> = asFuture {
        launchApp(activityClass, intentExtras, action)
    }

    override fun stopAppAsync(): ListenableFuture<BackupRestoreController> = asFuture { stopApp() }

    override fun close() {
        // Holds no resources of its own. The ADB session can be shared, and its owner closes it.
    }

    private companion object {
        /**
         * Minimum Google Play Services (GmsCore) version code (24.09.13) required by the underlying
         * backup transport emulation service library.
         */
        const val MIN_GMS_VERSION = 240913000

        /**
         * The lowest version code of Google Play services, 26.33.30, that restores cloud and
         * device-to-device backups on Android 16 QPR2 (API level 36.1) and higher.
         */
        const val MIN_GMS_VERSION_CODE_API_36_1 = 263330000L

        fun createBackupService(adbSession: AdbSession): Service =
            Service.getInstance(
                adbSession,
                BackupServiceLogger(BackupRestoreControllerImpl::class.java.name),
                MIN_GMS_VERSION,
            )

        /**
         * Returns [error] of the backup service as the [IOException] that [BackupRestoreController]
         * documents: a [BackupRestoreException] with the message of the failure, and the cause that
         * the error code of the service names, or else [fallback]. Cancellations and errors are
         * returned unchanged, so a timeout still cancels the call.
         */
        fun serviceFailure(error: BackupResult.Error, fallback: BackupErrorCode): Throwable =
            when (val failure = error.throwable) {
                is CancellationException,
                is Error -> failure
                else ->
                    BackupRestoreException(
                        causeOf(error.errorCode) ?: fallback,
                        failure.message,
                        failure,
                    )
            }

        /** Returns the cause that [errorCode] of the backup service names, if it names one. */
        private fun causeOf(errorCode: ErrorCode): BackupErrorCode? =
            when (errorCode) {
                ErrorCode.GMSCORE_NOT_FOUND,
                ErrorCode.GMSCORE_IS_TOO_OLD,
                ErrorCode.GMSCORE_IS_TOO_OLD_NO_PLAY_STORE,
                ErrorCode.PLAY_STORE_NOT_INSTALLED -> BackupErrorCode.GMSCORE_OUTDATED_OR_MISSING
                ErrorCode.CANNOT_ENABLE_BMGR,
                ErrorCode.BACKUP_NOT_SUPPORTED,
                ErrorCode.BACKUP_NOT_ACTIVATED,
                ErrorCode.BACKUP_MANAGER_IS_NOT_RUNNING,
                ErrorCode.TRANSPORT_NOT_SELECTED,
                ErrorCode.TRANSPORT_INIT_FAILED -> BackupErrorCode.BMGR_INIT_FAILED
                else -> null
            }

        fun backupFileName(mode: BackupTransportMode): String =
            "backup_${mode.toString().lowercase(Locale.ROOT)}_device.zip"

        /** Returns a fresh location in [outputDir] for the backup archive of [mode]. */
        fun prepareBackupFile(outputDir: Path, mode: BackupTransportMode): File {
            val file = File(outputDir.toFile(), backupFileName(mode))
            if (file.exists()) {
                file.delete()
            }
            file.parentFile?.mkdirs()
            return file
        }
    }
}
