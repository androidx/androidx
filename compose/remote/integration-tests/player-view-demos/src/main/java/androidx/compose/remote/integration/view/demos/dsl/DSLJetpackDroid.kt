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

import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcCanvasScope
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcPath
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcStrokeCap
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.min
import androidx.compose.remote.creation.dsl.minus
import androidx.compose.remote.creation.profile.RcPlatformProfiles

@Suppress("RestrictedApiAndroidX")
fun dslJetpackDroid(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX)) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = componentWidth()
                val h = componentHeight()
                val t = continuousSeconds()

                // Register Paths
                val flameOrange =
                    this@Canvas.remotePathData(
                        "M -78,700 A 78 78 0 0 1 78,700 C 78,790 22,855 0,905 C -22,855 -78,790 -78,700 Z"
                            .toPathData()
                    )
                val flameYellow =
                    this@Canvas.remotePathData(
                        "M -46,712 A 46 46 0 0 1 46,712 C 46,772 14,820 0,855 C -14,820 -46,772 -46,712 Z"
                            .toPathData()
                    )
                val rocketBody =
                    this@Canvas.remotePathData(
                        "M 0,0 C -54,54 -124,158 -124,248 L -124,615 L 124,615 L 124,248 C 124,158 54,54 0,0 Z"
                            .toPathData()
                    )
                val rocketShade =
                    this@Canvas.remotePathData(
                        "M 0,0 C 54,54 124,158 124,248 L 124,615 L 0,615 Z".toPathData()
                    )
                val rocketNozzle =
                    this@Canvas.remotePathData(
                        "M -124,598 L 124,598 L 124,658 Q 124,690 92,690 L -92,690 Q -124,690 -124,658 Z"
                            .toPathData()
                    )
                val droidTorso =
                    this@Canvas.remotePathData(
                        "M -290,34 L 290,34 L 290,322 Q 290,370 242,370 L -242,370 Q -290,370 -290,322 Z"
                            .toPathData()
                    )
                val droidHead =
                    this@Canvas.remotePathData("M -290,0 A 290 290 0 0 1 290,0 Z".toPathData())

                // Sky blue background
                paint {
                    color(0xFF35A7FFL.toInt())
                    style(RcPaintStyle.Fill)
                }
                drawRect(0f.rf, 0f.rf, w, h)

                // Draw centered on screen, scaled so 1400x1400 fits screen width
                this@Canvas.save {
                    translate((w / 2f).toFloat(), (h / 2f).toFloat())
                    val scale = (min(w, h) / 1400f).toFloat()
                    scale(scale, scale)
                    translate(-700f, -700f)

                    // Draw the droid assembly exactly as in DslGameFlappyDroid
                    this@Canvas.drawAndroidTest(
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

private const val GREEN = 0xFF72BD5A.toInt()
private const val WHITE = 0xFFFFFFFF.toInt()
private const val GRAY_LIGHT = 0xFFDADCE0.toInt()
private const val GRAY_MID = 0xFF9AA0A6.toInt()
private const val GRAY_DARK = 0xFF5F6368.toInt()
private const val BLUE = 0xFF1A73E8.toInt()
private const val YELLOW = 0xFFFBBC04.toInt()
private const val ORANGE = 0xFFE8710A.toInt()

@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.drawAndroidTest(
    flameOrange: RcPath,
    flameYellow: RcPath,
    rocketBody: RcPath,
    rocketShade: RcPath,
    rocketNozzle: RcPath,
    droidTorso: RcPath,
    droidHead: RcPath,
) {
    val t = continuousSeconds()
    // ============ ROCKET (+ flame) ============
    this.save {
        translate(432f, 283f)
        rotate(16f)

        // 1. Thrust Flames (orange #E8710A and yellow #FBBC04)
        this.save {
            val flamePulse = 1f.rf + sin(t * 35f) * 0.25f
            translate(0f, 700f)
            scale(flamePulse, flamePulse)
            translate(0f, -700f)

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
    this.save {
        translate(800f, 560f)
        rotate(14f)

        // 1. Legs
        paint {
            color(GREEN)
            style(RcPaintStyle.Stroke)
            strokeWidth(76f)
            strokeCap(RcStrokeCap.Round)
        }
        drawLine(-140f, 310f, -202f, 675f)
        drawLine(-30f, 310f, 11f, 660f)

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
