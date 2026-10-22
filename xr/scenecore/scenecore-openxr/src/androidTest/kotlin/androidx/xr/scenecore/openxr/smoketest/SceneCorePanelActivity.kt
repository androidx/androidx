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
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.xr.runtime.Config
import androidx.xr.runtime.CoreState
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.scene
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Minimal SceneCore panel sample: text panels in front of the viewer, or attached to ARCore
 * anchors, spawned, moved, resized, recolored and disposed from the phone through
 * [SceneCorePanelStateHolder]. [SceneCoreAnchors] creates and follows the anchors.
 *
 * Apart from the diagnostics in [SceneCoreDiagnostics], only public Jetpack XR APIs ([Session],
 * [PanelEntity], [androidx.xr.arcore.Anchor] and [androidx.xr.scenecore.AnchorSpace]) are used, so
 * the same code runs on top of any SceneCore runtime.
 */
@SuppressLint("NewApi", "SetTextI18n")
class SceneCorePanelActivity : ComponentActivity() {

    private class LivePanel(
        val id: Int,
        val entity: PanelEntity,
        val view: TextView,
        val sizeInPixels: IntSize2d,
    ) {
        /** The anchor that the panel is attached to, or null while it is in the activity space. */
        var anchorId: Int? = null
        /** The last position sent to [entity], relative to its current parent. */
        var position: Vector3? = null
    }

    private lateinit var session: Session

    private lateinit var anchors: SceneCoreAnchors

    /** Panels currently shown, keyed by [SceneCorePanelSpec.id]. */
    private val panels = mutableMapOf<Int, LivePanel>()

    /** Bumped to apply the scene again, for example once an anchor is ready for panels. */
    private val reapply = MutableStateFlow(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SceneCorePanelStateHolder.isPanelActivityRunning = true

        // Keep the activity window transparent so that only the panels show up on the glasses.
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        lifecycleScope.launch {
            session = createSession() ?: return@launch
            anchors =
                SceneCoreAnchors(
                    this@SceneCorePanelActivity,
                    session,
                    releasePanels = ::releasePanels,
                    reapply = { reapply.update { it + 1 } },
                )
            val stateBeforeUpdates = session.state.value
            if (configureSession()) waitForFirstSessionUpdate(stateBeforeUpdates)
            launch { anchors.reportForever() }
            // Follows the phone-side controller for as long as the activity lives.
            combine(SceneCorePanelStateHolder.scene, reapply) { scene, _ -> scene }
                .collect { scene -> apply(scene) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SceneCorePanelStateHolder.isPanelActivityRunning = false
        SceneCorePanelStateHolder.status.update {
            it.copy(
                runtimes = null,
                usesArCoreFakes = false,
                isConfigured = false,
                anchors = emptyMap(),
            )
        }
        logEvent("The glasses activity was destroyed, and its Session with it.")
    }

    private suspend fun createSession(): Session? {
        val result =
            try {
                Session.create(context = this, lifecycleOwner = this)
            } catch (e: Exception) {
                // For example, the OpenXR ARCore runtime can fail to initialize.
                logError("Session.create threw $e")
                return null
            }
        if (result !is SessionCreateSuccess) {
            logError("Session.create failed: ${result::class.simpleName}")
            return null
        }
        val runtimes = SceneCoreDiagnostics.describeRuntimes(result.session)
        val usesArCoreFakes = SceneCoreDiagnostics.usesArCoreFakes(result.session)
        SceneCorePanelStateHolder.status.update {
            it.copy(runtimes = runtimes, usesArCoreFakes = usesArCoreFakes)
        }
        logEvent("Session created with $runtimes.")
        return result.session
    }

    /**
     * Configures the Session, which starts its update loop: ARCore updates anchors from it.
     *
     * @return whether the Session is configured.
     */
    private fun configureSession(): Boolean {
        val result =
            try {
                session.configure(Config.Builder().build())
            } catch (e: Exception) {
                logError("Session.configure threw $e")
                return false
            }
        if (result !is SessionConfigureSuccess) {
            logError("Session.configure failed: ${result::class.simpleName}")
            return false
        }
        SceneCorePanelStateHolder.status.update { it.copy(isConfigured = true) }
        logEvent("Session configured.")
        return true
    }

    /**
     * Waits for the Session's first update after [stateBeforeUpdates]: ARCore stamps new anchors
     * with the time of its last update, which isn't valid before the first one.
     */
    private suspend fun waitForFirstSessionUpdate(stateBeforeUpdates: CoreState) {
        val update =
            withTimeoutOrNull(SESSION_UPDATE_TIMEOUT_MILLIS) {
                session.state.first { it != stateBeforeUpdates }
            }
        if (update == null) {
            logEvent(
                "The Session didn't update within $SESSION_UPDATE_TIMEOUT_MILLIS ms, so " +
                    "Anchor.create may fail."
            )
        }
    }

    /** Brings the live anchors and panels in line with [scene]. */
    private suspend fun apply(scene: SceneCoreSceneSpec) {
        val wantedPanels = scene.panels.associateBy { it.id }
        val wantedAnchors = scene.anchors.map { it.id }.toSet()
        (panels.keys - wantedPanels.keys).toList().forEach { id -> disposePanel(id) }
        // Disposing an AnchorSpace disposes its children too, so first move panels off the anchors
        // that they should leave, unless they can move straight to another ready anchor.
        panels.values.toList().forEach { panel ->
            val currentAnchorId = panel.anchorId ?: return@forEach
            val wantedAnchorId = wantedPanels.getValue(panel.id).anchorId
            val isLeavingCurrentAnchor =
                currentAnchorId !in wantedAnchors ||
                    anchors.isFailed(currentAnchorId) ||
                    wantedAnchorId != currentAnchorId
            val canAttachToWantedAnchorNow =
                wantedAnchorId != null &&
                    wantedAnchorId in wantedAnchors &&
                    anchors.spaceForPanels(wantedAnchorId) != null
            if (isLeavingCurrentAnchor && !canAttachToWantedAnchorNow) {
                moveToActivitySpace(panel)
            }
        }
        anchors.apply(scene.anchors)
        scene.panels.forEach { spec -> applyPanel(spec) }
    }

    /** Brings panel [spec] in line: spawns it, attaches it to its anchor, moves and recolors it. */
    private suspend fun applyPanel(spec: SceneCorePanelSpec) {
        // The OpenXR runtime sizes a panel once, when it is created (`sizeInPixels` only
        // re-lays out the hosted view), so a new size means a new panel.
        val live = panels[spec.id]
        if (live != null && live.sizeInPixels != spec.sizeInPixels) disposePanel(spec.id)
        val panel = panels[spec.id] ?: spawnPanel(spec).also { panels[spec.id] = it }
        val anchorId = spec.anchorId
        val space = anchorId?.let { anchors.spaceForPanels(it) }
        if (space != null && panel.anchorId != anchorId) {
            panel.entity.parent = space
            panel.anchorId = anchorId
            panel.position = null
            logEvent("Panel ${spec.id} attached to Anchor $anchorId.")
            settleTransactions()
        }
        // The position is relative to the panel's parent, so leave a panel that still waits for its
        // anchor where it is.
        if (panel.anchorId == anchorId && panel.position != spec.position) {
            panel.entity.setPose(Pose(spec.position))
            panel.position = spec.position
            settleTransactions()
        }
        // Only relabel on change: every new text re-lays out the auto-sized label.
        val label = describePanel(spec, panel)
        if (panel.view.text.toString() != label) {
            panel.view.maxLines = label.lines().size
            panel.view.text = label
        }
        panel.view.setBackgroundColor(spec.color)
    }

    private fun describePanel(spec: SceneCorePanelSpec, panel: LivePanel): String {
        val anchorId = spec.anchorId ?: return "Panel ${spec.id}"
        return if (panel.anchorId == anchorId) {
            "Panel ${spec.id}\non Anchor $anchorId"
        } else {
            "Panel ${spec.id}\n${anchors.describeWait(anchorId)}"
        }
    }

    /** Moves the panels on anchor [anchorId] to the activity space. */
    private suspend fun releasePanels(anchorId: Int) {
        panels.values.filter { it.anchorId == anchorId }.forEach { moveToActivitySpace(it) }
    }

    private suspend fun moveToActivitySpace(panel: LivePanel) {
        panel.entity.parent = session.scene.activitySpace
        logEvent("Panel ${panel.id} moved from Anchor ${panel.anchorId} to the activity space.")
        panel.anchorId = null
        panel.position = null
        settleTransactions()
    }

    private suspend fun disposePanel(id: Int) {
        val panel = panels.remove(id) ?: return
        // `parent = null` only detaches the entity and the OpenXR runtime keeps rendering a
        // detached panel until the entity is disposed, so dispose it explicitly.
        @Suppress("DEPRECATION") panel.entity.dispose()
        Log.i(TAG, "Panel $id disposed.")
        settleTransactions()
    }

    private suspend fun spawnPanel(spec: SceneCorePanelSpec): LivePanel {
        val view = createLabelView(this, spec.sizeInPixels)
        // Entities are only rendered once they are attached to the scene graph, so parent the
        // panel to the activity space explicitly. It moves to its anchor once that is ready.
        val entity =
            PanelEntity.create(
                session,
                view,
                spec.sizeInPixels,
                "Panel ${spec.id}",
                parent = session.scene.activitySpace,
            )
        settleTransactions()
        applyPanelScale(entity)
        settleTransactions()
        Log.i(
            TAG,
            "Panel ${spec.id} created (${spec.sizeInPixels.width}x${spec.sizeInPixels.height}).",
        )
        return LivePanel(spec.id, entity, view, spec.sizeInPixels)
    }

    private fun logEvent(message: String) = SceneCorePanelStateHolder.logEvent(TAG, message)

    private fun logError(message: String) =
        SceneCorePanelStateHolder.logEvent(TAG, message, isError = true)

    private companion object {
        const val TAG = "SceneCorePanel"
        const val SESSION_UPDATE_TIMEOUT_MILLIS = 2000L
    }
}

private const val MIN_TEXT_SIZE_SP = 8
private const val MAX_TEXT_SIZE_SP = 2000
private const val TEXT_SIZE_STEP_SP = 2

/** Applied on top of the panel size in meters, see [applyPanelScale]. */
private const val PANEL_SCALE = 0.5f

/** A white label for a panel of [sizeInPixels], with text that fills the panel. */
@SuppressLint("NewApi")
internal fun createLabelView(context: Context, sizeInPixels: IntSize2d): TextView {
    val padding = sizeInPixels.height / 8
    return TextView(context).apply {
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
}

/**
 * The OpenXR runtime currently draws every panel on a 1 m x 1 m quad whatever its pixel size, so
 * apply `size` (1000 px/m) through the scale, halved so that the panels fit comfortably in the
 * projected field of view at the default distance.
 */
internal fun applyPanelScale(entity: PanelEntity) {
    val size = entity.size
    entity.setScale(Vector3(size.width * PANEL_SCALE, size.height * PANEL_SCALE, 1f))
}

/**
 * The Vitreous OpenXR runtime only has two scene-transaction slots in flight at a time, and
 * [PanelEntity.create] uses both at once (one to flush the new entity, one to set its parent). When
 * the pool is full, `OpenXrEntity` drops parent, pose and scale changes, so let in-flight
 * transactions finish before sending more.
 */
internal suspend fun settleTransactions() {
    delay(TRANSACTION_SETTLE_MILLIS)
}

private const val TRANSACTION_SETTLE_MILLIS = 50L
