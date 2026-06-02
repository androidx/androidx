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

package androidx.compose.remote.integration.view.demos.examples

import android.annotation.SuppressLint
import android.graphics.Paint
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.ConditionalOperations
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.TouchExpression
import androidx.compose.remote.core.operations.utilities.MatrixOperations
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.min
import androidx.compose.remote.creation.rf
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview

/** Use touchpad swipes and TouchExpression kinetic easing to rotate a 3d cube natively */
@SuppressLint("PrimitiveInCollection")
@Suppress("RestrictedApiAndroidX")
fun cubeInteractive(): RemoteComposeContext {

    val vertices =
        arrayOf(
            floatArrayOf(-1f, -1f, -1f),
            floatArrayOf(1f, -1f, -1f),
            floatArrayOf(1f, 1f, -1f),
            floatArrayOf(-1f, 1f, -1f),
            floatArrayOf(-1f, -1f, 1f),
            floatArrayOf(1f, -1f, 1f),
            floatArrayOf(1f, 1f, 1f),
            floatArrayOf(-1f, 1f, 1f),
        )
    val tv =
        arrayOf(
            floatArrayOf(-1f, -1f, -1f),
            floatArrayOf(1f, -1f, -1f),
            floatArrayOf(1f, 1f, -1f),
            floatArrayOf(-1f, 1f, -1f),
            floatArrayOf(-1f, -1f, 1f),
            floatArrayOf(1f, -1f, 1f),
            floatArrayOf(1f, 1f, 1f),
            floatArrayOf(-1f, 1f, 1f),
        )

    val faces =
        intArrayOf(
            0,
            1,
            2,
            3, // Front face
            1,
            5,
            6,
            2, // Right face
            5,
            4,
            7,
            6, // Back face
            4,
            0,
            3,
            7, // Left face
            3,
            2,
            6,
            7, // Top face
            4,
            5,
            1,
            0,
        ) // Bottom face

    addHeaderParam(Header.DOC_CONTENT_DESCRIPTION, "Cube (Interactive)")
    addHeaderParam(Header.DOC_DESIRED_FPS, 120)

    return demo7 {
        root {
            box(Modifier.fillMaxWidth().fillMaxHeight()) {
                canvas(Modifier.fillMaxWidth().fillMaxHeight()) {
                    val width = ComponentWidth()
                    val height = ComponentHeight()

                    val centerX = width / 2f
                    val centerY = height / 2f
                    val radius = min(centerX, centerY)

                    painter.setColor(Color.DarkGray.toArgb()).setStyle(Paint.Style.FILL).commit()
                    drawCircle(centerX.toFloat(), centerY.toFloat(), radius.toFloat())

                    // Touch-Interactive Rotation Expressions
                    val rotX =
                        rf(
                            touchExpression(
                                RemoteContext.FLOAT_TOUCH_POS_Y,
                                0.75f,
                                Rc.FloatExpression.MUL,
                                defValue = 0f,
                                min = Float.NaN,
                                max = 360f,
                                touchMode = TouchExpression.STOP_GENTLY,
                            )
                        )

                    val rotY =
                        rf(
                            touchExpression(
                                RemoteContext.FLOAT_TOUCH_POS_X,
                                0.75f,
                                Rc.FloatExpression.MUL,
                                defValue = 0f,
                                min = Float.NaN,
                                max = 360f,
                                touchMode = TouchExpression.STOP_GENTLY,
                            )
                        )

                    val world =
                        matrix(
                            6f,
                            MatrixOperations.TRANSLATE_Z,
                            rotX.toFloat(),
                            MatrixOperations.ROT_X,
                            rotY.toFloat(),
                            MatrixOperations.ROT_Y,
                        )

                    val fov = 60f
                    val aspect = 1f
                    val near = 0.1f
                    val far = 100f
                    val sx = (centerX * 0.4f).toFloat()
                    val sy = (centerX * -0.4f).toFloat()
                    val pMatrix =
                        matrix(
                            fov,
                            aspect,
                            near,
                            far,
                            MatrixOperations.PROJECTION,
                            sx,
                            sy,
                            MatrixOperations.SCALE2,
                        )

                    var i = 0
                    while (i < vertices.size) {
                        val (x1, y1, z1) =
                            world.mult(vertices[i][0], vertices[i][1], vertices[i][2])
                        val (x, y, z) = pMatrix.projectionMult(x1, y1, z1)
                        tv[i][0] = rf(x).plus(centerX).toFloat()
                        tv[i][1] = rf(y).plus(centerY).toFloat()
                        tv[i][2] = z
                        i++
                    }

                    i = 0
                    @Suppress("PrimitiveIntInCollection") val paths = ArrayList<Int>()
                    val dir = FloatArray(6)

                    while (i < faces.size) {
                        val f1 = faces[i]
                        val f2 = faces[i + 1]
                        val f3 = faces[i + 2]
                        val f4 = faces[i + 3]

                        val path = RemotePath()
                        path.moveTo(tv[f1][0], tv[f1][1])
                        path.lineTo(tv[f2][0], tv[f2][1])
                        path.lineTo(tv[f3][0], tv[f3][1])
                        path.lineTo(tv[f4][0], tv[f4][1])
                        path.close()
                        paths.add(addPathData(path))
                        val fx1 = rf(tv[f1][0])
                        val fy1 = rf(tv[f1][1])
                        val fx2 = rf(tv[f2][0])
                        val fy2 = rf(tv[f2][1])
                        val fx3 = rf(tv[f3][0])
                        val fy3 = rf(tv[f3][1])

                        dir[i / 4] =
                            ((fx1 - fx2) * (fy3 - fy2) - (fy1 - fy2) * (fx3 - fx2)).toFloat()

                        i += 4
                    }

                    painter
                        .setShader(0)
                        .setColor(Color.LightGray.toArgb())
                        .setStyle(Paint.Style.FILL)
                        .commit()
                    val color =
                        intArrayOf(
                            Color.Red.toArgb(),
                            Color.Green.toArgb(),
                            Color.Blue.toArgb(),
                            Color.Yellow.toArgb(),
                            Color.Magenta.toArgb(),
                            Color.Cyan.toArgb(),
                        )
                    for (p in 0..5) {
                        conditionalOperations(ConditionalOperations.TYPE_GT, dir[p], 0f)
                        painter.setColor(color[p]).commit()
                        drawPath(paths[p])
                        endConditionalOperations()
                    }
                    painter
                        .setColor(Color.Black.toArgb())
                        .setStrokeJoin(Paint.Join.ROUND)
                        .setStrokeWidth(5f)
                        .setStyle(Paint.Style.STROKE)
                        .commit()

                    for (p in 0..5) {
                        conditionalOperations(ConditionalOperations.TYPE_GT, dir[p], 0f)
                        drawPath(paths[p])
                        endConditionalOperations()
                    }
                }
            }
        }
    }
}

@Preview @Composable private fun CubeInteractivePreview() = RemoteDocumentPreview(cubeInteractive())
