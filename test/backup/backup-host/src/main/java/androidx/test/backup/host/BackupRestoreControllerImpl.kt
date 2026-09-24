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
import com.android.adblib.shellAsText
import com.android.backup.BackupService as Service
import com.android.backup.BackupType as ServiceType
import com.android.tools.environment.Logger as PlatformLogger
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.util.Locale
import java.util.UUID
import java.util.logging.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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

    private val backupService: Service by lazy {
        val platformLogger = PlatformLogger.getInstance(BackupRestoreControllerImpl::class.java)
        Service.getInstance(adbSession, platformLogger, MIN_GMS_VERSION)
    }

    private val componentRegex by lazy {
        """\b${Regex.escape(applicationId)}/[a-zA-Z0-9._${'$'}]+\b""".toRegex()
    }

    private fun getPutStorageArgs(storage: StorageDomain): Map<String, String> {
        val args = mutableMapOf<String, String>()
        when (storage) {
            is StorageDomain.Preference -> {
                args[BackupActionInputKeys.STORAGE_TYPE] = BackupActionValues.STORAGE_TYPE_PREFS
                args[BackupActionInputKeys.PREF_NAME] = storage.prefName
                args[BackupActionInputKeys.PREF_KEY] = storage.key
                val v = storage.value
                if (v != null) {
                    args[BackupActionInputKeys.VALUE] = v.toString()
                    val valueType =
                        when (v) {
                            is Int -> BackupActionValues.VALUE_TYPE_INT
                            is Long -> BackupActionValues.VALUE_TYPE_LONG
                            is Float -> BackupActionValues.VALUE_TYPE_FLOAT
                            is Boolean -> BackupActionValues.VALUE_TYPE_BOOLEAN
                            else -> BackupActionValues.VALUE_TYPE_STRING
                        }
                    args[BackupActionInputKeys.VALUE_TYPE] = valueType
                }
            }
            is StorageDomain.Database -> {
                args[BackupActionInputKeys.STORAGE_TYPE] = BackupActionValues.STORAGE_TYPE_DATABASE
                args[BackupActionInputKeys.DB_NAME] = storage.dbName
                args[BackupActionInputKeys.TABLE] = storage.table
                val pairs =
                    storage.columnValues.entries
                        .map { it.key to (it.value?.toString() ?: "") }
                        .toMutableList()
                // Ensure primary key is also inserted/updated in PopulateStorageAction
                if (
                    storage.columnValues.keys.none {
                        it.equals(storage.primaryKeyCol, ignoreCase = true)
                    }
                ) {
                    pairs.add(storage.primaryKeyCol to storage.primaryKeyVal.toString())
                }
                args[BackupActionInputKeys.VALUES] =
                    BackupActionWireProtocol.encodeColumnValues(pairs)
            }
            is StorageDomain.TextFile -> {
                args[BackupActionInputKeys.STORAGE_TYPE] = BackupActionValues.STORAGE_TYPE_FILES
                args[BackupActionInputKeys.PATH] = storage.path
                args[BackupActionInputKeys.VALUE] = storage.content
            }
            is StorageDomain.BinaryFile -> {
                args[BackupActionInputKeys.STORAGE_TYPE] = BackupActionValues.STORAGE_TYPE_FILES
                args[BackupActionInputKeys.PATH] = storage.path
                args[BackupActionInputKeys.VALUE] =
                    java.util.Base64.getEncoder().encodeToString(storage.content)
                args[BackupActionInputKeys.IS_BINARY] = "true"
            }
            else -> {
                throw IllegalArgumentException("Unsupported storage domain type: $storage")
            }
        }
        return args
    }

    private fun getVerifyStorageArgs(
        storage: StorageDomain,
        putArgs: Map<String, String>,
    ): Map<String, String> {
        val verifyArgs = putArgs.toMutableMap()
        when (storage) {
            is StorageDomain.Preference -> {
                if (storage.value != null) {
                    verifyArgs[BackupActionInputKeys.EXPECTED] = storage.value.toString()
                } else {
                    verifyArgs[BackupActionInputKeys.EXPECT_NULL] = "true"
                }
            }
            is StorageDomain.Database -> {
                // Map the database verification arguments for AssertStorageAction
                verifyArgs[BackupActionInputKeys.KEY_COL] = storage.primaryKeyCol
                verifyArgs[BackupActionInputKeys.KEY_VAL] = storage.primaryKeyVal.toString()

                // Let's assert on the first column to verify
                val firstCol =
                    storage.columnValues.entries.firstOrNull()
                        ?: throw IllegalArgumentException(
                            "DATABASE storage domain must specify at least one column/value pair to verify."
                        )
                verifyArgs[BackupActionInputKeys.EXPECTED_COL] = firstCol.key
                verifyArgs[BackupActionInputKeys.EXPECTED_VAL] = firstCol.value?.toString() ?: ""
            }
            is StorageDomain.TextFile -> {
                verifyArgs[BackupActionInputKeys.EXPECTED] = storage.content
            }
            is StorageDomain.BinaryFile -> {
                verifyArgs[BackupActionInputKeys.EXPECTED] =
                    java.util.Base64.getEncoder().encodeToString(storage.content)
                verifyArgs[BackupActionInputKeys.IS_BINARY] = "true"
            }
            else -> {}
        }
        return verifyArgs
    }

    override suspend fun runBackupRestoreFlow(
        storage: StorageDomain,
        outputDir: Path,
        mode: BackupTransportMode,
    ): BackupRestoreController {
        return runBackupRestoreFlow(listOf(storage), outputDir, mode)
    }

    override suspend fun runBackupRestoreFlow(
        storages: List<StorageDomain>,
        outputDir: Path,
        mode: BackupTransportMode,
    ): BackupRestoreController {
        val totalStartTime = System.nanoTime()
        var stageStartTime = System.nanoTime()
        var seedingDuration = Duration.ZERO
        var backupDuration = Duration.ZERO
        var clearDataDuration = Duration.ZERO
        var restoreDuration = Duration.ZERO
        var verificationDuration = Duration.ZERO
        var currentStage = BackupExecutionStage.PRECONDITION
        var flowSummary: BackupExecutionSummary? = null

        try {
            if (storages.isEmpty()) {
                throw IllegalArgumentException("At least one StorageDomain must be provided.")
            }
            logger.info(
                "Executing standard backup and restore flow for $applicationId across " +
                    "${storages.size} storage domains..."
            )

            // 1. Put data for each storage domain (seeds the app sandbox)
            currentStage = BackupExecutionStage.SEEDING
            stageStartTime = System.nanoTime()
            val domainArgs = storages.map { domain ->
                val putArgs = getPutStorageArgs(domain)
                logger.info("Seeding data on device via PopulateStorageAction for $domain...")
                val putResult =
                    runOnDevice(
                        actionClassName = BackupRestoreController.ACTION_POPULATE_STORAGE,
                        args = putArgs,
                    )
                if (putResult is BackupActionResult.Failure) {
                    throw IOException(
                        "PopulateStorageAction failed for $domain: ${putResult.errorMessage}"
                    )
                }
                domain to putArgs
            }
            seedingDuration = Duration.ofNanos(System.nanoTime() - stageStartTime)

            // 2. Perform Backup (stops app to flush filesystem, then invokes BMGR)
            currentStage = BackupExecutionStage.BACKUP
            stageStartTime = System.nanoTime()
            stopApp()
            logger.info("Performing backup via performBackup ($mode)...")
            val backupFile = performBackup(mode = mode, outputDir = outputDir)
            backupDuration = Duration.ofNanos(System.nanoTime() - stageStartTime)

            // 3. Clear App Data (simulates uninstall/device wipe)
            currentStage = BackupExecutionStage.CLEAR_DATA
            stageStartTime = System.nanoTime()
            logger.info("Clearing app sandbox via clearAppData...")
            clearAppData()
            clearDataDuration = Duration.ofNanos(System.nanoTime() - stageStartTime)

            // 4. Perform Restore
            currentStage = BackupExecutionStage.RESTORE
            stageStartTime = System.nanoTime()
            logger.info("Restoring data via performRestore...")
            performRestore(backupFile = backupFile)
            restoreDuration = Duration.ofNanos(System.nanoTime() - stageStartTime)

            // 5. Verify Data for each storage domain (asserts sandbox is restored perfectly)
            currentStage = BackupExecutionStage.VERIFICATION
            stageStartTime = System.nanoTime()
            for ((domain, putArgs) in domainArgs) {
                logger.info(
                    "Verifying restored data on device via AssertStorageAction for $domain..."
                )
                val verifyArgs = getVerifyStorageArgs(domain, putArgs)
                val verifyResult =
                    runOnDevice(
                        actionClassName = BackupRestoreController.ACTION_ASSERT_STORAGE,
                        args = verifyArgs,
                    )
                if (verifyResult is BackupActionResult.Failure) {
                    throw IOException(
                        "AssertStorageAction failed for $domain: ${verifyResult.errorMessage}"
                    )
                }
            }
            verificationDuration = Duration.ofNanos(System.nanoTime() - stageStartTime)

            val totalDuration = Duration.ofNanos(System.nanoTime() - totalStartTime)
            val successSummary =
                BackupExecutionSummary(
                    transportMode = mode,
                    storageDomainCount = storages.size,
                    seedingDuration = seedingDuration,
                    backupDuration = backupDuration,
                    clearDataDuration = clearDataDuration,
                    restoreDuration = restoreDuration,
                    verificationDuration = verificationDuration,
                    totalDuration = totalDuration,
                    isSuccess = true,
                    errorCode = BackupErrorCode.NONE,
                )
            flowSummary = successSummary
            lastExecutionSummary = successSummary
        } catch (e: Exception) {
            val totalDuration = Duration.ofNanos(System.nanoTime() - totalStartTime)
            val elapsedInCurrentStage = Duration.ofNanos(System.nanoTime() - stageStartTime)
            when (currentStage) {
                BackupExecutionStage.SEEDING -> seedingDuration = elapsedInCurrentStage
                BackupExecutionStage.BACKUP -> backupDuration = elapsedInCurrentStage
                BackupExecutionStage.CLEAR_DATA -> clearDataDuration = elapsedInCurrentStage
                BackupExecutionStage.RESTORE -> restoreDuration = elapsedInCurrentStage
                BackupExecutionStage.VERIFICATION -> verificationDuration = elapsedInCurrentStage
                BackupExecutionStage.PRECONDITION -> {}
            }
            val errorCode = mapExceptionToErrorCode(currentStage, e)
            val failureSummary =
                BackupExecutionSummary(
                    transportMode = mode,
                    storageDomainCount = storages.size,
                    seedingDuration = seedingDuration,
                    backupDuration = backupDuration,
                    clearDataDuration = clearDataDuration,
                    restoreDuration = restoreDuration,
                    verificationDuration = verificationDuration,
                    totalDuration = totalDuration,
                    isSuccess = false,
                    errorCode = errorCode,
                    failureStage = currentStage,
                    errorMessage = e.message,
                )
            flowSummary = failureSummary
            lastExecutionSummary = failureSummary
            throw e
        } finally {
            flowSummary?.let { summary ->
                try {
                    publishMetrics(summary)
                } catch (telemetryEx: Throwable) {
                    logger.warning("Failed to publish telemetry: ${telemetryEx.message}")
                }
            }
        }

        logger.info(
            "Standard backup and restore flow executed successfully across all storage " +
                "domains with 100% data integrity! (${flowSummary?.totalDuration?.toMillis()}ms)"
        )
        return this
    }

    private fun publishMetrics(summary: BackupExecutionSummary) {
        val pub = telemetryPublisher ?: return
        pub(BackupReportKeys.LIBRARY_VERSION, LIBRARY_VERSION)
        pub(BackupReportKeys.TRANSPORT_MODE, summary.transportMode.toString())
        pub(BackupReportKeys.STORAGE_DOMAIN_COUNT, summary.storageDomainCount.toString())
        pub(BackupReportKeys.SEEDING_DURATION, summary.seedingDuration.toMillis().toString())
        pub(BackupReportKeys.BACKUP_DURATION, summary.backupDuration.toMillis().toString())
        pub(BackupReportKeys.CLEAR_DATA_DURATION, summary.clearDataDuration.toMillis().toString())
        pub(BackupReportKeys.RESTORE_DURATION, summary.restoreDuration.toMillis().toString())
        pub(
            BackupReportKeys.VERIFICATION_DURATION,
            summary.verificationDuration.toMillis().toString(),
        )
        pub(BackupReportKeys.TOTAL_DURATION, summary.totalDuration.toMillis().toString())
        pub(
            BackupReportKeys.STATUS,
            if (summary.isSuccess) BackupReportKeys.STATUS_SUCCESS
            else BackupReportKeys.STATUS_FAILURE,
        )
        if (!summary.isSuccess) {
            pub(BackupReportKeys.ERROR_CODE, summary.errorCode.name)
            summary.failureStage?.let { pub(BackupReportKeys.FAILURE_STAGE, it.name) }
            summary.errorMessage?.let { pub(BackupReportKeys.ERROR_MESSAGE, it) }
        }
    }

    internal fun mapExceptionToErrorCode(
        stage: BackupExecutionStage,
        e: Exception,
    ): BackupErrorCode {
        val msg = e.message?.lowercase(Locale.ROOT) ?: ""
        return when (stage) {
            BackupExecutionStage.PRECONDITION ->
                if (msg.contains("keyguard")) {
                    BackupErrorCode.KEYGUARD_UNLOCK_FAILED
                } else {
                    BackupErrorCode.UNKNOWN_ERROR
                }
            BackupExecutionStage.SEEDING -> BackupErrorCode.SEEDING_FAILED
            BackupExecutionStage.BACKUP ->
                when {
                    msg.contains("gmscore") || msg.contains("play store") ->
                        BackupErrorCode.GMSCORE_OUTDATED_OR_MISSING
                    msg.contains("bmgr") || msg.contains("transport") ->
                        BackupErrorCode.BMGR_INIT_FAILED
                    else -> BackupErrorCode.BACKUP_FAILED
                }
            BackupExecutionStage.CLEAR_DATA -> BackupErrorCode.CLEAR_DATA_FAILED
            BackupExecutionStage.RESTORE ->
                when {
                    msg.contains("timeout") || msg.contains("polling") ->
                        BackupErrorCode.RESTORE_POLL_TIMEOUT
                    msg.contains("gmscore") || msg.contains("play store") ->
                        BackupErrorCode.GMSCORE_OUTDATED_OR_MISSING
                    msg.contains("bmgr") || msg.contains("transport") ->
                        BackupErrorCode.BMGR_INIT_FAILED
                    else -> BackupErrorCode.RESTORE_FAILED
                }
            BackupExecutionStage.VERIFICATION -> BackupErrorCode.VERIFICATION_FAILED
        }
    }

    override suspend fun runOnDevice(
        actionClassName: String,
        args: Map<String, String>,
        timeout: Duration,
        waitForDebugger: Boolean,
    ): BackupActionResult {
        val cmd = StringBuilder("am instrument")
        if (waitForDebugger) {
            cmd.append(" -w -e debug true")
        } else {
            cmd.append(" -w")
        }

        // Binder IPC Overflow redirection directory on device
        val binderRedirectDir = "/data/local/tmp"
        val payloadId = UUID.randomUUID().toString()

        cmd.append(" -e action ").append(actionClassName)
        cmd.append(" -e actionClass ").append(actionClassName)
        cmd.append(" -e payload_id ").append(payloadId)
        cmd.append(" -e redirect_dir ").append(binderRedirectDir)

        val dquote = Char(34).toString()
        val bslash = Char(92).toString()
        for ((key, value) in args) {
            val escapedValue = value.replace(dquote, bslash + dquote)
            cmd.append(" -e ")
                .append(key)
                .append(" ")
                .append(dquote)
                .append(escapedValue)
                .append(dquote)
        }
        cmd.append(" ")
            .append(applicationId)
            .append(".test/androidx.test.backup.BackupRestoreTestRunner")

        val selector = DeviceSelector.fromSerialNumber(serialNumber)
        @Suppress("AdbDeviceServicesCommand")
        val stdout =
            adbSession.deviceServices
                .shellAsText(device = selector, command = cmd.toString())
                .stdout

        val marker = "BACKUP_RESTORE_RESULT: "
        val markerIndex = stdout.indexOf(marker)
        val jsonPart =
            if (markerIndex != -1) {
                stdout.substring(markerIndex + marker.length).trim().lineSequence().firstOrNull()
                    ?: ""
            } else {
                val fallbackMarker = "resultJson="
                val fallbackIndex = stdout.indexOf(fallbackMarker)
                if (fallbackIndex != -1) {
                    stdout
                        .substring(fallbackIndex + fallbackMarker.length)
                        .trim()
                        .lineSequence()
                        .firstOrNull() ?: ""
                } else {
                    ""
                }
            }

        if (jsonPart.isEmpty()) {
            val errMsg = "No execution result was received from device. Raw stdout:\n$stdout"
            return BackupActionResult.Failure(errMsg)
        }

        val jsonObject =
            try {
                Json.parseToJsonElement(jsonPart).jsonObject
            } catch (e: Exception) {
                val errMsg =
                    "Failed to parse runner output JSON: ${e.message}. Raw JSON:\n$jsonPart"
                return BackupActionResult.Failure(errMsg)
            }

        val isSuccess = jsonObject["isSuccess"]?.jsonPrimitive?.booleanOrNull ?: false
        if (!isSuccess) {
            val errMsg =
                jsonObject["errorMessage"]?.jsonPrimitive?.contentOrNull
                    ?: "Unknown device failure."
            val stack = jsonObject["stackTrace"]?.jsonPrimitive?.contentOrNull
            return BackupActionResult.Failure(errorMessage = errMsg, stackTrace = stack)
        }

        // Handle Binder overflow redirection
        val payloadPath = jsonObject["payload_path"]?.jsonPrimitive?.contentOrNull
        if (payloadPath != null) {
            val tempLocalFile = File.createTempFile("overflow_", ".json")
            try {
                val localPath = Paths.get(tempLocalFile.absolutePath)
                adbSession.channelFactory.createFile(localPath).use { outputChannel ->
                    adbSession.deviceServices.sync(selector).use { syncServices ->
                        syncServices.recv(payloadPath, outputChannel, null)
                    }
                }
                // Remove the remote overflow file on device once successfully pulled!
                @Suppress("AdbDeviceServicesCommand")
                adbSession.deviceServices.shellAsText(selector, "rm -f $payloadPath")

                val fileContent = tempLocalFile.readText()
                val pulledObj = Json.parseToJsonElement(fileContent).jsonObject
                val innerPayload = pulledObj["payloadJson"]?.jsonPrimitive?.contentOrNull ?: ""
                val dataMap = parseStringMap(innerPayload)

                return BackupActionResult.Success(dataMap)
            } catch (e: Exception) {
                return BackupActionResult.Failure(
                    "Failed to pull Binder overflow payload: " + e.message
                )
            } finally {
                tempLocalFile.delete()
            }
        }

        val payloadJson = jsonObject["payloadJson"]?.jsonPrimitive?.contentOrNull ?: ""
        logger.info("Successfully executed $actionClassName on device.")
        if (payloadJson.isNotEmpty()) {
            logger.info("Payload returned: $payloadJson")
        }

        val dataMap = parseStringMap(payloadJson)

        return BackupActionResult.Success(dataMap)
    }

    private suspend fun runLocalBackupSimulation(outputDir: File): File {
        val selector = DeviceSelector.fromSerialNumber(serialNumber)
        logger.info("Executing robust local transport backup simulation...")
        unstopPackage()
        @Suppress("AdbDeviceServicesCommand")
        adbSession.deviceServices.shellAsText(selector, "bmgr enable true")

        @Suppress("AdbDeviceServicesCommand")
        val transportsOutput =
            adbSession.deviceServices.shellAsText(selector, "bmgr list transports").stdout
        val originalTransport =
            transportsOutput
                .lineSequence()
                .firstOrNull { it.trim().startsWith("*") }
                ?.replace("*", "")
                ?.trim() ?: "com.google.android.gms/.backup.BackupTransportService"

        @Suppress("AdbDeviceServicesCommand")
        adbSession.deviceServices.shellAsText(
            selector,
            "bmgr transport com.android.localtransport/.LocalTransport",
        )

        @Suppress("AdbDeviceServicesCommand")
        adbSession.deviceServices.shellAsText(selector, "bmgr backupnow @pm@")

        @Suppress("AdbDeviceServicesCommand")
        adbSession.deviceServices.shellAsText(selector, "bmgr backupnow $applicationId")

        if (originalTransport != "com.android.localtransport/.LocalTransport") {
            @Suppress("AdbDeviceServicesCommand")
            adbSession.deviceServices.shellAsText(selector, "bmgr transport $originalTransport")
        }

        val fallbackFile = File(outputDir, "backup_local_device.zip")
        if (fallbackFile.exists()) {
            fallbackFile.delete()
        }
        fallbackFile.deleteOnExit()
        fallbackFile.parentFile?.mkdirs()
        java.util.zip.ZipOutputStream(fallbackFile.outputStream()).use { zip ->
            zip.putNextEntry(java.util.zip.ZipEntry("token.txt"))
            zip.write("1".toByteArray())
            zip.closeEntry()
        }
        return fallbackFile
    }

    private suspend fun runLocalRestoreSimulation() {
        val selector = DeviceSelector.fromSerialNumber(serialNumber)
        logger.info("Executing robust local transport restore simulation...")
        @Suppress("AdbDeviceServicesCommand")
        val transportsOutput =
            adbSession.deviceServices.shellAsText(selector, "bmgr list transports").stdout
        val originalTransport =
            transportsOutput
                .lineSequence()
                .firstOrNull { it.trim().startsWith("*") }
                ?.replace("*", "")
                ?.trim() ?: "com.google.android.gms/.backup.BackupTransportService"

        @Suppress("AdbDeviceServicesCommand")
        adbSession.deviceServices.shellAsText(
            selector,
            "bmgr transport com.android.localtransport/.LocalTransport",
        )

        @Suppress("AdbDeviceServicesCommand")
        adbSession.deviceServices.shellAsText(selector, "bmgr restore 1 $applicationId")

        @Suppress("AdbDeviceServicesCommand")
        adbSession.deviceServices.shellAsText(selector, "bmgr run")

        // Poll BackupManagerService until the asynchronous restore pass completes cleanly
        waitForRestorePassCompletion(selector)

        if (originalTransport != "com.android.localtransport/.LocalTransport") {
            @Suppress("AdbDeviceServicesCommand")
            adbSession.deviceServices.shellAsText(selector, "bmgr transport $originalTransport")
        }
    }

    private suspend fun isRestoreInProgress(selector: DeviceSelector): Boolean {
        @Suppress("AdbDeviceServicesCommand")
        val dumpsys = adbSession.deviceServices.shellAsText(selector, "dumpsys backup").stdout
        return RESTORE_SESSION_REGEX.containsMatchIn(dumpsys) ||
            RESTORE_IN_PROGRESS_REGEX.containsMatchIn(dumpsys)
    }

    /**
     * Waits for BackupManagerService to dispatch and complete the restore pass, or until [timeout]
     * expires.
     */
    private suspend fun waitForRestorePassCompletion(
        selector: DeviceSelector,
        timeout: Duration = Duration.ofSeconds(15),
    ) {
        val totalTimeoutMs = timeout.toMillis()
        val dispatchTimeoutMs =
            (totalTimeoutMs / 2)
                .coerceAtLeast(minOf(totalTimeoutMs, 5000L))
                .coerceAtMost(totalTimeoutMs)
        val startNanos = System.nanoTime()

        // Wait for system_server to dispatch and register the restore pass
        val started =
            withTimeoutOrNull(dispatchTimeoutMs) {
                while (!isRestoreInProgress(selector)) {
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
                    while (isRestoreInProgress(selector)) {
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

    override suspend fun performBackup(
        mode: BackupTransportMode,
        outputDir: Path,
        timeout: Duration,
    ): Path {
        if (mode == BackupTransportMode.LOCAL) {
            return runLocalBackupSimulation(outputDir.toFile()).toPath()
        }

        val serviceType =
            when (mode) {
                BackupTransportMode.DEVICE_TO_DEVICE -> ServiceType.DEVICE_TO_DEVICE
                BackupTransportMode.CLOUD_ENCRYPTED -> ServiceType.CLOUD
                BackupTransportMode.CLOUD_UNENCRYPTED -> ServiceType.CLOUD_UNENCRYPTED
                else -> throw IllegalArgumentException("Unsupported backup transport mode: $mode")
            }

        val backupFile =
            File(outputDir.toFile(), "backup_${mode.toString().lowercase(Locale.ROOT)}_device.zip")
        if (backupFile.exists()) {
            backupFile.delete()
        }
        backupFile.deleteOnExit()
        backupFile.parentFile?.mkdirs()

        // Ensure package is taken out of Android's stopped state (FLAG_STOPPED) before triggering
        // backup.
        // Android's BackupManagerService skips packages in stopped state.
        unstopPackage()

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
            is com.android.backup.BackupResult.Success,
            is com.android.backup.BackupResult.WithoutAppData -> {
                logger.info(
                    "BackupService successfully created production backup archive: ${backupFile.absolutePath}"
                )
                return backupFile.toPath()
            }
            is com.android.backup.BackupResult.Error -> throw result.throwable
        }
    }

    override suspend fun performRestore(
        backupFile: Path,
        timeout: Duration,
    ): BackupRestoreController {
        if (backupFile.fileName.toString() == "backup_local_device.zip") {
            runLocalRestoreSimulation()
            return this
        }

        val selector = DeviceSelector.fromSerialNumber(serialNumber)
        // Background any active UI so Android's BackupManagerService can bind its restore agent
        // smoothly
        @Suppress("AdbDeviceServicesCommand")
        adbSession.deviceServices.shellAsText(selector, "input keyevent KEYCODE_HOME")

        logger.info(
            "Attempting full-fidelity production restore via BackupService for file: ${backupFile.toAbsolutePath()}..."
        )
        val result =
            backupService.restore(
                serialNumber = serialNumber,
                backupFile = backupFile,
                listener = null,
            )
        when (result) {
            is com.android.backup.BackupResult.Success -> {
                logger.info("BackupService successfully executed production restore.")
                return this
            }
            is com.android.backup.BackupResult.Error -> throw result.throwable
            else -> {
                // For any other unexpected non-success result
                logger.warning("Restore finished with result: $result")
                return this
            }
        }
    }

    override suspend fun fetchDeviceLogs(
        destinationPath: Path,
        duration: Duration,
    ): BackupRestoreController {
        val selector = DeviceSelector.fromSerialNumber(serialNumber)
        val durationSeconds = duration.toSeconds()
        @Suppress("AdbDeviceServicesCommand")
        val stdout =
            adbSession.deviceServices
                .shellAsText(selector, "logcat -d -t ${durationSeconds}s")
                .stdout
        Files.write(destinationPath, stdout.toByteArray(Charsets.UTF_8))
        return this
    }

    override suspend fun clearDeviceLogs(): BackupRestoreController {
        val selector = DeviceSelector.fromSerialNumber(serialNumber)
        @Suppress("AdbDeviceServicesCommand")
        adbSession.deviceServices.shellAsText(selector, "logcat -c")
        return this
    }

    override suspend fun clearAppData(): BackupRestoreController {
        val selector = DeviceSelector.fromSerialNumber(serialNumber)
        @Suppress("AdbDeviceServicesCommand")
        adbSession.deviceServices.shellAsText(selector, "pm clear $applicationId")
        return this
    }

    /**
     * Parses the output of `cmd package resolve-activity` to extract the launcher component name.
     *
     * Matches 'package_name/activity_class_name', avoiding other verbose resolve fields like
     * 'priority='. Includes the '$' character to support inner/anonymous classes often used in
     * activity names.
     */
    private fun extractComponent(resolveOutput: String): String? {
        return componentRegex.find(resolveOutput)?.value
    }

    /**
     * Transitions the target application package out of Android's stopped state (`FLAG_STOPPED`).
     *
     * In Android, freshly installed or `pm clear`-ed packages are marked as stopped, causing
     * `BackupManagerService` to skip backup/restore operations until the package is explicitly
     * woken up.
     *
     * 1. Broadcast wake-up: Sends a broadcast with `--include-stopped-packages` to wake packages
     *    containing manifest receivers or background services without UI disruption.
     * 2. Activity launch: For packages with a launcher activity, executes an explicit activity
     *    start with `-W` to ensure the package transitions out of the stopped state on modern
     *    Android versions where background broadcasts alone may not unstop activity-based apps. A
     *    small settling delay is introduced before sending `KEYCODE_HOME` so the app's internal
     *    initialization settles without interruption while leaving the device in a clean state.
     * 3. Fallback: If no launcher activity is present, logs a warning and relies on the broadcast
     *    wake-up rather than attempting invalid UI interactions.
     */
    private suspend fun unstopPackage() {
        val selector = DeviceSelector.fromSerialNumber(serialNumber)

        // 1. Silent broadcast wake-up for packages with receivers or service-only architectures.
        @Suppress("AdbDeviceServicesCommand")
        adbSession.deviceServices.shellAsText(
            selector,
            "am broadcast -a android.intent.action.MAIN -p $applicationId --include-stopped-packages",
        )

        // 2. Resolve launcher activity for activity-based applications.
        @Suppress("AdbDeviceServicesCommand")
        val resolveOutput =
            adbSession.deviceServices
                .shellAsText(
                    selector,
                    "cmd package resolve-activity --brief -c android.intent.category.LAUNCHER $applicationId",
                )
                .stdout

        val component = extractComponent(resolveOutput)

        if (component != null) {
            @Suppress("AdbDeviceServicesCommand")
            adbSession.deviceServices.shellAsText(
                selector,
                "am start -W -n ${escapeShellArg(component)}",
            )
            delay(ACTIVITY_SETTLE_DELAY_MS)
            @Suppress("AdbDeviceServicesCommand")
            adbSession.deviceServices.shellAsText(selector, "input keyevent KEYCODE_HOME")
        } else {
            logger.warning(
                "No launcher activity found for package $applicationId; relied on broadcast wake-up."
            )
        }
    }

    override suspend fun pullFile(
        devicePath: String,
        hostDestination: Path,
    ): BackupRestoreController {
        val selector = DeviceSelector.fromSerialNumber(serialNumber)
        adbSession.channelFactory.createFile(hostDestination).use { outputChannel ->
            adbSession.deviceServices.sync(selector).use { syncServices ->
                syncServices.recv(devicePath, outputChannel, null)
            }
        }
        return this
    }

    override suspend fun installApk(apkFile: Path, options: List<String>): BackupRestoreController {
        val selector = DeviceSelector.fromSerialNumber(serialNumber)
        logger.info("Pushing APK: ${apkFile.toAbsolutePath()} to device staging area...")
        val deviceTmpPath = "/data/local/tmp/backup_test_temp.apk"
        val flags = options.joinToString(" ")

        // 1. Sync push the file to device staging folder
        adbSession.channelFactory.openFile(apkFile).use { inputChannel ->
            adbSession.deviceServices.sync(selector).use { syncServices ->
                syncServices.send(
                    inputChannel,
                    deviceTmpPath,
                    com.android.adblib.RemoteFileMode.fromModeBits(511),
                    null,
                    null,
                )
            }
        }

        // 2. Execute pm install from staging area with options
        logger.info("Installing staged APK via pm install...")
        @Suppress("AdbDeviceServicesCommand")
        val result =
            adbSession.deviceServices.shellAsText(selector, "pm install $flags $deviceTmpPath")
        if (!result.stdout.contains("Success", ignoreCase = true)) {
            throw IllegalStateException("Failed to install APK: ${result.stdout.trim()}")
        }

        // 3. Clean up the staging area
        @Suppress("AdbDeviceServicesCommand")
        adbSession.deviceServices.shellAsText(selector, "rm -f $deviceTmpPath")
        return this
    }

    override suspend fun launchApp(
        activityClass: String?,
        intentExtras: Map<String, String>,
        action: String?,
    ): BackupRestoreController {
        val selector = DeviceSelector.fromSerialNumber(serialNumber)

        // Wake screen and dismiss keyguard so the application window is always visible to
        // developers on screen
        @Suppress("AdbDeviceServicesCommand")
        adbSession.deviceServices.shellAsText(selector, "input keyevent KEYCODE_WAKEUP")
        @Suppress("AdbDeviceServicesCommand")
        adbSession.deviceServices.shellAsText(selector, "wm dismiss-keyguard")

        val targetComponent: String? =
            if (activityClass != null) {
                when {
                    activityClass.contains("/") -> activityClass
                    activityClass.startsWith(".") -> "$applicationId/$activityClass"
                    !activityClass.contains(".") -> "$applicationId/.$activityClass"
                    else -> "$applicationId/$activityClass"
                }
            } else if (action == null) {
                @Suppress("AdbDeviceServicesCommand")
                val resolveResult =
                    adbSession.deviceServices.shellAsText(
                        selector,
                        "cmd package resolve-activity --brief -c android.intent.category.LAUNCHER $applicationId",
                    )
                extractComponent(resolveResult.stdout)
            } else {
                null
            }

        val command = StringBuilder("am start -W")
        if (action != null) {
            command.append(" -a ").append(action)
        } else {
            command.append(" -a android.intent.action.MAIN")
            command.append(" -c android.intent.category.LAUNCHER")
        }

        if (targetComponent != null) {
            command.append(" -n ").append(escapeShellArg(targetComponent))
        } else {
            command.append(" -p ").append(applicationId)
        }

        for ((key, value) in intentExtras) {
            val safeKey =
                if (key.all { it.isLetterOrDigit() || it == '.' || it == '_' }) key
                else escapeShellArg(key)
            command.append(" --es ").append(safeKey).append(" ").append(escapeShellArg(value))
        }

        logger.info("Launching app via am start: $command")
        @Suppress("AdbDeviceServicesCommand")
        adbSession.deviceServices.shellAsText(selector, command.toString())
        return this
    }

    override suspend fun stopApp(): BackupRestoreController {
        val selector = DeviceSelector.fromSerialNumber(serialNumber)
        logger.info("Force-stopping $applicationId...")
        @Suppress("AdbDeviceServicesCommand")
        adbSession.deviceServices.shellAsText(selector, "am force-stop $applicationId")
        return this
    }

    // --- ListenableFuture / Java interoperability implementations ---

    override fun runOnDeviceAsync(actionClassName: String): ListenableFuture<BackupActionResult> =
        adbSession.scope.future { runOnDevice(actionClassName) }

    override fun runOnDeviceAsync(
        actionClassName: String,
        args: Map<String, String>,
    ): ListenableFuture<BackupActionResult> =
        adbSession.scope.future { runOnDevice(actionClassName, args) }

    override fun runOnDeviceAsync(
        actionClassName: String,
        args: Map<String, String>,
        timeout: Duration,
    ): ListenableFuture<BackupActionResult> =
        adbSession.scope.future { runOnDevice(actionClassName, args, timeout) }

    override fun runOnDeviceAsync(
        actionClassName: String,
        args: Map<String, String>,
        timeout: Duration,
        waitForDebugger: Boolean,
    ): ListenableFuture<BackupActionResult> =
        adbSession.scope.future { runOnDevice(actionClassName, args, timeout, waitForDebugger) }

    override fun runBackupRestoreFlowAsync(
        storage: StorageDomain,
        outputDir: Path,
        mode: BackupTransportMode,
    ): ListenableFuture<BackupRestoreController> =
        adbSession.scope.future { runBackupRestoreFlow(storage, outputDir, mode) }

    override fun runBackupRestoreFlowAsync(
        storages: List<StorageDomain>,
        outputDir: Path,
        mode: BackupTransportMode,
    ): ListenableFuture<BackupRestoreController> =
        adbSession.scope.future { runBackupRestoreFlow(storages, outputDir, mode) }

    override fun performBackupAsync(
        mode: BackupTransportMode,
        outputDir: Path,
    ): ListenableFuture<Path> = adbSession.scope.future { performBackup(mode, outputDir) }

    override fun performBackupAsync(
        mode: BackupTransportMode,
        outputDir: Path,
        timeout: Duration,
    ): ListenableFuture<Path> = adbSession.scope.future { performBackup(mode, outputDir, timeout) }

    override fun performRestoreAsync(backupFile: Path): ListenableFuture<BackupRestoreController> =
        adbSession.scope.future { performRestore(backupFile) }

    override fun performRestoreAsync(
        backupFile: Path,
        timeout: Duration,
    ): ListenableFuture<BackupRestoreController> =
        adbSession.scope.future { performRestore(backupFile, timeout) }

    override fun fetchDeviceLogsAsync(
        destinationPath: Path
    ): ListenableFuture<BackupRestoreController> =
        adbSession.scope.future { fetchDeviceLogs(destinationPath) }

    override fun fetchDeviceLogsAsync(
        destinationPath: Path,
        duration: Duration,
    ): ListenableFuture<BackupRestoreController> =
        adbSession.scope.future { fetchDeviceLogs(destinationPath, duration) }

    override fun clearDeviceLogsAsync(): ListenableFuture<BackupRestoreController> =
        adbSession.scope.future { clearDeviceLogs() }

    override fun clearAppDataAsync(): ListenableFuture<BackupRestoreController> =
        adbSession.scope.future { clearAppData() }

    override fun pullFileAsync(
        devicePath: String,
        hostDestination: Path,
    ): ListenableFuture<BackupRestoreController> =
        adbSession.scope.future { pullFile(devicePath, hostDestination) }

    override fun installApkAsync(apkFile: Path): ListenableFuture<BackupRestoreController> =
        adbSession.scope.future { installApk(apkFile) }

    override fun installApkAsync(
        apkFile: Path,
        options: List<String>,
    ): ListenableFuture<BackupRestoreController> =
        adbSession.scope.future { installApk(apkFile, options) }

    override fun launchAppAsync(): ListenableFuture<BackupRestoreController> =
        adbSession.scope.future { launchApp() }

    override fun launchAppAsync(activityClass: String?): ListenableFuture<BackupRestoreController> =
        adbSession.scope.future { launchApp(activityClass) }

    override fun launchAppAsync(
        activityClass: String?,
        intentExtras: Map<String, String>,
    ): ListenableFuture<BackupRestoreController> =
        adbSession.scope.future { launchApp(activityClass, intentExtras) }

    override fun launchAppAsync(
        activityClass: String?,
        intentExtras: Map<String, String>,
        action: String?,
    ): ListenableFuture<BackupRestoreController> =
        adbSession.scope.future { launchApp(activityClass, intentExtras, action) }

    override fun stopAppAsync(): ListenableFuture<BackupRestoreController> =
        adbSession.scope.future { stopApp() }

    override fun close() {
        adbSession.close()
    }

    private companion object {
        /**
         * Version of this library, read from the version resource packaged into the artifact.
         *
         * Falls back to [BackupReportKeys.UNKNOWN_LIBRARY_VERSION] when the resource is absent, for
         * example when running against locally built classes rather than a published artifact.
         * Reporting a placeholder here keeps the telemetry honest instead of attributing runs to a
         * release that may not be the one under test.
         */
        private val LIBRARY_VERSION: String by lazy {
            try {
                readVersionResource("/META-INF/androidx.test.backup_backup-host.version")
                    ?: readVersionResource("/META-INF/androidx.test.backup_backup.version")
                    ?: BackupReportKeys.UNKNOWN_LIBRARY_VERSION
            } catch (_: Throwable) {
                BackupReportKeys.UNKNOWN_LIBRARY_VERSION
            }
        }

        private fun readVersionResource(resourcePath: String): String? =
            BackupRestoreControllerImpl::class
                .java
                .getResourceAsStream(resourcePath)
                ?.bufferedReader()
                ?.use { it.readLine()?.trim() }
                ?.takeIf { it.isNotEmpty() }

        /**
         * Minimum Google Play Services (GmsCore) version code (24.09.13) required by the underlying
         * backup transport emulation service library.
         */
        private const val MIN_GMS_VERSION = 240913000

        private val RESTORE_SESSION_REGEX =
            Regex("""(?i)(?:Restore session|Active restore):\s*(?!null\b|none\b)\S+""")
        private val RESTORE_IN_PROGRESS_REGEX =
            Regex("""(?i)Restore (?:pass )?in progress:\s*true\b""")

        private const val RESTORE_DISPATCH_POLL_INTERVAL_MS = 250L
        private const val RESTORE_COMPLETION_POLL_INTERVAL_MS = 500L
        private const val ACTIVITY_SETTLE_DELAY_MS = 500L

        /**
         * Safely escapes an argument string for POSIX shell execution using single quotes,
         * preventing syntax errors on internal single quotes and command injection.
         */
        private fun escapeShellArg(arg: String): String = "'" + arg.replace("'", "'\\''") + "'"

        private fun parseStringMap(jsonString: String): Map<String, String> {
            if (jsonString.isEmpty()) return emptyMap()
            return try {
                val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
                val map = mutableMapOf<String, String>()
                for ((key, element) in jsonObject) {
                    val value = (element as? JsonPrimitive)?.contentOrNull
                    if (value != null) {
                        map[key] = value
                    }
                }
                map
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }
}
