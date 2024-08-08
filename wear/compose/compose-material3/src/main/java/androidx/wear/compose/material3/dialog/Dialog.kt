/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3.dialog

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.DialogProperties
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwipeToDismissBox
import androidx.wear.compose.material3.tokens.MotionTokens

/**
 * TODO(b/336715634): A copy of M2 dialog except Scaffold and SwipeToDismissBox. In and Out
 *   animations must be updated according to new motion designs.
 */
@Composable
internal fun Dialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    // Transitions for dialog animation.
    var transitionState by remember {
        mutableStateOf(MutableTransitionState(DialogVisibility.Hide))
    }
    val transition = rememberTransition(transitionState)

    var pendingOnDismissCall by remember { mutableStateOf(false) }

    if (showDialog || transition.currentState == DialogVisibility.Display) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = onDismissRequest,
            properties = properties,
        ) {
            val backgroundScrimAlpha by animateBackgroundScrimAlpha(transition)
            val contentAlpha by animateContentAlpha(transition)
            val scale by animateDialogScale(transition)
            ScreenScaffold(
                modifier = modifier,
            ) {
                SwipeToDismissBox(
                    state = rememberSwipeToDismissBoxState(),
                    modifier =
                        Modifier.graphicsLayer(
                            alpha = backgroundScrimAlpha,
                            scaleX = scale,
                            scaleY = scale,
                        ),
                    onDismissed = {
                        onDismissRequest()
                        // Reset state for the next time this dialog is shown.
                        transitionState = MutableTransitionState(DialogVisibility.Hide)
                    }
                ) { isBackground ->
                    if (!isBackground) {
                        Box(
                            modifier =
                                Modifier.matchParentSize()
                                    .graphicsLayer(alpha = contentAlpha)
                                    .background(MaterialTheme.colorScheme.background)
                        ) {
                            content()
                        }
                    }
                }
            }
            LaunchedEffect(showDialog) {
                if (showDialog) {
                    // a) Fade out previous screen contents b) Scale down dialog contents from 125%
                    transitionState.targetState = DialogVisibility.Display
                    pendingOnDismissCall = true
                } else {
                    // a) Fade out dialog contents b) Scale up dialog contents.
                    transitionState.targetState = DialogVisibility.Hide
                }
            }

            LaunchedEffect(transitionState.currentState) {
                if (
                    pendingOnDismissCall &&
                        transitionState.currentState == DialogVisibility.Hide &&
                        transitionState.isIdle
                ) {
                    // After the outro animation, leave the dialog & reset alpha/scale transitions.
                    onDismissRequest()
                    pendingOnDismissCall = false
                }
            }
        }
    }
}

@Composable
private fun animateBackgroundScrimAlpha(transition: Transition<DialogVisibility>) =
    transition.animateFloat(
        transitionSpec = {
            when (transition.targetState) {
                DialogVisibility.Display ->
                    tween(
                        durationMillis = (MotionTokens.DurationShort3 / 0.9f).toInt(),
                        easing = MotionTokens.EasingLegacyAccelerate
                    )
                DialogVisibility.Hide ->
                    keyframes {
                        // Outro
                        durationMillis = MotionTokens.DurationMedium1 + MotionTokens.DurationShort3
                        1f at 0
                        0.9f at
                            MotionTokens.DurationShort3 using
                            MotionTokens.EasingLegacyDecelerate
                        0.0f at MotionTokens.DurationShort3 + MotionTokens.DurationMedium1
                    }
            }
        },
        label = "background-scrim-alpha"
    ) { stage ->
        when (stage) {
            DialogVisibility.Hide -> 0f
            DialogVisibility.Display -> 1f
        }
    }

@Composable
private fun animateContentAlpha(transition: Transition<DialogVisibility>) =
    transition.animateFloat(
        transitionSpec = {
            when (transition.targetState) {
                DialogVisibility.Display ->
                    keyframes {
                        // Intro
                        durationMillis = MotionTokens.DurationMedium1 + MotionTokens.DurationShort3
                        0.0f at 0
                        0.1f at
                            MotionTokens.DurationShort3 using
                            MotionTokens.EasingLegacyDecelerate
                        1f at MotionTokens.DurationMedium1 + MotionTokens.DurationShort3
                    }
                DialogVisibility.Hide ->
                    tween(
                        durationMillis = (MotionTokens.DurationShort3 / 0.9f).toInt(),
                        easing = MotionTokens.EasingLegacyAccelerate
                    )
            }
        },
        label = "content-alpha"
    ) { stage ->
        when (stage) {
            DialogVisibility.Hide -> 0f
            DialogVisibility.Display -> 1f
        }
    }

@Composable
private fun animateDialogScale(transition: Transition<DialogVisibility>) =
    transition.animateFloat(
        transitionSpec = {
            when (transition.targetState) {
                DialogVisibility.Display ->
                    tween(
                        durationMillis = MotionTokens.DurationMedium4,
                        easing = MotionTokens.EasingLegacyDecelerate
                    )
                DialogVisibility.Hide ->
                    tween(
                        durationMillis = MotionTokens.DurationMedium4,
                        easing = MotionTokens.EasingLegacyAccelerate
                    )
            }
        },
        label = "scale"
    ) { stage ->
        when (stage) {
            DialogVisibility.Hide -> 1.25f
            DialogVisibility.Display -> 1.0f
        }
    }

private enum class DialogVisibility {
    Hide,
    Display
}
