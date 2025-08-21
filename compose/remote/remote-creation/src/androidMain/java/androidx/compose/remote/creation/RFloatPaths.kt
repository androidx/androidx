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
package androidx.compose.remote.creation

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

public fun RemotePath.moveTo(x: Number, y: Number) {
    moveTo(x.toFloat(), y.toFloat())
}

public fun RemotePath.cubicTo(
    x1: Number,
    y1: Number,
    x2: Number,
    y2: Number,
    x3: Number,
    y3: Number,
) {
    cubicTo(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), x3.toFloat(), y3.toFloat())
}

public fun RemotePath.lineTo(x: Number, y: Number) {
    lineTo(x.toFloat(), y.toFloat())
}

/**
 * This is used to create a dynamic circle. Such that the parameters can be NaN float id The number
 * of elements can be set to better integrate with path morphing functions.
 */
public fun createDynamicCircle(
    writer: RemoteComposeWriter,
    n: Int,
    radius: Float,
    cx: Float,
    cy: Float,
): RemotePath {
    val ret = RemotePath()
    val rad = RFloat(writer, radius)
    val controlPointDistance = rad * ((4.0 / 3.0) * tan(Math.PI / (2f * n))).toFloat()
    val centerX = RFloat(writer, cx)
    val centerY = RFloat(writer, cy)
    if (n < 3) {
        throw IllegalArgumentException("n must be greater than or equal to 3")
    }
    for (i in 0 until n) {
        // Calculate the start and end points of this segment
        val startAngle = ((i / n.toDouble()) * Math.PI * 2).toFloat()
        val endAngle = (((i + 1) / n.toDouble()) * Math.PI * 2).toFloat()

        // Start point
        val startX = centerX + (rad * cos(startAngle))
        val startY = centerY + (rad * sin(startAngle))

        // End point
        val endX = centerX + (rad * cos(endAngle))
        val endY = centerY + (rad * sin(endAngle))

        // Control points
        val control1X = centerX + rad * cos(startAngle) - controlPointDistance * sin(startAngle)
        val control1Y = centerY + rad * sin(startAngle) + controlPointDistance * cos(startAngle)

        val control2X = centerX + rad * cos(endAngle) + controlPointDistance * sin(endAngle)
        val control2Y = centerY + rad * sin(endAngle) - controlPointDistance * cos(endAngle)

        // If it's the first segment, move to the start point
        if (i == 0) {
            ret.moveTo(startX.toFloat(), startY.toFloat())
        }

        // Draw the cubic Bézier curve
        ret.cubicTo(
            control1X.toFloat(),
            control1Y.toFloat(),
            control2X.toFloat(),
            control2Y.toFloat(),
            endX.toFloat(),
            endY.toFloat(),
        )
    }
    ret.close()
    return ret
}

/**
 * Creates a Squircle path based on center coordinates, radius, and corner radius.
 *
 * @param rc The RemoteComposeWriter to use.
 * @param cx The x-coordinate of the squaricle's center.
 * @param cy The y-coordinate of the squaricle's center.
 * @param radius The radius of the squaricle (distance from center to side).
 * @param cornerRadius The radius of the corners.
 */
public fun createSquirclePath(
    rc: RemoteComposeWriter,
    cx: Float,
    cy: Float,
    radius: Float,
    cornerRadius: Float,
): RemotePath {
    return createSquirclePath(
        rc,
        RFloat(rc, cx),
        RFloat(rc, cy),
        RFloat(rc, radius),
        RFloat(rc, cornerRadius),
    )
}

/**
 * Creates a Squircle path based on center coordinates, radius, and corner radius.
 *
 * @param rc The RemoteComposeWriter to use.
 * @param cx The x-coordinate of the squaricle's center.
 * @param cy The y-coordinate of the squaricle's center.
 * @param radius The radius of the squaricle (distance from center to side).
 * @param cornerRadius The radius of the corners.
 */
public fun createSquirclePath(
    rc: RemoteComposeWriter,
    cx: RFloat,
    cy: RFloat,
    radius: RFloat,
    cornerRadius: RFloat,
): RemotePath {
    val squariclePath = RemotePath()

    // Calculate the half-width and half-height assuming a square bounding box
    val halfWidth = radius
    val halfHeight = radius

    squariclePath.moveTo(cx + halfWidth - cornerRadius, cy - halfHeight)

    val controlOffset = cornerRadius * 0.55228475f
    val left = cx - halfWidth
    val top = cy - halfHeight
    val right = cx + halfWidth
    val bottom = cy + halfHeight
    // flush the calculations
    left.toFloat()
    top.toFloat()
    right.toFloat()
    bottom.toFloat()

    squariclePath.cubicTo(
        right - cornerRadius + controlOffset,
        top,
        right,
        top + controlOffset,
        right,
        top + cornerRadius,
    )

    squariclePath.lineTo(cx + halfWidth, bottom - cornerRadius)

    squariclePath.cubicTo(
        right,
        bottom - cornerRadius + controlOffset,
        right - cornerRadius + controlOffset,
        bottom,
        right - cornerRadius,
        bottom,
    )

    squariclePath.lineTo(cx - halfWidth + cornerRadius, cy + halfHeight)

    squariclePath.cubicTo(
        left + cornerRadius - controlOffset,
        bottom,
        left,
        bottom - cornerRadius + controlOffset,
        left,
        bottom - cornerRadius,
    )

    // Left edge
    squariclePath.lineTo(cx - halfWidth, top + cornerRadius)

    // Top-left corner
    squariclePath.cubicTo(
        left,
        top + cornerRadius - controlOffset,
        left + cornerRadius - controlOffset,
        top,
        left + cornerRadius,
        top,
    )

    squariclePath.close()
    return squariclePath
}
