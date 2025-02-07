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

import androidx.xr.runtime.CoreState
import androidx.xr.runtime.StateExtender
import androidx.xr.runtime.internal.PerceptionManager
import androidx.xr.runtime.internal.Runtime
import kotlin.time.ComparableTimeMark

/** [StateExtender] in charge of extending [CoreState] with [PerceptionState]. */
internal class PerceptionStateExtender : StateExtender {

    internal companion object {
        internal const val MAX_PERCEPTION_STATE_EXTENSION_SIZE = 100

        internal val perceptionStateMap = mutableMapOf<ComparableTimeMark, PerceptionState>()

        private val timeMarkQueue = ArrayDeque<ComparableTimeMark>()
    }

    internal lateinit var perceptionManager: PerceptionManager

    internal val xrResourcesManager = XrResourcesManager()

    override fun initialize(runtime: Runtime) {
        perceptionManager = runtime.perceptionManager
        xrResourcesManager.initiateHands(perceptionManager.leftHand, perceptionManager.rightHand)
    }

    override suspend fun extend(coreState: CoreState) {
        check(this::perceptionManager.isInitialized) {
            "PerceptionStateExtender is not initialized."
        }

        xrResourcesManager.syncTrackables(perceptionManager.trackables)
        xrResourcesManager.update()

        xrResourcesManager.leftHand?.update()
        xrResourcesManager.rightHand?.update()

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
public val CoreState.perceptionState: PerceptionState?
    get() = PerceptionStateExtender.perceptionStateMap[this.timeMark]
