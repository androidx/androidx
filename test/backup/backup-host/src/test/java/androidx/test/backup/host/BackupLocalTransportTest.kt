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

import java.io.IOException
import java.util.zip.ZipFile
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BackupLocalTransportTest {

    @get:Rule val tempFolder = TemporaryFolder()

    private val device = FakeAdbDevice()
    private val transport =
        BackupLocalTransport(BackupDeviceShell(device.session, FAKE_SERIAL), PACKAGE)

    @Test
    fun backupRunsOnTheLocalTransportThenSelectsTheOriginalOneAgain() = runBlocking {
        device.onShell { command ->
            when {
                command == "bmgr list transports" -> shellOutput(TRANSPORTS_WITH_GMS_SELECTED)
                command.contains("resolve-activity") -> shellOutput("No activity found\n")
                else -> null
            }
        }
        val archive = archive()

        transport.backup(archive)

        assertEquals(
            listOf(
                "bmgr enable true",
                "bmgr list transports",
                "bmgr transport '$LOCAL_TRANSPORT'",
                "bmgr backupnow @pm@",
                "bmgr backupnow $PACKAGE",
                "bmgr transport '$GMS_TRANSPORT'",
            ),
            device.commands.dropWhile { !it.startsWith("bmgr") },
        )
        ZipFile(archive).use { zip ->
            assertEquals("1", zip.getInputStream(zip.getEntry("token.txt")).reader().readText())
        }
    }

    /** Backup skips packages in the stopped state, so the package is woken up first. */
    @Test
    fun backupUnstopsThePackageFirst() = runBlocking {
        transport.backup(archive())

        assertTrue(
            device.commands.first().startsWith("am broadcast -a android.intent.action.MAIN"),
            "Actual commands: ${device.commands}",
        )
    }

    @Test
    fun backupKeepsTheLocalTransportWhenItWasAlreadySelected() = runBlocking {
        device.onShell { command ->
            if (command == "bmgr list transports") shellOutput(TRANSPORTS_WITH_LOCAL_SELECTED)
            else null
        }

        transport.backup(archive())

        assertEquals(
            listOf("bmgr transport '$LOCAL_TRANSPORT'"),
            device.commands.filter { it.startsWith("bmgr transport") },
        )
    }

    @Test
    fun backupAssumesTheGmsTransportWhenNoneIsMarkedSelected() = runBlocking {
        device.onShell { command ->
            if (command == "bmgr list transports") shellOutput("    $LOCAL_TRANSPORT\n") else null
        }

        transport.backup(archive())

        assertEquals("bmgr transport '$GMS_TRANSPORT'", device.commands.last())
    }

    @Test
    fun backupThrowsWhenTheBackupManagerCannotBeEnabled() {
        device.onShell { command ->
            if (command == "bmgr enable true") {
                shellOutput(stderr = "Error: Backup Manager is not activated for user 0\n")
            } else {
                null
            }
        }

        val e =
            assertFailsWith<BackupRestoreException> { runBlocking { transport.backup(archive()) } }

        assertEquals(
            "Failed to enable the backup manager with bmgr: " +
                "Error: Backup Manager is not activated for user 0",
            e.message,
        )
        assertEquals(BackupErrorCode.BMGR_INIT_FAILED, e.errorCode)
        assertTrue(device.commands.none { it.startsWith("bmgr transport") }, "${device.commands}")
    }

    @Test
    fun backupThrowsWhenTheLocalTransportCannotBeSelected() {
        device.onShell { command ->
            when (command) {
                "bmgr list transports" -> shellOutput(TRANSPORTS_WITH_GMS_SELECTED)
                "bmgr transport '$LOCAL_TRANSPORT'" ->
                    shellOutput(
                        "Unknown transport '$LOCAL_TRANSPORT' specified; no changes made.\n"
                    )
                else -> null
            }
        }

        val e =
            assertFailsWith<BackupRestoreException> { runBlocking { transport.backup(archive()) } }

        assertEquals(
            "Failed to select backup transport $LOCAL_TRANSPORT: " +
                "Unknown transport '$LOCAL_TRANSPORT' specified; no changes made.",
            e.message,
        )
        assertEquals(BackupErrorCode.BMGR_INIT_FAILED, e.errorCode)
        assertTrue(device.commands.none { it.startsWith("bmgr backupnow") }, "${device.commands}")
        assertEquals("bmgr transport '$GMS_TRANSPORT'", device.commands.last())
    }

    /** A backup pass reports success even when it skips the package, e.g. if it is not allowed. */
    @Test
    fun backupThrowsWhenThePackageIsNotBackedUp() {
        val output =
            "Running incremental backup for 1 requested packages.\n" +
                "Package @pm@ with result: Success\n" +
                "Package $PACKAGE with result: Backup is not allowed\n" +
                "Backup finished with result: Success\n"
        device.onShell { command ->
            if (command == "bmgr backupnow $PACKAGE") shellOutput(output) else null
        }
        val archive = archive()

        val e =
            assertFailsWith<BackupRestoreException> { runBlocking { transport.backup(archive) } }

        assertEquals("Local backup of $PACKAGE failed: ${output.trim()}", e.message)
        assertEquals(BackupErrorCode.BACKUP_FAILED, e.errorCode)
        assertFalse(archive.exists())
        assertEquals("bmgr transport '$GMS_TRANSPORT'", device.commands.last())
    }

    @Test
    fun backupThrowsWhenTheBackupPassFails() {
        device.onShell { command ->
            if (command == "bmgr backupnow $PACKAGE") {
                shellOutput(
                    "Running incremental backup for 1 requested packages.\n" +
                        "Package $PACKAGE with result: Success\n" +
                        "Backup finished with result: Backup cancelled\n"
                )
            } else {
                null
            }
        }

        val e = assertFailsWith<IOException> { runBlocking { transport.backup(archive()) } }

        assertTrue(e.message!!.startsWith("Local backup of $PACKAGE failed: "), e.message)
        assertTrue(e.message!!.contains("Backup finished with result: Backup cancelled"), e.message)
    }

    @Test
    fun backupSelectsTheOriginalTransportAgainWhenItFails() {
        device.onShell { command ->
            when (command) {
                "bmgr list transports" -> shellOutput(TRANSPORTS_WITH_GMS_SELECTED)
                "bmgr backupnow $PACKAGE" -> throw IOException("bmgr backupnow failed")
                else -> null
            }
        }

        assertFailsWith<IOException> { runBlocking { transport.backup(archive()) } }
        assertEquals("bmgr transport '$GMS_TRANSPORT'", device.commands.last())
    }

    /** A backup that leaves the local transport selected would change later backups. */
    @Test
    fun backupThrowsWhenTheOriginalTransportCannotBeSelectedAgain() {
        device.onShell { command ->
            when (command) {
                "bmgr list transports" -> shellOutput(TRANSPORTS_WITH_GMS_SELECTED)
                "bmgr transport '$GMS_TRANSPORT'" ->
                    shellOutput("Unknown transport '$GMS_TRANSPORT' specified; no changes made.\n")
                else -> null
            }
        }

        val e = assertFailsWith<IOException> { runBlocking { transport.backup(archive()) } }

        assertEquals(
            "Failed to select backup transport $GMS_TRANSPORT: " +
                "Unknown transport '$GMS_TRANSPORT' specified; no changes made.",
            e.message,
        )
    }

    @Test
    fun restoreSelectsTheOriginalTransportAgainWhenItFails() {
        device.onShell { command ->
            when (command) {
                "bmgr list transports" -> shellOutput(TRANSPORTS_WITH_GMS_SELECTED)
                "bmgr restore 1 $PACKAGE" -> throw IOException("bmgr restore failed")
                else -> null
            }
        }

        assertFailsWith<IOException> { runBlocking { transport.restore(1.minutes) } }
        assertEquals("bmgr transport '$GMS_TRANSPORT'", device.commands.last())
    }

    @Test
    fun restoreThrowsWhenTheLocalTransportHasNoBackup() {
        device.onShell { command ->
            when (command) {
                "bmgr list transports" -> shellOutput(TRANSPORTS_WITH_GMS_SELECTED)
                "bmgr restore 1 $PACKAGE" ->
                    shellOutput("No available restore sets; no restore performed\ndone\n")
                else -> null
            }
        }

        val e =
            assertFailsWith<BackupRestoreException> { runBlocking { transport.restore(1.minutes) } }

        assertEquals(
            "Local restore of $PACKAGE failed: " +
                "No available restore sets; no restore performed\ndone",
            e.message,
        )
        assertEquals(BackupErrorCode.RESTORE_FAILED, e.errorCode)
        assertTrue(device.commands.none { it == "bmgr run" }, "${device.commands}")
        assertEquals("bmgr transport '$GMS_TRANSPORT'", device.commands.last())
    }

    @Test
    fun restoreThrowsWhenTheRestoreFails() {
        device.onShell { command ->
            when (command) {
                "bmgr list transports" -> shellOutput(TRANSPORTS_WITH_GMS_SELECTED)
                "bmgr restore 1 $PACKAGE" ->
                    shellOutput(
                        "Scheduling restore: Local disk image\n" +
                            "restoreStarting: 1 packages\n" +
                            "restoreFinished: -1000\n" +
                            "done\n"
                    )
                else -> null
            }
        }

        val e = assertFailsWith<IOException> { runBlocking { transport.restore(1.minutes) } }

        assertTrue(e.message!!.startsWith("Local restore of $PACKAGE failed: "), e.message)
        assertTrue(e.message!!.contains("restoreFinished: -1000"), e.message)
    }

    /**
     * Selecting the original transport again is bounded, so a device that stops responding cannot
     * keep a timed-out caller waiting.
     */
    @Test
    fun restoreStopsWaitingForAnUnresponsiveTransportReselection() = runBlocking {
        device.onShell { command ->
            if (command == "bmgr list transports") shellOutput(TRANSPORTS_WITH_GMS_SELECTED)
            else null
        }
        device.hangOn { it == "bmgr run" || it == "bmgr transport '$GMS_TRANSPORT'" }
        val boundedTransport =
            BackupLocalTransport(
                BackupDeviceShell(device.session, FAKE_SERIAL),
                PACKAGE,
                reselectTransportTimeout = 200.milliseconds,
            )

        val elapsed = measureTime {
            assertNull(withTimeoutOrNull(300.milliseconds) { boundedTransport.restore(1.minutes) })
        }

        assertTrue(elapsed < 2.seconds, "Restore took $elapsed")
        assertEquals("bmgr transport '$GMS_TRANSPORT'", device.commands.last())
    }

    /**
     * A restore that ends before the first check is never seen. With a short timeout, restore stops
     * looking for it after half the timeout, so that it still completes within the timeout.
     */
    @Test
    fun restoreLooksForTheRestorePassForHalfOfAShortTimeout() = runBlocking {
        device.onShell { command ->
            if (command == "bmgr list transports") shellOutput(TRANSPORTS_WITH_GMS_SELECTED)
            else null
        }

        val elapsed = measureTime { transport.restore(1.seconds) }

        assertTrue(elapsed < 1.seconds, "Restore took $elapsed")
        assertEquals("bmgr transport '$GMS_TRANSPORT'", device.commands.last())
    }

    /**
     * Restore waits for the restore pass to be registered and then to end. An idle session reads
     * `Restore session: null`, which must not count as a restore in progress.
     */
    @Test
    fun restoreWaitsForTheRestorePassToEnd() = runBlocking {
        var dumpsysCount = 0
        device.onShell { command ->
            when (command) {
                "bmgr list transports" -> shellOutput(TRANSPORTS_WITH_GMS_SELECTED)
                "dumpsys backup" ->
                    when (++dumpsysCount) {
                        2 ->
                            shellOutput(
                                "Restore session: com.android.server.backup.RestoreSession@123\n" +
                                    "Restore in progress: true\n"
                            )
                        else -> shellOutput("Restore session: null\nRestore in progress: false\n")
                    }
                else -> null
            }
        }

        transport.restore(1.minutes)

        assertEquals(3, dumpsysCount)
        assertEquals(
            listOf(
                "bmgr list transports",
                "bmgr transport '$LOCAL_TRANSPORT'",
                "bmgr restore 1 $PACKAGE",
                "bmgr run",
                "bmgr transport '$GMS_TRANSPORT'",
            ),
            device.commands.filter { it != "dumpsys backup" },
        )
    }

    private fun archive() = tempFolder.root.resolve("backup_local_device.zip")

    private companion object {
        const val PACKAGE = "com.example.app"
        const val GMS_TRANSPORT = "com.google.android.gms/.backup.BackupTransportService"
        const val TRANSPORTS_WITH_GMS_SELECTED = "    $LOCAL_TRANSPORT\n  * $GMS_TRANSPORT\n"
        const val TRANSPORTS_WITH_LOCAL_SELECTED = "  * $LOCAL_TRANSPORT\n    $GMS_TRANSPORT\n"
    }
}
