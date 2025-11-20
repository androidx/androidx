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

package androidx.xr.scenecore.testapp.common

import android.annotation.SuppressLint
import android.content.Context
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.PanelEntity

/**
 * A debug panel that holds a panel entity with a view that displays a list of text lines.
 *
 * Each line is associated with a key and can be edited or set. The view is intended to be used for
 * debugging purposes and takes in an optional name to display at the top of the panel. The text
 * lines can be added in code through the view. example: `debugPanel.view.setLine("key", "text")`.
 */
@SuppressLint("RestrictedApi")
class DebugTextPanel(
    context: Context,
    session: Session,
    /** The parent entity that the Debug panel is attached to. */
    var parent: Entity,
    pixelDimensions: IntSize2d = IntSize2d(550, 750),
    name: String = "DebugTextPanel",
    pose: Pose = Pose(Vector3(0f, -0.1f, 0.1f)),
) {
    /** The view that displays the text lines. */
    var view: DebugTextLinearView = DebugTextLinearView(context = context)

    /** The panel entity that is created to hold the Debug view. */
    var panelEntity =
        PanelEntity.create(
            session,
            view,
            name = name,
            pose = pose,
            pixelDimensions = pixelDimensions,
        )

    /** Optional entity that the Debug panel is tracking. */
    @SuppressLint("RestrictedApi") var trackedEntity: Entity? = null

    init {
        panelEntity.parent = parent
        view.setName(name)
    }
}
