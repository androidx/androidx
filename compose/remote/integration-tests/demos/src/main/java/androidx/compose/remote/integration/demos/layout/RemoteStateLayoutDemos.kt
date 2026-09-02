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

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.remote.core.operations.layout.animation.AnimationSpec
import androidx.compose.remote.core.operations.utilities.easing.GeneralEasing
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteStateLayout
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.animationSpec
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteBoolean
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteInt
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteInt
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.integration.demos.common.RemoteDemo
import androidx.compose.remote.tooling.preview.RemoteComponentPreview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Suppress("RestrictedApiAndroidX")
@Composable
fun RemoteStateLayoutSimpleDemo() {
    val stateId = "stateId"
    val states = intArrayOf(0, 1, 2)
    var selectedState by remember { mutableIntStateOf(states[0]) }
    var expanded by remember { mutableStateOf(false) }

    Column {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, Color.Gray)
                    .clickable { expanded = true }
                    .padding(16.dp)
        ) {
            Text("State: $selectedState")
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                states.forEach { state ->
                    DropdownMenuItem(
                        text = { Text("State $state") },
                        onClick = {
                            selectedState = state
                            expanded = false
                        },
                    )
                }
            }
        }

        RemoteDemo(update = { player -> player.setUserLocalInt(stateId, selectedState) }) {
            val remoteState = rememberNamedRemoteInt(stateId, states[0])

            RemoteStateLayout(currentState = remoteState, states = states) { state ->
                val color =
                    when (state) {
                        0 -> Color.Red
                        1 -> Color.Green
                        2 -> Color.Blue
                        else -> Color.Black
                    }
                RemoteBox(
                    modifier = RemoteModifier.size(RemoteDp(100.dp)).background(color.rc),
                    contentAlignment = RemoteAlignment.Center,
                ) {
                    RemoteText(text = "$state".rs, fontSize = 18.rsp)
                }
            }
        }
    }
}

/**
 * Remote composable card expansion content showing list-to-detail shared element transition with
 * StateLayout. Morphs an avatar into a wide banner header, moves title text, and morphs action
 * button.
 */
@Suppress("RestrictedApiAndroidX")
@RemoteComponentPreview
@Composable
@RemoteComposable
private fun RemoteStateLayoutCardExpansion() {
    val isExpanded = rememberMutableRemoteBoolean(false)

    RemoteBox(
        modifier =
            RemoteModifier.padding(12.rdp)
                .clip(RemoteRoundedCornerShape(16.rdp))
                .background(Color(0xFF1E1E2C).rc)
                .clickable(action = valueChange(isExpanded, !isExpanded))
                .padding(16.rdp),
        contentAlignment = RemoteAlignment.Center,
    ) {
        RemoteStateLayout(currentState = isExpanded) { expanded ->
            if (!expanded) {
                // State 0: Compact Card
                RemoteRow(
                    verticalAlignment = RemoteAlignment.CenterVertically,
                    horizontalArrangement = RemoteArrangement.spacedBy(12.rdp),
                ) {
                    // Shared Element 1: Thumbnail Avatar
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
                                .clip(RemoteRoundedCornerShape(24.rdp))
                                .background(Color(0xFF6750A4).rc),
                        contentAlignment = RemoteAlignment.Center,
                    ) {
                        RemoteText(text = "🎵".rs, fontSize = 20.rsp)
                    }

                    // Shared Element 2: Title Info
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
                    ) {
                        RemoteText(
                            text = "Concert Night".rs,
                            color = Color.White.rc,
                            fontSize = 16.rsp,
                        )
                        RemoteText(
                            text = "Live at Metropolis Arena".rs,
                            color = Color(0xFFAAAAAA).rc,
                            fontSize = 12.rsp,
                        )
                    }

                    // Shared Element 3: Expand Action Pill
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
                                .padding(start = 16.rdp)
                                .clip(RemoteRoundedCornerShape(12.rdp))
                                .background(Color(0xFFD0BCFF).rc)
                                .padding(8.rdp),
                        contentAlignment = RemoteAlignment.Center,
                    ) {
                        RemoteText(
                            text = "View".rs,
                            color = Color(0xFF381E72).rc,
                            fontSize = 12.rsp,
                        )
                    }
                }
            } else {
                // State 1: Expanded Detail View
                RemoteColumn(
                    modifier = RemoteModifier.width(260.rdp),
                    verticalArrangement = RemoteArrangement.spacedBy(12.rdp),
                ) {
                    // Shared Element 1: Morphs into Large Header Banner
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
                                .width(260.rdp)
                                .height(110.rdp)
                                .clip(RemoteRoundedCornerShape(12.rdp))
                                .background(Color(0xFF6750A4).rc),
                        contentAlignment = RemoteAlignment.Center,
                    ) {
                        RemoteText(
                            text = "🎵 Featured Event".rs,
                            color = Color.White.rc,
                            fontSize = 20.rsp,
                        )
                    }

                    // Shared Element 2: Title Info repositioned below banner
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
                    ) {
                        RemoteText(
                            text = "Concert Night 2026".rs,
                            color = Color.White.rc,
                            fontSize = 20.rsp,
                        )
                        RemoteText(
                            text = "Live at Metropolis Arena • Door opens 7PM".rs,
                            color = Color(0xFFAAAAAA).rc,
                            fontSize = 13.rsp,
                        )
                    }

                    // Non-shared content: fades in seamlessly
                    RemoteText(
                        text =
                            "Experience an immersive symphony under the stars with special guest artists."
                                .rs,
                        color = Color(0xFFCCCCCC).rc,
                        fontSize = 12.rsp,
                    )

                    // Shared Element 3: Action Pill morphs to full-width button
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
                                .width(260.rdp)
                                .height(36.rdp)
                                .clip(RemoteRoundedCornerShape(18.rdp))
                                .background(Color(0xFFD0BCFF).rc),
                        contentAlignment = RemoteAlignment.Center,
                    ) {
                        RemoteText(
                            text = "Collapse".rs,
                            color = Color(0xFF381E72).rc,
                            fontSize = 14.rsp,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Remote composable media player content transitioning between a compact player bar and a full
 * expanded player UI with shared album cover, track title, and controls.
 */
@Suppress("RestrictedApiAndroidX")
@RemoteComponentPreview
@Composable
@RemoteComposable
private fun RemoteStateLayoutMediaPlayer() {
    val isFullScreen = rememberMutableRemoteBoolean(false)

    RemoteBox(
        modifier =
            RemoteModifier.clip(RemoteRoundedCornerShape(20.rdp))
                .background(Color(0xFF141419).rc)
                .clickable(action = valueChange(isFullScreen, !isFullScreen))
                .padding(16.rdp),
        contentAlignment = RemoteAlignment.Center,
    ) {
        RemoteStateLayout(currentState = isFullScreen) { full ->
            if (!full) {
                // Mini Player
                RemoteRow(
                    modifier = RemoteModifier.width(280.rdp),
                    verticalAlignment = RemoteAlignment.CenterVertically,
                    horizontalArrangement = RemoteArrangement.SpaceBetween,
                ) {
                    RemoteRow(
                        verticalAlignment = RemoteAlignment.CenterVertically,
                        horizontalArrangement = RemoteArrangement.spacedBy(10.rdp),
                    ) {
                        // Shared 10: Album Art Thumbnail
                        RemoteBox(
                            modifier =
                                RemoteModifier.animationSpec(
                                        animationId = 10,
                                        motionDuration = 500f,
                                        motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                        visibilityDuration = 500f,
                                        visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                        enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                        exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                    )
                                    .size(38.rdp)
                                    .clip(RemoteRoundedCornerShape(8.rdp))
                                    .background(Color(0xFFFF5722).rc),
                            contentAlignment = RemoteAlignment.Center,
                        ) {
                            RemoteText(text = "🔥".rs, fontSize = 18.rsp)
                        }

                        // Shared 11: Track Info
                        RemoteColumn(
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
                        ) {
                            RemoteText(
                                text = "Solar Flare".rs,
                                color = Color.White.rc,
                                fontSize = 14.rsp,
                            )
                            RemoteText(
                                text = "Cosmic Wave".rs,
                                color = Color.Gray.rc,
                                fontSize = 11.rsp,
                            )
                        }
                    }

                    // Shared 12: Play Button
                    RemoteBox(
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
                                .size(34.rdp)
                                .clip(RemoteRoundedCornerShape(17.rdp))
                                .background(Color(0xFFE91E63).rc),
                        contentAlignment = RemoteAlignment.Center,
                    ) {
                        RemoteText(text = "▶".rs, color = Color.White.rc, fontSize = 14.rsp)
                    }
                }
            } else {
                // Full Player
                RemoteColumn(
                    modifier = RemoteModifier.width(260.rdp),
                    horizontalAlignment = RemoteAlignment.CenterHorizontally,
                    verticalArrangement = RemoteArrangement.spacedBy(16.rdp),
                ) {
                    // Shared 10: Album Art Cover (Expanded)
                    RemoteBox(
                        modifier =
                            RemoteModifier.animationSpec(
                                    animationId = 10,
                                    motionDuration = 500f,
                                    motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                    visibilityDuration = 500f,
                                    visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                    enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                    exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                )
                                .size(140.rdp)
                                .clip(RemoteRoundedCornerShape(16.rdp))
                                .background(Color(0xFFFF5722).rc),
                        contentAlignment = RemoteAlignment.Center,
                    ) {
                        RemoteText(text = "🔥".rs, fontSize = 54.rsp)
                    }

                    // Shared 11: Centered Track Info
                    RemoteColumn(
                        modifier =
                            RemoteModifier.animationSpec(
                                animationId = 11,
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
                            text = "Solar Flare".rs,
                            color = Color.White.rc,
                            fontSize = 20.rsp,
                        )
                        RemoteText(
                            text = "Cosmic Wave • Album 2026".rs,
                            color = Color.Gray.rc,
                            fontSize = 13.rsp,
                        )
                    }

                    // Non-shared progress bar
                    RemoteRow(
                        modifier =
                            RemoteModifier.width(240.rdp)
                                .height(4.rdp)
                                .clip(RemoteRoundedCornerShape(2.rdp))
                                .background(Color(0xFF333344).rc)
                    ) {
                        RemoteBox(
                            modifier =
                                RemoteModifier.width(150.rdp)
                                    .height(4.rdp)
                                    .background(Color(0xFFE91E63).rc)
                        )
                    }

                    // Controls Row with Shared 12 Play Button
                    RemoteRow(
                        modifier = RemoteModifier.width(220.rdp),
                        horizontalArrangement = RemoteArrangement.SpaceBetween,
                        verticalAlignment = RemoteAlignment.CenterVertically,
                    ) {
                        RemoteText(text = "⏮".rs, color = Color.White.rc, fontSize = 20.rsp)
                        RemoteBox(
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
                                    .size(54.rdp)
                                    .clip(RemoteRoundedCornerShape(27.rdp))
                                    .background(Color(0xFFE91E63).rc),
                            contentAlignment = RemoteAlignment.Center,
                        ) {
                            RemoteText(text = "⏸".rs, color = Color.White.rc, fontSize = 22.rsp)
                        }
                        RemoteText(text = "⏭".rs, color = Color.White.rc, fontSize = 20.rsp)
                    }
                }
            }
        }
    }
}

/**
 * Remote composable multi-state morphing layout demonstrating 4 shared items rearranging across 3
 * states: State 0: 2x2 Grid State 1: Horizontal Row State 2: Vertical Column
 */
@Suppress("RestrictedApiAndroidX")
@RemoteComponentPreview
@Composable
@RemoteComposable
private fun RemoteStateLayoutMorphGrid() {
    val morphState = rememberMutableRemoteInt(0)

    RemoteColumn(
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
        verticalArrangement = RemoteArrangement.spacedBy(16.rdp),
    ) {
        // Layout Selection Buttons
        RemoteRow(
            horizontalArrangement = RemoteArrangement.spacedBy(8.rdp),
            verticalAlignment = RemoteAlignment.CenterVertically,
        ) {
            RemoteBox(
                modifier =
                    RemoteModifier.clip(RemoteRoundedCornerShape(12.rdp))
                        .background(Color(0xFF381E72).rc)
                        .clickable(action = valueChange(morphState, 0.ri))
                        .padding(horizontal = 12.rdp, vertical = 6.rdp),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText(text = "2x2 Grid".rs, color = Color(0xFFD0BCFF).rc, fontSize = 12.rsp)
            }

            RemoteBox(
                modifier =
                    RemoteModifier.clip(RemoteRoundedCornerShape(12.rdp))
                        .background(Color(0xFF381E72).rc)
                        .clickable(action = valueChange(morphState, 1.ri))
                        .padding(horizontal = 12.rdp, vertical = 6.rdp),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText(text = "Row".rs, color = Color(0xFFD0BCFF).rc, fontSize = 12.rsp)
            }

            RemoteBox(
                modifier =
                    RemoteModifier.clip(RemoteRoundedCornerShape(12.rdp))
                        .background(Color(0xFF381E72).rc)
                        .clickable(action = valueChange(morphState, 2.ri))
                        .padding(horizontal = 12.rdp, vertical = 6.rdp),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText(text = "Column".rs, color = Color(0xFFD0BCFF).rc, fontSize = 12.rsp)
            }
        }

        // Interactive Morph Container
        RemoteBox(
            modifier =
                RemoteModifier.width(260.rdp)
                    .height(200.rdp)
                    .clip(RemoteRoundedCornerShape(16.rdp))
                    .background(Color(0xFF1E1E2C).rc)
                    .clickable(action = valueChange(morphState, (morphState + 1) % 3))
                    .padding(16.rdp),
            contentAlignment = RemoteAlignment.Center,
        ) {
            RemoteStateLayout(currentState = morphState, 0, 1, 2) { state ->
                when (state) {
                    0 -> {
                        // 2x2 Grid Layout
                        RemoteColumn(
                            verticalArrangement = RemoteArrangement.spacedBy(10.rdp),
                            horizontalAlignment = RemoteAlignment.CenterHorizontally,
                        ) {
                            RemoteRow(horizontalArrangement = RemoteArrangement.spacedBy(10.rdp)) {
                                Tile(
                                    id = 21,
                                    label = "A",
                                    color = Color(0xFF4CAF50),
                                    width = 60,
                                    height = 60,
                                )
                                Tile(
                                    id = 22,
                                    label = "B",
                                    color = Color(0xFF2196F3),
                                    width = 60,
                                    height = 60,
                                )
                            }
                            RemoteRow(horizontalArrangement = RemoteArrangement.spacedBy(10.rdp)) {
                                Tile(
                                    id = 23,
                                    label = "C",
                                    color = Color(0xFFFF9800),
                                    width = 60,
                                    height = 60,
                                )
                                Tile(
                                    id = 24,
                                    label = "D",
                                    color = Color(0xFF9C27B0),
                                    width = 60,
                                    height = 60,
                                )
                            }
                        }
                    }
                    1 -> {
                        // Horizontal Row Layout
                        RemoteRow(
                            horizontalArrangement = RemoteArrangement.spacedBy(8.rdp),
                            verticalAlignment = RemoteAlignment.CenterVertically,
                        ) {
                            Tile(
                                id = 21,
                                label = "A",
                                color = Color(0xFF4CAF50),
                                width = 45,
                                height = 90,
                            )
                            Tile(
                                id = 22,
                                label = "B",
                                color = Color(0xFF2196F3),
                                width = 45,
                                height = 90,
                            )
                            Tile(
                                id = 23,
                                label = "C",
                                color = Color(0xFFFF9800),
                                width = 45,
                                height = 90,
                            )
                            Tile(
                                id = 24,
                                label = "D",
                                color = Color(0xFF9C27B0),
                                width = 45,
                                height = 90,
                            )
                        }
                    }
                    else -> {
                        // Vertical Column Layout
                        RemoteColumn(
                            verticalArrangement = RemoteArrangement.spacedBy(8.rdp),
                            horizontalAlignment = RemoteAlignment.CenterHorizontally,
                        ) {
                            Tile(
                                id = 21,
                                label = "A - Fast",
                                color = Color(0xFF4CAF50),
                                width = 200,
                                height = 30,
                            )
                            Tile(
                                id = 22,
                                label = "B - Reliable",
                                color = Color(0xFF2196F3),
                                width = 200,
                                height = 30,
                            )
                            Tile(
                                id = 23,
                                label = "C - Flexible",
                                color = Color(0xFFFF9800),
                                width = 200,
                                height = 30,
                            )
                            Tile(
                                id = 24,
                                label = "D - Scalable",
                                color = Color(0xFF9C27B0),
                                width = 200,
                                height = 30,
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
private fun Tile(id: Int, label: String, color: Color, width: Int, height: Int) {
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
                .clip(RemoteRoundedCornerShape(8.rdp))
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
private fun TilePreview() {
    Tile(id = 1, label = "Tile 1", color = Color(0xFF6750A4), width = 80, height = 80)
}

/** Main container demo combining all StateLayout shared element showcases. */
@Suppress("RestrictedApiAndroidX")
@Composable
fun RemoteStateLayoutSharedElementsDemo() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Card Expansion", "Media Player", "Morph Grid")

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
            0 -> {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(
                        text = "Tap the card to morph between Compact and Expanded Detail view",
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    RemoteDemo { RemoteStateLayoutCardExpansion() }
                }
            }
            1 -> {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(
                        text = "Tap the player to morph between Mini Player and Full Player",
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    RemoteDemo { RemoteStateLayoutMediaPlayer() }
                }
            }
            2 -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Tap layout or buttons to morph arrangements",
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    RemoteDemo { RemoteStateLayoutMorphGrid() }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RemoteStateLayoutSharedElementsDemoPreview() {
    RemoteStateLayoutSharedElementsDemo()
}
