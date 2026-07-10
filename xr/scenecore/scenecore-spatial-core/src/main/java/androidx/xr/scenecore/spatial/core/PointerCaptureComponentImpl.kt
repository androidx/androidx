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
package androidx.xr.scenecore.spatial.core

import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.InputEventListener
import androidx.xr.scenecore.runtime.PointerCaptureComponent
import java.util.concurrent.Executor

/** Implementation of PointerCaptureComponent. */
internal class PointerCaptureComponentImpl(
    private val executor: Executor,
    private val stateListener: PointerCaptureComponent.StateListener,
    private val inputListener: InputEventListener,
) : PointerCaptureComponent {
    private var attachedEntity: AndroidXrEntity? = null

    override fun onAttach(entity: Entity): Boolean {
        if (entity !is AndroidXrEntity || attachedEntity != null) {
            return false
        }

        attachedEntity = entity
        return attachedEntity!!.requestPointerCapture(executor, inputListener, stateListener)
    }

    override fun onDetach(entity: Entity) {
        checkNotNull(attachedEntity) { "No attached Entity, cannot detach." }
        attachedEntity!!.stopPointerCapture()
        attachedEntity = null
    }
}
