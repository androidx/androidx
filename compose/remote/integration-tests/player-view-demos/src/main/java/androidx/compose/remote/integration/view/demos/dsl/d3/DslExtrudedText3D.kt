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
import android.graphics.Matrix
import android.graphics.Paint as AndroidPaint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.graphics.Typeface
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
import androidx.compose.remote.creation.dsl.primitiveExtrudedPath
import androidx.compose.remote.creation.dsl.primitivePlane
import androidx.compose.remote.creation.dsl.remoteBitmap
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

private const val HALF_PI = (Math.PI / 2.0).toFloat()

private const val TEXT = "Text"
private const val TEXT_COLOR = 0xFF4285F4.toInt() // a strong blue that pops on the white surface
private const val BG = 0xFFFFFFFF.toInt()

private const val CARD_W = 3.2f // world size of the (square) white floor
private const val TEXT_WIDTH = 1.8f // world width the text is scaled to
private const val TEXT_DEPTH = 0.18f // extrusion thickness (along Z, toward the camera)
private const val TEXT_BEVEL = 0.02f // chamfer on the cap↔wall edge (0 = sharp)
private const val STEP_WORLD = 0.01f // contour sampling step (smaller = smoother glyph outlines)
private const val BASE_SINK = 0.03f // how far the text baseline sits below the floor (anti z-fight)

// Baked drop shadow: a point of the standing text at height y projects onto the floor at
// z = SHADOW_K * y (negative → the shadow falls *behind* the text, away from the front camera),
// plus a small light offset. These are the knobs to tune the shadow against the lighting on device.
private const val SHADOW_K = -0.72f
private const val SHADOW_OFF_X = 0.05f
private const val SHADOW_OFF_Z = -0.04f

/**
 * Build the text as an [Path] (the `buildPathFromText` approach: [AndroidPaint.getTextPath]), then
 * transform it into world space: uniformly scaled to [TEXT_WIDTH] wide, **Y-flipped** so the glyphs
 * stand upright in a +Y-up frame, centered in X with the baseline at y = 0.
 */
private fun worldTextPath(): Path {
    val paint =
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.DEFAULT_BOLD
            textSize = 200f // large for a high-resolution outline; scaled to world below
        }
    val path = Path()
    paint.getTextPath(TEXT, 0, TEXT.length, 0f, 0f, path)

    val b = RectF()
    path.computeBounds(b, true)
    val scale = TEXT_WIDTH / b.width()
    // Scale + flip Y (text path is Y-down; the 3D scene is Y-up).
    path.transform(Matrix().apply { postScale(scale, -scale) })
    // Re-measure and place: center in X, lift so the lowest point sits at y = 0.
    path.computeBounds(b, true)
    path.transform(Matrix().apply { postTranslate(-(b.left + b.right) / 2f, -b.top) })
    return path
}

/**
 * Flatten a (possibly multi-contour) [path] into the list of closed polylines that
 * [primitiveExtrudedPath] consumes — one [FloatArray] of `x0,y0,x1,y1,…` per contour, evenly
 * sampled every ~[STEP_WORLD] world units, with no repeated closing point. Letter holes (the
 * counters of `e`/`d`) come out as their own contours and are resolved by even-odd fill on the
 * player.
 */
private fun pathContours(path: Path): List<FloatArray> {
    val out = ArrayList<FloatArray>()
    val pm = PathMeasure(path, false)
    val pos = FloatArray(2)
    do {
        val len = pm.length
        if (len > 1e-4f) {
            val steps = Math.max(3, Math.round(len / STEP_WORLD))
            val ring = FloatArray(steps * 2)
            for (i in 0 until steps) {
                pm.getPosTan(len * i / steps, pos, null)
                ring[i * 2] = pos[0]
                ring[i * 2 + 1] = pos[1]
            }
            out.add(ring)
        }
    } while (pm.nextContour())
    return out
}

/**
 * The white floor card with the text's **soft drop shadow** baked in (Type-A: drawn straight into
 * the opaque texels, so it costs nothing at render time). The standing text at world `(x, y)` is
 * projected to the floor at `(x, z = SHADOW_K * y)` — i.e. the same affine map the floor plane uses
 * to sample this bitmap, so the shadow lines up under the geometry. World `(x,z)` maps to the
 * bitmap as `px = (x/CARD_W + 0.5)*s`, matching the pie/donut card convention.
 */
private fun floorBitmap(textPath: Path): Bitmap {
    val s = 512
    val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(bmp)
    c.drawColor(BG)

    val sx = s / CARD_W
    // (x, y) → bitmap: col = sx*x + s/2 ; row = sx*(SHADOW_K*y) + s/2, plus the light offset.
    val m =
        Matrix().apply {
            setScale(sx, sx * SHADOW_K)
            postTranslate(0.5f * s + sx * SHADOW_OFF_X, 0.5f * s + sx * SHADOW_OFF_Z)
        }
    val shadowPath = Path(textPath).apply { transform(m) }
    val shadow =
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = 0x55000000 // ~33% black → soft gray on the white card
            maskFilter = BlurMaskFilter(s * 0.02f, BlurMaskFilter.Blur.NORMAL)
        }
    c.drawPath(shadowPath, shadow)
    return bmp
}

/**
 * Demo for the new **`EXTRUDE_PATH` mesh primitive**: an arbitrary 2D [Path] extruded into 3D. Here
 * the path is the glyph outline of "Text" (built with [AndroidPaint.getTextPath], the
 * `buildPathFromText` recipe), fed to [primitiveExtrudedPath]; the player triangulates the caps
 * (nonzero-winding fill, so letter counters/holes and overlapping bold contours both come out
 * right) and builds the side walls. The text stands on a white floor whose texture has a soft drop
 * shadow baked in. Drag horizontally to spin, vertically to tilt.
 */
@Suppress("RestrictedApiAndroidX")
public fun extrudedText3D(mode: RcMeshMode): ByteArray {
    val textPath = worldTextPath()
    val contours = pathContours(textPath)
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        RcHeaderTag.docWidth(1000),
        RcHeaderTag.docHeight(1000),
        RcHeaderTag.contentDescription("3D Extruded Text"),
    ) {
        val floorTex = remoteBitmap(floorBitmap(textPath))
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(BG)) {
                paint { color(0xFFFFFFFF.toInt()) }
                // Drag horizontally to spin, vertically to tilt; flick to coast.
                val spinY = verticalSpinAngle()
                val tiltX = verticalTiltAngle()
                scene3D {
                    val text = primitiveExtrudedPath(1, contours, TEXT_DEPTH, bevel = TEXT_BEVEL)
                    val floor = primitivePlane(2, CARD_W, CARD_W)

                    perspectiveCameraDeg(
                        fovYDeg = 33f,
                        aspect = 1f,
                        // Tight near/far bounding just this small scene: the 0.1..100 default
                        // wastes
                        // almost all depth-buffer precision (1000:1), which makes the text/floor
                        // contact seam z-fight. Everything here sits ~1.7..5.5 from the eye.
                        near = 1.2f,
                        far = 7f,
                        // Slightly off to the side + above so the extrusion depth reads at rest.
                        eye = vec(RcFloat(0.95f), RcFloat(0.85f), RcFloat(3.1f)),
                        center = vec(RcFloat(0f), RcFloat(0.32f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )

                    clearDepth()
                    clearLights()
                    // Key from the upper front-left (matches the baked shadow direction), soft
                    // fill.
                    directionalLight(
                        direction = vec(RcFloat(-0.35f), RcFloat(-0.7f), RcFloat(-0.6f)),
                        color = 0xFFFFFFFF.toInt(),
                        intensity = RcFloat(0.85f),
                    )
                    directionalLight(
                        direction = vec(RcFloat(0.5f), RcFloat(-0.25f), RcFloat(0.55f)),
                        color = 0xFFE6ECF5.toInt(),
                        intensity = RcFloat(0.4f),
                    )

                    // White floor with the baked drop shadow, lying flat (normal +Y) at y=0.
                    color(0xFFFFFFFF.toInt())
                    texture(floorTex)
                    orbitIdentity(spinY, tiltX)
                    rotate(angleRad = -HALF_PI, ax = 1f, ay = 0f, az = 0f)
                    drawMesh(floor, mode = mode)
                    texture(null)

                    // The extruded text. Its baseline is sunk slightly BELOW the floor so the
                    // letters'
                    // bottom faces (parallel to, and almost coincident with, the floor) are
                    // occluded by
                    // the opaque floor instead of z-fighting it — the contact line stays crisp.
                    color(TEXT_COLOR)
                    orbitIdentity(spinY, tiltX)
                    translate(0f, -BASE_SINK, 0f)
                    drawMesh(text, mode = mode)
                }

                // ---- 2D caption overlay ----
                paint {
                    color(0xFF222831.toInt())
                    textSize(34f)
                }
                drawTextAnchored(
                    remoteText("EXTRUDE_PATH primitive — text → 3D"),
                    500f,
                    60f,
                    0f,
                    0f,
                )
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
public fun extrudedText3DSoftware(): ByteArray = extrudedText3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun ExtrudedText3DPreview() {
    RemoteDocumentPreview(RemoteDocument(extrudedText3DSoftware()))
}
