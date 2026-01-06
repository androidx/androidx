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
import com.android.compose.animation.scene.TransitionBuilder
import com.android.compose.animation.scene.demo.Clock
import com.android.compose.animation.scene.demo.MediaPlayer
import com.android.compose.animation.scene.demo.NotificationShade
import com.android.compose.animation.scene.demo.Overlays
import com.android.compose.animation.scene.demo.notification.NotificationList
import com.android.compose.animation.scene.reveal.ContainerRevealHaptics
import com.android.compose.animation.scene.reveal.verticalContainerReveal
import com.android.mechanics.behavior.VerticalExpandContainerSpec

fun SceneTransitionsBuilder.notificationShadeTransitions(
    revealHaptics: ContainerRevealHaptics,
    shadeMotionSpec: VerticalExpandContainerSpec,
) {
    to(Overlays.Notifications) {
        spec = tween(500)
        toNotificationShade(revealHaptics, shadeMotionSpec)
        sharedElement(Clock.Elements.Clock, elevateInContent = Overlays.Notifications)
        sharedElement(MediaPlayer.Elements.MediaPlayer, elevateInContent = Overlays.Notifications)
        sharedElement(
            NotificationList.Elements.Notifications,
            elevateInContent = Overlays.Notifications,
        )
    }

    from(Overlays.Notifications) {
        spec = tween(500)
        reversed { toNotificationShade(revealHaptics, shadeMotionSpec) }
        sharedElement(Clock.Elements.Clock, enabled = false)
        sharedElement(MediaPlayer.Elements.MediaPlayer, enabled = false)
        sharedElement(NotificationList.Elements.Notifications, enabled = false)
    }
}

val ToNotificationShadeStartFadeProgress = 0.5f

private fun TransitionBuilder.toNotificationShade(
    revealHaptics: ContainerRevealHaptics,
    shadeMotionSpec: VerticalExpandContainerSpec,
) {
    verticalContainerReveal(NotificationShade.Elements.Root, shadeMotionSpec, revealHaptics)
}
