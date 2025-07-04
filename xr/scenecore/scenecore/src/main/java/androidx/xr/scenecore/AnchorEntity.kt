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

package androidx.xr.scenecore

import android.annotation.SuppressLint
import androidx.annotation.IntDef
import androidx.xr.arcore.Anchor
import androidx.xr.runtime.Config.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.AnchorEntity as RtAnchorEntity
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.math.FloatSize2d
import java.time.Duration
import java.util.UUID
import java.util.concurrent.Executor
import java.util.function.Consumer

/**
 * An AnchorEntity tracks a [androidx.xr.runtime.math.Pose] relative to some position or surface in
 * the "Real World." Children of this [Entity] will remain positioned relative to that location in
 * the real world, for the purposes of creating Augmented Reality experiences.
 *
 * Note that Anchors are only relative to the "real world", and not virtual environments. Also,
 * setting the [Entity.parent] property on an AnchorEntity has no effect, as the parenting of an
 * Anchor is controlled by the system.
 */
@Suppress("HiddenSuperclass") // TODO: b/427566816 - Fix HiddenSuperclass suppression
@SuppressLint("NewApi") // TODO: b/413661481 - Remove this suppression prior to JXR stable release.
public class AnchorEntity
private constructor(rtEntity: RtAnchorEntity, entityManager: EntityManager) :
    BaseEntity<RtAnchorEntity>(rtEntity, entityManager) {

    private var onStateChangedListener: Consumer<@StateValue Int>? = null
    private var onStateChangedExecutor: Executor = HandlerExecutor.mainThreadExecutor

    /** The current tracking state for this AnchorEntity. */
    public var state: @StateValue Int = rtEntity.state.fromRtState()
        private set(value) {
            field = value
            onStateChangedExecutor.execute { onStateChangedListener?.accept(value) }
        }

    /** Specifies the current tracking state of the Anchor. */
    @Target(AnnotationTarget.TYPE)
    @IntDef(State.ANCHORED, State.UNANCHORED, State.TIMEDOUT, State.ERROR)
    @Retention(AnnotationRetention.SOURCE)
    internal annotation class StateValue

    public object State {
        /**
         * An AnchorEntity in the ANCHORED stated is being actively tracked and updated by the
         * perception stack. Children of the AnchorEntity will maintain their relative positioning
         * to the system's best understanding of a pose in the real world.
         */
        public const val ANCHORED: Int = 0

        /**
         * An AnchorEntity in the UNANCHORED state does not currently have a real-world pose that is
         * being actively updated. This is the default state while searching for an anchorable
         * position, and can also occur if the perception system has lost tracking of the real-world
         * location.
         */
        public const val UNANCHORED: Int = 1

        /**
         * An AnchorEntity in the TIMEOUT state indicates that the perception system timed out while
         * searching for an underlying anchorable position in the real world. The AnchorEntity
         * cannot recover from this state.
         */
        public const val TIMEDOUT: Int = 2

        /**
         * An AnchorEntity in the ERROR state indicates that an unexpected error has occurred and
         * this AnchorEntity is invalid, without the possibility of recovery. Logcat may include
         * additional information about the error.
         */
        public const val ERROR: Int = 3
    }

    /**
     * Loads the ARCore for Jetpack XR Anchor using a Jetpack XR Runtime session.
     *
     * @param session the Jetpack XR Runtime session from which to load the Anchor.
     * @return the ARCore for Jetpack XR Anchor corresponding to the native pointer.
     */
    // TODO(b/373711152) : Remove this method once the ARCore for XR API migration is done.
    public fun getAnchor(session: Session): Anchor {
        return Anchor.loadFromNativePointer(session, rtEntity.nativePointer)
    }

    public companion object {
        /**
         * Factory method for AnchorEntity.
         *
         * @param adapter JxrPlatformAdapter to use.
         * @param minimumPlaneExtents The minimum extents (in meters) of the plane to which this
         *   AnchorEntity should attach.
         * @param planeType Orientation for the plane to which this Anchor should attach.
         * @param planeSemantic Semantics for the plane to which this Anchor should attach.
         * @param timeout Maximum time to search for the anchor, if a suitable plane is not found
         *   within the timeout time the AnchorEntity state will be set to TIMED_OUT.
         */
        internal fun create(
            adapter: JxrPlatformAdapter,
            entityManager: EntityManager,
            minimumPlaneExtents: FloatSize2d,
            planeType: @PlaneOrientationValue Int,
            planeSemantic: @PlaneSemanticTypeValue Int,
            timeout: Duration = Duration.ZERO,
        ): AnchorEntity {
            val rtAnchorEntity =
                adapter.createAnchorEntity(
                    minimumPlaneExtents.to3d().toRtDimensions(),
                    planeType.toRtPlaneType(),
                    planeSemantic.toRtPlaneSemantic(),
                    timeout,
                )
            return create(rtAnchorEntity, entityManager)
        }

        /**
         * Factory method for AnchorEntity.
         *
         * @param anchor Anchor to create an AnchorEntity for.
         */
        internal fun create(
            adapter: JxrPlatformAdapter,
            entityManager: EntityManager,
            anchor: Anchor,
        ): AnchorEntity = create(adapter.createAnchorEntity(anchor.runtimeAnchor), entityManager)

        /**
         * Factory method for AnchorEntity.
         *
         * @param rtAnchorEntity Runtime AnchorEntity instance.
         */
        internal fun create(
            rtAnchorEntity: RtAnchorEntity,
            entityManager: EntityManager,
        ): AnchorEntity {
            val anchorEntity = AnchorEntity(rtAnchorEntity, entityManager)
            rtAnchorEntity.setOnStateChangedListener { newRtState ->
                when (newRtState) {
                    RtAnchorEntity.State.UNANCHORED -> anchorEntity.state = State.UNANCHORED
                    RtAnchorEntity.State.ANCHORED -> anchorEntity.state = State.ANCHORED
                    RtAnchorEntity.State.TIMED_OUT -> anchorEntity.state = State.TIMEDOUT
                    RtAnchorEntity.State.ERROR -> anchorEntity.state = State.ERROR
                }
            }
            return anchorEntity
        }

        /**
         * Factory method for AnchorEntity.
         *
         * @param adapter JxrPlatformAdapter to use.
         * @param uuid UUID of the persisted Anchor Entity to create.
         * @param timeout Maximum time to search for the anchor, if a persisted anchor isn't located
         *   within the timeout time the AnchorEntity state will be set to TIMED_OUT.
         */
        internal fun create(
            adapter: JxrPlatformAdapter,
            entityManager: EntityManager,
            uuid: UUID,
            timeout: Duration = Duration.ZERO,
        ): AnchorEntity {
            val rtAnchorEntity = adapter.createPersistedAnchorEntity(uuid, timeout)
            val anchorEntity = AnchorEntity(rtAnchorEntity, entityManager)
            rtAnchorEntity.setOnStateChangedListener { newRtState ->
                when (newRtState) {
                    RtAnchorEntity.State.UNANCHORED -> anchorEntity.state = State.UNANCHORED
                    RtAnchorEntity.State.ANCHORED -> anchorEntity.state = State.ANCHORED
                    RtAnchorEntity.State.TIMED_OUT -> anchorEntity.state = State.TIMEDOUT
                    RtAnchorEntity.State.ERROR -> anchorEntity.state = State.ERROR
                }
            }
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
         * @param timeout The amount of time as a [Duration] to search for the a suitable plane to
         *   attach to. If a plane is not found within the timeout, the returned AnchorEntity state
         *   will be set to AnchorEntity.State.TIMEDOUT. It may take longer than the timeout period
         *   before the anchor state is updated. If the timeout duration is zero it will search for
         *   the anchor indefinitely.
         * @throws [IllegalStateException] if [session.config.planeTracking] is set to
         *   [PlaneTrackingMode.DISABLED].
         */
        @JvmStatic
        @JvmOverloads
        public fun create(
            session: Session,
            minimumPlaneExtents: FloatSize2d,
            planeOrientation: @PlaneOrientationValue Int,
            planeSemanticType: @PlaneSemanticTypeValue Int,
            timeout: Duration = Duration.ZERO,
        ): AnchorEntity {
            check(session.config.planeTracking != PlaneTrackingMode.DISABLED) {
                "Config.PlaneTrackingMode is set to Disabled."
            }

            return create(
                session.platformAdapter,
                session.scene.entityManager,
                minimumPlaneExtents,
                planeOrientation,
                planeSemanticType,
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
            return create(session.platformAdapter, session.scene.entityManager, anchor)
        }
    }

    /** Extension function that converts [RtAnchorEntity.State] to [AnchorEntity.State]. */
    private fun Int.fromRtState() =
        when (this) {
            RtAnchorEntity.State.UNANCHORED -> State.UNANCHORED
            RtAnchorEntity.State.ANCHORED -> State.ANCHORED
            RtAnchorEntity.State.TIMED_OUT -> State.TIMEDOUT
            RtAnchorEntity.State.ERROR -> State.ERROR
            else -> throw IllegalArgumentException("Unknown state: $this")
        }

    /**
     * Registers a listener to be invoked on the main thread when the AnchorEntity's state changes,
     * or unregisters the current listener if set to null.
     *
     * The listener will fire with the current [AnchorEntity.State] value immediately upon
     * registration.
     */
    public fun setOnStateChangedListener(listener: Consumer<@StateValue Int>?) {
        setOnStateChangedListener(HandlerExecutor.mainThreadExecutor, listener)
    }

    /**
     * Registers a listener to be invoked on the given [Executor] when the AnchorEntity's state
     * changes, or unregisters the current listener if set to null.
     *
     * The listener will fire with the current State value immediately upon registration.
     *
     * @param executor: The executor on which the specified listener will fire.
     * @param listener: The listener to fire upon invoking this method, and all subsequent state
     *   changes.
     */
    public fun setOnStateChangedListener(executor: Executor, listener: Consumer<@StateValue Int>?) {
        onStateChangedListener = listener
        onStateChangedExecutor = executor
        executor.execute { listener?.accept(state) }
    }

    /**
     * Registers a listener to be called when the [Anchor] moves relative to its underlying space.
     *
     * The callback is triggered on the supplied [Executor] by any anchor movements such as those
     * made by the underlying perception stack to maintain the anchor's position relative to the
     * real world. Any cached data relative to the activity space or any other "space" should be
     * updated when this callback is triggered.
     *
     * @param executor The executor to run the listener on.
     * @param listener The listener to register if non-null, else stops listening if null.
     */
    public fun setOnSpaceUpdatedListener(executor: Executor, listener: Runnable?) {
        rtEntity.setOnSpaceUpdatedListener(listener, executor)
    }

    /**
     * Registers a listener to be called when the [Anchor] moves relative to its underlying space.
     *
     * The callback is triggered on the default SceneCore [Executor] by any anchor movements such as
     * those made by the underlying perception stack to maintain the anchor's position relative to
     * the real world. Any cached data relative to the activity space or any other "space" should be
     * updated when this callback is triggered.
     *
     * @param listener The listener to register if non-null, else stops listening if null.
     */
    public fun setOnSpaceUpdatedListener(listener: Runnable?): Unit =
        rtEntity.setOnSpaceUpdatedListener(listener, null)
}
