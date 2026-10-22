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

import android.util.Log
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Vector3
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** One panel that the glasses-side [SceneCorePanelActivity] should show, as set up on the phone. */
internal data class SceneCorePanelSpec(
    /** Spawn order, also shown on the panel so it can be told apart on the glasses. */
    val id: Int,
    /**
     * Position of the panel relative to its parent, in meters. In the activity space, the origin
     * sits at the activity window plane with +Z pointing toward the viewer, so a positive Z places
     * the panel between the window and the viewer. On an anchor, the origin is the anchor.
     */
    val position: Vector3,
    /** Background color of the panel, as an ARGB color int. */
    val color: Int,
    /** Size of the panel in pixels, at [SceneCorePanelActivity]'s virtual pixel density. */
    val sizeInPixels: IntSize2d,
    /**
     * The [SceneCoreAnchorSpec.id] of the anchor that the panel is attached to, or null to keep the
     * panel in the activity space.
     */
    val anchorId: Int? = null,
) {
    companion object {
        /** In front of the viewer, in the activity space. */
        val DEFAULT_POSITION = Vector3(0f, 0f, 0.9f)
    }
}

/** One ARCore anchor that [SceneCorePanelActivity] should create and wrap in an AnchorSpace. */
internal data class SceneCoreAnchorSpec(
    /** Creation order, also shown on the anchor's marker panel. */
    val id: Int,
    /** Where to create the anchor, in the activity space at creation time, in meters. */
    val position: Vector3,
    /**
     * Whether to detach the ARCore anchor while its AnchorSpace still uses it. Apps must not do
     * this; it shows what the runtime does when they do.
     */
    val isDetachRequested: Boolean = false,
)

/** The panels and anchors that the phone wants the glasses to show, in creation order. */
internal data class SceneCoreSceneSpec(
    val panels: List<SceneCorePanelSpec> = emptyList(),
    val anchors: List<SceneCoreAnchorSpec> = emptyList(),
) {
    /** Adds [anchor], and attaches panel [panelId] to it where the panel is. */
    fun pinPanel(panelId: Int, anchor: SceneCoreAnchorSpec): SceneCoreSceneSpec =
        copy(anchors = anchors + anchor).updatePanels({ it.id == panelId }) {
            it.copy(position = Vector3.Zero, anchorId = anchor.id)
        }

    /**
     * Moves panel [panelId] to its next parent: the activity space, then each anchor in turn,
     * skipping [failedAnchorIds]. The panel starts at the anchor, or back in front of the viewer.
     */
    fun withNextParent(panelId: Int, failedAnchorIds: Set<Int> = emptySet()): SceneCoreSceneSpec {
        val panel = panels.firstOrNull { it.id == panelId } ?: return this
        val parents = listOf<Int?>(null) + anchors.map { it.id }.filterNot { it in failedAnchorIds }
        val next = parents[(parents.indexOf(panel.anchorId) + 1) % parents.size]
        val position = if (next == null) SceneCorePanelSpec.DEFAULT_POSITION else Vector3.Zero
        return updatePanels({ it.id == panelId }) { it.copy(anchorId = next, position = position) }
    }

    /** Removes anchor [id]. Its panels go back in front of the viewer. */
    fun withoutAnchor(id: Int): SceneCoreSceneSpec =
        copy(anchors = anchors.filterNot { it.id == id }).updatePanels({ it.anchorId == id }) {
            it.copy(anchorId = null, position = SceneCorePanelSpec.DEFAULT_POSITION)
        }

    /** Asks the glasses to detach the ARCore anchor of anchor [id], see [SceneCoreAnchorSpec]. */
    fun withDetachRequested(id: Int): SceneCoreSceneSpec =
        copy(anchors = anchors.map { if (it.id == id) it.copy(isDetachRequested = true) else it })

    /**
     * Moves the panels on the anchors [anchorIds] to the activity space, about where they were: at
     * the position where the anchor was created, plus their offset from the anchor.
     */
    fun releasePanels(anchorIds: Set<Int>): SceneCoreSceneSpec {
        val anchorPositions = anchors.associate { it.id to it.position }
        return updatePanels({ it.anchorId in anchorIds }) { panel ->
            val anchorPosition = anchorPositions[panel.anchorId] ?: Vector3.Zero
            panel.copy(anchorId = null, position = anchorPosition + panel.position)
        }
    }

    private fun updatePanels(
        predicate: (SceneCorePanelSpec) -> Boolean,
        update: (SceneCorePanelSpec) -> SceneCorePanelSpec,
    ) = copy(panels = panels.map { if (predicate(it)) update(it) else it })
}

/**
 * The state of one anchor, as the phone and the anchor's marker show it: the AnchorSpace state once
 * there is one, otherwise how far the glasses got.
 */
internal enum class SceneCoreAnchorState(val color: Int) {
    /** The phone asked for the anchor, and the glasses haven't reported on it yet. */
    REQUESTED(PENDING_COLOR),
    /** The glasses are creating the ARCore anchor and its AnchorSpace. */
    CREATING(PENDING_COLOR),
    UNANCHORED(PENDING_COLOR),
    ANCHORED(ANCHORED_COLOR),
    ERROR(ERROR_COLOR),
    /** The AnchorSpace reported ERROR and was disposed, which moved its panels back. */
    DISPOSED(ERROR_COLOR),
    /** The ARCore anchor or its AnchorSpace couldn't be created. The event log says why. */
    FAILED(ERROR_COLOR);

    /** Whether panels can no longer follow this anchor. */
    val isFailedOrDisposed: Boolean
        get() = this == FAILED || this == DISPOSED
}

private const val PENDING_COLOR = 0xFFE65100.toInt()
private const val ANCHORED_COLOR = 0xFF1B5E20.toInt()
private const val ERROR_COLOR = 0xFFB71C1C.toInt()

/** What [SceneCorePanelActivity] reports about one anchor. */
internal data class SceneCoreAnchorStatus(
    val state: SceneCoreAnchorState = SceneCoreAnchorState.CREATING,
    /** The tracking state of the ARCore anchor, once known. */
    val trackingState: String? = null,
    /** The native `XrSpace` of the anchor, or null if it has none, as with the ARCore fakes. */
    val nativeSpaceHandle: Long? = null,
    /** Where the AnchorSpace is in the activity space, in meters. */
    val activitySpacePosition: Vector3? = null,
    /** How many times the AnchorSpace reported that its origin moved. */
    val originUpdates: Int = 0,
    /** Whether the ARCore anchor was detached while its AnchorSpace still used it. */
    val isDetached: Boolean = false,
)

/** What the glasses report back to the phone. */
internal data class SceneCoreStatus(
    /** The runtimes that the Session picked, or null while there is no Session. */
    val runtimes: String? = null,
    /** Whether the Session uses the ARCore fakes, whose anchors have no `XrSpace`. */
    val usesArCoreFakes: Boolean = false,
    /** Whether the Session is configured, which ARCore needs to update anchors. */
    val isConfigured: Boolean = false,
    /**
     * Whether the scenecore-openxr native library can attach entities to an `XrSpace`, or null if
     * that wasn't checked yet.
     */
    val attachmentNatives: String? = null,
    /** The most recent error. */
    val lastError: String? = null,
    val anchors: Map<Int, SceneCoreAnchorStatus> = emptyMap(),
    /** Recent events, oldest first. */
    val events: List<String> = emptyList(),
) {
    /** The IDs of anchors that failed, or whose AnchorSpace was disposed. */
    val failedAnchorIds: Set<Int>
        get() = anchors.filterValues { it.state.isFailedOrDisposed }.keys
}

/** Formats a native handle for display, or returns "none" if there is no handle. */
internal fun describeNativeHandle(handle: Long?): String =
    if (handle == null) "none" else "0x${java.lang.Long.toHexString(handle)}"

/**
 * In-process bridge between [SceneCoreOpenXrLauncherActivity] on the phone and
 * [SceneCorePanelActivity] on the glasses: both run in the same process, so shared flows are all
 * that is needed to drive the scene and to report back.
 */
internal object SceneCorePanelStateHolder {
    /** How long to wait by default before attaching panels to a new anchor. */
    const val DEFAULT_ANCHOR_PARENTING_DELAY_MILLIS = 1000L

    private const val MAX_EVENTS = 30

    /** The panels and anchors that should currently exist. */
    val scene = MutableStateFlow(SceneCoreSceneSpec())

    /**
     * Whether to dispose an AnchorSpace once it reports ERROR. Otherwise its panels stay attached
     * to it, which shows where the runtime draws them after the error.
     */
    val disposeAnchorSpaceOnError = MutableStateFlow(true)

    /**
     * How long to wait after creating an AnchorSpace before attaching panels to it. The Vitreous
     * runtime refuses new scene transactions while it associates a new anchor, and the OpenXR
     * SceneCore runtime drops parent changes when no transaction is available.
     */
    val anchorParentingDelayMillis = MutableStateFlow(DEFAULT_ANCHOR_PARENTING_DELAY_MILLIS)

    /** Written by the glasses, read by the phone. */
    val status = MutableStateFlow(SceneCoreStatus())

    /**
     * Whether [SceneCorePanelActivity] exists. Starting it again while it runs delivers a new
     * intent, which pauses and resumes it, and its Session with it.
     */
    @Volatile var isPanelActivityRunning = false

    /**
     * Logs [message] and adds it to [SceneCoreStatus.events], and to [SceneCoreStatus.lastError] if
     * [isError].
     */
    fun logEvent(tag: String, message: String, isError: Boolean = false) {
        if (isError) Log.w(tag, message) else Log.i(tag, message)
        val event = "${SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())}  $message"
        status.update {
            it.copy(
                lastError = if (isError) message else it.lastError,
                events = (it.events + event).takeLast(MAX_EVENTS),
            )
        }
    }
}
