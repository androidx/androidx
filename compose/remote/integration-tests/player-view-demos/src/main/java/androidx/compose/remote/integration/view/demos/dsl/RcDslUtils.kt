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
    this.arcTo(x0, y0, rx, ry, angle, largeArc, sweep, x1, y1)
}
