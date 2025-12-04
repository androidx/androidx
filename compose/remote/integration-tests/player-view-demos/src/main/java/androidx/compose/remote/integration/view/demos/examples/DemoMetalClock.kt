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

package androidx.compose.remote.integration.view.demos.examples

import android.graphics.Color
import android.graphics.Paint
import androidx.compose.remote.core.RemoteContext.FLOAT_CONTINUOUS_SEC
import androidx.compose.remote.core.operations.ConditionalOperations
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.Rc.FloatExpression.*
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.RemoteComposeWriterInterface
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.tooling.preview.RemoteDocPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class ExampleTimer {}

@Suppress("RestrictedApiAndroidX")
// Loop conditional fix version
fun fancyClock2(): RemoteComposeWriter {
    val rc = RemoteComposeWriterAndroid(500, 500, "sd", 6, 0, AndroidxRcPlatformServices())
    rc.root(
        RemoteComposeWriterInterface {
            rc.startBox(RecordingModifier().fillMaxSize(), BoxLayout.START, BoxLayout.START)
            rc.startCanvas(RecordingModifier().fillMaxSize())
            val w = rc.addComponentWidthValue()
            val h = rc.addComponentHeightValue()
            val cx = rc.floatExpression(w, 2f, DIV)
            val cy = rc.floatExpression(h, 2f, DIV)
            val rad = rc.floatExpression(w, h, MIN)
            val clipRad1 = rc.floatExpression(rad, 2f, DIV)
            val rad1 = rc.floatExpression(rad, 2f, DIV)
            val rad2 = rc.floatExpression(rad, 5f, DIV)

            val pat2: Int = genPath(rc, cx, cy, clipRad1, rad2)

            rc.addClipPath(pat2)
            rc.save()
            val a2 = rc.floatExpression(FLOAT_CONTINUOUS_SEC, 360f, MOD)
            rc.rotate(a2, cx, cy)
            rc.getPainter()
                .setSweepGradient(
                    cx,
                    cy,
                    intArrayOf(
                        -0xb19a78,
                        -0xe7d6b9,
                        -0xe7d6b9,
                        -0xe7d6b9,
                        -0xb19a78,
                        -0xe7d6b9,
                        -0xb19a78,
                    ),
                    null,
                )
                .commit()
            val secondAngle = rc.floatExpression(FLOAT_CONTINUOUS_SEC, 360f, MOD, 11f, MUL)
            rc.drawCircle(cx, cy, rad)
            rc.rotate(secondAngle, cx, cy)
            rc.getPainter()
                .setSweepGradient(
                    cx,
                    cy,
                    intArrayOf(
                        -0x7fb19a78,
                        -0x7fe7d6b9,
                        -0x7fe7d6b9,
                        -0x7fe7d6b9,
                        -0x7fb19a78,
                        -0x7fe7d6b9,
                        -0x7fb19a78,
                    ),
                    null,
                )
                .commit()
            rc.drawCircle(cx, cy, rad)
            rc.restore()
            rc.getPainter().setShader(0).commit()

            drawTicksCond(rc, cx, cy, rad1, rad2)

            drawClock(rc, cx, cy)

            rc.endCanvas()
            rc.endBox()
        }
    )
    return rc
}

/** Draws the clock's ticks */
@Suppress("RestrictedApiAndroidX")
fun drawTicksCond(
    rc: RemoteComposeWriterAndroid,
    centerX: Float,
    centerY: Float,
    rad1: Float,
    rad2: Float,
) {
    val second = rc.createFloatId()
    rc.getPainter().setColor(Color.LTGRAY).setTextSize(80f).commit()
    // float  n = 1 / ( 0.5 -   Log2( SQRT2 + (rad2/rad1) * (1 - SQRT2) )
    val SQRT2 = 1.4142135f
    val n =
        rc.floatExpression(
            1f,
            0.5f,
            SQRT2,
            rad2,
            rad1,
            DIV,
            1f,
            SQRT2,
            SUB,
            MUL,
            ADD,
            LN,
            2f,
            LN,
            DIV,
            SUB,
            DIV,
        )
    val n_1 = rc.floatExpression(1f, n, DIV)
    val pi = Math.PI.toFloat()
    rc.loop(
        Utils.idFromNan(second),
        0f,
        1f,
        60f,
        RemoteComposeWriterInterface {
            val ang = rc.floatExpression(second, 2 * pi / 60, MUL, pi / 2, SUB)
            val angDeg = rc.floatExpression(second, 6f, MUL)
            val cosAng = rc.floatExpression(ang, COS)
            val sinAng = rc.floatExpression(ang, SIN)
            val cos4 = rc.floatExpression(cosAng, ABS, n, POW)
            val sin4 = rc.floatExpression(sinAng, ABS, n, POW)

            val polarRadius = rc.floatExpression(rad1, cos4, sin4, ADD, ABS, n_1, POW, DIV)
            val offsetX = rc.floatExpression(polarRadius, cosAng, MUL, centerX, ADD)
            val offsetY = rc.floatExpression(polarRadius, sinAng, MUL, centerY, ADD)
            rc.save()
            rc.rotate(angDeg, offsetX, offsetY)
            val posY = rc.floatExpression(offsetY, 6f, ADD)
            rc.scale(0.5f, 2f, offsetX, offsetY)
            rc.drawCircle(offsetX, posY, 6f)

            rc.conditionalOperations(
                ConditionalOperations.TYPE_EQ,
                0f,
                rc.floatExpression(second, 5f, MOD),
            )
            run {
                rc.getPainter().setColor(Color.WHITE).commit()
                rc.drawCircle(offsetX, posY, 10f)
                rc.getPainter().setColor(Color.LTGRAY).commit()
            }
            rc.endConditionalOperations()

            rc.restore()
            rc.conditionalOperations(
                ConditionalOperations.TYPE_EQ,
                0f,
                rc.floatExpression(second, 15f, MOD),
            )
            run {
                val inset = 70f
                val txtOffsetX =
                    rc.floatExpression(polarRadius, inset, SUB, cosAng, MUL, centerX, ADD)
                val txtOffsetY =
                    rc.floatExpression(polarRadius, inset, SUB, sinAng, MUL, centerY, ADD)
                val hr =
                    rc.floatExpression(second, 15f, DIV, 3f, ADD, 4f, MOD, 1f, ADD, 3f, MUL, ROUND)
                val tid = rc.createTextFromFloat(hr, 2, 0, 0)
                rc.drawTextAnchored(tid, txtOffsetX, txtOffsetY, 0f, 0f, 0)
            }
            rc.endConditionalOperations()
        },
    )
}

@Suppress("RestrictedApiAndroidX")
fun drawClock(rc: RemoteComposeWriterAndroid, centerX: Float, centerY: Float) {
    val secondAngle = rc.floatExpression(FLOAT_CONTINUOUS_SEC, 60f, MOD, 6f, MUL)
    val minAngle = rc.floatExpression(Rc.Time.TIME_IN_MIN, 6f, MUL)
    val hrAngle = rc.floatExpression(Rc.Time.TIME_IN_HR, 30f, MUL)
    val hourHandLength = rc.floatExpression(centerY, centerX, centerY, MIN, 0.3f, MUL, SUB)
    val minHandLength = rc.floatExpression(centerY, centerX, centerY, MIN, 0.7f, MUL, SUB)
    val hourWidth = 12f
    val handWidth = 6f
    // Hour
    rc.save()
    rc.getPainter()
        .setColor(Color.GRAY)
        .setStrokeWidth(hourWidth)
        .setStrokeCap(Paint.Cap.ROUND)
        .commit()
    rc.drawCircle(centerX, centerY, hourWidth)
    rc.rotate(hrAngle, centerX, centerY)
    rc.drawLine(centerX, centerY, centerX, hourHandLength)
    rc.restore()

    // min
    rc.save()
    rc.getPainter().setColor(Color.WHITE).setStrokeWidth(handWidth).commit()
    rc.drawCircle(centerX, centerY, handWidth)
    rc.rotate(minAngle, centerX, centerY)
    rc.drawLine(centerX, centerY, centerX, minHandLength)
    rc.restore()
    // Center
    //        rc.getPainter().setColor(Color.BLACK).setStyle(Paint.Style.FILL).commit();
    //        rc.drawCircle(centerX,centerY,8);
    rc.getPainter().setColor(Color.WHITE).commit()
    rc.drawCircle(centerX, centerY, 2f)
    // sec
    rc.save()
    rc.rotate(secondAngle, centerX, centerY)
    rc.getPainter().setColor(Color.RED).setStrokeWidth(4f).commit()
    rc.drawLine(centerX, centerY, centerX, minHandLength)
    rc.restore()
}

/** This generates a squircle */
@Suppress("RestrictedApiAndroidX")
fun genPath(
    rc: RemoteComposeWriter,
    centerX: Float,
    centerY: Float,
    rad1: Float,
    rad2: Float,
): Int {
    val second = rc.createFloatId()

    // float  n = 1 / ( 0.5 -   Log2( SQRT2 + (rad2/rad1) * (1 - SQRT2) )
    val SQRT2 = 1.4142135623730951f
    val n =
        rc.floatExpression(
            1f,
            0.5f,
            SQRT2,
            rad2,
            rad1,
            DIV,
            1f,
            SQRT2,
            SUB,
            MUL,
            ADD,
            LN,
            2f,
            LN,
            DIV,
            SUB,
            DIV,
        )

    val n_1 = rc.floatExpression(1f, n, DIV)
    val pi = Math.PI.toFloat()
    val pid = rc.pathCreate(centerX, rc.floatExpression(centerY, rad1, SUB))
    rc.loop(
        Utils.idFromNan(second),
        0f,
        0.2f,
        60f,
        RemoteComposeWriterInterface {
            val ang = rc.floatExpression(second, 2 * pi / 60, MUL, pi / 2, SUB)
            val angDeg = rc.floatExpression(second, 6f, MUL)
            val cosAng = rc.floatExpression(ang, COS)
            val sinAng = rc.floatExpression(ang, SIN)
            val cos4 = rc.floatExpression(cosAng, ABS, n, POW)
            val sin4 = rc.floatExpression(sinAng, ABS, n, POW)

            val polarRadius = rc.floatExpression(rad1, cos4, sin4, ADD, ABS, n_1, POW, DIV)
            val offsetX = rc.floatExpression(polarRadius, cosAng, MUL, centerX, ADD)
            val offsetY = rc.floatExpression(polarRadius, sinAng, MUL, centerY, ADD)
            rc.pathAppendLineTo(pid, offsetX, offsetY)
        },
    )

    rc.pathAppendClose(pid)
    return pid
}

@Preview @Composable fun FancyClock2Preview() = RemoteDocPreview(fancyClock2())
