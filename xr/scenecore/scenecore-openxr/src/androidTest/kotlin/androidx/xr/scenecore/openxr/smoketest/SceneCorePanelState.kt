/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.scenecore.openxr.smoketest

import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Vector3
import kotlinx.coroutines.flow.MutableStateFlow

/** One panel that the glasses-side [SceneCorePanelActivity] should show, as set up on the phone. */
internal data class SceneCorePanelSpec(
    /** Spawn order, also shown on the panel so it can be told apart on the glasses. */
    val id: Int,
    /**
     * Position of the panel in the activity space, in meters. The activity space origin sits at the
     * activity window plane with +Z pointing toward the viewer, so a positive Z places the panel
     * between the window and the viewer.
     */
    val position: Vector3,
    /** Background color of the panel, as an ARGB color int. */
    val color: Int,
    /** Size of the panel in pixels, at [SceneCorePanelActivity]'s virtual pixel density. */
    val sizeInPixels: IntSize2d,
)

/**
 * In-process bridge between [SceneCoreOpenXrLauncherActivity] on the phone and
 * [SceneCorePanelActivity] on the glasses: both run in the same process, so a shared flow is all
 * that is needed to drive the panels.
 */
internal object SceneCorePanelStateHolder {
    /** The panels that should currently exist, in spawn order. */
    val panels = MutableStateFlow<List<SceneCorePanelSpec>>(emptyList())
}
