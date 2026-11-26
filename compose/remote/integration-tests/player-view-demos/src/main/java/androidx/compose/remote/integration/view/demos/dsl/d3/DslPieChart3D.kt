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
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.perspectiveCameraDeg
import androidx.compose.remote.creation.dsl.primitiveExtrudedSector
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

private const val BG = 0xFFE9EAEE.toInt() // surround behind the card
private const val CARD_W = 3.2f // world size of the (square) card plane

private val PIE_VALUES = floatArrayOf(27f, 23f, 19f, 16f, 15f)
private val PIE_COLORS =
    intArrayOf(
        0xFF4C7DF0.toInt(), // blue
        0xFF2BB6A3.toInt(), // teal
        0xFF8BC34A.toInt(), // green
        0xFFF6B73C.toInt(), // amber
        0xFFEF5E6B.toInt(), // red
    )
private val PIE_LABELS = arrayOf("Search", "Direct", "Social", "Email", "Other")
private const val EXPLODE_INDEX = 0
private const val PIE_RADIUS = 0.74f
private const val PIE_DEPTH = 0.26f
private const val EXPLODE = 0.16f

/**
 * Slice start/sweep angles (radians), laid so the exploded slice's bisector points to +Z (front).
 */
private fun sliceAngles(): Pair<FloatArray, FloatArray> {
    val total = PIE_VALUES.sum()
    val starts = FloatArray(PIE_VALUES.size)
    val sweeps = FloatArray(PIE_VALUES.size)
    val sweepExp = 2f * PI * PIE_VALUES[EXPLODE_INDEX] / total
    // Rotate the whole pie so slice EXPLODE_INDEX is centered on -90° (→ +Z after lay-flat).
    var ang = -HALF_PI - sweepExp * 0.5f
    for (i in PIE_VALUES.indices) {
        val sw = 2f * PI * PIE_VALUES[i] / total
        starts[i] = ang
        sweeps[i] = sw
        ang += sw
    }
    return starts to sweeps
}

/**
 * Explode offset (world x,z) for a slice, along its bisector; (0,0) unless it is the pulled slice.
 */
private fun explodeXZ(i: Int, starts: FloatArray, sweeps: FloatArray): Pair<Float, Float> {
    if (i != EXPLODE_INDEX) return 0f to 0f
    val mid = starts[i] + sweeps[i] * 0.5f
    // After rotate(-90° about X): profile (cos,sin) → world (cos, 0, -sin).
    return (EXPLODE * Math.cos(mid.toDouble()).toFloat()) to
        (-EXPLODE * Math.sin(mid.toDouble()).toFloat())
}

/**
 * The white card texture with a **Type-A baked soft shadow**: the pie's contact shadow is drawn
 * (blurred, offset by the implied light) straight into the card's opaque texels, so it costs
 * nothing at render time and composites perfectly (it *is* the card surface). World (x,z) maps to
 * bitmap as `px = (x/CARD_W + 0.5)*s`, `py = (z/CARD_W + 0.5)*s`.
 */
private fun cardBitmap(starts: FloatArray, sweeps: FloatArray): Bitmap {
    val s = 512
    val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(bmp)
    c.drawColor(BG)
    // The white card itself (rounded rect, near-full bleed).
    val m = s * 0.06f
    val card = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
    c.drawRoundRect(m, m, s - m, s - m, s * 0.06f, s * 0.06f, card)

    fun toPx(x: Float) = (x / CARD_W + 0.5f) * s
    val rPx = PIE_RADIUS / CARD_W * s
    val shadow =
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = 0x66000000 // ~40% black → soft gray on the white card
            maskFilter = BlurMaskFilter(s * 0.05f, BlurMaskFilter.Blur.NORMAL) // soft penumbra
        }
    // Implied light from high front-left → shadow cast a little toward the back-right, and a touch
    // larger than the pie so a soft penumbra halo rings it on the card.
    val lx = 0.12f
    val lz = 0.1f
    // Main pie-body contact shadow.
    c.drawCircle(toPx(lx), toPx(lz), rPx * 1.16f, shadow)
    // The pulled-out slice carries its own shadow patch out with it.
    val (ex, ez) = explodeXZ(EXPLODE_INDEX, starts, sweeps)
    c.drawCircle(toPx(ex + lx), toPx(ez + lz), rPx * 0.62f, shadow)
    return bmp
}

/**
 * A 3D **pie chart on a white card** with one slice pulled out, using Type-A (baked) soft shadows.
 *
 * Each slice is an extruded sector ([primitiveExtrudedSector]) laid flat on the card; one slice is
 * offset along its bisector (exploded). The card is a textured [primitivePlane] whose texture has
 * the soft pie shadow baked in — no per-frame shadow work, no engine change. 2D title + legend
 * overlay on top.
 */
@Suppress("RestrictedApiAndroidX")
public fun pieChart3D(mode: RcMeshMode): ByteArray {
    val (starts, sweeps) = sliceAngles()
    val total = PIE_VALUES.sum()
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        RcHeaderTag.docWidth(1000),
        RcHeaderTag.docHeight(1000),
        RcHeaderTag.contentDescription("3D Pie Chart"),
    ) {
        val cardTex = remoteBitmap(cardBitmap(starts, sweeps))
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(BG)) {
                paint { color(0xFFFFFFFF.toInt()) }
                // Drag horizontally to spin the pie (card + baked shadow turn with it); flick to
                // coast.
                val spinY = verticalSpinAngle()
                scene3D {
                    val slices =
                        Array(PIE_VALUES.size) { i ->
                            primitiveExtrudedSector(
                                i + 1,
                                PIE_RADIUS,
                                starts[i],
                                sweeps[i],
                                PIE_DEPTH,
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
                        color = 0xFFDCE4F0.toInt(),
                        intensity = RcFloat(0.4f),
                    )

                    // White card with the baked shadow, lying flat (normal +Y), just under the pie.
                    color(0xFFFFFFFF.toInt())
                    texture(cardTex)
                    spunIdentity(spinY)
                    translate(0f, -0.004f, 0f)
                    rotate(angleRad = -HALF_PI, ax = 1f, ay = 0f, az = 0f)
                    drawMesh(card, mode = mode)
                    texture(null)

                    // Slices: laid flat, bottom resting on the card (y=0), exploded one offset.
                    for (i in slices.indices) {
                        val (ex, ez) = explodeXZ(i, starts, sweeps)
                        color(PIE_COLORS[i])
                        spunIdentity(spinY)
                        translate(ex, PIE_DEPTH * 0.5f, ez)
                        rotate(angleRad = -HALF_PI, ax = 1f, ay = 0f, az = 0f)
                        drawMesh(slices[i], mode = mode)
                    }
                }

                // ---- 2D overlay: title + legend ----
                paint {
                    color(0xFF222831.toInt())
                    textSize(44f)
                }
                drawTextAnchored(remoteText("Traffic by Source"), 500f, 78f, 0f, 0f)

                val lx = 808f
                var ly = 372f
                for (i in PIE_VALUES.indices) {
                    paint { color(PIE_COLORS[i]) }
                    drawCircle(lx, ly, 16f)
                    paint {
                        color(0xFF333A44.toInt())
                        textSize(30f)
                    }
                    val pct = Math.round(PIE_VALUES[i] / total * 100f)
                    drawTextAnchored(
                        remoteText("${PIE_LABELS[i]}   $pct%"),
                        lx + 34f,
                        ly,
                        -1f,
                        0f,
                    )
                    ly += 64f
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
public fun pieChart3DSoftware(): ByteArray = pieChart3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun PieChart3DPreview() {
    RemoteDocumentPreview(RemoteDocument(pieChart3DSoftware()))
}
