/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.compose.animation.scene.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.android.compose.animation.scene.ContentKey
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.MovableElementContentPicker
import com.android.compose.animation.scene.MovableElementKey
import com.android.compose.animation.scene.StaticElementContentPicker
import com.android.compose.animation.scene.content.state.TransitionState
import com.android.compose.modifiers.thenIf
import com.android.mechanics.compose.modifier.verticalFadeContentReveal
import com.android.mechanics.compose.modifier.verticalTactileSurfaceReveal

object MediaPlayer {
    object Elements {
        val MediaPlayer = MovableElementKey("MediaPlayer", contentPicker = ContentPicker)
        val SmallMediaPlayer =
            MovableElementKey(
                "SmallMediaPlayer",
                contentPicker = MovableElementContentPicker(setOf(Overlays.QuickSettings)),
            )
    }

    object Dimensions {
        val HeightLarge = 150.dp
        val HeightSmall = 70.dp
    }

    object Shapes {
        val Background = RoundedCornerShape(24.dp)
    }

    object ContentPicker : StaticElementContentPicker {
        override val contents =
            setOf(
                Scenes.Lockscreen,
                Scenes.SplitLockscreen,
                Scenes.Shade,
                Scenes.SplitShade,
                Scenes.QuickSettings,
                Overlays.Notifications,
            )

        override fun contentDuringTransition(
            element: ElementKey,
            transition: TransitionState.Transition,
            fromContentZIndex: Long,
            toContentZIndex: Long,
        ): ContentKey {
            return when {
                // During the Lockscreen => Shade transition, the media player is visible in the
                // Lockscreen when progress is in [0; 0.5]. It is then visible in the Shade scene
                // when progress is in [0.8; 1]. We move it half-way through, when progress = 0.65.
                transition.isTransitioning(from = Scenes.Lockscreen, to = Scenes.Shade) -> {
                    if (transition.progress < 0.65f) {
                        Scenes.Lockscreen
                    } else {
                        Scenes.Shade
                    }
                }

                // Same as Lockscreen => Shade, but with reversed progress.
                transition.isTransitioning(from = Scenes.Shade, to = Scenes.Lockscreen) -> {
                    if (transition.progress < 1f - 0.65f) {
                        Scenes.Shade
                    } else {
                        Scenes.Lockscreen
                    }
                }

                // When going from QS <=> Shade we always want to compose the media player in the QS
                // scene, otherwise it will be drawn above the QS footer actions.
                transition.isTransitioningBetween(Scenes.QuickSettings, Scenes.Shade) -> {
                    Scenes.QuickSettings
                }

                // When going from SplitLockscreen to SplitShade, we always compose the media player
                // in the SplitShade, otherwise the shade will fade above it.
                transition.isTransitioningBetween(Scenes.SplitLockscreen, Scenes.SplitShade) ->
                    Scenes.SplitShade
                transition.isTransitioningBetween(Scenes.Lockscreen, Scenes.QuickSettings) ->
                    Scenes.QuickSettings
                transition.isTransitioningFromOrTo(Overlays.Notifications) -> Overlays.Notifications
                else -> pickSingleContentIn(contents, transition, element)
            }
        }
    }
}

@Composable
fun ContentScope.MediaPlayer(
    presentationStyle: DemoMediaPresentationStyle,
    isPlaying: Boolean,
    onIsPlayingChange: (Boolean) -> Unit,
    onVisibilityChange: (Boolean) -> Unit,
    revealEffect: Boolean,
    modifier: Modifier = Modifier,
) {
    val injectedContentOrNull = LocalDependencies.current.mediaPlayer
    if (injectedContentOrNull != null) {
        Element(
            key =
                if (presentationStyle != DemoMediaPresentationStyle.Compact) {
                    MediaPlayer.Elements.MediaPlayer
                } else {
                    MediaPlayer.Elements.SmallMediaPlayer
                },
            modifier = modifier.fillMaxWidth(),
        ) {
            injectedContentOrNull(presentationStyle, onVisibilityChange)
        }
    } else {
        val isSmall = presentationStyle == DemoMediaPresentationStyle.Compact
        val key =
            if (isSmall) {
                MediaPlayer.Elements.SmallMediaPlayer
            } else {
                MediaPlayer.Elements.MediaPlayer
            }

        val deltaY = -with(LocalDensity.current) { QuickSettingsGrid.Dimensions.Spacing.toPx() }

        MovableElement(
            key,
            modifier
                .thenIf(revealEffect) {
                    Modifier.verticalTactileSurfaceReveal(deltaY = deltaY, label = "mediaPlayer")
                }
                .fillMaxWidth()
                .height(
                    if (isSmall) MediaPlayer.Dimensions.HeightSmall
                    else MediaPlayer.Dimensions.HeightLarge
                ),
        ) {
            content {
                Box(
                    Modifier.background(
                            MaterialTheme.colorScheme.tertiary,
                            MediaPlayer.Shapes.Background,
                        )
                        .thenIf(revealEffect) {
                            Modifier.verticalFadeContentReveal(
                                deltaY = deltaY,
                                label = "mediaPlayer",
                            )
                        }
                        .padding(8.dp)
                ) {
                    FilledIconButton(
                        onClick = { onIsPlayingChange(!isPlaying) },
                        Modifier.align(Alignment.CenterEnd),
                        colors =
                            IconButtonDefaults.filledIconButtonColors(
                                MaterialTheme.colorScheme.onTertiary
                            ),
                    ) {
                        val color = MaterialTheme.colorScheme.tertiary
                        if (isPlaying) {
                            Icon(Icons.Default.Pause, null, tint = color)
                        } else {
                            Icon(Icons.Default.PlayArrow, null, tint = color)
                        }
                    }
                }
            }
        }
    }
}

enum class DemoMediaPresentationStyle {
    Default,
    Compressed,
    Compact,
}
