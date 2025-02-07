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

import android.annotation.SuppressLint
import androidx.xr.runtime.internal.Hand as RuntimeHand
import androidx.xr.runtime.internal.Plane as RuntimePlane
import androidx.xr.runtime.internal.Trackable as RuntimeTrackable
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList

/** Manages all XR resources that are used by the ARCore for XR API. */
internal class XrResourcesManager {

    /** List of [Updatable]s that are updated every frame. */
    private val _updatables = CopyOnWriteArrayList<Updatable>()
    val updatables: List<Updatable> = _updatables

    /** Queue of [Anchor]s that will be detached on the next frame update. */
    private val _anchorsToDetachQueue = ConcurrentLinkedQueue<Anchor>()
    val anchorsToDetachQueue: Queue<Anchor> = _anchorsToDetachQueue

    /** Map of runtime trackable pointer to [Trackable]. */
    @SuppressLint("BanConcurrentHashMap")
    private val _trackablesMap =
        java.util.concurrent.ConcurrentHashMap<RuntimeTrackable, Trackable<Trackable.State>>()
    val trackablesMap: Map<RuntimeTrackable, Trackable<Trackable.State>> = _trackablesMap

    /** The data of hands */
    private var _leftRuntimeHand: RuntimeHand? = null
    private var _rightRuntimeHand: RuntimeHand? = null
    val leftHand: Hand? by lazy { _leftRuntimeHand?.let { Hand(it) } }
    val rightHand: Hand? by lazy { _rightRuntimeHand?.let { Hand(it) } }

    internal fun initiateHands(leftRuntimeHand: RuntimeHand?, rightRuntimeHand: RuntimeHand?) {
        _leftRuntimeHand = leftRuntimeHand
        _rightRuntimeHand = rightRuntimeHand
    }

    internal fun addUpdatable(updatable: Updatable) {
        _updatables.add(updatable)
    }

    internal fun removeUpdatable(updatable: Updatable) {
        _updatables.remove(updatable)
    }

    internal fun queueAnchorToDetach(anchor: Anchor) {
        _anchorsToDetachQueue.add(anchor)
    }

    internal suspend fun update() {
        while (!_anchorsToDetachQueue.isEmpty()) {
            _anchorsToDetachQueue.poll()?.runtimeAnchor?.detach()
        }

        for (updatable in updatables) {
            updatable.update()
        }
    }

    internal fun syncTrackables(runtimeTrackables: Collection<RuntimeTrackable>) {
        val toRemoveTrackables = _trackablesMap.keys - runtimeTrackables
        val toAddTrackables = runtimeTrackables - _trackablesMap.keys

        for (runtimeTrackable in toRemoveTrackables) {
            removeUpdatable(_trackablesMap[runtimeTrackable]!! as Updatable)
            _trackablesMap.remove(runtimeTrackable)
        }

        for (runtimeTrackable in toAddTrackables) {
            val trackable = createTrackable(runtimeTrackable)
            _trackablesMap[runtimeTrackable] = trackable
            addUpdatable(trackable as Updatable)
        }
    }

    internal fun clear() {
        _updatables.clear()
        _trackablesMap.clear()
    }

    private fun createTrackable(runtimeTrackable: RuntimeTrackable): Trackable<Trackable.State> {
        if (_trackablesMap.containsKey(runtimeTrackable)) {
            return _trackablesMap[runtimeTrackable]!!
        }

        val trackable =
            when (runtimeTrackable) {
                is RuntimePlane -> Plane(runtimeTrackable, this)
                else ->
                    throw IllegalArgumentException(
                        "Unsupported trackable type: ${runtimeTrackable.javaClass}"
                    )
            }
        _trackablesMap[runtimeTrackable] = trackable
        return trackable
    }
}
