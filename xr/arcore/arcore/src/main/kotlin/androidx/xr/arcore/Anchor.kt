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

package androidx.xr.arcore

import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.Anchor as RuntimeAnchor
import androidx.xr.arcore.runtime.AnchorInvalidUuidException
import androidx.xr.arcore.runtime.AnchorNotTrackingException
import androidx.xr.arcore.runtime.AnchorResourcesExhaustedException
import androidx.xr.runtime.AnchorPersistenceMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Pose
import java.util.UUID
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * An anchor describes a fixed location and orientation in the real world.
 *
 * To stay at a fixed location in physical space, the numerical description of this position may
 * update as ARCore for XR updates its understanding of the physical world.
 *
 * @property state the current [State] of this anchor
 */
public class Anchor
internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val runtimeAnchor: RuntimeAnchor,
    private val xrResourceManager: XrResourcesManager,
) : Updatable {
    public companion object {
        /**
         * Creates and attaches an [Anchor] at the given [pose].
         *
         * @param session the [Session] that is used to create the anchor
         * @param pose the [Pose] that describes the location and orientation of the anchor
         * @return a subtype of [AnchorCreateResult] based on the result of the operation
         * @sample androidx.xr.arcore.samples.callCreateAnchor
         */
        @JvmStatic
        public fun create(session: Session, pose: Pose): AnchorCreateResult {
            val perceptionStateExtender = getPerceptionStateExtender(session)
            val runtimeAnchor: RuntimeAnchor
            try {
                runtimeAnchor = session.perceptionRuntime.perceptionManager.createAnchor(pose)
            } catch (e: AnchorResourcesExhaustedException) {
                return AnchorCreateResourcesExhausted()
            } catch (e: AnchorNotTrackingException) {
                return AnchorCreateTrackingUnavailable()
            }
            val anchor = generateAnchor(runtimeAnchor, perceptionStateExtender.xrResourcesManager)
            return AnchorCreateSuccess(anchor)
        }

        /**
         * Retrieves all the [UUID] instances from [Anchor] objects that have been persisted by
         * [persist] that are still present in the local storage.
         *
         * @param session the [Session] to retrieve the persisted anchor UUIDs from
         * @throws [IllegalStateException] if [Session.config] is set to
         *   [androidx.xr.runtime.AnchorPersistenceMode.DISABLED].
         */
        @JvmStatic
        public fun getPersistedAnchorUuids(session: Session): List<UUID> {
            check(session.config.anchorPersistence != AnchorPersistenceMode.DISABLED) {
                "Config.AnchorPersistenceMode is set to DISABLED."
            }
            return session.perceptionRuntime.perceptionManager.getPersistedAnchorUuids()
        }

        /**
         * Loads an [Anchor] from local storage, using the given [uuid].
         *
         * The anchor will attempt to be attached in the same physical location as the anchor that
         * was previously persisted. The [uuid] should be the return value of a previous call to
         * [persist].
         *
         * @param session the [Session] to load the anchor from
         * @param uuid the [UUID] of the anchor to load
         * @throws [IllegalStateException] if [Session.config] is set to
         *   [AnchorPersistenceMode.DISABLED]
         */
        @JvmStatic
        public fun load(session: Session, uuid: UUID): AnchorCreateResult {
            check(session.config.anchorPersistence != AnchorPersistenceMode.DISABLED) {
                "Config.AnchorPersistenceMode is set to DISABLED."
            }

            val perceptionStateExtender = getPerceptionStateExtender(session)
            val runtimeAnchor: RuntimeAnchor
            try {
                runtimeAnchor = session.perceptionRuntime.perceptionManager.loadAnchor(uuid)
            } catch (e: AnchorInvalidUuidException) {
                return AnchorLoadInvalidUuid()
            } catch (e: AnchorResourcesExhaustedException) {
                return AnchorCreateResourcesExhausted()
            }
            val anchor = generateAnchor(runtimeAnchor, perceptionStateExtender.xrResourcesManager)
            return AnchorCreateSuccess(anchor)
        }

        /**
         * Deletes a persisted Anchor denoted by [uuid] from local storage.
         *
         * @param session the [Session] to unpersist the anchor from
         * @param uuid the [UUID] of the anchor to unpersist
         * @throws [IllegalStateException] if [Session.config] is set to
         *   [AnchorPersistenceMode.DISABLED] or the provided [uuid] is invalid
         */
        @JvmStatic
        public fun unpersist(session: Session, uuid: UUID) {
            check(session.config.anchorPersistence != AnchorPersistenceMode.DISABLED) {
                "Config.AnchorPersistenceMode is set to DISABLED."
            }
            session.perceptionRuntime.perceptionManager.unpersistAnchor(uuid)
        }

        private fun getPerceptionStateExtender(session: Session): PerceptionStateExtender {
            val perceptionStateExtender: PerceptionStateExtender? =
                session.stateExtenders.filterIsInstance<PerceptionStateExtender>().first()
            check(perceptionStateExtender != null) { "PerceptionStateExtender is not available." }
            return perceptionStateExtender
        }

        private fun generateAnchor(
            runtimeAnchor: RuntimeAnchor,
            xrResourceManager: XrResourcesManager,
        ): Anchor {
            val anchor = Anchor(runtimeAnchor, xrResourceManager)
            xrResourceManager.addUpdatable(anchor)
            return anchor
        }
    }

    // TODO(b/372049781): This constructor is only used for testing. Remove it once cl/683360061 is
    // submitted.
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public constructor(runtimeAnchor: RuntimeAnchor) : this(runtimeAnchor, XrResourcesManager())

    /**
     * The representation of the current state of an [Anchor].
     *
     * @property trackingState the current [TrackingState] of the anchor
     * @property pose the location of the anchor in the world coordinate space
     */
    public class State
    internal constructor(public val trackingState: TrackingState, public val pose: Pose) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is State) return false
            return pose == other.pose && trackingState == other.trackingState
        }

        override fun hashCode(): Int {
            var result = pose.hashCode()
            result = 31 * result + trackingState.hashCode()
            return result
        }
    }

    private val _state: MutableStateFlow<State> =
        MutableStateFlow<State>(State(runtimeAnchor.trackingState, runtimeAnchor.pose))

    public val state: StateFlow<State> = _state.asStateFlow()

    private var persistContinuation: Continuation<UUID>? = null

    /**
     * Stores this anchor in the application's local storage so that it can be shared across
     * sessions.
     *
     * @return the [UUID] that uniquely identifies this anchor
     * @throws [IllegalStateException] if [Session.config] is set to
     *   [AnchorPersistenceMode.DISABLED], or if there was an unexpected error persisting the anchor
     *   (e.g. ran out of memory)
     */
    public suspend fun persist(): UUID {
        val config = xrResourceManager.lifecycleManager.config
        check(config.anchorPersistence != AnchorPersistenceMode.DISABLED) {
            "Config.AnchorPersistenceMode is set to DISABLED."
        }
        runtimeAnchor.persist()
        // Suspend the coroutine until the anchor is persisted.
        return suspendCancellableCoroutine { persistContinuation = it }
    }

    /** Detaches this anchor. This anchor will no longer be updated or tracked. */
    public fun detach() {
        xrResourceManager.removeUpdatable(this)
        xrResourceManager.queueAnchorToDetach(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Anchor) return false
        return runtimeAnchor == other.runtimeAnchor
    }

    override fun hashCode(): Int = runtimeAnchor.hashCode()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override suspend fun update() {
        _state.emit(State(runtimeAnchor.trackingState, runtimeAnchor.pose))
        if (persistContinuation == null) {
            return
        }
        when (runtimeAnchor.persistenceState) {
            RuntimeAnchor.PersistenceState.PENDING -> {
                // Do nothing while we wait for the anchor to be persisted.
            }
            RuntimeAnchor.PersistenceState.PERSISTED -> {
                persistContinuation?.resume(runtimeAnchor.uuid!!)
                persistContinuation = null
            }
            RuntimeAnchor.PersistenceState.NOT_PERSISTED -> {
                persistContinuation?.resumeWithException(
                    RuntimeException("Anchor was not persisted.")
                )
                persistContinuation = null
            }
        }
    }
}
