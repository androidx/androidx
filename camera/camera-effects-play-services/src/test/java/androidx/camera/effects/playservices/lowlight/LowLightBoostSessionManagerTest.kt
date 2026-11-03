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

package androidx.camera.effects.playservices.lowlight

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.opengl.Matrix
import android.os.Build
import android.os.Handler
import android.os.Looper.getMainLooper
import android.util.Size
import android.view.Surface
import androidx.camera.core.DynamicRange
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.ForwardingCameraInfo
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.lifecycle.Observer
import androidx.test.filters.SdkSuppress
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Verifies [LowLightBoostSessionManager] with a fake Google Play Services client. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
// Tells lint that the test needs API level 30 for LowLightBoostSession.
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
class LowLightBoostSessionManagerTest {

    private lateinit var context: Context
    private lateinit var client: FakeLowLightBoostClient
    private lateinit var sessionManager: LowLightBoostSessionManager
    private lateinit var processor: PlayServicesLowLightBoostSurfaceProcessor
    private val surfaceRequests = mutableListOf<SurfaceRequest>()
    private val createdSessions = mutableListOf<FakeLowLightBoostSession>()
    private val renderers = mutableListOf<FakeGlRenderer>()
    private val outputTexture = SurfaceTexture(OUTPUT_TEXTURE_NAME)
    private val outputSurface = Surface(outputTexture)

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        PlayServicesLowLightBoostAvailability.reset()
        client = FakeLowLightBoostClient()
        PlayServicesLowLightBoostAvailability.clientFactoryForTesting = { client }
        createSessionManager()
    }

    /**
     * Creates the session manager. The test thread is its GL thread. By default, GL work runs
     * synchronously. With [postingExecutor], it is posted like in production.
     */
    private fun createSessionManager(
        failGlInit: Boolean = false,
        failOutputGlInit: Boolean = false,
        postingExecutor: Boolean = false,
    ) {
        val handler = Handler(getMainLooper())
        sessionManager =
            LowLightBoostSessionManager(
                if (postingExecutor) {
                    CameraXExecutors.newHandlerExecutor(handler)
                } else {
                    CameraXExecutors.directExecutor()
                },
                handler,
                rendererFactory = {
                    // The input bridge creates the first renderer.
                    val failInit = failGlInit || (failOutputGlInit && renderers.isNotEmpty())
                    FakeGlRenderer(renderers.size + 1, failInit = failInit).also {
                        renderers.add(it)
                    }
                },
            )
        processor = PlayServicesLowLightBoostSurfaceProcessor(context, { client }, sessionManager)
    }

    @After
    fun tearDown() {
        sessionManager.destroySessionInternal()
        surfaceRequests.forEach { it.deferrableSurface.close() }
        createdSessions.forEach { it.cameraSurfaceTexture.release() }
        outputSurface.release()
        outputTexture.release()
        PlayServicesLowLightBoostAvailability.reset()
    }

    @Test
    fun sessionCreated_reportsInactive() {
        // Act.
        startSession()
        completeSessionCreation(0)

        // Assert.
        assertThat(sessionManager.getCurrentSession()).isSameInstanceAs(createdSessions[0])
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.INACTIVE)
    }

    @Test
    fun sessionTerminated_releasesSessionOnceAndRecovers() {
        // Arrange.
        startSession()
        completeSessionCreation(0)
        processor.onOutputSurface(FakeSurfaceOutput(outputSurface))

        // Act.
        client.callbacks[0].onSessionDisconnected(Status(CommonStatusCodes.NETWORK_ERROR))
        shadowOf(getMainLooper()).idle()

        // Assert: the session is released once, ERROR is reported while frames pass through, and a
        // new session is requested.
        assertThat(createdSessions[0].releaseCount).isEqualTo(1)
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.ERROR)
        shadowOf(getMainLooper()).idleFor(1, TimeUnit.SECONDS)
        assertThat(client.createSessionCount).isEqualTo(2)
    }

    @Test
    fun sessionKeepsDyingRightAfterCreation_recoveryIsCapped() {
        // Arrange.
        startSession()
        processor.onOutputSurface(FakeSurfaceOutput(outputSurface))

        // Act: every new session dies right after it is created.
        repeat(10) {
            val index = client.createSessionCount - 1
            if (index >= createdSessions.size) {
                completeSessionCreation(index)
                client.callbacks[index].onSessionDestroyed()
            }
            shadowOf(getMainLooper()).idleFor(3, TimeUnit.SECONDS)
            sessionManager.triggerAutoRecoveryIfNeeded(processor)
            shadowOf(getMainLooper()).idleFor(1, TimeUnit.SECONDS)
        }

        // Assert: the initial session plus the capped recovery attempts.
        assertThat(client.createSessionCount).isEqualTo(1 + MAX_AUTO_RECOVERY_ATTEMPTS)
    }

    @Test
    fun sessionCallThrows_doesNotCrashAndReportsError() {
        // Arrange: a session that throws on every call, e.g. after it died in Play Services.
        startSession()
        completeSessionCreation(0)
        processor.onOutputSurface(FakeSurfaceOutput(outputSurface))
        createdSessions[0].throwOnCall = true

        // Act: a camera frame needs the session's camera surface.
        sessionManager.onInputBridgeFrameAvailable(nextTimestampNs(), IDENTITY)
        shadowOf(getMainLooper()).idle()

        // Assert: the session is treated as dead instead of crashing the GL thread.
        assertThat(createdSessions[0].releaseCount).isEqualTo(1)
        assertThat(sessionManager.getCurrentSession()).isNull()
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.ERROR)
    }

    @Test
    fun sessionCallThrowsWhileDisabling_keepsOffState() {
        // Arrange.
        startSession()
        completeSessionCreation(0)
        createdSessions[0].throwOnCall = true

        // Act.
        processor.setLowLightBoostEnabled(false)
        shadowOf(getMainLooper()).idle()

        // Assert: the dead session is released, and the state stays OFF while disabled.
        assertThat(createdSessions[0].releaseCount).isEqualTo(1)
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.OFF)
    }

    @Test
    fun openGlInitFails_noSurfaceProvidedAndErrorKeptAfterToggle() {
        // Arrange: OpenGL can't be initialized.
        createSessionManager(failGlInit = true)

        // Act.
        startSession()
        processor.setLowLightBoostEnabled(false)
        processor.setLowLightBoostEnabled(true)
        shadowOf(getMainLooper()).idle()

        // Assert: no broken Surface is connected to the camera, ERROR is reported, and no session
        // is created.
        assertThat(sessionManager.renderPipeline.inputBridge.inputSurface).isNull()
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.ERROR)
        assertThat(client.createSessionCount).isEqualTo(0)
    }

    @Test
    fun openGlInitFails_errorReturnsToOffAfterUnbind() {
        // Arrange: OpenGL can't be initialized while an output is registered.
        createSessionManager(failGlInit = true)
        startSession()
        val surfaceOutput = FakeSurfaceOutput(outputSurface)
        processor.onOutputSurface(surfaceOutput)
        shadowOf(getMainLooper()).idleFor(3, TimeUnit.SECONDS)
        val stateWhileBound = currentState().type

        // Act: the effect is unbound, which removes the output.
        sessionManager.renderPipeline.unregisterOutputSurface(surfaceOutput)
        sessionManager.scheduleSessionReleaseIfIdle()
        shadowOf(getMainLooper()).idleFor(3, TimeUnit.SECONDS)

        // Assert: ERROR stays while bound, and returns to OFF after the idle delay.
        assertThat(stateWhileBound).isEqualTo(LowLightBoostEffectState.ERROR)
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.OFF)
    }

    @Test
    fun outputAfterRequestCompleted_keepsIdleRelease() {
        // Arrange: the camera stream stops, which schedules the idle release.
        startSession()
        completeSessionCreation(0)
        surfaceRequests[0].deferrableSurface.close()
        shadowOf(getMainLooper()).idle()

        // Act: an output arrives before the release.
        processor.onOutputSurface(FakeSurfaceOutput(outputSurface))
        shadowOf(getMainLooper()).idleFor(3, TimeUnit.SECONDS)

        // Assert: the session is still released, since no camera stream feeds the output.
        assertThat(createdSessions[0].releaseCount).isEqualTo(1)
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.OFF)
    }

    @Test
    fun nonPrivateOutputAfterRequestCompleted_keepsIdleRelease() {
        // Arrange: the camera stream stops, which schedules the idle release.
        startSession()
        completeSessionCreation(0)
        surfaceRequests[0].deferrableSurface.close()
        shadowOf(getMainLooper()).idle()

        // Act: an output that can't be used arrives before the release.
        processor.onOutputSurface(
            FakeSurfaceOutput(outputSurface, format = ImageFormat.YUV_420_888)
        )
        shadowOf(getMainLooper()).idleFor(3, TimeUnit.SECONDS)

        // Assert: the session is still released.
        assertThat(createdSessions[0].releaseCount).isEqualTo(1)
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.OFF)
    }

    @Test
    fun toggleAfterUnrecoverableError_keepsErrorAndDoesNotCreateSession() {
        // Arrange: a resolution that Low Light Boost doesn't support.
        startSession(resolution = Size(3840, 2160))
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.ERROR)

        // Act.
        processor.setLowLightBoostEnabled(false)
        processor.setLowLightBoostEnabled(true)
        shadowOf(getMainLooper()).idle()

        // Assert.
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.ERROR)
        assertThat(client.createSessionCount).isEqualTo(0)
    }

    @Test
    fun sceneCallbackFromOldSession_isIgnored() {
        // Arrange: the first session is replaced by a recovered one.
        startSession()
        completeSessionCreation(0)
        processor.onOutputSurface(FakeSurfaceOutput(outputSurface))
        val oldSession = createdSessions[0]
        client.callbacks[0].onSessionDestroyed()
        shadowOf(getMainLooper()).idleFor(1, TimeUnit.SECONDS)
        completeSessionCreation(1)
        shadowOf(getMainLooper()).idle()

        // Act: a late scene callback from the old session reports a dark scene.
        requireNotNull(oldSession.sceneDetectorCallback).onSceneBrightnessChanged(oldSession, 0.9f)
        shadowOf(getMainLooper()).idle()

        // Assert.
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.INACTIVE)
    }

    @Test
    fun sessionNeverProducesFrames_isTreatedAsDeadAndRecovered() {
        // Arrange.
        startSession()
        completeSessionCreation(0)
        processor.onOutputSurface(FakeSurfaceOutput(outputSurface))

        // Act: camera frames keep arriving for 7 seconds, but no boosted frame does.
        feedCameraFrames(durationMs = 7_000L)

        // Assert: the session is released, ERROR is reported, and a new session is requested.
        assertThat(createdSessions[0].releaseCount).isEqualTo(1)
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.ERROR)
        shadowOf(getMainLooper()).idleFor(1, TimeUnit.SECONDS)
        assertThat(client.createSessionCount).isEqualTo(2)
    }

    @Test
    fun nonPrivateOutput_isClosed() {
        // Arrange.
        val surfaceOutput = FakeSurfaceOutput(outputSurface, format = ImageFormat.YUV_420_888)

        // Act.
        processor.onOutputSurface(surfaceOutput)

        // Assert.
        assertThat(surfaceOutput.isClosed).isTrue()
        assertThat(sessionManager.renderPipeline.isOutputSurfacesEmpty()).isTrue()
    }

    @Test
    fun moduleNotInstalled_reportsPendingInstallationUntilInstalled() {
        // Arrange.
        client.moduleInstalledTask = { Tasks.forResult(false) }

        // Act.
        startSession()

        // Assert.
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.PENDING_INSTALLATION)
        assertThat(client.createSessionCount).isEqualTo(0)

        // Act.
        requireNotNull(client.installCallbacks.single()).onInstalled()
        shadowOf(getMainLooper()).idle()

        // Assert: PENDING_INSTALLATION ends when the module is installed, before the session is
        // created.
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.INACTIVE)

        // Act.
        completeSessionCreation(0)

        // Assert.
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.INACTIVE)
    }

    @Test
    fun moduleNotInstalledOnUnsupportedDevice_reportsStickyErrorWithoutInstallation() {
        // Arrange: device support isn't cached yet, and the device is unsupported.
        client.moduleInstalledTask = { Tasks.forResult(false) }
        client.deviceSupportedTask = { Tasks.forResult(false) }
        val postedStates = mutableListOf<LowLightBoostEffectState>()
        val observer = Observer<LowLightBoostEffectState> { postedStates.add(it) }
        processor.stateLiveData.observeForever(observer)

        // Act.
        startSession()
        processor.onOutputSurface(FakeSurfaceOutput(outputSurface))
        processor.setLowLightBoostEnabled(false)
        processor.setLowLightBoostEnabled(true)
        feedCameraFrames(durationMs = 3_000L)

        // Assert: the module isn't installed, and ERROR stays after enabling again.
        assertThat(postedStates.map { it.type })
            .doesNotContain(LowLightBoostEffectState.PENDING_INSTALLATION)
        assertThat(client.installCallbacks).isEmpty()
        assertThat(client.createSessionCount).isEqualTo(0)
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.ERROR)
        assertThat(currentState().error).isInstanceOf(UnsupportedOperationException::class.java)
        assertThat(PlayServicesLowLightBoostAvailability.getCachedDeviceSupported()).isFalse()
        processor.stateLiveData.removeObserver(observer)
    }

    @Test
    fun postingExecutor_disabledDuringCreation_sessionCreatedDisabled() {
        // Arrange: GL work is posted like in production.
        createSessionManager(postingExecutor = true)
        startSession()

        // Act: Low Light Boost is disabled while the session is being created.
        processor.setLowLightBoostEnabled(false)
        completeSessionCreation(0)

        // Assert.
        assertThat(createdSessions[0].isLowLightBoostEnabled()).isFalse()
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.OFF)
    }

    @Test
    fun postingExecutor_sessionDestroyedDuringCreation_isReleasedAndReportsError() {
        // Arrange: GL work is posted like in production.
        createSessionManager(postingExecutor = true)
        startSession()

        // Act: the session is destroyed before its creation success is handled.
        client.callbacks[0].onSessionDestroyed()
        completeSessionCreation(0)

        // Assert.
        assertThat(createdSessions[0].releaseCount).isEqualTo(1)
        assertThat(sessionManager.getCurrentSession()).isNull()
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.ERROR)
    }

    @Test
    fun moduleInstallFails_reportsError() {
        // Arrange.
        client.moduleInstalledTask = { Tasks.forResult(false) }
        startSession()
        processor.onOutputSurface(FakeSurfaceOutput(outputSurface))

        // Act: the installation fails, then camera frames keep arriving.
        requireNotNull(client.installCallbacks.single()).onError("No network")
        feedCameraFrames(durationMs = 10_000L)

        // Assert: no background recovery requests the installation again.
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.ERROR)
        assertThat(client.createSessionCount).isEqualTo(0)
        assertThat(client.installCallbacks).hasSize(1)
    }

    @Test
    fun creationFailsAfterAllRetries_noBackgroundRecoveryUntilReEnabled() {
        // Arrange: session creation keeps failing with a transient error.
        client.createSessionError = ApiException(Status(CommonStatusCodes.INTERNAL_ERROR))
        startSession()
        processor.onOutputSurface(FakeSurfaceOutput(outputSurface))
        shadowOf(getMainLooper()).idleFor(30, TimeUnit.SECONDS)
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.ERROR)

        // Act: camera frames keep arriving after ERROR is reported.
        feedCameraFrames(durationMs = 10_000L)

        // Assert: ERROR stays, and no background recovery creates a session.
        assertThat(client.createSessionCount).isEqualTo(1 + MAX_RETRIES)
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.ERROR)

        // Act: the app disables and enables Low Light Boost again.
        processor.setLowLightBoostEnabled(false)
        processor.setLowLightBoostEnabled(true)
        shadowOf(getMainLooper()).idle()

        // Assert: a new session creation is attempted.
        assertThat(client.createSessionCount).isEqualTo(2 + MAX_RETRIES)
    }

    @Test
    fun cameraStreamResumesAfterFailedCreation_createsSessionAgain() {
        // Arrange: session creation fails after all retries while camera frames arrive.
        client.createSessionError = ApiException(Status(CommonStatusCodes.INTERNAL_ERROR))
        startSession()
        processor.onOutputSurface(FakeSurfaceOutput(outputSurface))
        feedCameraFrames(durationMs = 10_000L)
        val countAfterFailure = client.createSessionCount
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.ERROR)

        // Act: the camera stream pauses, e.g. while the app is in the background, then resumes,
        // and the session can be created now.
        client.createSessionError = null
        shadowOf(getMainLooper()).idleFor(5, TimeUnit.SECONDS)
        skipCameraTimestamps(durationMs = 5_000L)
        sessionManager.onInputBridgeFrameAvailable(nextTimestampNs(), IDENTITY)
        completeSessionCreation(client.createSessionCount - 1)

        // Assert: one new session creation is attempted, and it recovers Low Light Boost.
        assertThat(countAfterFailure).isEqualTo(1 + MAX_RETRIES)
        assertThat(client.createSessionCount).isEqualTo(countAfterFailure + 1)
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.INACTIVE)
    }

    @Test
    fun idleSession_isReleasedAfterDelay() {
        // Arrange: no output is connected.
        startSession()
        completeSessionCreation(0)

        // Act.
        sessionManager.scheduleSessionReleaseIfIdle()
        shadowOf(getMainLooper()).idleFor(1, TimeUnit.SECONDS)
        val releaseCountBeforeDelay = createdSessions[0].releaseCount
        shadowOf(getMainLooper()).idleFor(2, TimeUnit.SECONDS)

        // Assert.
        assertThat(releaseCountBeforeDelay).isEqualTo(0)
        assertThat(createdSessions[0].releaseCount).isEqualTo(1)
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.OFF)
    }

    @Test
    fun unboundDuringModuleInstallation_reportsOffAfterIdleDelay() {
        // Arrange.
        client.moduleInstalledTask = { Tasks.forResult(false) }
        startSession()
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.PENDING_INSTALLATION)

        // Act: the effect is unbound, so the request completes without an output.
        surfaceRequests[0].deferrableSurface.close()
        shadowOf(getMainLooper()).idleFor(3, TimeUnit.SECONDS)

        // Assert.
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.OFF)
    }

    @Test
    fun outputIdleDuringModuleInstallation_reportsOffAfterIdleDelay() {
        // Arrange: the module is being installed while the camera stream has no output.
        client.moduleInstalledTask = { Tasks.forResult(false) }
        startSession()
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.PENDING_INSTALLATION)

        // Act: the idle release aborts the installation, while the request stays active.
        sessionManager.scheduleSessionReleaseIfIdle()
        shadowOf(getMainLooper()).idleFor(3, TimeUnit.SECONDS)

        // Assert: the aborted installation isn't reported as pending anymore.
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.OFF)
    }

    @Test
    fun unboundAfterFailedCreation_reportsOffAfterIdleDelay() {
        // Arrange: session creation fails after all retries.
        client.createSessionError = ApiException(Status(CommonStatusCodes.INTERNAL_ERROR))
        startSession()
        shadowOf(getMainLooper()).idleFor(30, TimeUnit.SECONDS)
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.ERROR)

        // Act: the effect is unbound, so the request completes without an output.
        surfaceRequests[0].deferrableSurface.close()
        shadowOf(getMainLooper()).idleFor(3, TimeUnit.SECONDS)

        // Assert.
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.OFF)
    }

    @Test
    fun rebindWithinIdleDelay_reusesSession() {
        // Arrange: the camera stream stops, which schedules the idle release.
        startSession()
        completeSessionCreation(0)
        surfaceRequests[0].deferrableSurface.close()
        shadowOf(getMainLooper()).idle()

        // Act: the camera is bound again with the same resolution before the release.
        startSession()
        shadowOf(getMainLooper()).idleFor(3, TimeUnit.SECONDS)

        // Assert.
        assertThat(client.createSessionCount).isEqualTo(1)
        assertThat(createdSessions[0].releaseCount).isEqualTo(0)
        assertThat(sessionManager.getCurrentSession()).isSameInstanceAs(createdSessions[0])
    }

    @Test
    fun destroySessionInternal_releasesSessionAndRenderers() {
        // Arrange.
        startSession()
        completeSessionCreation(0)
        processor.onOutputSurface(FakeSurfaceOutput(outputSurface))
        val renderersBeforeDestroy = renderers.toList()

        // Act.
        sessionManager.destroySessionInternal()

        // Assert.
        assertThat(createdSessions[0].releaseCount).isEqualTo(1)
        assertThat(renderersBeforeDestroy).isNotEmpty()
        assertThat(renderersBeforeDestroy.all { it.isReleased }).isTrue()
        assertThat(sessionManager.renderPipeline.inputBridge.glRenderer).isNull()
        assertThat(sessionManager.renderPipeline.outputBridge.glRenderer).isNull()
    }

    @Test
    fun cameraStreamResumesAfterPauseWithFailedRecovery_createsSessionAgain() {
        // Arrange: a session is active while camera frames arrive.
        startSession()
        completeSessionCreation(0)
        processor.onOutputSurface(FakeSurfaceOutput(outputSurface))
        feedCameraFrames(durationMs = 1_000L)

        // Act: while the camera stream is paused, the session dies, and its recovery fails after
        // all retries, which call Google Play Services on the GL thread.
        client.createSessionError = ApiException(Status(CommonStatusCodes.INTERNAL_ERROR))
        client.callbacks[0].onSessionDestroyed()
        shadowOf(getMainLooper()).idleFor(10, TimeUnit.SECONDS)
        val countAfterFailure = client.createSessionCount
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.ERROR)
        client.createSessionError = null
        skipCameraTimestamps(durationMs = 10_000L)
        sessionManager.onInputBridgeFrameAvailable(nextTimestampNs(), IDENTITY)

        // Assert: the resumed camera stream allows a new creation round.
        assertThat(countAfterFailure).isEqualTo(2 + MAX_RETRIES)
        assertThat(client.createSessionCount).isEqualTo(countAfterFailure + 1)
    }

    @Test
    fun outputBridgeInitFails_reportsErrorAndStopsRecovery() {
        // Arrange: only the boosted output bridge can't be initialized.
        createSessionManager(failOutputGlInit = true)

        // Act.
        startSession()
        processor.onOutputSurface(FakeSurfaceOutput(outputSurface))
        feedCameraFrames(durationMs = 10_000L)

        // Assert: ERROR is reported once, the camera frames are passed through, and no session or
        // background recovery is attempted.
        assertThat(sessionManager.renderPipeline.inputBridge.inputSurface).isNotNull()
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.ERROR)
        assertThat(currentState().error).isInstanceOf(IllegalStateException::class.java)
        assertThat(client.createSessionCount).isEqualTo(0)
        assertThat(inputRenderer().renderedSurfaces).contains(outputSurface)
    }

    @Test
    fun redundantEnableAfterError_keepsErrorAndDoesNotRetry() {
        // Arrange: session creation fails after all retries.
        client.createSessionError = ApiException(Status(CommonStatusCodes.INTERNAL_ERROR))
        startSession()
        processor.onOutputSurface(FakeSurfaceOutput(outputSurface))
        shadowOf(getMainLooper()).idleFor(30, TimeUnit.SECONDS)
        val countAfterFailure = client.createSessionCount
        client.createSessionError = null

        // Act: the app enables Low Light Boost, which is already enabled.
        processor.setLowLightBoostEnabled(true)
        feedCameraFrames(durationMs = 5_000L)

        // Assert.
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.ERROR)
        assertThat(client.createSessionCount).isEqualTo(countAfterFailure)
    }

    @Test
    fun framesAfterIdleReleaseWithoutOutput_doNotCreateSession() {
        // Arrange: the output is removed while the SurfaceRequest stays alive.
        startSession()
        completeSessionCreation(0)
        val surfaceOutput = FakeSurfaceOutput(outputSurface)
        processor.onOutputSurface(surfaceOutput)
        sessionManager.renderPipeline.unregisterOutputSurface(surfaceOutput)
        sessionManager.scheduleSessionReleaseIfIdle()
        shadowOf(getMainLooper()).idleFor(3, TimeUnit.SECONDS)
        assertThat(createdSessions[0].releaseCount).isEqualTo(1)

        // Act: camera frames keep arriving.
        feedCameraFrames(durationMs = 5_000L)

        // Assert: no session is created for a stream that nothing shows.
        assertThat(client.createSessionCount).isEqualTo(1)
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.OFF)
    }

    @Test
    fun moduleInstalledWhileDisabled_reportsOffAndCreatesDisabledSession() {
        // Arrange.
        client.moduleInstalledTask = { Tasks.forResult(false) }
        startSession()
        processor.setLowLightBoostEnabled(false)
        shadowOf(getMainLooper()).idle()

        // Act.
        requireNotNull(client.installCallbacks.single()).onInstalled()
        shadowOf(getMainLooper()).idle()
        completeSessionCreation(0)

        // Assert.
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.OFF)
        assertThat(createdSessions[0].isLowLightBoostEnabled()).isFalse()
    }

    @Test
    fun cameraStreamResumesWhileDisabled_doesNotCreateSessionOrInstall() {
        // Arrange: the installation fails, then Low Light Boost is disabled.
        client.moduleInstalledTask = { Tasks.forResult(false) }
        startSession()
        processor.onOutputSurface(FakeSurfaceOutput(outputSurface))
        requireNotNull(client.installCallbacks.single()).onError("No network")
        processor.setLowLightBoostEnabled(false)
        feedCameraFrames(durationMs = 1_000L)

        // Act: the camera stream pauses and resumes.
        shadowOf(getMainLooper()).idleFor(5, TimeUnit.SECONDS)
        skipCameraTimestamps(durationMs = 5_000L)
        feedCameraFrames(durationMs = 5_000L)

        // Assert.
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.OFF)
        assertThat(client.installCallbacks).hasSize(1)
        assertThat(client.createSessionCount).isEqualTo(0)
    }

    @Test
    fun nonOwnerProcessorToggle_doesNotChangeOwnerSession() {
        // Arrange: the processor owns an active session.
        startSession()
        completeSessionCreation(0)
        val otherProcessor =
            PlayServicesLowLightBoostSurfaceProcessor(context, { client }, sessionManager)

        // Act.
        otherProcessor.setLowLightBoostEnabled(false)
        shadowOf(getMainLooper()).idle()

        // Assert: only the other processor's own state changes.
        assertThat(createdSessions[0].isLowLightBoostEnabled()).isTrue()
        assertThat(otherProcessor.stateLiveData.value?.type).isEqualTo(LowLightBoostEffectState.OFF)
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.INACTIVE)

        // Act.
        otherProcessor.setLowLightBoostEnabled(true)
        shadowOf(getMainLooper()).idle()

        // Assert.
        assertThat(createdSessions[0].isLowLightBoostEnabled()).isTrue()
        assertThat(otherProcessor.stateLiveData.value?.type).isEqualTo(LowLightBoostEffectState.OFF)
        assertThat(client.createSessionCount).isEqualTo(1)
    }

    @Test
    fun nonOwnerProcessor_cannotTriggerRecovery() {
        // Arrange: the owner's session died while the owner had Low Light Boost disabled.
        startSession()
        completeSessionCreation(0)
        processor.onOutputSurface(FakeSurfaceOutput(outputSurface))
        processor.setLowLightBoostEnabled(false)
        client.callbacks[0].onSessionDestroyed()
        shadowOf(getMainLooper()).idle()
        val otherProcessor =
            PlayServicesLowLightBoostSurfaceProcessor(context, { client }, sessionManager)

        // Act.
        sessionManager.triggerAutoRecoveryIfNeeded(otherProcessor)
        shadowOf(getMainLooper()).idleFor(1, TimeUnit.SECONDS)

        // Assert.
        assertThat(client.createSessionCount).isEqualTo(1)
    }

    @Test
    fun newOwnerDuringModuleInstallation_reportsPendingInstallation() {
        // Arrange: the module is being installed for the first processor's session.
        client.moduleInstalledTask = { Tasks.forResult(false) }
        startSession()
        val previousProcessor = processor
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.PENDING_INSTALLATION)

        // Act: another processor binds the same camera stream.
        processor = PlayServicesLowLightBoostSurfaceProcessor(context, { client }, sessionManager)
        startSession()

        // Assert: the installation continues for the new owner.
        assertThat(previousProcessor.stateLiveData.value?.type)
            .isEqualTo(LowLightBoostEffectState.OFF)
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.PENDING_INSTALLATION)
        assertThat(client.installCallbacks).hasSize(1)

        // Act.
        requireNotNull(client.installCallbacks.single()).onInstalled()
        shadowOf(getMainLooper()).idle()

        // Assert.
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.INACTIVE)
    }

    @Test
    fun newOwnerForAnotherTargetDuringModuleInstallation_doesNotReportStaleState() {
        // Arrange: the module is being installed for the first processor's session.
        client.moduleInstalledTask = { Tasks.forResult(false) }
        startSession()
        val previousProcessor = processor
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.PENDING_INSTALLATION)

        // Act: another processor binds a camera stream with another resolution, and the module is
        // installed meanwhile.
        client.moduleInstalledTask = { Tasks.forResult(true) }
        processor = PlayServicesLowLightBoostSurfaceProcessor(context, { client }, sessionManager)
        startSession(resolution = Size(1280, 720))

        // Assert: the new creation reports its own state.
        assertThat(previousProcessor.stateLiveData.value?.type)
            .isEqualTo(LowLightBoostEffectState.OFF)
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.OFF)
        // The session is created after the cooldown of the cancelled creation.
        shadowOf(getMainLooper()).idleFor(1, TimeUnit.SECONDS)
        assertThat(client.createSessionCount).isEqualTo(1)
    }

    @Test
    fun enabledWithoutRequestDuringModuleInstallation_keepsPendingInstallation() {
        // Arrange: the module is being installed, and the SurfaceRequest closes.
        client.moduleInstalledTask = { Tasks.forResult(false) }
        startSession()
        surfaceRequests[0].deferrableSurface.close()
        shadowOf(getMainLooper()).idle()

        // Act.
        sessionManager.onBoostEnabledChanged(processor, enabled = true)

        // Assert.
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.PENDING_INSTALLATION)
    }

    @Test
    fun errorOfReleasedRequest_isNotReportedByUnboundProcessors() {
        // Arrange: the first processor gets an unrecoverable error, then its request closes and
        // everything is released after the idle delay.
        startSession(resolution = Size(3840, 2160))
        val firstProcessor = processor
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.ERROR)
        surfaceRequests[0].deferrableSurface.close()
        shadowOf(getMainLooper()).idleFor(3, TimeUnit.SECONDS)
        val otherProcessor =
            PlayServicesLowLightBoostSurfaceProcessor(context, { client }, sessionManager)

        // Act: both unbound processors toggle Low Light Boost.
        firstProcessor.setLowLightBoostEnabled(false)
        firstProcessor.setLowLightBoostEnabled(true)
        otherProcessor.setLowLightBoostEnabled(false)
        otherProcessor.setLowLightBoostEnabled(true)
        shadowOf(getMainLooper()).idle()

        // Assert.
        assertThat(firstProcessor.stateLiveData.value?.type).isEqualTo(LowLightBoostEffectState.OFF)
        assertThat(otherProcessor.stateLiveData.value?.type).isEqualTo(LowLightBoostEffectState.OFF)
    }

    @Test
    fun clientCreationFails_reportsErrorOnlyOnceBound() {
        // Arrange: the LowLightBoostClient can't be created.
        val clientError = IllegalStateException("Google Play Services unavailable")
        processor =
            PlayServicesLowLightBoostSurfaceProcessor(
                context,
                { throw clientError },
                sessionManager,
            )

        // Assert: the initial state stays OFF.
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.OFF)

        // Act.
        startSession()

        // Assert.
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.ERROR)
        assertThat(currentState().error).isSameInstanceAs(clientError)
    }

    @Test
    fun newProcessorBinds_previousProcessorReportsOff() {
        // Arrange.
        startSession()
        completeSessionCreation(0)
        val previousProcessor = processor
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.INACTIVE)

        // Act: another processor binds a new request.
        processor = PlayServicesLowLightBoostSurfaceProcessor(context, { client }, sessionManager)
        startSession()

        // Assert.
        assertThat(previousProcessor.stateLiveData.value?.type)
            .isEqualTo(LowLightBoostEffectState.OFF)
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.INACTIVE)
    }

    @Test
    fun outputKeptAcrossSameStreamRequest_isRenderedByNewInputRendererOnly() {
        // Arrange: Low Light Boost is disabled, so frames are passed through to the output.
        processor.setLowLightBoostEnabled(false)
        startSession()
        processor.onOutputSurface(FakeSurfaceOutput(outputSurface))
        feedCameraFrames(durationMs = 300L)
        val oldRenderer = inputRenderer()
        assertThat(oldRenderer.registeredSurfaces).contains(outputSurface)

        // Act: a new request for the same camera stream, while the output stays registered.
        startSession()
        feedCameraFrames(durationMs = 300L)

        // Assert: the output moved to the new input renderer, and the old renderer, still
        // draining the old Surface, doesn't hold it anymore.
        val newRenderer = inputRenderer()
        assertThat(newRenderer).isNotSameInstanceAs(oldRenderer)
        assertThat(newRenderer.registeredSurfaces).contains(outputSurface)
        assertThat(newRenderer.renderedSurfaces).contains(outputSurface)
        assertThat(oldRenderer.registeredSurfaces).doesNotContain(outputSurface)
        assertThat(oldRenderer.isReleased).isFalse()
    }

    @Test
    fun newRequestForSameStream_reusesSessionWithoutNotifyingObservers() {
        // Arrange: an active session renders to an output.
        startSession()
        completeSessionCreation(0)
        processor.onOutputSurface(FakeSurfaceOutput(outputSurface))
        feedCameraFrames(durationMs = 500L)
        val oldInputSurface = requireNotNull(sessionManager.renderPipeline.inputBridge.inputSurface)
        val postedStates = mutableListOf<LowLightBoostEffectState>()
        val observer = Observer<LowLightBoostEffectState> { postedStates.add(it) }
        processor.stateLiveData.observeForever(observer)
        postedStates.clear()

        // Act: CameraX recreates the pipeline for the same camera stream, e.g. after a Preview
        // target rotation change. The new output replaces the old one, and then the old request
        // completes.
        startSession()
        processor.onOutputSurface(FakeSurfaceOutput(outputSurface))
        surfaceRequests[0].deferrableSurface.close()
        feedCameraFrames(durationMs = 3_000L)

        // Assert: the session is reused with a new input Surface, whose renderer sends camera
        // frames to the session, and no state is reported again.
        val newInputSurface = requireNotNull(sessionManager.renderPipeline.inputBridge.inputSurface)
        val cameraSurface = createdSessions[0].cameraSurfaceForTest
        assertThat(client.createSessionCount).isEqualTo(1)
        assertThat(createdSessions[0].releaseCount).isEqualTo(0)
        assertThat(sessionManager.getCurrentSession()).isSameInstanceAs(createdSessions[0])
        assertThat(newInputSurface).isNotSameInstanceAs(oldInputSurface)
        assertThat(oldInputSurface.isValid).isFalse()
        assertThat(sessionManager.renderPipeline.isSessionCameraSurfaceBound(cameraSurface))
            .isTrue()
        assertThat(inputRenderer().registeredSurfaces).contains(cameraSurface)
        assertThat(inputRenderer().renderedSurfaces).contains(cameraSurface)
        assertThat(postedStates).isEmpty()
        processor.stateLiveData.removeObserver(observer)
    }

    @Test
    fun newRequestForSameStreamAfterFailedCreation_keepsErrorWithoutRetrying() {
        // Arrange: session creation fails after all retries.
        client.createSessionError = ApiException(Status(CommonStatusCodes.INTERNAL_ERROR))
        startSession()
        shadowOf(getMainLooper()).idleFor(30, TimeUnit.SECONDS)
        val errorState = currentState()
        assertThat(errorState.type).isEqualTo(LowLightBoostEffectState.ERROR)
        val postedStates = mutableListOf<LowLightBoostEffectState>()
        val observer = Observer<LowLightBoostEffectState> { postedStates.add(it) }
        processor.stateLiveData.observeForever(observer)
        postedStates.clear()

        // Act: a new SurfaceRequest for the same camera stream.
        startSession()
        shadowOf(getMainLooper()).idleFor(30, TimeUnit.SECONDS)

        // Assert: the failed creation isn't repeated, and ERROR isn't reported again.
        assertThat(client.createSessionCount).isEqualTo(1 + MAX_RETRIES)
        assertThat(currentState()).isSameInstanceAs(errorState)
        assertThat(postedStates).isEmpty()
        processor.stateLiveData.removeObserver(observer)
    }

    @Test
    fun newRequestForAnotherResolutionAfterFailedCreation_retries() {
        // Arrange: session creation fails after all retries.
        client.createSessionError = ApiException(Status(CommonStatusCodes.INTERNAL_ERROR))
        startSession()
        shadowOf(getMainLooper()).idleFor(30, TimeUnit.SECONDS)
        assertThat(currentState().type).isEqualTo(LowLightBoostEffectState.ERROR)

        // Act: a SurfaceRequest for another resolution.
        startSession(resolution = Size(1280, 720))

        // Assert: a new creation is attempted.
        assertThat(client.createSessionCount).isEqualTo(2 + MAX_RETRIES)
    }

    @Test
    fun inputFromVirtualCamera_createsSessionForRealCamera() {
        // Act: e.g. a Preview child of StreamSharing gets a SurfaceRequest from a virtual camera,
        // whose CameraInfoInternal forwards to the real camera.
        sendRequestFromVirtualCamera("virtual-0")

        // Assert.
        assertThat(client.createSessionOptions.single().cameraId).isEqualTo("0")
    }

    @Test
    fun newRequestFromAnotherVirtualCameraOfSameCamera_keepsError() {
        // Arrange: the camera is cached as unsupported, and a request from a virtual camera of it
        // reports ERROR.
        PlayServicesLowLightBoostAvailability.setCameraSupported("0", false)
        sendRequestFromVirtualCamera("virtual-0-a")
        val errorState = currentState()
        assertThat(errorState.type).isEqualTo(LowLightBoostEffectState.ERROR)
        val postedStates = mutableListOf<LowLightBoostEffectState>()
        val observer = Observer<LowLightBoostEffectState> { postedStates.add(it) }
        processor.stateLiveData.observeForever(observer)
        postedStates.clear()

        // Act: StreamSharing is recreated, e.g. after a rebind, with a new virtual camera ID.
        sendRequestFromVirtualCamera("virtual-0-b")

        // Assert: it is the same camera stream, so ERROR isn't reported again.
        assertThat(currentState()).isSameInstanceAs(errorState)
        assertThat(postedStates).isEmpty()
        processor.stateLiveData.removeObserver(observer)
    }

    private fun sendRequestFromVirtualCamera(virtualCameraId: String) {
        val virtualCameraInfo =
            object : ForwardingCameraInfo(FakeCameraInfoInternal("0")) {
                override fun getCameraId(): String = virtualCameraId
            }
        val surfaceRequest =
            SurfaceRequest(
                Size(1920, 1080),
                FakeCamera(virtualCameraInfo),
                DynamicRange.SDR,
                FPS,
            ) {}
        surfaceRequests.add(surfaceRequest)
        processor.onInputSurface(surfaceRequest)
        shadowOf(getMainLooper()).idle()
    }

    @Test
    fun destroySessionInternal_releasesDetachedInputSurface() {
        // Arrange: the old input Surface is kept for a request that hasn't completed.
        startSession()
        val oldRenderer = inputRenderer()
        val oldInputSurface = requireNotNull(sessionManager.renderPipeline.inputBridge.inputSurface)
        startSession(resolution = Size(1280, 720))
        assertThat(oldInputSurface.isValid).isTrue()
        assertThat(oldRenderer.isReleased).isFalse()

        // Act.
        sessionManager.destroySessionInternal()

        // Assert.
        assertThat(oldInputSurface.isValid).isFalse()
        assertThat(oldRenderer.isReleased).isTrue()
    }

    private var lastTimestampNs = 0L

    private fun nextTimestampNs(): Long {
        lastTimestampNs += FRAME_INTERVAL_MS * 1_000_000L
        return lastTimestampNs
    }

    /** Advances the camera timestamps by [durationMs], like a pause of the camera stream. */
    private fun skipCameraTimestamps(durationMs: Long) {
        lastTimestampNs += durationMs * 1_000_000L
    }

    /** Sends a camera frame every [FRAME_INTERVAL_MS] for [durationMs]. */
    private fun feedCameraFrames(durationMs: Long) {
        repeat((durationMs / FRAME_INTERVAL_MS).toInt()) {
            shadowOf(getMainLooper()).idleFor(FRAME_INTERVAL_MS, TimeUnit.MILLISECONDS)
            sessionManager.onInputBridgeFrameAvailable(nextTimestampNs(), IDENTITY)
        }
    }

    private fun inputRenderer(): FakeGlRenderer =
        sessionManager.renderPipeline.inputBridge.glRenderer as FakeGlRenderer

    private fun outputRenderer(): FakeGlRenderer =
        sessionManager.renderPipeline.outputBridge.glRenderer as FakeGlRenderer

    private fun startSession(resolution: Size = Size(1920, 1080)) {
        val surfaceRequest = SurfaceRequest(resolution, FakeCamera("0"), DynamicRange.SDR, FPS) {}
        surfaceRequests.add(surfaceRequest)
        sessionManager.bindInputSurfaceRequest(
            processor,
            surfaceRequest,
            surfaceRequest.camera.cameraInfoInternal,
            "0",
        )
        sessionManager.provideInputSurfaceAndPrepareSession(
            processor = processor,
            surfaceRequest = surfaceRequest,
            resolution = resolution,
            cameraId = "0",
        )
        shadowOf(getMainLooper()).idle()
    }

    private fun completeSessionCreation(index: Int) {
        val session = FakeLowLightBoostSession()
        createdSessions.add(session)
        client.createSessionResults[index].setResult(session)
        shadowOf(getMainLooper()).idle()
    }

    private fun currentState(): LowLightBoostEffectState {
        shadowOf(getMainLooper()).idle()
        return requireNotNull(processor.stateLiveData.value)
    }

    private companion object {
        private const val OUTPUT_TEXTURE_NAME = 3
        private val IDENTITY = FloatArray(16).apply { Matrix.setIdentityM(this, 0) }
        private const val MAX_AUTO_RECOVERY_ATTEMPTS = 3
        private const val MAX_RETRIES = 3
        private const val FRAME_INTERVAL_MS = 100L
        private val FPS = SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED
    }
}
