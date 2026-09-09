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

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.scene
import kotlinx.coroutines.launch

/**
 * Minimal SceneCore panel sample: text panels in front of the viewer, spawned, moved, resized,
 * recolored and disposed from the phone through [SceneCorePanelStateHolder].
 *
 * Only public Jetpack XR APIs ([Session] and [PanelEntity]) are used, so the same code runs on top
 * of any SceneCore runtime.
 */
@SuppressLint("NewApi")
class SceneCorePanelActivity : ComponentActivity() {

    private class LivePanel(
        val entity: PanelEntity,
        val view: TextView,
        val sizeInPixels: IntSize2d,
    )

    /** Panels currently shown, keyed by [SceneCorePanelSpec.id]. */
    private val panels = mutableMapOf<Int, LivePanel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep the activity window transparent so that only the panels show up on the glasses.
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        lifecycleScope.launch {
            val result =
                Session.create(
                    context = this@SceneCorePanelActivity,
                    lifecycleOwner = this@SceneCorePanelActivity,
                )
            if (result !is SessionCreateSuccess) {
                Log.e(TAG, "Session.create failed: ${result::class.simpleName}")
                return@launch
            }
            // Follows the phone-side controller for as long as the activity lives.
            SceneCorePanelStateHolder.panels.collect { specs -> apply(result.session, specs) }
        }
    }

    /** Brings the live panels in line with [specs]: disposes, spawns, then updates each one. */
    private fun apply(session: Session, specs: List<SceneCorePanelSpec>) {
        val wanted = specs.associateBy { it.id }
        (panels.keys - wanted.keys).forEach { id -> dispose(id) }
        specs.forEach { spec ->
            // The OpenXR runtime sizes a panel once, when it is created (`sizeInPixels` only
            // re-lays out the hosted view), so a new size means a new panel.
            val live = panels[spec.id]
            if (live != null && live.sizeInPixels != spec.sizeInPixels) dispose(spec.id)
            val panel = panels.getOrPut(spec.id) { spawn(session, spec) }
            panel.entity.setPose(Pose(spec.position))
            panel.view.setBackgroundColor(spec.color)
        }
    }

    private fun dispose(id: Int) {
        val panel = panels.remove(id) ?: return
        // `parent = null` only detaches the entity and the OpenXR runtime keeps rendering a
        // detached panel until the entity is disposed, so dispose it explicitly.
        @Suppress("DEPRECATION") panel.entity.dispose()
        Log.i(TAG, "Panel $id disposed.")
    }

    private fun spawn(session: Session, spec: SceneCorePanelSpec): LivePanel {
        val padding = spec.sizeInPixels.height / 8
        val view =
            TextView(this).apply {
                text = "Panel ${spec.id}"
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                maxLines = 1
                setPadding(padding, padding, padding, padding)
                // Make the text fill the panel, whatever size it was spawned with.
                setAutoSizeTextTypeUniformWithConfiguration(
                    MIN_TEXT_SIZE_SP,
                    MAX_TEXT_SIZE_SP,
                    TEXT_SIZE_STEP_SP,
                    TypedValue.COMPLEX_UNIT_SP,
                )
            }
        // Entities are only rendered once they are attached to the scene graph, so parent the
        // panel to the activity space explicitly.
        val entity =
            PanelEntity.create(
                session,
                view,
                spec.sizeInPixels,
                "Panel ${spec.id}",
                parent = session.scene.activitySpace,
            )
        // The OpenXR runtime currently draws every panel on a 1 m x 1 m quad whatever its pixel
        // size, so apply `size` (1000 px/m) through the scale, halved so that the panels fit
        // comfortably in the projected field of view at the default distance.
        val size = entity.size
        entity.setScale(Vector3(size.width * PANEL_SCALE, size.height * PANEL_SCALE, 1f))
        Log.i(
            TAG,
            "Panel ${spec.id} created (${spec.sizeInPixels.width}x${spec.sizeInPixels.height}).",
        )
        return LivePanel(entity, view, spec.sizeInPixels)
    }

    private companion object {
        const val TAG = "SceneCorePanel"
        const val MIN_TEXT_SIZE_SP = 8
        const val MAX_TEXT_SIZE_SP = 2000
        const val TEXT_SIZE_STEP_SP = 2

        /** Applied on top of the panel size in meters, see [spawn]. */
        const val PANEL_SCALE = 0.5f
    }
}
