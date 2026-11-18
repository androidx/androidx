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
import android.graphics.Rect
import android.os.Looper
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector2
import androidx.xr.runtime.math.Vector3
import androidx.xr.testutils.runtime.cpm.captureSceneWithPolling
import java.util.regex.Pattern
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Represents a point-in-time snapshot of an individual OpenXR reference space (`XrSpace`) and its
 * associated scene entity (`XrSceneEntityEXTX1`) or composition layer (`XrCompositionLayerQuad`) in
 * the spatial compositor hierarchy.
 *
 * Each space represents a 3D transformable reference frame, application task root container
 * (`space-root-task-<taskId>`), window leash, spatial panel, 3D Subspace anchor, or system UI
 * affordance in the scene tree. Provides canonical geometric transforms (position, rotation, scale,
 * pose) compatible with OpenXR scene models, surface and buffer dimensions, input target
 * configuration, task and hierarchy navigation, settlement checks, and screenshot
 * capturing/cropping utilities.
 */
public class SnapshotXrSpace
@JvmOverloads
constructor(
    /** Raw header line from dumpsys output (e.g., `Node Root (id=0)`). */
    public val header: String = "",
    /** Space name identifier (e.g., `Root`, `space-root-task-123`, `task-123-window`). */
    public val name: String = "",
    /** Unique node ID within the spatial compositor scene graph, or -1 if unassigned. */
    public val id: Int = -1,
    /** Indentation / hierarchy depth within the scene tree. */
    public val depth: Int = 0,
    /** World position of the space in meters. */
    public val position: Vector3 = Vector3.Zero,
    /** World rotation quaternion of the space. */
    public val rotation: Quaternion = Quaternion.Identity,
    /** Scale factors along (x, y, z) axes. */
    public val scale: Vector3 = Vector3.One,
    /** Optional initial 2D surface dimensions `(width, height)` in meters. */
    surfaceBoundsInMeters: Vector2? = null,
    /** Optional initial 2D surface dimensions `(width, height)` in pixels. */
    surfaceBoundsInPixels: Vector2? = null,
    /** Optional initial 2D input touchable region bounds in pixels. */
    touchableRegion: Rect? = null,
    /** Buffer dimensions `(width, height)` in pixels of the underlying surface. */
    public val bufferSize: Vector2? = null,
    /** Scaling factor indicating how many pixels correspond to one meter. */
    computedPixelsPerMeter: Float? = null,
    /** Unique identifier for the input target associated with this space, or -1. */
    public val inputId: Int = -1,
    /** Whether this space is capable of receiving input events. */
    public val canReceiveInput: Boolean = true,
    /** Whether this space represents a 2D Android View or SurfaceView. */
    public val isAndroid2d: Boolean = false,
    /** Whether this space is an embedded window leash (e.g. spatial panel). */
    public val isEmbeddedWindowLeash: Boolean = false,
    /** Whether this space represents a system UI element (caption bar, drag handle, etc.). */
    public val isSysUi: Boolean = false,
    /** Whether this space represents or anchors a 3D Subspace. */
    public val hasSubspace: Boolean = false,
    /** Whether this space supports spatial movement/dragging. */
    isMovable: Boolean = false,
    /** Whether this space supports spatial resizing. */
    isResizable: Boolean = false,
    /** Stereo eye visibility mode (e.g. `"visible"`, `"hidden on both"`). */
    public val stereoVisibility: String? = null,
    /** Initial visibility flag indicating whether the space is marked visible. */
    isVisible: Boolean = true,
    /** Coordinate space identifier (e.g. 2 for HEAD space). */
    internal val coordinateSpace: Int? = null,
    /** Whether the node is active in the compositor. */
    isActive: Boolean = true,
    /** System UI special node type code (e.g. 1=Move, 2=Resize, 4=Caption). */
    internal val sysUiSpecialNode: Int? = null,
    /** Target alpha transparency for scrim background rendering. */
    internal val scrimTargetAlpha: Float = 0f,
    /** Raw key-value map of float array attributes parsed from dumpsys. */
    public val attributes: Map<String, FloatArray> = emptyMap(),
    /** Raw key-value map of string properties parsed from dumpsys. */
    public val properties: Map<String, String> = emptyMap(),
    /** Optional explicit package name associated with this task space. */
    packageName: String? = null,
) {
    internal val isNodeActive: Boolean = isActive
    private val directComputedPixelsPerMeter: Float? = computedPixelsPerMeter
    private val directIsMovable: Boolean = isMovable
    private val directIsResizable: Boolean = isResizable
    private val explicitPackageName: String? = packageName

    /** Whether this space (or its parent container) supports spatial movement/dragging. */
    public val isMovable: Boolean
        get() = directIsMovable || (parent?.directIsMovable == true)

    /** Whether this space (or its parent container) supports spatial resizing. */
    public val isResizable: Boolean
        get() = directIsResizable || (parent?.directIsResizable == true)

    /** The parent [SnapshotXrSpace] in the scene tree, or null if this is a root space. */
    public var parent: SnapshotXrSpace? = null
        internal set

    /** The [SnapshotXrScene] containing this space, or null if constructed standalone. */
    public var scene: SnapshotXrScene? = null
        internal set

    /** The topmost ancestor space in this space's hierarchy (the root of the tree). */
    public val root: SnapshotXrSpace
        get() {
            var curr = this
            while (curr.parent != null) {
                curr = curr.parent!!
            }
            return curr
        }

    /**
     * The world-space pose of the user's head (`HEAD` coordinate space,
     * `transformUpdateRequest.coordinateSpace=2` in `dumpsys spf_cpm`), or `null` if unavailable.
     */
    public val headPose: Pose?
        get() = scene?.headPose ?: findHeadPoseInHierarchy()

    private fun findHeadPoseInHierarchy(): Pose? {
        val rootNode = root
        val headTransformNodes = rootNode.findNodes { it.coordinateSpace == 2 }
        val selected =
            headTransformNodes.firstOrNull { it.isNodeActive || it.parent?.isNodeActive == true }
                ?: headTransformNodes.firstOrNull()
        return selected?.let { transformNode ->
            val parent = transformNode.parent
            if (parent != null && parent.name != "Root") parent.pose else transformNode.pose
        }
    }

    private val _children = mutableListOf<SnapshotXrSpace>()

    /** The direct child spaces of this space in the scene graph hierarchy. */
    public val children: List<SnapshotXrSpace>
        get() = _children

    /**
     * Adds [child] to this space's list of children, updating its parent and scene references.
     *
     * @param child The [SnapshotXrSpace] to attach as a child.
     */
    internal fun addChild(child: SnapshotXrSpace) {
        child.parent = this
        child.scene = this.scene
        if (this.scene != null) {
            for (descendant in child.getDescendants()) {
                descendant.scene = this.scene
            }
        }
        _children.add(child)
    }

    /**
     * Returns all descendants of this space in depth-first traversal order.
     *
     * @return A list containing all descendant [SnapshotXrSpace] entities.
     */
    public fun getDescendants(): List<SnapshotXrSpace> {
        val result = mutableListOf<SnapshotXrSpace>()
        fun traverse(node: SnapshotXrSpace) {
            for (child in node.children) {
                result.add(child)
                traverse(child)
            }
        }
        traverse(this)
        return result
    }

    /** Flat list of all spaces in this subtree (including `this`). */
    public val allNodes: List<SnapshotXrSpace>
        get() = listOf(this) + getDescendants()

    /** Finds the first descendant matching [predicate], or null. */
    public fun findDescendant(predicate: (SnapshotXrSpace) -> Boolean): SnapshotXrSpace? =
        getDescendants().firstOrNull(predicate)

    /**
     * Finds the first descendant whose name contains [nameSubstring] (case-insensitive), or null.
     */
    public fun findDescendantByName(nameSubstring: String): SnapshotXrSpace? =
        getDescendants().firstOrNull { it.name.contains(nameSubstring, ignoreCase = true) }

    /** Finds all descendants matching [predicate]. */
    public fun findDescendants(predicate: (SnapshotXrSpace) -> Boolean): List<SnapshotXrSpace> =
        getDescendants().filter(predicate)

    /** Finds all spaces in this subtree (including `this`) matching [predicate]. */
    public fun findNodes(predicate: (SnapshotXrSpace) -> Boolean): List<SnapshotXrSpace> =
        allNodes.filter(predicate)

    /** Finds the first space in this subtree (including `this`) matching [predicate], or null. */
    public fun findNode(predicate: (SnapshotXrSpace) -> Boolean): SnapshotXrSpace? =
        if (predicate(this)) this else findDescendant(predicate)

    /** Finds all descendants whose name contains [nameSubstring] (case-insensitive). */
    public fun findDescendantsByName(nameSubstring: String): List<SnapshotXrSpace> =
        getDescendants().filter { it.name.contains(nameSubstring, ignoreCase = true) }

    /**
     * Finds all spaces in this subtree (including `this`) whose name contains [nameSubstring]
     * (case-insensitive).
     */
    public fun findNodesByName(nameSubstring: String): List<SnapshotXrSpace> = allNodes.filter {
        it.name.contains(nameSubstring, ignoreCase = true)
    }

    /**
     * Finds the first space in this subtree (including `this`) whose name contains [nameSubstring]
     * (case-insensitive), or null.
     */
    public fun findNodeByName(nameSubstring: String): SnapshotXrSpace? = allNodes.firstOrNull {
        it.name.contains(nameSubstring, ignoreCase = true)
    }

    /** Returns true if this space is a descendant of [other]. */
    public fun isDescendantOf(other: SnapshotXrSpace): Boolean {
        var current = parent
        while (current != null) {
            if (current === other) {
                return true
            }
            current = current.parent
        }
        return false
    }

    // --- Transforms & Poses (OpenXR Scene Entity Congruent) ---

    /** The 3D [Pose] of this space combining [position] and [rotation]. */
    public val pose: Pose
        get() = Pose(position, rotation)

    /**
     * Transforms [point] from this space's local coordinate space into OpenXR world space, taking
     * into account [position], [rotation], and [scale].
     */
    public fun transformPointToWorld(point: Vector3): Vector3 {
        val scaled = point.scale(scale)
        val rotated = rotation * scaled
        return rotated + position
    }

    /** Transforms [worldPoint] from OpenXR world space into this space's local coordinate space. */
    public fun transformPointFromWorld(worldPoint: Vector3): Vector3 {
        val unrotated = rotation.inverse * (worldPoint - position)
        val sx = if (abs(scale.x) > 1e-6f) scale.x else 1.0f
        val sy = if (abs(scale.y) > 1e-6f) scale.y else 1.0f
        val sz = if (abs(scale.z) > 1e-6f) scale.z else 1.0f
        return Vector3(unrotated.x / sx, unrotated.y / sy, unrotated.z / sz)
    }

    /** Transforms [vector] direction from this space's local space to OpenXR world space. */
    public fun transformVectorToWorld(vector: Vector3): Vector3 {
        val scaled = vector.scale(scale)
        return rotation * scaled
    }

    /** Transforms [worldVector] direction from OpenXR world space into this space's local space. */
    public fun transformVectorFromWorld(worldVector: Vector3): Vector3 {
        val unrotated = rotation.inverse * worldVector
        val sx = if (abs(scale.x) > 1e-6f) scale.x else 1.0f
        val sy = if (abs(scale.y) > 1e-6f) scale.y else 1.0f
        val sz = if (abs(scale.z) > 1e-6f) scale.z else 1.0f
        return Vector3(unrotated.x / sx, unrotated.y / sy, unrotated.z / sz)
    }

    // --- Surface & Geometry Bounds ---

    /**
     * Whether this space wraps an Android task window (e.g. `window-task-<id>` or
     * `task-<id>-window`).
     */
    public val isTaskWindowWrapper: Boolean
        get() =
            name.startsWith("window-task-") ||
                (name.startsWith("task-") && name.endsWith("-window"))

    /**
     * Whether this space acts as a container for 2D surfaces, window leashes, or buffer-backed
     * children.
     */
    public val isSurfaceContainer: Boolean
        get() =
            isWindowLeash ||
                isTaskWindowWrapper ||
                name.startsWith("window-") ||
                directSurfaceBoundsInMeters != null ||
                children.any { it.bufferSize != null || it.isAndroid2d }

    private val directSurfaceBoundsInMeters: Vector2? = surfaceBoundsInMeters?.takeIf {
        it.x > 0f && it.y > 0f
    }

    /**
     * The 2D dimensions `(width, height)` in meters of the surface represented or contained by this
     * space, or `null` if undefined.
     */
    public val surfaceBoundsInMeters: Vector2?
        get() {
            if (directSurfaceBoundsInMeters != null) return directSurfaceBoundsInMeters
            if (isSurfaceContainer) {
                val orderedChildren = children.filterNot { it.isWindowLeash || it.isSysUi }
                for (child in orderedChildren) {
                    val childBounds = child.surfaceBoundsInMeters
                    if (childBounds != null) {
                        return childBounds
                    }
                }
            }
            return null
        }

    private val directSurfaceBoundsInPixels: Vector2? =
        surfaceBoundsInPixels?.takeIf { it.x > 0f && it.y > 0f }
            ?: bufferSize?.takeIf { it.x > 0f && it.y > 0f }

    /**
     * The 2D dimensions `(width, height)` in pixels of the surface represented or contained by this
     * space, or `null` if undefined.
     */
    public val surfaceBoundsInPixels: Vector2?
        get() {
            if (directSurfaceBoundsInPixels != null) return directSurfaceBoundsInPixels
            if (isSurfaceContainer) {
                val orderedChildren = children.filterNot { it.isWindowLeash || it.isSysUi }
                for (child in orderedChildren) {
                    val childPixels = child.surfaceBoundsInPixels
                    if (childPixels != null) {
                        return childPixels
                    }
                }
            }
            return null
        }

    private val directTouchableRegion: Rect? = touchableRegion

    /**
     * The 2D touchable input region bounds in pixels for this space or its surface container
     * children, or `null` if undefined.
     */
    public val touchableRegion: Rect?
        get() {
            if (directTouchableRegion != null) return directTouchableRegion
            if (isSurfaceContainer) {
                val orderedChildren = children.filterNot { it.isWindowLeash || it.isSysUi }
                for (child in orderedChildren) {
                    val childTouch = child.touchableRegion
                    if (childTouch != null) return childTouch
                }
            }
            return null
        }

    /** Whether this space has valid positive surface bounds in meters or pixels. */
    public val hasSurfaceBounds: Boolean
        get() =
            (surfaceBoundsInMeters?.let { it.x > 0f && it.y > 0f } == true) ||
                (surfaceBoundsInPixels?.let { it.x > 0f && it.y > 0f } == true)

    /**
     * The effective surface bounds `(width, height)` in meters, falling back to
     * [primarySurfaceNode] if not defined directly on this space.
     */
    public val effectiveSurfaceBoundsInMeters: Vector2?
        get() = surfaceBoundsInMeters ?: primarySurfaceNode?.surfaceBoundsInMeters

    /**
     * The effective surface bounds `(width, height)` in pixels, falling back to
     * [primarySurfaceNode] if not defined directly on this space.
     */
    public val effectiveSurfaceBoundsInPixels: Vector2?
        get() = surfaceBoundsInPixels ?: primarySurfaceNode?.surfaceBoundsInPixels

    /**
     * The effective buffer dimensions `(width, height)` in pixels, falling back to
     * [primarySurfaceNode] if not defined directly on this space.
     */
    public val effectiveBufferSize: Vector2?
        get() = bufferSize ?: primarySurfaceNode?.bufferSize

    /**
     * The 3D bounding dimensions `(width, height, depth)` in meters in world coordinates, computed
     * from surface bounds and scale factors, or `null` if undefined.
     */
    public val worldExtents: Vector3?
        get() {
            val bounds = surfaceBoundsInMeters
            val parentNode = parent
            val effectiveScale =
                if (isAndroid2d && parentNode != null && parentNode.isSurfaceContainer) {
                    parentNode.scale
                } else {
                    scale
                }
            if (bounds != null && bounds.x > 0f && bounds.y > 0f) {
                return Vector3(
                    bounds.x * effectiveScale.x,
                    bounds.y * effectiveScale.y,
                    if (effectiveScale.z > 0f) effectiveScale.z else 0.01f,
                )
            }
            if (isSurfaceContainer) {
                val orderedChildren = children.filterNot { it.isWindowLeash || it.isSysUi }
                for (child in orderedChildren) {
                    val childExtents = child.worldExtents
                    if (childExtents != null) return childExtents
                }
            }
            if (isInputTarget && effectiveScale.x > 0f && effectiveScale.y > 0f) {
                return effectiveScale
            }
            return null
        }

    /**
     * Half of [worldExtents], convenient for bounding-box extents and distance/radius calculations.
     */
    public val worldHalfExtents: Vector3?
        get() = worldExtents?.let { Vector3(it.x * 0.5f, it.y * 0.5f, it.z * 0.5f) }

    /** Whether this space represents a spatial panel (embedded window leash). */
    public val isSpatialPanel: Boolean
        get() = isEmbeddedWindowLeash

    /**
     * Whether this space represents a 2D SurfaceFlinger buffer quad layer (`isAndroid2d=true` with
     * a positive buffer or surface bounds).
     */
    public val isAndroid2dSurface: Boolean
        get() =
            isAndroid2d &&
                ((bufferSize != null && bufferSize.x > 0f && bufferSize.y > 0f) || hasSurfaceBounds)

    /** Whether this space is a window leash for an Android task window. */
    public val isTaskWindowLeash: Boolean
        get() = children.any { it.name.startsWith("window-task-") }

    /** Whether this space is a window leash for an Android activity window. */
    public val isActivityWindowLeash: Boolean
        get() = children.any { TASK_WINDOW_NODE_REGEX.matches(it.name) }

    /**
     * Whether this space is a window leash (task window leash, activity window leash, or embedded
     * window leash).
     */
    public val isWindowLeash: Boolean
        get() = isTaskWindowLeash || isActivityWindowLeash || isEmbeddedWindowLeash

    /** The nearest ancestor space that is a window leash, or `null` if none exists. */
    public val parentWindowLeash: SnapshotXrSpace?
        get() {
            var current = parent
            while (current != null) {
                if (current.isWindowLeash) return current
                current = current.parent
            }
            return null
        }

    /**
     * Whether this space is a top-level window leash without any enclosing parent window leashes.
     */
    public val isTopLevelWindowLeash: Boolean
        get() = isWindowLeash && parentWindowLeash == null

    /**
     * The primary child or self [SnapshotXrSpace] responsible for rendering an Android 2D surface.
     */
    public val primarySurfaceNode: SnapshotXrSpace?
        get() {
            if (isAndroid2dSurface) return this
            children
                .firstOrNull { it.isAndroid2dSurface && !it.isSysUi }
                ?.let {
                    return it
                }
            children
                .firstOrNull { it.isTaskWindowWrapper }
                ?.children
                ?.firstOrNull { it.isAndroid2dSurface && !it.isSysUi }
                ?.let {
                    return it
                }
            return null
        }

    /**
     * The [SnapshotXrSpace] responsible for receiving input on this space or its primary surface.
     */
    public val ownInputTarget: SnapshotXrSpace?
        get() = if (isInputTarget) this else primarySurfaceNode?.takeIf { it.isInputTarget }

    /**
     * All direct or indirect embedded child window leashes whose nearest parent window leash is
     * this space.
     */
    public val childWindowLeashes: List<SnapshotXrSpace>
        get() = findNodes { it != this && it.isEmbeddedWindowLeash && it.parentWindowLeash == this }

    /** Direct or indirect child spaces that represent system UI affordances. */
    public val sysUiAffordances: List<SnapshotXrSpace>
        get() =
            listOfNotNull(
                    this,
                    children.firstOrNull { it.isTaskWindowWrapper },
                    primarySurfaceNode,
                )
                .distinct()
                .flatMap { it.children }
                .filter { it.isSysUi || it.sysUiSpecialNode != null }

    /** Whether this space represents a window caption bar or task bar. */
    public val isCaptionBar: Boolean
        get() =
            isSysUi &&
                (name.contains("Caption", ignoreCase = true) ||
                    name.contains("TaskBar", ignoreCase = true) ||
                    sysUiSpecialNode == 4)

    /** Whether this space represents a drag bar or move affordance. */
    public val isMoveAffordance: Boolean
        get() =
            isSysUi &&
                (name.contains("Drag", ignoreCase = true) ||
                    name.contains("Move", ignoreCase = true) ||
                    sysUiSpecialNode == 1)

    /** Whether this space represents a window resize affordance. */
    public val isResizeAffordance: Boolean
        get() = isSysUi && (name.contains("Resize", ignoreCase = true) || sysUiSpecialNode == 2)

    /** Child system UI affordance spaces that handle move/drag interactions. */
    public val moveAffordanceNodes: List<SnapshotXrSpace>
        get() = sysUiAffordances.filter { it.isMoveAffordance }

    /** Child system UI affordance spaces that handle resize interactions. */
    public val resizeAffordanceNodes: List<SnapshotXrSpace>
        get() = sysUiAffordances.filter { it.isResizeAffordance }

    /** Whether this space is directly registered to receive input events. */
    public val isInputTarget: Boolean
        get() = inputId >= 0 && canReceiveInput

    /**
     * Whether this space and all of its ancestors are visible in the spatial compositor scene
     * graph.
     */
    public val isVisible: Boolean = isVisible
        get() {
            if (!field) return false
            if (header.contains("hidden") || stereoVisibility == "hidden on both") {
                return false
            }
            val p = parent
            if (p != null && p.name != "Root") {
                return p.isVisible
            }
            return true
        }

    /**
     * Whether this space represents an active, visible toast or mirror overlay in the compositor.
     */
    internal val isVisibleToastOverlay: Boolean
        get() {
            if (!isVisible) return false
            if (name.startsWith("Toast#")) {
                return isNodeActive
            }
            if (name == "MIRROR_TOAST_AREA") {
                if (isNodeActive && hasOwnRenderedSurface) {
                    return true
                }
                return getDescendants().any { child ->
                    child.isVisible &&
                        child.isNodeActive &&
                        (child.name.startsWith("Toast#") || child.hasOwnRenderedSurface)
                }
            }
            return false
        }

    private val hasOwnRenderedSurface: Boolean
        get() {
            if (bufferSize != null) return true
            val meters = directSurfaceBoundsInMeters
            val pixels = directSurfaceBoundsInPixels
            return (meters != null && meters.x > 0f && meters.y > 0f) ||
                (pixels != null && pixels.x > 0f && pixels.y > 0f)
        }

    // --- Task & Subspace Hierarchy Selectors ---

    /**
     * Whether this space is the root spatial container (`space-root-task-<id>`) for an Android
     * task.
     */
    public val isTaskRoot: Boolean
        get() = name.startsWith("space-root-task-")

    /** The Android task ID associated with this space, or `-1` if not in a task subtree. */
    public val taskId: Int
        get() =
            if (isTaskRoot) {
                name.removePrefix("space-root-task-").substringBefore(' ').toIntOrNull() ?: -1
            } else {
                parent?.taskId ?: -1
            }

    /**
     * The topmost [SnapshotXrSpace] representing the root of the activity space for this space, or
     * `null` if not in a task subtree.
     */
    public val activitySpaceRoot: SnapshotXrSpace?
        get() = if (isTaskRoot) this else parent?.activitySpaceRoot

    /** The package name associated with this space's task, if resolved or discovered. */
    public val packageName: String?
        get() =
            explicitPackageName
                ?: (if (taskId != -1) scene?.taskPackageMap?.get(taskId) else null)
                ?: (if (isTaskRoot) extractPackageFromNodes(this)
                else activitySpaceRoot?.packageName)

    /**
     * System transform space for 3D spatial/Subspace content (`space-transform-spatial-<id>`), or
     * null.
     */
    public val spatialTransform: SnapshotXrSpace?
        get() =
            (activitySpaceRoot ?: this).children.firstOrNull {
                it.name.startsWith("space-transform-spatial-")
            }

    /** All active raycast and touch input targets within this space's subtree. */
    public val inputTargets: List<SnapshotXrSpace>
        get() = findNodes { it.isInputTarget }

    /**
     * The primary task window leash container ([isTaskWindowLeash]) in this subtree, if present.
     */
    public val taskWindowLeash: SnapshotXrSpace?
        get() = findNode { it.isTaskWindowLeash }

    /**
     * All embedded Activity task window leash containers ([isActivityWindowLeash]) in this subtree.
     */
    public val activityWindowLeashes: List<SnapshotXrSpace>
        get() = findNodes { it.isActivityWindowLeash }

    /** All embedded spatial window leash containers ([isEmbeddedWindowLeash]) in this subtree. */
    public val embeddedWindowLeashes: List<SnapshotXrSpace>
        get() = findNodes { it.isEmbeddedWindowLeash }

    /** All spatial window leash containers ([isWindowLeash]) in this subtree. */
    public val windowLeashes: List<SnapshotXrSpace>
        get() = findNodes { it.isWindowLeash }

    /**
     * All top-level window leash containers in this subtree (window leashes that are not nested
     * inside another window leash).
     */
    public val topLevelWindowLeashes: List<SnapshotXrSpace>
        get() = findNodes { it.isTopLevelWindowLeash }

    /** All 3D subspace nodes (`hasSubspace=true`) in this subtree. */
    public val subspaceNodes: List<SnapshotXrSpace>
        get() = findNodes { it.hasSubspace }

    /**
     * True if this space is active in the scene graph. For a task root space ([isTaskRoot]),
     * requires that the root container is visible and active and hosts an active 3D spatial
     * transform ([isFullSpace]) or a visible window leash.
     */
    public val isActive: Boolean
        get() =
            if (isTaskRoot) {
                isVisible &&
                    isNodeActive &&
                    (isFullSpace ||
                        taskWindowLeash?.isVisible == true ||
                        windowLeashes.any { it.isVisible })
            } else {
                isNodeActive
            }

    /**
     * True if this task space has a visible 3D spatial transform node
     * (`space-transform-spatial-<id>`) in the scene graph, indicating Full Space Mode is active.
     */
    public val isFullSpace: Boolean
        get() =
            spatialTransform?.let {
                it.isVisible && it.isNodeActive
            } == true

    /**
     * Effective virtual pixel density in pixels per meter for this space. On a task root space
     * ([isTaskRoot]), resolves from [spatialTransform] or descendant spaces or derives from 2D
     * surface bounds if not defined directly on the root.
     */
    public val computedPixelsPerMeter: Float?
        get() {
            if (!isTaskRoot) {
                return directComputedPixelsPerMeter ?: parent?.computedPixelsPerMeter
            }
            return spatialTransform?.directComputedPixelsPerMeter
                ?: directComputedPixelsPerMeter
                ?: getDescendants().firstNotNullOfOrNull { it.directComputedPixelsPerMeter }
                ?: allNodes.firstNotNullOfOrNull { node ->
                    val px = node.surfaceBoundsInPixels
                    val m = node.surfaceBoundsInMeters
                    if (px != null && m != null && px.x > 0f && m.x > 0f) px.x / m.x else null
                }
        }

    /**
     * Returns `true` if this task space and its spatial scene have settled with no in-flight
     * transitions or alpha animations, and its panels match the expected visibility and count.
     *
     * @param expectedEmbeddedPanelCount Expected number of visible [embeddedWindowLeashes] with
     *   non-null [effectiveSurfaceBoundsInMeters], or `null` to not constrain the embedded panel
     *   count.
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
        if (scene?.hasActiveTransition == true) return false
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SnapshotXrSpace) return false
        return id == other.id &&
            name == other.name &&
            depth == other.depth &&
            header == other.header
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + name.hashCode()
        result = 31 * result + depth
        result = 31 * result + header.hashCode()
        return result
    }

    override fun toString(): String =
        "SnapshotXrSpace(name='$name', id=$id, depth=$depth, taskId=$taskId, packageName=$packageName, isVisible=$isVisible, isPanel=$isSpatialPanel, bounds=$surfaceBoundsInMeters)"

    /**
     * Crops [bitmap] to the foreground bounds of this spatial space.
     *
     * Computes the bounding rectangle of this space projected onto the camera view relative to its
     * enclosing activity space root, identifies the rendered foreground pixels, and extracts the
     * cropped bitmap.
     *
     * @param bitmap The full-screen or source [Bitmap] to crop.
     * @param padding Optional margin in pixels added around the cropped foreground bounds.
     * @return A cropped [Bitmap] isolating this space.
     */
    @JvmOverloads
    public fun crop(
        bitmap: Bitmap,
        padding: Int = 0,
    ): Bitmap {
        val softwareBitmap = toSoftwareBitmap(bitmap)
        val width = softwareBitmap.width
        val height = softwareBitmap.height
        val pixels = toPixelArray(softwareBitmap)
        val bgColor = detectBackgroundColor(pixels, width, height)
        val fullBounds = Rect(0, 0, width, height)
        val overallBounds =
            findForegroundBounds(pixels, width, height, fullBounds, bgColor)
                ?: return softwareBitmap

        val fraction = computeRelativeNodeBoundsFraction(this)
        val isFullFraction =
            fraction.left <= 0.001f &&
                fraction.top <= 0.001f &&
                fraction.right >= 0.999f &&
                fraction.bottom >= 0.999f
        val tightBounds =
            if (isFullFraction) {
                overallBounds
            } else {
                val w = overallBounds.width()
                val h = overallBounds.height()
                val searchBounds =
                    Rect(
                        (overallBounds.left + (fraction.left * w).toInt()).coerceAtLeast(
                            overallBounds.left
                        ),
                        (overallBounds.top + (fraction.top * h).toInt()).coerceAtLeast(
                            overallBounds.top
                        ),
                        (overallBounds.left + ceil((fraction.right * w).toDouble()).toInt())
                            .coerceAtMost(overallBounds.right),
                        (overallBounds.top + ceil((fraction.bottom * h).toDouble()).toInt())
                            .coerceAtMost(overallBounds.bottom),
                    )
                findForegroundBounds(
                    pixels,
                    width,
                    height,
                    searchBounds,
                    bgColor,
                ) ?: searchBounds
            }

        val safePadding = max(0, padding)
        val finalBounds =
            Rect(
                (tightBounds.left - safePadding).coerceAtLeast(0),
                (tightBounds.top - safePadding).coerceAtLeast(0),
                (tightBounds.right + safePadding).coerceAtMost(width),
                (tightBounds.bottom + safePadding).coerceAtMost(height),
            )

        val cropped =
            try {
                androidx.xr.testutils.crop(softwareBitmap, finalBounds)
            } catch (t: Throwable) {
                if (softwareBitmap !== bitmap && !softwareBitmap.isRecycled) {
                    softwareBitmap.recycle()
                }
                throw t
            }
        if (cropped !== softwareBitmap && softwareBitmap !== bitmap && !softwareBitmap.isRecycled) {
            softwareBitmap.recycle()
        }
        return cropped
    }

    /**
     * Captures the 3D XR compositor output and automatically crops the screenshot to the rendered
     * bounds of this [SnapshotXrSpace], accounting for its position relative to the user's head
     * pose.
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
        captureToBitmapInternal(
            padding = padding,
            timeout = timeout,
            pollInterval = pollInterval,
            uiAutomation = uiAutomation,
            pollScene = true,
        )

    internal fun captureToBitmapInternal(
        padding: Int = 0,
        timeout: Duration = 5.seconds,
        pollInterval: Duration = 100.milliseconds,
        uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation,
        pollScene: Boolean = true,
    ): Bitmap {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        }
        if (pollScene) {
            captureSceneWithPolling(
                uiAutomation = uiAutomation,
                maxWaitMs = max(MIN_SCREENSHOT_SCENE_SETTLE_WAIT_MS, timeout.inWholeMilliseconds),
            )
        }
        val settledBitmap = waitForSettledSoftwareScreenshot(uiAutomation, timeout, pollInterval)
        val processed =
            try {
                crop(
                    bitmap = settledBitmap,
                    padding = padding,
                )
            } catch (t: Throwable) {
                settledBitmap.recycle()
                throw t
            }
        if (processed !== settledBitmap && !settledBitmap.isRecycled) {
            settledBitmap.recycle()
        }
        return processed
    }

    internal companion object {
        private val TASK_WINDOW_NODE_REGEX = Regex("^task-\\d+-window$")
        private val PACKAGE_NAME_PATTERN =
            Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)+$")

        internal fun extractPackageFromNodes(root: SnapshotXrSpace): String? {
            fun search(node: SnapshotXrSpace): String? {
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
}
