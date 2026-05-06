/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.capture

import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.abs
import androidx.compose.remote.creation.compose.state.atan2
import androidx.compose.remote.creation.compose.state.ceil
import androidx.compose.remote.creation.compose.state.cos
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.sin
import androidx.compose.remote.creation.compose.state.sqrt
import androidx.compose.remote.creation.compose.state.tan
import androidx.compose.remote.creation.compose.state.toRad
import androidx.compose.remote.creation.compose.vector.RemotePathExtensions
import androidx.compose.remote.creation.compose.vector.RemotePathNode
import androidx.compose.remote.creation.compose.vector.RemotePathNode.ArcTo
import androidx.compose.remote.creation.compose.vector.RemotePathNode.Close
import androidx.compose.remote.creation.compose.vector.RemotePathNode.CurveTo
import androidx.compose.remote.creation.compose.vector.RemotePathNode.HorizontalTo
import androidx.compose.remote.creation.compose.vector.RemotePathNode.LineTo
import androidx.compose.remote.creation.compose.vector.RemotePathNode.MoveTo
import androidx.compose.remote.creation.compose.vector.RemotePathNode.QuadTo
import androidx.compose.remote.creation.compose.vector.RemotePathNode.ReflectiveCurveTo
import androidx.compose.remote.creation.compose.vector.RemotePathNode.ReflectiveQuadTo
import androidx.compose.remote.creation.compose.vector.RemotePathNode.RelativeArcTo
import androidx.compose.remote.creation.compose.vector.RemotePathNode.RelativeCurveTo
import androidx.compose.remote.creation.compose.vector.RemotePathNode.RelativeHorizontalTo
import androidx.compose.remote.creation.compose.vector.RemotePathNode.RelativeLineTo
import androidx.compose.remote.creation.compose.vector.RemotePathNode.RelativeMoveTo
import androidx.compose.remote.creation.compose.vector.RemotePathNode.RelativeQuadTo
import androidx.compose.remote.creation.compose.vector.RemotePathNode.RelativeReflectiveCurveTo
import androidx.compose.remote.creation.compose.vector.RemotePathNode.RelativeReflectiveQuadTo
import androidx.compose.remote.creation.compose.vector.RemotePathNode.RelativeVerticalTo
import androidx.compose.remote.creation.compose.vector.RemotePathNode.VerticalTo
import androidx.compose.ui.util.fastForEach
import kotlin.math.PI

internal fun List<RemotePathNode>.toRemotePath(
    target: RemotePath = RemotePath(),
    creationState: RemoteStateScope,
): RemotePath {
    // Rewind unsets the fill type so reset it here
    target.rewind()

    var currentX = 0.0f.rf
    var currentY = 0.0f.rf
    var ctrlX = 0.0f.rf
    var ctrlY = 0.0f.rf
    var segmentX = 0.0f.rf
    var segmentY = 0.0f.rf
    var reflectiveCtrlX: RemoteFloat
    var reflectiveCtrlY: RemoteFloat

    var previousNode = if (isEmpty()) Close else this[0]
    with(RemotePathExtensions(creationState)) {
        fastForEach { node ->
            when (node) {
                is Close -> {
                    currentX = segmentX
                    currentY = segmentY
                    ctrlX = segmentX
                    ctrlY = segmentY
                    target.close()
                }

                is RelativeMoveTo -> {
                    currentX += node.dx
                    currentY += node.dy
                    target.rMoveTo(node.dx, node.dy)
                    segmentX = currentX
                    segmentY = currentY
                }

                is MoveTo -> {
                    currentX = node.x
                    currentY = node.y
                    target.moveTo(node.x, node.y)
                    segmentX = currentX
                    segmentY = currentY
                }

                is RelativeLineTo -> {
                    target.rLineTo(node.dx, node.dy)
                    currentX += node.dx
                    currentY += node.dy
                }

                is LineTo -> {
                    target.lineTo(node.x, node.y)
                    currentX = node.x
                    currentY = node.y
                }

                is RelativeHorizontalTo -> {
                    target.rLineTo(node.dx, 0.0f.rf)
                    currentX += node.dx
                }

                is HorizontalTo -> {
                    target.lineTo(node.x, currentY)
                    currentX = node.x
                }

                is RelativeVerticalTo -> {
                    target.rLineTo(0.0f.rf, node.dy)
                    currentY += node.dy
                }

                is VerticalTo -> {
                    target.lineTo(currentX, node.y)
                    currentY = node.y
                }

                is RelativeCurveTo -> {
                    target.rCubicTo(node.dx1, node.dy1, node.dx2, node.dy2, node.dx3, node.dy3)
                    ctrlX = currentX + node.dx2
                    ctrlY = currentY + node.dy2
                    currentX += node.dx3
                    currentY += node.dy3
                }

                is CurveTo -> {
                    target.cubicTo(node.x1, node.y1, node.x2, node.y2, node.x3, node.y3)
                    ctrlX = node.x2
                    ctrlY = node.y2
                    currentX = node.x3
                    currentY = node.y3
                }

                is RelativeReflectiveCurveTo -> {
                    if (previousNode.isCurve) {
                        reflectiveCtrlX = currentX - ctrlX
                        reflectiveCtrlY = currentY - ctrlY
                    } else {
                        reflectiveCtrlX = 0.0f.rf
                        reflectiveCtrlY = 0.0f.rf
                    }
                    target.rCubicTo(
                        reflectiveCtrlX,
                        reflectiveCtrlY,
                        node.dx1,
                        node.dy1,
                        node.dx2,
                        node.dy2,
                    )
                    ctrlX = currentX + node.dx1
                    ctrlY = currentY + node.dy1
                    currentX += node.dx2
                    currentY += node.dy2
                }

                is ReflectiveCurveTo -> {
                    if (previousNode.isCurve) {
                        reflectiveCtrlX = 2.rf * currentX - ctrlX
                        reflectiveCtrlY = 2.rf * currentY - ctrlY
                    } else {
                        reflectiveCtrlX = currentX
                        reflectiveCtrlY = currentY
                    }
                    target.cubicTo(
                        reflectiveCtrlX,
                        reflectiveCtrlY,
                        node.x1,
                        node.y1,
                        node.x2,
                        node.y2,
                    )
                    ctrlX = node.x1
                    ctrlY = node.y1
                    currentX = node.x2
                    currentY = node.y2
                }

                is RelativeQuadTo -> {
                    target.rQuadTo(node.dx1, node.dy1, node.dx2, node.dy2)
                    ctrlX = currentX + node.dx1
                    ctrlY = currentY + node.dy1
                    currentX += node.dx2
                    currentY += node.dy2
                }

                is QuadTo -> {
                    target.quadTo(node.x1, node.y1, node.x2, node.y2)
                    ctrlX = node.x1
                    ctrlY = node.y1
                    currentX = node.x2
                    currentY = node.y2
                }

                is RelativeReflectiveQuadTo -> {
                    if (previousNode.isQuad) {
                        reflectiveCtrlX = currentX - ctrlX
                        reflectiveCtrlY = currentY - ctrlY
                    } else {
                        reflectiveCtrlX = 0.0f.rf
                        reflectiveCtrlY = 0.0f.rf
                    }
                    target.rQuadTo(reflectiveCtrlX, reflectiveCtrlY, node.dx, node.dy)
                    ctrlX = currentX + reflectiveCtrlX
                    ctrlY = currentY + reflectiveCtrlY
                    currentX += node.dx
                    currentY += node.dy
                }

                is ReflectiveQuadTo -> {
                    if (previousNode.isQuad) {
                        reflectiveCtrlX = 2.rf * currentX - ctrlX
                        reflectiveCtrlY = 2.rf * currentY - ctrlY
                    } else {
                        reflectiveCtrlX = currentX
                        reflectiveCtrlY = currentY
                    }
                    target.quadTo(reflectiveCtrlX, reflectiveCtrlY, node.x, node.y)
                    ctrlX = reflectiveCtrlX
                    ctrlY = reflectiveCtrlY
                    currentX = node.x
                    currentY = node.y
                }

                is RelativeArcTo -> {
                    // TODO: Uses RemotePath.arcTo
                    val arcStartX = node.arcStartDx + currentX
                    val arcStartY = node.arcStartDy + currentY
                    drawArc(
                        target,
                        currentX,
                        currentY,
                        arcStartX,
                        arcStartY,
                        node.horizontalEllipseRadius,
                        node.verticalEllipseRadius,
                        node.theta,
                        node.isMoreThanHalf,
                        node.isPositiveArc,
                        creationState,
                    )
                    currentX = arcStartX
                    currentY = arcStartY
                    ctrlX = currentX
                    ctrlY = currentY
                }

                is ArcTo -> {
                    // TODO: Uses RemotePath.arcTo
                    drawArc(
                        target,
                        currentX,
                        currentY,
                        node.arcStartX,
                        node.arcStartY,
                        node.horizontalEllipseRadius,
                        node.verticalEllipseRadius,
                        node.theta,
                        node.isMoreThanHalf,
                        node.isPositiveArc,
                        creationState,
                    )
                    currentX = node.arcStartX
                    currentY = node.arcStartY
                    ctrlX = currentX
                    ctrlY = currentY
                }
            }
            previousNode = node
        }
    }
    return target
}

private fun drawArc(
    p: RemotePath,
    x0: RemoteFloat,
    y0: RemoteFloat,
    x1: RemoteFloat,
    y1: RemoteFloat,
    a: RemoteFloat,
    b: RemoteFloat,
    theta: RemoteFloat,
    isMoreThanHalf: RemoteBoolean,
    isPositiveArc: RemoteBoolean,
    remoteStateScope: RemoteStateScope,
) {
    /* Convert rotation angle from degrees to radians */
    val thetaD: RemoteFloat = toRad(theta)
    /* Pre-compute rotation matrix entries */
    val cosTheta = cos(thetaD)
    val sinTheta = sin(thetaD)
    /* Transform (x0, y0) and (x1, y1) into unit space */
    /* using (inverse) rotation, followed by (inverse) scale */
    val x0p = (x0 * cosTheta + y0 * sinTheta) / a
    val y0p = (-x0 * sinTheta + y0 * cosTheta) / b
    val x1p = (x1 * cosTheta + y1 * sinTheta) / a
    val y1p = (-x1 * sinTheta + y1 * cosTheta) / b

    /* Compute differences and averages */
    val dx = x0p - x1p
    val dy = y0p - y1p
    val xm = (x0p + x1p) / 2.rf
    val ym = (y0p + y1p) / 2.rf
    /* Solve for intersecting unit circles */
    val dsq = dx * dx + dy * dy
    if (dsq.constantValueOrNull == 0.0f) {
        return /* Points are coincident */
    }
    val disc = 1.rf / dsq - 1.rf / 4.rf
    val s = sqrt(disc)
    val sdx = s * dx
    val sdy = s * dy
    val branch = isMoreThanHalf.eq(isPositiveArc)
    var cx: RemoteFloat = branch.select(xm - sdy, xm + sdy)
    var cy: RemoteFloat = branch.select(ym + sdx, ym - sdx)

    val eta0 = atan2(y0p - cy, x0p - cx)

    val eta1 = atan2(y1p - cy, x1p - cx)

    val initialSweep = eta1 - eta0
    val branch2 = isPositiveArc.ne(initialSweep.ge(0.rf))
    val pi2 = 2.rf * PI.toFloat().rf
    val sweep = initialSweep + branch2.select(initialSweep.gt(0.rf).select(-pi2, pi2), 0.rf)

    cx *= a
    cy *= b
    val tcx = cx
    cx = cx * cosTheta - cy * sinTheta
    cy = tcx * sinTheta + cy * cosTheta

    arcToBezier(p, cx, cy, a, b, x0, y0, thetaD, eta0, sweep, remoteStateScope)
}

/**
 * Converts an arc to cubic Bezier segments and records them in p.
 *
 * @param p The target for the cubic Bezier segments
 * @param cx The x coordinate center of the ellipse
 * @param cy The y coordinate center of the ellipse
 * @param a The radius of the ellipse in the horizontal direction
 * @param b The radius of the ellipse in the vertical direction
 * @param e1x E(eta1) x coordinate of the starting point of the arc
 * @param e1y E(eta2) y coordinate of the starting point of the arc
 * @param theta The angle that the ellipse bounding rectangle makes with horizontal plane
 * @param start The start angle of the arc on the ellipse
 * @param sweep The angle (positive or negative) of the sweep of the arc on the ellipse
 */
private fun arcToBezier(
    p: RemotePath,
    cx: RemoteFloat,
    cy: RemoteFloat,
    a: RemoteFloat,
    b: RemoteFloat,
    e1x: RemoteFloat,
    e1y: RemoteFloat,
    theta: RemoteFloat,
    start: RemoteFloat,
    sweep: RemoteFloat,
    remoteStateScope: RemoteStateScope,
) {
    var eta1x = e1x
    var eta1y = e1y
    // Taken from equations at: http://spaceroots.org/documents/ellipse/node8.html
    // and http://www.spaceroots.org/documents/ellipse/node22.html

    // Maximum of 45 degrees per cubic Bezier segment
    val numSegments =
        ceil(abs(sweep * 4.rf / PI.toFloat().rf))
            .toRemoteInt()
            // Temporary limitation that this must be a constant
            .constantValue

    var eta1 = start
    val cosTheta = cos(theta)
    val sinTheta = sin(theta)
    val cosEta1 = cos(eta1)
    val sinEta1 = sin(eta1)
    var ep1x = (-a * cosTheta * sinEta1) - (b * sinTheta * cosEta1)
    var ep1y = (-a * sinTheta * sinEta1) + (b * cosTheta * cosEta1)

    val anglePerSegment = sweep / numSegments.rf
    for (i in 0 until numSegments) {
        val eta2 = eta1 + anglePerSegment
        val sinEta2 = sin(eta2)
        val cosEta2 = cos(eta2)
        val e2x = cx + (a * cosTheta * cosEta2) - (b * sinTheta * sinEta2)
        val e2y = cy + (a * sinTheta * cosEta2) + (b * cosTheta * sinEta2)
        val ep2x = (-a * cosTheta * sinEta2) - (b * sinTheta * cosEta2)
        val ep2y = (-a * sinTheta * sinEta2) + (b * cosTheta * cosEta2)
        val tanDiff2 = tan((eta2 - eta1) / 2.rf)
        val alpha = sin(eta2 - eta1) * (sqrt(4.rf + 3.rf * tanDiff2 * tanDiff2) - 1.rf) / 3.rf
        val q1x = eta1x + alpha * ep1x
        val q1y = eta1y + alpha * ep1y
        val q2x = e2x - alpha * ep2x
        val q2y = e2y - alpha * ep2y

        // TODO (njawad) figure out if this is still necessary?
        // Adding this no-op call to workaround a proguard related issue.
        // p.relativeLineTo(0.0, 0.0)

        with(RemotePathExtensions(remoteStateScope)) { p.cubicTo(q1x, q1y, q2x, q2y, e2x, e2y) }
        eta1 = eta2
        eta1x = e2x
        eta1y = e2y
        ep1x = ep2x
        ep1y = ep2y
    }
}
