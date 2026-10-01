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
                else -> shellOutput()
            }
        }
        val archive = tempFolder.root.resolve("backup_local_device.zip")

        transport.backup(archive)

        assertEquals(
            listOf(
                "bmgr enable true",
                "bmgr list transports",
                "bmgr transport $LOCAL_TRANSPORT",
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
        transport.backup(tempFolder.root.resolve("backup_local_device.zip"))

        assertTrue(
            device.commands.first().startsWith("am broadcast -a android.intent.action.MAIN"),
            "Actual commands: ${device.commands}",
        )
    }

    @Test
    fun backupKeepsTheLocalTransportWhenItWasAlreadySelected() = runBlocking {
        device.onShell { command ->
            if (command == "bmgr list transports") shellOutput(TRANSPORTS_WITH_LOCAL_SELECTED)
            else shellOutput()
        }

        transport.backup(tempFolder.root.resolve("backup_local_device.zip"))

        assertEquals(
            listOf("bmgr transport $LOCAL_TRANSPORT"),
            device.commands.filter { it.startsWith("bmgr transport") },
        )
    }

    @Test
    fun backupAssumesTheGmsTransportWhenNoneIsMarkedSelected() = runBlocking {
        device.onShell { command ->
            if (command == "bmgr list transports") shellOutput("    $LOCAL_TRANSPORT\n")
            else shellOutput()
        }

        transport.backup(tempFolder.root.resolve("backup_local_device.zip"))

        assertEquals("bmgr transport '$GMS_TRANSPORT'", device.commands.last())
    }

    @Test
    fun backupSelectsTheOriginalTransportAgainWhenItFails() {
        device.onShell { command ->
            when (command) {
                "bmgr list transports" -> shellOutput(TRANSPORTS_WITH_GMS_SELECTED)
                "bmgr backupnow $PACKAGE" -> throw IOException("bmgr backupnow failed")
                else -> shellOutput()
            }
        }

        assertFailsWith<IOException> {
            runBlocking { transport.backup(tempFolder.root.resolve("backup_local_device.zip")) }
        }
        assertEquals("bmgr transport '$GMS_TRANSPORT'", device.commands.last())
    }

    @Test
    fun restoreSelectsTheOriginalTransportAgainWhenItFails() {
        device.onShell { command ->
            when (command) {
                "bmgr list transports" -> shellOutput(TRANSPORTS_WITH_GMS_SELECTED)
                "bmgr restore 1 $PACKAGE" -> throw IOException("bmgr restore failed")
                else -> shellOutput()
            }
        }

        assertFailsWith<IOException> { runBlocking { transport.restore(1.minutes) } }
        assertEquals("bmgr transport '$GMS_TRANSPORT'", device.commands.last())
    }

    /**
     * Selecting the original transport again is bounded, so a device that stops responding cannot
     * keep a timed-out caller waiting.
     */
    @Test
    fun restoreStopsWaitingForAnUnresponsiveTransportReselection() = runBlocking {
        device.onShell { command ->
            if (command == "bmgr list transports") shellOutput(TRANSPORTS_WITH_GMS_SELECTED)
            else shellOutput()
        }
        device.hangOn { it == "bmgr run" || it.startsWith("bmgr transport '") }
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
            else shellOutput()
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
                else -> shellOutput()
            }
        }

        transport.restore(1.minutes)

        assertEquals(3, dumpsysCount)
        assertEquals(
            listOf(
                "bmgr list transports",
                "bmgr transport $LOCAL_TRANSPORT",
                "bmgr restore 1 $PACKAGE",
                "bmgr run",
                "bmgr transport '$GMS_TRANSPORT'",
            ),
            device.commands.filter { it != "dumpsys backup" },
        )
    }

    private companion object {
        const val PACKAGE = "com.example.app"
        const val LOCAL_TRANSPORT = "com.android.localtransport/.LocalTransport"
        const val GMS_TRANSPORT = "com.google.android.gms/.backup.BackupTransportService"
        const val TRANSPORTS_WITH_GMS_SELECTED = "    $LOCAL_TRANSPORT\n  * $GMS_TRANSPORT\n"
        const val TRANSPORTS_WITH_LOCAL_SELECTED = "  * $LOCAL_TRANSPORT\n    $GMS_TRANSPORT\n"
    }
}
