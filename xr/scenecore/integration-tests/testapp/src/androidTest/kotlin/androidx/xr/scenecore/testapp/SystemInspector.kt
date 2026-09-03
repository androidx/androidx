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

package androidx.xr.scenecore.testapp

import androidx.test.uiautomator.UiDevice
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertWithMessage
import java.util.regex.Pattern
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Represents a parsed 3D spatial node from the system compositor / Core Presentation Manager (CPM).
 *
 * Note: World space in the system compositor is guaranteed to be 1:1 unit scale where 1.0 = 1
 * meter.
 */
data class SystemNode(
    val localPosition: Vector3,
    val worldPosition: Vector3,
    val localRotation: Quaternion,
    val worldRotation: Quaternion,
    val localScale: Vector3,
    val worldScale: Vector3,
)

/**
 * Inspector utility for querying and validating underlying platform (system compositor) scene graph
 * state from Core Presentation Manager (`spf_cpm`).
 *
 * Note about Compositor Node Hierarchy Lifecycle: Panels vs. 3D Meshes (glTF) In Android XR,
 * [PanelEntity] instances are backed by Android 2D window surfaces (`SurfaceControl`) managed by
 * WindowManager and SurfaceFlinger. Each panel is allocated an independent, named compositor node
 * in the system compositor (`dumpsys spf_cpm`), allowing OS-level verification of world position,
 * rotation, and scale.
 *
 * Nodes will only appear in the system compositor if one of their descendants is rendered (e.g.,
 * has an active surface buffer or texture layer). Pure, non-rendered [Entity] instances are
 * lightweight client-side transform nodes managed by SceneCore and SplitEngine; they do not
 * allocate compositor surface resources in the system compositor on their own. Attaching a
 * [PanelEntity] as a leaf node ensures the complete ancestor transform hierarchy is committed and
 * visible in the system hierarchy.
 *
 * In contrast, 3D meshes (such as glTF models) are rendered internally by SplitEngine / Filament.
 * The system compositor only composites the overall Subspace buffer rather than individual 3D mesh
 * nodes, so the source-of-truth transform data for non-panel 3D entities resides within SceneCore /
 * SplitEngine runtime state instead of the system compositor.
 */
object SystemInspector {
    private val ATTR_PATTERN = Pattern.compile("([a-zA-Z0-9_]+)\\s*=\\s*\\(([^)]+)\\)")
    private val WHITESPACE_REGEX = "\\s+".toRegex()

    /** Checks whether the target environment supports system compositor CPM dumpsys. */
    fun isAvailable(device: UiDevice): Boolean =
        device.executeShellCommand("dumpsys -l").contains("spf_cpm")

    /** Captures the live system compositor CPM dumpsys output. */
    fun getDump(device: UiDevice): String = device.executeShellCommand("dumpsys spf_cpm")

    /**
     * Parses the system compositor CPM dumpsys in a single pass to locate all nodes matching any of
     * [nodeFragments] and extracts their local and world transforms.
     */
    fun findNodes(dump: String, vararg nodeFragments: String): Map<String, SystemNode> {
        if (nodeFragments.isEmpty()) return emptyMap()
        val targetFragments = nodeFragments.toSet()
        val foundNodes = mutableMapOf<String, SystemNode>()
        var currentMatchingFragment: String? = null
        var localPos: Vector3? = null
        var worldPos: Vector3? = null
        var localRot: Quaternion? = null
        var worldRot: Quaternion? = null
        var localScale: Vector3? = null
        var worldScale: Vector3? = null

        fun commitCurrentNode() {
            val fragment = currentMatchingFragment
            val world = worldPos
            if (fragment != null && world != null) {
                foundNodes[fragment] =
                    SystemNode(
                        localPosition = localPos ?: world,
                        worldPosition = world,
                        localRotation = localRot ?: worldRot ?: Quaternion.Identity,
                        worldRotation = worldRot ?: Quaternion.Identity,
                        localScale = localScale ?: worldScale ?: Vector3.One,
                        worldScale = worldScale ?: Vector3.One,
                    )
            }
        }

        for (line in dump.lineSequence()) {
            val trimmed = line.trim()
            if (
                (trimmed.startsWith("Node ") || trimmed.startsWith("Node(")) &&
                    !line.contains("ImpNode")
            ) {
                commitCurrentNode()
                currentMatchingFragment = targetFragments.firstOrNull { line.contains(it) }
                localPos = null
                worldPos = null
                localRot = null
                worldRot = null
                localScale = null
                worldScale = null
                continue
            }

            if (currentMatchingFragment != null) {
                val matcher = ATTR_PATTERN.matcher(line)
                while (matcher.find()) {
                    val key = matcher.group(1)
                    val values =
                        matcher
                            .group(2)
                            ?.replace(",", " ")
                            ?.trim()
                            ?.split(WHITESPACE_REGEX)
                            ?.mapNotNull { it.toFloatOrNull() } ?: emptyList()

                    when (key) {
                        "ImpNodeLocalPosition" ->
                            if (values.size >= 3)
                                localPos = Vector3(values[0], values[1], values[2])
                        "ImpNodeWorldPosition" ->
                            if (values.size >= 3)
                                worldPos = Vector3(values[0], values[1], values[2])
                        "ImpNodeLocalRotation" ->
                            if (values.size >= 4)
                                localRot = Quaternion(values[1], values[2], values[3], values[0])
                        "ImpNodeWorldRotation" ->
                            if (values.size >= 4)
                                worldRot = Quaternion(values[1], values[2], values[3], values[0])
                        "ImpNodeLocalScale" ->
                            if (values.size >= 3)
                                localScale = Vector3(values[0], values[1], values[2])
                        "ImpNodeWorldScale" ->
                            if (values.size >= 3)
                                worldScale = Vector3(values[0], values[1], values[2])
                    }
                }
            }
        }
        commitCurrentNode()
        return foundNodes
    }

    /**
     * Parses the system compositor CPM dumpsys to locate a specific node matching [nodeFragment]
     * and extracts its local and world transforms.
     */
    fun findNode(dump: String, nodeFragment: String): SystemNode? =
        findNodes(dump, nodeFragment)[nodeFragment]

    /**
     * Polls the system compositor CPM until a single node matching [nodeFragment] satisfying
     * [predicate] is committed or [timeoutMs] expires.
     */
    suspend fun awaitNode(
        device: UiDevice,
        nodeFragment: String,
        timeoutMs: Long = 3000,
        pollIntervalMs: Long = 50,
        predicate: (SystemNode) -> Boolean = { true },
    ): SystemNode {
        var foundNode: SystemNode? = null
        val isSynced =
            withTimeoutOrNull(timeoutMs) {
                while (true) {
                    val dump = getDump(device)
                    val node = findNode(dump, nodeFragment)
                    if (node != null && predicate(node)) {
                        foundNode = node
                        break
                    }
                    delay(pollIntervalMs)
                }
                true
            } ?: false
        assertWithMessage("System compositor committed node: '$nodeFragment'")
            .that(isSynced)
            .isTrue()
        return checkNotNull(foundNode)
    }

    /**
     * Polls the system compositor CPM until two nodes matching [nameA] and [nameB] satisfying
     * [predicate] are committed or [timeoutMs] expires.
     */
    suspend fun awaitNodes(
        device: UiDevice,
        nameA: String,
        nameB: String,
        timeoutMs: Long = 3000,
        pollIntervalMs: Long = 50,
        predicate: (SystemNode, SystemNode) -> Boolean = { _, _ -> true },
    ): Pair<SystemNode, SystemNode> {
        var nodeA: SystemNode? = null
        var nodeB: SystemNode? = null
        val isSynced =
            withTimeoutOrNull(timeoutMs) {
                while (true) {
                    val dump = getDump(device)
                    val nodes = findNodes(dump, nameA, nameB)
                    val a = nodes[nameA]
                    val b = nodes[nameB]
                    if (a != null && b != null && predicate(a, b)) {
                        nodeA = a
                        nodeB = b
                        break
                    }
                    delay(pollIntervalMs)
                }
                true
            } ?: false
        assertWithMessage("System compositor committed nodes: '$nameA' and '$nameB'")
            .that(isSynced)
            .isTrue()
        return Pair(checkNotNull(nodeA), checkNotNull(nodeB))
    }
}
