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

import com.android.adblib.AdbChannelFactory
import com.android.adblib.AdbDeviceServices
import com.android.adblib.AdbDeviceSyncServices
import com.android.adblib.AdbHostServices
import com.android.adblib.AdbInputChannel
import com.android.adblib.AdbLogger
import com.android.adblib.AdbLoggerFactory
import com.android.adblib.AdbOutputChannel
import com.android.adblib.AdbSession
import com.android.adblib.AdbSessionHost
import com.android.adblib.CoroutineScopeCache
import com.android.adblib.DeviceCacheProvider
import com.android.adblib.DeviceSelector
import com.android.adblib.ShellCollector
import com.android.adblib.ShellCommandOutput
import com.android.adblib.TextShellCollector
import com.android.adblib.deviceCacheProvider
import java.nio.file.Path
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.mockito.Mockito.RETURNS_DEFAULTS
import org.mockito.Mockito.any
import org.mockito.Mockito.anyBoolean
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * Serial number of the device that [FakeAdbDevice] stands in for. Commands addressed to any other
 * device fail the test.
 */
internal const val FAKE_SERIAL = "fake-serial"

/** Name of the device's local backup transport, as `bmgr list transports` prints it. */
internal const val LOCAL_TRANSPORT = "com.android.localtransport/.LocalTransport"

/** Returns a [ShellCommandOutput] for a stubbed shell command. */
internal fun shellOutput(stdout: String = "", stderr: String = "", exitCode: Int = 0) =
    ShellCommandOutput(stdout, stderr, exitCode)

/** Returns what `am instrument` prints for a run in which the runner reported [resultJson]. */
internal fun instrumentOutput(resultJson: String): String =
    "INSTRUMENTATION_RESULT: resultJson=$resultJson\nINSTRUMENTATION_CODE: -1\n"

/** Returns what `am instrument` prints for a run of an action that returned [payloadJson]. */
internal fun runnerStdout(payloadJson: String): String =
    instrumentOutput(
        buildJsonObject {
            put("isSuccess", true)
            put("payloadJson", payloadJson)
        }
            .toString()
    )

/** Returns what `am instrument` prints when the runner wrote the payload to [devicePath]. */
internal fun overflowStdout(devicePath: String): String =
    instrumentOutput(
        buildJsonObject {
            put("isSuccess", true)
            put("payload_path", devicePath)
        }
            .toString()
    )

/**
 * Returns what `bmgr` prints when [command] succeeds, as captured on an API 34 device, or null if
 * [command] is not a `bmgr` command whose output is checked.
 *
 * Throws an [AssertionError] if [command] lacks an argument that its output names, so that code
 * under test that handles exceptions cannot swallow it.
 */
internal fun bmgrSuccessOutput(command: String): ShellCommandOutput? {
    if (!command.startsWith("bmgr ")) return null
    val args = command.removePrefix("bmgr ").split(' ').map { it.removeSurrounding("'") }
    fun arg(index: Int) =
        args.getOrNull(index) ?: throw AssertionError("Malformed bmgr command: $command")
    val stdout =
        when (args[0]) {
            "enable" -> "Backup Manager now enabled\n"
            "transport" -> "Selected transport ${arg(1)} (formerly ${arg(1)})\n"
            "backupnow" ->
                "Running incremental backup for 1 requested packages.\n" +
                    "Package ${arg(1)} with result: Success\n" +
                    "Backup finished with result: Success\n"
            "restore" ->
                "Scheduling restore: Local disk image\n" +
                    "restoreStarting: 1 packages\n" +
                    "onUpdate: 0 = ${arg(2)}\n" +
                    "restoreFinished: 0\n" +
                    "done\n"
            else -> return null
        }
    return shellOutput(stdout)
}

/**
 * A mocked [AdbSession] connected to one device, which records the shell commands and file
 * transfers issued through it.
 *
 * Every shell command succeeds unless [onShell] says otherwise: `bmgr` prints what it prints on
 * success, see [bmgrSuccessOutput], and other commands print nothing. Pulled files receive the
 * content returned by [onPull].
 */
internal class FakeAdbDevice {

    val session: AdbSession = mock(AdbSession::class.java)

    /** Shell commands run on the device, in order. */
    val commands: List<String>
        get() = _commands

    /** Device paths pulled from the device, in order. */
    val pulledPaths: List<String>
        get() = _pulledPaths

    /** Device paths pushed to the device, in order. */
    val pushedPaths: List<String>
        get() = _pushedPaths

    private val _commands = mutableListOf<String>()
    private val _pulledPaths = mutableListOf<String>()
    private val _pushedPaths = mutableListOf<String>()

    private var shellHandler: (String) -> ShellCommandOutput? = { null }
    private var pullHandler: (String) -> String = { "" }
    private var hangPredicate: (String) -> Boolean = { false }
    private var lastCreatedHostFile: Path? = null

    /**
     * Answers every later shell command with [handler]; an exception it throws is propagated. A
     * command for which it returns null succeeds as described in [FakeAdbDevice].
     */
    fun onShell(handler: (command: String) -> ShellCommandOutput?) {
        shellHandler = handler
    }

    /** Answers every later pull of a device file with the content returned by [handler]. */
    fun onPull(handler: (devicePath: String) -> String) {
        pullHandler = handler
    }

    /**
     * Makes every later shell command matching [predicate] run until it is cancelled, as on an
     * unresponsive device. The command is still recorded.
     */
    fun hangOn(predicate: (command: String) -> Boolean) {
        hangPredicate = predicate
    }

    init {
        // `sync` and `createFile` are suspend functions with default arguments, which Mockito
        // cannot stub with `when`; a default answer keyed on the method name handles them instead.
        val syncServices =
            mock(AdbDeviceSyncServices::class.java) { invocation ->
                when (invocation.method.name) {
                    "recv" -> {
                        val devicePath = invocation.arguments[0] as String
                        _pulledPaths += devicePath
                        lastCreatedHostFile!!.toFile().writeText(pullHandler(devicePath))
                        Unit
                    }
                    "send" -> {
                        _pushedPaths += invocation.arguments[1] as String
                        Unit
                    }
                    else -> RETURNS_DEFAULTS.answer(invocation)
                }
            }
        val deviceServices =
            mock(AdbDeviceServices::class.java) { invocation ->
                if (invocation.method.name == "sync") {
                    checkAddressedToThisDevice(invocation.arguments[0] as DeviceSelector)
                    syncServices
                } else {
                    RETURNS_DEFAULTS.answer(invocation)
                }
            }
        val channelFactory =
            mock(AdbChannelFactory::class.java) { invocation ->
                when (invocation.method.name) {
                    "createFile" -> {
                        lastCreatedHostFile = invocation.arguments[0] as Path
                        mock(AdbOutputChannel::class.java)
                    }
                    "openFile" -> mock(AdbInputChannel::class.java)
                    else -> RETURNS_DEFAULTS.answer(invocation)
                }
            }
        `when`(session.deviceServices).thenReturn(deviceServices)
        `when`(deviceServices.session).thenReturn(session)
        `when`(session.channelFactory).thenReturn(channelFactory)
        `when`(session.scope).thenReturn(CoroutineScope(Dispatchers.Unconfined))
        `when`(session.cache).thenReturn(mock(CoroutineScopeCache::class.java))
        `when`(session.deviceCacheProvider).thenReturn(mock(DeviceCacheProvider::class.java))
        stubHost()
        stubHostServices()
        stubShell(deviceServices)
    }

    @Suppress("UNCHECKED_CAST")
    private fun stubHost() {
        val host = mock(AdbSessionHost::class.java)
        `when`(session.host).thenReturn(host)
        val anyProperty = mock(AdbSessionHost.Property::class.java) as AdbSessionHost.Property<Any>
        `when`(host.getPropertyValue(any(AdbSessionHost.Property::class.java) ?: anyProperty))
            .thenAnswer { invocation ->
                (invocation.getArgument(0) as AdbSessionHost.Property<*>).defaultValue
            }
        val loggerFactory = mock(AdbLoggerFactory::class.java)
        `when`(host.loggerFactory).thenReturn(loggerFactory)
        val logger = mock(AdbLogger::class.java)
        `when`(loggerFactory.createLogger(any(Class::class.java) ?: Any::class.java))
            .thenReturn(logger)
        `when`(logger.minLevel).thenReturn(AdbLogger.Level.INFO)
    }

    private fun stubHostServices() {
        val hostServices = mock(AdbHostServices::class.java)
        `when`(session.hostServices).thenReturn(hostServices)
        `when`(hostServices.session).thenReturn(session)
        runBlocking {
            `when`(hostServices.features(any(DeviceSelector::class.java) ?: DeviceSelector.any()))
                .thenReturn(emptyList())
            `when`(hostServices.hostFeatures()).thenReturn(emptyList())
        }
    }

    private fun stubShell(deviceServices: AdbDeviceServices) {
        `when`(
                deviceServices.shell(
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
                checkAddressedToThisDevice(invocation.getArgument(0) as DeviceSelector)
                val command = invocation.getArgument(1) as String
                val collector = invocation.getArgument<Any>(2)::class.java.name
                // adblib probes the device with its own text commands; they are not ours to record.
                if (
                    collector.contains("TextShellCollector") ||
                        collector.contains("LineShellCollector")
                ) {
                    flowOf("")
                } else {
                    _commands += command
                    if (hangPredicate(command)) {
                        flow<ShellCommandOutput> { awaitCancellation() }
                    } else {
                        flowOf(shellHandler(command) ?: bmgrSuccessOutput(command) ?: shellOutput())
                    }
                }
            }
    }

    /**
     * Fails the test if [selector] addresses a device other than [FAKE_SERIAL]. Throws an [Error]
     * so that code under test that handles exceptions cannot swallow it.
     */
    private fun checkAddressedToThisDevice(selector: DeviceSelector) {
        if (selector.toString() != THIS_DEVICE.toString()) {
            throw AssertionError("Expected a request for $THIS_DEVICE, but got one for $selector")
        }
    }

    private companion object {
        val THIS_DEVICE: DeviceSelector = DeviceSelector.fromSerialNumber(FAKE_SERIAL)
    }
}
