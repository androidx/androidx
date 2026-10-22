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
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.TrackingState
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.AnchorSpace
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.scene
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The anchors of [SceneCorePanelActivity]: an ARCore anchor and an AnchorSpace for each
 * [SceneCoreAnchorSpec] from the phone, with a marker panel below it that shows its state. Reports
 * what happens to each anchor in [SceneCorePanelStateHolder.status].
 *
 * Runs on the main thread, like the activity.
 */
@SuppressLint("SetTextI18n")
internal class SceneCoreAnchors(
    private val activity: ComponentActivity,
    private val session: Session,
    /** Moves the panels on anchor `id` to the activity space, before its AnchorSpace goes away. */
    private val releasePanels: suspend (id: Int) -> Unit,
    /** Applies the scene again, for example once an anchor can hold panels. */
    private val reapply: () -> Unit,
) {

    private class LiveAnchor(
        val anchor: Anchor,
        val space: AnchorSpace,
        var state: AnchorSpace.State,
    ) {
        /** Whether the parenting delay has passed since [space] was created. */
        var isReady = false
        /** Shows the anchor and the state of [space] on the glasses. */
        var marker: TextView? = null
        var originUpdates = 0
        var isDetached = false

        /** Whether panels can be attached to [space] now. */
        val canHoldPanels: Boolean
            get() = isReady && state != AnchorSpace.State.ERROR
    }

    /** Anchors currently in use, keyed by [SceneCoreAnchorSpec.id]. */
    private val anchors = mutableMapOf<Int, LiveAnchor>()

    /** Anchors that failed, or whose AnchorSpace was disposed after an error, so not recreated. */
    private val failedIds = mutableSetOf<Int>()

    /** Whether anchor [id] failed, or its AnchorSpace was disposed after an error. */
    fun isFailed(id: Int): Boolean = id in failedIds

    /** Brings the anchors in line with [specs]: removes, creates or detaches them, adds markers. */
    suspend fun apply(specs: List<SceneCoreAnchorSpec>) {
        val wanted = specs.associateBy { it.id }
        (anchors.keys - wanted.keys).toList().forEach { id -> dispose(id) }
        failedIds.retainAll(wanted.keys)
        (anchors.keys intersect failedIds).toList().forEach { id ->
            dispose(id)
            updateStatus(id) { copy(state = SceneCoreAnchorState.DISPOSED) }
        }
        SceneCorePanelStateHolder.status.update { status ->
            status.copy(anchors = status.anchors.filterKeys { it in wanted })
        }
        specs.forEach { spec ->
            val anchor = anchors[spec.id]
            if (anchor == null) {
                if (spec.id !in failedIds) create(spec)
            } else if (spec.isDetachRequested && !anchor.isDetached) {
                detach(spec.id, anchor)
            }
        }
        anchors.entries.toList().forEach { (id, anchor) ->
            if (anchor.canHoldPanels && anchor.marker == null) spawnMarker(id, anchor)
        }
    }

    /** The AnchorSpace of anchor [id], if panels can be attached to it now. */
    fun spaceForPanels(id: Int): AnchorSpace? = anchors[id]?.takeIf { it.canHoldPanels }?.space

    /** Why panels can't be attached to anchor [id] yet, to show on them. */
    fun describeWait(id: Int): String =
        when {
            id in failedIds -> "Anchor $id failed"
            anchors[id]?.state == AnchorSpace.State.ERROR -> "Anchor $id is in ERROR"
            else -> "waiting for Anchor $id"
        }

    /**
     * Reports each anchor's tracking state, and where its AnchorSpace is in the activity space,
     * which follows the head, until cancelled.
     */
    suspend fun reportForever() {
        while (true) {
            delay(REPORT_INTERVAL_MILLIS)
            anchors.forEach { (id, anchor) ->
                val trackingState = describe(anchor.anchor.state.value.trackingState)
                val position = anchor.space.getPose(Space.ACTIVITY).translation
                updateStatus(id) {
                    copy(
                        trackingState = trackingState,
                        activitySpacePosition = position,
                        originUpdates = anchor.originUpdates,
                    )
                }
            }
        }
    }

    private suspend fun create(spec: SceneCoreAnchorSpec) {
        updateStatus(spec.id) { SceneCoreAnchorStatus() }
        // ARCore takes poses in its perception space, while the activity space moves with the
        // spatial container.
        val pose =
            session.scene.activitySpace.transformPoseTo(
                Pose(spec.position),
                session.scene.perceptionSpace,
            )
        val result =
            try {
                Anchor.create(session, pose)
            } catch (e: Exception) {
                fail(spec.id, "Anchor.create threw $e")
                return
            }
        if (result !is AnchorCreateSuccess) {
            fail(spec.id, "Anchor.create failed: ${result::class.simpleName}")
            return
        }
        val anchor = result.anchor
        val handle = SceneCoreDiagnostics.nativeSpaceHandle(anchor)
        logEvent("Anchor ${spec.id} created, XrSpace ${describeNativeHandle(handle)}.")
        val space =
            try {
                AnchorSpace.create(session, anchor)
            } catch (e: RuntimeException) {
                // For example, on a SceneCore runtime without AnchorEntity support.
                anchor.detach()
                fail(spec.id, "AnchorSpace.create threw $e")
                return
            }
        val live = LiveAnchor(anchor, space, space.state)
        anchors[spec.id] = live
        updateStatus(spec.id) {
            copy(state = toAnchorState(live.state), nativeSpaceHandle = handle)
        }
        // Both listeners run on the main thread, like the rest of this class.
        space.addStateChangedListener { state -> onStateChanged(spec.id, live, state) }
        space.addOriginChangedListener { live.originUpdates++ }
        // The Vitreous runtime refuses new scene transactions until it has associated the anchor,
        // so panels attached to it right away would be lost.
        activity.lifecycleScope.launch {
            delay(SceneCorePanelStateHolder.anchorParentingDelayMillis.value)
            if (anchors[spec.id] === live) {
                live.isReady = true
                reapply()
            }
        }
        settleTransactions()
    }

    private fun onStateChanged(id: Int, anchor: LiveAnchor, state: AnchorSpace.State) {
        // Ignore reports posted before the AnchorSpace was disposed.
        if (anchors[id] !== anchor) return
        anchor.state = state
        logEvent("AnchorSpace $id is $state.")
        updateStatus(id) { copy(state = toAnchorState(state)) }
        anchor.marker?.let { showState(it, id, state) }
        if (
            state == AnchorSpace.State.ERROR &&
                SceneCorePanelStateHolder.disposeAnchorSpaceOnError.value
        ) {
            // ERROR is final. Mark the anchor as failed so apply() moves its panels back to the
            // activity space and disposes the AnchorSpace.
            failedIds += id
        }
        reapply()
    }

    /** Detaches anchor [id] while its AnchorSpace still uses it, which apps must not do. */
    private fun detach(id: Int, anchor: LiveAnchor) {
        anchor.isDetached = true
        anchor.anchor.detach()
        updateStatus(id) { copy(isDetached = true) }
        logEvent("Anchor $id detached while its AnchorSpace still uses it.")
    }

    /**
     * Disposes the AnchorSpace of anchor [id], with its marker, and then detaches the ARCore
     * anchor: detaching destroys the `XrSpace` that the AnchorSpace's content is attached to.
     */
    private suspend fun dispose(id: Int) {
        val anchor = anchors.remove(id) ?: return
        // Disposing an AnchorSpace disposes its children too, so move the panels off it first.
        releasePanels(id)
        // dispose() is deprecated for `parent = null`, which an AnchorSpace doesn't support.
        @Suppress("DEPRECATION") anchor.space.dispose()
        if (!anchor.isDetached) anchor.anchor.detach()
        logEvent("Anchor $id removed.")
        settleTransactions()
    }

    /** Shows a small label at anchor [id], tinted by the state of its AnchorSpace. */
    private suspend fun spawnMarker(id: Int, anchor: LiveAnchor) {
        val view = createLabelView(activity, MARKER_SIZE)
        val entity =
            PanelEntity.create(session, view, MARKER_SIZE, "Anchor $id", parent = anchor.space)
        anchor.marker = view
        showState(view, id, anchor.state)
        settleTransactions()
        entity.setPose(Pose(MARKER_POSITION))
        applyPanelScale(entity)
        settleTransactions()
    }

    private fun showState(marker: TextView, id: Int, state: AnchorSpace.State) {
        marker.text = "Anchor $id: $state"
        marker.setBackgroundColor(toAnchorState(state).color)
    }

    private fun fail(id: Int, message: String) {
        failedIds += id
        updateStatus(id) { copy(state = SceneCoreAnchorState.FAILED) }
        logError("Anchor $id: $message")
    }

    private fun updateStatus(id: Int, update: SceneCoreAnchorStatus.() -> SceneCoreAnchorStatus) {
        SceneCorePanelStateHolder.status.update { status ->
            val anchor = status.anchors[id] ?: SceneCoreAnchorStatus()
            status.copy(anchors = status.anchors + (id to anchor.update()))
        }
    }

    private fun describe(trackingState: TrackingState): String =
        when (trackingState) {
            TrackingState.TRACKING -> "TRACKING"
            TrackingState.PAUSED -> "PAUSED"
            TrackingState.STOPPED -> "STOPPED"
            else -> trackingState.toString()
        }

    /** What the phone and the anchor's marker show for an AnchorSpace in [state]. */
    private fun toAnchorState(state: AnchorSpace.State): SceneCoreAnchorState =
        when (state) {
            AnchorSpace.State.ANCHORED -> SceneCoreAnchorState.ANCHORED
            AnchorSpace.State.UNANCHORED -> SceneCoreAnchorState.UNANCHORED
            // TIMED_OUT only comes from plane search, which this sample doesn't use.
            else -> SceneCoreAnchorState.ERROR
        }

    private fun logEvent(message: String) = SceneCorePanelStateHolder.logEvent(TAG, message)

    private fun logError(message: String) =
        SceneCorePanelStateHolder.logEvent(TAG, message, isError = true)

    private companion object {
        const val TAG = "SceneCoreAnchors"
        const val REPORT_INTERVAL_MILLIS = 500L

        val MARKER_SIZE = IntSize2d(800, 160)
        /** Just below a default-sized panel at the anchor. */
        val MARKER_POSITION = Vector3(0f, -0.33f, 0f)
    }
}
