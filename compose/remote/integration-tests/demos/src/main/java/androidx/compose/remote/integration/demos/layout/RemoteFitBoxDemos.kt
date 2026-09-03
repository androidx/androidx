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

package androidx.compose.remote.integration.demos.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.remote.core.operations.layout.animation.AnimationSpec
import androidx.compose.remote.core.operations.utilities.easing.GeneralEasing
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteFitBox
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.animationSpec
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.integration.demos.common.RemoteDemo
import androidx.compose.remote.tooling.preview.RemoteComponentPreview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Interactive demo demonstrating responsive shared element transitions in [RemoteFitBox]. As the
 * container width changes (via slider or preset chips), [RemoteFitBox] picks the best-fitting
 * alternative and smoothly morphs shared elements across layouts.
 */
@Suppress("RestrictedApiAndroidX")
@Composable
fun RemoteFitBoxSharedElementsDemo() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Responsive Player", "Card to Banner", "Row to Column")

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                )
            }
        }

        when (selectedTab) {
            0 -> RemoteFitBoxResponsivePlayerDemo()
            1 -> RemoteFitBoxCardToBannerDemo()
            2 -> RemoteFitBoxRowToColumnDemo()
        }
    }
}

/**
 * Media player that morphs between Expanded (wide controls), Medium (compact bar), and Minimal
 * (compact icon badge) as available width changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("RestrictedApiAndroidX", "DEPRECATION")
@Composable
private fun RemoteFitBoxResponsivePlayerDemo() {
    var containerWidth by remember { mutableFloatStateOf(340f) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Resize container to trigger FitBox transitions:",
            style = MaterialTheme.typography.titleMedium,
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = containerWidth == 100f,
                onClick = { containerWidth = 100f },
                label = { Text("Compact (100dp)") },
            )
            FilterChip(
                selected = containerWidth == 220f,
                onClick = { containerWidth = 220f },
                label = { Text("Medium (220dp)") },
            )
            FilterChip(
                selected = containerWidth == 340f,
                onClick = { containerWidth = 340f },
                label = { Text("Expanded (340dp)") },
            )
        }

        Slider(
            value = containerWidth,
            onValueChange = { containerWidth = it },
            valueRange = 80f..360f,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )

        Text(
            text = "Width: ${containerWidth.toInt()}dp",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier =
                Modifier.width(containerWidth.dp)
                    .height(110.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color.DarkGray, RoundedCornerShape(16.dp))
                    .background(Color(0xFF161622))
                    .padding(8.dp)
        ) {
            RemoteDemo {
                RemoteFitBox(
                    modifier =
                        RemoteModifier.animationSpec(
                            animationId = 100,
                            motionDuration = 500f,
                            motionEasingType = GeneralEasing.CUBIC_STANDARD,
                            visibilityDuration = 500f,
                            visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                            enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                            exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                        )
                ) {
                    // Alternative 1: Wide full player (fits >= 300dp)
                    RemoteBox(modifier = RemoteModifier.width(300.rdp).height(64.rdp)) {
                        RemoteRow(
                            verticalAlignment = RemoteAlignment.CenterVertically,
                            horizontalArrangement = RemoteArrangement.spacedBy(12.rdp),
                        ) {
                            // Album Art
                            RemoteBox(
                                modifier =
                                    RemoteModifier.animationSpec(
                                            animationId = 1,
                                            motionDuration = 500f,
                                            motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                            visibilityDuration = 500f,
                                            visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                            enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                            exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                        )
                                        .size(48.rdp)
                                        .clip(RemoteRoundedCornerShape(12.rdp))
                                        .background(Color(0xFF7C4DFF).rc),
                                contentAlignment = RemoteAlignment.Center,
                            ) {
                                RemoteText(text = "🎵".rs, fontSize = 22.rsp)
                            }

                            // Song Title & Artist
                            RemoteColumn(
                                modifier =
                                    RemoteModifier.animationSpec(
                                            animationId = 2,
                                            motionDuration = 500f,
                                            motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                            visibilityDuration = 500f,
                                            visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                            enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                            exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                        )
                                        .width(130.rdp)
                            ) {
                                RemoteText(
                                    text = "Midnight City".rs,
                                    color = Color.White.rc,
                                    fontSize = 14.rsp,
                                )
                                RemoteText(
                                    text = "M83".rs,
                                    color = Color.Gray.rc,
                                    fontSize = 11.rsp,
                                )
                            }

                            // Play Button
                            RemoteBox(
                                modifier =
                                    RemoteModifier.animationSpec(
                                            animationId = 3,
                                            motionDuration = 500f,
                                            motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                            visibilityDuration = 500f,
                                            visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                            enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                            exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                        )
                                        .size(40.rdp)
                                        .clip(RemoteRoundedCornerShape(20.rdp))
                                        .background(Color(0xFFFF4081).rc),
                                contentAlignment = RemoteAlignment.Center,
                            ) {
                                RemoteText(text = "▶".rs, color = Color.White.rc, fontSize = 16.rsp)
                            }
                        }
                    }

                    // Alternative 2: Medium compact bar (fits >= 190dp)
                    RemoteBox(modifier = RemoteModifier.width(190.rdp).height(56.rdp)) {
                        RemoteRow(
                            verticalAlignment = RemoteAlignment.CenterVertically,
                            horizontalArrangement = RemoteArrangement.spacedBy(10.rdp),
                        ) {
                            RemoteBox(
                                modifier =
                                    RemoteModifier.animationSpec(
                                            animationId = 1,
                                            motionDuration = 500f,
                                            motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                            visibilityDuration = 500f,
                                            visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                            enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                            exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                        )
                                        .size(38.rdp)
                                        .clip(RemoteRoundedCornerShape(10.rdp))
                                        .background(Color(0xFF7C4DFF).rc),
                                contentAlignment = RemoteAlignment.Center,
                            ) {
                                RemoteText(text = "🎵".rs, fontSize = 18.rsp)
                            }

                            RemoteColumn(
                                modifier =
                                    RemoteModifier.animationSpec(
                                            animationId = 2,
                                            motionDuration = 500f,
                                            motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                            visibilityDuration = 500f,
                                            visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                            enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                            exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                        )
                                        .width(80.rdp)
                            ) {
                                RemoteText(
                                    text = "Midnight".rs,
                                    color = Color.White.rc,
                                    fontSize = 13.rsp,
                                )
                            }

                            RemoteBox(
                                modifier =
                                    RemoteModifier.animationSpec(
                                            animationId = 3,
                                            motionDuration = 500f,
                                            motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                            visibilityDuration = 500f,
                                            visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                            enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                            exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                        )
                                        .size(34.rdp)
                                        .clip(RemoteRoundedCornerShape(17.rdp))
                                        .background(Color(0xFFFF4081).rc),
                                contentAlignment = RemoteAlignment.Center,
                            ) {
                                RemoteText(text = "▶".rs, color = Color.White.rc, fontSize = 14.rsp)
                            }
                        }
                    }

                    // Alternative 3: Minimal badge (fits narrow containers)
                    RemoteBox(modifier = RemoteModifier.width(70.rdp).height(50.rdp)) {
                        RemoteBox(
                            modifier =
                                RemoteModifier.animationSpec(
                                        animationId = 1,
                                        motionDuration = 500f,
                                        motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                        visibilityDuration = 500f,
                                        visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                        enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                        exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                    )
                                    .size(46.rdp)
                                    .clip(RemoteRoundedCornerShape(23.rdp))
                                    .background(Color(0xFF7C4DFF).rc),
                            contentAlignment = RemoteAlignment.Center,
                        ) {
                            RemoteText(text = "▶".rs, color = Color.White.rc, fontSize = 20.rsp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card that transforms from a horizontal banner into a vertical profile card when space becomes
 * constrained.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("RestrictedApiAndroidX", "DEPRECATION")
@Composable
private fun RemoteFitBoxCardToBannerDemo() {
    var containerWidth by remember { mutableFloatStateOf(340f) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Resize container to morph from Banner to Card layout:",
            style = MaterialTheme.typography.titleMedium,
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = containerWidth == 160f,
                onClick = { containerWidth = 160f },
                label = { Text("Vertical Card (160dp)") },
            )
            FilterChip(
                selected = containerWidth == 340f,
                onClick = { containerWidth = 340f },
                label = { Text("Wide Banner (340dp)") },
            )
        }

        Slider(
            value = containerWidth,
            onValueChange = { containerWidth = it },
            valueRange = 140f..360f,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )

        Text(
            text = "Width: ${containerWidth.toInt()}dp",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier =
                Modifier.width(containerWidth.dp)
                    .height(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color.DarkGray, RoundedCornerShape(16.dp))
                    .background(Color(0xFF1A1C2E))
                    .padding(12.dp)
        ) {
            RemoteDemo {
                RemoteFitBox(
                    modifier =
                        RemoteModifier.animationSpec(
                            animationId = 200,
                            motionDuration = 500f,
                            motionEasingType = GeneralEasing.CUBIC_STANDARD,
                            visibilityDuration = 500f,
                            visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                            enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                            exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                        )
                ) {
                    // Wide Banner (fits >= 270dp)
                    RemoteBox(modifier = RemoteModifier.width(270.rdp).height(72.rdp)) {
                        RemoteRow(
                            verticalAlignment = RemoteAlignment.CenterVertically,
                            horizontalArrangement = RemoteArrangement.spacedBy(12.rdp),
                        ) {
                            // Avatar
                            RemoteBox(
                                modifier =
                                    RemoteModifier.animationSpec(
                                            animationId = 11,
                                            motionDuration = 500f,
                                            motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                            visibilityDuration = 500f,
                                            visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                            enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                            exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                        )
                                        .size(54.rdp)
                                        .clip(RemoteRoundedCornerShape(27.rdp))
                                        .background(Color(0xFF00B0FF).rc),
                                contentAlignment = RemoteAlignment.Center,
                            ) {
                                RemoteText(text = "🚀".rs, fontSize = 24.rsp)
                            }

                            // Details (to the right of avatar in banner)
                            RemoteColumn(
                                modifier =
                                    RemoteModifier.animationSpec(
                                            animationId = 12,
                                            motionDuration = 500f,
                                            motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                            visibilityDuration = 500f,
                                            visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                            enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                            exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                        )
                                        .width(120.rdp)
                            ) {
                                RemoteText(
                                    text = "Space Explorer".rs,
                                    color = Color.White.rc,
                                    fontSize = 15.rsp,
                                )
                                RemoteText(
                                    text = "Ready to launch".rs,
                                    color = Color.LightGray.rc,
                                    fontSize = 12.rsp,
                                )
                            }

                            // Follow Action Button
                            RemoteBox(
                                modifier =
                                    RemoteModifier.animationSpec(
                                            animationId = 13,
                                            motionDuration = 500f,
                                            motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                            visibilityDuration = 500f,
                                            visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                            enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                            exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                        )
                                        .size(width = 72.rdp, height = 36.rdp)
                                        .clip(RemoteRoundedCornerShape(18.rdp))
                                        .background(Color(0xFF00E676).rc),
                                contentAlignment = RemoteAlignment.Center,
                            ) {
                                RemoteText(
                                    text = "Follow".rs,
                                    color = Color.Black.rc,
                                    fontSize = 13.rsp,
                                )
                            }
                        }
                    }

                    // Vertical Card (fits narrow containers >= 110dp)
                    // Details text is placed below the avatar so it moves downwards during
                    // transition
                    RemoteBox(modifier = RemoteModifier.width(110.rdp).height(190.rdp)) {
                        RemoteColumn(
                            horizontalAlignment = RemoteAlignment.CenterHorizontally,
                            verticalArrangement = RemoteArrangement.spacedBy(10.rdp),
                        ) {
                            // Avatar centered at the top
                            RemoteBox(
                                modifier =
                                    RemoteModifier.animationSpec(
                                            animationId = 11,
                                            motionDuration = 500f,
                                            motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                            visibilityDuration = 500f,
                                            visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                            enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                            exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                        )
                                        .size(64.rdp)
                                        .clip(RemoteRoundedCornerShape(32.rdp))
                                        .background(Color(0xFF00B0FF).rc),
                                contentAlignment = RemoteAlignment.Center,
                            ) {
                                RemoteText(text = "🚀".rs, fontSize = 28.rsp)
                            }

                            // Details placed below the avatar (centered)
                            RemoteColumn(
                                modifier =
                                    RemoteModifier.animationSpec(
                                        animationId = 12,
                                        motionDuration = 500f,
                                        motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                        visibilityDuration = 500f,
                                        visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                        enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                        exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                    ),
                                horizontalAlignment = RemoteAlignment.CenterHorizontally,
                            ) {
                                RemoteText(
                                    text = "Space Explorer".rs,
                                    color = Color.White.rc,
                                    fontSize = 13.rsp,
                                )
                                RemoteText(
                                    text = "Ready to launch".rs,
                                    color = Color.LightGray.rc,
                                    fontSize = 10.rsp,
                                )
                            }

                            // Follow Action Button below the text
                            RemoteBox(
                                modifier =
                                    RemoteModifier.animationSpec(
                                            animationId = 13,
                                            motionDuration = 500f,
                                            motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                            visibilityDuration = 500f,
                                            visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                            enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                            exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                        )
                                        .size(width = 96.rdp, height = 32.rdp)
                                        .clip(RemoteRoundedCornerShape(16.rdp))
                                        .background(Color(0xFF00E676).rc),
                                contentAlignment = RemoteAlignment.Center,
                            ) {
                                RemoteText(
                                    text = "Follow".rs,
                                    color = Color.Black.rc,
                                    fontSize = 12.rsp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Three tiles that morph from a horizontal [RemoteRow] layout into a vertical [RemoteColumn] layout
 * as available container width changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("RestrictedApiAndroidX", "DEPRECATION")
@Composable
private fun RemoteFitBoxRowToColumnDemo() {
    var containerWidth by remember { mutableFloatStateOf(320f) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Resize container to morph between Row and Column:",
            style = MaterialTheme.typography.titleMedium,
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = containerWidth == 140f,
                onClick = { containerWidth = 140f },
                label = { Text("Column (140dp)") },
            )
            FilterChip(
                selected = containerWidth == 320f,
                onClick = { containerWidth = 320f },
                label = { Text("Row (320dp)") },
            )
        }

        Slider(
            value = containerWidth,
            onValueChange = { containerWidth = it },
            valueRange = 120f..340f,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )

        Text(
            text = "Width: ${containerWidth.toInt()}dp",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier =
                Modifier.width(containerWidth.dp)
                    .height(240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color.DarkGray, RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E1E2C))
                    .padding(12.dp)
        ) {
            RemoteDemo {
                RemoteFitBox(
                    modifier =
                        RemoteModifier.animationSpec(
                            animationId = 300,
                            motionDuration = 500f,
                            motionEasingType = GeneralEasing.CUBIC_STANDARD,
                            visibilityDuration = 500f,
                            visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                            enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                            exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                        )
                ) {
                    // Wide: Horizontal Row of 3 tiles (fits >= 250dp)
                    RemoteBox(modifier = RemoteModifier.width(250.rdp).height(70.rdp)) {
                        RemoteRow(horizontalArrangement = RemoteArrangement.spacedBy(10.rdp)) {
                            FitBoxTile(
                                id = 21,
                                label = "Alpha",
                                color = Color(0xFFE91E63),
                                width = 76,
                                height = 64,
                            )
                            FitBoxTile(
                                id = 22,
                                label = "Beta",
                                color = Color(0xFF9C27B0),
                                width = 76,
                                height = 64,
                            )
                            FitBoxTile(
                                id = 23,
                                label = "Gamma",
                                color = Color(0xFF2196F3),
                                width = 76,
                                height = 64,
                            )
                        }
                    }

                    // Narrow: Vertical Column of 3 tiles (fits narrow containers >= 96dp)
                    RemoteBox(modifier = RemoteModifier.width(96.rdp).height(170.rdp)) {
                        RemoteColumn(verticalArrangement = RemoteArrangement.spacedBy(8.rdp)) {
                            FitBoxTile(
                                id = 21,
                                label = "Alpha",
                                color = Color(0xFFE91E63),
                                width = 96,
                                height = 48,
                            )
                            FitBoxTile(
                                id = 22,
                                label = "Beta",
                                color = Color(0xFF9C27B0),
                                width = 96,
                                height = 48,
                            )
                            FitBoxTile(
                                id = 23,
                                label = "Gamma",
                                color = Color(0xFF2196F3),
                                width = 96,
                                height = 48,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
@RemoteComposable
private fun FitBoxTile(id: Int, label: String, color: Color, width: Int, height: Int) {
    RemoteBox(
        modifier =
            RemoteModifier.animationSpec(
                    animationId = id,
                    motionDuration = 500f,
                    motionEasingType = GeneralEasing.CUBIC_STANDARD,
                    visibilityDuration = 500f,
                    visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                    enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                    exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                )
                .width(width.rdp)
                .height(height.rdp)
                .clip(RemoteRoundedCornerShape(10.rdp))
                .background(color.rc),
        contentAlignment = RemoteAlignment.Center,
    ) {
        RemoteText(text = label.rs, color = Color.White.rc, fontSize = 13.rsp)
    }
}

@Suppress("RestrictedApiAndroidX")
@RemoteComponentPreview
@Composable
@RemoteComposable
private fun FitBoxTilePreview() {
    FitBoxTile(id = 1, label = "Tile 1", color = Color(0xFF3F51B5), width = 80, height = 80)
}

@Preview(showBackground = true)
@Composable
private fun RemoteFitBoxSharedElementsDemoPreview() {
    RemoteFitBoxSharedElementsDemo()
}

@Preview(showBackground = true)
@Composable
private fun RemoteFitBoxResponsivePlayerDemoPreview() {
    RemoteFitBoxResponsivePlayerDemo()
}

@Preview(showBackground = true)
@Composable
private fun RemoteFitBoxCardToBannerDemoPreview() {
    RemoteFitBoxCardToBannerDemo()
}

@Preview(showBackground = true)
@Composable
private fun RemoteFitBoxRowToColumnDemoPreview() {
    RemoteFitBoxRowToColumnDemo()
}
