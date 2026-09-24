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

import com.android.adblib.AdbDeviceServices
import com.android.adblib.AdbSession
import com.android.adblib.AdbSessionHost
import com.android.adblib.DeviceSelector
import com.android.adblib.ShellCollector
import com.android.adblib.TextShellCollector
import com.android.adblib.deviceCacheProvider
import java.io.IOException
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.any
import org.mockito.Mockito.anyBoolean
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@Suppress("CheckResult")
class BackupRestoreControllerImplTest {

    @get:Rule val tempFolder = TemporaryFolder()

    private lateinit var mockSession: AdbSession
    private lateinit var mockDeviceServices: AdbDeviceServices

    @Suppress("UNCHECKED_CAST")
    @Before
    fun setUp() {
        mockSession = mock(AdbSession::class.java)
        mockDeviceServices = mock(AdbDeviceServices::class.java)
        `when`(mockSession.deviceServices).thenReturn(mockDeviceServices)
        `when`(mockDeviceServices.session).thenReturn(mockSession)

        val mockHost = mock(AdbSessionHost::class.java)
        `when`(mockSession.host).thenReturn(mockHost)

        val mockProp = mock(AdbSessionHost.Property::class.java) as AdbSessionHost.Property<Any>
        `when`(mockHost.getPropertyValue(any(AdbSessionHost.Property::class.java) ?: mockProp))
            .thenAnswer { invocation ->
                val prop = invocation.getArgument(0) as AdbSessionHost.Property<*>
                prop.defaultValue
            }

        val mockLoggerFactory = mock(com.android.adblib.AdbLoggerFactory::class.java)
        `when`(mockHost.loggerFactory).thenReturn(mockLoggerFactory)
        val mockAdbLogger = mock(com.android.adblib.AdbLogger::class.java)
        `when`(mockLoggerFactory.createLogger(any(Class::class.java) ?: Any::class.java))
            .thenReturn(mockAdbLogger)
        `when`(mockAdbLogger.minLevel).thenReturn(com.android.adblib.AdbLogger.Level.INFO)

        val mockHostServices = mock(com.android.adblib.AdbHostServices::class.java)
        `when`(mockSession.hostServices).thenReturn(mockHostServices)
        `when`(mockHostServices.session).thenReturn(mockSession)
        runBlocking {
            `when`(
                    mockHostServices.features(
                        any(DeviceSelector::class.java) ?: DeviceSelector.any()
                    )
                )
                .thenReturn(emptyList())
            `when`(mockHostServices.hostFeatures()).thenReturn(emptyList())
        }

        val mockCache = mock(com.android.adblib.CoroutineScopeCache::class.java)
        `when`(mockSession.cache).thenReturn(mockCache)

        val mockDeviceCacheProvider = mock(com.android.adblib.DeviceCacheProvider::class.java)
        `when`(mockSession.deviceCacheProvider).thenReturn(mockDeviceCacheProvider)
        `when`(mockSession.scope)
            .thenReturn(
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
            )
    }

    @Test
    fun testPropertiesExposedCorrectly() {
        val device =
            BackupRestoreControllerImpl(mockSession, "emulator-5554", 34, "com.example.app")
        assertEquals("emulator-5554", device.serialNumber)
        assertEquals(34, device.apiLevel)
        assertEquals("com.example.app", device.applicationId)
    }

    @Test
    fun testRunOnDeviceWithNormalPayload() = runBlocking {
        val device =
            BackupRestoreControllerImpl(mockSession, "emulator-5554", 34, "com.example.app")

        // Mock ADB shell output flow returning clean JSON result
        val dq = Char(34).toString()
        val bs = Char(92).toString()
        val jsonString =
            "{" +
                dq +
                "isSuccess" +
                dq +
                ":true," +
                dq +
                "payloadJson" +
                dq +
                ":" +
                dq +
                "{" +
                bs +
                dq +
                "user_id" +
                bs +
                dq +
                ":" +
                bs +
                dq +
                "123" +
                bs +
                dq +
                "}" +
                dq +
                "}"
        val mockStdout = "BACKUP_RESTORE_RESULT: " + jsonString + Char(10).toString()

        `when`(
                mockDeviceServices.shell(
                    any(DeviceSelector::class.java) ?: DeviceSelector.any(),
                    any(String::class.java) ?: "",
                    (any(ShellCollector::class.java) as? ShellCollector<*>) ?: TextShellCollector(),
                    any(),
                    any(),
                    any(Duration::class.java) ?: Duration.ofSeconds(1),
                    anyInt(),
                    anyBoolean(),
                    anyBoolean(),
                )
            )
            .thenAnswer { invocation ->
                val cmd = invocation.getArgument(1) as String
                if (cmd.contains("am instrument")) {
                    flowOf(com.android.adblib.ShellCommandOutput(mockStdout, "", 0))
                } else {
                    flowOf("")
                }
            }

        val result = device.runOnDevice("com.example.MyAction", mapOf("user_id" to "123"))

        assertTrue(result is BackupActionResult.Success)
        val successResult = result as BackupActionResult.Success
        assertEquals("123", successResult.data["user_id"])
    }

    @Test
    fun testRunOnDeviceWithErrorPayloadAndStackTrace() = runBlocking {
        val device =
            BackupRestoreControllerImpl(mockSession, "emulator-5554", 34, "com.example.app")

        val mockStdout =
            "BACKUP_RESTORE_RESULT: " +
                """{"isSuccess":false,"errorMessage":"Something broke","stackTrace":"at MyAction.kt:15"}""" +
                Char(10).toString()

        `when`(
                mockDeviceServices.shell(
                    any(DeviceSelector::class.java) ?: DeviceSelector.any(),
                    any(String::class.java) ?: "",
                    (any(ShellCollector::class.java) as? ShellCollector<*>) ?: TextShellCollector(),
                    any(),
                    any(),
                    any(Duration::class.java) ?: Duration.ofSeconds(1),
                    anyInt(),
                    anyBoolean(),
                    anyBoolean(),
                )
            )
            .thenAnswer { invocation ->
                val cmd = invocation.getArgument(1) as String
                if (cmd.contains("am instrument")) {
                    flowOf(com.android.adblib.ShellCommandOutput(mockStdout, "", 0))
                } else {
                    flowOf("")
                }
            }

        val result = device.runOnDevice("com.example.MyAction", emptyMap())

        assertTrue(result is BackupActionResult.Failure)
        val failureResult = result as BackupActionResult.Failure
        assertEquals("Something broke", failureResult.errorMessage)
        assertEquals("at MyAction.kt:15", failureResult.stackTrace)
    }

    /**
     * A failed assertion inside AssertStorageAction shows up as `status=failure` in the payload.
     * The envelope here reports `isSuccess=true`, as an older runner does, so this pins that the
     * host reads the payload rather than trusting the envelope alone.
     */
    @Test
    fun testRunOnDeviceSurfacesInBandActionFailure() = runBlocking {
        val device =
            BackupRestoreControllerImpl(mockSession, "emulator-5554", 34, "com.example.app")

        mockInstrumentationStdout(
            runnerStdout("""{"status":"failure","error":"Expected 'a' but found 'b'"}""")
        )

        val result = device.runOnDevice("com.example.MyAction", emptyMap())

        assertTrue(result is BackupActionResult.Failure)
        assertEquals(
            "Expected 'a' but found 'b'",
            (result as BackupActionResult.Failure).errorMessage,
        )
    }

    /** A failure with no error message still fails, naming the action so the report is usable. */
    @Test
    fun testRunOnDeviceSurfacesInBandFailureWithoutMessage() = runBlocking {
        val device =
            BackupRestoreControllerImpl(mockSession, "emulator-5554", 34, "com.example.app")

        mockInstrumentationStdout(runnerStdout("""{"status":"failure"}"""))

        val result = device.runOnDevice("com.example.MyAction", emptyMap())

        assertTrue(result is BackupActionResult.Failure)
        assertTrue(
            (result as BackupActionResult.Failure).errorMessage.contains("com.example.MyAction")
        )
    }

    /**
     * Only the recognized failure value fails an action.
     *
     * `status` predates this convention and custom actions publish their own vocabulary through it
     * — the AddressBook sample reports `status=verified` to mean "all checks passed". Treating
     * every non-success value as a failure broke that app, so unrecognized values must pass.
     */
    @Test
    fun testRunOnDeviceAcceptsUnrecognizedActionStatus() = runBlocking {
        val device =
            BackupRestoreControllerImpl(mockSession, "emulator-5554", 34, "com.example.app")

        mockInstrumentationStdout(runnerStdout("""{"status":"verified","authVerified":"true"}"""))

        val result = device.runOnDevice("com.example.MyAction", emptyMap())
        assertTrue(result is BackupActionResult.Success)
        assertEquals("verified", (result as BackupActionResult.Success).data["status"])
    }

    @Test
    fun testRunOnDeviceAcceptsInBandActionSuccess() = runBlocking {
        val device =
            BackupRestoreControllerImpl(mockSession, "emulator-5554", 34, "com.example.app")

        mockInstrumentationStdout(runnerStdout("""{"status":"success","rows":"3"}"""))

        val result = device.runOnDevice("com.example.MyAction", emptyMap())

        assertTrue(result is BackupActionResult.Success)
        assertEquals("3", (result as BackupActionResult.Success).data["rows"])
    }

    /** Custom actions are not required to report a status; silence still means success. */
    @Test
    fun testRunOnDeviceTreatsAbsentStatusAsSuccess() = runBlocking {
        val device =
            BackupRestoreControllerImpl(mockSession, "emulator-5554", 34, "com.example.app")

        mockInstrumentationStdout(runnerStdout("""{"rows":"3"}"""))

        assertTrue(
            device.runOnDevice("com.example.MyAction", emptyMap()) is BackupActionResult.Success
        )
    }

    /**
     * An action that reports an error but forgets the status still fails.
     *
     * Mirrors `androidx.test.backup.BackupDeviceActionResult.isSuccess`; without this the host
     * would report a Success carrying the error text in its data map.
     */
    @Test
    fun testRunOnDeviceSurfacesErrorWithoutStatus() = runBlocking {
        val device =
            BackupRestoreControllerImpl(mockSession, "emulator-5554", 34, "com.example.app")

        mockInstrumentationStdout(runnerStdout("""{"error":"Disk full"}"""))

        val result = device.runOnDevice("com.example.MyAction", emptyMap())

        assertTrue(result is BackupActionResult.Failure)
        assertEquals("Disk full", (result as BackupActionResult.Failure).errorMessage)
    }

    /**
     * An empty error without a status is not a failure.
     *
     * The AddressBook sample returns an empty `restore_credential_error` alongside a passing
     * verification, so the presence of the key alone cannot decide the outcome.
     */
    @Test
    fun testRunOnDeviceIgnoresEmptyErrorWithoutStatus() = runBlocking {
        val device =
            BackupRestoreControllerImpl(mockSession, "emulator-5554", 34, "com.example.app")

        mockInstrumentationStdout(runnerStdout("""{"error":""}"""))

        assertTrue(
            device.runOnDevice("com.example.MyAction", emptyMap()) is BackupActionResult.Success
        )
    }

    /**
     * The runner reports the action's own verdict and its error, and still forwards the payload.
     * The host reads the payload for the specific message.
     */
    @Test
    fun testRunOnDeviceReadsPayloadWhenRunnerReportsFailure() = runBlocking {
        val device =
            BackupRestoreControllerImpl(mockSession, "emulator-5554", 34, "com.example.app")

        mockInstrumentationStdout(
            runnerStdout(
                """{"status":"failure","error":"Expected 'a' but found 'b'"}""",
                isSuccess = false,
                errorMessage = "Expected 'a' but found 'b'",
            )
        )

        val result = device.runOnDevice("com.example.MyAction", emptyMap())

        assertTrue(result is BackupActionResult.Failure)
        assertEquals(
            "Expected 'a' but found 'b'",
            (result as BackupActionResult.Failure).errorMessage,
        )
    }

    /**
     * With no error on the action, the runner has no message to report; the payload still lets the
     * host name the action instead of falling back to "Unknown device failure.".
     */
    @Test
    fun testRunOnDeviceNamesActionWhenRunnerReportsFailureWithoutMessage() = runBlocking {
        val device =
            BackupRestoreControllerImpl(mockSession, "emulator-5554", 34, "com.example.app")

        mockInstrumentationStdout(runnerStdout("""{"status":"failure"}""", isSuccess = false))

        val result = device.runOnDevice("com.example.MyAction", emptyMap())

        assertTrue(result is BackupActionResult.Failure)
        assertTrue(
            (result as BackupActionResult.Failure).errorMessage.contains("com.example.MyAction")
        )
    }

    /** A failure reported by the runner is never downgraded by a payload that looks successful. */
    @Test
    fun testRunOnDeviceRunnerFailureWinsOverSuccessfulPayload() = runBlocking {
        val device =
            BackupRestoreControllerImpl(mockSession, "emulator-5554", 34, "com.example.app")

        mockInstrumentationStdout(
            runnerStdout("""{"rows":"3"}""", isSuccess = false, errorMessage = "Runner said no")
        )

        val result = device.runOnDevice("com.example.MyAction", emptyMap())

        assertTrue(result is BackupActionResult.Failure)
        assertEquals("Runner said no", (result as BackupActionResult.Failure).errorMessage)
    }

    /** Wraps an action payload in the envelope the on-device runner prints to stdout. */
    private fun runnerStdout(
        payloadJson: String,
        isSuccess: Boolean = true,
        errorMessage: String? = null,
    ): String {
        val envelope = buildJsonObject {
            put("isSuccess", isSuccess)
            errorMessage?.let { put("errorMessage", it) }
            put("payloadJson", payloadJson)
        }
            .toString()
        return "BACKUP_RESTORE_RESULT: $envelope\n"
    }

    private fun mockInstrumentationStdout(stdout: String) {
        `when`(
                mockDeviceServices.shell(
                    any(DeviceSelector::class.java) ?: DeviceSelector.any(),
                    any(String::class.java) ?: "",
                    (any(ShellCollector::class.java) as? ShellCollector<*>) ?: TextShellCollector(),
                    any(),
                    any(),
                    any(Duration::class.java) ?: Duration.ofSeconds(1),
                    anyInt(),
                    anyBoolean(),
                    anyBoolean(),
                )
            )
            .thenAnswer { invocation ->
                val cmd = invocation.getArgument(1) as String
                if (cmd.contains("am instrument")) {
                    flowOf(com.android.adblib.ShellCommandOutput(stdout, "", 0))
                } else {
                    flowOf("")
                }
            }
    }

    @Test
    fun testLaunchAppDefault() = runBlocking {
        val device =
            BackupRestoreControllerImpl(mockSession, "emulator-5554", 34, "com.example.app")

        val commandsExecuted = mutableListOf<String>()
        `when`(
                mockDeviceServices.shell(
                    any(DeviceSelector::class.java) ?: DeviceSelector.any(),
                    any(String::class.java) ?: "",
                    (any(ShellCollector::class.java) as? ShellCollector<*>) ?: TextShellCollector(),
                    any(),
                    any(),
                    any(Duration::class.java) ?: Duration.ofSeconds(1),
                    anyInt(),
                    anyBoolean(),
                    anyBoolean(),
                )
            )
            .thenAnswer { invocation ->
                val cmd = invocation.getArgument(1) as String
                val collector = invocation.getArgument<Any>(2)
                val isTextCollector =
                    collector::class.java.name.contains("TextShellCollector") ||
                        collector::class.java.name.contains("LineShellCollector")
                commandsExecuted.add(cmd)
                val stdout =
                    if (cmd.contains("resolve-activity")) {
                        "priority=0 preferredOrder=0 match=0x108000 specificIndex=-1 isDefault=true\ncom.example.app/com.example.app.MainActivity\n"
                    } else {
                        ""
                    }
                if (isTextCollector) {
                    flowOf(stdout)
                } else {
                    flowOf(com.android.adblib.ShellCommandOutput(stdout, "", 0))
                }
            }

        device.launchApp()

        assertTrue(commandsExecuted.isNotEmpty())
        val lastCommand = commandsExecuted.last()
        assertTrue(
            lastCommand.contains("am start -W") &&
                lastCommand.contains("-c android.intent.category.LAUNCHER") &&
                lastCommand.contains("-n 'com.example.app/com.example.app.MainActivity'"),
            "Expected 'am start' command but was '$lastCommand'",
        )
        assertTrue(
            commandsExecuted.none { it.contains("POST_NOTIFICATIONS") },
            "launchApp should not grant POST_NOTIFICATIONS permission",
        )
    }

    @Test
    fun testLaunchAppCustom() = runBlocking {
        val device =
            BackupRestoreControllerImpl(mockSession, "emulator-5554", 34, "com.example.app")

        val commandsExecuted = mutableListOf<String>()
        `when`(
                mockDeviceServices.shell(
                    any(DeviceSelector::class.java) ?: DeviceSelector.any(),
                    any(String::class.java) ?: "",
                    (any(ShellCollector::class.java) as? ShellCollector<*>) ?: TextShellCollector(),
                    any(),
                    any(),
                    any(Duration::class.java) ?: Duration.ofSeconds(1),
                    anyInt(),
                    anyBoolean(),
                    anyBoolean(),
                )
            )
            .thenAnswer { invocation ->
                val cmd = invocation.getArgument(1) as String
                commandsExecuted.add(cmd)
                flowOf("")
            }

        device.launchApp(
            activityClass = ".MyActivity",
            intentExtras = mapOf("foo" to "bar value", "baz" to "qux"),
            action = "android.intent.action.VIEW",
        )

        assertTrue(commandsExecuted.isNotEmpty())
        val cmd = commandsExecuted.last()
        assertTrue(cmd.contains("am start"), "Actual command was: '$cmd'")
        assertTrue(cmd.contains("-a android.intent.action.VIEW"), "Actual command was: '$cmd'")
        assertTrue(cmd.contains("-n 'com.example.app/.MyActivity'"), "Actual command was: '$cmd'")
        assertTrue(cmd.contains("--es foo 'bar value'"), "Actual command was: '$cmd'")
        assertTrue(cmd.contains("--es baz 'qux'"), "Actual command was: '$cmd'")
    }

    @Test
    fun testLaunchAppWithQuotesAndSpecialCharsInExtras() = runBlocking {
        val device =
            BackupRestoreControllerImpl(mockSession, "emulator-5554", 34, "com.example.app")

        val commandsExecuted = mutableListOf<String>()
        `when`(
                mockDeviceServices.shell(
                    any(DeviceSelector::class.java) ?: DeviceSelector.any(),
                    any(String::class.java) ?: "",
                    (any(ShellCollector::class.java) as? ShellCollector<*>) ?: TextShellCollector(),
                    any(),
                    any(),
                    any(Duration::class.java) ?: Duration.ofSeconds(1),
                    anyInt(),
                    anyBoolean(),
                    anyBoolean(),
                )
            )
            .thenAnswer { invocation ->
                val cmd = invocation.getArgument(1) as String
                commandsExecuted.add(cmd)
                flowOf("")
            }

        device.launchApp(
            activityClass = ".MyActivity",
            intentExtras = mapOf("name" to "O'Brian", "danger;cmd" to "'; rm -rf /; '"),
        )

        assertTrue(commandsExecuted.isNotEmpty())
        val cmd = commandsExecuted.last()
        assertTrue(cmd.contains("--es name 'O'\\''Brian'"), "Actual command was: '$cmd'")
        assertTrue(
            cmd.contains("""--es 'danger;cmd' ''\''; rm -rf /; '\'''"""),
            "Actual command was: '$cmd'",
        )
    }

    @Test
    fun testLaunchAppRelativeSubpackage() = runBlocking {
        val device =
            BackupRestoreControllerImpl(mockSession, "emulator-5554", 34, "com.example.app")

        val commandsExecuted = mutableListOf<String>()
        `when`(
                mockDeviceServices.shell(
                    any(DeviceSelector::class.java) ?: DeviceSelector.any(),
                    any(String::class.java) ?: "",
                    (any(ShellCollector::class.java) as? ShellCollector<*>) ?: TextShellCollector(),
                    any(),
                    any(),
                    any(Duration::class.java) ?: Duration.ofSeconds(1),
                    anyInt(),
                    anyBoolean(),
                    anyBoolean(),
                )
            )
            .thenAnswer { invocation ->
                val cmd = invocation.getArgument(1) as String
                commandsExecuted.add(cmd)
                flowOf("")
            }

        // Relative class with leading dot (e.g. ".subpackage.MyActivity")
        device.launchApp(activityClass = ".subpackage.MyActivity")
        var cmd = commandsExecuted.last()
        assertTrue(
            cmd.contains("-n 'com.example.app/.subpackage.MyActivity'"),
            "Expected leading dot for relative class: '$cmd'",
        )

        // Simple class name (e.g. "SimpleActivity") should resolve with leading dot
        device.launchApp(activityClass = "SimpleActivity")
        cmd = commandsExecuted.last()
        assertTrue(
            cmd.contains("-n 'com.example.app/.SimpleActivity'"),
            "Expected leading dot for simple activity: '$cmd'",
        )

        // Fully qualified class name matching applicationId
        device.launchApp(activityClass = "com.example.app.subpackage.MyActivity")
        cmd = commandsExecuted.last()
        assertTrue(
            cmd.contains("-n 'com.example.app/com.example.app.subpackage.MyActivity'"),
            "Expected full component name without extra dot: '$cmd'",
        )

        // Fully qualified class name belonging to a different package hierarchy
        device.launchApp(activityClass = "com.other.external.library.MyActivity")
        cmd = commandsExecuted.last()
        assertTrue(
            cmd.contains("-n 'com.example.app/com.other.external.library.MyActivity'"),
            "Expected external package hierarchy class without dot prefix: '$cmd'",
        )
    }

    @Test
    fun testLaunchAppImplicitAction() = runBlocking {
        val device =
            BackupRestoreControllerImpl(mockSession, "emulator-5554", 34, "com.example.app")

        val commandsExecuted = mutableListOf<String>()
        `when`(
                mockDeviceServices.shell(
                    any(DeviceSelector::class.java) ?: DeviceSelector.any(),
                    any(String::class.java) ?: "",
                    (any(ShellCollector::class.java) as? ShellCollector<*>) ?: TextShellCollector(),
                    any(),
                    any(),
                    any(Duration::class.java) ?: Duration.ofSeconds(1),
                    anyInt(),
                    anyBoolean(),
                    anyBoolean(),
                )
            )
            .thenAnswer { invocation ->
                val cmd = invocation.getArgument(1) as String
                commandsExecuted.add(cmd)
                flowOf("")
            }

        device.launchApp(
            activityClass = null,
            intentExtras = emptyMap(),
            action = "com.example.CUSTOM_ACTION",
        )

        assertTrue(commandsExecuted.isNotEmpty())
        val cmd = commandsExecuted.last()
        assertTrue(cmd.contains("am start"), "Actual command was: '$cmd'")
        assertTrue(cmd.contains("-a com.example.CUSTOM_ACTION"), "Actual command was: '$cmd'")
        assertTrue(cmd.contains("-p com.example.app"), "Actual command was: '$cmd'")
        // Assert we did not query package manager for launcher activity
        assertTrue(
            commandsExecuted.none { it.contains("resolve-activity") },
            "Should not query launcher",
        )
    }

    @Test
    fun testClearDeviceLogs() = runBlocking {
        val device =
            BackupRestoreControllerImpl(mockSession, "emulator-5554", 34, "com.example.app")

        val commandsExecuted = mutableListOf<String>()
        `when`(
                mockDeviceServices.shell(
                    any(DeviceSelector::class.java) ?: DeviceSelector.any(),
                    any(String::class.java) ?: "",
                    (any(ShellCollector::class.java) as? ShellCollector<*>) ?: TextShellCollector(),
                    any(),
                    any(),
                    any(Duration::class.java) ?: Duration.ofSeconds(1),
                    anyInt(),
                    anyBoolean(),
                    anyBoolean(),
                )
            )
            .thenAnswer { invocation ->
                val cmd = invocation.getArgument(1) as String
                commandsExecuted.add(cmd)
                flowOf("")
            }

        device.clearDeviceLogs()

        assertTrue(commandsExecuted.isNotEmpty())
        assertEquals("logcat -c", commandsExecuted.last())
    }

    @Test
    fun testAsyncMethods() {
        val device =
            BackupRestoreControllerImpl(mockSession, "emulator-5554", 34, "com.example.app")

        `when`(
                mockDeviceServices.shell(
                    any(DeviceSelector::class.java) ?: DeviceSelector.any(),
                    any(String::class.java) ?: "",
                    (any(ShellCollector::class.java) as? ShellCollector<*>) ?: TextShellCollector(),
                    any(),
                    any(),
                    any(Duration::class.java) ?: Duration.ofSeconds(1),
                    anyInt(),
                    anyBoolean(),
                    anyBoolean(),
                )
            )
            .thenReturn(flowOf(""))

        val future = device.stopAppAsync()
        val result = future.get()
        assertEquals(device, result)
    }

    @Test
    fun testPerformRestoreSessionFiltering() = runBlocking {
        val device =
            BackupRestoreControllerImpl(mockSession, "emulator-5554", 34, "com.example.app")

        var dumpsysCallCount = 0
        `when`(
                mockDeviceServices.shell(
                    any(DeviceSelector::class.java) ?: DeviceSelector.any(),
                    any(String::class.java) ?: "",
                    (any(ShellCollector::class.java) as? ShellCollector<*>) ?: TextShellCollector(),
                    any(),
                    any(),
                    any(Duration::class.java) ?: Duration.ofSeconds(1),
                    anyInt(),
                    anyBoolean(),
                    anyBoolean(),
                )
            )
            .thenAnswer { invocation ->
                val cmd = invocation.getArgument(1) as String
                val collector = invocation.getArgument<Any>(2)
                val isShellCommandOutput = collector::class.java.name.contains("ShellCommandOutput")
                if (cmd.contains("dumpsys backup")) {
                    dumpsysCallCount++
                    val stdout =
                        when (dumpsysCallCount) {
                            // 1. Idle state before dispatch: "Restore session: null" should NOT
                            // match
                            1 -> "Restore session: null\nRestore in progress: false\n"
                            // 2. Active state: valid session object dispatched
                            2 ->
                                "Restore session: com.android.server.backup.RestoreSession@123\n" +
                                    "Restore in progress: true\n"
                            // 3. Completed state: session finishes
                            else -> "Restore session: null\nRestore in progress: false\n"
                        }
                    flowOf(com.android.adblib.ShellCommandOutput(stdout, "", 0))
                } else if (cmd.contains("bmgr list transports")) {
                    flowOf(
                        com.android.adblib.ShellCommandOutput(
                            "* com.android.localtransport/.LocalTransport\n",
                            "",
                            0,
                        )
                    )
                } else if (isShellCommandOutput) {
                    flowOf(com.android.adblib.ShellCommandOutput("", "", 0))
                } else {
                    flowOf("")
                }
            }

        val localBackupFile = tempFolder.newFile("backup_local_device.zip")
        device.performRestore(localBackupFile.toPath(), Duration.ofSeconds(2))
        assertTrue(
            dumpsysCallCount >= 3,
            "Expected at least 3 dumpsys polls, actual: $dumpsysCallCount",
        )
    }

    @Test
    fun testUnstopPackageWithLauncherActivity() = runBlocking {
        val device =
            BackupRestoreControllerImpl(mockSession, "emulator-5554", 34, "com.example.app")

        val commandsExecuted = mutableListOf<String>()
        `when`(
                mockDeviceServices.shell(
                    any(DeviceSelector::class.java) ?: DeviceSelector.any(),
                    any(String::class.java) ?: "",
                    (any(ShellCollector::class.java) as? ShellCollector<*>) ?: TextShellCollector(),
                    any(),
                    any(),
                    any(Duration::class.java) ?: Duration.ofSeconds(1),
                    anyInt(),
                    anyBoolean(),
                    anyBoolean(),
                )
            )
            .thenAnswer { invocation ->
                val cmd = invocation.getArgument(1) as String
                val collector = invocation.getArgument<Any>(2)
                val isTextCollector =
                    collector::class.java.name.contains("TextShellCollector") ||
                        collector::class.java.name.contains("LineShellCollector")
                commandsExecuted.add(cmd)
                val stdout =
                    if (cmd.contains("resolve-activity")) {
                        "priority=0 preferredOrder=0 match=0x108000 specificIndex=-1 isDefault=true\ncom.example.app/com.example.app.MainActivity\n"
                    } else if (cmd.contains("bmgr list transports")) {
                        "* com.android.localtransport/.LocalTransport\n"
                    } else {
                        ""
                    }
                if (isTextCollector) {
                    flowOf(stdout)
                } else {
                    flowOf(com.android.adblib.ShellCommandOutput(stdout, "", 0))
                }
            }

        device.performBackup(BackupTransportMode.LOCAL, tempFolder.root.toPath())

        assertTrue(
            commandsExecuted.any {
                it.contains(
                    "am broadcast -a android.intent.action.MAIN -p com.example.app --include-stopped-packages"
                )
            },
            "Expected silent broadcast wake-up",
        )
        assertTrue(
            commandsExecuted.any { it.contains("resolve-activity") },
            "Expected resolve-activity query",
        )
        assertTrue(
            commandsExecuted.any {
                it.contains("am start -W -n 'com.example.app/com.example.app.MainActivity'")
            },
            "Expected explicit activity launch",
        )
        assertTrue(
            commandsExecuted.any { it.contains("input keyevent KEYCODE_HOME") },
            "Expected KEYCODE_HOME keyevent to restore home screen",
        )
        assertTrue(
            commandsExecuted.none { it.contains("monkey") },
            "Expected no monkey command execution",
        )
    }

    @Test
    fun testUnstopPackageWithoutLauncherActivity() = runBlocking {
        val device =
            BackupRestoreControllerImpl(mockSession, "emulator-5554", 34, "com.example.app")

        val commandsExecuted = mutableListOf<String>()
        `when`(
                mockDeviceServices.shell(
                    any(DeviceSelector::class.java) ?: DeviceSelector.any(),
                    any(String::class.java) ?: "",
                    (any(ShellCollector::class.java) as? ShellCollector<*>) ?: TextShellCollector(),
                    any(),
                    any(),
                    any(Duration::class.java) ?: Duration.ofSeconds(1),
                    anyInt(),
                    anyBoolean(),
                    anyBoolean(),
                )
            )
            .thenAnswer { invocation ->
                val cmd = invocation.getArgument(1) as String
                val collector = invocation.getArgument<Any>(2)
                val isTextCollector =
                    collector::class.java.name.contains("TextShellCollector") ||
                        collector::class.java.name.contains("LineShellCollector")
                commandsExecuted.add(cmd)
                val stdout =
                    if (cmd.contains("resolve-activity")) {
                        "No activity found\n"
                    } else if (cmd.contains("bmgr list transports")) {
                        "* com.android.localtransport/.LocalTransport\n"
                    } else {
                        ""
                    }
                if (isTextCollector) {
                    flowOf(stdout)
                } else {
                    flowOf(com.android.adblib.ShellCommandOutput(stdout, "", 0))
                }
            }

        device.performBackup(BackupTransportMode.LOCAL, tempFolder.root.toPath())

        assertTrue(
            commandsExecuted.any {
                it.contains(
                    "am broadcast -a android.intent.action.MAIN -p com.example.app --include-stopped-packages"
                )
            },
            "Expected silent broadcast wake-up",
        )
        assertTrue(
            commandsExecuted.any { it.contains("resolve-activity") },
            "Expected resolve-activity query",
        )
        assertTrue(
            commandsExecuted.none { it.contains("am start") },
            "Expected no activity start when launcher is absent",
        )
        assertTrue(
            commandsExecuted.none { it.contains("monkey") },
            "Expected no monkey execution when launcher is absent",
        )
        assertTrue(
            commandsExecuted.none { it.contains("KEYCODE_HOME") },
            "Expected no KEYCODE_HOME keyevent when no activity was launched",
        )
    }

    @Test
    fun testLaunchAppWithInnerClassActivity() = runBlocking {
        val device =
            BackupRestoreControllerImpl(mockSession, "emulator-5554", 34, "com.example.app")

        val commandsExecuted = mutableListOf<String>()
        `when`(
                mockDeviceServices.shell(
                    any(DeviceSelector::class.java) ?: DeviceSelector.any(),
                    any(String::class.java) ?: "",
                    (any(ShellCollector::class.java) as? ShellCollector<*>) ?: TextShellCollector(),
                    any(),
                    any(),
                    any(Duration::class.java) ?: Duration.ofSeconds(1),
                    anyInt(),
                    anyBoolean(),
                    anyBoolean(),
                )
            )
            .thenAnswer { invocation ->
                val cmd = invocation.getArgument(1) as String
                commandsExecuted.add(cmd)
                flowOf("")
            }

        device.launchApp(activityClass = "com.example.app.MainActivity\$InnerActivity")
        val cmd = commandsExecuted.last()
        assertTrue(
            cmd.contains("-n 'com.example.app/com.example.app.MainActivity\$InnerActivity'"),
            "Expected escaped inner class with dollar sign: '$cmd'",
        )
    }

    @Test
    fun testUnstopPackageWithInnerClassLauncherActivity() = runBlocking {
        val device =
            BackupRestoreControllerImpl(mockSession, "emulator-5554", 34, "com.example.app")

        val commandsExecuted = mutableListOf<String>()
        `when`(
                mockDeviceServices.shell(
                    any(DeviceSelector::class.java) ?: DeviceSelector.any(),
                    any(String::class.java) ?: "",
                    (any(ShellCollector::class.java) as? ShellCollector<*>) ?: TextShellCollector(),
                    any(),
                    any(),
                    any(Duration::class.java) ?: Duration.ofSeconds(1),
                    anyInt(),
                    anyBoolean(),
                    anyBoolean(),
                )
            )
            .thenAnswer { invocation ->
                val cmd = invocation.getArgument(1) as String
                val collector = invocation.getArgument<Any>(2)
                val isTextCollector =
                    collector::class.java.name.contains("TextShellCollector") ||
                        collector::class.java.name.contains("LineShellCollector")
                commandsExecuted.add(cmd)
                val stdout =
                    if (cmd.contains("resolve-activity")) {
                        "priority=0 preferredOrder=0 match=0x108000 specificIndex=-1 isDefault=true\ncom.example.app/com.example.app.MainActivity\$InnerLauncher\n"
                    } else if (cmd.contains("bmgr list transports")) {
                        "* com.android.localtransport/.LocalTransport\n"
                    } else {
                        ""
                    }
                if (isTextCollector) {
                    flowOf(stdout)
                } else {
                    flowOf(com.android.adblib.ShellCommandOutput(stdout, "", 0))
                }
            }

        device.performBackup(BackupTransportMode.LOCAL, tempFolder.root.toPath())

        assertTrue(
            commandsExecuted.any {
                it.contains(
                    "am start -W -n 'com.example.app/com.example.app.MainActivity\$InnerLauncher'"
                )
            },
            "Expected explicit activity launch escaping inner class dollar sign",
        )
    }

    @Test
    fun testRunBackupRestoreFlowRecordsExecutionSummaryAndPublishesMetrics() {
        val publishedMetrics = mutableMapOf<String, String>()
        val device =
            BackupRestoreControllerImpl(
                mockSession,
                "emulator-5554",
                34,
                "com.example.app",
                telemetryPublisher = { k, v -> publishedMetrics[k] = v },
            )

        `when`(
                mockDeviceServices.shell(
                    any(DeviceSelector::class.java) ?: DeviceSelector.any(),
                    any(String::class.java) ?: "",
                    (any(ShellCollector::class.java) as? ShellCollector<*>) ?: TextShellCollector(),
                    any(),
                    any(),
                    any(Duration::class.java) ?: Duration.ofSeconds(1),
                    anyInt(),
                    anyBoolean(),
                    anyBoolean(),
                )
            )
            .thenAnswer { invocation ->
                val cmd = invocation.getArgument(1) as String
                val collector = invocation.getArgument<Any>(2)
                val isTextCollector =
                    collector::class.java.name.contains("TextShellCollector") ||
                        collector::class.java.name.contains("LineShellCollector")
                val stdout =
                    if (cmd.contains("bmgr list transports")) {
                        "* com.android.localtransport/.LocalTransport\n"
                    } else if (cmd.contains("dumpsys backup")) {
                        "Restore complete: 0\n"
                    } else if (cmd.contains("am instrument")) {
                        "BACKUP_RESTORE_RESULT: {\"isSuccess\":true}\n"
                    } else {
                        ""
                    }
                if (isTextCollector) {
                    flowOf(stdout)
                } else {
                    flowOf(com.android.adblib.ShellCommandOutput(stdout, "", 0))
                }
            }

        runBlocking {
            device.runBackupRestoreFlow(
                listOf(StorageDomain.Preference("app_prefs", "key", "val")),
                tempFolder.root.toPath(),
                BackupTransportMode.LOCAL,
            )
        }

        val summary = device.lastExecutionSummary
        assertNotNull(summary)
        assertEquals(BackupTransportMode.LOCAL, summary.transportMode)
        assertEquals(1, summary.storageDomainCount)
        assertTrue(summary.isSuccess)
        assertEquals(BackupErrorCode.NONE, summary.errorCode)
        assertTrue(summary.totalDuration.toMillis() >= 0)

        assertEquals("unknown", publishedMetrics["BackupRestore.libraryVersion"])
        assertEquals("LOCAL", publishedMetrics["BackupRestore.transportMode"])
        assertEquals("1", publishedMetrics["BackupRestore.storageDomainCount"])
        assertEquals("SUCCESS", publishedMetrics["BackupRestore.status"])
        assertTrue(publishedMetrics.containsKey("BackupRestore.totalDurationMillis"))
    }

    @Test
    fun testRunBackupRestoreFlowMapsClearDataFailureToClearDataFailed() {
        val publishedMetrics = mutableMapOf<String, String>()
        val device =
            BackupRestoreControllerImpl(
                mockSession,
                "emulator-5554",
                34,
                "com.example.app",
                telemetryPublisher = { k, v -> publishedMetrics[k] = v },
            )

        `when`(
                mockDeviceServices.shell(
                    any(DeviceSelector::class.java) ?: DeviceSelector.any(),
                    any(String::class.java) ?: "",
                    (any(ShellCollector::class.java) as? ShellCollector<*>) ?: TextShellCollector(),
                    any(),
                    any(),
                    any(Duration::class.java) ?: Duration.ofSeconds(1),
                    anyInt(),
                    anyBoolean(),
                    anyBoolean(),
                )
            )
            .thenAnswer { invocation ->
                val cmd = invocation.getArgument(1) as String
                val collector = invocation.getArgument<Any>(2)
                val isTextCollector =
                    collector::class.java.name.contains("TextShellCollector") ||
                        collector::class.java.name.contains("LineShellCollector")
                val stdout =
                    if (cmd.contains("bmgr list transports")) {
                        "* com.android.localtransport/.LocalTransport\n"
                    } else if (cmd.contains("pm clear")) {
                        throw java.io.IOException("pm clear failed to clear package data")
                    } else if (cmd.contains("am instrument")) {
                        "BACKUP_RESTORE_RESULT: {\"isSuccess\":true}\n"
                    } else {
                        ""
                    }
                if (isTextCollector) {
                    flowOf(stdout)
                } else {
                    flowOf(com.android.adblib.ShellCommandOutput(stdout, "", 0))
                }
            }

        assertFailsWith<IOException> {
            runBlocking {
                device.runBackupRestoreFlow(
                    listOf(StorageDomain.Preference("app_prefs", "key", "val")),
                    tempFolder.root.toPath(),
                    BackupTransportMode.LOCAL,
                )
            }
        }

        val summary = device.lastExecutionSummary
        assertNotNull(summary)
        assertFalse(summary.isSuccess)
        assertEquals(BackupErrorCode.CLEAR_DATA_FAILED, summary.errorCode)
        assertEquals(BackupExecutionStage.CLEAR_DATA, summary.failureStage)
        assertEquals("FAILURE", publishedMetrics["BackupRestore.status"])
        assertEquals(
            BackupErrorCode.CLEAR_DATA_FAILED.name,
            publishedMetrics["BackupRestore.errorCode"],
        )
        assertEquals(
            BackupExecutionStage.CLEAR_DATA.name,
            publishedMetrics["BackupRestore.failureStage"],
        )
    }

    @Test
    fun testRunBackupRestoreFlowStopAppFailureMapsToBackupFailed() {
        val publishedMetrics = mutableMapOf<String, String>()
        val device =
            BackupRestoreControllerImpl(
                mockSession,
                "emulator-5554",
                34,
                "com.example.app",
                telemetryPublisher = { k, v -> publishedMetrics[k] = v },
            )

        `when`(
                mockDeviceServices.shell(
                    any(DeviceSelector::class.java) ?: DeviceSelector.any(),
                    any(String::class.java) ?: "",
                    (any(ShellCollector::class.java) as? ShellCollector<*>) ?: TextShellCollector(),
                    any(),
                    any(),
                    any(Duration::class.java) ?: Duration.ofSeconds(1),
                    anyInt(),
                    anyBoolean(),
                    anyBoolean(),
                )
            )
            .thenAnswer { invocation ->
                val cmd = invocation.getArgument(1) as String
                val collector = invocation.getArgument<Any>(2)
                val isTextCollector =
                    collector::class.java.name.contains("TextShellCollector") ||
                        collector::class.java.name.contains("LineShellCollector")
                val stdout =
                    if (cmd.contains("am instrument")) {
                        "BACKUP_RESTORE_RESULT: {\"isSuccess\":true}\n"
                    } else if (cmd.contains("am force-stop")) {
                        throw java.io.IOException("Failed to stop process")
                    } else {
                        ""
                    }
                if (isTextCollector) {
                    flowOf(stdout)
                } else {
                    flowOf(com.android.adblib.ShellCommandOutput(stdout, "", 0))
                }
            }

        assertFailsWith<IOException> {
            runBlocking {
                device.runBackupRestoreFlow(
                    listOf(StorageDomain.Preference("app_prefs", "key", "val")),
                    tempFolder.root.toPath(),
                    BackupTransportMode.LOCAL,
                )
            }
        }

        val summary = device.lastExecutionSummary
        assertNotNull(summary)
        assertFalse(summary.isSuccess)
        assertEquals(BackupErrorCode.BACKUP_FAILED, summary.errorCode)
        assertEquals(BackupExecutionStage.BACKUP, summary.failureStage)
        assertEquals("FAILURE", publishedMetrics["BackupRestore.status"])
        assertEquals(
            BackupErrorCode.BACKUP_FAILED.name,
            publishedMetrics["BackupRestore.errorCode"],
        )
        assertEquals(
            BackupExecutionStage.BACKUP.name,
            publishedMetrics["BackupRestore.failureStage"],
        )
    }

    @Test
    fun testRunBackupRestoreFlowSeedingTimeoutMapsToSeedingFailedNotRestorePollTimeout() {
        val publishedMetrics = mutableMapOf<String, String>()
        val device =
            BackupRestoreControllerImpl(
                mockSession,
                "emulator-5554",
                34,
                "com.example.app",
                telemetryPublisher = { k, v -> publishedMetrics[k] = v },
            )

        `when`(
                mockDeviceServices.shell(
                    any(DeviceSelector::class.java) ?: DeviceSelector.any(),
                    any(String::class.java) ?: "",
                    (any(ShellCollector::class.java) as? ShellCollector<*>) ?: TextShellCollector(),
                    any(),
                    any(),
                    any(Duration::class.java) ?: Duration.ofSeconds(1),
                    anyInt(),
                    anyBoolean(),
                    anyBoolean(),
                )
            )
            .thenAnswer { invocation ->
                val cmd = invocation.getArgument(1) as String
                if (cmd.contains("am instrument")) {
                    throw java.io.IOException("Command execution timed out after 30 seconds")
                }
                val collector = invocation.getArgument<Any>(2)
                val isTextCollector =
                    collector::class.java.name.contains("TextShellCollector") ||
                        collector::class.java.name.contains("LineShellCollector")
                if (isTextCollector) {
                    flowOf("")
                } else {
                    flowOf(com.android.adblib.ShellCommandOutput("", "", 0))
                }
            }

        assertFailsWith<IOException> {
            runBlocking {
                device.runBackupRestoreFlow(
                    listOf(StorageDomain.Preference("app_prefs", "key", "val")),
                    tempFolder.root.toPath(),
                    BackupTransportMode.LOCAL,
                )
            }
        }

        val summary = device.lastExecutionSummary
        assertNotNull(summary)
        assertFalse(summary.isSuccess)
        assertEquals(BackupErrorCode.SEEDING_FAILED, summary.errorCode)
        assertEquals(BackupExecutionStage.SEEDING, summary.failureStage)
        assertEquals("FAILURE", publishedMetrics["BackupRestore.status"])
        assertEquals(
            BackupErrorCode.SEEDING_FAILED.name,
            publishedMetrics["BackupRestore.errorCode"],
        )
        assertEquals(
            BackupExecutionStage.SEEDING.name,
            publishedMetrics["BackupRestore.failureStage"],
        )
    }

    @Test
    fun testRunBackupRestoreFlowTelemetryPublisherExceptionDoesNotMaskSuccess() {
        val device =
            BackupRestoreControllerImpl(
                mockSession,
                "emulator-5554",
                34,
                "com.example.app",
                telemetryPublisher = { _, _ -> throw RuntimeException("Telemetry publish failed") },
            )

        `when`(
                mockDeviceServices.shell(
                    any(DeviceSelector::class.java) ?: DeviceSelector.any(),
                    any(String::class.java) ?: "",
                    (any(ShellCollector::class.java) as? ShellCollector<*>) ?: TextShellCollector(),
                    any(),
                    any(),
                    any(Duration::class.java) ?: Duration.ofSeconds(1),
                    anyInt(),
                    anyBoolean(),
                    anyBoolean(),
                )
            )
            .thenAnswer { invocation ->
                val cmd = invocation.getArgument(1) as String
                val collector = invocation.getArgument<Any>(2)
                val isTextCollector =
                    collector::class.java.name.contains("TextShellCollector") ||
                        collector::class.java.name.contains("LineShellCollector")
                val stdout =
                    if (cmd.contains("bmgr list transports")) {
                        "* com.android.localtransport/.LocalTransport\n"
                    } else if (cmd.contains("am instrument")) {
                        "BACKUP_RESTORE_RESULT: {\"isSuccess\":true}\n"
                    } else {
                        ""
                    }
                if (isTextCollector) {
                    flowOf(stdout)
                } else {
                    flowOf(com.android.adblib.ShellCommandOutput(stdout, "", 0))
                }
            }

        // Even though telemetryPublisher throws, the flow must complete successfully
        runBlocking {
            device.runBackupRestoreFlow(
                listOf(StorageDomain.Preference("app_prefs", "key", "val")),
                tempFolder.root.toPath(),
                BackupTransportMode.LOCAL,
            )
        }

        val summary = device.lastExecutionSummary
        assertNotNull(summary)
        assertTrue(summary.isSuccess)
        assertEquals(BackupErrorCode.NONE, summary.errorCode)
    }

    @Test
    fun testRunBackupRestoreFlowEmptyStoragesFailsAtPrecondition() {
        val publishedMetrics = mutableMapOf<String, String>()
        val device =
            BackupRestoreControllerImpl(
                mockSession,
                "emulator-5554",
                34,
                "com.example.app",
                telemetryPublisher = { k, v -> publishedMetrics[k] = v },
            )

        assertFailsWith<IllegalArgumentException> {
            runBlocking {
                device.runBackupRestoreFlow(
                    emptyList(),
                    tempFolder.root.toPath(),
                    BackupTransportMode.LOCAL,
                )
            }
        }

        val summary = device.lastExecutionSummary
        assertNotNull(summary)
        assertFalse(summary.isSuccess)
        assertEquals(BackupExecutionStage.PRECONDITION, summary.failureStage)
        assertEquals(BackupErrorCode.UNKNOWN_ERROR, summary.errorCode)
        assertEquals("FAILURE", publishedMetrics["BackupRestore.status"])
        assertEquals(
            BackupExecutionStage.PRECONDITION.name,
            publishedMetrics["BackupRestore.failureStage"],
        )
    }

    @Test
    fun testRunBackupRestoreFlowRestoreBmgrFailureMapsToBmgrInitFailed() {
        val publishedMetrics = mutableMapOf<String, String>()
        val device =
            BackupRestoreControllerImpl(
                mockSession,
                "emulator-5554",
                34,
                "com.example.app",
                telemetryPublisher = { k, v -> publishedMetrics[k] = v },
            )

        `when`(
                mockDeviceServices.shell(
                    any(DeviceSelector::class.java) ?: DeviceSelector.any(),
                    any(String::class.java) ?: "",
                    (any(ShellCollector::class.java) as? ShellCollector<*>) ?: TextShellCollector(),
                    any(),
                    any(),
                    any(Duration::class.java) ?: Duration.ofSeconds(1),
                    anyInt(),
                    anyBoolean(),
                    anyBoolean(),
                )
            )
            .thenAnswer { invocation ->
                val cmd = invocation.getArgument(1) as String
                val collector = invocation.getArgument<Any>(2)
                val isTextCollector =
                    collector::class.java.name.contains("TextShellCollector") ||
                        collector::class.java.name.contains("LineShellCollector")
                val stdout =
                    if (cmd.contains("am instrument")) {
                        "BACKUP_RESTORE_RESULT: {\"isSuccess\":true}\n"
                    } else if (cmd.contains("bmgr restore")) {
                        throw java.io.IOException("bmgr: transport initialization error")
                    } else {
                        ""
                    }
                if (isTextCollector) {
                    flowOf(stdout)
                } else {
                    flowOf(com.android.adblib.ShellCommandOutput(stdout, "", 0))
                }
            }

        assertFailsWith<IOException> {
            runBlocking {
                device.runBackupRestoreFlow(
                    listOf(StorageDomain.Preference("app_prefs", "key", "val")),
                    tempFolder.root.toPath(),
                    BackupTransportMode.LOCAL,
                )
            }
        }

        val summary = device.lastExecutionSummary
        assertNotNull(summary)
        assertFalse(summary.isSuccess)
        assertEquals(BackupErrorCode.BMGR_INIT_FAILED, summary.errorCode)
        assertEquals(BackupExecutionStage.RESTORE, summary.failureStage)
        assertEquals(
            BackupErrorCode.BMGR_INIT_FAILED.name,
            publishedMetrics["BackupRestore.errorCode"],
        )
        assertEquals(
            BackupExecutionStage.RESTORE.name,
            publishedMetrics["BackupRestore.failureStage"],
        )
    }

    @Test
    fun testRunBackupRestoreFlowRestoreUppercaseTimeoutMapsToRestorePollTimeout() {
        val publishedMetrics = mutableMapOf<String, String>()
        val device =
            BackupRestoreControllerImpl(
                mockSession,
                "emulator-5554",
                34,
                "com.example.app",
                telemetryPublisher = { k, v -> publishedMetrics[k] = v },
            )

        `when`(
                mockDeviceServices.shell(
                    any(DeviceSelector::class.java) ?: DeviceSelector.any(),
                    any(String::class.java) ?: "",
                    (any(ShellCollector::class.java) as? ShellCollector<*>) ?: TextShellCollector(),
                    any(),
                    any(),
                    any(Duration::class.java) ?: Duration.ofSeconds(1),
                    anyInt(),
                    anyBoolean(),
                    anyBoolean(),
                )
            )
            .thenAnswer { invocation ->
                val cmd = invocation.getArgument(1) as String
                val collector = invocation.getArgument<Any>(2)
                val isTextCollector =
                    collector::class.java.name.contains("TextShellCollector") ||
                        collector::class.java.name.contains("LineShellCollector")
                val stdout =
                    if (cmd.contains("am instrument")) {
                        "BACKUP_RESTORE_RESULT: {\"isSuccess\":true}\n"
                    } else if (cmd.contains("dumpsys backup")) {
                        throw java.io.IOException(
                            "RESTORE POLLING TIMED OUT WAITING FOR COMPLETION"
                        )
                    } else {
                        ""
                    }
                if (isTextCollector) {
                    flowOf(stdout)
                } else {
                    flowOf(com.android.adblib.ShellCommandOutput(stdout, "", 0))
                }
            }

        assertFailsWith<IOException> {
            runBlocking {
                device.runBackupRestoreFlow(
                    listOf(StorageDomain.Preference("app_prefs", "key", "val")),
                    tempFolder.root.toPath(),
                    BackupTransportMode.LOCAL,
                )
            }
        }

        val summary = device.lastExecutionSummary
        assertNotNull(summary)
        assertFalse(summary.isSuccess)
        assertEquals(BackupErrorCode.RESTORE_POLL_TIMEOUT, summary.errorCode)
        assertEquals(BackupExecutionStage.RESTORE, summary.failureStage)
        assertEquals(
            BackupErrorCode.RESTORE_POLL_TIMEOUT.name,
            publishedMetrics["BackupRestore.errorCode"],
        )
    }

    @Test
    fun testMapExceptionToErrorCodeCaseInsensitive() {
        val device =
            BackupRestoreControllerImpl(mockSession, "emulator-5554", 34, "com.example.app")

        assertEquals(
            BackupErrorCode.RESTORE_POLL_TIMEOUT,
            device.mapExceptionToErrorCode(
                BackupExecutionStage.RESTORE,
                IOException("TIMEOUT DURING RESTORE"),
            ),
        )
        assertEquals(
            BackupErrorCode.RESTORE_POLL_TIMEOUT,
            device.mapExceptionToErrorCode(
                BackupExecutionStage.RESTORE,
                IOException("POLLING FAILED"),
            ),
        )
        assertEquals(
            BackupErrorCode.GMSCORE_OUTDATED_OR_MISSING,
            device.mapExceptionToErrorCode(
                BackupExecutionStage.BACKUP,
                IOException("MISSING GMSCORE"),
            ),
        )
        assertEquals(
            BackupErrorCode.BMGR_INIT_FAILED,
            device.mapExceptionToErrorCode(
                BackupExecutionStage.BACKUP,
                IOException("BMGR TRANSPORT ERROR"),
            ),
        )
    }

    @Test
    fun testMapExceptionToErrorCodeKeyguardFailure() {
        val device =
            BackupRestoreControllerImpl(mockSession, "emulator-5554", 34, "com.example.app")

        val code =
            device.mapExceptionToErrorCode(
                BackupExecutionStage.PRECONDITION,
                IllegalStateException("Device keyguard dismiss failed"),
            )
        assertEquals(BackupErrorCode.KEYGUARD_UNLOCK_FAILED, code)
    }
}
