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
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Modifier
import java.lang.reflect.Parameter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.Locale
import java.util.Properties
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.opentest4j.TestAbortedException

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
 *
 * When a test fails, the extension saves the logs of each device that the test used while it ran,
 * and names the saved files in the test failure. Logs aren't saved for aborted tests, such as a
 * failed assumption. To also save them when a `@BeforeAll` or `@AfterAll` method fails, register
 * the extension on the test class, with `@ExtendWith` or a static `@RegisterExtension` field. To
 * turn off saving logs, set the system property `androidx.test.backup.device.logs.enabled` to
 * `false`. To also save device logs when tests pass, set
 * `androidx.test.backup.report.captureLogcatOnPass` to `true`; to copy raw backup archives into the
 * report artifacts, set `androidx.test.backup.report.includeRawArchives` to `true`.
 *
 * The files are in `suite-details` in the results directory of the test suite. Outside of a test
 * suite, they are in a new temporary directory, which the extension keeps after the test run.
 */
public class BackupRestoreExtension
internal constructor(private val adbSessionProvider: () -> AdbSession) :
    BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback, ParameterResolver {

    /**
     * Creates an extension, which manages its own ADB session.
     *
     * All extensions in a test run share one session. It is created when a test first requests a
     * [BackupRestoreController] and closed when the test run ends.
     */
    public constructor() : this({ createStandaloneSession() })

    private val logger = Logger.getLogger(BackupRestoreExtension::class.java.name)

    /**
     * Returns the ADB session to resolve parameters with in [context]. Callers must not close it.
     *
     * The session is kept in the store of the root context, so that every extension of the test run
     * shares it, and JUnit closes it when the test run ends.
     */
    internal fun sessionFor(context: ExtensionContext): AdbSession =
        context.root
            .getStore(NAMESPACE)
            .getOrComputeIfAbsent(
                STANDALONE_SESSION_KEY,
                { StandaloneSession(adbSessionProvider()) },
                StandaloneSession::class.java,
            )
            .session

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
    ): A? = classAnnotations(testClass, annotationClass).firstOrNull()

    /**
     * Returns the [annotationClass] annotations of [testClass] and of the classes that enclose it,
     * from innermost to outermost. Only inner classes, such as `@Nested` test classes, take the
     * annotations of the classes that enclose them.
     */
    private fun <A : Annotation> classAnnotations(
        testClass: Class<*>,
        annotationClass: Class<A>,
    ): Sequence<A> =
        generateSequence(testClass) { cls ->
                cls.enclosingClass.takeIf { cls.isMemberClass && !Modifier.isStatic(cls.modifiers) }
            }
            .mapNotNull { it.getAnnotation(annotationClass) }

    override fun resolveParameter(
        parameterContext: ParameterContext?,
        extensionContext: ExtensionContext?,
    ): Any {
        val requiredClass =
            extensionContext?.requiredTestClass
                ?: throw IllegalStateException("Required test class is missing")
        val deviceIndex = parameterContext?.let { deviceIndexOf(it) } ?: 0
        val isConstructorParam = parameterContext?.parameter?.declaringExecutable is Constructor<*>
        val isPerMethodClassConstructor =
            isConstructorParam &&
                isClassLevelContext(extensionContext) &&
                isPerMethodLifecycle(extensionContext)
        val wasBoundBeforeResolve =
            !isConstructorParam &&
                captureOrNull(extensionContext)
                    ?.let(ActiveCaptureContext::isBoundOnCurrentThread) == true
        val pendingAttempt =
            if (isPerMethodClassConstructor) {
                ensurePendingInstanceCapture(
                    extensionContext,
                    isFirstControllerParam = deviceIndex == 0,
                )
            } else {
                null
            }
        val capture = pendingAttempt?.capture ?: ensureActiveCapture(extensionContext)
        try {
            val suiteProperties = suiteProperties()
            val applicationId = applicationIdFor(requiredClass, suiteProperties)
            val testApplicationId = testApplicationIdFor(requiredClass)
            val adbSession = sessionFor(extensionContext)
            val requests =
                parameterContext?.let { deviceRequestsOf(it.parameter.declaringExecutable) }
                    ?: listOf(DeviceRequest(requestedSerial(null, 0), requestedApi(null, 0)))
            val request = requests[deviceIndex]
            val role =
                parameterContext?.parameter?.getAnnotation(Device::class.java)?.role.orEmpty()

            val devices = runBlocking {
                val connected =
                    withTimeoutOrNull(ADB_TIMEOUT_MS) {
                        adbSession.connectedDevicesTracker.connectedDevices.first { list ->
                            list.any { it.isOnline }
                        }
                    } ?: emptyList()
                connected
                    .filter { it.isOnline }
                    .map { device ->
                        async {
                            OnlineDevice(device.serialNumber, device.deviceProperties().api(0))
                        }
                    }
                    .awaitAll()
            }

            if (devices.isEmpty()) {
                val errorMsg = "No online Android devices or emulators were detected via ADB."
                capture.preconditionFailed = true
                extensionContext.reportPreconditionFailure(
                    BackupErrorCode.NO_ONLINE_DEVICE,
                    errorMsg,
                )
                throw IllegalStateException(errorMsg)
            }

            val assignedSerials = assignDevices(requests, devices)
            val serial = assignedSerials[deviceIndex]
            if (serial == null) {
                val errorMsg = noMatchingDeviceMessage(request, role, devices)
                capture.preconditionFailed = true
                extensionContext.reportPreconditionFailure(
                    BackupErrorCode.NO_MATCHING_DEVICE,
                    errorMsg,
                )
                throw IllegalStateException(errorMsg)
            }
            // A parameter without a serial number shares a device only when every device that
            // matches it is bound to, or already taken by, another parameter.
            if (
                request.serialNumber.isEmpty() &&
                    (requests.any { it.serialNumber == serial } ||
                        serial in assignedSerials.take(deviceIndex))
            ) {
                logger.warning(
                    "Controller parameter $deviceIndex" +
                        (if (role.isNotEmpty()) " (role '$role')" else "") +
                        " shares device $serial with another controller parameter, since no other " +
                        "online device matches it."
                )
            }
            val api = devices.first { it.serialNumber == serial }.api

            if (api < MIN_REQUIRED_API) {
                val errorMsg =
                    "Backup & Restore testing requires Android 12 (API level 31) or higher. " +
                        "Detected device/emulator $serial is running API level $api."
                capture.preconditionFailed = true
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
                    testApplicationId = testApplicationId,
                    telemetryPublisher = { key, value ->
                        extensionContext.publishReportEntry(key, value)
                    },
                )
            deviceImpl.explicitRole = role
            deviceImpl.cachedDeviceFacts = cachedDeviceFactsFor(extensionContext, deviceImpl)
            capture.addParticipant(deviceImpl)
            prepareResolvedController(
                extensionContext,
                capture,
                deviceImpl,
                suiteApks(suiteProperties),
            )

            return deviceImpl
        } catch (t: Throwable) {
            if (pendingAttempt != null) {
                pendingAttempt.failure = t
                ActiveCaptureContext.unbindCurrentThread(capture)
            } else {
                if (!wasBoundBeforeResolve) {
                    ActiveCaptureContext.unbindCurrentThread(capture)
                    // When constructor parameter resolution fails on a method-scoped context
                    // before beforeEach runs, JUnit skips afterEach and closes the store directly.
                    capture.onUnfinalizedClose = {
                        val failure = extensionContext.executionException?.orElse(null) ?: t
                        finalizeCapture(
                            context = extensionContext,
                            capture = capture,
                            isClassLevel = isClassLevelContext(extensionContext),
                            explicitFailure = failure,
                        )
                    }
                }
            }
            throw t
        }
    }

    /**
     * Prepares [controller] by installing APKs, unlocking the device, and isolating app data.
     *
     * Records the combined preparation work as an unnumbered `"Prepare"` step on [capture].
     */
    @VisibleForTesting
    internal fun prepareResolvedController(
        context: ExtensionContext,
        capture: TestExecutionCapture = ensureActiveCapture(context),
        controller: BackupRestoreControllerImpl,
        apks: List<File>,
    ) {
        val autoClear = isolationPolicyFor(context) == IsolationPolicy.AUTOMATIC
        val prepareCall = buildPrepareCallDescription(apks.isNotEmpty(), autoClear)

        runBlocking(capture.coroutineContext()) {
            capture.runStep(
                isNumbered = false,
                kind = TestExecutionCapture.StepKind.PREPARE,
                name = "Prepare",
                controller = controller,
                call = prepareCall,
            ) {
                // Automatically install tested APKs (main app) and test APKs if provided by AGP
                // test suite task
                installOnce(context, controller, apks)

                // Proactively unlock the emulator lockscreen/keyguard to ensure Credential
                // Protected storage is decrypted and accessible
                try {
                    controller.wakeAndDismissKeyguard()
                } catch (e: Exception) {
                    logger.info("Failed to proactively dismiss keyguard: ${e.message}")
                }

                // Run automatic pm clear before the test starts if policy is AUTOMATIC
                if (autoClear) {
                    logger.info("Initializing test. Running automatic pm clear for isolation.")
                    controller.clearAppData()
                }
            }
        }
    }

    // JUnit never passes a null context, but its parameters have no nullness annotations.
    override fun beforeAll(@Suppress("InvalidNullabilityOverride") context: ExtensionContext) {
        generateSequence(context.parent?.orElse(null)) { it.parent?.orElse(null) }
            .forEach(::evictUnclaimedPendingCapture)
        ensureActiveCapture(context, forceClassLevel = true)
    }

    // JUnit never passes a null context, but its parameters have no nullness annotations.
    override fun beforeEach(@Suppress("InvalidNullabilityOverride") context: ExtensionContext) {
        generateSequence(context.parent?.orElse(null)) { it.parent?.orElse(null) }
            .forEach { captureOrNull(it)?.sealSharedPrefix() }
        claimPendingInstanceCapture(context)
        ensureActiveCapture(context, forceClassLevel = false)
    }

    // JUnit never passes a null context, but its parameters have no nullness annotations.
    override fun afterEach(@Suppress("InvalidNullabilityOverride") context: ExtensionContext) {
        if (captureOrNull(context) == null && context.executionException?.orElse(null) != null) {
            claimPendingInstanceCapture(context)
        }
        finalizeExecution(context, isClassLevel = false)
    }

    // JUnit never passes a null context, but its parameters have no nullness annotations.
    override fun afterAll(@Suppress("InvalidNullabilityOverride") context: ExtensionContext) {
        generateSequence(context) { it.parent?.orElse(null) }
            .forEach(::evictUnclaimedPendingCapture)
        finalizeExecution(context, isClassLevel = true)
    }

    /** Records that the test or the test class of [context] uses [controller]. */
    @VisibleForTesting
    internal fun track(context: ExtensionContext, controller: BackupRestoreControllerImpl) {
        captureOf(context, isClassLevel = isClassLevelContext(context)).addParticipant(controller)
    }

    /** Returns cached [DeviceFacts] for [controller], reading from the device once per serial. */
    internal fun cachedDeviceFactsFor(
        context: ExtensionContext,
        controller: BackupRestoreControllerImpl,
    ): DeviceFacts =
        context.root
            .getStore(NAMESPACE)
            .getOrComputeIfAbsent(
                DeviceFactsKey(controller.serialNumber),
                {
                    runBlocking {
                        DeviceFactsReader.read(controller.deviceShell, controller.apiLevel)
                    }
                },
                DeviceFacts::class.java,
            )

    private fun isPerMethodLifecycle(context: ExtensionContext): Boolean =
        when (context.testInstanceLifecycle?.orElse(null)) {
            TestInstance.Lifecycle.PER_CLASS -> false
            TestInstance.Lifecycle.PER_METHOD -> true
            null ->
                findClassAnnotation(context.requiredTestClass, TestInstance::class.java)?.value !=
                    TestInstance.Lifecycle.PER_CLASS
        }

    /**
     * Returns a per-instance [PendingInstanceCapture] for a [TestInstance.Lifecycle.PER_METHOD]
     * constructor parameter.
     *
     * Keeps constructor `"Prepare"` steps and logs off the enclosing class capture, and registers
     * [TestExecutionCapture.onUnfinalizedClose] under a unique `_ctor_` detail ID in case
     * construction fails before [beforeEach] runs.
     */
    private fun ensurePendingInstanceCapture(
        classContext: ExtensionContext,
        isFirstControllerParam: Boolean,
    ): PendingInstanceCapture {
        generateSequence(classContext) { it.parent?.orElse(null) }
            .forEach { captureOrNull(it)?.sealSharedPrefix() }
        val parentClassCapture =
            generateSequence(classContext) { it.parent?.orElse(null) }
                .firstNotNullOfOrNull(::captureOrNull)
        val store = classContext.getStore(NAMESPACE)
        val pendingKey = PendingInstanceCaptureKey(classContext.uniqueId)
        if (isFirstControllerParam) {
            store.remove(pendingKey, PendingInstanceCapture::class.java)?.close()
            val adoptedFromOuter =
                generateSequence(classContext.parent?.orElse(null)) { it.parent?.orElse(null) }
                    .firstNotNullOfOrNull { ancestor ->
                        ancestor
                            .getStore(NAMESPACE)
                            .remove(
                                PendingInstanceCaptureKey(ancestor.uniqueId),
                                PendingInstanceCapture::class.java,
                            )
                    }
            if (adoptedFromOuter != null) {
                adoptedFromOuter.capture.testClassName = resolveTestClassName(classContext)
                adoptedFromOuter.capture.parent = parentClassCapture
                bindPendingUnfinalizedClose(classContext, adoptedFromOuter)
                store.put(pendingKey, adoptedFromOuter)
            }
        }
        val pending =
            store.getOrComputeIfAbsent(
                pendingKey,
                {
                    val ctorSeq = ctorAttemptCounter.incrementAndGet()
                    val ctorDetailId = "${detailIdOf(classContext.uniqueId)}_ctor_$ctorSeq"
                    val created =
                        TestExecutionCapture(
                            start = Instant.now(),
                            isShared = false,
                            testClassName = resolveTestClassName(classContext),
                            testMethodName = null,
                            parent = parentClassCapture,
                        )
                    PendingInstanceCapture(created, ctorDetailId).also { createdPending ->
                        bindPendingUnfinalizedClose(classContext, createdPending)
                    }
                },
                PendingInstanceCapture::class.java,
            )
        ActiveCaptureContext.pushCurrentThread(pending.capture)
        return pending
    }

    private fun bindPendingUnfinalizedClose(
        classContext: ExtensionContext,
        pending: PendingInstanceCapture,
    ) {
        pending.capture.onUnfinalizedClose = {
            val failure = pending.failure ?: classContext.executionException?.orElse(null)
            if (
                failure != null &&
                    (!pending.capture.preconditionFailed ||
                        pending.capture.allControllers().isNotEmpty())
            ) {
                finalizeCapture(
                    context = classContext,
                    capture = pending.capture,
                    isClassLevel = false,
                    explicitFailure = failure,
                    detailIdOverride = pending.detailId,
                    allowPublishReportEntries = false,
                )
            } else {
                ActiveCaptureContext.unbindCurrentThread(pending.capture)
            }
        }
    }

    /**
     * Moves any pending [TestInstance.Lifecycle.PER_METHOD] constructor capture into
     * [methodContext]'s store.
     */
    private fun claimPendingInstanceCapture(methodContext: ExtensionContext) {
        val claimed =
            generateSequence(methodContext.parent?.orElse(null)) { it.parent?.orElse(null) }
                .firstNotNullOfOrNull { ancestor ->
                    ancestor
                        .getStore(NAMESPACE)
                        .remove(
                            PendingInstanceCaptureKey(ancestor.uniqueId),
                            PendingInstanceCapture::class.java,
                        )
                }
                ?.capture ?: return
        claimed.testClassName = resolveTestClassName(methodContext)
        claimed.testMethodName = resolveTestMethodName(methodContext)
        claimed.parent =
            generateSequence(methodContext.parent?.orElse(null)) { it.parent?.orElse(null) }
                .firstNotNullOfOrNull(::captureOrNull)
        claimed.onUnfinalizedClose = {
            if (methodContext.executionException?.orElse(null) != null) {
                finalizeExecution(methodContext, isClassLevel = false)
            }
        }
        methodContext.getStore(NAMESPACE).put(TestCaptureKey(methodContext.uniqueId), claimed)
    }

    private fun evictUnclaimedPendingCapture(classContext: ExtensionContext) {
        classContext
            .getStore(NAMESPACE)
            .remove(
                PendingInstanceCaptureKey(classContext.uniqueId),
                PendingInstanceCapture::class.java,
            )
            ?.close()
    }

    private fun ensureActiveCapture(
        context: ExtensionContext,
        forceClassLevel: Boolean? = null,
    ): TestExecutionCapture {
        val isClassLevel = forceClassLevel ?: isClassLevelContext(context)
        val capture = captureOf(context, isClassLevel)
        ActiveCaptureContext.pushCurrentThread(capture)
        return capture
    }

    /**
     * Returns or creates the [TestExecutionCapture] for [context].
     *
     * Creating on first access lets constructor injection before [beforeEach] share the test's
     * capture.
     */
    private fun captureOf(context: ExtensionContext, isClassLevel: Boolean): TestExecutionCapture =
        context
            .getStore(NAMESPACE)
            .getOrComputeIfAbsent(
                TestCaptureKey(context.uniqueId),
                {
                    val parentCapture =
                        generateSequence(context.parent?.orElse(null)) {
                                it.parent?.orElse(null)
                            }
                            .mapNotNull { captureOrNull(it) }
                            .firstOrNull()
                    TestExecutionCapture(
                            start = Instant.now(),
                            isShared = isClassLevel,
                            testClassName = resolveTestClassName(context),
                            testMethodName =
                                if (isClassLevel) null else resolveTestMethodName(context),
                            parent = parentCapture,
                        )
                        .also { created ->
                            created.onUnfinalizedClose = {
                                if (context.executionException?.orElse(null) != null) {
                                    finalizeExecution(context, isClassLevel)
                                }
                            }
                        }
                },
                TestExecutionCapture::class.java,
            )

    private fun captureOrNull(context: ExtensionContext): TestExecutionCapture? =
        context
            .getStore(NAMESPACE)
            .get(TestCaptureKey(context.uniqueId), TestExecutionCapture::class.java)

    /** Finalizes the [TestExecutionCapture] recorded for [context], if one exists. */
    private fun finalizeExecution(context: ExtensionContext, isClassLevel: Boolean) {
        // Without its own capture, the test didn't run: another extension's beforeEach failed
        // first.
        val capture = captureOrNull(context) ?: return
        finalizeCapture(context, capture, isClassLevel)
    }

    private fun finalizeCapture(
        context: ExtensionContext,
        capture: TestExecutionCapture,
        isClassLevel: Boolean,
        explicitFailure: Throwable? = null,
        detailIdOverride: String? = null,
        allowPublishReportEntries: Boolean = true,
    ) {
        if (capture.finalized) {
            ActiveCaptureContext.unbindCurrentThread(capture)
            return
        }
        capture.finalized = true

        ActiveCaptureContext.pushCurrentThread(capture)
        try {
            val failure = explicitFailure ?: context.executionException?.orElse(null)
            if (failure is TestAbortedException) return

            val suiteProps = suiteProperties()
            val resultsDirPath = suiteProperty(suiteProps, PROP_RESULTS_DIR)
            val detailId = detailIdOverride ?: detailIdOf(context.uniqueId)

            val deviceLogsEnabled =
                !suiteProperty(suiteProps, PROP_DEVICE_LOGS).equals("false", ignoreCase = true)
            val captureLogcatOnPass =
                suiteProperty(suiteProps, PROP_CAPTURE_LOGCAT_ON_PASS)
                    .equals("true", ignoreCase = true)
            val shouldSaveLogcat =
                deviceLogsEnabled && (failure != null || (!isClassLevel && captureLogcatOnPass))

            val savedLogcatFiles = LinkedHashMap<String, TestExecutionCapture.SavedLogcatFile>()
            if (shouldSaveLogcat) {
                val controllers = capture.allControllers()
                // The device clock that dates the logs is read to the second.
                val window = Duration.between(capture.start, Instant.now()).plusSeconds(1)
                for (controller in controllers) {
                    try {
                        val dir = deviceLogsDir(context, suiteProps).resolve(detailId)
                        val file =
                            Files.createDirectories(dir)
                                .resolve("logcat-${fileSafe(controller.serialNumber)}.txt")
                        val logcatTruncated = runBlocking {
                            withTimeout(DEVICE_LOGS_TIMEOUT_MS) {
                                controller.saveBoundedDeviceLogs(file, window, MAX_LOGCAT_BYTES)
                            }
                        }
                        logger.info("Saved the logs of ${controller.serialNumber} to $file")
                        failure?.addSuppressed(DeviceLogsNotice(file))
                        savedLogcatFiles[controller.serialNumber] =
                            TestExecutionCapture.SavedLogcatFile(file, logcatTruncated)
                    } catch (e: Exception) {
                        logger.warning(
                            "Failed to save the logs of ${controller.serialNumber}: ${e.message}"
                        )
                    }
                }
            }

            val shouldWriteDetailRecord =
                resultsDirPath != null && (!isClassLevel || failure != null)
            val finalized =
                try {
                    if (shouldWriteDetailRecord) {
                        val suiteDetailsDir = Paths.get(resultsDirPath, "suite-details")
                        val detailDir = suiteDetailsDir.resolve(detailId)
                        val report =
                            capture.buildFinalizedReport(
                                detailId = detailId,
                                detailDir = detailDir,
                                savedLogcatFiles = savedLogcatFiles,
                                failure = failure,
                                resultsDirPath = resultsDirPath,
                            )
                        Files.createDirectories(suiteDetailsDir)
                        Files.write(
                            suiteDetailsDir.resolve("$detailId.json"),
                            report.record.toJsonString().toByteArray(Charsets.UTF_8),
                        )
                        if (allowPublishReportEntries) {
                            context.publishReportEntry(
                                BackupReportKeys.DETAIL_PATH,
                                "suite-details/$detailId.json",
                            )
                        }
                        report
                    } else if (failure != null) {
                        capture.buildFinalizedReport(
                            detailId = null,
                            detailDir = null,
                            savedLogcatFiles = savedLogcatFiles,
                            failure = failure,
                            resultsDirPath = null,
                        )
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    logger.warning(
                        "Failed to write report details for ${context.displayName}: ${e.message}"
                    )
                    null
                }

            if (failure != null) {
                finalized?.failureNoticeText?.let { notice ->
                    failure.addSuppressed(FailureSummaryNotice(notice))
                }
            }

            if (allowPublishReportEntries && (!isClassLevel || failure != null)) {
                capture.publishStepByStepTelemetryIfNeeded(failure) { key, value ->
                    context.publishReportEntry(key, value)
                }
            }
        } finally {
            capture.close()
        }
    }

    /**
     * Returns the directory for the device logs of failed tests: `suite-details` in the results
     * directory of the test suite, or else a temporary directory for the test run.
     */
    private fun deviceLogsDir(context: ExtensionContext, suiteProps: Properties): Path =
        suiteProperty(suiteProps, PROP_RESULTS_DIR)?.let { Paths.get(it, "suite-details") }
            ?: context.root
                .getStore(NAMESPACE)
                .getOrComputeIfAbsent(
                    DEVICE_LOGS_DIR_KEY,
                    { Files.createTempDirectory("backup-test-logs") },
                    Path::class.java,
                )

    /**
     * Builds a filesystem-safe detail identifier for [uniqueId] with a readable prefix and hash.
     */
    @VisibleForTesting
    internal fun detailIdOf(uniqueId: String): String {
        val segments = uniqueId.split("]/[").drop(1)
        val readableParts = segments.mapNotNull { raw ->
            val clean = raw.removeSuffix("]")
            val type = clean.substringBefore(':', missingDelimiterValue = "")
            val value = clean.substringAfter(':')
            when (type) {
                "class",
                "test-class" -> value.substringAfterLast('.').substringAfterLast('$')
                "nested-class" -> value.substringAfterLast('$').substringAfterLast('.')
                "method",
                "test-template" -> value.substringBefore('(')
                "test-template-invocation",
                "dynamic-test",
                "dynamic-container" -> value.removePrefix("#")
                else -> value.substringBefore('(').substringAfterLast('.')
            }.takeIf { it.isNotEmpty() }
        }
        val basePrefix =
            fileSafe(readableParts.joinToString("."))
                .take(MAX_DETAIL_PREFIX_LENGTH)
                .trim('.', '_', '-')
                .ifEmpty { "test" }
        val hashSuffix = sha256HexPrefix(uniqueId, 8)
        return "$basePrefix-$hashSuffix"
    }

    /** Returns [name] with each run of characters that file names can't hold replaced by `_`. */
    private fun fileSafe(name: String): String = name.replace(Regex("[^A-Za-z0-9._-]+"), "_")

    private fun isClassLevelContext(context: ExtensionContext): Boolean =
        context.testMethod.orElse(null) == null

    private fun resolveTestClassName(context: ExtensionContext): String? =
        context.testClass.orElse(null)?.name

    private fun resolveTestMethodName(context: ExtensionContext): String? =
        context.testMethod.orElse(null)?.name

    private fun buildPrepareCallDescription(hasApks: Boolean, autoClear: Boolean): String =
        buildList {
            if (hasApks) add("Install APKs")
            add(if (hasApks) "unlock device" else "Unlock device")
            if (autoClear) add("clear app data")
        }
        .joinToString(", ")

    /**
     * Returns the devices that the [BackupRestoreController] parameters of [executable] ask for, in
     * the order of the parameters.
     *
     * @throws IllegalArgumentException if the [Device] of a parameter has a negative API level, a
     *   [Device.minApi] above its [Device.maxApi], or a [Device.api] or a system property API level
     *   outside them
     */
    internal fun deviceRequestsOf(executable: Executable): List<DeviceRequest> =
        executable.parameters
            .filter { isController(it) }
            .mapIndexed { index, parameter ->
                val device = parameter.getAnnotation(Device::class.java)
                val api = device?.api ?: 0
                val minApi = device?.minApi ?: 0
                val maxApi = device?.maxApi ?: Int.MAX_VALUE
                val role = device?.role.orEmpty()
                val where =
                    "controller parameter $index" + if (role.isEmpty()) "" else " (role '$role')"
                listOf("api" to api, "minApi" to minApi, "maxApi" to maxApi)
                    .firstOrNull { it.second < 0 }
                    ?.let { (name, value) ->
                        throw IllegalArgumentException(
                            "The @Device of $where sets $name to $value, but an API level can't " +
                                "be negative."
                        )
                    }
                require(minApi <= maxApi) {
                    "The @Device of $where sets minApi to $minApi, above its maxApi of $maxApi."
                }
                require(api == 0 || api in minApi..maxApi) {
                    "The @Device of $where asks for API level $api, but its minApi and maxApi " +
                        "allow only ${apiRange(minApi, maxApi)}."
                }
                val property = apiProperty(index)
                if (property != null) {
                    require(property.second == 0 || property.second in minApi..maxApi) {
                        "The system property ${property.first} asks for API level " +
                            "${property.second}, but the @Device of $where allows only " +
                            "${apiRange(minApi, maxApi)}. Use an API level in that range" +
                            if (property.first == PROP_DEVICE_API) {
                                ", or $PROP_DEVICE_APIS for an API level per parameter."
                            } else {
                                "."
                            }
                    }
                }
                DeviceRequest(
                    requestedSerial(device, index),
                    property?.second ?: api,
                    minApi,
                    maxApi,
                )
            }

    /**
     * Returns the serial numbers of the [devices] assigned to [requests], in the order of
     * [requests], or null for a request that no device matches.
     *
     * A request for a serial number gets the device with that serial number. The other requests
     * each get a device of their own when enough devices match them: in order, each takes the first
     * free device by serial number that matches it, and earlier requests move to other devices that
     * match them if that frees one for it. A request that can't get a device of its own shares the
     * first device that matches it.
     */
    internal fun assignDevices(
        requests: List<DeviceRequest>,
        devices: List<OnlineDevice>,
    ): List<String?> {
        val sortedDevices = devices.sortedBy { it.serialNumber }
        val matching = requests.map { request -> sortedDevices.filter { request.matches(it) } }
        val bound = requests.map { it.serialNumber }.filterTo(HashSet()) { it.isNotEmpty() }
        val owners = HashMap<String, Int>()

        // Finds a device of its own for the request at [index], if necessary by moving requests to
        // other devices that match them, and returns whether there is one.
        fun place(index: Int, visited: MutableSet<String>): Boolean {
            val candidates = matching[index].map { it.serialNumber }.filter { it !in bound }
            val free = candidates.firstOrNull { it !in owners }
            if (free != null) {
                owners[free] = index
                return true
            }
            for (serial in candidates) {
                if (visited.add(serial) && place(owners.getValue(serial), visited)) {
                    owners[serial] = index
                    return true
                }
            }
            return false
        }

        requests.indices
            .filter { requests[it].serialNumber.isEmpty() }
            .forEach { place(it, HashSet()) }
        val assigned = owners.entries.associate { (serial, index) -> index to serial }
        return requests.mapIndexed { index, request ->
            val first = matching[index].firstOrNull()?.serialNumber
            if (request.serialNumber.isNotEmpty()) first else assigned[index] ?: first
        }
    }

    /**
     * Returns the failure message for a controller parameter with the role [role] whose [request]
     * no online device of [devices] matches.
     */
    internal fun noMatchingDeviceMessage(
        request: DeviceRequest,
        role: String,
        devices: List<OnlineDevice>,
    ): String {
        val available = devices.joinToString(", ") { "${it.serialNumber} (API ${it.api})" }
        val range =
            (if (request.minApi != 0) ", minApi=${request.minApi}" else "") +
                (if (request.maxApi != Int.MAX_VALUE) ", maxApi=${request.maxApi}" else "")
        return "Could not find any online devices matching requirements: " +
            "API=${request.api}$range, Serial='${request.serialNumber}'" +
            (if (role.isNotEmpty()) " (role '$role')" else "") +
            ". Available devices: [$available]"
    }

    /**
     * Returns the serial number of the device that the controller parameter at [deviceIndex] with
     * the annotation [device] asks for, or "" for any device.
     *
     * In order of precedence, that is the serial number that `androidx.test.backup.device.serial.`
     * followed by the role of [device], or by its [Device.serial], binds it to; the one at
     * [deviceIndex] in `androidx.test.backup.device.serials`; the one in
     * `androidx.test.backup.device.serial`; and the [Device.serial] of [device].
     */
    internal fun requestedSerial(device: Device?, deviceIndex: Int): String {
        val annotationSerial = device?.serial.orEmpty()
        for (name in listOf(device?.role.orEmpty(), annotationSerial)) {
            if (name.isEmpty()) continue
            val bound = System.getProperty("$PROP_DEVICE_SERIAL_PREFIX$name")
            if (!bound.isNullOrEmpty()) return bound
        }
        val serials = listProperty(PROP_DEVICE_SERIALS)
        if (deviceIndex < serials.size) return serials[deviceIndex]
        val global = System.getProperty(PROP_DEVICE_SERIAL)
        if (!global.isNullOrEmpty()) return global
        return annotationSerial
    }

    /**
     * Returns the API level of the device that the controller parameter at [deviceIndex] with the
     * annotation [device] asks for, or 0 for any API level: the one that [apiProperty] sets, or
     * else the [Device.api] of [device].
     */
    internal fun requestedApi(device: Device?, deviceIndex: Int): Int =
        apiProperty(deviceIndex)?.second ?: device?.api ?: 0

    /**
     * Returns the name and the value of the system property that sets the API level of the
     * controller parameter at [deviceIndex], or null if none does.
     *
     * In order of precedence, that is the entry at [deviceIndex] in
     * `androidx.test.backup.device.apis`, and `androidx.test.backup.device.api`.
     */
    private fun apiProperty(deviceIndex: Int): Pair<String, Int>? {
        val apis = listProperty(PROP_DEVICE_APIS).mapNotNull { it.toIntOrNull() }
        if (deviceIndex < apis.size) return PROP_DEVICE_APIS to apis[deviceIndex]
        return System.getProperty(PROP_DEVICE_API)?.toIntOrNull()?.let { PROP_DEVICE_API to it }
    }

    /** Returns the API levels from [minApi] to [maxApi], as failure messages name them. */
    private fun apiRange(minApi: Int, maxApi: Int): String =
        when {
            maxApi == Int.MAX_VALUE -> "API level $minApi or higher"
            minApi == maxApi -> "API level $minApi"
            minApi == 0 -> "API level $maxApi or lower"
            else -> "API levels $minApi to $maxApi"
        }

    /** Returns the trimmed, non-empty entries of the comma-separated system property [name]. */
    private fun listProperty(name: String): List<String> =
        System.getProperty(name).orEmpty().split(",").map { it.trim() }.filter { it.isNotEmpty() }

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
     * there and neither the app nor its test APKs have been installed again or uninstalled since.
     *
     * The installed APK paths identify an installation, since they change on every install. A test
     * that installs another version of the app or of one of its test APKs, or uninstalls one of
     * them, thus gets [apks] installed again for the next test.
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
     * Returns the ID of the app that the tests of [testClass] target: the
     * [BackupRestoreConfig.applicationId] of [testClass] or of an enclosing class, or else the one
     * of the app that the test suite of [suiteProperties] tests.
     */
    internal fun applicationIdFor(testClass: Class<*>, suiteProperties: Properties): String =
        classAnnotations(testClass, BackupRestoreConfig::class.java)
            .map { it.applicationId }
            .firstOrNull { it.isNotEmpty() }
            ?: suiteProperty(suiteProperties, PROP_TESTED_APPLICATION_ID)
            ?: throw IllegalStateException(
                "No application ID for ${testClass.name}: annotate it with @BackupRestoreConfig, " +
                    "or run it in an Android Gradle Plugin backup test suite."
            )

    /**
     * Returns the package of the test APK whose runner runs the device actions of the tests of
     * [testClass]: the [BackupRestoreConfig.testApplicationId] of [testClass] or of the nearest
     * enclosing class that sets one, or null if none is set. The test APK of an enclosing class
     * doesn't apply when [testClass] or a class in between sets
     * [BackupRestoreConfig.applicationId], because it was chosen for another app.
     */
    internal fun testApplicationIdFor(testClass: Class<*>): String? {
        for (config in classAnnotations(testClass, BackupRestoreConfig::class.java)) {
            if (config.testApplicationId.isNotEmpty()) return config.testApplicationId
            if (config.applicationId.isNotEmpty()) return null
        }
        return null
    }

    /** Returns the APK files of the tested app, then of the test app, of [suiteProperties]. */
    internal fun suiteApks(suiteProperties: Properties): List<File> =
        apkFilesIn(suiteProperty(suiteProperties, PROP_TESTED_APKS)) +
            apkFilesIn(suiteProperty(suiteProperties, PROP_TEST_APKS))

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

    /**
     * A standalone ADB session, which JUnit closes when the context it is stored in ends.
     *
     * It is [AutoCloseable] as well as a [CloseableResource]: JUnit 5.13 and later close stored
     * [AutoCloseable] values, and warn about a value that is only a [CloseableResource].
     */
    private class StandaloneSession(val session: AdbSession) : CloseableResource, AutoCloseable {
        override fun close() {
            try {
                session.close()
            } catch (e: Exception) {
                Logger.getLogger(BackupRestoreExtension::class.java.name)
                    .warning("Failed to close the ADB session: ${e.message}")
            }
        }
    }

    /**
     * Wraps an unclaimed [TestInstance.Lifecycle.PER_METHOD] constructor [capture] alongside its
     * unique [detailId].
     */
    private class PendingInstanceCapture(
        val capture: TestExecutionCapture,
        val detailId: String,
        var failure: Throwable? = null,
    ) : CloseableResource, AutoCloseable {
        override fun close() {
            capture.close()
        }
    }

    /** Store key of the installed APK paths after [installOnce] installed [apks] on a device. */
    private data class InstalledApp(
        val serialNumber: String,
        val applicationId: String,
        val apks: List<File>,
    )

    /** Store key of the cached [DeviceFacts] for [serialNumber]. */
    private data class DeviceFactsKey(val serialNumber: String)

    /** Store key of the [TestExecutionCapture] of the test or test class with [uniqueId]. */
    private data class TestCaptureKey(val uniqueId: String)

    /**
     * Identifies a pending [TestInstance.Lifecycle.PER_METHOD] constructor [PendingInstanceCapture]
     * on [classUniqueId].
     */
    private data class PendingInstanceCaptureKey(val classUniqueId: String)

    /**
     * Names the failed step and likely backup-rule cause in a test failure.
     *
     * Attached as a suppressed exception without a stack trace.
     */
    private class FailureSummaryNotice(summary: String) : Exception(summary, null, false, false) {
        override fun toString(): String = message.orEmpty()
    }

    /**
     * Names the [file] with the logs of a device in a test failure, as a suppressed exception
     * without a stack trace, which test reports print with the failure.
     */
    private class DeviceLogsNotice(file: Path) :
        Exception("Device logs saved to $file", null, false, false) {
        override fun toString(): String = message.orEmpty()
    }

    /**
     * The device that a [BackupRestoreController] parameter asks for: the one with [serialNumber],
     * or any device for "", which runs API level [api], or any API level for 0, and an API level
     * from [minApi] to [maxApi].
     */
    internal data class DeviceRequest(
        val serialNumber: String,
        val api: Int,
        val minApi: Int = 0,
        val maxApi: Int = Int.MAX_VALUE,
    ) {
        /** Returns whether [device] is a device that this request asks for. */
        fun matches(device: OnlineDevice): Boolean =
            (serialNumber.isEmpty() || device.serialNumber == serialNumber) &&
                (api == 0 || device.api == api) &&
                device.api in minApi..maxApi
    }

    /** An online device with [serialNumber], which runs API level [api]. */
    internal data class OnlineDevice(val serialNumber: String, val api: Int)

    /** Configuration constants and system property keys for device resolution and setup. */
    private companion object {
        /** Store namespace for the state that this extension keeps in extension contexts. */
        private val NAMESPACE =
            ExtensionContext.Namespace.create(BackupRestoreExtension::class.java)

        /** Store key of the [StandaloneSession] that all extensions in a test run share. */
        private const val STANDALONE_SESSION_KEY = "standaloneSession"

        /** Store key of the directory for device logs that all extensions in a test run share. */
        private const val DEVICE_LOGS_DIR_KEY = "deviceLogsDir"

        /** Timeout limit for saving the logs of a device after a test ends. */
        private const val DEVICE_LOGS_TIMEOUT_MS = 10000L

        /** Maximum size of a saved per-device logcat file (512 KB). */
        private const val MAX_LOGCAT_BYTES = 512 * 1024

        /** Maximum character length of the readable prefix in a detail file identifier. */
        private const val MAX_DETAIL_PREFIX_LENGTH = 96

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

        /** Property that turns off saving the device logs of failed tests when set to `false`. */
        private const val PROP_DEVICE_LOGS = "androidx.test.backup.device.logs.enabled"

        /** Property that enables saving device logcat even on passing tests when set to `true`. */
        private const val PROP_CAPTURE_LOGCAT_ON_PASS =
            "androidx.test.backup.report.captureLogcatOnPass"

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

        /** Test suite input with the directory for the results of the tests. */
        private const val PROP_RESULTS_DIR = "com.android.junit.engine.results.dir"

        /** Timeout limit for discovering connected devices via ADB. */
        private const val ADB_TIMEOUT_MS = 30000L

        /** Minimum Android API level required for backup/restore capability (Android 12). */
        private const val MIN_REQUIRED_API = 31

        private val ctorAttemptCounter = AtomicLong()

        /**
         * Returns the test suite input [name] of [suiteProperties], or else the system property.
         */
        private fun suiteProperty(suiteProperties: Properties, name: String): String? =
            suiteProperties.getProperty(name)?.takeIf { it.isNotEmpty() }
                ?: System.getProperty(name)?.takeIf { it.isNotEmpty() }

        /**
         * Returns whether [parameter] is a [BackupRestoreController], which this extension
         * resolves.
         */
        private fun isController(parameter: Parameter): Boolean =
            parameter.type == BackupRestoreController::class.java

        private fun sha256HexPrefix(input: String, hexLength: Int): String {
            val digest =
                MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            return buildString(hexLength) {
                    for (b in digest) {
                        if (length >= hexLength) break
                        append(String.format(Locale.ROOT, "%02x", b.toInt() and 0xff))
                    }
                }
                .take(hexLength)
        }
    }
}
