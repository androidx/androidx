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

@file:Suppress("BanConcurrentHashMap")

package androidx.xr.scenecore

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.ActivitySpace as RtActivitySpace
import androidx.xr.scenecore.runtime.SceneRuntime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.Executor
import java.util.function.Consumer

/**
 * ActivitySpace is an [Entity] used to track the system-managed pose and boundary of the volume
 * associated with a spatialized Activity. The Application cannot directly control this volume, but
 * the system might update it in response to the User moving it or entering or exiting Full Space
 * Mode.
 */
// TODO: b/440429756 - Define dispose policy for SceneCore singletons like main panel and system
// spaces.
public class ActivitySpace
private constructor(rtActivitySpace: RtActivitySpace, entityManager: EntityManager) :
    BaseEntity<RtActivitySpace>(rtActivitySpace, entityManager) {

    internal companion object {
        internal fun create(
            sceneRuntime: SceneRuntime,
            entityManager: EntityManager,
        ): ActivitySpace = ActivitySpace(sceneRuntime.activitySpace, entityManager)
    }

    private val boundsListeners:
        ConcurrentMap<Consumer<FloatSize3d>, RtActivitySpace.OnBoundsChangedListener> =
        ConcurrentHashMap()

    private val originChangedListeners: ConcurrentMap<Runnable, Executor?> = ConcurrentHashMap()

    private val rtOriginChangedListener = {
        for ((listener, executor) in originChangedListeners.entries) {
            if (executor == null) {
                // The rtListener requested the default executor, so we can directly invoke.
                listener.run()
            } else {
                executor.execute { listener.run() }
            }
        }
    }

    /** The current bounds of this ActivitySpace. */
    public val bounds: FloatSize3d
        get() {
            checkNotDisposed()
            return rtEntity!!.bounds.toFloatSize3d()
        }

    /**
     * Adds the given [Consumer] as a listener to be invoked when this ActivitySpace's current
     * boundary changes.
     *
     * [Consumer.accept] will be invoked on the main thread.
     *
     * @param listener The Consumer to be invoked when this ActivitySpace's current boundary
     *   changes.
     */
    public fun addOnBoundsChangedListener(listener: Consumer<FloatSize3d>): Unit =
        addOnBoundsChangedListener(HandlerExecutor.mainThreadExecutor, listener)

    /**
     * Adds the given [Consumer] as a listener to be invoked when this ActivitySpace's current
     * boundary changes.
     *
     * [Consumer.accept] will be invoked on the given executor.
     *
     * @param callbackExecutor The executor on which to invoke the listener on.
     * @param listener The Consumer to be invoked when this ActivitySpace's current boundary
     *   changes.
     */
    public fun addOnBoundsChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<FloatSize3d>,
    ) {
        checkNotDisposed()
        val rtListener: RtActivitySpace.OnBoundsChangedListener =
            RtActivitySpace.OnBoundsChangedListener { rtDimensions ->
                callbackExecutor.execute { listener.accept(rtDimensions.toFloatSize3d()) }
            }
        boundsListeners.compute(listener) { _, _ ->
            rtEntity!!.addOnBoundsChangedListener(rtListener)
            rtListener
        }
    }

    /**
     * Releases the given [Consumer] from receiving updates when the ActivitySpace's boundary
     * changes.
     *
     * All listeners are automatically removed when the ActivitySpace is disposed even if this
     * method is not explicitly called.
     *
     * @param listener The Consumer to be removed from receiving updates.
     */
    public fun removeOnBoundsChangedListener(listener: Consumer<FloatSize3d>) {
        checkNotDisposed()
        boundsListeners.computeIfPresent(listener) { _, rtListener ->
            rtEntity!!.removeOnBoundsChangedListener(rtListener)
            null // returning null from computeIfPresent removes this entry from the Map
        }
    }

    /**
     * Adds a listener to be called when the ActivitySpace's origin has moved or changed, typically
     * due to an internal system event.
     *
     * When this event occurs, any [ScenePose] that is not a child of ActivitySpace, such as
     * [AnchorEntity] and [CameraView], will have a different position relative to the
     * [ActivitySpace]. Therefore, this listener can be used to indicate when to invalidate any
     * cached information about the relative difference in Pose between ActivitySpace's children and
     * children of non-ActivitySpace ScenePoses.
     *
     * @param listener The listener to register.
     * @param executor The [Executor] on which to run the listener.
     */
    public fun addOnOriginChangedListener(executor: Executor, listener: Runnable) {
        checkNotDisposed()
        val addRtListener = originChangedListeners.isEmpty()
        originChangedListeners[listener] = executor
        if (addRtListener) {
            rtEntity!!.setOnOriginChangedListener(rtOriginChangedListener, null)
        }
    }

    /**
     * Adds a listener to be called when the ActivitySpace's origin has moved or changed, typically
     * due to an internal system event.
     *
     * When this event occurs, any [ScenePose] that is not a child of ActivitySpace, such as
     * [AnchorEntity] and [CameraView], will have a different position relative to the
     * [ActivitySpace]. Therefore, this listener can be used to indicate when to invalidate any
     * cached information about the relative difference in Pose between ActivitySpace's children and
     * children of non-ActivitySpace ScenePoses.
     *
     * The callback will be made on the SceneCore executor.
     *
     * @param listener The listener to register.
     */
    public fun addOnOriginChangedListener(listener: Runnable): Unit =
        addOnOriginChangedListener(DirectExecutor, listener)

    @Deprecated(
        "Use addOnOriginChangedListener",
        replaceWith = ReplaceWith("addOnOriginChangedListener()"),
    )
    public fun addOnSpaceUpdatedListener(executor: Executor, listener: Runnable) {
        addOnOriginChangedListener(executor, listener)
    }

    @Deprecated(
        "Use addOnOriginChangedListener",
        replaceWith = ReplaceWith("addOnOriginChangedListener()"),
    )
    public fun addOnSpaceUpdatedListener(listener: Runnable) {
        addOnOriginChangedListener(listener)
    }

    /**
     * Removes the previously-added listener.
     *
     * All listeners are automatically removed when the ActivitySpace is disposed even if this
     * method is not explicitly called.
     */
    public fun removeOnOriginChangedListener(listener: Runnable) {
        checkNotDisposed()
        originChangedListeners.remove(listener)
        if (originChangedListeners.isEmpty()) {
            rtEntity!!.setOnOriginChangedListener(null, null)
        }
    }

    @Deprecated(
        "Use removeOnOriginChangedListener",
        replaceWith = ReplaceWith("removeOnOriginChangedListener()"),
    )
    public fun removeOnSpaceUpdatedListener(listener: Runnable) {
        removeOnOriginChangedListener(listener)
    }

    /**
     * A recommended box for content to be placed in when in Full Space Mode.
     *
     * The box is relative to the ActivitySpace's coordinate system. It is not scaled by the
     * ActivitySpace's transform. The dimensions are always in meters. This provides a
     * device-specific default volume that developers can use to size their content appropriately.
     */
    public val recommendedContentBoxInFullSpace: BoundingBox
        get() {
            checkNotDisposed()
            return rtEntity!!.recommendedContentBoxInFullSpace
        }

    /**
     * Throws [UnsupportedOperationException] if called.
     *
     * **Note:** The pose of the `ActivitySpace` is managed by the system. Applications should not
     * call this method, as any changes may be overwritten by the system.
     *
     * @param pose The new pose to set.
     * @param relativeTo The space in which the pose is defined.
     * @throws UnsupportedOperationException if called.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun setPose(pose: Pose, relativeTo: Space) {
        checkNotDisposed()
        throw UnsupportedOperationException("Cannot set 'pose' on an ActivitySpace.")
    }

    /**
     * Returns the pose of the `ActivitySpace` relative to the specified coordinate space.
     *
     * @param relativeTo The coordinate space to get the pose relative to. Defaults to
     *   [Space.PARENT].
     * @return The current pose of the `ActivitySpace`.
     * @throws IllegalArgumentException if called with Space.PARENT since ActivitySpace has no
     *   parents.
     */
    override fun getPose(relativeTo: Space): Pose {
        checkNotDisposed()
        return when (relativeTo) {
            Space.PARENT ->
                throw IllegalArgumentException(
                    "ActivitySpace is a root space and it does not have a parent."
                )
            Space.ACTIVITY,
            Space.REAL_WORLD -> super.getPose(relativeTo)
            else -> throw IllegalArgumentException("Unsupported relativeTo value: $relativeTo")
        }
    }

    /**
     * Throws [UnsupportedOperationException] if called.
     *
     * **Note:** The scale of the `ActivitySpace` is managed by the system. Applications should not
     * call this method, as any changes may be overwritten by the system.
     *
     * @param scale The new scale to set.
     * @param relativeTo The space in which the scale is defined.
     * @throws UnsupportedOperationException if called.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun setScale(scale: Float, relativeTo: Space) {
        checkNotDisposed()
        throw UnsupportedOperationException("Cannot set 'scale' on an ActivitySpace.")
    }

    /**
     * Throws [UnsupportedOperationException] if called.
     *
     * **Note:** The scale of the `ActivitySpace` is managed by the system. Applications should not
     * call this method, as any changes may be overwritten by the system.
     *
     * @param scale The new scale to set.
     * @param relativeTo The space in which the scale is defined.
     * @throws UnsupportedOperationException if called.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun setScale(scale: Vector3, relativeTo: Space) {
        throw UnsupportedOperationException("Cannot set 'scale' on an ActivitySpace.")
    }

    /**
     * Returns the scale of the `ActivitySpace` along each axis, relative to the specified
     * coordinate space.
     *
     * @param relativeTo The coordinate space to get the scale relative to. Defaults to
     *   [Space.PARENT].
     * @return The current scale of the `ActivitySpace` along each axis.
     * @throws IllegalArgumentException if called with Space.PARENT since ActivitySpace has no
     *   parents.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun getNonUniformScale(relativeTo: Space): Vector3 {
        checkNotDisposed()
        return when (relativeTo) {
            Space.PARENT ->
                throw IllegalArgumentException(
                    "ActivitySpace is a root space and it does not have a parent."
                )
            Space.ACTIVITY,
            Space.REAL_WORLD -> super.getNonUniformScale(relativeTo)
            else -> throw IllegalArgumentException("Unsupported relativeTo value: $relativeTo")
        }
    }

    /**
     * Returns the scale of the `ActivitySpace` relative to the specified coordinate space.
     *
     * @param relativeTo The coordinate space to get the scale relative to. Defaults to
     *   [Space.PARENT].
     * @return The current scale of the `ActivitySpace`.
     * @throws IllegalArgumentException if called with Space.PARENT since ActivitySpace has no
     *   parents.
     */
    override fun getScale(relativeTo: Space): Float {
        checkNotDisposed()
        return when (relativeTo) {
            Space.PARENT ->
                throw IllegalArgumentException(
                    "ActivitySpace is a root space and it does not have a parent."
                )
            Space.ACTIVITY,
            Space.REAL_WORLD -> super.getScale(relativeTo)
            else -> throw IllegalArgumentException("Unsupported relativeTo value: $relativeTo")
        }
    }

    override fun dispose() {
        boundsListeners.keys.forEach { removeOnBoundsChangedListener(it) }
        originChangedListeners.keys.forEach { removeOnOriginChangedListener(it) }
        super.dispose()
    }
}
