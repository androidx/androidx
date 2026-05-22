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
 * A demo showcasing the `CollapsibleRow` container layout combined with the newly introduced
 * `horizontalCollapsiblePriority(...)` modifier.
 *
 * Showcases:
 * 1. Three cards inside a collapsible horizontal row.
 * 2. Weight-based priority limits:
 *     - Card A: Priority = 1f (Lowest - collapses first)
 *     - Card B: Priority = 10f (Highest - last standing)
 *     - Card C: Priority = 5f (Medium - collapses second)
 */
@Suppress("RestrictedApiAndroidX")
public fun dslCollapsiblePriorityDemo(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        Column(
            modifier = Modifier.fillMaxSize().background(0xFF0F172A.toInt()).padding(24.rdp),
            horizontal = RcHorizontalPositioning.Center,
            vertical = RcColumnVerticalPositioning.Top,
        ) {
            // Title Header
            Text(
                text = "Collapsible Layout \nPriorities",
                weight = RcWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 72.rsp,
                textAlign = RcTextAlign.Center,
                color = 0xFFFFFFFF.toInt(),
            )

            Spacer(Modifier.height(16.rdp))
            val w = windowWidth() * (sin(continuousSeconds()) / 3f + 0.666f)
            // Subtitle
            Text(
                text =
                    "Elements below are placed inside a CollapsibleRow. \n When the screen constraints tighten, cards with lower priority collapse first, leaving high priority cards standing.",
                weight = RcWeight.Normal,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 58.rsp,
                textAlign = RcTextAlign.Center,
                color = 0xFF94A3B8.toInt(),
            )

            Spacer(Modifier.height(32.rdp))
            Box(Modifier.fillMaxHeight().width((w).toFloat().rdp)) {
                // Collapsible Horizontal Row Layout
                CollapsibleRow(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(240.rdp)
                            .background(0xFF1E293B.toInt())
                            .padding(16.rdp),
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcVerticalPositioning.Center,
                ) {
                    // Box A: Low Priority Card (1f)
                    Column(
                        modifier =
                            Modifier.size(140.rdp, 180.rdp)
                                .background(0xFFEF4444.toInt()) // Red/Low
                                .padding(16.rdp)
                                .horizontalCollapsiblePriority(1f)
                    ) {
                        Text(
                            text = "CARD A",
                            weight = RcWeight.Bold,
                            fontSize = 24.rsp,
                            color = 0xFFFFFFFF.toInt(),
                        )
                        Spacer(Modifier.height(8.rdp))
                        Text(
                            text = "Priority: 1f",
                            weight = RcWeight.Medium,
                            fontSize = 18.rsp,
                            color = 0xFFFFFFFF.toInt(),
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    // Box B: High Priority Card (10f)
                    Column(
                        modifier =
                            Modifier.size(140.rdp, 180.rdp)
                                .background(0xFF10B981.toInt()) // Green/High - Last standing!
                                .padding(16.rdp)
                                .horizontalCollapsiblePriority(10f)
                    ) {
                        Text(
                            text = "CARD B",
                            weight = RcWeight.Bold,
                            fontSize = 24.rsp,
                            color = 0xFFFFFFFF.toInt(),
                        )
                        Spacer(Modifier.height(8.rdp))
                        Text(
                            text = "Priority: 10f",
                            weight = RcWeight.Medium,
                            fontSize = 18.rsp,
                            color = 0xFFFFFFFF.toInt(),
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    // Box C: Medium Priority Card (5f)
                    Column(
                        modifier =
                            Modifier.size(140.rdp, 180.rdp)
                                .background(0xFFF59E0B.toInt()) // Amber/Medium
                                .padding(16.rdp)
                                .horizontalCollapsiblePriority(5f)
                    ) {
                        Text(
                            text = "CARD C",
                            weight = RcWeight.Bold,
                            fontSize = 24.rsp,
                            color = 0xFFFFFFFF.toInt(),
                        )
                        Spacer(Modifier.height(8.rdp))
                        Text(
                            text = "Priority: 5f",
                            weight = RcWeight.Medium,
                            fontSize = 18.rsp,
                            color = 0xFFFFFFFF.toInt(),
                        )
                    }
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
fun DslCollapsiblePriorityDemoPreview() {
    RemoteDocumentPreview(RemoteDocument(dslCollapsiblePriorityDemo()))
}
