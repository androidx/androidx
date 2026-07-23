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
import androidx.compose.remote.creation.dsl.ifElse
import androidx.compose.remote.creation.dsl.minus
import androidx.compose.remote.creation.dsl.onClick
import androidx.compose.remote.creation.profile.RcPlatformProfiles

/**
 * A Flappy Droid style interaction experiment using a particle system of one. The Android mascot
 * with a jetpack falls to the ground automatically. Touch and hold anywhere to activate jetpack
 * thrust. Navigate through the moving green pipes.
 */
@Suppress("RestrictedApiAndroidX")
fun dslGameFlappyDroid(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX)) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().onClick {}) {
                val w = componentWidth()
                val h = componentHeight()
                val t = continuousSeconds()

                // Setup Particle System of One for Android
                // Variables: [ y,  dy]
                val px = 160f.rf
                val variables = FloatArray(2)
                val pipeWidth = 80f.rf
                val pipeX = w - ((t * 130f) % (w + 100f))
                val gapY = h / 2f + sin(t * 0.8f) * 120f
                val gapHalf = 110f.rf

                // Draw background sky & clouds
                paint {
                    color(0xFF35A7FF.toInt()) // Sky blue
                    style(RcPaintStyle.Fill)
                }
                drawRect(0f.rf, 0f.rf, w, h)

                // Draw Pipes (green columns)
                paint {
                    color(0xFF4CAF50.toInt()) // Pipe green
                    style(RcPaintStyle.Fill)
                }
                // Upper pipe
                drawRect(pipeX, 0f.rf, pipeX + pipeWidth, gapY - gapHalf)
                // Lower pipe
                drawRect(pipeX, gapY + gapHalf, pipeX + pipeWidth, h)

                // ===================== ANDROID WITH JETPACK =============================

                // 1. Detect if currently touching using the time-delta trick
                val isTouching = sign(max(0f.rf, touchTime() - animationTime() + 0.15f))

                impulse(20000.rf, 0.rf) {
                    // =========== Create Paths ==========
                    val headDomePath = this@Canvas.remotePath(-14f, -4f)
                    headDomePath.quadTo(0f, -20f, 14f, -4f)
                    headDomePath.close()

                    val leftAntennaPath = this@Canvas.remotePath(-7f, -12f)
                    leftAntennaPath.lineTo(-12f, -20f)

                    val rightAntennaPath = this@Canvas.remotePath(7f, -12f)
                    rightAntennaPath.lineTo(12f, -20f)

                    val bodyTorsoPath = this@Canvas.remotePath(-14f, -2f)
                    bodyTorsoPath.lineTo(14f, -2f)
                    bodyTorsoPath.lineTo(14f, 12f)
                    bodyTorsoPath.quadTo(14f, 16f, 10f, 16f)
                    bodyTorsoPath.lineTo(-10f, 16f)
                    bodyTorsoPath.quadTo(-14f, 16f, -14f, 12f)
                    bodyTorsoPath.close()

                    val jetpackBodyPath = this@Canvas.remotePath(-24f, -4f)
                    jetpackBodyPath.lineTo(-14f, -4f)
                    jetpackBodyPath.lineTo(-14f, 16f)
                    jetpackBodyPath.lineTo(-24f, 16f)
                    jetpackBodyPath.close()

                    val jetpackNozzlePath = this@Canvas.remotePath(-23f, 16f)
                    jetpackNozzlePath.lineTo(-15f, 16f)
                    jetpackNozzlePath.lineTo(-17f, 21f)
                    jetpackNozzlePath.lineTo(-21f, 21f)
                    jetpackNozzlePath.close()

                    val flameShapePath = this@Canvas.remotePath(-22f, 21f)
                    flameShapePath.lineTo(-16f, 21f)
                    flameShapePath.lineTo(-19f, 36f)
                    flameShapePath.close()

                    val headDome = headDomePath.getPath()
                    val leftAntenna = leftAntennaPath.getPath()
                    val rightAntenna = rightAntennaPath.getPath()
                    val bodyTorso = bodyTorsoPath.getPath()
                    val jetpackBody = jetpackBodyPath.getPath()
                    val jetpackNozzle = jetpackNozzlePath.getPath()
                    val flameShape = flameShapePath.getPath()
                    // =========== Create Paths ==========

                    val ps = createParticles(variables, arrayOf(h / 2f, 0f.rf), 1)
                    val py = RcFloat(variables[0])
                    val pdy = RcFloat(variables[1])
                    val dt = deltaTime()
                    impulseProcess() {
                        particlesLoop(
                            ps,
                            null,
                            arrayOf(
                                min(h, py + pdy * dt),
                                (pdy + dt * 900f) * (1f - isTouching) - isTouching * 200f,
                            ),
                        ) {
                            this@Canvas.drawAndroid(
                                px,
                                py,
                                isTouching,
                                headDome,
                                leftAntenna,
                                rightAntenna,
                                bodyTorso,
                                jetpackBody,
                                jetpackNozzle,
                                flameShape,
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
    isTouching: RcFloat,
    headDome: RcPath,
    leftAntenna: RcPath,
    rightAntenna: RcPath,
    bodyTorso: RcPath,
    jetpackBody: RcPath,
    jetpackNozzle: RcPath,
    flameShape: RcPath,
) {
    val t = continuousSeconds()
    this.save {
        translate(px, py)

        // Tilt forward slightly when diving, upward when thrusting
        val tiltAngle = ifElse(isTouching, (-15f).rf, 15f.rf)
        rotate(tiltAngle)

        // 1. Jetpack Thrust Flame (behind body)
        conditionalOperations(RcConditionOp.Gt, isTouching, 0f.rf) {
            paint {
                color(0xFFFF5722.toInt()) // Bright orange flame
                style(RcPaintStyle.Fill)
            }
            this@drawAndroid.save {
                val flamePulse = 1f.rf + sin(t * 35f) * 0.25f
                scale(flamePulse, flamePulse)
                drawPath(flameShape)
            }

            // Inner yellow core flame
            paint {
                color(0xFFFFEB3B.toInt()) // Yellow core
                style(RcPaintStyle.Fill)
            }
            this@drawAndroid.save {
                scale(0.6f.rf, 0.7f.rf)
                drawPath(flameShape)
            }
        }

        // Idle pilot light flame when falling/not touching
        conditionalOperations(RcConditionOp.Lte, isTouching, 0f.rf) {
            paint {
                color(0xFF2196F3.toInt()) // Blue pilot light
                style(RcPaintStyle.Fill)
            }
            this@drawAndroid.save {
                scale(0.4f.rf, 0.3f.rf)
                drawPath(flameShape)
            }
        }

        // 2. Jetpack Tank & Nozzle
        paint {
            color(0xFF505050.toInt()) // Metallic gray
            style(RcPaintStyle.Fill)
        }
        drawPath(jetpackBody)

        // Jetpack straps / detail
        paint {
            color(0xFF808080.toInt())
            style(RcPaintStyle.Fill)
        }
        drawRect(-22f, -2f, -14f, 0f)
        drawRect(-22f, 10f, -14f, 12f)

        // Jetpack Nozzle
        paint {
            color(0xFF282828.toInt()) // Dark nozzle
            style(RcPaintStyle.Fill)
        }
        drawPath(jetpackNozzle)

        // 3. Android Body (Torso)
        paint {
            color(0xFF3DDC84.toInt()) // Official Android Green
            style(RcPaintStyle.Fill)
        }
        drawPath(bodyTorso)

        // 4. Android Head Dome
        paint {
            color(0xFF3DDC84.toInt())
            style(RcPaintStyle.Fill)
        }
        drawPath(headDome)

        // 5. Antennas
        paint {
            color(0xFF3DDC84.toInt())
            style(RcPaintStyle.Stroke)
            strokeWidth(3f)
            strokeCap(RcStrokeCap.Round)
        }
        drawPath(leftAntenna)
        drawPath(rightAntenna)

        // 6. Eyes
        paint {
            color(0xFFFFFFFF.toInt()) // White eyes
            style(RcPaintStyle.Fill)
        }
        drawCircle(-5f.rf, -8f.rf, 2.5f.rf)
        drawCircle(5f.rf, -8f.rf, 2.5f.rf)

        // 7. Arms & Legs
        paint {
            color(0xFF3DDC84.toInt())
            style(RcPaintStyle.Fill)
        }
        // Left Arm (holding jetpack)
        drawRoundRect(-18f, 0f, -14f, 10f, 2f, 2f)
        // Right Arm (front/pointing forward)
        drawRoundRect(14f, 0f, 18f, 10f, 2f, 2f)
        // Legs
        drawRoundRect(-8f, 16f, -4f, 22f, 2f, 2f)
        drawRoundRect(4f, 16f, 8f, 22f, 2f, 2f)
    }
}
