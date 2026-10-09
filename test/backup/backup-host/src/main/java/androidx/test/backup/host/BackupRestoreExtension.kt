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

import androidx.annotation.VisibleForTesting
import com.android.adblib.AdbSession
import com.android.adblib.connectedDevicesTracker
import com.android.adblib.deviceProperties
import com.android.adblib.isOnline
import com.android.adblib.serialNumber
import com.android.adblib.tools.createStandaloneSession
import java.io.File
import java.lang.reflect.Modifier
import java.lang.reflect.Parameter
import java.util.Properties
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
 *
 * Each controller targets the app named by the test class's [BackupRestoreConfig]. Without one, it
 * targets the tested app that the test suite properties name.
 */
public class BackupRestoreExtension
internal constructor(
    private val isStandalone: Boolean,
    private val adbSessionProvider: () -> AdbSession,
) : ParameterResolver {

    /**
     * Creates a standalone extension, which manages its own ADB session.
     *
     * All standalone extensions in a test run share one session. It is created when a test first
     * requests a [BackupRestoreController] and closed when the test run ends.
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

    private val logger = Logger.getLogger(BackupRestoreExtension::class.java.name)

    /**
     * Returns the ADB session to resolve parameters with in [context]. Callers must not close it.
     *
     * A standalone session is kept in the store of the root context, so that every standalone
     * extension of the test run shares it, and JUnit closes it when the test run ends. A shared
     * session stays open until the code that passed it to the constructor closes it.
     */
    internal fun sessionFor(context: ExtensionContext): AdbSession {
        if (!isStandalone) return adbSessionProvider()
        return context.root
            .getStore(NAMESPACE)
            .getOrComputeIfAbsent(
                STANDALONE_SESSION_KEY,
                { StandaloneSession(adbSessionProvider()) },
                StandaloneSession::class.java,
            )
            .session
    }

    override fun supportsParameter(
        parameterContext: ParameterContext?,
        extensionContext: ExtensionContext?,
    ): Boolean {
        return parameterContext?.parameter?.let { isController(it) } ?: false
    }

    /**
     * Returns the position of the parameter of [parameterContext] among the
     * [BackupRestoreController] parameters of its method or constructor.
     *
     * Device serials, API levels and online devices are assigned to controllers in this order, so
     * that parameters of other types, such as a `@TempDir` path, do not shift the assignment.
     */
    @VisibleForTesting
    internal fun deviceIndexOf(parameterContext: ParameterContext): Int =
        parameterContext.parameter.declaringExecutable.parameters
            .take(parameterContext.index)
            .count { isController(it) }

    /**
     * Returns the [annotationClass] annotation of [testClass], or else of the innermost class that
     * encloses it and has one. Only inner classes, such as `@Nested` test classes, take the
     * annotations of the classes that enclose them.
     */
    internal fun <A : Annotation> findClassAnnotation(
        testClass: Class<*>,
        annotationClass: Class<A>,
    ): A? =
        generateSequence(testClass) { cls ->
                cls.enclosingClass.takeIf { cls.isMemberClass && !Modifier.isStatic(cls.modifiers) }
            }
            .firstNotNullOfOrNull { it.getAnnotation(annotationClass) }

    override fun resolveParameter(
        parameterContext: ParameterContext?,
        extensionContext: ExtensionContext?,
    ): Any {

        val requiredClass =
            extensionContext?.requiredTestClass
                ?: throw IllegalStateException("Required test class is missing")
        val suiteProperties = suiteProperties()
        val applicationId = applicationIdFor(requiredClass, suiteProperties)
        val adbSession = sessionFor(extensionContext)
        val deviceIndex = parameterContext?.let { deviceIndexOf(it) } ?: 0

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
                if (deviceIndex < list.size) {
                    return@run list[deviceIndex]
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
                if (deviceIndex < list.size) {
                    return@run list[deviceIndex]
                }
            }
            val globalApiProp = System.getProperty(PROP_DEVICE_API)
            if (!globalApiProp.isNullOrEmpty()) {
                val globalApi = globalApiProp.toIntOrNull()
                if (globalApi != null) return@run globalApi
            }
            deviceAnnotation?.api ?: 0
        }

        val devices = runBlocking {
            withTimeoutOrNull(ADB_TIMEOUT_MS) {
                adbSession.connectedDevicesTracker.connectedDevices.first { list ->
                    list.any { it.isOnline }
                }
            } ?: emptyList()
        }
            .filter { it.isOnline }

        if (devices.isEmpty()) {
            val errorMsg = "No online Android devices or emulators were detected via ADB."
            extensionContext.reportPreconditionFailure(BackupErrorCode.NO_ONLINE_DEVICE, errorMsg)
            throw IllegalStateException(errorMsg)
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
            val errorMsg =
                "Could not find any online devices matching requirements: API=$requestedApi, Serial='$requestedSerial'. " +
                    "Available devices: [$available]"
            extensionContext.reportPreconditionFailure(BackupErrorCode.NO_MATCHING_DEVICE, errorMsg)
            throw IllegalStateException(errorMsg)
        }

        // Resolve the matching device (default to distributing them over the controller parameters
        // if multiple devices match)
        val selectedDevice =
            if (requestedSerial.isEmpty() && deviceIndex < matchingDevices.size) {
                matchingDevices[deviceIndex]
            } else {
                matchingDevices.first()
            }
        val serial = selectedDevice.serialNumber
        val api = runBlocking { selectedDevice.deviceProperties().api(0) }

        if (api < MIN_REQUIRED_API) {
            val errorMsg =
                "Backup & Restore testing requires Android 12 (API level 31) or higher. " +
                    "Detected device/emulator $serial is running API level $api."
            extensionContext.reportPreconditionFailure(
                BackupErrorCode.UNSUPPORTED_API_LEVEL,
                errorMsg,
            )
            throw IllegalStateException(errorMsg)
        }

        val deviceImpl =
            BackupRestoreControllerImpl(
                adbSession = adbSession,
                serialNumber = serial,
                apiLevel = api,
                applicationId = applicationId,
                telemetryPublisher = { key, value ->
                    extensionContext.publishReportEntry(key, value)
                },
            )

        // Automatically install tested APKs (main app) and test APKs if provided by AGP test suite
        // task
        runBlocking { installOnce(extensionContext, deviceImpl, suiteApks(suiteProperties)) }

        // Proactively unlock the emulator lockscreen/keyguard to ensure Credential Protected
        // storage is decrypted and accessible
        runBlocking {
            try {
                deviceImpl.wakeAndDismissKeyguard()
            } catch (e: Exception) {
                logger.info("Failed to proactively dismiss keyguard: ${e.message}")
            }
        }

        // Run automatic pm clear before the test starts if policy is AUTOMATIC
        if (isolationPolicyFor(extensionContext) == IsolationPolicy.AUTOMATIC) {
            logger.info("Initializing test. Running automatic pm clear for isolation.")
            runBlocking { deviceImpl.clearAppData() }
        }

        return deviceImpl
    }

    /**
     * Returns the isolation policy of a controller provided in [context]: the one that the
     * [Isolation] annotation of its test method names, or else that of its test class.
     *
     * A controller that a test class constructor or a `@BeforeAll` method receives has no test
     * method, so it takes the policy of the class.
     */
    internal fun isolationPolicyFor(context: ExtensionContext): IsolationPolicy {
        val isolation =
            context.testMethod.orElse(null)?.getAnnotation(Isolation::class.java)
                ?: findClassAnnotation(context.requiredTestClass, Isolation::class.java)
        return isolation?.value ?: IsolationPolicy.AUTOMATIC
    }

    /**
     * Installs [apks] on the device of [controller], unless this test run already installed them
     * there and neither the app nor its test APK has been installed again or uninstalled since.
     *
     * The installed APK paths identify an installation, since they change on every install. A test
     * that installs another version of the app or of its test APK, or uninstalls one of them, thus
     * gets [apks] installed again for the next test.
     */
    internal suspend fun installOnce(
        context: ExtensionContext,
        controller: BackupRestoreControllerImpl,
        apks: List<File>,
    ) {
        if (apks.isEmpty()) return
        val store = context.root.getStore(NAMESPACE)
        val key = InstalledApp(controller.serialNumber, controller.applicationId, apks)
        val installed = store.get(key)
        if (installed != null && installed == controller.installedApkPaths()) {
            logger.info("APKs already installed on ${controller.serialNumber}.")
            return
        }
        for (apk in apks) {
            logger.info("Automatically installing APK: ${apk.absolutePath}")
            controller.installApk(apk.toPath())
        }
        store.put(key, controller.installedApkPaths())
    }

    /**
     * Returns the APK files in [paths]: APK files and directories of APK files, separated by
     * [File.pathSeparator].
     */
    internal fun apkFilesIn(paths: String?): List<File> =
        paths
            .orEmpty()
            .split(File.pathSeparator)
            .filter { it.isNotEmpty() }
            .map { File(it) }
            .flatMap { if (it.isDirectory) it.listFiles().orEmpty().asList() else listOf(it) }
            .filter { it.isFile && it.name.endsWith(".apk", ignoreCase = true) }

    /**
     * Returns the inputs that an Android Gradle Plugin test suite passes to its tests in the
     * properties file at [path], or no properties outside of a test suite or if the file can't be
     * read.
     */
    internal fun suiteProperties(path: String? = System.getenv(SUITE_PROPERTIES_ENV)): Properties {
        val file = path?.takeIf { it.isNotEmpty() }?.let { File(it) }
        if (file == null || !file.exists()) return Properties()
        return try {
            Properties().apply { file.reader(Charsets.UTF_8).use { load(it) } }
        } catch (e: Exception) {
            logger.warning("Failed to load parameters properties file: ${e.message}")
            Properties()
        }
    }

    /**
     * Returns the ID of the app that the tests of [testClass] target: the one that its
     * [BackupRestoreConfig] names, or else the one of the app that the test suite of
     * [suiteProperties] tests.
     */
    internal fun applicationIdFor(testClass: Class<*>, suiteProperties: Properties): String =
        findClassAnnotation(testClass, BackupRestoreConfig::class.java)?.applicationId
            ?: suiteProperty(suiteProperties, PROP_TESTED_APPLICATION_ID)
            ?: throw IllegalStateException(
                "No application ID for ${testClass.name}: annotate it with @BackupRestoreConfig, " +
                    "or run it in an Android Gradle Plugin backup test suite."
            )

    /** Returns the APK files of the tested app, then of the test app, of [suiteProperties]. */
    internal fun suiteApks(suiteProperties: Properties): List<File> =
        apkFilesIn(suiteProperty(suiteProperties, PROP_TESTED_APKS)) +
            apkFilesIn(suiteProperty(suiteProperties, PROP_TEST_APKS))

    /** Returns the test suite input [name] of [suiteProperties], or else the system property. */
    private fun suiteProperty(suiteProperties: Properties, name: String): String? =
        suiteProperties.getProperty(name)?.takeIf { it.isNotEmpty() }
            ?: System.getProperty(name)?.takeIf { it.isNotEmpty() }

    /**
     * Publishes the report entries describing a precondition failure, so that failures detected
     * before a [BackupRestoreController] exists are reported with the same vocabulary that
     * [BackupRestoreControllerImpl] uses for completed executions.
     */
    private fun ExtensionContext?.reportPreconditionFailure(
        errorCode: BackupErrorCode,
        errorMessage: String,
    ) {
        val context = this ?: return
        context.publishReportEntry(BackupReportKeys.STATUS, BackupReportKeys.REPORT_STATUS_FAILURE)
        context.publishReportEntry(BackupReportKeys.ERROR_CODE, errorCode.name)
        context.publishReportEntry(
            BackupReportKeys.FAILURE_STAGE,
            BackupExecutionStage.PRECONDITION.name,
        )
        context.publishReportEntry(BackupReportKeys.ERROR_MESSAGE, errorMessage)
    }

    /** A standalone ADB session, which JUnit closes when the context it is stored in ends. */
    private class StandaloneSession(val session: AdbSession) : CloseableResource {
        override fun close() {
            try {
                session.close()
            } catch (e: Exception) {
                Logger.getLogger(BackupRestoreExtension::class.java.name)
                    .warning("Failed to close the ADB session: ${e.message}")
            }
        }
    }

    /** Store key of the installed APK paths after [installOnce] installed [apks] on a device. */
    private data class InstalledApp(
        val serialNumber: String,
        val applicationId: String,
        val apks: List<File>,
    )

    /** Configuration constants and system property keys for device resolution and setup. */
    private companion object {
        /** Store namespace for the state that this extension keeps in extension contexts. */
        private val NAMESPACE =
            ExtensionContext.Namespace.create(BackupRestoreExtension::class.java)

        /** Store key of the [StandaloneSession] that all standalone extensions share. */
        private const val STANDALONE_SESSION_KEY = "standaloneSession"

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

        /**
         * Environment variable with the path of the properties file in which an Android Gradle
         * Plugin test suite passes its inputs to the tests.
         */
        private const val SUITE_PROPERTIES_ENV = "com.android.junit.engine.input.parameters"

        /** Test suite input with the application ID of the tested app. */
        private const val PROP_TESTED_APPLICATION_ID =
            "com.android.junit.engine.tested.application.id"

        /** Test suite input with the APK files of the tested app. */
        private const val PROP_TESTED_APKS = "com.android.agp.test.TESTED_APKS"

        /** Test suite input with the APK files of the test app. */
        private const val PROP_TEST_APKS = "com.android.agp.test.TEST_APKS"

        /** Timeout limit for discovering connected devices via ADB. */
        private const val ADB_TIMEOUT_MS = 30000L

        /** Minimum Android API level required for backup/restore capability (Android 12). */
        private const val MIN_REQUIRED_API = 31

        /**
         * Returns whether [parameter] is a [BackupRestoreController], which this extension
         * resolves.
         */
        private fun isController(parameter: Parameter): Boolean =
            parameter.type == BackupRestoreController::class.java
    }
}
