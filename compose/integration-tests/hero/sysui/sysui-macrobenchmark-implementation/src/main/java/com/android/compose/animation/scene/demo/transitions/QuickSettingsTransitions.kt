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
import com.android.compose.animation.scene.demo.MediaPlayer
import com.android.compose.animation.scene.demo.QuickSettings
import com.android.compose.animation.scene.demo.QuickSettingsGrid
import com.android.compose.animation.scene.demo.Scenes
import com.android.compose.animation.scene.demo.Shade

val QuickSettingsBackgroundEndProgress = 0.5f

fun SceneTransitionsBuilder.quickSettingsTransitions() {
    to(Scenes.QuickSettings) {
        spec = tween(durationMillis = 500)

        // TODO(b/308961608): Share those elements and animate their color.
        sharedElement(Shade.Elements.BatteryPercentage)
        sharedElement(QuickSettings.Elements.Operator)

        fractionRange(end = QuickSettingsBackgroundEndProgress) {
            fade(QuickSettings.Elements.Background)
        }

        fractionRange(start = QuickSettingsBackgroundEndProgress) {
            fade(Shade.Elements.Time)
            fade(Shade.Elements.BatteryPercentage)
            fade(QuickSettings.Elements.Date)
            fade(QuickSettings.Elements.Operator)
            fade(QuickSettings.Elements.BrightnessSlider)
            fade(QuickSettings.Elements.ExpandedGrid)
            fade(QuickSettings.Elements.PagerIndicators)
            fade(QuickSettings.Elements.FooterActions)
            fade(MediaPlayer.Elements.MediaPlayer)

            scaleSize(QuickSettingsGrid.Elements.Tiles, height = 0.5f)
        }
    }
}
