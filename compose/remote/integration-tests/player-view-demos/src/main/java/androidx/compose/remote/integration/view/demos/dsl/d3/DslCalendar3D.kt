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
@file:SuppressLint("PrimitiveInCollection")

package androidx.compose.remote.integration.view.demos.dsl.d3

import android.annotation.SuppressLint
import androidx.compose.remote.creation.ExperimentalRemoteCreationApi
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcCanvasScope
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcHaptic
import androidx.compose.remote.creation.dsl.RcHeaderTag
import androidx.compose.remote.creation.dsl.RcImage
import androidx.compose.remote.creation.dsl.RcMesh
import androidx.compose.remote.creation.dsl.RcMeshMode
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcScene3DScope
import androidx.compose.remote.creation.dsl.RcTouchStopMode
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.perspectiveCameraDeg
import androidx.compose.remote.creation.dsl.primitiveExtrudedRoundedRect
import androidx.compose.remote.creation.dsl.primitivePlane
import androidx.compose.remote.creation.dsl.remoteBitmap
import androidx.compose.remote.creation.dsl.touchPosY
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.integration.view.demos.dsl.kit.Textures3D
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import java.util.Calendar
import java.util.GregorianCalendar

private const val WEEKS = 52
private const val DAYS = WEEKS * 7

// Layout sizes — 50% larger than the first pass for a bolder, closer view.
private const val CELL = 1.62f // grid spacing (world units) in both the day and week directions
private const val TILE_W = 1.38f
private const val TILE_H = 1.38f
private const val CORNER = 0.3f
private const val NORMAL_DEPTH = 0.24f // a free day is a thin tile
private const val BUSY_DEPTH = 0.93f // a busy day stands taller (pops out toward the viewer)

// The whole grid is one board tipped back about X: increasing week → up (+Y) AND away (−Z), so the
// future recedes up into the distance. The tile front faces (with the date) tip toward the camera.
private const val BOARD_TILT = -1.02f // ~ −58°

private const val BG = 0xFF0B0F18L.toInt()
private const val COL_WEEKDAY = 0xFFC2CCDAL.toInt() // Mon–Fri: light slate
private const val COL_SUNDAY = 0xFFE0876EL.toInt() // Sun: warm red
private const val COL_SATURDAY = 0xFF6E97E0L.toInt() // Sat: cool blue
private const val COL_PAST = 0x55899099L.toInt() // days before today: translucent gray (alpha ~33%)
private const val COL_PAST_DIGIT = 0x66FFFFFFL.toInt() // faded numerals on past days
private const val COL_DIGIT =
    0xFF141A26L.toInt() // date numerals (near-black, drawn into the digit textures)

/**
 * A free, unbounded **vertical scroll** value (world units) driven by vertical drag, with momentum.
 * Same `addTouch` recipe as [verticalSpinAngle] but on the Y axis and unclamped — pulling up/down
 * slides the calendar through time.
 */
@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.verticalScroll(worldPerPixel: Float): RcFloat =
    addTouch(
        defaultValue = 0f,
        range = -100_000f..100_000f,
        stopMode = RcTouchStopMode.Gently,
        expression = touchPosY(),
        velocity = 0f,
        notchHaptic = RcHaptic.NoHaptics,
        notches = null,
        easing = null,
    ) * worldPerPixel

/** Stable pseudo-random "is this day busy?" so the layout is the same every build. */
private fun isBusy(pos: Int): Boolean {
    var h = pos * 374761393 + 668265263
    h = (h xor (h ushr 13)) * 1274126177
    return (h ushr 5) and 0x3 == 0 // ~25% of days are busy
}

/**
 * A **3D year calendar**. Each day is a rounded-rect tile; a left-to-right row is one week; time
 * runs **up and into the distance**, so a full year of 52 weeks climbs away toward the future. Rows
 * are laid out exactly like [ShaderCalendar.month] computes them — `week = pos / 7`, `dayOfWeek =
 * pos % 7` from the weekday offset of the year's first day — except the **columns are reversed**
 * (`col = 6 − dayOfWeek`, so Saturday is on the left). **Busy days stand taller** than free days;
 * **Saturdays and Sundays are colored differently** from weekdays. Each day shows its date, drawn
 * by placing the reusable **digit textures (0–9)** on the tile's front face (one quad per numeral,
 * transparent background so only the glyph shows). Drag up/down to move through time.
 */
@Suppress("RestrictedApiAndroidX")
public fun calendar3D(mode: RcMeshMode): ByteArray {
    // --- Compute the calendar grid (mirrors ShaderCalendar.month's row/offset math) ---
    val now: Calendar = GregorianCalendar()
    val year = now.get(Calendar.YEAR)
    val grid: Calendar = GregorianCalendar()
    grid.clear()
    grid.set(year, Calendar.JANUARY, 1)
    // Weekday of Jan 1 (0 = Sunday) — the same "offset" ShaderCalendar uses to place day 1.
    val offset = grid.get(Calendar.DAY_OF_WEEK) - 1
    // Back up to the Sunday that starts the grid, so pos % 7 == day-of-week directly.
    grid.add(Calendar.DAY_OF_MONTH, -offset)
    val gridStartMs = grid.timeInMillis
    // Today's grid position, so the current week can be the anchored "present" row and earlier days
    // can be drawn as past. Compare at local midnight (+half a day to absorb DST hour shifts).
    val todayMid: Calendar = GregorianCalendar()
    todayMid.clear()
    todayMid.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH))
    val todayPos = ((todayMid.timeInMillis - gridStartMs + 43_200_000L) / 86_400_000L).toInt()
    val currentWeek = todayPos / 7
    val dom = IntArray(DAYS)
    for (pos in 0 until DAYS) {
        dom[pos] = grid.get(Calendar.DAY_OF_MONTH)
        grid.add(Calendar.DAY_OF_MONTH, 1)
    }

    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        RcHeaderTag.docWidth(1000),
        RcHeaderTag.docHeight(1000),
        RcHeaderTag.contentDescription("3D Calendar"),
    ) {
        val digitTex: List<RcImage> =
            (0..9).map { remoteBitmap(Textures3D.digit(it, color = COL_DIGIT)) }
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(BG)) {
                paint { color(0xFFFFFFFF.toInt()) }
                // Pull up/down to travel through time (future is up the board, in the distance).
                val scroll = verticalScroll(0.045f)
                scene3D {
                    val normalTile =
                        primitiveExtrudedRoundedRect(
                            1,
                            TILE_W,
                            TILE_H,
                            CORNER,
                            NORMAL_DEPTH,
                            segments = 3f,
                        )
                    val busyTile =
                        primitiveExtrudedRoundedRect(
                            2,
                            TILE_W,
                            TILE_H,
                            CORNER,
                            BUSY_DEPTH,
                            segments = 3f,
                        )
                    val digitQuad = primitivePlane(3, 1f, 1f)

                    // Framed so the current week (relative week 0) sits full-width across the lower
                    // third of the screen, with the future climbing away into the distance above
                    // it.
                    perspectiveCameraDeg(
                        fovYDeg = 50f,
                        aspect = 1f,
                        near = 0.5f,
                        far = 120f,
                        eye = vec(RcFloat(0f), RcFloat(4.0f), RcFloat(11.2f)),
                        center = vec(RcFloat(0f), RcFloat(8.5f), RcFloat(0.6f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )

                    clearDepth()
                    clearLights()
                    directionalLight(
                        direction = vec(RcFloat(-0.3f), RcFloat(-0.55f), RcFloat(-0.75f)),
                        color = 0xFFFFFFFF.toInt(),
                        intensity = RcFloat(1.0f),
                    )
                    directionalLight(
                        direction = vec(RcFloat(0.4f), RcFloat(0.4f), RcFloat(0.5f)),
                        color = 0xFF9FB0D0.toInt(),
                        intensity = RcFloat(0.5f),
                    )

                    // Draw the days FAR → NEAR (week 51 first, present last). The software backend
                    // is
                    // depth-buffered so order is moot, but this painter order also makes the
                    // depth-buffer-less canvas backend composite correctly (nearer weeks over
                    // farther ones). Each cell draws its tile, then the date digits just in front.
                    val baseDigW = 0.3f
                    val baseDigH = 0.375f
                    for (week in WEEKS - 1 downTo 0) {
                        val relWeek = week - currentWeek // 0 = this week, + future (up), − past
                        for (dow in 0..6) {
                            val pos = week * 7 + dow
                            val col = dow // days run left → right (Sun … Sat)
                            val past = pos < todayPos
                            val busy = isBusy(pos)
                            val depth = if (busy) BUSY_DEPTH else NORMAL_DEPTH
                            val lx = (col - 3) * CELL
                            // Anchor the current week at the front (relWeek 0); scroll slides time.
                            val ly: RcFloat = scroll * -1f + (relWeek * CELL)

                            // The day tile — past days are a translucent gray ghost.
                            texture(null)
                            color(
                                if (past) {
                                    COL_PAST
                                } else {
                                    when (dow) {
                                        0 -> COL_SUNDAY
                                        6 -> COL_SATURDAY
                                        else -> COL_WEEKDAY
                                    }
                                }
                            )
                            identity()
                            rotate(BOARD_TILT, 1f, 0f, 0f)
                            // back face on the board, front pops out toward the viewer
                            translate(RcFloat(lx), ly, RcFloat(depth / 2f))
                            drawMesh(if (busy) busyTile else normalTile, mode = mode)

                            // The date numerals. They grow from left→right (col) and bottom→top
                            // (relWeek), so distant/future dates stay legible against perspective.
                            val sizeMul = (1f + 0.06f * col + 0.05f * relWeek).coerceIn(0.55f, 2.2f)
                            val digW = baseDigW * sizeMul
                            val digH = baseDigH * sizeMul
                            val digDx = 0.17f * sizeMul
                            color(if (past) COL_PAST_DIGIT else 0xFFFFFFFF.toInt())
                            val frontZ = depth + 0.012f
                            val n = dom[pos]
                            val tens = n / 10
                            val ones = n % 10
                            if (tens > 0) {
                                placeDigit(
                                    digitTex[tens],
                                    digitQuad,
                                    lx - digDx,
                                    ly,
                                    frontZ,
                                    digW,
                                    digH,
                                    mode,
                                )
                                placeDigit(
                                    digitTex[ones],
                                    digitQuad,
                                    lx + digDx,
                                    ly,
                                    frontZ,
                                    digW,
                                    digH,
                                    mode,
                                )
                            } else {
                                placeDigit(
                                    digitTex[ones],
                                    digitQuad,
                                    lx,
                                    ly,
                                    frontZ,
                                    digW,
                                    digH,
                                    mode,
                                )
                            }
                        }
                    }
                }

                paint {
                    color(0xFFE9ECF2.toInt())
                    textSize(34f)
                }
                drawTextAnchored(remoteText("$year — pull to travel in time"), 500f, 54f, 0f, 0f)
            }
        }
    }
}

/** Draw one digit [tex] as a small textured quad at board-local ([lx],[ly],[z]), sized [w]×[h]. */
@Suppress("RestrictedApiAndroidX")
private fun RcScene3DScope.placeDigit(
    tex: RcImage,
    quad: RcMesh,
    lx: Float,
    ly: RcFloat,
    z: Float,
    w: Float,
    h: Float,
    mode: RcMeshMode,
) {
    texture(tex)
    identity()
    rotate(BOARD_TILT, 1f, 0f, 0f)
    translate(RcFloat(lx), ly, RcFloat(z))
    scale(w, h, 1f)
    drawMesh(quad, mode = mode)
}

@Suppress("RestrictedApiAndroidX")
public fun calendar3DSoftware(): ByteArray = calendar3D(RcMeshMode.SoftwareSmooth)

// Canvas (GPU drawVertices) backend: no depth buffer, but the far→near draw order composites the
// rows correctly — and it's far faster than the CPU rasterizer for this many tiles, so scrolling
// stays smooth.
@Suppress("RestrictedApiAndroidX")
public fun calendar3DCanvas(): ByteArray = calendar3D(RcMeshMode.CanvasSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun Calendar3DPreview() {
    RemoteDocumentPreview(RemoteDocument(calendar3DSoftware()))
}
