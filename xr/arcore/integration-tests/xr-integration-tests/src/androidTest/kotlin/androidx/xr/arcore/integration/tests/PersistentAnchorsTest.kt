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
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.AnchorInvalidUuidException
import androidx.xr.arcore.AnchorResult
import androidx.xr.arcore.ArDevice
import androidx.xr.arcore.TrackingState
import androidx.xr.arcore.perceptionState
import androidx.xr.arcore.runtime.PerceptionRuntime
import androidx.xr.runtime.AnchorPersistenceMode
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.filterSupportedPermissions
import androidx.xr.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated integration tests for JXR ARCore Persistent Anchors ([Anchor] creation, persistence,
 * cross-session loading, and deletion).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class PersistentAnchorsTest {

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

    private class ActiveSessionTracker(var current: Session? = null)

    private fun launchWithSessionCleanup(
        block: (ActivityScenario<ComponentActivity>, ActiveSessionTracker) -> Unit
    ) {
        val sessionTracker = ActiveSessionTracker()
        var primaryFailure: Throwable? = null
        try {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                block(scenario, sessionTracker)
            }
        } catch (t: Throwable) {
            primaryFailure = t
            throw t
        } finally {
            val cleanupTracker = CleanupErrorTracker(primaryFailure)
            sessionTracker.current?.let { session ->
                cleanupTracker.runStep { awaitSessionDestroyed(session) }
            }
            cleanupTracker.throwIfNeeded()
        }
    }

    private fun runWithConfiguredSession(
        persistenceMode: AnchorPersistenceMode,
        block: (Session) -> Unit,
    ) {
        launchWithSessionCleanup { scenario, sessionTracker ->
            val session =
                createConfiguredSession(
                    scenario = scenario,
                    sessionTracker = sessionTracker,
                    persistenceMode = persistenceMode,
                )
            block(session)
        }
    }

    private fun createConfiguredSession(
        scenario: ActivityScenario<ComponentActivity>,
        sessionTracker: ActiveSessionTracker,
        persistenceMode: AnchorPersistenceMode = AnchorPersistenceMode.LOCAL,
    ): Session {
        var activity: ComponentActivity? = null
        scenario.onActivity {
            // Prime LifecycleRegistry's LazyThreadSafetyMode.NONE StateFlow on the main thread
            // before isDeviceTracking polls currentStateFlow.value on the instrumentation thread.
            it.lifecycle.currentStateFlow
            activity = it
        }
        val resolvedActivity =
            checkNotNull(activity) { "ComponentActivity was not available from ActivityScenario." }
        val session = runBlocking(Dispatchers.IO) { createSessionOrSkip(resolvedActivity) }
        sessionTracker.current = session
        val config =
            Config.Builder()
                .setAnchorPersistence(persistenceMode)
                .setDeviceTracking(DeviceTrackingMode.SPATIAL)
                .setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
                .build()
        val configResult = session.configure(config)
        check(configResult is SessionConfigureSuccess) {
            "Failed to configure session: $configResult"
        }
        return session
    }

    /**
     * Launches a [ComponentActivity] with a configured [Session] and awaits sustained spatial
     * device tracking before invoking [block].
     *
     * On cold start after a system restart, the system home launcher can appear shortly after
     * initial head tracking begins and take spatial focus from an already-launched task, leaving
     * its [Session] paused. Requiring tracking to remain active across
     * [DEVICE_TRACKING_STABILITY_WINDOW] avoids entering [block] during that transient pre-focus
     * window, and closing and relaunching [ActivityScenario] brings a fresh task to the foreground
     * and dismisses the launcher.
     */
    private fun runWithTrackedSession(
        persistenceMode: AnchorPersistenceMode = AnchorPersistenceMode.LOCAL,
        block: (ActivityScenario<ComponentActivity>, ActiveSessionTracker, Session) -> Unit,
    ) {
        for (attempt in 1..MAX_TRACKING_WARMUP_ATTEMPTS) {
            var completed = false
            launchWithSessionCleanup { scenario, sessionTracker ->
                val session =
                    createConfiguredSession(
                        scenario = scenario,
                        sessionTracker = sessionTracker,
                        persistenceMode = persistenceMode,
                    )
                val isReady =
                    if (attempt == MAX_TRACKING_WARMUP_ATTEMPTS) {
                        awaitStableDeviceTracking(session)
                        true
                    } else {
                        tryAwaitStableDeviceTracking(session)
                    }
                if (!isReady) return@launchWithSessionCleanup
                block(scenario, sessionTracker, session)
                completed = true
            }
            if (completed) return
        }
    }

    private class CleanupErrorTracker(private val primaryFailure: Throwable?) {
        private var firstCleanupFailure: Throwable? = null

        fun runStep(action: () -> Unit) {
            try {
                action()
            } catch (t: Throwable) {
                val existingFailure = firstCleanupFailure
                when {
                    primaryFailure != null -> primaryFailure.addSuppressed(t)
                    existingFailure != null -> existingFailure.addSuppressed(t)
                    else -> firstCleanupFailure = t
                }
            }
        }

        fun throwIfNeeded() {
            val failureToThrow = firstCleanupFailure
            if (primaryFailure == null && failureToThrow != null) throw failureToThrow
        }
    }

    private sealed interface PersistCleanupState {
        data object NotStarted : PersistCleanupState

        data class InProgress(val baselineUuids: Set<UUID>) : PersistCleanupState

        data class PersistedPendingSync(val uuid: UUID) : PersistCleanupState

        data class PersistedSynced(val uuid: UUID) : PersistCleanupState
    }

    private fun cleanupPersistedUuids(
        session: Session,
        persistState: PersistCleanupState,
        tracker: CleanupErrorTracker,
    ) {
        when (persistState) {
            PersistCleanupState.NotStarted -> return
            is PersistCleanupState.InProgress -> {
                var extraUuids: Set<UUID> = emptySet()
                tracker.runStep {
                    extraUuids =
                        waitUntilFoundOrNull(timeout = PERCEPTION_DISCOVERY_TIMEOUT) {
                            val current = Anchor.getPersistedAnchorUuids(session).toSet()
                            (current - persistState.baselineUuids).ifEmpty { null }
                        } ?: emptySet()
                }
                extraUuids.forEach { uuid -> tracker.runStep { unpersistIfPresent(session, uuid) } }
            }
            is PersistCleanupState.PersistedPendingSync -> {
                tracker.runStep { waitForPersistedUuid(session, persistState.uuid) }
                tracker.runStep { unpersistIfPresent(session, persistState.uuid) }
            }
            is PersistCleanupState.PersistedSynced ->
                tracker.runStep { unpersistIfPresent(session, persistState.uuid) }
        }
    }

    private class PersistenceSessionScope(val session: Session) {
        private val _trackedAnchors = mutableListOf<Anchor>()
        val trackedAnchors: List<Anchor>
            get() = _trackedAnchors.toList()

        var persistState: PersistCleanupState = PersistCleanupState.NotStarted
            private set

        fun createAndTrackAnchor(pose: Pose): Anchor =
            createTrackedAnchor(session, pose).also { _trackedAnchors.add(it) }

        fun loadAnchor(uuid: UUID): AnchorResult =
            Anchor.load(session, uuid).also { result ->
                if (result is AnchorCreateSuccess) {
                    _trackedAnchors.add(result.anchor)
                }
            }

        fun persistAnchor(anchor: Anchor, awaitStoreSync: Boolean = true): UUID {
            persistState =
                PersistCleanupState.InProgress(Anchor.getPersistedAnchorUuids(session).toSet())
            val uuid = persistAnchorWithTimeout(anchor)
            persistState = PersistCleanupState.PersistedPendingSync(uuid)
            if (awaitStoreSync) {
                waitForPersistedUuid(session, uuid)
                persistState = PersistCleanupState.PersistedSynced(uuid)
            }
            return uuid
        }

        fun detachAnchor(anchor: Anchor) {
            anchor.detach()
            _trackedAnchors.remove(anchor)
        }
    }

    private fun runWithPersistenceSession(block: (PersistenceSessionScope) -> Unit) {
        runWithTrackedSession { _, _, session ->
            val scope = PersistenceSessionScope(session)
            var primaryFailure: Throwable? = null
            try {
                block(scope)
            } catch (t: Throwable) {
                primaryFailure = t
                throw t
            } finally {
                val tracker = CleanupErrorTracker(primaryFailure)
                scope.trackedAnchors.forEach { anchor -> tracker.runStep { anchor.detach() } }
                cleanupPersistedUuids(session, scope.persistState, tracker)
                tracker.throwIfNeeded()
            }
        }
    }

    private class CrossSessionPersistenceScope(
        val session: Session,
        val uuid: UUID,
        val expectedPose: Pose,
        private val state: CrossSessionCleanupState,
    ) {
        fun loadAnchor(): AnchorResult =
            Anchor.load(session, uuid).also { result ->
                if (result is AnchorCreateSuccess) {
                    state.trackedAnchors.add(result.anchor)
                }
            }

        fun unpersistAnchor() {
            Anchor.unpersist(session, uuid)
            state.persistState = PersistCleanupState.NotStarted
        }
    }

    private class CrossSessionCleanupState(var activeSession: Session? = null) {
        var persistState: PersistCleanupState = PersistCleanupState.NotStarted
        var needsFallbackCleanup: Boolean = false
        val trackedAnchors = mutableListOf<Anchor>()
    }

    private fun runWithRecreatedPersistedAnchor(
        pose: Pose,
        block: (CrossSessionPersistenceScope) -> Unit,
    ) {
        val state = CrossSessionCleanupState()
        var primaryFailure: Throwable? = null
        try {
            runWithTrackedSession { scenario, sessionTracker, initialSession ->
                state.activeSession = initialSession
                state.persistState = PersistCleanupState.NotStarted
                state.needsFallbackCleanup = false
                state.trackedAnchors.clear()
                var attemptFailure: Throwable? = null
                try {
                    val (uuid, expectedPose) = persistInInitialSession(initialSession, pose, state)
                    val recreatedSession =
                        recreateSessionAndAwaitUuid(scenario, sessionTracker, uuid, state)
                    val activeScope =
                        CrossSessionPersistenceScope(
                            session = recreatedSession,
                            uuid = uuid,
                            expectedPose = expectedPose,
                            state = state,
                        )
                    block(activeScope)
                } catch (t: Throwable) {
                    attemptFailure = t
                    throw t
                } finally {
                    cleanupCrossSessionAttempt(state, attemptFailure)
                }
            }
        } catch (t: Throwable) {
            primaryFailure = t
            throw t
        } finally {
            if (state.needsFallbackCleanup) {
                runFallbackPersistenceCleanup(state.persistState, primaryFailure)
            }
        }
    }

    private fun persistInInitialSession(
        initialSession: Session,
        pose: Pose,
        state: CrossSessionCleanupState,
    ): Pair<UUID, Pose> {
        val anchor = createTrackedAnchor(initialSession, pose).also { state.trackedAnchors.add(it) }
        val expectedPose = waitForStableAnchorTracking(initialSession, anchor).pose
        state.persistState =
            PersistCleanupState.InProgress(Anchor.getPersistedAnchorUuids(initialSession).toSet())
        val uuid = persistAnchorWithTimeout(anchor)
        state.persistState = PersistCleanupState.PersistedPendingSync(uuid)
        waitForPersistedUuid(initialSession, uuid)
        anchor.detach()
        state.trackedAnchors.remove(anchor)
        repeat(POST_DETACH_DRAIN_UPDATES) { waitForPerceptionStateUpdate(initialSession) }
        return uuid to expectedPose
    }

    private fun recreateSessionAndAwaitUuid(
        scenario: ActivityScenario<ComponentActivity>,
        sessionTracker: ActiveSessionTracker,
        uuid: UUID,
        state: CrossSessionCleanupState,
    ): Session {
        val previousSession = state.activeSession
        state.activeSession = null
        scenario.recreate()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        if (previousSession != null) {
            try {
                awaitSessionDestroyed(previousSession)
            } finally {
                sessionTracker.current = null
            }
        }
        val recreatedSession = createConfiguredSession(scenario, sessionTracker)
        awaitDeviceTracking(recreatedSession)
        waitForPersistedUuid(recreatedSession, uuid)
        state.activeSession = recreatedSession
        state.persistState = PersistCleanupState.PersistedSynced(uuid)
        return recreatedSession
    }

    private fun cleanupCrossSessionAttempt(
        state: CrossSessionCleanupState,
        attemptFailure: Throwable?,
    ) {
        val tracker = CleanupErrorTracker(attemptFailure)
        state.trackedAnchors.forEach { anchor -> tracker.runStep { anchor.detach() } }
        state.trackedAnchors.clear()
        val liveSession =
            state.activeSession?.takeIf { session ->
                isDeviceTracking(session, ArDevice.getInstance(session))
            }
        if (liveSession != null) {
            cleanupPersistedUuids(liveSession, state.persistState, tracker)
        } else if (state.persistState !is PersistCleanupState.NotStarted) {
            state.needsFallbackCleanup = true
        }
        tracker.throwIfNeeded()
    }

    private fun runFallbackPersistenceCleanup(
        persistState: PersistCleanupState,
        primaryFailure: Throwable?,
    ) {
        val fallbackPersistState =
            when (persistState) {
                is PersistCleanupState.PersistedSynced ->
                    PersistCleanupState.PersistedPendingSync(persistState.uuid)
                else -> persistState
            }
        val tracker = CleanupErrorTracker(primaryFailure)
        tracker.runStep {
            runWithTrackedSession { _, _, cleanupSession ->
                cleanupPersistedUuids(cleanupSession, fallbackPersistState, tracker)
            }
        }
        tracker.throwIfNeeded()
    }

    @Test
    @XrDeviceTest
    fun createAnchor_withValidPose_returnsAnchorCreateSuccess() {
        runWithTrackedSession { _, _, session ->
            val result = Anchor.create(session, VALID_ANCHOR_POSE)

            val anchor = assertIs<AnchorCreateSuccess>(result).anchor
            try {
                val trackedState = waitForAnchorTracking(anchor)
                assertPoseWithinTolerance(
                    actual = trackedState.pose,
                    expected = VALID_ANCHOR_POSE,
                    translationToleranceMeters = SAME_SESSION_TRANSLATION_TOLERANCE_METERS,
                    rotationToleranceDegrees = SAME_SESSION_ROTATION_TOLERANCE_DEGREES,
                )
            } finally {
                anchor.detach()
            }
        }
    }

    @Test
    @XrDeviceTest
    fun persistAnchor_whenAnchorPersistenceEnabled_returnsValidUuid() {
        runWithPersistenceSession { scope ->
            val trackedAnchor = scope.createAndTrackAnchor(VALID_ANCHOR_POSE)

            val uuid = scope.persistAnchor(trackedAnchor, awaitStoreSync = false)

            assertThat(uuid).isNotEqualTo(UUID(0L, 0L))
            waitForPersistedUuid(scope.session, uuid)
        }
    }

    @Test
    @XrDeviceTest
    fun getPersistedAnchorUuids_whenPersistenceDisabled_throwsIllegalStateException() {
        runWithConfiguredSession(AnchorPersistenceMode.DISABLED) { session ->
            assertFailsWith<IllegalStateException> { Anchor.getPersistedAnchorUuids(session) }
        }
    }

    @Test
    @XrDeviceTest
    fun persistAnchor_whenPersistenceDisabled_throwsIllegalStateException() {
        runWithTrackedSession(AnchorPersistenceMode.DISABLED) { _, _, session ->
            val result = Anchor.create(session, VALID_ANCHOR_POSE)
            check(result is AnchorCreateSuccess) {
                "Expected AnchorCreateSuccess for pose $VALID_ANCHOR_POSE, but got $result"
            }
            val anchor = result.anchor

            val exception =
                try {
                    assertFailsWith<IllegalStateException> { persistAnchorWithTimeout(anchor) }
                } finally {
                    anchor.detach()
                }

            assertThat(exception).isNotInstanceOf(CancellationException::class.java)
            assertThat(exception).hasMessageThat().contains("DISABLED")
        }
    }

    @Test
    @XrDeviceTest
    fun loadAnchor_withPersistedUuid_returnsAnchorCreateSuccess() {
        runWithPersistenceSession { scope ->
            val trackedAnchor = scope.createAndTrackAnchor(VALID_ANCHOR_POSE)
            val recordedPose = waitForStableAnchorTracking(scope.session, trackedAnchor).pose
            val uuid = scope.persistAnchor(trackedAnchor)

            val loadResult = scope.loadAnchor(uuid)

            val restoredAnchor = assertIs<AnchorCreateSuccess>(loadResult).anchor
            val loadedState = waitForStableAnchorTracking(scope.session, restoredAnchor)
            assertPoseWithinTolerance(
                actual = loadedState.pose,
                expected = recordedPose,
                translationToleranceMeters = SAME_SESSION_TRANSLATION_TOLERANCE_METERS,
                rotationToleranceDegrees = SAME_SESSION_ROTATION_TOLERANCE_DEGREES,
            )
        }
    }

    @Test
    @XrDeviceTest
    fun loadAnchor_acrossRecreatedSessions_preservesPose() {
        runWithRecreatedPersistedAnchor(CROSS_SESSION_ANCHOR_POSE) { scope ->
            val loadResult = scope.loadAnchor()

            val restoredAnchor = assertIs<AnchorCreateSuccess>(loadResult).anchor
            val restoredState = waitForStableAnchorTracking(scope.session, restoredAnchor)
            assertPoseWithinTolerance(
                actual = restoredState.pose,
                expected = scope.expectedPose,
                translationToleranceMeters = RELOCALIZED_TRANSLATION_TOLERANCE_METERS,
                rotationToleranceDegrees = RELOCALIZED_ROTATION_TOLERANCE_DEGREES,
            )
        }
    }

    @Test
    @XrDeviceTest
    fun loadAnchor_withInvalidUuid_throwsAnchorInvalidUuidException() {
        runWithPersistenceSession { scope ->
            val sentinelAnchor = scope.createAndTrackAnchor(VALID_ANCHOR_POSE)
            val sentinelUuid = scope.persistAnchor(sentinelAnchor)
            val unknownUuid = generateSequence { UUID.randomUUID() }.first { it != sentinelUuid }

            assertFailsWith<AnchorInvalidUuidException> { Anchor.load(scope.session, unknownUuid) }
        }
    }

    @Test
    @XrDeviceTest
    fun loadAnchor_whenPersistenceDisabled_throwsIllegalStateException() {
        runWithConfiguredSession(AnchorPersistenceMode.DISABLED) { session ->
            assertFailsWith<IllegalStateException> { Anchor.load(session, UUID.randomUUID()) }
        }
    }

    @Test
    @XrDeviceTest
    fun unpersistAnchor_removesUuidFromPersistedList() {
        runWithPersistenceSession { scope ->
            val trackedAnchor = scope.createAndTrackAnchor(VALID_ANCHOR_POSE)
            val uuid = scope.persistAnchor(trackedAnchor)

            Anchor.unpersist(scope.session, uuid)

            waitUntilRemovedFromPersistedUuids(scope.session, uuid)
        }
    }

    @Test
    @XrDeviceTest
    fun unpersistAnchor_whenAnchorLoaded_keepsLoadedAnchorTracking() {
        runWithRecreatedPersistedAnchor(VALID_ANCHOR_POSE) { scope ->
            val loadResult = scope.loadAnchor()
            check(loadResult is AnchorCreateSuccess) {
                "Expected AnchorCreateSuccess for persisted UUID ${scope.uuid}, but got $loadResult"
            }
            val restoredAnchor = loadResult.anchor
            waitForStableAnchorTracking(scope.session, restoredAnchor)

            scope.unpersistAnchor()

            waitUntilRemovedFromPersistedUuids(scope.session, scope.uuid)
            assertInvariantAcrossSettleWindow(scope.session) {
                assertThat(restoredAnchor.state.value.trackingState)
                    .isEqualTo(TrackingState.TRACKING)
            }
        }
    }

    @Test
    @XrDeviceTest
    fun unpersistAnchor_withInvalidUuid_throwsAnchorInvalidUuidException() {
        runWithPersistenceSession { scope ->
            val sentinelAnchor = scope.createAndTrackAnchor(VALID_ANCHOR_POSE)
            val sentinelUuid = scope.persistAnchor(sentinelAnchor)
            val unknownUuid = generateSequence { UUID.randomUUID() }.first { it != sentinelUuid }

            assertFailsWith<AnchorInvalidUuidException> {
                Anchor.unpersist(scope.session, unknownUuid)
            }
        }
    }

    @Test
    @XrDeviceTest
    fun unpersistAnchor_whenPersistenceDisabled_throwsIllegalStateException() {
        runWithConfiguredSession(AnchorPersistenceMode.DISABLED) { session ->
            assertFailsWith<IllegalStateException> { Anchor.unpersist(session, UUID.randomUUID()) }
        }
    }

    @Suppress("RestrictedApiAndroidX")
    @Test
    @XrDeviceTest
    fun detachAnchor_completesSuccessfully() {
        runWithTrackedSession(AnchorPersistenceMode.DISABLED) { _, _, session ->
            val anchor = createTrackedAnchor(session, VALID_ANCHOR_POSE)
            val initialSpaceHandle =
                checkNotNull(anchor.nativeSpaceHandle) {
                    "Tracked OpenXR anchor must expose a non-null XrSpace handle before detach."
                }
            check(initialSpaceHandle != 0L) {
                "Tracked OpenXR anchor must expose a non-zero XrSpace handle before detach."
            }

            anchor.detach()

            assertThat(anchor.nativeSpaceHandle).isNull()
            repeat(POST_DETACH_DRAIN_UPDATES) { waitForPerceptionStateUpdate(session) }
            val stateAfterDetach = anchor.state.value
            assertInvariantAcrossSettleWindow(session) {
                assertThat(anchor.state.value).isSameInstanceAs(stateAfterDetach)
            }
        }
    }

    @Test
    @XrDeviceTest
    fun detachAnchor_preservesPersistedUuid() {
        runWithPersistenceSession { scope ->
            val trackedAnchor = scope.createAndTrackAnchor(VALID_ANCHOR_POSE)
            val uuid = scope.persistAnchor(trackedAnchor)

            scope.detachAnchor(trackedAnchor)

            assertInvariantAcrossSettleWindow(scope.session) {
                assertThat(Anchor.getPersistedAnchorUuids(scope.session)).contains(uuid)
            }
        }
    }

    private companion object {
        /**
         * Maximum launch attempts when cold-start system UI takes spatial focus from an initial
         * task.
         */
        const val MAX_TRACKING_WARMUP_ATTEMPTS = 3
        /**
         * Per-attempt wait for spatial tracking before relaunching a foreground task on cold start.
         */
        val DEVICE_TRACKING_ATTEMPT_TIMEOUT = 10.seconds
        /**
         * Maximum time to wait for HMD spatial tracking on the final warmup attempt or recreate.
         */
        val DEVICE_TRACKING_WARMUP_TIMEOUT = 15.seconds
        /**
         * Continuous [TrackingState.TRACKING] window required before entering a tracked test body
         * so cold-start system UI focus transitions settle before anchors are created.
         */
        val DEVICE_TRACKING_STABILITY_WINDOW = 1.seconds
        /**
         * Perception updates needed after [Anchor.detach] so at least one full update drains the
         * detach queue even if an update was already in flight when [Anchor.detach] ran.
         */
        const val POST_DETACH_DRAIN_UPDATES = 2
        const val MIN_SETTLE_UPDATE_COUNT = 2
        val INVARIANT_SETTLE_WINDOW = 300.milliseconds
        /** Same-session anchor pose tolerance accounting for normal OpenXR localization noise. */
        const val SAME_SESSION_TRANSLATION_TOLERANCE_METERS = 1e-2f
        /**
         * Same-session anchor rotation tolerance accounting for normal OpenXR localization noise.
         */
        const val SAME_SESSION_ROTATION_TOLERANCE_DEGREES = 1.0f
        /**
         * Pose tolerances across in-process [ActivityScenario.recreate] sessions on a stationary
         * device without an intervening reference-space recenter.
         */
        const val RELOCALIZED_TRANSLATION_TOLERANCE_METERS = 0.05f
        const val RELOCALIZED_ROTATION_TOLERANCE_DEGREES = 5.0f
        val VALID_ANCHOR_POSE =
            Pose(
                Vector3(0.25f, -0.1f, -1.5f),
                Quaternion.fromEulerAngles(pitch = 0f, yaw = 30f, roll = 0f),
            )
        val CROSS_SESSION_ANCHOR_POSE =
            Pose(
                Vector3(-0.3f, 0.15f, -1.75f),
                Quaternion.fromEulerAngles(pitch = 0f, yaw = -45f, roll = 0f),
            )

        @Suppress("RestrictedApiAndroidX")
        fun awaitSessionDestroyed(session: Session) {
            val perceptionRuntimes = session.runtimes.filterIsInstance<PerceptionRuntime>()
            waitUntil(
                description = "previous Session PerceptionRuntime native teardown",
                timeout = SESSION_STATE_TIMEOUT,
            ) {
                var allDestroyed = false
                InstrumentationRegistry.getInstrumentation().runOnMainSync {
                    allDestroyed = perceptionRuntimes.all { runtime ->
                        val pointer = runtime.sessionPointer
                        pointer == null || pointer == 0L
                    }
                }
                allDestroyed
            }
        }

        fun isDeviceTracking(session: Session, arDevice: ArDevice): Boolean =
            session.lifecycleOwner.lifecycle.currentStateFlow.value.isAtLeast(
                Lifecycle.State.RESUMED
            ) &&
                arDevice.state.value.trackingState == TrackingState.TRACKING &&
                session.state.value.perceptionState?.arDeviceState?.trackingState ==
                    TrackingState.TRACKING

        fun awaitDeviceTracking(session: Session) {
            val arDevice = ArDevice.getInstance(session)
            waitUntil(
                description = "ArDevice and PerceptionState to reach TrackingState.TRACKING",
                timeout = DEVICE_TRACKING_WARMUP_TIMEOUT,
            ) {
                isDeviceTracking(session, arDevice)
            }
        }

        private class StabilityWindowTracker(private val requiredWindow: Duration) {
            private var windowStart: TimeSource.Monotonic.ValueTimeMark? = null
            private var lastCoreState: Any? = null
            private var trackedUpdates = 0

            fun <T : Any> observe(currentCoreState: Any, activeSample: T?): T? {
                if (activeSample == null) {
                    windowStart = null
                    lastCoreState = null
                    trackedUpdates = 0
                    return null
                }
                val start = windowStart
                if (start == null) {
                    windowStart = TimeSource.Monotonic.markNow()
                    lastCoreState = currentCoreState
                    trackedUpdates = 1
                    return null
                }
                if (currentCoreState === lastCoreState) return null
                lastCoreState = currentCoreState
                trackedUpdates++
                return activeSample.takeIf {
                    trackedUpdates >= MIN_SETTLE_UPDATE_COUNT &&
                        start.elapsedNow() >= requiredWindow
                }
            }
        }

        fun tryAwaitStableDeviceTracking(
            session: Session,
            timeout: Duration = DEVICE_TRACKING_ATTEMPT_TIMEOUT,
        ): Boolean {
            val arDevice = ArDevice.getInstance(session)
            val tracker = StabilityWindowTracker(DEVICE_TRACKING_STABILITY_WINDOW)
            return waitUntilFoundOrNull(timeout = timeout) {
                tracker.observe(
                    currentCoreState = session.state.value,
                    activeSample = Unit.takeIf { isDeviceTracking(session, arDevice) },
                )
            } != null
        }

        fun awaitStableDeviceTracking(session: Session) {
            check(tryAwaitStableDeviceTracking(session, timeout = DEVICE_TRACKING_WARMUP_TIMEOUT)) {
                "Timed out after $DEVICE_TRACKING_WARMUP_TIMEOUT waiting for ArDevice and " +
                    "PerceptionState to stabilize in TrackingState.TRACKING."
            }
        }

        fun createTrackedAnchor(session: Session, pose: Pose): Anchor {
            val createResult = Anchor.create(session, pose)
            check(createResult is AnchorCreateSuccess) {
                "Expected AnchorCreateSuccess for pose $pose, but got $createResult"
            }
            val anchor = createResult.anchor
            return try {
                waitForAnchorTracking(anchor)
                anchor
            } catch (t: Throwable) {
                anchor.detach()
                throw t
            }
        }

        fun waitForAnchorTracking(anchor: Anchor): Anchor.State {
            var trackedState: Anchor.State? = null
            waitUntil(
                description = "anchor to enter TrackingState.TRACKING",
                timeout = PERCEPTION_DISCOVERY_TIMEOUT,
            ) {
                val currentState = anchor.state.value
                val isTracking = currentState.trackingState == TrackingState.TRACKING
                if (isTracking) {
                    trackedState = currentState
                }
                isTracking
            }
            return checkNotNull(trackedState)
        }

        fun waitForStableAnchorTracking(session: Session, anchor: Anchor): Anchor.State {
            val tracker = StabilityWindowTracker(INVARIANT_SETTLE_WINDOW)
            var settledState: Anchor.State? = null
            waitUntil(
                description = "anchor to stabilize in TrackingState.TRACKING",
                timeout = PERCEPTION_DISCOVERY_TIMEOUT,
            ) {
                val sample =
                    anchor.state.value.takeIf { it.trackingState == TrackingState.TRACKING }
                settledState = tracker.observe(session.state.value, sample)
                settledState != null
            }
            return checkNotNull(settledState)
        }

        fun persistAnchorWithTimeout(anchor: Anchor): UUID = runBlocking {
            withTimeout(PERCEPTION_DISCOVERY_TIMEOUT) { anchor.persist() }
        }

        /** Polls persisted UUIDs until [uuid] is present in the persistence store. */
        fun waitForPersistedUuid(session: Session, uuid: UUID) {
            waitUntil(
                "persisted anchor UUID $uuid to appear",
                timeout = PERCEPTION_DISCOVERY_TIMEOUT,
            ) {
                uuid in Anchor.getPersistedAnchorUuids(session)
            }
        }

        fun unpersistIfPresent(session: Session, uuid: UUID) {
            try {
                Anchor.unpersist(session, uuid)
            } catch (e: AnchorInvalidUuidException) {
                if (uuid !in Anchor.getPersistedAnchorUuids(session)) return
                throw e
            }
            waitUntilRemovedFromPersistedUuids(session, uuid)
        }

        fun waitUntilRemovedFromPersistedUuids(session: Session, uuid: UUID) {
            waitUntil(
                "persisted anchor UUID $uuid to be removed",
                timeout = PERCEPTION_DISCOVERY_TIMEOUT,
            ) {
                uuid !in Anchor.getPersistedAnchorUuids(session)
            }
        }

        /**
         * Asserts [invariant] across observed [Session.state] perception updates spanning at least
         * [INVARIANT_SETTLE_WINDOW] and [MIN_SETTLE_UPDATE_COUNT] updates.
         */
        fun assertInvariantAcrossSettleWindow(session: Session, invariant: () -> Unit) {
            val settleStart = TimeSource.Monotonic.markNow()
            var updatesObserved = 0
            while (
                updatesObserved < MIN_SETTLE_UPDATE_COUNT ||
                    settleStart.elapsedNow() < INVARIANT_SETTLE_WINDOW
            ) {
                waitForPerceptionStateUpdate(session)
                updatesObserved++
                invariant()
            }
        }

        fun assertPoseWithinTolerance(
            actual: Pose,
            expected: Pose,
            translationToleranceMeters: Float,
            rotationToleranceDegrees: Float,
        ) {
            assertFinitePose(actual)
            assertThat(Pose.distance(actual, expected)).isAtMost(translationToleranceMeters)
            assertThat(Quaternion.angle(actual.rotation, expected.rotation))
                .isAtMost(rotationToleranceDegrees)
        }
    }
}
