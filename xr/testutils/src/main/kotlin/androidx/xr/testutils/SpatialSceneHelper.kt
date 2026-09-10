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
import android.graphics.Rect
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector2
import androidx.xr.runtime.math.Vector3
import java.io.InputStream
import java.util.regex.Pattern
import kotlin.math.abs

/**
 * Utility for capturing, inspecting, and querying spatial scenes from Android XR devices and
 * emulators.
 *
 * Parses point-in-time scene graph snapshots from `dumpsys spf_cpm` into a clean object structure
 * based strictly on system-guaranteed CPM and SpaceFlinger keywords, properties, and task
 * boundaries.
 *
 * For the complete structural and keyword specification of `dumpsys spf_cpm`, see
 * `CPM_DUMPSYS_ARCHITECTURE.md` located in the `xr:testutils` root directory.
 */
public object SpatialSceneHelper {

    /**
     * Captures a point-in-time snapshot of the spatial scene graph from `dumpsys spf_cpm`.
     *
     * @param uiAutomation The [UiAutomation] instance used to execute the shell command.
     */
    @JvmStatic
    @JvmOverloads
    public fun captureSpatialScene(
        uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
    ): SpatialScene = captureSceneWithPolling(uiAutomation = uiAutomation)

    private fun captureSceneWithPolling(uiAutomation: UiAutomation): SpatialScene {
        var scene: SpatialScene
        val startTime = System.currentTimeMillis()
        while (true) {
            val rawDump = executeShellCommand("dumpsys spf_cpm", uiAutomation)
            val activitiesDump = executeShellCommand("dumpsys activity activities", uiAutomation)
            val taskPackageMap = parseTaskPackageMap(activitiesDump)
            scene = parseSpatialScene(rawDump = rawDump, taskPackageMap = taskPackageMap)
            val hasTransition =
                scene.tasks.any { task ->
                    task.allNodes.any { it.name.contains("transition snapshot") }
                }
            val isReady =
                scene.tasks.any { task ->
                    task.allNodes.any { it.hasSurfaceBounds || it.isEmbeddedWindowLeash }
                }
            if ((!hasTransition && isReady) || System.currentTimeMillis() - startTime > 3000) {
                break
            }
            try {
                Thread.sleep(100)
            } catch (_: InterruptedException) {}
        }
        return scene
    }

    /**
     * Parses the raw output of `dumpsys spf_cpm` and an optional task-package mapping into a
     * [SpatialScene].
     */
    @VisibleForTesting
    internal fun parseSpatialScene(
        rawDump: String,
        taskPackageMap: Map<Int, String> = emptyMap(),
    ): SpatialScene =
        SpatialScene(rootNodes = parseRootNodes(rawDump), taskPackageMap = taskPackageMap)

    /** Parses the raw output of `dumpsys spf_cpm` into a list of [SpatialNode] root entities. */
    @VisibleForTesting
    internal fun parseRootNodes(dump: String): List<SpatialNode> {
        val rootNodes = mutableListOf<SpatialNode>()
        val nodeStack = ArrayDeque<SpatialNode>()

        var currentNode: SpatialNode? = null
        val currentAttributes = mutableMapOf<String, FloatArray>()
        val currentProperties = mutableMapOf<String, String>()

        fun flushCurrentNode() {
            val node = currentNode ?: return
            val finishedNode =
                SpatialNode(
                    header = node.header,
                    name = node.name,
                    id = node.id,
                    depth = node.depth,
                    attributes = currentAttributes.toMap(),
                    properties = currentProperties.toMap(),
                )
            while (nodeStack.isNotEmpty() && nodeStack.last().depth >= finishedNode.depth) {
                nodeStack.removeLast()
            }

            if (nodeStack.isEmpty()) {
                rootNodes.add(finishedNode)
            } else {
                nodeStack.last().addChild(finishedNode)
            }

            nodeStack.addLast(finishedNode)
            currentNode = null
            currentAttributes.clear()
            currentProperties.clear()
        }

        for (line in dump.lineSequence()) {
            val trimmed = line.trim()
            if (
                trimmed.startsWith("InputTargetManager state:") ||
                    trimmed.startsWith("Stats since reset:") ||
                    trimmed.startsWith("Number of living nodes:") ||
                    trimmed.startsWith("Total size of buffers in scene:")
            ) {
                break
            }
            if (trimmed.isEmpty() || trimmed.startsWith("SPF Nodes:")) {
                continue
            }

            var indent = 0
            while (indent < line.length && line[indent] == ' ') {
                indent++
            }

            if (trimmed.startsWith("Node ")) {
                flushCurrentNode()

                val matcher = NODE_LINE_PATTERN.matcher(line)
                var nodeName = ""
                var nodeId = -1

                if (matcher.find()) {
                    nodeName = matcher.group(1)?.trim() ?: ""
                    nodeId = matcher.group(2)?.toIntOrNull() ?: -1
                } else {
                    val nodeIdx = line.indexOf("Node ")
                    if (nodeIdx >= 0) {
                        val rest = line.substring(nodeIdx + 5).trim()
                        val colonIdx = rest.lastIndexOf(':')
                        nodeName = if (colonIdx > 0) rest.substring(0, colonIdx).trim() else rest
                    }
                }

                currentNode =
                    SpatialNode(header = trimmed, name = nodeName, id = nodeId, depth = indent)
            }

            if (currentNode != null) {
                if (trimmed.contains("visible on both")) {
                    currentProperties["stereoVisibility"] = "visible on both"
                } else if (trimmed.contains("hidden on both")) {
                    currentProperties["stereoVisibility"] = "hidden on both"
                }

                val reformMatcher = REFORM_OPTIONS_PATTERN.matcher(line)
                if (reformMatcher.find()) {
                    currentProperties["reformOptions"] = reformMatcher.group(1) ?: ""
                }

                val sysUiMetadataMatcher = SYS_UI_METADATA_PATTERN.matcher(line)
                if (sysUiMetadataMatcher.find()) {
                    currentProperties["sysUIMetadata"] = sysUiMetadataMatcher.group(1) ?: ""
                }

                val attrMatcher = ATTR_ARRAY_PATTERN.matcher(line)
                while (attrMatcher.find()) {
                    val key = attrMatcher.group(1) ?: continue
                    val valueStr = attrMatcher.group(2) ?: continue
                    val arr = parseArray(valueStr)
                    if (arr.isNotEmpty()) {
                        currentAttributes[key] = arr
                    } else {
                        currentProperties[key] = valueStr
                    }
                }

                val propMatcher = PROP_KEY_VALUE_PATTERN.matcher(line)
                while (propMatcher.find()) {
                    val key = propMatcher.group(1) ?: continue
                    val value = propMatcher.group(2) ?: continue
                    if (!currentProperties.containsKey(key)) {
                        currentProperties[key] = value
                    }
                }
            }
        }

        flushCurrentNode()
        return rootNodes
    }

    private fun parseArray(input: String): FloatArray {
        val cleaned = input.replace("(", "").replace(")", "").replace(",", " ").trim()
        if (cleaned.isEmpty()) return FloatArray(0)
        val parts = cleaned.split(WHITESPACE_PATTERN).filter { it.isNotEmpty() }
        return try {
            FloatArray(parts.size) { i -> parts[i].toFloat() }
        } catch (e: NumberFormatException) {
            FloatArray(0)
        }
    }

    /**
     * Parses the output of `dumpsys activity activities` to build a map of task IDs to their
     * associated Android package names.
     */
    internal fun parseTaskPackageMap(activitiesDump: String): Map<Int, String> {
        if (activitiesDump.isBlank()) return emptyMap()
        val map = mutableMapOf<Int, String>()

        // Pattern 1: ActivityRecord{... <package>/<activity> ... t<taskId> ...}
        val actMatcher = ACTIVITY_RECORD_PATTERN.matcher(activitiesDump)
        while (actMatcher.find()) {
            val pkg = actMatcher.group(1) ?: continue
            val taskId = actMatcher.group(2)?.toIntOrNull() ?: continue
            map[taskId] = pkg
        }

        // Pattern 2: Task{... #<taskId> ... A=(<uid>:)?<package> ...}
        val taskMatcher = TASK_AFFINITY_PATTERN.matcher(activitiesDump)
        while (taskMatcher.find()) {
            val taskId = taskMatcher.group(1)?.toIntOrNull() ?: continue
            val pkg = taskMatcher.group(2) ?: continue
            if (!map.containsKey(taskId)) {
                map[taskId] = pkg
            }
        }

        // Pattern 3: Task{... #<taskId> ... <package>/...}
        val compMatcher = TASK_COMPONENT_PATTERN.matcher(activitiesDump)
        while (compMatcher.find()) {
            val taskId = compMatcher.group(1)?.toIntOrNull() ?: continue
            val pkg = compMatcher.group(2) ?: continue
            if (!map.containsKey(taskId)) {
                map[taskId] = pkg
            }
        }

        return map
    }

    /**
     * Executes a shell command via [uiAutomation] and returns the complete trimmed stdout output.
     */
    internal fun executeShellCommand(
        command: String,
        uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation,
    ): String {
        return try {
            uiAutomation.executeShellCommand(command).use { pfd ->
                ParcelFileDescriptor.AutoCloseInputStream(pfd).use { stream: InputStream ->
                    stream.bufferedReader().readText().trim()
                }
            }
        } catch (e: Exception) {
            Log.e("SpatialSceneHelper", "Error executing shell command: $command", e)
            ""
        }
    }

    private fun isPackageChar(c: Char): Boolean =
        c.isLetterOrDigit() || c == '_' || c == '.' || c == '$'

    private fun containsSegment(
        text: String,
        target: String,
        requireTrailingBoundary: Boolean,
    ): Boolean {
        if (target.isEmpty() || text.isEmpty()) return false
        var index = 0
        while (true) {
            val found = text.indexOf(target, index)
            if (found < 0) return false
            val end = found + target.length
            val validStart = (found == 0 || !isPackageChar(text[found - 1]))
            val validEnd =
                (!requireTrailingBoundary || end == text.length || !isPackageChar(text[end]))
            if (validStart && validEnd) {
                return true
            }
            index = found + 1
        }
    }

    internal fun containsPackage(text: String, pkg: String): Boolean =
        containsSegment(text, pkg, requireTrailingBoundary = true)

    private val ACTIVITY_RECORD_PATTERN =
        Pattern.compile(
            "ActivityRecord\\{[^}]*?\\s+([a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)+)/[^}\\s]+\\s+t(\\d+)\\b"
        )
    private val TASK_AFFINITY_PATTERN =
        Pattern.compile(
            "Task\\{[^}]*?#(\\d+)[^}]*?\\bA=(?:\\d+:)?([a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)+)"
        )
    private val TASK_COMPONENT_PATTERN =
        Pattern.compile("Task\\{[^}]*?#(\\d+)[^}]*?\\s+([a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)+)/")
    private val NODE_LINE_PATTERN =
        Pattern.compile("^\\s*Node\\s+(.+?)(?:\\s*\\([^)]*\\))?(?:\\s+id=(\\d+))?\\s*:?\\s*$")
    private val ATTR_ARRAY_PATTERN = Pattern.compile("([a-zA-Z0-9_#.-]+)=\\(([^)]+)\\)")
    private val REFORM_OPTIONS_PATTERN = Pattern.compile("reformOptions=<([^>]+)>")
    private val SYS_UI_METADATA_PATTERN = Pattern.compile("sysUIMetadata=<([^>]+)>")
    private val PROP_KEY_VALUE_PATTERN = Pattern.compile("([a-zA-Z0-9_#.-]+)=([a-zA-Z0-9_#.-]+)")
    private val WHITESPACE_PATTERN = Pattern.compile("\\s+")
}

/**
 * Represents a spatial scene graph snapshot consisting of root [SpatialNode] entities.
 *
 * @param rootNodes Top-level root nodes in the spatial scene graph (typically the single `Root`
 *   node).
 * @param taskPackageMap Optional mapping from task IDs to Android package names.
 */
public class SpatialScene
@JvmOverloads
constructor(
    public val rootNodes: List<SpatialNode>,
    public val taskPackageMap: Map<Int, String> = emptyMap(),
) {

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
}

/**
 * Encapsulates an Android task's entire spatial scene subtree, rooted at
 * `space-root-task-<taskId>`.
 *
 * In OpenXR, this corresponds to the application's root reference space (`XrSpace`).
 */
public class SpatialTask
@JvmOverloads
constructor(public val activitySpaceRoot: SpatialNode, packageName: String? = null) {

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
}

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

    private val _children = mutableListOf<SpatialNode>()

    public val children: List<SpatialNode>
        get() = _children

    internal fun addChild(child: SpatialNode) {
        child.parent = this
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

    internal companion object {
        private val REFORM_ENABLE_REGEX = Regex("enable=(\\d+)")
        private val SYS_UI_SPECIAL_NODE_REGEX = Regex("specialNode=(\\d+)")
        private val TASK_WINDOW_NODE_REGEX = Regex("^task-\\d+-window$")
    }
}
