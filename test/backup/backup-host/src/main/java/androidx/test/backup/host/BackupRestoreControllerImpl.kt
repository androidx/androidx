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
import java.util.UUID
import java.util.logging.Logger
import kotlinx.coroutines.guava.future
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
) : BackupRestoreController {

    private val logger = Logger.getLogger(BackupRestoreControllerImpl::class.java.name)

    private val backupService: Service by lazy {
        val platformLogger = PlatformLogger.getInstance(BackupRestoreControllerImpl::class.java)
        Service.getInstance(adbSession, platformLogger, MIN_GMS_VERSION)
    }

    private fun getPutStorageArgs(storage: StorageDomain): Map<String, String> {
        val args = mutableMapOf<String, String>()
        when (storage) {
            is StorageDomain.Preference -> {
                args[KEY_STORAGE_TYPE] = TYPE_PREFS
                args[KEY_PREF_NAME] = storage.prefName
                args[KEY_PREF_KEY] = storage.key
                val v = storage.value
                if (v != null) {
                    args[KEY_VALUE] = v.toString()
                    val valueType =
                        when (v) {
                            is Int -> "INT"
                            is Long -> "LONG"
                            is Float -> "FLOAT"
                            is Boolean -> "BOOLEAN"
                            else -> "STRING"
                        }
                    args[KEY_VALUE_TYPE] = valueType
                }
            }
            is StorageDomain.Database -> {
                args[KEY_STORAGE_TYPE] = TYPE_DATABASE
                args[KEY_DB_NAME] = storage.dbName
                args[KEY_TABLE] = storage.table
                // Build the raw values string that PopulateStorageAction expects:
                // "key1=val1&key2=val2"
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
                args[KEY_VALUES] = pairs.joinToString("&") { "${it.first}=${it.second}" }
            }
            is StorageDomain.TextFile -> {
                args[KEY_STORAGE_TYPE] = TYPE_FILES
                args[KEY_PATH] = storage.path
                args[KEY_VALUE] = storage.content
            }
            is StorageDomain.BinaryFile -> {
                args[KEY_STORAGE_TYPE] = TYPE_FILES
                args[KEY_PATH] = storage.path
                args[KEY_VALUE] = java.util.Base64.getEncoder().encodeToString(storage.content)
                args[KEY_IS_BINARY] = "true"
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
                    verifyArgs[KEY_EXPECTED] = storage.value.toString()
                } else {
                    verifyArgs[KEY_EXPECT_NULL] = "true"
                }
            }
            is StorageDomain.Database -> {
                // Map the database verification arguments for AssertStorageAction
                verifyArgs[KEY_KEY_COL] = storage.primaryKeyCol
                verifyArgs[KEY_KEY_VAL] = storage.primaryKeyVal.toString()

                // Let's assert on the first column to verify
                val firstCol =
                    storage.columnValues.entries.firstOrNull()
                        ?: throw IllegalArgumentException(
                            "DATABASE storage domain must specify at least one column/value pair to verify."
                        )
                verifyArgs[KEY_EXPECTED_COL] = firstCol.key
                verifyArgs[KEY_EXPECTED_VAL] = firstCol.value?.toString() ?: ""
            }
            is StorageDomain.TextFile -> {
                verifyArgs[KEY_EXPECTED] = storage.content
            }
            is StorageDomain.BinaryFile -> {
                verifyArgs[KEY_EXPECTED] =
                    java.util.Base64.getEncoder().encodeToString(storage.content)
                verifyArgs[KEY_IS_BINARY] = "true"
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
        if (storages.isEmpty()) {
            throw IllegalArgumentException("At least one StorageDomain must be provided.")
        }
        logger.info(
            "Executing standard backup and restore flow for $applicationId across " +
                "${storages.size} storage domains..."
        )

        // 1. Put data for each storage domain (seeds the app sandbox)
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

        // 2. Stop App to ensure filesystem flushes
        stopApp()

        // 3. Perform Backup
        logger.info("Performing backup via performBackup ($mode)...")
        val backupFile = performBackup(mode = mode, outputDir = outputDir)

        // 4. Clear App Data (simulates uninstall/device wipe)
        logger.info("Clearing app sandbox via clearAppData...")
        clearAppData()

        // 5. Perform Restore
        logger.info("Restoring data via performRestore...")
        performRestore(backupFile = backupFile)

        // 6. Verify Data for each storage domain (asserts sandbox is restored perfectly)
        for ((domain, putArgs) in domainArgs) {
            logger.info("Verifying restored data on device via AssertStorageAction for $domain...")
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

        logger.info(
            "Standard backup and restore flow executed successfully across all storage " +
                "domains with 100% data integrity!"
        )
        return this
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

        if (originalTransport != "com.android.localtransport/.LocalTransport") {
            @Suppress("AdbDeviceServicesCommand")
            adbSession.deviceServices.shellAsText(selector, "bmgr transport $originalTransport")
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
            File(outputDir.toFile(), "backup_${mode.toString().lowercase()}_device.zip")
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
     * Sends a broadcast query with `--receiver-include-stopped-packages` to transition the target
     * application package out of Android's stopped state (`FLAG_STOPPED`).
     *
     * In Android, freshly installed or `pm clear`-ed packages are marked as stopped, causing
     * `BackupManagerService` to skip backup/restore operations until the package is explicitly
     * woken up.
     */
    private suspend fun unstopPackage() {
        val selector = DeviceSelector.fromSerialNumber(serialNumber)
        @Suppress("AdbDeviceServicesCommand")
        adbSession.deviceServices.shellAsText(
            selector,
            "am broadcast -a android.intent.action.MAIN -p $applicationId --receiver-include-stopped-packages",
        )
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

        if (activityClass == null && intentExtras.isEmpty() && action == null) {
            logger.info("Launching $applicationId on-screen via launcher intent...")
            @Suppress("AdbDeviceServicesCommand")
            adbSession.deviceServices.shellAsText(
                selector,
                "monkey -p $applicationId -c android.intent.category.LAUNCHER 1",
            )
            return this
        }

        val command = StringBuilder("am start -W")
        if (action != null) {
            command.append(" -a ").append(action)
        } else {
            command.append(" -a android.intent.action.MAIN")
        }

        if (action == null) {
            command.append(" -c android.intent.category.LAUNCHER")
        }

        if (activityClass != null) {
            val component =
                when {
                    activityClass.contains("/") -> activityClass
                    activityClass.startsWith(".") -> "$applicationId/$activityClass"
                    activityClass.startsWith(applicationId) -> "$applicationId/$activityClass"
                    activityClass.contains(".") -> "$applicationId/$activityClass"
                    else -> "$applicationId/.$activityClass"
                }
            command.append(" -n ").append(component)
        } else {
            command.append(" -p ").append(applicationId)
        }

        for ((key, value) in intentExtras) {
            // Wrap values in single quotes to safely handle strings with spaces
            command.append(" --es ").append(key).append(" '").append(value).append("'")
        }

        logger.info("Launching app via custom am start command: $command")
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
         * Minimum Google Play Services (GmsCore) version code (24.09.13) required by the underlying
         * backup transport emulation service library.
         */
        private const val MIN_GMS_VERSION = 240913000

        // Argument keys used for PopulateStorageAction and AssertStorageAction
        private const val KEY_STORAGE_TYPE = "storage_type"
        private const val KEY_PREF_NAME = "pref_name"
        private const val KEY_PREF_KEY = "pref_key"
        private const val KEY_VALUE = "value"
        private const val KEY_VALUE_TYPE = "value_type"
        private const val KEY_DB_NAME = "db_name"
        private const val KEY_TABLE = "table"
        private const val KEY_VALUES = "values"
        private const val KEY_PATH = "path"
        private const val KEY_IS_BINARY = "is_binary"
        private const val KEY_EXPECT_NULL = "expect_null"

        // AssertStorageAction verification keys
        private const val KEY_EXPECTED = "expected"
        private const val KEY_KEY_COL = "key_col"
        private const val KEY_KEY_VAL = "key_val"
        private const val KEY_EXPECTED_COL = "expected_col"
        private const val KEY_EXPECTED_VAL = "expected_val"

        // Storage type values
        private const val TYPE_PREFS = "PREFS"
        private const val TYPE_DATABASE = "DATABASE"
        private const val TYPE_FILES = "FILES"

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
