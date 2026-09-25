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

package androidx.compose.remote.integration.view.demos.blog

import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeWriter
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
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * Flappy Droid: a complete tap-to-fly game in a single Remote Compose document.
 *
 * Touch and hold to fire the jetpack; let go and gravity pulls the droid down. Fly through the gap
 * in the moving pipe to score.
 *
 * It combines every technique from the earlier blog demos:
 * - **Expression-driven scenery** (like AnimatedRadius): sky, parallax clouds, and the pipe are all
 *   functions of `continuousSeconds()`, evaluated by the player each frame.
 * - **A particle system of one** (like touchBounce): two registers, `py`/`pdy`, hold the droid's
 *   vertical position and velocity, integrated with `deltaTime()` and gated by touch.
 * - **`particlesComparison` as a collision test** (like CollisionDetection): the condition
 *   `(pipeX - px) * (px - (pipeX + pipeWidth))` is positive only while the droid is horizontally
 *   inside the pipe, and the body then checks whether it is inside the gap.
 *
 * What's new here is **game state that survives across frames without particles**. `current` and
 * `highScore` are committed variables, and the document mutates them itself via `runAction` and
 * `setValue`. Hitting a pipe snaps `current` to `flow`, which resets the score; passing through the
 * gap raises `highScore`.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslFlappyDroid(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 1000),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 1000),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Flappy Droid"),
        experimental = true,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().onClick {}) {
                // ======================= RESOURCES =======================
                val flameOrange =
                    remotePathData(
                        RcPathData(
                            "M -78,700 A 78 78 0 0 1 78,700 C 78,790 22,855 0,905 " +
                                "C -22,855 -78,790 -78,700 Z"
                        )
                    )
                val flameYellow =
                    remotePathData(
                        RcPathData(
                            "M -46,712 A 46 46 0 0 1 46,712 C 46,772 14,820 0,855 " +
                                "C -14,820 -46,772 -46,712 Z"
                        )
                    )
                val rocketBody =
                    remotePathData(
                        RcPathData(
                            "M 0,0 C -54,54 -124,158 -124,248 L -124,615 L 124,615 " +
                                "L 124,248 C 124,158 54,54 0,0 Z"
                        )
                    )
                val rocketShade =
                    remotePathData(RcPathData("M 0,0 C 54,54 124,158 124,248 L 124,615 L 0,615 Z"))
                val rocketNozzle =
                    remotePathData(
                        RcPathData(
                            "M -124,598 L 124,598 L 124,658 Q 124,690 92,690 " +
                                "L -92,690 Q -124,690 -124,658 Z"
                        )
                    )
                val droidTorso =
                    remotePathData(
                        RcPathData(
                            "M -290,34 L 290,34 L 290,322 Q 290,370 242,370 " +
                                "L -242,370 Q -290,370 -290,322 Z"
                        )
                    )
                val droidHead = remotePathData(RcPathData("M -290,0 A 290 290 0 0 1 290,0 Z"))

                // ======================= VARIABLES =======================
                val w = componentWidth().flush()
                val h = componentHeight().flush()
                val t = continuousSeconds().flush()
                val px = 160f.rf // Droid's fixed horizontal position
                val pipeWidth = 80f.rf
                // Total scroll distance. Score = pipes passed since `current` was last reset.
                val flow = (t * 130f).flush()
                // Mutable game state, written by runAction { setValue(...) } below.
                val current = 0.rf.flush()
                val highScore = 0f.rf.flush()
                val pipeX = w - (flow % (w + 100f))
                val gapY = h / 2f + sin(t * 0.8f) * 120f
                val gapHalf = 110f.rf
                val rounding = 10f.rf

                // ======================= SCENERY =======================
                // Sky
                paint {
                    color(0xFF35A7FF.toInt())
                    style(RcPaintStyle.Fill)
                }
                drawRect(0f.rf, 0f.rf, w, h)

                // Far clouds
                paint {
                    color(0x22FFFFFF)
                    style(RcPaintStyle.Fill)
                }
                loop(1.rf, 1.rf, 30.rf) { index ->
                    val wrap = (index * 123f + (t * (index / 30f) * 123f)) % (w + 600f)
                    val pos = w - wrap + 300f
                    drawCircle(pos, sin(index * 323.25f) * h / 2f + h / 2f, index * 6f)
                    val wrap2 = smoothStep((t / 60f) % 1f, (-300).rf, w + 300f)
                    val pos2 = w - wrap2 + 300f
                    drawCircle(pos2, sin(index * 124.32f) * h / 2f + h / 2f, index + 1f)
                }

                // Near clouds
                paint { color(0x44FFFFFF) }
                loop(1.rf, 1.rf, 30.rf) { index ->
                    val wrap2 =
                        lerp((-300).rf, w + 300f, (t * (index / 100f) * abs(sin(index))) % 1f)
                    val pos2 = w - wrap2
                    drawCircle(pos2, sin(index * 124.32f) * h / 2f + h / 2f, index / 6f + 5f)
                }

                // Ground
                paint { color(0xFF4C9950.toInt()) }
                drawRect(0f.rf, h * 0.9f, w, h)

                // Pipes
                paint {
                    color(0xFF4CAF50.toInt())
                    style(RcPaintStyle.Fill)
                }
                drawRoundRect(
                    pipeX,
                    (-30f).rf,
                    pipeX + pipeWidth,
                    gapY - gapHalf,
                    rounding,
                    rounding,
                )
                drawRoundRect(
                    pipeX,
                    gapY + gapHalf,
                    pipeX + pipeWidth,
                    h + 100f,
                    rounding,
                    rounding,
                )

                // ======================= DROID =======================
                // 1 for 150 ms after each touch event, then 0. Held touches keep refreshing it.
                val isTouching = sign(max(0f.rf, touchTime() - animationTime() + 0.15f))

                impulse(20000.rf, 0.rf) {
                    runAction { setValue(current, flow) }

                    // Registers: [py, pdy] = vertical position and velocity.
                    val variables = FloatArray(2)
                    val ps = createParticles(variables, arrayOf(h / 2f, 0f.rf), 1)
                    val py = RcFloat(variables[0])
                    val pdy = RcFloat(variables[1])
                    val dt = deltaTime()

                    impulseProcess {
                        // Collision test: condition > 0 only while px is between the pipe edges.
                        particlesComparison(
                            id = ps,
                            flags = 0,
                            min = 0.rf,
                            max = 1.rf,
                            condition = (pipeX - px) * (px - (pipeX + pipeWidth)),
                            then = arrayOf(py, pdy),
                        ) {
                            // Inside the gap: bank the score, draw nothing.
                            conditionalOperations(RcConditionOp.Gt, gapHalf, abs(gapY - py)) {
                                val hs = max(highScore, floor(max(0f, flow - current) / (w + 100f)))
                                runAction { setValue(highScore, hs) }
                                paint { color(0x00000000) }
                            }
                            // Hit the pipe: reset the score and flash red.
                            conditionalOperations(RcConditionOp.Gt, abs(gapY - py), gapHalf) {
                                runAction { setValue(current, flow) }
                                paint { color(0xFF990000.toInt()) }
                            }
                            drawCircle(px, py, 60.rf)
                        }

                        particlesLoop(
                            ps,
                            null,
                            arrayOf(
                                // Fall, but never below the ground.
                                min(h * 0.95f, py + pdy * dt),
                                // Gravity while released; fixed upward thrust while touching.
                                (pdy + dt * 900f) * (1f - isTouching) - isTouching * 200f,
                            ),
                        ) {
                            val positionGap = max(0f, flow - current)
                            val floatCount = (positionGap / (w + 100f)).flush()
                            val count = floor(floatCount)
                            val hitWall = sign(flow - current)
                            // Pulses the score briefly each time a new pipe is cleared.
                            val scale = max(0f, (floatCount - count - 0.9f) * 10f) + 1f
                            drawTextAnchored(scale.genTextId(3, 3), w, 0.rf, 4.rf, 2.rf, 0)

                            // Current score
                            save()
                            scale(scale, scale, w, 0.rf)
                            paint {
                                color(0xFFFFFFFF.toInt())
                                style(RcPaintStyle.Stroke)
                                alpha(2f - scale)
                                strokeWidth(8f)
                                textSize(120f)
                            }
                            drawTextAnchored(count.genTextId(3, 0), w, 0.rf, 3.rf, 2.rf, 0)
                            restore()

                            // High score
                            paint {
                                color(0xFFFFFF00.toInt())
                                style(RcPaintStyle.Fill)
                                strokeWidth(8f)
                                textSize(60f)
                            }
                            drawTextAnchored(
                                "HIGH SCORE :".join(highScore.genTextId(3, 0)),
                                w,
                                h,
                                1.2f.rf,
                                (-2f).rf,
                                0,
                            )

                            // The droid itself. Qualified because the particle loop body runs in a
                            // plain RcScope, which has no canvas transform operations.
                            this@Canvas.drawJetpackDroid(
                                px,
                                py,
                                hitWall,
                                isTouching,
                                DroidPaths(
                                    flameOrange,
                                    flameYellow,
                                    rocketBody,
                                    rocketShade,
                                    rocketNozzle,
                                    droidTorso,
                                    droidHead,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The vector paths that make up the droid and its jetpack, in a 1300x1100 local space. */
@Suppress("RestrictedApiAndroidX")
private class DroidPaths(
    val flameOrange: RcPath,
    val flameYellow: RcPath,
    val rocketBody: RcPath,
    val rocketShade: RcPath,
    val rocketNozzle: RcPath,
    val droidTorso: RcPath,
    val droidHead: RcPath,
)

/**
 * Draws the droid centered on ([px], [py]), tilted by thrust and spun 360° after a crash.
 *
 * The artwork is authored at roughly 1300x1100 units, so it is scaled by 0.14 and offset by half
 * its size to put the origin at the droid's center.
 */
@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.drawJetpackDroid(
    px: RcFloat,
    py: RcFloat,
    hitWall: RcFloat,
    isTouching: RcFloat,
    paths: DroidPaths,
) {
    save()
    translate(px, py)
    // Nose up while thrusting, down while falling; a full spin whenever hitWall flips.
    val tiltAngle = (hitWall * 360f) + ifElse(isTouching, 15f.rf, (-15f).rf)
    rotate(tiltAngle.anim(0.2f))
    scale(0.14f, 0.14f)
    translate(-650f, -550f)

    // ============ ROCKET (+ flame) ============
    save()
    translate(432f, 283f)
    rotate(16f)

    // Thrust flames, only while touching
    conditionalOperations(RcConditionOp.Gt, isTouching, 0f.rf) {
        save()
        scale(1.35f, 1.25f, 0f, 700f)
        paint {
            color(ORANGE)
            style(RcPaintStyle.Fill)
        }
        drawPath(paths.flameOrange)
        paint {
            color(YELLOW)
            style(RcPaintStyle.Fill)
        }
        drawPath(paths.flameYellow)
        restore()
    }

    // Body
    paint {
        color(WHITE)
        style(RcPaintStyle.Fill)
    }
    drawPath(paths.rocketBody)

    // Shaded right half
    paint {
        color(GRAY_LIGHT)
        style(RcPaintStyle.Fill)
    }
    drawPath(paths.rocketShade)

    // Band
    paint {
        color(GRAY_MID)
        style(RcPaintStyle.Fill)
    }
    drawRect(-124f, 212f, 124f, 270f)

    // Nozzle
    paint {
        color(GRAY_DARK)
        style(RcPaintStyle.Fill)
    }
    drawPath(paths.rocketNozzle)

    // Blue stripe
    paint {
        color(BLUE)
        style(RcPaintStyle.Stroke)
        strokeWidth(78f)
        strokeCap(RcStrokeCap.Round)
    }
    drawLine(-15f, 322f, -36f, 592f)
    restore()

    // ============ DROID ============
    save()
    translate(800f, 560f)
    rotate(14f)

    // Legs
    paint {
        color(GREEN)
        style(RcPaintStyle.Stroke)
        strokeWidth(76f)
        strokeCap(RcStrokeCap.Round)
    }
    drawLine(-100f, 310f, -150f, 660f)
    drawLine(100f, 310f, 50f, 660f)

    // Arms
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

    // Torso
    paint {
        color(GREEN)
        style(RcPaintStyle.Fill)
    }
    drawPath(paths.droidTorso)

    // Belt
    paint {
        color(WHITE)
        style(RcPaintStyle.Fill)
    }
    drawRect(-290f, 285f, 290f, 340f)

    // Antennae
    paint {
        color(GREEN)
        style(RcPaintStyle.Stroke)
        strokeWidth(30f)
        strokeCap(RcStrokeCap.Round)
    }
    drawLine(-152f, -247f, -198f, -345f)
    drawLine(152f, -247f, 198f, -345f)

    // Head/body seam
    paint {
        color(WHITE)
        style(RcPaintStyle.Fill)
    }
    drawRect(-290f, -2f, 290f, 34f)

    // Head
    paint {
        color(GREEN)
        style(RcPaintStyle.Fill)
    }
    drawPath(paths.droidHead)

    // Eyes
    paint {
        color(WHITE)
        style(RcPaintStyle.Fill)
    }
    drawCircle(-131f, -142f, 30f)
    drawCircle(131f, -142f, 30f)
    restore()

    restore()
}

// ---- palette -----------------------------------------------------------
private const val GREEN = 0xFF72BD5A.toInt()
private const val WHITE = 0xFFFFFFFF.toInt()
private const val GRAY_LIGHT = 0xFFDADCE0.toInt()
private const val GRAY_MID = 0xFF9AA0A6.toInt()
private const val GRAY_DARK = 0xFF5F6368.toInt()
private const val BLUE = 0xFF1A73E8.toInt()
private const val YELLOW = 0xFFFBBC04.toInt()
private const val ORANGE = 0xFFE8710A.toInt()

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
fun DslFlappyDroidPreview() {
    RemoteDocumentPreview(RemoteDocument(dslFlappyDroid()))
}
