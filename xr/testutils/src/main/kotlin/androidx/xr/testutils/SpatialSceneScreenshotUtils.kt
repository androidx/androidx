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
import android.graphics.RectF
import android.os.SystemClock
import android.util.Log
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.TimeSource

private const val SCENE_CAPTURE_TAG = "SpatialSceneHelper"
internal const val MIN_SCREENSHOT_SCENE_SETTLE_WAIT_MS = 5000L
private const val FRAME_STABILITY_MSSIM_THRESHOLD: Double = 0.999

/**
 * Recursively traverses [root] to find all visible, non-system-UI [SpatialNode] entities that have
 * defined spatial geometry (either [SpatialNode.worldExtents] or
 * [SpatialNode.effectiveSurfaceBoundsInMeters]).
 *
 * @param root The root [SpatialNode] to search.
 * @return A list of geometry-bearing descendants (and/or the root itself).
 */
internal fun collectGeometryNodes(root: SpatialNode): List<SpatialNode> = root.findNodes { node ->
    node.isVisible &&
        !node.isSysUi &&
        (node.worldExtents != null || node.effectiveSurfaceBoundsInMeters != null)
}

/**
 * Computes the effective 3D dimensions of this node in world coordinates (meters).
 *
 * If [SpatialNode.worldExtents] is explicitly defined, it is returned directly. Otherwise, if
 * [SpatialNode.effectiveSurfaceBoundsInMeters] is available, scales those 2D surface dimensions by
 * [SpatialNode.scale] along the X and Y axes, applying a default minimum thickness (0.01m) along Z.
 * Returns `null` if neither metric is present.
 *
 * @return A [Vector3] representing (width, height, depth) in meters, or `null`.
 */
internal fun SpatialNode.effectiveWorldExtents(): Vector3? {
    worldExtents?.let {
        return it
    }
    val bounds = effectiveSurfaceBoundsInMeters ?: return null
    return Vector3(
        bounds.x * scale.x,
        bounds.y * scale.y,
        if (scale.z > 0f) scale.z else 0.01f,
    )
}

/**
 * Computes the relative 2D bounding rectangle fraction `[0..1]` of [node] projected onto the camera
 * view relative to its enclosing task/activity space root.
 *
 * Used during screenshot testing to compute auto-crop regions when isolating individual spatial
 * nodes or subtrees.
 *
 * @param node The [SpatialNode] whose projected bounds fraction should be computed.
 * @return A [RectF] with `[left, top, right, bottom]` fractions in `[0f..1f]` relative to the
 *   enclosing root's projected extents. Returns `(0f, 0f, 1f, 1f)` if full bounds apply.
 */
internal fun computeRelativeNodeBoundsFraction(node: SpatialNode): RectF {
    if (node.isTaskRoot || node.name.startsWith("space-transform-spatial-")) {
        return RectF(0f, 0f, 1f, 1f)
    }
    val contextRoot = node.activitySpaceRoot ?: node.root
    if (contextRoot === node) {
        return RectF(0f, 0f, 1f, 1f)
    }

    val targetNodes = collectGeometryNodes(node).ifEmpty { listOf(node) }
    val allNodes = collectGeometryNodes(contextRoot)
    if (allNodes.isEmpty() || allNodes.size <= targetNodes.size) {
        return RectF(0f, 0f, 1f, 1f)
    }

    val head = node.headPose ?: Pose.Identity
    val viewRotInv = head.rotation.inverse

    fun computeBoundsInHead(nodes: List<SpatialNode>): FloatArray? {
        var minX = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var count = 0
        for (n in nodes) {
            val ext = n.effectiveWorldExtents() ?: continue
            val center = viewRotInv * (n.position - head.translation)
            val depth = (-center.z).coerceAtLeast(0.01f)
            val projX = center.x / depth
            val projY = center.y / depth

            val localRot = viewRotInv * n.rotation
            val rx = localRot * Vector3(ext.x / 2f, 0f, 0f)
            val ry = localRot * Vector3(0f, ext.y / 2f, 0f)
            val halfW = (abs(rx.x) + abs(ry.x)) / depth
            val halfH = (abs(rx.y) + abs(ry.y)) / depth

            if (projX - halfW < minX) minX = projX - halfW
            if (projX + halfW > maxX) maxX = projX + halfW
            if (projY - halfH < minY) minY = projY - halfH
            if (projY + halfH > maxY) maxY = projY + halfH
            count++
        }
        return if (count > 0) floatArrayOf(minX, minY, maxX, maxY) else null
    }

    val allBounds = computeBoundsInHead(allNodes) ?: return RectF(0f, 0f, 1f, 1f)
    val targetBounds = computeBoundsInHead(targetNodes) ?: return RectF(0f, 0f, 1f, 1f)

    val totalW = allBounds[2] - allBounds[0]
    val totalH = allBounds[3] - allBounds[1]
    if (totalW <= 1e-4f || totalH <= 1e-4f) {
        return RectF(0f, 0f, 1f, 1f)
    }

    val leftFrac = ((targetBounds[0] - allBounds[0]) / totalW).coerceIn(0f, 1f)
    val rightFrac = ((targetBounds[2] - allBounds[0]) / totalW).coerceIn(0f, 1f)
    val topFrac = ((allBounds[3] - targetBounds[3]) / totalH).coerceIn(0f, 1f)
    val bottomFrac = ((allBounds[3] - targetBounds[1]) / totalH).coerceIn(0f, 1f)

    if (rightFrac - leftFrac < 1e-3f || bottomFrac - topFrac < 1e-3f) {
        return RectF(0f, 0f, 1f, 1f)
    }
    return RectF(leftFrac, topFrac, rightFrac, bottomFrac)
}

private fun logUnsettledTimeout(timeout: Duration) {
    Log.w(
        SCENE_CAPTURE_TAG,
        "Compositor frames did not stabilize within $timeout; returning the latest frame. " +
            "The screenshot may contain in-flight animation or rendering.",
    )
}

/**
 * Polls [UiAutomation.takeScreenshot] until two consecutive compositor frames match within
 * [FRAME_STABILITY_MSSIM_THRESHOLD] (ensuring newly registered SpaceFlinger/Impress buffers have
 * finished latching), or returns the latest non-null frame (logging a warning) if the timeout
 * elapses. All intermediate bitmaps are recycled before returning.
 */
internal fun waitForSettledSoftwareScreenshot(
    uiAutomation: UiAutomation,
    timeout: Duration,
    pollInterval: Duration,
): Bitmap {
    val start = TimeSource.Monotonic.markNow()
    val stabilityMatcher = MSSIMMatcher(threshold = FRAME_STABILITY_MSSIM_THRESHOLD)
    var previousBitmap: Bitmap? = null
    var previousPixels: IntArray? = null
    var currentPixels: IntArray? = null
    var currentBitmap: Bitmap? = null

    try {
        while (true) {
            val raw = uiAutomation.takeScreenshot()
            if (raw != null) {
                val softwareBitmap =
                    try {
                        toSoftwareBitmap(raw)
                    } finally {
                        if (isHardwareBitmap(raw) && !raw.isRecycled) {
                            raw.recycle()
                        }
                    }
                currentBitmap = softwareBitmap
                currentPixels = toPixelArray(softwareBitmap, currentPixels)
                val prev = previousBitmap
                val prevPix = previousPixels

                if (
                    prev != null &&
                        prevPix != null &&
                        prev.width == softwareBitmap.width &&
                        prev.height == softwareBitmap.height
                ) {
                    val matchResult =
                        stabilityMatcher.compareBitmaps(
                            expected = prevPix,
                            given = currentPixels,
                            width = softwareBitmap.width,
                            height = softwareBitmap.height,
                        )
                    prev.recycle()
                    previousBitmap = null
                    matchResult.diff?.recycle()
                    if (matchResult.matches) {
                        currentBitmap = null
                        return softwareBitmap
                    }
                    if (start.elapsedNow() >= timeout) {
                        logUnsettledTimeout(timeout)
                        currentBitmap = null
                        return softwareBitmap
                    }
                } else {
                    prev?.recycle()
                    previousBitmap = null
                    if (start.elapsedNow() >= timeout) {
                        logUnsettledTimeout(timeout)
                        currentBitmap = null
                        return softwareBitmap
                    }
                }

                previousBitmap = softwareBitmap
                val temp = previousPixels
                previousPixels = currentPixels
                currentPixels = temp
                currentBitmap = null
            } else if (start.elapsedNow() >= timeout) {
                previousBitmap?.let {
                    logUnsettledTimeout(timeout)
                    previousBitmap = null
                    return it
                }
                throw AssertionError(
                    "Timed out after $timeout waiting for UiAutomation.takeScreenshot() to " +
                        "return a non-null Bitmap"
                )
            }
            SystemClock.sleep(pollInterval.inWholeMilliseconds)
        }
    } catch (t: Throwable) {
        currentBitmap?.let {
            if (!it.isRecycled) it.recycle()
        }
        previousBitmap?.let {
            if (!it.isRecycled) it.recycle()
        }
        throw t
    }
}
