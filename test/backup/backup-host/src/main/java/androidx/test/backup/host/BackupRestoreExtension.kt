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

import com.android.adblib.AdbSession
import com.android.adblib.connectedDevicesTracker
import com.android.adblib.deviceProperties
import com.android.adblib.isOnline
import com.android.adblib.serialNumber
import com.android.adblib.shellAsText
import com.android.adblib.tools.createStandaloneSession
import java.util.logging.Logger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

/**
 * A JUnit 5 Jupiter extension that resolves [BackupRestoreController] test parameters.
 *
 * It automatically initializes ADB sessions, selects an online Android device matching target API
 * and serial constraints (specified via system properties or [Device] annotations), handles
 * automatic APK installation for tested and test packages, proactively dismisses
 * lockscreens/keyguards, and sets up data isolation (e.g. running `pm clear`) prior to test
 * execution.
 */
public class BackupRestoreExtension
private constructor(
    private val isStandalone: Boolean,
    private val adbSessionProvider: () -> AdbSession,
) : ParameterResolver {

    /**
     * Creates a standalone extension that automatically initializes and manages its own ADB
     * session.
     *
     * In this standalone mode, the extension is fully self-contained: it automatically initializes
     * an independent ADB session on startup, and automatically closes/disposes of the session when
     * the test execution completes to prevent resource/thread leaks. This is ideal for isolated
     * test runs where no external or shared ADB lifecycle management is active.
     */
    public constructor() : this(isStandalone = true, { createStandaloneSession() })

    /**
     * Creates an extension that integrates with an existing shared [AdbSession].
     *
     * In this shared mode, the extension integrates with a shared ADB session provided by an
     * external framework or test suite. The lifecycle of this session (creation and close/cleanup)
     * is managed externally, meaning the extension will not close or dispose of the shared session.
     *
     * @param adbSession The active [AdbSession] to be used by the extension.
     */
    public constructor(adbSession: AdbSession) : this(isStandalone = false, { adbSession })

    private val adbSession: AdbSession by lazy { adbSessionProvider() }

    private val logger = Logger.getLogger(BackupRestoreExtension::class.java.name)

    override fun supportsParameter(
        parameterContext: ParameterContext?,
        extensionContext: ExtensionContext?,
    ): Boolean {
        return parameterContext?.parameter?.type?.name ==
            "androidx.test.backup.host.BackupRestoreController"
    }

    override fun resolveParameter(
        parameterContext: ParameterContext?,
        extensionContext: ExtensionContext?,
    ): Any {

        val requiredClass =
            extensionContext?.requiredTestClass
                ?: throw IllegalStateException("Required test class is missing")
        val config =
            requiredClass.getAnnotation(BackupRestoreConfig::class.java)
                ?: throw IllegalStateException(
                    "BackupRestoreConfig annotation is required on class ${requiredClass.name}"
                )

        val deviceAnnotation = parameterContext?.parameter?.getAnnotation(Device::class.java)
        val requestedSerial = run {
            val annotationSerial = deviceAnnotation?.serial ?: ""
            if (annotationSerial.isNotEmpty()) {
                val keyed = System.getProperty("$PROP_DEVICE_SERIAL_PREFIX$annotationSerial")
                if (!keyed.isNullOrEmpty()) return@run keyed
            }
            val serialsProp = System.getProperty(PROP_DEVICE_SERIALS)
            if (!serialsProp.isNullOrEmpty()) {
                val list = serialsProp.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val index = parameterContext?.index
                if (index != null && index >= 0 && index < list.size) {
                    return@run list[index]
                }
            }
            val global = System.getProperty(PROP_DEVICE_SERIAL)
            if (!global.isNullOrEmpty()) return@run global
            annotationSerial
        }
        val requestedApi = run {
            val apisProp = System.getProperty(PROP_DEVICE_APIS)
            if (!apisProp.isNullOrEmpty()) {
                val list =
                    apisProp
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .mapNotNull { it.toIntOrNull() }
                val index = parameterContext?.index
                if (index != null && index >= 0 && index < list.size) {
                    return@run list[index]
                }
            }
            val globalApiProp = System.getProperty(PROP_DEVICE_API)
            if (!globalApiProp.isNullOrEmpty()) {
                val globalApi = globalApiProp.toIntOrNull()
                if (globalApi != null) return@run globalApi
            }
            deviceAnnotation?.api ?: 0
        }

        val devices =
            runBlocking {
                    withTimeoutOrNull(ADB_TIMEOUT_MS) {
                        adbSession.connectedDevicesTracker.connectedDevices.first { list ->
                            list.any { it.isOnline }
                        }
                    } ?: emptyList()
                }
                .filter { it.isOnline }

        if (devices.isEmpty()) {
            throw IllegalStateException(
                "No online Android devices or emulators were detected via ADB."
            )
        }

        // Filter devices based on annotation qualifiers
        val matchingDevices =
            devices
                .filter { device ->
                    val serialMatches =
                        requestedSerial.isEmpty() || device.serialNumber == requestedSerial
                    val apiMatches =
                        requestedApi == 0 ||
                            runBlocking { device.deviceProperties().api(0) } == requestedApi
                    serialMatches && apiMatches
                }
                .sortedBy { it.serialNumber } // Deterministic sorting alphanumerically by serial

        if (matchingDevices.isEmpty()) {
            val available =
                devices
                    .map { d ->
                        val api = runBlocking { d.deviceProperties().api(0) }
                        "${d.serialNumber} (API $api)"
                    }
                    .joinToString(", ")
            throw IllegalStateException(
                "Could not find any online devices matching requirements: API=$requestedApi, Serial='$requestedSerial'. " +
                    "Available devices: [$available]"
            )
        }

        // Resolve the matching device (default to parameter index distribution if multiple devices
        // match)
        val paramIndex = parameterContext?.index ?: 0
        val selectedDevice =
            if (requestedSerial.isEmpty() && paramIndex < matchingDevices.size) {
                matchingDevices[paramIndex]
            } else {
                matchingDevices.first()
            }
        val serial = selectedDevice.serialNumber
        val api = runBlocking { selectedDevice.deviceProperties().api(0) }

        if (api < MIN_REQUIRED_API) {
            throw IllegalStateException(
                "Backup & Restore testing requires Android 12 (API level 31) or higher. " +
                    "Detected device/emulator $serial is running API level $api."
            )
        }

        val deviceImpl =
            BackupRestoreControllerImpl(
                adbSession = adbSession,
                serialNumber = serial,
                apiLevel = api,
                applicationId = config.applicationId,
            )

        // Automatically install tested APKs (main app) and test APKs if provided by AGP test suite
        // task
        val propertiesFileEnv = System.getenv("com.android.junit.engine.input.parameters")
        var testedApkPath: String? = null
        var testApkPath: String? = null

        if (!propertiesFileEnv.isNullOrEmpty()) {
            val propertiesFile = java.io.File(propertiesFileEnv)
            if (propertiesFile.exists()) {
                try {
                    val props = java.util.Properties()
                    propertiesFile.reader(Charsets.UTF_8).use { props.load(it) }
                    testedApkPath = props.getProperty("com.android.agp.test.TESTED_APKS")
                    testApkPath = props.getProperty("com.android.agp.test.TEST_APKS")
                } catch (e: Exception) {
                    logger.warning("Failed to load parameters properties file: ${e.message}")
                }
            }
        }

        if (testedApkPath.isNullOrEmpty()) {
            testedApkPath = System.getProperty("com.android.agp.test.TESTED_APKS")
        }
        if (testApkPath.isNullOrEmpty()) {
            testApkPath = System.getProperty("com.android.agp.test.TEST_APKS")
        }

        fun installApkFromPath(path: String) {
            if (path.isEmpty()) return
            val file = java.io.File(path)
            if (file.exists()) {
                if (file.isDirectory) {
                    file.listFiles()?.forEach { child ->
                        if (child.name.endsWith(".apk", ignoreCase = true)) {
                            logger.info("Automatically installing child APK: ${child.absolutePath}")
                            runBlocking { deviceImpl.installApk(child.toPath()) }
                        }
                    }
                } else if (file.name.endsWith(".apk", ignoreCase = true)) {
                    logger.info("Automatically installing APK: ${file.absolutePath}")
                    runBlocking { deviceImpl.installApk(file.toPath()) }
                }
            }
        }

        if (!testedApkPath.isNullOrEmpty()) {
            logger.info("Automatically installing tested APK from: $testedApkPath")
            testedApkPath.split(java.io.File.pathSeparator).forEach { path ->
                installApkFromPath(path)
            }
        }
        if (!testApkPath.isNullOrEmpty()) {
            logger.info("Automatically installing test APK from: $testApkPath")
            testApkPath.split(java.io.File.pathSeparator).forEach { path ->
                installApkFromPath(path)
            }
        }

        // Proactively unlock the emulator lockscreen/keyguard to ensure Credential Protected
        // storage is decrypted and accessible
        runBlocking {
            try {
                val selector = com.android.adblib.DeviceSelector.fromSerialNumber(serial)
                adbSession.deviceServices.shellAsText(selector, "wm dismiss-keyguard")
                adbSession.deviceServices.shellAsText(selector, "input keyevent 82")
            } catch (e: Exception) {
                logger.info("Failed to proactively dismiss keyguard: ${e.message}")
            }
        }

        // Run automatic pm clear before the test starts if policy is AUTOMATIC
        val testMethod = extensionContext?.requiredTestMethod
        val testClass = extensionContext?.requiredTestClass
        val sandboxIsolation =
            testMethod?.getAnnotation(Isolation::class.java)
                ?: testClass?.getAnnotation(Isolation::class.java)
        val policy = sandboxIsolation?.value ?: IsolationPolicy.AUTOMATIC

        if (policy == IsolationPolicy.AUTOMATIC) {
            logger.info("Initializing test. Running automatic pm clear for isolation.")
            runBlocking { deviceImpl.clearAppData() }
        }

        // Register standalone session for automatic disposal at root context close to prevent
        // memory/socket leaks
        if (isStandalone) {
            val store = extensionContext.root.getStore(ExtensionContext.Namespace.GLOBAL)
            val key = BackupRestoreExtension::class.java.name + "_session"
            if (store.get(key) == null) {
                store.put(
                    key,
                    CloseableResource {
                        try {
                            adbSession.close()
                        } catch (e: Exception) {
                            // ignore
                        }
                    },
                )
            }
        }

        return deviceImpl
    }

    /** Configuration constants and system property keys for device resolution and setup. */
    private companion object {
        /** Property specifying a comma-separated list of device serial numbers. */
        private const val PROP_DEVICE_SERIALS = "androidx.test.backup.device.serials"

        /** Property prefix for binding specific serials to test parameters. */
        private const val PROP_DEVICE_SERIAL_PREFIX = "androidx.test.backup.device.serial."

        /** Global property specifying a single fallback device serial number. */
        private const val PROP_DEVICE_SERIAL = "androidx.test.backup.device.serial"

        /** Property specifying a comma-separated list of device API levels. */
        private const val PROP_DEVICE_APIS = "androidx.test.backup.device.apis"

        /** Global property specifying a fallback device API level. */
        private const val PROP_DEVICE_API = "androidx.test.backup.device.api"

        /** Timeout limit for discovering connected devices via ADB. */
        private const val ADB_TIMEOUT_MS = 30000L

        /** Minimum Android API level required for backup/restore capability (Android 12). */
        private const val MIN_REQUIRED_API = 31
    }
}
