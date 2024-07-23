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

package androidx.tv.material3

import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.tv.material3.tokens.SurfaceScaleTokens

@Composable
internal fun Modifier.tvSurfaceScale(
    scale: Float,
    interactionSource: MutableInteractionSource,
): Modifier {
    val interaction by
        interactionSource.interactions.collectAsState(initial = FocusInteraction.Focus())

    val animationSpec = defaultScaleAnimationSpec(interaction)

    val animatedScale by
        animateFloatAsState(
            targetValue = scale,
            animationSpec = animationSpec,
            label = "tv-surface-scale"
        )

    return this.graphicsLayer(scaleX = animatedScale, scaleY = animatedScale)
}

private fun defaultScaleAnimationSpec(interaction: Interaction): TweenSpec<Float> =
    tween(
        durationMillis =
            when (interaction) {
                is FocusInteraction.Focus -> SurfaceScaleTokens.focusDuration
                is FocusInteraction.Unfocus -> SurfaceScaleTokens.unFocusDuration
                is PressInteraction.Press -> SurfaceScaleTokens.pressedDuration
                is PressInteraction.Release -> SurfaceScaleTokens.releaseDuration
                is PressInteraction.Cancel -> SurfaceScaleTokens.releaseDuration
                else -> SurfaceScaleTokens.releaseDuration
            },
        easing = SurfaceScaleTokens.enterEasing
    )
