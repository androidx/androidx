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
import com.android.backup.BackupException
import com.android.backup.BackupProgressListener
import com.android.backup.BackupResult
import com.android.backup.BackupService
import com.android.backup.BackupType
import com.android.backup.ErrorCode
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

    /** What every restore through the fake backup service ends with. */
    private var restoreServiceResult: BackupResult = BackupResult.Success

    /** Runs when a restore through the fake backup service starts. */
    private var onServiceRestore: () -> Unit = {}
    private val controller = controllerOnApi(34)

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
        assertTrue(
            command.startsWith("am instrument -w -e actionClass 'com.example.MyAction' "),
            command,
        )
        assertTrue(command.contains(" -e user_id '123' "), command)
        assertTrue(command.endsWith(" $PACKAGE.test/$RUNNER"), command)
    }

    /** The runner reads these keys itself, so an action argument must not set them. */
    @Test
    fun runOnDeviceRejectsArgumentsWithTheKeysOfTheRunner() {
        for (key in listOf("actionClass", "debug", "argsFile", "redirect_dir")) {
            val e =
                assertFailsWith<IllegalArgumentException> {
                    runBlocking {
                        controller.runOnDevice("com.example.MyAction", mapOf(key to "x"))
                    }
                }
            assertTrue(e.message.orEmpty().contains(key), e.message)
        }
        assertTrue(device.commands.isEmpty(), "${device.commands}")
    }

    /** Arguments that would make the command too long for adb reach the runner in a file. */
    @Test
    fun runOnDevicePassesArgumentsTooLongForTheCommandInAFile() = runBlocking {
        device.onShell { command ->
            if (command.startsWith("am instrument")) shellOutput(runnerStdout("{}")) else null
        }
        val args = mapOf("content" to "x'y".repeat(12_000), "other" to "1")

        val result = controller.runOnDevice("com.example.MyAction", args)

        assertEquals(BackupActionResult.Success(emptyMap()), result)
        val argsFile = device.pushedPaths.single()
        assertTrue(Regex("/data/local/tmp/backup_args_[0-9a-f-]+\\.json").matches(argsFile))
        assertEquals(
            args,
            Json.parseToJsonElement(device.pushedContents.getValue(argsFile))
                .jsonObject
                .mapValues { (_, value) ->
                    value.jsonPrimitive.content
                },
        )
        assertEquals(384, device.pushedModes.getValue(argsFile).modeBits)
        assertEquals(
            "am instrument -w -e actionClass 'com.example.MyAction' -e argsFile '$argsFile' " +
                "$PACKAGE.test/$RUNNER",
            instrumentCommand(),
        )
        assertEquals("rm -f -- '$argsFile'", device.commands.last())
    }

    @Test
    fun runOnDeviceKeepsArgumentsThatFitInTheCommandInline() = runBlocking {
        device.onShell { shellOutput(runnerStdout("{}")) }

        controller.runOnDevice("com.example.MyAction", mapOf("content" to "x".repeat(30_000)))

        assertTrue(device.pushedPaths.isEmpty(), "${device.pushedPaths}")
        assertTrue(instrumentCommand().contains(" -e content 'xxx"), "Arguments not inline")
    }

    /** The arguments file is deleted however the action ends. */
    @Test
    fun runOnDeviceDeletesTheArgumentsFileWhenTheActionTimesOut() = runBlocking {
        device.hangOn { it.startsWith("am instrument") }

        val result =
            controller.runOnDevice(
                "com.example.MyAction",
                mapOf("content" to "x".repeat(40_000)),
                timeout = SHORT_TIMEOUT,
            )

        assertEquals(BackupActionResult.Failure("Timed out after 200ms"), result)
        val argsFile = device.pushedPaths.single()
        assertTrue(device.commands.contains("rm -f -- '$argsFile'"), "${device.commands}")
    }

    /** The runner sends a result too large for one Binder transaction in parts. */
    @Test
    fun runOnDeviceReturnsAPayloadThatTheRunnerSentInParts() = runBlocking {
        val data = "d".repeat(200_000)
        val resultJson = buildJsonObject {
            put("isSuccess", true)
            put("payloadJson", buildJsonObject { put("data", data) }.toString())
        }
            .toString()
        device.onShell { command ->
            if (command.startsWith("am instrument")) {
                shellOutput(partedInstrumentOutput(resultJson, partLength = 65_536))
            } else {
                null
            }
        }

        val result = controller.runOnDevice("com.example.MyAction")

        assertEquals(BackupActionResult.Success(mapOf("data" to data)), result)
    }

    @Test
    fun runOnDeviceReturnsTheRunnerFailureAndStackTrace() = runBlocking {
        device.onShell {
            shellOutput(
                instrumentOutput(
                    """{"isSuccess":false,"errorMessage":"Something broke","stackTrace":"at MyAction.kt:15"}"""
                )
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
        assertTrue(command.startsWith("am instrument -w -e debug 'true' -e actionClass "), command)
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

    /** The installed paths identify an installation of the app and of its test APKs. */
    @Test
    fun installedApkPathsCoverATestApkOutsideThePackagesOfTheApp() = runBlocking {
        device.onShell { command ->
            when (command) {
                "pm list instrumentation $PACKAGE" ->
                    shellOutput(
                        "instrumentation:com.example.tests/$RUNNER (target=$PACKAGE)\n" +
                            "instrumentation:$PACKAGE.test/$RUNNER (target=$PACKAGE)\n"
                    )
                "pm list packages -f $PACKAGE" ->
                    shellOutput(
                        "package:/data/app/b/base.apk=$PACKAGE.test\n" +
                            "package:/data/app/a/base.apk=$PACKAGE\n"
                    )
                "pm list packages -f com.example.tests" ->
                    shellOutput("package:/data/app/t/base.apk=com.example.tests\n")
                else -> null
            }
        }
        val paths =
            listOf(
                "package:/data/app/a/base.apk=$PACKAGE",
                "package:/data/app/b/base.apk=$PACKAGE.test",
                "package:/data/app/t/base.apk=com.example.tests",
            )

        assertEquals(paths, controller.installedApkPaths())
        // The packages of the app already include its test APK.
        assertTrue(device.commands.none { it == "pm list packages -f $PACKAGE.test" })
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
    fun launchAppThrowsWhenTheActivityDoesNotExist() {
        device.onShell { command ->
            if (command.startsWith("am start")) {
                shellOutput(
                    "Starting: Intent { act=android.intent.action.MAIN " +
                        "cmp=$PACKAGE/.DoesNotExistActivity }\n" +
                        "Error type 3\n" +
                        "Error: Activity class {$PACKAGE/$PACKAGE.DoesNotExistActivity} " +
                        "does not exist.\n"
                )
            } else {
                null
            }
        }

        val e =
            assertFailsWith<IOException> {
                runBlocking { controller.launchApp(".DoesNotExistActivity") }
            }

        assertTrue(
            e.message.orEmpty().startsWith("Failed to start $PACKAGE/.DoesNotExistActivity "),
            e.message,
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

        controller.performRestore(localArchive())

        assertTrue(device.commands.contains("bmgr restore 1 $PACKAGE"), "${device.commands}")
    }

    @Test
    fun performRestoreRestoresTheLocalArchiveThatPerformBackupWrote() = runBlocking {
        onHealthyDevice()
        val archive = controller.performBackup(LOCAL, outputDir())

        controller.performRestore(archive)

        assertTrue(device.commands.contains("bmgr restore 1 $PACKAGE"), "${device.commands}")
    }

    /** A LOCAL archive is recognized by what it holds, so it can be renamed or moved. */
    @Test
    fun performRestoreRecognizesALocalArchiveWhateverItsName() = runBlocking {
        onHealthyDevice()

        controller.performRestore(localArchive(name = "renamed.zip"))

        assertTrue(device.commands.contains("bmgr restore 1 $PACKAGE"), "${device.commands}")
    }

    @Test
    fun performRestoreRejectsTheLocalArchiveOfAnotherDevice() {
        onHealthyDevice()

        val e =
            assertFailsWith<IOException> {
                runBlocking { controller.performRestore(localArchive(serial = "emulator-5556")) }
            }

        assertTrue(e.message!!.startsWith("backup_local_device.zip is a LOCAL backup made on"))
        assertTrue(device.commands.none { it.startsWith("bmgr") }, "${device.commands}")
    }

    /** A file named like a LOCAL archive that isn't one goes to the backup service. */
    @Test
    fun performRestoreSendsAnArchiveThatIsNotALocalOneToTheBackupService() {
        val failure = BackupException(ErrorCode.INVALID_BACKUP_FILE, "File is not a valid backup")
        restoreServiceResult = BackupResult.Error(ErrorCode.INVALID_BACKUP_FILE, failure)
        val archive = tempFolder.newFile("backup_local_device.zip").toPath()

        val e =
            assertFailsWith<BackupRestoreException> {
                runBlocking { controller.performRestore(archive) }
            }

        assertSame(failure, e.rootCause())
        assertTrue(device.commands.none { it.startsWith("bmgr") }, "${device.commands}")
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
    fun performBackupThrowsAFailureOfTheBackupServiceAsAnIOException() {
        val failure = BackupException(ErrorCode.APP_NOT_INSTALLED, "$PACKAGE is not installed")
        backupServiceResult = BackupResult.Error(ErrorCode.APP_NOT_INSTALLED, failure)

        val e =
            assertFailsWith<BackupRestoreException> {
                runBlocking {
                    controller.performBackup(BackupTransportMode.CLOUD_ENCRYPTED, outputDir())
                }
            }

        assertEquals("$PACKAGE is not installed", e.message)
        assertSame(failure, e.rootCause())
        assertEquals(BackupErrorCode.BACKUP_FAILED, e.errorCode)
    }

    @Test
    fun performRestoreThrowsAFailureOfTheBackupServiceAsAnIOException() {
        val failure = BackupException(ErrorCode.INVALID_BACKUP_FILE, "File is not a valid backup")
        restoreServiceResult = BackupResult.Error(ErrorCode.INVALID_BACKUP_FILE, failure)
        val archive = tempFolder.newFile("backup_cloud_encrypted_device.zip").toPath()

        val e =
            assertFailsWith<BackupRestoreException> {
                runBlocking { controller.performRestore(archive) }
            }

        assertEquals("File is not a valid backup", e.message)
        assertSame(failure, e.rootCause())
        assertEquals(BackupErrorCode.RESTORE_FAILED, e.errorCode)
    }

    @Test
    fun performRestoreThrowsWhenTheBackupServiceRestoresNoAppData() {
        restoreServiceResult = BackupResult.WithoutAppData
        val archive = tempFolder.newFile("backup_cloud_encrypted_device.zip").toPath()

        val e = assertFailsWith<IOException> { runBlocking { controller.performRestore(archive) } }

        assertEquals(
            "Restore of $PACKAGE from backup_cloud_encrypted_device.zip restored no app data",
            e.message,
        )
    }

    /** Google Play services reports the restore of an app that it skips as successful. */
    @Test
    fun performRestoreThrowsWhenGooglePlayServicesLogsThatItSkippedTheApp() {
        val events = mutableListOf<String>()
        onServiceRestore = { events += "restore" }
        device.onShell { command ->
            events += command
            when {
                command == "date +%s" -> shellOutput("1700000000\n")
                command.startsWith("logcat") ->
                    shellOutput(
                        "10-14 07:21:50.666 18335 18694 I Backup  : [CacheRestoreHelper] " +
                            "Package $PACKAGE is not eligible for D2D restore: " +
                            "INELIGIBLE_DENYLISTED\n"
                    )
                else -> null
            }
        }
        val archive = tempFolder.newFile("backup_cloud_encrypted_device.zip").toPath()

        val e = assertFailsWith<IOException> { runBlocking { controller.performRestore(archive) } }

        assertEquals(
            "Restore of $PACKAGE from backup_cloud_encrypted_device.zip restored no app data: " +
                "Google Play services skipped the app as not eligible for restore " +
                "(INELIGIBLE_DENYLISTED)",
            e.message,
        )
        // Only the entries logged since the restore started are read.
        assertEquals(
            listOf(
                "input keyevent KEYCODE_HOME",
                "date +%s",
                "restore",
                "logcat -d -t 1700000000.000",
            ),
            events,
        )
    }

    @Test
    fun performRestoreSucceedsWhenGooglePlayServicesLogsNoSkipOfTheApp() = runBlocking {
        device.onShell { command ->
            when {
                command == "date +%s" -> shellOutput("1700000000\n")
                command.startsWith("logcat") ->
                    shellOutput(
                        "10-14 07:29:35.360   686  1708 D BackupManagerService: restoreFinished " +
                            "packageName=$PACKAGE\n"
                    )
                else -> null
            }
        }
        val archive = tempFolder.newFile("backup_cloud_encrypted_device.zip").toPath()

        assertSame(controller, controller.performRestore(archive))
        assertEquals("logcat -d -t 1700000000.000", device.commands.last())
    }

    /** The log only explains a skip, so a log that cannot be read leaves the restore successful. */
    @Test
    fun performRestoreSucceedsWhenLogcatCannotBeRead() = runBlocking {
        device.onShell { command ->
            when {
                command == "date +%s" -> shellOutput("1700000000\n")
                command.startsWith("logcat") ->
                    shellOutput(stderr = "logcat: Unexpected EOF!\n", exitCode = 1)
                else -> null
            }
        }
        val archive = tempFolder.newFile("backup_cloud_encrypted_device.zip").toPath()

        assertSame(controller, controller.performRestore(archive))
    }

    @Test
    fun performRestoreDoesNotReadLogcatWhenTheDeviceClockCannotBeRead() = runBlocking {
        device.onShell { command ->
            if (command == "date +%s") shellOutput(stderr = "date: not found\n", exitCode = 127)
            else null
        }
        val archive = tempFolder.newFile("backup_cloud_encrypted_device.zip").toPath()

        assertSame(controller, controller.performRestore(archive))
        assertTrue(device.commands.none { it.startsWith("logcat") }, "${device.commands}")
    }

    @Test
    fun performRestoreThrowsWhenGooglePlayServicesIsTooOldToRestoreOnApi37() {
        val controller = controllerOnApi(37)
        var serviceRestores = 0
        onServiceRestore = { serviceRestores++ }
        device.onShell(gmsAnswers(gmsVersionCode = 263238038))
        val archive = tempFolder.newFile("backup_cloud_encrypted_device.zip").toPath()

        val e =
            assertFailsWith<BackupRestoreException> {
                runBlocking { controller.performRestore(archive) }
            }

        assertEquals(BackupErrorCode.GMSCORE_OUTDATED_OR_MISSING, e.errorCode)
        assertEquals(
            "Can't restore the backup on $FAKE_SERIAL: Google Play services 26.32.38 is too old. " +
                "On Android 16 QPR2 (API level 36.1) and higher, restoring cloud and " +
                "device-to-device backups requires Google Play services 26.33.30 or higher. " +
                "Update Google Play services through the Play Store on the device, or use a " +
                "system image that comes with a newer version",
            e.message,
        )
        assertEquals(0, serviceRestores)
        assertEquals(
            listOf("pm list packages --show-versioncode com.google.android.gms"),
            device.commands,
        )
    }

    @Test
    fun performRestoreThrowsWhenGooglePlayServicesIsTooOldToRestoreOnApi36Point1() {
        val controller = controllerOnApi(36)
        device.onShell(gmsAnswers(gmsVersionCode = 263238038, sdkMinor = "1"))
        val archive = tempFolder.newFile("backup_cloud_encrypted_device.zip").toPath()

        val e =
            assertFailsWith<BackupRestoreException> {
                runBlocking { controller.performRestore(archive) }
            }

        assertEquals(BackupErrorCode.GMSCORE_OUTDATED_OR_MISSING, e.errorCode)
    }

    /** Android 16 (API level 36.0) restores with any version of Google Play services. */
    @Test
    fun performRestoreDoesNotCheckTheGooglePlayServicesVersionOnApi36() = runBlocking {
        val controller = controllerOnApi(36)
        device.onShell(gmsAnswers(gmsVersionCode = 263238038))
        val archive = tempFolder.newFile("backup_cloud_encrypted_device.zip").toPath()

        assertSame(controller, controller.performRestore(archive))
        assertTrue(device.commands.contains("getprop ro.build.version.sdk_minor"))
        assertTrue(device.commands.none { it.startsWith("pm list packages") }, "${device.commands}")
    }

    @Test
    fun performRestoreGoesAheadWhenGooglePlayServicesIsRecentEnoughOnApi37() = runBlocking {
        val controller = controllerOnApi(37)
        var serviceRestores = 0
        onServiceRestore = { serviceRestores++ }
        device.onShell(gmsAnswers(gmsVersionCode = 263330038))
        val archive = tempFolder.newFile("backup_cloud_encrypted_device.zip").toPath()

        assertSame(controller, controller.performRestore(archive))
        assertEquals(1, serviceRestores)
    }

    /** The backup service reports a device without Google Play services itself. */
    @Test
    fun performRestoreGoesAheadWhenGooglePlayServicesIsNotInstalled() = runBlocking {
        val controller = controllerOnApi(37)
        var serviceRestores = 0
        onServiceRestore = { serviceRestores++ }
        device.onShell(gmsAnswers(gmsVersionCode = null))
        val archive = tempFolder.newFile("backup_cloud_encrypted_device.zip").toPath()

        assertSame(controller, controller.performRestore(archive))
        assertEquals(1, serviceRestores)
    }

    @Test
    fun performRestoreDoesNotCheckTheGooglePlayServicesVersionForALocalArchive() = runBlocking {
        val controller = controllerOnApi(37)
        onHealthyDevice(gmsAnswers(gmsVersionCode = 263238038))

        controller.performRestore(localArchive())

        assertTrue(device.commands.none { it.startsWith("pm list packages") }, "${device.commands}")
    }

    /** Studio's backup service returns its own cancellation, such as by a timeout, as an error. */
    @Test
    fun performBackupTimesOutWhenTheBackupServiceReturnsItsCancellation() {
        val controller =
            BackupRestoreControllerImpl(
                device.session,
                FAKE_SERIAL,
                34,
                PACKAGE,
                backupServiceProvider = { CancellationReturningBackupService() },
            )

        val e =
            assertFailsWith<IOException> {
                runBlocking {
                    controller.performBackup(
                        BackupTransportMode.CLOUD_ENCRYPTED,
                        outputDir(),
                        SHORT_TIMEOUT,
                    )
                }
            }

        assertEquals("Backup (CLOUD_ENCRYPTED) timed out after 200ms", e.message)
        assertEquals("am force-stop $PACKAGE", device.commands.last())
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
        val archive = localArchive()

        val e =
            assertFailsWith<BackupRestoreException> {
                runBlocking { controller.performRestore(archive, SHORT_TIMEOUT) }
            }

        assertEquals("Restore timed out after 200ms", e.message)
        assertEquals(BackupErrorCode.RESTORE_POLL_TIMEOUT, e.errorCode)
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
        val archive = localArchive()

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
        val archive = localArchive()

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
    fun flowClassifiesAFailureToSelectTheLocalTransportForTheRestoreAsABmgrFailure() {
        var transportSelections = 0
        onHealthyDevice { command ->
            // The backup selects the local transport first; the restore's selection fails.
            if (command == "bmgr transport '$LOCAL_TRANSPORT'" && ++transportSelections == 2) {
                shellOutput("Unknown transport '$LOCAL_TRANSPORT' specified; no changes made.\n")
            } else {
                null
            }
        }

        assertFailsWith<IOException> {
            runBlocking { controller.runBackupRestoreFlow(PREFERENCE, outputDir(), LOCAL) }
        }

        assertFailure(BackupExecutionStage.RESTORE, BackupErrorCode.BMGR_INIT_FAILED)
    }

    /** The cause comes from the step that failed, not from words in the message. */
    @Test
    fun flowClassifiesAFailedRestoreCommandAsARestoreFailureWhateverItsMessage() {
        onHealthyDevice { command ->
            if (command.startsWith("bmgr restore")) {
                throw IOException("bmgr: transport initialization error, timed out polling")
            } else {
                null
            }
        }

        assertFailsWith<IOException> {
            runBlocking { controller.runBackupRestoreFlow(PREFERENCE, outputDir(), LOCAL) }
        }

        assertFailure(BackupExecutionStage.RESTORE, BackupErrorCode.RESTORE_FAILED)
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
    fun flowClassifiesAFailureWhileWaitingForTheLocalRestoreAsARestoreFailure() {
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

        assertFailure(BackupExecutionStage.RESTORE, BackupErrorCode.RESTORE_FAILED)
    }

    /** The message of this backup service error names neither GmsCore nor the Play Store. */
    @Test
    fun flowClassifiesAnOutdatedGmsCoreByTheErrorCodeOfTheBackupService() {
        onHealthyDevice()
        backupServiceResult =
            BackupResult.Error(
                ErrorCode.GMSCORE_IS_TOO_OLD,
                BackupException(ErrorCode.GMSCORE_IS_TOO_OLD, "Google Services are outdated"),
            )

        assertFailsWith<IOException> {
            runBlocking { controller.runBackupRestoreFlow(PREFERENCE, outputDir(), CLOUD) }
        }

        assertFailure(BackupExecutionStage.BACKUP, BackupErrorCode.GMSCORE_OUTDATED_OR_MISSING)
    }

    @Test
    fun flowClassifiesGooglePlayServicesTooOldToRestoreAsAnOutdatedGmsCore() {
        val controller = controllerOnApi(37)
        onHealthyDevice(gmsAnswers(gmsVersionCode = 263238038))

        assertFailsWith<IOException> {
            runBlocking { controller.runBackupRestoreFlow(PREFERENCE, outputDir(), CLOUD) }
        }

        assertFailure(
            BackupExecutionStage.RESTORE,
            BackupErrorCode.GMSCORE_OUTDATED_OR_MISSING,
            controller,
        )
    }

    /** The error code decides the cause, even though the message says "Timed out". */
    @Test
    fun flowClassifiesATransportFailureByTheErrorCodeOfTheBackupService() {
        onHealthyDevice()
        restoreServiceResult =
            BackupResult.Error(
                ErrorCode.TRANSPORT_NOT_SELECTED,
                BackupException(
                    ErrorCode.TRANSPORT_NOT_SELECTED,
                    "Timed out when setting transport",
                ),
            )

        assertFailsWith<IOException> {
            runBlocking { controller.runBackupRestoreFlow(PREFERENCE, outputDir(), CLOUD) }
        }

        assertFailure(BackupExecutionStage.RESTORE, BackupErrorCode.BMGR_INIT_FAILED)
    }

    @Test
    fun flowClassifiesAnyOtherErrorOfTheBackupServiceByItsStage() {
        onHealthyDevice()
        backupServiceResult =
            BackupResult.Error(ErrorCode.UNEXPECTED_ERROR, IOException("bmgr transport GmsCore"))

        assertFailsWith<IOException> {
            runBlocking { controller.runBackupRestoreFlow(PREFERENCE, outputDir(), CLOUD) }
        }

        assertFailure(BackupExecutionStage.BACKUP, BackupErrorCode.BACKUP_FAILED)
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

    /** A cancelled flow, such as one that a test timeout stops, neither failed nor succeeded. */
    @Test
    fun flowRecordsNothingWhenCancelled() = runBlocking {
        device.hangOn { it.startsWith("am instrument") }

        val flow = launch { controller.runBackupRestoreFlow(PREFERENCE, outputDir(), LOCAL) }
        withTimeout(10.seconds) {
            while (device.commands.none { it.startsWith("am instrument") }) delay(10)
        }
        flow.cancelAndJoin()

        assertNull(controller.lastExecutionSummary)
        assertTrue(publishedMetrics.isEmpty(), "$publishedMetrics")
    }

    /** A CancellationException that does not cancel the flow reports an error, like a timeout. */
    @Test
    fun flowRecordsACancellationExceptionOfAnActiveFlowAsAFailure() {
        onHealthyDevice { command ->
            if (command.startsWith("am instrument")) throw CancellationException("Timed out")
            else null
        }

        assertFailsWith<CancellationException> {
            runBlocking { controller.runBackupRestoreFlow(PREFERENCE, outputDir(), LOCAL) }
        }

        assertFailure(BackupExecutionStage.SEEDING, BackupErrorCode.SEEDING_FAILED)
        assertEquals("FAILURE", publishedMetrics[BackupReportKeys.STATUS])
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

    /** Returns a controller of the app on the fake device, taken to run API level [apiLevel]. */
    private fun controllerOnApi(apiLevel: Int) =
        BackupRestoreControllerImpl(
            device.session,
            FAKE_SERIAL,
            apiLevel,
            PACKAGE,
            telemetryPublisher = { key, value -> publishedMetrics[key] = value },
            backupServiceProvider = { fakeBackupService() },
        )

    /**
     * Returns a shell handler that reports the minor API level as [sdkMinor] and Google Play
     * services as installed with [gmsVersionCode], or as not installed when it is null, and leaves
     * other commands to the default output.
     */
    private fun gmsAnswers(
        gmsVersionCode: Long?,
        sdkMinor: String = "",
    ): (String) -> ShellCommandOutput? = { command ->
        when (command) {
            "getprop ro.build.version.sdk_minor" -> shellOutput("$sdkMinor\n")
            "pm list packages --show-versioncode com.google.android.gms" ->
                shellOutput(
                    "package:com.google.android.gms.supervision versionCode:106632\n" +
                        gmsVersionCode
                            ?.let { "package:com.google.android.gms versionCode:$it\n" }
                            .orEmpty()
                )
            else -> null
        }
    }

    /**
     * Returns a backup service whose backups end with [backupServiceResult] and whose restores end
     * with [restoreServiceResult]. Unless the backup result is an error, a backup first writes the
     * archive, as Studio's backup service does.
     */
    private fun fakeBackupService(): BackupService =
        // `backup` and `restore` are suspend functions, which Mockito cannot stub with `when`.
        mock(BackupService::class.java) { invocation ->
            when (invocation.method.name) {
                "backup" -> {
                    val result = backupServiceResult
                    if (result !is BackupResult.Error) {
                        (invocation.arguments[3] as Path).toFile().writeText("archive")
                    }
                    result
                }
                "restore" -> {
                    onServiceRestore()
                    restoreServiceResult
                }
                else -> RETURNS_DEFAULTS.answer(invocation)
            }
        }

    /**
     * A backup service whose backups run until they are cancelled and then return the cancellation
     * as an error, as Studio's backup service does.
     */
    private class CancellationReturningBackupService :
        BackupService by mock(BackupService::class.java) {
        override suspend fun backup(
            serialNumber: String,
            applicationId: String,
            type: BackupType,
            backupFile: Path,
            listener: BackupProgressListener?,
        ): BackupResult =
            try {
                awaitCancellation()
            } catch (e: CancellationException) {
                BackupResult.Error(ErrorCode.UNEXPECTED_ERROR, e)
            }
    }

    private fun assertFailure(
        stage: BackupExecutionStage,
        errorCode: BackupErrorCode,
        controller: BackupRestoreControllerImpl = this.controller,
    ) {
        val summary = assertNotNull(controller.lastExecutionSummary)
        assertFalse(summary.isSuccess)
        assertEquals(stage, summary.failureStage)
        assertEquals(errorCode, summary.errorCode)
    }

    private fun outputDir() = tempFolder.root.toPath()

    /** Returns a new marker of a LOCAL backup of [packageName] made on [serial], named [name]. */
    private fun localArchive(
        packageName: String = PACKAGE,
        serial: String = FAKE_SERIAL,
        name: String = "backup_local_device.zip",
    ): Path {
        val archive = tempFolder.root.toPath().resolve(name)
        ZipOutputStream(Files.newOutputStream(archive)).use { zip ->
            zip.putNextEntry(ZipEntry("androidx.test.backup.local.properties"))
            zip.write("package=$packageName\nserial=$serial\n".toByteArray())
            zip.closeEntry()
        }
        return archive
    }

    /** Stack-trace recovery may wrap an exception that crosses a coroutine in a copy of it. */
    private fun Throwable.rootCause(): Throwable = generateSequence(this) { it.cause }.last()

    private companion object {
        const val PACKAGE = "com.example.app"
        const val RUNNER = "androidx.test.backup.BackupRestoreTestRunner"
        val LOCAL = BackupTransportMode.LOCAL
        val CLOUD = BackupTransportMode.CLOUD_ENCRYPTED
        val PREFERENCE = StorageDomain.Preference("app_prefs", "key", "val")
        val SHORT_TIMEOUT: Duration = Duration.ofMillis(200)
    }
}
