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

package com.android.compose.animation.scene.demo.transitions

import androidx.compose.animation.core.tween
import com.android.compose.animation.scene.SceneTransitionsBuilder
import com.android.compose.animation.scene.TransitionBuilder
import com.android.compose.animation.scene.demo.MediaPlayer
import com.android.compose.animation.scene.demo.QuickSettings
import com.android.compose.animation.scene.demo.QuickSettingsGrid
import com.android.compose.animation.scene.demo.Scenes
import com.android.compose.animation.scene.demo.Shade
import com.android.compose.animation.scene.demo.SplitShade
import com.android.compose.animation.scene.demo.notification.NotificationList

fun SceneTransitionsBuilder.splitShadeTransitions() {
    to(Scenes.SplitShade) {
        spec = tween(durationMillis = 500)
        toSplitShadeTransformations()
    }

    from(Scenes.SplitShade) {
        spec = tween(durationMillis = 500)

        // Same transition as when going *to* the split shade, except that we never share
        // notifications.
        reversed { toSplitShadeTransformations() }
        sharedElement(NotificationList.Elements.Notifications, enabled = false)
    }
}

val ToSplitShadeOpaqueBackgroundProgress = 0.5f

private fun TransitionBuilder.toSplitShadeTransformations() {
    sharedElement(Shade.Elements.BatteryPercentage, enabled = false)
    sharedElement(QuickSettings.Elements.Operator, enabled = false)

    fractionRange(end = ToSplitShadeOpaqueBackgroundProgress) {
        fade(SplitShade.Elements.Background)
    }

    fractionRange(start = ToSplitShadeOpaqueBackgroundProgress) {
        fade(Shade.Elements.Time)
        fade(Shade.Elements.Date)
        fade(Shade.Elements.BatteryPercentage)
        fade(Shade.Elements.ScrimBackground)

        fade(QuickSettings.Elements.Operator)
        fade(QuickSettings.Elements.BrightnessSlider)
        fade(QuickSettings.Elements.FooterActions)
        fade(QuickSettings.Elements.PagerIndicators)

        fade(MediaPlayer.Elements.MediaPlayer)

        fade(QuickSettingsGrid.Elements.Tiles)
        scaleSize(QuickSettingsGrid.Elements.Tiles, height = 0.5f)

        fade(NotificationList.Elements.Notifications)
    }
}
