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
import java.time.Duration
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito
import org.mockito.Mockito.any
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
                    Mockito.anyInt(),
                    Mockito.anyBoolean(),
                    Mockito.anyBoolean(),
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
                    Mockito.anyInt(),
                    Mockito.anyBoolean(),
                    Mockito.anyBoolean(),
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
                    Mockito.anyInt(),
                    Mockito.anyBoolean(),
                    Mockito.anyBoolean(),
                )
            )
            .thenAnswer { invocation ->
                val cmd = invocation.getArgument(1) as String
                commandsExecuted.add(cmd)
                flowOf("")
            }

        device.launchApp()

        assertTrue(commandsExecuted.isNotEmpty())
        assertEquals(
            "monkey -p com.example.app -c android.intent.category.LAUNCHER 1",
            commandsExecuted.last(),
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
                    Mockito.anyInt(),
                    Mockito.anyBoolean(),
                    Mockito.anyBoolean(),
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
        assertTrue("Actual command was: '$cmd'", cmd.contains("am start"))
        assertTrue("Actual command was: '$cmd'", cmd.contains("-a android.intent.action.VIEW"))
        assertTrue("Actual command was: '$cmd'", cmd.contains("-n com.example.app/.MyActivity"))
        assertTrue("Actual command was: '$cmd'", cmd.contains("--es foo 'bar value'"))
        assertTrue("Actual command was: '$cmd'", cmd.contains("--es baz 'qux'"))
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
                    Mockito.anyInt(),
                    Mockito.anyBoolean(),
                    Mockito.anyBoolean(),
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
                    Mockito.anyInt(),
                    Mockito.anyBoolean(),
                    Mockito.anyBoolean(),
                )
            )
            .thenReturn(flowOf(""))

        val future = device.stopAppAsync()
        val result = future.get()
        assertEquals(device, result)
    }
}
