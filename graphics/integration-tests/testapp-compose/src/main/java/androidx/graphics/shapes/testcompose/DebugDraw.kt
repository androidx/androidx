/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.graphics.shapes.testcompose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.graphics.shapes.Cubic

internal fun Cubic.pointOnCurve(t: Float): Offset {
    val u = 1 - t
    return Offset(anchor0X * (u * u * u) + control0X * (3 * t * u * u) +
        control1X * (3 * t * t * u) + anchor1X * (t * t * t),
    anchor0Y * (u * u * u) + control0Y * (3 * t * u * u) +
        control1Y * (3 * t * t * u) + anchor1Y * (t * t * t)
    )
}

internal fun DrawScope.debugDraw(bezier: Cubic) {
    // Draw red circles for start and end.
    drawCircle(Color.Red, radius = 6f, center = bezier.anchor0(), style = Stroke(2f))
    drawCircle(Color.Magenta, radius = 8f, center = bezier.anchor1(), style = Stroke(2f))

    // Draw a circle for the first control point, and a line from start to it.
    // The curve will start in this direction
    drawLine(Color.Yellow, bezier.anchor0(), bezier.control0(), strokeWidth = 0f)
    drawCircle(Color.Yellow, radius = 4f, center = bezier.control0(), style = Stroke(2f))

    // Draw a circle for the second control point, and a line from it to the end.
    // The curve will end in this direction
    drawLine(Color.Yellow, bezier.control1(), bezier.anchor1(), strokeWidth = 0f)
    drawCircle(Color.Yellow, radius = 4f, center = bezier.control1(), style = Stroke(2f))

    // Draw dots along each curve
    var t = .1f
    while (t < 1f) {
        drawCircle(Color.White, radius = 2f, center = bezier.pointOnCurve(t), style = Stroke(2f))
        t += .1f
    }
}

private fun Cubic.anchor0() = Offset(anchor0X, anchor0Y)
private fun Cubic.control0() = Offset(control0X, control0Y)
private fun Cubic.control1() = Offset(control1X, control1Y)
private fun Cubic.anchor1() = Offset(anchor1X, anchor1Y)
