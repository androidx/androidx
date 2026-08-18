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

package androidx.compose.remote.integration.view.demos.dsl

import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.creation.RemotePathBase
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

@Suppress("RestrictedApiAndroidX")
public fun String.toPathData(): RcPlatformServices.RcPathArrayCreator {
    return parsePath(this)
}

/**
 * A more complete SVG path parser that supports relative commands and elliptical arcs.
 *
 * @param pathData SVG path data string
 * @return Path data creator
 */
@Suppress("RestrictedApiAndroidX")
fun parsePath(pathData: String): RcPlatformServices.RcPathArrayCreator {
    val path = RemotePathBase()
    val pattern = Regex("([-+]?[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?)|([A-Za-z])")
    val tokens = pattern.findAll(pathData).map { it.value }.toList()

    var currentX = 0f
    var currentY = 0f
    var startX = 0f
    var startY = 0f
    var lastControlX = 0f
    var lastControlY = 0f
    var lastCmdChar = ' '
    var i = 0
    var cmdChar = ' '

    while (i < tokens.size) {
        val firstChar = tokens[i][0]
        if (firstChar.isLetter() && firstChar != 'e' && firstChar != 'E') {
            cmdChar = tokens[i][0]
            i++
        }

        val isRelative = cmdChar.isLowerCase()
        val cmd = cmdChar.uppercaseChar()

        when (cmd) {
            'M' -> {
                val x = tokens[i++].toFloat().let { if (isRelative) it + currentX else it }
                val y = tokens[i++].toFloat().let { if (isRelative) it + currentY else it }
                path.moveTo(x, y)
                currentX = x
                currentY = y
                startX = x
                startY = y
                lastControlX = x
                lastControlY = y
                cmdChar = if (isRelative) 'l' else 'L'
            }
            'L' -> {
                val x = tokens[i++].toFloat().let { if (isRelative) it + currentX else it }
                val y = tokens[i++].toFloat().let { if (isRelative) it + currentY else it }
                path.lineTo(x, y)
                currentX = x
                currentY = y
                lastControlX = x
                lastControlY = y
            }
            'H' -> {
                val x = tokens[i++].toFloat().let { if (isRelative) it + currentX else it }
                path.lineTo(x, currentY)
                currentX = x
                lastControlX = x
                lastControlY = currentY
            }
            'V' -> {
                val y = tokens[i++].toFloat().let { if (isRelative) it + currentY else it }
                path.lineTo(currentX, y)
                currentY = y
                lastControlX = currentX
                lastControlY = y
            }
            'C' -> {
                val x1 = tokens[i++].toFloat().let { if (isRelative) it + currentX else it }
                val y1 = tokens[i++].toFloat().let { if (isRelative) it + currentY else it }
                val x2 = tokens[i++].toFloat().let { if (isRelative) it + currentX else it }
                val y2 = tokens[i++].toFloat().let { if (isRelative) it + currentY else it }
                val x = tokens[i++].toFloat().let { if (isRelative) it + currentX else it }
                val y = tokens[i++].toFloat().let { if (isRelative) it + currentY else it }
                path.cubicTo(x1, y1, x2, y2, x, y)
                lastControlX = x2
                lastControlY = y2
                currentX = x
                currentY = y
            }
            'S' -> {
                val x2 = tokens[i++].toFloat().let { if (isRelative) it + currentX else it }
                val y2 = tokens[i++].toFloat().let { if (isRelative) it + currentY else it }
                val x = tokens[i++].toFloat().let { if (isRelative) it + currentX else it }
                val y = tokens[i++].toFloat().let { if (isRelative) it + currentY else it }
                val x1 =
                    if (lastCmdChar == 'C' || lastCmdChar == 'S') 2 * currentX - lastControlX
                    else currentX
                val y1 =
                    if (lastCmdChar == 'C' || lastCmdChar == 'S') 2 * currentY - lastControlY
                    else currentY
                path.cubicTo(x1, y1, x2, y2, x, y)
                lastControlX = x2
                lastControlY = y2
                currentX = x
                currentY = y
            }
            'Q' -> {
                val x1 = tokens[i++].toFloat().let { if (isRelative) it + currentX else it }
                val y1 = tokens[i++].toFloat().let { if (isRelative) it + currentY else it }
                val x = tokens[i++].toFloat().let { if (isRelative) it + currentX else it }
                val y = tokens[i++].toFloat().let { if (isRelative) it + currentY else it }
                path.quadTo(x1, y1, x, y)
                lastControlX = x1
                lastControlY = y1
                currentX = x
                currentY = y
            }
            'T' -> {
                val x = tokens[i++].toFloat().let { if (isRelative) it + currentX else it }
                val y = tokens[i++].toFloat().let { if (isRelative) it + currentY else it }
                val x1 =
                    if (lastCmdChar == 'Q' || lastCmdChar == 'T') 2 * currentX - lastControlX
                    else currentX
                val y1 =
                    if (lastCmdChar == 'Q' || lastCmdChar == 'T') 2 * currentY - lastControlY
                    else currentY
                path.quadTo(x1, y1, x, y)
                lastControlX = x1
                lastControlY = y1
                currentX = x
                currentY = y
            }
            'A' -> {
                val rx = abs(tokens[i++].toFloat())
                val ry = abs(tokens[i++].toFloat())
                val angle = tokens[i++].toFloat()
                val largeArc = tokens[i++].toFloat() != 0f
                val sweep = tokens[i++].toFloat() != 0f
                val x = tokens[i++].toFloat().let { if (isRelative) it + currentX else it }
                val y = tokens[i++].toFloat().let { if (isRelative) it + currentY else it }
                if (rx == 0f || ry == 0f) {
                    path.lineTo(x, y)
                } else {
                    path.arcTo(currentX, currentY, rx, ry, angle, largeArc, sweep, x, y)
                }
                currentX = x
                currentY = y
                lastControlX = x
                lastControlY = y
            }
            'Z' -> {
                path.close()
                currentX = startX
                currentY = startY
                lastControlX = startX
                lastControlY = startY
            }
        }
        lastCmdChar = cmd
    }
    return RcPlatformServices.RcPathArrayCreator { path.createFloatArray() }
}

@Suppress("RestrictedApiAndroidX")
public fun RemotePathBase.arcTo(
    x0: Float,
    y0: Float,
    rx: Float,
    ry: Float,
    angle: Float,
    largeArc: Boolean,
    sweep: Boolean,
    x1: Float,
    y1: Float,
) {
    if (rx == 0f || ry == 0f) {
        lineTo(x1, y1)
        return
    }
    val alpha = Math.toRadians(angle.toDouble())
    val cosAlpha = cos(alpha)
    val sinAlpha = sin(alpha)

    val dx = (x0 - x1) / 2.0
    val dy = (y0 - y1) / 2.0
    val x1p = cosAlpha * dx + sinAlpha * dy
    val y1p = -sinAlpha * dx + cosAlpha * dy

    var rxp = abs(rx).toDouble()
    var ryp = abs(ry).toDouble()
    val check = (x1p * x1p) / (rxp * rxp) + (y1p * y1p) / (ryp * ryp)
    if (check > 1.0) {
        val s = sqrt(check)
        rxp *= s
        ryp *= s
    }

    val sign = if (largeArc == sweep) -1.0 else 1.0
    val numerator = ((rxp * rxp * ryp * ryp) - (rxp * rxp * y1p * y1p) - (ryp * ryp * x1p * x1p))
    val denominator = (rxp * rxp * y1p * y1p) + (ryp * ryp * x1p * x1p)
    val root = sqrt(max(0.0, numerator / denominator))
    val cxp = sign * root * rxp * y1p / ryp
    val cyp = -sign * root * ryp * x1p / rxp

    val cx = cosAlpha * cxp - sinAlpha * cyp + (x0 + x1) / 2.0
    val cy = sinAlpha * cxp + cosAlpha * cyp + (y0 + y1) / 2.0

    val theta1 = atan2((y1p - cyp) / ryp, (x1p - cxp) / rxp)
    var dTheta = atan2((-y1p - cyp) / ryp, (-x1p - cxp) / rxp) - theta1

    if (sweep && dTheta < 0) {
        dTheta += 2 * Math.PI
    } else if (!sweep && dTheta > 0) {
        dTheta -= 2 * Math.PI
    }

    var segments = ceil(abs(dTheta) / (Math.PI / 2.0)).toInt()
    if (segments == 0) {
        segments = 1
    }

    for (i in 0..<segments) {
        val s1 = theta1 + i * dTheta / segments
        val s2 = theta1 + (i + 1) * dTheta / segments

        val t = 4.0 / 3.0 * tan((s2 - s1) / 4.0)

        val xstart = cosAlpha * rxp * cos(s1) - sinAlpha * ryp * sin(s1) + cx
        val ystart = sinAlpha * rxp * cos(s1) + cosAlpha * ryp * sin(s1) + cy

        val xend = cosAlpha * rxp * cos(s2) - sinAlpha * ryp * sin(s2) + cx
        val yend = sinAlpha * rxp * cos(s2) + cosAlpha * ryp * sin(s2) + cy

        val cp1x = xstart + t * (-cosAlpha * rxp * sin(s1) - sinAlpha * ryp * cos(s1))
        val cp1y = ystart + t * (-sinAlpha * rxp * sin(s1) + cosAlpha * ryp * cos(s1))

        val cp2x = xend - t * (-cosAlpha * rxp * sin(s2) - sinAlpha * ryp * cos(s2))
        val cp2y = yend - t * (-sinAlpha * rxp * sin(s2) + cosAlpha * ryp * cos(s2))

        cubicTo(
            cp1x.toFloat(),
            cp1y.toFloat(),
            cp2x.toFloat(),
            cp2y.toFloat(),
            xend.toFloat(),
            yend.toFloat(),
        )
    }
}
