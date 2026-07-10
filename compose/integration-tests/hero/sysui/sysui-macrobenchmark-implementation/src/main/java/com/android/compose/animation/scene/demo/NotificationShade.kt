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
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.compose.animation.scene.Back
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.Swipe
import com.android.compose.animation.scene.UserActionResult
import com.android.compose.animation.scene.UserActionResult.ShowOverlay
import com.android.compose.animation.scene.UserActionResult.ShowOverlay.HideCurrentOverlays

object NotificationShade {
    object Elements {
        val Root = ElementKey("NotificationShadeRoot")
        val Content = ElementKey("NotificationShadeContent")
    }

    val UserActions =
        mapOf(
            Back to UserActionResult.HideOverlay(Overlays.Notifications),
            Swipe.Up to UserActionResult.HideOverlay(Overlays.Notifications),
            Swipe.Down(fromSource = SceneContainerArea.EndHalf) to
                ShowOverlay(
                    Overlays.QuickSettings,
                    hideCurrentOverlays = HideCurrentOverlays.Some(Overlays.Notifications),
                ),
        )
}

@Composable
fun ContentScope.NotificationShade(
    clock: (@Composable ContentScope.() -> Unit)?,
    mediaPlayer: (@Composable ContentScope.() -> Unit)?,
    notificationList: @Composable ContentScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    PartialShade(NotificationShade.Elements.Root, modifier) {
        Column(Modifier.element(NotificationShade.Elements.Content)) {
            if (clock != null || mediaPlayer != null) {
                Column(Modifier.padding(horizontal = 16.dp).padding(top = 16.dp)) {
                    clock?.let { it() }
                    mediaPlayer?.let { it() }
                }
            }

            // Don't resize the notifications during the reveal.
            Box(Modifier.noResizeDuringTransitions()) { notificationList() }
        }
    }
}
