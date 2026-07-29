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

package androidx.compose.remote.integration.view.demos.dsl.games

import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcCanvasScope
import androidx.compose.remote.creation.dsl.RcConditionOp
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcPath
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcStrokeCap
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.floor
import androidx.compose.remote.creation.dsl.ifElse
import androidx.compose.remote.creation.dsl.lerp
import androidx.compose.remote.creation.dsl.minus
import androidx.compose.remote.creation.dsl.onClick
import androidx.compose.remote.creation.dsl.smoothStep
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.integration.view.demos.dsl.RcPathData

/**
 * A Flappy Droid style interaction experiment using a particle system of one. The Android mascot
 * with a jetpack falls to the ground automatically. Touch and hold anywhere to activate jetpack
 * thrust. Navigate through the moving green pipes.
 */
@Suppress("RestrictedApiAndroidX")
fun dslGameFlappyDroid(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX)) {
        floatArrayOf()

        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().onClick {}) {
                val w = componentWidth()
                val h = componentHeight()
                val t = continuousSeconds()

                //                Text(
                //                    score.genTextId(3,0),
                //                    fontSize = 120f.rsp
                //                )
                // Setup Particle System of One for Android
                // Variables: [ y,  dy]
                val px = 160f.rf
                val variables = FloatArray(2)
                val pipeWidth = 80f.rf
                val flow = (t * 130f).flush()
                val current = 0.rf.flush()
                val pipeX = w - ((flow) % (w + 100f))
                val gapY = h / 2f + sin(t * 0.8f) * 120f
                val gapHalf = 110f.rf
                val highScore = 0f.rf.flush()

                // Draw background sky & clouds
                paint {
                    color(0xFF35A7FFL.toInt()) // Sky blue
                    style(RcPaintStyle.Fill)
                }
                drawRect(0f.rf, 0f.rf, w, h)

                paint {
                    color(0x22FFFFFFL.toInt()) // clouds
                    style(RcPaintStyle.Fill)
                }
                loop(1.rf, 1.rf, 30.rf) { index ->
                    val wrap = (index * 123f + (t * (index / 30f) * 123f)) % (w + 600f)

                    val pos = w - wrap + 300f
                    drawCircle(pos, sin(index * 323.25f) * h / 2f + h / 2f, index * 6f)
                    val wrap2 = smoothStep(((t / 60f)) % 1f, -300.rf, w + 300f)
                    val pos2 = w - wrap2 + 300f
                    drawCircle(pos2, sin(index * 124.32f) * h / 2f + h / 2f, index + 1f)
                }
                paint {
                    color(0x44FFFFFFL.toInt()) // clouds
                }
                loop(1.rf, 1.rf, 30.rf) { index ->
                    val wrap2 =
                        lerp((-300).rf, w + 300f, (t * (index / 100f) * abs(sin(index))) % 1f)
                    val pos2 = w - wrap2
                    drawCircle(pos2, sin(index * 124.32f) * h / 2f + h / 2f, index / 6f + 5f)
                }
                paint {
                    color(0xFF_4C9950.toInt()) // ground
                }
                drawRect(0f.rf, h * 0.9f, w, h)
                // Draw Pipes (green columns)
                paint {
                    color(0xFF4CAF50.toInt()) // Pipe green
                    style(RcPaintStyle.Fill)
                }
                val rounding = 10f.rf
                // Upper pipe
                drawRoundRect(
                    pipeX,
                    (-30f).rf,
                    pipeX + pipeWidth,
                    gapY - gapHalf,
                    rounding,
                    rounding,
                )
                // Lower pipe
                drawRoundRect(
                    pipeX,
                    gapY + gapHalf,
                    pipeX + pipeWidth,
                    h + 100f,
                    rounding,
                    rounding,
                )

                // ===================== ANDROID WITH JETPACK =============================

                val isTouching = sign(max(0f.rf, touchTime() - animationTime() + 0.15f))
                val score = 0.rf.flush()

                impulse(20000.rf, 0.rf) {
                    runAction { setValue(current, flow) }

                    // =========== Create Paths ==========
                    val flameOrange =
                        this@Canvas.remotePathData(
                            RcPathData(
                                "M -78,700 A 78 78 0 0 1 78,700 C 78,790 22,855 0,905 C -22,855 -78,790 -78,700 Z"
                            )
                        )
                    val flameYellow =
                        this@Canvas.remotePathData(
                            RcPathData(
                                "M -46,712 A 46 46 0 0 1 46,712 C 46,772 14,820 0,855 C -14,820 -46,772 -46,712 Z"
                            )
                        )
                    val rocketBody =
                        this@Canvas.remotePathData(
                            RcPathData(
                                "M 0,0 C -54,54 -124,158 -124,248 L -124,615 L 124,615 L 124,248 C 124,158 54,54 0,0 Z"
                            )
                        )
                    val rocketShade =
                        this@Canvas.remotePathData(
                            RcPathData("M 0,0 C 54,54 124,158 124,248 L 124,615 L 0,615 Z")
                        )
                    val rocketNozzle =
                        this@Canvas.remotePathData(
                            RcPathData(
                                "M -124,598 L 124,598 L 124,658 Q 124,690 92,690 L -92,690 Q -124,690 -124,658 Z"
                            )
                        )
                    val droidTorso =
                        this@Canvas.remotePathData(
                            RcPathData(
                                "M -290,34 L 290,34 L 290,322 Q 290,370 242,370 L -242,370 Q -290,370 -290,322 Z"
                            )
                        )
                    val droidHead =
                        this@Canvas.remotePathData(RcPathData("M -290,0 A 290 290 0 0 1 290,0 Z"))
                    // =========== Create Paths ==========

                    val ps = createParticles(variables, arrayOf(h / 2f, 0f.rf), 1)
                    val py = RcFloat(variables[0])
                    val pdy = RcFloat(variables[1])
                    val dt = deltaTime()
                    impulseProcess() {
                        particlesComparison(
                            id = ps,
                            flags = 0,
                            min = 0.rf,
                            max = 1.rf,
                            condition = (pipeX - px) * (px - (pipeX + pipeWidth)),
                            then = arrayOf(py, pdy),
                        ) {
                            conditionalOperations(RcConditionOp.Gt, gapHalf, abs(gapY - py)) {
                                val hs =
                                    max(highScore, floor((max(0f, (flow - current))) / (w + 100f)))
                                runAction { setValue(highScore, hs) }

                                paint {
                                    color(0x00000000L.toInt()) // Sky blue
                                }
                            }
                            conditionalOperations(RcConditionOp.Gt, abs(gapY - py), gapHalf) {
                                runAction { setValue(current, flow) }
                                paint {
                                    color(0xFF990000L.toInt()) // Sky blue
                                }
                            }
                            drawCircle(px, py, 60.rf)
                        }
                        particlesLoop(
                            ps,
                            null,
                            arrayOf(
                                min(h * 0.95f, py + pdy * dt),
                                (pdy + dt * 900f) * (1f - isTouching) - isTouching * 200f,
                            ),
                        ) {
                            val positionGap = max(0f, (flow - current))
                            val floatCount = ((positionGap) / (w + 100f)).flush()
                            val count = floor(floatCount)
                            //                            drawTextAnchored(count.genTextId(3, 0), w,
                            // 0.rf, 2.rf, 2.rf, 0)
                            val hitWall = sign(flow - current)

                            val scale = max(0f, (floatCount - count - 0.9f) * 10f) + 1f
                            drawTextAnchored(scale.genTextId(3, 3), w, 0.rf, 4.rf, 2.rf, 0)

                            save()

                            scale(scale, scale, w, 0.rf)
                            paint {
                                color(0xFFFFFFFFL.toInt()) // Sky blue
                                style(RcPaintStyle.Stroke)
                                alpha(2f - scale)
                                strokeWidth(8f)
                                textSize(120f)
                            }
                            drawTextAnchored(count.genTextId(3, 0), w, 0.rf, 3.rf, 2.rf, 0)

                            restore()
                            paint {
                                color(0xFFFFFF00L.toInt()) // Sky blue
                                style(RcPaintStyle.Fill)
                                strokeWidth(8f)
                                textSize(60f)
                            }
                            drawTextAnchored(
                                "HIGH SCORE :".join(highScore.genTextId(3, 0)),
                                w,
                                h,
                                1.2f.rf,
                                (-2).rf,
                                0,
                            )
                            this@Canvas.drawAndroid(
                                px,
                                py,
                                hitWall,
                                isTouching,
                                flameOrange,
                                flameYellow,
                                rocketBody,
                                rocketShade,
                                rocketNozzle,
                                droidTorso,
                                droidHead,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.drawAndroid(
    px: RcFloat,
    py: RcFloat,
    hitWall: RcFloat,
    isTouching: RcFloat,
    flameOrange: RcPath,
    flameYellow: RcPath,
    rocketBody: RcPath,
    rocketShade: RcPath,
    rocketNozzle: RcPath,
    droidTorso: RcPath,
    droidHead: RcPath,
) {
    val t = continuousSeconds()
    this.save {
        translate(px, py)
        // Tilt forward slightly when diving, upward when thrusting
        val tiltAngle = (hitWall * 360f) + ifElse(isTouching, 15f.rf, (-15f).rf)

        rotate(tiltAngle.anim(0.2f))

        scale(0.14f, 0.14f)
        translate(-650f, -550f)

        // ============ ROCKET (+ flame) ============
        this@drawAndroid.save {
            translate(432f, 283f)
            rotate(16f)

            // 1. Thrust Flames (orange #E8710A and yellow #FBBC04)
            //            conditionalOperations(RcConditionOp.Lt, isTouching, 0f.rf) {
            //                this@drawAndroid.save {
            //                    val flamePulse = 10f.rf + sin(t * 35f) * 1.5f
            //                 //   translate(0f, 700f)
            //                    scale(flamePulse, flamePulse,0.rf,700.rf)
            //               //     translate(0f, -700f)
            //
            //                    paint {
            //                        color(ORANGE)
            //                        style(RcPaintStyle.Fill)
            //                    }
            //                    drawPath(flameOrange)
            //
            //                    paint {
            //                        color(YELLOW)
            //                        style(RcPaintStyle.Fill)
            //                    }
            //                    drawPath(flameYellow)
            //                }
            //            }
            conditionalOperations(RcConditionOp.Gt, isTouching, 0f.rf) {
                this@drawAndroid.save {
                    //  translate(0f, 700f)
                    scale(1.35f, 1.25f, 0f, 700f)
                    //   translate(0f, -700f)

                    paint {
                        color(ORANGE)
                        style(RcPaintStyle.Fill)
                    }
                    drawPath(flameOrange)

                    paint {
                        color(YELLOW)
                        style(RcPaintStyle.Fill)
                    }
                    drawPath(flameYellow)
                }
            }

            // 2. Rocket Body (#FFFFFF)
            paint {
                color(WHITE)
                style(RcPaintStyle.Fill)
            }
            drawPath(rocketBody)

            // 3. Shaded right half (#DADCE0)
            paint {
                color(GRAY_LIGHT)
                style(RcPaintStyle.Fill)
            }
            drawPath(rocketShade)

            // 4. Band
            paint {
                color(GRAY_MID)
                style(RcPaintStyle.Fill)
            }
            drawRect(-124f, 212f, 124f, 270f)

            // 5. Nozzle (#5F6368)
            paint {
                color(GRAY_DARK)
                style(RcPaintStyle.Fill)
            }
            drawPath(rocketNozzle)

            // 6. Blue stripe
            paint {
                color(BLUE)
                style(RcPaintStyle.Stroke)
                strokeWidth(78f)
                strokeCap(RcStrokeCap.Round)
            }
            drawLine(-15f, 322f, -36f, 592f)
        }

        // ============ DROID ============
        this@drawAndroid.save {
            translate(800f, 560f)
            rotate(14f)

            // 1. Legs
            paint {
                color(GREEN)
                style(RcPaintStyle.Stroke)
                strokeWidth(76f)
                strokeCap(RcStrokeCap.Round)
            }
            drawLine(-100f, 310f, -150f, 660f)
            drawLine(100f, 310f, 50f, 660f)

            // 2. Arms
            paint {
                color(GREEN)
                style(RcPaintStyle.Stroke)
                strokeWidth(110f)
                strokeCap(RcStrokeCap.Round)
            }
            drawLine(200f, 110f, 404f, -42f)

            paint {
                color(GREEN)
                style(RcPaintStyle.Stroke)
                strokeWidth(108f)
                strokeCap(RcStrokeCap.Round)
            }
            drawLine(-250f, 120f, -395f, 345f)

            // 3. Torso
            paint {
                color(GREEN)
                style(RcPaintStyle.Fill)
            }
            drawPath(droidTorso)

            // 4. Belt
            paint {
                color(WHITE)
                style(RcPaintStyle.Fill)
            }
            drawRect(-290f, 285f, 290f, 340f)

            // 5. Antennae
            paint {
                color(GREEN)
                style(RcPaintStyle.Stroke)
                strokeWidth(30f)
                strokeCap(RcStrokeCap.Round)
            }
            drawLine(-152f, -247f, -198f, -345f)
            drawLine(152f, -247f, 198f, -345f)

            // 6. White head/body seam
            paint {
                color(WHITE)
                style(RcPaintStyle.Fill)
            }
            drawRect(-290f, -2f, 290f, 34f)

            // 7. Head
            paint {
                color(GREEN)
                style(RcPaintStyle.Fill)
            }
            drawPath(droidHead)

            // 8. Eyes
            paint {
                color(WHITE)
                style(RcPaintStyle.Fill)
            }
            drawCircle(-131f, -142f, 30f)
            drawCircle(131f, -142f, 30f)
        }
    }
}

private const val VIEWBOX = 1400f

// ---- palette -----------------------------------------------------------
private const val GREEN = 0xFF72BD5A.toInt()
private const val WHITE = 0xFFFFFFFF.toInt()
private const val GRAY_LIGHT = 0xFFDADCE0.toInt()
private const val GRAY_MID = 0xFF9AA0A6.toInt()
private const val GRAY_DARK = 0xFF5F6368.toInt()
private const val BLUE = 0xFF1A73E8.toInt()
private const val YELLOW = 0xFFFBBC04.toInt()
private const val ORANGE = 0xFFE8710A.toInt()
