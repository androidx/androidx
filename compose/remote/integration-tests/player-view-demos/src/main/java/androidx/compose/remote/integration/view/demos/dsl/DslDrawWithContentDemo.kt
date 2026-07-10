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

import android.graphics.Color
import androidx.compose.remote.creation.dsl.*
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * A demo showcasing both the `ripple()` interactive click-feedback modifier and the
 * `drawWithContent` drawing interception modifier.
 *
 * Showcases:
 * 1. `Modifier.ripple()` — Injects dynamic, tactile tap-ripple animations on clicks.
 * 2. `Modifier.drawWithContent { ... }` — Intercepts component rendering to:
 *     - Paint custom background grids / gradients BEHIND the content.
 *     - Explicitly draw the children content using `drawComponentContent()`.
 *     - Paint modern glowing border frames & notification badges ON TOP of the content.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslDrawWithContentDemo(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        Column(
            modifier = Modifier.fillMaxSize().background(0xFF0B0E14.toInt()).padding(32.rdp),
            horizontal = RcHorizontalPositioning.Center,
            vertical = RcColumnVerticalPositioning.Top,
        ) {
            // Title
            Text(
                text = "Draw Interception \n& Ripple",
                weight = RcWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 120.rsp,
                textAlign = RcTextAlign.Center,
                color = 0xFFFFFFFF.toInt(),
            )

            Spacer(Modifier.height(24.rdp))

            // Description
            Text(
                text =
                    "Below is an interactive card.\n Click to trigger Ripple. The custom borders and background are painted dynamically using drawWithContent.",
                weight = RcWeight.Normal,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 60.rsp,
                textAlign = RcTextAlign.Center,
                color = 0xFF94A3B8.toInt(),
            )

            Spacer(Modifier.height(64.rdp))

            // Interactive Custom Decorated Card Component
            Box(
                modifier =
                    Modifier.background(Color.GRAY)
                        .size(480.rdp, 280.rdp)
                        // Injects Ripple feedback for tap interactions
                        .ripple()
                        // Injects action handler to register the click
                        .onClick { hostAction("CardClicked") }
                        // Draw Interception Modifiers
                        .drawWithContent {
                            val w = width
                            val h = height

                            // A. Paint futuristic neon background grid BEHIND layout content
                            paint {
                                color(0x551E1E2F.rcColor())
                                style(RcPaintStyle.Fill)
                            }
                            drawRoundRect(0f.rf, 0f.rf, w, h, 32f.rf, 32f.rf)

                            paint {
                                color(0xFF2D2D44.rcColor())
                                style(RcPaintStyle.Stroke)
                                strokeWidth(4f)
                            }
                            // Draw grid diagonals
                            drawLine(0f.rf, 0f.rf, w, h)
                            drawLine(0f.rf, h, w, 0f.rf)

                            // B. Draw actual component layout children (Text / Columns)
                            drawComponentContent()

                            // C. Paint custom cyber glowing borders ON TOP of layout content
                            paint {
                                color(0xFF38BDF8.toInt()) // Sky Blue Glow
                                style(RcPaintStyle.Stroke)
                                strokeWidth(8f)
                            }
                            drawRoundRect(0f.rf, 0f.rf, w, h, 32f.rf, 32f.rf)

                            // Paint glowing corner accents
                            paint {
                                color(0xFF00FFCC.toInt()) // Neon Teal accent
                                style(RcPaintStyle.Fill)
                            }
                            drawCircle(0f.rf, 0f.rf, 16f.rf)
                            drawCircle(w, 0f.rf, 16f.rf)
                            drawCircle(0f.rf, h, 16f.rf)
                            drawCircle(w, h, 16f.rf)
                        }
            ) {
                // Component Children Content
                Column(
                    modifier = Modifier.fillMaxSize().padding(36.rdp),
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcColumnVerticalPositioning.Center,
                ) {
                    Text(
                        text = "INTERACTIVE HUD",
                        weight = RcWeight.Bold,
                        fontSize = 28.rsp,
                        textAlign = RcTextAlign.Center,
                        color = 0xFF00FFCC.toInt(),
                    )

                    Spacer(Modifier.height(10.rdp))

                    Text(
                        text = "TAP TO TRIGGER RIPPLE",
                        weight = RcWeight.Medium,
                        fontSize = 18.rsp,
                        textAlign = RcTextAlign.Center,
                        color = 0xFFE2E8F0.toInt(),
                    )
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
fun DslDrawWithContentDemoPreview() {
    RemoteDocumentPreview(RemoteDocument(dslDrawWithContentDemo()))
}
