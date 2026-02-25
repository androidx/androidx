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
import androidx.xr.arcore.runtime.ArDevice as RuntimeArDevice
import androidx.xr.arcore.runtime.AugmentedObject as RuntimeObject
import androidx.xr.arcore.runtime.DepthMap as RuntimeDepthMap
import androidx.xr.arcore.runtime.Eye as RuntimeEye
import androidx.xr.arcore.runtime.Face as RuntimeFace
import androidx.xr.arcore.runtime.Geospatial as RuntimeGeospatial
import androidx.xr.arcore.runtime.Hand as RuntimeHand
import androidx.xr.arcore.runtime.Plane as RuntimePlane
import androidx.xr.arcore.runtime.RenderViewpoint as RuntimeRenderViewpoint
import androidx.xr.arcore.runtime.Trackable as RuntimeTrackable
import androidx.xr.runtime.internal.LifecycleManager
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList

/** Manages all XR resources that are used by the ARCore for XR API. */
internal class XrResourcesManager {

    internal lateinit var lifecycleManager: LifecycleManager

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

    /** The data of eyes */
    private var _leftRuntimeEye: RuntimeEye? = null
    private var _rightRuntimeEye: RuntimeEye? = null
    val leftEye: Eye? by lazy { _leftRuntimeEye?.let { Eye(it) } }
    val rightEye: Eye? by lazy { _rightRuntimeEye?.let { Eye(it) } }

    /** The data of hands */
    private var _leftRuntimeHand: RuntimeHand? = null
    private var _rightRuntimeHand: RuntimeHand? = null
    val leftHand: Hand? by lazy { _leftRuntimeHand?.let { Hand(it) } }
    val rightHand: Hand? by lazy { _rightRuntimeHand?.let { Hand(it) } }

    /** The ar device tracking data */
    lateinit var arDevice: ArDevice
        private set

    /** The render viewpoint data */
    var monoRenderViewpoint: RenderViewpoint? = null
        private set

    var leftRenderViewpoint: RenderViewpoint? = null
        private set

    var rightRenderViewpoint: RenderViewpoint? = null
        private set

    /** The data of the user's face */
    private var _userFace: RuntimeFace? = null
    val userFace: Face? by lazy { _userFace?.let { Face(it, this) } }

    /** Geospatial data */
    private var _geospatial: Geospatial? = null
    val geospatial: Geospatial
        get() = checkNotNull(_geospatial)

    /** The depth map data */
    var leftDepthMap: DepthMap? = null
        private set

    var rightDepthMap: DepthMap? = null
        private set

    var monoDepthMap: DepthMap? = null
        private set

    internal fun initiateGeospatial(runtimeGeospatial: RuntimeGeospatial) {
        _geospatial = Geospatial(runtimeGeospatial, this)
    }

    internal fun initiateEyes(leftRuntimeEye: RuntimeEye?, rightRuntimeEye: RuntimeEye?) {
        _leftRuntimeEye = leftRuntimeEye
        _rightRuntimeEye = rightRuntimeEye
    }

    internal fun initiateHands(leftRuntimeHand: RuntimeHand?, rightRuntimeHand: RuntimeHand?) {
        _leftRuntimeHand = leftRuntimeHand
        _rightRuntimeHand = rightRuntimeHand
    }

    internal fun initiateArDeviceAndRenderViewpoints(
        runtimeArDevice: RuntimeArDevice,
        runtimeLeftRenderViewpoint: RuntimeRenderViewpoint?,
        runtimeRightRenderViewpoint: RuntimeRenderViewpoint?,
        runtimeMonoRenderViewpoint: RuntimeRenderViewpoint?,
    ) {
        arDevice = ArDevice(runtimeArDevice)
        runtimeLeftRenderViewpoint?.let {
            leftRenderViewpoint = RenderViewpoint(it, runtimeArDevice)
        }
        runtimeRightRenderViewpoint?.let {
            rightRenderViewpoint = RenderViewpoint(it, runtimeArDevice)
        }
        runtimeMonoRenderViewpoint?.let {
            monoRenderViewpoint = RenderViewpoint(it, runtimeArDevice)
        }
    }

    internal fun initiateDepthMaps(
        runtimeLeftDepthMap: RuntimeDepthMap?,
        runtimeRightDepthMap: RuntimeDepthMap?,
        runtimeMonoDepthMap: RuntimeDepthMap?,
    ) {
        runtimeLeftDepthMap?.let { leftDepthMap = DepthMap(it) }
        runtimeRightDepthMap?.let { rightDepthMap = DepthMap(it) }
        runtimeMonoDepthMap?.let { monoDepthMap = DepthMap(it) }
    }

    internal fun initiateFace(userFace: RuntimeFace?) {
        _userFace = userFace
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

        // Geospatial should always be initialized if a runtime is present. This check should only
        // fail
        // in unit tests.
        if (_geospatial != null) {
            geospatial.update()
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
                is RuntimeObject -> AugmentedObject(runtimeTrackable, this)
                is RuntimeFace -> Face(runtimeTrackable, this)
                is RuntimeEye -> Eye(runtimeTrackable)
                is RuntimeHand -> Hand(runtimeTrackable)
                else ->
                    throw IllegalArgumentException(
                        "Unsupported trackable type: ${runtimeTrackable.javaClass}"
                    )
            }
        _trackablesMap[runtimeTrackable] = trackable
        return trackable
    }
}
