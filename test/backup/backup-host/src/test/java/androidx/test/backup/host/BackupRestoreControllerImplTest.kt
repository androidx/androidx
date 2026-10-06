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

import com.android.adblib.ShellCommandOutput
import com.android.backup.BackupResult
import com.android.backup.BackupService
import java.io.IOException
import java.nio.file.Path
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.RETURNS_DEFAULTS
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

class BackupRestoreControllerImplTest {

    @get:Rule val tempFolder = TemporaryFolder()

    private val device = FakeAdbDevice()
    private val publishedMetrics = mutableMapOf<String, String>()

    /** What every backup through the fake backup service ends with. */
    private var backupServiceResult: BackupResult = BackupResult.Success
    private val controller =
        BackupRestoreControllerImpl(
            device.session,
            FAKE_SERIAL,
            34,
            PACKAGE,
            telemetryPublisher = { key, value -> publishedMetrics[key] = value },
            backupServiceProvider = { fakeBackupService() },
        )

    @Test
    fun propertiesAreExposed() {
        assertEquals(FAKE_SERIAL, controller.serialNumber)
        assertEquals(34, controller.apiLevel)
        assertEquals(PACKAGE, controller.applicationId)
    }

    @Test
    fun runOnDeviceRunsTheActionAndReturnsItsPayload() = runBlocking {
        device.onShell { shellOutput(runnerStdout("""{"user_id":"123"}""")) }

        val result = controller.runOnDevice("com.example.MyAction", mapOf("user_id" to "123"))

        assertEquals(BackupActionResult.Success(mapOf("user_id" to "123")), result)
        val command = instrumentCommand()
        assertTrue(command.startsWith("am instrument -w -e action "), command)
        assertTrue(command.contains(" -e user_id '123' "), command)
        assertTrue(command.endsWith(" $PACKAGE.test/$RUNNER"), command)
    }

    @Test
    fun runOnDeviceReturnsTheRunnerFailureAndStackTrace() = runBlocking {
        device.onShell {
            shellOutput(
                RESULT_MARKER +
                    """{"isSuccess":false,"errorMessage":"Something broke","stackTrace":"at MyAction.kt:15"}"""
            )
        }

        assertEquals(
            BackupActionResult.Failure("Something broke", "at MyAction.kt:15"),
            controller.runOnDevice("com.example.MyAction", emptyMap()),
        )
    }

    @Test
    fun runOnDeviceEscapesActionClassAndArgsForShell() = runBlocking {
        device.onShell { shellOutput(runnerStdout("""{"status":"success"}""")) }

        val result =
            controller.runOnDevice(
                actionClassName = "com.example.MyTest\$CustomAction",
                args =
                    mapOf(
                        "dollar_var" to "\$100 \${USER} \$(id)",
                        "backticks" to "`whoami`",
                        "trailing_slash" to """C:\path\trailing\""",
                        "double_quotes" to """{"k":"v"}""",
                        "single_quote" to "O'Brian",
                        "danger;key\$1" to "val",
                        "" to "empty_key_val",
                    ),
            )

        assertIs<BackupActionResult.Success>(result)
        val cmd = instrumentCommand()
        listOf(
                "-e action 'com.example.MyTest\$CustomAction'",
                "-e actionClass 'com.example.MyTest\$CustomAction'",
                "-e dollar_var '\$100 \${USER} \$(id)'",
                "-e backticks '`whoami`'",
                """-e trailing_slash 'C:\path\trailing\'""",
                """-e double_quotes '{"k":"v"}'""",
                """-e single_quote 'O'\''Brian'""",
                "-e 'danger;key\$1' 'val'",
                "-e '' 'empty_key_val'",
            )
            .forEach { expected -> assertTrue(cmd.contains(expected), "No `$expected` in: $cmd") }
    }

    @Test
    fun runOnDeviceAsksTheRunnerToWaitForADebugger() = runBlocking {
        device.onShell { shellOutput(runnerStdout("{}")) }

        controller.runOnDevice("com.example.MyAction", emptyMap(), waitForDebugger = true)

        val command = instrumentCommand()
        assertTrue(command.startsWith("am instrument -w -e debug 'true' -e action "), command)
    }

    /** `<applicationId>.test` is only the package that AGP gives the test APK. */
    @Test
    fun runOnDeviceRunsTheRunnerInstalledForTheApp() = runBlocking {
        onDeviceWithInstrumentations(
            "com.example.tests/$RUNNER",
            "$PACKAGE.test/androidx.test.runner.AndroidJUnitRunner",
        )

        controller.runOnDevice("com.example.MyAction")

        assertTrue(instrumentCommand().endsWith(" com.example.tests/$RUNNER"), instrumentCommand())
        assertEquals("pm list instrumentation $PACKAGE", device.commands.first())
    }

    @Test
    fun runOnDevicePrefersTheRunnerOfAgpAmongSeveral() = runBlocking {
        onDeviceWithInstrumentations("com.example.tests/$RUNNER", "$PACKAGE.test/$RUNNER")

        controller.runOnDevice("com.example.MyAction")

        assertTrue(instrumentCommand().endsWith(" $PACKAGE.test/$RUNNER"), instrumentCommand())
    }

    /**
     * Without a runner, the default one is run, so that `am instrument` reports it missing. Every
     * action looks the runner up, so it runs the one installed at that time.
     */
    @Test
    fun runOnDeviceLooksUpTheRunnerForEveryAction() = runBlocking {
        onDeviceWithInstrumentations()
        controller.runOnDevice("com.example.MyAction")
        onDeviceWithInstrumentations("com.example.tests/$RUNNER")
        controller.runOnDevice("com.example.MyAction")
        onDeviceWithInstrumentations("com.example.other/$RUNNER")
        controller.runOnDevice("com.example.MyAction")

        assertEquals(
            listOf(
                "$PACKAGE.test/$RUNNER",
                "com.example.tests/$RUNNER",
                "com.example.other/$RUNNER",
            ),
            device.commands
                .filter { it.startsWith("am instrument") }
                .map { it.substringAfterLast(' ') },
        )
    }

    @Test
    fun runOnDevicePullsAndRemovesTheOverflowPayload() = runBlocking {
        val overflowPath = "/data/local/tmp/overflow_\$special'file.json"
        device.onShell { command ->
            if (command.startsWith("am instrument")) shellOutput(overflowStdout(overflowPath))
            else shellOutput()
        }
        device.onPull {
            buildJsonObject { put("payloadJson", """{"status":"success","rows":"5"}""") }.toString()
        }

        val result = controller.runOnDevice("com.example.MyAction", emptyMap())

        assertEquals(
            BackupActionResult.Success(mapOf("status" to "success", "rows" to "5")),
            result,
        )
        assertEquals(listOf(overflowPath), device.pulledPaths)
        assertEquals(
            """rm -f -- '/data/local/tmp/overflow_${'$'}special'\''file.json'""",
            device.commands.last(),
        )
    }

    @Test
    fun runOnDeviceReportsAFailedOverflowPullAndKeepsTheFile() = runBlocking {
        device.onShell { command ->
            if (command.startsWith("am instrument")) shellOutput(overflowStdout("/tmp/p.json"))
            else shellOutput()
        }
        device.onPull { throw IOException("device disconnected") }

        assertEquals(
            BackupActionResult.Failure(
                "Failed to pull Binder overflow payload: device disconnected"
            ),
            controller.runOnDevice("com.example.MyAction", emptyMap()),
        )
        assertTrue(device.commands.none { it.startsWith("rm ") }, "${device.commands}")
    }

    @Test
    fun launchAppStartsTheLauncherActivityByDefault() = runBlocking {
        device.onShell { command ->
            if (command.contains("resolve-activity")) {
                shellOutput("priority=0 isDefault=true\n$PACKAGE/$PACKAGE.MainActivity\n")
            } else {
                shellOutput()
            }
        }

        controller.launchApp()

        assertEquals(
            listOf(
                "input keyevent KEYCODE_WAKEUP",
                "wm dismiss-keyguard",
                "cmd package resolve-activity --brief -c android.intent.category.LAUNCHER $PACKAGE",
                "am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER " +
                    "-n '$PACKAGE/$PACKAGE.MainActivity'",
            ),
            device.commands,
        )
    }

    @Test
    fun launchAppQualifiesTheActivityClassWithTheApplicationId() = runBlocking {
        mapOf(
                ".MyActivity" to "$PACKAGE/.MyActivity",
                ".subpackage.MyActivity" to "$PACKAGE/.subpackage.MyActivity",
                "SimpleActivity" to "$PACKAGE/.SimpleActivity",
                "$PACKAGE.subpackage.MyActivity" to "$PACKAGE/$PACKAGE.subpackage.MyActivity",
                "com.other.library.MyActivity" to "$PACKAGE/com.other.library.MyActivity",
                "$PACKAGE.MainActivity\$Inner" to "$PACKAGE/$PACKAGE.MainActivity\$Inner",
                "com.other/.Explicit" to "com.other/.Explicit",
            )
            .forEach { (activityClass, component) ->
                controller.launchApp(activityClass)

                assertTrue(
                    device.commands.last().endsWith(" -n '$component'"),
                    "For $activityClass: ${device.commands.last()}",
                )
            }
        assertTrue(device.commands.none { it.contains("resolve-activity") })
    }

    @Test
    fun launchAppWithOnlyAnActionResolvesItWithinThePackage() = runBlocking {
        controller.launchApp(activityClass = null, action = "com.example.CUSTOM_ACTION")

        assertEquals("am start -W -a com.example.CUSTOM_ACTION -p $PACKAGE", device.commands.last())
        assertTrue(device.commands.none { it.contains("resolve-activity") })
    }

    @Test
    fun launchAppPassesTheActionAndExtras() = runBlocking {
        controller.launchApp(
            activityClass = ".MyActivity",
            intentExtras = linkedMapOf("foo" to "bar value", "baz" to "qux"),
            action = "android.intent.action.VIEW",
        )

        assertEquals(
            "am start -W -a android.intent.action.VIEW -n '$PACKAGE/.MyActivity' " +
                "--es foo 'bar value' --es baz 'qux'",
            device.commands.last(),
        )
    }

    @Test
    fun deviceOperationsRunOnTheApplication() = runBlocking {
        device.onShell { command ->
            if (command.startsWith("pm clear")) shellOutput("Success\n") else shellOutput()
        }

        assertEquals(controller, controller.stopApp())
        assertEquals(controller, controller.clearAppData())
        assertEquals(controller, controller.clearDeviceLogs())
        assertEquals(
            controller,
            controller.pullFile("/sdcard/a.txt", tempFolder.root.toPath().resolve("a.txt")),
        )

        assertEquals(
            listOf("am force-stop $PACKAGE", "pm clear $PACKAGE", "logcat -c"),
            device.commands,
        )
        assertEquals(listOf("/sdcard/a.txt"), device.pulledPaths)
    }

    @Test
    fun fetchDeviceLogsWritesTheLogcatEntriesOfTheLast30Seconds() = runBlocking {
        device.onShell { command ->
            if (command == "date +%s") shellOutput("1700000000\n")
            else shellOutput("line 1\nline 2\n")
        }
        val destination = tempFolder.root.toPath().resolve("logcat.txt")

        controller.fetchDeviceLogs(destination)

        assertEquals("line 1\nline 2\n", destination.toFile().readText())
        assertEquals("logcat -d -t 1699999970.000", device.commands.last())
    }

    @Test
    fun fetchDeviceLogsThrowsWithoutWritingTheFileWhenLogcatFails() {
        device.onShell { command ->
            if (command == "date +%s") shellOutput("1700000000\n")
            else shellOutput(stderr = "logcat: Unexpected EOF!\n", exitCode = 1)
        }
        val destination = tempFolder.root.toPath().resolve("logcat.txt")

        assertFailsWith<IOException> { runBlocking { controller.fetchDeviceLogs(destination) } }
        assertFalse(destination.toFile().exists())
    }

    @Test
    fun asyncMethodsCompleteWithTheController() {
        assertEquals(controller, controller.stopAppAsync().get())
    }

    /** Other controllers can run on the same session, so closing one must leave it open. */
    @Test
    fun closeLeavesTheAdbSessionOpen() {
        controller.close()

        verify(device.session, never()).close()
    }

    @Test
    fun performBackupWritesTheLocalArchiveToTheOutputDirectory() = runBlocking {
        onHealthyDevice()

        val archive = controller.performBackup(BackupTransportMode.LOCAL, tempFolder.root.toPath())

        assertEquals(tempFolder.root.toPath().resolve("backup_local_device.zip"), archive)
        assertTrue(archive.toFile().isFile)
    }

    @Test
    fun performRestoreUsesTheLocalTransportForALocalArchive() = runBlocking {
        onHealthyDevice()

        controller.performRestore(tempFolder.newFile("backup_local_device.zip").toPath())

        assertTrue(device.commands.contains("bmgr restore 1 $PACKAGE"), "${device.commands}")
    }

    @Test
    fun performBackupReturnsTheArchiveOfACloudBackup() = runBlocking {
        val archive = controller.performBackup(BackupTransportMode.CLOUD_ENCRYPTED, outputDir())

        assertEquals(outputDir().resolve("backup_cloud_encrypted_device.zip"), archive)
        assertTrue(archive.toFile().isFile)
    }

    /** A backup without app data cannot be restored into the data the test expects. */
    @Test
    fun performBackupThrowsAndDeletesTheArchiveWhenItHoldsNoAppData() {
        backupServiceResult = BackupResult.WithoutAppData

        val e =
            assertFailsWith<IOException> {
                runBlocking {
                    controller.performBackup(BackupTransportMode.CLOUD_ENCRYPTED, outputDir())
                }
            }

        assertEquals(
            "Backup (CLOUD_ENCRYPTED) of $PACKAGE contains no app data, because the app does not " +
                "allow backups (android:allowBackup=\"false\")",
            e.message,
        )
        assertFalse(outputDir().resolve("backup_cloud_encrypted_device.zip").toFile().exists())
    }

    @Test
    fun runOnDeviceReturnsAFailureAndStopsTheAppWhenTheActionTimesOut() = runBlocking {
        device.hangOn { it.startsWith("am instrument") }

        val result = controller.runOnDevice("com.example.MyAction", timeout = SHORT_TIMEOUT)

        assertEquals(BackupActionResult.Failure("Timed out after 200ms"), result)
        assertEquals("am force-stop $PACKAGE", device.commands.last())
    }

    /** A force-stop that fails must not replace the timeout as the reported outcome. */
    @Test
    fun runOnDeviceReportsTheTimeoutWhenForceStopFails() = runBlocking {
        device.onShell { command ->
            if (command.startsWith("am force-stop")) throw IOException("device offline")
            shellOutput()
        }
        device.hangOn { it.startsWith("am instrument") }

        val result = controller.runOnDevice("com.example.MyAction", timeout = SHORT_TIMEOUT)

        assertEquals(BackupActionResult.Failure("Timed out after 200ms"), result)
        assertEquals("am force-stop $PACKAGE", device.commands.last())
    }

    /** A force-stop that never returns is abandoned after its own bound. */
    @Test
    fun runOnDeviceReportsTheTimeoutWhenForceStopHangs() = runBlocking {
        val controller =
            BackupRestoreControllerImpl(
                device.session,
                FAKE_SERIAL,
                34,
                PACKAGE,
                cleanupTimeout = 100.milliseconds,
            )
        device.hangOn { it.startsWith("am instrument") || it.startsWith("am force-stop") }

        val elapsed = measureTime {
            val result = controller.runOnDevice("com.example.MyAction", timeout = SHORT_TIMEOUT)
            assertEquals(BackupActionResult.Failure("Timed out after 200ms"), result)
        }

        assertTrue(elapsed < 2.seconds, "runOnDevice took $elapsed")
        assertEquals("am force-stop $PACKAGE", device.commands.last())
    }

    /** A timeout truncated to zero would expire before the action sends any command. */
    @Test
    fun runOnDeviceRunsTheActionForASubMillisecondTimeout() = runBlocking {
        device.hangOn { it.startsWith("am instrument") }

        val result = controller.runOnDevice("com.example.MyAction", timeout = Duration.ofNanos(500))

        assertEquals(BackupActionResult.Failure("Timed out after 500ns"), result)
        // The action starts by looking up its runner, and the deadline may end it there.
        assertEquals(
            "pm list instrumentation $PACKAGE",
            device.commands.first(),
            "${device.commands}",
        )
    }

    @Test
    fun runOnDeviceRejectsANonPositiveTimeout() {
        assertFailsWith<IllegalArgumentException> {
            runBlocking { controller.runOnDevice("com.example.MyAction", timeout = Duration.ZERO) }
        }
        assertTrue(device.commands.isEmpty(), "${device.commands}")
    }

    @Test
    fun performBackupThrowsAndStopsTheAppWhenTheBackupTimesOut() {
        onHealthyDevice()
        device.hangOn { it == "bmgr backupnow $PACKAGE" }

        val e =
            assertFailsWith<IOException> {
                runBlocking { controller.performBackup(LOCAL, outputDir(), SHORT_TIMEOUT) }
            }

        assertEquals("Backup (LOCAL) timed out after 200ms", e.message)
        assertEquals("am force-stop $PACKAGE", device.commands.last())
    }

    @Test
    fun performRestoreThrowsAndStopsTheAppWhenTheRestoreTimesOut() {
        onHealthyDevice()
        device.hangOn { it == "bmgr run" }
        val archive = tempFolder.newFile("backup_local_device.zip").toPath()

        val e =
            assertFailsWith<IOException> {
                runBlocking { controller.performRestore(archive, SHORT_TIMEOUT) }
            }

        assertEquals("Restore timed out after 200ms", e.message)
        assertEquals("am force-stop $PACKAGE", device.commands.last())
    }

    @Test
    fun performRestoreWaitsForTheRestorePassUntilItsTimeout() {
        var restoreProgressChecks = 0
        onHealthyDevice { command ->
            if (command == "dumpsys backup") {
                restoreProgressChecks++
                shellOutput("Restore in progress: true\n")
            } else {
                null
            }
        }
        val archive = tempFolder.newFile("backup_local_device.zip").toPath()

        val e =
            assertFailsWith<IOException> {
                runBlocking { controller.performRestore(archive, Duration.ofSeconds(1)) }
            }

        assertEquals("Restore timed out after 1s", e.message)
        assertTrue(restoreProgressChecks > 1, "Checks: $restoreProgressChecks")
    }

    /**
     * A local restore that ends before its first progress check is never seen in progress. It must
     * still succeed within a timeout shorter than the usual window for detecting the restore pass.
     */
    @Test
    fun performRestoreCompletesAnUnseenLocalRestoreWithinAShortTimeout() = runBlocking {
        onHealthyDevice { command ->
            if (command == "dumpsys backup") shellOutput("Restore in progress: false\n") else null
        }
        val archive = tempFolder.newFile("backup_local_device.zip").toPath()

        controller.performRestore(archive, Duration.ofSeconds(1))

        assertTrue(device.commands.none { it.startsWith("am force-stop") }, "${device.commands}")
    }

    @Test
    fun runOnDeviceDoesNotReportCancellationAsATimeout() = runBlocking {
        device.hangOn { it.startsWith("am instrument") }
        var result: BackupActionResult? = null

        val job = launch { result = controller.runOnDevice("com.example.MyAction") }
        delay(200)
        job.cancelAndJoin()

        assertNull(result)
        assertTrue(device.commands.none { it.startsWith("am force-stop") }, "${device.commands}")
    }

    @Test
    fun flowRunsEveryStageInOrderAndPublishesTheSummary() {
        onHealthyDevice()

        runBlocking { controller.runBackupRestoreFlow(PREFERENCE, outputDir(), LOCAL) }

        val stageCommands =
            listOf(
                    BackupRestoreController.ACTION_POPULATE_STORAGE,
                    "am force-stop",
                    "bmgr backupnow $PACKAGE",
                    "pm clear",
                    "bmgr restore",
                    BackupRestoreController.ACTION_ASSERT_STORAGE,
                )
                .map { stage -> device.commands.indexOfFirst { it.contains(stage) } }
        assertEquals(stageCommands.sorted(), stageCommands, "${device.commands}")
        assertFalse(stageCommands.contains(-1), "${device.commands}")

        val summary = assertNotNull(controller.lastExecutionSummary)
        assertEquals(LOCAL, summary.transportMode)
        assertEquals(1, summary.storageDomainCount)
        assertTrue(summary.isSuccess)
        assertEquals(BackupErrorCode.NONE, summary.errorCode)
        assertEquals("LOCAL", publishedMetrics[BackupReportKeys.TRANSPORT_MODE])
        assertEquals("SUCCESS", publishedMetrics[BackupReportKeys.STATUS])
    }

    @Test
    fun flowStopsBeforeRestoreWhenClearingDataFails() {
        onHealthyDevice { command ->
            if (command.startsWith("pm clear")) shellOutput(stderr = "Failed\n", exitCode = 1)
            else null
        }

        assertFailsWith<IOException> {
            runBlocking { controller.runBackupRestoreFlow(PREFERENCE, outputDir(), LOCAL) }
        }

        assertFailure(BackupExecutionStage.CLEAR_DATA, BackupErrorCode.CLEAR_DATA_FAILED)
        assertEquals("FAILURE", publishedMetrics[BackupReportKeys.STATUS])
        assertEquals("CLEAR_DATA_FAILED", publishedMetrics[BackupReportKeys.ERROR_CODE])
        assertEquals("CLEAR_DATA", publishedMetrics[BackupReportKeys.FAILURE_STAGE])
        assertTrue(
            device.commands.none {
                it.contains("bmgr restore") ||
                    it.contains(BackupRestoreController.ACTION_ASSERT_STORAGE)
            },
            "Restore and verification must not run after a failed clear: ${device.commands}",
        )
    }

    @Test
    fun flowAttributesAStopFailureToTheBackup() {
        onHealthyDevice { command ->
            if (command.startsWith("am force-stop")) throw IOException("Failed to stop process")
            else null
        }

        assertFailsWith<IOException> {
            runBlocking { controller.runBackupRestoreFlow(PREFERENCE, outputDir(), LOCAL) }
        }

        assertFailure(BackupExecutionStage.BACKUP, BackupErrorCode.BACKUP_FAILED)
    }

    @Test
    fun flowAttributesASeedingTimeoutToSeedingRatherThanRestore() {
        onHealthyDevice { command ->
            if (command.startsWith("am instrument")) {
                throw IOException("Command execution timed out after 30 seconds")
            } else {
                null
            }
        }

        assertFailsWith<IOException> {
            runBlocking { controller.runBackupRestoreFlow(PREFERENCE, outputDir(), LOCAL) }
        }

        assertFailure(BackupExecutionStage.SEEDING, BackupErrorCode.SEEDING_FAILED)
    }

    @Test
    fun flowClassifiesARestoreBmgrFailure() {
        onHealthyDevice { command ->
            if (command.startsWith("bmgr restore")) {
                throw IOException("bmgr: transport initialization error")
            } else {
                null
            }
        }

        assertFailsWith<IOException> {
            runBlocking { controller.runBackupRestoreFlow(PREFERENCE, outputDir(), LOCAL) }
        }

        assertFailure(BackupExecutionStage.RESTORE, BackupErrorCode.BMGR_INIT_FAILED)
    }

    @Test
    fun flowClassifiesAPackageThatIsNotBackedUpAsABackupFailure() {
        onHealthyDevice { command ->
            if (command == "bmgr backupnow $PACKAGE") {
                shellOutput(
                    "Running incremental backup for 1 requested packages.\n" +
                        "Package $PACKAGE with result: Backup is not allowed\n" +
                        "Backup finished with result: Success\n"
                )
            } else {
                null
            }
        }

        assertFailsWith<IOException> {
            runBlocking { controller.runBackupRestoreFlow(PREFERENCE, outputDir(), LOCAL) }
        }

        assertFailure(BackupExecutionStage.BACKUP, BackupErrorCode.BACKUP_FAILED)
    }

    @Test
    fun flowClassifiesAnUnknownLocalTransportAsABmgrFailure() {
        onHealthyDevice { command ->
            if (command == "bmgr transport '$LOCAL_TRANSPORT'") {
                shellOutput("Unknown transport '$LOCAL_TRANSPORT' specified; no changes made.\n")
            } else {
                null
            }
        }

        assertFailsWith<IOException> {
            runBlocking { controller.runBackupRestoreFlow(PREFERENCE, outputDir(), LOCAL) }
        }

        assertFailure(BackupExecutionStage.BACKUP, BackupErrorCode.BMGR_INIT_FAILED)
    }

    @Test
    fun flowClassifiesAMissingLocalBackupAsARestoreFailure() {
        onHealthyDevice { command ->
            if (command == "bmgr restore 1 $PACKAGE") {
                shellOutput("No available restore sets; no restore performed\ndone\n")
            } else {
                null
            }
        }

        assertFailsWith<IOException> {
            runBlocking { controller.runBackupRestoreFlow(PREFERENCE, outputDir(), LOCAL) }
        }

        assertFailure(BackupExecutionStage.RESTORE, BackupErrorCode.RESTORE_FAILED)
        assertTrue(
            device.commands.none { it.contains(BackupRestoreController.ACTION_ASSERT_STORAGE) },
            "${device.commands}",
        )
    }

    @Test
    fun flowClassifiesABackupWithoutAppDataAsABackupFailure() {
        onHealthyDevice()
        backupServiceResult = BackupResult.WithoutAppData

        assertFailsWith<IOException> {
            runBlocking {
                controller.runBackupRestoreFlow(
                    PREFERENCE,
                    outputDir(),
                    BackupTransportMode.CLOUD_ENCRYPTED,
                )
            }
        }

        assertFailure(BackupExecutionStage.BACKUP, BackupErrorCode.BACKUP_FAILED)
        assertTrue(device.commands.none { it.startsWith("pm clear") }, "${device.commands}")
    }

    @Test
    fun flowClassifiesARestorePollingTimeout() {
        onHealthyDevice { command ->
            if (command == "dumpsys backup") {
                throw IOException("RESTORE POLLING TIMED OUT WAITING FOR COMPLETION")
            } else {
                null
            }
        }

        assertFailsWith<IOException> {
            runBlocking { controller.runBackupRestoreFlow(PREFERENCE, outputDir(), LOCAL) }
        }

        assertFailure(BackupExecutionStage.RESTORE, BackupErrorCode.RESTORE_POLL_TIMEOUT)
    }

    @Test
    fun flowReportsWhichDomainFailedVerification() {
        onHealthyDevice { command ->
            if (command.contains(BackupRestoreController.ACTION_ASSERT_STORAGE)) {
                shellOutput(
                    runnerStdout("""{"status":"failure","error":"Expected 'v' but found 'x'"}""")
                )
            } else {
                null
            }
        }

        val e =
            assertFailsWith<IOException> {
                runBlocking { controller.runBackupRestoreFlow(PREFERENCE, outputDir(), LOCAL) }
            }

        assertEquals(
            "AssertStorageAction failed for $PREFERENCE: Expected 'v' but found 'x'",
            e.message,
        )
        assertFailure(BackupExecutionStage.VERIFICATION, BackupErrorCode.VERIFICATION_FAILED)
    }

    @Test
    fun flowSucceedsWhenPublishingTelemetryFails() {
        onHealthyDevice()
        val controller =
            BackupRestoreControllerImpl(
                device.session,
                FAKE_SERIAL,
                34,
                PACKAGE,
                telemetryPublisher = { _, _ -> throw RuntimeException("Telemetry publish failed") },
            )

        runBlocking { controller.runBackupRestoreFlow(PREFERENCE, outputDir(), LOCAL) }

        assertTrue(assertNotNull(controller.lastExecutionSummary).isSuccess)
    }

    @Test
    fun flowRejectsAnEmptyDomainListAsAPreconditionFailure() {
        assertFailsWith<IllegalArgumentException> {
            runBlocking { controller.runBackupRestoreFlow(emptyList(), outputDir(), LOCAL) }
        }

        assertFailure(BackupExecutionStage.PRECONDITION, BackupErrorCode.UNKNOWN_ERROR)
        assertTrue(device.commands.isEmpty(), "${device.commands}")
    }

    /**
     * Answers shell commands as a device on which every step of the local backup and restore flow
     * succeeds, except where [override] returns an output or throws.
     */
    private fun onHealthyDevice(override: (String) -> ShellCommandOutput? = { null }) {
        var restoreProgressChecks = 0
        device.onShell { command ->
            override(command)
                ?: when {
                    command.startsWith("am instrument") -> shellOutput(runnerStdout("{}"))
                    command.startsWith("pm clear") -> shellOutput("Success\n")
                    command == "bmgr list transports" -> shellOutput("  * $LOCAL_TRANSPORT\n")
                    // The restore pass is reported as registered once and then as ended.
                    command == "dumpsys backup" ->
                        if (++restoreProgressChecks == 1) {
                            shellOutput("Restore in progress: true\n")
                        } else {
                            shellOutput("Restore in progress: false\n")
                        }
                    else -> null
                }
        }
    }

    /**
     * Answers shell commands as a device on which [instrumentations] target the app, given as
     * `package/class` components, and every action succeeds.
     */
    private fun onDeviceWithInstrumentations(vararg instrumentations: String) {
        device.onShell { command ->
            when {
                command.startsWith("pm list instrumentation") ->
                    shellOutput(
                        instrumentations.joinToString("") {
                            "instrumentation:$it (target=$PACKAGE)\n"
                        }
                    )
                command.startsWith("am instrument") -> shellOutput(runnerStdout("{}"))
                else -> null
            }
        }
    }

    /** Returns the only `am instrument` command run on the device. */
    private fun instrumentCommand(): String =
        device.commands.single { it.startsWith("am instrument") }

    /**
     * Returns a backup service whose backups end with [backupServiceResult]. Unless that is an
     * error, a backup first writes the archive, as Studio's backup service does.
     */
    private fun fakeBackupService(): BackupService =
        // `backup` is a suspend function, which Mockito cannot stub with `when`.
        mock(BackupService::class.java) { invocation ->
            if (invocation.method.name == "backup") {
                val result = backupServiceResult
                if (result !is BackupResult.Error) {
                    (invocation.arguments[3] as Path).toFile().writeText("archive")
                }
                result
            } else {
                RETURNS_DEFAULTS.answer(invocation)
            }
        }

    private fun assertFailure(stage: BackupExecutionStage, errorCode: BackupErrorCode) {
        val summary = assertNotNull(controller.lastExecutionSummary)
        assertFalse(summary.isSuccess)
        assertEquals(stage, summary.failureStage)
        assertEquals(errorCode, summary.errorCode)
    }

    private fun outputDir() = tempFolder.root.toPath()

    private companion object {
        const val PACKAGE = "com.example.app"
        const val RUNNER = "androidx.test.backup.BackupRestoreTestRunner"
        const val RESULT_MARKER = "BACKUP_RESTORE_RESULT: "
        val LOCAL = BackupTransportMode.LOCAL
        val PREFERENCE = StorageDomain.Preference("app_prefs", "key", "val")
        val SHORT_TIMEOUT: Duration = Duration.ofMillis(200)

        /** Wraps an action payload in the envelope the on-device runner prints to stdout. */
        fun runnerStdout(payloadJson: String): String {
            val envelope = buildJsonObject {
                put("isSuccess", true)
                put("payloadJson", payloadJson)
            }
            return "$RESULT_MARKER$envelope\n"
        }

        /** Runner output for a payload that was written to [devicePath] instead of printed. */
        fun overflowStdout(devicePath: String): String {
            val envelope = buildJsonObject {
                put("isSuccess", true)
                put("payload_path", devicePath)
            }
            return "$RESULT_MARKER$envelope\n"
        }
    }
}
