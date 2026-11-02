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

import java.io.File
import java.nio.file.Files
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import org.junit.After
import org.junit.Test

class BackupReportCaptureTest {

    @After
    fun verifyNoLeakedJulHandlers() {
        assertEquals(0, HostLogManager.activeCollectorCountForTesting())
        assertFalse(HostLogManager.hasAttachedHandlersForTesting())
    }

    @Test
    fun testDetailRecordSerializesAllSchemaFieldsAndRoundTrips() {
        val original =
            TestDetailRecord(
                sourceLocation =
                    TestDetailRecord.SourceLocation(
                        file = "CrossDeviceMigrationTests.kt",
                        line = 47,
                        className = "com.example.CrossDeviceMigrationTests",
                        methodName = "testSessionTokenRestored",
                    ),
                summaryAttributes =
                    listOf(
                        TestDetailRecord.SummaryAttribute(
                            key = "transport",
                            label = "Transport",
                            value = "DEVICE_TO_DEVICE",
                            displayValue = "Device to device",
                            filterable = true,
                        )
                    ),
                callout =
                    TestDetailRecord.Callout(
                        kind = "ASSERTION_FAILURE",
                        failedStep = 5,
                        title = "Assertion failed at step 5 \u00b7 Verify",
                        message = "expected auth_token to be restored",
                        likelyCause =
                            "data_extraction_rules.xml excludes domain=\"sharedpref\" " +
                                "path=\"user_session.xml\" for <device-transfer>.",
                    ),
                actors =
                    listOf(
                        TestDetailRecord.ActorRecord(
                            id = "source",
                            role = "SOURCE",
                            name = "Pixel 6",
                            api = 31,
                            serial = SERIAL_SOURCE,
                            image = "Google APIs",
                            playServices = "26.39.33",
                        )
                    ),
                steps =
                    listOf(
                        TestDetailRecord.StepRecord(
                            n = null,
                            name = "Prepare",
                            on = "source",
                            shared = false,
                            call = "Install APKs, unlock device, clear app data",
                            durationMs = 820,
                            status = "PASSED",
                        ),
                        TestDetailRecord.StepRecord(
                            n = 1,
                            name = "Setup",
                            on = "source",
                            shared = true,
                            call = "runOnDevice(\"\u2026StageSession\")",
                            durationMs = 1420,
                            status = "PASSED",
                        ),
                    ),
                logs =
                    listOf(
                        TestDetailRecord.LogRecord(
                            title = "Host log",
                            path = "suite-details/test-1/host-log.txt",
                        ),
                        TestDetailRecord.LogRecord(
                            title = "Logcat (source)",
                            path = "suite-details/test-1/logcat-emulator-5554.txt",
                            truncated = true,
                        ),
                    ),
                artifacts =
                    listOf(
                        TestDetailRecord.ArtifactEntry(
                            name = "Backup file summary",
                            values = mapOf("app_backup" to "1.8 MB", "pm_backup" to "3.1 KB"),
                            path = null,
                        )
                    ),
            )

        val jsonText = original.toJsonString()
        val parsedObj = Json.parseToJsonElement(jsonText).jsonObject
        assertEquals(
            setOf(
                "schemaVersion",
                "suiteType",
                "sourceLocation",
                "summaryAttributes",
                "callout",
                "actors",
                "steps",
                "logs",
                "artifacts",
            ),
            parsedObj.keys,
        )
        assertEquals(original, TestDetailRecord.fromJson(jsonText))

        // Verify empty record emits explicit JSON arrays and nulls rather than omitting keys.
        val minimalObj = TestDetailRecord().toJsonObject()
        assertTrue(minimalObj["callout"] is JsonNull)
        assertNotNull(minimalObj["steps"])
        assertNotNull(minimalObj["actors"])
        assertNotNull(minimalObj["artifacts"])
    }

    @Test
    fun formatByteSizeFormatsHumanReadableUnits() {
        assertEquals("0 B", formatByteSize(0))
        assertEquals("412 B", formatByteSize(412))
        assertEquals("3.1 KB", formatByteSize(3174))
        assertEquals("1.8 MB", formatByteSize(1_887_437))
    }

    @Test
    fun closeUnregistersCollectorCaptureAndDeletesTempArchiveWhenHookThrows() {
        val rawSource = Files.createTempFile("source-archive-", ".zip")
        val tempDir = File(System.getProperty("java.io.tmpdir"))
        val tempBefore =
            tempDir
                .listFiles { _, name ->
                    name.startsWith("backup_raw_archive_") && name.endsWith("_hook_throw.zip")
                }
                .orEmpty()
                .toSet()

        val firstCapture =
            TestExecutionCapture(
                start = java.time.Instant.now(),
                isShared = false,
                testClassName = BackupReportCaptureTest::class.java.name,
                testMethodName = "throwingClose",
            )
        val secondCapture =
            TestExecutionCapture(
                start = java.time.Instant.now(),
                isShared = false,
                testClassName = BackupReportCaptureTest::class.java.name,
                testMethodName = "subsequentLeaf",
            )
        try {
            Files.write(rawSource, ByteArray(32) { 1 })
            ActiveCaptureContext.pushCurrentThread(firstCapture)
            firstCapture.recordArchive(
                fileName = "hook_throw.zip",
                summaryValues = mapOf("app_backup" to "32 B"),
                rawSourceFile = rawSource,
                includeRawCopy = true,
            )
            firstCapture.onUnfinalizedClose = { throw IllegalStateException("finalize failed") }

            val err = assertFailsWith<IllegalStateException> { firstCapture.close() }
            assertEquals("finalize failed", err.message)
            assertTrue(firstCapture.isClosed)
            assertNull(ActiveCaptureContext.currentThreadBound())
            assertEquals(secondCapture, ActiveCaptureContext.current())

            val tempAfter =
                tempDir
                    .listFiles { _, name ->
                        name.startsWith("backup_raw_archive_") && name.endsWith("_hook_throw.zip")
                    }
                    .orEmpty()
                    .toSet()
            assertEquals(tempBefore, tempAfter)
        } finally {
            runCatching { firstCapture.close() }
            secondCapture.close()
            Files.deleteIfExists(rawSource)
        }
        assertEquals(0, HostLogManager.activeCollectorCountForTesting())
        assertFalse(HostLogManager.hasAttachedHandlersForTesting())
    }

    @Test
    fun capsUtf8TextAtTailBoundaryWithoutSplittingMultiByteCharacters() {
        val shortText = "line1\nline2\n"
        val unchanged = capUtf8TailBounded(shortText, maxBytes = 64)
        assertFalse(unchanged.truncated)
        assertEquals(shortText, unchanged.bytes.decodeToString())

        val multiLine = "alpha-first-long-header-line\nbeta-line-two\ngamma-line-three\n"
        val snapped = capUtf8TailBounded(multiLine, maxBytes = 52)
        assertTrue(snapped.truncated)
        assertEquals(
            "... [truncated]\nbeta-line-two\ngamma-line-three\n",
            snapped.bytes.decodeToString(),
        )

        // Multi-byte UTF-8 characters (3-byte snowman \u2603) at a small maxBytes boundary.
        val multiByte = "\u2603\u2603\u2603"
        val mbCapped = capUtf8TailBounded(multiByte, maxBytes = 7)
        assertTrue(mbCapped.truncated)
        assertEquals("\u2603\u2603", mbCapped.bytes.decodeToString())

        val zeroCap = capUtf8TailBounded("non-empty", maxBytes = 0)
        assertTrue(zeroCap.truncated)
        assertEquals("", zeroCap.bytes.decodeToString())
    }

    @Test
    fun readsDeviceFactsAndCapturesHostLogs() {
        val device = FakeAdbDevice("emulator-5554")
        device.stubReadonlyProperties(
            mapOf(
                "ro.product.model" to "sdk_gphone64_arm64",
                "ro.boot.qemu.avd_name" to "Pixel_8_API_34",
            )
        )
        device.onShell { cmd ->
            when (cmd) {
                "pm list packages --show-versioncode com.google.android.gms" ->
                    shellOutput("package:com.google.android.gms versionCode:263933000\n")
                "pm path com.android.vending" -> shellOutput("")
                else -> shellOutput("")
            }
        }
        val shell = BackupDeviceShell(device.session, device.serialNumber)
        val facts = kotlinx.coroutines.runBlocking { DeviceFactsReader.read(shell, 34) }
        assertEquals("Pixel 8", facts.model)
        assertEquals(34, facts.api)
        assertEquals("Google APIs", facts.image)
        assertEquals("26.39.33", facts.playServices)

        val attrs =
            DeviceFactsReader.buildSummaryAttributes(
                BackupTransportMode.CLOUD_ENCRYPTED,
                listOf(
                    TestDetailRecord.ActorRecord(
                        id = "device",
                        role = "DEVICE",
                        name = facts.model,
                        api = facts.api,
                        serial = device.serialNumber,
                        image = facts.image,
                        playServices = facts.playServices,
                    )
                ),
            )
        assertEquals("CLOUD_ENCRYPTED", attrs.first { it.key == "transport" }.value)
        assertContains(attrs.first { it.key == "devices" }.displayValue, "Pixel 8 (API 34)")

        TestExecutionCapture(
                start = java.time.Instant.now(),
                isShared = false,
                testClassName = BackupReportCaptureTest::class.java.name,
                testMethodName = "readsDeviceFactsAndCapturesHostLogs",
            )
            .use { capture ->
                ActiveCaptureContext.pushCurrentThread(capture)
                try {
                    java.util.logging.Logger.getLogger("androidx.test.backup.host.Test")
                        .info("captured host log line")
                    assertContains(
                        capture.hostLogCollector.snapshotText(),
                        "captured host log line",
                    )
                } finally {
                    ActiveCaptureContext.unbindCurrentThread(capture)
                }
            }
    }

    private companion object {
        const val SERIAL_SOURCE = "emulator-5554"
    }

    @Test
    fun recordsStepsDerivesPhasesAndUnwrapsAsyncExceptions() {
        val capture =
            TestExecutionCapture(
                start = java.time.Instant.now(),
                isShared = false,
                testClassName = BackupReportCaptureTest::class.java.name,
                testMethodName = "recordsStepsDerivesPhasesAndUnwrapsAsyncExceptions",
            )
        capture.use {
            ActiveCaptureContext.pushCurrentThread(capture)
            try {
                assertEquals(capture, ActiveCaptureContext.current())
                assertEquals("Setup", capture.deriveStepPhase(isClearLike = false))
                capture.markBackupStarted("emulator-5554", BackupTransportMode.CLOUD_ENCRYPTED)
                assertEquals("Clear", capture.deriveStepPhase(isClearLike = true))
                assertEquals("Mutate", capture.deriveStepPhase(isClearLike = false))
                capture.markRestoreStarted("emulator-5556")
                assertEquals("Verify", capture.deriveStepPhase(isClearLike = false))
                capture.addStep(
                    isNumbered = true,
                    kind = TestExecutionCapture.StepKind.BACKUP,
                    name = "Backup",
                    controller = null,
                    call = "performBackup()",
                    durationMs = 42L,
                    status = TestDetailRecord.STATUS_PASSED,
                )
                assertTrue(capture.hasSeenBackup)
                assertTrue(capture.hasSeenRestore)
            } finally {
                ActiveCaptureContext.unbindCurrentThread(capture)
            }
        }
        val root = IllegalStateException("root cause")
        val wrapped = java.util.concurrent.ExecutionException(root)
        assertEquals(root, TestExecutionCapture.unwrapAsyncFailure(wrapped))
    }

    @Test
    fun buildsFinalizedReportAndOrdersCrossDeviceSourceBeforeTarget() {
        val sourceDev = FakeAdbDevice("emulator-5554")
        val targetDev = FakeAdbDevice("emulator-5556")
        val sourceCtrl =
            BackupRestoreControllerImpl(
                    sourceDev.session,
                    sourceDev.serialNumber,
                    34,
                    "com.example.app",
                )
                .also {
                    it.cachedDeviceFacts = DeviceFacts("Pixel 8", 34, "Google APIs", "26.39.33")
                }
        val targetCtrl =
            BackupRestoreControllerImpl(
                    targetDev.session,
                    targetDev.serialNumber,
                    34,
                    "com.example.app",
                )
                .also {
                    it.cachedDeviceFacts = DeviceFacts("Pixel 8a", 34, "Google Play", "26.39.33")
                }
        val capture =
            TestExecutionCapture(
                start = java.time.Instant.now(),
                isShared = false,
                testClassName = BackupReportCaptureTest::class.java.name,
                testMethodName = "buildsFinalizedReportAndOrdersCrossDeviceSourceBeforeTarget",
            )
        capture.use {
            // Register target first, then source, and verify backup/restore endpoints order source
            // first.
            capture.addParticipant(targetCtrl)
            capture.addParticipant(sourceCtrl)
            capture.recordFlowEndpoints(
                mode = BackupTransportMode.DEVICE_TO_DEVICE,
                backupSerial = sourceCtrl.serialNumber,
                restoreSerial = targetCtrl.serialNumber,
            )
            capture.addStep(
                isNumbered = true,
                kind = TestExecutionCapture.StepKind.BACKUP,
                name = "Backup",
                controller = sourceCtrl,
                call = "performBackup(DEVICE_TO_DEVICE)",
                durationMs = 120L,
                status = TestDetailRecord.STATUS_PASSED,
            )
            val report =
                capture.buildFinalizedReport(
                    detailId = null,
                    detailDir = null,
                    savedLogcatFiles = emptyMap(),
                    failure = null,
                    resultsDirPath = null,
                )
            assertEquals(listOf("source", "target"), report.record.actors.map { it.id })
            assertEquals("emulator-5554", report.record.actors[0].serial)
            assertEquals("emulator-5556", report.record.actors[1].serial)
            assertEquals(1, report.record.steps.size)
            assertEquals("source", report.record.steps[0].on)
        }
    }

    @Test
    fun formatsControllerCallLabelsAndDetectsArchiveTransportMode() {
        assertEquals(
            "runOnDevice(\"\u2026SeedNotesAction\")",
            BackupRestoreControllerImpl.formatRunOnDeviceCall("com.example.app.SeedNotesAction"),
        )
        assertEquals(
            "PopulateStorageAction Preference(\"user_prefs\")",
            BackupRestoreControllerImpl.formatFlowDomainCall(
                "PopulateStorageAction",
                listOf(StorageDomain.Preference("user_prefs", "k", "v")),
            ),
        )
        val tmpZip = java.nio.file.Files.createTempFile("test_backup_", ".backup")
        try {
            java.util.zip.ZipOutputStream(java.nio.file.Files.newOutputStream(tmpZip)).use { zos ->
                zos.putNextEntry(java.util.zip.ZipEntry("metadata.txt"))
                zos.write("application-id=com.example.app\nbackup-type=CLOUD\n".toByteArray())
                zos.closeEntry()
                zos.putNextEntry(java.util.zip.ZipEntry("app_backup"))
                zos.write(ByteArray(128))
                zos.closeEntry()
            }
            assertEquals(
                BackupTransportMode.CLOUD_ENCRYPTED,
                BackupRestoreControllerImpl.detectTransportModeFromArchive(tmpZip),
            )
            val entries = BackupRestoreControllerImpl.inspectBackupArchive(tmpZip.toFile())
            assertEquals("128 B", entries["app_backup"])
        } finally {
            java.nio.file.Files.deleteIfExists(tmpZip)
        }
    }
}

/** Parses a [TestDetailRecord] from JSON text in tests. */
internal fun TestDetailRecord.Companion.fromJson(jsonText: String): TestDetailRecord {
    val root = Json.parseToJsonElement(jsonText).jsonObject
    return TestDetailRecord(
        schemaVersion = root["schemaVersion"].asIntOrNull() ?: 1,
        suiteType = root["suiteType"].asStringOrNull() ?: "androidx.test.backup",
        sourceLocation =
            root["sourceLocation"].asObjectOrNull()?.let { loc ->
                TestDetailRecord.SourceLocation(
                    file = loc["file"].asStringOrNull(),
                    line = loc["line"].asIntOrNull(),
                    className = loc["className"].asString(),
                    methodName = loc["methodName"].asStringOrNull(),
                )
            },
        summaryAttributes =
            root["summaryAttributes"]?.jsonArray?.map { el ->
                val obj = el.jsonObject
                TestDetailRecord.SummaryAttribute(
                    key = obj["key"].asString(),
                    label = obj["label"].asString(),
                    value = obj["value"].asString(),
                    displayValue = obj["displayValue"].asString(),
                    filterable = obj["filterable"].asBooleanOrNull() ?: false,
                )
            } ?: emptyList(),
        callout =
            root["callout"].asObjectOrNull()?.let { co ->
                TestDetailRecord.Callout(
                    kind = co["kind"].asString(),
                    failedStep = co["failedStep"].asIntOrNull(),
                    title = co["title"].asString(),
                    message = co["message"].asString(),
                    likelyCause = co["likelyCause"].asStringOrNull(),
                )
            },
        actors =
            root["actors"]?.jsonArray?.map { el ->
                val obj = el.jsonObject
                TestDetailRecord.ActorRecord(
                    id = obj["id"].asString(),
                    role = obj["role"].asString(),
                    name = obj["name"].asString(),
                    api = obj["api"].asIntOrNull() ?: 0,
                    serial = obj["serial"].asString(),
                    image = obj["image"].asString(),
                    playServices = obj["playServices"].asStringOrNull(),
                )
            } ?: emptyList(),
        steps =
            root["steps"]?.jsonArray?.map { el ->
                val obj = el.jsonObject
                TestDetailRecord.StepRecord(
                    n = obj["n"].asIntOrNull(),
                    name = obj["name"].asString(),
                    on = obj["on"].asStringOrNull(),
                    shared = obj["shared"].asBooleanOrNull() ?: false,
                    call = obj["call"].asString(),
                    durationMs = obj["durationMs"].asLongOrNull() ?: 0L,
                    status = obj["status"].asString(),
                )
            } ?: emptyList(),
        logs =
            root["logs"]?.jsonArray?.map { el ->
                val obj = el.jsonObject
                TestDetailRecord.LogRecord(
                    title = obj["title"].asString(),
                    path = obj["path"].asString(),
                    truncated = obj["truncated"].asBooleanOrNull() ?: false,
                )
            } ?: emptyList(),
        artifacts =
            root["artifacts"]?.jsonArray?.map { el ->
                val obj = el.jsonObject
                val valuesMap =
                    obj["values"].asObjectOrNull()?.mapValues { (_, v) -> v.asString() }
                        ?: emptyMap()
                TestDetailRecord.ArtifactEntry(
                    name = obj["name"].asString(),
                    values = valuesMap,
                    path = obj["path"].asStringOrNull(),
                )
            } ?: emptyList(),
    )
}

private fun JsonElement?.asObjectOrNull(): JsonObject? =
    if (this == null || this is JsonNull) null else jsonObject

private fun JsonElement?.asStringOrNull(): String? = (this as? JsonPrimitive)?.contentOrNull

private fun JsonElement?.asString(): String = asStringOrNull().orEmpty()

private fun JsonElement?.asIntOrNull(): Int? = (this as? JsonPrimitive)?.intOrNull

private fun JsonElement?.asLongOrNull(): Long? = (this as? JsonPrimitive)?.longOrNull

private fun JsonElement?.asBooleanOrNull(): Boolean? = (this as? JsonPrimitive)?.booleanOrNull
