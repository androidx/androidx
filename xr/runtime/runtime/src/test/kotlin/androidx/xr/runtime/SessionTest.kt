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
import android.content.Context
import android.graphics.Bitmap
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.internal.ApkCheckAvailabilityErrorException
import androidx.xr.runtime.internal.ApkCheckAvailabilityInProgressException
import androidx.xr.runtime.internal.ApkNotInstalledException
import androidx.xr.runtime.internal.JxrRuntime
import androidx.xr.runtime.internal.UnsupportedDeviceException
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.ContinuationInterceptor
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

@RunWith(AndroidJUnit4::class)
class SessionTest {
    private lateinit var underTest: Session
    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var application: Context
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()

        val shadowApplication = shadowOf(activity.application)
        StubPerceptionRuntime.TestPermissions.forEach { permission ->
            shadowApplication.grantPermissions(permission)
        }

        StubPerceptionRuntimeFactory.hasCreatePermission = true
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

        val session = (result as SessionCreateSuccess).session
        assertThat(session).isNotNull()
        assertThat(session.lifecycleOwner).isEqualTo(activity)
    }

    @Test
    fun create_setsLifecycleToInitialized() {
        activityController.create()

        underTest = createSession()

        val stubRuntime = getStubRuntime()
        assertThat(stubRuntime.state).isEqualTo(StubPerceptionRuntime.State.INITIALIZED)
    }

    @Test
    fun create_initializesStateExtender() {
        activityController.create()

        underTest = createSession()

        val stateExtender = underTest.stateExtenders.last() as StubStateExtender
        assertThat(stateExtender.isInitialized).isTrue()
    }

    @Test
    fun create_initializesSessionConnector() {
        activityController.create()

        underTest = createSession()

        val sessionConnector = getStubSessionConnector()
        assertThat(sessionConnector.isInitialized).isTrue()
        assertThat(sessionConnector.initializedRuntimes).isEqualTo(underTest.runtimes)
    }

    @Test
    fun create_withApplicationContext_returnsSuccessResultWithNonNullSession() {
        activityController.create()

        val context = activity.applicationContext
        val result = Session.create(context, activity, testDispatcher)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)
        assertThat((result as SessionCreateSuccess).session).isNotNull()
    }

    @Test
    fun create_withActivityAndLifecycleOwner_usesProvidedLifecycleOwner() {
        activityController.create()
        val customLifecycleOwner =
            object : LifecycleOwner {
                override val lifecycle = LifecycleRegistry(this)
            }

        val result = Session.create(activity, lifecycleOwner = customLifecycleOwner)

        val session = (result as SessionCreateSuccess).session
        assertThat(session.lifecycleOwner).isEqualTo(customLifecycleOwner)
    }

    @Test
    fun create_withActivityAndCoroutineContext_returnsSuccessResultWithNonNullSession() {
        activityController.create()

        val result = Session.create(activity, coroutineContext = testDispatcher)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)
        val session = (result as SessionCreateSuccess).session
        assertThat(session).isNotNull()
        assertThat(session.coroutineScope.coroutineContext[ContinuationInterceptor])
            .isEqualTo(testDispatcher)
    }

    @Test
    @Suppress("DEPRECATION")
    fun create_withUnscaledGravityAlignedActivitySpace_returnsSuccessResultWithNonNullSession() {
        activityController.create()

        val result = Session.create(activity, unscaledGravityAlignedActivitySpace = false)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)
        assertThat((result as SessionCreateSuccess).session).isNotNull()
    }

    @Test
    fun create_permissionNotGranted_throwsSecurityException() {
        val shadowApplication = shadowOf(activity.application)
        shadowApplication.denyPermissions(Manifest.permission.CAMERA)
        StubPerceptionRuntimeFactory.hasCreatePermission = false

        activityController.create()

        assertFailsWith<SecurityException> { Session.create(activity) }
    }

    @Test
    fun create_arcoreNotInstalledException_returnsApkRequiredResult() {
        StubPerceptionRuntimeFactory.lifecycleCreateException =
            ApkNotInstalledException(ARCORE_PACKAGE_NAME)
        activityController.create()

        val result = Session.create(activity)

        assertThat(result).isInstanceOf(SessionCreateApkRequired::class.java)
        assertThat((result as SessionCreateApkRequired).requiredApk).isEqualTo(ARCORE_PACKAGE_NAME)
    }

    @Test
    fun create_arcoreUnsupportedDeviceException_returnsUnsupportedDeviceResult() {
        StubPerceptionRuntimeFactory.lifecycleCreateException = UnsupportedDeviceException()
        activityController.create()

        val result = Session.create(activity)

        assertThat(result).isInstanceOf(SessionCreateUnsupportedDevice::class.java)
    }

    @Test
    fun create_arcoreCheckAvailabilityInProgressException_returnsApkRequiredResult() {
        StubPerceptionRuntimeFactory.lifecycleCreateException =
            ApkCheckAvailabilityInProgressException(ARCORE_PACKAGE_NAME)
        activityController.create()

        val result = Session.create(activity)

        assertThat(result).isInstanceOf(SessionCreateApkRequired::class.java)
        assertThat((result as SessionCreateApkRequired).requiredApk).isEqualTo(ARCORE_PACKAGE_NAME)
    }

    @Test
    fun create_arcoreCheckAvailabilityErrorException_returnsApkRequiredResult() {
        StubPerceptionRuntimeFactory.lifecycleCreateException =
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
            .isEqualTo("Cannot create a new session on a destroyed lifecycleOwner.")
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

        val stubRuntime = getStubRuntime()
        check(
            stubRuntime.config ==
                Config(
                    planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
                    // Needs to contain at least one AugmentedObjectCategory to enable
                    augmentedObjectCategories = setOf(AugmentedObjectCategory.MOUSE),
                    handTracking = HandTrackingMode.BOTH,
                    deviceTracking = DeviceTrackingMode.SPATIAL_LAST_KNOWN,
                    depthEstimation = DepthEstimationMode.SMOOTH_AND_RAW,
                    anchorPersistence = AnchorPersistenceMode.LOCAL,
                )
        )
        val newConfig =
            Config(
                planeTracking = PlaneTrackingMode.DISABLED,
                augmentedObjectCategories = setOf<AugmentedObjectCategory>(),
                handTracking = HandTrackingMode.DISABLED,
                deviceTracking = DeviceTrackingMode.DISABLED,
                depthEstimation = DepthEstimationMode.DISABLED,
                anchorPersistence = AnchorPersistenceMode.DISABLED,
            )

        val result = underTest.configure(newConfig)

        assertThat(result).isInstanceOf(SessionConfigureSuccess::class.java)
        assertThat(stubRuntime.config).isEqualTo(newConfig)
    }

    @Test
    fun configure_permissionNotGranted_throwsSecurityException() {
        activityController.create().start().resume()
        underTest = createSession()
        val stubRuntime = getStubRuntime()

        val currentConfig = stubRuntime.config
        check(currentConfig.depthEstimation == DepthEstimationMode.SMOOTH_AND_RAW)
        stubRuntime.hasMissingPermission = true

        assertFailsWith<SecurityException> {
            underTest.configure(
                underTest.config.copy(
                    depthEstimation = DepthEstimationMode.DISABLED,
                    faceTracking = FaceTrackingMode.DISABLED,
                )
            )
        }
        assertThat(stubRuntime.config).isEqualTo(currentConfig)
    }

    @Test
    fun configure_unsupportedMode_throwsUnsupportedOperationException() {
        activityController.create().start().resume()
        underTest = createSession()

        val stubRuntime = getStubRuntime()

        val currentConfig = underTest.config
        stubRuntime.shouldSupportPlaneTracking = false

        assertFailsWith<UnsupportedOperationException> {
            underTest.configure(
                currentConfig.copy(planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
            )
        }
        assertThat(underTest.config).isEqualTo(currentConfig)
        stubRuntime.shouldSupportPlaneTracking = true
    }

    @Test
    fun configure_unsupportedImageMode_returnsConfigurationNotSupportedResult() {
        activityController.create().start().resume()
        underTest = createSession()
        val stubRuntime = getStubRuntime()

        val currentConfig = underTest.config
        stubRuntime.shouldSupportImageTracking = false

        assertFailsWith<UnsupportedOperationException> {
            underTest.configure(
                currentConfig.copy(
                    augmentedImageDatabase =
                        AugmentedImageDatabase().apply {
                            addAugmentedImageDatabaseEntry(
                                mode = AugmentedImageDatabaseEntryMode.DYNAMIC,
                                bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
                            )
                        }
                )
            )
        }
        assertThat(underTest.config).isEqualTo(currentConfig)
        stubRuntime.shouldSupportImageTracking = true
    }

    @Test
    fun resume_returnsSuccessAndSetsLifecycleToResumed() {
        activityController.create().start()
        underTest = createSession()

        activityController.resume()

        val stubRuntime = getStubRuntime()

        assertThat(stubRuntime.state).isEqualTo(StubPerceptionRuntime.State.RESUMED)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun update_emitsUpdatedState() =
        runTest(testDispatcher) {
            activityController.create().start()
            underTest = createSession(coroutineDispatcher = testDispatcher)
            val stubRuntime = getStubRuntime()

            val timeSource = stubRuntime.timeSource
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

            stubRuntime.allowOneMoreCallToUpdate()
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

            val stateExtender = underTest.stateExtenders.last() as StubStateExtender
            assertThat(stateExtender.extended).isNotEmpty()
        }

    @Test
    fun pause_setsLifecycleToPaused() {
        activityController.create().start().resume()
        underTest = createSession()

        activityController.pause()

        val stubRuntime = getStubRuntime()
        assertThat(stubRuntime.state).isEqualTo(StubPerceptionRuntime.State.PAUSED)
    }

    @Test
    fun destroy_initialized_setsLifecycleToStopped() {
        activityController.create() // Session is created here
        underTest = createSession()

        activityController.destroy() // Triggers session destroy

        val stubRuntime = getStubRuntime()
        assertThat(stubRuntime.state).isEqualTo(StubPerceptionRuntime.State.DESTROYED)
    }

    @Test
    fun destroy_resumed_setsLifecycleToDestroyed() {
        activityController.create().start().resume()
        underTest = createSession()

        activityController.destroy()

        val stubRuntime = getStubRuntime()
        assertThat(stubRuntime.state).isEqualTo(StubPerceptionRuntime.State.DESTROYED)
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

        val stubRuntime = getStubRuntime()
        // This should not be stopped because there is still an active activity but it will update
        // to PAUSED.
        assertThat(stubRuntime.state).isEqualTo(StubPerceptionRuntime.State.PAUSED)

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

        val stubRuntime = getStubRuntime()
        assertThat(stubRuntime.state).isEqualTo(StubPerceptionRuntime.State.DESTROYED)
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
        underTest =
            (Session.create(activity, lifecycleOwner = lifecycleOwner) as SessionCreateSuccess)
                .session

        activityController.destroy()

        val stubRuntime = getStubRuntime()
        assertThat(stubRuntime.state).isEqualTo(StubPerceptionRuntime.State.DESTROYED)
    }

    @Test
    fun destroy_closesSessionConnector() {
        activityController.create()
        underTest = createSession()
        val sessionConnector = getStubSessionConnector()

        activityController.destroy()

        assertThat(sessionConnector.isClosed).isTrue()
    }

    @Test
    fun destroy_callsCleanupsInReverseOrder() {
        val callOrder = mutableListOf<String>()
        val runtime1 =
            object : JxrRuntime {
                override fun destroy() {
                    callOrder.add("runtime1")
                }
            }
        val runtime2 =
            object : JxrRuntime {
                override fun destroy() {
                    callOrder.add("runtime2")
                }
            }
        val connector1 =
            object : SessionConnector {
                override fun initialize(runtimes: List<JxrRuntime>) {}

                override fun close() {
                    callOrder.add("connector1")
                }
            }
        val extender1 =
            object : StateExtender {
                override fun initialize(runtimes: List<JxrRuntime>) {}

                override suspend fun extend(coreState: CoreState) {}
            }
        val session =
            Session(
                activity,
                listOf(extender1),
                listOf(connector1),
                listOf(runtime1, runtime2),
                kotlinx.coroutines.test.TestScope(testDispatcher),
                activity,
            )
        activity.lifecycle.addObserver(session.lifecycleObserver)

        activityController.create().start().resume().pause().stop().destroy()

        assertThat(callOrder).containsExactly("connector1", "runtime2", "runtime1").inOrder()
    }

    private fun createSession(coroutineDispatcher: CoroutineDispatcher = testDispatcher): Session {
        val result = Session.create(activity, coroutineDispatcher)
        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)
        return (result as SessionCreateSuccess).session
    }

    private fun getStubRuntime(): StubPerceptionRuntime {
        return underTest.runtimes.filterIsInstance<StubPerceptionRuntime>().first()
    }

    private fun getStubSessionConnector(): androidx.xr.runtime.StubSessionConnector {
        return underTest.sessionConnectors
            .filterIsInstance<androidx.xr.runtime.StubSessionConnector>()
            .first()
    }

    private companion object {
        private const val ARCORE_PACKAGE_NAME = "com.google.ar.core"
    }
}
