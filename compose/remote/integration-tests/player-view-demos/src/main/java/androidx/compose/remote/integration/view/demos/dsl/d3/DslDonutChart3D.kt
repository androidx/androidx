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
@file:OptIn(ExperimentalRemoteCreationApi::class)
@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.integration.view.demos.dsl.d3

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import androidx.compose.remote.creation.ExperimentalRemoteCreationApi
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcHeaderTag
import androidx.compose.remote.creation.dsl.RcMeshMode
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.perspectiveCameraDeg
import androidx.compose.remote.creation.dsl.primitiveExtrudedArc
import androidx.compose.remote.creation.dsl.primitivePlane
import androidx.compose.remote.creation.dsl.remoteBitmap
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

private const val PI = Math.PI.toFloat()
private const val HALF_PI = (Math.PI / 2.0).toFloat()

private const val BG = 0xFFFFFFFF.toInt() // page background (the infographic is on white)
private const val CARD_W = 3.2f // world size of the (square) card plane

// Slices in clockwise order from the top: Mercury (red), Venus (amber), Earth (green), Jupiter
// (blue).
// Values/colors/labels match the "3D Pie Charts Infographics" reference (Google brand palette).
private val DONUT_VALUES = floatArrayOf(15f, 45f, 25f, 15f)
private val DONUT_COLORS =
    intArrayOf(
        0xFFEA4335.toInt(), // red   — Mercury
        0xFFFBBC05.toInt(), // amber — Venus
        0xFF34A853.toInt(), // green — Earth
        0xFF4285F4.toInt(), // blue  — Jupiter
    )
private val DONUT_LABELS = arrayOf("Mercury", "Venus", "Earth", "Jupiter")

private const val DONUT_INNER = 0.34f
private const val DONUT_OUTER = 0.78f
private const val DONUT_DEPTH = 0.30f

/**
 * Slice start/sweep angles (radians), first slice starting at the top (-90°) and going clockwise.
 */
private fun sliceAngles(): Pair<FloatArray, FloatArray> {
    val total = DONUT_VALUES.sum()
    val starts = FloatArray(DONUT_VALUES.size)
    val sweeps = FloatArray(DONUT_VALUES.size)
    var ang = -HALF_PI
    for (i in DONUT_VALUES.indices) {
        val sw = 2f * PI * DONUT_VALUES[i] / total
        starts[i] = ang
        sweeps[i] = sw
        ang += sw
    }
    return starts to sweeps
}

/**
 * White card texture with a **Type-A baked soft shadow**: the donut's contact shadow is blurred and
 * offset by the implied light straight into the card's opaque texels, so it costs nothing at render
 * time and composites perfectly (it *is* the card surface). The hole is carved back out so no
 * shadow pools under the empty center. World (x,z) maps as `px = (x/CARD_W + 0.5)*s`.
 */
private fun cardBitmap(): Bitmap {
    val s = 512
    val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(bmp)
    c.drawColor(BG)

    fun toPx(x: Float) = (x / CARD_W + 0.5f) * s
    val rOuter = DONUT_OUTER / CARD_W * s
    val rInner = DONUT_INNER / CARD_W * s
    val shadow =
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = 0x55000000 // ~33% black → soft gray on the white card
            maskFilter = BlurMaskFilter(s * 0.045f, BlurMaskFilter.Blur.NORMAL) // soft penumbra
        }
    // Implied light from high front-left → shadow cast a touch toward the back-right.
    val lx = 0.1f
    val lz = 0.09f
    c.drawCircle(toPx(lx), toPx(lz), rOuter * 1.1f, shadow)
    // Carve the hole back out so the donut's center reads as empty card, not a shadow puddle.
    val erase =
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = BG
            maskFilter = BlurMaskFilter(s * 0.04f, BlurMaskFilter.Blur.NORMAL)
        }
    c.drawCircle(toPx(lx), toPx(lz), rInner * 0.92f, erase)
    return bmp
}

/** One side label: planet name, two-line blurb, percentage and the matching slice color. */
private class Label(
    val name: String,
    val pct: Int,
    val line1: String,
    val line2: String,
    val color: Int,
    val nameX: Float,
    val nameY: Float,
    val anchor: Float, // -1 = left-aligned, 1 = right-aligned
    val leaderX: Float, // where the leader line meets the chart
    val leaderY: Float,
)

/**
 * A 3D **donut chart on a white card** — a recreation of the "3D Pie Charts Infographics" slide.
 *
 * Each slice is an extruded ring sector ([primitiveExtrudedArc], so the chart has a hollow center)
 * laid flat on the card. The card is a textured [primitivePlane] whose texture has the soft donut
 * shadow baked in (Type-A) — no per-frame shadow work. A 2D overlay draws the title plus the four
 * planet labels (icon dot, name, two-line blurb, percentage) with leader lines, just like the
 * slide. Drag horizontally to spin the chart.
 */
@Suppress("RestrictedApiAndroidX")
public fun donutChart3D(mode: RcMeshMode): ByteArray {
    val (starts, sweeps) = sliceAngles()
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        RcHeaderTag.docWidth(1000),
        RcHeaderTag.docHeight(1000),
        RcHeaderTag.contentDescription("3D Donut Chart"),
    ) {
        val cardTex = remoteBitmap(cardBitmap())
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(BG)) {
                paint { color(0xFFFFFFFF.toInt()) }
                // Drag horizontally to spin the donut (card + baked shadow turn with it); flick to
                // coast.
                val spinY = verticalSpinAngle()
                scene3D {
                    val slices =
                        Array(DONUT_VALUES.size) { i ->
                            primitiveExtrudedArc(
                                i + 1,
                                DONUT_INNER,
                                DONUT_OUTER,
                                starts[i],
                                sweeps[i],
                                DONUT_DEPTH,
                            )
                        }
                    val card = primitivePlane(100, CARD_W, CARD_W)

                    perspectiveCameraDeg(
                        fovYDeg = 33f,
                        aspect = 1f,
                        near = 0.1f,
                        far = 100f,
                        eye = vec(RcFloat(0f), RcFloat(3.5f), RcFloat(2.95f)),
                        center = vec(RcFloat(0f), RcFloat(0f), RcFloat(0.1f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )

                    clearDepth()
                    clearLights()
                    // Mostly top-down key so the slice tops read; soft fill from the front-left.
                    directionalLight(
                        direction = vec(RcFloat(-0.25f), RcFloat(-0.92f), RcFloat(-0.3f)),
                        color = 0xFFFFFFFF.toInt(),
                        intensity = RcFloat(0.85f),
                    )
                    directionalLight(
                        direction = vec(RcFloat(0.4f), RcFloat(-0.25f), RcFloat(0.55f)),
                        color = 0xFFE6ECF5.toInt(),
                        intensity = RcFloat(0.4f),
                    )

                    // White card with the baked shadow, lying flat (normal +Y), just under the
                    // donut.
                    color(0xFFFFFFFF.toInt())
                    texture(cardTex)
                    spunIdentity(spinY)
                    translate(0f, -0.004f, 0f)
                    rotate(angleRad = -HALF_PI, ax = 1f, ay = 0f, az = 0f)
                    drawMesh(card, mode = mode)
                    texture(null)

                    // Slices: laid flat, bottom resting on the card (y=0).
                    for (i in slices.indices) {
                        color(DONUT_COLORS[i])
                        spunIdentity(spinY)
                        translate(0f, DONUT_DEPTH * 0.5f, 0f)
                        rotate(angleRad = -HALF_PI, ax = 1f, ay = 0f, az = 0f)
                        drawMesh(slices[i], mode = mode)
                    }
                }

                // ---- 2D overlay: title + planet labels with leader lines ----
                paint {
                    color(0xFF222831.toInt())
                    textSize(52f)
                }
                drawTextAnchored(remoteText("3D Pie Charts Infographics"), 500f, 60f, 0f, 0f)

                // Mercury=red, Venus=amber, Earth=green, Jupiter=blue (indices into DONUT_*).
                val labels =
                    arrayOf(
                        Label(
                            DONUT_LABELS[0],
                            DONUT_VALUES[0].toInt(),
                            "Mercury is the smallest",
                            "planet in the Solar System",
                            DONUT_COLORS[0],
                            60f,
                            210f,
                            -1f,
                            360f,
                            360f,
                        ),
                        Label(
                            DONUT_LABELS[3],
                            DONUT_VALUES[3].toInt(),
                            "Jupiter is the biggest",
                            "planet in the Solar System",
                            DONUT_COLORS[3],
                            60f,
                            590f,
                            -1f,
                            360f,
                            560f,
                        ),
                        Label(
                            DONUT_LABELS[1],
                            DONUT_VALUES[1].toInt(),
                            "Venus is the second",
                            "planet from the Sun",
                            DONUT_COLORS[1],
                            940f,
                            210f,
                            1f,
                            640f,
                            360f,
                        ),
                        Label(
                            DONUT_LABELS[2],
                            DONUT_VALUES[2].toInt(),
                            "Earth is the third planet",
                            "and is where we all live on",
                            DONUT_COLORS[2],
                            940f,
                            590f,
                            1f,
                            640f,
                            560f,
                        ),
                    )

                for (lab in labels) {
                    // Leader line: a short horizontal stub at the label, then an elbow to the
                    // chart.
                    paint {
                        color(0xFF222831.toInt())
                        style(RcPaintStyle.Stroke)
                        strokeWidth(2f)
                    }
                    val stubX = if (lab.anchor < 0f) lab.nameX + 260f else lab.nameX - 260f
                    drawLine(lab.nameX, lab.nameY + 16f, stubX, lab.nameY + 16f)
                    drawLine(stubX, lab.nameY + 16f, lab.leaderX, lab.leaderY)

                    // Color dot (the slide's small icon), name, percentage and the two blurb lines.
                    paint { color(lab.color) }
                    val dotX = if (lab.anchor < 0f) lab.nameX + 14f else lab.nameX - 14f
                    drawCircle(dotX, lab.nameY - 46f, 18f)

                    paint {
                        color(0xFF222831.toInt())
                        textSize(40f)
                    }
                    drawTextAnchored(remoteText(lab.name), lab.nameX, lab.nameY, lab.anchor, 0f)

                    paint {
                        color(0xFF5A6470.toInt())
                        textSize(26f)
                    }
                    drawTextAnchored(
                        remoteText(lab.line1),
                        lab.nameX,
                        lab.nameY + 44f,
                        lab.anchor,
                        0f,
                    )
                    drawTextAnchored(
                        remoteText(lab.line2),
                        lab.nameX,
                        lab.nameY + 78f,
                        lab.anchor,
                        0f,
                    )

                    paint {
                        color(lab.color)
                        textSize(34f)
                    }
                    val pctX = if (lab.anchor < 0f) stubX + 24f else stubX - 24f
                    drawTextAnchored(
                        remoteText("${lab.pct}%"),
                        pctX,
                        lab.nameY - 8f,
                        lab.anchor,
                        0f,
                    )
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
public fun donutChart3DSoftware(): ByteArray = donutChart3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun DonutChart3DPreview() {
    RemoteDocumentPreview(RemoteDocument(donutChart3DSoftware()))
}
