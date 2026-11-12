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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.xr.arcore.Plane
import androidx.xr.arcore.TrackingState
import androidx.xr.arcore.perceptionState
import androidx.xr.runtime.AnchorPersistenceMode
import androidx.xr.runtime.Config
import androidx.xr.runtime.CoreState
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.EyeTrackingMode
import androidx.xr.runtime.FaceTrackingMode
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.HandTrackingMode
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionConfigureResult
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.SessionCreateApkRequired
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.Pose
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.filterSupportedPermissions
import androidx.xr.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.test.assertNotNull
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
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

    @Test
    @XrDeviceTest
    fun sessionLifecycle_rapidPauseAndResumeCycling_maintainsHealthySession() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                val handle = createManualLifecycle(scenario, Lifecycle.State.RESUMED)
                val session =
                    createConfiguredDefaultSession(scenario, lifecycleOwner = handle.owner)

                repeat(RAPID_LIFECYCLE_CYCLES) {
                    scenario.onActivity {
                        handle.registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                        handle.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
                    }
                }
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                val postCycleBaseline = awaitRuntimeCoreState(session)
                val configResult = session.configure(session.config)
                val advancingState = awaitCoreStateAfter(session, postCycleBaseline.timeMark)

                assertThat(configResult).isInstanceOf(SessionConfigureSuccess::class.java)
                assertNotNull(
                    advancingState,
                    "Expected advancing CoreState after rapid pause/resume cycles.",
                )
            }
        }
    }

    @Test
    @XrDeviceTest
    fun sessionLifecycle_activityPausedToStartedAndResumed_haltsAndResumesStateUpdates() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                val session = createConfiguredDefaultSession(scenario)

                val outcome =
                    observePauseAndResumeLifecycle(scenario, session, Lifecycle.State.STARTED)

                assertThat(outcome.emissionWhilePaused).isNull()
                assertNotNull(
                    outcome.stateAfterResume,
                    "Expected CoreState updates to resume after ON_RESUME.",
                )
                assertThat(outcome.configureAfterResume)
                    .isInstanceOf(SessionConfigureSuccess::class.java)
            }
        }
    }

    @Test
    @XrDeviceTest
    fun sessionLifecycle_activityStoppedToCreatedAndResumed_haltsAndResumesStateUpdates() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                val session = createConfiguredDefaultSession(scenario)

                val outcome =
                    observePauseAndResumeLifecycle(scenario, session, Lifecycle.State.CREATED)

                assertThat(outcome.emissionWhilePaused).isNull()
                assertNotNull(
                    outcome.stateAfterResume,
                    "Expected CoreState updates to resume after ON_RESUME.",
                )
                assertThat(outcome.configureAfterResume)
                    .isInstanceOf(SessionConfigureSuccess::class.java)
            }
        }
    }

    @Test
    @XrDeviceTest
    fun sessionLifecycle_planeTrackingAcrossPauseAndResume_maintainsPlaneContinuity() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                val session = createConfiguredPlaneTrackingSession(scenario)
                val prePauseSnapshot = awaitTrackingPlanesAfter(session)
                assumeTrue(
                    "Skipping test: No tracking planes detected before pause within timeout.",
                    prePauseSnapshot != null,
                )
                val recordedSnapshot = checkNotNull(prePauseSnapshot)

                val postResumeRecovery =
                    pauseResumeAndAwaitTrackingPlanes(scenario, session, recordedSnapshot)

                val recoveredSnapshot =
                    assertNotNull(
                        postResumeRecovery,
                        "Timed out waiting for pre-pause planes to resume TRACKING.",
                    )
                val recoveredByOwner = recoveredSnapshot.trackingPlanes.associateBy { it.owner }
                for (prePausePlane in recordedSnapshot.trackingPlanes) {
                    // awaitTrackingPlanesAfter guarantees each recorded Plane is present and
                    // TRACKING.
                    val resumedPlane = recoveredByOwner.getValue(prePausePlane.owner)
                    assertPlanePoseContinuity(resumedPlane.centerPose, prePausePlane.centerPose)
                }
            }
        }
    }

    @Test
    @XrDeviceTest
    fun sessionLifecycle_activityRecreateWithPlaneTracking_detectsTrackingPlanes() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                val initialSession = createConfiguredPlaneTrackingSession(scenario)
                val preRecreateSnapshot = awaitTrackingPlanesAfter(initialSession)
                assumeTrue(
                    "Skipping test: No tracking planes detected before recreate within timeout.",
                    preRecreateSnapshot != null,
                )

                scenario.recreate()
                val recreatedSession = createConfiguredPlaneTrackingSession(scenario)
                val recreatedBaselineState = awaitRuntimeCoreState(recreatedSession)
                val recreatedAdvancingState =
                    awaitCoreStateAfter(recreatedSession, recreatedBaselineState.timeMark)
                val recreatedPlanes = awaitTrackingPlanesAfter(recreatedSession)

                assertThat(recreatedSession).isNotSameInstanceAs(initialSession)
                assertNotNull(
                    recreatedAdvancingState,
                    "Expected advancing CoreState after Activity recreation.",
                )
                val resolvedPlanes =
                    assertNotNull(
                        recreatedPlanes,
                        "Expected at least one TRACKING plane after Activity recreation.",
                    )
                assertThat(resolvedPlanes.trackingPlanes).isNotEmpty()
            }
        }
    }

    @Test
    @XrDeviceTest
    fun sessionLifecycle_configureInFlightDuringRapidPauseAndResume_maintainsHealthySession() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                val handle = createManualLifecycle(scenario, Lifecycle.State.RESUMED)
                val session =
                    createConfiguredDefaultSession(scenario, lifecycleOwner = handle.owner)

                val outcome =
                    runConcurrentConfigureAndLifecycleBurst(scenario, handle.registry, session)

                assertThat(outcome.overlappingConfigureCount)
                    .isAtLeast(MIN_OVERLAPPING_CONFIGURE_CALLS)
                assertThat(
                        outcome.concurrentConfigResults.filterNot {
                            it is SessionConfigureSuccess
                        }
                    )
                    .isEmpty()
                assertThat(outcome.postStressConfigResult)
                    .isInstanceOf(SessionConfigureSuccess::class.java)
                assertNotNull(
                    outcome.postStressAdvancingState,
                    "Expected advancing CoreState after concurrent configure stress.",
                )
            }
        }
    }

    @Test
    @XrDeviceTest
    fun sessionLifecycle_activityRecreate_reinitializesSession() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                val initialSession = createConfiguredDefaultSession(scenario)

                scenario.recreate()
                val newActivity = getActivity(scenario)
                val newSession = createSessionOrSkip(newActivity)
                val configResult = newSession.configure(Config.Builder().build())
                val recreatedState = awaitRuntimeCoreState(newSession)
                val recreatedAdvancingState =
                    awaitCoreStateAfter(newSession, recreatedState.timeMark)

                assertThat(newSession.activity).isEqualTo(newActivity)
                assertThat(newSession).isNotSameInstanceAs(initialSession)
                assertThat(configResult).isInstanceOf(SessionConfigureSuccess::class.java)
                assertNotNull(
                    recreatedAdvancingState,
                    "Expected advancing CoreState after Activity recreation.",
                )
            }
        }
    }

    /**
     * Validates that destroying and recreating sessions across multiple activity recreations
     * produces distinct functional sessions, with the final session remaining configurable and
     * emitting advancing state updates.
     */
    @Test
    @XrDeviceTest
    fun sessionLifecycle_repeatedRecreationChurn_maintainsFunctionalSession() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                val createdSessions = mutableListOf<Session>()

                repeat(RECREATION_CHURN_COUNT) { iteration ->
                    val currentSession = createSessionOrSkip(getActivity(scenario))
                    createdSessions.add(currentSession)
                    if (iteration < RECREATION_CHURN_COUNT - 1) {
                        scenario.recreate()
                    }
                }
                val finalSession = createdSessions.last()
                val finalConfigResult = finalSession.configure(Config.Builder().build())
                val finalBaseline = awaitRuntimeCoreState(finalSession)
                val finalAdvancingState = awaitCoreStateAfter(finalSession, finalBaseline.timeMark)

                assertThat(createdSessions).hasSize(RECREATION_CHURN_COUNT)
                assertThat(createdSessions.distinct()).hasSize(RECREATION_CHURN_COUNT)
                assertThat(finalConfigResult).isInstanceOf(SessionConfigureSuccess::class.java)
                assertNotNull(
                    finalAdvancingState,
                    "Expected advancing CoreState on final recreated session.",
                )
            }
        }
    }

    @Test
    @XrDeviceTest
    fun sessionLifecycle_destroyedLifecycleOwner_cannotCreateSession() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            val activity = getActivity(scenario)
            val handle = createManualLifecycle(scenario, Lifecycle.State.DESTROYED)

            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    Session.create(context = activity, lifecycleOwner = handle.owner)
                }
            }
        }
    }

    @Test
    @XrDeviceTest
    fun sessionLifecycle_destroyedSession_cannotConfigure() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            val handle = createManualLifecycle(scenario, Lifecycle.State.RESUMED)
            val session = runBlocking {
                createSessionOrSkip(getActivity(scenario), lifecycleOwner = handle.owner)
            }

            scenario.onActivity { handle.registry.currentState = Lifecycle.State.DESTROYED }

            assertThrows(IllegalStateException::class.java) {
                session.configure(Config.Builder().build())
            }
        }
    }

    @Test
    @XrDeviceTest
    fun session_stateFlow_emitsValidState() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                val session = createSessionOrSkip(getActivity(scenario))
                val configResult = session.configure(Config.Builder().build())
                check(configResult is SessionConfigureSuccess) {
                    "Failed to configure session: $configResult"
                }

                val initialState = awaitRuntimeCoreState(session)
                val nextState = awaitCoreStateAfter(session, initialState.timeMark)

                val resolvedState =
                    assertNotNull(nextState, "Expected advancing CoreState from Session.state.")
                assertThat(resolvedState.perceptionState).isNotNull()
            }
        }
    }

    private class ManualLifecycleHandle(
        val owner: LifecycleOwner,
        val registry: LifecycleRegistry,
    )

    private data class PauseResumeOutcome(
        val emissionWhilePaused: CoreState?,
        val stateAfterResume: CoreState?,
        val configureAfterResume: SessionConfigureResult,
    )

    private data class PlaneSnapshot(
        val coreState: CoreState,
        val trackingPlanes: List<Plane.State>,
    )

    private data class ConcurrentConfigureBurstOutcome(
        val concurrentConfigResults: List<SessionConfigureResult>,
        val overlappingConfigureCount: Int,
        val postStressConfigResult: SessionConfigureResult,
        val postStressAdvancingState: CoreState?,
    )

    private fun getActivity(scenario: ActivityScenario<ComponentActivity>): ComponentActivity {
        lateinit var activity: ComponentActivity
        scenario.onActivity { activity = it }
        return activity
    }

    private fun createManualLifecycle(
        scenario: ActivityScenario<ComponentActivity>,
        initialState: Lifecycle.State = Lifecycle.State.RESUMED,
    ): ManualLifecycleHandle {
        lateinit var registry: LifecycleRegistry
        val owner =
            object : LifecycleOwner {
                override val lifecycle: Lifecycle
                    get() = registry
            }
        scenario.onActivity {
            registry =
                LifecycleRegistry(owner).apply {
                    currentState = Lifecycle.State.CREATED
                    if (initialState != Lifecycle.State.CREATED) {
                        currentState = initialState
                    }
                }
        }
        return ManualLifecycleHandle(owner, registry)
    }

    private suspend fun createConfiguredDefaultSession(
        scenario: ActivityScenario<ComponentActivity>,
        lifecycleOwner: LifecycleOwner? = null,
    ): Session {
        val activity = getActivity(scenario)
        val session = createSessionOrSkip(activity, lifecycleOwner = lifecycleOwner ?: activity)
        val configResult = session.configure(Config.Builder().build())
        check(configResult is SessionConfigureSuccess) {
            "Failed to configure default session: $configResult"
        }
        val initialState = awaitRuntimeCoreState(session)
        checkNotNull(awaitCoreStateAfter(session, initialState.timeMark)) {
            "Timed out after $SESSION_STATE_TIMEOUT waiting for initial CoreState progression."
        }
        return session
    }

    private suspend fun createConfiguredPlaneTrackingSession(
        scenario: ActivityScenario<ComponentActivity>
    ): Session {
        val session = createSessionOrSkip(getActivity(scenario))
        val planeConfig =
            Config.Builder().setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL).build()
        val configResult = session.configure(planeConfig)
        check(configResult is SessionConfigureSuccess) {
            "Failed to configure plane tracking: $configResult"
        }
        return session
    }

    private suspend fun awaitRuntimeCoreState(
        session: Session,
        timeout: Duration = SESSION_STATE_TIMEOUT,
    ): CoreState {
        val state =
            withTimeoutOrNull(timeout) { session.state.first { it.perceptionState != null } }
        return checkNotNull(state) {
            "Timed out after $timeout waiting for runtime CoreState with PerceptionState."
        }
    }

    private suspend fun awaitCoreStateAfter(
        session: Session,
        afterMark: ComparableTimeMark,
        timeout: Duration = SESSION_STATE_TIMEOUT,
    ): CoreState? =
        withTimeoutOrNull(timeout) {
            session.state.first { it.perceptionState != null && it.timeMark > afterMark }
        }

    // Observe session.state directly because Plane.subscribe uses stateIn, which conflates
    // updates when the tracked Plane set remains unchanged across frames.
    private suspend fun awaitTrackingPlanesAfter(
        session: Session,
        afterMark: ComparableTimeMark? = null,
        requiredPlanes: Set<Plane> = emptySet(),
        timeout: Duration = PERCEPTION_DISCOVERY_TIMEOUT,
    ): PlaneSnapshot? =
        withTimeoutOrNull(timeout) {
            session.state
                .mapNotNull { coreState ->
                    val perception = coreState.perceptionState ?: return@mapNotNull null
                    if (afterMark != null && coreState.timeMark <= afterMark) {
                        return@mapNotNull null
                    }
                    val allTracking =
                        perception.trackableStates.filterIsInstance<Plane.State>().filter {
                            it.trackingState == TrackingState.TRACKING
                        }
                    if (allTracking.isEmpty()) {
                        return@mapNotNull null
                    }
                    val activeTracking =
                        allTracking.filter { it.subsumedBy == null }.ifEmpty { allTracking }
                    if (
                        requiredPlanes.isNotEmpty() &&
                            !requiredPlanes.all { req -> allTracking.any { it.owner === req } }
                    ) {
                        return@mapNotNull null
                    }
                    PlaneSnapshot(
                        coreState,
                        if (requiredPlanes.isEmpty()) activeTracking else allTracking,
                    )
                }
                .first()
        }

    // pause() cancels updateJob without awaiting an in-flight configurationMutex.withLock {
    // update() } frame, so drain any trailing emission before measuring paused quiescence.
    private suspend fun settlePausedStateAndCaptureTimeMark(
        session: Session,
        fallbackMark: ComparableTimeMark,
    ): ComparableTimeMark {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        var settledMark = maxRuntimeTimeMark(fallbackMark, session.state.value)
        repeat(MAX_IN_FLIGHT_SETTLE_ATTEMPTS) {
            val trailingState =
                withTimeoutOrNull(IN_FLIGHT_UPDATE_SETTLE_TIMEOUT) {
                    session.state.first { it.perceptionState != null && it.timeMark > settledMark }
                } ?: return settledMark
            settledMark = trailingState.timeMark
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        }
        return maxRuntimeTimeMark(settledMark, session.state.value)
    }

    private fun maxRuntimeTimeMark(
        baseline: ComparableTimeMark,
        candidateState: CoreState,
    ): ComparableTimeMark {
        if (candidateState.perceptionState == null) {
            return baseline
        }
        return if (candidateState.timeMark > baseline) candidateState.timeMark else baseline
    }

    private suspend fun observePauseAndResumeLifecycle(
        scenario: ActivityScenario<ComponentActivity>,
        session: Session,
        pausedLifecycleState: Lifecycle.State,
    ): PauseResumeOutcome {
        val baseline = awaitRuntimeCoreState(session)
        moveActivityToInactiveState(scenario, pausedLifecycleState)
        val pausedSettledMark = settlePausedStateAndCaptureTimeMark(session, baseline.timeMark)
        val emissionWhilePaused =
            withTimeoutOrNull(PAUSED_QUIESCENCE_WINDOW) {
                session.state.first {
                    it.perceptionState != null && it.timeMark > pausedSettledMark
                }
            }
        resumeActivityFromInactiveState(scenario)
        val postPauseBaseline = emissionWhilePaused?.timeMark ?: pausedSettledMark
        val stateAfterResume = awaitCoreStateAfter(session, postPauseBaseline)
        val configureAfterResume = session.configure(session.config)
        return PauseResumeOutcome(
            emissionWhilePaused = emissionWhilePaused,
            stateAfterResume = stateAfterResume,
            configureAfterResume = configureAfterResume,
        )
    }

    private suspend fun pauseResumeAndAwaitTrackingPlanes(
        scenario: ActivityScenario<ComponentActivity>,
        session: Session,
        prePauseSnapshot: PlaneSnapshot,
    ): PlaneSnapshot? {
        val recordedOwners = prePauseSnapshot.trackingPlanes.map { it.owner }.toSet()
        moveActivityToInactiveState(scenario, Lifecycle.State.CREATED)
        val pausedSettledMark =
            settlePausedStateAndCaptureTimeMark(session, prePauseSnapshot.coreState.timeMark)
        resumeActivityFromInactiveState(scenario)
        return awaitTrackingPlanesAfter(
            session,
            afterMark = pausedSettledMark,
            requiredPlanes = recordedOwners,
        )
    }

    // ActivityScenario.moveToState launches facilitators with FLAG_ACTIVITY_NEW_TASK, which opens a
    // separate multi-resumed window on XR; stack test-owned facilitators on the same task instead.
    private fun moveActivityToInactiveState(
        scenario: ActivityScenario<ComponentActivity>,
        targetState: Lifecycle.State,
    ) {
        val (activityClass, resumedAction) =
            when (targetState) {
                Lifecycle.State.STARTED ->
                    LifecycleFacilitatorActivities.FloatingFacilitatorActivity::class.java to
                        LifecycleFacilitatorActivities.ACTION_FLOATING_FACILITATOR_RESUMED
                Lifecycle.State.CREATED ->
                    LifecycleFacilitatorActivities.OpaqueFacilitatorActivity::class.java to
                        LifecycleFacilitatorActivities.ACTION_OPAQUE_FACILITATOR_RESUMED
                else ->
                    throw IllegalArgumentException(
                        "Unsupported inactive target state: $targetState"
                    )
            }
        val facilitatorIntent =
            Intent()
                .setClassName(
                    InstrumentationRegistry.getInstrumentation().context,
                    activityClass.name,
                )
        try {
            awaitFacilitatorActivityResumed(resumedAction) {
                scenario.onActivity { activity -> activity.startActivity(facilitatorIntent) }
            }
            waitUntil(
                description = "activity transitions to $targetState",
                timeout = SESSION_STATE_TIMEOUT,
            ) {
                scenario.state == targetState
            }
        } catch (t: Throwable) {
            InstrumentationRegistry.getInstrumentation()
                .targetContext
                .applicationContext
                .sendBroadcast(
                    Intent(LifecycleFacilitatorActivities.ACTION_FINISH_FACILITATOR_ACTIVITIES)
                )
            throw t
        }
    }

    private fun resumeActivityFromInactiveState(scenario: ActivityScenario<ComponentActivity>) {
        InstrumentationRegistry.getInstrumentation()
            .targetContext
            .applicationContext
            .sendBroadcast(
                Intent(LifecycleFacilitatorActivities.ACTION_FINISH_FACILITATOR_ACTIVITIES)
            )
        waitUntil(
            description = "activity transitions to ${Lifecycle.State.RESUMED}",
            timeout = SESSION_STATE_TIMEOUT,
        ) {
            scenario.state == Lifecycle.State.RESUMED
        }
    }

    private fun awaitFacilitatorActivityResumed(resumedAction: String, launchActivity: () -> Unit) {
        val appContext =
            InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val resumedLatch = CountDownLatch(1)
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    resumedLatch.countDown()
                }
            }
        appContext.registerReceiver(
            receiver,
            IntentFilter(resumedAction),
            Context.RECEIVER_EXPORTED,
        )
        try {
            launchActivity()
            check(
                resumedLatch.await(
                    SESSION_STATE_TIMEOUT.inWholeMilliseconds,
                    TimeUnit.MILLISECONDS,
                )
            ) {
                "Timed out after $SESSION_STATE_TIMEOUT waiting for $resumedAction."
            }
        } finally {
            appContext.unregisterReceiver(receiver)
        }
    }

    private fun startConcurrentConfigureJob(
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher,
        session: Session,
        firstConfigureCompleted: CompletableDeferred<Unit>,
        completedConfigureCount: AtomicInteger,
        stopConfiguring: AtomicBoolean,
    ): Deferred<List<SessionConfigureResult>> =
        scope.async(dispatcher) {
            val results = mutableListOf<SessionConfigureResult>()
            var index = 0
            while (!stopConfiguring.get() && results.size < MAX_CONCURRENT_CONFIGURE_CALLS) {
                val mode =
                    if (index % 2 == 0) {
                        DeviceTrackingMode.SPATIAL
                    } else {
                        DeviceTrackingMode.DISABLED
                    }
                results.add(session.configure(Config.Builder().setDeviceTracking(mode).build()))
                completedConfigureCount.incrementAndGet()
                firstConfigureCompleted.complete(Unit)
                index++
                yield()
            }
            results.toList()
        }

    private suspend fun runConcurrentConfigureAndLifecycleBurst(
        scenario: ActivityScenario<ComponentActivity>,
        registry: LifecycleRegistry,
        session: Session,
    ): ConcurrentConfigureBurstOutcome =
        Executors.newSingleThreadExecutor().asCoroutineDispatcher().use { configureDispatcher ->
            coroutineScope {
                val firstConfigureCompleted = CompletableDeferred<Unit>()
                val completedConfigureCount = AtomicInteger(0)
                val stopConfiguring = AtomicBoolean(false)
                val configureJob =
                    startConcurrentConfigureJob(
                        scope = this,
                        dispatcher = configureDispatcher,
                        session = session,
                        firstConfigureCompleted = firstConfigureCompleted,
                        completedConfigureCount = completedConfigureCount,
                        stopConfiguring = stopConfiguring,
                    )
                val overlappingConfigureCount =
                    try {
                        check(
                            withTimeoutOrNull(SESSION_STATE_TIMEOUT) {
                                firstConfigureCompleted.await()
                            } != null
                        ) {
                            "Timed out after $SESSION_STATE_TIMEOUT waiting for concurrent " +
                                "configure."
                        }
                        val baselineCompleted = completedConfigureCount.get()
                        val burstStart = TimeSource.Monotonic.markNow()
                        var cycles = 0
                        while (
                            (cycles < RAPID_LIFECYCLE_CYCLES ||
                                completedConfigureCount.get() - baselineCompleted <
                                    MIN_OVERLAPPING_CONFIGURE_CALLS) &&
                                burstStart.elapsedNow() < SESSION_STATE_TIMEOUT
                        ) {
                            scenario.onActivity {
                                registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                                registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
                            }
                            cycles++
                            yield()
                        }
                        completedConfigureCount.get() - baselineCompleted
                    } finally {
                        stopConfiguring.set(true)
                    }
                val concurrentConfigResults = configureJob.await()
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                val postStressConfigResult = session.configure(Config.Builder().build())
                val postStressBaseline = awaitRuntimeCoreState(session)
                val postStressAdvancingState =
                    awaitCoreStateAfter(session, postStressBaseline.timeMark)
                ConcurrentConfigureBurstOutcome(
                    concurrentConfigResults = concurrentConfigResults,
                    overlappingConfigureCount = overlappingConfigureCount,
                    postStressConfigResult = postStressConfigResult,
                    postStressAdvancingState = postStressAdvancingState,
                )
            }
        }

    // Allows up to 5 cm per axis for bounding-box center shifts as plane extents refine across
    // pause/resume, and rotation dot delta <= 2e-3 (~7.2 degrees: 2 * acos(1 - 2e-3)).
    private fun assertPlanePoseContinuity(actual: Pose, expected: Pose) {
        assertFinitePose(actual)
        assertValidQuaternion(actual.rotation)
        assertPoseEquals(
            actual = actual,
            expected = expected,
            translationEpsilon = PLANE_PAUSE_TRANSLATION_EPSILON_METERS,
            rotationDotEpsilon = PLANE_PAUSE_ROTATION_DOT_EPSILON,
        )
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

    private companion object {
        private val IN_FLIGHT_UPDATE_SETTLE_TIMEOUT = 250.milliseconds
        private val PAUSED_QUIESCENCE_WINDOW = 350.milliseconds
        private const val MAX_IN_FLIGHT_SETTLE_ATTEMPTS = 2
        private const val RAPID_LIFECYCLE_CYCLES = 20
        private const val MIN_OVERLAPPING_CONFIGURE_CALLS = 5
        private const val MAX_CONCURRENT_CONFIGURE_CALLS = 10_000
        private const val RECREATION_CHURN_COUNT = 3
        private const val PLANE_PAUSE_TRANSLATION_EPSILON_METERS = 0.05f
        private const val PLANE_PAUSE_ROTATION_DOT_EPSILON = 2e-3f
    }
}
