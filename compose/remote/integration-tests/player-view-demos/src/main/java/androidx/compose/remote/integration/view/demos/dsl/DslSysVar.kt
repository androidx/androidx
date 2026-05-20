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

import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcScope
import androidx.compose.remote.creation.dsl.RcText
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.profile.RcPlatformProfiles

/**
 * DSL conversion of `examples/DemotSystemVar.kt`.
 *
 * Reads three player-injected system variables (background radius, inner radius, font weight) by
 * registering named floats with `system.*` prefixes, then draws labels and shapes using those
 * values. Exercises:
 * - `remoteNamedFloat("system.X", default)` for system-variable reading
 * - `debug(msg, value)` for player-side debug output
 * - `paint { color(...); style(...) }` typed paint scope
 * - `drawRoundRect(left: RcFloat, ..., radiusX: RcFloat, radiusY: RcFloat)` — animated rect
 * - `textMerge(RcText, RcText)` + `+` operator
 * - `drawTextAnchored(text, x: RcFloat, y: RcFloat, panX: RcFloat, panY: RcFloat, flags)`
 */
@Suppress("RestrictedApiAndroidX")
public fun dslSysVar(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Activity Rings"),
        experimental = true,
    ) {
        val rad = remoteNamedFloat("system.system_app_widget_background_radius", 32f)
        val innerRad = remoteNamedFloat("system.system_app_widget_inner_radius", 25f)
        val fontWeight = remoteNamedFloat("system.font_weight", 0f)

        debug("backgroundRadius ", rad)
        debug("innerRadius", innerRad)
        debug("fontWeight", fontWeight)

        Canvas(modifier = Modifier.fillMaxSize().background(0xFFAABBCC.toInt())) {
            val w = componentWidth()
            val h = componentHeight()
            val cx = w / 2f
            val cy = h / 2f

            paint {
                color(0xFF0000FF.toInt())
                style(androidx.compose.remote.creation.dsl.RcPaintStyle.Fill)
            }
            drawRoundRect(0f.rf, 0f.rf, w, h, rad, rad)

            paint {
                color(0xFFCCCCCC.toInt())
                style(androidx.compose.remote.creation.dsl.RcPaintStyle.Fill)
            }
            drawRoundRect(100f.rf, 100f.rf, w - 100f, h - 100f, rad, rad)

            paint {
                color(0xFF000000.toInt())
                textSize(55f)
            }
            printLine(cx, cy, -4f.rf, "backgroundRadius ", rad)
            printLine(cx, cy, 0f.rf, "innerRadius ", innerRad)
            printLine(cx, cy, 4f.rf, "fontWeight ", fontWeight)
        }
    }
}

/**
 * Helper that draws a labeled float value anchored at `(x, y)` with a vertical line offset
 * `lineOffset`. The label and value are merged into one text via [textMerge].
 */
@Suppress("RestrictedApiAndroidX")
private fun RcScope.printLine(
    x: RcFloat,
    y: RcFloat,
    lineOffset: RcFloat,
    label: String,
    value: RcFloat,
) {
    val valueText: RcText = createTextFromFloat(value, 7, 1, 0)
    val merged: RcText = remoteText("$label: ") + valueText
    drawTextAnchored(merged, x, y, 0f.rf, lineOffset, 0)
}
