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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.android.compose.animation.scene.demo.notification

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextMeasurer
import com.android.compose.animation.scene.ContentKey
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.MovableElementKey
import com.android.compose.animation.scene.MutableSceneTransitionLayoutState
import com.android.compose.animation.scene.SceneTransitions
import com.android.compose.animation.scene.StaticElementContentPicker
import com.android.compose.animation.scene.content.state.TransitionState
import com.android.compose.animation.scene.demo.Overlays
import com.android.compose.animation.scene.demo.Scenes
import com.android.compose.animation.scene.demo.transitions.QuickSettingsToNotificationShadeFadeProgress
import com.android.compose.animation.scene.demo.transitions.ToNotificationShadeStartFadeProgress
import com.android.compose.animation.scene.demo.transitions.ToShadeScrimFadeEndFraction
import com.android.compose.animation.scene.demo.transitions.ToSplitShadeOpaqueBackgroundProgress

fun notifications(
    isInteractive: Boolean,
    n: Int,
    nInLockscreen: Int,
    textMeasurer: TextMeasurer,
    motionScheme: MotionScheme,
): List<NotificationViewModel> {
    val transitions = NotificationContent.transitions()
    return List(n) { i ->
        val shownInLockscreen = i <= nInLockscreen - 1
        notification(
            isInteractive = isInteractive,
            i = i + 1,
            transitions = transitions,
            textMeasurer = textMeasurer,
            shownInLockscreen = shownInLockscreen,
            motionScheme = motionScheme,
        )
    }
}

private fun notification(
    isInteractive: Boolean,
    i: Int,
    transitions: SceneTransitions,
    textMeasurer: TextMeasurer,
    shownInLockscreen: Boolean,
    motionScheme: MotionScheme,
): NotificationViewModel {
    val key =
        MovableElementKey(
            "Notification:$i",
            identity = NotificationIdentity(),
            contentPicker =
                if (shownInLockscreen) NotificationThatCanBeOnLockscreenPicker
                else NotificationOnlyInShadePicker,
        )

    return object : NotificationViewModel {
        override val key: MovableElementKey = key
        override val state: MutableSceneTransitionLayoutState =
            @Suppress("DEPRECATION") // This code is copied from SysUi and should not diverge
            MutableSceneTransitionLayoutState(
                initialScene = Notification.Scenes.Collapsed,
                motionScheme = motionScheme,
                transitions = transitions,
            )
        override val isInteractive: Boolean = isInteractive

        override val collapsedContent: @Composable ContentScope.() -> Unit = {
            CollapsedNotificationContent(i, textMeasurer)
        }

        override val expandedContent: @Composable ContentScope.() -> Unit = {
            ExpandedNotificationContent(i, textMeasurer)
        }
    }
}

private object NotificationOnlyInShadePicker : StaticElementContentPicker {
    override val contents = setOf(Scenes.Shade, Scenes.SplitShade, Overlays.Notifications)

    override fun contentDuringTransition(
        element: ElementKey,
        transition: TransitionState.Transition,
        fromContentZIndex: Long,
        toContentZIndex: Long,
    ): ContentKey {
        return pickSingleContentIn(contents, transition, element)
    }
}

private object NotificationThatCanBeOnLockscreenPicker : StaticElementContentPicker {
    override val contents =
        setOf(
            Scenes.Lockscreen,
            Scenes.Shade,
            Scenes.SplitLockscreen,
            Scenes.SplitShade,
            Overlays.Notifications,
        )

    override fun contentDuringTransition(
        element: ElementKey,
        transition: TransitionState.Transition,
        fromContentZIndex: Long,
        toContentZIndex: Long,
    ): ContentKey {
        fun handleLockscreenShadeTransition(
            lockscreen: ContentKey,
            shade: ContentKey,
            toShadeOpaqueProgress: Float,
        ): ContentKey? {
            if (transition.isTransitioning(from = lockscreen, to = shade)) {
                return shade
            }

            if (transition.isTransitioning(from = shade, to = lockscreen)) {
                // From Shade to Lockscreen the shared element animation is disabled, so we first
                // show the notification in the shade/scrim as long as the scrim is opaque, then we
                // show them in the lockscreen once the scrim fades out.
                return if (transition.progress < 1f - toShadeOpaqueProgress) {
                    shade
                } else {
                    lockscreen
                }
            }

            return null
        }

        fun handleNotificationToQsOverLockscreen(): ContentKey? {
            if (
                !transition.isTransitioningBetween(
                    Overlays.QuickSettings,
                    Overlays.Notifications,
                ) ||
                    (transition.currentScene != Scenes.Lockscreen &&
                        transition.currentScene != Scenes.SplitLockscreen)
            ) {
                return null
            }

            return if (
                transition.progressTo(Overlays.Notifications) >=
                    QuickSettingsToNotificationShadeFadeProgress
            ) {
                Overlays.Notifications
            } else {
                transition.currentScene
            }
        }

        return handleLockscreenShadeTransition(
            Scenes.Lockscreen,
            Scenes.Shade,
            ToShadeScrimFadeEndFraction,
        )
            ?: handleLockscreenShadeTransition(
                Scenes.SplitLockscreen,
                Scenes.SplitShade,
                ToSplitShadeOpaqueBackgroundProgress,
            )
            ?: handleLockscreenShadeTransition(
                Scenes.Lockscreen,
                Overlays.Notifications,
                ToNotificationShadeStartFadeProgress,
            )
            ?: handleLockscreenShadeTransition(
                Scenes.SplitLockscreen,
                Overlays.Notifications,
                ToNotificationShadeStartFadeProgress,
            )
            ?: handleNotificationToQsOverLockscreen()
            ?: pickSingleContentIn(contents, transition, element)
    }
}
