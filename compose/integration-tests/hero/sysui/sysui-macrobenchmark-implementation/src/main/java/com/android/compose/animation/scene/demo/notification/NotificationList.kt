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

package com.android.compose.animation.scene.demo.notification

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.withoutVisualEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.ObservableTransitionState
import com.android.compose.animation.scene.SceneTransitionLayoutState
import com.android.compose.animation.scene.demo.DemoConfiguration
import com.android.compose.animation.scene.demo.Scenes
import com.android.compose.animation.scene.observableTransitionState
import com.android.compose.gesture.NestedScrollableBound
import com.android.compose.modifiers.thenIf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest

class NotificationIdentity

object NotificationList {
    object Elements {
        val Notifications = ElementKey.withIdentity { it is NotificationIdentity }
    }
}

@Composable
fun ContentScope.NotificationList(
    notifications: List<NotificationViewModel>,
    maxNotificationCount: Int?,
    demoConfiguration: DemoConfiguration,
    modifier: Modifier = Modifier,
    isScrollable: Boolean = true,
    overscrollEffect: OverscrollEffect? = null,
) {
    if (demoConfiguration.interactiveNotifications) {
        ExpandFirstNotificationWhenSwipingFromLockscreenToShade(notifications)
    }

    // TODO(b/291025415): Do not share elements that are laid out fully outside the
    // SceneTransitionLayout bounds.
    // TODO(b/291025415): Make sure everything still works when using `LazyColumn` instead of a
    // scrollable `Column`.
    val scrollState = if (isScrollable) rememberScrollState() else null
    Column(
        modifier
            .thenIf(scrollState != null) {
                Modifier.disableSwipesWhenScrolling(NestedScrollableBound.BottomOrRight)
                    .verticalScroll(scrollState!!, overscrollEffect?.withoutVisualEffect())
            }
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        val n = maxNotificationCount ?: notifications.size
        repeat(n) { i ->
            val notification = notifications[i]
            val isFirst = i == 0
            val isLast = i == n - 1

            key(notification.key) {
                Notification(
                    notification,
                    isFirst,
                    isLast,
                    // Make sure that notifications that are shared (which are always the first
                    // n notifications) are always drawn above the notifications that are not
                    // shared.
                    Modifier.zIndex((n - i).toFloat()),
                )
            }
        }
    }
}

@Composable
private fun ContentScope.ExpandFirstNotificationWhenSwipingFromLockscreenToShade(
    notifications: List<NotificationViewModel>
) {
    val firstNotification = notifications.firstOrNull() ?: return
    val coroutineScope = rememberCoroutineScope()
    val layoutState = layoutState

    LaunchedEffect(coroutineScope, layoutState, firstNotification, firstNotification.isExpanded) {
        if (!firstNotification.isExpanded) {
            expandFirstNotificationWhenSwipingFromLockscreenToShade(
                coroutineScope,
                layoutState,
                firstNotification,
            )
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun expandFirstNotificationWhenSwipingFromLockscreenToShade(
    coroutineScope: CoroutineScope,
    layoutState: SceneTransitionLayoutState,
    firstNotification: NotificationViewModel,
) {
    // Wait for the user to release their finger during the next swipe transition from Lockscreen to
    // Shade.
    layoutState
        .observableTransitionState()
        .filterIsInstance<ObservableTransitionState.Transition.ChangeScene>()
        .filter {
            it.isInitiatedByUserInput &&
                it.isTransitioning(from = Scenes.Lockscreen, to = Scenes.Shade)
        }
        .flatMapLatest { it.isUserInputOngoing.combine(it.currentScene) { a, b -> a to b } }
        .first { (isUserInputOngoing, currentScene) ->
            !isUserInputOngoing && currentScene == Scenes.Shade
        }

    // Expand the first notification.
    firstNotification.state.setTargetScene(Notification.Scenes.Expanded, coroutineScope)
}
