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
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Represents an individual spatial entity in the CPM scene graph.
 *
 * Corresponds conceptually to an OpenXR Scene Entity (`XrSceneEntityKHRX1`).
 */
public class SpatialNode
@JvmOverloads
constructor(
    public val header: String = "",
    public val name: String = "",
    public val id: Int = -1,
    public val depth: Int = 0,
    public val attributes: Map<String, FloatArray> = emptyMap(),
    public val properties: Map<String, String> = emptyMap(),
) {
    public var parent: SpatialNode? = null
        internal set

    /** The [SpatialScene] containing this node, or null if constructed standalone. */
    public var scene: SpatialScene? = null
        internal set

    /** The topmost ancestor node in this spatial node's hierarchy (the root of the tree). */
    public val root: SpatialNode
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
        val headTransformNodes = rootNode.findNodes {
            it.properties["transformUpdateRequest.coordinateSpace"] == "2"
        }
        val selected =
            headTransformNodes.firstOrNull {
                it.properties["ImpNodeIsActive"] == "true" ||
                    it.parent?.properties?.get("ImpNodeIsActive") == "true"
            } ?: headTransformNodes.firstOrNull()
        return selected?.let { transformNode ->
            val parent = transformNode.parent
            if (parent != null && parent.name != "Root") parent.pose else transformNode.pose
        }
    }

    private val _children = mutableListOf<SpatialNode>()

    public val children: List<SpatialNode>
        get() = _children

    internal fun addChild(child: SpatialNode) {
        child.parent = this
        child.scene = this.scene
        if (this.scene != null) {
            for (descendant in child.getDescendants()) {
                descendant.scene = this.scene
            }
        }
        _children.add(child)
    }

    public fun getDescendants(): List<SpatialNode> {
        val result = mutableListOf<SpatialNode>()
        fun traverse(node: SpatialNode) {
            for (child in node.children) {
                result.add(child)
                traverse(child)
            }
        }
        traverse(this)
        return result
    }

    /** Finds the first descendant matching [predicate], or null. */
    public fun findDescendant(predicate: (SpatialNode) -> Boolean): SpatialNode? =
        getDescendants().firstOrNull(predicate)

    /**
     * Finds the first descendant whose name contains [nameSubstring] (case-insensitive), or null.
     */
    public fun findDescendantByName(nameSubstring: String): SpatialNode? =
        getDescendants().firstOrNull { it.name.contains(nameSubstring, ignoreCase = true) }

    /** Finds all descendants matching [predicate]. */
    public fun findDescendants(predicate: (SpatialNode) -> Boolean): List<SpatialNode> =
        getDescendants().filter(predicate)

    /** Finds all nodes in this subtree (including `this`) matching [predicate]. */
    public fun findNodes(predicate: (SpatialNode) -> Boolean): List<SpatialNode> =
        (listOf(this) + getDescendants()).filter(predicate)

    /** Finds the first node in this subtree (including `this`) matching [predicate], or null. */
    public fun findNode(predicate: (SpatialNode) -> Boolean): SpatialNode? =
        if (predicate(this)) this else findDescendant(predicate)

    /** Finds all descendants whose name contains [nameSubstring] (case-insensitive). */
    public fun findDescendantsByName(nameSubstring: String): List<SpatialNode> =
        getDescendants().filter { it.name.contains(nameSubstring, ignoreCase = true) }

    public fun isDescendantOf(other: SpatialNode): Boolean {
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

    public val position: Vector3
        get() {
            val raw = attributes["ImpNodeWorldPosition"]
            return if (raw != null && raw.size >= 3) {
                Vector3(raw[0], raw[1], raw[2])
            } else {
                Vector3.Zero
            }
        }

    public val rotation: Quaternion
        get() {
            val raw = attributes["ImpNodeWorldRotation"]
            return if (raw != null && raw.size >= 4) {
                Quaternion(x = raw[1], y = raw[2], z = raw[3], w = raw[0])
            } else {
                Quaternion.Identity
            }
        }

    public val scale: Vector3
        get() {
            val raw = attributes["ImpNodeWorldScale"]
            return if (raw != null && raw.size >= 3) {
                Vector3(raw[0], raw[1], raw[2])
            } else {
                Vector3.One
            }
        }

    public val pose: Pose
        get() = Pose(position, rotation)

    /**
     * Transforms [point] from this node's local coordinate space into OpenXR world space, taking
     * into account [position], [rotation], and [scale].
     *
     * @param point 3D coordinates in this node's local space.
     * @return 3D coordinates in OpenXR world space.
     */
    public fun transformPointToWorld(point: Vector3): Vector3 {
        val scaled = point.scale(scale)
        return position + (rotation * scaled)
    }

    /**
     * Transforms [worldPoint] from OpenXR world space into this node's local coordinate space,
     * taking into account [position], [rotation], and [scale].
     *
     * @param worldPoint 3D coordinates in OpenXR world space.
     * @return 3D coordinates in this node's local space.
     */
    public fun transformPointFromWorld(worldPoint: Vector3): Vector3 {
        val unrotated = rotation.inverse * (worldPoint - position)
        val sx = if (abs(scale.x) > 1e-6f) scale.x else 1.0f
        val sy = if (abs(scale.y) > 1e-6f) scale.y else 1.0f
        val sz = if (abs(scale.z) > 1e-6f) scale.z else 1.0f
        return Vector3(unrotated.x / sx, unrotated.y / sy, unrotated.z / sz)
    }

    /**
     * Transforms [vector] from this node's local coordinate space into OpenXR world space, taking
     * into account [rotation] and [scale].
     *
     * @param vector Translation/displacement vector in this node's local space.
     * @return Translation/displacement vector in OpenXR world space.
     */
    public fun transformVectorToWorld(vector: Vector3): Vector3 {
        val scaled = vector.scale(scale)
        return rotation * scaled
    }

    /**
     * Transforms [worldVector] from OpenXR world space into this node's local coordinate space,
     * taking into account [rotation] and [scale].
     *
     * @param worldVector Translation/displacement vector in OpenXR world space.
     * @return Translation/displacement vector in this node's local space.
     */
    public fun transformVectorFromWorld(worldVector: Vector3): Vector3 {
        val unrotated = rotation.inverse * worldVector
        val sx = if (abs(scale.x) > 1e-6f) scale.x else 1.0f
        val sy = if (abs(scale.y) > 1e-6f) scale.y else 1.0f
        val sz = if (abs(scale.z) > 1e-6f) scale.z else 1.0f
        return Vector3(unrotated.x / sx, unrotated.y / sy, unrotated.z / sz)
    }

    // --- Geometry & Metric Bounds ---

    public val surfaceBoundsInMeters: Vector2?
        get() {
            val raw = attributes["root2DSurfaceBoundsInMeters"]
            if (raw != null && raw.size >= 2 && raw[0] > 0f && raw[1] > 0f) {
                return Vector2(raw[0], raw[1])
            }
            if (isSurfaceContainer) {
                val orderedChildren = children.filterNot { it.isWindowLeash || it.isSysUi }
                for (child in orderedChildren) {
                    val childBounds = child.surfaceBoundsInMeters
                    if (childBounds != null) return childBounds
                }
            }
            return null
        }

    public val surfaceBoundsInPixels: Vector2?
        get() {
            val rawPixels = attributes["root2DSurfaceBoundsInPixels"]
            if (
                rawPixels != null && rawPixels.size >= 2 && rawPixels[0] > 0f && rawPixels[1] > 0f
            ) {
                return Vector2(rawPixels[0], rawPixels[1])
            }
            val rawTouch = attributes["touchableRegion"]
            if (rawTouch != null && rawTouch.size >= 4) {
                val w = abs(rawTouch[2] - rawTouch[0])
                val h = abs(rawTouch[3] - rawTouch[1])
                if (w > 0f && h > 0f) {
                    return Vector2(w, h)
                }
            }
            bufferSize?.let {
                return it
            }
            if (isSurfaceContainer) {
                val orderedChildren = children.filterNot { it.isWindowLeash || it.isSysUi }
                for (child in orderedChildren) {
                    val childPixels = child.surfaceBoundsInPixels
                    if (childPixels != null) return childPixels
                }
            }
            return null
        }

    public val hasSurfaceBounds: Boolean
        get() = surfaceBoundsInMeters != null || surfaceBoundsInPixels != null

    public val touchableRegion: Rect?
        get() {
            val raw = attributes["touchableRegion"]
            if (raw != null && raw.size >= 4) {
                return Rect(raw[0].toInt(), raw[1].toInt(), raw[2].toInt(), raw[3].toInt())
            }
            if (isSurfaceContainer) {
                val orderedChildren = children.filterNot { it.isWindowLeash || it.isSysUi }
                for (child in orderedChildren) {
                    val childTouch = child.touchableRegion
                    if (childTouch != null) return childTouch
                }
            }
            return null
        }

    public val bufferSize: Vector2?
        get() {
            val bufferProp = properties["buffer"]
            if (bufferProp != null && bufferProp.contains("x") && bufferProp != "null") {
                val parts = bufferProp.split("x")
                if (parts.size == 2) {
                    val w = parts[0].toFloatOrNull()
                    val h = parts[1].toFloatOrNull()
                    if (w != null && h != null && w > 0f && h > 0f) {
                        return Vector2(w, h)
                    }
                }
            }
            return null
        }

    /**
     * Explicit effective 2D surface bounds in meters (`width, height`), delegating to
     * [primarySurfaceNode] when queried on a window leash container.
     */
    public val effectiveSurfaceBoundsInMeters: Vector2?
        get() = surfaceBoundsInMeters ?: primarySurfaceNode?.surfaceBoundsInMeters

    /**
     * Explicit effective 2D surface bounds in pixels (`width, height`), delegating to
     * [primarySurfaceNode] when queried on a window leash container.
     */
    public val effectiveSurfaceBoundsInPixels: Vector2?
        get() = surfaceBoundsInPixels ?: primarySurfaceNode?.surfaceBoundsInPixels

    /**
     * Explicit effective 2D buffer size in pixels (`width, height`), delegating to
     * [primarySurfaceNode] when queried on a window leash container.
     */
    public val effectiveBufferSize: Vector2?
        get() = bufferSize ?: primarySurfaceNode?.bufferSize

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

    public val worldHalfExtents: Vector3?
        get() = worldExtents?.div(2f)

    /**
     * Effective virtual pixel density in pixels per meter (`computedPixelsPerMeter` or positive
     * `pixelsPerMeter` in `dumpsys spf_cpm`), inheriting from [parent] if not explicitly present on
     * this node, or `null` if unavailable.
     */
    public val computedPixelsPerMeter: Float?
        get() =
            properties["computedPixelsPerMeter"]?.toFloatOrNull()?.takeIf { it > 0f }
                ?: properties["pixelsPerMeter"]?.toFloatOrNull()?.takeIf { it > 0f }
                ?: parent?.computedPixelsPerMeter

    // --- System Flags & Capabilities ---

    public val canReceiveInput: Boolean
        get() = properties["canReceiveInput"]?.toBoolean() ?: true

    public val isAndroid2d: Boolean
        get() = properties["isAndroid2d"]?.toBoolean() ?: false

    public val isEmbeddedWindowLeash: Boolean
        get() = properties["isAppSpatialEmbeddedWindowLeash"]?.toBoolean() ?: false

    public val isSysUi: Boolean
        get() = properties["isSysUi"]?.toBoolean() ?: false

    /** True if this node hosts a 3D subspace (`hasSubspace=true` in CPM). */
    public val hasSubspace: Boolean
        get() = properties["hasSubspace"]?.toBoolean() ?: false

    /**
     * True if this node is a 2D SurfaceFlinger buffer quad layer (`isAndroid2d=true` with a buffer
     * or surface bounds).
     */
    public val isAndroid2dSurface: Boolean
        get() = isAndroid2d && (bufferSize != null || hasSurfaceBounds)

    /**
     * True if this node is a CPM/WindowManager task window wrapper node (`window-task-<id>` or
     * `task-<id>-window`).
     */
    public val isTaskWindowWrapper: Boolean
        get() = name.startsWith("window-task-") || TASK_WINDOW_NODE_REGEX.matches(name)

    /**
     * True if this node is the container leash for the task's primary window, identified by
     * directly hosting the WindowManager/CPM `window-task-<taskId>` node.
     */
    public val isTaskWindowLeash: Boolean
        get() = children.any { it.name.startsWith("window-task-") }

    /**
     * True if this node is the container leash for an embedded Activity task window, identified by
     * directly hosting a WindowManager/CPM `task-<id>-window` node.
     */
    public val isActivityWindowLeash: Boolean
        get() = children.any { TASK_WINDOW_NODE_REGEX.matches(it.name) }

    /**
     * True if this node is any spatial window leash container in CPM ([isTaskWindowLeash],
     * [isActivityWindowLeash], or [isEmbeddedWindowLeash]).
     */
    public val isWindowLeash: Boolean
        get() = isTaskWindowLeash || isActivityWindowLeash || isEmbeddedWindowLeash

    /**
     * The closest ancestor node that is a spatial window leash ([isWindowLeash]), or `null` if this
     * node is not nested inside another window leash.
     */
    public val parentWindowLeash: SpatialNode?
        get() {
            var current = parent
            while (current != null) {
                if (current.isWindowLeash) return current
                current = current.parent
            }
            return null
        }

    /**
     * True if this node is a top-level window leash container in the task (i.e. [isWindowLeash] is
     * true and [parentWindowLeash] is `null`).
     */
    public val isTopLevelWindowLeash: Boolean
        get() = isWindowLeash && parentWindowLeash == null

    /**
     * Returns this node's own 2D surface/buffer node ([isAndroid2dSurface]).
     *
     * In CPM, a node's 2D surface is either:
     * - This node itself (if [isAndroid2dSurface] is true),
     * - A direct child of this node (e.g. `#<layerId>` under an embedded window leash), or
     * - A direct child of this node's direct [isTaskWindowWrapper] child (e.g.
     *   `<pkg>/<activity>#<id>` under `window-task-<id>` or `task-<id>-window`).
     */
    public val primarySurfaceNode: SpatialNode?
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
     * Returns this node's own hit-testable input target node (the [primarySurfaceNode] if it has an
     * `input-id`, or `this` if directly hit-testable).
     */
    public val ownInputTarget: SpatialNode?
        get() = if (isInputTarget) this else primarySurfaceNode?.takeIf { it.isInputTarget }

    /**
     * Embedded window leashes (`isEmbeddedWindowLeash=true`) directly or indirectly attached inside
     * this node whose closest enclosing window leash is `this`.
     */
    public val childWindowLeashes: List<SpatialNode>
        get() = findNodes { it != this && it.isEmbeddedWindowLeash && it.parentWindowLeash == this }

    /** Special node ID from `sysUIMetadata=<specialNode=...>` if present, or `null`. */
    public val sysUiSpecialNode: Int?
        get() {
            val metadata = properties["sysUIMetadata"] ?: return null
            val match = SYS_UI_SPECIAL_NODE_REGEX.find(metadata)
            return match?.groupValues?.get(1)?.toIntOrNull()
        }

    internal val scrimTargetAlpha: Float
        get() {
            val metadata = properties["sysUIMetadata"] ?: return 0f
            val match = SCRIM_TARGET_ALPHA_REGEX.find(metadata) ?: return 0f
            return match.groupValues[1].toFloatOrNull() ?: 0f
        }

    /**
     * All SysUI affordance/decoration nodes (`isSysUi=true` or `sysUiSpecialNode != null`) directly
     * associated with this node (direct children of this node, its [isTaskWindowWrapper], or its
     * [primarySurfaceNode]).
     */
    public val sysUiAffordances: List<SpatialNode>
        get() {
            val candidateParents =
                listOfNotNull(
                        this,
                        children.firstOrNull { it.isTaskWindowWrapper },
                        primarySurfaceNode,
                    )
                    .distinct()
            return candidateParents
                .flatMap { it.children }
                .filter { it.isSysUi || it.sysUiSpecialNode != null }
        }

    /**
     * True if this node represents a system caption bar / task bar decoration (`Caption` or
     * `TaskBar`).
     */
    public val isCaptionBar: Boolean
        get() =
            name.contains("Caption", ignoreCase = true) ||
                name.contains("TaskBar", ignoreCase = true)

    /**
     * True if this node is a system move/drag affordance (`isSysUi=true` and name contains `Move`
     * or `Drag`). Note: Caption bars ([isCaptionBar]) are excluded as they do not react to move
     * actions.
     */
    public val isMoveAffordance: Boolean
        get() =
            isSysUi &&
                (name.contains("Move", ignoreCase = true) ||
                    name.contains("Drag", ignoreCase = true))

    /**
     * True if this node is a system resize handle/corner affordance (`isSysUi=true` and name
     * contains `Resize`).
     */
    public val isResizeAffordance: Boolean
        get() = isSysUi && name.contains("Resize", ignoreCase = true)

    /** Move affordance nodes directly associated with this node. */
    public val moveAffordanceNodes: List<SpatialNode>
        get() = sysUiAffordances.filter { it.isMoveAffordance }

    /** Resize affordance nodes directly associated with this node. */
    public val resizeAffordanceNodes: List<SpatialNode>
        get() = sysUiAffordances.filter { it.isResizeAffordance }

    public val inputId: Int
        get() = properties["input-id"]?.toIntOrNull() ?: -1

    public val isInputTarget: Boolean
        get() = inputId >= 0 && canReceiveInput

    private val reformBitmask: Int
        get() {
            val reform =
                properties["reformOptions"] ?: parent?.properties?.get("reformOptions") ?: return 0
            val enableMatch = REFORM_ENABLE_REGEX.find(reform)
            return enableMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        }

    public val isMovable: Boolean
        get() = (reformBitmask and 1) != 0

    public val isResizable: Boolean
        get() = (reformBitmask and 2) != 0

    public val stereoVisibility: String?
        get() = properties["stereoVisibility"]

    public val isVisible: Boolean
        get() {
            if (
                header.contains("hidden") ||
                    properties["requested-vis"] == "hide" ||
                    stereoVisibility == "hidden on both"
            ) {
                return false
            }
            val p = parent
            if (p != null && p.name != "Root") {
                return p.isVisible
            }
            return true
        }

    private val hasOwnRenderedSurface: Boolean
        get() {
            if (bufferSize != null) return true
            val meters = attributes["root2DSurfaceBoundsInMeters"]
            if (meters != null && meters.size >= 2 && meters[0] > 0f && meters[1] > 0f) {
                return true
            }
            val pixels = attributes["root2DSurfaceBoundsInPixels"]
            if (pixels != null && pixels.size >= 2 && pixels[0] > 0f && pixels[1] > 0f) {
                return true
            }
            return false
        }

    /**
     * True if this node represents a toast overlay that can actually render onto the display.
     * Pre-allocated `MIRROR_TOAST_AREA` containers (for example on Moohan where the container node
     * stays `visible on both` with `buffer=null` and `ImpNodeIsActive=false` while empty) are
     * ignored unless they or a visible active descendant have an active toast node or rendered
     * surface.
     */
    internal val isVisibleToastOverlay: Boolean
        get() {
            if (!isVisible) return false
            if (name.startsWith("Toast#")) {
                return properties["ImpNodeIsActive"] != "false"
            }
            if (name == "MIRROR_TOAST_AREA") {
                if (properties["ImpNodeIsActive"] != "false" && hasOwnRenderedSurface) {
                    return true
                }
                return getDescendants().any { child ->
                    child.isVisible &&
                        child.properties["ImpNodeIsActive"] != "false" &&
                        (child.name.startsWith("Toast#") || child.hasOwnRenderedSurface)
                }
            }
            return false
        }

    /**
     * Whether this node is an Android task root reference space (`space-root-task-<taskId>`).
     *
     * In Android XR CPM dumpsys, task roots are created directly under the compositor `Root` node
     * by Window Manager.
     */
    public val isTaskRoot: Boolean
        get() = name.startsWith("space-root-task-")

    public val taskId: Int?
        get() =
            if (isTaskRoot) {
                name.removePrefix("space-root-task-").substringBefore(' ').toIntOrNull()
            } else {
                parent?.taskId
            }

    /**
     * The root reference space container node (`space-root-task-<taskId>`) for the task this node
     * belongs to, or this node itself if it is the task root, or null if not within a task tree.
     */
    public val activitySpaceRoot: SpatialNode?
        get() = if (isTaskRoot) this else parent?.activitySpaceRoot

    internal val isSurfaceContainer: Boolean
        get() =
            isWindowLeash ||
                isTaskWindowWrapper ||
                name.startsWith("window-") ||
                attributes.containsKey("root2DSurfaceBoundsInMeters") ||
                children.any { it.bufferSize != null || it.isAndroid2d }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpatialNode) return false
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
        "SpatialNode(name='$name', id=$id, depth=$depth, pos=$position, children=${children.size})"

    /**
     * Detects the bounds of this [SpatialNode]'s rendered content within [bitmap] (projected
     * relative to the user's head pose) and crops the resulting [Bitmap] to the node's content
     * bounds (plus optional [padding]).
     *
     * @param bitmap Source [Bitmap] from the XR compositor.
     * @param padding Extra padding in pixels to preserve around the cropped content bounds.
     * @return A software-backed [Bitmap.Config.ARGB_8888] [Bitmap] containing the cropped content
     *   of this [SpatialNode].
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
                crop(softwareBitmap, finalBounds)
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
     * bounds of this [SpatialNode], accounting for its position relative to the user's head pose.
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
        private val REFORM_ENABLE_REGEX = Regex("enable=(\\d+)")
        private val SYS_UI_SPECIAL_NODE_REGEX = Regex("specialNode=(\\d+)")
        private val SCRIM_TARGET_ALPHA_REGEX = Regex("targetAlpha=([0-9.+-]+)")
        private val TASK_WINDOW_NODE_REGEX = Regex("^task-\\d+-window$")
    }
}
