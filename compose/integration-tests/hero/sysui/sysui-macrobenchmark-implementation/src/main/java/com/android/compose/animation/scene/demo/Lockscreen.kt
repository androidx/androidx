/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.SceneKey
import com.android.compose.animation.scene.Swipe
import com.android.compose.animation.scene.UserAction
import com.android.compose.animation.scene.UserActionResult

object Lockscreen {
    fun userActions(
        isLockscreenDismissable: Boolean,
        shadeScene: SceneKey,
        requiresFullDistanceSwipeToShade: Boolean,
        configuration: DemoConfiguration,
        fastSwipeToQuickSettings: Boolean = true,
    ): Map<UserAction, UserActionResult> {
        return buildList {
                if (configuration.enableOverlays) {
                    add(
                        Swipe.Down(fromSource = SceneContainerArea.StartHalf) to
                            UserActionResult.ShowOverlay(Overlays.Notifications)
                    )
                    add(
                        Swipe.Down(fromSource = SceneContainerArea.EndHalf) to
                            UserActionResult.ShowOverlay(Overlays.QuickSettings)
                    )
                } else {
                    add(
                        Swipe.Down to
                            UserActionResult(
                                shadeScene,
                                requiresFullDistanceSwipe = requiresFullDistanceSwipeToShade,
                            )
                    )
                }

                add(Swipe.Start to Scenes.StubEnd)
                add(Swipe.End to Scenes.StubStart)
                add(
                    Swipe.Up to
                        if (isLockscreenDismissable) {
                            Scenes.Launcher
                        } else {
                            Scenes.Bouncer
                        }
                )

                if (fastSwipeToQuickSettings) {
                    add(Swipe.Down(pointerCount = 2) to Scenes.QuickSettings)
                }
            }
            .toMap()
    }

    object Elements {
        val Scene = ElementKey("LockscreenScene")
        val LockButton = ElementKey("LockscreenLockButton")
    }
}

@Composable
fun ContentScope.Lockscreen(
    notificationList: @Composable ContentScope.() -> Unit,
    mediaPlayer: (@Composable ContentScope.() -> Unit)?,
    isDismissable: Boolean,
    onToggleDismissable: () -> Unit,
    onChangeScene: (SceneKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.element(Lockscreen.Elements.Scene).fillMaxSize()) {
        StatusBar(showDateAndTime = false, Modifier.padding(horizontal = 16.dp))

        Column(Modifier.padding(start = 16.dp, bottom = 16.dp)) {
            Clock(MaterialTheme.colorScheme.onSurfaceVariant)
            SmartSpace(MaterialTheme.colorScheme.onSurface)
        }

        if (mediaPlayer != null) {
            Box(Modifier.padding(horizontal = 16.dp)) { mediaPlayer() }
        }

        Box(Modifier.weight(1f)) { notificationList() }

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

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun ContentScope.LockButton(
    isDismissable: Boolean,
    onToggleDismissable: () -> Unit,
    onChangeScene: (SceneKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .element(Lockscreen.Elements.LockButton)
            .size(70.dp)
            .background(MaterialTheme.colorScheme.surfaceBright, shape = CircleShape)
            .clip(CircleShape)
            .combinedClickable(
                onClick = onToggleDismissable,
                onLongClick = {
                    if (isDismissable) {
                        onChangeScene(Scenes.Launcher)
                    } else {
                        onChangeScene(Scenes.Bouncer)
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        val icon = if (isDismissable) Icons.Default.LockOpen else Icons.Default.Lock
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
internal fun ContentScope.LockscreenCameraButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CameraButton(
        backgroundColor = MaterialTheme.colorScheme.surfaceBright,
        iconColor = MaterialTheme.colorScheme.onSurface,
        onClick,
        modifier.padding(top = 16.dp, start = 16.dp, end = 32.dp, bottom = 32.dp).size(48.dp),
    )
}
