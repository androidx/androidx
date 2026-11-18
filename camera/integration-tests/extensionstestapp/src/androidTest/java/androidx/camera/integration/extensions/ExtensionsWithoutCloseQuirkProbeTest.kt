/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.integration.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.compat.quirk.CloseCameraDeviceOnCameraGraphCloseQuirk
import androidx.camera.camera2.compat.quirk.Device.isSamsungDevice
import androidx.camera.camera2.compat.quirk.DeviceQuirks
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.impl.QuirkSettings
import androidx.camera.core.impl.QuirkSettingsHolder
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionSessionConfig
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil
import androidx.camera.integration.extensions.utils.CameraSelectorUtil
import androidx.camera.integration.extensions.utils.ExtensionModeUtil
import androidx.camera.integration.extensions.utils.ExtensionModeUtil.AVAILABLE_EXTENSION_MODES
import androidx.camera.lifecycle.ExperimentalCameraProviderConfiguration
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.ExtensionsUtil.assumePcsSupportedForImageCapture
import androidx.camera.testing.impl.LabTestRule
import androidx.camera.testing.impl.StressTestRule
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Lab graduation probe verifying whether devices targeted solely for Extensions by
 * [CloseCameraDeviceOnCameraGraphCloseQuirk] (as well as Samsung API 31+ cohorts graduated into its
 * verified-clean allowlist) can recreate Extensions sessions without closing the underlying camera
 * device across rapid bind/unbind, same-lens mode switches, and lifecycle stop/start transitions.
 *
 * Intentionally kept out of standard daily CI suites because currently affected devices are
 * expected to fail when the quirk is force-disabled. Run on demand when onboarding new hardware or
 * OS builds to evaluate allowlist graduation candidates and audit graduated cohorts across vendor
 * or PCS updates.
 */
@LargeTest
@RunWith(Parameterized::class)
@OptIn(ExperimentalCameraProviderConfiguration::class)
@SuppressLint("RestrictedApiAndroidX")
class ExtensionsWithoutCloseQuirkProbeTest(private val cameraId: String) {

    @get:Rule(order = 0) val labTestRule = LabTestRule()

    @get:Rule(order = 1)
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            PreTestCameraIdList(Camera2Config.defaultConfig())
        )

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var extensionsManager: ExtensionsManager
    private lateinit var cameraSelector: CameraSelector
    private lateinit var lifecycleOwner: FakeLifecycleOwner
    private lateinit var supportedModes: List<Int>

    private var quirkSettingsOverridden = false
    private var configuredProbeProvider = false

    @Before
    fun setUp(): Unit = runBlocking {
        assumeTrue(CameraUtil.deviceHasCamera())
        assumeTrue(CameraXExtensionsTestUtil.isTargetDeviceAvailableForExtensions())
        assumePcsSupportedForImageCapture(context)

        // Ensure default quirk settings are active before checking whether this device uses the
        // Extensions-only branch of CloseCameraDeviceOnCameraGraphCloseQuirk or was graduated from
        // it via the Samsung API 31+ verified-clean allowlist.
        ProcessCameraProvider.shutdown().awaitWithProviderTimeout()
        QuirkSettingsHolder.instance().set(QuirkSettingsHolder.DEFAULT)
        val defaultQuirk = DeviceQuirks[CloseCameraDeviceOnCameraGraphCloseQuirk::class.java]
        val isActiveExtensionsOnlyQuirk =
            defaultQuirk != null &&
                !defaultQuirk.shouldCloseCameraDevice(isExtensions = false) &&
                defaultQuirk.shouldCloseCameraDevice(isExtensions = true)
        // Unconditional Samsung close-quirk devices always have defaultQuirk != null; Samsung API
        // 31+ devices with defaultQuirk == null are precisely the graduated allowlist cohorts.
        val isGraduatedSamsungExtensionsCohort =
            defaultQuirk == null &&
                isSamsungDevice() &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        assumeTrue(
            "Skipping device neither targeted solely for Extensions nor graduated from " +
                "CloseCameraDeviceOnCameraGraphCloseQuirk",
            isActiveExtensionsOnlyQuirk || isGraduatedSamsungExtensionsCohort,
        )

        val quirkDisabledConfig =
            CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
                .setQuirkSettings(
                    QuirkSettings.withQuirksForceDisabled(
                        setOf(CloseCameraDeviceOnCameraGraphCloseQuirk::class.java)
                    )
                )
                .build()
        quirkSettingsOverridden = true
        configuredProbeProvider = true
        ProcessCameraProvider.configureInstance(quirkDisabledConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context).awaitWithProviderTimeout()
        assertThat(DeviceQuirks[CloseCameraDeviceOnCameraGraphCloseQuirk::class.java]).isNull()

        extensionsManager =
            ExtensionsManager.getInstanceAsync(context, cameraProvider).awaitWithProviderTimeout()
        cameraSelector = CameraSelectorUtil.createCameraSelectorById(cameraId)
        supportedModes =
            AVAILABLE_EXTENSION_MODES.filter { mode ->
                    extensionsManager.isExtensionAvailable(cameraSelector, mode)
                }
                .sortedBy { mode -> if (mode == ExtensionMode.NIGHT) 0 else 1 }
        assumeTrue(
            "No Extension modes supported on cameraId=$cameraId",
            supportedModes.isNotEmpty(),
        )

        lifecycleOwner = FakeLifecycleOwner().apply { startAndResume() }
    }

    @After
    fun tearDown() {
        var tearDownError: Throwable? = null
        fun recordFailure(t: Throwable) {
            val primary = tearDownError
            if (primary == null) tearDownError = t else primary.addSuppressed(t)
        }
        try {
            if (::lifecycleOwner.isInitialized) {
                runCatching {
                        val state = lifecycleOwner.lifecycle.currentState
                        if (state == Lifecycle.State.RESUMED) lifecycleOwner.pauseAndStop()
                        else if (state == Lifecycle.State.STARTED) lifecycleOwner.stop()
                        if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.CREATED) {
                            lifecycleOwner.destroy()
                        }
                    }
                    .onFailure(::recordFailure)
            }
            // Shut down ProcessCameraProvider before ExtensionsManager (isolated via runCatching so
            // earlier failures never skip shutdown or leak @CameraScope state across tests).
            if (configuredProbeProvider) {
                configuredProbeProvider = false
                runCatching { ProcessCameraProvider.shutdown().awaitWithProviderTimeout() }
                    .onFailure(::recordFailure)
            }
            if (::extensionsManager.isInitialized) {
                runCatching { extensionsManager.shutdown().awaitWithProviderTimeout() }
                    .onFailure(::recordFailure)
            }
        } finally {
            // Restore DEFAULT via set(...) rather than reset() so static DeviceQuirks observers
            // remain subscribed across subsequent tests in the same process.
            if (quirkSettingsOverridden) {
                QuirkSettingsHolder.instance().set(QuirkSettingsHolder.DEFAULT)
                quirkSettingsOverridden = false
            }
        }
        tearDownError?.let { throw it }
    }

    @LabTestRule.LabTestOnly
    @Test
    fun bindUnbindEachSupportedMode_canCaptureImage_withoutCloseQuirk(): Unit =
        runPerModeProbe("bindUnbind") { mode, repeatCount, firstFrameLatenciesMs ->
            val monitorsToRelease = mutableListOf<PreviewStreamMonitor>()
            var modeFailure: String? = null
            // Reuse the same Preview and ImageCapture instances across rapid bind -> immediate
            // takePicture() -> immediate unbindAll() cycles to match BindUnbindUseCasesStressTest's
            // bindUnbindUseCases_checkImageCaptureInEachTime_withPreviewImageCapture timing (which
            // triggers vendor SuperNight light-map / session-recreation stalls when CameraDevice
            // remains open across rapid unbind/rebind transitions; b/582678736).
            val preview = Preview.Builder().build()
            val imageCapture = ImageCapture.Builder().build()

            try {
                for (iteration in 1..repeatCount) {
                    val monitor = PreviewStreamMonitor(PREVIEW_STREAMING_FRAME_COUNT)
                    monitorsToRelease.add(monitor)
                    val bindStartMs = SystemClock.elapsedRealtime()
                    try {
                        val stepError =
                            runProbeBlock("iter=$iteration:") {
                                bindExtensionSession(mode, preview, imageCapture, monitor)
                                val captureError = captureImage(imageCapture)
                                monitor.recordLatencySince(bindStartMs, firstFrameLatenciesMs)
                                captureError
                            }
                        if (stepError != null) {
                            modeFailure = stepError
                            break
                        }
                    } finally {
                        withContext(Dispatchers.Main + NonCancellable) {
                            cameraProvider.unbindAll()
                        }
                    }
                }

                // Also exercise rapid burst bind/unbind (leaving bound on the final iteration)
                // followed by immediate capture and preview streaming verification, matching
                // BindUnbindUseCasesStressTest's burst-rebind capture flow.
                if (modeFailure == null) {
                    modeFailure =
                        runProbeBlock("burst:") {
                            var finalMonitor: PreviewStreamMonitor? = null
                            var finalBindStartMs = 0L
                            for (iteration in 1..repeatCount) {
                                val monitor = PreviewStreamMonitor(PREVIEW_STREAMING_FRAME_COUNT)
                                monitorsToRelease.add(monitor)
                                finalMonitor = monitor
                                finalBindStartMs = SystemClock.elapsedRealtime()
                                bindExtensionSession(
                                    mode,
                                    preview,
                                    imageCapture,
                                    monitor,
                                    unbindImmediately = iteration != repeatCount,
                                )
                            }
                            val burstError = captureImage(imageCapture)
                            val streamed =
                                burstError != null ||
                                    finalMonitor?.awaitStreamingFrames(PREVIEW_FRAME_TIMEOUT_MS) ==
                                        true
                            finalMonitor?.recordLatencySince(
                                finalBindStartMs,
                                firstFrameLatenciesMs,
                            )
                            burstError ?: if (!streamed) "preview_frames_timeout" else null
                        }
                }
            } finally {
                val releaseErr = unbindAndAwaitSurfaceRelease(listOf(preview), monitorsToRelease)
                if (releaseErr != null && modeFailure == null) modeFailure = releaseErr
            }
            modeFailure
        }

    @LabTestRule.LabTestOnly
    @Test
    fun switchBetweenSupportedModes_canCaptureImage_withoutCloseQuirk(): Unit = runBlocking {
        assumeTrue(
            "At least 2 Extension modes required to probe same-lens mode switching " +
                "(cameraId=$cameraId, supported=${supportedModes.map(::modeLabel)})",
            supportedModes.size >= 2,
        )

        val repeatCount = CameraXExtensionsTestUtil.getStressTestRepeatingCount()
        val modeSummaries = mutableListOf<String>()
        val failures = mutableListOf<String>()
        val firstFrameLatenciesMs = mutableListOf<Long>()
        val monitorsToRelease = mutableListOf<PreviewStreamMonitor>()
        val previewsToClear = mutableListOf<Preview>()

        suspend fun bindModeForSwitch(
            mode: Int,
            outgoingMonitor: PreviewStreamMonitor?,
            latenciesOut: MutableList<Long>?,
        ): Pair<PreviewStreamMonitor, String?> {
            val monitor = PreviewStreamMonitor(PREVIEW_STREAMING_FRAME_COUNT)
            monitorsToRelease.add(monitor)
            val isPostview = isPostviewSupported(mode)
            val preview = Preview.Builder().build().also(previewsToClear::add)
            val imageCapture = ImageCapture.Builder().setPostviewEnabled(isPostview).build()
            val startMs = SystemClock.elapsedRealtime()
            // Bind directly without unbindAll() to exercise implicit-unbind fast mode switching.
            bindExtensionSession(mode, preview, imageCapture, monitor)
            val err =
                if (outgoingMonitor != null && !outgoingMonitor.awaitSurfaceReleased()) {
                    "prev_surface_release_timeout"
                } else {
                    verifyStreamingAndStillCapture(
                        monitor,
                        imageCapture,
                        isPostview,
                        startMs,
                        latenciesOut,
                    )
                }
            return monitor to err
        }

        try {
            val initialMode = supportedModes[0]
            var previousMonitor: PreviewStreamMonitor? = null
            val initErr =
                runProbeBlock("init(${modeLabel(initialMode)}):") {
                    val (mon, err) = bindModeForSwitch(initialMode, null, null)
                    previousMonitor = mon
                    err
                }
            if (initErr != null) {
                failures.add(initErr)
            } else {
                for (step in 1..repeatCount) {
                    val targetMode = supportedModes[step % supportedModes.size]
                    val outgoingMonitor = previousMonitor
                    val switchError =
                        runProbeBlock("step=$step(${modeLabel(targetMode)}):") {
                            val (mon, err) =
                                bindModeForSwitch(
                                    targetMode,
                                    outgoingMonitor,
                                    firstFrameLatenciesMs,
                                )
                            previousMonitor = mon
                            err
                        }
                    if (switchError != null) {
                        failures.add(switchError)
                        break
                    }
                }
            }
        } finally {
            val releaseErr = unbindAndAwaitSurfaceRelease(previewsToClear, monitorsToRelease)
            if (releaseErr != null && failures.isEmpty()) {
                failures.add("teardown:$releaseErr")
            }
        }

        val avgMs = firstFrameLatenciesMs.averageOrMinusOne()
        val modesSequence = supportedModes.joinToString("->") { modeLabel(it) }
        if (failures.isEmpty()) {
            modeSummaries.add("$modesSequence:PASS(avgSwitchFirstFrameMs=$avgMs)")
        } else {
            modeSummaries.add("$modesSequence:FAIL(${sanitizeToken(failures.first())})")
        }

        assertProbeFlowClean("modeSwitch", repeatCount, modeSummaries, failures)
    }

    @LabTestRule.LabTestOnly
    @Test
    fun pauseResumeEachSupportedMode_canCaptureImage_withoutCloseQuirk(): Unit =
        runPerModeProbe("pauseResume") { mode, repeatCount, firstFrameLatenciesMs ->
            val isPostviewSupported = isPostviewSupported(mode)
            var modeFailure: String? = null
            val monitor = PreviewStreamMonitor(PREVIEW_STREAMING_FRAME_COUNT)
            val preview = Preview.Builder().build()
            val imageCapture =
                ImageCapture.Builder().setPostviewEnabled(isPostviewSupported).build()

            try {
                modeFailure = runProbeBlock {
                    if (lifecycleOwner.lifecycle.currentState != Lifecycle.State.RESUMED) {
                        lifecycleOwner.startAndResume()
                    }
                    val initialBindMs = SystemClock.elapsedRealtime()
                    val camera = bindExtensionSession(mode, preview, imageCapture, monitor)
                    verifyStreamingAndStillCapture(
                            monitor,
                            imageCapture,
                            isPostviewSupported,
                            initialBindMs,
                        )
                        ?.let {
                            return@runProbeBlock "init:$it"
                        }
                    for (iteration in 1..repeatCount) {
                        // Preview retains its SurfaceRequest across onStop/onStart (detachUseCases
                        // does not call Preview.onUnbind); gate frame counting across CLOSED ->
                        // OPEN so trailing pre-pause frames never satisfy the post-resume latch.
                        monitor.pauseFrameCounting()
                        if (
                            !awaitCameraState(camera.cameraInfo, CameraState.Type.CLOSED) {
                                lifecycleOwner.pauseAndStop()
                            }
                        ) {
                            return@runProbeBlock "iter=$iteration:camera_close_state_timeout"
                        }
                        val resumeStartMs = SystemClock.elapsedRealtime()
                        if (
                            !awaitCameraState(
                                camera.cameraInfo,
                                CameraState.Type.OPEN,
                                onTargetState = {
                                    monitor.resumeFrameCounting(PREVIEW_STREAMING_FRAME_COUNT)
                                },
                            ) {
                                lifecycleOwner.startAndResume()
                            }
                        ) {
                            return@runProbeBlock "iter=$iteration:camera_open_state_timeout"
                        }
                        verifyStreamingAndStillCapture(
                                monitor,
                                imageCapture,
                                isPostviewSupported,
                                resumeStartMs,
                                firstFrameLatenciesMs,
                            )
                            ?.let {
                                return@runProbeBlock "iter=$iteration:$it"
                            }
                    }
                    null
                }
            } finally {
                // Unbind all use cases before resuming lifecycleOwner so aborting in CREATED state
                // never triggers a spurious camera/ExtensionSession reopen during teardown.
                val releaseErr = unbindAndAwaitSurfaceRelease(listOf(preview), listOf(monitor))
                if (releaseErr != null && modeFailure == null) modeFailure = releaseErr
                if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.STARTED) {
                    lifecycleOwner.stop()
                }
                if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.CREATED) {
                    lifecycleOwner.startAndResume()
                }
            }
            modeFailure
        }

    private fun runPerModeProbe(
        flow: String,
        block: suspend (mode: Int, repeatCount: Int, latenciesOut: MutableList<Long>) -> String?,
    ): Unit = runBlocking {
        val repeatCount = CameraXExtensionsTestUtil.getStressTestRepeatingCount()
        val modeSummaries = mutableListOf<String>()
        val failures = mutableListOf<String>()
        for (mode in supportedModes) {
            val firstFrameLatenciesMs = mutableListOf<Long>()
            val modeFailure = block(mode, repeatCount, firstFrameLatenciesMs)
            recordModeOutcome(
                flow,
                modeLabel(mode),
                repeatCount,
                firstFrameLatenciesMs,
                modeFailure,
                modeSummaries,
                failures,
            )
        }
        assertProbeFlowClean(flow, repeatCount, modeSummaries, failures)
    }

    private inline fun runProbeBlock(prefix: String = "", block: () -> String?): String? =
        try {
            block()?.let { "$prefix$it" }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            "$prefix${e.javaClass.simpleName}(${e.message})"
        }

    private suspend fun bindExtensionSession(
        mode: Int,
        preview: Preview,
        imageCapture: ImageCapture,
        monitor: PreviewStreamMonitor,
        unbindImmediately: Boolean = false,
    ): Camera =
        withContext(Dispatchers.Main) {
            preview.surfaceProvider = monitor.createSurfaceProvider()
            val camera =
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    ExtensionSessionConfig(mode, extensionsManager, preview, imageCapture),
                )
            if (unbindImmediately) {
                cameraProvider.unbindAll()
            }
            camera
        }

    private suspend fun unbindAndAwaitSurfaceRelease(
        previews: List<Preview>,
        monitors: List<PreviewStreamMonitor>,
    ): String? {
        withContext(Dispatchers.Main + NonCancellable) {
            previews.forEach { it.surfaceProvider = null }
            cameraProvider.unbindAll()
        }
        for ((idx, monitor) in monitors.withIndex()) {
            if (!monitor.awaitSurfaceReleased()) {
                return "iter=${idx + 1}:surface_release_timeout"
            }
        }
        return null
    }

    private inline fun awaitCameraState(
        cameraInfo: CameraInfo,
        targetState: CameraState.Type,
        crossinline onTargetState: () -> Unit = {},
        transition: () -> Unit,
    ): Boolean {
        val latch = CountDownLatch(1)
        val listener =
            Consumer<CameraState> { state ->
                if (state.type == targetState && latch.count > 0L) {
                    onTargetState()
                    latch.countDown()
                }
            }
        cameraInfo.addCameraStateListener(CameraXExecutors.directExecutor(), listener)
        return try {
            transition()
            latch.await(CAMERA_STATE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } finally {
            cameraInfo.removeCameraStateListener(listener)
        }
    }

    private fun assertProbeFlowClean(
        flow: String,
        repeatCount: Int,
        modeSummaries: List<String>,
        failures: List<String>,
    ) {
        emitProbeTelemetryLog(flow, repeatCount, modeSummaries, failures.isNotEmpty())
        assertWithMessage(
                "Extensions $flow probe failed without " +
                    "CloseCameraDeviceOnCameraGraphCloseQuirk on cameraId=$cameraId: $failures"
            )
            .that(failures)
            .isEmpty()
    }

    private fun isPostviewSupported(mode: Int): Boolean =
        ImageCapture.getImageCaptureCapabilities(
                cameraProvider.getCameraInfo(
                    cameraSelector,
                    ExtensionSessionConfig(mode, extensionsManager),
                )
            )
            .isPostviewSupported

    private fun verifyStreamingAndStillCapture(
        monitor: PreviewStreamMonitor,
        imageCapture: ImageCapture,
        expectPostview: Boolean,
        bindStartMs: Long,
        firstFrameLatenciesMs: MutableList<Long>? = null,
    ): String? {
        if (!monitor.awaitStreamingFrames(PREVIEW_FRAME_TIMEOUT_MS)) {
            return "preview_frames_timeout"
        }
        monitor.recordLatencySince(bindStartMs, firstFrameLatenciesMs)
        return captureImage(imageCapture, expectPostview)
    }

    private fun captureImage(imageCapture: ImageCapture, expectPostview: Boolean = false): String? {
        val captureMonitor = StillCaptureMonitor(expectPostview)
        imageCapture.takePicture(CameraXExecutors.ioExecutor(), captureMonitor)
        return captureMonitor.awaitCaptureOrFailureReason(IMAGE_CAPTURE_TIMEOUT_MS)
    }

    private fun <T> ListenableFuture<T>.awaitWithProviderTimeout(): T =
        get(PROVIDER_SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS)

    private fun recordModeOutcome(
        flow: String,
        modeName: String,
        repeatCount: Int,
        firstFrameLatenciesMs: List<Long>,
        modeFailure: String?,
        modeSummaries: MutableList<String>,
        failures: MutableList<String>,
    ) {
        if (modeFailure == null) {
            val avgMs = firstFrameLatenciesMs.averageOrMinusOne()
            modeSummaries.add("$modeName:PASS(runs=$repeatCount,avgFirstFrameMs=$avgMs)")
        } else {
            modeSummaries.add("$modeName:FAIL(${sanitizeToken(modeFailure)})")
            failures.add("flow=$flow,cameraId=$cameraId,mode=$modeName,$modeFailure")
        }
    }

    private fun List<Long>.averageOrMinusOne(): Long = if (isEmpty()) -1L else sum() / size

    private fun emitProbeTelemetryLog(
        flow: String,
        repeatCount: Int,
        modeSummaries: List<String>,
        hasFailures: Boolean,
    ) {
        val status = if (hasFailures) "NEEDS_QUIRK" else "PASS_WITHOUT_QUIRK"
        val soc =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) sanitizeToken(Api31Impl.socModel())
            else "unknown"
        // Structured key-value probe telemetry parsed by automated lab log collectors under
        // key "ExtensionsCloseQuirkProbe".
        val modes = modeSummaries.joinToString(";")
        Log.i(
            TAG,
            "CameraX_ExtensionsCloseQuirkProbe_Log flow=$flow status=$status " +
                "cameraId=$cameraId device=${sanitizeToken(Build.DEVICE)} " +
                "model=${sanitizeToken(Build.MODEL)} hardware=${sanitizeToken(Build.HARDWARE)} " +
                "board=${sanitizeToken(Build.BOARD)} soc=$soc sdk=${Build.VERSION.SDK_INT} " +
                "repeat=$repeatCount modes=$modes",
        )
    }

    private fun modeLabel(mode: Int): String =
        ExtensionModeUtil.getExtensionModeStringFromId(mode).replace(' ', '_')

    private fun sanitizeToken(raw: String?): String =
        (raw ?: "unknown").trim().replace(Regex("\\s+"), "_")

    private class PreviewStreamMonitor(initialFrameCount: Int) {
        val firstFrameElapsedRealtimeMs = AtomicLong(0L)
        private val activeFrameLatchRef = AtomicReference(CountDownLatch(initialFrameCount))
        private val surfaceLock = Any()
        private var pendingSurfaceCount = 0
        private var surfaceProvided = false
        private var surfaceReleaseLatch = CountDownLatch(1)
        @Volatile private var acceptingFrames = true

        fun pauseFrameCounting() {
            acceptingFrames = false
        }

        fun resumeFrameCounting(frameCount: Int) {
            firstFrameElapsedRealtimeMs.set(0L)
            activeFrameLatchRef.set(CountDownLatch(frameCount))
            acceptingFrames = true
        }

        fun recordLatencySince(startMs: Long, latenciesOut: MutableList<Long>?) {
            val firstFrameAtMs = firstFrameElapsedRealtimeMs.get()
            if (firstFrameAtMs >= startMs && startMs > 0L)
                latenciesOut?.add(firstFrameAtMs - startMs)
        }

        fun createSurfaceProvider(): Preview.SurfaceProvider =
            SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider(
                {
                    if (acceptingFrames) {
                        firstFrameElapsedRealtimeMs.compareAndSet(0L, SystemClock.elapsedRealtime())
                        activeFrameLatchRef.get()?.countDown()
                    }
                },
                {
                    synchronized(surfaceLock) {
                        surfaceProvided = true
                        if (pendingSurfaceCount++ == 0) surfaceReleaseLatch = CountDownLatch(1)
                    }
                    acceptingFrames = true
                },
                {
                    synchronized(surfaceLock) {
                        if (pendingSurfaceCount > 0 && --pendingSurfaceCount == 0) {
                            surfaceReleaseLatch.countDown()
                        }
                    }
                },
            )

        fun awaitStreamingFrames(timeoutMs: Long): Boolean =
            activeFrameLatchRef.get()?.await(timeoutMs, TimeUnit.MILLISECONDS) == true

        fun awaitSurfaceReleased(timeoutMs: Long = SURFACE_RELEASE_TIMEOUT_MS): Boolean {
            val latch =
                synchronized(surfaceLock) {
                    if (!surfaceProvided || pendingSurfaceCount == 0) return true
                    surfaceReleaseLatch
                }
            return latch.await(timeoutMs, TimeUnit.MILLISECONDS)
        }
    }

    private class StillCaptureMonitor(expectPostview: Boolean) :
        ImageCapture.OnImageCapturedCallback() {
        private val captureLatch = CountDownLatch(1)
        private val postviewLatch = CountDownLatch(if (expectPostview) 1 else 0)
        private val errorRef = AtomicReference<String?>(null)

        override fun onPostviewBitmapAvailable(bitmap: Bitmap) {
            bitmap.recycle()
            postviewLatch.countDown()
        }

        override fun onCaptureSuccess(image: ImageProxy) {
            image.close()
            captureLatch.countDown()
        }

        override fun onError(exception: ImageCaptureException) {
            errorRef.set(
                "capture_error(code=${exception.imageCaptureError},msg=${exception.message})"
            )
            postviewLatch.countDown()
            captureLatch.countDown()
        }

        fun awaitCaptureOrFailureReason(timeoutMs: Long): String? =
            when {
                !captureLatch.await(timeoutMs, TimeUnit.MILLISECONDS) -> "still_capture_timeout"
                errorRef.get() != null -> errorRef.get()
                !postviewLatch.await(POSTVIEW_TIMEOUT_MS, TimeUnit.MILLISECONDS) ->
                    "postview_timeout"
                else -> null
            }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private object Api31Impl {
        fun socModel(): String = Build.SOC_MODEL
    }

    companion object {
        private const val TAG = "ExtCloseQuirkProbeTest"
        private const val PROVIDER_SHUTDOWN_TIMEOUT_SEC = 10L
        private const val PREVIEW_STREAMING_FRAME_COUNT = 10
        private const val PREVIEW_FRAME_TIMEOUT_MS = 10_000L
        private const val IMAGE_CAPTURE_TIMEOUT_MS = 10_000L
        private const val POSTVIEW_TIMEOUT_MS = 5_000L
        private const val SURFACE_RELEASE_TIMEOUT_MS = 5_000L
        private const val CAMERA_STATE_TIMEOUT_MS = 10_000L

        @ClassRule @JvmField val stressTest = StressTestRule()

        @Parameterized.Parameters(name = "cameraId = {0}")
        @JvmStatic
        fun parameters(): List<Array<Any>> =
            CameraUtil.getBackwardCompatibleCameraIdListOrThrow().map { arrayOf(it) }
    }
}
