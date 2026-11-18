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

package androidx.xr.testutils.runtime.cpm

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
import androidx.xr.testutils.SnapshotXrScene
import androidx.xr.testutils.SnapshotXrSpace
import java.io.EOFException
import java.io.InputStream
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.math.abs
import kotlin.math.hypot

internal const val ARG_SCENE_USE_PROTO: String = "xrSceneUseProto"
internal const val DEFAULT_BINARY_SHELL_TIMEOUT_MS: Long = 3_000L

private val binaryCommandExecutor = Executors.newCachedThreadPool { runnable ->
    Thread(runnable, "XrBinaryShellExecutor").apply { isDaemon = true }
}

internal fun shouldUseProtoCaptureByDefault(
    argLookup: (String) -> String? = { key ->
        try {
            InstrumentationRegistry.getArguments().getString(key)
        } catch (_: IllegalStateException) {
            null
        }
    }
): Boolean = argLookup(ARG_SCENE_USE_PROTO)?.equals("true", ignoreCase = true) == true

internal fun captureDefaultSceneWithPolling(
    uiAutomation: UiAutomation,
    maxWaitMs: Long = DEFAULT_SCENE_POLL_MAX_WAIT_MS,
    useProto: Boolean = shouldUseProtoCaptureByDefault(),
): SnapshotXrScene =
    captureDefaultSceneWithPolling(
        includeRenderer = true,
        maxWaitMs = maxWaitMs,
        useProto = useProto,
        binaryShellRunner = { command -> executeBinaryShellCommand(command, uiAutomation) },
        textShellRunner = { command -> executeSceneShellCommand(command, uiAutomation) },
    )

@VisibleForTesting
internal fun captureDefaultSceneWithPolling(
    includeRenderer: Boolean = true,
    maxWaitMs: Long = DEFAULT_SCENE_POLL_MAX_WAIT_MS,
    useProto: Boolean = shouldUseProtoCaptureByDefault(),
    sleeper: (Long) -> Unit = { ms ->
        try {
            Thread.sleep(ms)
        } catch (_: InterruptedException) {}
    },
    clockMs: () -> Long = System::currentTimeMillis,
    binaryShellRunner: (String) -> ByteArray,
    textShellRunner: (String) -> String,
): SnapshotXrScene =
    if (useProto) {
        captureProtoSceneWithPolling(
            includeRenderer = includeRenderer,
            maxWaitMs = maxWaitMs,
            sleeper = sleeper,
            clockMs = clockMs,
            binaryShellRunner = binaryShellRunner,
            textShellRunner = textShellRunner,
        )
    } else {
        captureSceneWithPolling(
            maxWaitMs = maxWaitMs,
            sleeper = sleeper,
            clockMs = clockMs,
            shellRunner = textShellRunner,
        )
    }

/**
 * Executes [command] via [uiAutomation] and returns raw stdout bytes, bounding reads with
 * [timeoutMs] so a stalled compositor cannot hang the test runner.
 */
internal fun executeBinaryShellCommand(
    command: String,
    uiAutomation: UiAutomation? = null,
    timeoutMs: Long = DEFAULT_BINARY_SHELL_TIMEOUT_MS,
): ByteArray {
    return try {
        val automation = uiAutomation ?: InstrumentationRegistry.getInstrumentation().uiAutomation
        if (automation == null) {
            Log.w(SCENE_CAPTURE_TAG, "UiAutomation unavailable for binary shell command: $command")
            return ByteArray(0)
        }
        val pfd = automation.executeShellCommand(command)
        val bytes =
            readBinaryShellWithTimeout(
                command = command,
                timeoutMs = timeoutMs,
                onTimeout = {
                    try {
                        pfd.close()
                    } catch (_: Exception) {}
                },
            ) {
                pfd.use {
                    ParcelFileDescriptor.AutoCloseInputStream(it).use { stream: InputStream ->
                        stream.readBytes()
                    }
                }
            }
        if (bytes.isEmpty()) {
            Log.w(SCENE_CAPTURE_TAG, "Binary shell command returned empty bytes: $command")
        }
        bytes
    } catch (e: Exception) {
        Log.w(SCENE_CAPTURE_TAG, "Error executing binary shell command: $command", e)
        ByteArray(0)
    }
}

@VisibleForTesting
internal fun readBinaryShellWithTimeout(
    command: String,
    timeoutMs: Long = DEFAULT_BINARY_SHELL_TIMEOUT_MS,
    onTimeout: () -> Unit = {},
    reader: () -> ByteArray,
): ByteArray {
    val future = binaryCommandExecutor.submit(Callable(reader))
    return try {
        future.get(timeoutMs, TimeUnit.MILLISECONDS)
    } catch (e: TimeoutException) {
        Log.w(SCENE_CAPTURE_TAG, "Binary shell command timed out after ${timeoutMs}ms: $command", e)
        future.cancel(true)
        onTimeout()
        ByteArray(0)
    } catch (e: InterruptedException) {
        Log.w(SCENE_CAPTURE_TAG, "Interrupted while reading binary shell output: $command", e)
        future.cancel(true)
        onTimeout()
        Thread.currentThread().interrupt()
        ByteArray(0)
    } catch (e: Exception) {
        Log.e(SCENE_CAPTURE_TAG, "Error reading binary shell output: $command", e)
        ByteArray(0)
    }
}

/**
 * Polls `dumpsys spf_cpm --proto` (and optional `--proto --renderer`) until active transitions
 * clear and tasks are ready, falling back to [captureSceneWithPolling] when binary proto output is
 * empty or unparseable.
 */
internal fun captureProtoSceneWithPolling(
    uiAutomation: UiAutomation,
    includeRenderer: Boolean = true,
    maxWaitMs: Long = DEFAULT_SCENE_POLL_MAX_WAIT_MS,
): SnapshotXrScene =
    captureProtoSceneWithPolling(
        includeRenderer = includeRenderer,
        maxWaitMs = maxWaitMs,
        binaryShellRunner = { command -> executeBinaryShellCommand(command, uiAutomation) },
        textShellRunner = { command -> executeSceneShellCommand(command, uiAutomation) },
    )

@VisibleForTesting
internal fun captureProtoSceneWithPolling(
    includeRenderer: Boolean = true,
    maxWaitMs: Long = DEFAULT_SCENE_POLL_MAX_WAIT_MS,
    sleeper: (Long) -> Unit = { ms ->
        try {
            Thread.sleep(ms)
        } catch (_: InterruptedException) {}
    },
    clockMs: () -> Long = System::currentTimeMillis,
    binaryShellRunner: (String) -> ByteArray,
    textShellRunner: (String) -> String,
): SnapshotXrScene {
    var scene: SnapshotXrScene
    var cachedTaskPackageMap: Map<Int, String>? = null
    val lastQueriedAtMs = mutableMapOf<Int, Long>()
    val startTime = clockMs()
    while (true) {
        val cpmBytes = binaryShellRunner("dumpsys spf_cpm --proto")
        if (cpmBytes.isEmpty()) {
            Log.w(
                SCENE_CAPTURE_TAG,
                "dumpsys spf_cpm --proto returned empty bytes; falling back to text parser",
            )
            return captureSceneWithPolling(
                maxWaitMs = maxWaitMs,
                sleeper = sleeper,
                clockMs = clockMs,
                shellRunner = textShellRunner,
            )
        }
        val rendererBytes =
            if (includeRenderer) {
                val bytes = binaryShellRunner("dumpsys spf_cpm --proto --renderer")
                if (bytes.isEmpty()) {
                    Log.w(
                        SCENE_CAPTURE_TAG,
                        "dumpsys spf_cpm --proto --renderer returned empty bytes; falling back to text parser",
                    )
                    return captureSceneWithPolling(
                        maxWaitMs = maxWaitMs,
                        sleeper = sleeper,
                        clockMs = clockMs,
                        shellRunner = textShellRunner,
                    )
                }
                bytes
            } else {
                null
            }
        val parsed =
            try {
                CpmProtoSceneParser.parseScene(
                    cpmProtoBytes = cpmBytes,
                    rendererProtoBytes = rendererBytes,
                )
            } catch (e: Exception) {
                Log.w(
                    SCENE_CAPTURE_TAG,
                    "Failed to parse dumpsys spf_cpm --proto (${cpmBytes.size} bytes); falling back to text parser",
                    e,
                )
                return captureSceneWithPolling(
                    maxWaitMs = maxWaitMs,
                    sleeper = sleeper,
                    clockMs = clockMs,
                    shellRunner = textShellRunner,
                )
            }
        val taskIds = extractTaskIdsFromRoots(parsed.rootNodes)
        val currentMap = cachedTaskPackageMap
        val nowMs = clockMs()
        val unresolvedTaskIds = taskIds.filter { taskId ->
            taskId >= 0 && (currentMap == null || !currentMap.containsKey(taskId))
        }
        val needsTaskPackageRefresh =
            currentMap == null ||
                unresolvedTaskIds.any { taskId ->
                    val lastQueried = lastQueriedAtMs[taskId]
                    lastQueried == null ||
                        nowMs - lastQueried >= UNRESOLVED_TASK_REFRESH_COOLDOWN_MS
                }
        val resolvedMap =
            if (needsTaskPackageRefresh) {
                val activitiesDump = textShellRunner("dumpsys activity activities")
                val parsedMap = parseTaskPackageMap(activitiesDump)
                for (taskId in unresolvedTaskIds) {
                    lastQueriedAtMs[taskId] = nowMs
                }
                ((currentMap ?: emptyMap()) + parsedMap).also { cachedTaskPackageMap = it }
            } else {
                currentMap!!
            }
        scene =
            SnapshotXrScene(
                rootNodes = parsed.rootNodes,
                taskPackageMap = resolvedMap,
                stats = parsed.stats,
            )
        val hasTransition = scene.hasActiveTransition
        val isReady =
            scene.tasks.any { task ->
                task.allNodes.any {
                    it.hasSurfaceBounds || it.isEmbeddedWindowLeash || it.hasSubspace
                }
            }
        if ((!hasTransition && isReady) || clockMs() - startTime > maxWaitMs) {
            break
        }
        sleeper(100L)
    }
    return scene
}

/**
 * Collider dimensions decoded from an Impress node component.
 *
 * Extents are approximate world-scaled AABB dimensions in the enclosing subspace root's coordinate
 * frame (before applying [SnapshotXrSpace.rotation], which downstream screenshot and interaction
 * helpers apply). Assumes the enclosing Impress subspace root is rotation-aligned with the CPM node
 * carrying [SnapshotXrSpace.subspaceBoundsInMeters].
 */
internal data class ImpressColliderBounds(
    val type: String,
    val centerInMeters: Vector3,
    val extentsInMeters: Vector3,
    val radiusInMeters: Float? = null,
    val heightInMeters: Float? = null,
)

/**
 * SplitEngine/Impress 3D subspace subtree decoded from `dumpsys spf_cpm --proto --renderer`.
 *
 * @property isDisabled Whether this subspace node or any of its Impress ancestors is disabled
 *   (`!isActive`).
 */
internal data class ImpressSubspaceNode(
    val subspaceId: Int,
    val subspaceName: String,
    val nodeName: String,
    val worldPose: Pose,
    val worldScale: Vector3 = Vector3.One,
    val isDisabled: Boolean = false,
    val colliders: List<ImpressColliderBounds> = emptyList(),
    val directChildLocalScales: List<Vector3> = emptyList(),
    val childNodeNames: List<String> = emptyList(),
) {
    /**
     * Approximate 3D bounding extents in meters `(width, height, depth)` in the subspace root's
     * local frame (before [SnapshotXrSpace.rotation]), or `null` if [isDisabled] or unavailable.
     */
    val estimatedExtentsInMeters: Vector3?
        get() {
            if (isDisabled) return null
            if (colliders.isNotEmpty()) {
                var maxX = 0f
                var maxY = 0f
                var maxZ = 0f
                for (collider in colliders) {
                    if (collider.extentsInMeters.x > maxX) maxX = collider.extentsInMeters.x
                    if (collider.extentsInMeters.y > maxY) maxY = collider.extentsInMeters.y
                    if (collider.extentsInMeters.z > maxZ) maxZ = collider.extentsInMeters.z
                }
                if (maxX > 0f || maxY > 0f || maxZ > 0f) {
                    return Vector3(maxX, maxY, maxZ)
                }
            }
            if (subspaceName.startsWith(STEREO_SURFACE_SUBSPACE_PREFIX)) {
                val childScale = directChildLocalScales.firstOrNull()
                if (childScale != null) {
                    val sx = abs(childScale.x * worldScale.x)
                    val sy = abs(childScale.y * worldScale.y)
                    val sz = abs(childScale.z * worldScale.z)
                    if (sx > 0f && sy > 0f) {
                        return Vector3(sx, sy, if (sz > 0f) sz else 0.01f)
                    }
                }
            }
            return null
        }

    private companion object {
        private const val STEREO_SURFACE_SUBSPACE_PREFIX = "stereo_surface_panel_entity_subspace_"
    }
}

/**
 * Parses binary protobuf dumps produced by `dumpsys spf_cpm --proto` (`CpmNodeTrace`) and optional
 * `dumpsys spf_cpm --proto --renderer` (`ImpressSceneDump`) without external protobuf runtime
 * dependencies.
 */
internal object CpmProtoSceneParser {

    /**
     * Decodes a binary [CpmNodeTrace] snapshot and optional binary [ImpressSceneDump] renderer
     * snapshot into a [SnapshotXrScene] with exact IEEE-754 transforms and resolved subspace
     * extents.
     */
    fun parseScene(
        cpmProtoBytes: ByteArray,
        rendererProtoBytes: ByteArray? = null,
        taskPackageMap: Map<Int, String> = emptyMap(),
    ): SnapshotXrScene {
        val cpmTrace = parseCpmNodeTrace(cpmProtoBytes)
        val impressData =
            if (rendererProtoBytes != null && rendererProtoBytes.isNotEmpty()) {
                parseImpressSceneDump(rendererProtoBytes)
            } else {
                ImpressParsedData(emptyMap(), emptyList())
            }

        val cpmNodesById = LinkedHashMap<Long, MutableCpmNode>(cpmTrace.size)
        for (node in cpmTrace) {
            val impressTransform = impressData.nodeTransformsByCpmId[node.id]
            // Match SpaceFlingerScene::dumpNode ImpNodeWorld* when mirrored in ImpressSceneDump;
            // CPM NodeComputedState resets to origin on transformUpdateRequest nodes (Home Space,
            // HEAD, ANCHOR, FOLLOW).
            node.position =
                impressTransform?.worldPose?.translation
                    ?: node.computedPos
                    ?: node.requestedPos
                    ?: Vector3.Zero
            node.rotation =
                impressTransform?.worldPose?.rotation
                    ?: node.composedQuat
                    ?: node.requestedQuat
                    ?: Quaternion.Identity
            node.scale =
                impressTransform?.worldScale
                    ?: node.computedScale
                    ?: node.requestedScale
                    ?: Vector3.One
            node.isActive =
                impressTransform?.isActive ?: (node.computedVisible ?: node.localVisible)
            cpmNodesById[node.id] = node
        }

        // Link tree relationships using parent/children IDs while preventing cycles.
        for (node in cpmTrace) {
            for (childId in node.childIds) {
                val child = cpmNodesById[childId] ?: continue
                if (child !== node && child.parentNode == null && !isAncestor(child, node)) {
                    child.parentNode = node
                    node.childNodes += child
                }
            }
        }
        val roots = mutableListOf<MutableCpmNode>()
        for (node in cpmTrace) {
            val parent = node.parentNode
            if (parent == null) {
                val declaredParent = if (node.parentId > 0L) cpmNodesById[node.parentId] else null
                if (
                    declaredParent != null &&
                        declaredParent !== node &&
                        !isAncestor(node, declaredParent)
                ) {
                    node.parentNode = declaredParent
                    if (!declaredParent.childNodes.contains(node)) {
                        declaredParent.childNodes += node
                    }
                } else {
                    roots += node
                }
            }
        }

        // CpmNodeTrace does not serialize isAppSpatialEmbeddedWindowLeash; infer it for embedded
        // panel containers before constructing SnapshotXrSpace instances.
        val leashVisited = HashSet<Long>(cpmTrace.size)
        for (root in roots) {
            inferEmbeddedWindowLeashesInTree(root, leashVisited)
        }

        val subspaceByName = HashMap<String, ImpressSubspaceNode>()
        val subspaceById = HashMap<Int, ImpressSubspaceNode>()
        val subspaceIdByName = HashMap<String, Int>()
        for (node in cpmTrace) {
            val name = node.subspaceName
            val sid = node.subspaceId
            if (name != null && sid != null && sid >= 0) {
                subspaceIdByName[name] = sid
            }
        }
        for (raw in impressData.subspaces) {
            val resolvedId = subspaceIdByName[raw.subspaceName] ?: raw.subspaceId
            val resolved =
                if (resolvedId != raw.subspaceId) raw.copy(subspaceId = resolvedId) else raw
            if (
                resolved.subspaceName.isNotEmpty() &&
                    shouldPreferSubspace(subspaceByName[resolved.subspaceName], resolved)
            ) {
                subspaceByName[resolved.subspaceName] = resolved
            }
            if (shouldPreferSubspace(subspaceById[resolved.subspaceId], resolved)) {
                subspaceById[resolved.subspaceId] = resolved
            }
        }

        val spaceVisited = HashSet<Long>(cpmTrace.size)
        val rootSpaces = roots.mapNotNull {
            buildSnapshotXrSpace(
                mutable = it,
                depth = 0,
                subspaceByName = subspaceByName,
                subspaceById = subspaceById,
                visited = spaceVisited,
            )
        }

        return SnapshotXrScene(rootNodes = rootSpaces, taskPackageMap = taskPackageMap)
    }

    private fun shouldPreferSubspace(
        existing: ImpressSubspaceNode?,
        candidate: ImpressSubspaceNode,
    ): Boolean {
        if (existing == null) return true
        if (candidate.isDisabled != existing.isDisabled) {
            return !candidate.isDisabled
        }
        val existingIsAppSide = existing.nodeName.endsWith(APP_SIDE_SUBSPACE_SUFFIX)
        val candidateIsAppSide = candidate.nodeName.endsWith(APP_SIDE_SUBSPACE_SUFFIX)
        if (candidateIsAppSide != existingIsAppSide) {
            return candidateIsAppSide
        }
        return existing.estimatedExtentsInMeters == null &&
            candidate.estimatedExtentsInMeters != null
    }

    private fun resolveSubspaceExtents(
        node: MutableCpmNode,
        subspaceByName: Map<String, ImpressSubspaceNode>,
        subspaceById: Map<Int, ImpressSubspaceNode>,
    ): Vector3? {
        val target =
            if (node.hasSubspace && (node.subspaceName != null || node.subspaceId != null)) {
                node
            } else {
                node.childNodes.firstOrNull { it.hasSubspace } ?: node
            }
        target.subspaceName
            ?.takeIf { it.isNotEmpty() }
            ?.let { subspaceByName[it]?.estimatedExtentsInMeters }
            ?.let {
                return it
            }
        target.subspaceId
            ?.takeIf { it >= 0 }
            ?.let { subspaceById[it]?.estimatedExtentsInMeters }
            ?.let {
                return it
            }
        return null
    }

    private fun isAncestor(candidateAncestor: MutableCpmNode, node: MutableCpmNode): Boolean {
        var curr: MutableCpmNode? = node
        val visited = HashSet<MutableCpmNode>()
        while (curr != null) {
            if (curr === candidateAncestor) return true
            if (!visited.add(curr)) return true
            curr = curr.parentNode
        }
        return false
    }

    private fun buildSnapshotXrSpace(
        mutable: MutableCpmNode,
        depth: Int,
        subspaceByName: Map<String, ImpressSubspaceNode>,
        subspaceById: Map<Int, ImpressSubspaceNode>,
        visited: MutableSet<Long>,
    ): SnapshotXrSpace? {
        if (mutable.id != -1L && !visited.add(mutable.id)) {
            return null
        }
        val visibleOnBoth = mutable.computedVisible ?: mutable.localVisible
        val stereoVisibility = if (visibleOnBoth) "visible on both" else "hidden on both"
        val isVisible = mutable.localVisible && visibleOnBoth
        val surfaceBoundsInPixels =
            (mutable.surfaceBoundsInPx
                    ?: mutable.touchableRegion?.let {
                        Vector2(
                            abs((it.right - it.left).toFloat()),
                            abs((it.bottom - it.top).toFloat()),
                        )
                    })
                ?.takeIf { it.x > 0f && it.y > 0f }
        val ppm = mutable.computedPixelsPerMeter ?: mutable.pixelsPerMeter
        val resolvedBounds =
            if (mutable.hasSubspace || mutable.childNodes.any { it.hasSubspace }) {
                resolveSubspaceExtents(mutable, subspaceByName, subspaceById)
            } else {
                null
            }

        val attributes = buildMap {
            put(
                "ImpNodeWorldPosition",
                floatArrayOf(mutable.position.x, mutable.position.y, mutable.position.z),
            )
            put(
                "ImpNodeWorldRotation",
                floatArrayOf(
                    mutable.rotation.w,
                    mutable.rotation.x,
                    mutable.rotation.y,
                    mutable.rotation.z,
                ),
            )
            put(
                "ImpNodeWorldScale",
                floatArrayOf(mutable.scale.x, mutable.scale.y, mutable.scale.z),
            )
            mutable.surfaceBoundsInMeters?.let {
                put("root2DSurfaceBoundsInMeters", floatArrayOf(it.x, it.y))
            }
            surfaceBoundsInPixels?.let {
                put("root2DSurfaceBoundsInPixels", floatArrayOf(it.x, it.y))
            }
            mutable.touchableRegion?.let {
                put(
                    "touchableRegion",
                    floatArrayOf(
                        it.left.toFloat(),
                        it.top.toFloat(),
                        it.right.toFloat(),
                        it.bottom.toFloat(),
                    ),
                )
            }
        }

        val properties = buildMap {
            put("stereoVisibility", stereoVisibility)
            if (!mutable.localVisible) {
                put("requested-vis", "hide")
            }
            put("ImpNodeIsActive", mutable.isActive.toString())
            mutable.coordinateSpace?.let {
                put("transformUpdateRequest.coordinateSpace", it.toString())
            }
            mutable.bufferSize?.let { put("buffer", "${it.x.toInt()}x${it.y.toInt()}") }
            ppm?.let { put("computedPixelsPerMeter", it.toString()) }
            if (mutable.inputId >= 0) {
                put("input-id", mutable.inputId.toString())
            }
            if (!mutable.canReceiveInput) {
                put("canReceiveInput", "false")
            }
            if (mutable.isAndroid2d) {
                put("isAndroid2d", "true")
            }
            if (mutable.isEmbeddedWindowLeash) {
                put("isAppSpatialEmbeddedWindowLeash", "true")
            }
            if (mutable.isSysUi) {
                put("isSysUi", "true")
            }
            if (mutable.hasSubspace) {
                put("hasSubspace", "true")
            }
            if (mutable.reformEnable != 0) {
                put("reformOptions", "<enable=${mutable.reformEnable}>")
            }
            mutable.sysUiSpecialNode?.let { put("sysUIMetadata", "<specialNode=$it>") }
            mutable.requestedAlpha?.let { put("requestedAlpha", it.toString()) }
            mutable.computedAlpha?.let { put("computedAlpha", it.toString()) }
        }

        val header =
            if (isVisible) {
                "Node ${mutable.name} id=${mutable.id.toInt()}:"
            } else {
                "Node ${mutable.name} id=${mutable.id.toInt()} (hidden):"
            }
        val space =
            SnapshotXrSpace(
                header = header,
                name = mutable.name,
                id = mutable.id.toInt(),
                depth = depth,
                position = mutable.position,
                rotation = mutable.rotation,
                scale = mutable.scale,
                surfaceBoundsInMeters = mutable.surfaceBoundsInMeters,
                surfaceBoundsInPixels = surfaceBoundsInPixels,
                touchableRegion = mutable.touchableRegion,
                bufferSize = mutable.bufferSize,
                computedPixelsPerMeter = ppm,
                inputId = mutable.inputId,
                canReceiveInput = mutable.canReceiveInput,
                isAndroid2d = mutable.isAndroid2d,
                isEmbeddedWindowLeash = mutable.isEmbeddedWindowLeash,
                isSysUi = mutable.isSysUi,
                hasSubspace = mutable.hasSubspace,
                isMovable = (mutable.reformEnable and 1) != 0,
                isResizable = (mutable.reformEnable and 2) != 0,
                stereoVisibility = stereoVisibility,
                isVisible = isVisible,
                coordinateSpace = mutable.coordinateSpace,
                isActive = mutable.isActive,
                sysUiSpecialNode = mutable.sysUiSpecialNode,
                scrimTargetAlpha = 0f,
                attributes = attributes,
                properties = properties,
                subspaceBoundsInMeters = resolvedBounds,
            )
        for (child in mutable.childNodes) {
            buildSnapshotXrSpace(child, depth + 2, subspaceByName, subspaceById, visited)?.let {
                space.addChild(it)
            }
        }
        return space
    }

    private fun inferEmbeddedWindowLeashesInTree(
        root: MutableCpmNode,
        visited: MutableSet<Long>,
    ) {
        fun visit(node: MutableCpmNode, inTask: Boolean, hasTaskWindowWrapperAncestor: Boolean) {
            if (node.id != -1L && !visited.add(node.id)) return
            val isTaskRoot = node.name.startsWith("space-root-task-")
            val nowInTask = inTask || isTaskRoot
            val isTaskWindowWrapper = isTaskWindowWrapperName(node.name)
            val nowHasWrapperAncestor = hasTaskWindowWrapperAncestor || isTaskWindowWrapper

            if (nowInTask && !isTaskRoot && !node.name.startsWith("space-transform-")) {
                val isTaskWindowLeash = node.childNodes.any { it.name.startsWith("window-task-") }
                val isActivityWindowLeash =
                    node.childNodes.any { TASK_WINDOW_NODE_REGEX.matches(it.name) }
                if (
                    !node.isSysUi &&
                        !isTaskWindowLeash &&
                        !isActivityWindowLeash &&
                        !isTaskWindowWrapper &&
                        !hasTaskWindowWrapperAncestor
                ) {
                    val parentIsAndroid2d = node.parentNode?.isAndroid2d == true
                    val parentIsEmbeddedLeash = node.parentNode?.isEmbeddedWindowLeash == true
                    val hasDirectNonSysUi2dChild =
                        node.childNodes.any { child ->
                            child.isAndroid2d &&
                                !child.isSysUi &&
                                !isTaskWindowWrapperName(child.name)
                        }
                    if (
                        (!node.isAndroid2d && hasDirectNonSysUi2dChild) ||
                            (node.isAndroid2d && !parentIsAndroid2d && !parentIsEmbeddedLeash)
                    ) {
                        node.isEmbeddedWindowLeash = true
                    }
                }
            }

            for (child in node.childNodes) {
                visit(child, nowInTask, nowHasWrapperAncestor)
            }
        }
        visit(root, inTask = false, hasTaskWindowWrapperAncestor = false)
    }

    private fun isTaskWindowWrapperName(name: String): Boolean =
        name.startsWith("window-task-") || TASK_WINDOW_NODE_REGEX.matches(name)

    private fun parseCpmNodeTrace(bytes: ByteArray): List<MutableCpmNode> {
        val reader = ProtoWireReader(bytes)
        val nodes = mutableListOf<MutableCpmNode>()
        while (!reader.isAtEnd) {
            val tag = reader.readTag()
            val fieldNumber = tag ushr 3
            val wireType = tag and 0x7
            when (fieldNumber) {
                1 -> {
                    if (wireType == WIRETYPE_FIXED64) {
                        reader.readFixed64()
                    } else {
                        reader.skipField(wireType)
                    }
                }
                2 -> {
                    require(wireType == WIRETYPE_LENGTH_DELIMITED) {
                        "Expected length-delimited NodeProto, got wireType=$wireType"
                    }
                    nodes += parseNodeProto(reader.subReader())
                }
                else -> reader.skipField(wireType)
            }
        }
        return nodes
    }

    private fun parseNodeProto(reader: ProtoWireReader): MutableCpmNode {
        val node = MutableCpmNode()
        while (!reader.isAtEnd) {
            val tag = reader.readTag()
            val fieldNumber = tag ushr 3
            val wireType = tag and 0x7
            when (fieldNumber) {
                1 -> {
                    require(wireType == WIRETYPE_VARINT) {
                        "Expected varint NodeProto.id, got wireType=$wireType"
                    }
                    node.id = reader.readVarint64()
                }
                3 -> {
                    when (wireType) {
                        WIRETYPE_VARINT -> node.childIds += reader.readVarint64()
                        WIRETYPE_LENGTH_DELIMITED -> {
                            val packed = reader.subReader()
                            while (!packed.isAtEnd) {
                                node.childIds += packed.readVarint64()
                            }
                        }
                        else -> reader.skipField(wireType)
                    }
                }
                4 -> {
                    if (wireType == WIRETYPE_VARINT) {
                        node.parentId = reader.readVarint64()
                    } else {
                        reader.skipField(wireType)
                    }
                }
                5 -> {
                    require(wireType == WIRETYPE_LENGTH_DELIMITED) {
                        "Expected length-delimited local_state, got wireType=$wireType"
                    }
                    parseNodeLocalState(reader.subReader(), node)
                }
                6 -> {
                    require(wireType == WIRETYPE_LENGTH_DELIMITED) {
                        "Expected length-delimited computed_state, got wireType=$wireType"
                    }
                    parseNodeComputedState(reader.subReader(), node)
                }
                else -> reader.skipField(wireType)
            }
        }
        return node
    }

    private fun parseNodeLocalState(reader: ProtoWireReader, node: MutableCpmNode) {
        while (!reader.isAtEnd) {
            val tag = reader.readTag()
            val fieldNumber = tag ushr 3
            val wireType = tag and 0x7
            when (fieldNumber) {
                1 ->
                    if (wireType == WIRETYPE_LENGTH_DELIMITED)
                        node.requestedQuat = parseQuat(reader.subReader())
                    else reader.skipField(wireType)
                2 ->
                    if (wireType == WIRETYPE_LENGTH_DELIMITED)
                        node.requestedPos = parseVec3(reader.subReader())
                    else reader.skipField(wireType)
                3 ->
                    if (wireType == WIRETYPE_LENGTH_DELIMITED)
                        node.requestedScale = parseVec3(reader.subReader())
                    else reader.skipField(wireType)
                4 -> {
                    if (wireType == WIRETYPE_LENGTH_DELIMITED) {
                        val size = parseVec3(reader.subReader())
                        if (size.x > 0f && size.y > 0f) {
                            node.surfaceBoundsInPx = Vector2(size.x, size.y)
                        }
                    } else {
                        reader.skipField(wireType)
                    }
                }
                5 ->
                    if (wireType == WIRETYPE_VARINT) node.localVisible = reader.readVarint64() != 0L
                    else reader.skipField(wireType)
                6 ->
                    if (wireType == WIRETYPE_FIXED32) node.requestedAlpha = reader.readFloat()
                    else reader.skipField(wireType)
                9 ->
                    if (wireType == WIRETYPE_VARINT) node.inputId = reader.readVarint64().toInt()
                    else reader.skipField(wireType)
                12 -> {
                    if (wireType == WIRETYPE_LENGTH_DELIMITED) {
                        val dims = parseGraphicBuffer(reader.subReader())
                        if (dims != null) {
                            node.bufferSize = Vector2(dims.first.toFloat(), dims.second.toFloat())
                        }
                    } else {
                        reader.skipField(wireType)
                    }
                }
                17 -> {
                    if (wireType == WIRETYPE_LENGTH_DELIMITED) {
                        val name = reader.readString()
                        node.name = name
                        if (name.endsWith(SYSTEM_SIDE_SUBSPACE_SUFFIX)) {
                            node.hasSubspace = true
                            node.subspaceName = name.removeSuffix(SYSTEM_SIDE_SUBSPACE_SUFFIX)
                        }
                    } else {
                        reader.skipField(wireType)
                    }
                }
                18 ->
                    if (wireType == WIRETYPE_VARINT)
                        node.canReceiveInput = reader.readVarint64() != 0L
                    else reader.skipField(wireType)
                21 ->
                    if (wireType == WIRETYPE_VARINT) node.isSysUi = reader.readVarint64() != 0L
                    else reader.skipField(wireType)
                22 ->
                    if (wireType == WIRETYPE_VARINT) node.isAndroid2d = reader.readVarint64() != 0L
                    else reader.skipField(wireType)
                29 -> {
                    if (wireType == WIRETYPE_LENGTH_DELIMITED) {
                        node.touchableRegion = parseRegionBounds(reader.subReader())
                    } else {
                        reader.skipField(wireType)
                    }
                }
                47 -> {
                    if (wireType == WIRETYPE_LENGTH_DELIMITED) {
                        node.reformEnable = parseReformEnable(reader.subReader())
                    } else {
                        reader.skipField(wireType)
                    }
                }
                50 -> {
                    if (wireType == WIRETYPE_LENGTH_DELIMITED) {
                        node.coordinateSpace = parseTransformCoordinateSpace(reader.subReader())
                    } else {
                        reader.skipField(wireType)
                    }
                }
                51 -> {
                    if (wireType == WIRETYPE_LENGTH_DELIMITED) {
                        node.hasSubspace = true
                        node.subspaceId = parseRenderSubspaceId(reader.subReader())
                    } else {
                        reader.skipField(wireType)
                    }
                }
                52 -> {
                    if (wireType == WIRETYPE_LENGTH_DELIMITED) {
                        val dims = parseXrGraphicBuffer(reader.subReader())
                        if (dims != null && node.bufferSize == null) {
                            node.bufferSize = Vector2(dims.first.toFloat(), dims.second.toFloat())
                        }
                    } else {
                        reader.skipField(wireType)
                    }
                }
                54 -> {
                    if (wireType == WIRETYPE_LENGTH_DELIMITED) {
                        node.sysUiSpecialNode = parseSysUiMetadata(reader.subReader())
                    } else {
                        reader.skipField(wireType)
                    }
                }
                55 -> {
                    if (wireType == WIRETYPE_FIXED32) {
                        val ppm = reader.readFloat()
                        if (ppm > 0f) {
                            node.pixelsPerMeter = ppm
                        }
                    } else {
                        reader.skipField(wireType)
                    }
                }
                else -> reader.skipField(wireType)
            }
        }
    }

    private fun parseNodeComputedState(reader: ProtoWireReader, node: MutableCpmNode) {
        var visible = false
        var hasVisible = false

        while (!reader.isAtEnd) {
            val tag = reader.readTag()
            val fieldNumber = tag ushr 3
            val wireType = tag and 0x7
            when (fieldNumber) {
                1 ->
                    if (wireType == WIRETYPE_LENGTH_DELIMITED)
                        node.composedQuat = parseQuat(reader.subReader())
                    else reader.skipField(wireType)
                2 ->
                    if (wireType == WIRETYPE_LENGTH_DELIMITED)
                        node.computedPos = parseVec3(reader.subReader())
                    else reader.skipField(wireType)
                3 ->
                    if (wireType == WIRETYPE_LENGTH_DELIMITED)
                        node.computedScale = parseVec3(reader.subReader())
                    else reader.skipField(wireType)
                5 -> {
                    if (wireType == WIRETYPE_VARINT) {
                        visible = reader.readVarint64() != 0L
                        hasVisible = true
                    } else {
                        reader.skipField(wireType)
                    }
                }
                6 ->
                    if (wireType == WIRETYPE_FIXED32) node.computedAlpha = reader.readFloat()
                    else reader.skipField(wireType)
                8 ->
                    if (wireType == WIRETYPE_VARINT)
                        node.canReceiveInput = reader.readVarint64() != 0L
                    else reader.skipField(wireType)
                9 -> {
                    if (wireType == WIRETYPE_VARINT) {
                        if (reader.readVarint64() != 0L) {
                            node.isSysUi = true
                        }
                    } else {
                        reader.skipField(wireType)
                    }
                }
                17 -> {
                    if (wireType == WIRETYPE_LENGTH_DELIMITED) {
                        val sizeMeters = parseVec3(reader.subReader())
                        if (sizeMeters.x > 0f && sizeMeters.y > 0f) {
                            node.surfaceBoundsInMeters = Vector2(sizeMeters.x, sizeMeters.y)
                        }
                    } else {
                        reader.skipField(wireType)
                    }
                }
                18 -> {
                    if (wireType == WIRETYPE_FIXED32) {
                        val ppm = reader.readFloat()
                        if (ppm > 0f) {
                            node.computedPixelsPerMeter = ppm
                        }
                    } else {
                        reader.skipField(wireType)
                    }
                }
                else -> reader.skipField(wireType)
            }
        }

        if (hasVisible) {
            node.computedVisible = visible
        }
    }

    @VisibleForTesting
    internal fun parseImpressSceneDump(bytes: ByteArray): ImpressParsedData {
        val reader = ProtoWireReader(bytes)
        val transformsByCpmId = HashMap<Long, ImpressNodeTransform>()
        val subspaces = mutableListOf<ImpressSubspaceNode>()
        while (!reader.isAtEnd) {
            val tag = reader.readTag()
            val fieldNumber = tag ushr 3
            val wireType = tag and 0x7
            when (fieldNumber) {
                1 -> {
                    require(wireType == WIRETYPE_LENGTH_DELIMITED) {
                        "Expected length-delimited Impress NodeData, got wireType=$wireType"
                    }
                    parseImpressNodeData(
                        reader = reader.subReader(),
                        parentWorldPos = Vector3.Zero,
                        parentWorldRot = Quaternion.Identity,
                        parentWorldScale = Vector3.One,
                        parentActive = true,
                        transformsByCpmId = transformsByCpmId,
                        subspaces = subspaces,
                        activeSubspaceCollector = null,
                    )
                }
                else -> reader.skipField(wireType)
            }
        }
        return ImpressParsedData(transformsByCpmId, subspaces)
    }

    private class SubspaceCollector(val rootWorldRot: Quaternion) {
        val colliders = mutableListOf<ImpressColliderBounds>()
        val directChildLocalScales = mutableListOf<Vector3>()
        val childNodeNames = mutableListOf<String>()
    }

    private data class ImpressChildResult(val localScale: Vector3, val isActive: Boolean)

    private fun parseImpressNodeData(
        reader: ProtoWireReader,
        parentWorldPos: Vector3,
        parentWorldRot: Quaternion,
        parentWorldScale: Vector3,
        parentActive: Boolean,
        transformsByCpmId: MutableMap<Long, ImpressNodeTransform>,
        subspaces: MutableList<ImpressSubspaceNode>,
        activeSubspaceCollector: SubspaceCollector?,
    ): ImpressChildResult {
        var name = ""
        var disabled = false
        var localPos = Vector3.Zero
        var localScale = Vector3.One
        var localRot = Quaternion.Identity
        val components = mutableListOf<Pair<String, ByteArray>>()
        val childReaders = mutableListOf<ProtoWireReader>()

        while (!reader.isAtEnd) {
            val tag = reader.readTag()
            val fieldNumber = tag ushr 3
            val wireType = tag and 0x7
            when (fieldNumber) {
                1 ->
                    if (wireType == WIRETYPE_LENGTH_DELIMITED) name = reader.readString()
                    else reader.skipField(wireType)
                4 ->
                    if (wireType == WIRETYPE_VARINT) disabled = reader.readVarint64() != 0L
                    else reader.skipField(wireType)
                6 -> {
                    if (wireType == WIRETYPE_LENGTH_DELIMITED) {
                        val tx = parsePreciseTransform(reader.subReader())
                        localPos = tx.translation
                        localScale = tx.scale
                        localRot = tx.rotation
                    } else {
                        reader.skipField(wireType)
                    }
                }
                7 -> {
                    if (wireType == WIRETYPE_LENGTH_DELIMITED) {
                        parseProtoAny(reader.subReader())?.let { components += it }
                    } else {
                        reader.skipField(wireType)
                    }
                }
                8 -> {
                    if (wireType == WIRETYPE_LENGTH_DELIMITED) {
                        parseComponentWithMetadata(reader.subReader())?.let { components += it }
                    } else {
                        reader.skipField(wireType)
                    }
                }
                9 -> {
                    if (wireType == WIRETYPE_LENGTH_DELIMITED) {
                        childReaders += reader.subReader()
                    } else {
                        reader.skipField(wireType)
                    }
                }
                else -> reader.skipField(wireType)
            }
        }

        val isEnabled = !disabled
        val isActive = parentActive && isEnabled
        val worldPos = parentWorldPos + (parentWorldRot * localPos.scale(parentWorldScale))
        val worldRot = parentWorldRot * localRot
        val worldScale = parentWorldScale.scale(localScale)

        val subspaceName = extractSubspaceName(name)
        val ownSubspaceCollector =
            if (subspaceName != null) SubspaceCollector(rootWorldRot = worldRot) else null
        val targetCollector = ownSubspaceCollector ?: activeSubspaceCollector

        if (activeSubspaceCollector != null && name.isNotEmpty()) {
            activeSubspaceCollector.childNodeNames += name
        }

        for ((typeUrl, valueBytes) in components) {
            when (typeUrl) {
                TYPE_NODE_STATE_MANAGER -> {
                    val cpmNodeId = parseNodeStateManagerCpmId(ProtoWireReader(valueBytes))
                    if (cpmNodeId != null) {
                        transformsByCpmId[cpmNodeId] =
                            ImpressNodeTransform(
                                worldPose = Pose(worldPos, worldRot),
                                worldScale = worldScale,
                                isActive = isActive,
                                isEnabled = isEnabled,
                            )
                    }
                }
                TYPE_BOX_COLLIDER -> {
                    if (isActive && targetCollector != null) {
                        val subspaceRelativeRot = targetCollector.rootWorldRot.inverse * worldRot
                        parseBoxCollider(
                                ProtoWireReader(valueBytes),
                                worldScale,
                                subspaceRelativeRot,
                            )
                            ?.let { targetCollector.colliders += it }
                    }
                }
                TYPE_SPHERE_COLLIDER -> {
                    if (isActive && targetCollector != null) {
                        val subspaceRelativeRot = targetCollector.rootWorldRot.inverse * worldRot
                        parseSphereCollider(
                                ProtoWireReader(valueBytes),
                                worldScale,
                                subspaceRelativeRot,
                            )
                            ?.let { targetCollector.colliders += it }
                    }
                }
                TYPE_CYLINDER_COLLIDER -> {
                    if (isActive && targetCollector != null) {
                        val subspaceRelativeRot = targetCollector.rootWorldRot.inverse * worldRot
                        parseCylinderCollider(
                                ProtoWireReader(valueBytes),
                                worldScale,
                                subspaceRelativeRot,
                            )
                            ?.let { targetCollector.colliders += it }
                    }
                }
            }
        }

        for (childReader in childReaders) {
            val childResult =
                parseImpressNodeData(
                    reader = childReader,
                    parentWorldPos = worldPos,
                    parentWorldRot = worldRot,
                    parentWorldScale = worldScale,
                    parentActive = isActive,
                    transformsByCpmId = transformsByCpmId,
                    subspaces = subspaces,
                    activeSubspaceCollector = targetCollector,
                )
            if (ownSubspaceCollector != null && childResult.isActive) {
                ownSubspaceCollector.directChildLocalScales += childResult.localScale
            }
        }

        if (subspaceName != null && ownSubspaceCollector != null) {
            val fallbackId =
                SUBSPACE_ID_SUFFIX_REGEX.find(subspaceName)?.groupValues?.get(1)?.toIntOrNull()
                    ?: -1
            subspaces +=
                ImpressSubspaceNode(
                    subspaceId = fallbackId,
                    subspaceName = subspaceName,
                    nodeName = name,
                    worldPose = Pose(worldPos, worldRot),
                    worldScale = worldScale,
                    isDisabled = !isActive,
                    colliders = ownSubspaceCollector.colliders,
                    directChildLocalScales = ownSubspaceCollector.directChildLocalScales,
                    childNodeNames = ownSubspaceCollector.childNodeNames,
                )
        }

        return ImpressChildResult(localScale = localScale, isActive = isActive)
    }

    private fun extractSubspaceName(nodeName: String): String? =
        when {
            nodeName.endsWith(APP_SIDE_SUBSPACE_SUFFIX) ->
                nodeName.removeSuffix(APP_SIDE_SUBSPACE_SUFFIX)
            nodeName.endsWith(SYSTEM_SIDE_SUBSPACE_SUFFIX) ->
                nodeName.removeSuffix(SYSTEM_SIDE_SUBSPACE_SUFFIX)
            else -> null
        }

    private data class ParsedTransform(
        val translation: Vector3,
        val scale: Vector3,
        val rotation: Quaternion,
    )

    private fun parsePreciseTransform(reader: ProtoWireReader): ParsedTransform {
        var translation = Vector3.Zero
        var scale = Vector3.One
        var quat: Quaternion? = null
        var eulerDeg: Vector3? = null

        while (!reader.isAtEnd) {
            val tag = reader.readTag()
            val fieldNumber = tag ushr 3
            val wireType = tag and 0x7
            when (fieldNumber) {
                1 ->
                    if (wireType == WIRETYPE_LENGTH_DELIMITED)
                        translation = parseVec3(reader.subReader())
                    else reader.skipField(wireType)
                2 ->
                    if (wireType == WIRETYPE_LENGTH_DELIMITED) scale = parseVec3(reader.subReader())
                    else reader.skipField(wireType)
                3 ->
                    if (wireType == WIRETYPE_LENGTH_DELIMITED) quat = parseQuat(reader.subReader())
                    else reader.skipField(wireType)
                4 ->
                    if (wireType == WIRETYPE_LENGTH_DELIMITED)
                        eulerDeg = parseVec3(reader.subReader())
                    else reader.skipField(wireType)
                else -> reader.skipField(wireType)
            }
        }

        val resolvedRot =
            quat
                ?: eulerDeg?.let { angles ->
                    // Impress PreciseTransform euler_angles are YXZ in degrees.
                    // Vector3.Forward is (0, 0, -1), so pass explicit +Z axis (0, 0, 1).
                    Quaternion.fromAxisAngle(Vector3(0f, 1f, 0f), angles.y) *
                        Quaternion.fromAxisAngle(Vector3(1f, 0f, 0f), angles.x) *
                        Quaternion.fromAxisAngle(Vector3(0f, 0f, 1f), angles.z)
                }
                ?: Quaternion.Identity

        return ParsedTransform(translation = translation, scale = scale, rotation = resolvedRot)
    }

    private fun parseComponentWithMetadata(reader: ProtoWireReader): Pair<String, ByteArray>? {
        var disabled = false
        var protoAny: Pair<String, ByteArray>? = null
        while (!reader.isAtEnd) {
            val tag = reader.readTag()
            val fieldNumber = tag ushr 3
            val wireType = tag and 0x7
            when (fieldNumber) {
                1 ->
                    if (wireType == WIRETYPE_VARINT) disabled = reader.readVarint64() != 0L
                    else reader.skipField(wireType)
                2 ->
                    if (wireType == WIRETYPE_LENGTH_DELIMITED)
                        protoAny = parseProtoAny(reader.subReader())
                    else reader.skipField(wireType)
                else -> reader.skipField(wireType)
            }
        }
        return if (!disabled) protoAny else null
    }

    private fun parseProtoAny(reader: ProtoWireReader): Pair<String, ByteArray>? {
        var typeUrl = ""
        var valueBytes = ByteArray(0)
        while (!reader.isAtEnd) {
            val tag = reader.readTag()
            val fieldNumber = tag ushr 3
            val wireType = tag and 0x7
            when (fieldNumber) {
                1 ->
                    if (wireType == WIRETYPE_LENGTH_DELIMITED) typeUrl = reader.readString()
                    else reader.skipField(wireType)
                2 ->
                    if (wireType == WIRETYPE_LENGTH_DELIMITED) valueBytes = reader.readBytes()
                    else reader.skipField(wireType)
                else -> reader.skipField(wireType)
            }
        }
        return if (typeUrl.isNotEmpty()) typeUrl to valueBytes else null
    }

    private fun parseNodeStateManagerCpmId(reader: ProtoWireReader): Long? {
        var cpmId: Long? = null
        while (!reader.isAtEnd) {
            val tag = reader.readTag()
            val fieldNumber = tag ushr 3
            val wireType = tag and 0x7
            when (fieldNumber) {
                1 ->
                    if (wireType == WIRETYPE_VARINT) cpmId = reader.readVarint64()
                    else reader.skipField(wireType)
                else -> reader.skipField(wireType)
            }
        }
        return cpmId
    }

    private fun parseBoxCollider(
        reader: ProtoWireReader,
        worldScale: Vector3,
        subspaceRelativeRot: Quaternion,
    ): ImpressColliderBounds? {
        var center = Vector3.Zero
        var halfExtent = Vector3.Zero
        var hasBox = false
        while (!reader.isAtEnd) {
            val tag = reader.readTag()
            val fieldNumber = tag ushr 3
            val wireType = tag and 0x7
            when (fieldNumber) {
                1 -> {
                    if (wireType == WIRETYPE_LENGTH_DELIMITED) {
                        val boxReader = reader.subReader()
                        while (!boxReader.isAtEnd) {
                            val bTag = boxReader.readTag()
                            val bField = bTag ushr 3
                            val bWire = bTag and 0x7
                            when (bField) {
                                1 ->
                                    if (bWire == WIRETYPE_LENGTH_DELIMITED)
                                        center = parseVec3(boxReader.subReader())
                                    else boxReader.skipField(bWire)
                                2 -> {
                                    if (bWire == WIRETYPE_LENGTH_DELIMITED) {
                                        halfExtent = parseVec3(boxReader.subReader())
                                        hasBox = true
                                    } else {
                                        boxReader.skipField(bWire)
                                    }
                                }
                                else -> boxReader.skipField(bWire)
                            }
                        }
                    } else {
                        reader.skipField(wireType)
                    }
                }
                else -> reader.skipField(wireType)
            }
        }
        if (!hasBox) return null
        val hx = abs(halfExtent.x * worldScale.x)
        val hy = abs(halfExtent.y * worldScale.y)
        val hz = abs(halfExtent.z * worldScale.z)
        val rx = subspaceRelativeRot * Vector3(hx, 0f, 0f)
        val ry = subspaceRelativeRot * Vector3(0f, hy, 0f)
        val rz = subspaceRelativeRot * Vector3(0f, 0f, hz)
        return ImpressColliderBounds(
            type = "Box",
            centerInMeters = subspaceRelativeRot * center.scale(worldScale),
            extentsInMeters =
                Vector3(
                    (abs(rx.x) + abs(ry.x) + abs(rz.x)) * 2f,
                    (abs(rx.y) + abs(ry.y) + abs(rz.y)) * 2f,
                    (abs(rx.z) + abs(ry.z) + abs(rz.z)) * 2f,
                ),
        )
    }

    private fun parseSphereCollider(
        reader: ProtoWireReader,
        worldScale: Vector3,
        subspaceRelativeRot: Quaternion,
    ): ImpressColliderBounds? {
        var center = Vector3.Zero
        var radius = 0f
        while (!reader.isAtEnd) {
            val tag = reader.readTag()
            val fieldNumber = tag ushr 3
            val wireType = tag and 0x7
            when (fieldNumber) {
                1 ->
                    if (wireType == WIRETYPE_LENGTH_DELIMITED)
                        center = parseVec3(reader.subReader())
                    else reader.skipField(wireType)
                2 ->
                    if (wireType == WIRETYPE_FIXED32) radius = reader.readFloat()
                    else reader.skipField(wireType)
                else -> reader.skipField(wireType)
            }
        }
        if (radius <= 0f) return null
        val maxScale = maxOf(abs(worldScale.x), abs(worldScale.y), abs(worldScale.z))
        val scaledRadius = abs(radius * maxScale)
        val diameter = scaledRadius * 2f
        return ImpressColliderBounds(
            type = "Sphere",
            centerInMeters = subspaceRelativeRot * center.scale(worldScale),
            extentsInMeters = Vector3(diameter, diameter, diameter),
            radiusInMeters = scaledRadius,
        )
    }

    private fun parseCylinderCollider(
        reader: ProtoWireReader,
        worldScale: Vector3,
        subspaceRelativeRot: Quaternion,
    ): ImpressColliderBounds? {
        var base = Vector3.Zero
        var radius = 0f
        var height = 0f
        while (!reader.isAtEnd) {
            val tag = reader.readTag()
            val fieldNumber = tag ushr 3
            val wireType = tag and 0x7
            when (fieldNumber) {
                1 ->
                    if (wireType == WIRETYPE_LENGTH_DELIMITED) base = parseVec3(reader.subReader())
                    else reader.skipField(wireType)
                2 ->
                    if (wireType == WIRETYPE_FIXED32) radius = reader.readFloat()
                    else reader.skipField(wireType)
                3 ->
                    if (wireType == WIRETYPE_FIXED32) height = reader.readFloat()
                    else reader.skipField(wireType)
                else -> reader.skipField(wireType)
            }
        }
        if (radius <= 0f || height <= 0f) return null
        val scaledRadius = abs(radius * maxOf(abs(worldScale.x), abs(worldScale.z)))
        val scaledHeight = abs(height * abs(worldScale.y))
        val halfHeightAxis = subspaceRelativeRot * Vector3(0f, scaledHeight / 2f, 0f)
        val diskX = subspaceRelativeRot * Vector3(scaledRadius, 0f, 0f)
        val diskZ = subspaceRelativeRot * Vector3(0f, 0f, scaledRadius)
        val center = subspaceRelativeRot * (base + Vector3(0f, height / 2f, 0f)).scale(worldScale)
        return ImpressColliderBounds(
            type = "Cylinder",
            centerInMeters = center,
            extentsInMeters =
                Vector3(
                    (abs(halfHeightAxis.x) + hypot(diskX.x, diskZ.x)) * 2f,
                    (abs(halfHeightAxis.y) + hypot(diskX.y, diskZ.y)) * 2f,
                    (abs(halfHeightAxis.z) + hypot(diskX.z, diskZ.z)) * 2f,
                ),
            radiusInMeters = scaledRadius,
            heightInMeters = scaledHeight,
        )
    }

    private fun parseVec3(reader: ProtoWireReader): Vector3 {
        var x = 0f
        var y = 0f
        var z = 0f
        while (!reader.isAtEnd) {
            val tag = reader.readTag()
            val fieldNumber = tag ushr 3
            val wireType = tag and 0x7
            when (fieldNumber) {
                1 ->
                    if (wireType == WIRETYPE_FIXED32) x = reader.readFloat()
                    else reader.skipField(wireType)
                2 ->
                    if (wireType == WIRETYPE_FIXED32) y = reader.readFloat()
                    else reader.skipField(wireType)
                3 ->
                    if (wireType == WIRETYPE_FIXED32) z = reader.readFloat()
                    else reader.skipField(wireType)
                else -> reader.skipField(wireType)
            }
        }
        return Vector3(x, y, z)
    }

    private fun parseQuat(reader: ProtoWireReader): Quaternion {
        var x = 0f
        var y = 0f
        var z = 0f
        var w = 0f
        while (!reader.isAtEnd) {
            val tag = reader.readTag()
            val fieldNumber = tag ushr 3
            val wireType = tag and 0x7
            when (fieldNumber) {
                1 ->
                    if (wireType == WIRETYPE_FIXED32) x = reader.readFloat()
                    else reader.skipField(wireType)
                2 ->
                    if (wireType == WIRETYPE_FIXED32) y = reader.readFloat()
                    else reader.skipField(wireType)
                3 ->
                    if (wireType == WIRETYPE_FIXED32) z = reader.readFloat()
                    else reader.skipField(wireType)
                4 ->
                    if (wireType == WIRETYPE_FIXED32) w = reader.readFloat()
                    else reader.skipField(wireType)
                else -> reader.skipField(wireType)
            }
        }
        return Quaternion(x = x, y = y, z = z, w = w)
    }

    private fun parseGraphicBuffer(reader: ProtoWireReader): Pair<Int, Int>? {
        var width = 0
        var height = 0
        while (!reader.isAtEnd) {
            val tag = reader.readTag()
            val fieldNumber = tag ushr 3
            val wireType = tag and 0x7
            when (fieldNumber) {
                1 ->
                    if (wireType == WIRETYPE_VARINT) width = reader.readVarint64().toInt()
                    else reader.skipField(wireType)
                2 ->
                    if (wireType == WIRETYPE_VARINT) height = reader.readVarint64().toInt()
                    else reader.skipField(wireType)
                else -> reader.skipField(wireType)
            }
        }
        return if (width > 0 && height > 0) width to height else null
    }

    private fun parseXrGraphicBuffer(reader: ProtoWireReader): Pair<Int, Int>? {
        var graphicBufferDims: Pair<Int, Int>? = null
        var vulkanDims: Pair<Int, Int>? = null
        while (!reader.isAtEnd) {
            val tag = reader.readTag()
            val fieldNumber = tag ushr 3
            val wireType = tag and 0x7
            when (fieldNumber) {
                1 ->
                    if (wireType == WIRETYPE_LENGTH_DELIMITED)
                        graphicBufferDims = parseGraphicBuffer(reader.subReader())
                    else reader.skipField(wireType)
                2 ->
                    if (wireType == WIRETYPE_LENGTH_DELIMITED)
                        vulkanDims = parseGraphicBuffer(reader.subReader())
                    else reader.skipField(wireType)
                else -> reader.skipField(wireType)
            }
        }
        return graphicBufferDims ?: vulkanDims
    }

    private fun parseRegionBounds(reader: ProtoWireReader): Rect? {
        var minL = Int.MAX_VALUE
        var minT = Int.MAX_VALUE
        var maxR = Int.MIN_VALUE
        var maxB = Int.MIN_VALUE
        var hasRect = false

        while (!reader.isAtEnd) {
            val tag = reader.readTag()
            val fieldNumber = tag ushr 3
            val wireType = tag and 0x7
            when (fieldNumber) {
                2 -> {
                    if (wireType == WIRETYPE_LENGTH_DELIMITED) {
                        val rect = reader.subReader()
                        var l = 0
                        var t = 0
                        var r = 0
                        var b = 0
                        while (!rect.isAtEnd) {
                            val rTag = rect.readTag()
                            val rField = rTag ushr 3
                            val rWire = rTag and 0x7
                            when (rField) {
                                1 ->
                                    if (rWire == WIRETYPE_VARINT) l = rect.readVarint64().toInt()
                                    else rect.skipField(rWire)
                                2 ->
                                    if (rWire == WIRETYPE_VARINT) t = rect.readVarint64().toInt()
                                    else rect.skipField(rWire)
                                3 ->
                                    if (rWire == WIRETYPE_VARINT) r = rect.readVarint64().toInt()
                                    else rect.skipField(rWire)
                                4 ->
                                    if (rWire == WIRETYPE_VARINT) b = rect.readVarint64().toInt()
                                    else rect.skipField(rWire)
                                else -> rect.skipField(rWire)
                            }
                        }
                        if (r > l && b > t) {
                            hasRect = true
                            if (l < minL) minL = l
                            if (t < minT) minT = t
                            if (r > maxR) maxR = r
                            if (b > maxB) maxB = b
                        }
                    } else {
                        reader.skipField(wireType)
                    }
                }
                else -> reader.skipField(wireType)
            }
        }
        return if (hasRect) Rect(minL, minT, maxR, maxB) else null
    }

    private fun parseReformEnable(reader: ProtoWireReader): Int {
        var enable = 0
        while (!reader.isAtEnd) {
            val tag = reader.readTag()
            val fieldNumber = tag ushr 3
            val wireType = tag and 0x7
            when (fieldNumber) {
                1 ->
                    if (wireType == WIRETYPE_VARINT) enable = reader.readVarint64().toInt()
                    else reader.skipField(wireType)
                else -> reader.skipField(wireType)
            }
        }
        return enable
    }

    private fun parseTransformCoordinateSpace(reader: ProtoWireReader): Int? {
        var coordSpace: Int? = null
        while (!reader.isAtEnd) {
            val tag = reader.readTag()
            val fieldNumber = tag ushr 3
            val wireType = tag and 0x7
            when (fieldNumber) {
                3 ->
                    if (wireType == WIRETYPE_VARINT) coordSpace = reader.readVarint64().toInt()
                    else reader.skipField(wireType)
                else -> reader.skipField(wireType)
            }
        }
        return coordSpace
    }

    private fun parseRenderSubspaceId(reader: ProtoWireReader): Int {
        var subspaceId = -1
        while (!reader.isAtEnd) {
            val tag = reader.readTag()
            val fieldNumber = tag ushr 3
            val wireType = tag and 0x7
            when (fieldNumber) {
                2 ->
                    if (wireType == WIRETYPE_VARINT) subspaceId = reader.readVarint64().toInt()
                    else reader.skipField(wireType)
                else -> reader.skipField(wireType)
            }
        }
        return subspaceId
    }

    /**
     * Parses `perfetto.protos.SysUIMetadata`
     * (`external/perfetto/protos/perfetto/trace/android/cpm.proto:102-107`). Note: Neither
     * `cpm.proto` nor `frameworks/native/services/cpm/shared/NodeProtoHelper.cpp:316-326`
     * serializes `scrimConfig` (`targetAlpha`) into `CpmNodeTrace`, so only `special_node_type`
     * (field 1) is populated by the platform proto trace.
     */
    private fun parseSysUiMetadata(reader: ProtoWireReader): Int? {
        var specialNode: Int? = null
        while (!reader.isAtEnd) {
            val tag = reader.readTag()
            val fieldNumber = tag ushr 3
            val wireType = tag and 0x7
            when (fieldNumber) {
                1 ->
                    if (wireType == WIRETYPE_VARINT) specialNode = reader.readVarint64().toInt()
                    else reader.skipField(wireType)
                else -> reader.skipField(wireType)
            }
        }
        return specialNode
    }

    private class MutableCpmNode {
        var id: Long = -1L
        var parentId: Long = 0L
        val childIds: MutableList<Long> = mutableListOf()
        var name: String = ""
        var localVisible: Boolean = true
        var computedVisible: Boolean? = null
        var requestedPos: Vector3? = null
        var requestedQuat: Quaternion? = null
        var requestedScale: Vector3? = null
        var computedPos: Vector3? = null
        var composedQuat: Quaternion? = null
        var computedScale: Vector3? = null
        var position: Vector3 = Vector3.Zero
        var rotation: Quaternion = Quaternion.Identity
        var scale: Vector3 = Vector3.One
        var surfaceBoundsInMeters: Vector2? = null
        var surfaceBoundsInPx: Vector2? = null
        var touchableRegion: Rect? = null
        var bufferSize: Vector2? = null
        var pixelsPerMeter: Float? = null
        var computedPixelsPerMeter: Float? = null
        var inputId: Int = -1
        var canReceiveInput: Boolean = true
        var isAndroid2d: Boolean = false
        var isEmbeddedWindowLeash: Boolean = false
        var isSysUi: Boolean = false
        var hasSubspace: Boolean = false
        var subspaceName: String? = null
        var subspaceId: Int? = null
        var reformEnable: Int = 0
        var coordinateSpace: Int? = null
        var isActive: Boolean = true
        var sysUiSpecialNode: Int? = null
        var requestedAlpha: Float? = null
        var computedAlpha: Float? = null
        var parentNode: MutableCpmNode? = null
        val childNodes: MutableList<MutableCpmNode> = mutableListOf()
    }

    internal data class ImpressNodeTransform(
        val worldPose: Pose,
        val worldScale: Vector3,
        val isActive: Boolean,
        val isEnabled: Boolean,
    )

    internal data class ImpressParsedData(
        val nodeTransformsByCpmId: Map<Long, ImpressNodeTransform>,
        val subspaces: List<ImpressSubspaceNode>,
    )

    private const val WIRETYPE_VARINT = 0
    private const val WIRETYPE_FIXED64 = 1
    private const val WIRETYPE_LENGTH_DELIMITED = 2
    private const val WIRETYPE_FIXED32 = 5

    private const val SYSTEM_SIDE_SUBSPACE_SUFFIX = "_system_side_subspace_root"
    private const val APP_SIDE_SUBSPACE_SUFFIX = "_app_side_subspace_root"

    private const val TYPE_NODE_STATE_MANAGER =
        "type.googleapis.com/spaceflinger.NodeStateManagerState"
    private const val TYPE_BOX_COLLIDER = "type.googleapis.com/imp.BoxColliderState"
    private const val TYPE_SPHERE_COLLIDER = "type.googleapis.com/imp.SphereColliderState"
    private const val TYPE_CYLINDER_COLLIDER = "type.googleapis.com/imp.CylinderColliderState"

    private val TASK_WINDOW_NODE_REGEX = Regex("^task-\\d+-window$")
    private val SUBSPACE_ID_SUFFIX_REGEX = Regex("subspace_(-?\\d+)$")
}

internal class ProtoWireReader(
    private val bytes: ByteArray,
    private var pos: Int = 0,
    private val limit: Int = bytes.size,
) {
    val isAtEnd: Boolean
        get() = pos >= limit

    fun readTag(): Int {
        val raw64 = readVarint64()
        require(raw64 in 8L..0xFFFF_FFFFL && (raw64.toInt() ushr 3) > 0) {
            "Invalid protobuf tag: $raw64"
        }
        return raw64.toInt()
    }

    fun readVarint64(): Long {
        var shift = 0
        var result = 0L
        while (shift < 64) {
            if (pos >= limit) {
                throw IllegalArgumentException(
                    EOFException("Truncated varint at offset $pos").message
                )
            }
            val b = bytes[pos++].toInt() and 0xFF
            result = result or ((b.toLong() and 0x7FL) shl shift)
            if ((b and 0x80) == 0) {
                return result
            }
            shift += 7
        }
        throw IllegalArgumentException("Malformed varint exceeding 64 bits")
    }

    fun readFixed32(): Int {
        require(limit - pos >= 4) { "Truncated fixed32 at offset $pos" }
        val b0 = bytes[pos++].toInt() and 0xFF
        val b1 = bytes[pos++].toInt() and 0xFF
        val b2 = bytes[pos++].toInt() and 0xFF
        val b3 = bytes[pos++].toInt() and 0xFF
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    fun readFixed64(): Long {
        require(limit - pos >= 8) { "Truncated fixed64 at offset $pos" }
        var result = 0L
        for (i in 0 until 8) {
            result = result or ((bytes[pos++].toLong() and 0xFFL) shl (i * 8))
        }
        return result
    }

    fun readFloat(): Float = Float.fromBits(readFixed32())

    fun subReader(): ProtoWireReader {
        val rawLen = readVarint64()
        require(rawLen in 0L..(limit - pos).toLong()) {
            "Truncated length-delimited field len=$rawLen at offset $pos (limit=$limit)"
        }
        val len = rawLen.toInt()
        val sub = ProtoWireReader(bytes, pos, pos + len)
        pos += len
        return sub
    }

    fun readBytes(): ByteArray {
        val rawLen = readVarint64()
        require(rawLen in 0L..(limit - pos).toLong()) {
            "Truncated bytes field len=$rawLen at offset $pos (limit=$limit)"
        }
        val len = rawLen.toInt()
        val slice = bytes.copyOfRange(pos, pos + len)
        pos += len
        return slice
    }

    fun readString(): String = readBytes().decodeToString()

    fun skipField(wireType: Int) {
        when (wireType) {
            0 -> readVarint64()
            1 -> {
                require(limit - pos >= 8) { "Truncated fixed64 skip at $pos" }
                pos += 8
            }
            2 -> {
                val rawLen = readVarint64()
                require(rawLen in 0L..(limit - pos).toLong()) {
                    "Truncated length-delimited skip len=$rawLen at $pos"
                }
                pos += rawLen.toInt()
            }
            5 -> {
                require(limit - pos >= 4) { "Truncated fixed32 skip at $pos" }
                pos += 4
            }
            else -> throw IllegalArgumentException("Unsupported protobuf wireType=$wireType")
        }
    }
}
