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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.SceneKey

object SplitLockscreen {
    fun userActions(
        isLockscreenDismissable: Boolean,
        shadeScene: SceneKey,
        configuration: DemoConfiguration,
    ) =
        Lockscreen.userActions(
            isLockscreenDismissable,
            shadeScene,
            requiresFullDistanceSwipeToShade = false,
            configuration,
            fastSwipeToQuickSettings = false,
        )
}

@Composable
fun ContentScope.SplitLockscreen(
    notificationList: @Composable ContentScope.() -> Unit,
    mediaPlayer: @Composable (ContentScope.() -> Unit)?,
    isDismissable: Boolean,
    onToggleDismissable: () -> Unit,
    onChangeScene: (SceneKey) -> Unit,
    configuration: DemoConfiguration,
    modifier: Modifier = Modifier,
) {
    Column(modifier.element(Lockscreen.Elements.Scene).fillMaxSize()) {
        StatusBar(showDateAndTime = false, Modifier.padding(horizontal = 16.dp))
        Row(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.weight(1f).padding(16.dp)) {
                Clock(MaterialTheme.colorScheme.onSurfaceVariant)
                SmartSpace(MaterialTheme.colorScheme.onSurface)

                if (mediaPlayer != null) {
                    val endPadding = if (configuration.enableOverlays) 16.dp else 0.dp
                    Box(Modifier.padding(top = 32.dp, start = 16.dp, end = endPadding)) {
                        mediaPlayer()
                    }
                }

                if (configuration.enableOverlays) {
                    notificationList()
                }
            }

            Box(Modifier.weight(1f).padding(16.dp)) {
                if (!configuration.enableOverlays) {
                    notificationList()
                }
            }
        }

        LockButton(
            isDismissable,
            onToggleDismissable,
            onChangeScene,
            Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp),
        )

        LockscreenCameraButton(
            onClick = { onChangeScene(Scenes.Camera) },
            Modifier.align(Alignment.End),
        )
    }
}
