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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Represents a point-in-time snapshot of the spatial scene graph.
 *
 * Provides a structured, queryable model of all root nodes, tasks, window leashes, input targets,
 * and spatial transforms present in the XR compositor.
 */
public class SpatialScene
@JvmOverloads
constructor(
    /** Top-level root nodes in the spatial scene graph (typically the single `Root` node). */
    public val rootNodes: List<SpatialNode>,
    /** Optional mapping from task IDs to Android package names. */
    public val taskPackageMap: Map<Int, String> = emptyMap(),
) {
    init {
        fun attach(n: SpatialNode) {
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
     * Discovers all structured tasks in the scene graph.
     *
     * In the Android XR CPM (`dumpsys spf_cpm`) architecture, Window Manager creates
     * `space-root-task-<taskId>` containers directly under the compositor `Root` node. Each
     * container defines the root reference space for an independent Android task. Task roots are
     * guaranteed to be direct children of the root node (or the root nodes themselves when
     * evaluating an isolated task subtree) and never nest within other tasks or intermediate
     * wrapper nodes. Consequently, task discovery only inspects top-level roots and their direct
     * children without recursing down the hierarchy.
     */
    public val tasks: List<SpatialTask> by lazy {
        val taskRoots = rootNodes.flatMap { root ->
            // In CPM dumpsys, task roots are always direct children of the compositor Root node
            // (or the root itself if scoped to a task subtree). Intermediate wrapper nodes are not
            // used by Window Manager/CPM for task roots, so deeper recursion is unnecessary.
            if (root.isTaskRoot) listOf(root) else root.children.filter { it.isTaskRoot }
        }
        taskRoots.map { root ->
            val taskId =
                root.name.removePrefix("space-root-task-").substringBefore(' ').toIntOrNull() ?: -1
            val pkg = taskPackageMap[taskId] ?: SpatialTask.extractPackageFromNodes(root)
            SpatialTask(root, pkg)
        }
    }

    /** Finds the first task belonging to [packageName], or null if not found. */
    public fun findTaskByPackageName(packageName: String): SpatialTask? = tasks.firstOrNull {
        it.packageName == packageName
    }

    /** Finds all tasks belonging to [packageName]. */
    public fun findTasksByPackageName(packageName: String): List<SpatialTask> = tasks.filter {
        it.packageName == packageName
    }

    /** Finds a task by its [taskId], or null if not found. */
    public fun findTaskById(taskId: Int): SpatialTask? = tasks.firstOrNull { it.taskId == taskId }

    /** Finds the task associated with the given [activity], or null if not found. */
    public fun findTaskByActivity(activity: Activity): SpatialTask? =
        findTaskById(activity.taskId) ?: findTaskByPackageName(activity.packageName)

    /**
     * The currently active foreground [SpatialTask] in the scene graph, or `null` if no task is
     * active.
     */
    public val activeTask: SpatialTask?
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
                .filter { it.properties["transformUpdateRequest.coordinateSpace"] == "2" }
        val selected =
            headTransformNodes.firstOrNull {
                it.properties["ImpNodeIsActive"] == "true" ||
                    it.parent?.properties?.get("ImpNodeIsActive") == "true"
            } ?: headTransformNodes.firstOrNull()
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

    override fun toString(): String = "SpatialScene(tasks=$tasks, rootNodes=${rootNodes.size})"
}
