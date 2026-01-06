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

package com.android.compose.animation.scene.demo.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.MovableElementKey
import com.android.compose.animation.scene.MutableSceneTransitionLayoutState
import com.android.compose.animation.scene.SceneKey
import com.android.compose.animation.scene.SceneTransitionLayout
import com.android.compose.animation.scene.Swipe
import com.android.compose.animation.scene.ValueKey
import com.android.compose.animation.scene.animateElementDpAsState
import com.android.compose.modifiers.thenIf

object Notification {
    object Values {
        val BackgroundTopRadius = ValueKey("NotificationBackgroundTopRadius")
        val BackgroundBottomRadius = ValueKey("NotificationBackgroundTopRadius")
    }

    object Scenes {
        val Collapsed = SceneKey("CollapsedNotification")
        val Expanded = SceneKey("ExpandedNotification")
    }
}

@Stable
interface NotificationViewModel {
    val key: MovableElementKey
    val state: MutableSceneTransitionLayoutState
    val isExpanded: Boolean
        get() = state.transitionState.currentScene == Notification.Scenes.Expanded

    /** Whether the user can click or swipe on the notification to expand or collapse it. */
    val isInteractive: Boolean

    /** The different contents of this notification. */
    val collapsedContent: @Composable ContentScope.() -> Unit
    val expandedContent: @Composable ContentScope.() -> Unit
}

@Composable
internal fun ContentScope.Notification(
    viewModel: NotificationViewModel,
    isFirstNotification: Boolean,
    isLastNotification: Boolean,
    modifier: Modifier = Modifier,
) {
    fun targetRadius(isLarge: Boolean): Dp {
        return if (isLarge) 24.dp else 4.dp
    }

    val key = viewModel.key
    MovableElement(key, modifier) {
        val topRadius by
            animateElementDpAsState(
                targetRadius(isLarge = isFirstNotification),
                Notification.Values.BackgroundTopRadius,
            )

        val bottomRadius by
            animateElementDpAsState(
                targetRadius(isLarge = isLastNotification),
                Notification.Values.BackgroundBottomRadius,
            )

        content {
            val backgroundColor = MaterialTheme.colorScheme.surfaceBright
            val contentColor = MaterialTheme.colorScheme.onSurface
            val isInteractive = viewModel.isInteractive

            val coroutineScope = rememberCoroutineScope()
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                SceneTransitionLayout(
                    state = viewModel.state,
                    Modifier.fillMaxWidth()
                        .notificationClip(remember { Path() }, { topRadius }, { bottomRadius })
                        .thenIf(isInteractive) {
                            Modifier.clickable {
                                viewModel.state.setTargetScene(
                                    when (viewModel.state.transitionState.currentScene) {
                                        Notification.Scenes.Expanded ->
                                            Notification.Scenes.Collapsed
                                        else -> Notification.Scenes.Expanded
                                    },
                                    coroutineScope,
                                )
                            }
                        }
                        .background(backgroundColor),
                ) {
                    scene(
                        Notification.Scenes.Collapsed,
                        if (isInteractive) CollapsedUserActions else emptyMap(),
                    ) {
                        viewModel.collapsedContent(/* sceneScope= */ this)
                    }
                    scene(
                        Notification.Scenes.Expanded,
                        if (isInteractive) ExpandedUserActions else emptyMap(),
                    ) {
                        viewModel.expandedContent(/* sceneScope= */ this)
                    }
                }
            }
        }
    }
}

private val CollapsedUserActions = mapOf(Swipe.Down to Notification.Scenes.Expanded)
private val ExpandedUserActions = mapOf(Swipe.Up to Notification.Scenes.Collapsed)
