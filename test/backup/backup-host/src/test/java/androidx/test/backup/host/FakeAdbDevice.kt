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
import com.android.adblib.ConnectedDevice
import com.android.adblib.ConnectedDeviceList
import com.android.adblib.ConnectedDevicesTracker
import com.android.adblib.CoroutineScopeCache
import com.android.adblib.DeviceCacheProvider
import com.android.adblib.DeviceInfo
import com.android.adblib.DeviceSelector
import com.android.adblib.DeviceState
import com.android.adblib.RemoteFileMode
import com.android.adblib.ShellCollector
import com.android.adblib.ShellCommandOutput
import com.android.adblib.StateFlowStatus
import com.android.adblib.TextShellCollector
import com.android.adblib.connectedDevicesTracker
import com.android.adblib.deviceCacheProvider
import java.nio.file.Path
import java.time.Duration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
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

/** Default serial number for a [FakeAdbDevice]. */
internal const val FAKE_SERIAL = "fake-serial"

/** Name of the device's local backup transport, as `bmgr list transports` prints it. */
internal const val LOCAL_TRANSPORT = "com.android.localtransport/.LocalTransport"

/** Name of the backup transport of Google Play services, as `bmgr list transports` prints it. */
internal const val GMS_TRANSPORT = "com.google.android.gms/.backup.BackupTransportService"

/** Returns a [ShellCommandOutput] for a stubbed shell command. */
internal fun shellOutput(stdout: String = "", stderr: String = "", exitCode: Int = 0) =
    ShellCommandOutput(stdout, stderr, exitCode)

/** Returns what `am instrument` prints for a run in which the runner reported [resultJson]. */
internal fun instrumentOutput(resultJson: String): String =
    "INSTRUMENTATION_RESULT: resultJson=$resultJson\nINSTRUMENTATION_CODE: -1\n"

/**
 * Returns what `am instrument` prints for a run in which the runner sent [resultJson] in parts of
 * [partLength] characters, each in a status, and then named them in its result.
 */
internal fun partedInstrumentOutput(resultJson: String, partLength: Int): String = buildString {
    val parts = resultJson.chunked(partLength)
    for (part in parts) {
        append("INSTRUMENTATION_STATUS: resultPart=").append(part).append('\n')
        append("INSTRUMENTATION_STATUS_CODE: 1\n")
    }
    // `am instrument` prints the keys of a result in sorted order.
    append("INSTRUMENTATION_RESULT: resultLength=${resultJson.length}\n")
    append("INSTRUMENTATION_RESULT: resultParts=${parts.size}\n")
    append("INSTRUMENTATION_CODE: -1\n")
}

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
                "Running incremental backup for ${args.size - 1} requested packages.\n" +
                    "Package ${arg(1)} with result: Success\n" +
                    args.drop(2).joinToString("") { "Package $it with result: Success\n" } +
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
 * Returns what `dumpsys backup` prints on a device with the local transport and the one of Google
 * Play services, in the form of an API 34 device. For user 0, backup is enabled or not as [enabled]
 * says, and [selectedTransport] is the selected transport, or none is if it is null.
 */
internal fun backupDump(enabled: Boolean, selectedTransport: String?): String {
    fun transport(name: String, destination: String) =
        (if (name == selectedTransport) "  * " else "    ") +
            "$name\n" +
            "       destination: $destination\n" +
            "       intent: null\n" +
            "       @pm@ - 153 state bytes\n"
    return "Backup Manager is ${if (enabled) "enabled" else "disabled"} / setup complete / " +
        "not pending init\n" +
        "Auto-restore is enabled\n" +
        "No backups running\n" +
        "Framework scheduling is enabled\n" +
        "Last backup pass started: 1700000000000 (now = 1700000100000)\n" +
        "  next scheduled: 1700014400000\n" +
        "Transport whitelist:\n" +
        "    $LOCAL_TRANSPORT\n" +
        "    $GMS_TRANSPORT\n" +
        "Available transports:\n" +
        transport(LOCAL_TRANSPORT, "Backing up to debug-only private cache") +
        transport(GMS_TRANSPORT, "Need to set the backup account") +
        "Transport clients created: 4\n" +
        "Current transport clients: 0\n" +
        "Pending init: 0\n" +
        "Ancestral: 0\n" +
        "Current:   1\n" +
        "Participants:\n" +
        "  uid: 10190\n" +
        "    com.example.app\n" +
        "Ancestral packages: none\n" +
        "Ever backed up: 1\n" +
        "    com.example.app\n" +
        "Pending key/value backup: 0\n" +
        "Full backup queue:1\n" +
        "    0 : com.example.app\n"
}

/**
 * Returns what `dumpsys package` prints for the installed app [packageName], in the form of an API
 * 34 device. For user 0, the app is in the stopped state or not as [stopped] says.
 */
internal fun packageDump(packageName: String, stopped: Boolean): String {
    val codePath = "/data/app/~~L_no4vk9HaTF2FdNk0W9TA==/$packageName-U-qJtSkwMYR1ilsn6EHImQ=="
    return "Key Set Manager:\n" +
        "  [$packageName]\n" +
        "      Signing KeySets: 37\n" +
        "\n" +
        "Packages:\n" +
        "  Package [$packageName] (9a8b7c6):\n" +
        "    appId=10190\n" +
        "    pkg=Package{d3f4a5b $packageName}\n" +
        "    codePath=$codePath\n" +
        "    versionCode=1 minSdk=23 targetSdk=34\n" +
        "    flags=[ DEBUGGABLE HAS_CODE ALLOW_CLEAR_USER_DATA ALLOW_BACKUP ]\n" +
        "    install permissions:\n" +
        "      android.permission.INTERNET: granted=true\n" +
        "    User 0: ceDataInode=131450 installed=true hidden=false suspended=false " +
        "distractionFlags=0 stopped=$stopped notLaunched=false enabled=0 instant=false " +
        "virtual=false\n" +
        "      installReason=0\n" +
        "      firstInstallTime=2026-01-01 00:00:00\n" +
        "      uninstallReason=0\n" +
        "      gids=[3003]\n" +
        "      runtime permissions:\n" +
        "\n" +
        "Queries:\n" +
        "  system apps queryable: false\n" +
        "  queryable via interaction:\n" +
        "    User 0:\n" +
        "      com.android.shell:\n" +
        "        $packageName\n" +
        "\n" +
        "Dexopt state:\n" +
        "  [$packageName]\n" +
        "    path: $codePath/base.apk\n"
}

/**
 * A mocked [AdbSession] connected to one device, which records the shell commands and file
 * transfers issued through it.
 *
 * Every shell command succeeds unless [onShell] says otherwise: `bmgr` prints what it prints on
 * success, see [bmgrSuccessOutput], and other commands print nothing. Pulled files receive the
 * content returned by [onPull].
 */
internal class FakeAdbDevice(
    val serialNumber: String = FAKE_SERIAL,
    scopeDispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
) {

    private val thisDevice: DeviceSelector = DeviceSelector.fromSerialNumber(serialNumber)

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

    /** Content of each file pushed to the device, by device path. */
    val pushedContents: Map<String, String>
        get() = _pushedContents

    /** Permission bits of each file pushed to the device, by device path. */
    val pushedModes: Map<String, RemoteFileMode>
        get() = _pushedModes

    private val _commands = mutableListOf<String>()
    private val _pulledPaths = mutableListOf<String>()
    private val _pushedPaths = mutableListOf<String>()
    private val _pushedContents = mutableMapOf<String, String>()
    private val _pushedModes = mutableMapOf<String, RemoteFileMode>()

    private var shellHandler: (String) -> ShellCommandOutput? = { null }
    private var pullHandler: (String) -> String = { "" }
    private var hangPredicate: (String) -> Boolean = { false }
    private var onlineDevicesApi: Map<String, Int> = emptyMap()
    private var readonlyProps: Map<String, String> = emptyMap()
    private var lastCreatedHostFile: Path? = null
    private var lastOpenedHostFile: Path? = null

    /**
     * Stubs read-only (`ro.*`) system properties in [properties] returned by
     * [com.android.adblib.deviceProperties].
     */
    fun stubReadonlyProperties(properties: Map<String, String>) {
        readonlyProps = properties
    }

    /**
     * Stubs [session]'s [com.android.adblib.AdbSession.connectedDevicesTracker] and
     * [com.android.adblib.deviceProperties] for [devices].
     */
    fun stubOnlineDevices(vararg devices: Pair<String, Int> = arrayOf(serialNumber to 34)) {
        onlineDevicesApi = devices.toMap()
        val connectedList = devices.map { (serial, _) ->
            val info = DeviceInfo(serialNumber = serial, deviceState = DeviceState.ONLINE)
            mock(ConnectedDevice::class.java).also { cd ->
                `when`(cd.session).thenReturn(session)
                `when`(cd.deviceInfoFlow).thenReturn(MutableStateFlow(info))
            }
        }
        val tracker = mock(ConnectedDevicesTracker::class.java)
        `when`(tracker.session).thenReturn(session)
        `when`(tracker.connectedDevices)
            .thenReturn(
                MutableStateFlow(ConnectedDeviceList(connectedList, StateFlowStatus.active))
            )
        `when`(session.connectedDevicesTracker).thenReturn(tracker)
    }

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
                        val devicePath = invocation.arguments[1] as String
                        _pushedPaths += devicePath
                        // Tests may push host files that don't exist, such as fake APKs.
                        lastOpenedHostFile
                            ?.toFile()
                            ?.takeIf { it.isFile }
                            ?.let {
                                _pushedContents[devicePath] = it.readText()
                            }
                        _pushedModes[devicePath] = invocation.arguments[2] as RemoteFileMode
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
                    "openFile" -> {
                        lastOpenedHostFile = invocation.arguments[0] as Path
                        mock(AdbInputChannel::class.java)
                    }
                    else -> RETURNS_DEFAULTS.answer(invocation)
                }
            }
        `when`(session.deviceServices).thenReturn(deviceServices)
        `when`(deviceServices.session).thenReturn(session)
        `when`(session.channelFactory).thenReturn(channelFactory)
        `when`(session.scope).thenReturn(CoroutineScope(scopeDispatcher))
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
                val selector = invocation.getArgument(0) as DeviceSelector
                val command = invocation.getArgument(1) as String
                val collector = invocation.getArgument<Any>(2)::class.java.name
                // adblib probes the device with its own text commands; they are not ours to record.
                if (
                    collector.contains("TextShellCollector") ||
                        collector.contains("LineShellCollector")
                ) {
                    val stubbedApi =
                        onlineDevicesApi.entries
                            .firstOrNull { (s, _) ->
                                DeviceSelector.fromSerialNumber(s).toString() == selector.toString()
                            }
                            ?.value
                    if (command == "getprop") {
                        val lines = buildList {
                            if (stubbedApi != null) {
                                add("[ro.build.version.sdk]: [$stubbedApi]")
                            } else {
                                checkAddressedToThisDevice(selector)
                            }
                            for ((k, v) in readonlyProps) {
                                add("[$k]: [$v]")
                            }
                        }
                        if (lines.isEmpty()) flowOf("") else lines.asFlow()
                    } else {
                        checkAddressedToThisDevice(selector)
                        flowOf("")
                    }
                } else {
                    checkAddressedToThisDevice(selector)
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
     * Fails the test if [selector] addresses a device other than [serialNumber]. Throws an [Error]
     * so that code under test that handles exceptions cannot swallow it.
     */
    private fun checkAddressedToThisDevice(selector: DeviceSelector) {
        if (selector.toString() != thisDevice.toString()) {
            throw AssertionError("Expected a request for $thisDevice, but got one for $selector")
        }
    }
}
