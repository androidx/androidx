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

import android.app.UiAutomation
import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.util.regex.Pattern
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Represents an Android XR Task within the spatial scene graph.
 *
 * A spatial task encapsulates an application's root container (`space-root-task-<taskId>`), its
 * window leashes, 2D surfaces, and optional 3D Subspace content ([spatialTransform]).
 */
public class SpatialTask
@JvmOverloads
constructor(
    /** The root [SpatialNode] representing the task container in the scene graph. */
    public val activitySpaceRoot: SpatialNode,
    /** Optional package name to initialize this task with, or null to auto-extract from nodes. */
    packageName: String? = null,
) {

    /** The integer task ID extracted from `space-root-task-<taskId>`. */
    public val taskId: Int =
        activitySpaceRoot.name.removePrefix("space-root-task-").substringBefore(' ').toIntOrNull()
            ?: -1

    /** The package name associated with this task, if resolved or discovered. */
    public val packageName: String? = packageName ?: extractPackageFromNodes(activitySpaceRoot)

    /** The node name of the task root (e.g. `space-root-task-<taskId>`). */
    public val name: String
        get() = activitySpaceRoot.name

    /** System transform node for 3D spatial/Subspace content, or null. */
    public val spatialTransform: SpatialNode? by lazy {
        activitySpaceRoot.children.firstOrNull { it.name.startsWith("space-transform-spatial-") }
    }

    /** Flat list of all nodes belonging to this task hierarchy. */
    public val allNodes: List<SpatialNode> by lazy {
        buildList {
            fun collect(node: SpatialNode) {
                add(node)
                for (child in node.children) {
                    collect(child)
                }
            }
            collect(activitySpaceRoot)
        }
    }

    /** All active raycast and touch input targets within this task. */
    public val inputTargets: List<SpatialNode> by lazy { allNodes.filter { it.isInputTarget } }

    /** The task's primary window leash container ([SpatialNode.isTaskWindowLeash]), if present. */
    public val taskWindowLeash: SpatialNode?
        get() = findNode { it.isTaskWindowLeash }

    /**
     * All embedded Activity task window leash containers ([SpatialNode.isActivityWindowLeash]) in
     * this task.
     */
    public val activityWindowLeashes: List<SpatialNode>
        get() = findNodes { it.isActivityWindowLeash }

    /**
     * All embedded spatial window leash containers ([SpatialNode.isEmbeddedWindowLeash]) in this
     * task.
     */
    public val embeddedWindowLeashes: List<SpatialNode>
        get() = findNodes { it.isEmbeddedWindowLeash }

    /** All spatial window leash containers ([SpatialNode.isWindowLeash]) in this task. */
    public val windowLeashes: List<SpatialNode>
        get() = findNodes { it.isWindowLeash }

    /**
     * All top-level window leash containers in this task (window leashes that are not nested inside
     * another window leash).
     */
    public val topLevelWindowLeashes: List<SpatialNode>
        get() = findNodes { it.isTopLevelWindowLeash }

    /** All 3D subspace nodes (`hasSubspace=true`) in this task. */
    public val subspaceNodes: List<SpatialNode>
        get() = findNodes { it.hasSubspace }

    /**
     * True if this task's root container is visible and active in the scene graph and hosts an
     * active 3D spatial transform or visible window leash.
     */
    public val isActive: Boolean
        get() =
            activitySpaceRoot.isVisible &&
                activitySpaceRoot.properties["ImpNodeIsActive"] != "false" &&
                (isFullSpace ||
                    taskWindowLeash?.isVisible == true ||
                    windowLeashes.any { it.isVisible })

    /**
     * True if this task has a visible 3D spatial transform node (`space-transform-spatial-<id>`) in
     * the scene graph, indicating Full Space Mode is active for the task.
     */
    public val isFullSpace: Boolean
        get() =
            spatialTransform?.let {
                it.isVisible && it.properties["ImpNodeIsActive"] != "false"
            } == true

    /**
     * Effective virtual pixel density in pixels per meter for this task, resolved from
     * [SpatialNode.computedPixelsPerMeter] on the task hierarchy or derived from 2D surface bounds,
     * or `null` if unavailable.
     */
    public val computedPixelsPerMeter: Float?
        get() =
            (spatialTransform ?: activitySpaceRoot).computedPixelsPerMeter
                ?: allNodes.firstNotNullOfOrNull { it.computedPixelsPerMeter }
                ?: allNodes.firstNotNullOfOrNull { node ->
                    val px = node.surfaceBoundsInPixels
                    val m = node.surfaceBoundsInMeters
                    if (px != null && m != null && px.x > 0f && m.x > 0f) px.x / m.x else null
                }

    /**
     * Returns `true` if this task and its spatial scene have settled with no in-flight transitions
     * or alpha animations, and its panels match the expected visibility and count.
     *
     * @param expectedEmbeddedPanelCount Expected number of visible [embeddedWindowLeashes] with
     *   non-null [SpatialNode.effectiveSurfaceBoundsInMeters], or `null` to not constrain the
     *   embedded panel count.
     * @param expectedMainPanelVisible Expected visibility of [taskWindowLeash], or `null` to not
     *   constrain the main panel visibility.
     * @param requireFullSpace Whether the task must be in Full Space Mode ([isFullSpace]).
     */
    @JvmOverloads
    public fun isSettled(
        expectedEmbeddedPanelCount: Int? = null,
        expectedMainPanelVisible: Boolean? = null,
        requireFullSpace: Boolean = true,
    ): Boolean {
        if (!isActive) return false
        if (requireFullSpace && !isFullSpace) return false
        if (activitySpaceRoot.scene?.hasActiveTransition == true) return false
        if (allNodes.any { it.name.contains("transition snapshot", ignoreCase = true) }) {
            return false
        }
        val alphasSettled =
            allNodes
                .filter { it.isVisible }
                .all { node ->
                    val requested = node.properties["requestedAlpha"]?.toFloatOrNull()
                    val computed = node.properties["computedAlpha"]?.toFloatOrNull()
                    requested == null || computed == null || abs(requested - computed) <= 0.01f
                }
        if (!alphasSettled) return false

        if (
            expectedMainPanelVisible != null &&
                (taskWindowLeash?.isVisible == true) != expectedMainPanelVisible
        ) {
            return false
        }

        if (expectedEmbeddedPanelCount != null) {
            val visiblePanels = embeddedWindowLeashes.count {
                it.isVisible && it.effectiveSurfaceBoundsInMeters != null
            }
            if (visiblePanels != expectedEmbeddedPanelCount) return false
        }

        return true
    }

    /** Finds all nodes matching [predicate] within this task. */
    public fun findNodes(predicate: (SpatialNode) -> Boolean): List<SpatialNode> =
        allNodes.filter(predicate)

    /** Finds the first node matching [predicate] within this task, or null. */
    public fun findNode(predicate: (SpatialNode) -> Boolean): SpatialNode? =
        allNodes.firstOrNull(predicate)

    /** Finds all nodes whose name contains [nameSubstring] (case-insensitive) within this task. */
    public fun findNodesByName(nameSubstring: String): List<SpatialNode> = allNodes.filter {
        it.name.contains(nameSubstring, ignoreCase = true)
    }

    /**
     * Finds the first node whose name contains [nameSubstring] (case-insensitive) within this task.
     */
    public fun findNodeByName(nameSubstring: String): SpatialNode? = allNodes.firstOrNull {
        it.name.contains(nameSubstring, ignoreCase = true)
    }

    override fun toString(): String =
        "SpatialTask(taskId=$taskId, packageName=$packageName, nodes=${allNodes.size})"

    internal companion object {
        private val PACKAGE_NAME_PATTERN =
            Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)+$")

        internal fun extractPackageFromNodes(root: SpatialNode): String? {
            fun search(node: SpatialNode): String? {
                val segment = node.name.substringBefore('/').trim()
                if (PACKAGE_NAME_PATTERN.matcher(segment).matches()) {
                    return segment
                }
                for (child in node.children) {
                    val found = search(child)
                    if (found != null) return found
                }
                return null
            }
            return search(root)
        }
    }

    /**
     * Crops [bitmap] to the rendered content bounds of this task (plus optional [padding]),
     * delegating to [spatialTransform] or [activitySpaceRoot].
     *
     * @param bitmap Source [Bitmap] from the XR compositor.
     * @param padding Extra padding in pixels to preserve around the cropped content bounds.
     * @return A software-backed [Bitmap.Config.ARGB_8888] [Bitmap] containing the cropped content.
     */
    @JvmOverloads
    public fun crop(
        bitmap: Bitmap,
        padding: Int = 0,
    ): Bitmap = (spatialTransform ?: activitySpaceRoot).crop(bitmap, padding)

    /**
     * Captures the 3D XR compositor output and automatically crops the screenshot to the rendered
     * bounds of this task's main spatial transform or activity space root.
     *
     * @param padding Extra padding in pixels to preserve around the cropped content bounds.
     * @param timeout Maximum duration to wait for consecutive frames to stabilize.
     * @param pollInterval Delay between successive compositor captures.
     * @param uiAutomation The [UiAutomation] instance used to capture the compositor frame.
     * @return A cropped software-backed [Bitmap.Config.ARGB_8888] [Bitmap] owned by the caller.
     */
    @JvmOverloads
    public fun captureToBitmap(
        padding: Int = 0,
        timeout: Duration = 5.seconds,
        pollInterval: Duration = 100.milliseconds,
        uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation,
    ): Bitmap =
        (spatialTransform ?: activitySpaceRoot).captureToBitmap(
            padding = padding,
            timeout = timeout,
            pollInterval = pollInterval,
            uiAutomation = uiAutomation,
        )
}
