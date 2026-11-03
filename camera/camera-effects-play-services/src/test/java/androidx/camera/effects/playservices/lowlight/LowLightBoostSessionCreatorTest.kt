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
import android.os.Build
import android.os.Handler
import android.os.Looper.getMainLooper
import android.util.Size
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.test.filters.SdkSuppress
import com.google.android.gms.cameralowlight.LowLightBoostSession
import com.google.android.gms.cameralowlight.LowLightBoostStatusCodes
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

/**
 * Verifies [LowLightBoostSessionCreator] with a fake Google Play Services client and a fake host.
 * How the session manager handles the outcomes is covered by its own tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
// Tells lint that the test needs API level 30 for LowLightBoostSession.
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
class LowLightBoostSessionCreatorTest {

    private val handler = Handler(getMainLooper())
    private val renderPipeline =
        LowLightBoostRenderPipeline(
            glHandler = handler,
            rendererFactory = { FakeGlRenderer() },
            onInputFrameAvailable = { _, _ -> },
            onOutputFrameAvailable = { _, _ -> },
        )
    private val client = FakeLowLightBoostClient()
    private val host = FakeHost()
    private val creator =
        LowLightBoostSessionCreator(
            CameraXExecutors.directExecutor(),
            handler,
            renderPipeline,
            BlockingCallTracker(),
            host,
        )
    private val createdSessions = mutableListOf<FakeLowLightBoostSession>()

    @Before
    fun setUp() {
        PlayServicesLowLightBoostAvailability.reset()
        PlayServicesLowLightBoostAvailability.clientFactoryForTesting = { client }
        host.ownerContext = RuntimeEnvironment.getApplication()
    }

    @After
    fun tearDown() {
        renderPipeline.releaseBridges()
        createdSessions.forEach { it.cameraSurfaceTexture.release() }
        PlayServicesLowLightBoostAvailability.reset()
    }

    @Test
    fun createdSession_isReportedAndSupportIsCached() {
        // Act.
        startCreation()
        completeSessionCreation(0)

        // Assert.
        assertThat(host.createdSession).isSameInstanceAs(createdSessions[0])
        assertThat(PlayServicesLowLightBoostAvailability.getCachedDeviceSupported()).isTrue()
        assertThat(PlayServicesLowLightBoostAvailability.getCachedCameraSupported("0")).isTrue()
    }

    @Test
    fun noOwner_creationIsAbortedWithoutCreatingSession() {
        // Arrange: the session has no owner, e.g. the effect was unbound meanwhile.
        host.ownerContext = null

        // Act.
        startCreation()

        // Assert.
        assertThat(host.abortedCount).isEqualTo(1)
        assertThat(client.createSessionCount).isEqualTo(0)
        assertThat(host.failure).isNull()
    }

    @Test
    fun boostEnabledSetting_isPassedToCreateSession() {
        // Act: one creation with Low Light Boost disabled, then one with it enabled.
        host.boostEnabled = false
        startCreation(FIRST_CREATION_ID)
        host.boostEnabled = true
        startCreation(OTHER_CREATION_ID)

        // Assert.
        assertThat(client.createSessionOptions.map { it.enableLowLightBoost })
            .containsExactly(false, true)
            .inOrder()
    }

    @Test
    fun creationAfterSessionRelease_waitsForCleanupDelay() {
        // Arrange.
        creator.onSessionReleased()

        // Act.
        startCreation()
        val countBeforeDelay = client.createSessionCount
        shadowOf(getMainLooper()).idleFor(300, TimeUnit.MILLISECONDS)
        val countBeforeDelayEnds = client.createSessionCount
        shadowOf(getMainLooper()).idleFor(100, TimeUnit.MILLISECONDS)

        // Assert: createSession() is called once the 350 ms cleanup delay has passed.
        assertThat(countBeforeDelay).isEqualTo(0)
        assertThat(countBeforeDelayEnds).isEqualTo(0)
        assertThat(client.createSessionCount).isEqualTo(1)
    }

    @Test
    fun sessionTerminatedBeforeCreationCompletes_isReleasedAndReported() {
        // Arrange.
        startCreation()

        var releaseCountWhenReported: Int? = null
        host.onCreatedSessionTerminatedListener = {
            releaseCountWhenReported = createdSessions[0].releaseCount
        }

        // Act: the session is destroyed before the creation success is delivered.
        client.callbacks[0].onSessionDestroyed()
        completeSessionCreation(0)

        // Assert: the dead session is released, before the termination is reported, instead of
        // being reported as created.
        assertThat(createdSessions[0].releaseCount).isEqualTo(1)
        assertThat(releaseCountWhenReported).isEqualTo(1)
        assertThat(host.createdSession).isNull()
        assertThat(host.createdSessionTerminatedCount).isEqualTo(1)
    }

    @Test
    fun creationCompletesAfterItWasSuperseded_staleSessionIsReleased() {
        // Arrange: another creation replaces the first one, e.g. for a new SurfaceRequest.
        startCreation()
        host.currentCreationId = OTHER_CREATION_ID

        // Act.
        completeSessionCreation(0)

        // Assert.
        assertThat(createdSessions[0].releaseCount).isEqualTo(1)
        assertThat(host.createdSession).isNull()
    }

    @Test
    fun createSessionFailsWithApiNotConnected_deviceNotCachedAsUnsupported() {
        // Arrange: Play Services is temporarily unavailable, e.g. while it is being updated.
        client.createSessionError = ApiException(Status(CommonStatusCodes.API_NOT_CONNECTED))

        // Act: run through all retries.
        startCreation()
        shadowOf(getMainLooper()).idleFor(30, TimeUnit.SECONDS)

        // Assert: the failure is reported, but the device is not cached as unsupported.
        assertThat(host.failure).isInstanceOf(ApiException::class.java)
        assertThat(host.isFailureUnrecoverable).isFalse()
        assertThat(PlayServicesLowLightBoostAvailability.getCachedDeviceSupported()).isNull()
    }

    @Test
    fun transientCreationFailure_isRetriedWithBackoffThenReported() {
        // Arrange.
        client.createSessionError = ApiException(Status(CommonStatusCodes.INTERNAL_ERROR))

        // Act.
        startCreation()
        shadowOf(getMainLooper()).idleFor(500, TimeUnit.MILLISECONDS)
        val countAfterHalfSecond = client.createSessionCount
        shadowOf(getMainLooper()).idleFor(3_000, TimeUnit.MILLISECONDS)
        val countBeforeLastRetry = client.createSessionCount
        val failureBeforeLastRetry = host.failure
        shadowOf(getMainLooper()).idleFor(1_000, TimeUnit.MILLISECONDS)

        // Assert: the retries run at 1, 2 and 4 seconds, and the failure is only reported after
        // the last one fails.
        assertThat(countAfterHalfSecond).isEqualTo(1)
        assertThat(countBeforeLastRetry).isEqualTo(MAX_RETRIES)
        assertThat(failureBeforeLastRetry).isNull()
        assertThat(client.createSessionCount).isEqualTo(1 + MAX_RETRIES)
        assertThat(host.failure).isInstanceOf(ApiException::class.java)
        assertThat(host.isFailureUnrecoverable).isFalse()
    }

    @Test
    fun createSessionThrowsSynchronously_isRetriedThenReported() {
        // Arrange: createSession() throws instead of returning a failed task.
        client.createSessionThrowable = IllegalStateException("Binder failed")

        // Act: run through all retries.
        startCreation()
        shadowOf(getMainLooper()).idleFor(30, TimeUnit.SECONDS)

        // Assert: the call is retried like a failed task.
        assertThat(client.createSessionCount).isEqualTo(1 + MAX_RETRIES)
        assertThat(host.failure).isInstanceOf(IllegalStateException::class.java)
        assertThat(host.isFailureUnrecoverable).isFalse()
    }

    @Test
    fun cameraNotSupported_isNotRetriedAndIsCached() {
        // Arrange.
        client.createSessionError =
            ApiException(Status(LowLightBoostStatusCodes.CAMERA_NOT_SUPPORTED))

        // Act.
        startCreation()
        shadowOf(getMainLooper()).idleFor(30, TimeUnit.SECONDS)

        // Assert.
        assertThat(client.createSessionCount).isEqualTo(1)
        assertThat(host.isFailureUnrecoverable).isTrue()
        assertThat(PlayServicesLowLightBoostAvailability.getCachedCameraSupported("0")).isFalse()
    }

    @Test
    fun moduleNotInstalledOnSupportedDevice_queriesSupportOnceAndInstalls() {
        // Arrange: support isn't cached yet.
        client.moduleInstalledTask = { Tasks.forResult(false) }

        // Act.
        startCreation()

        // Assert.
        assertThat(client.deviceSupportedCount).isEqualTo(1)
        assertThat(client.cameraSupportedCount).isEqualTo(1)
        assertThat(client.installCallbacks).hasSize(1)
        assertThat(host.installationStartedCount).isEqualTo(1)
    }

    @Test
    fun moduleNotInstalledOnUnsupportedCamera_failsWithoutInstallation() {
        // Arrange: camera support isn't cached yet, and the camera is unsupported.
        client.moduleInstalledTask = { Tasks.forResult(false) }
        client.cameraSupportedTask = { Tasks.forResult(false) }

        // Act.
        startCreation()

        // Assert.
        assertThat(client.installCallbacks).isEmpty()
        assertThat(host.installationStartedCount).isEqualTo(0)
        assertThat(host.failure).isInstanceOf(UnsupportedOperationException::class.java)
        assertThat(host.isFailureUnrecoverable).isTrue()
    }

    @Test
    fun installCallbackOfSupersededCreation_isIgnored() {
        // Arrange: the module isn't installed, and a new creation replaces the first one.
        client.moduleInstalledTask = { Tasks.forResult(false) }
        startCreation(creationId = FIRST_CREATION_ID)
        startCreation(creationId = OTHER_CREATION_ID)

        // Act: the installation of the first creation completes.
        requireNotNull(client.installCallbacks[0]).onInstalled()
        shadowOf(getMainLooper()).idleFor(1, TimeUnit.SECONDS)

        // Assert: only the current creation can create a session.
        assertThat(client.createSessionCount).isEqualTo(0)
        assertThat(host.installedCount).isEqualTo(0)
        requireNotNull(client.installCallbacks[1]).onInstalled()
        shadowOf(getMainLooper()).idleFor(1, TimeUnit.SECONDS)
        assertThat(client.createSessionCount).isEqualTo(1)
        assertThat(host.installedCount).isEqualTo(1)
    }

    @Test
    fun installModuleTaskFails_failsCreation() {
        // Arrange: the installation request fails without calling the install callback.
        client.moduleInstalledTask = { Tasks.forResult(false) }
        client.installModuleTask = {
            Tasks.forException(ApiException(Status(CommonStatusCodes.NETWORK_ERROR)))
        }

        // Act.
        startCreation()

        // Assert.
        assertThat(host.failure).hasMessageThat().startsWith("Module install failed")
        assertThat(host.isFailureUnrecoverable).isFalse()
        assertThat(client.installCallbacks).hasSize(1)
        assertThat(client.createSessionCount).isEqualTo(0)
    }

    @Test
    fun installModuleTaskCancelled_failsCreation() {
        // Arrange: the installation request is cancelled without calling the install callback.
        client.moduleInstalledTask = { Tasks.forResult(false) }
        client.installModuleTask = { Tasks.forCanceled() }

        // Act.
        startCreation()

        // Assert.
        assertThat(host.failure).hasMessageThat().isEqualTo("Module install cancelled")
        assertThat(client.installCallbacks).hasSize(1)
        assertThat(client.createSessionCount).isEqualTo(0)
    }

    @Test
    fun testIsUnrecoverableError() {
        // Unrecoverable exceptions
        assertThat(
                LowLightBoostSessionCreator.isUnrecoverableError(
                    IllegalArgumentException("Invalid")
                )
            )
            .isTrue()
        assertThat(
                LowLightBoostSessionCreator.isUnrecoverableError(
                    UnsupportedOperationException("Unsupported")
                )
            )
            .isTrue()
        assertThat(LowLightBoostSessionCreator.isUnrecoverableError(SecurityException("Denied")))
            .isTrue()

        // Unrecoverable Status Codes
        assertThat(
                LowLightBoostSessionCreator.isUnrecoverableError(
                    ApiException(
                        Status(
                            LowLightBoostStatusCodes.CAMERA_NOT_SUPPORTED,
                            "Camera not supported",
                        )
                    )
                )
            )
            .isTrue()
        assertThat(
                LowLightBoostSessionCreator.isUnrecoverableError(
                    ApiException(
                        Status(
                            LowLightBoostStatusCodes.UNSUPPORTED_DATASPACE,
                            "Dataspace not supported",
                        )
                    )
                )
            )
            .isTrue()
        assertThat(
                LowLightBoostSessionCreator.isUnrecoverableError(
                    ApiException(
                        Status(
                            LowLightBoostStatusCodes.MISSING_CAMERA_CHARACTERISTICS,
                            "Missing characteristics",
                        )
                    )
                )
            )
            .isTrue()
        assertThat(
                LowLightBoostSessionCreator.isUnrecoverableError(
                    ApiException(
                        Status(
                            LowLightBoostStatusCodes.INVALID_FRAME_METADATA,
                            "Invalid frame metadata",
                        )
                    )
                )
            )
            .isTrue()
        assertThat(
                LowLightBoostSessionCreator.isUnrecoverableError(
                    ApiException(Status(CommonStatusCodes.DEVELOPER_ERROR, "Developer error"))
                )
            )
            .isTrue()

        // When device support is unknown (null) or true, API_NOT_CONNECTED is treated as transient
        assertThat(
                LowLightBoostSessionCreator.isUnrecoverableError(
                    ApiException(Status(CommonStatusCodes.API_NOT_CONNECTED, "Not connected"))
                )
            )
            .isFalse()
        PlayServicesLowLightBoostAvailability.setDeviceSupported(true)
        assertThat(
                LowLightBoostSessionCreator.isUnrecoverableError(
                    ApiException(Status(CommonStatusCodes.API_NOT_CONNECTED, "Not connected"))
                )
            )
            .isFalse()

        // When device is explicitly cached as unsupported (false), API_NOT_CONNECTED is
        // unrecoverable
        PlayServicesLowLightBoostAvailability.setDeviceSupported(false)
        assertThat(
                LowLightBoostSessionCreator.isUnrecoverableError(
                    ApiException(Status(CommonStatusCodes.API_NOT_CONNECTED, "Not connected"))
                )
            )
            .isTrue()
        PlayServicesLowLightBoostAvailability.reset()

        // Retriable / Recoverable Status Codes
        assertThat(
                LowLightBoostSessionCreator.isUnrecoverableError(
                    ApiException(
                        Status(
                            LowLightBoostStatusCodes.MAX_SESSIONS_REACHED,
                            "Max sessions reached",
                        )
                    )
                )
            )
            .isFalse()
        assertThat(
                LowLightBoostSessionCreator.isUnrecoverableError(
                    ApiException(
                        Status(LowLightBoostStatusCodes.GLOBAL_INIT_FAILED, "Global init failed")
                    )
                )
            )
            .isFalse()
        assertThat(
                LowLightBoostSessionCreator.isUnrecoverableError(
                    ApiException(
                        Status(LowLightBoostStatusCodes.SESSION_INIT_FAILED, "Session init failed")
                    )
                )
            )
            .isFalse()
        assertThat(
                LowLightBoostSessionCreator.isUnrecoverableError(
                    RuntimeException("Generic transient error")
                )
            )
            .isFalse()
    }

    private fun startCreation(creationId: Long = FIRST_CREATION_ID) {
        host.currentCreationId = creationId
        creator.checkModuleAndCreateSession(creationId, client, TARGET)
        shadowOf(getMainLooper()).idle()
    }

    private fun completeSessionCreation(index: Int) {
        val session = FakeLowLightBoostSession()
        createdSessions.add(session)
        client.createSessionResults[index].setResult(session)
        shadowOf(getMainLooper()).idle()
    }

    /** Records the outcomes. Like the manager, it ends the current creation with an outcome. */
    private class FakeHost : LowLightBoostSessionCreator.Host {
        var currentCreationId = NO_CREATION_ID
        var failure: Throwable? = null
        var isFailureUnrecoverable: Boolean? = null
        var createdSession: LowLightBoostSession? = null
        var createdSessionTerminatedCount = 0
        var onCreatedSessionTerminatedListener: () -> Unit = {}
        var installationStartedCount = 0
        var installedCount = 0
        var abortedCount = 0
        var boostEnabled = true

        override var ownerContext: Context? = null

        override fun isBoostEnabled(): Boolean = boostEnabled

        override fun isCurrentCreation(creationId: Long): Boolean = creationId == currentCreationId

        override fun onCreationAborted() {
            abortedCount++
            currentCreationId = NO_CREATION_ID
        }

        override fun onModuleInstallationStarted() {
            installationStartedCount++
        }

        override fun onModuleInstalled() {
            installedCount++
        }

        override fun onCreationFailed(error: Throwable, unrecoverable: Boolean) {
            failure = error
            isFailureUnrecoverable = unrecoverable
            currentCreationId = NO_CREATION_ID
        }

        override fun onCreatedSessionTerminated() {
            createdSessionTerminatedCount++
            onCreatedSessionTerminatedListener()
            currentCreationId = NO_CREATION_ID
        }

        override fun onSessionCreated(session: LowLightBoostSession, target: SessionTarget) {
            createdSession = session
            currentCreationId = NO_CREATION_ID
        }

        override fun onSessionTerminated(session: LowLightBoostSession, errorMessage: String) {}
    }

    private companion object {
        private const val NO_CREATION_ID = 0L
        private const val FIRST_CREATION_ID = 1L
        private const val OTHER_CREATION_ID = 2L
        private const val MAX_RETRIES = 3
        private val TARGET = SessionTarget(Size(1920, 1080), "0")
    }
}
