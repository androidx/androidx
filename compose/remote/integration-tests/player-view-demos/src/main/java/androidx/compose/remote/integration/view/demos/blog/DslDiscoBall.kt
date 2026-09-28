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
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.ifElse
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * A wireframe sphere that spins forever, written in the Remote Compose DSL.
 *
 * Everything that moves is an *expression*, not a value. `continuousSeconds()` is a dynamic
 * reference, so `spinTime * k + initialAngle`, its `sin`/`cos`, and the `ifElse` front/back test
 * all become an expression graph that ships inside the document and is re-evaluated by the player
 * on every frame. The creator emits `numVerticalLines` arcs exactly once; the host never calls
 * back.
 *
 * The geometry is a cheap orthographic projection of a unit sphere:
 * - Horizontal lines are latitude circles seen edge-on, so they collapse to straight chords whose
 *   half-width is `sqrt(1 - y^2)`. That is a compile-time constant per line, so plain Kotlin math
 *   is used and nothing dynamic is emitted for it.
 * - Vertical lines are longitude circles seen edge-on, so they project to ellipses. Each is drawn
 *   as a full-height arc whose *horizontal* radius shrinks to zero as the line rotates toward the
 *   poles of the projection, which is what sells the rotation.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslDiscoBall(): ByteArray {
    val sphereBaseColor = 0xFF34A853.toInt()
    val gridAccentColor = 0xFFE8F5E9.toInt()

    val numHorizontalLines = 10
    val numVerticalLines = 20
    val rotationPeriodSec = 24f

    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Disco Ball"),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(0xFFFFFFFF.toInt())) {
            // ============================================================
            // 1. SPHERE DIMENSIONS & BASE
            // ============================================================
            val width = componentWidth()
            val height = componentHeight()
            // flush() commits each expression to the document once and hands back a reference to
            // it, so the 30+ draw calls below share one evaluation instead of re-inlining the math.
            val centerX = (width * 0.5f).flush()
            val centerY = (height * 0.5f).flush()
            val sphereRadius = (min(width, height) * 0.4f).flush()

            // Solid green base sphere.
            paint {
                style(RcPaintStyle.Fill)
                color(sphereBaseColor)
            }
            drawCircle(centerX, centerY, sphereRadius)

            // Grid paint setup.
            paint {
                style(RcPaintStyle.Stroke)
                strokeWidth(2f)
                color(gridAccentColor)
            }

            // ============================================================
            // 2. STRAIGHT HORIZONTAL LINES
            // ============================================================
            val maxExtent = 0.85f
            val verticalStep = (2.0f * maxExtent) / (numHorizontalLines - 1)
            repeat(numHorizontalLines) { lineIndex ->
                val relativeY = -maxExtent + (lineIndex * verticalStep)

                // Pythagorean theorem to find the normalized radius (0.0 to 1.0) of the horizontal
                // cross-section. In unit coordinates, 1.0 represents 100% of the sphere's radius.
                val normalizedRadius = kotlin.math.sqrt(1.0f - relativeY * relativeY)
                val radiusPx = (sphereRadius * normalizedRadius).flush()
                val lineY = (centerY + (sphereRadius * relativeY)).flush()

                drawLine(centerX - radiusPx, lineY, centerX + radiusPx, lineY)
            }

            // ============================================================
            // 3. CURVED ROTATING VERTICAL LINES
            // Transform pipeline: Radians (Angle) -> Normalized (-1 to +1) -> Canvas Pixels
            //
            // Android Canvas angles:
            //             270° (Top / 12 o'clock)
            //                    ▲
            //                    │
            //  180° (Left) ◄─────┼─────► 0° (Right / 3 o'clock)
            //                    │
            //                    ▼
            //              90° (Bottom / 6 o'clock)
            // ============================================================
            val spinTime = continuousSeconds()
            val twoPi = 2.0f * Math.PI.toFloat() // 360° in radians

            repeat(numVerticalLines) { lineIndex ->
                // Used to space each vertical line evenly around the sphere.
                // Unit: Radians
                val initialAngle = lineIndex.toFloat() * (twoPi / numVerticalLines.toFloat())

                // The line's angle around the sphere's middle horizontal cross-section. Evaluated
                // dynamically on every frame by the player to rotate the line across the screen.
                // Unit: Radians
                val rotationAngle = (spinTime * (twoPi / rotationPeriodSec)) + initialAngle

                // Translates rotationAngle to screen position from -1.0 (left edge) to +1.0 (right
                // edge).
                // Unit: Unit Coordinates (-1.0 to +1.0)
                val relativeX = sin(rotationAngle)
                // Depth relative to camera (> 0 is front-facing, <= 0 is behind).
                val depthZ = cos(rotationAngle)

                // Scale normalized horizontal position (relativeX) into pixel offset (arcRadiusX)
                // from centerX. If facing front (depthZ > 0), arcRadiusX is the pixel distance from
                // centerX; if in back, collapses to 0.
                // Unit: Pixels
                val arcRadiusX = ifElse(depthZ, abs(relativeX) * sphereRadius, 0f.rf).flush()

                // Direction to trace the line from the top: +180° for the right side, -180° for the
                // left side.
                val sweepAngle = ifElse(relativeX, 180.0f.rf, (-180.0f).rf).flush()

                // Trace the curved vertical line from the top pole down to the bottom pole.
                drawArc(
                    centerX - arcRadiusX, // Left
                    centerY - sphereRadius, // Top
                    centerX + arcRadiusX, // Right
                    centerY + sphereRadius, // Bottom
                    270.0f.rf, // Start at top of the sphere
                    sweepAngle, // Draw left (-180°) or right (+180°) causing curve
                )
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
fun DslDiscoBallPreview() {
    RemoteDocumentPreview(RemoteDocument(dslDiscoBall()))
}
