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

import androidx.test.backup.host.BackupDeviceShell.Companion.quote
import androidx.test.backup.host.BackupDeviceShell.Companion.quoteIfNeeded
import com.android.adblib.ShellCommandOutput
import java.io.IOException
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BackupDeviceShellTest {

    @get:Rule val tempFolder = TemporaryFolder()

    private val device = FakeAdbDevice()
    private val shell = BackupDeviceShell(device.session, FAKE_SERIAL)

    @Test
    fun quoteWrapsInSingleQuotesSoTheShellExpandsNothing() {
        assertEquals("'plain'", quote("plain"))
        assertEquals("''", quote(""))
        assertEquals("'\$HOME `id` \\ \"x\"'", quote("\$HOME `id` \\ \"x\""))
        assertEquals("""'O'\''Brian'""", quote("O'Brian"))
    }

    @Test
    fun quoteIfNeededLeavesShellSafeTokensBare() {
        assertEquals("com.example.app", quoteIfNeeded("com.example.app"))
        assertEquals("-r", quoteIfNeeded("-r"))
        assertEquals("--user=10", quoteIfNeeded("--user=10"))
        assertEquals("/data/local/tmp/a_b", quoteIfNeeded("/data/local/tmp/a_b"))
    }

    @Test
    fun quoteIfNeededQuotesEmptyAndUnsafeTokens() {
        assertEquals("''", quoteIfNeeded(""))
        assertEquals("'a b'", quoteIfNeeded("a b"))
        assertEquals("'danger;key\$1'", quoteIfNeeded("danger;key\$1"))
        assertEquals("'Outer\$Inner'", quoteIfNeeded("Outer\$Inner"))
    }

    @Test
    fun execReturnsTheCommandOutput() = runBlocking {
        device.onShell { shellOutput(stdout = "out", stderr = "err", exitCode = 3) }

        val output = shell.exec("echo hi")

        assertEquals(listOf("echo hi"), device.commands)
        assertEquals("out", output.stdout)
        assertEquals("err", output.stderr)
        assertEquals(3, output.exitCode)
    }

    @Test
    fun execAddressesOnlyTheShellsOwnDevice() {
        val otherShell = BackupDeviceShell(device.session, "other-serial")

        assertFailsWith<AssertionError> { runBlocking { otherShell.exec("echo hi") } }
        assertEquals(emptyList<String>(), device.commands)
    }

    @Test
    fun pullFileWritesTheDeviceFileToTheHost() = runBlocking {
        device.onPull { "content of $it" }
        val hostFile = tempFolder.root.toPath().resolve("pulled.txt")

        shell.pullFile("/sdcard/a.txt", hostFile)

        assertEquals(listOf("/sdcard/a.txt"), device.pulledPaths)
        assertEquals("content of /sdcard/a.txt", hostFile.toFile().readText())
    }

    @Test
    fun removeFileQuotesThePathAndEndsOptions() = runBlocking {
        shell.removeFile("/data/local/tmp/-odd \$name'.json")

        assertEquals(
            listOf("""rm -f -- '/data/local/tmp/-odd ${'$'}name'\''.json'"""),
            device.commands,
        )
    }

    @Test
    fun instrumentQuotesEveryValueAndOnlyUnsafeKeys() = runBlocking {
        device.onShell { shellOutput(stdout = "runner output") }

        val stdout =
            shell.instrument(
                "com.example.app.test/androidx.test.backup.BackupRestoreTestRunner",
                linkedMapOf(
                    "actionClass" to "com.example.My\$Action",
                    "value" to "`whoami` \$(id)",
                    "danger;key" to "v",
                    "" to "empty key",
                ),
            )

        assertEquals("runner output", stdout)
        assertEquals(
            listOf(
                "am instrument -w" +
                    " -e actionClass 'com.example.My\$Action'" +
                    " -e value '`whoami` \$(id)'" +
                    " -e 'danger;key' 'v'" +
                    " -e '' 'empty key'" +
                    " com.example.app.test/androidx.test.backup.BackupRestoreTestRunner"
            ),
            device.commands,
        )
    }

    @Test
    fun startActivityTargetsTheComponentWhenGiven() = runBlocking {
        shell.startActivity(
            action = BackupDeviceShell.ACTION_MAIN,
            category = BackupDeviceShell.CATEGORY_LAUNCHER,
            component = "com.example.app/.Main\$Inner",
            packageName = "com.example.app",
            stringExtras = emptyMap(),
        )

        assertEquals(
            listOf(
                "am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER" +
                    " -n 'com.example.app/.Main\$Inner'"
            ),
            device.commands,
        )
    }

    @Test
    fun startActivityResolvesWithinThePackageWithoutAComponent() = runBlocking {
        shell.startActivity(
            action = "com.example.CUSTOM",
            category = null,
            component = null,
            packageName = "com.example.app",
            stringExtras = emptyMap(),
        )

        assertEquals(
            listOf("am start -W -a com.example.CUSTOM -p com.example.app"),
            device.commands,
        )
    }

    @Test
    fun startActivityQuotesExtras() = runBlocking {
        shell.startActivity(
            action = "android.intent.action.VIEW",
            category = null,
            component = null,
            packageName = "com.example.app",
            stringExtras =
                linkedMapOf(
                    "name" to "O'Brian",
                    "danger;cmd" to "'; rm -rf /; '",
                    "" to "empty_key_val",
                ),
        )

        assertEquals(
            "am start -W -a android.intent.action.VIEW -p com.example.app" +
                """ --es name 'O'\''Brian'""" +
                """ --es 'danger;cmd' ''\''; rm -rf /; '\'''""" +
                " --es '' 'empty_key_val'",
            device.commands.single(),
        )
    }

    @Test
    fun startActivityThrowsWhenTheActivityDoesNotExist() {
        // `am start -W` exits with 0 here.
        val stdout =
            "Starting: Intent { act=android.intent.action.MAIN cmp=com.example.app/.Missing }\n" +
                "Error type 3\n" +
                "Error: Activity class {com.example.app/com.example.app.Missing} does not exist.\n"
        device.onShell { shellOutput(stdout = stdout) }

        val e =
            assertFailsWith<IOException> {
                runBlocking { startMainActivity("com.example.app/.Missing") }
            }

        assertEquals(
            "Failed to start com.example.app/.Missing (exit code 0): ${stdout.trim()}",
            e.message,
        )
    }

    @Test
    fun startActivityThrowsWhenTheIntentDoesNotResolve() {
        val stdout =
            "Starting: Intent { act=android.intent.action.MAIN pkg=com.example.app }\n" +
                "Error: Activity not started, unable to resolve Intent " +
                "{ act=android.intent.action.MAIN flg=0x10000000 pkg=com.example.app }\n"
        device.onShell { shellOutput(stdout = stdout) }

        val e = assertFailsWith<IOException> { runBlocking { startMainActivity(component = null) } }

        assertEquals("Failed to start com.example.app (exit code 0): ${stdout.trim()}", e.message)
    }

    @Test
    fun startActivityThrowsWhenAmStartFails() {
        val stdout = "Starting: Intent { cmp=com.example.app/.Private }\n"
        val stderr =
            "\nException occurred while executing 'start':\n" +
                "java.lang.SecurityException: Permission Denial: starting Intent " +
                "{ cmp=com.example.app/.Private } not exported from uid 10123\n"
        device.onShell { shellOutput(stdout = stdout, stderr = stderr, exitCode = 255) }

        val e =
            assertFailsWith<IOException> {
                runBlocking { startMainActivity("com.example.app/.Private") }
            }

        assertEquals(
            "Failed to start com.example.app/.Private (exit code 255): " +
                "${stderr.trim()} ${stdout.trim()}",
            e.message,
        )
    }

    @Test
    fun startActivityAcceptsAnActivityThatIsAlreadyRunning() = runBlocking {
        device.onShell {
            shellOutput(
                stdout =
                    "Starting: Intent { act=android.intent.action.MAIN cmp=com.example.app/.Main }\n" +
                        "Warning: Activity not started, its current task has been brought to " +
                        "the front\n" +
                        "Status: ok\n" +
                        "LaunchState: HOT\n" +
                        "Activity: com.example.app/.Main\n" +
                        "TotalTime: 923\n" +
                        "WaitTime: 925\n" +
                        "Complete\n"
            )
        }

        startMainActivity("com.example.app/.Main")

        assertEquals(1, device.commands.size)
    }

    @Test
    fun resolveLauncherActivitySkipsTheOtherResolveFields() = runBlocking {
        device.onShell {
            shellOutput(RESOLVE_OUTPUT_PREFIX + "com.example.app/.ui.Main\$Launcher\n")
        }

        assertEquals(
            "com.example.app/.ui.Main\$Launcher",
            shell.resolveLauncherActivity("com.example.app"),
        )
        assertEquals(
            listOf(
                "cmd package resolve-activity --brief -c android.intent.category.LAUNCHER " +
                    "com.example.app"
            ),
            device.commands,
        )
    }

    @Test
    fun resolveLauncherActivityReturnsNullWithoutALauncher() = runBlocking {
        device.onShell { shellOutput("No activity found\n") }

        assertNull(shell.resolveLauncherActivity("com.example.app"))
    }

    /** `pm` shortens a class in the instrumentation's own package to `.<name>`. */
    @Test
    fun listInstrumentationsReturnsFullyQualifiedComponentsForTheTarget() = runBlocking {
        device.onShell {
            shellOutput(
                "instrumentation:com.example.app.test/androidx.test.backup.BackupRestoreTestRunner" +
                    " (target=com.example.app)\n" +
                    "instrumentation:com.example.tests/.Runner (target=com.example.app)\n" +
                    "instrumentation:com.other.test/.Runner (target=com.other)\n"
            )
        }

        assertEquals(
            listOf(
                "com.example.app.test/androidx.test.backup.BackupRestoreTestRunner",
                "com.example.tests/com.example.tests.Runner",
            ),
            shell.listInstrumentations("com.example.app"),
        )
        assertEquals(listOf("pm list instrumentation com.example.app"), device.commands)
    }

    @Test
    fun listInstrumentationsReturnsNothingWhenNoneIsInstalled() = runBlocking {
        assertEquals(emptyList(), shell.listInstrumentations("com.example.app"))
    }

    @Test
    fun listPackagePathsReturnsTheSortedLinesOfThePackagesThatMatchTheFilter() = runBlocking {
        val app = "package:/data/app/~~bW9Kg==/com.example.app-aXd2Q==/base.apk=com.example.app"
        val testApk =
            "package:/data/app/~~Zr3Fq==/com.example.app.test-Qm1Lp==/base.apk=com.example.app.test"
        device.onShell { shellOutput("$app\n$testApk\n") }

        assertEquals(listOf(testApk, app), shell.listPackagePaths("com.example.app"))
        assertEquals(listOf("pm list packages -f com.example.app"), device.commands)
    }

    @Test
    fun listPackagePathsReturnsNothingWhenNoPackageMatches() = runBlocking {
        device.onShell { shellOutput() }

        assertEquals(emptyList(), shell.listPackagePaths("com.example.app"))
    }

    @Test
    fun unstopPackageStartsTheLauncherActivityThenGoesHome() = runBlocking {
        device.onShell { command ->
            if (command.contains("resolve-activity")) {
                shellOutput(RESOLVE_OUTPUT_PREFIX + "com.example.app/com.example.app.Main\$Inner\n")
            } else {
                shellOutput()
            }
        }

        shell.unstopPackage("com.example.app")

        assertEquals(
            listOf(
                "am broadcast -a android.intent.action.MAIN -p com.example.app " +
                    "--include-stopped-packages",
                "cmd package resolve-activity --brief -c android.intent.category.LAUNCHER " +
                    "com.example.app",
                "am start -W -n 'com.example.app/com.example.app.Main\$Inner'",
                "input keyevent KEYCODE_HOME",
            ),
            device.commands,
        )
    }

    @Test
    fun unstopPackageReliesOnTheBroadcastWithoutALauncher() = runBlocking {
        device.onShell { command ->
            if (command.contains("resolve-activity")) shellOutput("No activity found\n")
            else shellOutput()
        }

        shell.unstopPackage("com.example.app")

        assertEquals(
            listOf(
                "am broadcast -a android.intent.action.MAIN -p com.example.app " +
                    "--include-stopped-packages",
                "cmd package resolve-activity --brief -c android.intent.category.LAUNCHER " +
                    "com.example.app",
            ),
            device.commands,
        )
    }

    @Test
    fun forceStopStopsThePackage() = runBlocking {
        shell.forceStop("com.example.app")

        assertEquals(listOf("am force-stop com.example.app"), device.commands)
    }

    @Test
    fun clearPackageDataSucceedsWhenPmClearReportsSuccess() = runBlocking {
        device.onShell { shellOutput("Success\n") }

        shell.clearPackageData("com.example.app")

        assertEquals(listOf("pm clear com.example.app"), device.commands)
    }

    @Test
    fun clearPackageDataThrowsWhenPmClearReportsFailure() {
        device.onShell { shellOutput(stderr = "Failed\n", exitCode = 1) }

        val e = assertFailsWith<IOException> { runBlocking { shell.clearPackageData(PACKAGE) } }
        assertEquals("Failed to clear app data for $PACKAGE (exit code 1): Failed", e.message)
    }

    @Test
    fun clearPackageDataThrowsWhenPmClearDoesNotReportSuccess() {
        device.onShell { shellOutput() }

        val e = assertFailsWith<IOException> { runBlocking { shell.clearPackageData(PACKAGE) } }
        assertEquals("Failed to clear app data for $PACKAGE (exit code 0)", e.message)
    }

    @Test
    fun clearPackageDataThrowsWhenFailureIsReportedOnStdout() {
        // Without the shell v2 protocol stderr is merged into stdout and no exit code is reported.
        device.onShell { shellOutput("Failed\n") }

        val e = assertFailsWith<IOException> { runBlocking { shell.clearPackageData(PACKAGE) } }
        assertEquals("Failed to clear app data for $PACKAGE (exit code 0): Failed", e.message)
    }

    @Test
    fun clearPackageDataReportsIdenticalStreamsOnce() {
        device.onShell { shellOutput("Failed\n", "Failed\n", 1) }

        val e = assertFailsWith<IOException> { runBlocking { shell.clearPackageData(PACKAGE) } }
        assertEquals("Failed to clear app data for $PACKAGE (exit code 1): Failed", e.message)
    }

    @Test
    fun installPackageStagesInstallsAndRemovesTheApk() = runBlocking {
        device.onShell { command ->
            if (command.startsWith("pm install")) shellOutput("Success\n") else shellOutput()
        }

        shell.installPackage(tempFolder.newFile("app.apk").toPath(), listOf("-r", "-t"))

        val stagedApk = device.pushedPaths.single()
        assertTrue(STAGED_APK.matches(stagedApk), stagedApk)
        assertEquals(
            listOf("pm install -r -t '$stagedApk'", "rm -f -- '$stagedApk'"),
            device.commands,
        )
    }

    @Test
    fun installPackageStagesEachApkAtItsOwnPath() = runBlocking {
        device.onShell { command ->
            if (command.startsWith("pm install")) shellOutput("Success\n") else shellOutput()
        }
        val apk = tempFolder.newFile("app.apk").toPath()

        shell.installPackage(apk, emptyList())
        shell.installPackage(apk, emptyList())

        assertEquals(2, device.pushedPaths.distinct().size, "${device.pushedPaths}")
    }

    @Test
    fun installPackageRemovesTheStagedApkWhenPmInstallFails() {
        device.onShell { command ->
            if (command.startsWith("pm install")) {
                shellOutput("Failure [INSTALL_FAILED_OLDER_SDK]\n", exitCode = 1)
            } else {
                shellOutput()
            }
        }
        val apk = tempFolder.newFile("app.apk").toPath()

        val e =
            assertFailsWith<IOException> { runBlocking { shell.installPackage(apk, emptyList()) } }
        assertEquals(
            "Failed to install $apk (exit code 1): Failure [INSTALL_FAILED_OLDER_SDK]",
            e.message,
        )
        assertEquals("rm -f -- '${device.pushedPaths.single()}'", device.commands.last())
    }

    @Test
    fun installPackageThrowsWhenPmInstallCannotParseTheApk() {
        val stderr =
            "\nException occurred while executing 'install':\n" +
                "java.lang.IllegalArgumentException: Error: Failed to parse APK file\n"
        device.onShell { command ->
            if (command.startsWith("pm install")) {
                shellOutput(stderr = stderr, exitCode = 255)
            } else {
                shellOutput()
            }
        }
        val apk = tempFolder.newFile("not_an.apk").toPath()

        val e =
            assertFailsWith<IOException> { runBlocking { shell.installPackage(apk, emptyList()) } }
        assertEquals("Failed to install $apk (exit code 255): ${stderr.trim()}", e.message)
    }

    @Test
    fun withCleanupCleansUpAfterAFailure() {
        var cleanedUp = false

        assertFailsWith<IOException> {
            runBlocking { withCleanup(cleanup = { cleanedUp = true }) { throw IOException() } }
        }
        assertTrue(cleanedUp)
    }

    /** A cleanup that fails for the same reason as the block must not hide the root cause. */
    @Test
    fun withCleanupKeepsTheBlockFailureWhenCleanupAlsoFails() {
        val e =
            assertFailsWith<IOException> {
                runBlocking {
                    withCleanup(cleanup = { throw IOException("device offline") }) {
                        throw IOException("bmgr restore failed")
                    }
                }
            }
        assertEquals("bmgr restore failed", e.message)
        assertEquals(listOf("device offline"), e.suppressed.map { it.message })
    }

    @Test
    fun withCleanupRunsASuspendingCleanupWhenCancelled() = runBlocking {
        var cleanedUp = false
        val job =
            launch(start = CoroutineStart.UNDISPATCHED) {
                withCleanup(
                    cleanup = {
                        yield()
                        cleanedUp = true
                    }
                ) {
                    awaitCancellation()
                }
            }

        job.cancelAndJoin()

        assertTrue(cleanedUp)
    }

    @Test
    fun withCleanupRunsASuspendingCleanupWhenCancelledAfterTheBlockSucceeds() = runBlocking {
        var cleanedUp = false
        launch {
            withCleanup(
                cleanup = {
                    yield()
                    cleanedUp = true
                }
            ) {
                currentCoroutineContext().job.cancel()
            }
        }
            .join()

        assertTrue(cleanedUp)
    }

    @Test
    fun withCleanupFailsWhenCleanupDoesNotFinishInTime() {
        val e =
            assertFailsWith<IOException> {
                runBlocking {
                    withCleanup(
                        cleanup = { awaitCancellation() },
                        cleanupTimeout = 100.milliseconds,
                    ) {}
                }
            }
        assertEquals("Cleanup did not finish within 100ms", e.message)
    }

    @Test
    fun withCleanupKeepsTheBlockFailureWhenCleanupDoesNotFinishInTime() {
        val e =
            assertFailsWith<IOException> {
                runBlocking {
                    withCleanup(
                        cleanup = { awaitCancellation() },
                        cleanupTimeout = 100.milliseconds,
                    ) {
                        throw IOException("bmgr restore failed")
                    }
                }
            }
        assertEquals("bmgr restore failed", e.message)
        assertEquals(
            listOf("Cleanup did not finish within 100ms"),
            e.suppressed.map { it.message },
        )
    }

    @Test
    fun wakeAndDismissKeyguardWakesTheScreenFirst() = runBlocking {
        shell.wakeAndDismissKeyguard()

        assertEquals(
            listOf("input keyevent KEYCODE_WAKEUP", "wm dismiss-keyguard"),
            device.commands,
        )
    }

    @Test
    fun pressHomeSendsTheHomeKey() = runBlocking {
        shell.pressHome()

        assertEquals(listOf("input keyevent KEYCODE_HOME"), device.commands)
    }

    @Test
    fun dumpLogcatReadsTheEntriesSinceTheStartOfTheWindow() = runBlocking {
        onDeviceClock(logcat = shellOutput("log line\n"))

        assertEquals("log line\n", shell.dumpLogcat(Duration.ofSeconds(30)))
        assertEquals(listOf("date +%s", "logcat -d -t 1699999970.000"), device.commands)
    }

    @Test
    fun dumpLogcatKeepsTheMillisecondsOfTheWindow() = runBlocking {
        onDeviceClock()

        shell.dumpLogcat(Duration.ofMillis(1_500))

        assertEquals("logcat -d -t 1699999998.500", device.commands.last())
    }

    @Test
    fun dumpLogcatLimitsTheWindowToTheTimeSinceTheEpoch() = runBlocking {
        onDeviceClock()

        shell.dumpLogcat(Duration.ofSeconds(Long.MAX_VALUE))
        shell.dumpLogcat(Duration.ofSeconds(-30))

        assertEquals(
            listOf("logcat -d -t 0.000", "logcat -d -t 1700000000.000"),
            device.commands.filter { it.startsWith("logcat") },
        )
    }

    @Test
    fun dumpLogcatThrowsWhenLogcatFails() {
        onDeviceClock(
            logcat =
                shellOutput(
                    stderr = "logcat: -t '1699999970.000' not in time format\n",
                    exitCode = 1,
                )
        )

        val e = assertFailsWith<IOException> { runBlocking { shell.dumpLogcat(THIRTY_SECONDS) } }
        assertEquals(
            "logcat failed (exit code 1): logcat: -t '1699999970.000' not in time format",
            e.message,
        )
    }

    @Test
    fun dumpLogcatThrowsWhenTheDeviceClockCannotBeRead() {
        device.onShell { shellOutput(stderr = "date: not found\n", exitCode = 127) }

        val e = assertFailsWith<IOException> { runBlocking { shell.dumpLogcat(THIRTY_SECONDS) } }
        assertEquals("Failed to read the device clock (exit code 127): date: not found", e.message)
        assertEquals(listOf("date +%s"), device.commands)
    }

    /** A Linux clock cannot be set before the epoch, so a negative reading is not a time. */
    @Test
    fun dumpLogcatThrowsWhenTheDeviceClockReadsBeforeTheEpoch() {
        onDeviceClock(epochSeconds = -1)

        val e = assertFailsWith<IOException> { runBlocking { shell.dumpLogcat(THIRTY_SECONDS) } }
        assertEquals("Failed to read the device clock (exit code 0): -1", e.message)
        assertEquals(listOf("date +%s"), device.commands)
    }

    @Test
    fun dumpLogcatDoesNotOverflowOnTheLargestClockReading() = runBlocking {
        onDeviceClock(epochSeconds = Long.MAX_VALUE)

        shell.dumpLogcat(THIRTY_SECONDS)

        assertEquals("logcat -d -t ${Long.MAX_VALUE - 30}.000", device.commands.last())
    }

    @Test
    fun clearLogcatClearsTheBuffer() = runBlocking {
        shell.clearLogcat()

        assertEquals(listOf("logcat -c"), device.commands)
    }

    /** Answers `date +%s` with [epochSeconds] and logcat dumps with [logcat]. */
    private fun onDeviceClock(
        epochSeconds: Long = DEVICE_EPOCH_SECONDS,
        logcat: ShellCommandOutput = shellOutput(),
    ) {
        device.onShell { command ->
            if (command == "date +%s") shellOutput("$epochSeconds\n") else logcat
        }
    }

    /** Starts the main activity [component], or resolves one within [PACKAGE] if it is null. */
    private suspend fun startMainActivity(component: String?) {
        shell.startActivity(
            action = BackupDeviceShell.ACTION_MAIN,
            category = null,
            component = component,
            packageName = PACKAGE,
            stringExtras = emptyMap(),
        )
    }

    private companion object {
        const val PACKAGE = "com.example.app"
        val STAGED_APK = Regex("""/data/local/tmp/backup_test_[0-9a-f-]{36}\.apk""")
        const val RESOLVE_OUTPUT_PREFIX =
            "priority=0 preferredOrder=0 match=0x108000 specificIndex=-1 isDefault=true\n"
        const val DEVICE_EPOCH_SECONDS = 1_700_000_000L
        val THIRTY_SECONDS: Duration = Duration.ofSeconds(30)
    }
}
