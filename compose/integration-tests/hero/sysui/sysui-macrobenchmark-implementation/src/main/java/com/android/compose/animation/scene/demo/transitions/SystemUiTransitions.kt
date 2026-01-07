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

package com.android.compose.animation.scene.demo.transitions

import androidx.compose.foundation.pager.PagerState
import com.android.compose.animation.scene.InterruptionHandler
import com.android.compose.animation.scene.InterruptionResult
import com.android.compose.animation.scene.SceneKey
import com.android.compose.animation.scene.content.state.TransitionState
import com.android.compose.animation.scene.demo.Scenes
import com.android.compose.animation.scene.reveal.ContainerRevealHaptics
import com.android.compose.animation.scene.transitions
import com.android.mechanics.behavior.VerticalExpandContainerSpec

fun systemUiTransitions(
    qsPagerState: PagerState,
    revealHaptics: ContainerRevealHaptics,
    shadeMotionSpec: VerticalExpandContainerSpec,
) = transitions {
    interruptionHandler = DemoInterruptionHandler

    alwaysOnDisplayTransitions()
    shadeTransitions(qsPagerState)
    splitShadeTransitions()
    quickSettingsTransitions()
    lockscreenTransitions()
    launcherTransitions()
    notificationShadeTransitions(revealHaptics, shadeMotionSpec)
    quickSettingsShadeTransitions(revealHaptics, shadeMotionSpec)
}

object DemoInterruptionHandler : InterruptionHandler {
    override fun onInterruption(
        interrupted: TransitionState.Transition.ChangeScene,
        newTargetScene: SceneKey,
    ): InterruptionResult? {
        return handleLauncherDuringLockscreenToBouncer(interrupted, newTargetScene)
            ?: handleAod(interrupted)
            ?: handleShadeLauncherQuickSettings(interrupted, newTargetScene)
    }

    private fun handleLauncherDuringLockscreenToBouncer(
        transition: TransitionState.Transition,
        targetScene: SceneKey,
    ): InterruptionResult? {
        if (
            targetScene != Scenes.Launcher ||
                !transition.isTransitioningBetween(Scenes.Lockscreen, Scenes.Bouncer)
        ) {
            return null
        }

        // Animate from bouncer only when the bouncer is fully opaque, otherwise animate from
        // lockscreen.
        val animatesFromBouncer =
            if (transition.isTransitioning(to = Scenes.Bouncer)) {
                transition.progress >= BouncerBackgroundEndProgress
            } else {
                transition.progress <= 1f - BouncerBackgroundEndProgress
            }

        return if (animatesFromBouncer) {
            InterruptionResult(
                animateFrom = Scenes.Bouncer,

                // We don't want the content of the lockscreen to be shown during the Bouncer =>
                // Launcher transition. We disable chaining of the transitions so that only the
                // Bouncer and Launcher scenes are composed.
                chain = false,
            )
        } else {
            InterruptionResult(animateFrom = Scenes.Lockscreen)
        }
    }

    private fun handleAod(transition: TransitionState.Transition): InterruptionResult? {
        if (
            transition !is TransitionState.Transition.ChangeScene ||
                (!transition.isTransitioning(from = Scenes.AlwaysOnDisplay) &&
                    !transition.isTransitioning(to = Scenes.AlwaysOnDisplay))
        ) {
            return null
        }

        // Animate from AOD only when it is fully opaque, otherwise animate from the other scene in
        // the transition.
        val otherScene: SceneKey
        val animatesFromAod =
            if (transition.isTransitioning(to = Scenes.AlwaysOnDisplay)) {
                otherScene = transition.fromScene
                transition.progress >= AodBackgroundEndProgress
            } else {
                otherScene = transition.toScene
                transition.progress <= 1f - AodBackgroundEndProgress
            }

        return if (animatesFromAod) {
            InterruptionResult(animateFrom = Scenes.AlwaysOnDisplay, chain = false)
        } else {
            InterruptionResult(animateFrom = otherScene)
        }
    }

    private fun handleShadeLauncherQuickSettings(
        transition: TransitionState.Transition,
        targetScene: SceneKey,
    ): InterruptionResult? {
        if (
            (transition.isTransitioningBetween(Scenes.Shade, Scenes.Launcher) &&
                targetScene == Scenes.QuickSettings) ||
                (transition.isTransitioningBetween(Scenes.Shade, Scenes.QuickSettings) &&
                    targetScene == Scenes.Launcher)
        ) {
            return InterruptionResult(animateFrom = Scenes.Shade)
        }

        if (
            transition.isTransitioningBetween(Scenes.QuickSettings, Scenes.Launcher) &&
                targetScene == Scenes.Shade
        ) {
            val animatesFromQS =
                if (transition.isTransitioning(to = Scenes.QuickSettings)) {
                    transition.progress >= QuickSettingsBackgroundEndProgress
                } else {
                    transition.progress <= 1f - QuickSettingsBackgroundEndProgress
                }

            return InterruptionResult(
                animateFrom = if (animatesFromQS) Scenes.QuickSettings else Scenes.Launcher
            )
        }

        return null
    }
}
