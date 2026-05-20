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
import androidx.xr.runtime.math.Vector3
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
 * An AnchorEntity tracks a [Pose] relative to some position or surface in the "Real World."
 * Children of this [Entity] will remain positioned relative to that location in the real world, for
 * the purposes of creating Augmented Reality experiences.
 *
 * Note that Anchors are only relative to the "real world", and not virtual environments. Also,
 * setting the [Entity.parent] property on an AnchorEntity has no effect, as the parenting of an
 * Anchor is controlled by the system.
 *
 * AnchorEntity users must maintain a strong reference while the instance is in use. If there's no
 * strong references to anchor instance in client code, anchor instance may become phantom
 * reachable, and it will be garbage collected.
 */
@SuppressLint("NewApi") // TODO: b/413661481 - Remove this suppression prior to JXR stable release.
public class AnchorEntity
private constructor(rtAnchorEntity: RtAnchorEntity, entityRegistry: EntityRegistry) :
    Entity(rtAnchorEntity, entityRegistry) {

    private val rtAnchorEntity: RtAnchorEntity
        get() = rtEntity as RtAnchorEntity

    private val onStateChangedListeners = ConsumerListenerMap<State>()
    private val onOriginChangedListeners = RunnableListenerMap()

    /** Asynchronous job responsible for finding a suitable plane to anchor this entity to. */
    private var planeFindingJob: Job? = null
    /**
     * Plane [Anchor] that was found semantically. This will be null if the AnchorEntity was created
     * with a previously created Anchor.
     */
    private var ownedAnchor: Anchor? = null

    /**
     * Returns the ARCore for Jetpack XR Anchor associated with this AnchorEntity.
     *
     * @return the ARCore for Jetpack XR Anchor associated with this AnchorEntity. This may be null
     *   if the AnchorEntity is still searching for a suitable anchor.
     */
    public var anchor: Anchor? = null
        get() {
            checkNotDisposed()
            return field
        }
        private set

    /** The current tracking state for this AnchorEntity. */
    public var state: State = fromRtState(rtAnchorEntity.state)
        private set(value) {
            // TODO: b/440191514 - On dispose, verify any pending anchor entity ops are cancelled.
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
             * An AnchorEntity in the UNANCHORED state does not currently have a real-world pose
             * that is being actively updated. This is the default state while searching for an
             * anchorable position, and can also occur if the perception system has lost tracking of
             * the real-world location.
             */
            @JvmField public val UNANCHORED: State = State(0)

            /**
             * An AnchorEntity in the ANCHORED state is being actively tracked and updated by the
             * perception stack. Children of the AnchorEntity will maintain their relative
             * positioning to the system's best understanding of a pose in the real world.
             */
            @JvmField public val ANCHORED: State = State(1)

            /**
             * An AnchorEntity in the TIMED_OUT state indicates that the perception system timed out
             * while searching for an underlying anchorable position in the real world. The
             * AnchorEntity cannot recover from this state.
             */
            @JvmField public val TIMED_OUT: State = State(2)

            /**
             * An AnchorEntity in the ERROR state indicates that an unexpected error has occurred
             * and this AnchorEntity is invalid, without the possibility of recovery. Logcat may
             * include additional information about the error.
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
            anchorEntity: AnchorEntity,
        ) {
            val weakAnchorEntity = WeakReference(anchorEntity)
            anchorEntity.planeFindingJob =
                session.coroutineScope.launch {
                    Plane.subscribe(session).collect { planes ->
                        val entity = weakAnchorEntity.get() ?: return@collect
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
         * Factory method for AnchorEntity.
         *
         * @param session Session to use.
         * @param entityRegistry [EntityRegistry] to use.
         * @param minimumPlaneExtents The minimum extents (in meters) of the plane to which this
         *   AnchorEntity should attach.
         * @param planeOrientations Orientation for the plane to which this Anchor should attach.
         * @param planeSemanticTypes Semantics for the plane to which this Anchor should attach.
         * @param timeout Maximum time to search for the anchor, if a suitable plane is not found
         *   within the timeout time the AnchorEntity state will be set to TIMED_OUT.
         */
        internal fun create(
            session: Session,
            entityRegistry: EntityRegistry,
            minimumPlaneExtents: FloatSize2d,
            planeOrientations: Set<PlaneOrientation>,
            planeSemanticTypes: Set<PlaneSemanticType>,
            timeout: Duration = Duration.ZERO,
        ): AnchorEntity {
            check(session.config.planeTracking != PlaneTrackingMode.DISABLED) {
                "Config.PlaneTrackingMode is set to Disabled."
            }

            val rtAnchorEntity = session.sceneRuntime.createAnchorEntity()
            val anchorEntity = AnchorEntity(rtAnchorEntity, entityRegistry)
            rtAnchorEntity.setOnStateChangedListener(
                WeakListener<AnchorEntity, Int>(anchorEntity) { entity, state ->
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
            findAndSetPlaneAnchor(session, info, anchorEntity)

            return anchorEntity
        }

        /**
         * Factory method for AnchorEntity.
         *
         * @param rtAnchorEntity Runtime AnchorEntity instance.
         */
        internal fun create(
            rtAnchorEntity: RtAnchorEntity,
            entityRegistry: EntityRegistry,
        ): AnchorEntity {
            val anchorEntity = AnchorEntity(rtAnchorEntity, entityRegistry)
            rtAnchorEntity.setOnStateChangedListener(
                WeakListener<AnchorEntity, Int>(anchorEntity) { entity, state ->
                    entity.updateState(entity.fromRtState(state))
                }::invoke
            )
            return anchorEntity
        }

        /**
         * Factory for an AnchorEntity which searches for a real-world surface on which to anchor,
         * from the set of tracked planes available to the perception system.
         *
         * @param session [Session] in which to create the AnchorEntity.
         * @param minimumPlaneExtents The minimum extents (in meters) of the plane to which this
         *   AnchorEntity should attach.
         * @param planeOrientation [PlaneOrientation] of the plane to which this AnchorEntity should
         *   attach.
         * @param planeSemanticType [PlaneSemanticType] of the plane to which this AnchorEntity
         *   should attach.
         * @param timeout The amount of time as a [Duration] to search for the suitable plane to
         *   attach to. If a plane is not found within the timeout, the returned AnchorEntity state
         *   will be set to AnchorEntity.State.TIMED_OUT. It may take longer than the timeout period
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
        ): AnchorEntity {
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
         * Factory for an AnchorEntity which searches for a real-world surface on which to anchor,
         * from the set of tracked planes available to the perception system.
         *
         * @param session [Session] in which to create the AnchorEntity.
         * @param minimumPlaneExtents The minimum extents (in meters) of the plane to which this
         *   AnchorEntity should attach.
         * @param planeOrientations [PlaneOrientation]s of the plane to which this AnchorEntity
         *   should attach.
         * @param planeSemanticTypes [PlaneSemanticType]s of the plane to which this AnchorEntity
         *   should attach.
         * @param timeout The amount of time as a [Duration] to search for the suitable plane to
         *   attach to. If a plane is not found within the timeout, the returned AnchorEntity state
         *   will be set to AnchorEntity.State.TIMED_OUT. It may take longer than the timeout period
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
        ): AnchorEntity {
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
         * Public factory for an AnchorEntity which uses an [Anchor] from ARCore for Jetpack XR.
         *
         * @param session [Session] in which to create the AnchorEntity.
         * @param anchor The [Anchor] to use for this AnchorEntity.
         */
        @JvmStatic
        public fun create(session: Session, anchor: Anchor): AnchorEntity {
            val rtAnchorEntity = session.sceneRuntime.createAnchorEntity()
            val anchorEntity = AnchorEntity(rtAnchorEntity, session.scene.entityRegistry)
            anchorEntity.anchor = anchor
            rtAnchorEntity.setOnStateChangedListener(
                WeakListener<AnchorEntity, Int>(anchorEntity) { entity, state ->
                    entity.updateState(entity.fromRtState(state))
                }::invoke
            )
            rtAnchorEntity.setAnchor(anchor)
            return anchorEntity
        }
    }

    /** Converts [androidx.xr.scenecore.runtime.AnchorEntity.State] to [AnchorEntity.State]. */
    private fun fromRtState(state: Int): State =
        when (state) {
            RtAnchorEntity.State.UNANCHORED -> State.UNANCHORED
            RtAnchorEntity.State.ANCHORED -> State.ANCHORED
            RtAnchorEntity.State.TIMED_OUT -> State.TIMED_OUT
            RtAnchorEntity.State.ERROR -> State.ERROR
            else -> throw IllegalArgumentException("Unknown state: $state")
        }

    /**
     * Adds a listener to be invoked on the main thread when the AnchorEntity's state changes.
     *
     * The listener will fire on the main thread with the current [AnchorEntity.State] value
     * immediately upon registration. It will be automatically unregistered when the entity is
     * disposed.
     */
    public fun addStateChangedListener(listener: Consumer<State>): Unit =
        addStateChangedListener(HandlerExecutor.mainThreadExecutor, listener)

    /**
     * Adds a listener to be invoked on the given [Executor] when the AnchorEntity's state changes.
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

    /**
     * Throws [UnsupportedOperationException] if called.
     *
     * **Note:** The pose of the `AnchorEntity` is managed by the system. Applications should not
     * call this method, as any changes may be overwritten by the system.
     *
     * @param pose The new pose to set.
     * @param relativeTo The space in which the pose is defined.
     * @throws UnsupportedOperationException if called.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    override fun setPose(pose: Pose, relativeTo: Space) {
        checkNotDisposed()
        throw UnsupportedOperationException("Cannot set 'pose' on an AnchorEntity.")
    }

    /**
     * Returns the pose of the `AnchorEntity` relative to the specified coordinate space.
     *
     * @param relativeTo The coordinate space to get the pose relative to. Defaults to
     *   [Space.PARENT].
     * @return The current pose of the `AnchorEntity`.
     * @throws IllegalArgumentException if called with Space.PARENT since AnchorEntity has no
     *   parents.
     */
    override fun getPose(relativeTo: Space): Pose {
        checkNotDisposed()
        return when (relativeTo) {
            Space.PARENT ->
                throw IllegalArgumentException(
                    "AnchorEntity is a root space and it does not have a parent."
                )
            Space.ACTIVITY,
            @Suppress("DEPRECATION") // TODO - b/415320653
            Space.REAL_WORLD -> super.getPose(relativeTo)
            else -> throw IllegalArgumentException("Unsupported relativeTo value: $relativeTo")
        }
    }

    /**
     * Throws [UnsupportedOperationException] if called.
     *
     * **Note:** The scale of the `AnchorEntity` is managed by the system. Applications should not
     * call this method, as any changes may be overwritten by the system.
     *
     * @param scale The new scale to set.
     * @param relativeTo The space in which the scale is defined.
     * @throws UnsupportedOperationException if called.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    override fun setScale(scale: Float, relativeTo: Space) {
        checkNotDisposed()
        throw UnsupportedOperationException("Cannot set 'scale' on an AnchorEntity.")
    }

    /**
     * Throws [UnsupportedOperationException] if called.
     *
     * **Note:** The scale of the `AnchorEntity` is managed by the system. Applications should not
     * call this method, as any changes may be overwritten by the system.
     *
     * @param scale The new scale to set.
     * @param relativeTo The space in which the scale is defined.
     * @throws UnsupportedOperationException if called.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    override fun setScale(scale: Vector3, relativeTo: Space) {
        throw UnsupportedOperationException("Cannot set 'scale' on an AnchorEntity.")
    }

    /**
     * Returns the scale of the `AnchorEntity` along each axis, relative to the specified coordinate
     * space.
     *
     * @param relativeTo The coordinate space to get the scale relative to. Defaults to
     *   [Space.PARENT].
     * @return The current scale of the `AnchorEntity` along each axis.
     * @throws IllegalArgumentException if called with Space.PARENT since AnchorEntity has no
     *   parents.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    override fun getNonUniformScale(relativeTo: Space): Vector3 {
        checkNotDisposed()
        return when (relativeTo) {
            Space.PARENT ->
                throw IllegalArgumentException(
                    "AnchorEntity is a root space and it does not have a parent."
                )
            Space.ACTIVITY,
            @Suppress("DEPRECATION") // TODO - b/415320653
            Space.REAL_WORLD -> super.getNonUniformScale(relativeTo)
            else -> throw IllegalArgumentException("Unsupported relativeTo value: $relativeTo")
        }
    }

    /**
     * Returns the scale of the `AnchorEntity` relative to the specified coordinate space.
     *
     * @param relativeTo The coordinate space to get the scale relative to. Defaults to
     *   [Space.PARENT].
     * @return The current scale of the `AnchorEntity`.
     * @throws IllegalArgumentException if called with Space.PARENT since AnchorEntity has no
     *   parents.
     */
    override fun getScale(relativeTo: Space): Float {
        checkNotDisposed()
        return when (relativeTo) {
            Space.PARENT ->
                throw IllegalArgumentException(
                    "AnchorEntity is a root space and it does not have a parent."
                )
            Space.ACTIVITY,
            @Suppress("DEPRECATION") // TODO - b/415320653: Space.REAL_WORLD
            Space.REAL_WORLD -> super.getScale(relativeTo)
            else -> throw IllegalArgumentException("Unsupported relativeTo value: $relativeTo")
        }
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
