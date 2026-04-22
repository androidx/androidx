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

package androidx.xr.arcore.openxr

import androidx.xr.arcore.runtime.RenderViewpoint
import androidx.xr.runtime.math.FieldOfView
import androidx.xr.runtime.math.Pose

/**
 * Wraps the device tracking data.
 *
 * @property pose the [Pose] of the render viewpoint
 * @property fieldOfView the [FieldOfView] of the render viewpoint
 */
internal class OpenXrRenderViewpoint internal constructor() : RenderViewpoint {

    override var pose: Pose = Pose()
        private set

    override var fieldOfView: FieldOfView = FieldOfView(0f, 0f, 0f, 0f)
        private set

    internal fun update(state: ViewCameraState) {
        pose = state.pose
        fieldOfView =
            FieldOfView(
                state.fieldOfView.angleLeft,
                state.fieldOfView.angleRight,
                state.fieldOfView.angleUp,
                state.fieldOfView.angleDown,
            )
    }
}
