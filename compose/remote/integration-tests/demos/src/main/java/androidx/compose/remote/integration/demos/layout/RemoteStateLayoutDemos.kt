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
import androidx.compose.material3.PrimaryScrollableTabRow
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
import androidx.compose.remote.creation.compose.modifier.border
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.graphicsLayer
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.rotate
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.MutableRemoteInt
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteBoolean
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteInt
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteInt
import androidx.compose.remote.creation.compose.state.rf
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

/**
 * Demonstrates modifier transitions across states:
 * - Corner radius morphing (sharp rectangle -> rounded card -> circular badge)
 * - Border width and color morphing (1dp subtle -> 3dp accent -> no border / asymmetric)
 * - Graphics layer transforms (rotation, scale)
 */
@Suppress("RestrictedApiAndroidX")
@RemoteComponentPreview
@Composable
@RemoteComposable
private fun RemoteStateLayoutModifierTransitions() {
    val styleState = rememberMutableRemoteInt(0)

    RemoteColumn(
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
        verticalArrangement = RemoteArrangement.spacedBy(16.rdp),
    ) {
        // State selector buttons
        RemoteRow(
            horizontalArrangement = RemoteArrangement.spacedBy(8.rdp),
            verticalAlignment = RemoteAlignment.CenterVertically,
        ) {
            RemoteBox(
                modifier =
                    RemoteModifier.clip(RemoteRoundedCornerShape(8.rdp))
                        .background(Color(0xFF381E72).rc)
                        .clickable(action = valueChange(styleState, 0.ri))
                        .padding(horizontal = 10.rdp, vertical = 6.rdp),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText(text = "Minimal".rs, color = Color(0xFFD0BCFF).rc, fontSize = 12.rsp)
            }
            RemoteBox(
                modifier =
                    RemoteModifier.clip(RemoteRoundedCornerShape(8.rdp))
                        .background(Color(0xFF381E72).rc)
                        .clickable(action = valueChange(styleState, 1.ri))
                        .padding(horizontal = 10.rdp, vertical = 6.rdp),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText(text = "Accent Pill".rs, color = Color(0xFFD0BCFF).rc, fontSize = 12.rsp)
            }
            RemoteBox(
                modifier =
                    RemoteModifier.clip(RemoteRoundedCornerShape(8.rdp))
                        .background(Color(0xFF381E72).rc)
                        .clickable(action = valueChange(styleState, 2.ri))
                        .padding(horizontal = 10.rdp, vertical = 6.rdp),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText(text = "Badge Tilt".rs, color = Color(0xFFD0BCFF).rc, fontSize = 12.rsp)
            }
        }

        // Morphable Container
        RemoteBox(
            modifier =
                RemoteModifier.width(260.rdp)
                    .height(180.rdp)
                    .clip(RemoteRoundedCornerShape(16.rdp))
                    .background(Color(0xFF1E1E2C).rc)
                    .clickable(action = valueChange(styleState, (styleState + 1) % 3))
                    .padding(16.rdp),
            contentAlignment = RemoteAlignment.Center,
        ) {
            RemoteStateLayout(
                currentState = styleState,
                0,
                1,
                2,
                modifier =
                    RemoteModifier.animationSpec(
                        motionDuration = 500f,
                        motionEasingType = GeneralEasing.CUBIC_STANDARD,
                    ),
            ) { state ->
                when (state) {
                    0 -> {
                        // State 0: Minimal Style - Sharp rect, 1dp subtle border, scale 1.0, rot 0
                        RemoteBox(
                            modifier =
                                RemoteModifier.animationSpec(
                                        animationId = 50,
                                        motionDuration = 500f,
                                        motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                        visibilityDuration = 500f,
                                        visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                        enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                        exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                    )
                                    .width(220.rdp)
                                    .height(100.rdp)
                                    .border(
                                        1.rdp,
                                        Color(0xFF555566).rc,
                                        RemoteRoundedCornerShape(6.rdp),
                                    )
                                    .clip(RemoteRoundedCornerShape(6.rdp))
                                    .background(Color(0xFF282838).rc)
                                    .padding(12.rdp),
                            contentAlignment = RemoteAlignment.Center,
                        ) {
                            RemoteRow(
                                verticalAlignment = RemoteAlignment.CenterVertically,
                                horizontalArrangement = RemoteArrangement.spacedBy(12.rdp),
                            ) {
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.animationSpec(
                                                animationId = 51,
                                                motionDuration = 500f,
                                                motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                                visibilityDuration = 500f,
                                                visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                                enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                                exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                            )
                                            .size(36.rdp)
                                            .clip(RemoteRoundedCornerShape(4.rdp))
                                            .background(Color(0xFF5E81AC).rc),
                                    contentAlignment = RemoteAlignment.Center,
                                ) {
                                    RemoteText(
                                        text = "◆".rs,
                                        color = Color.White.rc,
                                        fontSize = 16.rsp,
                                    )
                                }
                                RemoteColumn(
                                    modifier =
                                        RemoteModifier.animationSpec(
                                            animationId = 52,
                                            motionDuration = 500f,
                                            motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                            visibilityDuration = 500f,
                                            visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                            enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                            exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                        )
                                ) {
                                    RemoteText(
                                        text = "Minimal Card".rs,
                                        color = Color.White.rc,
                                        fontSize = 15.rsp,
                                    )
                                    RemoteText(
                                        text = "1dp border • 6dp radius".rs,
                                        color = Color(0xFFAAAAAA).rc,
                                        fontSize = 11.rsp,
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // State 1: Accent Pill - Pill shape, 3dp bright border, scale 1.05
                        RemoteBox(
                            modifier =
                                RemoteModifier.animationSpec(
                                        animationId = 50,
                                        motionDuration = 500f,
                                        motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                        visibilityDuration = 500f,
                                        visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                        enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                        exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                    )
                                    .width(240.rdp)
                                    .height(110.rdp)
                                    .graphicsLayer(scaleX = 1.05f.rf, scaleY = 1.05f.rf)
                                    .border(
                                        3.rdp,
                                        Color(0xFF88C0D0).rc,
                                        RemoteRoundedCornerShape(32.rdp),
                                    )
                                    .clip(RemoteRoundedCornerShape(32.rdp))
                                    .background(Color(0xFF3B4252).rc)
                                    .padding(16.rdp),
                            contentAlignment = RemoteAlignment.Center,
                        ) {
                            RemoteRow(
                                verticalAlignment = RemoteAlignment.CenterVertically,
                                horizontalArrangement = RemoteArrangement.spacedBy(14.rdp),
                            ) {
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.animationSpec(
                                                animationId = 51,
                                                motionDuration = 500f,
                                                motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                                visibilityDuration = 500f,
                                                visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                                enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                                exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                            )
                                            .size(44.rdp)
                                            .rotate(45f.rf)
                                            .clip(RemoteRoundedCornerShape(22.rdp))
                                            .background(Color(0xFF88C0D0).rc),
                                    contentAlignment = RemoteAlignment.Center,
                                ) {
                                    RemoteText(
                                        text = "★".rs,
                                        color = Color(0xFF2E3440).rc,
                                        fontSize = 18.rsp,
                                    )
                                }
                                RemoteColumn(
                                    modifier =
                                        RemoteModifier.animationSpec(
                                            animationId = 52,
                                            motionDuration = 500f,
                                            motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                            visibilityDuration = 500f,
                                            visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                            enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                            exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                        )
                                ) {
                                    RemoteText(
                                        text = "Accent Pill".rs,
                                        color = Color.White.rc,
                                        fontSize = 16.rsp,
                                    )
                                    RemoteText(
                                        text = "3dp border • 32dp radius".rs,
                                        color = Color(0xFFD8DEE9).rc,
                                        fontSize = 11.rsp,
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        // State 2: Tilted Badge - No border (asymmetric), rotated -4 deg, scale
                        // 0.95
                        RemoteBox(
                            modifier =
                                RemoteModifier.animationSpec(
                                        animationId = 50,
                                        motionDuration = 500f,
                                        motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                        visibilityDuration = 500f,
                                        visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                        enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                        exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                    )
                                    .width(210.rdp)
                                    .height(95.rdp)
                                    .graphicsLayer(
                                        scaleX = 0.95f.rf,
                                        scaleY = 0.95f.rf,
                                        rotationZ = (-4f).rf,
                                    )
                                    .clip(RemoteRoundedCornerShape(18.rdp))
                                    .background(Color(0xFF4C566A).rc)
                                    .padding(12.rdp),
                            contentAlignment = RemoteAlignment.Center,
                        ) {
                            RemoteRow(
                                verticalAlignment = RemoteAlignment.CenterVertically,
                                horizontalArrangement = RemoteArrangement.spacedBy(10.rdp),
                            ) {
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.animationSpec(
                                                animationId = 51,
                                                motionDuration = 500f,
                                                motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                                visibilityDuration = 500f,
                                                visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                                enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                                exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                            )
                                            .size(38.rdp)
                                            .rotate((-15f).rf)
                                            .clip(RemoteRoundedCornerShape(10.rdp))
                                            .background(Color(0xFFB48EAD).rc),
                                    contentAlignment = RemoteAlignment.Center,
                                ) {
                                    RemoteText(
                                        text = "✦".rs,
                                        color = Color.White.rc,
                                        fontSize = 16.rsp,
                                    )
                                }
                                RemoteColumn(
                                    modifier =
                                        RemoteModifier.animationSpec(
                                            animationId = 52,
                                            motionDuration = 500f,
                                            motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                            visibilityDuration = 500f,
                                            visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                            enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                            exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                        )
                                ) {
                                    RemoteText(
                                        text = "Tilted Badge".rs,
                                        color = Color.White.rc,
                                        fontSize = 15.rsp,
                                    )
                                    RemoteText(
                                        text = "No border • -4° rotation".rs,
                                        color = Color(0xFFE5E9F0).rc,
                                        fontSize = 11.rsp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Demonstrates shared indicator movement combined with entering/exiting unshared content:
 * - Shared pill indicator moves across 3 tabs (Overview, Metrics, Settings)
 * - Each tab displays distinct content that fades in on enter and fades out on exit
 */
@Suppress("RestrictedApiAndroidX")
@RemoteComponentPreview
@Composable
@RemoteComposable
private fun RemoteStateLayoutTabContent() {
    val activeTab = rememberMutableRemoteInt(0)

    RemoteColumn(
        modifier =
            RemoteModifier.width(280.rdp)
                .clip(RemoteRoundedCornerShape(16.rdp))
                .background(Color(0xFF1B1D28).rc)
                .padding(14.rdp),
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
        verticalArrangement = RemoteArrangement.spacedBy(12.rdp),
    ) {
        // Tab Bar with Shared Indicator
        RemoteBox(
            modifier =
                RemoteModifier.fillMaxWidth()
                    .height(38.rdp)
                    .clip(RemoteRoundedCornerShape(19.rdp))
                    .background(Color(0xFF262938).rc)
                    .padding(3.rdp)
        ) {
            RemoteStateLayout(currentState = activeTab, 0, 1, 2) { tab ->
                val indicatorStart =
                    when (tab) {
                        0 -> 0.rdp
                        1 -> 88.rdp
                        else -> 176.rdp
                    }
                // Shared Element 60: Sliding Selection Pill
                RemoteRow(modifier = RemoteModifier.fillMaxSize()) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.padding(start = indicatorStart)
                                .animationSpec(
                                    animationId = 60,
                                    motionDuration = 400f,
                                    motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                    visibilityDuration = 400f,
                                    visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                    enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                    exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                )
                                .width(88.rdp)
                                .height(32.rdp)
                                .clip(RemoteRoundedCornerShape(16.rdp))
                                .background(Color(0xFF6C5CE7).rc)
                    )
                }
            }

            // Tab Text Buttons overlaid on top
            RemoteRow(
                modifier = RemoteModifier.fillMaxSize(),
                horizontalArrangement = RemoteArrangement.SpaceBetween,
                verticalAlignment = RemoteAlignment.CenterVertically,
            ) {
                RemoteBox(
                    modifier =
                        RemoteModifier.width(88.rdp)
                            .height(32.rdp)
                            .clickable(action = valueChange(activeTab, 0.ri)),
                    contentAlignment = RemoteAlignment.Center,
                ) {
                    RemoteText("Overview".rs, color = Color.White.rc, fontSize = 12.rsp)
                }
                RemoteBox(
                    modifier =
                        RemoteModifier.width(88.rdp)
                            .height(32.rdp)
                            .clickable(action = valueChange(activeTab, 1.ri)),
                    contentAlignment = RemoteAlignment.Center,
                ) {
                    RemoteText("Metrics".rs, color = Color.White.rc, fontSize = 12.rsp)
                }
                RemoteBox(
                    modifier =
                        RemoteModifier.width(88.rdp)
                            .height(32.rdp)
                            .clickable(action = valueChange(activeTab, 2.ri)),
                    contentAlignment = RemoteAlignment.Center,
                ) {
                    RemoteText("Settings".rs, color = Color.White.rc, fontSize = 12.rsp)
                }
            }
        }

        // Tab Content Pane with Enter/Exit animations
        RemoteBox(
            modifier =
                RemoteModifier.fillMaxWidth()
                    .height(130.rdp)
                    .clip(RemoteRoundedCornerShape(12.rdp))
                    .background(Color(0xFF222533).rc)
                    .padding(12.rdp),
            contentAlignment = RemoteAlignment.Center,
        ) {
            RemoteStateLayout(currentState = activeTab, 0, 1, 2) { tab ->
                when (tab) {
                    0 -> {
                        // Pane 0: Overview
                        RemoteColumn(
                            modifier =
                                RemoteModifier.animationSpec(
                                    motionDuration = 350f,
                                    motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                    visibilityDuration = 350f,
                                    visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                    enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                    exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                ),
                            verticalArrangement = RemoteArrangement.spacedBy(8.rdp),
                            horizontalAlignment = RemoteAlignment.CenterHorizontally,
                        ) {
                            RemoteText(
                                "Monthly Active Users".rs,
                                color = Color(0xFFAAAAAA).rc,
                                fontSize = 12.rsp,
                            )
                            RemoteText(
                                "142,850".rs,
                                color = Color.White.rc,
                                fontSize = 24.rsp,
                            )
                            RemoteText(
                                "+12.4% from last month".rs,
                                color = Color(0xFF00B894).rc,
                                fontSize = 11.rsp,
                            )
                        }
                    }
                    1 -> {
                        // Pane 1: Metrics
                        RemoteColumn(
                            modifier =
                                RemoteModifier.animationSpec(
                                    motionDuration = 350f,
                                    motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                    visibilityDuration = 350f,
                                    visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                    enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                    exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                ),
                            verticalArrangement = RemoteArrangement.spacedBy(8.rdp),
                        ) {
                            MetricBar(label = "CPU", percent = 68, barWidth = 140)
                            MetricBar(label = "Memory", percent = 42, barWidth = 90)
                            MetricBar(label = "Storage", percent = 85, barWidth = 175)
                        }
                    }
                    else -> {
                        // Pane 2: Settings
                        RemoteColumn(
                            modifier =
                                RemoteModifier.animationSpec(
                                    motionDuration = 350f,
                                    motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                    visibilityDuration = 350f,
                                    visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                    enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                    exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                ),
                            verticalArrangement = RemoteArrangement.spacedBy(10.rdp),
                        ) {
                            RemoteRow(
                                modifier = RemoteModifier.fillMaxWidth(),
                                horizontalArrangement = RemoteArrangement.SpaceBetween,
                                verticalAlignment = RemoteAlignment.CenterVertically,
                            ) {
                                RemoteText(
                                    "Push Alerts".rs,
                                    color = Color.White.rc,
                                    fontSize = 12.rsp,
                                )
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.clip(RemoteRoundedCornerShape(8.rdp))
                                            .background(Color(0xFF00B894).rc)
                                            .padding(horizontal = 8.rdp, vertical = 2.rdp)
                                ) {
                                    RemoteText("ON".rs, color = Color.White.rc, fontSize = 10.rsp)
                                }
                            }
                            RemoteRow(
                                modifier = RemoteModifier.fillMaxWidth(),
                                horizontalArrangement = RemoteArrangement.SpaceBetween,
                                verticalAlignment = RemoteAlignment.CenterVertically,
                            ) {
                                RemoteText(
                                    "Auto Sync".rs,
                                    color = Color.White.rc,
                                    fontSize = 12.rsp,
                                )
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.clip(RemoteRoundedCornerShape(8.rdp))
                                            .background(Color(0xFF00B894).rc)
                                            .padding(horizontal = 8.rdp, vertical = 2.rdp)
                                ) {
                                    RemoteText("ON".rs, color = Color.White.rc, fontSize = 10.rsp)
                                }
                            }
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
private fun MetricBar(label: String, percent: Int, barWidth: Int) {
    RemoteRow(
        modifier = RemoteModifier.fillMaxWidth(),
        verticalAlignment = RemoteAlignment.CenterVertically,
        horizontalArrangement = RemoteArrangement.SpaceBetween,
    ) {
        RemoteText(label.rs, color = Color.LightGray.rc, fontSize = 11.rsp)
        RemoteBox(
            modifier =
                RemoteModifier.width(180.rdp)
                    .height(6.rdp)
                    .clip(RemoteRoundedCornerShape(3.rdp))
                    .background(Color(0xFF333344).rc)
        ) {
            RemoteBox(
                modifier =
                    RemoteModifier.width(barWidth.rdp)
                        .height(6.rdp)
                        .clip(RemoteRoundedCornerShape(3.rdp))
                        .background(Color(0xFF6C5CE7).rc)
            )
        }
        RemoteText("$percent%".rs, color = Color.White.rc, fontSize = 10.rsp)
    }
}

/**
 * Demonstrates rapid multi-state switching and mid-flight interruption:
 * - 4-step progress flow (Cart -> Shipping -> Payment -> Success)
 * - Shared progress indicator morphs across the 4 steps
 * - Provides Next/Prev and Direct Step jumps for rapid-fire input testing
 */
@Suppress("RestrictedApiAndroidX")
@RemoteComponentPreview
@Composable
@RemoteComposable
private fun RemoteStateLayoutStepperInterruption() {
    val step = rememberMutableRemoteInt(0)

    RemoteColumn(
        modifier =
            RemoteModifier.width(280.rdp)
                .clip(RemoteRoundedCornerShape(16.rdp))
                .background(Color(0xFF161823).rc)
                .padding(16.rdp),
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
        verticalArrangement = RemoteArrangement.spacedBy(14.rdp),
    ) {
        // Step progress header with shared indicator
        RemoteBox(
            modifier = RemoteModifier.fillMaxWidth().height(36.rdp),
            contentAlignment = RemoteAlignment.CenterStart,
        ) {
            // Track line behind steps
            RemoteBox(
                modifier =
                    RemoteModifier.fillMaxWidth()
                        .height(4.rdp)
                        .clip(RemoteRoundedCornerShape(2.rdp))
                        .background(Color(0xFF2C3048).rc)
            )

            RemoteStateLayout(currentState = step, 0, 1, 2, 3) { currentStep ->
                val pillOffset =
                    when (currentStep) {
                        0 -> 0.rdp
                        1 -> 76.rdp
                        2 -> 152.rdp
                        else -> 228.rdp
                    }
                // Shared Element 70: Progress Badge
                RemoteBox(
                    modifier =
                        RemoteModifier.padding(start = pillOffset)
                            .animationSpec(
                                animationId = 70,
                                motionDuration = 450f,
                                motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                visibilityDuration = 450f,
                                visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                            )
                            .size(32.rdp)
                            .clip(RemoteRoundedCornerShape(16.rdp))
                            .background(Color(0xFF00CEC9).rc),
                    contentAlignment = RemoteAlignment.Center,
                ) {
                    RemoteText(
                        text = "${currentStep + 1}".rs,
                        color = Color(0xFF0F172A).rc,
                        fontSize = 14.rsp,
                    )
                }
            }
        }

        // Step description card
        RemoteBox(
            modifier =
                RemoteModifier.fillMaxWidth()
                    .height(90.rdp)
                    .clip(RemoteRoundedCornerShape(12.rdp))
                    .background(Color(0xFF202334).rc)
                    .padding(12.rdp),
            contentAlignment = RemoteAlignment.Center,
        ) {
            RemoteStateLayout(currentState = step, 0, 1, 2, 3) { s ->
                val title =
                    when (s) {
                        0 -> "Step 1: Review Cart"
                        1 -> "Step 2: Shipping Address"
                        2 -> "Step 3: Payment Method"
                        else -> "Step 4: Order Confirmed!"
                    }
                val subtitle =
                    when (s) {
                        0 -> "3 items in your shopping cart"
                        1 -> "Deliver to 1600 Amphitheatre Pkwy"
                        2 -> "Google Pay (•••• 4242)"
                        else -> "Thank you for your order!"
                    }
                RemoteColumn(
                    modifier =
                        RemoteModifier.animationSpec(
                            motionDuration = 300f,
                            motionEasingType = GeneralEasing.CUBIC_STANDARD,
                            visibilityDuration = 300f,
                            visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                            enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                            exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                        ),
                    horizontalAlignment = RemoteAlignment.CenterHorizontally,
                    verticalArrangement = RemoteArrangement.spacedBy(4.rdp),
                ) {
                    RemoteText(title.rs, color = Color.White.rc, fontSize = 15.rsp)
                    RemoteText(subtitle.rs, color = Color(0xFFAAAAAA).rc, fontSize = 12.rsp)
                }
            }
        }

        // Stepper Navigation Controls
        RemoteRow(
            modifier = RemoteModifier.fillMaxWidth(),
            horizontalArrangement = RemoteArrangement.SpaceBetween,
            verticalAlignment = RemoteAlignment.CenterVertically,
        ) {
            RemoteBox(
                modifier =
                    RemoteModifier.clip(RemoteRoundedCornerShape(8.rdp))
                        .background(Color(0xFF2C3048).rc)
                        .clickable(action = valueChange(step, (step + 3) % 4))
                        .padding(horizontal = 14.rdp, vertical = 6.rdp),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText("◀ Prev".rs, color = Color.White.rc, fontSize = 12.rsp)
            }

            RemoteText(
                text = "Rapid click to test interruption".rs,
                color = Color.Gray.rc,
                fontSize = 9.rsp,
            )

            RemoteBox(
                modifier =
                    RemoteModifier.clip(RemoteRoundedCornerShape(8.rdp))
                        .background(Color(0xFF00CEC9).rc)
                        .clickable(action = valueChange(step, (step + 1) % 4))
                        .padding(horizontal = 14.rdp, vertical = 6.rdp),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText("Next ▶".rs, color = Color(0xFF0F172A).rc, fontSize = 12.rsp)
            }
        }
    }
}

/**
 * Demonstrates hierarchy isolation with nested StateLayouts:
 * - Outer StateLayout switches dashboard view between Expanded Card and Compact List
 * - Inner nested StateLayout toggles an embedded status indicator (Online <-> Away <-> Busy)
 * - Toggling the inner state does not trigger or reset the outer transition, and vice versa
 */
@Suppress("RestrictedApiAndroidX")
@RemoteComponentPreview
@Composable
@RemoteComposable
private fun RemoteStateLayoutNestedDashboard() {
    val outerExpanded = rememberMutableRemoteBoolean(false)
    val innerStatus = rememberMutableRemoteInt(0)

    RemoteColumn(
        modifier =
            RemoteModifier.width(280.rdp)
                .clip(RemoteRoundedCornerShape(16.rdp))
                .background(Color(0xFF1A1C29).rc)
                .padding(14.rdp),
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
        verticalArrangement = RemoteArrangement.spacedBy(12.rdp),
    ) {
        // Outer Switch Button
        RemoteBox(
            modifier =
                RemoteModifier.clip(RemoteRoundedCornerShape(8.rdp))
                    .background(Color(0xFF4A4E69).rc)
                    .clickable(action = valueChange(outerExpanded, !outerExpanded))
                    .padding(horizontal = 12.rdp, vertical = 6.rdp),
            contentAlignment = RemoteAlignment.Center,
        ) {
            RemoteText(
                text = "Toggle Outer View".rs,
                color = Color.White.rc,
                fontSize = 12.rsp,
            )
        }

        // Outer StateLayout
        RemoteStateLayout(currentState = outerExpanded) { expanded ->
            if (!expanded) {
                // Outer State 0: Compact Row Layout
                RemoteRow(
                    modifier =
                        RemoteModifier.fillMaxWidth()
                            .clip(RemoteRoundedCornerShape(12.rdp))
                            .background(Color(0xFF222232).rc)
                            .padding(10.rdp),
                    verticalAlignment = RemoteAlignment.CenterVertically,
                    horizontalArrangement = RemoteArrangement.SpaceBetween,
                ) {
                    RemoteColumn(
                        modifier =
                            RemoteModifier.animationSpec(
                                animationId = 80,
                                motionDuration = 450f,
                                motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                visibilityDuration = 450f,
                                visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                            )
                    ) {
                        RemoteText("Dev Server".rs, color = Color.White.rc, fontSize = 14.rsp)
                        RemoteText("Port 8080".rs, color = Color.Gray.rc, fontSize = 11.rsp)
                    }

                    // Nested StateLayout inside the row
                    NestedStatusWidget(innerStatus = innerStatus)
                }
            } else {
                // Outer State 1: Expanded Card Layout
                RemoteColumn(
                    modifier =
                        RemoteModifier.fillMaxWidth()
                            .clip(RemoteRoundedCornerShape(12.rdp))
                            .background(Color(0xFF222232).rc)
                            .padding(14.rdp),
                    verticalArrangement = RemoteArrangement.spacedBy(10.rdp),
                    horizontalAlignment = RemoteAlignment.CenterHorizontally,
                ) {
                    RemoteColumn(
                        modifier =
                            RemoteModifier.animationSpec(
                                animationId = 80,
                                motionDuration = 450f,
                                motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                visibilityDuration = 450f,
                                visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                            ),
                        horizontalAlignment = RemoteAlignment.CenterHorizontally,
                    ) {
                        RemoteText(
                            "Dev Server (Expanded)".rs,
                            color = Color.White.rc,
                            fontSize = 16.rsp,
                        )
                        RemoteText(
                            "Production Mirror • Port 8080".rs,
                            color = Color.Gray.rc,
                            fontSize = 12.rsp,
                        )
                    }

                    // Non-shared metrics in expanded view
                    RemoteText(
                        text = "Latency: 24ms • Uptime: 99.98%".rs,
                        color = Color(0xFFAAAAAA).rc,
                        fontSize = 11.rsp,
                    )

                    // Nested StateLayout inside expanded card
                    NestedStatusWidget(innerStatus = innerStatus)
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
@RemoteComposable
private fun NestedStatusWidget(innerStatus: MutableRemoteInt) {
    RemoteBox(
        modifier =
            RemoteModifier.clickable(action = valueChange(innerStatus, (innerStatus + 1) % 3))
    ) {
        RemoteStateLayout(currentState = innerStatus, 0, 1, 2) { st ->
            val color =
                when (st) {
                    0 -> Color(0xFF00B894)
                    1 -> Color(0xFFFDCB6E)
                    else -> Color(0xFFD63031)
                }
            val label =
                when (st) {
                    0 -> "● Online"
                    1 -> "● Away"
                    else -> "● Busy"
                }
            RemoteBox(
                modifier =
                    RemoteModifier.animationSpec(
                            animationId = 85,
                            motionDuration = 300f,
                            motionEasingType = GeneralEasing.CUBIC_STANDARD,
                            visibilityDuration = 300f,
                            visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                            enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                            exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                        )
                        .clip(RemoteRoundedCornerShape(12.rdp))
                        .background(color.rc)
                        .padding(horizontal = 10.rdp, vertical = 4.rdp),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText(text = label.rs, color = Color.White.rc, fontSize = 11.rsp)
            }
        }
    }
}

/** Main container demo combining all StateLayout shared element showcases. */
@Suppress("RestrictedApiAndroidX")
@Composable
fun RemoteStateLayoutSharedElementsDemo() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles =
        listOf(
            "Card Expansion",
            "Media Player",
            "Morph Grid",
            "Modifier Transitions",
            "Tabbed Content",
            "Interruption Stepper",
            "Nested Dashboard",
        )

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 8.dp) {
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
            3 -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Morphing shapes, borders, scale, and rotation across states",
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    RemoteDemo { RemoteStateLayoutModifierTransitions() }
                }
            }
            4 -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Shared pill indicator sliding with entering/exiting unshared views",
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    RemoteDemo { RemoteStateLayoutTabContent() }
                }
            }
            5 -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Multi-step flow testing rapid mid-flight transition interruption",
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    RemoteDemo { RemoteStateLayoutStepperInterruption() }
                }
            }
            6 -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Hierarchy isolation: Outer view switch with nested status toggle",
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    RemoteDemo { RemoteStateLayoutNestedDashboard() }
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
