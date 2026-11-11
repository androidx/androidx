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
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
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
            when (command) {
                "dumpsys package $PACKAGE" -> shellOutput(packageDump(PACKAGE, stopped = false))
                "bmgr list transports" -> shellOutput(TRANSPORTS_WITH_GMS_SELECTED)
                else -> null
            }
        }
        val archive = archive()

        transport.backup(archive)

        assertEquals(
            listOf(
                "dumpsys package $PACKAGE",
                "bmgr enable true",
                "bmgr list transports",
                "bmgr transport '$LOCAL_TRANSPORT'",
                "bmgr backupnow @pm@",
                "bmgr backupnow $PACKAGE",
                "bmgr transport '$GMS_TRANSPORT'",
            ),
            device.commands,
        )
        assertEquals(
            BackupLocalTransport.LocalBackup(PACKAGE, FAKE_SERIAL),
            BackupLocalTransport.localBackupOf(archive.toPath()),
        )
    }

    /** The marker is a zip file with one properties entry that names the app and the device. */
    @Test
    fun backupWritesAMarkerThatNamesTheAppAndTheDevice() = runBlocking {
        device.onShell { command ->
            if (command.contains("resolve-activity")) shellOutput("No activity found\n") else null
        }
        val archive = archive()

        transport.backup(archive)

        ZipFile(archive).use { zip ->
            assertEquals(
                listOf("androidx.test.backup.local.properties"),
                zip.entries().toList().map { it.name },
            )
            val properties = Properties()
            zip.getInputStream(zip.entries().nextElement()).use { properties.load(it) }
            assertEquals(PACKAGE, properties.getProperty("package"))
            assertEquals(FAKE_SERIAL, properties.getProperty("serial"))
        }
    }

    /** Earlier versions marked a LOCAL backup with a zip file holding only `token.txt`. */
    @Test
    fun localBackupOfReadsTheMarkerOfEarlierVersions() {
        val archive = zip("token.txt" to "1")

        assertEquals(
            BackupLocalTransport.LocalBackup(packageName = null, serialNumber = null),
            BackupLocalTransport.localBackupOf(archive),
        )
    }

    @Test
    fun localBackupOfRejectsOtherArchives() {
        val notAZip = tempFolder.newFile("backup_local_device.zip").toPath()
        val missing = tempFolder.root.toPath().resolve("missing.zip")
        val otherZip = zip("token.txt" to "1", "metadata.json" to "{}")

        assertNull(BackupLocalTransport.localBackupOf(notAZip))
        assertNull(BackupLocalTransport.localBackupOf(missing))
        assertNull(BackupLocalTransport.localBackupOf(otherZip))
    }

    @Test
    fun localBackupOfThrowsAnIOExceptionForAMalformedMarker() {
        val archive = zip("androidx.test.backup.local.properties" to "package=\\u00zz\n")

        val e = assertFailsWith<IOException> { BackupLocalTransport.localBackupOf(archive) }

        assertEquals("$archive has a malformed LOCAL backup marker", e.message)
        assertIs<IllegalArgumentException>(e.cause)
    }

    /** This version always writes both, so a marker without one of them has been altered. */
    @Test
    fun localBackupOfThrowsAnIOExceptionForAMarkerWithoutTheAppOrTheDevice() {
        for (content in
            listOf("serial=$FAKE_SERIAL\n", "package=$PACKAGE\n", "package=$PACKAGE\nserial= \n")) {
            val archive = zip("androidx.test.backup.local.properties" to content)

            val e = assertFailsWith<IOException> { BackupLocalTransport.localBackupOf(archive) }

            assertEquals("$archive has a malformed LOCAL backup marker", e.message, content)
        }
    }

    @Test
    fun restoreRejectsTheLocalBackupOfAnotherApp() {
        val e =
            assertFailsWith<IOException> {
                runBlocking {
                    transport.restore(
                        archive().toPath(),
                        BackupLocalTransport.LocalBackup("com.example.other", FAKE_SERIAL),
                    )
                }
            }

        assertEquals(
            "backup_local_device.zip is a LOCAL backup of com.example.other, not of $PACKAGE",
            e.message,
        )
        assertTrue(device.commands.isEmpty(), "${device.commands}")
    }

    /** The data of a LOCAL backup stays on the device that made it. */
    @Test
    fun restoreRejectsALocalBackupMadeOnAnotherDevice() {
        val e =
            assertFailsWith<IOException> {
                runBlocking {
                    transport.restore(
                        archive().toPath(),
                        BackupLocalTransport.LocalBackup(PACKAGE, "emulator-5556"),
                    )
                }
            }

        assertEquals(
            "backup_local_device.zip is a LOCAL backup made on emulator-5556. Its data stays in " +
                "the local transport of that device, so $FAKE_SERIAL can't restore it.",
            e.message,
        )
        assertTrue(device.commands.isEmpty(), "${device.commands}")
    }

    /** Backup skips packages in the stopped state, so the package is taken out of it first. */
    @Test
    fun backupUnstopsThePackageFirst() = runBlocking {
        device.onShell { command ->
            when (command) {
                "dumpsys package $PACKAGE" -> shellOutput(packageDump(PACKAGE, stopped = true))
                "pm unstop $PACKAGE" -> shellOutput("Package $PACKAGE new stopped state: false\n")
                else -> null
            }
        }

        transport.backup(archive())

        assertEquals(
            listOf("dumpsys package $PACKAGE", "pm unstop $PACKAGE", "bmgr enable true"),
            device.commands.take(3),
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

        assertFailsWith<IOException> { runBlocking { transport.restore() } }
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

        val e = assertFailsWith<BackupRestoreException> { runBlocking { transport.restore() } }

        assertEquals(
            "Local restore of $PACKAGE failed: " +
                "No available restore sets; no restore performed\ndone",
            e.message,
        )
        assertEquals(BackupErrorCode.RESTORE_FAILED, e.errorCode)
        assertEquals(
            listOf("bmgr restore 1 $PACKAGE", "bmgr transport '$GMS_TRANSPORT'"),
            device.commands.takeLast(2),
        )
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

        val e = assertFailsWith<IOException> { runBlocking { transport.restore() } }

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
        device.hangOn {
            it == "bmgr restore 1 $PACKAGE" || it == "bmgr transport '$GMS_TRANSPORT'"
        }
        val boundedTransport =
            BackupLocalTransport(
                BackupDeviceShell(device.session, FAKE_SERIAL),
                PACKAGE,
                reselectTransportTimeout = 200.milliseconds,
            )

        val elapsed = measureTime {
            assertNull(withTimeoutOrNull(300.milliseconds) { boundedTransport.restore() })
        }

        assertTrue(elapsed < 2.seconds, "Restore took $elapsed")
        assertEquals("bmgr transport '$GMS_TRANSPORT'", device.commands.last())
    }

    /** `bmgr restore` waits for the restore to finish, so nothing runs after it but cleanup. */
    @Test
    fun restoreRunsOnTheLocalTransportThenSelectsTheOriginalOneAgain() = runBlocking {
        device.onShell { command ->
            if (command == "bmgr list transports") shellOutput(TRANSPORTS_WITH_GMS_SELECTED)
            else null
        }

        transport.restore()

        assertEquals(
            listOf(
                "bmgr list transports",
                "bmgr transport '$LOCAL_TRANSPORT'",
                "bmgr restore 1 $PACKAGE",
                "bmgr transport '$GMS_TRANSPORT'",
            ),
            device.commands,
        )
    }

    private fun archive() = tempFolder.root.resolve("backup_local_device.zip")

    /** Returns a new zip file with the text [entries]. */
    private fun zip(vararg entries: Pair<String, String>): Path {
        val file = tempFolder.newFile().toPath()
        ZipOutputStream(Files.newOutputStream(file)).use { zip ->
            for ((name, text) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(text.toByteArray())
                zip.closeEntry()
            }
        }
        return file
    }

    /** Restores the LOCAL backup of [PACKAGE] made on this test's device. */
    private suspend fun BackupLocalTransport.restore() =
        restore(archive().toPath(), BackupLocalTransport.LocalBackup(PACKAGE, FAKE_SERIAL))

    private companion object {
        const val PACKAGE = "com.example.app"
        const val GMS_TRANSPORT = "com.google.android.gms/.backup.BackupTransportService"
        const val TRANSPORTS_WITH_GMS_SELECTED = "    $LOCAL_TRANSPORT\n  * $GMS_TRANSPORT\n"
        const val TRANSPORTS_WITH_LOCAL_SELECTED = "  * $LOCAL_TRANSPORT\n    $GMS_TRANSPORT\n"
    }
}
