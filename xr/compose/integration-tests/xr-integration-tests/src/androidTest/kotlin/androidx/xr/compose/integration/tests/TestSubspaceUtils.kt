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

package androidx.xr.compose.integration.tests

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Looper
import android.view.Choreographer
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.SpaceRequestResult
import androidx.xr.compose.platform.requestFullSpace
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.ExperimentalSpatialGltfModelApi
import androidx.xr.compose.subspace.SpatialBoxScope
import androidx.xr.compose.subspace.SpatialGltfModelState
import androidx.xr.compose.subspace.SpatialGltfModelStatus
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SubspaceLayoutCoordinates
import androidx.xr.compose.subspace.node.SubspaceSemanticsInfo
import androidx.xr.compose.testing.SubspaceSemanticsMatcher
import androidx.xr.compose.testing.SubspaceSemanticsNodeInteraction
import androidx.xr.compose.testing.onAllSubspaceNodes
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.ImageBasedLightingAsset
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.SpatialCapability
import androidx.xr.scenecore.SpatialEnvironment
import androidx.xr.scenecore.SpatialEnvironment.SpatialEnvironmentPreference
import androidx.xr.scenecore.scene
import androidx.xr.testutils.SnapshotXrSpace
import androidx.xr.testutils.XrScreenshotTestRule
import androidx.xr.testutils.captureSceneToBitmap
import androidx.xr.testutils.crop
import androidx.xr.testutils.detectBackgroundColor
import androidx.xr.testutils.findForegroundBounds
import androidx.xr.testutils.toPixelArray
import androidx.xr.testutils.toSoftwareBitmap
import androidx.xr.testutils.waitForSettledSpatialTask
import java.nio.file.Paths
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.AssumptionViolatedException

/**
 * Golden image directory path for Compose for XR integration screenshot tests, used with
 * [XrScreenshotTestRule].
 */
internal const val GOLDEN_PATH = "xr/compose/integration-tests/xr-integration-tests"

/**
 * Studio lighting IBL asset relative to `assets/` providing even 360-degree lighting on glTF models
 * without adding distracting skybox scenery when used with `geometry = null`.
 */
internal const val STUDIO_LIGHTING_IBL_ASSET_PATH = "skyboxes/studio_lighting_ibl.zip"

private val SCENE_POLL_TIMEOUT = 10.seconds
private const val PASSTHROUGH_OPACITY_TOLERANCE = 0.01f

/**
 * Distance in meters in front of the head pose at which the Subspace root is assumed to be placed
 * when the CPM scene graph reports the Subspace root at the head position.
 */
private const val DEFAULT_SUBSPACE_DISTANCE_METERS = 1.75f

/**
 * Fallback virtual pixel density (pixels per meter) used only if `dumpsys spf_cpm` does not report
 * `computedPixelsPerMeter` on the task hierarchy.
 */
private const val DEFAULT_PIXELS_PER_METER = 1600f

/** Minimum squared offset (m^2) for trusting CPM's `SubspaceRoot` position over head pose. */
private const val MIN_ROOT_OFFSET_SQUARED_METERS = 0.01f

/** Near-plane depth clamp (m) to avoid division by zero or sign inversion during projection. */
private const val MIN_PROJECTION_DEPTH_METERS = 0.01f

/** Comparison epsilon for normalized camera-space bounds. */
private const val PROJECTION_EPSILON = 1e-4f

private val anySubspaceNode = SubspaceSemanticsMatcher("anySubspaceNode") { true }

/**
 * Transitions [AndroidComposeTestRule.activity] to Full Space Mode, configures a deterministic
 * spatial environment background (`preferredPassthroughOpacity = passthroughOpacity` and
 * `preferredSpatialEnvironment`), composes [content] inside a top-level [Subspace], and waits for
 * the spatial environment and passthrough preferences to settle.
 *
 * When [preferredEnvironment] is `null` and [defaultIblAssetPath] is non-null, loads the
 * even-lighting studio IBL asset ([STUDIO_LIGHTING_IBL_ASSET_PATH]) with `geometry = null` so GLTF
 * models are evenly illuminated without altering the black background void.
 *
 * The spatial environment preferences are restored when the content is disposed.
 *
 * Note: Including a top-level [Subspace] (without a `SpatialMainPanel`) automatically disables
 * `session.scene.mainPanelEntity`, so tests do not need to manually hide the 2D main panel.
 *
 * Skips the test if the device does not support Full Space Mode.
 */
internal fun AndroidComposeTestRule<*, out ComponentActivity>.setXrScreenshotContent(
    passthroughOpacity: Float = 0.0f,
    preferredEnvironment: SpatialEnvironmentPreference? = null,
    defaultIblAssetPath: String? = STUDIO_LIGHTING_IBL_ASSET_PATH,
    content: @Composable @SubspaceComposable SpatialBoxScope.() -> Unit,
) {
    require(passthroughOpacity in 0f..1f) {
        "passthroughOpacity must be in [0, 1], was $passthroughOpacity"
    }
    when (
        val spaceResult = runBlocking(Dispatchers.Main.immediate) { activity.requestFullSpace() }
    ) {
        SpaceRequestResult.Success -> Unit
        SpaceRequestResult.Unsupported ->
            throw AssumptionViolatedException("Full Space Mode is not supported on this device")
        is SpaceRequestResult.Error ->
            throw AssertionError("Failed to enter Full Space Mode", spaceResult.cause)
        else -> throw AssertionError("Unexpected SpaceRequestResult: $spaceResult")
    }
    var activeSession: Session? = null
    var environmentApplied = false
    setContent {
        val session = LocalSession.current
        val coroutineScope = rememberCoroutineScope()
        DisposableEffect(session, passthroughOpacity, preferredEnvironment, defaultIblAssetPath) {
            activeSession = session
            var ownedIblAsset: ImageBasedLightingAsset? = null
            var disposed = false
            val loadJob =
                if (session != null) {
                    val env = session.scene.spatialEnvironment
                    env.preferredPassthroughOpacity = passthroughOpacity
                    if (preferredEnvironment != null || defaultIblAssetPath == null) {
                        env.preferredSpatialEnvironment =
                            preferredEnvironment ?: SpatialEnvironmentPreference(null, null)
                        environmentApplied = true
                        null
                    } else {
                        coroutineScope.launch {
                            val loaded =
                                ImageBasedLightingAsset.createFromZip(
                                    session,
                                    Paths.get(defaultIblAssetPath),
                                )
                            if (disposed) {
                                loaded.close()
                                return@launch
                            }
                            ownedIblAsset = loaded
                            env.preferredSpatialEnvironment =
                                SpatialEnvironmentPreference(loaded, null)
                            environmentApplied = true
                        }
                    }
                } else {
                    null
                }
            onDispose {
                disposed = true
                loadJob?.cancel()
                activeSession = null
                environmentApplied = false
                if (session != null) {
                    runCatching {
                        val env = session.scene.spatialEnvironment
                        env.preferredPassthroughOpacity =
                            SpatialEnvironment.NO_PASSTHROUGH_OPACITY_PREFERENCE
                        env.preferredSpatialEnvironment = null
                    }
                    runCatching { ownedIblAsset?.close() }
                }
            }
        }
        Subspace(content = content)
    }
    waitUntil(
        conditionDescription =
            "Full Space Mode, hermetic environment active, and passthrough opacity settled",
        timeoutMillis = SCENE_POLL_TIMEOUT.inWholeMilliseconds,
    ) {
        runOnUiThread { environmentApplied && activeSession?.isEnvironmentSettled() == true }
    }
    waitForIdle()
}

/**
 * Waits for the spatial scene to settle and captures the 3D compositor output cropped to the root
 * [Subspace] hierarchy, for assertion with `androidx.test.screenshot.assertAgainstGolden`.
 *
 * Waits up to 10 seconds for the task and panels in the CPM scene graph to settle, then up to 5
 * seconds for consecutive compositor frames to stabilize.
 *
 * @param padding Extra padding in pixels to preserve around the cropped content bounds.
 * @return A cropped software-backed [Bitmap.Config.ARGB_8888] [Bitmap] owned by the caller.
 * @throws IllegalStateException if no [Subspace] content is found in the hierarchy.
 */
internal fun AndroidComposeTestRule<*, out ComponentActivity>.captureToBitmap(
    padding: Int = 0
): Bitmap {
    require(padding >= 0) { "padding must be non-negative, was $padding" }
    val initialRoot =
        checkNotNull(
            onAllSubspaceNodes(anySubspaceNode)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .firstOrNull()
                ?.let(::findSubspaceRoot)
        ) {
            "No Subspace content found"
        }
    val task = awaitSettledSnapshotXrSpace(initialRoot, activity = activity)
    val settledRoot =
        checkNotNull(
            onAllSubspaceNodes(anySubspaceNode)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .firstOrNull()
                ?.let(::findSubspaceRoot)
        ) {
            "No Subspace content found"
        }
    return captureAndCropToComposeNode(settledRoot, task, padding)
}

/**
 * Waits for the spatial scene to settle and captures the 3D compositor output cropped to this
 * Subspace node, for assertion with `androidx.test.screenshot.assertAgainstGolden`.
 *
 * Uses the node's bounding size ([SubspaceSemanticsInfo.size]) and pose
 * ([SubspaceSemanticsInfo.poseInRoot]) together with the CPM scene graph's virtual pixel density
 * ([SnapshotXrSpace.computedPixelsPerMeter]) to calculate the crop framing, supporting 3D
 * containers (such as `SpatialRow`, `SpatialColumn`, and `SpatialBox`), 3D models, and panels.
 *
 * @param padding Extra padding in pixels to preserve around the cropped content bounds.
 * @return A cropped software-backed [Bitmap.Config.ARGB_8888] [Bitmap] owned by the caller.
 * @throws AssertionError if this interaction does not match exactly one Subspace node.
 */
internal fun SubspaceSemanticsNodeInteraction.captureToBitmap(padding: Int = 0): Bitmap {
    require(padding >= 0) { "padding must be non-negative, was $padding" }
    val initialNode = fetchSemanticsNode("Failed to fetch Subspace node for captureToBitmap")
    val task = awaitSettledSnapshotXrSpace(findSubspaceRoot(initialNode))
    val settledNode = fetchSemanticsNode("Failed to fetch Subspace node for captureToBitmap")
    return captureAndCropToComposeNode(settledNode, task, padding)
}

/**
 * Waits for every provided [SpatialGltfModelState] to finish loading and asserts that none entered
 * [SpatialGltfModelStatus.Failed], failing immediately with the underlying exception on load error.
 * Also awaits a few compositor VSync frames so asynchronous Impress/SplitEngine skinning and node
 * or material updates flush before screenshot capture.
 */
@OptIn(ExperimentalSpatialGltfModelApi::class)
internal fun AndroidComposeTestRule<*, out ComponentActivity>.waitForGltfModelToLoad(
    modelState: SpatialGltfModelState,
    vararg additionalModelStates: SpatialGltfModelState,
    timeoutMillis: Long = SCENE_POLL_TIMEOUT.inWholeMilliseconds,
) {
    val modelStates = listOf(modelState, *additionalModelStates)
    waitUntil(
        conditionDescription = "All SpatialGltfModelState instances finish loading without errors",
        timeoutMillis = timeoutMillis,
    ) {
        modelStates.any { it.status is SpatialGltfModelStatus.Failed } ||
            modelStates.all { it.status !is SpatialGltfModelStatus.Loading }
    }
    assertSpatialGltfModelsLoaded(*modelStates.toTypedArray())
    waitForIdle()
    runBlocking(Dispatchers.Main) {
        val choreographer = Choreographer.getInstance()
        repeat(18) {
            suspendCancellableCoroutine<Unit> { continuation ->
                val callback = Choreographer.FrameCallback {
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }
                choreographer.postFrameCallback(callback)
                continuation.invokeOnCancellation { choreographer.removeFrameCallback(callback) }
            }
        }
    }
}

/**
 * Asserts synchronously that all provided [SpatialGltfModelState] instances have reached
 * [SpatialGltfModelStatus.Loaded] with no errors.
 */
@OptIn(ExperimentalSpatialGltfModelApi::class)
internal fun assertSpatialGltfModelsLoaded(vararg modelStates: SpatialGltfModelState) {
    for (modelState in modelStates) {
        when (val status = modelState.status) {
            is SpatialGltfModelStatus.Loaded -> Unit
            is SpatialGltfModelStatus.Failed ->
                throw AssertionError(
                    "SpatialGltfModel failed to load: ${status.exception.message ?: status.exception}",
                    status.exception,
                )
            SpatialGltfModelStatus.Loading ->
                throw AssertionError("SpatialGltfModel is still loading and not ready for capture")
            else -> throw AssertionError("Unexpected SpatialGltfModelStatus: $status")
        }
    }
}

/**
 * Waits for the active [SnapshotXrSpace] in the CPM scene graph to settle with all enabled Subspace
 * panels and the main panel at their expected visibility.
 */
private fun awaitSettledSnapshotXrSpace(
    subspaceRoot: SubspaceSemanticsInfo,
    activity: ComponentActivity? = null,
): SnapshotXrSpace {
    val treeNodes = collectNodes(subspaceRoot)
    val panelEntities = treeNodes.mapNotNull { it.semanticsEntity as? PanelEntity }.distinct()
    val (expectedEmbeddedPanelCount, expectedMainPanelVisible) =
        runOnMainThread {
            panelEntities.count { !it.isMainPanelEntity && it.isEnabled(includeParents = true) } to
                panelEntities.any { it.isMainPanelEntity && it.isEnabled(includeParents = true) }
        }
    return waitForSettledSpatialTask(
        activity = activity,
        expectedEmbeddedPanelCount = expectedEmbeddedPanelCount,
        expectedMainPanelVisible = expectedMainPanelVisible,
        requireFullSpace = true,
        timeout = SCENE_POLL_TIMEOUT,
    )
}

private fun <T> runOnMainThread(block: () -> T): T {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        return block()
    }
    var result: Result<T>? = null
    InstrumentationRegistry.getInstrumentation().runOnMainSync { result = runCatching(block) }
    return checkNotNull(result).getOrThrow()
}

/** Must be called on the main thread. */
private fun Session.isEnvironmentSettled(): Boolean {
    val capabilities = scene.spatialCapabilities
    val env = scene.spatialEnvironment
    if (!capabilities.contains(SpatialCapability.SPATIAL_UI)) return false
    if (
        capabilities.contains(SpatialCapability.APP_ENVIRONMENT) &&
            env.isPreferredSpatialEnvironmentActive != (env.preferredSpatialEnvironment != null)
    ) {
        return false
    }
    val targetOpacity = env.preferredPassthroughOpacity
    return !capabilities.contains(SpatialCapability.PASSTHROUGH_CONTROL) ||
        targetOpacity == SpatialEnvironment.NO_PASSTHROUGH_OPACITY_PREFERENCE ||
        abs(env.currentPassthroughOpacity - targetOpacity) <= PASSTHROUGH_OPACITY_TOLERANCE
}

private fun findSubspaceRoot(start: SubspaceSemanticsInfo): SubspaceSemanticsInfo {
    var curr: Any = start
    while (true) {
        val next =
            (curr as? SubspaceLayoutCoordinates)?.parentLayoutCoordinates
                ?: (curr as? SubspaceSemanticsInfo)?.parentInfo
                ?: break
        curr = next
    }
    return (curr as? SubspaceSemanticsInfo) ?: start
}

private fun collectNodes(node: SubspaceSemanticsInfo): List<SubspaceSemanticsInfo> = buildList {
    add(node)
    for (child in node.childrenInfo) {
        addAll(collectNodes(child))
    }
}

/** Must be called on the main thread. */
private fun isRenderedEntity(node: SubspaceSemanticsInfo): Boolean {
    val entity = node.semanticsEntity ?: return false
    return entity::class != Entity::class && entity.isEnabled(includeParents = true)
}

/**
 * Captures the full compositor frame and crops it to [semanticsInfo], recycling the full frame once
 * it is no longer needed.
 */
private fun captureAndCropToComposeNode(
    semanticsInfo: SubspaceSemanticsInfo,
    task: SnapshotXrSpace,
    padding: Int,
): Bitmap {
    val fullBitmap = captureSceneToBitmap(cropBounds = null, autoCrop = false)
    val cropped =
        try {
            cropToComposeNode(fullBitmap, semanticsInfo, task, padding)
        } catch (t: Throwable) {
            if (!fullBitmap.isRecycled) {
                fullBitmap.recycle()
            }
            throw t
        }
    if (cropped !== fullBitmap && !fullBitmap.isRecycled) {
        fullBitmap.recycle()
    }
    return cropped
}

/**
 * Crops [bitmap] using the 3D bounding size and pose information from [semanticsInfo] in Compose.
 *
 * Unlike CPM which only tracks 2D surface bounds for panel window leashes, Compose maintains
 * bounding volumes ([SubspaceSemanticsInfo.size]) and poses ([SubspaceSemanticsInfo.poseInRoot])
 * for all Subspace nodes, including layout containers (such as `SpatialRow`, `SpatialColumn`, and
 * `SpatialBox`) and 3D models.
 */
private fun cropToComposeNode(
    bitmap: Bitmap,
    semanticsInfo: SubspaceSemanticsInfo,
    task: SnapshotXrSpace,
    padding: Int,
): Bitmap {
    val softwareBitmap = toSoftwareBitmap(bitmap)
    val width = softwareBitmap.width
    val height = softwareBitmap.height
    val pixels = toPixelArray(softwareBitmap)
    val bgColor = detectBackgroundColor(pixels, width, height)
    val overallBounds =
        findForegroundBounds(pixels, width, height, Rect(0, 0, width, height), bgColor)
            ?: return softwareBitmap

    val subspaceRoot = findSubspaceRoot(semanticsInfo)
    val tightBounds =
        if (semanticsInfo === subspaceRoot) {
            overallBounds
        } else {
            resolveSubtreeCropBounds(
                semanticsInfo = semanticsInfo,
                subspaceRoot = subspaceRoot,
                task = task,
                overallBounds = overallBounds,
                pixels = pixels,
                bitmapWidth = width,
                bitmapHeight = height,
                bgColor = bgColor,
            )
        }

    return padAndCropSafely(bitmap, softwareBitmap, tightBounds, padding)
}

/**
 * Resolves the tight pixel crop rectangle for a non-root [semanticsInfo] subtree by comparing its
 * camera-space projection against all rendered nodes under [subspaceRoot].
 */
private fun resolveSubtreeCropBounds(
    semanticsInfo: SubspaceSemanticsInfo,
    subspaceRoot: SubspaceSemanticsInfo,
    task: SnapshotXrSpace,
    overallBounds: Rect,
    pixels: IntArray,
    bitmapWidth: Int,
    bitmapHeight: Int,
    bgColor: Int,
): Rect {
    val treeNodes = collectNodes(subspaceRoot)
    val allVisualNodes = treeNodes.distinct().filter { it.size.width > 0 && it.size.height > 0 }
    val renderedNodeIds = runOnMainThread {
        (allVisualNodes + semanticsInfo).filter(::isRenderedEntity).mapTo(mutableSetOf()) {
            it.semanticsId
        }
    }
    // Prefer rendered SceneCore entities; fall back to leaf nodes or all visual nodes when Compose
    // layout containers lack corresponding SceneCore entities.
    val allRenderedNodes =
        allVisualNodes
            .filter { it.semanticsId in renderedNodeIds }
            .ifEmpty {
                allVisualNodes.filter { it.childrenInfo.isEmpty() }.ifEmpty { allVisualNodes }
            }

    val projection = SubspaceProjection.fromSnapshot(task)
    val allProjected = allRenderedNodes.unionProjectedBounds(projection) ?: return overallBounds

    val targetSubtreeNodes = collectNodes(semanticsInfo)
    val targetSubtreeIds = targetSubtreeNodes.mapTo(mutableSetOf()) { it.semanticsId }
    val targetNodes =
        allRenderedNodes
            .filter { it.semanticsId in targetSubtreeIds }
            .ifEmpty {
                targetSubtreeNodes.filter {
                    it !== semanticsInfo && it.size.width > 0 && it.size.height > 0
                }
            }
    val targetProjected =
        targetNodes.unionProjectedBounds(projection)
            ?: projection.projectNode(semanticsInfo)
            ?: return overallBounds

    val projectedRect =
        mapProjectedBoundsToBitmapRect(
            allBounds = allProjected,
            targetBounds = targetProjected,
            overallBounds = overallBounds,
            bitmapWidth = bitmapWidth,
            bitmapHeight = bitmapHeight,
        )
    // Refine leaf rendered entities against actual rendered pixels within the projected estimate.
    return if (
        semanticsInfo.semanticsId in renderedNodeIds && semanticsInfo.childrenInfo.isEmpty()
    ) {
        findForegroundBounds(pixels, bitmapWidth, bitmapHeight, projectedRect, bgColor)
            ?: projectedRect
    } else {
        projectedRect
    }
}

/**
 * Expands [tightBounds] by [padding] pixels clamped to bitmap edges and crops [softwareBitmap].
 * [sourceBitmap] is retained solely for reference-identity comparison so a temporary converted
 * [softwareBitmap] can be recycled safely without recycling the caller's input bitmap.
 */
private fun padAndCropSafely(
    sourceBitmap: Bitmap,
    softwareBitmap: Bitmap,
    tightBounds: Rect,
    padding: Int,
): Bitmap {
    val safePadding = max(0, padding)
    val finalBounds =
        Rect(
            (tightBounds.left - safePadding).coerceAtLeast(0),
            (tightBounds.top - safePadding).coerceAtLeast(0),
            (tightBounds.right + safePadding).coerceAtMost(softwareBitmap.width),
            (tightBounds.bottom + safePadding).coerceAtMost(softwareBitmap.height),
        )
    val cropped =
        try {
            crop(softwareBitmap, finalBounds)
        } catch (t: Throwable) {
            if (softwareBitmap !== sourceBitmap && !softwareBitmap.isRecycled) {
                softwareBitmap.recycle()
            }
            throw t
        }
    if (
        cropped !== softwareBitmap && softwareBitmap !== sourceBitmap && !softwareBitmap.isRecycled
    ) {
        softwareBitmap.recycle()
    }
    return cropped
}

/** Perspective-divided 2D bounds in camera view space (`x / depth`, `y / depth`). */
internal data class ProjectedBounds(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float,
) {
    val width: Float
        get() = maxX - minX

    val height: Float
        get() = maxY - minY

    fun union(other: ProjectedBounds): ProjectedBounds =
        ProjectedBounds(
            minX = minOf(minX, other.minX),
            minY = minOf(minY, other.minY),
            maxX = maxOf(maxX, other.maxX),
            maxY = maxOf(maxY, other.maxY),
        )

    fun covers(other: ProjectedBounds, tolerance: Float = PROJECTION_EPSILON): Boolean =
        minX <= other.minX + tolerance &&
            minY <= other.minY + tolerance &&
            maxX >= other.maxX - tolerance &&
            maxY >= other.maxY - tolerance
}

/** Transforms Subspace-local 3D volumes into normalized camera-space [ProjectedBounds]. */
internal data class SubspaceProjection(
    val headPose: Pose,
    val rootPosition: Vector3,
    val rootRotation: Quaternion,
    val rootScale: Vector3,
    val pixelsPerMeter: Float,
) {
    private val viewRotInv: Quaternion = headPose.rotation.inverse

    fun projectNode(node: SubspaceSemanticsInfo): ProjectedBounds? =
        projectVolume(node.size.width, node.size.height, node.size.depth, node.poseInRoot)

    fun projectVolume(
        widthPx: Int,
        heightPx: Int,
        depthPx: Int,
        poseInRoot: Pose,
    ): ProjectedBounds? {
        if (widthPx <= 0 || heightPx <= 0) return null

        val halfW = ((widthPx.toFloat() / pixelsPerMeter) * rootScale.x) / 2f
        val halfH = ((heightPx.toFloat() / pixelsPerMeter) * rootScale.y) / 2f
        val halfD = ((depthPx.toFloat() / pixelsPerMeter) * rootScale.z).coerceAtLeast(0f) / 2f

        val scaledTranslation =
            Vector3(
                (poseInRoot.translation.x / pixelsPerMeter) * rootScale.x,
                (poseInRoot.translation.y / pixelsPerMeter) * rootScale.y,
                (poseInRoot.translation.z / pixelsPerMeter) * rootScale.z,
            )
        val worldPos = rootPosition + (rootRotation * scaledTranslation)
        val worldRot = rootRotation * poseInRoot.rotation

        return projectBoxCorners(halfW, halfH, halfD, worldPos, worldRot)
    }

    private fun projectBoxCorners(
        halfW: Float,
        halfH: Float,
        halfD: Float,
        worldPos: Vector3,
        worldRot: Quaternion,
    ): ProjectedBounds {
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        for (sx in CORNER_SIGNS) {
            for (sy in CORNER_SIGNS) {
                for (sz in CORNER_SIGNS) {
                    val localCorner = Vector3(sx * halfW, sy * halfH, sz * halfD)
                    val worldCorner = worldPos + (worldRot * localCorner)
                    val camCorner = viewRotInv * (worldCorner - headPose.translation)
                    val depth = (-camCorner.z).coerceAtLeast(MIN_PROJECTION_DEPTH_METERS)
                    val projX = camCorner.x / depth
                    val projY = camCorner.y / depth
                    minX = minOf(minX, projX)
                    minY = minOf(minY, projY)
                    maxX = maxOf(maxX, projX)
                    maxY = maxOf(maxY, projY)
                }
            }
        }
        return ProjectedBounds(minX, minY, maxX, maxY)
    }

    companion object {
        private val CORNER_SIGNS = floatArrayOf(-1f, 1f)

        fun fromSnapshot(task: SnapshotXrSpace): SubspaceProjection {
            val subspaceCpmNode = task.findNodeByName("SubspaceRoot") ?: task.spatialTransform
            val pixelsPerMeter =
                subspaceCpmNode?.computedPixelsPerMeter
                    ?: task.computedPixelsPerMeter
                    ?: DEFAULT_PIXELS_PER_METER
            val headPose = (subspaceCpmNode ?: task).headPose ?: Pose.Identity
            val rootRotation = subspaceCpmNode?.rotation ?: headPose.rotation
            val rootScale = subspaceCpmNode?.scale ?: Vector3.One
            // CPM sometimes reports SubspaceRoot at head origin; project forward in that case.
            val rootPosition =
                if (
                    subspaceCpmNode != null &&
                        (subspaceCpmNode.position - headPose.translation).lengthSquared >=
                            MIN_ROOT_OFFSET_SQUARED_METERS
                ) {
                    subspaceCpmNode.position
                } else {
                    headPose.translation +
                        (headPose.rotation * Vector3(0f, 0f, -DEFAULT_SUBSPACE_DISTANCE_METERS))
                }
            return SubspaceProjection(
                headPose = headPose,
                rootPosition = rootPosition,
                rootRotation = rootRotation,
                rootScale = rootScale,
                pixelsPerMeter = pixelsPerMeter,
            )
        }
    }
}

private fun List<SubspaceSemanticsInfo>.unionProjectedBounds(
    projection: SubspaceProjection
): ProjectedBounds? = mapNotNull(projection::projectNode).reduceOrNull(ProjectedBounds::union)

/**
 * Maps [targetBounds] relative to [allBounds] onto pixel coordinates inside [overallBounds],
 * flipping camera +Y-up to bitmap +Y-down and clamping to `[0, bitmapWidth] x [0, bitmapHeight]`.
 */
internal fun mapProjectedBoundsToBitmapRect(
    allBounds: ProjectedBounds,
    targetBounds: ProjectedBounds,
    overallBounds: Rect,
    bitmapWidth: Int,
    bitmapHeight: Int,
): Rect {
    if (
        targetBounds.covers(allBounds) ||
            allBounds.width <= PROJECTION_EPSILON ||
            allBounds.height <= PROJECTION_EPSILON
    ) {
        return overallBounds
    }
    val leftFrac = ((targetBounds.minX - allBounds.minX) / allBounds.width).coerceIn(0f, 1f)
    val rightFrac = ((targetBounds.maxX - allBounds.minX) / allBounds.width).coerceIn(leftFrac, 1f)
    val topFrac = ((allBounds.maxY - targetBounds.maxY) / allBounds.height).coerceIn(0f, 1f)
    val bottomFrac = ((allBounds.maxY - targetBounds.minY) / allBounds.height).coerceIn(topFrac, 1f)

    val w = overallBounds.width()
    val h = overallBounds.height()
    val leftPx = (overallBounds.left + floor(leftFrac * w).toInt()).coerceIn(0, bitmapWidth)
    val topPx = (overallBounds.top + floor(topFrac * h).toInt()).coerceIn(0, bitmapHeight)
    val rightPx = (overallBounds.left + ceil(rightFrac * w).toInt()).coerceIn(leftPx, bitmapWidth)
    val bottomPx = (overallBounds.top + ceil(bottomFrac * h).toInt()).coerceIn(topPx, bitmapHeight)
    return Rect(leftPx, topPx, rightPx, bottomPx)
}
