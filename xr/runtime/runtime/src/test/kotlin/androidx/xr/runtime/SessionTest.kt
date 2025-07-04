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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.internal.ApkCheckAvailabilityErrorException
import androidx.xr.runtime.internal.ApkCheckAvailabilityInProgressException
import androidx.xr.runtime.internal.ApkNotInstalledException
import androidx.xr.runtime.internal.UnsupportedDeviceException
import androidx.xr.runtime.testing.FakeJxrPlatformAdapter
import androidx.xr.runtime.testing.FakeLifecycleManager
import androidx.xr.runtime.testing.FakeRuntimeFactory
import androidx.xr.runtime.testing.FakeStateExtender
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
import org.robolectric.annotation.LooperMode

@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.PAUSED)
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

        FakeRuntimeFactory.hasCreatePermission = true
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

        val lifecycleManager = underTest.runtime.lifecycleManager as FakeLifecycleManager
        assertThat(lifecycleManager.state).isEqualTo(FakeLifecycleManager.State.INITIALIZED)
    }

    @Test
    fun create_initializesStateExtender() {
        activityController.create()

        underTest = createSession()

        val stateExtender = underTest.stateExtenders.first() as FakeStateExtender
        assertThat(stateExtender.isInitialized).isTrue()
    }

    @Test
    fun create_initializesPlatformAdapter() {
        activityController.create()

        underTest = createSession()

        val platformAdapter = underTest.platformAdapter as FakeJxrPlatformAdapter
        assertThat(platformAdapter).isNotNull()
        assertThat(platformAdapter.state.name).isEqualTo("CREATED")
    }

    @Test
    fun create_permissionException_returnsPermissionsNotGrantedResult() {
        val shadowApplication = shadowOf(activity.application)
        shadowApplication.denyPermissions(Manifest.permission.CAMERA)
        FakeRuntimeFactory.hasCreatePermission = false

        activityController.create()

        val result = Session.create(activity)
        assertThat(result).isInstanceOf(SessionCreatePermissionsNotGranted::class.java)
    }

    @Test
    fun create_arcoreNotInstalledException_returnsApkRequiredResult() {
        FakeRuntimeFactory.lifecycleCreateException = ApkNotInstalledException(ARCORE_PACKAGE_NAME)
        activityController.create()

        val result = Session.create(activity)

        assertThat(result).isInstanceOf(SessionCreateApkRequired::class.java)
        assertThat((result as SessionCreateApkRequired).requiredApk).isEqualTo(ARCORE_PACKAGE_NAME)
    }

    @Test
    fun create_arcoreUnsupportedDeviceException_returnsUnsupportedDeviceResult() {
        FakeRuntimeFactory.lifecycleCreateException = UnsupportedDeviceException()
        activityController.create()

        val result = Session.create(activity)

        assertThat(result).isInstanceOf(SessionCreateUnsupportedDevice::class.java)
    }

    @Test
    fun create_arcoreCheckAvailabilityInProgressException_returnsApkRequiredResult() {
        FakeRuntimeFactory.lifecycleCreateException =
            ApkCheckAvailabilityInProgressException(ARCORE_PACKAGE_NAME)
        activityController.create()

        val result = Session.create(activity)

        assertThat(result).isInstanceOf(SessionCreateApkRequired::class.java)
        assertThat((result as SessionCreateApkRequired).requiredApk).isEqualTo(ARCORE_PACKAGE_NAME)
    }

    @Test
    fun create_arcoreCheckAvailabilityErrorException_returnsApkRequiredResult() {
        FakeRuntimeFactory.lifecycleCreateException =
            ApkCheckAvailabilityErrorException(ARCORE_PACKAGE_NAME)
        activityController.create()

        val result = Session.create(activity)

        assertThat(result).isInstanceOf(SessionCreateApkRequired::class.java)
        assertThat((result as SessionCreateApkRequired).requiredApk).isEqualTo(ARCORE_PACKAGE_NAME)
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
        check(
            underTest.config ==
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
        assertThat(underTest.config).isEqualTo(newConfig)
    }

    @Test
    fun configure_permissionNotGranted_returnsPermissionNotGrantedResult() {
        activityController.create().start().resume()
        underTest = createSession()
        val lifecycleManager = underTest.runtime.lifecycleManager as FakeLifecycleManager
        val currentConfig = underTest.config
        check(currentConfig.depthEstimation == Config.DepthEstimationMode.SMOOTH_AND_RAW)
        lifecycleManager.hasMissingPermission = true

        val result =
            underTest.configure(
                underTest.config.copy(
                    depthEstimation = Config.DepthEstimationMode.DISABLED,
                    faceTracking = Config.FaceTrackingMode.DISABLED,
                )
            )

        assertThat(result).isInstanceOf(SessionConfigurePermissionsNotGranted::class.java)
        assertThat(underTest.config).isEqualTo(currentConfig)
    }

    @Test
    fun configure_unsupportedMode_returnsConfigurationNotSupportedResult() {
        activityController.create().start().resume()
        underTest = createSession()
        val lifecycleManager = underTest.runtime.lifecycleManager as FakeLifecycleManager
        val currentConfig = underTest.config
        lifecycleManager.shouldSupportPlaneTracking = false

        val result =
            underTest.configure(
                currentConfig.copy(planeTracking = Config.PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
            )

        assertThat(result).isInstanceOf(SessionConfigureConfigurationNotSupported::class.java)
        assertThat(underTest.config).isEqualTo(currentConfig)
        lifecycleManager.shouldSupportPlaneTracking = true
    }

    @Test
    fun resume_returnsSuccessAndSetsLifecycleToResumed() {
        activityController.create().start()
        underTest = createSession()

        activityController.resume()

        val lifecycleManager = underTest.runtime.lifecycleManager as FakeLifecycleManager
        assertThat(lifecycleManager.state).isEqualTo(FakeLifecycleManager.State.RESUMED)
    }

    @Test
    fun resume_returnsSuccessAndSetsPlatformAdapterToResumed() {
        activityController.create().start()
        underTest = createSession()

        activityController.resume()

        assertThat((underTest.platformAdapter as FakeJxrPlatformAdapter).state)
            .isEqualTo(FakeJxrPlatformAdapter.State.STARTED) // Corresponds to resumed
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun update_emitsUpdatedState() =
        runTest(testDispatcher) {
            activityController.create().start()
            underTest = createSession(coroutineDispatcher = testDispatcher)
            val lifecycleManager = underTest.runtime.lifecycleManager as FakeLifecycleManager
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

            val stateExtender = underTest.stateExtenders.first() as FakeStateExtender
            assertThat(stateExtender.extended).isNotEmpty()
        }

    @Test
    fun pause_setsLifecycleToPaused() {
        activityController.create().start().resume()
        underTest = createSession()

        activityController.pause()

        val lifecycleManager = underTest.runtime.lifecycleManager as FakeLifecycleManager
        assertThat(lifecycleManager.state).isEqualTo(FakeLifecycleManager.State.PAUSED)
    }

    @Test
    fun pause_setsPlatformAdapterToPaused() {
        activityController.create().start().resume()
        underTest = createSession()

        activityController.pause()

        val platformAdapter = underTest.platformAdapter as FakeJxrPlatformAdapter
        assertThat(platformAdapter.state).isEqualTo(FakeJxrPlatformAdapter.State.PAUSED)
    }

    @Test
    fun destroy_initialized_setsLifecycleToStopped() {
        activityController.create() // Session is created here
        underTest = createSession()

        activityController.destroy() // Triggers session destroy

        val lifecycleManager = underTest.runtime.lifecycleManager as FakeLifecycleManager
        assertThat(lifecycleManager.state).isEqualTo(FakeLifecycleManager.State.DESTROYED)
    }

    @Test
    fun destroy_resumed_setsLifecycleToStopped() {
        activityController.create().start().resume()
        underTest = createSession()

        activityController.destroy()

        val lifecycleManager = underTest.runtime.lifecycleManager as FakeLifecycleManager
        assertThat(lifecycleManager.state).isEqualTo(FakeLifecycleManager.State.DESTROYED)
    }

    @Test
    fun destroy_setsPlatformAdapterToStopped() {
        activityController.create().start().resume()
        underTest = createSession()

        activityController.destroy()

        val platformAdapter = underTest.platformAdapter as FakeJxrPlatformAdapter
        assertThat(platformAdapter.state).isEqualTo(FakeJxrPlatformAdapter.State.DESTROYED)
    }

    fun destroy_withMultiple_doesNotSetFinalActivity() {
        val activityController2 = Robolectric.buildActivity(ComponentActivity::class.java)
        val secondActivity = activityController2.get()

        val underTest = (Session.create(activity) as SessionCreateSuccess).session
        val secondSession = (Session.create(secondActivity!!) as SessionCreateSuccess).session
        activityController.create().start().resume()
        activityController2.create().start().resume()

        // Destroy the session while the other session is still active.
        activityController.destroy()

        val lifecycleManager = underTest.runtime.lifecycleManager as FakeLifecycleManager
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
        val underTest = (Session.create(activity) as SessionCreateSuccess).session
        val secondSession = (Session.create(secondActivity!!) as SessionCreateSuccess).session
        activityController2.create().start().resume()
        activityController2.destroy()
        activityController.create().start().resume()

        // Destroy the session after the other session was destroyed.
        activityController.destroy()

        val lifecycleManager = underTest.runtime.lifecycleManager as FakeLifecycleManager
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

    private fun createSession(coroutineDispatcher: CoroutineDispatcher = testDispatcher): Session {
        val result = Session.create(activity, coroutineDispatcher)
        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)
        return (result as SessionCreateSuccess).session
    }

    private companion object {
        private const val ARCORE_PACKAGE_NAME = "com.google.ar.core"
    }
}
