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

@file:Suppress("PrimitiveInCollection", "BanThreadSleep")

package androidx.xr.testutils

import android.app.Activity
import android.app.UiAutomation
import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.runtime.math.Pose
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Holds attached tree counts and summed buffer pixel area for a [SnapshotXrScene].
 *
 * @property attachedNodes total [SnapshotXrSpace] count across [SnapshotXrScene.rootNodes]
 * @property bufferPixels total 2D buffer area in pixels (`width * height` summed across
 *   [SnapshotXrSpace.bufferSize] in [SnapshotXrScene.rootNodes])
 */
public data class SpatialSceneStats
@JvmOverloads
constructor(
    public val attachedNodes: Int = 0,
    public val bufferPixels: Long = 0L,
)

/**
 * Computes [SpatialSceneStats] directly from [rootNodes].
 *
 * Returns `null` when [rootNodes] is empty, preventing empty or failed shell dumps from passing
 * leak checks vacuously.
 */
internal fun computeSceneStats(rootNodes: List<SnapshotXrSpace>): SpatialSceneStats? {
    var attachedNodes = 0
    var bufferPixels = 0L
    val visited = Collections.newSetFromMap(IdentityHashMap<SnapshotXrSpace, Boolean>())
    val stack = ArrayDeque<SnapshotXrSpace>(rootNodes.size)
    for (root in rootNodes) {
        if (visited.add(root)) {
            stack.addLast(root)
        }
    }
    while (stack.isNotEmpty()) {
        val node = stack.removeLast()
        attachedNodes++
        val size = node.bufferSize
        if (size != null && size.x.isFinite() && size.y.isFinite() && size.x > 0f && size.y > 0f) {
            bufferPixels += size.x.roundToLong() * size.y.roundToLong()
        }
        for (child in node.children) {
            if (visited.add(child)) {
                stack.addLast(child)
            }
        }
    }
    if (attachedNodes == 0) {
        return null
    }
    return SpatialSceneStats(
        attachedNodes = attachedNodes,
        bufferPixels = bufferPixels,
    )
}

/**
 * Represents a point-in-time snapshot of the OpenXR spatial compositor scene graph.
 *
 * Provides a structured, queryable model of all root spaces, task spaces, window leashes, input
 * targets, and spatial transforms present in the XR compositor.
 */
public class SnapshotXrScene
@JvmOverloads
constructor(
    /** Top-level root spaces in the spatial scene graph (typically the single `Root` space). */
    public val rootNodes: List<SnapshotXrSpace>,
    /** Optional mapping from task IDs to Android package names. */
    public val taskPackageMap: Map<Int, String> = emptyMap(),
    /** Optional compositor node and buffer totals for this scene snapshot. */
    public val stats: SpatialSceneStats? = computeSceneStats(rootNodes),
) {
    /** Whether this snapshot was decoded from binary protobuf dumps (`dumpsys spf_cpm --proto`). */
    internal var isProtoCapture: Boolean = false
        private set

    internal constructor(
        rootNodes: List<SnapshotXrSpace>,
        taskPackageMap: Map<Int, String> = emptyMap(),
        stats: SpatialSceneStats? = computeSceneStats(rootNodes),
        isProtoCapture: Boolean,
    ) : this(
        rootNodes = rootNodes,
        taskPackageMap = taskPackageMap,
        stats = stats,
    ) {
        this.isProtoCapture = isProtoCapture
    }

    init {
        fun attach(n: SnapshotXrSpace) {
            n.scene = this
            for (c in n.children) {
                attach(c)
            }
        }
        for (r in rootNodes) {
            attach(r)
        }
    }

    /**
     * Discovers all application task root spaces in the scene graph.
     *
     * In the Android XR CPM (`dumpsys spf_cpm`) architecture, Window Manager creates
     * `space-root-task-<taskId>` containers directly under the compositor `Root` node. Each
     * container defines the root reference space (`XrSpace`) for an independent Android task. Task
     * roots are guaranteed to be direct children of the root node (or the root nodes themselves
     * when evaluating an isolated task subtree) and never nest within other tasks or intermediate
     * wrapper nodes. Consequently, task discovery only inspects top-level roots and their direct
     * children without recursing down the hierarchy.
     */
    public val tasks: List<SnapshotXrSpace> by lazy {
        rootNodes.flatMap { root ->
            // In CPM dumpsys, task roots are always direct children of the compositor Root node
            // (or the root itself if scoped to a task subtree). Intermediate wrapper nodes are not
            // used by Window Manager/CPM for task roots, so deeper recursion is unnecessary.
            if (root.isTaskRoot) listOf(root) else root.children.filter { it.isTaskRoot }
        }
    }

    /** Finds the first task root space belonging to [packageName], or null if not found. */
    public fun findTaskByPackageName(packageName: String): SnapshotXrSpace? = tasks.firstOrNull {
        it.packageName == packageName
    }

    /** Finds all task root spaces belonging to [packageName]. */
    public fun findTasksByPackageName(packageName: String): List<SnapshotXrSpace> = tasks.filter {
        it.packageName == packageName
    }

    /** Finds a task root space by its [taskId], or null if not found. */
    public fun findTaskById(taskId: Int): SnapshotXrSpace? = tasks.firstOrNull {
        it.taskId == taskId
    }

    /** Finds the task root space associated with the given [activity], or null if not found. */
    public fun findTaskByActivity(activity: Activity): SnapshotXrSpace? =
        findTaskById(activity.taskId) ?: findTaskByPackageName(activity.packageName)

    /**
     * The currently active foreground task root [SnapshotXrSpace] in the scene graph, or `null` if
     * no task is active.
     */
    public val activeTask: SnapshotXrSpace?
        get() = tasks.firstOrNull { it.isActive }

    /**
     * True if the scene graph currently contains in-flight window transition snapshots, an active
     * system transition scrim, or a visible transient system toast overlay.
     */
    public val hasActiveTransition: Boolean
        get() = rootNodes.any { root ->
            root.findNode { node ->
                node.name.contains("transition snapshot", ignoreCase = true) ||
                    (node.name == "Scrim" && node.isVisible && node.scrimTargetAlpha > 0.01f) ||
                    node.isVisibleToastOverlay
            } != null
        }

    /**
     * The world-space pose of the user's head (`HEAD` coordinate space,
     * `transformUpdateRequest.coordinateSpace=2` in `dumpsys spf_cpm`) if a head-locked transform
     * node is present in the scene graph, or `null` if unavailable.
     */
    public val headPose: Pose? by lazy {
        val headTransformNodes =
            rootNodes
                .flatMap { listOf(it) + it.getDescendants() }
                .filter { it.coordinateSpace == 2 }
        val selected =
            headTransformNodes.firstOrNull { it.isNodeActive || it.parent?.isNodeActive == true }
                ?: headTransformNodes.firstOrNull()
        selected?.let { transformNode ->
            val parent = transformNode.parent
            if (parent != null && parent.name != "Root") parent.pose else transformNode.pose
        }
    }

    /**
     * Crops [bitmap] to the foreground content of this scene (plus optional [padding]), cropping
     * out the surrounding black environment background.
     *
     * Delegates to [cropForeground].
     *
     * @param bitmap Source [Bitmap] from the XR compositor.
     * @param padding Extra padding in pixels to preserve around the cropped content bounds.
     * @return A software-backed [Bitmap.Config.ARGB_8888] [Bitmap] containing the cropped
     *   foreground.
     */
    @JvmOverloads
    public fun crop(
        bitmap: Bitmap,
        padding: Int = 0,
    ): Bitmap = cropForeground(bitmap, padding)

    /**
     * Captures the 3D XR compositor output and automatically crops the screenshot to the rendered
     * foreground content of the entire scene, cropping out the surrounding black background.
     *
     * This is particularly useful for scenes containing 3D entities (such as GLTF models) that do
     * not have 2D surface bounds in CPM.
     *
     * @param padding Extra padding in pixels to preserve around the cropped content bounds.
     * @param timeout Maximum duration to wait for consecutive frames to stabilize.
     * @param pollInterval Delay between successive compositor captures.
     * @param uiAutomation The [UiAutomation] instance used to capture the compositor frame.
     * @return A cropped software-backed [Bitmap.Config.ARGB_8888] [Bitmap] owned by the caller.
     * @throws AssertionError if no screenshot could be captured within [timeout].
     */
    @JvmOverloads
    public fun captureToBitmap(
        padding: Int = 0,
        timeout: Duration = 5.seconds,
        pollInterval: Duration = 100.milliseconds,
        uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation,
    ): Bitmap =
        captureSceneToBitmap(
            cropBounds = null,
            padding = padding,
            timeout = timeout,
            pollInterval = pollInterval,
            uiAutomation = uiAutomation,
        )

    override fun toString(): String = "SnapshotXrScene(tasks=$tasks, rootNodes=${rootNodes.size})"
}
