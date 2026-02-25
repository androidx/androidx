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
import androidx.xr.arcore.runtime.PerceptionManager
import androidx.xr.arcore.runtime.PerceptionRuntime
import androidx.xr.runtime.CoreState
import androidx.xr.runtime.StateExtender
import androidx.xr.runtime.internal.JxrRuntime
import kotlin.time.ComparableTimeMark

/** [StateExtender] in charge of extending [CoreState] with [PerceptionState]. */
// TODO: b/455593773 - Restrict ctor once YTXR ports to JXR proper, and is no longer a chimeric app.
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PerceptionStateExtender : StateExtender {

    internal companion object {
        internal const val MAX_PERCEPTION_STATE_EXTENSION_SIZE = 100

        internal val perceptionStateMap = mutableMapOf<ComparableTimeMark, PerceptionState>()

        private val timeMarkQueue = ArrayDeque<ComparableTimeMark>()
    }

    internal lateinit var perceptionManager: PerceptionManager

    internal val xrResourcesManager = XrResourcesManager()

    override fun initialize(runtimes: List<JxrRuntime>) {
        val perceptionRuntime = runtimes.filterIsInstance<PerceptionRuntime>().single()
        perceptionManager = perceptionRuntime.perceptionManager
        xrResourcesManager.lifecycleManager = perceptionRuntime.lifecycleManager
        xrResourcesManager.initiateHands(perceptionManager.leftHand, perceptionManager.rightHand)
        xrResourcesManager.initiateArDeviceAndRenderViewpoints(
            perceptionManager.arDevice,
            perceptionManager.leftRenderViewpoint,
            perceptionManager.rightRenderViewpoint,
            perceptionManager.monoRenderViewpoint,
        )
        xrResourcesManager.initiateGeospatial(perceptionManager.geospatial)
        xrResourcesManager.initiateDepthMaps(
            perceptionManager.leftDepthMap,
            perceptionManager.rightDepthMap,
            perceptionManager.monoDepthMap,
        )
        xrResourcesManager.initiateFace(perceptionManager.userFace)
        xrResourcesManager.initiateEyes(perceptionManager.leftEye, perceptionManager.rightEye)
    }

    override suspend fun extend(coreState: CoreState) {
        check(this::perceptionManager.isInitialized) {
            "PerceptionStateExtender is not initialized."
        }

        xrResourcesManager.syncTrackables(perceptionManager.trackables)
        xrResourcesManager.update()

        xrResourcesManager.leftEye?.update()
        xrResourcesManager.rightEye?.update()
        xrResourcesManager.leftHand?.update()
        xrResourcesManager.rightHand?.update()
        xrResourcesManager.arDevice.update()
        xrResourcesManager.leftRenderViewpoint?.update()
        xrResourcesManager.rightRenderViewpoint?.update()
        xrResourcesManager.monoRenderViewpoint?.update()
        xrResourcesManager.leftDepthMap?.update()
        xrResourcesManager.rightDepthMap?.update()
        xrResourcesManager.monoDepthMap?.update()

        xrResourcesManager.userFace?.update()

        updatePerceptionStateMap(coreState)
    }

    internal fun close() {
        perceptionStateMap.clear()
        timeMarkQueue.clear()
        xrResourcesManager.clear()
    }

    private fun updatePerceptionStateMap(coreState: CoreState) {
        perceptionStateMap.put(
            coreState.timeMark,
            PerceptionState(
                coreState.timeMark,
                xrResourcesManager.trackablesMap.values,
                xrResourcesManager.leftHand,
                xrResourcesManager.rightHand,
                xrResourcesManager.arDevice,
                xrResourcesManager.leftRenderViewpoint,
                xrResourcesManager.rightRenderViewpoint,
                xrResourcesManager.monoRenderViewpoint,
                xrResourcesManager.leftDepthMap,
                xrResourcesManager.rightDepthMap,
                xrResourcesManager.monoDepthMap,
                xrResourcesManager.userFace,
                xrResourcesManager.leftEye,
                xrResourcesManager.rightEye,
            ),
        )
        timeMarkQueue.add(coreState.timeMark)

        if (timeMarkQueue.size > MAX_PERCEPTION_STATE_EXTENSION_SIZE) {
            val timeMark = timeMarkQueue.removeFirst()
            perceptionStateMap.remove(timeMark)
        }
    }
}

/** The state of the perception system. */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public val CoreState.perceptionState: PerceptionState?
    get() = PerceptionStateExtender.perceptionStateMap[this.timeMark]
