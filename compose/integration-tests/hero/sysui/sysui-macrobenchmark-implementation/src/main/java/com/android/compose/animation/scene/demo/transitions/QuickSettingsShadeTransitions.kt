/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.compose.animation.scene.demo.transitions

import androidx.compose.animation.core.tween
import com.android.compose.animation.scene.SceneTransitionsBuilder
import com.android.compose.animation.scene.and
import com.android.compose.animation.scene.demo.Clock
import com.android.compose.animation.scene.demo.MediaPlayer
import com.android.compose.animation.scene.demo.Overlays
import com.android.compose.animation.scene.demo.QuickSettings
import com.android.compose.animation.scene.demo.QuickSettingsGrid
import com.android.compose.animation.scene.demo.QuickSettingsShade
import com.android.compose.animation.scene.demo.Scenes
import com.android.compose.animation.scene.demo.notification.NotificationList
import com.android.compose.animation.scene.inContent
import com.android.compose.animation.scene.or
import com.android.compose.animation.scene.reveal.ContainerRevealHaptics
import com.android.compose.animation.scene.reveal.verticalContainerReveal
import com.android.mechanics.behavior.VerticalExpandContainerSpec

val QuickSettingsToNotificationShadeFadeProgress = 0.5f

fun SceneTransitionsBuilder.quickSettingsShadeTransitions(
    revealHaptics: ContainerRevealHaptics,
    shadeMotionSpec: VerticalExpandContainerSpec,
) {
    to(Overlays.QuickSettings) {
        spec = tween(500)

        sharedElement(MediaPlayer.Elements.MediaPlayer, elevateInContent = Overlays.QuickSettings)
        verticalContainerReveal(QuickSettingsShade.Elements.Root, shadeMotionSpec, revealHaptics)
    }

    from(Overlays.QuickSettings, to = Overlays.Notifications) {
        spec = tween(500)

        // Don't share the notifications with lockscreen when replacing the notification shade with
        // the QS one.
        sharedElement(NotificationList.Elements.Notifications, enabled = false)

        // Elevate the media player so that they are not clipped when shared with the split
        // lockscreen.
        sharedElement(MediaPlayer.Elements.MediaPlayer, elevateInContent = Overlays.QuickSettings)
        sharedElement(Clock.Elements.Clock, enabled = false)

        fractionRange(end = QuickSettingsToNotificationShadeFadeProgress) {
            fade(MediaPlayer.Elements.MediaPlayer)
            fade(QuickSettingsGrid.Elements.Tiles)
            fade(QuickSettings.Elements.PagerIndicators)
            fade(
                NotificationList.Elements.Notifications and
                    (inContent(Scenes.Lockscreen) or inContent(Scenes.SplitLockscreen))
            )
        }
        fractionRange(start = QuickSettingsToNotificationShadeFadeProgress) {
            fade(Clock.Elements.Clock)
            fade(NotificationList.Elements.Notifications and inContent(Overlays.Notifications))
        }
    }
}
