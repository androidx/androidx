/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.scenecore

import android.annotation.SuppressLint
import android.os.SystemClock
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.Plane
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.runtime.AnchorEntity as RtAnchorEntity
import androidx.xr.scenecore.runtime.HandlerExecutor
import java.lang.ref.WeakReference
import java.time.Duration
import java.util.concurrent.Executor
import java.util.function.Consumer
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * An AnchorSpace is a [SpaceEntity] tracks a [Pose] relative to some position or surface in the
 * "Real World." Children of this [SpaceEntity] will remain positioned relative to that location in
 * the real world.
 *
 * Note that, as a SpaceEntity, the AnchorSpace's position is independent of the [ActivitySpace]. It
 * is parentless and its position and scale is controlled by the system. Calling [setPose],
 * [setScale], or setting the [Entity.parent] property on the AnchorSpace will result in an
 * [UnsupportedOperationException].
 *
 * AnchorSpace users must maintain a strong reference while the instance is in use. If there's no
 * strong references to anchor instance in client code, anchor instance may become phantom
 * reachable, and it will be garbage collected.
 */
@SuppressLint("NewApi") // TODO: b/413661481 - Remove this suppression prior to JXR stable release.
public class AnchorSpace
private constructor(rtAnchorEntity: RtAnchorEntity, entityRegistry: EntityRegistry) :
    SpaceEntity(rtAnchorEntity, entityRegistry) {

    private val rtAnchorEntity: RtAnchorEntity
        get() = rtEntity as RtAnchorEntity

    private val onStateChangedListeners = ConsumerListenerMap<State>()
    private val onOriginChangedListeners = RunnableListenerMap()

    /** Asynchronous job responsible for finding a suitable plane to anchor this entity to. */
    private var planeFindingJob: Job? = null
    /**
     * Plane [Anchor] that was found semantically. This will be null if the AnchorSpace was created
     * with a previously created Anchor.
     */
    private var ownedAnchor: Anchor? = null

    /**
     * Returns the ARCore for Jetpack XR Anchor associated with this AnchorSpace.
     *
     * @return the ARCore for Jetpack XR Anchor associated with this AnchorSpace. This may be null
     *   if the AnchorSpace is still searching for a suitable anchor.
     */
    public var anchor: Anchor? = null
        get() {
            checkNotDisposed()
            return field
        }
        private set

    /** The current tracking state for this AnchorSpace. */
    public var state: State = fromRtState(rtAnchorEntity.state)
        private set(value) {
            // TODO: b/440191514 - On dispose, verify any pending anchor space ops are cancelled.
            field = value
            onStateChangedListeners.fire(value)
        }

    init {
        rtAnchorEntity.setOnOriginChangedListener(
            WeakRunnable(onOriginChangedListeners) { it.fire(Unit) },
            // Use the default executor for the rtEntity runtime callback. We fan out to the client
            // executors when the event fires.
            null,
        )
    }

    public class State private constructor(private val value: Int) {

        public companion object {
            /**
             * An AnchorSpace in the UNANCHORED state does not currently have a real-world pose that
             * is being actively updated. This is the default state while searching for an
             * anchorable position, and can also occur if the perception system has lost tracking of
             * the real-world location.
             */
            @JvmField public val UNANCHORED: State = State(0)

            /**
             * An AnchorSpace in the ANCHORED state is being actively tracked and updated by the
             * perception stack. Children of the AnchorSpace will maintain their relative
             * positioning to the system's best understanding of a pose in the real world.
             */
            @JvmField public val ANCHORED: State = State(1)

            /**
             * An AnchorSpace in the TIMED_OUT state indicates that the perception system timed out
             * while searching for an underlying anchorable position in the real world. The
             * AnchorSpace cannot recover from this state.
             */
            @JvmField public val TIMED_OUT: State = State(2)

            /**
             * An AnchorSpace in the ERROR state indicates that an unexpected error has occurred and
             * this AnchorSpace is invalid, without the possibility of recovery. Logcat may include
             * additional information about the error.
             */
            @JvmField public val ERROR: State = State(-1)
        }
    }

    internal data class PlaneFindingInfo(
        val dimensions: FloatSize2d,
        val orientations: Set<PlaneOrientation>,
        val semanticTypes: Set<PlaneSemanticType>,
        val searchDeadline: Long?,
    )

    private fun updateState(state: State) {
        if (state != this.state) {
            this.state = state
            when (state) {
                State.ANCHORED,
                State.TIMED_OUT -> {
                    planeFindingJob?.cancel()
                    planeFindingJob = null
                }
                State.ERROR -> {
                    ownedAnchor?.detach()
                }
            }
        }
    }

    public companion object {
        private fun getAnchorDeadline(anchorSearchTimeout: Duration?): Long? {
            // If the timeout is zero or null then we return null here and the anchor search will
            // continue indefinitely.
            if (anchorSearchTimeout == null || anchorSearchTimeout.isZero) {
                return null
            }
            return SystemClock.uptimeMillis() + anchorSearchTimeout.toMillis()
        }

        private fun findAndSetPlaneAnchor(
            session: Session,
            info: PlaneFindingInfo,
            anchorSpace: AnchorSpace,
        ) {
            val weakAnchorSpace = WeakReference(anchorSpace)
            anchorSpace.planeFindingJob =
                session.coroutineScope.launch {
                    Plane.subscribe(session).collect { planes ->
                        val entity = weakAnchorSpace.get() ?: return@collect
                        val timeNow = SystemClock.uptimeMillis()
                        if (info.searchDeadline != null && timeNow > info.searchDeadline) {
                            entity.updateState(State.TIMED_OUT)
                            this.cancel()
                            return@collect
                        }

                        val plane =
                            planes.firstOrNull {
                                val planeState = it.state.value
                                val planeOrientation = it.type.toSceneCoreOrientation()
                                val planeSemanticType = planeState.label.toSceneCoreSemanticType()
                                info.orientations.contains(planeOrientation) &&
                                    info.semanticTypes.contains(planeSemanticType) &&
                                    info.dimensions.width <= planeState.extents.width &&
                                    info.dimensions.height <= planeState.extents.height
                            }

                        if (plane != null && entity.state != State.ANCHORED) {
                            val anchorCreateResult = plane.createAnchor(Pose.Identity)
                            if (anchorCreateResult is AnchorCreateSuccess) {
                                val anchor = anchorCreateResult.anchor
                                if (entity.rtAnchorEntity.setAnchor(anchor)) {
                                    entity.anchor = anchor
                                    // Set the owned Anchor separately as it is being detached when
                                    // the Entity is in an Error state.
                                    entity.ownedAnchor = anchor
                                    entity.updateState(State.ANCHORED)
                                    this.cancel()
                                } else {
                                    anchor.detach()
                                }
                            }
                        }
                    }
                }
        }

        /**
         * Factory method for AnchorSpace.
         *
         * @param session Session to use.
         * @param entityRegistry [EntityRegistry] to use.
         * @param minimumPlaneExtents The minimum extents (in meters) of the plane to which this
         *   AnchorSpace should attach.
         * @param planeOrientations Orientation for the plane to which this Anchor should attach.
         * @param planeSemanticTypes Semantics for the plane to which this Anchor should attach.
         * @param timeout Maximum time to search for the anchor, if a suitable plane is not found
         *   within the timeout time the AnchorSpace state will be set to TIMED_OUT.
         */
        internal fun create(
            session: Session,
            entityRegistry: EntityRegistry,
            minimumPlaneExtents: FloatSize2d,
            planeOrientations: Set<PlaneOrientation>,
            planeSemanticTypes: Set<PlaneSemanticType>,
            timeout: Duration = Duration.ZERO,
        ): AnchorSpace {
            check(session.config.planeTracking != PlaneTrackingMode.DISABLED) {
                "Config.PlaneTrackingMode is set to Disabled."
            }

            val rtAnchorEntity = session.sceneRuntime.createAnchorEntity()
            val anchorSpace = AnchorSpace(rtAnchorEntity, entityRegistry)
            rtAnchorEntity.setOnStateChangedListener(
                WeakListener<AnchorSpace, Int>(anchorSpace) { entity, state ->
                    entity.updateState(entity.fromRtState(state))
                }::invoke
            )

            val info =
                PlaneFindingInfo(
                    minimumPlaneExtents,
                    planeOrientations,
                    planeSemanticTypes,
                    getAnchorDeadline(timeout),
                )
            findAndSetPlaneAnchor(session, info, anchorSpace)

            return anchorSpace
        }

        /**
         * Factory method for AnchorSpace.
         *
         * @param rtAnchorEntity Runtime AnchorEntity instance.
         */
        internal fun create(
            rtAnchorEntity: RtAnchorEntity,
            entityRegistry: EntityRegistry,
        ): AnchorSpace {
            val anchorSpace = AnchorSpace(rtAnchorEntity, entityRegistry)
            rtAnchorEntity.setOnStateChangedListener(
                WeakListener<AnchorSpace, Int>(anchorSpace) { entity, state ->
                    entity.updateState(entity.fromRtState(state))
                }::invoke
            )
            return anchorSpace
        }

        /**
         * Factory for an AnchorSpace which searches for a real-world surface on which to anchor,
         * from the set of tracked planes available to the perception system.
         *
         * @param session [Session] in which to create the AnchorSpace.
         * @param minimumPlaneExtents The minimum extents (in meters) of the plane to which this
         *   AnchorSpace should attach.
         * @param planeOrientation [PlaneOrientation] of the plane to which this AnchorSpace should
         *   attach.
         * @param planeSemanticType [PlaneSemanticType] of the plane to which this AnchorSpace
         *   should attach.
         * @param timeout The amount of time as a [Duration] to search for the suitable plane to
         *   attach to. If a plane is not found within the timeout, the returned AnchorSpace state
         *   will be set to AnchorSpace.State.TIMED_OUT. It may take longer than the timeout period
         *   before the anchor state is updated. If the timeout duration is zero it will search for
         *   the anchor indefinitely.
         * @throws [IllegalStateException] if [Session.config] is set to
         *   [PlaneTrackingMode.DISABLED].
         */
        @JvmStatic
        @JvmOverloads
        @Deprecated(
            "Use the factory which accepts Set<PlaneOrientation> and Set<PlaneSemanticType> instead."
        )
        // TODO: b/500464864 - Remove this factory method.
        @RestrictTo(Scope.LIBRARY_GROUP)
        public fun create(
            session: Session,
            minimumPlaneExtents: FloatSize2d,
            planeOrientation: PlaneOrientation,
            planeSemanticType: PlaneSemanticType,
            timeout: Duration = Duration.ZERO,
        ): AnchorSpace {
            return create(
                session,
                session.scene.entityRegistry,
                minimumPlaneExtents,
                setOf(planeOrientation),
                setOf(planeSemanticType),
                timeout,
            )
        }

        /**
         * Factory for an AnchorSpace which searches for a real-world surface on which to anchor,
         * from the set of tracked planes available to the perception system.
         *
         * @param session [Session] in which to create the AnchorSpace.
         * @param minimumPlaneExtents The minimum extents (in meters) of the plane to which this
         *   AnchorSpace should attach.
         * @param planeOrientations [PlaneOrientation]s of the plane to which this AnchorSpace
         *   should attach.
         * @param planeSemanticTypes [PlaneSemanticType]s of the plane to which this AnchorSpace
         *   should attach.
         * @param timeout The amount of time as a [Duration] to search for the suitable plane to
         *   attach to. If a plane is not found within the timeout, the returned AnchorSpace state
         *   will be set to AnchorSpace.State.TIMED_OUT. It may take longer than the timeout period
         *   before the anchor state is updated. If the timeout duration is zero it will search for
         *   the anchor indefinitely.
         * @throws [IllegalStateException] if [Session.config] is set to
         *   [PlaneTrackingMode.DISABLED].
         */
        @JvmStatic
        @JvmOverloads
        public fun create(
            session: Session,
            minimumPlaneExtents: FloatSize2d,
            planeOrientations: Set<PlaneOrientation>,
            planeSemanticTypes: Set<PlaneSemanticType>,
            timeout: Duration = Duration.ZERO,
        ): AnchorSpace {
            return create(
                session,
                session.scene.entityRegistry,
                minimumPlaneExtents,
                planeOrientations,
                planeSemanticTypes,
                timeout,
            )
        }

        /**
         * Public factory for an AnchorSpace which uses an [Anchor] from ARCore for Jetpack XR.
         *
         * @param session [Session] in which to create the AnchorSpace.
         * @param anchor The [Anchor] to use for this AnchorSpace.
         */
        @JvmStatic
        public fun create(session: Session, anchor: Anchor): AnchorSpace {
            val rtAnchorEntity = session.sceneRuntime.createAnchorEntity()
            val anchorSpace = AnchorSpace(rtAnchorEntity, session.scene.entityRegistry)
            anchorSpace.anchor = anchor
            rtAnchorEntity.setOnStateChangedListener(
                WeakListener<AnchorSpace, Int>(anchorSpace) { entity, state ->
                    entity.updateState(entity.fromRtState(state))
                }::invoke
            )
            rtAnchorEntity.setAnchor(anchor)
            return anchorSpace
        }
    }

    /** Converts [androidx.xr.scenecore.runtime.AnchorEntity.State] to [AnchorSpace.State]. */
    private fun fromRtState(state: Int): State =
        when (state) {
            RtAnchorEntity.State.UNANCHORED -> State.UNANCHORED
            RtAnchorEntity.State.ANCHORED -> State.ANCHORED
            RtAnchorEntity.State.TIMED_OUT -> State.TIMED_OUT
            RtAnchorEntity.State.ERROR -> State.ERROR
            else -> throw IllegalArgumentException("Unknown state: $state")
        }

    /**
     * Adds a listener to be invoked on the main thread when the AnchorSpace's state changes.
     *
     * The listener will fire on the main thread with the current [AnchorSpace.State] value
     * immediately upon registration. It will be automatically unregistered when the entity is
     * disposed.
     */
    public fun addStateChangedListener(listener: Consumer<State>): Unit =
        addStateChangedListener(HandlerExecutor.mainThreadExecutor, listener)

    /**
     * Adds a listener to be invoked on the given [Executor] when the AnchorSpace's state changes.
     *
     * The listener will fire with the current State value immediately upon registration. It will be
     * automatically unregistered when the entity is disposed.
     *
     * @param executor: The executor on which the specified listener will fire.
     * @param listener: The listener to fire upon invoking this method, and all subsequent state
     *   changes.
     */
    public fun addStateChangedListener(executor: Executor, listener: Consumer<State>) {
        checkNotDisposed()
        onStateChangedListeners.add(executor, listener)
        executor.execute { listener.accept(state) }
    }

    /**
     * Removes the given state changed listener.
     *
     * @param listener: The listener to be removed.
     */
    public fun removeStateChangedListener(listener: Consumer<State>) {
        checkNotDisposed()
        onStateChangedListeners.remove(listener)
    }

    /**
     * Adds a listener to be called when the [Anchor]'s origin moves relative to its underlying
     * space.
     *
     * The callback is triggered on the supplied [Executor] by any anchor movements, for example
     * when the perception system moves the anchor's origin to maintain the anchor's position
     * relative to the real world. Any cached data relative to the activity space or any other
     * "space" should be updated when this callback is triggered. It will be automatically
     * unregistered when the entity is disposed.
     *
     * @param executor The executor to run the listener on.
     * @param listener The listener to register.
     */
    public fun addOriginChangedListener(executor: Executor, listener: Runnable) {
        checkNotDisposed()
        onOriginChangedListeners.add(executor, listener)
    }

    /**
     * Adds a listener to be called when the [Anchor]'s origin moves relative to its underlying
     * space.
     *
     * The callback is triggered on the main thread by any anchor movements, for example when the
     * perception system moves the anchor's origin to maintain the anchor's position relative to the
     * real world. Any cached data relative to the activity space or any other "space" should be
     * updated when this callback is triggered. It will be automatically unregistered when the
     * entity is disposed.
     *
     * @param listener The listener to register. Events will fire on the main thread.
     */
    public fun addOriginChangedListener(listener: Runnable) {
        checkNotDisposed()
        onOriginChangedListeners.add(HandlerExecutor.mainThreadExecutor, listener)
    }

    /**
     * Removes the listener to be called when the [Anchor]'s origin moves relative to its underlying
     * space.
     *
     * @param listener The listener to remove.
     */
    public fun removeOriginChangedListener(listener: Runnable) {
        checkNotDisposed()
        onOriginChangedListeners.remove(listener)
    }

    override fun disposeInternal() {
        if (isDisposed) return
        onOriginChangedListeners.clear()
        onStateChangedListeners.clear()
        planeFindingJob?.cancel()
        rtAnchorEntity.setOnOriginChangedListener(null, null)
        rtAnchorEntity.setOnStateChangedListener(null)
        super.disposeInternal()
    }
}
