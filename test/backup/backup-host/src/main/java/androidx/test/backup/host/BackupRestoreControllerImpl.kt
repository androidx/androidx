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
import com.android.backup.BackupResult
import com.android.backup.BackupService as Service
import com.android.backup.BackupType as ServiceType
import com.android.tools.environment.Logger as PlatformLogger
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.Locale
import java.util.logging.Logger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.delay
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.withTimeoutOrNull

internal class BackupRestoreControllerImpl(
    private val adbSession: AdbSession,
    override val serialNumber: String,
    override val apiLevel: Int,
    override val applicationId: String,
    private val telemetryPublisher: ((key: String, value: String) -> Unit)? = null,
) : BackupRestoreController {

    internal var lastExecutionSummary: BackupExecutionSummary? = null
        private set

    private val logger = Logger.getLogger(BackupRestoreControllerImpl::class.java.name)

    private val shell = BackupDeviceShell(adbSession, serialNumber)

    private val backupService: Service by lazy {
        val platformLogger = PlatformLogger.getInstance(BackupRestoreControllerImpl::class.java)
        Service.getInstance(adbSession, platformLogger, MIN_GMS_VERSION)
    }

    override suspend fun runBackupRestoreFlow(
        storage: StorageDomain,
        outputDir: Path,
        mode: BackupTransportMode,
    ): BackupRestoreController = runBackupRestoreFlow(listOf(storage), outputDir, mode)

    override suspend fun runBackupRestoreFlow(
        storages: List<StorageDomain>,
        outputDir: Path,
        mode: BackupTransportMode,
    ): BackupRestoreController {
        val tracker = BackupExecutionTracker(mode, storages.size)
        try {
            require(storages.isNotEmpty()) { "At least one StorageDomain must be provided." }
            logger.info(
                "Executing standard backup and restore flow for $applicationId across " +
                    "${storages.size} storage domains..."
            )
            tracker.stage(BackupExecutionStage.SEEDING) {
                for (domain in storages) {
                    logger.info("Seeding data on device via PopulateStorageAction for $domain...")
                    runStorageAction(
                        BackupRestoreController.ACTION_POPULATE_STORAGE,
                        domain,
                        BackupActionWireProtocol.populateArgs(domain),
                    )
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
                clearAppData()
            }
            tracker.stage(BackupExecutionStage.RESTORE) {
                logger.info("Restoring data via performRestore...")
                performRestore(backupFile)
            }
            tracker.stage(BackupExecutionStage.VERIFICATION) {
                for (domain in storages) {
                    logger.info(
                        "Verifying restored data on device via AssertStorageAction for $domain..."
                    )
                    runStorageAction(
                        BackupRestoreController.ACTION_ASSERT_STORAGE,
                        domain,
                        BackupActionWireProtocol.assertArgs(domain),
                    )
                }
            }
        } catch (e: Exception) {
            record(tracker.failed(e))
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

    /** Runs a storage action for [domain] and throws if it fails. */
    private suspend fun runStorageAction(
        actionClassName: String,
        domain: StorageDomain,
        args: Map<String, String>,
    ) {
        val result = runOnDevice(actionClassName = actionClassName, args = args)
        if (result is BackupActionResult.Failure) {
            throw IOException(
                "${actionClassName.substringAfterLast('.')} failed for $domain: " +
                    result.errorMessage
            )
        }
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

    override suspend fun runOnDevice(
        actionClassName: String,
        args: Map<String, String>,
        timeout: Duration,
        waitForDebugger: Boolean,
    ): BackupActionResult {
        val stdout =
            shell.instrument(
                BackupActionWireProtocol.runnerComponent(applicationId),
                BackupActionWireProtocol.instrumentationArgs(
                    actionClassName,
                    args,
                    waitForDebugger,
                ),
            )
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
                        if (it.isNotEmpty()) logger.info("Payload returned: $it")
                    }
            }
        return report.resultFor(payloadJson, actionClassName)
    }

    /** Reads the payload the runner wrote to [devicePath], then deletes that file. */
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

    /** Backs up the package to the local transport and returns a placeholder archive. */
    private suspend fun runLocalBackup(outputDir: Path): File {
        logger.info("Executing robust local transport backup simulation...")
        shell.unstopPackage(applicationId)
        shell.exec("bmgr enable true")
        withLocalTransport {
            shell.exec("bmgr backupnow @pm@")
            shell.exec("bmgr backupnow ${BackupDeviceShell.quoteIfNeeded(applicationId)}")
        }
        val archive = prepareBackupFile(outputDir, BackupTransportMode.LOCAL)
        ZipOutputStream(archive.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("token.txt"))
            zip.write("1".toByteArray())
            zip.closeEntry()
        }
        return archive
    }

    /** Restores the package from the local transport and waits for the restore pass to end. */
    private suspend fun runLocalRestore() {
        logger.info("Executing robust local transport restore simulation...")
        withLocalTransport {
            shell.exec("bmgr restore 1 ${BackupDeviceShell.quoteIfNeeded(applicationId)}")
            shell.exec("bmgr run")
            waitForRestorePassCompletion(RESTORE_TIMEOUT)
        }
    }

    /** Runs [block] with the local transport selected, then selects the original one again. */
    private suspend fun withLocalTransport(block: suspend () -> Unit) {
        val originalTransport =
            shell
                .exec("bmgr list transports")
                .stdout
                .lineSequence()
                .firstOrNull { it.trim().startsWith("*") }
                ?.replace("*", "")
                ?.trim() ?: DEFAULT_TRANSPORT
        shell.exec("bmgr transport $LOCAL_TRANSPORT")
        block()
        if (originalTransport != LOCAL_TRANSPORT) {
            shell.exec("bmgr transport ${BackupDeviceShell.quote(originalTransport)}")
        }
    }

    /**
     * Waits for BackupManagerService to dispatch and complete the restore pass, or until [timeout]
     * expires.
     */
    private suspend fun waitForRestorePassCompletion(timeout: Duration) {
        val totalTimeoutMs = timeout.toMillis()
        val dispatchTimeoutMs =
            (totalTimeoutMs / 2)
                .coerceAtLeast(minOf(totalTimeoutMs, 5000L))
                .coerceAtMost(totalTimeoutMs)
        val startNanos = System.nanoTime()

        // Wait for system_server to dispatch and register the restore pass
        val started =
            withTimeoutOrNull(dispatchTimeoutMs) {
                while (!isRestoreInProgress()) {
                    delay(RESTORE_DISPATCH_POLL_INTERVAL_MS)
                }
                true
            }

        if (started == true) {
            val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000
            val remainingMs = (totalTimeoutMs - elapsedMs).coerceAtLeast(1000L)
            // Wait until the active restore pass completes
            val completed =
                withTimeoutOrNull(remainingMs) {
                    while (isRestoreInProgress()) {
                        delay(RESTORE_COMPLETION_POLL_INTERVAL_MS)
                    }
                    true
                }

            if (completed == null) {
                logger.warning(
                    "Restore pass active polling reached timeout (${timeout.toSeconds()}s); proceeding."
                )
            }
        } else {
            logger.warning(
                "Restore pass registration was not detected within ${dispatchTimeoutMs}ms; proceeding."
            )
        }
    }

    private suspend fun isRestoreInProgress(): Boolean {
        val dumpsys = shell.exec("dumpsys backup").stdout
        return RESTORE_SESSION_REGEX.containsMatchIn(dumpsys) ||
            RESTORE_IN_PROGRESS_REGEX.containsMatchIn(dumpsys)
    }

    override suspend fun performBackup(
        mode: BackupTransportMode,
        outputDir: Path,
        timeout: Duration,
    ): Path {
        if (mode == BackupTransportMode.LOCAL) {
            return runLocalBackup(outputDir).toPath()
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
            is BackupResult.Success,
            is BackupResult.WithoutAppData -> {
                logger.info(
                    "BackupService successfully created production backup archive: " +
                        backupFile.absolutePath
                )
                return backupFile.toPath()
            }
            is BackupResult.Error -> throw result.throwable
        }
    }

    override suspend fun performRestore(
        backupFile: Path,
        timeout: Duration,
    ): BackupRestoreController {
        if (backupFile.fileName.toString() == backupFileName(BackupTransportMode.LOCAL)) {
            runLocalRestore()
            return this
        }
        // Moves any foreground activity to the background so the restore agent can bind.
        shell.pressHome()

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
            is BackupResult.Success ->
                logger.info("BackupService successfully executed production restore.")
            is BackupResult.Error -> throw result.throwable
            else -> logger.warning("Restore finished with result: $result")
        }
        return this
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
        storages: List<StorageDomain>,
        outputDir: Path,
        mode: BackupTransportMode,
    ): ListenableFuture<BackupRestoreController> = asFuture {
        runBackupRestoreFlow(storages, outputDir, mode)
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
        adbSession.close()
    }

    private companion object {
        /**
         * Minimum Google Play Services (GmsCore) version code (24.09.13) required by the underlying
         * backup transport emulation service library.
         */
        const val MIN_GMS_VERSION = 240913000

        const val LOCAL_TRANSPORT = "com.android.localtransport/.LocalTransport"

        /** Assumed to be the selected transport when `bmgr` does not mark one. */
        const val DEFAULT_TRANSPORT = "com.google.android.gms/.backup.BackupTransportService"

        val RESTORE_TIMEOUT: Duration = Duration.ofSeconds(15)
        const val RESTORE_DISPATCH_POLL_INTERVAL_MS = 250L
        const val RESTORE_COMPLETION_POLL_INTERVAL_MS = 500L

        val RESTORE_SESSION_REGEX =
            Regex("""(?i)(?:Restore session|Active restore):\s*(?!null\b|none\b)\S+""")
        val RESTORE_IN_PROGRESS_REGEX = Regex("""(?i)Restore (?:pass )?in progress:\s*true\b""")

        fun backupFileName(mode: BackupTransportMode): String =
            "backup_${mode.toString().lowercase(Locale.ROOT)}_device.zip"

        /** Returns a fresh location in [outputDir] for the backup archive of [mode]. */
        fun prepareBackupFile(outputDir: Path, mode: BackupTransportMode): File {
            val file = File(outputDir.toFile(), backupFileName(mode))
            if (file.exists()) {
                file.delete()
            }
            file.deleteOnExit()
            file.parentFile?.mkdirs()
            return file
        }
    }
}
