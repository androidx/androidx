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

package com.android.compose.animation.scene.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.android.compose.animation.scene.Back
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.Swipe
import com.android.compose.animation.scene.UserActionResult
import com.android.compose.animation.scene.UserActionResult.ShowOverlay
import com.android.compose.animation.scene.UserActionResult.ShowOverlay.HideCurrentOverlays

object QuickSettingsShade {
    object Elements {
        val Root = ElementKey("QuickSettingsShadeRoot")
        val Content = ElementKey("QuickSettingsShadeContent")
    }

    val UserActions =
        mapOf(
            Back to UserActionResult.HideOverlay(Overlays.QuickSettings),
            Swipe.Up to UserActionResult.HideOverlay(Overlays.QuickSettings),
            Swipe.Down(fromSource = SceneContainerArea.StartHalf) to
                ShowOverlay(
                    Overlays.Notifications,
                    hideCurrentOverlays = HideCurrentOverlays.Some(Overlays.QuickSettings),
                ),
        )
}

@Composable
fun ContentScope.QuickSettingsShade(
    qsPager: @Composable ContentScope.(revealEffect: Boolean) -> Unit,
    mediaPlayer: @Composable (ContentScope.(revealEffect: Boolean) -> Unit)?,
    revealEffect: Boolean,
    modifier: Modifier = Modifier,
) {
    PartialShade(
        rootElement = QuickSettingsShade.Elements.Root,
        modifier = modifier,
        revealEffect = revealEffect,
    ) {
        Column(Modifier.element(QuickSettingsShade.Elements.Content)) {
            if (mediaPlayer != null) {
                // Ensure that the media player is above the QS tiles when they fade in.
                Box(Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp).zIndex(1f)) {
                    mediaPlayer(revealEffect)
                }
            }

            Spacer(Modifier.padding(top = 16.dp))
            qsPager(revealEffect)
        }
    }
}
