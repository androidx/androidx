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

import androidx.compose.remote.creation.dsl.*
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * A demo showcasing dynamic layout measurement and positioning computations inside the DSL.
 *
 * Showcases:
 * 1. `Modifier.computeMeasure { ... }` — Dynamically updates width & height at runtime on the
 *    player, forcing the card's aspect ratio to be a perfect 1:1 square (`height = width`).
 * 2. `Modifier.computePosition { ... }` — Dynamically computes absolute X and Y coordinate
 *    positions to center the card within its parent container (`x = (parentWidth - width) / 2f` &
 *    `y = (parentHeight - height) / 2f`).
 */
@Suppress("RestrictedApiAndroidX")
public fun dslLayoutComputeDemo(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        Box(
            modifier = Modifier.fillMaxSize().background(0xFF0F172A.toInt()) // Slate 900 background
        ) {
            val swing = 1f / (sin(continuousSeconds()) + 2f)
            // Card centered dynamically on screen using computePosition and computeMeasure
            Column(
                modifier =
                    Modifier
                        // Base background & shape border
                        .background(0xFF1E293B.toInt())
                        .border(1.5f, 24f, 0xFF38BDF8.rcColor(), RcBorderShape.RoundedRectangle)
                        .padding(24.rdp)
                        // Force perfect 1:1 aspect ratio: height equals width
                        .computeMeasure { height = width * swing }
                        // Center dynamically relative to parent width/height
                        .computePosition {
                            x = (parentWidth - width) / 2f
                            y = (parentHeight - height) / 2f
                        },
                horizontal = RcHorizontalPositioning.Center,
                vertical = RcColumnVerticalPositioning.Center,
            ) {
                Text(
                    text = "DYNAMIC HUD",
                    weight = RcWeight.Bold,
                    fontSize = 28.rsp,
                    color = 0xFF38BDF8.toInt(), // Sky Blue
                )

                Spacer(Modifier.height(16.rdp))

                Text(
                    text = "Centered & Sized via compute modifiers on player",
                    weight = RcWeight.Medium,
                    fontSize = 18.rsp,
                    textAlign = RcTextAlign.Center,
                    color = 0xFF94A3B8.toInt(), // Muted text
                )
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
fun DslLayoutComputeDemoPreview() {
    RemoteDocumentPreview(RemoteDocument(dslLayoutComputeDemo()))
}
