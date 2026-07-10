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

import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.creation.dsl.*
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * A demo showcasing the newly added `axis(...)` paint properties on a variable font. An interactive
 * slider allows the user to dynamically transition the text's font axis ("wght") from Thin (100) to
 * Black (900) in real-time.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslFontAxisDemo(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        Column(
            modifier = Modifier.fillMaxSize().background(0xFF0F172A.toInt()).padding(32.rdp),
            horizontal = RcHorizontalPositioning.Center,
            vertical = RcColumnVerticalPositioning.Top,
        ) {
            // Title header
            Text(
                text = "Variable Font Axis Demo",
                weight = RcWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 36.rsp,
                textAlign = RcTextAlign.Center,
                color = 0xFFFFFFFF.toInt(),
            )

            Spacer(Modifier.height(24.rdp))

            // Slider label
            Text(
                text = "Drag slider to adjust Font Axis 'wght' (100 - 900)",
                weight = RcWeight.Medium,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 20.rsp,
                textAlign = RcTextAlign.Start,
                color = 0xFF94A3B8.toInt(),
            )

            Spacer(Modifier.height(12.rdp))

            // Canvas containing both the interactive slider and the variable font text
            Canvas(
                modifier =
                    Modifier.size(560.rdp, 360.rdp)
                        .background(0xFF1E293B.toInt())
                        .border(1.5f, 16f, 0xFF334155.rcColor(), RcBorderShape.RoundedRectangle)
            ) {
                val w = width
                val h = height

                val cx = w * 0.5f

                val left = 40f
                val right = w - 40f

                // Touch slider for controlling weight: range [100, 900]
                val wghtVal =
                    addTouch(
                        defaultValue = 400f,
                        range = 100f..900f,
                        stopMode = RcTouchStopMode.Gently,
                        velocity = 0f,
                        notchHaptic = RcHaptic.NoHaptics,
                        notches = null,
                        easing = null,
                        RemoteContext.FLOAT_TOUCH_POS_X,
                    )

                // Draw track for weight slider
                paint {
                    color(0xFF334155.toInt())
                    style(RcPaintStyle.Stroke)
                    strokeWidth(10f)
                    strokeCap(RcStrokeCap.Round)
                }
                drawLine(left.rf, 60f.rf, right, 60f.rf)

                // Map touch [100, 900] to screen coordinate x for drawing slider knob
                // x = left + (right - left) * (wghtVal - 100) / 800
                val knobX = left.rf + (right - left.rf) * ((wghtVal - 100f.rf) / 800f.rf)

                // Draw slider fill
                paint {
                    color(0xFF38BDF8.toInt()) // Light sky blue
                    style(RcPaintStyle.Stroke)
                    strokeWidth(10f)
                    strokeCap(RcStrokeCap.Round)
                }
                drawLine(left.rf, 60f.rf, knobX, 60f.rf)

                // Draw slider handle knob
                paint {
                    color(0xFF38BDF8.toInt())
                    style(RcPaintStyle.Fill)
                }
                drawCircle(knobX, 60f.rf, 20f.rf)

                paint {
                    color(0xFF1E293B.toInt())
                    style(RcPaintStyle.Fill)
                }
                drawCircle(knobX, 60f.rf, 8f.rf)

                // --- Draw Variable Axis Font ---
                // Create format string for weight value: e.g., "Weight: 400"
                val valStr = wghtVal.format(3, 0, 0)
                val label = remoteText("Axis: 'wght' = ") + valStr

                paint {
                    color(0xFFE2E8F0.toInt())
                    textSize(32f)
                }
                drawTextAnchored(label, cx, 150f.rf, 0f.rf, 0f.rf, RcTextAnchorFlags.None)

                // Display variable-axis text: we use standard variable font and apply "wght" axis
                // dynamically
                paint {
                    color(0xFF38BDF8.toInt())
                    textSize(64f)
                    typeface("sans-serif")
                    // Apply the new axis(...) paint property!
                    axis(arrayOf("wght"), floatArrayOf(wghtVal.toFloat()))
                }
                drawTextAnchored(
                    remoteText("VARIABLE FONT"),
                    cx,
                    260f.rf,
                    0f.rf,
                    0f.rf,
                    RcTextAnchorFlags.None,
                )
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
fun DslFontAxisDemoPreview() {
    RemoteDocumentPreview(RemoteDocument(dslFontAxisDemo()))
}
