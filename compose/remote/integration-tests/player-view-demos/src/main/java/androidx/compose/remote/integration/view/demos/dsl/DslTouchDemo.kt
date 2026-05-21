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
 * A demo showcasing interactive touch gestures and haptic notch feedback using the new Remote
 * Compose DSL.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslTouchDemo(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        Column(
            modifier = Modifier.fillMaxSize().background(0xFF121214.toInt()).padding(32.rdp),
            horizontal = RcHorizontalPositioning.Center,
            vertical = RcColumnVerticalPositioning.Top,
        ) {
            // Header title
            Text(
                text = "Interactive DSL Touch & Haptic Showcase",
                weight = RcWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 32.rsp,
                textAlign = RcTextAlign.Center,
                color = 0xFFFFFFFF.toInt(),
            )

            Spacer(Modifier.height(16.rdp))

            sectionLabel("1. Stop Mode: Notches Even (Crossing triggers SegmentTick haptic)")

            // Notched Snapping Slider
            Canvas(
                modifier =
                    Modifier.size(560.rdp, 180.rdp)
                        .background(0xFF1E1E24.toInt())
                        .border(1.5f, 16f, 0xFF3A3A45.rcColor(), RcBorderShape.RoundedRectangle)
            ) {
                val w = width
                val h = height
                val cy = h * 0.5f

                val left = 40f
                val right = w - 40f

                // Notched slider driven by touch positions along the X axis
                val notchedPos =
                    addTouch(
                        defaultValue = 280f,
                        range = 40f..520f,
                        stopMode = RcTouchStopMode.NotchesEven,
                        velocity = 0f,
                        notchHaptic = RcHaptic.SegmentTick,
                        notches = floatArrayOf(5f), // 5 snapping points
                        easing = null,
                        RemoteContext.FLOAT_TOUCH_POS_X,
                    )

                // Draw base slider track
                paint {
                    color(0xFF2C2C35.toInt())
                    style(RcPaintStyle.Stroke)
                    strokeWidth(12f)
                    strokeCap(RcStrokeCap.Round)
                }
                drawLine(left.rf, cy, right, cy)

                // Draw notch markers
                paint {
                    color(0xFF555565.toInt())
                    style(RcPaintStyle.Fill)
                }
                loop(0f.rf, 1f.rf, 4f.rf) { index ->
                    val notchX = left.rf + (right - left.rf) * (index / 4f)
                    drawCircle(notchX, cy, 8f.rf)
                }

                // Draw active/colored value track
                paint {
                    color(0xFF00FFCC.toInt()) // Glowing neon teal
                    style(RcPaintStyle.Stroke)
                    strokeWidth(12f)
                    strokeCap(RcStrokeCap.Round)
                }
                drawLine(left.rf, cy, notchedPos, cy)

                // Draw sliding handle knob
                paint {
                    color(0xFF00FFCC.toInt())
                    style(RcPaintStyle.Fill)
                }
                drawCircle(notchedPos, cy, 24f.rf)

                // Inner details for the handle knob
                paint {
                    color(0xFF1E1E24.toInt())
                    style(RcPaintStyle.Fill)
                }
                drawCircle(notchedPos, cy, 10f.rf)
            }

            Spacer(Modifier.height(16.rdp))

            sectionLabel("2. Stop Mode: Gently (Friction-based free deceleration)")

            // Frictional free-deceleration slider
            Canvas(
                modifier =
                    Modifier.size(560.rdp, 180.rdp)
                        .background(0xFF1E1E24.toInt())
                        .border(1.5f, 16f, 0xFF3A3A45.rcColor(), RcBorderShape.RoundedRectangle)
            ) {
                val w = width
                val h = height
                val cy = h * 0.5f

                val left = 40f
                val right = w - 40f

                // Friction slider driven by touch positions along the X axis
                val gentlyPos =
                    addTouch(
                        defaultValue = 280f,
                        range = 40f..520f,
                        stopMode = RcTouchStopMode.Gently,
                        velocity = 0f,
                        notchHaptic = RcHaptic.NoHaptics,
                        notches = null,
                        easing = null,
                        RemoteContext.FLOAT_TOUCH_POS_X,
                    )

                // Draw base track
                paint {
                    color(0xFF2C2C35.toInt())
                    style(RcPaintStyle.Stroke)
                    strokeWidth(12f)
                    strokeCap(RcStrokeCap.Round)
                }
                drawLine(left.rf, cy, right, cy)

                // Draw active track
                paint {
                    color(0xFFFF3366.toInt()) // Glowing neon pink
                    style(RcPaintStyle.Stroke)
                    strokeWidth(12f)
                    strokeCap(RcStrokeCap.Round)
                }
                drawLine(left.rf, cy, gentlyPos, cy)

                // Draw handle knob
                paint {
                    color(0xFFFF3366.toInt())
                    style(RcPaintStyle.Fill)
                }
                drawCircle(gentlyPos, cy, 24f.rf)

                paint {
                    color(0xFF1E1E24.toInt())
                    style(RcPaintStyle.Fill)
                }
                drawCircle(gentlyPos, cy, 10f.rf)
            }
        }
    }
}

/** Helper to add styled section headers. */
@Suppress("RestrictedApiAndroidX")
private fun RcScope.sectionLabel(text: String) {
    Text(
        text = text,
        weight = RcWeight.SemiBold,
        modifier = Modifier.fillMaxWidth().padding(top = 8f, bottom = 8f),
        fontSize = 18.rsp,
        textAlign = RcTextAlign.Start,
        color = 0xFFAAAAAA.toInt(),
    )
}

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
fun DslTouchDemoPreview() {
    RemoteDocumentPreview(RemoteDocument(dslTouchDemo()))
}
