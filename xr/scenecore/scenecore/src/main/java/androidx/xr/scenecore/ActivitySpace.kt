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

import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.scenecore.runtime.ActivitySpace as RtActivitySpace
import androidx.xr.scenecore.runtime.DirectExecutor
import androidx.xr.scenecore.runtime.HandlerExecutor
import androidx.xr.scenecore.runtime.SceneRuntime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.Executor
import java.util.function.Consumer

/**
 * ActivitySpace is a [SpaceEntity] used to track the system-managed pose and boundary of the volume
 * associated with a spatialized Activity. The Application cannot directly control this volume, but
 * the system might update it in response to the User moving it or entering or exiting Full Space
 * Mode.
 */
// TODO: b/440429756 - Define dispose policy for SceneCore singletons like main panel and system
// spaces.
public class ActivitySpace
private constructor(rtActivitySpace: RtActivitySpace, entityRegistry: EntityRegistry) :
    SpaceEntity(rtActivitySpace, entityRegistry) {

    private val rtActivitySpace: RtActivitySpace
        get() = rtEntity as RtActivitySpace

    internal companion object {
        internal fun create(
            sceneRuntime: SceneRuntime,
            entityRegistry: EntityRegistry,
        ): ActivitySpace = ActivitySpace(sceneRuntime.activitySpace, entityRegistry)
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
            return rtActivitySpace.bounds.toFloatSize3d()
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
    public fun addBoundsChangedListener(listener: Consumer<FloatSize3d>): Unit =
        addBoundsChangedListener(HandlerExecutor.mainThreadExecutor, listener)

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
    public fun addBoundsChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<FloatSize3d>,
    ) {
        checkNotDisposed()
        val rtListener: RtActivitySpace.OnBoundsChangedListener =
            RtActivitySpace.OnBoundsChangedListener { rtDimensions ->
                callbackExecutor.execute { listener.accept(rtDimensions.toFloatSize3d()) }
            }
        boundsListeners.compute(listener) { _, _ ->
            rtActivitySpace.addOnBoundsChangedListener(rtListener)
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
    public fun removeBoundsChangedListener(listener: Consumer<FloatSize3d>) {
        checkNotDisposed()
        boundsListeners.computeIfPresent(listener) { _, rtListener ->
            rtActivitySpace.removeOnBoundsChangedListener(rtListener)
            null // returning null from computeIfPresent removes this entry from the Map
        }
    }

    /**
     * Adds a listener to be called when the ActivitySpace's origin has moved or changed, typically
     * due to an internal system event.
     *
     * When this event occurs, any [ScenePose] that is not a child of ActivitySpace, such as
     * [AnchorSpace], will have a different position relative to the [ActivitySpace]. Therefore,
     * this listener can be used to indicate when to invalidate any cached information about the
     * relative difference in Pose between ActivitySpace's children and children of
     * non-ActivitySpace ScenePoses.
     *
     * @param listener The listener to register.
     * @param executor The [Executor] on which to run the listener.
     */
    public fun addOriginChangedListener(executor: Executor, listener: Runnable) {
        checkNotDisposed()
        val addRtListener = originChangedListeners.isEmpty()
        originChangedListeners[listener] = executor
        if (addRtListener) {
            rtActivitySpace.setOnOriginChangedListener(rtOriginChangedListener, null)
        }
    }

    /**
     * Adds a listener to be called when the ActivitySpace's origin has moved or changed, typically
     * due to an internal system event.
     *
     * When this event occurs, any [ScenePose] that is not a child of ActivitySpace, such as
     * [AnchorSpace], will have a different position relative to the [ActivitySpace]. Therefore,
     * this listener can be used to indicate when to invalidate any cached information about the
     * relative difference in Pose between ActivitySpace's children and children of
     * non-ActivitySpace ScenePoses.
     *
     * The callback will be made on the SceneCore executor.
     *
     * @param listener The listener to register.
     */
    public fun addOriginChangedListener(listener: Runnable): Unit =
        addOriginChangedListener(DirectExecutor, listener)

    /**
     * Removes the previously-added listener.
     *
     * All listeners are automatically removed when the ActivitySpace is disposed even if this
     * method is not explicitly called.
     */
    public fun removeOriginChangedListener(listener: Runnable) {
        checkNotDisposed()
        originChangedListeners.remove(listener)
        if (originChangedListeners.isEmpty()) {
            rtActivitySpace.setOnOriginChangedListener(null, null)
        }
    }

    /**
     * A recommended box for content to be placed in when in Full Space.
     *
     * The recommended content box is a static 3D volume that uses the device's field of view (FOV)
     * angles, the system's default launch distance from the user, and the default scale of the
     * system to calculate a box size that is sized to encompass the user's primary field of view.
     *
     * This size does not change throughout the lifecycle of the application. Furthermore, the
     * recommended content box does not have an independent concept of pose; its position is defined
     * by the origin of this [ActivitySpace].
     *
     * The box is relative to the ActivitySpace's coordinate system. It is not scaled by the
     * ActivitySpace's transform. The dimensions are always in meters. This provides a
     * device-specific default volume that developers can use to size their content appropriately.
     */
    public val recommendedContentBoxInFullSpace: BoundingBox
        get() {
            checkNotDisposed()
            return rtActivitySpace.recommendedContentBoxInFullSpace
        }

    override fun disposeInternal() {
        if (isDisposed) return
        boundsListeners.keys.forEach { removeBoundsChangedListener(it) }
        originChangedListeners.keys.forEach { removeOriginChangedListener(it) }
        boundsListeners.clear()
        originChangedListeners.clear()
        rtActivitySpace.setOnOriginChangedListener(null, null)
        super.disposeInternal()
    }
}
