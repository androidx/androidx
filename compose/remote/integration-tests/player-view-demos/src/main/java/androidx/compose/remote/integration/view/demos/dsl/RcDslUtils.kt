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
import kotlin.math.*

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
                    arcTo(path, currentX, currentY, rx, ry, angle, largeArc, sweep, x, y)
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
private fun arcTo(
    path: RemotePathBase,
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
    val alpha = angle.toDouble() * PI / 180.0
    val cosAlpha = cos(alpha)
    val sinAlpha = sin(alpha)

    val dx = (x0 - x1).toDouble() / 2.0
    val dy = (y0 - y1).toDouble() / 2.0
    val x1_ = cosAlpha * dx + sinAlpha * dy
    val y1_ = -sinAlpha * dx + cosAlpha * dy

    var rx_ = abs(rx.toDouble())
    var ry_ = abs(ry.toDouble())
    val check = x1_ * x1_ / (rx_ * rx_) + y1_ * y1_ / (ry_ * ry_)
    if (check > 1.0) {
        val s = sqrt(check)
        rx_ *= s
        ry_ *= s
    }

    val sign = if (largeArc == sweep) -1.0 else 1.0
    val numerator = (rx_ * rx_ * ry_ * ry_) - (rx_ * rx_ * y1_ * y1_) - (ry_ * ry_ * x1_ * x1_)
    val denominator = (rx_ * rx_ * y1_ * y1_) + (ry_ * ry_ * x1_ * x1_)
    val root = sqrt(max(0.0, numerator / denominator))
    val cx_ = sign * root * rx_ * y1_ / ry_
    val cy_ = -sign * root * ry_ * x1_ / rx_

    val cx = cosAlpha * cx_ - sinAlpha * cy_ + (x0 + x1) / 2.0
    val cy = sinAlpha * cx_ + cosAlpha * cy_ + (y0 + y1) / 2.0

    val theta1 = atan2((y1_ - cy_) / ry_, (x1_ - cx_) / rx_)
    var dTheta = atan2((-y1_ - cy_) / ry_, (-x1_ - cx_) / rx_) - theta1

    if (sweep && dTheta < 0) dTheta += 2 * PI else if (!sweep && dTheta > 0) dTheta -= 2 * PI

    val segments = ceil(abs(dTheta) / (PI / 2.0)).toInt()
    for (i in 0 until segments) {
        val s1 = theta1 + i * dTheta / segments
        val s2 = theta1 + (i + 1) * dTheta / segments

        val t = 4.0 / 3.0 * tan((s2 - s1) / 4.0)

        val xstart = cosAlpha * rx_ * cos(s1) - sinAlpha * ry_ * sin(s1) + cx
        val ystart = sinAlpha * rx_ * cos(s1) + cosAlpha * ry_ * sin(s1) + cy

        val xend = cosAlpha * rx_ * cos(s2) - sinAlpha * ry_ * sin(s2) + cx
        val yend = sinAlpha * rx_ * cos(s2) + cosAlpha * ry_ * sin(s2) + cy

        val cp1x = xstart + t * (-cosAlpha * rx_ * sin(s1) - sinAlpha * ry_ * cos(s1))
        val cp1y = ystart + t * (-sinAlpha * rx_ * sin(s1) + cosAlpha * ry_ * cos(s1))

        val cp2x = xend - t * (-cosAlpha * rx_ * sin(s2) - sinAlpha * ry_ * cos(s2))
        val cp2y = yend - t * (-sinAlpha * rx_ * sin(s2) + cosAlpha * ry_ * cos(s2))

        path.cubicTo(
            cp1x.toFloat(),
            cp1y.toFloat(),
            cp2x.toFloat(),
            cp2y.toFloat(),
            xend.toFloat(),
            yend.toFloat(),
        )
    }
}
