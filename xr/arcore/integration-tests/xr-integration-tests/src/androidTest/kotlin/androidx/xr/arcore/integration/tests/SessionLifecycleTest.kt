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

package androidx.xr.arcore.integration.tests

import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import androidx.xr.runtime.AnchorPersistenceMode
import androidx.xr.runtime.Config
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.EyeTrackingMode
import androidx.xr.runtime.FaceTrackingMode
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.HandTrackingMode
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.SessionCreateApkRequired
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.filterSupportedPermissions
import com.google.common.truth.Truth.assertThat
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated integration and stress tests for JXR ARCore [Session] lifecycle, dynamic
 * reconfiguration, and resource management on real XR devices.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SessionLifecycleTest {

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            *filterSupportedPermissions(
                "android.permission.SCENE_UNDERSTANDING_COARSE",
                "android.permission.SCENE_UNDERSTANDING_FINE",
                "android.permission.HAND_TRACKING",
                "android.permission.HEAD_TRACKING",
                "android.permission.FACE_TRACKING",
                "android.permission.EYE_TRACKING_COARSE",
                "android.permission.EYE_TRACKING_FINE",
                "android.permission.INTERNET",
                "android.permission.ACCESS_COARSE_LOCATION",
                "android.permission.ACCESS_FINE_LOCATION",
            )
        )

    @Test
    @XrDeviceTest
    fun createSession_withDefaultConfig_returnsSuccess() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            lateinit var activity: ComponentActivity
            scenario.onActivity { activity = it }

            val result = runBlocking { Session.create(context = activity) }
            assumeFalse(
                "ARCore APK is required but not installed or up-to-date on this device.",
                result is SessionCreateApkRequired,
            )
            assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

            val session = (result as SessionCreateSuccess).session
            assertThat(session).isNotNull()
            assertThat(session.activity).isEqualTo(activity)
            assertThat(session.lifecycleOwner).isEqualTo(activity)
            val defaultConfig = Config.Builder().build()
            assertThat(session.config).isEqualTo(defaultConfig)

            val configResult = session.configure(defaultConfig)
            assertThat(configResult).isInstanceOf(SessionConfigureSuccess::class.java)
        }
    }

    @Test
    @XrDeviceTest
    fun createSession_sameContext_returnsCachedInstance() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            lateinit var activity: ComponentActivity
            scenario.onActivity { activity = it }

            val firstResult = runBlocking { Session.create(context = activity) }
            assumeFalse(
                "ARCore APK is required but not installed or up-to-date on this device.",
                firstResult is SessionCreateApkRequired,
            )
            assertThat(firstResult).isInstanceOf(SessionCreateSuccess::class.java)
            val firstSession = (firstResult as SessionCreateSuccess).session

            val secondResult = runBlocking { Session.create(context = activity) }
            assertThat(secondResult).isInstanceOf(SessionCreateSuccess::class.java)
            val secondSession = (secondResult as SessionCreateSuccess).session

            assertThat(secondSession).isSameInstanceAs(firstSession)
        }
    }

    /**
     * Stress-tests dynamic reconfigurations across combinatorial and deterministic pseudo-random
     * configurations on live hardware without crashing or leaving session in invalid state.
     */
    @Test
    @XrDeviceTest
    fun configure_combinatorialAndDeterministicRandomConfigs_maintainsValidState() {
        val configs =
            generateCombinatorialConfigs() +
                generateDeterministicRandomConfigs(seed = 42, count = 20)

        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            lateinit var activity: ComponentActivity
            scenario.onActivity { activity = it }

            val result = runBlocking { Session.create(context = activity) }
            assumeFalse(
                "ARCore APK is required but not installed or up-to-date on this device.",
                result is SessionCreateApkRequired,
            )
            assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)
            val session = (result as SessionCreateSuccess).session

            for (config in configs) {
                val previousConfig = session.config
                val outcome = runCatching { session.configure(config) }

                outcome.fold(
                    onSuccess = { configResult ->
                        if (configResult is SessionConfigureSuccess) {
                            assertThat(session.config).isEqualTo(config)
                        } else {
                            // On unsupported modes or configuration failure, session state must
                            // remain consistent
                            assertThat(session.config).isEqualTo(previousConfig)
                        }
                    },
                    onFailure = { throwable ->
                        // Only documented runtime exceptions should be thrown on unsupported
                        // hardware combinations
                        assertThat(throwable)
                            .isInstanceOf(UnsupportedOperationException::class.java)
                        assertThat(session.config).isEqualTo(previousConfig)
                    },
                )
            }
        }
    }

    /**
     * Rapidly cycles ON_PAUSE and ON_RESUME events to stress-test asynchronous native camera / XR
     * pipeline synchronization and guard against race conditions.
     */
    @Test
    @XrDeviceTest
    fun sessionLifecycle_rapidPauseAndResumeCycling_maintainsHealthySession() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            lateinit var activity: ComponentActivity
            scenario.onActivity { activity = it }

            lateinit var registry: LifecycleRegistry
            val lifecycleOwner =
                object : LifecycleOwner {
                    override val lifecycle: Lifecycle
                        get() = registry
                }
            scenario.onActivity {
                registry =
                    LifecycleRegistry(lifecycleOwner).apply {
                        currentState = Lifecycle.State.CREATED
                        currentState = Lifecycle.State.RESUMED
                    }
            }

            val result = runBlocking {
                Session.create(context = activity, lifecycleOwner = lifecycleOwner)
            }
            assumeFalse(
                "ARCore APK is required but not installed or up-to-date on this device.",
                result is SessionCreateApkRequired,
            )
            assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)
            val session = (result as SessionCreateSuccess).session

            // Rapidly cycle pause/resume across 20 iterations on the UI thread
            repeat(20) {
                scenario.onActivity {
                    registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                    registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
                }
            }

            // Verify session remains active and configurable after thrashing
            val configResult = session.configure(session.config)
            assertThat(configResult).isInstanceOf(SessionConfigureSuccess::class.java)
            assertThat(session.state.value).isNotNull()
        }
    }

    @Test
    @XrDeviceTest
    fun sessionLifecycle_activityRecreate_reinitializesSession() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            lateinit var initialActivity: ComponentActivity
            scenario.onActivity { initialActivity = it }

            val result = runBlocking { Session.create(context = initialActivity) }
            assumeFalse(
                "ARCore APK is required but not installed or up-to-date on this device.",
                result is SessionCreateApkRequired,
            )
            assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)
            val initialSession = (result as SessionCreateSuccess).session

            scenario.recreate()

            lateinit var newActivity: ComponentActivity
            scenario.onActivity { newActivity = it }

            val newResult = runBlocking { Session.create(context = newActivity) }
            assumeFalse(
                "ARCore APK is required but not installed or up-to-date on this device.",
                newResult is SessionCreateApkRequired,
            )
            assertThat(newResult).isInstanceOf(SessionCreateSuccess::class.java)
            val newSession = (newResult as SessionCreateSuccess).session

            assertThat(newSession).isNotNull()
            assertThat(newSession.activity).isEqualTo(newActivity)
            assertThat(newSession).isNotSameInstanceAs(initialSession)

            val configResult = newSession.configure(Config.Builder().build())
            assertThat(configResult).isInstanceOf(SessionConfigureSuccess::class.java)
        }
    }

    /**
     * Validates that destroying and recreating sessions across multiple activity recreations
     * properly releases native resources without leaking handles or failing future initializations.
     */
    @Test
    @XrDeviceTest
    fun sessionLifecycle_repeatedRecreationChurn_succeedsWithoutNativeLeaks() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            repeat(3) {
                lateinit var currentActivity: ComponentActivity
                scenario.onActivity { currentActivity = it }

                val result = runBlocking { Session.create(context = currentActivity) }
                assumeFalse(
                    "ARCore APK is required but not installed or up-to-date on this device.",
                    result is SessionCreateApkRequired,
                )
                assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

                scenario.recreate()
            }
        }
    }

    @Test
    @XrDeviceTest
    fun sessionLifecycle_destroyedLifecycleOwner_cannotCreateSession() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            lateinit var activity: ComponentActivity
            scenario.onActivity { activity = it }

            lateinit var registry: LifecycleRegistry
            val lifecycleOwner =
                object : LifecycleOwner {
                    override val lifecycle: Lifecycle
                        get() = registry
                }
            scenario.onActivity {
                registry =
                    LifecycleRegistry(lifecycleOwner).apply {
                        currentState = Lifecycle.State.CREATED
                        currentState = Lifecycle.State.DESTROYED
                    }
            }

            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    Session.create(context = activity, lifecycleOwner = lifecycleOwner)
                }
            }
        }
    }

    @Test
    @XrDeviceTest
    fun sessionLifecycle_destroyedSession_cannotConfigure() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            lateinit var activity: ComponentActivity
            scenario.onActivity { activity = it }

            lateinit var registry: LifecycleRegistry
            val lifecycleOwner =
                object : LifecycleOwner {
                    override val lifecycle: Lifecycle
                        get() = registry
                }
            scenario.onActivity {
                registry =
                    LifecycleRegistry(lifecycleOwner).apply {
                        currentState = Lifecycle.State.CREATED
                        currentState = Lifecycle.State.RESUMED
                    }
            }

            val result = runBlocking {
                Session.create(context = activity, lifecycleOwner = lifecycleOwner)
            }
            assumeFalse(
                "ARCore APK is required but not installed or up-to-date on this device.",
                result is SessionCreateApkRequired,
            )
            assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)
            val session = (result as SessionCreateSuccess).session

            scenario.onActivity { registry.currentState = Lifecycle.State.DESTROYED }

            assertThrows(IllegalStateException::class.java) {
                session.configure(Config.Builder().build())
            }
        }
    }

    @Test
    @XrDeviceTest
    fun session_stateFlow_emitsValidState() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            lateinit var activity: ComponentActivity
            scenario.onActivity { activity = it }

            val result = runBlocking { Session.create(context = activity) }
            assumeFalse(
                "ARCore APK is required but not installed or up-to-date on this device.",
                result is SessionCreateApkRequired,
            )
            assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)
            val session = (result as SessionCreateSuccess).session

            val initialState = session.state.value
            assertThat(initialState).isNotNull()
            assertThat(initialState.timeMark).isNotNull()
        }
    }

    // --- Helpers for deterministic config generation ---

    private fun generateCombinatorialConfigs(): List<Config> {
        val planeModes =
            listOf(PlaneTrackingMode.DISABLED, PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
        val deviceModes = listOf(DeviceTrackingMode.DISABLED, DeviceTrackingMode.SPATIAL)
        val depthModes = listOf(DepthEstimationMode.DISABLED, DepthEstimationMode.SMOOTH_ONLY)
        val handModes = listOf(HandTrackingMode.DISABLED, HandTrackingMode.BOTH)

        val configs = mutableListOf<Config>()
        for (plane in planeModes) {
            for (device in deviceModes) {
                for (depth in depthModes) {
                    for (hand in handModes) {
                        configs.add(
                            Config.Builder()
                                .setPlaneTracking(plane)
                                .setDeviceTracking(device)
                                .setDepthEstimation(depth)
                                .setHandTracking(hand)
                                .build()
                        )
                    }
                }
            }
        }
        return configs
    }

    private fun generateDeterministicRandomConfigs(seed: Long, count: Int): List<Config> {
        val rng = Random(seed)
        val planes = listOf(PlaneTrackingMode.DISABLED, PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
        val devices = listOf(DeviceTrackingMode.DISABLED, DeviceTrackingMode.SPATIAL)
        val depths =
            listOf(
                DepthEstimationMode.DISABLED,
                DepthEstimationMode.RAW_ONLY,
                DepthEstimationMode.SMOOTH_ONLY,
                DepthEstimationMode.SMOOTH_AND_RAW,
            )
        val hands = listOf(HandTrackingMode.DISABLED, HandTrackingMode.BOTH)
        val eyes =
            listOf(
                EyeTrackingMode.DISABLED,
                EyeTrackingMode.COARSE_TRACKING,
                EyeTrackingMode.FINE_TRACKING,
            )
        val faces = listOf(FaceTrackingMode.DISABLED, FaceTrackingMode.BLEND_SHAPES)
        val geospatials = listOf(GeospatialMode.DISABLED, GeospatialMode.SPATIAL)
        val persistence = listOf(AnchorPersistenceMode.DISABLED, AnchorPersistenceMode.LOCAL)

        return List(count) {
            Config.Builder()
                .setPlaneTracking(planes[rng.nextInt(planes.size)])
                .setDeviceTracking(devices[rng.nextInt(devices.size)])
                .setDepthEstimation(depths[rng.nextInt(depths.size)])
                .setHandTracking(hands[rng.nextInt(hands.size)])
                .setEyeTracking(eyes[rng.nextInt(eyes.size)])
                .setFaceTracking(faces[rng.nextInt(faces.size)])
                .setGeospatial(geospatials[rng.nextInt(geospatials.size)])
                .setAnchorPersistence(persistence[rng.nextInt(persistence.size)])
                .build()
        }
    }
}
