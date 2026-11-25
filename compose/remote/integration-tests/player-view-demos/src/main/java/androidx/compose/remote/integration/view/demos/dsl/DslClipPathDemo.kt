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
import androidx.compose.remote.creation.dsl.RcColumnVerticalPositioning
import androidx.compose.remote.creation.dsl.RcFontWeight
import androidx.compose.remote.creation.dsl.RcHeaderTag
import androidx.compose.remote.creation.dsl.RcHorizontalPositioning
import androidx.compose.remote.creation.dsl.RcPathType
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcRowHorizontalPositioning
import androidx.compose.remote.creation.dsl.RcScope
import androidx.compose.remote.creation.dsl.RcShape
import androidx.compose.remote.creation.dsl.RcText
import androidx.compose.remote.creation.dsl.RcTextAlign
import androidx.compose.remote.creation.dsl.RcVerticalPositioning
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.clip
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.fillMaxWidth
import androidx.compose.remote.creation.dsl.onClick
import androidx.compose.remote.creation.dsl.padding
import androidx.compose.remote.creation.dsl.rsp
import androidx.compose.remote.creation.dsl.sin
import androidx.compose.remote.creation.dsl.size
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private const val SHAPE_SIZE = 220f
private const val TAU = (2.0 * Math.PI).toFloat()

/**
 * Demonstrates `Modifier.clip(RcShape.path(path))`, the experimental MODIFIER_CLIP_PATH operation.
 *
 * Three clickable boxes are clipped to a hexagon, a five-pointed star and a heart. The background
 * and the click ripple are confined to the shape because the clip modifier precedes them in the
 * modifier chain, exactly like `Modifier.clip(shape).clickable { }` in Compose. Tapping a shape
 * updates the header.
 *
 * A fourth box is clipped to a path expression that reads `continuousSeconds()`: the player
 * re-samples the path every frame, so the clip outline morphs over time. The clip modifier only
 * references the path by id and picks up the new geometry on each paint.
 *
 * Hit testing is still rectangular for now: a tap in the corner of a box, outside its shape, also
 * triggers the click. Shaped hit testing is tracked separately.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslClipPathDemo(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        RcHeaderTag.docWidth(800),
        RcHeaderTag.docHeight(820),
        RcHeaderTag.contentDescription("Clip Path Demo"),
    ) {
        val header = remoteNamedText("header", "Tap a shape")

        // Path coordinates are in the clipped component's local pixel space (0..SHAPE_SIZE).
        val hexagon = remotePathData(regularPolygon(sides = 6, rotationDegrees = 30f))
        val star = remotePathData(starPolygon(points = 5))
        val heart =
            remotePathData(
                "M110,205 C40,155 10,115 10,72 C10,38 38,12 70,12 C90,12 104,24 110,42 " +
                    "C116,24 130,12 150,12 C182,12 210,38 210,72 C210,115 180,155 110,205 Z"
            )

        Column(
            modifier = Modifier.fillMaxSize().background(0xFF181A20.toInt()).padding(24f),
            horizontal = RcHorizontalPositioning.Center,
            vertical = RcColumnVerticalPositioning.Center,
        ) {
            // A time-varying polar path, r(θ) = R·(0.7 + 0.18·sin(5θ + 1.3t) + 0.08·sin(3θ − 2.1t))
            // with t the continuous time; R·0.96 keeps it inside the SHAPE_SIZE square.
            // Declared inside the Column on purpose: a layout component re-applies its data
            // operations every frame, while operations at the top level of the document are only
            // applied once, so a path expression declared there would not animate.
            val t = continuousSeconds()
            val radius = SHAPE_SIZE / 2f
            val blob =
                remotePolarPath(
                    expression =
                        remoteFunction { th ->
                            (sin(th * 5f + t * 1.3f) * 0.18f +
                                sin(th * 3f - t * 2.1f) * 0.08f +
                                0.7f) * radius
                        },
                    start = 0f,
                    end = TAU,
                    count = 120,
                    centerX = radius.rf,
                    centerY = radius.rf,
                    type = RcPathType.Loop,
                )

            Text(
                text = header,
                fontSize = 40.rsp,
                fontWeight = RcFontWeight.Bold,
                color = 0xFFFFFFFF.toInt(),
                textAlign = RcTextAlign.Center,
                modifier = Modifier.padding(bottom = 40f),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontal = RcRowHorizontalPositioning.SpaceEvenly,
                vertical = RcVerticalPositioning.Center,
            ) {
                clippedButton(header, "Hexagon", RcShape.path(hexagon), 0xFF4CAF50.toInt())
                clippedButton(header, "Star", RcShape.path(star), 0xFFFFC107.toInt())
                clippedButton(header, "Heart", RcShape.path(heart), 0xFFE91E63.toInt())
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24f),
                horizontal = RcRowHorizontalPositioning.Center,
                vertical = RcVerticalPositioning.Center,
            ) {
                clippedButton(header, "Blob", RcShape.path(blob), 0xFF2196F3.toInt())
            }
            Text(
                text = "Background and ripple are clipped; the blob is re-sampled every frame",
                fontSize = 20.rsp,
                color = 0xFF9AA0A6.toInt(),
                textAlign = RcTextAlign.Center,
                modifier = Modifier.padding(top = 40f),
            )
        }
    }
}

/** A clickable box clipped to [shape]; the clip precedes the background and click modifiers. */
@Suppress("RestrictedApiAndroidX")
private fun RcScope.clippedButton(header: RcText, label: String, shape: RcShape, color: Int) {
    Box(
        modifier =
            Modifier.size(SHAPE_SIZE).clip(shape).background(color).onClick {
                setValue(header, "$label tapped")
            },
        horizontal = RcHorizontalPositioning.Center,
        vertical = RcVerticalPositioning.Center,
    ) {
        Text(
            text = label,
            fontSize = 24.rsp,
            fontWeight = RcFontWeight.Medium,
            color = 0xFF181A20.toInt(),
            textAlign = RcTextAlign.Center,
        )
    }
}

/** SVG path data for a regular polygon inscribed in the SHAPE_SIZE square. */
private fun regularPolygon(sides: Int, rotationDegrees: Float): String {
    val radius = SHAPE_SIZE / 2f
    return polygonPath(sides) { i ->
        val angle = Math.toRadians((rotationDegrees + 360f * i / sides).toDouble())
        Pair(radius + radius * cos(angle), radius + radius * sin(angle))
    }
}

/** SVG path data for a star with [points] points inscribed in the SHAPE_SIZE square. */
private fun starPolygon(points: Int): String {
    val outer = SHAPE_SIZE / 2f
    val inner = outer * 0.45f
    return polygonPath(points * 2) { i ->
        val radius = if (i % 2 == 0) outer else inner
        val angle = Math.toRadians((-90f + 180f * i / points).toDouble())
        Pair(outer + radius * cos(angle), outer + radius * sin(angle))
    }
}

private fun polygonPath(vertices: Int, vertex: (Int) -> Pair<Double, Double>): String {
    val sb = StringBuilder()
    for (i in 0 until vertices) {
        val (x, y) = vertex(i)
        sb.append(if (i == 0) "M" else "L").append(String.format(Locale.ROOT, "%.1f,%.1f ", x, y))
    }
    return sb.append("Z").toString()
}
