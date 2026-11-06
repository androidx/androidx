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
import com.android.backup.BackupProgressListener
import com.android.backup.BackupResult
import com.android.backup.BackupService
import com.android.backup.ErrorCode
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.RETURNS_DEFAULTS
import org.mockito.Mockito.anyString
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class BackupReportCaptureTest {

    @get:Rule val tempFolder = TemporaryFolder()

    private val rootStore = FakeStore()
    private val stores = mutableListOf(rootStore)
    private val rootContext = contextWithRootStore(rootStore)
    private val sourceDevice = FakeAdbDevice(SERIAL_SOURCE)
    private val targetDevice = FakeAdbDevice(SERIAL_TARGET)

    @After
    fun verifyNoLeakedJulHandlers() {
        stores.asReversed().forEach { it.closeAll() }
        rootStore.storedValues.filterIsInstance<Path>().forEach {
            it.toFile().deleteRecursively()
        }
        ActiveCaptureContext.clearCurrentThread()
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
    fun passingLocalFlowWritesDetailSidecarHostLogAndDeviceStoredSummaryWithoutXmlFiles() =
        withDeviceProperties {
            val resultsDir = tempFolder.newFolder("results-local")
            System.setProperty(RESULTS_DIR, resultsDir.absolutePath)

            stubHealthyDevice(
                sourceDevice,
                avdName = "Pixel_6_API_31",
                gmsVersionName = "26.39.33",
                hasPlayStore = false,
            )

            val published = LinkedHashMap<String, String>()
            val extension = BackupRestoreExtension { sourceDevice.session }
            val classCtx = childContext(rootContext, CLASS_ID)
            val testCtx =
                childContext(
                    classCtx,
                    TEST_ID,
                    reportSink = published,
                    testClass = BackupReportCaptureTest::class.java,
                )

            extension.beforeAll(classCtx)
            extension.beforeEach(testCtx)

            val controller =
                newController(
                    sourceDevice,
                    apiLevel = 31,
                    telemetrySink = published,
                    facts =
                        extension.cachedDeviceFactsFor(testCtx, newController(sourceDevice, 31)),
                )
            extension.track(testCtx, controller)
            val apk = tempFolder.newFile("app-debug.apk")
            extension.prepareResolvedController(
                testCtx,
                controller = controller,
                apks = listOf(apk),
            )

            runBlocking {
                controller.runBackupRestoreFlow(
                    StorageDomain.Preference("prefs", "theme", "dark"),
                    tempFolder.newFolder("flow-out").toPath(),
                    BackupTransportMode.LOCAL,
                )
            }

            extension.afterEach(testCtx)
            extension.afterAll(classCtx)

            val detailId = extension.detailIdOf(TEST_ID)
            assertEquals("suite-details/$detailId.json", published[BackupReportKeys.DETAIL_PATH])

            val jsonFile = File(resultsDir, "suite-details/$detailId.json")
            assertTrue(jsonFile.isFile)
            val record = TestDetailRecord.fromJson(jsonFile.readText())

            assertEquals(1, record.schemaVersion)
            assertEquals("androidx.test.backup", record.suiteType)
            assertEquals("BackupReportCaptureTest.kt", record.sourceLocation?.file)
            assertTrue((record.sourceLocation?.line ?: 0) > 0)

            val actor = record.actors.single()
            assertEquals("device", actor.id)
            assertEquals("DEVICE", actor.role)
            assertEquals("Pixel 6", actor.name)
            assertEquals(31, actor.api)
            assertEquals("Google APIs", actor.image)
            assertEquals("26.39.33", actor.playServices)

            val attrs = record.summaryAttributes.associateBy { it.key }
            assertEquals("LOCAL", attrs["transport"]?.value)
            assertEquals("Local", attrs["transport"]?.displayValue)
            assertEquals("Pixel 6 (API 31)", attrs["devices"]?.displayValue)

            assertEquals(
                listOf(null, 1, 2, 3, 4, 5),
                record.steps.map { it.n },
            )
            assertEquals(
                listOf("Prepare", "Setup", "Backup", "Clear", "Restore", "Verify"),
                record.steps.map { it.name },
            )
            assertEquals("Install APKs, unlock device, clear app data", record.steps.first().call)
            assertTrue(record.steps.all { it.status == "PASSED" })
            assertNull(record.callout)

            val summaryArtifact = record.artifacts.single()
            assertEquals("Backup file summary", summaryArtifact.name)
            assertEquals(mapOf("storage" to "Stored on the device"), summaryArtifact.values)
            assertNull(summaryArtifact.path)

            // Passing tests do not capture logcat unless captureLogcatOnPass=true.
            assertEquals(listOf("Host log"), record.logs.map { it.title })
            val hostLogFile = File(resultsDir, "suite-details/$detailId/host-log.txt")
            assertTrue(hostLogFile.isFile)
            assertContains(hostLogFile.readText(), "Executing standard backup and restore flow")

            // Never write .xml files into RESULTS_DIR.
            val xmlFiles =
                resultsDir
                    .walkTopDown()
                    .filter { it.isFile && it.extension.equals("xml", true) }
                    .toList()
            assertEquals(emptyList(), xmlFiles)
        }

    @Test
    fun failingFlowEmitsSkippedStagesAfterFailureWithZeroDuration() = withDeviceProperties {
        val resultsDir = tempFolder.newFolder("results-skipped")
        System.setProperty(RESULTS_DIR, resultsDir.absolutePath)

        stubHealthyDevice(sourceDevice, avdName = "Pixel_6_API_31", gmsVersionName = "26.39.33")
        stubHealthyDevice(targetDevice, avdName = "Pixel_8_API_34", gmsVersionName = "26.40.12")

        val failingService =
            fakeBackupService(
                backupResult =
                    BackupResult.Error(
                        ErrorCode.TRANSPORT_INIT_FAILED,
                        IOException("GMS transport failed to initialize"),
                    )
            )

        val extension = BackupRestoreExtension { sourceDevice.session }
        val classCtx = childContext(rootContext, CLASS_ID)
        val failure = run {
            val published = LinkedHashMap<String, String>()
            val source =
                newController(
                    sourceDevice,
                    apiLevel = 31,
                    backupService = failingService,
                    telemetrySink = published,
                    facts = DeviceFacts("Pixel 6", 31, "Google APIs", "26.39.33"),
                )
            val target =
                newController(
                    targetDevice,
                    apiLevel = 34,
                    backupService = failingService,
                    telemetrySink = published,
                    facts = DeviceFacts("Pixel 8", 34, "Google Play", "26.40.12"),
                )

            var thrown: Throwable? = null
            val testCtx =
                childContext(
                    classCtx,
                    TEST_ID,
                    failureProvider = { thrown },
                    reportSink = published,
                )
            extension.beforeAll(classCtx)
            extension.beforeEach(testCtx)
            extension.track(testCtx, source)
            extension.track(testCtx, target)

            thrown =
                assertFailsWith<BackupRestoreException> {
                    runBlocking {
                        source.runBackupRestoreFlow(
                            StorageDomain.Preference("session", "token", "abc"),
                            tempFolder.newFolder("fail-out").toPath(),
                            BackupTransportMode.DEVICE_TO_DEVICE,
                            restoreOn = target,
                        )
                    }
                }

            extension.afterEach(testCtx)
            extension.afterAll(classCtx)
            thrown!!
        }

        val detailId = extension.detailIdOf(TEST_ID)
        val record =
            TestDetailRecord.fromJson(File(resultsDir, "suite-details/$detailId.json").readText())

        assertEquals(listOf("source", "target"), record.actors.map { it.id })
        assertEquals(listOf("SOURCE", "TARGET"), record.actors.map { it.role })
        val attrs = record.summaryAttributes.associateBy { it.key }
        assertEquals("Device to device", attrs["transport"]?.displayValue)
        assertEquals("Pixel 6 (API 31) -> Pixel 8 (API 34)", attrs["devices"]?.displayValue)

        assertEquals(
            listOf(
                Triple(1, "Setup", "PASSED"),
                Triple(2, "Backup", "FAILED"),
                Triple(3, "Clear", "SKIPPED"),
                Triple(4, "Restore", "SKIPPED"),
                Triple(5, "Verify", "SKIPPED"),
            ),
            record.steps.map { Triple(it.n, it.name, it.status) },
        )
        assertEquals(
            listOf("source", "source", "target", "target", "target"),
            record.steps.map { it.on },
        )
        assertTrue(record.steps.drop(2).all { it.durationMs == 0L })

        val callout = assertNotNull(record.callout)
        assertEquals("STEP_FAILURE", callout.kind)
        assertEquals(2, callout.failedStep)
        assertEquals("Step failed at step 2 \u00b7 Backup", callout.title)
        assertTrue(failure.suppressed.any { "Failed at step 2 (Backup)" in it.toString() })
    }

    @Test
    fun stepByStepCrossDeviceTestRecordsPhasesSharedStepsBackupRulesProgressAndTelemetry() =
        withDeviceProperties {
            val resultsDir = tempFolder.newFolder("results-step-by-step")
            writeSuiteEnvironmentJson(
                resultsDir,
                allowBackup = true,
                dataExtractionRulesRef = "@xml/data_extraction_rules",
                dataExtractionRulesXml =
                    """
                    <data-extraction-rules>
                        <device-transfer>
                            <exclude domain="sharedpref" path="user_session.xml" />
                        </device-transfer>
                    </data-extraction-rules>
                    """
                        .trimIndent(),
            )
            System.setProperty(RESULTS_DIR, resultsDir.absolutePath)

            stubHealthyDevice(sourceDevice, avdName = "Pixel_6_API_31", gmsVersionName = "26.39.33")
            stubHealthyDevice(targetDevice, avdName = "Pixel_8_API_34", gmsVersionName = "26.39.33")

            val zipService =
                fakeZipBackupService(
                    entries =
                        mapOf(
                            "app_backup" to ByteArray(2048) { 1 },
                            "pm_backup" to ByteArray(412) { 2 },
                        ),
                    emitProgress = true,
                )

            val extension = BackupRestoreExtension { sourceDevice.session }
            val classCtx = childContext(rootContext, CLASS_ID)

            // Shared @BeforeAll setup on source controller.
            extension.beforeAll(classCtx)
            val sharedSource =
                newController(
                    sourceDevice,
                    apiLevel = 31,
                    role = "source",
                    backupService = zipService,
                    facts = DeviceFacts("Pixel 6", 31, "Google APIs", "26.39.33"),
                )
            extension.track(classCtx, sharedSource)
            runBlocking { sharedSource.runOnDevice("com.example.journaling.PrepareAccount") }

            // Test 1: fails on post-restore JUnit assertion after step-by-step calls.
            val published1 = LinkedHashMap<String, String>()
            var failure1: Throwable? = null
            val test1Id = "$CLASS_ID/[method:testSessionTokenRestored()]"
            val test1Ctx =
                childContext(
                    classCtx,
                    test1Id,
                    failureProvider = { failure1 },
                    reportSink = published1,
                )
            extension.beforeEach(test1Ctx)
            val target =
                newController(
                    targetDevice,
                    apiLevel = 34,
                    role = "target",
                    backupService = zipService,
                    facts = DeviceFacts("Pixel 8", 34, "Google APIs", "26.39.33"),
                )
            extension.track(test1Ctx, target)

            runBlocking {
                sharedSource.runOnDevice("com.example.journaling.StageSession")
                val archive =
                    sharedSource.performBackup(
                        BackupTransportMode.DEVICE_TO_DEVICE,
                        tempFolder.newFolder("bkp-1").toPath(),
                    )
                target.clearAppData()
                target.stopApp()
                target.performRestore(archive)
            }
            failure1 = AssertionError("expected user_session auth_token to be restored on target")
            extension.afterEach(test1Ctx)

            // Test 2: passes in same class, still receives shared @BeforeAll step without Test 1's
            // steps.
            val published2 = LinkedHashMap<String, String>()
            val test2Id = "$CLASS_ID/[method:testDraftsRestored()]"
            val test2Ctx = childContext(classCtx, test2Id, reportSink = published2)
            extension.beforeEach(test2Ctx)
            runBlocking {
                val archive2 =
                    sharedSource.performBackup(
                        BackupTransportMode.CLOUD_ENCRYPTED,
                        tempFolder.newFolder("bkp-2").toPath(),
                    )
                sharedSource.performRestore(archive2)
                sharedSource.runOnDevice("com.example.journaling.VerifyDrafts")
            }
            extension.afterEach(test2Ctx)
            extension.afterAll(classCtx)

            // Verify Test 1 record.
            val detail1Id = extension.detailIdOf(test1Id)
            val record1 =
                TestDetailRecord.fromJson(
                    File(resultsDir, "suite-details/$detail1Id.json").readText()
                )
            assertEquals(
                listOf(
                    Triple(1, "Setup", true),
                    Triple(2, "Setup", false),
                    Triple(3, "Backup", false),
                    Triple(4, "Clear", false),
                    Triple(5, "Mutate", false),
                    Triple(6, "Restore", false),
                    Triple(7, "Verify", false),
                ),
                record1.steps.map { Triple(it.n, it.name, it.shared) },
            )
            assertEquals("runOnDevice(\"\u2026PrepareAccount\")", record1.steps[0].call)
            assertEquals("runOnDevice(\"\u2026StageSession\")", record1.steps[1].call)
            assertEquals("performBackup(DEVICE_TO_DEVICE)", record1.steps[2].call)
            assertEquals("clearAppData()", record1.steps[3].call)
            assertEquals("stopApp()", record1.steps[4].call)
            assertEquals("performRestore(backupFile)", record1.steps[5].call)
            assertEquals("FAILED", record1.steps[6].status)
            assertEquals("Assertion in testSessionTokenRestored()", record1.steps[6].call)

            val callout1 = assertNotNull(record1.callout)
            assertEquals("ASSERTION_FAILURE", callout1.kind)
            assertEquals(7, callout1.failedStep)
            assertEquals("Assertion failed at step 7 \u00b7 Verify", callout1.title)
            assertEquals(
                "data_extraction_rules.xml excludes domain=\"sharedpref\" " +
                    "path=\"user_session.xml\" for <device-transfer>.",
                callout1.likelyCause,
            )

            // Failure notice includes failed step + likelyCause alongside saved device logcat
            // notices.
            val suppressedMessages = failure1!!.suppressed.map { it.toString() }
            assertTrue(
                suppressedMessages.any {
                    "Failed at step 7 (Verify). Likely cause: data_extraction_rules.xml excludes" in
                        it
                }
            )
            assertEquals(2, suppressedMessages.count { it.startsWith("Device logs saved to ") })

            // Verify BackupService progress sub-steps in host-log.txt.
            val hostLog1 = File(resultsDir, "suite-details/$detail1Id/host-log.txt").readText()
            assertContains(hostLog1, "Backup step 1/2: Initializing transport")
            assertContains(hostLog1, "Restore step 1/2: Pushing restore archive")

            // Verify zip entry size summary without raw archive copy when flag is unset.
            val archiveSummary = record1.artifacts.single()
            assertEquals("Backup file summary", archiveSummary.name)
            assertEquals(
                mapOf("app_backup" to "2.0 KB", "pm_backup" to "412 B"),
                archiveSummary.values,
            )
            assertNull(archiveSummary.path)

            // Verify step-by-step telemetry entries published for Test 1 & Test 2.
            assertEquals("DEVICE_TO_DEVICE", published1[BackupReportKeys.TRANSPORT_MODE])
            assertEquals("FAILURE", published1[BackupReportKeys.STATUS])
            assertEquals("VERIFICATION_FAILED", published1[BackupReportKeys.ERROR_CODE])

            assertEquals("CLOUD_ENCRYPTED", published2[BackupReportKeys.TRANSPORT_MODE])
            assertEquals("SUCCESS", published2[BackupReportKeys.STATUS])

            // Verify Test 2 inherited only the shared step (n = 1) plus its own 3 steps (n = 2..4).
            val detail2Id = extension.detailIdOf(test2Id)
            val record2 =
                TestDetailRecord.fromJson(
                    File(resultsDir, "suite-details/$detail2Id.json").readText()
                )
            assertEquals(
                listOf(1 to true, 2 to false, 3 to false, 4 to false),
                record2.steps.map { it.n to it.shared },
            )
        }

    @Test
    fun includeRawArchivesAndCaptureLogcatOnPassFlagsControlOptionalFiles() = withDeviceProperties {
        val resultsDir = tempFolder.newFolder("results-flags")
        System.setProperty(RESULTS_DIR, resultsDir.absolutePath)
        System.setProperty(BackupRestoreControllerImpl.PROP_INCLUDE_RAW_ARCHIVES, "true")
        System.setProperty(PROP_CAPTURE_LOGCAT_ON_PASS, "true")

        stubHealthyDevice(sourceDevice, logcat = "I BackupManager: backup pass ok\n")
        val zipService = fakeZipBackupService(entries = mapOf("app_backup" to ByteArray(500) { 7 }))

        val extension = BackupRestoreExtension { sourceDevice.session }
        val classCtx = childContext(rootContext, CLASS_ID)
        val testCtx = childContext(classCtx, TEST_ID)

        extension.beforeAll(classCtx)
        extension.beforeEach(testCtx)
        val controller = newController(sourceDevice, apiLevel = 34, backupService = zipService)
        extension.track(testCtx, controller)

        val callerTempDir = tempFolder.newFolder("caller-temp")
        runBlocking {
            controller.performBackup(
                BackupTransportMode.CLOUD_ENCRYPTED,
                callerTempDir.toPath(),
            )
        }
        // Simulate caller deleting @TempDir before afterEach runs.
        callerTempDir.deleteRecursively()

        extension.afterEach(testCtx)
        extension.afterAll(classCtx)

        val detailId = extension.detailIdOf(TEST_ID)
        val record =
            TestDetailRecord.fromJson(File(resultsDir, "suite-details/$detailId.json").readText())

        assertEquals(
            listOf("Host log", "Logcat (device)"),
            record.logs.map { it.title },
        )
        val savedLogcat = File(resultsDir, "suite-details/$detailId/logcat-$SERIAL_SOURCE.txt")
        assertEquals("I BackupManager: backup pass ok\n", savedLogcat.readText())

        assertEquals(2, record.artifacts.size)
        val rawAttachment = record.artifacts[1]
        assertEquals(
            "suite-details/$detailId/backup_cloud_encrypted_device.zip",
            rawAttachment.path,
        )
        assertTrue(File(resultsDir, rawAttachment.path!!).isFile)
    }

    @Test
    fun hostLogAndDeviceLogcatRespectByteSizeCaps() = withDeviceProperties {
        val resultsDir = tempFolder.newFolder("results-caps")
        System.setProperty(RESULTS_DIR, resultsDir.absolutePath)

        // Generate > 700 KB of logcat lines with a distinctive tail marker.
        val oversizedLogcat = buildString {
            repeat(10_000) { idx ->
                append(
                    "01-01 12:00:00.000  1000  1000 I BackupLogLine: filler-$idx-\u00e9\u2603-padding-0123456789abcdef\n"
                )
            }
            append("01-01 12:00:01.000  1000  1000 E BackupAgent: FINAL_LOGCAT_TAIL_MARKER\n")
        }
        stubHealthyDevice(sourceDevice, logcat = oversizedLogcat)

        val failure = AssertionError("verify failed")
        val extension = BackupRestoreExtension { sourceDevice.session }
        val classCtx = childContext(rootContext, CLASS_ID)
        val testCtx = childContext(classCtx, TEST_ID, failureProvider = { failure })

        extension.beforeAll(classCtx)
        extension.beforeEach(testCtx)
        val controller = newController(sourceDevice, apiLevel = 34)
        extension.track(testCtx, controller)

        // Emit > 400 KB of JUL host log entries with a distinctive tail marker.
        val testLogger = Logger.getLogger("androidx.test.backup.host.ChattyTest")
        repeat(6_000) { idx ->
            testLogger.info(
                "Host log filler entry #$idx with utf8 \u00fc\u00f1\u00ee\u00e7\u00f8\u00f0\u00e9 payload 0123456789abcdef"
            )
        }
        testLogger.severe("FINAL_HOST_LOG_TAIL_MARKER")

        extension.afterEach(testCtx)
        extension.afterAll(classCtx)

        val detailId = extension.detailIdOf(TEST_ID)
        val record =
            TestDetailRecord.fromJson(File(resultsDir, "suite-details/$detailId.json").readText())
        assertEquals(listOf(true, true), record.logs.map { it.truncated })

        val hostLogBytes =
            Files.readAllBytes(resultsDir.toPath().resolve("suite-details/$detailId/host-log.txt"))
        assertTrue(
            hostLogBytes.size <= 256 * 1024,
            "Expected host-log.txt <= 262144 bytes, got ${hostLogBytes.size}",
        )
        val hostLogText = hostLogBytes.toString(Charsets.UTF_8)
        assertContains(hostLogText, "FINAL_HOST_LOG_TAIL_MARKER")

        val logcatBytes =
            Files.readAllBytes(
                resultsDir.toPath().resolve("suite-details/$detailId/logcat-$SERIAL_SOURCE.txt")
            )
        assertTrue(
            logcatBytes.size <= 512 * 1024,
            "Expected logcat <= 524288 bytes, got ${logcatBytes.size}",
        )
        assertContains(logcatBytes.toString(Charsets.UTF_8), "FINAL_LOGCAT_TAIL_MARKER")
    }

    @Test
    fun deviceFactsAreCachedOncePerSerialPerTestRunWithoutGetpropShellCommands() {
        sourceDevice.stubReadonlyProperties(
            mapOf(
                "ro.product.model" to "sdk_gphone64_arm64",
                "ro.boot.qemu.avd_name" to "Pixel_7_Pro_API_34",
                "ro.build.flavor" to "sdk_gphone64_arm64-userdebug",
            )
        )
        sourceDevice.onShell { cmd ->
            when (cmd) {
                "pm list packages --show-versioncode com.google.android.gms" ->
                    shellOutput("package:com.google.android.gms versionCode:263933000\n")
                "pm path com.android.vending" ->
                    shellOutput("package:/product/priv-app/Phonesky/Phonesky.apk\n")
                else -> null
            }
        }

        val extension = BackupRestoreExtension { sourceDevice.session }
        val c1 = newController(sourceDevice, apiLevel = 34)
        val c2 = newController(sourceDevice, apiLevel = 34)

        val facts1 = extension.cachedDeviceFactsFor(rootContext, c1)
        val callsAfterFirst = sourceDevice.commands.size
        assertTrue(callsAfterFirst > 0)
        assertTrue(sourceDevice.commands.none { it.startsWith("getprop") })
        assertEquals(DeviceFacts("Pixel 7 Pro", 34, "Google Play", "26.39.33"), facts1)

        val facts2 = extension.cachedDeviceFactsFor(rootContext, c2)
        assertEquals(facts1, facts2)
        assertEquals(callsAfterFirst, sourceDevice.commands.size)
    }

    @Test
    fun sharedPrefixAndOverflowCollectorPlaceSingleTruncationNoticeAtTop() {
        val collector = HostLogCollector(maxBytes = 128)
        repeat(40) { idx -> collector.appendRaw("method-log-line-$idx-0123456789\n") }
        assertTrue(collector.isOverflowTruncated())
        assertFalse(collector.snapshotText().contains(TRUNCATION_NOTICE))

        // Combining a short @BeforeAll prefix with an overflow-truncated method buffer places the
        // truncation notice once at byte 0 rather than in the middle of host-log.txt.
        val wideCollector = HostLogCollector(maxBytes = 4096)
        repeat(400) { idx -> wideCollector.appendRaw("wide-line-$idx-0123456789abcdef\n") }
        assertTrue(wideCollector.isOverflowTruncated())
        val combined = wideCollector.toBoundedUtf8(prefixText = "[00:00:00.000] INFO beforeAll\n")
        assertTrue(combined.truncated)
        val decoded = combined.bytes.decodeToString()
        assertTrue(decoded.startsWith(TRUNCATION_NOTICE))
        assertEquals(1, Regex(Regex.escape(TRUNCATION_NOTICE)).findAll(decoded).count())
        assertContains(decoded, "wide-line-399-0123456789abcdef")
    }

    @Test
    fun detailIdDistinguishesOverloadedParameterizedRepeatedAndNestedTests() {
        val extension = BackupRestoreExtension()
        val ids =
            listOf(
                    "[engine:junit-jupiter]/[class:com.a.BackupTest]/[method:run(androidx.test.backup.host.BackupRestoreController)]",
                    "[engine:junit-jupiter]/[class:com.a.BackupTest]/[method:run(androidx.test.backup.host.BackupRestoreController,java.nio.file.Path)]",
                    "[engine:junit-jupiter]/[class:com.b.BackupTest]/[method:run(androidx.test.backup.host.BackupRestoreController)]",
                    "[engine:junit-jupiter]/[class:com.a.BackupTest]/[test-template:run(java.lang.String)]/[test-template-invocation:#1]",
                    "[engine:junit-jupiter]/[class:com.a.BackupTest]/[test-template:run(java.lang.String)]/[test-template-invocation:#2]",
                    "[engine:junit-jupiter]/[class:com.a.BackupTest]/[nested-class:SignedIn]/[method:run()]",
                    "[engine:junit-jupiter]/[class:com.a.BackupTest]/[nested-class:SignedOut]/[method:run()]",
                )
                .map { extension.detailIdOf(it) }

        assertEquals(ids.size, ids.toSet().size, "Expected all detailIds to be unique: $ids")
        val safePattern = Regex("^[A-Za-z0-9._-]+$")
        for (id in ids) {
            assertTrue(safePattern.matches(id), "Unsafe detailId: $id")
        }
        assertTrue(ids[0].startsWith("BackupTest.run-"))
        assertTrue(ids[3].startsWith("BackupTest.run.1-"))
        assertTrue(ids[5].startsWith("BackupTest.SignedIn.run-"))
    }

    @Test
    fun deviceLogsDisabledOverridesCaptureLogcatOnPass() = withDeviceProperties {
        val resultsDir = tempFolder.newFolder("results-disabled-logs")
        System.setProperty(RESULTS_DIR, resultsDir.absolutePath)
        System.setProperty(PROP_CAPTURE_LOGCAT_ON_PASS, "true")
        System.setProperty("androidx.test.backup.device.logs.enabled", "false")

        stubHealthyDevice(sourceDevice, logcat = "I Backup: should not save\n")
        val extension = BackupRestoreExtension { sourceDevice.session }
        val classCtx = childContext(rootContext, CLASS_ID)
        val testCtx = childContext(classCtx, TEST_ID)

        extension.beforeAll(classCtx)
        extension.beforeEach(testCtx)
        extension.track(testCtx, newController(sourceDevice, apiLevel = 34))
        extension.afterEach(testCtx)
        extension.afterAll(classCtx)

        val detailId = extension.detailIdOf(TEST_ID)
        val record =
            TestDetailRecord.fromJson(File(resultsDir, "suite-details/$detailId.json").readText())
        assertEquals(listOf("Host log"), record.logs.map { it.title })
        assertFalse(File(resultsDir, "suite-details/$detailId/logcat-$SERIAL_SOURCE.txt").exists())
    }

    @Test
    fun restoreOnlyTestDetectsTransportModeFromGoldenArchiveMetadata() = withDeviceProperties {
        val resultsDir = tempFolder.newFolder("results-golden-restore")
        System.setProperty(RESULTS_DIR, resultsDir.absolutePath)
        stubHealthyDevice(sourceDevice)

        val goldenArchive = tempFolder.newFile("golden_fixture.backup").toPath()
        ZipOutputStream(Files.newOutputStream(goldenArchive)).use { zos ->
            zos.putNextEntry(ZipEntry(BackupService.METADATA_FILE))
            zos.write(
                ("${BackupService.PROPERTY_APPLICATION_ID}=$PACKAGE\n" +
                        "${BackupService.PROPERTY_BACKUP_TYPE}=DEVICE_TO_DEVICE\n")
                    .toByteArray(Charsets.UTF_8)
            )
            zos.closeEntry()
        }

        val published = LinkedHashMap<String, String>()
        val extension = BackupRestoreExtension { sourceDevice.session }
        val classCtx = childContext(rootContext, CLASS_ID)
        val testCtx = childContext(classCtx, TEST_ID, reportSink = published)

        extension.beforeAll(classCtx)
        extension.beforeEach(testCtx)
        val controller = newController(sourceDevice, apiLevel = 34)
        extension.track(testCtx, controller)

        runBlocking { controller.performRestore(goldenArchive) }

        extension.afterEach(testCtx)
        extension.afterAll(classCtx)

        val detailId = extension.detailIdOf(TEST_ID)
        val record =
            TestDetailRecord.fromJson(File(resultsDir, "suite-details/$detailId.json").readText())
        val attrs = record.summaryAttributes.associateBy { it.key }
        assertEquals("DEVICE_TO_DEVICE", attrs["transport"]?.value)
        assertEquals("Device to device", attrs["transport"]?.displayValue)
        assertEquals("DEVICE_TO_DEVICE", published[BackupReportKeys.TRANSPORT_MODE])
        assertEquals("SUCCESS", published[BackupReportKeys.STATUS])
    }

    @Test
    fun asyncFutureCallsPropagateStepAndHostLogCaptureAcrossConcurrentTests() =
        withDeviceProperties {
            val resultsDir = tempFolder.newFolder("results-async")
            System.setProperty(RESULTS_DIR, resultsDir.absolutePath)

            val executor = Executors.newFixedThreadPool(4)
            val poolDispatcher = executor.asCoroutineDispatcher()
            try {
                val deviceA = FakeAdbDevice(SERIAL_SOURCE, scopeDispatcher = poolDispatcher)
                val deviceB = FakeAdbDevice(SERIAL_TARGET, scopeDispatcher = poolDispatcher)
                stubHealthyDevice(deviceA)
                stubHealthyDevice(deviceB)

                val extension = BackupRestoreExtension { deviceA.session }
                val classCtx = childContext(rootContext, CLASS_ID)
                extension.beforeAll(classCtx)

                val testAId = "$CLASS_ID/[method:testOne()]"
                val testBId = "$CLASS_ID/[method:testTwo()]"
                val testACtx = childContext(classCtx, testAId)
                val testBCtx = childContext(classCtx, testBId)

                val ctrlA =
                    newController(
                        deviceA,
                        apiLevel = 34,
                        backupService =
                            fakeZipBackupService(
                                mapOf("app_backup" to ByteArray(128)),
                                emitProgress = true,
                            ),
                    )
                val ctrlB =
                    newController(
                        deviceB,
                        apiLevel = 34,
                        backupService =
                            fakeZipBackupService(
                                mapOf("app_backup" to ByteArray(256)),
                                emitProgress = true,
                            ),
                    )
                val outA = tempFolder.newFolder("async-out-a").toPath()
                val outB = tempFolder.newFolder("async-out-b").toPath()

                // Keep BOTH leaf captures registered simultaneously on two worker threads so the
                // single-leaf fallback in ActiveCaptureContext.current() returns null and only
                // CaptureContextElement coroutine propagation routes steps and host logs.
                val bothActiveBarrier = CyclicBarrier(2)
                val runnerPool = Executors.newFixedThreadPool(2)
                try {
                    val fA =
                        runnerPool.submit(
                            Callable {
                                extension.beforeEach(testACtx)
                                extension.track(testACtx, ctrlA)
                                bothActiveBarrier.await(5, TimeUnit.SECONDS)
                                Logger.getLogger("androidx.test.backup.host.AsyncA")
                                    .info("ASYNC_ONLY_A")
                                ctrlA.runOnDeviceAsync("com.example.SeedAsyncA").get()
                                val archive =
                                    ctrlA
                                        .performBackupAsync(
                                            BackupTransportMode.CLOUD_UNENCRYPTED,
                                            outA,
                                        )
                                        .get()
                                ctrlA.performRestoreAsync(archive).get()
                                bothActiveBarrier.await(5, TimeUnit.SECONDS)
                                extension.afterEach(testACtx)
                            }
                        )
                    val fB =
                        runnerPool.submit(
                            Callable {
                                extension.beforeEach(testBCtx)
                                extension.track(testBCtx, ctrlB)
                                bothActiveBarrier.await(5, TimeUnit.SECONDS)
                                Logger.getLogger("androidx.test.backup.host.AsyncB")
                                    .info("ASYNC_ONLY_B")
                                ctrlB.runOnDeviceAsync("com.example.SeedAsyncB").get()
                                val archive =
                                    ctrlB
                                        .performBackupAsync(
                                            BackupTransportMode.DEVICE_TO_DEVICE,
                                            outB,
                                        )
                                        .get()
                                ctrlB.performRestoreAsync(archive).get()
                                bothActiveBarrier.await(5, TimeUnit.SECONDS)
                                extension.afterEach(testBCtx)
                            }
                        )
                    fA.get()
                    fB.get()
                } finally {
                    runnerPool.shutdownNow()
                    runnerPool.awaitTermination(5, TimeUnit.SECONDS)
                }
                extension.afterAll(classCtx)

                val idA = extension.detailIdOf(testAId)
                val recA =
                    TestDetailRecord.fromJson(
                        File(resultsDir, "suite-details/$idA.json").readText()
                    )
                assertEquals(listOf("Setup", "Backup", "Restore"), recA.steps.map { it.name })
                assertEquals("runOnDevice(\"\u2026SeedAsyncA\")", recA.steps[0].call)
                assertEquals("performBackup(CLOUD_UNENCRYPTED)", recA.steps[1].call)
                val logA = File(resultsDir, "suite-details/$idA/host-log.txt").readText()
                assertContains(logA, "ASYNC_ONLY_A")
                assertContains(logA, "Backup step 1/2: Initializing transport")
                assertFalse("ASYNC_ONLY_B" in logA)

                val idB = extension.detailIdOf(testBId)
                val recB =
                    TestDetailRecord.fromJson(
                        File(resultsDir, "suite-details/$idB.json").readText()
                    )
                assertEquals(listOf("Setup", "Backup", "Restore"), recB.steps.map { it.name })
                assertEquals("runOnDevice(\"\u2026SeedAsyncB\")", recB.steps[0].call)
                assertEquals("performBackup(DEVICE_TO_DEVICE)", recB.steps[1].call)
                val logB = File(resultsDir, "suite-details/$idB/host-log.txt").readText()
                assertContains(logB, "ASYNC_ONLY_B")
                assertFalse("ASYNC_ONLY_A" in logB)
            } finally {
                poolDispatcher.close()
                executor.shutdownNow()
                executor.awaitTermination(5, TimeUnit.SECONDS)
            }
        }

    @Test
    fun multiThreadedDispatcherIsolatesStepsAndHostLogsAcrossSequentialTests() =
        withDeviceProperties {
            val resultsDir = tempFolder.newFolder("results-multithreaded")
            System.setProperty(RESULTS_DIR, resultsDir.absolutePath)

            val executor = Executors.newFixedThreadPool(4)
            val poolDispatcher = executor.asCoroutineDispatcher()
            try {
                val pooledDevice = FakeAdbDevice(SERIAL_SOURCE, scopeDispatcher = poolDispatcher)
                stubHealthyDevice(pooledDevice)
                val workerLogger = Logger.getLogger("androidx.test.backup.host.PooledWorker")

                val switchingService =
                    mock(BackupService::class.java) { invocation ->
                        when (invocation.method.name) {
                            "backup" -> {
                                val outPath = invocation.arguments[3] as Path
                                val listener = invocation.arguments[4] as? BackupProgressListener
                                runBlocking(poolDispatcher) {
                                    workerLogger.info("Pooled backup worker log")
                                    listener?.onStep(
                                        BackupProgressListener.Step(1, 1, "Backing up on pool")
                                    )
                                    ZipOutputStream(Files.newOutputStream(outPath)).use { zos ->
                                        zos.putNextEntry(ZipEntry("app_backup"))
                                        zos.write(ByteArray(32))
                                        zos.closeEntry()
                                    }
                                }
                                BackupResult.Success
                            }
                            "restore" -> {
                                runBlocking(poolDispatcher) {
                                    workerLogger.info("Pooled restore worker log")
                                }
                                BackupResult.Success
                            }
                            else -> RETURNS_DEFAULTS.answer(invocation)
                        }
                    }

                val extension = BackupRestoreExtension { pooledDevice.session }
                val classCtx = childContext(rootContext, CLASS_ID)
                extension.beforeAll(classCtx)
                val sharedController =
                    newController(pooledDevice, apiLevel = 34, backupService = switchingService)
                extension.track(classCtx, sharedController)

                // Spawn worker threads during @BeforeAll and run a shared setup step on the pool.
                runBlocking {
                    withContext(poolDispatcher) {
                        Logger.getLogger("androidx.test.backup.host.BeforeAllPool")
                            .info("SHARED_BEFORE_ALL_MARKER")
                        sharedController.runOnDevice("com.example.SharedPoolInit")
                    }
                }

                // Test 1 switches across worker threads in both step-by-step and BackupService.
                val test1Id = "$CLASS_ID/[method:testOne()]"
                val test1Ctx = childContext(classCtx, test1Id)
                extension.beforeEach(test1Ctx)
                val out1 = tempFolder.newFolder("pool-out-1").toPath()
                runBlocking {
                    withContext(poolDispatcher) {
                        Logger.getLogger("androidx.test.backup.host.TestOnePool")
                            .info("TEST_ONE_ONLY_MARKER")
                        val archive =
                            sharedController.performBackup(
                                BackupTransportMode.CLOUD_ENCRYPTED,
                                out1,
                            )
                        sharedController.clearAppData()
                        sharedController.performRestore(archive)
                    }
                }
                extension.afterEach(test1Ctx)

                // Test 2 reuses the same worker pool; nothing from Test 1 may leak into Test 2.
                val test2Id = "$CLASS_ID/[method:testTwo()]"
                val test2Ctx = childContext(classCtx, test2Id)
                extension.beforeEach(test2Ctx)
                val out2 = tempFolder.newFolder("pool-out-2").toPath()
                runBlocking {
                    withContext(poolDispatcher) {
                        Logger.getLogger("androidx.test.backup.host.TestTwoPool")
                            .info("TEST_TWO_ONLY_MARKER")
                        sharedController.runBackupRestoreFlow(
                            StorageDomain.Preference("prefs", "k", "v"),
                            out2,
                            BackupTransportMode.CLOUD_ENCRYPTED,
                        )
                    }
                }
                extension.afterEach(test2Ctx)
                extension.afterAll(classCtx)

                val id1 = extension.detailIdOf(test1Id)
                val rec1 =
                    TestDetailRecord.fromJson(
                        File(resultsDir, "suite-details/$id1.json").readText()
                    )
                assertEquals(
                    listOf("Setup", "Backup", "Clear", "Restore"),
                    rec1.steps.map { it.name },
                )
                assertEquals(listOf(true, false, false, false), rec1.steps.map { it.shared })
                val log1 = File(resultsDir, "suite-details/$id1/host-log.txt").readText()
                assertContains(log1, "SHARED_BEFORE_ALL_MARKER")
                assertContains(log1, "TEST_ONE_ONLY_MARKER")
                assertContains(log1, "Pooled backup worker log")
                assertFalse("TEST_TWO_ONLY_MARKER" in log1)

                val id2 = extension.detailIdOf(test2Id)
                val rec2 =
                    TestDetailRecord.fromJson(
                        File(resultsDir, "suite-details/$id2.json").readText()
                    )
                assertEquals(
                    listOf("Setup", "Setup", "Backup", "Clear", "Restore", "Verify"),
                    rec2.steps.map { it.name },
                )
                assertEquals(
                    listOf(true, false, false, false, false, false),
                    rec2.steps.map { it.shared },
                )
                val log2 = File(resultsDir, "suite-details/$id2/host-log.txt").readText()
                assertContains(log2, "SHARED_BEFORE_ALL_MARKER")
                assertContains(log2, "TEST_TWO_ONLY_MARKER")
                assertContains(log2, "Pooled backup worker log")
                assertFalse("TEST_ONE_ONLY_MARKER" in log2, "Test 1 worker logs leaked into Test 2")
            } finally {
                poolDispatcher.close()
                executor.shutdownNow()
                executor.awaitTermination(5, TimeUnit.SECONDS)
            }
        }

    @Test
    fun backupRulesLikelyCauseSkippedForSetupClearAndInfrastructureErrors() = withDeviceProperties {
        val resultsDir = tempFolder.newFolder("results-rules-gate")
        writeSuiteEnvironmentJson(resultsDir, allowBackup = false)
        System.setProperty(RESULTS_DIR, resultsDir.absolutePath)

        // (a) Setup failure in runBackupRestoreFlow must NOT blame allowBackup="false".
        sourceDevice.onShell { cmd ->
            when {
                cmd == "pm list instrumentation $PACKAGE" -> shellOutput("")
                cmd == "date +%s" -> shellOutput("1700000100\n")
                cmd.startsWith("logcat ") -> shellOutput("E Runner: missing test apk\n")
                else -> ShellCommandOutput("", "", 0)
            }
        }
        val extension = BackupRestoreExtension { sourceDevice.session }
        val classCtx = childContext(rootContext, CLASS_ID)
        extension.beforeAll(classCtx)

        var setupErr: Throwable? = null
        val setupTestId = "$CLASS_ID/[method:setupFails()]"
        val setupCtx = childContext(classCtx, setupTestId, failureProvider = { setupErr })
        extension.beforeEach(setupCtx)
        val c1 = newController(sourceDevice, apiLevel = 34)
        extension.track(setupCtx, c1)
        setupErr =
            assertFailsWith<Exception> {
                runBlocking {
                    c1.runBackupRestoreFlow(
                        StorageDomain.Preference("prefs", "k", "v"),
                        tempFolder.newFolder("setup-fail-out").toPath(),
                        BackupTransportMode.CLOUD_ENCRYPTED,
                    )
                }
            }
        extension.afterEach(setupCtx)
        val setupRecord =
            TestDetailRecord.fromJson(
                File(resultsDir, "suite-details/${extension.detailIdOf(setupTestId)}.json")
                    .readText()
            )
        assertEquals("Setup", setupRecord.steps.first { it.status == "FAILED" }.name)
        assertNull(setupRecord.callout?.likelyCause)

        // (b) Infrastructure BMGR_INIT_FAILED during Backup must NOT blame allowBackup="false".
        stubHealthyDevice(sourceDevice)
        val nonInfraBackupFailService =
            fakeBackupService(
                backupResult =
                    BackupResult.Error(
                        ErrorCode.BACKUP_FAILED,
                        IOException("Backup rejected package"),
                    )
            )
        var infraErr: Throwable? = null
        val infraTestId = "$CLASS_ID/[method:bmgrInitFails()]"
        val infraCtx = childContext(classCtx, infraTestId, failureProvider = { infraErr })
        extension.beforeEach(infraCtx)
        val c2 =
            newController(
                sourceDevice,
                apiLevel = 34,
                backupService =
                    mock(BackupService::class.java) { inv ->
                        if (inv.method.name == "backup") {
                            throw BackupRestoreException(
                                BackupErrorCode.BMGR_INIT_FAILED,
                                "Backup Manager failed to initialize",
                            )
                        } else {
                            RETURNS_DEFAULTS.answer(inv)
                        }
                    },
            )
        extension.track(infraCtx, c2)
        infraErr =
            assertFailsWith<BackupRestoreException> {
                runBlocking {
                    c2.performBackup(
                        BackupTransportMode.CLOUD_ENCRYPTED,
                        tempFolder.newFolder("infra-fail-out").toPath(),
                    )
                }
            }
        extension.afterEach(infraCtx)
        val infraRecord =
            TestDetailRecord.fromJson(
                File(resultsDir, "suite-details/${extension.detailIdOf(infraTestId)}.json")
                    .readText()
            )
        assertNull(infraRecord.callout?.likelyCause)

        // (c) Non-infrastructure BACKUP_FAILED during Backup DOES report allowBackup="false".
        var backupErr: Throwable? = null
        val backupTestId = "$CLASS_ID/[method:backupRejected()]"
        val backupCtx = childContext(classCtx, backupTestId, failureProvider = { backupErr })
        extension.beforeEach(backupCtx)
        val c3 =
            newController(sourceDevice, apiLevel = 34, backupService = nonInfraBackupFailService)
        extension.track(backupCtx, c3)
        backupErr =
            assertFailsWith<BackupRestoreException> {
                runBlocking {
                    c3.performBackup(
                        BackupTransportMode.CLOUD_ENCRYPTED,
                        tempFolder.newFolder("backup-fail-out").toPath(),
                    )
                }
            }
        extension.afterEach(backupCtx)
        extension.afterAll(classCtx)

        val backupRecord =
            TestDetailRecord.fromJson(
                File(resultsDir, "suite-details/${extension.detailIdOf(backupTestId)}.json")
                    .readText()
            )
        assertContains(backupRecord.callout?.likelyCause.orEmpty(), "allowBackup=\"false\"")
    }

    @Test
    fun twoDeviceReversedParameterOrderKeepsSourceBeforeTargetOnBackupFailure() =
        withDeviceProperties {
            val resultsDir = tempFolder.newFolder("results-actor-order")
            System.setProperty(RESULTS_DIR, resultsDir.absolutePath)
            stubHealthyDevice(sourceDevice, avdName = "Pixel_6_API_31")
            stubHealthyDevice(targetDevice, avdName = "Pixel_8_API_34")

            val failingService =
                fakeBackupService(
                    backupResult =
                        BackupResult.Error(
                            ErrorCode.BACKUP_FAILED,
                            IOException("transport error"),
                        )
                )

            var failure: Throwable? = null
            val extension = BackupRestoreExtension { sourceDevice.session }
            val classCtx = childContext(rootContext, CLASS_ID)
            val testCtx = childContext(classCtx, TEST_ID, failureProvider = { failure })
            extension.beforeAll(classCtx)
            extension.beforeEach(testCtx)

            // Parameters resolved in (target, source) order without explicit @Device(role = ...).
            val targetCtrl =
                newController(
                    targetDevice,
                    apiLevel = 34,
                    facts = DeviceFacts("Pixel 8", 34, "Google APIs", "26.39.33"),
                )
            val sourceCtrl =
                newController(
                    sourceDevice,
                    apiLevel = 31,
                    backupService = failingService,
                    facts = DeviceFacts("Pixel 6", 31, "Google APIs", "26.39.33"),
                )
            extension.track(testCtx, targetCtrl)
            extension.track(testCtx, sourceCtrl)

            failure =
                assertFailsWith<BackupRestoreException> {
                    runBlocking {
                        sourceCtrl.performBackup(
                            BackupTransportMode.DEVICE_TO_DEVICE,
                            tempFolder.newFolder("rev-out").toPath(),
                        )
                    }
                }

            extension.afterEach(testCtx)
            extension.afterAll(classCtx)

            val detailId = extension.detailIdOf(TEST_ID)
            val record =
                TestDetailRecord.fromJson(
                    File(resultsDir, "suite-details/$detailId.json").readText()
                )
            assertEquals(
                listOf("source" to SERIAL_SOURCE, "target" to SERIAL_TARGET),
                record.actors.map { it.id to it.serial },
            )
        }

    @Test
    fun stepByStepActionFailurePreconditionClassTelemetryAndLogDirResolution() =
        withDeviceProperties {
            stubHealthyDevice(sourceDevice)
            val extension = BackupRestoreExtension { sourceDevice.session }

            // 1. Run a failing test WITHOUT RESULTS_DIR first (caching fallback temp dir in root).
            val preFailure = AssertionError("outside suite failure")
            val preClass = childContext(rootContext, "$CLASS_ID-pre")
            val preTest = childContext(preClass, "$TEST_ID-pre", failureProvider = { preFailure })
            extension.beforeEach(preTest)
            extension.track(preTest, newController(sourceDevice, apiLevel = 34))
            extension.afterEach(preTest)
            assertEquals(1, preFailure.suppressed.size)

            // 2. Now enable RESULTS_DIR on the same rootStore; logcat must land under RESULTS_DIR.
            val resultsDir = tempFolder.newFolder("results-telemetry")
            System.setProperty(RESULTS_DIR, resultsDir.absolutePath)

            // 3. Step-by-step runOnDevice returning BackupActionResult.Failure + getOrThrow()
            // must emit SEEDING_FAILED rather than UNKNOWN_ERROR.
            sourceDevice.onShell { cmd ->
                when {
                    cmd == "pm list instrumentation $PACKAGE" ->
                        shellOutput(
                            "instrumentation:$PACKAGE.test/${BackupActionWireProtocol.RUNNER_CLASS} (target=$PACKAGE)\n"
                        )
                    cmd.startsWith("am instrument ") ->
                        shellOutput(
                            instrumentOutput(
                                """{"isSuccess":false,"errorMessage":"seed crash on device"}"""
                            )
                        )
                    cmd == "date +%s" -> shellOutput("1700000100\n")
                    cmd.startsWith("logcat ") -> shellOutput("E SeedAction: seed crash\n")
                    else -> ShellCommandOutput("", "", 0)
                }
            }
            val actionPublished = LinkedHashMap<String, String>()
            var actionErr: Throwable? = null
            val classCtx = childContext(rootContext, CLASS_ID)
            extension.beforeAll(classCtx)
            val actionTestId = "$CLASS_ID/[method:actionFailure()]"
            val actionCtx =
                childContext(
                    classCtx,
                    actionTestId,
                    failureProvider = { actionErr },
                    reportSink = actionPublished,
                )
            extension.beforeEach(actionCtx)
            val c1 = newController(sourceDevice, apiLevel = 34)
            extension.track(actionCtx, c1)
            actionErr =
                assertFailsWith<IOException> {
                    runBlocking { c1.runOnDevice("com.example.BrokenSeed").getOrThrow() }
                }
            extension.afterEach(actionCtx)

            assertEquals(
                BackupErrorCode.SEEDING_FAILED.name,
                actionPublished[BackupReportKeys.ERROR_CODE],
            )
            val actionDetailId = extension.detailIdOf(actionTestId)
            assertTrue(
                File(resultsDir, "suite-details/$actionDetailId/logcat-$SERIAL_SOURCE.txt").isFile
            )

            // 4. Failed require(...) in runBackupRestoreFlow after a shared step must publish
            // BackupRestore.status exactly once.
            stubHealthyDevice(sourceDevice)
            val sharedClassId = "$CLASS_ID-shared"
            val sharedClassCtx = childContext(rootContext, sharedClassId)
            extension.beforeAll(sharedClassCtx)
            val sharedCtrl = newController(sourceDevice, apiLevel = 34)
            extension.track(sharedClassCtx, sharedCtrl)
            runBlocking { sharedCtrl.runOnDevice("com.example.SharedSeed") }

            val statusEmissions = mutableListOf<String>()
            var reqErr: Throwable? = null
            val reqTestCtx =
                childContext(
                    sharedClassCtx,
                    "$sharedClassId/[method:emptyStorages()]",
                    failureProvider = { reqErr },
                )
            doAnswer { inv ->
                    if (inv.getArgument<String>(0) == BackupReportKeys.STATUS) {
                        statusEmissions += inv.getArgument<String>(1)
                    }
                    null
                }
                .`when`(reqTestCtx)
                .publishReportEntry(anyString(), anyString())
            extension.beforeEach(reqTestCtx)
            val flowCtrl =
                BackupRestoreControllerImpl(
                    adbSession = sourceDevice.session,
                    serialNumber = SERIAL_SOURCE,
                    apiLevel = 34,
                    applicationId = PACKAGE,
                    telemetryPublisher = { k, v -> reqTestCtx.publishReportEntry(k, v) },
                )
            extension.track(reqTestCtx, flowCtrl)
            reqErr =
                assertFailsWith<IllegalArgumentException> {
                    runBlocking {
                        flowCtrl.runBackupRestoreFlow(
                            emptyList<StorageDomain>(),
                            tempFolder.newFolder("empty-flow").toPath(),
                            BackupTransportMode.LOCAL,
                        )
                    }
                }
            extension.afterEach(reqTestCtx)
            extension.afterAll(sharedClassCtx)
            assertEquals(listOf("FAILURE"), statusEmissions)

            // 5. Failed @BeforeAll step-by-step call emits class-level BackupRestore.errorCode.
            val classPublished = LinkedHashMap<String, String>()
            var classErr: Throwable? = null
            val failingClassId = "$CLASS_ID-beforeAllFail"
            val failingClassCtx =
                childContext(
                    rootContext,
                    failingClassId,
                    failureProvider = { classErr },
                    reportSink = classPublished,
                )
            extension.beforeAll(failingClassCtx)
            val failingClassCtrl =
                newController(
                    sourceDevice,
                    apiLevel = 34,
                    backupService =
                        fakeBackupService(
                            backupResult =
                                BackupResult.Error(
                                    ErrorCode.BACKUP_FAILED,
                                    IOException("class backup failed"),
                                )
                        ),
                )
            extension.track(failingClassCtx, failingClassCtrl)
            classErr =
                assertFailsWith<BackupRestoreException> {
                    runBlocking {
                        failingClassCtrl.performBackup(
                            BackupTransportMode.CLOUD_ENCRYPTED,
                            tempFolder.newFolder("class-fail").toPath(),
                        )
                    }
                }
            extension.afterAll(failingClassCtx)
            extension.afterAll(classCtx)

            assertEquals("FAILURE", classPublished[BackupReportKeys.STATUS])
            assertEquals(
                BackupErrorCode.BACKUP_FAILED.name,
                classPublished[BackupReportKeys.ERROR_CODE],
            )
        }

    @Test
    fun perMethodConstructorResolvedInClassContextAndLogcatSaveDoNotLeakAcrossTests() =
        withDeviceProperties {
            val resultsDir = tempFolder.newFolder("results-per-method-ctor")
            System.setProperty(RESULTS_DIR, resultsDir.absolutePath)

            sourceDevice.stubOnlineDevices(SERIAL_SOURCE to 34)
            stubHealthyDevice(
                sourceDevice,
                avdName = "Pixel_8_API_34",
                gmsVersionName = "26.40.12",
                logcat = "E AppInit: test1 failure log\n",
            )

            val extension = BackupRestoreExtension { sourceDevice.session }
            val classCtx =
                childContext(
                    rootContext,
                    CLASS_ID,
                    testClass = ConstructorInjectedDummyTestClass::class.java,
                )
            val ctor = ConstructorInjectedDummyTestClass::class.java.declaredConstructors.single()
            val paramCtx = mock(ParameterContext::class.java)
            `when`(paramCtx.parameter).thenReturn(ctor.parameters[0])
            `when`(paramCtx.index).thenReturn(0)

            extension.beforeAll(classCtx)

            // Test 1: JUnit resolves constructor parameter in classCtx (DEFAULT/CLASS scope), then
            // runs beforeEach -> test (fails) -> afterEach (saves logcat & logs "Saved the logs").
            val published1 = LinkedHashMap<String, String>()
            var test1Failure: Throwable? = null
            val test1Id = "$CLASS_ID/[method:testOne()]"
            val test1Ctx =
                childContext(
                    classCtx,
                    test1Id,
                    failureProvider = { test1Failure },
                    reportSink = published1,
                    testClass = ConstructorInjectedDummyTestClass::class.java,
                )
            extension.resolveParameter(paramCtx, classCtx)
            extension.beforeEach(test1Ctx)
            test1Failure = AssertionError("test 1 assertion failed")
            extension.afterEach(test1Ctx)

            // Test 2: JUnit resolves constructor parameter in classCtx again for the next instance,
            // then runs beforeEach -> test (passes) -> afterEach.
            val published2 = LinkedHashMap<String, String>()
            val test2Id = "$CLASS_ID/[method:testTwo()]"
            val test2Ctx =
                childContext(
                    classCtx,
                    test2Id,
                    reportSink = published2,
                    testClass = ConstructorInjectedDummyTestClass::class.java,
                )
            extension.resolveParameter(paramCtx, classCtx)
            extension.beforeEach(test2Ctx)
            extension.afterEach(test2Ctx)
            extension.afterAll(classCtx)

            val detail1Id = extension.detailIdOf(test1Id)
            val record1 =
                TestDetailRecord.fromJson(
                    File(resultsDir, "suite-details/$detail1Id.json").readText()
                )
            assertEquals(listOf("Prepare"), record1.steps.map { it.name })
            assertFalse(record1.steps.single().shared)
            val hostLog1 = File(resultsDir, "suite-details/$detail1Id/host-log.txt").readText()
            assertContains(hostLog1, "Saved the logs of $SERIAL_SOURCE")

            val detail2Id = extension.detailIdOf(test2Id)
            val record2 =
                TestDetailRecord.fromJson(
                    File(resultsDir, "suite-details/$detail2Id.json").readText()
                )
            assertEquals(listOf("Prepare"), record2.steps.map { it.name })
            assertFalse(record2.steps.single().shared)
            val hostLog2 = File(resultsDir, "suite-details/$detail2Id/host-log.txt").readText()
            assertFalse(
                hostLog2.contains("Saved the logs of $SERIAL_SOURCE"),
                "Test 1 logcat-save log line must not leak into Test 2 host-log.txt",
            )
        }

    @Test
    fun postRestoreCleanupErrorsPreservePassedVerifyAndSharedBackupPassingTestsOmitSuccessStatus() =
        withDeviceProperties {
            val resultsDir = tempFolder.newFolder("results-attribution")
            writeSuiteEnvironmentJson(resultsDir, allowBackup = false)
            System.setProperty(RESULTS_DIR, resultsDir.absolutePath)

            var failUninstall = false
            sourceDevice.onShell { cmd ->
                when {
                    cmd == "pm path $PACKAGE" -> shellOutput("package:/data/app/base.apk\n")
                    cmd == "pm uninstall $PACKAGE" && failUninstall ->
                        shellOutput("Failure [DELETE_FAILED_INTERNAL_ERROR]\n", exitCode = 1)
                    cmd == "pm list instrumentation $PACKAGE" ->
                        shellOutput(
                            "instrumentation:$PACKAGE.test/${BackupActionWireProtocol.RUNNER_CLASS} (target=$PACKAGE)\n"
                        )
                    cmd.startsWith("am instrument ") ->
                        shellOutput(runnerStdout("""{"status":"ok"}"""))
                    cmd == "date +%s" -> shellOutput("1700000100\n")
                    cmd.startsWith("logcat ") -> shellOutput("I Backup: ok\n")
                    else -> ShellCommandOutput("", "", 0)
                }
            }

            val extension = BackupRestoreExtension { sourceDevice.session }
            val classCtx = childContext(rootContext, CLASS_ID)
            extension.beforeAll(classCtx)
            val sharedCtrl = newController(sourceDevice, apiLevel = 34)
            extension.track(classCtx, sharedCtrl)
            val sharedArchive = runBlocking {
                sharedCtrl.performBackup(
                    BackupTransportMode.CLOUD_ENCRYPTED,
                    tempFolder.newFolder("shared-bkp").toPath(),
                )
            }

            // 1. Passing @Test after @BeforeAll performBackup that runs neither backup nor restore
            // itself must NOT publish BackupRestore.status=SUCCESS.
            val passPublished = LinkedHashMap<String, String>()
            val passId = "$CLASS_ID/[method:passingAfterSharedBackup()]"
            val passCtx = childContext(classCtx, passId, reportSink = passPublished)
            extension.beforeEach(passCtx)
            runBlocking { sharedCtrl.runOnDevice("com.example.InspectState") }
            extension.afterEach(passCtx)
            assertNull(passPublished[BackupReportKeys.STATUS])

            // 2. Post-restore uninstallApp() IOException must NOT rewrite a passed "Verify" step,
            // must NOT run BackupRulesChecker, and must publish UNKNOWN_ERROR.
            failUninstall = true
            val cleanupPublished = LinkedHashMap<String, String>()
            var cleanupErr: Throwable? = null
            val cleanupId = "$CLASS_ID/[method:postRestoreCleanupFails()]"
            val cleanupCtx =
                childContext(
                    classCtx,
                    cleanupId,
                    failureProvider = { cleanupErr },
                    reportSink = cleanupPublished,
                )
            extension.beforeEach(cleanupCtx)
            cleanupErr =
                assertFailsWith<IOException> {
                    runBlocking {
                        sharedCtrl.performRestore(sharedArchive)
                        sharedCtrl.runOnDevice("com.example.VerifyState")
                        sharedCtrl.uninstallApp()
                    }
                }
            extension.afterEach(cleanupCtx)

            val cleanupRecord =
                TestDetailRecord.fromJson(
                    File(resultsDir, "suite-details/${extension.detailIdOf(cleanupId)}.json")
                        .readText()
                )
            assertEquals("PASSED", cleanupRecord.steps.first { it.name == "Verify" }.status)
            assertEquals("FAILED", cleanupRecord.steps.last().status)
            assertNull(cleanupRecord.callout?.likelyCause)
            assertEquals(
                BackupErrorCode.UNKNOWN_ERROR.name,
                cleanupPublished[BackupReportKeys.ERROR_CODE],
            )

            // 3. Mid-flow uninstallApp() failure (after backup, before restore) emits
            // CLEAR_DATA_FAILED.
            val midPublished = LinkedHashMap<String, String>()
            var midErr: Throwable? = null
            val midId = "$CLASS_ID/[method:midFlowUninstallFails()]"
            val midCtx =
                childContext(
                    classCtx,
                    midId,
                    failureProvider = { midErr },
                    reportSink = midPublished,
                )
            extension.beforeEach(midCtx)
            val midCtrl = newController(sourceDevice, apiLevel = 34)
            extension.track(midCtx, midCtrl)
            midErr =
                assertFailsWith<IOException> {
                    runBlocking {
                        midCtrl.performBackup(
                            BackupTransportMode.CLOUD_ENCRYPTED,
                            tempFolder.newFolder("mid-bkp").toPath(),
                        )
                        midCtrl.uninstallApp()
                    }
                }
            extension.afterEach(midCtx)
            extension.afterAll(classCtx)

            assertEquals(
                BackupErrorCode.CLEAR_DATA_FAILED.name,
                midPublished[BackupReportKeys.ERROR_CODE],
            )
        }

    @Test
    fun multiLevelNestedClassesInheritSharedStepsSerialsArchiveAndHostLogs() =
        withDeviceProperties {
            val resultsDir = tempFolder.newFolder("results-nested")
            System.setProperty(RESULTS_DIR, resultsDir.absolutePath)
            stubHealthyDevice(sourceDevice, avdName = "Pixel_6_API_31")
            stubHealthyDevice(targetDevice, avdName = "Pixel_8_API_34")

            val extension = BackupRestoreExtension { sourceDevice.session }
            val outerCtx = childContext(rootContext, CLASS_ID)
            val middleId = "$CLASS_ID/[nested-class:Middle]"
            val middleCtx = childContext(outerCtx, middleId)
            val innerId = "$middleId/[nested-class:Inner]"
            val innerCtx = childContext(middleCtx, innerId)
            val leafId = "$innerId/[method:nestedTest()]"
            val leafCtx = childContext(innerCtx, leafId)

            extension.beforeAll(outerCtx)
            val sharedSource =
                newController(
                    sourceDevice,
                    apiLevel = 31,
                    facts = DeviceFacts("Pixel 6", 31, "Google APIs", "26.39.33"),
                )
            extension.track(outerCtx, sharedSource)
            Logger.getLogger("androidx.test.backup.host.Outer").info("OUTER_ANCESTOR_LOG")
            val archive = runBlocking {
                sharedSource.performBackup(
                    BackupTransportMode.DEVICE_TO_DEVICE,
                    tempFolder.newFolder("nested-bkp").toPath(),
                )
            }

            extension.beforeAll(middleCtx)
            Logger.getLogger("androidx.test.backup.host.Middle").info("MIDDLE_ANCESTOR_LOG")

            extension.beforeAll(innerCtx)
            Logger.getLogger("androidx.test.backup.host.Inner").info("INNER_ANCESTOR_LOG")

            extension.beforeEach(leafCtx)
            val targetCtrl =
                newController(
                    targetDevice,
                    apiLevel = 34,
                    facts = DeviceFacts("Pixel 8", 34, "Google APIs", "26.40.12"),
                )
            extension.track(leafCtx, targetCtrl)
            runBlocking { targetCtrl.performRestore(archive) }
            extension.afterEach(leafCtx)

            extension.afterAll(innerCtx)
            extension.afterAll(middleCtx)
            extension.afterAll(outerCtx)

            val detailId = extension.detailIdOf(leafId)
            val record =
                TestDetailRecord.fromJson(
                    File(resultsDir, "suite-details/$detailId.json").readText()
                )
            assertEquals(
                listOf("source" to SERIAL_SOURCE, "target" to SERIAL_TARGET),
                record.actors.map { it.id to it.serial },
            )
            assertEquals(
                listOf("Backup" to true, "Restore" to false),
                record.steps.map { it.name to it.shared },
            )
            assertEquals("Backup file summary", record.artifacts.single().name)
            val hostLog = File(resultsDir, "suite-details/$detailId/host-log.txt").readText()
            assertContains(hostLog, "OUTER_ANCESTOR_LOG")
            assertContains(hostLog, "MIDDLE_ANCESTOR_LOG")
            assertContains(hostLog, "INNER_ANCESTOR_LOG")
        }

    @Test
    fun sourceLocationNeverFabricatesFileNameAndIgnoresLibraryFailureFrames() =
        withDeviceProperties {
            val resultsDir = tempFolder.newFolder("results-source-location")
            System.setProperty(RESULTS_DIR, resultsDir.absolutePath)
            stubHealthyDevice(sourceDevice)

            val extension = BackupRestoreExtension { sourceDevice.session }
            val classCtx = childContext(rootContext, CLASS_ID)
            extension.beforeAll(classCtx)

            // 1. No controller call and no testClass stack frame -> file is null, never fabricated.
            val noFrameId = "$CLASS_ID/[method:testOne()]"
            val noFrameCtx = childContext(classCtx, noFrameId)
            extension.beforeEach(noFrameCtx)
            extension.afterEach(noFrameCtx)

            val noFrameRecord =
                TestDetailRecord.fromJson(
                    File(resultsDir, "suite-details/${extension.detailIdOf(noFrameId)}.json")
                        .readText()
                )
            assertNull(noFrameRecord.sourceLocation?.file)
            assertEquals(
                ConfiguredDummyTestClass::class.java.name,
                noFrameRecord.sourceLocation?.className,
            )
            assertEquals("testOne", noFrameRecord.sourceLocation?.methodName)

            // 2. Failure carrying only library stack frames falls back to ownFirstCallFrame rather
            // than blaming com.android.backup.* at line 9999.
            var libErr: Throwable? = null
            val libFailId = "$CLASS_ID/[method:testTwo()]"
            val libFailCtx =
                childContext(
                    classCtx,
                    libFailId,
                    failureProvider = { libErr },
                    testClass = BackupReportCaptureTest::class.java,
                )
            extension.beforeEach(libFailCtx)
            val ctrl = newController(sourceDevice, apiLevel = 34)
            extension.track(libFailCtx, ctrl)
            runBlocking { ctrl.clearAppData() }
            libErr =
                IOException("library transport failure").apply {
                    stackTrace =
                        arrayOf(
                            StackTraceElement(
                                "com.android.backup.BackupServiceImpl",
                                "backup",
                                "BackupServiceImpl.kt",
                                9999,
                            )
                        )
                }
            extension.afterEach(libFailCtx)
            extension.afterAll(classCtx)

            val libFailRecord =
                TestDetailRecord.fromJson(
                    File(resultsDir, "suite-details/${extension.detailIdOf(libFailId)}.json")
                        .readText()
                )
            assertEquals("BackupReportCaptureTest.kt", libFailRecord.sourceLocation?.file)
            assertNotEquals(9999, libFailRecord.sourceLocation?.line)
            assertTrue((libFailRecord.sourceLocation?.line ?: 0) > 0)
        }

    @Test
    fun coversAsyncUnwrapStepAttributionClosedCaptureRolePreserveCancellationAndLogDrop() =
        withDeviceProperties {
            val resultsDir = tempFolder.newFolder("results-r2-coverage")
            writeSuiteEnvironmentJson(resultsDir, allowBackup = false)
            System.setProperty(RESULTS_DIR, resultsDir.absolutePath)
            stubHealthyDevice(sourceDevice)

            val extension = BackupRestoreExtension { sourceDevice.session }

            // 1. (R2-A-M1) Java *Async ExecutionException wrapping BackupRestoreException(GMSCORE)
            // unwraps to GMSCORE_OUTDATED_OR_MISSING and skips BackupRulesChecker.
            val asyncClassId = "$CLASS_ID-async-unwrap"
            val asyncClassCtx = childContext(rootContext, asyncClassId)
            extension.beforeAll(asyncClassCtx)

            val gmsAsyncPublished = LinkedHashMap<String, String>()
            var gmsAsyncErr: Throwable? = null
            val gmsTestId = "$asyncClassId/[method:bmgrInitFails()]"
            val gmsTestCtx =
                childContext(
                    asyncClassCtx,
                    gmsTestId,
                    failureProvider = { gmsAsyncErr },
                    reportSink = gmsAsyncPublished,
                )
            extension.beforeEach(gmsTestCtx)
            val gmsCtrl =
                newController(
                    sourceDevice,
                    apiLevel = 34,
                    backupService =
                        mock(BackupService::class.java) { inv ->
                            if (inv.method.name == "backup") {
                                throw BackupRestoreException(
                                    BackupErrorCode.GMSCORE_OUTDATED_OR_MISSING,
                                    "GMS Core version too old",
                                )
                            } else {
                                RETURNS_DEFAULTS.answer(inv)
                            }
                        },
                )
            extension.track(gmsTestCtx, gmsCtrl)
            gmsAsyncErr =
                assertFailsWith<ExecutionException> {
                    gmsCtrl
                        .performBackupAsync(
                            BackupTransportMode.CLOUD_ENCRYPTED,
                            tempFolder.newFolder("gms-async-out").toPath(),
                        )
                        .get()
                }
            extension.afterEach(gmsTestCtx)

            assertEquals(
                BackupErrorCode.GMSCORE_OUTDATED_OR_MISSING.name,
                gmsAsyncPublished[BackupReportKeys.ERROR_CODE],
            )
            val gmsRecord =
                TestDetailRecord.fromJson(
                    File(resultsDir, "suite-details/${extension.detailIdOf(gmsTestId)}.json")
                        .readText()
                )
            assertNull(gmsRecord.callout?.likelyCause)
            assertEquals("STEP_FAILURE", gmsRecord.callout?.kind)

            // 2. (R2-A-M1 & R2-A-M2) Caught earlier step failure before post-restore AssertionError
            // wrapped in ExecutionException adds synthetic Verify (FAILED) rather than blaming the
            // caught step, and unwraps AssertionError to ASSERTION_FAILURE.
            val caughtPublished = LinkedHashMap<String, String>()
            var caughtErr: Throwable? = null
            val caughtTestId = "$asyncClassId/[method:testOne()]"
            val caughtTestCtx =
                childContext(
                    asyncClassCtx,
                    caughtTestId,
                    failureProvider = { caughtErr },
                    reportSink = caughtPublished,
                )
            extension.beforeEach(caughtTestCtx)
            var failFirstClear = true
            sourceDevice.onShell { cmd ->
                when {
                    cmd == "pm clear $PACKAGE" && failFirstClear -> {
                        failFirstClear = false
                        shellOutput("Failed\n", exitCode = 1)
                    }
                    cmd == "pm clear $PACKAGE" -> shellOutput("Success\n")
                    cmd == "date +%s" -> shellOutput("1700000100\n")
                    cmd.startsWith("logcat ") -> shellOutput("I Backup: ok\n")
                    else -> ShellCommandOutput("", "", 0)
                }
            }
            val caughtCtrl = newController(sourceDevice, apiLevel = 34)
            extension.track(caughtTestCtx, caughtCtrl)
            val bkpArchive = runBlocking {
                try {
                    caughtCtrl.clearAppData()
                } catch (_: IOException) {
                    // Caught by test before backup/restore.
                }
                val archive =
                    caughtCtrl.performBackup(
                        BackupTransportMode.CLOUD_ENCRYPTED,
                        tempFolder.newFolder("caught-bkp").toPath(),
                    )
                caughtCtrl.performRestore(archive)
                archive
            }
            caughtErr = ExecutionException(AssertionError("post-restore state mismatch"))
            extension.afterEach(caughtTestCtx)
            extension.afterAll(asyncClassCtx)

            val caughtRecord =
                TestDetailRecord.fromJson(
                    File(resultsDir, "suite-details/${extension.detailIdOf(caughtTestId)}.json")
                        .readText()
                )
            assertEquals("ASSERTION_FAILURE", caughtRecord.callout?.kind)
            assertEquals("Verify", caughtRecord.steps.last().name)
            assertEquals("FAILED", caughtRecord.steps.last().status)
            assertEquals(
                BackupErrorCode.VERIFICATION_FAILED.name,
                caughtPublished[BackupReportKeys.ERROR_CODE],
            )

            // 3. (R2-A-M2) @BeforeAll runOnDevice(...).getOrThrow() failure marks the shared Setup
            // step as FAILED in afterAll; and a 0-step @Test failure after @BeforeAll backup does
            // not re-emit BACKUP_FAILED or mark the shared Backup step as FAILED.
            stubHealthyDevice(sourceDevice)
            val sharedBackupClassId = "$CLASS_ID-shared-bkp-zero"
            val sharedBackupClassCtx = childContext(rootContext, sharedBackupClassId)
            extension.beforeAll(sharedBackupClassCtx)
            val sharedBkpCtrl = newController(sourceDevice, apiLevel = 34)
            extension.track(sharedBackupClassCtx, sharedBkpCtrl)
            runBlocking {
                sharedBkpCtrl.performBackup(
                    BackupTransportMode.CLOUD_ENCRYPTED,
                    tempFolder.newFolder("shared-zero-bkp").toPath(),
                )
            }
            val zeroStepPublished = LinkedHashMap<String, String>()
            var zeroStepErr: Throwable? = null
            val zeroStepId = "$sharedBackupClassId/[method:testTwo()]"
            val zeroStepCtx =
                childContext(
                    sharedBackupClassCtx,
                    zeroStepId,
                    failureProvider = { zeroStepErr },
                    reportSink = zeroStepPublished,
                )
            extension.beforeEach(zeroStepCtx)
            // Merge child controller with explicitRole="source" to verify
            // sharedBkpCtrl.explicitRole
            // is not mutated (R2-A-M4).
            val childSourceCtrl = newController(sourceDevice, apiLevel = 34, role = "source")
            extension.track(zeroStepCtx, childSourceCtrl)
            assertEquals("", sharedBkpCtrl.explicitRole)
            zeroStepErr = IllegalStateException("unrelated test body failure with 0 own steps")
            extension.afterEach(zeroStepCtx)
            extension.afterAll(sharedBackupClassCtx)

            assertNull(zeroStepPublished[BackupReportKeys.STATUS])
            val zeroStepRecord =
                TestDetailRecord.fromJson(
                    File(resultsDir, "suite-details/${extension.detailIdOf(zeroStepId)}.json")
                        .readText()
                )
            assertEquals("PASSED", zeroStepRecord.steps.single().status)

            // Also verify @BeforeAll getOrThrow() failure marks the class's own Setup step FAILED.
            sourceDevice.onShell { cmd ->
                when {
                    cmd == "pm list instrumentation $PACKAGE" ->
                        shellOutput(
                            "instrumentation:$PACKAGE.test/${BackupActionWireProtocol.RUNNER_CLASS} (target=$PACKAGE)\n"
                        )
                    cmd.startsWith("am instrument ") ->
                        shellOutput(
                            instrumentOutput(
                                """{"isSuccess":false,"errorMessage":"beforeAll seed failed"}"""
                            )
                        )
                    cmd == "date +%s" -> shellOutput("1700000100\n")
                    cmd.startsWith("logcat ") -> shellOutput("E Seed: fail\n")
                    else -> ShellCommandOutput("", "", 0)
                }
            }
            var beforeAllActionErr: Throwable? = null
            val beforeAllFailClassId = "$CLASS_ID-beforeAllAction"
            val beforeAllFailClassCtx =
                childContext(
                    rootContext,
                    beforeAllFailClassId,
                    failureProvider = { beforeAllActionErr },
                )
            extension.beforeAll(beforeAllFailClassCtx)
            val beforeAllCtrl = newController(sourceDevice, apiLevel = 34)
            extension.track(beforeAllFailClassCtx, beforeAllCtrl)
            beforeAllActionErr =
                assertFailsWith<IOException> {
                    runBlocking { beforeAllCtrl.runOnDevice("com.example.Seed").getOrThrow() }
                }
            extension.afterAll(beforeAllFailClassCtx)
            val beforeAllRecord =
                TestDetailRecord.fromJson(
                    File(
                            resultsDir,
                            "suite-details/${extension.detailIdOf(beforeAllFailClassId)}.json",
                        )
                        .readText()
                )
            assertEquals("FAILED", beforeAllRecord.steps.single().status)

            // 4. (R2-A-M3) Closed CaptureContextElement suppresses single-leaf fallback to Test 2,
            // and recordArchive after close() deletes its temp copy immediately.
            val closedCapture =
                TestExecutionCapture(
                    start = java.time.Instant.now(),
                    isShared = false,
                    testClassName = "com.example.Test",
                    testMethodName = "closed",
                    parent = null,
                )
            val openCapture =
                TestExecutionCapture(
                    start = java.time.Instant.now(),
                    isShared = false,
                    testClassName = "com.example.Test",
                    testMethodName = "open",
                    parent = null,
                )
            try {
                val closedCtx = closedCapture.coroutineContext()
                closedCapture.close()
                ActiveCaptureContext.clearCurrentThread()
                runBlocking(closedCtx) {
                    assertNull(ActiveCaptureContext.current())
                }
                val tempBefore =
                    File(System.getProperty("java.io.tmpdir"))
                        .listFiles { _, name ->
                            name.startsWith("backup_raw_archive_") &&
                                name.endsWith("_closed_test.zip")
                        }
                        .orEmpty()
                        .toSet()
                closedCapture.recordArchive(
                    fileName = "closed_test.zip",
                    summaryValues = mapOf("app_backup" to "64 B"),
                    rawSourceFile = bkpArchive,
                    includeRawCopy = true,
                )
                val tempAfter =
                    File(System.getProperty("java.io.tmpdir"))
                        .listFiles { _, name ->
                            name.startsWith("backup_raw_archive_") &&
                                name.endsWith("_closed_test.zip")
                        }
                        .orEmpty()
                        .toSet()
                assertEquals(tempBefore, tempAfter)
            } finally {
                openCapture.close()
            }

            // 5. (R2-A-M5) DeviceFactsReader.read rethrows CancellationException.
            sourceDevice.onShell { cmd ->
                if (cmd == "pm list packages --show-versioncode com.google.android.gms") {
                    throw CancellationException("cancelled during device facts read")
                } else {
                    ShellCommandOutput("", "", 0)
                }
            }
            assertFailsWith<CancellationException> {
                runBlocking {
                    DeviceFactsReader.read(
                        BackupDeviceShell(sourceDevice.session, SERIAL_SOURCE),
                        34,
                    )
                }
            }

            // 6. (R2-A-M6) HostLogCollector.appendRaw bounded newline scan preserves 256 KB tail
            // on long single-line inputs and does not split UTF-16 surrogate pairs.
            val collector = HostLogCollector()
            val surrogatePair = "\uD83D\uDE00" // 😀 (2 UTF-16 chars, 4 UTF-8 bytes)
            val longSingleLine = buildString {
                repeat(270_000) { append(surrogatePair) }
                append("SINGLE_LINE_TAIL_MARKER\n")
            }
            collector.appendRaw(longSingleLine)
            assertTrue(collector.isOverflowTruncated())
            val snapText = collector.snapshotText()
            assertContains(snapText, "SINGLE_LINE_TAIL_MARKER")
            assertFalse(snapText.first().isLowSurrogate())
            val bounded = collector.toBoundedUtf8()
            assertTrue(bounded.truncated)
            assertTrue(bounded.bytes.decodeToString().startsWith(TRUNCATION_NOTICE))
            assertTrue(bounded.bytes.size <= HostLogCollector.MAX_HOST_LOG_BYTES)
        }

    @BackupRestoreConfig(applicationId = PACKAGE)
    private class ConstructorInjectedDummyTestClass(val controller: BackupRestoreController)

    private fun newController(
        adbDevice: FakeAdbDevice,
        apiLevel: Int,
        role: String = "",
        backupService: BackupService = fakeZipBackupService(mapOf("app_backup" to ByteArray(64))),
        telemetrySink: MutableMap<String, String>? = null,
        facts: DeviceFacts? = null,
    ): BackupRestoreControllerImpl =
        BackupRestoreControllerImpl(
                adbSession = adbDevice.session,
                serialNumber = adbDevice.serialNumber,
                apiLevel = apiLevel,
                applicationId = PACKAGE,
                telemetryPublisher = telemetrySink?.let { sink -> { k, v -> sink[k] = v } },
                backupServiceProvider = { backupService },
            )
            .also {
                it.explicitRole = role
                it.cachedDeviceFacts = facts
            }

    private fun stubHealthyDevice(
        adbDevice: FakeAdbDevice,
        avdName: String = "Pixel_6_API_31",
        gmsVersionName: String? = "26.39.33",
        hasPlayStore: Boolean = false,
        logcat: String = "I Backup: healthy\n",
    ) {
        adbDevice.stubReadonlyProperties(
            mapOf(
                "ro.product.model" to "sdk_gphone64_arm64",
                "ro.boot.qemu.avd_name" to avdName,
            )
        )
        val gmsVersionCode =
            gmsVersionName?.split('.')?.let { (maj, min, pat) ->
                maj.toLong() * 10_000_000L + min.toLong() * 100_000L + pat.toLong() * 1_000L
            }
        var restoreActive = false
        val populatePayload = """{"status":"success"}"""
        adbDevice.onShell { cmd ->
            when {
                cmd == "pm list packages --show-versioncode com.google.android.gms" ->
                    shellOutput(
                        gmsVersionCode
                            ?.let { "package:com.google.android.gms versionCode:$it\n" }
                            .orEmpty()
                    )
                cmd == "pm path com.android.vending" ->
                    shellOutput(if (hasPlayStore) "package:/system/vending.apk\n" else "")
                cmd == "pm clear $PACKAGE" || cmd.startsWith("pm install ") ->
                    shellOutput("Success\n")
                cmd.startsWith("pm list packages -f") ->
                    shellOutput("package:/data/app/base.apk=$PACKAGE\n")
                cmd == "pm list instrumentation $PACKAGE" ->
                    shellOutput(
                        "instrumentation:$PACKAGE.test/${BackupActionWireProtocol.RUNNER_CLASS} " +
                            "(target=$PACKAGE)\n"
                    )
                cmd.startsWith("am instrument ") -> shellOutput(runnerStdout(populatePayload))
                cmd == "bmgr list transports" -> shellOutput("  * $LOCAL_TRANSPORT\n")
                cmd == "dumpsys backup" -> {
                    restoreActive = !restoreActive
                    shellOutput("Restore in progress: $restoreActive\n")
                }
                cmd == "date +%s" -> shellOutput("1700000100\n")
                cmd.startsWith("logcat ") -> shellOutput(logcat)
                else -> bmgrSuccessOutput(cmd) ?: ShellCommandOutput("", "", 0)
            }
        }
    }

    private fun fakeBackupService(
        backupResult: BackupResult = BackupResult.Success,
        restoreResult: BackupResult = BackupResult.Success,
    ): BackupService =
        mock(BackupService::class.java) { invocation ->
            when (invocation.method.name) {
                "backup" -> backupResult
                "restore" -> restoreResult
                else -> RETURNS_DEFAULTS.answer(invocation)
            }
        }

    private fun fakeZipBackupService(
        entries: Map<String, ByteArray>,
        emitProgress: Boolean = false,
    ): BackupService =
        mock(BackupService::class.java) { invocation ->
            when (invocation.method.name) {
                "backup" -> {
                    val outPath = invocation.arguments[3] as Path
                    val listener = invocation.arguments[4] as? BackupProgressListener
                    if (emitProgress && listener != null) {
                        runBlocking {
                            listener.onStep(
                                BackupProgressListener.Step(1, 2, "Initializing transport")
                            )
                            listener.onStep(
                                BackupProgressListener.Step(2, 2, "Pulling backup archive")
                            )
                        }
                    }
                    ZipOutputStream(Files.newOutputStream(outPath)).use { zos ->
                        for ((name, data) in entries) {
                            zos.putNextEntry(ZipEntry(name))
                            zos.write(data)
                            zos.closeEntry()
                        }
                    }
                    BackupResult.Success
                }
                "restore" -> {
                    val listener = invocation.arguments[2] as? BackupProgressListener
                    if (emitProgress && listener != null) {
                        runBlocking {
                            listener.onStep(
                                BackupProgressListener.Step(1, 2, "Pushing restore archive")
                            )
                            listener.onStep(
                                BackupProgressListener.Step(2, 2, "Restoring package data")
                            )
                        }
                    }
                    BackupResult.Success
                }
                else -> RETURNS_DEFAULTS.answer(invocation)
            }
        }

    private fun childContext(
        parent: ExtensionContext,
        uniqueId: String,
        failureProvider: () -> Throwable? = { null },
        reportSink: MutableMap<String, String>? = null,
        testClass: Class<*> = ConfiguredDummyTestClass::class.java,
    ): ExtensionContext =
        createChildContext(
            parent = parent,
            uniqueId = uniqueId,
            stores = stores,
            failureProvider = failureProvider,
            reportSink = reportSink,
            testClass = testClass,
        )

    @BackupRestoreConfig(applicationId = PACKAGE) private class ConfiguredDummyTestClass

    private companion object {
        const val PACKAGE = "com.example.journaling"
        const val SERIAL_SOURCE = "emulator-5554"
        const val SERIAL_TARGET = "emulator-5556"
        const val PROP_CAPTURE_LOGCAT_ON_PASS = "androidx.test.backup.report.captureLogcatOnPass"
        const val CLASS_ID = "[engine:junit-jupiter]/[class:com.example.journaling.MigrationTests]"
        const val TEST_ID =
            "$CLASS_ID/[method:restoresData(androidx.test.backup.host.BackupRestoreController)]"
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
