/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.runtime

import android.Manifest
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.testing.FakeLifecycleManager
import androidx.xr.arcore.testing.FakePerceptionRuntime
import androidx.xr.arcore.testing.FakePerceptionRuntimeFactory
import androidx.xr.arcore.testing.FakeStateExtender
import androidx.xr.runtime.internal.ApkCheckAvailabilityErrorException
import androidx.xr.runtime.internal.ApkCheckAvailabilityInProgressException
import androidx.xr.runtime.internal.ApkNotInstalledException
import androidx.xr.runtime.internal.UnsupportedDeviceException
import androidx.xr.scenecore.testing.FakeRenderingRuntime
import androidx.xr.scenecore.testing.FakeSceneRuntime
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

// TODO(b/440615454) - Use local Fakes instead of FakeSceneRuntime/FakePerceptionRuntime.
@RunWith(AndroidJUnit4::class)
class SessionTest {
    private lateinit var underTest: Session
    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()

        val shadowApplication = shadowOf(activity.application)
        FakeLifecycleManager.TestPermissions.forEach { permission ->
            shadowApplication.grantPermissions(permission)
        }

        FakePerceptionRuntimeFactory.hasCreatePermission = true
    }

    @After
    fun tearDown() {
        if (activity.lifecycle.currentState != Lifecycle.State.DESTROYED) {
            activityController.destroy()
        }
    }

    @Test
    fun create_returnsSuccessResultWithNonNullSession() {
        activityController.create()

        val result = Session.create(activity)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)
        assertThat((result as SessionCreateSuccess).session).isNotNull()
    }

    @Test
    fun create_setsLifecycleToInitialized() {
        activityController.create()

        underTest = createSession()

        val lifecycleManager = getLifecycleManager()
        assertThat(lifecycleManager.state).isEqualTo(FakeLifecycleManager.State.INITIALIZED)
    }

    @Test
    fun create_initializesStateExtender() {
        activityController.create()

        underTest = createSession()

        val stateExtender = underTest.stateExtenders.last() as FakeStateExtender
        assertThat(stateExtender.isInitialized).isTrue()
    }

    @Test
    fun create_initializesRuntime() {
        activityController.create()

        underTest = createSession()

        assertThat(getSceneRuntime()).isNotNull()
        assertThat(getRenderingRuntime().state.name).isEqualTo("CREATED")
    }

    @Test
    fun create_permissionNotGranted_throwsSecurityException() {
        val shadowApplication = shadowOf(activity.application)
        shadowApplication.denyPermissions(Manifest.permission.CAMERA)
        FakePerceptionRuntimeFactory.hasCreatePermission = false

        activityController.create()

        assertFailsWith<SecurityException> { Session.create(activity) }
    }

    @Test
    fun create_arcoreNotInstalledException_returnsApkRequiredResult() {
        FakePerceptionRuntimeFactory.lifecycleCreateException =
            ApkNotInstalledException(ARCORE_PACKAGE_NAME)
        activityController.create()

        val result = Session.create(activity)

        assertThat(result).isInstanceOf(SessionCreateApkRequired::class.java)
        assertThat((result as SessionCreateApkRequired).requiredApk).isEqualTo(ARCORE_PACKAGE_NAME)
    }

    @Test
    fun create_arcoreUnsupportedDeviceException_returnsUnsupportedDeviceResult() {
        FakePerceptionRuntimeFactory.lifecycleCreateException = UnsupportedDeviceException()
        activityController.create()

        val result = Session.create(activity)

        assertThat(result).isInstanceOf(SessionCreateUnsupportedDevice::class.java)
    }

    @Test
    fun create_arcoreCheckAvailabilityInProgressException_returnsApkRequiredResult() {
        FakePerceptionRuntimeFactory.lifecycleCreateException =
            ApkCheckAvailabilityInProgressException(ARCORE_PACKAGE_NAME)
        activityController.create()

        val result = Session.create(activity)

        assertThat(result).isInstanceOf(SessionCreateApkRequired::class.java)
        assertThat((result as SessionCreateApkRequired).requiredApk).isEqualTo(ARCORE_PACKAGE_NAME)
    }

    @Test
    fun create_arcoreCheckAvailabilityErrorException_returnsApkRequiredResult() {
        FakePerceptionRuntimeFactory.lifecycleCreateException =
            ApkCheckAvailabilityErrorException(ARCORE_PACKAGE_NAME)
        activityController.create()

        val result = Session.create(activity)

        assertThat(result).isInstanceOf(SessionCreateApkRequired::class.java)
        assertThat((result as SessionCreateApkRequired).requiredApk).isEqualTo(ARCORE_PACKAGE_NAME)
    }

    @Test
    fun create_onDestroyedActivity_throwsIllegalStateException() {
        activityController.create().destroy()

        val exception = assertFailsWith<IllegalStateException> { Session.create(activity) }
        assertThat(exception)
            .hasMessageThat()
            .isEqualTo("Cannot create a new session on a destroyed activity.")
    }

    @Test
    fun configure_destroyed_throwsIllegalStateException() {
        activityController.create().start().resume()
        underTest = createSession()
        activityController.destroy()

        assertFailsWith<IllegalStateException> { underTest.configure(Config()) }
    }

    @Test
    fun configure_returnsSuccessAndChangesConfig() {
        activityController.create().start().resume()
        underTest = createSession()
        val lifecycleManager = getLifecycleManager()
        check(
            lifecycleManager.config ==
                Config(
                    planeTracking = Config.PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
                    augmentedObjectCategories = AugmentedObjectCategory.all(),
                    handTracking = Config.HandTrackingMode.BOTH,
                    deviceTracking = Config.DeviceTrackingMode.LAST_KNOWN,
                    depthEstimation = Config.DepthEstimationMode.SMOOTH_AND_RAW,
                    anchorPersistence = Config.AnchorPersistenceMode.LOCAL,
                )
        )
        val newConfig =
            Config(
                planeTracking = Config.PlaneTrackingMode.DISABLED,
                augmentedObjectCategories = listOf<AugmentedObjectCategory>(),
                handTracking = Config.HandTrackingMode.DISABLED,
                deviceTracking = Config.DeviceTrackingMode.DISABLED,
                depthEstimation = Config.DepthEstimationMode.DISABLED,
                anchorPersistence = Config.AnchorPersistenceMode.DISABLED,
            )

        val result = underTest.configure(newConfig)

        assertThat(result).isInstanceOf(SessionConfigureSuccess::class.java)
        assertThat(lifecycleManager.config).isEqualTo(newConfig)
    }

    @Test
    fun configure_permissionNotGranted_throwsSecurityException() {
        activityController.create().start().resume()
        underTest = createSession()
        val lifecycleManager = getLifecycleManager()

        val currentConfig = lifecycleManager.config
        check(currentConfig.depthEstimation == Config.DepthEstimationMode.SMOOTH_AND_RAW)
        lifecycleManager.hasMissingPermission = true

        assertFailsWith<SecurityException> {
            underTest.configure(
                underTest.config.copy(
                    depthEstimation = Config.DepthEstimationMode.DISABLED,
                    faceTracking = Config.FaceTrackingMode.DISABLED,
                )
            )
        }
        assertThat(lifecycleManager.config).isEqualTo(currentConfig)
    }

    @Test
    fun configure_unsupportedMode_throwsUnsupportedOperationException() {
        activityController.create().start().resume()
        underTest = createSession()
        val lifecycleManager = getLifecycleManager()

        val currentConfig = underTest.config
        lifecycleManager.shouldSupportPlaneTracking = false

        assertFailsWith<UnsupportedOperationException> {
            underTest.configure(
                currentConfig.copy(planeTracking = Config.PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
            )
        }
        assertThat(underTest.config).isEqualTo(currentConfig)
        lifecycleManager.shouldSupportPlaneTracking = true
    }

    @Test
    fun resume_returnsSuccessAndSetsLifecycleToResumed() {
        activityController.create().start()
        underTest = createSession()

        activityController.resume()

        val lifecycleManager = getLifecycleManager()

        assertThat(lifecycleManager.state).isEqualTo(FakeLifecycleManager.State.RESUMED)
    }

    @Test
    fun resume_returnsSuccessAndSetsPlatformAdapterToResumed() {
        activityController.create().start()
        underTest = createSession()

        activityController.resume()

        assertThat(getRenderingRuntime().state)
            .isEqualTo(FakeRenderingRuntime.State.STARTED) // Corresponds to resumed
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun update_emitsUpdatedState() =
        runTest(testDispatcher) {
            activityController.create().start()
            underTest = createSession(coroutineDispatcher = testDispatcher)
            val lifecycleManager = getLifecycleManager()

            val timeSource = lifecycleManager.timeSource
            val expectedDuration = 100.milliseconds
            val initialTimeMark = underTest.state.value.timeMark

            // First resume and update
            activityController.resume()
            shadowOf(Looper.getMainLooper()).idle()
            advanceUntilIdle()
            val beforeTimeMark = underTest.state.value.timeMark
            check(beforeTimeMark != initialTimeMark)
            activityController.pause()
            shadowOf(Looper.getMainLooper()).idle()
            advanceUntilIdle()
            timeSource += expectedDuration

            lifecycleManager.allowOneMoreCallToUpdate()
            activityController.resume()
            shadowOf(Looper.getMainLooper()).idle()
            advanceUntilIdle()

            val afterTimeMark = underTest.state.value.timeMark
            val actualDuration = afterTimeMark - beforeTimeMark
            assertThat(actualDuration).isEqualTo(expectedDuration)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun update_extendsState() =
        runTest(testDispatcher) {
            activityController.create().start()
            underTest = createSession(coroutineDispatcher = testDispatcher)

            activityController.resume() // Triggers update
            advanceUntilIdle()

            val stateExtender = underTest.stateExtenders.last() as FakeStateExtender
            assertThat(stateExtender.extended).isNotEmpty()
        }

    @Test
    fun pause_setsLifecycleToPaused() {
        activityController.create().start().resume()
        underTest = createSession()

        activityController.pause()

        val lifecycleManager = getLifecycleManager()
        assertThat(lifecycleManager.state).isEqualTo(FakeLifecycleManager.State.PAUSED)
    }

    @Test
    fun pause_setsRuntimeToPaused() {
        activityController.create().start().resume()
        underTest = createSession()

        activityController.pause()

        val renderingRuntime = getRenderingRuntime()
        assertThat(renderingRuntime.state).isEqualTo(FakeRenderingRuntime.State.PAUSED)
    }

    @Test
    fun destroy_initialized_setsLifecycleToStopped() {
        activityController.create() // Session is created here
        underTest = createSession()

        activityController.destroy() // Triggers session destroy

        val lifecycleManager = getLifecycleManager()
        assertThat(lifecycleManager.state).isEqualTo(FakeLifecycleManager.State.DESTROYED)
    }

    @Test
    fun destroy_resumed_setsLifecycleToDestroyed() {
        activityController.create().start().resume()
        underTest = createSession()

        activityController.destroy()

        val lifecycleManager = getLifecycleManager()
        assertThat(lifecycleManager.state).isEqualTo(FakeLifecycleManager.State.DESTROYED)
    }

    @Test
    fun destroy_setsRuntimeToDestroyed() {
        activityController.create().start().resume()
        underTest = createSession()

        activityController.destroy()

        val renderingRuntime = getRenderingRuntime()
        assertThat(renderingRuntime.state).isEqualTo(FakeRenderingRuntime.State.DESTROYED)
    }

    fun destroy_withMultiple_doesNotSetFinalActivity() {
        val activityController2 = Robolectric.buildActivity(ComponentActivity::class.java)
        val secondActivity = activityController2.get()

        val underTest = createSession()
        val secondSession =
            (Session.create(secondActivity!!, testDispatcher) as SessionCreateSuccess).session
        activityController.create().start().resume()
        activityController2.create().start().resume()

        // Destroy the session while the other session is still active.
        activityController.destroy()

        val lifecycleManager = getLifecycleManager()
        // This should not be stopped because there is still an active activity but it will update
        // to PAUSED.
        assertThat(lifecycleManager.state).isEqualTo(FakeLifecycleManager.State.PAUSED)

        // Destroy the second session to clean up the static activity map.
        activityController2.destroy()
    }

    @Test
    fun destroy_lastDestroyed_setFinalActivityTrue() {
        val activityController2 = Robolectric.buildActivity(ComponentActivity::class.java)
        val secondActivity = activityController2.get()
        underTest = createSession()
        val secondSession =
            (Session.create(secondActivity!!, testDispatcher) as SessionCreateSuccess).session
        activityController2.create().start().resume()
        activityController2.destroy()
        activityController.create().start().resume()

        // Destroy the session after the other session was destroyed.
        activityController.destroy()

        val lifecycleManager = getLifecycleManager()
        assertThat(lifecycleManager.state).isEqualTo(FakeLifecycleManager.State.DESTROYED)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun destroy_cancelsCoroutineScope() =
        runTest(testDispatcher) {
            activityController.create().start().resume()
            underTest = createSession(coroutineDispatcher = testDispatcher)
            val job = underTest.coroutineScope.launch { delay(12.hours) }

            activityController.destroy()
            advanceUntilIdle()

            assertThat(job.isCancelled).isTrue()
        }

    @Test
    fun destroy_activityDestroyedWithCustomLifecycleOwner_setsLifecycleToDestroyed() {
        activityController.create().start().resume()
        val lifecycleOwner =
            object : LifecycleOwner {
                override val lifecycle: Lifecycle
                    get() = LifecycleRegistry(this)
            }
        underTest = (Session.create(activity, lifecycleOwner) as SessionCreateSuccess).session

        activityController.destroy()

        val lifecycleManager = getLifecycleManager()
        assertThat(lifecycleManager.state).isEqualTo(FakeLifecycleManager.State.DESTROYED)
    }

    private fun createSession(coroutineDispatcher: CoroutineDispatcher = testDispatcher): Session {
        val result = Session.create(activity, coroutineDispatcher)
        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)
        return (result as SessionCreateSuccess).session
    }

    private fun getLifecycleManager(): FakeLifecycleManager {
        return underTest.runtimes.filterIsInstance<FakePerceptionRuntime>().first().lifecycleManager
    }

    private fun getSceneRuntime(): FakeSceneRuntime {
        return underTest.runtimes.filterIsInstance<FakeSceneRuntime>().first()
    }

    private fun getRenderingRuntime(): FakeRenderingRuntime {
        return underTest.runtimes.filterIsInstance<FakeRenderingRuntime>().first()
    }

    private companion object {
        private const val ARCORE_PACKAGE_NAME = "com.google.ar.core"
    }
}
