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

@file:Suppress("BanConcurrentHashMap")

package androidx.xr.scenecore

import androidx.xr.runtime.internal.ActivitySpace as RtActivitySpace
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.FloatSize3d
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
@Suppress("HiddenSuperclass")
public class ActivitySpace
private constructor(rtActivitySpace: RtActivitySpace, entityManager: EntityManager) :
    BaseEntity<RtActivitySpace>(rtActivitySpace, entityManager) {

    internal companion object {
        internal fun create(
            adapter: JxrPlatformAdapter,
            entityManager: EntityManager,
        ): ActivitySpace = ActivitySpace(adapter.activitySpace, entityManager)
    }

    private val boundsListeners:
        ConcurrentMap<Consumer<FloatSize3d>, RtActivitySpace.OnBoundsChangedListener> =
        ConcurrentHashMap()

    private val spaceUpdatedListeners: ConcurrentMap<Runnable, Executor?> = ConcurrentHashMap()

    private val rtSpaceUpdatedListener = {
        for ((listener, executor) in spaceUpdatedListeners.entries) {
            if (executor == null) {
                // The rtListener requested the default executor, so we can directly invoke.
                listener.run()
            } else {
                executor.execute { listener.run() }
            }
        }
    }

    /** The current bounds of this ActivitySpace. */
    public var bounds: FloatSize3d
        get() = rtEntity.bounds.toFloatSize3d()
        private set(value) {} // not used, but required by the compiler

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
        val rtListener: RtActivitySpace.OnBoundsChangedListener =
            RtActivitySpace.OnBoundsChangedListener { rtDimensions ->
                callbackExecutor.execute { listener.accept(rtDimensions.toFloatSize3d()) }
            }
        boundsListeners.compute(listener) { _, _ ->
            rtEntity.addOnBoundsChangedListener(rtListener)
            rtListener
        }
    }

    /**
     * Releases the given [Consumer] from receiving updates when the ActivitySpace's boundary
     * changes.
     *
     * @param listener The Consumer to be removed from receiving updates.
     */
    public fun removeOnBoundsChangedListener(listener: Consumer<FloatSize3d>) {
        boundsListeners.computeIfPresent(listener) { _, rtListener ->
            rtEntity.removeOnBoundsChangedListener(rtListener)
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
    public fun addOnSpaceUpdatedListener(executor: Executor, listener: Runnable) {
        val addRtListener = spaceUpdatedListeners.isEmpty()
        spaceUpdatedListeners.put(listener, executor)
        if (addRtListener) {
            rtEntity.setOnSpaceUpdatedListener(rtSpaceUpdatedListener, null)
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
    public fun addOnSpaceUpdatedListener(listener: Runnable): Unit =
        addOnSpaceUpdatedListener(DirectExecutor, listener)

    /** Removes the previously-added listener. */
    public fun removeOnSpaceUpdatedListener(listener: Runnable) {
        spaceUpdatedListeners.remove(listener)
        if (spaceUpdatedListeners.isEmpty()) {
            rtEntity.setOnSpaceUpdatedListener(null, null)
        }
    }

    /**
     * A recommended box for content to be placed in when in Full Space Mode.
     *
     * The box is relative to the ActivitySpace's coordinate system. It is not scaled by the
     * ActivitySpace's transform. The dimensions are always in meters. This provides a
     * device-specific default volume that developers can use to size their content appropriately.
     */
    public val recommendedContentBoxInFullSpace: BoundingBox =
        rtEntity.recommendedContentBoxInFullSpace
}
