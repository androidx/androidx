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

import android.graphics.Paint.Style
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.creation.ComponentHeight
import androidx.compose.remote.creation.ComponentWidth
import androidx.compose.remote.creation.ContinuousSec
import androidx.compose.remote.creation.RFloat
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.abs
import androidx.compose.remote.creation.arrayValue
import androidx.compose.remote.creation.clamp
import androidx.compose.remote.creation.component6
import androidx.compose.remote.creation.createParticles
import androidx.compose.remote.creation.deltaTime
import androidx.compose.remote.creation.div
import androidx.compose.remote.creation.floor
import androidx.compose.remote.creation.index
import androidx.compose.remote.creation.max
import androidx.compose.remote.creation.min
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.particlesComparison
import androidx.compose.remote.creation.particlesLoops
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.rand
import androidx.compose.remote.creation.rf
import androidx.compose.remote.creation.sign
import androidx.compose.remote.creation.sin
import androidx.compose.remote.creation.times
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import kotlin.random.Random

@Suppress("RestrictedApiAndroidX")
fun ball(): RemoteComposeWriter {
    val rc = RemoteComposeWriterAndroid(500, 500, "sd", 6, 0, AndroidxRcPlatformServices())
    rc.root {
        rc.startBox(
            RecordingModifier().fillMaxWidth().fillMaxHeight(),
            BoxLayout.START,
            BoxLayout.START,
        )

        rc.startCanvas(RecordingModifier().fillMaxSize())

        rc.painter.setColor(Color(0xFFDAEFCF).toArgb()).commit()

        val w = rc.ComponentWidth()
        val h = rc.ComponentWidth()
        rc.ContinuousSec()
        rc.drawRoundRect(0f, 0f, w.toFloat(), h.toFloat(), 10f, 10f)

        rc.save()
        rc.painter.setColor(Color(0xFF053117).toArgb()).commit()

        val cx = w / 2f
        val cy = h / 2f
        val variables: Array<RFloat> = Array<RFloat>(5, { RFloat(rc, 0f) })
        with(rc) {
            painter.setColor(Color(0xFF59584C).toArgb()).setAlpha(1f).setShader(0).commit()
            drawCircle((cy).toFloat(), (cx).toFloat(), 40f)

            val ax = rf(RemoteContext.FLOAT_ACCELERATION_X) * -5f
            val ay = rf(RemoteContext.FLOAT_ACCELERATION_Y) * 5f
            drawCircle((cx + ax * 10f).toFloat(), (cy + ay * 10f).toFloat(), 32f)

            painter.setColor(Color(0xFF8D6376).toArgb()).setAlpha(1f).setShader(0).commit()
            val ax1 =
                rf(floatExpression(floatArrayOf(ax.toFloat()), rc.spring(30f, 10f, 0.001f, 0)))

            val ay1 =
                rf(floatExpression(floatArrayOf(ay.toFloat()), rc.spring(30f, 10f, 0.001f, 0)))

            drawCircle((cx + ax1 * 10f).toFloat(), (cy + ay1 * 10f).toFloat(), 32f)

            val pCount = 1
            val skip = 20f
            painter.setColor(Color(0xFF002AFF).toArgb()).setAlpha(1f).setShader(0).commit()
            val event = ContinuousSec().toFloat()
            impulse(2000f, event) {
                val variables: Array<RFloat> = Array<RFloat>(4, { RFloat(this, 0f) })
                val pi = (Math.PI).toFloat()
                val ps = createParticles(variables, arrayOf(cx, cy, 0f, 0f), pCount)
                val (px, py, dx, dy) = variables

                val dt = 1f // deltTime()
                impulseProcess() {
                    particlesLoops(
                        ps,
                        null,
                        arrayOf(
                            min(max(0f, px + dx * dt), w),
                            min(max(0f, py + dy * dt), h),
                            dx + ax * dt - dx * 0.2f,
                            dy + dt * ay - dy * 0.2f,
                        ),
                    ) {
                        drawCircle((px).toFloat(), py.toFloat(), 32f)
                        //            addDebugMessage("x ", px.toFloat())
                        //            addDebugMessage("dx ", dx.toFloat())
                    }
                }
            }
        }
        rc.endCanvas()
        rc.endBox()
    }
    return rc
}

@Suppress("RestrictedApiAndroidX")
fun maze(): RemoteComposeWriter {
    val rc = RemoteComposeWriterAndroid(500, 500, "sd", 6, 0, AndroidxRcPlatformServices())
    val timeId = rc.createTextFromFloat(Rc.Time.ANIMATION_TIME, 3, 1, Rc.TextFromFloat.PAD_PRE_ZERO)

    rc.root {
        rc.startColumn(RecordingModifier().fillMaxWidth().fillMaxHeight(), 1, 1)
        rc.startTextComponent(
            RecordingModifier().fillMaxWidth().height(122f).background(Color.DarkGray.toArgb()),
            timeId,
            Color.Yellow.toArgb(),
            100f,
            0,
            1f,
            null,
            3,
            0,
            1,
        )
        rc.endTextComponent()
        rc.startCanvas(RecordingModifier().fillMaxSize())

        rc.painter.setColor(Color(0xFF000000).toArgb()).setAlpha(1f).commit()

        val w = rc.ComponentWidth()
        val h = rc.ComponentWidth()
        rc.conditionalOperations(Rc.Condition.GT, w.toFloat(), 10f)
        val mDim = 10
        //    val mDim1 = (mDim - 1f)
        rc.drawRoundRect(0f, 0f, w.toFloat(), h.toFloat(), 10f, 10f)
        val r = Random(System.currentTimeMillis())
        val maze = ExampleUtils.Maze.genMaze(mDim, mDim, r.nextLong())
        val mazePath = ExampleUtils.Maze.genPath(maze, 10, 10)
        val mazeWalls = ExampleUtils.Maze.genWalls(maze, 10, 10)

        val gap = w / mDim.toFloat()
        val y1 = (sin(rc.ContinuousSec() / 13f) * 0.5f + 0.5f) * h
        val x1 = (sin(rc.ContinuousSec() / 7f) * 0.5f + 0.5f) * w

        // roaming ball
        rc.painter.setColor(Color(0x881C4606).toArgb()).commit()
        rc.drawCircle(x1.toFloat(), y1.toFloat(), (gap / 5f).toFloat())
        val left = rc.addFloatArray(mazeWalls[0])
        val right = rc.addFloatArray(mazeWalls[1])
        val top = rc.addFloatArray(mazeWalls[2])
        val bottom = rc.addFloatArray(mazeWalls[3])
        val walls = floatArrayOf(left, right, top, bottom)
        rc.painter.setColor(Color(0xFF09A804).toArgb()).commit()
        drawRelatedWalls(rc, walls, x1, y1, gap, mDim, w, h)
        gap.toFloat()

        rc.painter
            .setColor(Color(0xFF866767).toArgb())
            .setStrokeWidth(10f)
            .setStyle(Style.STROKE)
            .commit()
        val id = rc.addPathData(mazePath)

        rc.save()
        rc.scale((w / 1000f).toFloat(), (h / 1000f).toFloat())
        rc.drawPath(id)
        rc.restore()

        val cx = w / 2f
        val cy = h / 2f

        with(rc) {
            val ax = rf(RemoteContext.FLOAT_ACCELERATION_X) * -1f
            val ay = rf(RemoteContext.FLOAT_ACCELERATION_Y) * 1f
            val pCount = 1
            painter
                .setColor(Color(0xFF002AFF).toArgb())
                .setAlpha(1f)
                .setStyle(Style.FILL)
                .setShader(0)
                .commit()
            val event = ContinuousSec().toFloat()
            impulse(20000f, event) {
                val variables: Array<RFloat> = Array<RFloat>(6, { RFloat(this, 0f) })
                val pi = (Math.PI).toFloat()
                val ps =
                    createParticles(
                        variables,
                        arrayOf(
                            0f,
                            0f,
                            0f * (rand() - 0.5f),
                            0f * (rand() - 0.5f),
                            cx * (rand() - 0.5f) / 2f,
                            cy * (rand() - 0.5f) / 2f,
                        ),
                        pCount,
                    )
                val (wx, wy, dx, dy, px, py) = variables

                val dt = deltaTime() * 33f

                impulseProcess() {
                    particlesLoops(
                        ps,
                        null,
                        arrayOf(
                            hWallRepel(rc, walls, px, py, gap, mDim, w, h),
                            vWallRepel(rc, walls, px, py, gap, mDim, w, h),
                            dx + (ax - dx * 0.2f /*+  (cx - px) * 0.01f*/ + wx) * dt,
                            dy + (ay - dy * 0.2f /*+ (cy - py) * 0.01f*/ - wy) * dt,
                            clamp(40f, w - 40f, px + dx * dt / 2f),
                            clamp(40f, h - 40f, py + dy * dt / 2f),
                        ),
                    ) {
                        //            painter.setColor(Color(0xFFFA0202).toArgb()).commit()
                        //
                        //            drawCircle(cx.toFloat(), cy.toFloat(), 2f)
                        //            drawLine(cx.toFloat(),cy.toFloat(),px.toFloat(), py.toFloat())
                        painter.setColor(Color(0x99B4BBDE).toArgb()).commit()
                        drawCircle(px.toFloat(), py.toFloat(), 16f)
                        //            var hyp = hypot(dx, dy)
                        //            drawLine(
                        //              px.toFloat(),
                        //              py.toFloat(),
                        //              (px - dx * 100f / hyp).toFloat(),
                        //              (py - dy * 100f / hyp).toFloat()
                        //            )
                        //    rc.painter
                        //      .setColor(Color(0xFFA80416).toArgb()).commit()
                        //     drawRelatedWalls(rc, walls, px, py, gap, mDim, w, h)
                        //            val repX = hWallRepel(rc, walls, px, py, gap, mDim, w, h)
                        //            val repY = vWallRepel(rc, walls, px, py, gap, mDim, w, h)
                        //            val repXf = rf(repX.toFloat())
                        //            val repYf = rf(repY.toFloat())
                        //            rc.painter
                        //              .setColor(Color(0xFF002AFF).toArgb()).commit()
                        //            var hyp2 = hypot(repXf, repYf)
                        //            drawLine(
                        //              px.toFloat(),
                        //              py.toFloat(),
                        //              (px + repXf * 100f / hyp2).toFloat(),
                        //              (py - repYf * 100f / hyp2).toFloat()
                        //            )

                    }
                }
            }
        }
        rc.endConditionalOperations()
        rc.endCanvas()

        rc.endColumn()
    }
    return rc
}

@Suppress("RestrictedApiAndroidX")
fun RemoteComposeWriterAndroid.lookup(
    array: Float,
    x: RFloat,
    y: RFloat,
    gap: RFloat,
    max: Float,
): RFloat {
    val index = (clamp(0f, max, floor(x / gap)) + clamp(0f, max, floor(y / gap)) * 10f)
    val ret = arrayValue(array, index)
    return ret
}

@Suppress("RestrictedApiAndroidX")
fun RemoteComposeWriterAndroid.lBounce(
    x: RFloat,
    y: RFloat,
    left: Float,
    right: Float,
    gap: RFloat,
    max: Float,
): RFloat {
    //  val index = (clamp(0f,max,floor(x / gap)) + clamp(0f,max,floor(y / gap)) * 10f)
    //  val leftv =  arrayValue(left, index)
    //  val rightv =  arrayValue(right, index)
    //  return  sign((x-leftv) * (x-rightv))
    return sign(x - 40f)
}

@Suppress("RestrictedApiAndroidX")
fun RemoteComposeWriterAndroid.lsBounce(x: RFloat, w: RFloat): RFloat {
    val ret = sign((x - 42f) * (w - 42f - x))
    addDebugMessage(" ret ", ret.toFloat())

    val ret1 = max(ret, 0f)
    return ret1
}

@Suppress("RestrictedApiAndroidX")
fun hWallRepel(
    rc: RemoteComposeWriterAndroid,
    mazeWalls: FloatArray,
    x1: RFloat,
    y1: RFloat,
    gap: RFloat,
    mDim: Int,
    w: RFloat,
    h: RFloat,
): RFloat {

    val (left, right, top, bottom) = mazeWalls

    val lWall = rc.lookup(left, x1, y1, gap, (mDim - 1f)) * w / 1000f
    val rWall = rc.lookup(right, x1, y1, gap, (mDim - 1f)) * w / 1000f
    return 50f / abs(x1 - rc.rf(lWall.toFloat())) - 50f / abs(x1 - rc.rf(rWall.toFloat()))
}

@Suppress("RestrictedApiAndroidX")
fun vWallRepel(
    rc: RemoteComposeWriterAndroid,
    mazeWalls: FloatArray,
    x1: RFloat,
    y1: RFloat,
    gap: RFloat,
    mDim: Int,
    w: RFloat,
    h: RFloat,
): RFloat {

    val (left, right, top, bottom) = mazeWalls

    val tWall = rc.lookup(top, x1, y1, gap, (mDim - 1f)) * h / 1000f
    val bWall = rc.lookup(bottom, x1, y1, gap, (mDim - 1f)) * h / 1000f

    return 50f / abs(y1 - rc.rf(tWall.toFloat())) - 50f / abs(y1 - rc.rf(bWall.toFloat()))
}

@Suppress("RestrictedApiAndroidX")
fun drawRelatedWalls(
    rc: RemoteComposeWriterAndroid,
    mazeWalls: FloatArray,
    x1: RFloat,
    y1: RFloat,
    gap: RFloat,
    mDim: Int,
    w: RFloat,
    h: RFloat,
) {
    val (left, right, top, bottom) = mazeWalls

    val lWall = rc.lookup(left, x1, y1, gap, (mDim - 1f)) * w / 1000f

    rc.drawRect(
        (lWall - 2f).toFloat(),
        (y1 - gap / 2f).toFloat(),
        (lWall + 3f).toFloat(),
        (y1 + gap / 2f).toFloat(),
    )
    val rWall = rc.lookup(right, x1, y1, gap, (mDim - 1f)) * h / 1000f
    rc.drawRect(
        (rWall - 2f).toFloat(),
        (y1 - gap / 2f).toFloat(),
        (rWall + 3f).toFloat(),
        (y1 + gap / 2f).toFloat(),
    )

    val tWall = rc.lookup(top, x1, y1, gap, (mDim - 1f)) * h / 1000f

    rc.drawRect(
        (x1 - gap / 2f).toFloat(),
        (tWall - 2f).toFloat(),
        (x1 + gap / 2f).toFloat(),
        (tWall + 3f).toFloat(),
    )

    val bWall = rc.lookup(bottom, x1, y1, gap, (mDim - 1f)) * h / 1000f

    rc.drawRect(
        (x1 - gap / 2f).toFloat(),
        (bWall - 2f).toFloat(),
        (x1 + gap / 2f).toFloat(),
        (bWall + 3f).toFloat(),
    )
    rc.painter.setColor(Color(0x4F3D4572).toArgb()).commit()
    rc.drawRoundRect(lWall.toFloat(), tWall.toFloat(), rWall.toFloat(), bWall.toFloat(), 500f, 500f)
}

@Suppress("RestrictedApiAndroidX")
fun pmaze1(): RemoteComposeWriter {
    return psMaze2(1, false, mazeDim = 20)
}

@Suppress("RestrictedApiAndroidX")
fun pmaze2(): RemoteComposeWriter {
    return psMaze2(120, false, mazeDim = 10)
}

@Suppress("RestrictedApiAndroidX")
fun pmaze(): RemoteComposeWriter {
    return psMaze2(1, false, mazeDim = 6)
}

@Suppress("RestrictedApiAndroidX")
fun psMaze2(
    pCount: Int = 120,
    drawWalls: Boolean = false,
    haptics: Boolean = false,
    mazeDim: Int = 6,
    randomMaze: Boolean = true,
    timeScale: Float = 3f,
    gravity: Float = 30f,
    friction: Float = 0.02f,
    elastic: Float = .6f,
    brownian: Float = 3f,
): RemoteComposeWriter {
    val rc = ExampleUtils.getWriter(7)
    val timeId = rc.createTextFromFloat(Rc.Time.ANIMATION_TIME, 3, 1, Rc.TextFromFloat.PAD_PRE_ZERO)

    rc.root {
        rc.startColumn(RecordingModifier().fillMaxWidth().fillMaxHeight(), 1, 1)

        rc.startCanvas(RecordingModifier().fillMaxSize())

        rc.painter.setColor(Color(0xFF000000).toArgb()).setAlpha(1f).commit()

        val w = rc.ComponentWidth()
        val h = rc.ComponentHeight() - 4f
        rc.conditionalOperations(Rc.Condition.GT, w.toFloat(), 10f)
        val dim = mazeDim
        val dim1 = dim - 1f

        rc.drawRoundRect(0f, 0f, w.toFloat(), h.toFloat(), 10f, 10f)
        val r = Random(if (randomMaze) System.currentTimeMillis() else 4567L)
        val maze = ExampleUtils.Maze.genMaze(dim, dim, r.nextLong())
        val mazePath = ExampleUtils.Maze.genPath(maze, dim, dim)
        val mazeWalls = ExampleUtils.Maze.genWalls(maze, dim, dim)

        val gap = w / dim.toFloat()

        rc.painter.setColor(Color(0x881C4606).toArgb()).commit()
        val left = rc.addFloatArray(mazeWalls[0])
        val right = rc.addFloatArray(mazeWalls[1])
        val top = rc.addFloatArray(mazeWalls[2])
        val bottom = rc.addFloatArray(mazeWalls[3])
        val walls = floatArrayOf(left, right, top, bottom)

        gap.toFloat()

        rc.painter
            .setColor(Color(0x8B1A50B0).toArgb())
            .setStrokeWidth(10f)
            .setStyle(Style.STROKE)
            .commit()
        val id = rc.addPathData(mazePath)

        rc.save()
        rc.scale((w / 1000f).toFloat(), (h / 1000f).toFloat())
        rc.drawPath(id)
        rc.restore()

        rc.painter
            .setColor(Color(0xFFA13636).toArgb())
            .setStrokeWidth(5f)
            .setStyle(Style.STROKE)
            .commit()

        with(rc) {
            val ballRad = 16f
            val ax = rf(RemoteContext.FLOAT_ACCELERATION_X) * -gravity
            val ay = rf(RemoteContext.FLOAT_ACCELERATION_Y) * gravity

            painter
                .setColor(Color(0xFF002AFF).toArgb())
                .setAlpha(1f)
                .setStyle(Style.FILL)
                .setShader(0)
                .commit()
            val event = ContinuousSec().toFloat()
            impulse(20000f, event) {
                val variables: Array<RFloat> = Array<RFloat>(5, { RFloat(this, 0f) })
                val ps =
                    createParticles(
                        variables,
                        arrayOf(
                            rand() * 120f - 5f, // dx
                            rand() * 120f - 5f, // dy
                            w * rand(), // x
                            50f, // y
                            index() / pCount.toFloat(), // hue
                        ),
                        pCount,
                    )
                val (dx, dy, px, py, hue) = variables

                val dt = deltaTime() * timeScale
                impulseProcess() {
                    if (true) {
                        painter.setColor(Color(0xFD01EF06).toArgb()).commit()
                        val minVal = -1f
                        val maxVal = -1f
                        // ===== below =====
                        particlesComparison(
                            id = ps,
                            min = minVal,
                            max = maxVal,
                            condition =
                                py + ballRad - lookup2(top, px, py, gap, dim1, dim) * w / 1000f,
                            then =
                                arrayOf(
                                    dx,
                                    dy * -elastic,
                                    px,
                                    lookup2(top, px, py, gap, dim1, dim) * w / 1000f - ballRad - 1f,
                                    hue,
                                ),
                        ) {
                            drawCircle(px.toFloat(), (py + ballRad).toFloat(), 5f)
                            // performHaptic(2)
                        }

                        // ===== above =====
                        particlesComparison(
                            id = ps,
                            min = minVal,
                            max = maxVal,
                            condition =
                                lookup2(bottom, px, py, gap, dim1, dim) * w / 1000f + ballRad - py,
                            then =
                                arrayOf(
                                    dx,
                                    dy * -elastic,
                                    px,
                                    lookup2(bottom, px, py, gap, dim1, dim) * w / 1000f +
                                        ballRad +
                                        1f,
                                    hue,
                                ),
                        ) {
                            drawCircle(px.toFloat(), (py - ballRad).toFloat(), 5f)
                            if (haptics) {
                                performHaptic(2)
                            }
                        }

                        // ===== left =====
                        particlesComparison(
                            id = ps,
                            min = minVal,
                            max = maxVal,
                            condition =
                                lookup2(left, px, py, gap, dim1, dim) * w / 1000f + ballRad - px,
                            then =
                                arrayOf(
                                    dx * -elastic,
                                    dy,
                                    lookup2(left, px, py, gap, dim1, dim) * w / 1000f +
                                        ballRad +
                                        1f,
                                    py,
                                    hue,
                                ),
                        ) {
                            drawCircle((px - ballRad).toFloat(), py.toFloat(), 5f)
                            if (haptics) {
                                performHaptic(2)
                            }
                        }
                        // ===== right =====
                        particlesComparison(
                            id = ps,
                            min = minVal,
                            max = maxVal,
                            condition =
                                px + ballRad - lookup2(right, px, py, gap, dim1, dim) * w / 1000f,
                            then =
                                arrayOf(
                                    dx * -elastic,
                                    dy,
                                    lookup2(right, px, py, gap, dim1, dim) * w / 1000f -
                                        ballRad -
                                        1f,
                                    py,
                                    hue,
                                ),
                        ) {
                            drawCircle((px + ballRad).toFloat(), py.toFloat(), 5f)
                            if (haptics) {
                                performHaptic(2)
                            }
                        }
                    }
                    particlesLoops(
                        ps,
                        null,
                        arrayOf(
                            dx + (ax - dx * friction) * dt + (rand() - 0.5f) * brownian,
                            dy + (ay - dy * friction) * dt + (rand() - 0.5f) * brownian,
                            px + dx * dt / 2f,
                            py + dy * dt / 2f,
                            hue,
                        ),
                    ) {
                        val col = addColorExpression(244, hue.toFloat(), 0.8f, 0.9f)
                        painter.setColorId(col.toInt()).commit()
                        drawCircle(px.toFloat(), py.toFloat(), ballRad)
                        if (drawWalls) {
                            drawRelatedWalls2(rc, walls, px, py, gap, mazeDim, w, h)
                        }
                    }
                }
            }
        }
        rc.endConditionalOperations()
        rc.endCanvas()

        rc.endColumn()
    }
    return rc
}

@Suppress("RestrictedApiAndroidX")
fun drawRelatedWalls2(
    rc: RemoteComposeWriterAndroid,
    mazeWalls: FloatArray,
    x1: RFloat,
    y1: RFloat,
    gap: RFloat,
    mDim: Int,
    w: RFloat,
    h: RFloat,
) {
    val (left, right, top, bottom) = mazeWalls

    val lWall = rc.lookup2(left, x1, y1, gap, (mDim - 1f), mDim) * w / 1000f

    rc.drawRect(
        (lWall - 2f).toFloat(),
        (y1 - gap / 2f).toFloat(),
        (lWall + 3f).toFloat(),
        (y1 + gap / 2f).toFloat(),
    )
    val rWall = rc.lookup2(right, x1, y1, gap, (mDim - 1f), mDim) * h / 1000f
    rc.drawRect(
        (rWall - 2f).toFloat(),
        (y1 - gap / 2f).toFloat(),
        (rWall + 3f).toFloat(),
        (y1 + gap / 2f).toFloat(),
    )

    val tWall = rc.lookup2(top, x1, y1, gap, (mDim - 1f), mDim) * h / 1000f

    rc.drawRect(
        (x1 - gap / 2f).toFloat(),
        (tWall - 2f).toFloat(),
        (x1 + gap / 2f).toFloat(),
        (tWall + 3f).toFloat(),
    )

    val bWall = rc.lookup2(bottom, x1, y1, gap, (mDim - 1f), mDim) * h / 1000f

    rc.drawRect(
        (x1 - gap / 2f).toFloat(),
        (bWall - 2f).toFloat(),
        (x1 + gap / 2f).toFloat(),
        (bWall + 3f).toFloat(),
    )
    rc.painter.setColor(Color(0x4F3D4572).toArgb()).commit()
    rc.drawRoundRect(lWall.toFloat(), tWall.toFloat(), rWall.toFloat(), bWall.toFloat(), 500f, 500f)
}

@Suppress("RestrictedApiAndroidX")
fun RemoteComposeWriterAndroid.lookup2(
    array: Float,
    x: RFloat,
    y: RFloat,
    gap: RFloat,
    max: Float,
    dim: Int,
): RFloat {

    return arrayValue(
        array,
        (clamp(0f, max, floor(x / gap)) + clamp(0f, max, floor(y / gap)) * dim.toFloat()),
    )
}

@Preview @Composable fun BallPreview() = RemoteDocPreview(ball())

@Preview @Composable fun MazePreview() = RemoteDocPreview(maze())
